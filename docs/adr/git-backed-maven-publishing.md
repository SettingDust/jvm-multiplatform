# Git-backed Maven publishing

Status: Accepted. This records the user-confirmed architecture for jvm-multiplatform; it does not establish that CI is implemented or publication has been verified.

## Context

The accepted publishing arrangement gives each project a Maven repository in its own GitHub repository. This ADR owns the jvm-multiplatform publishing decision. Existing module documentation and implementation plans concern other responsibilities and are not publishing authority. No applicable publishing ADR or Spec was found when this record was prepared.

CI preparation must use the current local working tree without synchronizing remote source changes. This is infrastructure work without a change to product requirements, so no PRD change is required.

## Decision

- Publish to the `maven` branch of `github.repository`, with the Maven repository layout at the branch root.
- Automatically publish on pushes to `main` that change `**/gradle.properties`, and provide `workflow_dispatch` for manual execution. Pull requests, other source branches, and tags must not automatically publish.
- Do not create or move source tags as part of publishing.
- Use the same repository's `GITHUB_TOKEN` with `contents: write` for branch publication.
- Generate the project's actual Maven publications, including Gradle plugin marker publications, into a dedicated local output directory. Publish that output rather than copying the entire `mavenLocal` repository.
- Preserve existing HTTP publishing capability, while keeping it out of the new CI publishing path.

This decision does not introduce an external Deno publisher, a shared publishing framework, or a manifest/receipt system.

## Rationale

Using `github.repository` keeps publication ownership and write credentials within the project repository. A root-level Maven layout makes the branch itself the repository content. A dedicated output directory limits the published content to the project's actual publications, including plugin markers, without importing unrelated local Maven artifacts.

The accepted trigger boundary connects automatic publication to version-property changes on `main`, while manual dispatch provides an explicit execution path. Leaving source tags untouched keeps publication separate from source-tag management. Retaining HTTP support preserves that existing capability without making it a dependency of the new CI path.

## Consequences and follow-up

The `maven` branch becomes the project's Git-backed Maven publication destination. CI will need repository-content write permission and must preserve the repository layout and complete publication set. Consumers using an older destination may need to update their repository configuration; this ADR does not claim that any consumer migration has occurred.

The handoff reports that the three projects' former `maven-repo` branches were already deleted and their absence checked with `ls-remote`. That is supplied historical evidence, not a remote check performed for this ADR. The central `SettingDust/maven` repository is retained. The separate Codev project is to replace its old invalid publishing workflow; this ADR does not implement or own that replacement.

The [Maven publication CI Spec](../spec/maven-publication-ci.md) defines the publishing contract. The user-confirmed policies close the former follow-up questions: identical complete release payloads are skipped; any release-payload difference fails and requires a version bump; snapshots use Gradle's native updates and retain historical snapshots without cleanup. Both automatic and manual publication are limited to `main`, with the built source commit checked against current remote `main` HEAD immediately before publication; stale runs skip publication and the still-current commit may be rerun. This is a source-freshness rule, not a guarantee of increasing version numbers.

The accepted output arrangement seeds the dedicated directory from the existing `maven` branch, or an empty Maven tree when absent, and lets Gradle's native publisher maintain metadata and checksums while preserving history. The Spec defines the boundary between immutable release payloads and mutable repository metadata. CI configuration, builds, commits, and live publication are outside this documentation change.

## Revisit criteria

Reconsider this decision if Git-backed storage or retrieval no longer meets publication needs, repository token permissions cannot support the workflow, or the later Spec exposes a conflict or metadata constraint that requires changing this architecture. Record any accepted replacement through this decision's lifecycle.
