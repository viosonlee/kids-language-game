# PRD 证据审查更新与未完成工作

> Author / Agent: Codex  
> Date: 2026-10-03  
> Status: 阶段审查，整体目标未完成  
> Scope: docs/01 全部 83 个 FR/NFR；增量更新 docs/33，不修改历史文件

本轮基线为源码提交 `536e53f`。复核现有源码、测试 XML、音频报告对应字节与最新 APK；证据快照见 [53_Review_Evidence.json](assets/53_Review_Evidence.json)，包含生产源码哈希、测试名称和 APK 哈希。它记录证据来源，不是自动验收证书。

Gradle 最近完整运行：Debug 200 + Release 172 = 372 项，无失败/错误/跳过；本轮重新运行 Python 26 项通过。仅新增审查文档，没有再次修改或重跑 Android 代码。129 条音频与既有测量报告哈希一致，且与当前 APK 字节相同。

## 逐项状态

| ID | 当前证据 | 仍需确认 / 范围边界 |
| --- | --- | --- |
| FR-APP-001 | MainActivity / Manifest；AppNavigationJourneyTest 可进入首页；此前 PJE110 横屏启动 | Android 10 目标平板冷启动及系统安全区尚未验收 |
| FR-APP-002 | 三语随包题库、音频、Vosk；ContentRepositoryTest / BundledModelsTest；三语导航测试 | 真实三语飞行模式整轮仍缺设备证据；异常预录音频的 TTS 回退需另查离线保证 |
| FR-APP-003 | MainActivity / Manifest；AppNavigationJourneyTest 可进入首页；此前 PJE110 横屏启动 | Android 10 目标平板冷启动及系统安全区尚未验收 |
| FR-APP-004 | SettingsRepositoryTest / SettingsReadRecoveryTest；失败不覆盖已存偏好 | 真实进程终止及升级后逐项偏好复核未完成 |
| FR-LANG-001 | 三语随包题库、音频、Vosk；ContentRepositoryTest / BundledModelsTest；三语导航测试 | 真实三语飞行模式整轮仍缺设备证据；异常预录音频的 TTS 回退需另查离线保证 |
| FR-LANG-002 | HomeScreen / CategoryScreen；旗帜与分类图标；docs/48 手机截图 | 儿童能否理解入口语义未验收 |
| FR-LANG-003 | VoskSpeechRecognizerEngine 的 modelLock、generation、释放顺序；VoskSpeechRecognizerEngineTest | 原生三模型切换、句柄与 RSS 尚无本轮实测 |
| FR-LANG-004 | VoskSpeechRecognizerEngine 的 modelLock、generation、释放顺序；VoskSpeechRecognizerEngineTest | 原生三模型切换、句柄与 RSS 尚无本轮实测 |
| FR-CONTENT-001 | ContentRepositoryTest / LessonPlanTest；12 个课程；docs/51 仓库与规划一致性修复 | 自动化覆盖已列行为；新增题目的语义仍须内容审查 |
| FR-CONTENT-002 | HomeScreen / CategoryScreen；旗帜与分类图标；docs/48 手机截图 | 儿童能否理解入口语义未验收 |
| FR-CONTENT-003 | ContentRepositoryTest / LessonPlanTest；12 个课程；docs/51 仓库与规划一致性修复 | 自动化覆盖已列行为；新增题目的语义仍须内容审查 |
| FR-CONTENT-004 | ContentRepositoryTest / LessonPlanTest；12 个课程；docs/51 仓库与规划一致性修复 | 自动化覆盖已列行为；新增题目的语义仍须内容审查 |
| FR-SPEAK-001 | ContentIntegrityTest / ContentRepositoryTest；全局 ID、资源引用、逐题隔离；docs/34、51 | 旧表所列字段与全局 ID 验证缺口已补；不能据此证明任意坏文件均可恢复 |
| FR-SPEAK-002 | RepeatViewModelTest；AppNavigationJourneyTest；docs/50 中断后不自动录音且可主动重播 | 自动/手动及权限时序有自动化；实际喇叭、麦克风路由仍待设备验证 |
| FR-SPEAK-003 | RepeatViewModelTest；AppNavigationJourneyTest；docs/50 中断后不自动录音且可主动重播 | 自动/手动及权限时序有自动化；实际喇叭、麦克风路由仍待设备验证 |
| FR-SPEAK-004 | Vosk 本地采集适配器；原生边界以替身测试；docs/21 超时调查 | 真实识别率与尾部延迟未测；超时最终文本回收缺口仍未修改，待用户放开调查限制 |
| FR-SPEAK-005 | SpeechEvaluationServiceTest；别名、文本归一化与宽松匹配 | 只证明文本规则；不证明声学/音素评测；后续规则按 docs/12、15 生效 |
| FR-SPEAK-006 | 两种 ViewModel / 记录测试、导航重试与辅助通过；docs/41、42、50 | 真实儿童鼓励感受、音画联合效果仍待观察 |
| FR-SPEAK-007 | 两种 ViewModel / 记录测试、导航重试与辅助通过；docs/41、42、50 | 真实儿童鼓励感受、音画联合效果仍待观察 |
| FR-SPEAK-008 | 有效文本三次辅助通过有测试；空文本进入图形恢复；录音沿用 Vosk endpoint | 有声但未识别与物理静音未区分，不能称已满足全部防卡题；儿童停顿和超时回收待处理 |
| FR-SPEAK-009 | 两种 ViewModel / 记录测试、导航重试与辅助通过；docs/41、42、50 | 真实儿童鼓励感受、音画联合效果仍待观察 |
| FR-SPEAK-010 | 有效文本三次辅助通过有测试；空文本进入图形恢复；录音沿用 Vosk endpoint | 有声但未识别与物理静音未区分，不能称已满足全部防卡题；儿童停顿和超时回收待处理 |
| FR-SPEAK-011 | 有效文本三次辅助通过有测试；空文本进入图形恢复；录音沿用 Vosk endpoint | 有声但未识别与物理静音未区分，不能称已满足全部防卡题；儿童停顿和超时回收待处理 |
| FR-SPEAK-012 | RepeatViewModelTest；AppNavigationJourneyTest；docs/50 中断后不自动录音且可主动重播 | 自动/手动及权限时序有自动化；实际喇叭、麦克风路由仍待设备验证 |
| FR-LISTEN-001 | LessonPlanTest / ChooseViewModelTest / AppNavigationJourneyTest；2～4 有效图片、重试提示、第三次辅助、手动继续 | 手机尺寸有渲染；内容图片能否解释目标句含义仍未通过观察 |
| FR-LISTEN-002 | ChooseViewModel.setupCurrentQuestion 使用 shuffled()，无固定正确索引 | 源码证明可随机化；不把单次排列当随机分布验收 |
| FR-LISTEN-003 | LessonPlanTest / ChooseViewModelTest / AppNavigationJourneyTest；2～4 有效图片、重试提示、第三次辅助、手动继续 | 手机尺寸有渲染；内容图片能否解释目标句含义仍未通过观察 |
| FR-LISTEN-004 | LessonPlanTest / ChooseViewModelTest / AppNavigationJourneyTest；2～4 有效图片、重试提示、第三次辅助、手动继续 | 手机尺寸有渲染；内容图片能否解释目标句含义仍未通过观察 |
| FR-LISTEN-005 | LessonPlanTest / ChooseViewModelTest / AppNavigationJourneyTest；2～4 有效图片、重试提示、第三次辅助、手动继续 | 手机尺寸有渲染；内容图片能否解释目标句含义仍未通过观察 |
| FR-FEEDBACK-001 | SuccessStars / MascotView；700ms 有限弹跳与星光；MascotMotionTest / MotionTest | 基础庆祝已实现；用户另要的眨眼、挥手、口型动作尚未实现 |
| FR-FEEDBACK-002 | SuccessFeedbackTest；129 音频哈希与当前 APK 一致；docs/41 名义总时长约 1.23～1.46 秒 | 真实硬件准备和回调延迟不受名义时长证明；docs/50 中断改为主动继续 |
| FR-FEEDBACK-003 | SettingsPromptPlayerTest / SettingsRepositoryTest / MotionTest；静音与关闭动效 | 参数与状态传播已测，设备听感与动态观感待验 |
| FR-FEEDBACK-004 | MotionTest / MascotMotionTest；有限庆祝、关闭循环动画 | 完整真机动态视觉验收仍缺 |
| FR-FEEDBACK-005 | 模型、反馈与统计实现中无扣生命、扣星或签到惩罚 | 源码审查；不等同于儿童心理体验验证 |
| FR-TEXT-001 | RepeatRecoveryDialog 图形入口与自定义提示；完整导航与拒绝权限恢复测试 | 零识字主流程仍需儿童观察；docs/47 语义缺口影响声音到含义关联 |
| FR-TEXT-002 | 设置三种文字模式；CJK 长句、960×600 大字体、794×361 紧凑布局测试 | 不能推广为所有字号、所有窗口与字体均已验收 |
| FR-TEXT-003 | 设置三种文字模式；CJK 长句、960×600 大字体、794×361 紧凑布局测试 | 不能推广为所有字号、所有窗口与字体均已验收 |
| FR-PARENT-001 | ParentControlsTest 与 AppNavigationJourneyTest 验证乘法门禁、错误答案与进入设置 | 乘法门禁已有流程证据；实际防误入效果待观察 |
| FR-PARENT-002 | SettingsRepositoryTest / ParentControlsTest / ParentViewModelTest；重置确认、事务回滚与失败反馈 | 旧表重置整合缺口已补；完整家长页多设备可读性待验 |
| FR-PARENT-003 | Room 查询、ProgressSummary、StorageRecoveryControlsTest；docs/52 午夜/时区/DST 验证 | 持续打开时日期最多约 30 秒刷新延迟；真实设备长期统计验收未完成 |
| FR-PARENT-004 | AppVersionInfo 读取包、content/models manifest；损坏清单显示未知 | 版本展示仍主要为源码证据，尚缺专门坏清单组件测试 |
| FR-PROGRESS-001 | ProgressRecordTest / RoomProgressDatabaseTest / LegacyProgressMigrationTest；逐项持久化与原子清空 | 自动化覆盖计数与事务；真实断电等外部条件未测 |
| FR-PROGRESS-002 | Room 磁盘关闭重开与迁移测试；此前已完成 debug 覆盖安装 | 升级后 preferences/历史累计逐项核对未完成，不宣称升级数据验收通过 |
| FR-PROGRESS-003 | ProgressRecordTest / RoomProgressDatabaseTest / LegacyProgressMigrationTest；逐项持久化与原子清空 | 自动化覆盖计数与事务；真实断电等外部条件未测 |
| FR-PROGRESS-004 | 数据库仅计数/时间/标识；无录音保存、账户、广告或 Debug Audio Capture 路径 | 源码审查；未来加入录音采集仍需明确授权和删除入口 |
| FR-DATA-001 | ContentRepositoryTest / LessonPlanTest；12 个课程；docs/51 仓库与规划一致性修复 | 自动化覆盖已列行为；新增题目的语义仍须内容审查 |
| FR-DATA-002 | ContentIntegrityTest / ContentRepositoryTest；全局 ID、资源引用、逐题隔离；docs/34、51 | 旧表所列字段与全局 ID 验证缺口已补；不能据此证明任意坏文件均可恢复 |
| FR-DATA-003 | 当前 contentVersion=1.0.4；126 题；99 个练习 PNG；APK 包含本地音频与模型 | 资产字节可核对；不代表插画语义验收 |
| FR-DATA-004 | ContentIntegrityTest / ContentRepositoryTest；全局 ID、资源引用、逐题隔离；docs/34、51 | 旧表所列字段与全局 ID 验证缺口已补；不能据此证明任意坏文件均可恢复 |
| FR-DATA-005 | 当前 contentVersion=1.0.4；126 题；99 个练习 PNG；APK 包含本地音频与模型 | 资产字节可核对；不代表插画语义验收 |
| FR-AUDIO-001 | 按 docs/10 演进为生成式预录语音，随包离线资产 | 语言自然度、儿童可模仿性及设备音量需听音验收；不是实际真人录音 |
| FR-AUDIO-002 | docs/40、41 响度/峰值测量；本轮重新核对129条文件及APK字节与报告一致 | 数值归一化证据已补；本轮未重新测量相同字节，也不证明听感 |
| FR-AUDIO-003 | 按 docs/10 演进为生成式预录语音，随包离线资产 | 语言自然度、儿童可模仿性及设备音量需听音验收；不是实际真人录音 |
| FR-AUDIO-004 | SuccessFeedbackTest / AndroidPromptPlayerTest；docs/49、50 异步准备、焦点拒绝/中断、退出取消 | 实际 OEM 来电、外放/耳机/SoundPool 混音仍需设备验证 |
| FR-AUDIO-005 | SuccessFeedbackTest / AndroidPromptPlayerTest；docs/49、50 异步准备、焦点拒绝/中断、退出取消 | 实际 OEM 来电、外放/耳机/SoundPool 混音仍需设备验证 |
| FR-PRIV-001 | 数据库仅计数/时间/标识；无录音保存、账户、广告或 Debug Audio Capture 路径 | 源码审查；未来加入录音采集仍需明确授权和删除入口 |
| FR-PRIV-002 | VoskSpeechRecognizerEngine 无系统识别回退；docs/35；依赖/Manifest 及源码核对 | 旧默认系统识别上传冲突已消除；TTS 回退与更新网络边界需独立审查，不能笼统称无网络路径 |
| FR-PRIV-003 | 数据库仅计数/时间/标识；无录音保存、账户、广告或 Debug Audio Capture 路径 | 源码审查；未来加入录音采集仍需明确授权和删除入口 |
| FR-PRIV-004 | 数据库仅计数/时间/标识；无录音保存、账户、广告或 Debug Audio Capture 路径 | 源码审查；未来加入录音采集仍需明确授权和删除入口 |
| FR-PRIV-005 | 数据库仅计数/时间/标识；无录音保存、账户、广告或 Debug Audio Capture 路径 | 源码审查；未来加入录音采集仍需明确授权和删除入口 |
| NFR-COMP-001 | minSdk 29；Room、MediaPlayer shadow、Compose 测试 API 29 | 真实 Android 10 设备与原生 Vosk 的整体验收缺失 |
| NFR-COMP-002 | dp/sp 布局，docs/44～48 长文字/大字体/短横屏修复与渲染 | 原目标 2560×1600 平板安全区与触控验收未完成；手机不能代替平板 |
| NFR-COMP-003 | dp/sp 布局，docs/44～48 长文字/大字体/短横屏修复与渲染 | 原目标 2560×1600 平板安全区与触控验收未完成；手机不能代替平板 |
| NFR-PERF-001 | 题库/模型/图片 IO 后台；docs/49 移除 Main 同步 prepare；异步及取消测试 | 帧率、主线程 trace、长时卡顿指标仍缺设备测量 |
| NFR-PERF-002 | VoskSpeechRecognizerEngine 的 modelLock、generation、释放顺序；VoskSpeechRecognizerEngineTest | 原生三模型切换、句柄与 RSS 尚无本轮实测 |
| NFR-PERF-003 | Vosk 本地采集适配器；原生边界以替身测试；docs/21 超时调查 | 真实识别率与尾部延迟未测；超时最终文本回收缺口仍未修改，待用户放开调查限制 |
| NFR-PERF-004 | 题库/模型/图片 IO 后台；docs/49 移除 Main 同步 prepare；异步及取消测试 | 帧率、主线程 trace、长时卡顿指标仍缺设备测量 |
| NFR-PERF-005 | RepeatScreen / 诊断加载提示；模型延迟用例禁用录音 | 真实首次解压进度/耗时未测，当前显示等待状态并非精确百分比 |
| NFR-REL-001 | ContentIntegrityTest / ContentRepositoryTest；全局 ID、资源引用、逐题隔离；docs/34、51 | 旧表所列字段与全局 ID 验证缺口已补；不能据此证明任意坏文件均可恢复 |
| NFR-REL-002 | RepeatRecoveryDialogTest；图形重试、选图、换场景和家长帮助；无儿童直接系统设置入口 | 旧实现缺口已补；儿童理解恢复按钮仍需观察 |
| NFR-REL-003 | VoskSpeechRecognizerEngineTest；JNI/资源边界、取消与旧回调隔离；两玩法离开后不再推进 | 替身测试不证明原生 AudioRecord 释放、无泄漏或零崩溃 |
| NFR-REL-004 | VoskSpeechRecognizerEngineTest；JNI/资源边界、取消与旧回调隔离；两玩法离开后不再推进 | 替身测试不证明原生 AudioRecord 释放、无泄漏或零崩溃 |
| NFR-UX-001 | RepeatRecoveryDialog 图形入口与自定义提示；完整导航与拒绝权限恢复测试 | 零识字主流程仍需儿童观察；docs/47 语义缺口影响声音到含义关联 |
| NFR-UX-002 | 儿童控件保留较大点击区；手机紧凑头部48dp、录音80dp、下一题56dp；恢复按钮88dp | PRD建议≥88dp非全部满足；docs/48作手机空间折中，触控舒适度待真机观察 |
| NFR-UX-003 | 正确高亮之外有兔子姿态、图标和星光；重试温和提示 | 非颜色提示能否被识别仍需实际观察 |
| NFR-UX-004 | RepeatRecoveryDialog 图形入口与自定义提示；完整导航与拒绝权限恢复测试 | 零识字主流程仍需儿童观察；docs/47 语义缺口影响声音到含义关联 |
| NFR-UX-005 | RepeatRecoveryDialogTest；图形重试、选图、换场景和家长帮助；无儿童直接系统设置入口 | 旧实现缺口已补；儿童理解恢复按钮仍需观察 |
| FR-MAS-001 | 六姿态 PNG 与状态映射；导航、反馈和手机渲染证据；docs/42 | 基本六状态已存在，风格一致性与儿童反应未验收；眨眼/挥手/口型仍缺角色动作资源 |
| FR-MAS-002 | 六姿态 PNG 与状态映射；导航、反馈和手机渲染证据；docs/42 | 基本六状态已存在，风格一致性与儿童反应未验收；眨眼/挥手/口型仍缺角色动作资源 |
| FR-MAS-003 | 六姿态 PNG 与状态映射；导航、反馈和手机渲染证据；docs/42 | 基本六状态已存在，风格一致性与儿童反应未验收；眨眼/挥手/口型仍缺角色动作资源 |
| FR-MAS-004 | SuccessStars / MascotView；700ms 有限弹跳与星光；MascotMotionTest / MotionTest | 基础庆祝已实现；用户另要的眨眼、挥手、口型动作尚未实现 |
| FR-MAS-005 | 六姿态 PNG 与状态映射；导航、反馈和手机渲染证据；docs/42 | 基本六状态已存在，风格一致性与儿童反应未验收；眨眼/挥手/口型仍缺角色动作资源 |
| FR-MAS-006 | SettingsPromptPlayerTest / SettingsRepositoryTest / MotionTest；静音与关闭动效 | 参数与状态传播已测，设备听感与动态观感待验 |

## 主要未完成工作与下一步

1. **识别与防卡题**：最终文本超时回收、有声但空识别、儿童停顿与真实延迟仍未完成。此前用户仅授权调查；本轮已询问是否允许修复超时结果丢失，在答复前不修改识别策略。
2. **角色动作**：当前有六姿态、轻微浮动、一次弹跳与星光；没有把静态兔子改造成用户要求的眨眼、挥手和口型动画。需要适用的分层或逐帧资源，不能以整体缩放替代动作完成。
3. **图片语义**：99 张 PNG 解决字体/打包问题，docs/47 的126项语义记录仍标为未验证；问候、请求、轮流等场景仍需真正可辨的插画与观察。
4. **设备门禁**：用户要求手机断开并取消模拟器。目标 Android 10 平板、整轮飞行模式、前后台/锁屏、30分钟稳定性、音频路由、内存、低空间、升级后数据核对仍未完成。此前成功安装不等于这些门禁通过。
5. **儿童观察**：PRD §21.3 七项观察和零识字闭环无完整记录，不能由成人/mock测试替代。
6. **代码审查后续**：TTS 回退离线保证、版本清单坏数据的组件证据、更新链和实际CI状态需继续核查，不能因应用内单元测试通过直接认定发布门禁完成。

## 其他规范与交付门禁

- docs/02 §20.1 单元与内容校验已扩展；§20.2 真实音频黄金集仍缺；§20.3 核心 Compose 流程已有 Robolectric 自动化，设备端仪器包构建不等于运行；§20.4/20.5 设备和儿童验收仍缺。
- docs/03～07 视觉基线与 docs/48 手机布局需要各自对应设备证据；没有重设计竖屏。
- docs/08～09 的 M0～M7 多数代码交付可由源码和测试定位；M3原生识别、M8设备稳定性与观察门禁不能关闭。
- docs/10～15 的后续内容/评分演进仍生效。英语评估当前按宽松文本漏斗，不是原始技术设计全部 WER/音素级能力；本次不擅改评分。
- docs/25～29 发布工具 Python 测试已重跑；源码推送与公开 APK Release 是不同交付。本轮不发布新 Release，未查询或声称远端 CI 最新成功。
- G-001～G-006、P-001～P-008、UC-001～003 不能从功能存在推断儿童有效使用。NG-001～009 仍为范围排除；§22建议统计不是全部已实现；§23未来候选不作为当前V1交付。
- AGENT.md 要求新增文档、WORKLOG仅追加、署名日期、改动提交；本轮按此执行。总目标保持进行中。
