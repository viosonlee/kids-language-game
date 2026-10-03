# 版本信息坏数据恢复与平板验收状态

> Author / Agent: Codex  
> Date: 2026-10-03  
> Status: Version reader validated; tablet acceptance pending

本记录增量补充 docs/53 版本清单错误恢复证据及 docs/55 平板检查，不修改既有文档。

## 版本信息

原 AppVersionInfo 对 JSON primitive 直接读取 content，导致 null、数字、布尔值或空白字段被显示成伪版本；空 models 列表也没有明确的未知提示。现在仅接受非空字符串作为版本和语言标签；无效内容清单显示“内容版本：未知”，无效或空模型清单显示“语音模型版本：未知”。各来源独立容错，包信息失败不会抹掉有效的内容/模型信息。

读取逻辑提取为组件实际调用的 readVersionInfo，仍在 Dispatchers.IO 执行。退出页面的协程取消向上传递，不吞作版本读取错误。

新增五项测试覆盖真实随包清单、坏 JSON 和错误字段类型、空或损坏模型列表、包信息/资产读取异常、协程取消。完整 test assembleDebug lintDebug --offline 通过，Debug 213 + Release 185 = 398 项零失败/错误。测试验证版本读取逻辑，不代替诊断页实机显示验收。

## 平板状态与剩余优先顺序

HUAWEI MRX-W09 仍可通过 ADB 连接，但 Keyguard showing=true。本轮没有再次发起安装。上一轮普通调试签名安装被拒绝；使用原签名重新打包后，两次安装返回 INSTALL_FAILED_ABORTED / User rejected permissions。没有卸载、清除数据或绕过锁屏，不能将现有安装认作最新源码版本。

已确认麦克风权限 granted=true，应用 files/models 目录存在；这只能说明权限/目录可用，不能证明模型加载和识别成功。已请求解锁及允许覆盖安装。

设备可用后优先：
1. 同签名覆盖安装、确认版本与数据保留。
2. 中英文模型就绪、实际发声识别及录音停止恢复。
3. 诊断页/练习页文字、按钮和滚动布局。
4. 结果弹窗全屏遮罩及兔子切换闪帧。
5. 离开页面、切后台、锁屏后录音和声音停止。

短时窗口完成这些仍不能代替 30 分钟稳定性、儿童观察、全部三语离线场景与识别准确率验收。整体审查目标保持未完成。
