# 26. 模型维护脚本可靠性修复

> Author / Agent: Codex
> Date: 2026-09-28
> Status: Local tests passed; device deployment verification pending
> Scope: 对 docs/14 脚本基线的增量替代

## 行为

- 模型来源、文件名、大小与 SHA-256 统一读取应用 models/manifest.json，优先复用内置 ZIP；下载采用临时文件，校验成功才发布。
- 解压到独立临时目录，拒绝目录穿越、绝对路径、Windows 驱动器/反斜杠与符号链接条目，校验六个模型核心文件后再发布。旧目录保留为带唯一后缀的 previous 备份，发布失败尝试还原。
- 本地缓存匹配来源 SHA 标记及核心文件完整性才可复用；--push-only 同样验证准备状态。核心文件非空检查不等价于对已解压模型每字节重新验证。
- 部署默认 com.kidslanguage.companion.debug，可用 --package 指定 release 包；检查已安装包，多设备必须 --device，--adb 支持工具路径。
- 设备先推送到 staging 并逐个核对六个核心文件 SHA，再发布；保留 previous 备份。应在退出应用后部署，完成后重新打开以加载；不在已运行的原生识别器上热替换。
- 未授权/离线设备、下载/校验/解压/ADB 失败返回非零，不能再输出“全部成功”。--download-only 与 --push-only 互斥；--lang zh 和 cn 均指中文。

## 使用

Python 3.11+（CI 使用 3.13）。普通安装 APK 已内置模型，不需要该脚本。

```sh
python scripts/setup_vosk_models.py --download-only
python scripts/setup_vosk_models.py --push-only --device SERIAL --package com.kidslanguage.companion.debug
python -m unittest discover -s scripts -p test_setup_vosk_models.py
```

/models 与 Python 缓存忽略提交；历史备份不会自动删除，确认无需恢复后由维护者清理。

## 验证

六项 Python 测试覆盖校验失败、路径穿越、不完整包不覆盖已有目录、完整发布与备份、设备选择和失败退出码。使用三份真实内置模型完成 --download-only 成功。CI 已接入脚本单元测试。设备阶段尚未实测，不声明部署验证完成。
