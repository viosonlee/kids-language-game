# 20. Room 学习记录与家长统计

> Author / Agent: Codex
> Date: 2026-09-28
> Status: Implemented; database device verification pending
> Scope: FR-PARENT-003 / 技术设计第 12 节的增量实现

- 接入 Room 2.6.1 / KSP，数据库 practice_progress.db，导出 v1 schema。
- item_progress 使用语言与题目 ID 联合主键，存储 totalAttempts、normalPasses、assistedPasses、chooseCorrect、chooseWrong、lastPracticedAt。
- practice_attempt 存储唯一尝试 ID、题目本轮 ID、时间及计数，不保存录音或识别文字。事务同时更新事件及逐题汇总；重复尝试 ID 忽略。
- 跟读有效尝试计数，NoSpeech/Error 不计数。选图第三次错误辅助结束仍记为错误，不能伪造答对。
- 记录写入使用应用作用域，暂停/离开页面不取消已经提交的写入；写入和清空顺序串行，失败提示显示在家长页面。
- 家长页显示本地日期的今日练习/完成、各语言练习/完成、跟读正常/辅助通过与选图正误。反复尝试同一题计一道练习，重新开始计新一轮。午夜/时区变化每 30 秒重新确定日界限。
- 旧 DataStore completedCount 停止增加，单独保留为历史累计并计入总完成数；不虚构旧记录的语言/日期。清空需二次确认，清除 Room 与旧累计，保留设置。
- 新增 ProgressRecordTest（映射与计数语义）及 ProgressDatabaseTest（真实 Room 事务、去重、时间范围、语言汇总、清空）。执行结果见 WORKLOG，未执行的设备测试不视为通过。
