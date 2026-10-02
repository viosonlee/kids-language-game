# 家长设置保存与进度清空审查

> Author / Agent: Codex
>
> Date: 2026-10-02
>
> Status: 代码与测试已写入工作区；因自动审批服务故障，本批运行验证与提交未完成
>
> Scope: FR-PARENT-001/002/003、FR-PROGRESS-001/003 和 docs/33 的进度重置/门禁界面证据缺口

## 源码发现与本批修改

家长页各设置按钮原先直接在 viewModelScope 中写 DataStore，没有处理普通写入异常。新增统一的保存入口：写入失败保持持久化状态不变，显示“设置未能保存，请检查设备存储空间后重试”；后续成功保存清除提示；协程取消仍向上传播。

进度清空涉及两个存储：Room 的练习事件/逐题统计，以及 DataStore 的旧版本 completed_count。此前只有运行中布尔值与一条通用错误消息。改为 Idle、Running、Complete、Failed 四种互斥状态，运行中禁止重复请求；成功显示“清空操作已完成”，错误提供具体状态。如果 Room 已清空而旧版本累计写入失败，明确提示“练习明细已清空，但旧版本累计数未能清空，请重试”。取消不产生成功消息。

`ProgressResetControl` 复用原来的二次确认内容，显示运行/完成/失败状态。取消确认不调用清空；重试仍要求再次确认清空全部记录，未加入自动重试。家长页统计继续由仓库 Flow 驱动，没有通过先把界面数字改成零来掩盖存储结果。

没有改动 Room schema 或把两个存储宣称为跨存储原子事务。两者之间进程退出仍可能留下部分完成状态；家长再次确认执行的是一次新的全量清空，包括期间新增的记录。本批只让当前操作的完成/部分失败可见，没有静默回滚或恢复已经确认删除的数据。

## 增加的验证代码与各自范围

| 测试文件 | 新覆盖的情形 | 证据限制 |
| --- | --- | --- |
| ParentViewModelTest | 一次确认只执行一次；保留全部学习偏好；数据库清空失败不改旧累计；旧累计失败显示部分完成；取消；设置保存失败与重试 | 使用受控仓库和 DataStore，不是真实设备存储故障 |
| ProgressRepositoryTest | 清空等待较早写入、保留较晚写入；取消排队清空；离开页面不取消应用级写入；失败保存警告直到成功清空才消失 | 验证真实仓库的 Mutex/协程逻辑，DAO 为受控替身 |
| RoomProgressDatabaseTest | 给真实临时 Room 数据库第二步 DELETE 注入 SQL 触发器错误，检查两张表均回滚，再移除触发器重试 | 只操作测试数据库，不操作用户练习数据 |
| ParentControlsTest | 实际 Compose 清空确认/取消、运行中禁用、完成与错误提示、重试确认；家长门禁拒绝错误答案后接受当前答案 | Debug/Robolectric API 29；未覆盖完整 Home→家长页导航或真实输入法 |

现有 RoomProgressRepository 按调用顺序排队写入/清空，Room 的两张表已有 @Transaction。本批对这些行为补证据，不因测试尚未运行就认定它们通过，也没有重写已存在的排队逻辑。

## 当前验证状态

静态检查 `git diff --check` 通过，旧 resetError/resetting 的家长页调用已替换。新增及扩展测试尚未执行，不能把上一提交 Debug 122/Release 116 的结果归于当前工作区。

请求运行定向 Gradle 测试时，工具自动审批服务返回 HTTP 404：其配置的 `gpt-5.6-luna` 模型不受支持。审批未完成，命令没有执行；这不是测试失败或安全审查判定命令危险。工作区外的 JDK/SDK 访问需要可用审批，未尝试绕过该边界。

恢复工具后需执行：

```powershell
.\gradlew.bat testDebugUnitTest --offline --tests com.kidslanguage.companion.ParentViewModelTest --tests com.kidslanguage.companion.ProgressRepositoryTest --tests com.kidslanguage.companion.RoomProgressDatabaseTest --tests com.kidslanguage.companion.ParentControlsTest
.\gradlew.bat test assembleDebug lintDebug --offline
```

随后核对实际报告、补记最终结果，再提交推送。按 AGENT.md 的提交前验证要求，本批目前没有提交；旧规范保留，WORKLOG 只追加。

## 仍需继续的范围

本批未覆盖 Settings Flow/Room Flow 的读取失败恢复、首页语言保存异常、跨存储进程中断的恢复协议、完整应用 Compose 导航、设备音频/布局/长期运行和儿童观察。ASR 参数/超时优化及手机布局仍按原来的调查/评估范围保留。原始总目标不因本批代码存在或测试源码增加而认定完成。
