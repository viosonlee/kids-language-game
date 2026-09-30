# SSH 自动发布与免费分发平台核查

> Author / Agent: Codex
>
> Date: 2026-09-30
>
> Status: 首次 GitHub Release 已公开，匿名更新入口已验证
>
> Scope: 补充 docs/28 的首次发布步骤，采用已有 SSH 和仓库自身的 Actions 权限自动上传 APK

## 已落地流程

Git 的 SSH 接口本身没有创建 Release 和上传附件的命令，但可以通过 SSH 推送触发 GitHub Actions。工作流使用分发仓库自身的临时 `GITHUB_TOKEN` 创建 Release，因此不需要用户登录 Codex 内置浏览器，也不需要另行提供个人访问 token。

本流程使用公开分发仓库 <https://github.com/viosonlee/kid-game-release>，不改变源码仓库可见性，不上传应用源码或签名私钥。

```text
本机测试、固定私钥签名构建 APK
→ prepare_app_update.py 生成更新清单
→ Git LFS 保存 APK，普通 Git 保存 JSON 与发布工作流
→ SSH push main
→ GitHub Actions 拉取 LFS 文件
→ 核对真实 APK 包名 / 版本 / minSdk / SHA-256 / 签名
→ 上传 Release 草稿的两个附件
→ 发布并设为 Latest
→ 应用家长页匿名读取 JSON、下载 APK
```

单个 APK 为 238,657,519 字节，超过普通 Git 文件的 100 MB 限制，所以使用 Git LFS 传入工作流。用户从 Releases 下载，不从 LFS 下载。工作流不重新构建或重新签名安装包，保留已知平板旧版签名兼容性。

分发仓库文件：

- `artifacts/app-debug.apk`：Git LFS 管理的已签名 APK。
- `artifacts/update-debug.json`：更新清单。
- `signer-sha256.txt`：可信签名证书的公开指纹，不是私钥。
- `.github/scripts/verify_release.py`：二进制验证。
- `.github/workflows/publish-prebuilt.yml`：校验后发布，只在相关输入变更或手动触发时运行。

源码仓库保留后两项的模板于 `scripts/distribution/`。已有公开 tag 不会被重跑覆盖；新的 versionCode 必须大于已公开版本。未完成的草稿可以重试附件上传后再发布，避免 latest 指向缺文件的版本。

## 首次发布的证据

- Release：<https://github.com/viosonlee/kid-game-release/releases/tag/v1.0.1-2>。
- 成功的工作流：<https://github.com/viosonlee/kid-game-release/actions/runs/36706776961>。
- 最新 APK：<https://github.com/viosonlee/kid-game-release/releases/latest/download/app-debug.apk>。
- 应用内更新源：<https://github.com/viosonlee/kid-game-release/releases/latest/download/update-debug.json>。
- 版本：1.0.1 / versionCode 2；包名：`com.kidslanguage.companion.debug`。
- APK SHA-256：`00fb6f497514795bab96611e75dfae3a508f70054504181e55936c9a040524a6`。
- GitHub Release API 显示非草稿且具有 APK、JSON 两个附件；服务器计算的 APK digest 与本机验证值一致。
- 匿名请求 latest JSON，与本地清单逐字段一致；匿名 APK 下载响应为 200、长度正确、文件开头为 APK/ZIP 标识。
- 前两次运行在 SDK 工具证书输出解析处失败，未发布不完整版本；修正为提取证书指纹集合后成功，仍要求 apksigner 签名验证通过且所有证书均匹配固定指纹。
- 新增六项分发校验测试，总计 18 项 Python 测试通过；本轮 assembleDebug 成功，应用代码未变更。

首次安装 1.0.1 仍需手动覆盖安装，因为旧版没有更新功能。公开分发链路已验证；真实平板安装器授权、覆盖安装保留数据以及从 versionCode 2 升级到更高版本仍需设备验证。

## 下一次发布

1. 在源码仓库按 docs/28 的命令增加 versionCode/versionName，使用固定私钥构建并测试。
2. 运行 `scripts/prepare_app_update.py`，目标仓库仍为 `viosonlee/kid-game-release`，tag 与新版本一致。
3. 在已有分发仓库 checkout 中替换两个产物：

```powershell
Copy-Item build/update-release/app-debug.apk build/distribution-repository/artifacts/app-debug.apk
Copy-Item build/update-release/update-debug.json build/distribution-repository/artifacts/update-debug.json
git -C build/distribution-repository add artifacts
git -C build/distribution-repository commit -m 'release: publish version 1.0.2'
git -C build/distribution-repository push origin main
```

首次在另一台电脑操作时安装 Git LFS，并通过 SSH 克隆分发仓库。推送前须确认两个文件属于同一次签名构建，不能只改 JSON 版本号。之后在 Actions 查看结果；成功后再用匿名 latest 地址核对清单和附件。

docs/28 中“源码仓库构建并使用跨仓库 token 发布”的流程仍可作为另一种方案；本次实际采用上述本机签名、分发仓库自动发布方案，未配置这些额外 secrets。

## 免费平台核查（2026-09-30）

| 方案 | 官方免费说明 | 对本应用的适用性 |
| --- | --- | --- |
| GitHub Releases + LFS + Actions | GitHub Free 的 LFS 包含 10 GiB 存储和 10 GiB 下载带宽；额度由账户共享 | 已跑通。LFS 只用于发布输入，应用更新走 Releases。历史二进制版本仍占 LFS 存储，不承诺无限免费存储 |
| Cloudflare R2 Standard | 每月 10 GB-month 存储、100 万 Class A 和 1,000 万 Class B 操作免费；互联网出站流量不收费 | 可放 APK 和同协议 JSON，适合作为 HTTPS 更新源；需开通账户及公开下载地址，超过免费存储/请求额度按价计费 |
| 蒲公英 PGYER | 当前官方分发页声明分发功能永久免费、无付费套餐和试用期，并提供中国/海外 CDN、上传与版本 API | 可作为国内分发候选；需要账户/API Key，需验证账户下的发布条件和 APK 下载接口；不能直接把网页分享地址当作当前 JSON 更新源 |

官方依据：

- GitHub LFS：<https://docs.github.com/en/billing/concepts/product-billing/git-lfs>。
- Cloudflare R2：<https://developers.cloudflare.com/r2/pricing/>。
- 蒲公英当前分发说明：<https://www.pgyer.com/app/distribution#price>。
- 蒲公英 API 鉴权说明：<https://www.pgyer.com/doc/view/api>。

本轮没有创建其他平台账户、接入新的计费项目或购买额度，也未读取当前 GitHub 账户剩余 LFS 额度。现有 GitHub 分发已可用；其他平台仅完成官方资料核查，未上传应用或进行终端下载测试。
