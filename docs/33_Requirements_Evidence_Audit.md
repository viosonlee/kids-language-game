# PRD 逐项证据审查与剩余验收

> Author / Agent: Codex
>
> Date: 2026-09-30
>
> Status: 审查进行中；不是全产品验收通过声明
>
> Scope: docs/01 全部 83 个 FR/NFR 条目，结合 docs/02、docs/09 和后续增量规范核查；以当前源码和实际测试为依据

## 判读规则与证据索引

`A` 表示有针对该行为的自动化证据，但只证明测试覆盖范围；`S` 表示已检查源码，缺少独立运行验证；`D` 表示还需设备或真实儿童验收；`G` 表示已找到缺口或要求冲突。包含 D/G 的条目均不能认定完成。源码存在、构建成功和无报错不等于满足整条需求。

下表的短文件名对应 `app/src/main/java/com/kidslanguage/companion/` 中同名唯一文件；测试对应 `app/src/test/java/com/kidslanguage/companion/`。资产位于 `app/src/main/assets/`。

- 内容证据：ContentValidationTest、ContentRepositoryTest、LessonPlanTest、BundledModelsTest、OfflineModelArchiveTest。
- 状态机证据：RepeatViewModelTest、ChooseViewModelTest、HomeViewModelTest、PlaybackGateTest。
- 评分证据：SpeechEvaluationServiceTest（文本匹配；不测真实语音识别率）。
- 设置/声音/动效：SettingsRepositoryTest、SettingsPromptPlayerTest、MotionTest。
- 数据库证据：ProgressRecordTest、RoomProgressDatabaseTest；另有设备端 ProgressDatabaseTest 源码，不能把其存在当作已运行。
- 更新证据：AppUpdateTest、Python 发布工具测试、docs/29 的公开 Release 下载验证。
- 视觉证据：UiContrastTest 只验证颜色数值；它不能证明截图布局和可读性。当前没有完整 Compose 页面流程测试。

## 应用、语言与内容

| ID | 当前实现与证据 | 判定/剩余工作 |
| --- | --- | --- |
| FR-APP-001 | MainActivity → AppNavigation → Splash/Home；minSdk 29，无登录页 | S/D：Android 10 冷启动实测 |
| FR-APP-002 | 三语言音频和模型随 APK，核心题库从 assets 读取 | A/D：整轮飞行模式；系统识别回退仍可能联网 |
| FR-APP-003 | AndroidManifest 的 sensorLandscape | S/D：目标设备实际方向 |
| FR-APP-004 | SettingsRepository 的语言及偏好 DataStore | A/D：设置测试通过范围内，真实进程重启待验 |
| FR-LANG-001 | LanguageCode 三种语言；各语言题库和模型归档测试 | A/D：三语真机全流程 |
| FR-LANG-002 | LanguageCard 使用旗帜/图标 | S/D：儿童能否理解入口 |
| FR-LANG-003 | 路由传入 language；题库过滤与 Vosk.load(language) | A/S/D：过滤有测试，原生模型切换待测 |
| FR-LANG-004 | Vosk modelLock 串行，先 releaseCurrentModel 再加载 | S/D：模型句柄与 RSS 实测 |
| FR-CONTENT-001 | 语言文件、category 字段与 getCategories/getItems 过滤 | A：ContentRepositoryTest |
| FR-CONTENT-002 | CategoryCard 显示图标 | S/D：图标语义与儿童辨认 |
| FR-CONTENT-003 | LessonConfig、planLesson、12 个课程 | A：顺序/随机/题数/模式测试；docs/30 |
| FR-CONTENT-004 | JSON 题库、课程清单、资产路径 | A/S：增加已有场景题目无需改 Kotlin；新场景需数据与资源 |

## 跟读与选图

| ID | 当前实现与证据 | 判定/剩余工作 |
| --- | --- | --- |
| FR-SPEAK-001 | PracticeItem 包含目标、语言、audio/image、aliases | S/G：模型字段允许 audio/image 为 null；缺失字段的完整运行时验证仍需补齐 |
| FR-SPEAK-002 | RepeatViewModel 自动示范受 autoPlayPrompt 控制 | A：关闭自动播放仍可手动重播 |
| FR-SPEAK-003 | SpeakerButton → playCurrentPrompt | A/S/D：回调行为有测试，设备点击范围与音频待验 |
| FR-SPEAK-004 | Vosk SpeechService 麦克风输入与 recognizeFromAudio | S/D：三语真实采集/识别率不是 mock 回调能证明的 |
| FR-SPEAK-005 | 归一化、aliases、子串、Levenshtein；按 docs/15 的宽松规则 | A：文本评测测试；不宣称音素评分 |
| FR-SPEAK-006 | Passed 立即设置反馈，声音顺序由协程控制 | A/S/D：界面和音频联合验收 |
| FR-SPEAK-007 | TryAgain 表情与温和文案，无红叉/扣分 | A/S/D：实际重试体验 |
| FR-SPEAK-008 | 第 2 次部分匹配、第 3 次有效文本辅助通过 | A/G/D：空识别文本不计尝试，有声未识别仍可能反复重试，见 docs/21 |
| FR-SPEAK-009 | SoftPass 使用成功反馈，数据库单独统计 | A/S：状态机与记录测试 |
| FR-SPEAK-010 | 空文本及纯标点返回 NoSpeech，不计错题 | A/G：提示未区分物理无声与有声未识别 |
| FR-SPEAK-011 | SpeechService 内建 endpoint；总时限 10 秒 | S/G/D：儿童停顿行为未测；超时丢 final 文本已确认，按用户要求先调查 |
| FR-SPEAK-012 | autoStartRecording 默认 false；权限/页面状态约束 | A：自动与手动录音时序测试 |
| FR-LISTEN-001 | 2～4 张选图；不够时使用有效数量；无可玩题目隐藏玩法 | A/S/D：LessonPlanTest、ChooseViewModelTest；屏幕布局待验 |
| FR-LISTEN-002 | 每题 options.shuffled() | S：可随机化已实现；正确位置无固定索引 |
| FR-LISTEN-003 | 点击正确立即设置 Passed，反馈后按 autoNext 进入下一题 | A：重复点击、手动下一题、声音等待测试 |
| FR-LISTEN-004 | 选错重播，第二次高亮，紫色温和提示 | A/S/D：行为测试通过，图形反馈需观察 |
| FR-LISTEN-005 | 第三次选错辅助通过 | A：repeatedMistakesReplayThenHintThenAssist |

## 反馈、文字与家长模式

| ID | 当前实现与证据 | 判定/剩余工作 |
| --- | --- | --- |
| FR-FEEDBACK-001 | 开心兔子、星光符号/提示、成功音效 | S/G/D：尚无独立粒子/星光动画验收；不能将静态符号视为全部动画要求 |
| FR-FEEDBACK-002 | 等待完整表扬语音后跳题 | S/G：未核对所有语音后的总时长是否达到 0.8～1.5 秒目标 |
| FR-FEEDBACK-003 | SettingsPromptPlayer + SoundEffectManager 遵守反馈音量 | A：静音后流程仍推进，示范仍可播放 |
| FR-FEEDBACK-004 | 浮动/脉冲周期为数百毫秒以上；新增关闭动效入口 | A/S/D：MotionTest 证明停止循环；实际视觉频闪仍需验收 |
| FR-FEEDBACK-005 | 无扣生命/扣星/签到中断逻辑 | S：已检查领域模型、反馈和进度实现 |
| FR-TEXT-001 | 图片、喇叭、麦克风、图标可操作 | S/G/D：错误恢复仍依赖文字；零识字全流程需儿童验证 |
| FR-TEXT-002 | 标准/小字/隐藏三种偏好 | A/S/D：持久化和状态传播已测，视觉效果待验 |
| FR-TEXT-003 | reading 可空，跟读/选图按需显示 | S/D：实际假名/CJK 字体显示 |
| FR-PARENT-001 | 点击齿轮后要求乘法门禁，不是直接进入设置 | S/D：门禁 UI 流程尚无 Compose 测试；长按是原规范建议而非唯一机制 |
| FR-PARENT-002 | 语言、文字、自动示范/录音/跳题、题数、音效/音乐、重置 | A/S/G：偏好覆盖；重置失败/竞态整合测试尚不足 |
| FR-PARENT-003 | 今日、语言、时间、正常/辅助通过统计 | A/S/D：Room 聚合测试；家长页实际展示待验 |
| FR-PARENT-004 | AppVersionInfo 读包版本、内容清单、模型清单 | S/D：损坏清单 fallback 已有源码，界面待验 |

## 进度与内容管理

| ID | 当前实现与证据 | 判定/剩余工作 |
| --- | --- | --- |
| FR-PROGRESS-001 | item_progress 以 language/itemId 聚合，practice_attempt 记录事件 | A：真实 Room 测试 |
| FR-PROGRESS-002 | 数据库关闭后重开，逐题状态保留 | A/D：真实 Android 进程结束/升级后保留待验 |
| FR-PROGRESS-003 | attempts/normalPasses/assistedPasses/lastPracticedAt | A：ProgressRecordTest、RoomProgressDatabaseTest |
| FR-PROGRESS-004 | 表结构只存计数、时间、语言与题目 ID | S：无音频列、无保存 PCM 路径 |
| FR-DATA-001 | JSON 内容和课程配置 | A/S：解析与规划测试 |
| FR-DATA-002 | 当前 ID 使用语言前缀，仓库每语言去重 | S/G：缺少跨语言全局唯一 ID 的构建门禁 |
| FR-DATA-003 | content/manifest.json 的 schemaVersion/contentVersion | S：版本信息读取路径；内容版本 1.0.1 |
| FR-DATA-004 | 解析逐条隔离，非法音频路径/不存在的音频跳过 | A/G：音频 null、图片缺失等验证范围仍不完整 |
| FR-DATA-005 | 内容、音频、模型随 APK | A/S：资产测试与实际 APK 构建 |

## 音频与隐私

| ID | 当前实现与证据 | 判定/剩余工作 |
| --- | --- | --- |
| FR-AUDIO-001 | docs/10 将预录来源演进为生成式语音文件 | S/D：采用后续资产方案，非真人录音；听感待验 |
| FR-AUDIO-002 | generate_audio.py 的合成参数与现有 MP3 | G/D：没有统一响度测量/归一化验证证据 |
| FR-AUDIO-003 | 已有三语言示范资产 | D：自然度、儿童可模仿性需听音验收 |
| FR-AUDIO-004 | 等反馈声音结束再播放下一题，离开页面取消 | A/D：状态机/PlaybackGate 测试；设备音频焦点仍待验 |
| FR-AUDIO-005 | 录音前停止 prompt，首页音乐离开即停止 | A/S/D：外部播放器、路由与设备焦点实测 |
| FR-PRIV-001 | 无姓名/生日/学校/账户数据表或输入流程 | S：设置、模型和页面检查 |
| FR-PRIV-002 | Vosk 默认本地；模型失败后自动回退系统识别器 | G：系统识别器可能上传语音；与“默认不上传”有冲突，不能以 PREFER_OFFLINE 保证修复 |
| FR-PRIV-003 | 没有原始 PCM 持久化，诊断只保存页面内文字状态 | S：引擎/诊断/数据库检查 |
| FR-PRIV-004 | 未实装 Debug Audio Capture | S：未来条件需求目前不触发；后续增加必须提供明确开关与删除入口 |
| FR-PRIV-005 | Gradle 依赖与 Child Mode 无广告 SDK/广告流程 | S：依赖和路由检查 |

## 非功能要求

| ID | 当前实现与证据 | 判定/剩余工作 |
| --- | --- | --- |
| NFR-COMP-001 | minSdk 29；Robolectric Room/Motion 在 API 29 | A/D：原生 ASR 与完整应用 Android 10 真机 |
| NFR-COMP-002 | 主设计为 16:10 横屏平板 | D：2560×1600 目标设备完整截图验收 |
| NFR-COMP-003 | 使用 dp/sp，部分组件响应式 | S/G/D：首页与跟读仍有较多固定尺寸；手机评估见 docs/31 |
| NFR-PERF-001 | Compose 页面、异步题库/数据库 | D：帧率/卡顿实测缺失 |
| NFR-PERF-002 | Vosk 单模型替换顺序 | S/D：实际 RSS 与泄漏测量 |
| NFR-PERF-003 | Vosk endpoint → 回调 → 文本评分 | G/D：真实尾部延迟未测，超时路径有已知问题 |
| NFR-PERF-004 | 模型/题库 IO 已移后台，本轮兔子 PNG 解码也移后台 | S/D：主线程耗时 trace 缺失 |
| NFR-PERF-005 | 跟读加载进度指示与诊断加载提示 | A/S/D：状态测试覆盖，真实首次解压观察未完成 |
| NFR-REL-001 | readEntries 逐条容错，过滤无效内容 | A/G：剩余字段验证缺口见 DATA-004 |
| NFR-REL-002 | 拒绝权限弹文字对话框，录音错误主要提示文本 | G：缺少图形化直接进入选图的恢复操作 |
| NFR-REL-003 | Vosk load 捕获 Exception；失败结果传播至页面 | A/S/G/D：系统引擎/JNI 边界异常需要独立审查，不能保证“零崩溃” |
| NFR-REL-004 | LifecycleResumeEffect 清理、取消 SpeechService 并关闭识别器 | A/S/D：ViewModel 已测，AudioRecord 真机释放待验 |
| NFR-UX-001 | 自动示范后只需说话/点击图片，必要时点击麦克风 | A/S/D：正常流程实现，错误和权限流程还需减负 |
| NFR-UX-002 | 麦克风大尺寸；主按钮默认最小高度 80dp，喇叭有 76dp 调用 | S/G/D：原规范建议 ≥88dp，需结合布局复核 |
| NFR-UX-003 | 颜色之外还有兔子姿态、提示图标与文字 | S/D：选图提示的非颜色可辨性需实际验证 |
| NFR-UX-004 | 图片/声音/兔子承担正常流程提示 | S/G/D：错误恢复的非文字提示不足 |
| NFR-UX-005 | 常规错误为自定义提示；拒绝权限页可直接进入系统设置 | G：儿童路径应优先图形恢复，系统设置应放在家长操作路径 |

## 吉祥物追加要求

| ID | 当前实现与证据 | 判定/剩余工作 |
| --- | --- | --- |
| FR-MAS-001 | 首页、练习、结果页共用 MascotView | S/D：实际资源和布局验收 |
| FR-MAS-002 | 六状态枚举映射六张 PNG | S/D：素材身份、比例、表情一致性验收 |
| FR-MAS-003 | ReadyToSpeak/Recording 使用 Speak，PromptPlaying 使用 Listen | A/S：跟读状态机测试与 UI 映射 |
| FR-MAS-004 | 成功使用 Great/Praise、正反馈声音 | S/G/D：独立星光/粒子动效仍未完整验证 |
| FR-MAS-005 | 重试使用 TryAgain 与温和提示 | A/S/D：真实儿童反应待观察 |
| FR-MAS-006 | 反馈静音在 docs/30 补齐；本轮接入保存的动画开关 | A/S/D：MotionTest/SettingsRepositoryTest；组件视觉验收仍需设备 |

## 产品目标、非目标与用户流程

- G-001～G-006、P-001～P-008：架构和短任务流程支持这些目标，但识别率、声音到场景关联、主动开口和自主使用不能由代码证明。需要 PRD §21.3 的儿童观察。上述 G 类缺口也影响“无死路/无需阅读/离线”原则。
- NG-001～NG-009：现有领域模型和路由没有复杂课程树、拼写考试、数值儿童成绩、开放聊天、社交、商城、云账号或服务端 CMS。应用内更新仅由家长操作，不等同于新增云账号。
- UC-001/UC-002：三语内容、分类、玩法、示范、反馈代码已存在，但完整 UI 自动化和零识字观察未完成。
- UC-003：有效文本的三次辅助通过有测试；有声但无识别文本不能用该测试证明可恢复，须继续 docs/21 调查后的决策。
- 日语 ≥60、中文 ≥30、英语 ≥30：ContentValidationTest 和 ContentRepositoryTest 验证当前 62/32/32 个可加载条目。数量不能证明动作/社交场景图片语义正确。
- PRD §22 的完成率、主动复玩率和重播次数属于建议统计，当前未全部实现；现有完成计数不构成完整用户行为数据集。
- PRD §23 的 V1.1/V1.2/V2 候选、开放聊天等明确延后内容不作为本次 V1 完成条件。手机适配目前是用户询问后的方案评估，不以此替代平板验收。

## 其他文档、命令与验收门禁

| 来源 | 当前证据与范围 | 仍需完成 |
| --- | --- | --- |
| docs/02 §5、§20.1 | 资产/内容/评分/会话/进度测试已存在 | 全局 ID、Category 引用、全部缺失资源验证；英语 WER 设计与后续 docs/15 文本漏斗差异需明确记录 |
| docs/02 §20.2 | 尚无 tests/audio 黄金测试集 | 成人/噪声/近似/错误/静音数据与期望；儿童录音须用户明确许可，不默认采集 |
| docs/02 §20.3 | androidTest 当前只有 ProgressDatabaseTest | Home→课程、Retry、SoftPass、选图正误、家长门禁、拒绝权限 Compose 流程 |
| docs/02 §20.4、POC-ASR-001～004 | 历史安装/调试证据只覆盖部分路径 | Android 10 平板、内存、延迟、30 分钟、前后台/锁屏、断网、低空间、重启数据 |
| docs/02 §20.5、PRD §21.3 | 未获得完整儿童观察报告 | 七项儿童观察和图片理解度；成人测试不能替代 |
| docs/03～07 | 页面、tokens、六姿态资源已存在，截图投诉有历史修复 | 平板安全区、触摸大小、CJK 长句、大字体、素材风格和声音清晰度逐页验收 |
| docs/08～09 M0～M2 | 项目/页面/三语资产和模型均可构建 | M0-07 真机启动证据、M1 整体 UI 与 M2 全资源校验补齐 |
| docs/09 M3 | Vosk 接口/适配器/模型/诊断具备 | M3-05/07 实际 20 句与儿童语音评测；不能用 mock 替代 |
| docs/09 M4～M5 | 会话状态在 ViewModel 实装，非独立 SessionController 类 | 整轮离线、零识字用户流程端到端；类名不同本身不是功能缺陷 |
| docs/09 M6～M7 | Room/偏好/门禁/文字/声音/动效/题数/重置，三语数量达标 | 重置整合与 UI 验证、原生模型切换与内存 |
| docs/09 M8-01～08 | 部分单元回归与历史设备调试 | M8 全部设备稳定性/权限/声音/长期练习/儿童观察门禁尚未整体通过 |
| docs/10 | 生成式示范语音取代真人录音偏好；离线资产存在 | 响度与听感验收 |
| docs/11 | 单模型/本地 Vosk 具备 | 原文纯离线与系统回退存在冲突；“200ms/零崩溃/无泄漏”缺少可复核证据 |
| docs/12、docs/15 | 后续宽松漏斗优先，已有边界测试 | 第三次“有声”与“识别文本非空”不等价；不能宣称物理语音检测已实现 |
| docs/13 | SoundPool 与诊断页 | 音频路由、焦点、真实硬件错误/指标 |
| docs/14、docs/25～29 | CI 构建、Python工具、固定签名与 Release 校验；SSH 自动公开发布已跑通 | 私有源码 CI 最新运行状态、真机覆盖升级和记录保留 |
| docs/16～31 | 历次修复和局部验证记录 | 以当前源码为准，历史“待测”不自动变为通过 |
| Gradle test / assembleDebug | 上一提交为各 90 测试通过；本轮 MotionTest 单项已通过 | 本轮最终全量结果见 WORKLOG，不能从单项推断全量 |
| Gradle lintDebug | 初次 --offline 缺 lint-gradle 缓存，已发起联网检查 | 最终结果见本轮 WORKLOG；未通过前不称 lint 无问题 |
| Gradle assembleDebugAndroidTest / connectedDebugAndroidTest | 仪器测试包曾构建；本轮 SDK 未配置 platform-tools | 构建不等于运行，尚无当前设备测试结果 |

## 后续工作优先级

1. 完成动画与主线程图片解码修复、全量测试、lint 结果审查，并提交推送。
2. 补齐图形化录音失败恢复、内容完整性校验及对应界面/逻辑回归。
3. 审查识别器异常与系统回退策略；ASR 参数/超时优化仍尊重用户“先调查”的限制。
4. 建立 Compose 关键流程和进度重置整合测试，补齐自动化证据缺口。
5. 完成声音响度、动画时长、图片语义与平板布局检查；随后进行目标设备和儿童观察。

仅当所有必须项获得与其范围相符的证据后，才能把原始总目标标为完成。本记录不将设备不可用转换成“自动验收通过”。
