# Fork 版本与发布约定

本规则适用于本仓库所有模块的版本维护与发布。

- 每个模块独立维护 fork SemVer，版本格式为 `<独立维护的fork SemVer>+terrarium.<该模块上游基版本>`。前缀是 fork 自身版本，不是上游版本的复制；后缀记录该模块对应的上游基版本。
- 仅对发生变化的模块改号。发布依赖的 POM / Gradle Module Metadata 变化也属于发布 payload 变化；未改模块不得为了统一格式而批量改号。
- 已发布的正式版本不可覆盖；不同发布内容必须使用新的 fork SemVer。插件 marker 与其 implementation 版本必须一致。
- 显式 IDEA SNAPSHOT 保持原生历史版本规则；不得为根项目的 `unspecified` 擅造版本。

发布前检查受影响模块的实际发布 payload（包括依赖 POM / Gradle Module Metadata），确认变化已使用新的 fork SemVer、上游基版本准确、marker 与 implementation 版本一致，且不会覆盖已发布正式版本；未变化模块保持原版本。
