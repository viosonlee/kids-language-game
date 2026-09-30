# 动效偏好与图片解码修复

> Author / Agent: Codex
>
> Date: 2026-09-30
>
> Status: 本地测试、构建、lint 通过；实际设备视觉验收待完成
>
> Scope: 补齐 docs/09 M6-06、PRD FR-MAS-006 与 NFR-PERF-004 的相关实现，保留其他规范

## 已确认的问题

1. SettingsRepository 已经存在 `animationEnabled` 和 setter，但没有设置界面入口，也没有消费者；保存的动画偏好不生效。
2. MascotView 即使 `animate=false`，仍创建无限动画，且缩放目标仍是 1.01，无法真正静止。
3. 兔子 PNG 在 LaunchedEffect 的主线程直接打开、解码；切换表情时文件读取和解码可能阻塞 UI。

## 本次修复

- 家长设置新增“界面动效”开关，保留现有 `animation_enabled` DataStore key，默认开启。根界面按生命周期读取偏好并通过 LocalAnimationsEnabled 传递。
- 偏好尚未读取完成时暂不启动装饰动画，避免保存为关闭的用户先看到一段动画。
- 共用 loopingFloat 在禁用时直接返回静止值，不创建/保留 InfiniteTransition；兔子浮动和缩放、喇叭与麦克风脉冲均使用该规则。
- 首页兔子点击缩放、反馈层淡入淡出、导航淡入淡出受同一偏好控制。关闭时仍保留录音状态、表情、图标、视觉反馈与点击行为；加载进度指示和系统/Material 交互反馈不属于本次装饰动效开关范围。
- `MascotView(animate=false)` 与全局关闭组合后始终静止；图片读取与解码移到 Dispatchers.IO，页面离开时正常传播协程取消。
- lint 对原有 produceState 报“没有给 value 赋值”，尽管 lambda 已赋值；改为按语言/场景 remember 的状态配合 LaunchedEffect，维持异步加载及取消语义，未添加 lint suppression/baseline。
- CI 增加 lintDebug 并保存 lint 报告。

## 验证与边界

- 新增 MotionTest：在真实 Compose Recomposer 与受控帧时钟上切换偏好，证明禁用时使用静止值、启用后实际变化、运行中关闭后立即静止且不再等待帧。此为运行时动画逻辑测试，不是屏幕截图测试。
- SettingsRepositoryTest 新增偏好重建、进度重置后仍保留动画开关的测试。
- 最终 `gradlew.bat test assembleDebug lintDebug --offline` 成功，Debug/Release 各 92 项测试，0 失败、0 错误、0 跳过。
- lint 为 0 errors、26 warnings、2 information。警告分布：依赖更新建议 13；圆形图标 5；图标 XML/PNG 同名、单色图标、旧版本资源目录、未使用图标各 1；Modifier 参数顺序 2；横屏限制 1；存储可分配空间建议 1。另有两项 primitive state 优化提示。
- 初次离线 lint 因 lint-gradle 工具未缓存而失败；联网取得工具后才能完成检查。没有把该失败解释成代码通过。
- 不在本轮批量升级依赖或改变用户仍在评估的手机方向策略；图标/资源与布局建议继续随相应审查处理。
- 真实平板动效开关、PNG 加载平滑性和大字体布局仍需设备验证；完整审查状态见 docs/33。

公开 APK 仍为 docs/29 的 1.0.1，本轮仅更新正在开发的 1.0.2 源码与本地构建。
