# 练习图片离线打包与语义审查

> Author / Agent: Codex  
> Date: 2026-10-03  
> Status: 位图兼容性修复完成；场景语义未验收  
> Scope: 对 docs/03、docs/10、docs/33、docs/34 的图片显示与内容审查增量补充。

## 问题与边界

126 道题的主图和干扰项原先全部是 Emoji 字符，显示效果取决于系统字体。滑梯、泡泡等较新字符不保证在旧设备字体中存在，组合人物也可能分拆显示。此前字段合法、图片非空的测试不能证明旧系统一定绘制出这些符号。

此外，许多符号只能提供联想，不能独立表达社交动作。例如彩虹表示“明天见”、男孩头像表示“轮到我”、足球表示“让我加入”、数字表示“轮流玩”；上衣图标也不准确对应英语 coat。这些语义问题本批没有假装通过，也没有擅自修改教学文字或音频。

## 实施

将主图及全部干扰项共 99 个不同符号替换为 APK 内的 512×512 PNG，使用已有 PracticeIllustration 的异步本地位图分支。126 条题目的 ID、文字、读音、翻译、音频、别名和玩法均保持原值，干扰项顺序及对应符号不变。内容版本升为 1.0.4。

图片来自 Google Noto Emoji v2.047，固定提交 `22e564626297b4df0a40570ad81d6c05cc7c38bd`，不用 OpenAI API。96 个单符号保留源 PNG 字节，三个多符号组合按原左右顺序缩放并合成透明 PNG；总图片大小 2,224,938 字节。数据加载和运行时不增加网络依赖。

来源：<https://github.com/googlefonts/noto-emoji/tree/22e564626297b4df0a40570ad81d6c05cc7c38bd/png/512>。该提交 README 声明图像资源为 Apache 2.0，svg/LICENSE 保留 Google 版权声明；本次没有嵌入字体或地区旗帜。完整 Apache 2.0 许可、上游图像声明、原 README、来源和组合修改说明随 APK 放在 `assets/images/practice/`。

`images/practice/manifest.json` 记录符号、资产路径、源文件、源/输出 SHA-256、是否组合修改和固定版本。`scripts/bundle_practice_icons.py` 可重建这批图片，需要 Pillow；全部下载与像素解码完成后才开始写题库，遇到不支持的图片路径或源下载失败停止。它是维护工具，不在应用启动或 CI 默认流程中联网运行。

## 验证

- 99 个源图片全部通过 Pillow 完整解码，单图尺寸检查为 512×512；组合图以透明画布生成。
- 已查看全部 99 个图片的联系表 `build/visual-review/practice-icons.png`，包括滑梯、泡泡、人物和三个组合符号。该表验证实际像素和组合次序，不是幼儿语义验证。
- 对照 Git 中的上一版本，自动确认 126 条题目仅图片引用变化，原主图/干扰符号可由映射完全还原。
- 新增离线测试检查全部主图与干扰项均存在于图片清单、无多余清单项、PNG 签名/尺寸、内容哈希及许可文件。现有 CI 的 Python unittest discovery 自动包含该测试。
- Python 26 项通过。Gradle `test assembleDebug lintDebug --offline` 成功，Debug 175 / Release 150 项无失败或错误；lint 保持 0 errors、13 warnings、2 information。
- 实际 APK 内 99 个 PNG 的 SHA-256 均与清单一致，许可、NOTICE、清单及内容版本也逐项核对。

本次证明练习符号已经是随包位图、无需 Emoji 字体才能显示；没有声称完成所有真实 Android 10 设备、图片解码性能或字体兼容性测试。首页旗帜、分类和其他装饰 Emoji 仍使用系统渲染，不属于本批练习图片范围。

## 尚待完成的内容审查

新增 `docs/assets/47_Practice_Pictogram_Review.json`，列出全部 126 个目标词、原符号、现资产路径及语义审查意见，全部保持 semanticAcceptance=notValidated。该表没有把简单物品图直接认定为儿童验收通过。

下一步的高价值素材应是明确表达动作和对象关系的场景插画，例如接过玩具后道谢、两人排队轮流、指着肚子求助。此次固定 Emoji 位图解决显示依赖，不能代替这些插画或真实儿童观察。兔子眨眼/挥手/开口动画仍是独立未完成项，等待素材制作方式落实。本批未安装或发布 APK，总审查目标继续。
