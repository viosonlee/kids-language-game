# 21. “没有声音”提示与识别率调查

> Author / Agent: Codex
> Date: 2026-09-28
> Status: Investigation only; no ASR parameter changes

## 已验证的软件证据

- VoskSpeechRecognizerEngine 使用 16 kHz 单声道识别，SpeechService 最长输入 10000 ms。
- 对本机实际依赖 vosk-android-0.3.47-runtime.jar 使用 javap 核查：SpeechService 使用 VOICE_RECOGNITION (6) 音源；识别线程达到 timeoutSamples 后只调用 onTimeout，不调用 getFinalResult。普通 stop 则调用 getFinalResult/onFinalResult。
- 应用 onTimeout 直接返回成功但 text 为空的结果，随后取消并释放识别器；onPartialResult 全部忽略。因此尚未形成 endpoint 的文字可能在 10 秒超时时丢失。这证明存在软件路径缺陷，尚不能证明用户每次失败都走了这条路径。
- RepeatViewModel 将成功但无文本的结果统一提示为“没有听到声音”，未区分物理静音与有声但未识别。
- 当前没有 PCM 音量、削波、有效语音时长、输入设备路由等诊断指标，不能凭此提示判断麦克风故障。
- 当前三语言均使用 Vosk small 通用模型；儿童发音效果需要成人/儿童实测，不能由通用模型指标推算。

## 建议实施顺序

1. 先修复超时 finalization，在结束线程后、释放 recognizer 前提取最终结果；记录结束原因。保留中间文字用于诊断，不直接当作正确答案。
2. 增加不保存原音的实时音量与有效语音时长指标，区分静音、音量低、有声未识别、设备/模型错误。检查输入路由及实际采样格式。
3. 同一平板成人/儿童、安静近距离/日常距离、同一词句分别测试；必要时与系统录音机回放对照，定位采集质量或模型问题。
4. 基于音量/VAD 调整等待开口和尾部静音窗口，避免幼儿停顿被截断；不能只盲目放大增益或延长总时限。
5. 对支持动态词表的模型试验本课候选词、别名和 unknown；加入相近错误词与无关语音测试，不能只给唯一正确答案强制命中。
6. 在相同测试语料上比较更大 Vosk 模型或其他离线多语识别模型，测量空结果率、正确/错误接受率、延迟、内存；较大模型不是自动更好的保证。联网识别属于另一方案，需用户决定隐私、网络和成本取舍。

参考：项目实际 0.3.47 二进制为行为证据；上游源码 https://github.com/alphacep/vosk-api/blob/master/android/lib/src/main/java/org/vosk/android/SpeechService.java 仅作为可读辅助，master 可能与安装版不同。
