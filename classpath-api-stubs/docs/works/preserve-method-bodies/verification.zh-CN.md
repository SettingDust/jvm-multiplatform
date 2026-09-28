# preserveMethodBodies 实现验证报告

## 结果与范围

已在独立工作树 `E:/Projects/cloche/jvm-multiplatform-preserve-method-bodies`、分支 `feat/preserve-method-bodies` 完成 T1–T4 的实现与验证。基线为 `9bd0e543ab6d2de4698846cda47b21356ae04acb`，未提交、推送或合并。

代码、构建配置及测试变更均在 `classpath-api-stubs` 内。main 工作树 `E:/Projects/cloche/jvm-multiplatform` 保持 `main`；检查时两个工作树的 HEAD 均为上述基线。已获用户实现授权；本报告不声称对 Plan 存在另一次单独确认。Spec、Work、Context、Plan、Tasks 和本报告保留在 main 工作树；实现工作树仅保留代码、测试、隔离验证 settings 和构建输出。未修改 main 的 Spec；未提交、推送或合并。

## 实现

- `GenerateStubApi.kt`：新增默认 `false` 的 `@Input preserveMethodBodies`，传递给生成器。嵌套输入文件使用 `@InputFile` 与 `@PathSensitive(NAME_ONLY)`，追踪完整文件内容及复制文件名，覆盖默认关闭时的单输入透传和排除产物复制。
- `MethodBodies.kt`：用 `visitCode` 记录 Code 是否存在；比较完整指令序列、类型化操作数、常量及递归 ConstantDynamic、invokedynamic 数据、分支与 switch 目标位置、有序异常表。忽略调试信息、冗余标签和 frame 表示；Code 类型注解、未知属性/指令/常量及无法解析的控制流标签不能建立相等。
- `StubGenerator.kt`：在既有声明交集完成后，以名称和描述符查询每个原始输入的方法。仅在开启、多输入且所有原始 Code 相等时，整体转移同一来源的指令、标签、异常表、frames、调试表和 maxima；方法声明注解、声明异常等仍来自原有交集。单输入及关闭选项时沿用原行为。枚举 jar 项目后关闭输入流，避免重复运行时遗留句柄。
- `build.gradle.kts`：测试使用运行 Gradle 的 JDK，生产代码仍以 Java 8 toolchain 编译；向 TestKit 提供真实主源码运行类路径。
- 新增两个测试类，以及 `verification/settings.gradle`，用于绕过无关根构建配置故障而编译、测试同一模块源码和构建文件。

## T1–T4 证据

| 任务 | 验证内容 | 结果 |
| --- | --- | --- |
| T1 | 默认关闭；开关改变输入状态；单输入开启/关闭均在仅方法体变化后重新执行；排除复制文件变化后的输出字节及缓存恢复 | 通过 |
| T2 | 四个 ASM 比较测试，覆盖双向对称、Code 存在性、空指令 Code 与缺失区分、抽象/native、指令和操作数、嵌套动态常量、不可达指令、跳转/switch、异常范围/类型/顺序/handler、debug/frame/冗余标签、Code 注解及未知元数据/常量 | 通过 |
| T3 | ASM 读取实际生成 jar；两/三输入及倒序的保留/省略；单输入两种开关；默认多输入无 Code；不同、缺失、不可比较 Code 仍保留声明；构造器、类初始化器、抽象/native；声明成员、注解、异常交集；来源 Code 的调试表、frame 和 maxima | 通过 |
| T4 | 一个 TestKit 集成测试执行 11 次真实 Gradle 构建；断言未变输入 `UP_TO_DATE`，仅方法体变化后 `SUCCESS` 及保留→省略变化，删除输出后 `FROM_CACHE`，恢复原输入后正确缓存命中；检查单输入和排除产物复制 | 通过 |

测试总计 **6 项，0 失败、0 错误、0 跳过**：`MethodBodiesTest` 4 项，`PreserveMethodBodiesTest` 1 项，原有 `ParsingTest` 1 项。最终 `git diff --check` 通过。

## 实际成功命令

从实现工作树运行，使用 Git Bash：

```sh
unset KOTLIN_HOME
export JAVA_HOME='D:/Applications/Scoop/apps/intellij-jbr21-sdk-jcef/current'
/d/Applications/Scoop/apps/gradle/current/.gradle/wrapper/dists/gradle-9.4.1-bin/arn2x92ynaizyzdaamcbpbhtj/gradle-9.4.1/bin/gradle \
  -p classpath-api-stubs/verification :classpath-api-stubs:test \
  -Porg.gradle.java.installations.paths=D:/Applications/Scoop/apps/zulu8-jdk/current \
  --console=plain
```

结果：`BUILD SUCCESSFUL in 48s`，7 个任务执行。其他环境可用独立安装的 Gradle 9.4.1 替代上述可执行文件路径，并配置本机 Java 8 toolchain 和运行 Gradle 的 JDK。

本地证据（以下链接指向实现工作树中的生成输出，不纳入源码；相对链接从 main 工作文档目录出发）：

- [成功构建日志](../../../../../jvm-multiplatform-preserve-method-bodies/classpath-api-stubs/build/verification/verification-gradle94.log)。
- [MethodBodiesTest XML](../../../../../jvm-multiplatform-preserve-method-bodies/classpath-api-stubs/build/test-results/test/TEST-net.msrandom.stubs.MethodBodiesTest.xml)。
- [PreserveMethodBodiesTest XML](../../../../../jvm-multiplatform-preserve-method-bodies/classpath-api-stubs/build/test-results/test/TEST-net.msrandom.stubs.PreserveMethodBodiesTest.xml)。
- [ParsingTest XML](../../../../../jvm-multiplatform-preserve-method-bodies/classpath-api-stubs/build/test-results/test/TEST-net.msrandom.stubs.signature.ParsingTest.xml)。
- [HTML 测试报告](../../../../../jvm-multiplatform-preserve-method-bodies/classpath-api-stubs/build/reports/tests/test/index.html)。

上述输出位于实现工作树，`clean` 会删除这些生成证据；本报告保留核查时的结论，不将构建输出复制入 main。

## 环境限制与保守边界

- 原仓库 wrapper 为 Gradle 9.2.0。实际执行 `./gradlew :classpath-api-stubs:test --console=plain` 在根 `settings.gradle.kts:8` 报 `Unresolved reference 'file'`；更换 JDK 并清除 `KOTLIN_HOME` 后运行 `./gradlew help` 仍失败。使用隔离 settings 时，该 wrapper 还在模块构建脚本报 `Unresolved reference 'plugins'` 等错误。未修改根构建文件，未声称原 wrapper 或全仓库构建通过。
- Scoop Gradle 9.5.1 可编译模块，但其安装目录内同时存放 Gradle 用户缓存，TestKit 的 `gradleHome` 输入哈希遇到 `fileContent.lock`，测试未启动。改用现有独立 Gradle 9.4.1 distribution 后完整测试通过。相关失败日志保留在实现工作树 `classpath-api-stubs/build/verification/`。
- 原插件声明引用的 `ClasspathApiStubsPlugin` 实现类不在 jar 中，构建仍有该基线警告。TestKit 按计划直接注册实际 `GenerateStubApi` 类型，验证的是任务与产物，不是插件应用流程。
- Code 类型注解和未知属性采取保守省略。ASM 无法可靠判定未知属性归属时，方法上的未知属性也会阻止多输入保留。
- 验证不以 JVM 加载/运行、stub 编译、frame verifier 或注入成功为门槛。保留对缺失 helper 的相同调用只是字节码形状一致，不保证 helper 存在或行为一致。没有 Cloche/Codev 接入或 worker/coroutine 改动。
