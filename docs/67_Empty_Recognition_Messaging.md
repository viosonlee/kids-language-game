# 空识别结果的提示语义

> Author / Agent: Codex  
> Date: 2026-10-03  
> Status: Implemented; targeted tests passed

本记录增量补充 docs/53、66。空识别文本不能证明设备没有采集到声音，也可能是语音未被识别。儿童恢复弹窗已使用“还没听清”，本次补齐另外两处不一致的提示。

- 评分服务的空文本提示由“没有听到声音哦”改为“还没听清，再说一次吧”。
- 诊断页成功回调但文本为空时，显示“未识别到文字，请靠近麦克风再试一次”，不再统一显示“识别完成”。诊断评分枚举说明改为“未识别到有效文字”。
- 内部 NoSpeech 状态、错误处理、重试计数与辅助通过规则不变。本次仅修正提示语义，没有增加 VAD、音量测量或声学分类，不能宣称已区分物理静音与有声但未识别，也不关闭真机验收。

验证：执行 testDebugUnitTest，筛选 SpeechEvaluationServiceTest 与 landscapePhoneDiagnosticRecordingAndResultsRemainReachable；15 项评分测试和 1 项诊断布局测试通过。未新增仅校验文案的测试，未重复全量构建。
