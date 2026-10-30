import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory

check(project == rootProject) { "Apply Maven CI wiring only to the root project" }

val ciDirectory = providers.gradleProperty("ciMavenDirectory")
val inspectCiMaven =
    tasks.register("inspectCiMaven") {
        group = "publishing"
        description = "Inspect every final Maven publication and its ciMaven destination task."
    }
val validateCiMaven =
    tasks.register("validateCiMaven") {
        group = "publishing"
        description = "Validate the named Maven candidate path and complete publication inventory."
    }
val prepareCiMaven =
    tasks.register("prepareCiMaven") {
        group = "publishing"
        description = "Seed the candidate from its immutable baseline and clear release payloads."
    }
val publishCiMaven =
    tasks.register("publishCiMaven") {
        group = "publishing"
        description = "Publish and verify all Maven publications in the named filesystem repository."
    }

fun canonicalPath(file: File) = file.canonicalFile.toPath()

fun overlaps(
    first: Path,
    second: Path,
) = first == second || first.startsWith(second) || second.startsWith(first)

fun isGitPath(path: Path) = path.any { it.toString().equals(".git", ignoreCase = true) }

allprojects {
    pluginManager.withPlugin("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories.maven {
                name = "ciMaven"
                url = uri(ciDirectory.getOrElse(File(System.getProperty("java.io.tmpdir"), "cloche-ci-maven-unconfigured").path))
            }
        }
    }
}

gradle.projectsEvaluated {
    val publications =
        allprojects.sortedBy { it.path }.flatMap { owner ->
            owner.extensions
                .findByType(PublishingExtension::class.java)
                ?.publications
                ?.withType(MavenPublication::class.java)
                ?.map { owner to it }
                .orEmpty()
        }

    fun identity(publication: MavenPublication) = "${publication.groupId}:${publication.artifactId}:${publication.version}"

    fun versionPath(publication: MavenPublication) =
        "${publication.groupId.replace('.', '/')}/${publication.artifactId}/${publication.version}"

    fun safe(value: String) = value.matches(Regex("[A-Za-z0-9_][A-Za-z0-9_.+-]*")) && value != "." && value != ".."

    fun verifyInventory() {
        check(publications.isNotEmpty()) { "No Maven publications configured" }
        val paths = mutableSetOf<String>()
        publications.forEach { (_, publication) ->
            check(publication.groupId.split('.').all(::safe) && safe(publication.artifactId) && safe(publication.version)) {
                "Unsafe Maven identity: ${identity(publication)}"
            }
            val path = versionPath(publication)
            check(paths.add(if (System.getProperty("os.name").startsWith("Windows")) path.lowercase() else path)) {
                "Duplicate Maven version destination: ${identity(publication)}"
            }
            publication.artifacts.forEach { artifact ->
                check(safe(artifact.extension) && (artifact.classifier.isNullOrEmpty() || safe(artifact.classifier.orEmpty()))) {
                    "Unsafe Maven artifact: $artifact"
                }
            }
        }
    }

    val destinationTasks =
        publications.map { (owner, publication) ->
            val matches =
                owner.tasks.withType(PublishToMavenRepository::class.java).filter {
                    it.repository.name == "ciMaven" && it.publication == publication
                }
            check(matches.size == 1) { "Expected one ciMaven task for ${owner.path}/${publication.name}, found ${matches.size}" }
            matches.single()
        }

    fun currentDirectories(): Pair<File, File?> {
        val candidate = file(ciDirectory.get()).canonicalFile
        val baseline = providers.gradleProperty("ciMavenBaselineDirectory").orNull?.let { file(it).canonicalFile }
        val protected =
            listOf(rootDir, gradle.gradleUserHomeDir, File(System.getProperty("user.home"), ".m2/repository"))
                .map(::canonicalPath)
        val candidatePath = canonicalPath(candidate)
        check(protected.none { overlaps(candidatePath, it) }) {
            "ciMavenDirectory must not overlap source or caches: $candidate"
        }
        baseline?.let {
            val baselinePath = canonicalPath(it)
            check(protected.none { protectedPath -> overlaps(baselinePath, protectedPath) }) {
                "ciMavenBaselineDirectory must not overlap source or caches: $it"
            }
            check(!overlaps(candidatePath, baselinePath)) {
                "ciMavenDirectory and ciMavenBaselineDirectory must be separate"
            }
            check(!isGitPath(candidatePath) && !isGitPath(baselinePath)) { "Git internals are not Maven directories" }
            check(it.isDirectory) { "ciMavenBaselineDirectory must be an existing directory: $it" }
        }
        check(!isGitPath(candidatePath)) { "Git internals are not a Maven destination" }
        return candidate to baseline
    }

    fun relativeFiles(root: File): Set<String> =
        if (!root.isDirectory) {
            emptySet()
        } else {
            root
                .walkTopDown()
                .filter(File::isFile)
                .map { it.relativeTo(root).invariantSeparatorsPath }
                .toSet()
        }

    fun sameBytes(
        first: File,
        second: File,
    ) = first.isFile && second.isFile && first.length() == second.length() && first.readBytes().contentEquals(second.readBytes())

    fun sameTree(
        first: File,
        second: File,
    ): Boolean {
        val files = relativeFiles(first)
        return files == relativeFiles(second) && files.all { sameBytes(first.resolve(it), second.resolve(it)) }
    }

    val checksumAlgorithms = listOf("md5" to "MD5", "sha1" to "SHA-1", "sha256" to "SHA-256", "sha512" to "SHA-512")
    val checksumSuffixes = checksumAlgorithms.map { ".${it.first}" }.toSet()

    fun verifyRepositoryFile(file: File) {
        check(file.isFile && file.length() > 0) { "Missing Maven output: $file" }
        checksumAlgorithms.forEach { (suffix, algorithm) ->
            val checksum = File("${file.path}.$suffix")
            val digest = MessageDigest.getInstance(algorithm).digest(file.readBytes()).joinToString("") { "%02x".format(it) }
            check(checksum.isFile && checksum.readText().trim().equals(digest, ignoreCase = true)) {
                "Invalid or missing checksum: $checksum"
            }
        }
    }

    fun verifyRepositoryTree(root: File) {
        check(root.isDirectory) { "Missing Maven directory: $root" }
        root.walkTopDown().filter(File::isFile).forEach { file ->
            if (checksumSuffixes.none { file.name.endsWith(it) }) {
                verifyRepositoryFile(file)
            } else {
                val base = file.name.substringBeforeLast('.')
                check(root.resolve(file.relativeTo(root).parent ?: "").resolve(base).isFile) {
                    "Orphan Maven checksum: $file"
                }
            }
        }
    }

    val xml =
        DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }

    fun xmlField(
        element: org.w3c.dom.Element,
        name: String,
    ) = (0 until element.childNodes.length)
        .map { element.childNodes.item(it) }
        .single { it.nodeName == name }
        .textContent
        .trim()

    fun verifyPom(
        file: File,
        owner: Project,
        publication: MavenPublication,
    ) {
        verifyRepositoryFile(file)
        val model = xml.newDocumentBuilder().parse(file).documentElement
        check(
            xmlField(model, "groupId") == publication.groupId &&
                xmlField(model, "artifactId") == publication.artifactId &&
                xmlField(model, "version") == publication.version,
        ) { "POM identity mismatch: $file" }
        if (publication.name.endsWith("PluginMarkerMaven")) {
            val implementation =
                owner.extensions
                    .getByType(
                        PublishingExtension::class.java,
                    ).publications
                    .getByName("pluginMaven") as MavenPublication
            val dependencies = model.getElementsByTagName("dependency")
            check(dependencies.length == 1) { "Expected one marker implementation dependency: $file" }
            val dependency = dependencies.item(0) as org.w3c.dom.Element
            check(
                xmlField(dependency, "groupId") == implementation.groupId &&
                    xmlField(dependency, "artifactId") == implementation.artifactId &&
                    xmlField(dependency, "version") == implementation.version,
            ) { "Marker implementation mismatch: $file" }
        }
    }

    fun moduleIsPublished(
        owner: Project,
        publication: MavenPublication,
    ) = owner.tasks
        .withType(GenerateModuleMetadata::class.java)
        .filter { it.publication.get() == publication }
        .any { it.enabled && it.onlyIf.isSatisfiedBy(it) }

    fun verifyPublication(
        owner: Project,
        publication: MavenPublication,
        candidate: File,
        baseline: File?,
    ) {
        val directory = candidate.resolve(versionPath(publication))
        val baselineDirectory = baseline?.resolve(versionPath(publication))
        val snapshot = publication.version.endsWith("-SNAPSHOT")
        val metadata =
            candidate
                .resolve(
                    publication.groupId.replace('.', '/'),
                ).resolve(publication.artifactId)
                .resolve("maven-metadata.xml")
        verifyRepositoryFile(metadata)

        if (!snapshot) {
            val stem = "${publication.artifactId}-${publication.version}"
            val pom = directory.resolve("$stem.pom")
            verifyPom(pom, owner, publication)
            publication.artifacts.forEach { artifact ->
                val classifier =
                    artifact.classifier
                        ?.takeIf(String::isNotEmpty)
                        ?.let { "-$it" }
                        .orEmpty()
                verifyRepositoryFile(directory.resolve("$stem$classifier.${artifact.extension}"))
            }
            if (moduleIsPublished(owner, publication)) verifyRepositoryFile(directory.resolve("$stem.module"))
            verifyRepositoryTree(directory)
            if (baselineDirectory?.exists() == true) {
                check(sameTree(directory, baselineDirectory)) {
                    "Release payload conflict for ${identity(publication)}; use a version bump"
                }
            }
        } else {
            check(directory.isDirectory) { "Missing snapshot directory: $directory" }
            val snapshotMetadata = directory.resolve("maven-metadata.xml")
            verifyRepositoryFile(snapshotMetadata)
            val model = xml.newDocumentBuilder().parse(snapshotMetadata).documentElement
            check(
                xmlField(model, "groupId") == publication.groupId &&
                    xmlField(model, "artifactId") == publication.artifactId &&
                    xmlField(model, "version") == publication.version,
            ) { "Snapshot metadata identity mismatch: $snapshotMetadata" }
            val versions = model.getElementsByTagName("snapshotVersion")
            fun snapshotFile(extension: String, classifier: String = ""): File {
                val entries = (0 until versions.length).map { versions.item(it) as org.w3c.dom.Element }
                val entry = entries.singleOrNull {
                    xmlField(it, "extension") == extension &&
                        it.getElementsByTagName("classifier").let { nodes ->
                            if (nodes.length == 0) classifier.isEmpty() else nodes.item(0).textContent.trim() == classifier
                        }
                }
                check(entry != null) { "Missing or ambiguous current snapshot entry for ${identity(publication)}: $extension/$classifier" }
                val value = xmlField(entry, "value")
                check(safe(value)) { "Unsafe snapshot value: $value" }
                val suffix = classifier.takeIf(String::isNotEmpty)?.let { "-$it" }.orEmpty()
                return directory.resolve("${publication.artifactId}-$value$suffix.$extension")
            }
            verifyPom(snapshotFile("pom"), owner, publication)
            publication.artifacts.forEach { artifact ->
                verifyRepositoryFile(snapshotFile(artifact.extension, artifact.classifier.orEmpty()))
            }
            if (moduleIsPublished(owner, publication)) verifyRepositoryFile(snapshotFile("module"))
            verifyRepositoryTree(directory)
            baselineDirectory?.takeIf(File::isDirectory)?.let { previous ->
                relativeFiles(previous)
                    .filter { relative ->
                        !relative.substringAfterLast('/').let { it == "maven-metadata.xml" || checksumSuffixes.any(it::endsWith) }
                    }.forEach { relative ->
                        check(sameBytes(previous.resolve(relative), directory.resolve(relative))) {
                            "Snapshot history was changed for ${identity(publication)}: $relative"
                        }
                    }
            }
        }
        logger.lifecycle("CI_VERIFIED ${identity(publication)}|${directory.absolutePath}")
    }

    validateCiMaven.configure {
        doLast {
            check(
                !gradle.startParameter.isConfigureOnDemand && !gradle.startParameter.isParallelProjectExecutionEnabled &&
                    !gradle.startParameter.isConfigurationCacheRequested,
            ) {
                "Use --no-configure-on-demand --no-configuration-cache --no-parallel"
            }
            verifyInventory()
            currentDirectories()
        }
    }

    prepareCiMaven.configure {
        dependsOn(validateCiMaven)
        doLast {
            val (candidate, baseline) = currentDirectories()
            if (baseline != null) {
                if (candidate.exists()) check(candidate.deleteRecursively()) { "Cannot clear candidate: $candidate" }
                check(candidate.mkdirs()) { "Cannot create candidate: $candidate" }
                baseline.copyRecursively(candidate, overwrite = false)
            } else {
                if (candidate.exists()) {
                    check(
                        relativeFiles(candidate).isEmpty(),
                    ) { "Candidate must be empty when no immutable baseline is supplied: $candidate" }
                } else {
                    check(candidate.mkdirs()) { "Cannot create candidate: $candidate" }
                }
            }
            publications.filterNot { it.second.version.endsWith("-SNAPSHOT") }.forEach { (_, publication) ->
                val releaseDirectory = candidate.resolve(versionPath(publication))
                if (releaseDirectory.exists()) {
                    check(
                        releaseDirectory.deleteRecursively(),
                    ) { "Cannot clear release payload: $releaseDirectory" }
                }
            }
            check(!isGitPath(canonicalPath(candidate))) { "Git internals are not a Maven candidate" }
            logger.lifecycle("CI_CANDIDATE_PREPARED candidate=${candidate.absolutePath} baseline=${baseline?.absolutePath ?: "<empty>"}")
        }
    }

    destinationTasks.forEach { task ->
        task.dependsOn(prepareCiMaven)
        task.outputs.upToDateWhen { false }
    }

    inspectCiMaven.configure {
        doLast {
            verifyInventory()
            allprojects.sortedBy { it.path }.forEach { owner ->
                if (publications.none { it.first == owner }) logger.lifecycle("CI_MAVEN_EMPTY ${owner.path}")
            }
            publications.zip(destinationTasks).forEach { (entry, task) ->
                val (owner, publication) = entry
                logger.lifecycle(
                    "CI_MAVEN ${owner.path}|${publication.name}|${identity(publication)}|${task.path}|${versionPath(publication)}",
                )
                publication.artifacts.forEach { artifact ->
                    logger.lifecycle(
                        "CI_ARTIFACT ${identity(publication)}|${artifact.classifier.orEmpty()}|${artifact.extension}|${artifact.file}",
                    )
                }
            }
        }
    }

    publishCiMaven.configure {
        dependsOn(destinationTasks)
        doLast {
            val (candidate, baseline) = currentDirectories()
            publications.forEach { (owner, publication) -> verifyPublication(owner, publication, candidate, baseline) }
            logger.lifecycle("CI_MAVEN_COMPLETE publications=${publications.size} candidate=${candidate.absolutePath}")
        }
    }
}
