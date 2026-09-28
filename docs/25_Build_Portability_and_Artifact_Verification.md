# 25. 构建可移植性、模型校验与说明修正

> Author / Agent: Codex
> Date: 2026-09-28
> Status: Locally verified; remote CI pending push
> Scope: docs/14 CI 基线增量修正

- 原 gradle.properties 包含本机 Windows JDK 绝对路径，Linux CI 无法使用；删除仓库级路径，改由 JAVA_HOME / Android Studio Gradle JDK 配置。本机验证仅为该命令设置 JAVA_HOME=JDK21，未修改用户系统环境。
- CI 使用只读 contents 权限、Gradle setup v4，一次执行 test / assembleDebug / assembleDebugAndroidTest；即使失败也归档可用测试报告。保留 Debug APK 14 天制品。
- BundledModelsTest 新增三语言清单、字节大小和流式 SHA-256 校验；原测试仅确认 ZIP 中存在核心文件，不能证明下载内容与 manifest 一致。
- 重写根 README 的当前实现和构建说明，移除无法证明的 COPPA 合规、全部路径绝不上网、低内存无压力等声明；明确系统识别回退及识别效果验证限制，纠正包体仅含日语、题数选择、架构/目录等过时说明。
- 本机 test assembleDebug assembleDebugAndroidTest --offline 成功，单元测试 Debug/Release 各 66 项。远端 CI 尚未触发，不声明 Linux 构建验证完成。
- 旧 setup_vosk_models.py 仍存在按文件大小跳过校验、任意非空目录视为完整、多设备默认首台、固定 release 包路径、失败后仍输出成功等问题，后续需修复并测试。
- 背景音乐、设备验证、语音优化决策及总目标完成审查仍待处理，保留所有原始规范。
