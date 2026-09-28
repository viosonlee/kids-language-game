# Kids Language Companion Android 项目初始化与任务拆解

> Version: 1.0  
> Scope: Android V1  
> Target: Android 10 / API 29+, 10.8-inch Tablet, 2560×1600  
> Stack: Kotlin + Jetpack Compose  
> Related: `01_Product_Requirements.md`, `02_Technical_Design.md`, `07_High_Fidelity_Figma_Design_Spec.md`

---

# 1. 开发原则

V1 目标是尽快完成真实儿童可用的稳定闭环，而不是构建通用教育平台。

原则：

1. 小步实现核心闭环；
2. 优先真机验证；
3. 语音识别引擎必须可替换；
4. 内容必须数据驱动；
5. UI 与业务状态分离；
6. 默认离线；
7. 不保存原始录音；
8. 不在每个步骤执行全量回归测试；
9. 功能块完成后再执行针对性测试与阶段回归。

---

# 2. 仓库建议结构

```text
kids-language-companion/
├─ README.md
├─ docs/
│  ├─ requirements/
│  ├─ architecture/
│  ├─ ui/
│  └─ testing/
├─ app/
│  └─ src/
│     ├─ main/
│     │  ├─ java/com/example/kidslanguage/
│     │  │  ├─ app/
│     │  │  ├─ core/
│     │  │  │  ├─ audio/
│     │  │  │  ├─ asr/
│     │  │  │  ├─ content/
│     │  │  │  ├─ mascot/
│     │  │  │  ├─ model/
│     │  │  │  ├─ persistence/
│     │  │  │  └─ scoring/
│     │  │  ├─ feature/
│     │  │  │  ├─ home/
│     │  │  │  ├─ category/
│     │  │  │  ├─ mode/
│     │  │  │  ├─ repeat/
│     │  │  │  ├─ choose/
│     │  │  │  ├─ result/
│     │  │  │  └─ parent/
│     │  │  └─ ui/
│     │  │     ├─ components/
│     │  │     ├─ theme/
│     │  │     └─ navigation/
│     │  ├─ assets/
│     │  │  ├─ content/
│     │  │  ├─ audio/
│     │  │  ├─ images/
│     │  │  ├─ mascot/
│     │  │  └─ models/
│     │  └─ res/
│     ├─ test/
│     └─ androidTest/
├─ build.gradle.kts
├─ settings.gradle.kts
└─ gradle.properties
```

Package name 在立项时确定，不建议直接使用 `com.example` 上线。

---

# 3. 初始化阶段（Phase 0）

## P0-01 创建 Android 项目

要求：

- Kotlin
- Jetpack Compose
- minSdk = 29
- landscape 优先
- 单 Activity
- Material 3 可作为基础，但 Child Mode 需高度定制

验收：

- Android 10 真机可启动；
- 2560×1600 下无布局异常；
- Release build 可生成 APK。

---

## P0-02 建立基础工程规则

包含：

- Kotlin formatting
- static analysis
- Git ignore
- package convention
- resource naming
- docs location

建议资源命名：

```text
img_scene_<language>_<category>_<id>
aud_prompt_<language>_<id>
mascot_<state>
sfx_<event>
```

---

## P0-03 建立 Navigation Shell

路由：

```text
Splash
Home
Category
ModeSelect
Repeat
Choose
SessionComplete
ParentGate
ParentSettings
```

验收：

- 每个页面可用 placeholder 进入；
- Back 行为明确；
- Child Flow 可完整走通。

---

# 4. 基础 UI 阶段（Phase 1）

## P1-01 Theme & Design Tokens

实现：

- Color tokens
- Typography
- Shape
- Spacing constants
- Elevation

来源：`07_High_Fidelity_Figma_Design_Spec.md`

验收：

- 颜色不散落硬编码；
- 所有主按钮走统一组件。

---

## P1-02 Mascot Component

实现：

```kotlin
@Composable
fun Mascot(
    state: MascotState,
    size: MascotSize,
    animate: Boolean = true,
)
```

验收：

- 6 状态可切换；
- 图片比例正确；
- Great 有轻量庆祝动画；
- TryAgain 动画温和。

---

## P1-03 Child UI Core Components

实现：

- PrimaryLargeButton
- SecondaryLargeButton
- BigMicButton
- SpeakerButton
- LanguageCard
- CategoryCard
- ImageOptionCard
- ProgressIndicator
- FeedbackOverlay

---

# 5. 内容系统（Phase 2）

## P2-01 定义 Content Schema

建议：

```json
{
  "id": "ja_play_001",
  "language": "ja-JP",
  "category": "play",
  "mode": ["repeat", "choose"],
  "text": "いっしょにあそぼう",
  "reading": "いっしょにあそぼう",
  "audio": "audio/ja/play_001.ogg",
  "image": "images/ja/play_001.webp",
  "aliases": ["いっしょにあそぼ"]
}
```

---

## P2-02 ContentRepository

接口：

```kotlin
interface ContentRepository {
    suspend fun getCategories(language: LanguageCode): List<Category>
    suspend fun getItems(language: LanguageCode, categoryId: String): List<PracticeItem>
}
```

验收：

- JSON 可加载；
- schema 错误有日志；
- 找不到资源不会崩溃。

---

## P2-03 初始日语内容包

优先完成 20 条 PoC 内容：

- おはよう
- ありがとう
- ごめんね
- いれて
- かして
- どうぞ
- まって
- もういっかい
- いっしょにあそぼう
- わからない
- トイレにいきたい
- おみずください
- すわって
- あつまって
- ならんで
- かたづけて
- てをあらって
- おきがえして
- いたい
- せんせい

正式 V1 再扩到 ≥60 条日语。

---

# 6. 音频系统（Phase 3）

## P3-01 PromptAudioPlayer

职责：

- 播放预录标准音频；
- Replay；
- 播放结束事件；
- 不与录音冲突。

---

## P3-02 SoundEffectPlayer

事件：

- success
- praise
- retry-soft
- session-complete

音效规则：

- 短；
- 不刺耳；
- 可关闭。

---

# 7. ASR PoC（Phase 4）

这是最关键的风险验证阶段。

## P4-01 SpeechRecognizerEngine 抽象

```kotlin
interface SpeechRecognizerEngine {
    suspend fun load(language: LanguageCode): Result<Unit>
    suspend fun recognize(pcm: ByteArray): RecognitionResult
    suspend fun unload()
}
```

---

## P4-02 Vosk Adapter

V1 首选：Vosk。

要求：

- 当前语言模型按需加载；
- 切换语言释放旧模型；
- 不保存原始音频；
- 对短句识别优化。

---

## P4-03 儿童真声测试

必须用真实儿童语音验证至少 20 个日语目标词/句。

记录：

- Target
- Recognized
- Confidence
- 是否应判通过
- 是否误拒绝
- 是否误接受

关键指标不是成人 ASR 准确率，而是儿童体验：

- False Negative 应尽量低；
- 连续 2~3 次失败不能卡住。

---

# 8. Speech Evaluation（Phase 5）

## P5-01 Normalization

日语：

- 统一假名；
- 去标点；
- 允许常见语尾省略。

中文：

- 去标点；
- 简体标准化；
- 必要时引入拼音辅助。

英语：

- lowercase；
- 去标点；
- token compare。

---

## P5-02 Similarity Strategy

建议输出内部状态：

```text
Pass
Retry
SoftPass
NoSpeech
AsrError
```

不要直接向儿童显示百分比。

---

# 9. Listen & Repeat（Phase 6）

## P6-01 PracticeSessionController

状态：

```text
Loading
PromptPlaying
ReadyToSpeak
Recording
Recognizing
Evaluating
Passed
Retry
SoftPassed
Completed
```

---

## P6-02 RepeatScreen

要求：

- 页面进入自动播音；
- 自动/手动开始录音策略可配置；
- Recording 有视觉波形；
- Retry 可再听一次；
- 成功自动下一题。

---

# 10. Listen & Choose（Phase 7）

实现：

- 播放短句；
- 显示 3 张场景图；
- 点中即时判断；
- 不需要 Submit。

验收：

- 不看文字也可完成；
- 图片选项差异明显；
- 错误后允许继续选择或自动辅助。

---

# 11. Progress & Settings（Phase 8）

## P8-01 DataStore

保存：

- current language
- show text
- auto next
- animation enabled
- sound enabled
- session length

## P8-02 Room Progress

保存：

- item seen count
- item pass count
- last practiced
- lightweight mastery level

不保存原始语音。

---

# 12. Parent Mode（Phase 9）

实现：

- Parent Gate
- Settings
- Progress summary
- Reset progress
- Diagnostics

V1 不做云同步。

---

# 13. Content Expansion（Phase 10）

目标：

- JA ≥60
- ZH ≥30
- EN ≥30

每条必须具备：

- target text
- prompt audio
- scene image
- category
- repeat availability
- choose distractors or selection metadata

---

# 14. Testing Strategy

## 14.1 开发阶段

每次小改动不要求全量自动化测试。

优先：

- 编译
- Lint / static analysis
- targeted unit tests
- 单页面真机验证

## 14.2 功能块完成后

执行：

- feature unit tests
- Compose UI tests
- Android 10 instrumented tests
- target tablet real-device test

## 14.3 阶段结束

执行：

- full regression
- offline test
- long session test
- rotation / lifecycle test
- memory test
- ASR model switching test

---

# 15. 非功能验证项

必须检查：

- 冷启动时间；
- ASR 模型加载时间；
- 单题响应延迟；
- 内存峰值；
- 无网络行为；
- 录音权限拒绝；
- App 切后台后恢复；
- 长时间使用资源释放；
- 音频播放与录音互斥。

---

# 16. 推荐 Git / Agent 开发节奏

每个 Phase 使用独立任务组，不一次让 Agent 横跨全部模块。

建议：

```text
Phase 0-1  → UI skeleton
Phase 2-3  → content/audio
Phase 4-5  → ASR + scoring PoC
Phase 6    → Repeat complete
Phase 7    → Choose complete
Phase 8-9  → persistence/parent
Phase 10   → content expansion
```

每个 Phase 结束后：

1. 更新 docs；
2. targeted test；
3. 真机验收；
4. commit；
5. 再进入下一 Phase。

---

# 17. V1 Release Gate

发布前必须满足：

- Android 10 真机稳定；
- 离线可练习；
- JA/ZH/EN 三语可切换；
- Repeat / Choose 完整；
- 兔子 6 状态正常；
- ≥120 条内容；
- ASR 失败不会卡死；
- 默认不保存原始录音；
- 主要页面不依赖文字；
- Parent Settings 可控制声音/动画/文字；
- 长时间使用无明显内存泄漏；
- 经过至少一轮真实儿童使用观察。
