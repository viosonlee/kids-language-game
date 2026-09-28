# Kids Language Companion 开发工作日志 (Work Log)

本文档用于持续跟踪和记录项目的开发迭代、技术落地、问题修复与版本交付历程。

---

## 2026-09-28 | V1.0 首轮工程实装与核心闭环交付 | By: Antigravity Agent

### 1. 任务背景与目标
- **输入来源**：根据 `docs/` 下的 PRD (`01_Product_Requirements.md`)、技术架构设计 (`02_Technical_Design.md`)、UI/UX 规范 (`03_UI_UX_Design.md`)、吉祥物设计规格 (`04_Mascot_Design.md` / `05_Mascot_Asset_Spec.md`)、原型与 Figma Tokens (`07_High_Fidelity_Figma_Design_Spec.md`) 以及任务清单 (`08_Android_Project_Initialization_and_Task_Plan.md` / `09_Development_Backlog.md`)。
- **目标**：从零建立 Android Native 工程，实现面向学龄前幼儿的低识字门槛语言练习应用，打通听、说、选、反馈的核心闭环，并在本地完成构建与全量测试验证。

---

### 2. 关键实施成果

#### 2.1 项目初始化与工程基础设施 (Milestone M0)
- **技术栈配置**：Kotlin 2.0.21 + Jetpack Compose + Compose Navigation + ViewModel (StateFlow) + DataStore + AGP 8.7.3。
- **环境基准**：
  - `minSdk = 29` (Android 10)
  - `compileSdk = 35`
  - `targetSdk = 35`
  - JDK 21 (OpenJDK 21.0.8 LTS)
  - Gradle 8.11.1 Wrapper
- **设备适配**：强制配置 `sensorLandscape` 横屏方向与无边框沉浸式窗口（自动隐藏系统状态栏与导航栏），深度适配 10.8 英寸 2560×1600 (16:10) 平板。
- **版本控制**：配置标准 Android `.gitignore`，过滤 build 目录、`.gradle` 及本地开发配置。

#### 2.2 Design Tokens 与儿童专属 UI 组件库 (Milestone M1)
- **Design Tokens**：严格对齐 `figma_design_tokens.json` 实现：
  - `Color.kt`：`Primary500` (#4DA3FF)、`Primary100`、`AccentYellow` (#FFC94A)、`SuccessBg` (#FFF6CE)、`RetryBg` (#F2EDFF) 等。
  - `Spacing.kt`：4~64dp 阶梯间距系统及 28~32dp 大圆角卡片定义。
  - `Type.kt` / `Theme.kt`：22~44sp 大号字阶，适配幼儿视力与低识字场景。
- **Momo Bunny 吉祥物系统 (`MascotView.kt`)**：
  - 接入完整 6 态资源（`Idle`, `Listen`, `Speak`, `Great`, `TryAgain`, `Praise`）；
  - 配合平滑 Compose 动画：待机微浮动、倾听微摆、答对跳跃庆祝、重试轻量温和反馈。
- **幼儿交互组件**：
  - `BigMicButton`：大靶点（130dp+），带动态波纹脉冲扩散效果；
  - `SpeakerButton`：大音响图标，播放时高亮跳动；
  - `LanguageCard` / `CategoryCard` / `ImageOptionCard`：高对比、大触摸区域、直观卡片；
  - `ProgressIndicatorBar` 与 `FeedbackOverlay`：柔和色系正向即时反馈，杜绝挫败感。

#### 2.3 领域模型与离线内容体系 (Milestone M2)
- **数据结构**：定义 `LanguageCode`、`Category`、`PracticeItem`、`SessionStepState`、`EvaluationResult` 等领域模型。
- **内容仓库 (`AssetContentRepository`)**：从本地 Assets 加载并缓存分类与题目 JSON。
- **内容数据包**：
  - `categories.json`：问候与礼貌、一起玩耍、日常习惯、求助与表达 4 大场景；
  - `items_ja.json`：完整录入 20 条日本幼儿园高频生活用语（おはよう、ありがとう、ごめんね、どうぞ、いっしょにあそぼう、いれて、かして、まって、もういっかい、おみずください、てをあらって、すわって、あつまって、ならんで、かたづけて、おきがえして、トイレにいきたい、いたい、せんせい、わからない）；
  - `items_zh.json` & `items_en.json`：中英对照数据包。

#### 2.4 语音识别与儿童发音判定引擎 (Milestone M3 & M5)
- **语音识别抽象 (`SpeechRecognizerEngine`)**：支持系统语音识别与离线引擎扩展，并提供 Mock 引擎供自动化测试。
- **发音评分与容错 (`ChildSpeechEvaluationService`)**：
  - 日语片假名自动转换为平假名进行宽松匹配；
  - 支持别名与常见口语语尾省略（如 `いっしょにあそぼ` -> `いっしょにあそぼう`）；
  - 引入 Levenshtein 模糊相似度判定；
  - 针对幼儿提供宽松阈值保护：同一题目尝试 2 次以上触发 SoftPass 辅助通过，避免卡题挫败。
- **发音播放器 (`AndroidPromptPlayer`)**：优先读取本地音频资产，缺省状态自适应降级为幼儿语速的 TextToSpeech 朗读。

#### 2.5 核心练习玩法与页面流程串联 (Milestone M4, M5, M6)
- **完整路由架构 (`AppNavigation.kt`)**：
  - `SplashScreen`（吉祥物待机迎宾与短延时路由）
  - `HomeScreen`（大兔子、日/中/英三语选择卡片、大“开始练习”入口、右上角家长门禁）
  - `CategoryScreen`（2 列大卡片直选场景）
  - `ModeSelectScreen`（跟读模式 vs 选图模式）
  - `RepeatScreen`（跟读模式：看场景图/大麦克风录音/状态机驱动兔子表情/答对 1.4s 自动跳题）
  - `ChooseScreen`（选图模式：纯听音 3 张大卡片点选，即时变色与激励）
  - `SessionCompleteScreen`（大兔子星星庆祝，提供“再玩一次”与“回主页”）
  - `ParentScreens.kt`（乘法算术验证门、字幕显示开关、自动跳题控制、题数调整与数据重置）

---

### 3. 构建与验证记录

1. **单元测试验证 (`gradlew.bat test`)**：
   - 运行测试用例 `SpeechEvaluationServiceTest.kt`；
   - 全部 6 个核心测试全部通过：
     - `exact match passes with high score` (通过)
     - `alias match passes` (通过)
     - `katakana to hiragana normalizes and passes` (通过)
     - `empty input returns NoSpeech` (通过)
     - `repeated attempt with partial similarity soft passes for preschooler` (通过)
     - `english phrases ignore casing and punctuation` (通过)
   - 结果：`BUILD SUCCESSFUL in 46s`。

2. **APK 编译打包 (`gradlew.bat assembleDebug`)**：
   - 验证资源合并、DEX 编译及打包管线；
   - 产物：`app/build/outputs/apk/debug/app-debug.apk` (大小约 65.4 MB，内含全套表情图集与内容资产)；
   - 结果：`BUILD SUCCESSFUL in 1m 44s`。

---

### 4. Git 提交与版本同步

- **分支**：`main`
- **提交哈希**：`7826b29`
- **提交信息**：`feat: implement Kids Language Companion V1 Android client and docs`
- **变更统计**：101 个文件新增，9853 行代码与配置。
- **远端同步**：已成功推送到 `origin/main` (`https://github.com/viosonlee/kids-language-game.git`)。

---

### 5. 后续推进计划 (Next Steps)
1. **ASR 离线引擎打包 (Milestone M3/M7)**：集成 Vosk 针对日语小模型的 `.aar` 与离线 model 资产解压。
2. **真机与音频体验测试**：在真实 Android 10 平板上试听 Prompt 音频与儿童语音交互顺畅度。
3. **内容扩充 (Milestone M7)**：将日语词库从 20 条扩充至 ≥ 60 条，中英语各扩充至 ≥ 30 条。

---

## 2026-09-28 | V1.1 题库全量扩充与 Vosk 离线 ASR 引擎落地 | By: Antigravity Agent

### 1. 任务背景与目标
- **输入来源**：用户指令要求继续推进第 1 点（题库规模扩充至 PRD 标准）与第 2 点（纯离线 Vosk ASR 引擎接入），并评估第 3 点（卡通音发音包）的 AI 替代方案。
- **目标**：
  1. 将题库从 32 条扩展至 ≥ 120 条（日语 ≥ 60，中文 ≥ 30，英语 ≥ 30）；
  2. 接入 Vosk Android Native SDK，实现 `VoskSpeechRecognizerEngine` 并配置单模型内存管理机制；
  3. 论证 AI 生成卡通语音（如 Edge-TTS、CosyVoice、ElevenLabs）离线音频包的替代方案可行性并给出规划；
  4. 保持单元测试与 APK 打包 100% 绿灯，并进行本地 Git 提交。

### 2. 关键实施成果

#### 2.1 题库规模全量达标（Milestone M7）
- **日语题库 (`items_ja.json`)**：由 20 条扩展至 **62 条**（满足 PRD ≥ 60 要求），全方位覆盖：
  - 问候与礼貌（15 条）
  - 一起玩与加入游戏（15 条）
  - 日常作息与指令理解（16 条）
  - 身体不适、求助与接送（16 条）
- **中文题库 (`items_zh.json`)**：由 6 条扩展至 **32 条**（满足 PRD ≥ 30 要求）。
- **英语题库 (`items_en.json`)**：由 6 条扩展至 **32 条**（满足 PRD ≥ 30 要求）。
- **总计**：全库累计 **126 条练习条目**，全部配备拼音/平假名读音、中文含义、场景插图 Emoji、口语别名及 3 选 1 干扰项。
- **自动化测试**：新增 `ContentValidationTest.kt`，从条目数、Schema 字段完整性、三大语言下限对 JSON 数据进行自动化测试验证。

#### 2.2 Vosk 离线 ASR 引擎接入（Milestone M3 / M7）
- **依赖集成**：引入 `com.alphacephei:vosk-android:0.3.47@aar` 与 `net.java.dev.jna:jna:5.13.0@aar`。
- **引擎落地 (`VoskSpeechRecognizerEngine.kt`)**：
  - 支持从应用外部/内部存储的 `models/vosk-model-small-{ja|cn|en-us}` 读取离线模型；
  - 严格遵循 PRD 规范：内存中仅驻留当前所选语言的单个 ASR 模型，语言切换时主动 `unload()` 释放旧模型资源；
  - 具备优雅降级能力：当特定语言模型文件尚未在真机解压就绪时，自动平滑 fallback 至系统识别/模拟引擎，保证 App 0 崩溃。
- **依赖注入**：在 `AppContainer` 中完成替换，全局统一接入 Vosk 引擎。

#### 2.3 关于“AI 生成卡通音”替代真人录音的评估方案
- **可行性分析**：完全可行且属于业内目前效果与性价比最高的方案。
- **方案建议**：
  - 采用高质量神经语音合成模型（如 Edge-TTS 的 `ja-JP-NanamiNeural` 或动漫卡通声线、`zh-CN-XiaoxiaoNeural`、`en-US-AnaNeural`，或 CosyVoice/ElevenLabs 的可爱幼童声线）；
  - 离线批处理脚本一次性为 126 条题目生成轻量 `.ogg` / `.mp3` 音频文件，存放于 `app/src/main/assets/audio/{lang}/`；
  - 优势：发音纯正统一、语速可控（设定为 0.85x 幼儿友好语速）、语气软萌活泼、彻底免除真人录音周期与成本，同时在 Android 端实现 100% 零网络延迟离线播放。

### 3. 构建与验证记录
1. **单元测试 (`gradlew.bat test`)**：
   - 运行 `SpeechEvaluationServiceTest` 与 `ContentValidationTest`；
   - 全部 11 个测试用例通过（题库规模校验用例全部通过）；
   - 结果：`BUILD SUCCESSFUL in 23s`。
2. **APK 打包验证 (`gradlew.bat assembleDebug`)**：
   - 包含 Vosk Native So 库（`libvosk.so`, `libjnidispatch.so`）及 126 条题库内容；
   - 产物大小：103.5 MB；
   - 结果：`BUILD SUCCESSFUL in 26s`。

