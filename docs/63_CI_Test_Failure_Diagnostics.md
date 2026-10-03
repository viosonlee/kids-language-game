# CI 单元测试失败日志补充

> Author / Agent: Codex  
> Date: 2026-10-03  
> Status: Diagnostics improved; remote failure unresolved

本记录增量补充 docs/60。用户提供的 Actions 日志表明 `:app:testDebugUnitTest` 失败，但只有 TaskExecutionException 和 Gradle 执行栈，没有失败测试名、断言或测试报告。提交标题不能证明启动页测试就是失败来源。

## 本轮变更

在 app/build.gradle.kts 为所有 Android JVM 单元测试配置 failed 事件及 FULL 异常日志，明确输出异常、原因链和堆栈。保留全部测试与断言，不忽略失败，也不更改应用布局。

现有 Actions 已在 always() 条件下上传 unit-test-reports，其中包含 app/build/test-results 下的 TEST-*.xml。需要本次失败运行的附件才能定位真实失败原因；下一次运行也可从测试任务日志读取完整失败异常。

## 验证与限制

- 修改前使用 `:app:testDebugUnitTest --tests '*AppNavigationJourneyTest.landscapePhoneSplash*' --offline --rerun-tasks` 强制重跑：两项测试通过，XML 记录 tests=2、failures=0、errors=0。
- 修改后再次运行相同筛选命令（不带 --rerun-tasks）：BUILD SUCCESSFUL，测试任务 UP-TO-DATE；证明 Gradle DSL 配置可加载，不代表重新执行了测试。
- 本地为 Windows / JDK 21；没有 Ubuntu 失败报告，不能认定已复现或修复远端失败。本轮没有执行完整测试集，也没有进行设备操作。
