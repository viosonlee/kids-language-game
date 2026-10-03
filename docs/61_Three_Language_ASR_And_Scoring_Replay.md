# 三语示范音频与实际评分器回放

> Author / Agent: Codex  
> Date: 2026-10-03  
> Status: Investigation; no model, threshold, alias or scoring changes

本记录增量补充 docs/59，把简单文字匹配与应用实际评分明确分开。

## 日文原生基线

同一脚本、Windows Vosk 0.3.45、随包日文 small 0.22 模型，执行：

```powershell
$env:PYTHONPATH = (Resolve-Path build/asr-baseline-tools).Path
python scripts/asr_reference_baseline.py --languages ja-JP --report build/asr-reference-ja.json
```

62 条全部返回非空文本，只有 16 条与目标/别名的规范化文字匹配；大量差异为假名与汉字表示，不能直接判作识别错误。49 条在未追加尾部静音的测试条件下没有非空 endpoint，只在 FinalResult 获取最终文本；与 docs/59 一样，不能据此推断真实麦克风十秒超时的比例。

## 实际应用评分回放

使用 JDK 21 Java 源码运行器，直接调用当前已编译的 ChildSpeechEvaluationService；没有在 Python 中重写评分逻辑。使用真实题目的 language、text、aliases；当前评分器仅访问这三个题目字段，其他展示字段不参与评估。将三语 126 条识别文字分别作为第 1、2、3 次尝试，得到 378 条评估结果。

| 语言 | 第一次 | 第二次 | 第三次 |
| --- | --- | --- | --- |
| 中文 32 条 | 32 Pass | 32 Pass | 32 Pass |
| 英文 32 条 | 31 Pass / 1 Retry | 31 Pass / 1 SoftPass | 31 Pass / 1 SoftPass |
| 日文 62 条 | 36 Pass / 26 Retry | 36 Pass / 19 SoftPass / 7 Retry | 36 Pass / 26 SoftPass |

这些是同一份识别文字的条件回放，不是让儿童真正说了三遍，不是现场准确率或误接受率。当前宽松评分会接受多条不完全一致的识别文字，因此 docs/59 的文字不匹配数量并不等于应用失败数量。

英文唯一首次 Retry 为 en_daily_007（Put on coat → polanco，score 0.36363637）。日文存在明确的书写归一化缺口，例如じゅんばん → 順番、ねむい → 眠い。当前归一化只处理片假名/平假名与基础符号，不进行汉字读音转换，部分同读音词因此需要重试。也有真正的识别偏差，不能将全部汉字输出或相似词自动视为正确。

## 证据与复现

assets/61_ASR_Replay 中保存日文逐条模型结果、Java 调用桥、Base64 字段 TSV 输入、评分 TSV 输出、结构化评分汇总。scoring-results.json 包含源码提交与评分器 SHA256。桥接类没有生产逻辑，只构造题目并调用已有评分器。

使用 `java -cp` 加入 app/build/tmp/kotlin-classes/debug、Kotlin stdlib 2.0.21 与 kotlinx-serialization-core-jvm 1.7.3 后，运行 ReferenceScoreProbe.java，传入 input.tsv 与一个 build/ 下输出路径即可重放。不要将复现输出覆盖本目录的历史证据。中英文识别原始输入仍引用 docs/assets/59_ASR_Reference_Baseline.json。

## 后续边界

用户此前要求识别先调查，本轮已询问是否允许仅修复同读音书写归一化；答复前未更换模型、放宽阈值、添加别名或修改评分。若获准，应同时覆盖同音正确词、近音错误词、错误词序与无关语音，不能为了本次示范集通过而只记忆结果。

平板仍锁屏；实际输入路由、儿童说话、Android 原生版本差异、后台停止、弹窗和长时间稳定性仍未验证。本轮新增调查证据，应用源码未变，不重复运行已有完整 Gradle 400 项测试。整体目标未完成。
