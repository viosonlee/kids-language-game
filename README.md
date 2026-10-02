# Kids Language Companion

面向平板的幼儿语言练习应用，支持日语、中文和英语的跟读、听音选图、兔子反馈与家长设置。最低 Android 10（API 29），目标 API 35。

## 当前实现

- 126 道内置练习：日语 62、中文 32、英语 32；包含标准发音、图片符号和干扰选项。
- APK 内置三语言 Vosk small 模型，首次使用相应语言时解压；正常安装不需要 ADB 推送模型。
- 发音评测采用宽松文字匹配与渐进辅助通过；这是识别结果匹配，不是音素级专业发音评分。
- 家长门禁、默认语言、标准/辅助小字/隐藏文字、自动示范、自动录音、自动下一题、每轮题数、随机出题、2～4 张选图，以及反馈语音与音效开关/音量。
- 12 个语言/场景课程配置，支持题目子集与顺序、随机/固定顺序、题数上限和可用玩法；配置说明见 [课程与反馈设置](docs/30_Lesson_Settings_and_Feedback_Volume.md)。
- 家长可关闭兔子浮动、点击缩放、麦克风/喇叭脉冲和页面淡入淡出；录音状态、反馈表情与操作仍保留。
- Room 保存逐题与逐次练习计数，家长页显示今日、各语言、正常/辅助通过、选图正误和最近练习时间；不保存原始录音或识别文本。
- 诊断页提供模型加载、示范播放、手动识别和评测结果查看。

语音识别使用内置离线模型。模型不可用时显示恢复操作，可重试、重听或切换选图；不会自动转向可能联网的系统语音识别服务。异常处理和资源释放说明见 [离线语音生命周期](docs/35_Offline_ASR_Failure_and_Resource_Lifecycle.md)。儿童真实发音识别率需要在目标平板上验证，超时空结果问题及优化建议见 [调查记录](docs/21_ASR_Empty_Result_Investigation.md)。

## 构建

使用 JDK 21、Android SDK API 35 和 Gradle Wrapper。将 `JAVA_HOME` 指向本机 JDK 21，在 Android Studio 配置 Gradle JDK；SDK 路径通过本地 `local.properties` 或环境提供。个人路径不提交到仓库。

Windows PowerShell：

```powershell
.\gradlew.bat test assembleDebug assembleDebugAndroidTest
```

Linux/macOS：

```sh
./gradlew test assembleDebug assembleDebugAndroidTest
```

依赖已缓存时可加 `--offline`。产物：

- 应用：`app/build/outputs/apk/debug/app-debug.apk`
- 仪器测试包：`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`
- 单元测试报告：`app/build/reports/tests/`

Debug 包名为 `com.kidslanguage.companion.debug`。覆盖安装须使用与设备现有版本相同的签名；签名密钥留在本机。首次进入跟读并点击麦克风时申请录音权限，拒绝后仍可玩选图。

已连接测试设备并配置 SDK platform-tools 时可运行：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

仪器测试会安装/启动测试 APK，应使用测试设备。构建测试包成功不代表设备测试已执行。

## 应用内更新

家长设置提供手动检查、下载和安装更新，默认分发源为 [kid-game-release](https://github.com/viosonlee/kid-game-release) 的 GitHub Releases。首次需手动安装带更新功能的版本；后续升级必须保持相同包名和签名，并递增 versionCode。支持自定义 HTTPS 更新地址。

打包清单、首次发布和可选 Actions 自动签名发布步骤见 [应用内升级与分发说明](docs/28_In_App_Updates_and_GitHub_Distribution.md)。源码和私钥不上传公开分发仓库。

首个版本已经发布：[下载 APK](https://github.com/viosonlee/kid-game-release/releases/latest/download/app-debug.apk)。当前使用已有 SSH、Git LFS 和分发仓库自身的 GitHub Actions 自动发布，无需额外网页登录或提供个人 token，具体见 [自动发布与免费平台核查](docs/29_SSH_Automated_Releases_and_Free_Hosting.md)。

## 测试与 CI

GitHub Actions 在 main 推送或 PR 时执行 lint、单元测试、应用构建和仪器测试包构建。测试覆盖题库容错、资产、三语言模型大小/哈希、评测规则、识别器失败/取消/资源释放、会话生命周期、动效控制、设置及记录语义；实际 Room 事务有 Robolectric 测试和独立仪器测试。CI 保存 APK、测试与 lint 报告。

真实麦克风采集、儿童发音、设备音频路由及动画效果需要设备验证，不能由单元测试替代。

## 目录

- `app/src/main/java/com/kidslanguage/companion/core/`：内容、语音、音频、评分、持久化。
- `app/src/main/java/com/kidslanguage/companion/feature/`：页面和 ViewModel。
- `app/src/main/assets/content/manifest.json`：内容版本与语言清单。
- `app/src/main/assets/models/manifest.json`：模型版本、官方来源、大小和 SHA-256。
- `app/schemas/`：Room 数据库 schema，后续结构变更须提供迁移。
- `scripts/`：图标、发音、音效等资产维护工具；旧模型 ADB 部署工具仅用于开发维护，普通安装无需运行。
- `docs/`：原始产品/技术规范及编号增量审查记录。

## 协作规范与审查状态

遵守 [AGENT.md](AGENT.md)：既有 `docs/` 文件不覆盖，新增文档注明作者和日期，`docs/WORKLOG.md` 只追加；修改需验证并本地提交。

本轮完整代码审查仍在进行。已完成的修复及证据见 `docs/16` 起的增量记录；背景音乐听感、设备测试、识别率优化决策及最终验收仍有待处理。历史文档中的“已完成/已验证”应结合这些修正记录和实际测试结果阅读。

83 条 PRD 功能/非功能需求的逐项证据与剩余缺口见 [需求证据审查](docs/33_Requirements_Evidence_Audit.md)。自动化通过范围与设备/儿童观察验收分开记录，不将编译成功视为整体产品验收。

录音图形恢复、题库完整性、本地图片渲染及首次点击修复见 [增量记录](docs/34_Recording_Recovery_and_Content_Integrity.md)。该记录保留了中途审批故障；恢复后最终验证已通过（Debug 107 项、Release 101 项，构建与 lint 通过），最新结果见 [工作日志](docs/WORKLOG.md)。这批改动尚未发布到公开 APK。

家长设置保存异常与进度清空状态的后续修复见 [审查记录](docs/36_Parent_Settings_and_Progress_Reset.md)。该记录保留了中途审批故障；恢复后 Debug 134 项、Release 125 项测试全部通过，APK 构建与 lint 通过，最终结果见 [工作日志](docs/WORKLOG.md)。这些验证不替代真实设备测试。
