# 提示音频异步准备与取消隔离

> Author / Agent: Codex  
> Date: 2026-10-03  
> Scope: AndroidPromptPlayer / PlaybackGate；增量补充 docs/15、33、41

## 审查发现

PRD 的 NFR-PERF-001/004 要求切换与反馈不阻塞 UI，FR-AUDIO-004 要求反馈不覆盖下一题。原 AndroidPromptPlayer 的整个播放操作在 Dispatchers.Main 上执行，且使用 MediaPlayer.prepare() 同步准备音频。音频打开、解码准备耗时较长时，主线程无法及时处理触摸、导航与生命周期取消。这是源码级阻塞路径证据，不是已经测得的设备卡顿耗时。

现有 ViewModel 的 stopSession 会取消播放协程；PlaybackGate 会隔离旧的完成回调。但改为异步准备时，还必须处理准备中取消、迟到准备回调以及准备失败，否则可能退出后重新播放或等待到超时。

## 实现

- 使用 prepareAsync()，通过可挂起的准备信号等待，不在 UI 线程同步等待解码准备。
- 同时等待准备信号和播放票据完成/取消，stop() 即使发生在没有准备回调时也可立即结束挂起任务。
- 开始声音之前核对当前 MediaPlayer 身份与未完成的 PlaybackGate 票据；旧 prepared/error 回调不能复活已取消音频或影响替代播放。
- 准备错误变为可捕获的 IO 异常：提示音保留现有 TTS 回退，鼓励音保留现有容错行为。
- 取消、自然结束、准备失败和现有 30 秒保护超时仍经同一 finally 释放播放器和焦点。30 秒是播放器已有保护值，本轮未改 ASR 超时或模型参数。

## 验证范围

新增 AndroidPromptPlayerTest，使用 API 29 Robolectric MediaPlayer shadow 控制实际 AndroidPromptPlayer 的准备与播放事件（不是仅验证 PlaybackGate）：

1. 准备未完成时主线程仍可执行其他任务，只有准备完成才开始声音。
2. stop() 在准备期间取消等待，迟到的 prepared 回调不能启动声音。
3. 快速替换音频会释放旧播放器，旧 prepared/error 事件不影响新播放器。
4. 页面所属协程取消也释放准备中的播放器。
5. 准备失败时可选鼓励音结束，不挂起或崩溃。
6. 无准备回调时，已有保护超时释放播放器。

第一次定向测试 5 项中 4 项通过；替换用例对旧播放器最终 shadow 状态的断言失败，因为测试注入 invokeErrorListener 本身把已释放的 shadow 从 END 改为 ERROR。改为在注入前断言 END，并在注入后断言旧音频未播放、新播放仍正常，保留资源释放及隔离验证。

## 边界与后续

本轮测试不代表真机延迟、音频焦点与儿童听感验收；手机按用户要求保持断开，没有安装或使用模拟器。音频焦点被系统拒绝、失去焦点时是否自动进入下一步等行为仍需要独立审查。整体需求审查继续进行，真实 ASR、插画语义和角色动画未因本轮测试通过而视为完成。

最终 `test assembleDebug lintDebug --offline` 通过；Debug 184 + Release 156 = 340 测试全部通过。已生成本地 debug APK，未安装到手机或发布公开更新。
