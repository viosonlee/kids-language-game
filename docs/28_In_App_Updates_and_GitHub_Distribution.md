# 应用内升级与 GitHub 免费分发

> Author / Agent: Codex
>
> Date: 2026-09-29
>
> Status: Implemented; 首次公开发布与平板安装流程待验证
>
> Scope: 家长页升级、APK 校验、独立公开分发仓库、发布工具

本文基于既有离线学习与家长设置规范增量补充，自应用 1.0.1（versionCode 2）起提供手动更新。新增联网仅用于家长主动检查/下载更新，不上传录音、学习记录或设备标识。儿童练习不触发更新检查。

## 分发选择

用户选择独立公开 GitHub 仓库，仅分发安装包，并提供：

- 源码仓库：`git@github.com:viosonlee/kids-language-game.git`，不改变可见性。
- 分发仓库：<https://github.com/viosonlee/kid-game-release>，本轮匿名 API 已确认公开，尚无 Release。
- 调试版默认清单：`https://github.com/viosonlee/kid-game-release/releases/latest/download/update-debug.json`。

GitHub Releases 可免费承载公开安装包，设备无需 GitHub 账号。GitHub 在部分网络环境可能无法连接，应用会给出错误并允许重试；不承诺特定地区的下载速度。其他有免费额度的 HTTPS 对象存储也能使用同一清单协议，但需核对容量、请求量和流量政策。普通网盘分享页不能直接作为更新地址，必须提供无需登录的文件下载 URL。

当前 APK 含三语言离线模型，约 227 MiB，采用完整包下载；暂不实现增量包、后台自动下载、强制更新、断点续传。推荐有稳定网络时更新。

## 家长使用流程

1. 首次手动覆盖安装带更新功能的 1.0.1；旧 1.0.0 没有检查更新入口。
2. 首页通过家长验证，进入家长设置中的“应用更新”。
3. 点击“检查更新”，显示新版本、大小、说明；当前版本相同或更高时不提供降级。
4. 点击“下载更新”，显示进度，可取消。网络操作有连接/读取超时；取消在当前网络读取退出后清理临时文件。
5. 校验通过后点击“安装更新”。首次需在 Android 设置中允许本应用安装未知应用；返回后再次点击安装。
6. Android 系统安装器要求家长最终确认。取消安装可以再次点击；不自动在返回页面或后台拉起安装器。

覆盖安装使用相同包名、相同签名、更高 versionCode，保留应用数据；不要卸载旧版再装。正式版包名 `com.kidslanguage.companion`，当前平板调试版为 `com.kidslanguage.companion.debug`，两者不能互相覆盖。

页面提供“设置更新地址”和恢复默认值；更换分发服务时可填写新的 HTTPS JSON 地址。默认源在构建时也可用 `-PupdateRepository=owner/repository` 覆盖。

## 更新协议及校验

清单为 UTF-8 JSON，最多 64 KiB，允许未来新增字段；示意结构如下，实际大小和哈希必须由脚本生成：

```json
{
  "schemaVersion": 1,
  "packageName": "com.kidslanguage.companion.debug",
  "versionCode": 3,
  "versionName": "1.0.2",
  "minSdk": 29,
  "apkUrl": "https://github.com/viosonlee/kid-game-release/releases/download/v1.0.2-3/app-debug.apk",
  "bytes": 123456,
  "sha256": "由发布脚本计算的64位十六进制SHA256",
  "notes": "本次更新说明"
}
```

- 只接受 HTTPS，无 URL 用户名/密码和 fragment；逐次验证重定向，拒绝降级到 HTTP。
- 下载到应用私有 cache/updates；限制 512 MiB，预查剩余空间，并检查服务端长度、实际字节数与 SHA-256。
- 下载后读取 APK 的真实包名、versionCode、minSdk、当前签名证书集合。必须与清单/当前应用匹配且版本递增。当前严格匹配签名，不支持换密钥或签名轮换迁移。
- 安装前再次校验文件哈希与身份；系统安装器最终验证 APK 签名有效性。
- FileProvider 仅暴露 updates 缓存子目录，临时授予安装器读取权限，不申请外部存储权限。
- 家长导航页面持有任务，退出页面取消未完成工作；进程结束不会自动恢复下载或安装。缓存不参与 Android 数据备份。

## 首次手动发布（无需配置 CI 凭据）

本轮在 `build/update-release/` 准备 `app-debug.apk` 与 `update-debug.json`。文件不提交源码 Git 历史。

1. 打开 <https://github.com/viosonlee/kid-game-release/releases/new>。
2. 创建 tag `v1.0.1-2`，标题 `1.0.1`；该 tag 必须与清单内 APK URL 一致。
3. 同时上传以上两个文件，保持文件名不变。
4. 作为正式 Release 发布并设为 Latest；不要设为预发布或只保存草稿。
5. 匿名打开默认清单地址，确认 JSON 能下载；再下载安装包核对哈希。
6. 手动安装本轮 APK，家长页检查应显示最新；以后发布 versionCode 3 或更高版本验证完整下载/覆盖安装。

首次空仓库若 GitHub 要求目标分支，先通过网页添加一份简短 README 建立默认分支即可，无需上传源码。此仓库保持调试版渠道；正式版应另用一个分发仓库，以免 latest Release 缺少另一个渠道的清单。

## 后续本地打包

JDK 21、Android SDK 已配置后，使用与设备一致的密钥构建。以下 `$signingFile` 等是发布者自己的变量，不要将私钥或口令提交仓库：

```powershell
.\gradlew.bat test assembleDebug '-PappVersionCode=3' '-PappVersionName=1.0.2' `
  '-PupdateRepository=viosonlee/kid-game-release' `
  "-Pandroid.injected.signing.store.file=$signingFile" `
  "-Pandroid.injected.signing.store.password=$storePassword" `
  "-Pandroid.injected.signing.key.alias=$keyAlias" `
  "-Pandroid.injected.signing.key.password=$keyPassword"

python scripts/prepare_app_update.py `
  --apk app/build/outputs/apk/debug/app-debug.apk `
  --aapt "$buildTools/aapt.exe" --apksigner "$buildTools/apksigner.bat" `
  --repository viosonlee/kid-game-release --tag v1.0.2-3 --variant debug `
  --notes release-notes.txt
```

脚本调用 apksigner 验证签名，再由 aapt 读取实际 APK 元数据，计算大小/哈希；不信任可能过期的 Gradle metadata。生成的 APK 与 JSON 必须一起上传。每次发布使用更高 versionCode；不能只改变显示版本名。

## 可选 GitHub Actions 发布

源码仓库新增手动触发的 `.github/workflows/publish-update.yml`，普通 push 不发布 APK。手动选择目标仓库、debug/release、递增版本号和更新说明，流程先测试、签名构建、生成清单，再上传草稿并发布完整 Release。

需要发布者在源码仓库 Actions secrets 中自行配置：

| Secret | 内容 |
| --- | --- |
| `DISTRIBUTION_TOKEN` | 仅目标分发仓库 Contents 读写权限的 GitHub token |
| `UPDATE_SIGNING_KEY_BASE64` | 与目标已安装应用一致的签名 keystore 的 Base64 |
| `UPDATE_STORE_PASSWORD` | keystore 口令 |
| `UPDATE_KEY_ALIAS` | 密钥别名 |
| `UPDATE_KEY_PASSWORD` | 密钥口令 |

密钥只放受控 secrets 或本机，不放 APK、清单、公开仓库或源码。平板旧版使用本机已有 debug 密钥，不能使用 CI 每次自动生成的 debug key；否则无法覆盖升级。正式版也必须在第一次分发前选定并保管固定私钥。工作流结束清理 runner 的临时 keystore。

工作流检查匿名可读的公开目标、递增 versionCode 和渠道一致性；相同仓库发布串行。若上传草稿后失败，应由发布者检查并处理该草稿，不重复覆盖已经公开的版本。工作流不会替用户创建凭据或更改源码仓库可见性。

## 验证与边界

- 新增 8 项 Android/JVM 单元测试，覆盖清单、包名/版本/签名/系统约束、下载大小/哈希、取消及清单大小限制；新增 6 项 Python 发布工具测试。
- 完整 Gradle 单元测试通过（Debug/Release 各 76 项），包括上一轮未验证的 Room 真实数据库测试；Python 共 12 项通过。
- 本地 assembleDebug 成功；本机签名证书 SHA-256 与已知平板旧包一致：`876fb59b246fd418fc2fb98f54bcdbcea70909926b97fb13eb30b6d27786d6b5`。
- Windows JDK 曾因默认临时路径创建 Unix domain socket 失败；本轮在命令环境中指定现有实际目录 `-Djdk.net.unixdomain.tmpdir=...` 恢复构建，未将本机路径写入项目 Gradle 配置。
- Room 测试等待的线程栈显示 Robolectric 正在获取额外 Android SDK artifact，并非已证实的数据库死锁；依赖获取完成后测试通过。Gradle `--offline` 不会约束 Robolectric 自身的依赖下载。
- 尚未执行 GitHub 公开 Release 上传、Actions 发布、平板网络下载/系统安装器确认/覆盖安装保留数据的端到端验证。本轮不声称这些外部步骤已完成。
