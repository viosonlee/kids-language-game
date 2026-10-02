# 离线语音失败处理与资源生命周期

> Author / Agent: Codex
>
> Date: 2026-10-02
>
> Status: 本地单元测试、应用/仪器测试包构建、lint 通过；原生设备验收待完成
>
> Scope: FR-PRIV-002、NFR-REL-003/004，以及 docs/33 的识别器异常和系统回退缺口

## 要求冲突与处理决定

本方案基于 docs/11 和 docs/33 进行增量补充/替代，自开发版 1.0.2 起生效，保留所有历史规范。

PRD FR-PRIV-002 要求“默认不上传任何语音”，docs/11 §1 同样要求本地识别；但 docs/11 §5 的模型失败后自动转向系统 ASR/Mock 的回退图与这一要求不相容。Android 系统识别服务可能联网，系统服务可用不等于离线可用。未授权的联网录音也不能由一个偏好离线的 Intent 参数保证避免。

本次以“不上传默认录音”和真实练习结果为约束：生产识别链只使用内置 Vosk；模型缺失、加载失败或本地驱动不可用时返回失败，由 docs/34 的恢复界面提供重新加载、重听、换场景及可玩的选图。没有加入联网识别开关，也不以模拟识别结果替代儿童真实发音。

移除 `SystemSpeechRecognizerEngine`、未被使用的 `MockSpeechRecognizerEngine` 及旧模型路径探测死代码。`AppContainer` 仍构造 `VoskSpeechRecognizerEngine(context)`，但该构造不再包含系统 ASR 实例。Manifest 的服务查询改为已有示范播放所需的 `android.intent.action.TTS_SERVICE`，不再查询 RecognitionService；应用内更新所需 INTERNET 权限保留。TTS 接收示范文字，不是儿童录音输入。

## 已修复的错误路径

| 原有问题 | 当前行为 | 自动化证据 |
| --- | --- | --- |
| 模型加载只处理普通 Exception，JNI 加载的 LinkageError 可向 UI 传播 | 模型加载、识别器创建、启动/停止和 PCM 识别处理普通异常及 LinkageError；模型不可用返回失败 | 缺失模型、UnsatisfiedLinkError、NoClassDefFoundError、普通加载错误、重试 |
| 取消/释放都在同一个 try 中，前一步失败会跳过后续清理 | 先作废回调，再独立尝试停止录音、释放 AudioRecord、关闭识别器；清理错误记录并反馈 | stop/shutdown 异常、重复取消、释放顺序与次数 |
| 取消期间的最终回调可能与新一轮重叠 | 每轮请求带 generation，释放前递增；旧回调及重复结果不能完成新一轮 | 同步 final、重复 final、取消期间 final、新一轮开始后的旧 error |
| 模型在 IO 线程创建后、返回 Main 前取消，存在丢失句柄风险 | pendingModel 在 IO 内赋值，取消时在 NonCancellable 清理；取消继续向上传播 | 创建模型后取消、重试加载和关闭次数 |
| 停止失败时直接释放仍在使用的原生识别器有风险 | cancel 异常时再尝试 stop；两者都失败时保留句柄并拒绝替换模型，之后重试停止成功才释放 | 双重停止失败、模型替换拒绝、后续恢复 |

`OfflineSpeechResources` 将模型、识别器、SpeechService 的所有权分开，生产适配器仍直接调用 Vosk 0.3.47 原有 API。引擎负责时序、单次结果交付与错误归一化；测试注入受控资源，能够独立让某个资源操作失败，而不加载桌面环境不支持的 Android 原生库。

按本机实际依赖的 `javap` 输出复核：SpeechService.cancel/stop 正常路径会 interrupt/join 识别线程，shutdown 释放 AudioRecord；构造时录音器未初始化会先释放再抛 IOException。代码按该生命周期调用，而不是只把字段设为 null。

本次不宣称能够捕获所有原生崩溃。进程级 native signal、内存耗尽、平台调用永久阻塞、库内部线程中断边界都不由这些 JVM 测试证明解决；不捕获并隐藏所有 Throwable。普通协程取消不能被误判为识别失败或触发表扬。

## 未改动的识别行为

- 仍为原有三语言 small 模型、16 kHz 输入及 10000 ms 的 SpeechService 时限；没有引入新录音采集器。
- 没有修改音量、VAD、词表、阈值或评分规则。
- onPartialResult 仍不用于评分；有效 endpoint/final 文本才送入评测。
- docs/21 已确认的“超时丢失 final 文本”仍未优化，onTimeout 保持原来空结果语义。本轮测试仅防止无关的异常重构意外改变行为，不把该测试当成超时缺陷已解决的证明。
- 没有保存 PCM、上传儿童音频或增加自动采集。

## 验证结果

新增 `VoskSpeechRecognizerEngineTest` 15 项，覆盖上述路径、同语言模型复用、切换语言时先关闭旧模型、并发加载串行、错误语言拒绝录音、麦克风构造失败、start 返回 false/抛异常、PCM 成功/失败后的清理，以及超时/损坏 JSON 的既有空结果语义。

首次定向运行 14/15 通过；唯一失败是测试对跨协程异常使用了引用相等断言。协程堆栈恢复可复制异常，改为核对异常类型和消息，保留对实际失败及恢复行为的断言；重跑 15/15 通过。

最终执行：

```powershell
.\gradlew.bat test assembleDebug assembleDebugAndroidTest lintDebug --offline
```

使用 JDK 21、现有 SDK 和本机既有 Debug 签名。实际退出码为 0：

- Debug：122 tests，0 failures，0 errors，0 skipped。
- Release：116 tests，0 failures，0 errors，0 skipped。
- Debug 应用和仪器测试包构建成功；应用元数据仍为 `com.kidslanguage.companion.debug`、1.0.2 / code 3。
- lint：0 errors、13 warnings、2 information。本次离线报告未输出此前的 15 条 GradleDependency 升级建议；没有升级依赖或声称通过本轮代码消除了这些建议。其余警告分类与前次一致。
- 源码复核确认生产路径中无 Android SpeechRecognizer 创建/启动代码和 RecognitionService 查询。

这些是适配层和会话控制测试，不是真实麦克风或识别率评测；没有运行 connectedDebugAndroidTest。当前 SDK 仍提示缺少 platform-tools。没有安装到平板，也没有替换公开发布的 1.0.1 APK。

## 总审查剩余工作

docs/33 的系统 ASR 自动联网回退冲突已在当前生产代码中消除；初始化异常及录音清理增加了可复核的控制层证据，但不能把整个 NFR-REL-003/004 标为所有设备情形均已验收。仍需完整 Compose 流程、进度重置整合、声音响度/反馈时长、实际平板录音/生命周期/内存与儿童观察。用户要求先调查的识别率优化和手机布局方案保持原范围。
