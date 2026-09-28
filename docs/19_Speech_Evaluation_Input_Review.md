# 19. 发音判定输入边界审查补充

> Author / Agent: Codex
> Date: 2026-09-28
> Status: Implemented; broader review remains in progress
> Scope: 对 docs/12 与 docs/15 的增量修复说明

## 基线与修复

沿用 docs/15 的最新判定阈值：相似度至少 0.55 正常通过，第二次至少 0.25 辅助通过，第三次有效语音辅助通过。该规则优先于 docs/12 的早期阈值。

- 原先仅在标准化之前检查空输入，标点或符号可能进入匹配/第三次辅助通过。现在对标准化后的有效文字检查，无有效文字返回 NoSpeech。
- 空题目返回 Error；空别名忽略，避免 contains(空字符串) 导致全部输入通过。
- 别名的反向子串匹配应用 docs/15 的最少三字符、至少 70% 覆盖限制，避免单字符误通过。完整配置别名仍可通过。
- 英语完整包含匹配尊重词边界，hi there 可命中 hi，但 this 不可因内部含 hi 而通过。编辑距离宽松判定仍保留。
- NFKC 统一全半角和组合假名，大小写使用 Locale.ROOT；删除符号后保留英语词间边界。
- RepeatViewModel 对评测层 NoSpeech/Error 不累计尝试、不显示失败反馈、不播放鼓励或推进题目。

## 验证

新增六个判定测试覆盖空别名、短片段、词边界、Unicode、无效目标和最新辅助阈值；扩展会话测试覆盖只有标点/emoji 的识别结果。单元测试结果见本次 WORKLOG。

## 未完成范围

技术设计第 12 节明确要求 Room ItemProgressEntity / ProgressRepository；当前仍只有 DataStore completedCount。本次确认了缺口，后续需补齐逐题尝试、正常/辅助通过、选图正误、日期/语言汇总与清空功能。旧累计数不能推断出历史日期或语言，不应伪造迁移数据。家长设置、音效生命周期、内容容错和整体验收亦仍需完成；本补充不代表完整审查通过。
