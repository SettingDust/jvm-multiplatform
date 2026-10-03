# Plugin Marker / 入口集成记录

## 边界与状态

`I-plugin` 已完成并经 Router 验证；本记录与四个普通 Work 文档构成 `I-plugin-docs` 的五路径签名交付。签名身份、精确 docs diff 与完成证据由本次 Result 提供，不在提交内自引用新 docs SHA。`W-plugin` 的 release 仍要求消费并验证该 Result；`I-final` 独立待验，Work 未整体验收。本文是派生证据入口，不改变任何 Spec，不恢复发布流水线。

完整 [plugin code Result](C:/Users/WINDOWS/.pi/agent/sessions/--E--Projects-cloche--/2026-09-26T19-48-28-195Z_01a0df43-0ba3-7696-8006-8634ea05c989/tasks/plugin-code-integration-main-rebuild-artifacts/result-1.json) 已逐字消费（generation 1，内层/外层均 `completed`；SHA-256 `9C9DBDC7F6D561A0B23807B9634E15A954A0763E634D8C6A6EA98B0DB630A40F`）。其 [最终核验](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-20261004-01/final-verification.json)、[73 文件证据清单](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-20261004-01/artifact-hashes.json)、[33 测试输入](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-20261004-01/tested-inputs.json) 与 [FIR27/Actual44 复用映射](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-20261004-01/reused-inputs.json) 是该实现候选的证据，不是新执行的测试。

## 恢复与签名实现候选

- 恢复 payload 来自 `refs/heads/recovery/i-preserve-20261003-01/plugin` → `1c917c0a16aacac07758de35416605c86814f439`，其 parent 是旧 main `cd623a95a75f07639f35a2bbf1a36f1926605551`。仅移植 delta，未合入恢复分支祖先。
- feature `fix/plugin-main-rebuild-20261004-01`：`3c47f3d56e461c0a82ab650cec7566d68e341f5e`，parent `0ebd81fb3d988d9bc1fd8e54294283b25354f2ac`。
- main code merge：`d1d260f08b35d37656e111facc5995a0aa3cf9dc`；有序 parents 为 `0ebd81fb3d988d9bc1fd8e54294283b25354f2ac`、`3c47f3d56e461c0a82ab650cec7566d68e341f5e`。两者 tree 均为 `5b653e58ab9078018e2f3c51d84eeacd6c64ed60`，均为 Good EDDSA，key `3B992602764F685EE1DD065D4FFAF1C206BF8586`；docs writer 再次核验成功。
- code first-parent diff 只有 `classpath-api-stubs/build.gradle.kts`、`classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/ClasspathApiStubsPlugin.kt`、`classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/ClasspathApiStubsPluginTest.kt`。保留 suffix 版本，未动 FIR/Actual、其他 classpath 实现或 release 流水线。
- [payload mapping](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-20261004-01/payload-mapping.json) 区分恢复与最终 blob：11 行入口和 177 行 marker 测试均与 recovery blob 一致；build 仅增加 27 行 UUID 仓库、两个本地 publication 依赖和四个 marker 测试属性，保留已有 launcher、classpath、依赖与 `jvmToolchain(8)`。入口仅 lazy register `generateStubApi`（`GenerateStubApi`），输入由调用者配置，未应用额外插件。

## 真实 marker、任务输出

最终有效 fixture 从 UUID `markerTest` Maven 本地仓库通过插件 DSL 应用 `net.msrandom.classpath-api-stubs`，不使用 `withPluginClasspath`。精确坐标：

```text
net.msrandom.classpath-api-stubs:net.msrandom.classpath-api-stubs.gradle.plugin:0.1.12+terrarium.0.1.12
  -> net.msrandom:classpath-api-stubs:0.1.12+terrarium.0.1.12
```

两次有效执行均显式 `-Pgroup=net.msrandom`。此前不同 group 的 isolated 执行只留作历史，不能替代最终坐标证据。`test` 的新增本地 publication 依赖仅为 `publishPluginMavenPublicationToMarkerTestRepository` 和 `publishClasspathApiStubsPluginMarkerMavenPublicationToMarkerTestRepository`；这两个命名的本地 tasks 已在 code 验证阶段运行，用户取消的 pipeline/远端发布未运行。

真实 `GenerateStubApi` 处理两份不同 API jar：输出仅保留 `fixture/Common.class` 与公共静态 `common()I`，单边 `onlyLeft()`/`onlyRight()` 以及 `fixture/OnlyLeft.class`/`fixture/OnlyRight.class` 去掉；断言默认输出名 `api-stub.jar`、`preserveMethodBodies=false`、输出 class major52 与默认多输入无 `Code`，输出 JAR 可读，两份输入哈希在生成前后不变。fixture 未断言 field 或 manifest 行为，不把其他测试覆盖冒充为 marker 测试覆盖。[最终 marker/output 摘录](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-20261004-01/check-marker-output.txt) 记录了 production marker/implementation 与输入/输出 SHA-256、真实任务类型、`generateStubApi` 成功，而非只验证插件可 apply。

## 最终检查与运行时范围

| 保存执行 | 结果 | 证据 |
| --- | --- | --- |
| root wrapper 9.2.0，offline | 配置阶段 `org.gradle.kotlin.embedded-kotlin:6.4.1` 无法解析；测试没有运行 | [root invocation](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-20261004-01/root-invocation.json) / [root log](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-20261004-01/root.log) |
| isolated focused plugin test，最终 group | exit 0；1 XML、1 test、0 failures/errors/skips | [focused invocation](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-20261004-01/focused-invocation.json) / [focused XML](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-20261004-01/focused-xml/TEST-net.msrandom.stubs.ClasspathApiStubsPluginTest.xml) |
| isolated `:classpath-api-stubs:check`，最终 group | exit 0；10 XML、27 tests、0 failures/errors/skips（含 plugin fixture） | [check invocation](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-20261004-01/check-invocation.json) / [XML identities](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-20261004-01/reports.json) |

有效命令都使用物理 `D:/Applications/Scoop/apps/gradle/9.5.1/bin/gradle.bat -p E:/Projects/cloche/jvm-multiplatform/classpath-api-stubs/verification`、`--offline --no-daemon --no-configuration-cache --max-workers=2 --rerun-tasks`、JVM 21、Zulu 8 toolchain（禁止自动下载）；embedded Kotlin **2.3.20**。TestKit 保存输出也报告 Gradle **9.5.1**，其运行 JVM 为 `21.0.10+7-LTS`；不把默认 runner 行为描述成显式设置物理 Gradle home。[49 个生产 class](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-20261004-01/production-classes.json) 的 major 全为 52（Java 8）。这不是 root/embedded Kotlin 2.2.20 classpath 验证，更不是 Cloche 消费已上线。

## Docs-only 派生状态与待验候选

本次只读取/复用既有测试证据，不重跑测试。已读取所有既有 feature docs 及 [15 份完整前驱/重试 Result 清单](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-docs-20261004-01/consumed-results.json)，区分历史 blocked/failed 尝试与后续 signed completed 结果，不删改历史 feature docs。[docs writer 核验入口](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-docs-20261004-01/verify-docs.cjs) 与 [入场核验](C:/Users/WINDOWS/AppData/Local/Temp/i-plugin-docs-20261004-01/verification-before.json) 为本次局部正确性证据；本次 Result 给出实际签名 docs candidate 及最终核验文件，不构成 `I-final` 自验收。

候选须保持 classpath33/FIR27/Actual44 测试输入物理 SHA-256 和 code candidate 的 committed blobs、四个来源 checkout/515 文件、47 refs（仅 docs 推进 main）、18 个 generated XML、既有 `.git/info/exclude` 与继承的 `.git/next-index-264852.lock`。交付要求 tracked/index diff 为空，但这 18 个未跟踪 `.intellijPlatform/localPlatformArtifacts/*.xml` 必须继续存在；不声称 literal empty porcelain，也不清理 exclude、lock 或 refs。独立 `I-final` 的签名/祖先/版本/图释放/运行时限制接受仍待验证。
