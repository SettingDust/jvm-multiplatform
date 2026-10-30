// Repository-local orchestration; Maven policy belongs to publishCiMaven.
export type GitResult = { code: number; stdout: Uint8Array };
export type Git = (directory: string, args: string[]) => Promise<GitResult>;
export type Generate = (baseline: string, candidate: string) => Promise<void>;
export type PublishOptions = {
  sourceDirectory: string;
  sourceRef: string;
  sourceSha: string;
  remote: string;
};

const decode = (bytes: Uint8Array) => new TextDecoder().decode(bytes);

export const gitCommand: Git = async (directory, args) => {
  const token = Deno.env.get("GITHUB_TOKEN");
  const env: Record<string, string> = { GIT_TERMINAL_PROMPT: "0" };
  if (token) {
    // Process-only configuration: no token in arguments, URLs or repository files.
    env.GIT_CONFIG_COUNT = "1";
    env.GIT_CONFIG_KEY_0 = "http.https://github.com/.extraheader";
    env.GIT_CONFIG_VALUE_0 = `AUTHORIZATION: basic ${
      btoa(`x-access-token:${token}`)
    }`;
  }
  const result = await new Deno.Command("git", {
    cwd: directory,
    args: ["-c", "core.autocrlf=false", ...args],
    env,
    stdout: "piped",
    stderr: "piped",
  }).output();
  // Git diagnostics can contain credentials supplied by its environment/config.
  return { code: result.code, stdout: result.stdout };
};

export async function generateCandidate(
  sourceDirectory: string,
  baseline: string,
  candidate: string,
): Promise<void> {
  const windows = Deno.build.os === "windows";
  const args = [
    "--no-configure-on-demand",
    "--no-configuration-cache",
    "--no-parallel",
    "--console=plain",
    "-Porg.gradle.java.installations.fromEnv=JAVA_HOME_8_X64,JAVA_HOME_11_X64",
    `-PciMavenBaselineDirectory=${baseline}`,
    `-PciMavenDirectory=${candidate}`,
    "publishCiMaven",
  ];
  const result = await new Deno.Command(windows ? "cmd.exe" : "./gradlew", {
    cwd: sourceDirectory,
    args: windows ? ["/d", "/s", "/c", "gradlew.bat", ...args] : args,
    env: { GITHUB_TOKEN: "" },
    stdout: "inherit",
    stderr: "inherit",
  }).output();
  if (!result.success) {
    throw new Error(`Candidate generation failed (exit ${result.code})`);
  }
}

export async function publishMaven(
  options: PublishOptions,
  generate: Generate = (baseline, candidate) =>
    generateCandidate(options.sourceDirectory, baseline, candidate),
  git: Git = gitCommand,
): Promise<string> {
  if (options.sourceRef !== "refs/heads/main") {
    return "skipped: ineligible source ref";
  }
  if (!/^[0-9a-f]{40}$/.test(options.sourceSha)) {
    throw new Error("Invalid event SHA");
  }

  const checked = async (directory: string, args: string[]) => {
    const result = await git(directory, args);
    if (result.code !== 0) {
      throw new Error(`Git ${args[0]} failed (exit ${result.code})`);
    }
    return result.stdout;
  };
  const head = async (
    directory: string,
    ref: string,
    absentAllowed = false,
  ) => {
    const result = await git(directory, [
      "ls-remote",
      "--exit-code",
      options.remote,
      ref,
    ]);
    if (result.code === 2 && absentAllowed) return undefined;
    if (result.code !== 0) {
      throw new Error(`Remote ${ref} lookup failed (exit ${result.code})`);
    }
    const lines = decode(result.stdout).trim().split("\n");
    const match = lines.length === 1 &&
      lines[0].match(/^([0-9a-f]{40})\s+(.+)$/);
    if (!match || match[2] !== ref) {
      throw new Error(`Invalid remote ${ref} response`);
    }
    return match[1];
  };

  const actualSource = decode(
    await checked(options.sourceDirectory, ["rev-parse", "HEAD"]),
  ).trim();
  if (actualSource !== options.sourceSha) {
    throw new Error("Checkout does not match event SHA");
  }

  // Default OS temp is outside the checkout; Gradle also guards source/cache overlap.
  const temporary = await Deno.makeTempDir({ prefix: "jvm-maven-" });
  try {
    const repository = `${temporary}/git`;
    const baseline = `${temporary}/baseline`;
    const candidate = `${temporary}/candidate`;
    await Deno.mkdir(repository);
    await Deno.mkdir(baseline);
    await Deno.mkdir(candidate);
    await checked(repository, ["init", "--bare", "."]);
    let parent: string | undefined;
    if (await head(repository, "refs/heads/maven", true)) {
      await checked(repository, [
        "fetch",
        "--no-tags",
        options.remote,
        "refs/heads/maven",
      ]);
      parent = decode(await checked(repository, ["rev-parse", "FETCH_HEAD"]))
        .trim();
      const entries = decode(
        await checked(repository, ["ls-tree", "-rz", parent]),
      );
      for (const entry of entries.split("\0").filter(Boolean)) {
        const match = entry.match(/^100644 blob ([0-9a-f]{40})\t([\s\S]+)$/);
        if (!match) {
          throw new Error("Maven baseline must contain regular files only");
        }
        const [, object, path] = match;
        if (
          path.split("/").some((part) =>
            part === ".." || part.toLowerCase() === ".git"
          )
        ) {
          throw new Error("Unsafe Maven baseline path");
        }
        const file = `${baseline}/${path}`;
        await Deno.mkdir(file.slice(0, file.lastIndexOf("/")), {
          recursive: true,
        });
        await Deno.writeFile(
          file,
          await checked(repository, ["cat-file", "blob", object]),
        );
      }
    }

    await generate(baseline, candidate);
    // Validation is Gradle's responsibility; staging still rejects an empty tree.
    await checked(repository, [
      "--work-tree",
      candidate,
      "read-tree",
      "--empty",
    ]);
    await checked(repository, [
      "--work-tree",
      candidate,
      "add",
      "--all",
      "--force",
      ".",
    ]);
    const tree = decode(await checked(repository, ["write-tree"])).trim();
    if (!decode(await checked(repository, ["ls-tree", tree])).trim()) {
      throw new Error("Candidate generation produced an empty tree");
    }
    const previousTree = parent
      ? decode(await checked(repository, ["rev-parse", `${parent}^{tree}`]))
        .trim()
      : undefined;
    let commit: string | undefined;
    if (tree !== previousTree) {
      commit = decode(
        await checked(repository, [
          "-c",
          "user.name=github-actions[bot]",
          "-c",
          "user.email=41898282+github-actions[bot]@users.noreply.github.com",
          "-c",
          "commit.gpgsign=false",
          "commit-tree",
          tree,
          ...(parent ? ["-p", parent] : []),
          "-m",
          `Publish Maven from ${options.sourceSha}`,
        ]),
      ).trim();
    }
    // This checkpoint applies equally to reruns and no-change candidates.
    if (await head(repository, "refs/heads/main") !== options.sourceSha) {
      return "skipped: stale source SHA";
    }
    if (!commit) return "no-change: complete candidate tree is identical";

    const pushed = await git(repository, [
      "push",
      "--porcelain",
      options.remote,
      `${commit}:refs/heads/maven`,
    ]);
    if (pushed.code !== 0) {
      // One attempt only. A transport failure may follow a successful remote update.
      if (await head(repository, "refs/heads/maven", true) !== commit) {
        throw new Error(
          `Maven push failed or is unconfirmed (exit ${pushed.code})`,
        );
      }
      return `published: ${commit} (confirmed after uncertain push)`;
    }
    return `published: ${commit}`;
  } finally {
    await Deno.remove(temporary, { recursive: true });
  }
}

if (import.meta.main) {
  try {
    const required = (key: string) => {
      const value = Deno.env.get(key);
      if (!value) throw new Error(`Missing ${key}`);
      return value;
    };
    console.log(
      await publishMaven({
        sourceDirectory: Deno.cwd(),
        sourceRef: required("GITHUB_REF"),
        sourceSha: required("GITHUB_SHA"),
        remote: `https://github.com/${required("GITHUB_REPOSITORY")}.git`,
      }),
    );
  } catch (error) {
    console.error(
      error instanceof Error ? error.message : "Maven publication failed",
    );
    Deno.exitCode = 1;
  }
}
