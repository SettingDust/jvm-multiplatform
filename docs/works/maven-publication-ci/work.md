# Maven publication CI Work

[Context](context.md) | [Plan](plan.md) | [Waves](waves.md)

Prepare and deliver the repository-local Maven publication path defined by the [Spec](../../spec/maven-publication-ci.md) and accepted [ADR](../../adr/git-backed-maven-publishing.md). These four documents are derived delivery material; the Spec and ADR own policy.

## Current boundary

The original independent review completed and the implementation scope was confirmed. Historical `implement-jvm-maven-local` g1 remains `failed` during settings ABI configuration; the later Gradle 9.8.2/KSP configuration check did not complete a producer. The latest adopted `implement-jvm-maven-local-tree` generation 2 is terminal **partial**, despite outer runtime `completed`: one real named-repository publication succeeded for all 14 dynamically observed publications under Microsoft JDK 17, but plugin-ID/Java 11 consumption and complete runtime/toolchain evidence remain missing. Candidate-policy code was also written prematurely without S2 acceptance. [Context](context.md#mixed-generation-2-partial-and-retained-effects) preserves those effects and limits; no item or Maven gate is complete.

This C5 maintenance keeps S1 producer/consumer and S2 candidate policy independent. Historical `I-local-recovery` is retained, not resumed or relabelled completed. New `I-local-acceptance-recovery` has its own future Handler/Result and `G-local-acceptance-recovery`, carrying the full original S1 predicate; `I-candidate` still waits for that S1 Result and gate. Reuse applicable existing output/evidence first; missing consumption is not permission to replay all publication/policy work. The changed recovery boundary has an author self-check and requires independent local review before affected implementation; this record neither dispatches work nor releases gates.

Only this Work/Context/Plan/Waves set is changed here. No implementation, build, test, publication, Git write, cache clearing, LSP disabling or process termination is performed. The user's P4/Cloche/Codev deferral remains intact, as recorded in the [migration closeout](../kotlin-2410-plugin-migration/context.md#accepted-closeout-and-deferred-actual-boundary); Maven output does not prove P4 acceptance. Advice about test bloat is not deletion authorization.

The selected tuple is `docs/works/maven-publication-ci/work.md`, `docs/works/maven-publication-ci/context.md`, `docs/works/maven-publication-ci/plan.md`, and `docs/works/maven-publication-ci/waves.md`, relative to this repository. There is one unsliced Plan/Waves pair, no Tasks layer and no module-feature-plan expansion.

## Outcome and completion signal

The future outcome is a main-only workflow that generates every configured Maven publication, validates release immutability and snapshot/history preservation, and updates this repository's `maven` branch safely. The Plan defines scope and verification; Waves is the sole executable graph. Completion requires all three gates identified in the Plan with their required evidence. Local acceptance cannot establish that hosted CI, token permissions, or a real remote publication succeeded.

No central-repository migration, old-branch recreation, source-tag changes, consumer migration, shared publication framework, manifest, receipt, or unrelated module work belongs to this Work. Existing HTTP publishing remains usable and excluded from the CI task graph.
