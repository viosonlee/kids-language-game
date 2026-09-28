# 🐰 Kids Language Companion (幼儿多语言语音启蒙伴学)

[![Android CI](https://github.com/viosonlee/kids-language-game/actions/workflows/android.yml/badge.svg)](https://github.com/viosonlee/kids-language-game/actions/workflows/android.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%2010%2B%20(API%2029%2B)-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.10.01-orange.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVI%20%2F%20Clean%20Architecture-purple.svg)]()
[![Offline ASR](https://img.shields.io/badge/ASR-Vosk%20Offline-success.svg)](https://alphacephei.com/vosk/)
[![Privacy](https://img.shields.io/badge/Privacy-COPPA%20Compliant%20(100%25%20Offline)-blue.svg)]()

> 面向 **3~6 岁学龄前幼儿** 的平板端多语言（中、日、英）纯离线语音互动伴学应用。  
> 围绕萌兔伴侣 **Momo Bunny** 的 6 态动态表情与交互反馈，以“听音选图”与“跟读开口”为核心玩法，打造零门槛、零广告、纯离线安全的趣味启蒙空间。

---

## 🌟 核心特性 (Key Features)

- **🐰 萌兔伴侣 Momo Bunny 6 态动画交互**：
  - 待机（Idle 微浮动）、倾听（Listen 耳朵轻晃）、示范发音（Speak 节律嘴型）、答对激励（Great / Praise 欢跃撒花）、重试鼓励（TryAgain 温和眨眼），全程提供强情感连接。
- **🎮 双模式趣味启蒙与正向游戏音效 (SFX)**：
  - **跟读模式 (Repeat Mode)**：看生动 Emoji 插图，听 AI 卡通标准声线发音，按住 130dp 大麦克风开口跟读，多级宽容度算法智能判定。
  - **选图模式 (Choose Mode)**：纯听音辨意，3 张大卡片直观点选，实时音效与即时色彩反馈。
  - **低延迟正向音效**：基于 Android 原生 `SoundPool` 驱动 4 款柔和游戏音效（泡泡点击、答对叮咚、结算欢庆华彩、温和重试），杜绝挫败感。
- **🌐 126 道高频生活主题题库**：
  - 日语 62 题（满足日本幼儿园日常生活全部高频交际用语）、中文 32 题、英语 32 题。
  - 涵盖问候与礼貌、一起玩耍、生活作息、求助与表达 4 大生活核心场景。
- **🎙️ AI 生成可爱卡通离线发音包**：
  - 全量 126 道题目内置高质量神经网络卡通声线（日语 Nanami 萌兔音、中文 Xiaoyi 灵动童声、英语 Ana 纯真童音）。
  - 总大小仅 1.45 MB，100% 离线播放，零网络流量消耗，未命中条目自动无缝回退至系统 TTS。
- **🛡️ 纯离线语音识别与 COPPA 隐私合规**：
  - 接入 Vosk 离线语音识别引擎，语音数据 100% 留在设备本地处理，绝不上传云端，彻底杜绝儿童隐私外泄。
  - 单模型互斥常驻内存管理机制，低内存设备流畅运行无压力。
- **🛠️ 开发者 / 家长 ASR 诊断实验室 (Developer Lab)**：
  - 家长中心提供实时语音识别与发音实验室，支持全量 126 题多语言试听、麦克风现场录音、原始文本与 5 层算法评分的实时可视化控制台。
- **👨‍👩‍👧‍👦 家长门禁与控制中心 (Parent Gate)**：
  - 随机两位数乘法防护门禁，有效阻隔幼儿误触误改。
  - 支持每轮题数调节（3 / 5 / 10 题）、自动跳题开关、字幕显示开关及练习记录安全重置。
- **📱 幼儿专属 Human Interface Guidelines (HIG)**：
  - 强制横屏沉浸式全屏展示，隐藏系统状态栏与底部手势条，防止幼儿误滑退出。
  - 最小点击靶点 ≥ 96dp，主按钮 ≥ 130dp，阶梯 28~32dp 大圆角卡片，保护视力的温润柔和配色。

---

## 🏗️ 架构与技术栈 (Tech Stack & Architecture)

| 层次 / 领域 | 核心技术选型 | 说明 |
| :--- | :--- | :--- |
| **语言与运行时** | Kotlin 2.0.21, OpenJDK 21 | 纯 Kotlin 现代化代码库 |
| **UI 框架** | Jetpack Compose + Material 3 | 声明式响应式 UI，无传统 XML 布局 |
| **架构模式** | MVI / Clean Architecture + UDF | `ViewModel` + `StateFlow` 单向数据流 |
| **导航** | Compose Navigation 2.8.5 | 类型安全路由跳转与参数传递 |
| **语音识别 (ASR)** | Vosk Android SDK (`0.3.47@aar`) + JNA | 纯离线 Kaldi 识别，分层回退系统 ASR / Mock |
| **发音播放 (TTS)** | `MediaPlayer` (Assets MP3) + Android TTS | 优先加载本地卡通离线包，静默兜底原生 TTS |
| **游戏交互音效** | `SoundPool` (4 通道低延迟) | 泡泡点击、和弦叮咚、欢庆华彩、温和重试 |
| **持久化** | Jetpack DataStore Preferences | 响应式保存家长设置，单例非阻塞读取 |
| **持续集成** | GitHub Actions (`android.yml`) | 云端自动化构建、测试与 APK 制品打包 |
| **工程构建** | Gradle 8.11.1 + AGP 8.7.3 | MinSdk 29 (Android 10), TargetSdk 35 |

---

## 📁 目录结构 (Directory Structure)

```text
kids-language-game/
├── .github/workflows/
│   └── android.yml                  # GitHub Actions CI/CD 云端构建与测试工作流
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   ├── audio/           # 126 个 AI 生成卡通发音 MP3 + 4 种 SFX WAV
│   │   │   │   ├── content/         # 题库 JSON (categories, items_{ja,zh,en}.json)
│   │   │   │   └── models/          # 离线 Vosk 语音模型解压存储目录
│   │   │   ├── java/com/kidslanguage/companion/
│   │   │   │   ├── app/             # Application, AppContainer (依赖注入)
│   │   │   │   ├── core/
│   │   │   │   │   ├── asr/         # 离线 Vosk / 系统 ASR 抽象与引擎实现
│   │   │   │   │   ├── audio/       # PromptPlayer (卡通MP3/TTS) & SoundEffectManager (SoundPool)
│   │   │   │   │   ├── content/     # AssetContentRepository 题库仓库
│   │   │   │   │   ├── model/       # 核心领域模型 (Item, Session, Evaluation)
│   │   │   │   │   ├── persistence/ # SettingsRepository (DataStore)
│   │   │   │   │   └── scoring/     # 儿童发音分层宽容度评测算法
│   │   │   │   ├── feature/
│   │   │   │   │   ├── splash/      # 启蒙欢迎引导
│   │   │   │   │   ├── home/        # 主页语言选择与吉祥物互动
│   │   │   │   │   ├── category/    # 生活主题分类选择
│   │   │   │   │   ├── mode/        # 练习模式选择
│   │   │   │   │   ├── repeat/      # 跟读模式主练习
│   │   │   │   │   ├── choose/      # 选图模式主练习
│   │   │   │   │   ├── summary/     # 结算与大星星庆祝
│   │   │   │   │   └── parent/      # 家长门禁、设置中心 & ASR 诊断实验室
│   │   │   │   ├── navigation/      # Compose 路由导航图
│   │   │   │   └── ui/              # Design Tokens, Momo Bunny 6态组件, 大按钮
│   │   │   └── AndroidManifest.xml
│   │   └── test/                    # 单元测试 (发音评测算法、题库与全量音频/音效资产完整性)
│   └── build.gradle.kts
├── docs/                            # 完整设计与实装技术文档集 (00~14)
├── scripts/
│   ├── generate_audio.py            # AI 卡通语音批量生成管线脚本
│   ├── generate_sfx.py              # 游戏化交互音效合成脚本
│   └── setup_vosk_models.py         # Vosk 离线模型一键下载与 ADB 部署工具
├── AGENT.md                         # 研发多智能体协同规范与行为准则
├── README.md                        # 本说明文件
└── gradlew.bat                      # Gradle 构建脚本
```

---

## 🚀 快速开始与构建 (Getting Started)

### 环境要求
- **Android Studio**：Ladybug (2024.2+) 或更高版本
- **JDK**：OpenJDK 21 (推荐 21.0.8 LTS)
- **Android SDK**：API 35 (Android 15), Build-Tools 35.0.0, MinSdk 29 (Android 10)

### 1. 克隆代码库
```bash
git clone https://github.com/viosonlee/kids-language-game.git
cd kids-language-game
```

### 2. 执行单元测试
验证发音评测算法、题库 Schema 及全量 126 项音频 + 4 项 SFX 音效文件完整性：
```bash
./gradlew.bat test
```

### 3. 编译打包 Debug APK
```bash
./gradlew.bat assembleDebug
```
产物位置：`app/build/outputs/apk/debug/app-debug.apk`。

---

## 📦 实用自动化脚本集 (Automation Tools)

### 1. Vosk 离线模型一键下载与真机部署
自动下载官方日语、中文、英语小型 Kaldi 模型并通过 ADB 推送至平板设备：
```bash
python scripts/setup_vosk_models.py
```

### 2. AI 卡通发音资产生成管线
扩充题库后一键批量生成高拟真卡通音频并自动注入 JSON 映射：
```bash
python scripts/generate_audio.py
```

### 3. 游戏互动音效合成
重新合成低延迟 WAV 正向反馈音效：
```bash
python scripts/generate_sfx.py
```

---

## 📚 详细设计文档索引 (Documentation Index)

本项目遵照严格的软件工程全生命周期规范推进，完整文档存档于 [`docs/`](file:///d:/personalworkspace/kids-language-game/docs/)：

| 编号 | 文档名称 | 核心内容 |
| :--- | :--- | :--- |
| **00** | [文档体系导航与总览](file:///d:/personalworkspace/kids-language-game/docs/00_README.md) | 架构体系、各阶段产物索引与目录说明 |
| **01** | [产品需求说明书 (PRD)](file:///d:/personalworkspace/kids-language-game/docs/01_Product_Requirements.md) | 目标用户、核心功能、合规要求与指标基准 |
| **02** | [系统技术架构设计](file:///d:/personalworkspace/kids-language-game/docs/02_Technical_Design.md) | 总体技术分层、离线 ASR、状态机与数据流设计 |
| **03** | [UI/UX 设计规范](file:///d:/personalworkspace/kids-language-game/docs/03_UI_UX_Design.md) | 3~6岁儿童交互设计原则、色盘与大靶点规范 |
| **04** | [吉祥物形象设计](file:///d:/personalworkspace/kids-language-game/docs/04_Mascot_Design.md) | Momo Bunny 兔兔角色背景、性格设定与动作规范 |
| **05** | [吉祥物资源规范](file:///d:/personalworkspace/kids-language-game/docs/05_Mascot_Asset_Spec.md) | 6 态 SVG 资源尺寸、切图规范与动效配置 |
| **06** | [原型与线框图](file:///d:/personalworkspace/kids-language-game/docs/06_Wireframes_and_Page_Prototype.md) | 核心 8 页面 ASCII/Markdown 线框图流转原型 |
| **07** | [高保真 Figma 设计规格](file:///d:/personalworkspace/kids-language-game/docs/07_High_Fidelity_Figma_Design_Spec.md) | 像素级排版、Design Tokens JSON 映射表 |
| **08** | [工程初始化与任务规划](file:///d:/personalworkspace/kids-language-game/docs/08_Android_Project_Initialization_and_Task_Plan.md) | 里程碑 M0~M8 详细任务分解与依赖关系 |
| **09** | [研发 Backlog 追踪](file:///d:/personalworkspace/kids-language-game/docs/09_Development_Backlog.md) | 任务状态清单与开发优先级 |
| **10** | [AI 卡通发音包生成规格](file:///d:/personalworkspace/kids-language-game/docs/10_AI_Audio_Asset_Generation_Spec.md) | 神经声线选型、调音参数、资产存储与回退机制 |
| **11** | [Vosk 离线语音识别引擎规格](file:///d:/personalworkspace/kids-language-game/docs/11_Vosk_Offline_ASR_Engine_Spec.md) | JNI Native 集成、单模型内存驻留管理与容灾降级 |
| **12** | [儿童发音宽容度评测算法规格](file:///d:/personalworkspace/kids-language-game/docs/12_Child_Speech_Evaluation_Algorithm_Spec.md) | 跨语言归一化、编辑距离与幼儿 SoftPass 保护机制 |
| **13** | [交互音效与诊断实验室规格](file:///d:/personalworkspace/kids-language-game/docs/13_Sound_Effects_and_Diagnostic_Lab_Spec.md) | SoundPool 架构、4 种游戏音效与 ASR 现场实验室设计 |
| **14** | [CI/CD 与离线模型部署规格](file:///d:/personalworkspace/kids-language-game/docs/14_CI_CD_and_Model_Deployment_Automation_Spec.md) | GitHub Actions 自动化门禁与 Vosk 模型部署管线 |
| **--** | [工作进展全量日志 (WORKLOG)](file:///d:/personalworkspace/kids-language-game/docs/WORKLOG.md) | 详细记录 V1.0 ~ V1.4 各版本研发演进轨迹 |
| **--** | [多智能体协同准则 (AGENT.md)](file:///d:/personalworkspace/kids-language-game/AGENT.md) | 团队协作规范、文档防篡改与 Git 提交铁律 |

---

## 📄 开源许可证与声明 (License)

本项目遵循 Apache 2.0 License 授权，离线语音模型遵循各自开源许可（Vosk Apache 2.0 / Kaldi）。
