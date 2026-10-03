# CI 导航测试同步调整

> Author / Agent: Codex  
> Date: 2026-10-03  
> Status: Local tests passed; remote confirmation pending

本记录增量补充 docs/63。用户提供 unit-test-reports.zip 后，已读取真实 JUnit XML，不再依据提交标题猜测失败来源。

## 报告证据

AppNavigationJourneyTest 报告时间为 2026-10-03T08:31:27，17 个用例中 2 个失败：

- repeatRetriesThenAssistsAndReplayStartsWithANewRecordingState：`IllegalArgumentException: performMeasureAndLayout called during measure layout`，调用链经 advance 第 139 行、测试第 248 行，发生在测试推进时钟后的布局等待。
- landscapePhoneKeepsHomeAndPracticeControlsInsideViewport：`The component is not displayed!`，第 482 行检查首页 Mascot Idle 图片时失败。

新增的两项 Splash 测试在该份远端报告中均通过。不能将这次失败归因于启动页尺寸改动。

## 调整

- advance 仍推进原有两种虚拟时钟，但将 Android Looper 推进放入 runOnIdle，先完成 Compose 当前布局工作；此前 runOnUiThread 只保证执行线程，不保证 Compose 已空闲。
- 横屏首页测试等待 Mascot Idle 图片实际显示后，再执行原有完整边界检查。MascotView 使用 Dispatchers.IO 解码图片，Compose 空闲不保证 IO 已结束或新图片已布局。
- 保留业务结果、重试次数、辅助通过、回放状态和完整可视边界断言；未修改应用代码、屏幕尺寸或超时阈值，没有忽略失败或添加失败重试。

## 验证及限制

修改前本地完整 AppNavigationJourneyTest 通过，未复现远端异常。修改后执行 `gradlew.bat test --offline`，BUILD SUCCESSFUL；Debug 测试重新执行，Release 测试 UP-TO-DATE。

这是根据失败堆栈和异步加载路径进行的测试同步调整。Windows 本地通过不能证明 Ubuntu 上的间歇失败已根治，需要新一次 Actions 报告确认。现有完整失败日志配置可继续提供原因链，不将远端结果提前登记为通过。
