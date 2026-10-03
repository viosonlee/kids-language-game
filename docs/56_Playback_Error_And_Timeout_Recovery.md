# 播放中错误与超时的恢复语义

> Author / Agent: Codex  
> Date: 2026-10-03  
> Status: Implemented; device fault injection not performed

本方案基于 docs/49、50、54 增量补充提示音异常处理，不涉及 ASR 超时、模型或评分参数。

## 审查发现与修复

1. MediaPlayer 在准备完成后调用 onError，旧代码直接完成播放票据，将半途失败当作正常结束。现在每次资源播放拥有独立完成信号，错误向调用者抛出 IOException，题目提示音沿用已安装本地 TTS 回退；回退不可用时返回播放中断。可选鼓励音仍保持已有的普通资源错误容错策略。
2. 外层 30 秒 withTimeoutOrNull 忽略返回值，旧代码在播放器不回调时正常返回。现在超时明确抛出 AudioPlaybackInterruptedException，沿用既有 ViewModel 中断处理：提示音失败不自动录音，反馈播放中断不自动下一题；用户仍可手动操作。
3. 资源完成回调同时校验播放器与票据身份。已释放播放器的迟到完成信号不能结束正在进行的 TTS 回退。诊断页通用提示调整为“播放中断或超时，请点击重播”。

## 验证

- AndroidPromptPlayerTest 定向测试通过。新增播放中错误回退、旧完成回调隔离、无本地语音恢复、播放完成回调缺失超时覆盖；加强原准备超时测试，要求明确中断。
- 完整 `test assembleDebug lintDebug --offline` 通过。Debug 208 + Release 180 = 388 项测试，零失败、错误及跳过。
- 未进行设备端故障注入；新 APK 已构建，本轮未覆盖安装或公开发布。正常预录播放、取消、音频焦点丢失与新旧请求隔离仍由现有回归用例覆盖。

## Actions 调查仍待日志

用户报告 GitHub Actions 经常失败。已检查两份工作流：本机 26 项 Python 测试、129 项音频质量检查、assembleDebugAndroidTest 均通过；这些是 Windows 本机证据，不能证明 Ubuntu runner 或远端流程通过。

公开分发仓库最新已查询运行 36706776961 成功，前两次为失败。源码私有仓库匿名 API 和未登录浏览器均返回 404；已请求具体失败日志或登录，尚不能定位当前源码 CI 故障。本轮不据猜测修改工作流或宣称修复 Actions。整体审查及 docs/53 的其余验收项继续进行。
