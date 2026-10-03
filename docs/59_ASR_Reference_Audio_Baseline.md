# 中英文示范音频的本机离线识别基线

> Author / Agent: Codex  
> Date: 2026-10-03  
> Status: Investigation only; not device/child acceptance

本记录增量补充 docs/21、53，不修改应用 ASR 模型、超时、词表或评分。

## 实验

新增 scripts/asr_reference_baseline.py，读取随包模型清单并验证 ZIP 大小及 SHA256，使用现有安全解压工具准备模型；逐条将随包 MP3 解码为 16 kHz、单声道 PCM，以 100 ms 分块输入本机 Vosk，保存 endpoint 文本及显式 FinalResult 文本。没有添加尾部静音，没有采集或保存麦克风录音。每种语言结束释放该模型再加载下一种。

本次 Windows Python Vosk 0.3.45；Android 应用使用 vosk-android 0.3.47，二者运行平台和封装版本不同。使用同一份中文 small 0.22、英文 small 0.15 模型。模型/音频哈希、环境版本、目标与别名、识别文字和本机解码耗时见 assets/59_ASR_Reference_Baseline.json。

## 结果

| 语言 | 样本数 | 空文本 | 与目标/别名规范化文字匹配 |
| --- | ---: | ---: | ---: |
| 中文 | 32 | 0 | 28 |
| 英文 | 32 | 0 | 26 |

这里的匹配仅去掉空白/标点并规范化字符后比较；不是应用中的多层评分结果，更不是儿童识别准确率。

不匹配样本：

| 题目 | 目标 | 原始识别 |
| --- | --- | --- |
| zh_greet_003 | 对不起 | 对不起 啊 |
| zh_play_003 | 借我玩一下 | 七 我 玩 一下 |
| zh_play_004 | 请等一等 | 清 等一等 |
| zh_daily_002 | 洗手手 | 洗手 少 |
| en_daily_001 | Water please | why oh please |
| en_daily_004 | Gather around | gather lan |
| en_daily_006 | Clean up | clean out |
| en_daily_007 | Put on coat | polanco |
| en_help_006 | Tummy hurts | tammy hurts |
| en_help_008 | Dad is here | dallas here |

即使绕开平板麦克风，也存在文字偏差，因此不能把全部识别问题归因于硬件。仍需区分示范音频本身的清晰度、通用模型能力与实际麦克风条件。应用别名、拼音或宽松评分可能接受部分偏差；不要把上表直接当作应用失败清单。

在不追加尾部静音的输入条件下，中文 12 条、英文 11 条只有 FinalResult 才输出文字。这是后续超时回收测试可使用的候选样本，不证明实际 10 秒麦克风超时必然丢掉这些句子，因为实际流还包含环境与尾部静音。

## 复现

在独立目录安装可选本机工具依赖（不改应用或 CI 依赖）：

```powershell
python -m pip install --target build/asr-baseline-tools vosk==0.3.45
$env:PYTHONPATH = (Resolve-Path build/asr-baseline-tools).Path
python scripts/asr_reference_baseline.py --report build/asr-reference-baseline.json
```

需要 FFmpeg 在 PATH；默认只测中英文，可用 --languages 选择日文。报告正常产生即退出 0；该工具是调查报告，不充当识别率放行门禁。识别不匹配不会被伪装成异常退出或自动改变应用阈值。

本次 64 条本机原生推理成功，Python 29 项工具回归测试通过；未验证 Android JNI 性能、实际输入路由、儿童发声、后台与锁屏行为。平板仍锁屏，实机窗口恢复后优先对照上表样本。整体审查未完成。
