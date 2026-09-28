# 27. 首页背景音乐及生命周期

> Author / Agent: Codex
> Date: 2026-09-28
> Status: Implemented; listening/device focus verification pending
> Scope: FR-PARENT-002 背景音乐开关补齐

- 新增持久化 musicEnabled，默认关闭。家长页可开启首页背景音乐；进入练习或后台后不播放，避免示范和录音受到音乐干扰。
- 新增独立 BackgroundMusicPlayer，异步准备并循环播放；停止释放 MediaPlayer 和音频焦点，旧准备/焦点回调按实例身份隔离。外部音频焦点永久丢失停止，短暂丢失暂停，duck 降低音量。
- HomeViewModel 仅在首页活动期间观察音乐设置；兔子招呼开始先停音乐，当前招呼正常结束后再恢复。旧招呼取消回调不能影响新招呼；暂停首页使后续结束回调不能重启音乐。
- scripts/generate_home_music.py 使用标准库生成原创 16 秒正弦乐音循环，保存 mono PCM16 / 22050 Hz home.wav；各音符包络淡入淡出，首末采样为零。播放相对增益 0.18，设备媒体音量仍有效。
- HomeViewModelTest 验证默认关闭、启用、招呼替换、后台取消、返回恢复与关闭；音频文件检查时长、采样格式、非削波峰值与循环端点。
- test assembleDebug --offline 成功，Debug/Release 各 67 项单元测试通过。未进行实际试听/设备音频焦点验证，不据此宣称听感验收完成。
- 数据库设备验证、ASR 用户决策、总体需求核查和最终推送仍未完成。
