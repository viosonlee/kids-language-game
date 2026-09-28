# Kids Language Companion 文档总览

> 文档状态：V1.3 Draft Baseline  
> 目标平台：Android 10 平板，10.8 英寸，2560×1600，16:10  
> 目标用户：不会或尚不能依赖文字阅读的幼儿  
> 核心场景：在日本生活的幼儿，通过“看图 / 听音 → 跟说或选择 → 即时正反馈”练习日语、中文、英语  
> V1 优先级：日语幼儿园生存沟通 > 中文家庭语言维持 > 英语启蒙

---

## 1. 文档组成

本目录包含以下正式文档：

1. `01_Product_Requirements.md`  
   产品需求规格书（PRD/SRS）。定义为什么做、为谁做、做什么、不做什么，以及功能需求、非功能需求、验收标准。

2. `02_Technical_Design.md`  
   技术开发设计书。定义 Android 技术栈、模块划分、数据模型、内容格式、语音识别与判定、离线资源管理、状态机、测试策略和演进路径。

3. `03_UI_UX_Design.md`  
   UI/UX 设计书。定义面向不识字幼儿的交互原则、页面流、布局规范、组件、动画、反馈、家长模式以及 2560×1600 平板适配要求。

4. `04_Mascot_Design.md`  
   吉祥物 / 卡通角色设计说明。定义角色定位、视觉设定、动作状态、应用场景、资源命名与动画建议，并附 V1 角色素材。

5. `05_Mascot_Asset_Spec.md`  
   吉祥物资源规格书。定义资源拆分、命名、导出格式、状态映射与运行时建议。

6. `06_Wireframes_and_Page_Prototype.md`  
   页面原型与线框设计说明。定义可直接进入 Figma 的页面列表、布局结构、组件与原型流。

7. `07_High_Fidelity_Figma_Design_Spec.md`  
   高保真 Figma 设计规格。定义 Design System、颜色、Typography、组件、页面高保真布局、状态与动效。

8. `08_Android_Project_Initialization_and_Task_Plan.md`  
   Android 项目初始化与任务拆解。定义工程结构、Phase 划分、ASR PoC、内容、测试与 Release Gate。

9. `09_Development_Backlog.md`  
   面向 Human / AI Agent 的 V1 Milestone Backlog。

---

## 2. 产品一句话定义

**Kids Language Companion 是一个不依赖识字能力、优先离线运行、专门面向幼儿的三语“听懂 + 开口”练习 App。**

它不是完整语言课程平台，也不是儿童考试系统。

核心闭环只有两类：

```text
模式 A：看图 / 听标准语音 → 孩子跟说 → 宽松语音判定 → 正反馈 → 自动下一题
模式 B：听一句话 → 从图片中选择含义 → 正反馈 → 自动下一题
```

V1 不要求孩子阅读文字，不要求键盘输入，不要求理解菜单说明。

---

## 3. 已冻结的 V1 核心决策

除非产生明确的新需求或 PoC 结果证明不可行，以下决策视为 V1 基线：

| 项目 | V1 决策 |
|---|---|
| 客户端 | Native Android |
| 语言 | Kotlin |
| UI | Jetpack Compose |
| 最低系统 | Android 10 / API 29 |
| 设备方向 | Child Mode 优先固定横屏 |
| 主要分辨率 | 2560×1600；布局使用 dp 自适应，不直接以 px 写死 |
| 网络依赖 | 核心学习流程离线可用 |
| 账号 | V1 无账号、无登录 |
| 云后端 | V1 无必需后端 |
| 语音输入 | `AudioRecord` + 本地 ASR |
| V1 ASR 首选 | Vosk；通过抽象接口保留替换 sherpa-onnx / Whisper / Cloud 的能力 |
| 标准发音 | 预录音频优先，不依赖实时 TTS |
| 内容 | JSON + 本地图片 / 音频资源，数据驱动 |
| 进度 | 本地保存 |
| 原始录音 | 默认不保存 |
| 儿童判定 | 宽松；优先减少 False Negative |
| 失败处理 | 不无限卡题；重复失败后允许辅助通过并继续 |
| 首要内容 | 日本幼儿园日常听说 |
| V1 练习模式 | Listen & Repeat、Listen & Choose |
| V1 非目标 | 排行榜、商城、连续签到、复杂课程树、文字拼写题、聊天机器人 |

---

## 4. V1 最小内容规模

开发验收最低内容集：

- 日语：≥ 60 条
- 中文：≥ 30 条
- 英语：≥ 30 条
- 合计：≥ 120 条学习 Item

建议首发内容规模：120～180 条。

日语必须至少覆盖：

- 问候与礼貌
- 老师常用指令理解
- 一起玩 / 加入游戏
- 借东西 / 轮流 / 等待
- 吃饭 / 喝水
- 厕所
- 收拾 / 排队 / 集合
- 身体不舒服 / 求助
- 不明白 / 再说一次
- 回家 / 接送

---

## 5. V1 Definition of Done

只有同时满足以下条件，才认为 V1 完成：

- Android 10 目标平板可安装并稳定运行。
- 在无网络情况下可完成核心练习。
- 三种语言都可进入和练习。
- Listen & Repeat 完整闭环可运行。
- Listen & Choose 完整闭环可运行。
- 日语至少 60 条、中文至少 30 条、英语至少 30 条有效内容。
- 孩子不阅读任何文字也能完成主要练习。
- 单题不会因为 ASR 失败永久阻塞。
- 语音识别只加载当前语言模型，切换后旧模型可释放。
- 默认不上传音频、不保存原始录音。
- 进度和设置重启后保留。
- 家长可以控制语言、文字显示、自动下一题、音量及难度相关设置。
- 关键页面不存在触摸目标过小、文字溢出、图像遮挡或旋转错位。
- 通过真机长时间回归测试和至少一轮真实儿童可用性观察。

---

## 6. 新增的卡通角色设计基线

V1.1 新增卡通吉祥物设计要求：

- App 引入一只固定主角色，作为儿童模式的引导者与反馈角色。
- V1 主角色选型：**可爱白色小兔子**。
- 角色在听、说、答对、鼓励重试等关键节点提供明确动作反馈。
- 角色风格需统一于主 UI：明亮、软萌、低压、鼓励式，不采用夸张失败演出。
- 初版素材以静态表情/姿势集为主，运行时通过位移、缩放、淡入淡出与粒子特效组合出轻量动画。

角色素材路径：`assets/mascot/bunny_mascot_expression_sheet.png`。

## 7. 文档维护原则

### 6.1 Requirement ID 不复用

需求一旦分配 ID，例如 `FR-SPEAK-003`，即使删除也不得把 ID 分配给其他需求。

### 6.2 设计必须可追溯到需求

技术设计与测试用例应引用对应 Requirement ID。

例如：

```text
FR-SPEAK-004
  └─ Technical: SpeechEvaluationService
       └─ Test: ASR-EVAL-012
```

### 6.3 内容和代码分离

新增一句课程内容原则上不应要求修改 Kotlin 业务代码。

### 6.4 儿童体验优先于评分精确度

这是本项目最重要的产品原则之一：

> 在启蒙阶段，机器偶尔“放宽通过”的成本，小于孩子明明认真模仿却不断被机器判失败造成的挫败成本。

因此 ASR 结果不是考试成绩，而只是决定“是否给出鼓励反馈”的输入之一。

---

## 7. 术语

| 术语 | 定义 |
|---|---|
| Item | 一条可学习内容，例如「いっしょにあそぼう」 |
| Lesson | 一组 Item 的练习集合 |
| Category | 场景分类，如“幼儿园 / 一起玩” |
| Prompt Audio | 标准示范语音 |
| ASR | Automatic Speech Recognition，自动语音识别 |
| Recognition Result | ASR 输出文本及可选置信信息 |
| Similarity | 识别文本与期望表达的相似度 |
| Soft Pass | 达不到正常阈值但允许继续的辅助通过，不对儿童显示“失败分数” |
| Child Mode | 儿童使用的极简学习界面 |
| Parent Mode | 家长设置、内容浏览、统计与诊断界面 |

---

## 8. 建议仓库结构

```text
repo/
├─ app/
├─ docs/
│  ├─ 00_README.md
│  ├─ 01_Product_Requirements.md
│  ├─ 02_Technical_Design.md
│  └─ 03_UI_UX_Design.md
├─ content/
│  ├─ ja/
│  ├─ zh/
│  └─ en/
├─ tools/
└─ tests/
```

后续如果进入实际开发，可再增加：

```text
04_Content_Guide.md
05_Test_Plan.md
06_Release_Checklist.md
adr/
```



## 8. 资源与原型交付

本版本文档包已附带：

- 兔子吉祥物总览图；
- 拆分后的单姿势 PNG / WebP 资源；
- 资源清单 `assets/mascot/manifest.json`；
- 页面原型与线框设计说明，可直接用于 Figma 建模。


## 9. 结构化辅助文件

- `figma_design_tokens.json`：设计 Token，可供 Figma/设计 Agent/Android Theme 共用。
- `android_task_manifest.json`：开发 Milestone 与依赖关系的机器可读版本。
