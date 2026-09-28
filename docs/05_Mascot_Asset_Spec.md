# Kids Language Companion 吉祥物资源规格书

> Version: 1.0  
> Character: Momo Bunny（暂定名）  
> Related: `04_Mascot_Design.md`

---

# 1. 目的

本文件定义兔子吉祥物在开发落地阶段的资源拆分、命名规则、导出格式、使用场景与动画建议，便于开发与设计协作。

---

# 2. 资源目录

```text
assets/mascot/
├─ bunny_mascot_expression_sheet.png
├─ manifest.json
├─ png/
│  ├─ bunny_idle.png
│  ├─ bunny_listen.png
│  ├─ bunny_speak.png
│  ├─ bunny_great.png
│  ├─ bunny_try_again.png
│  └─ bunny_praise.png
├─ webp/
│  ├─ bunny_idle.webp
│  ├─ bunny_listen.webp
│  ├─ bunny_speak.webp
│  ├─ bunny_great.webp
│  ├─ bunny_try_again.webp
│  └─ bunny_praise.webp
└─ preview/
   ├─ bunny_idle.png
   ├─ bunny_listen.png
   ├─ bunny_speak.png
   ├─ bunny_great.png
   ├─ bunny_try_again.png
   └─ bunny_praise.png
```

说明：
- `bunny_mascot_expression_sheet.png`：设计确认用总览图；
- `png/`：无损透明资源，适合继续编辑；
- `webp/`：推荐运行时使用；
- `preview/`：统一放大并居中的预览图，方便在文档/Figma 中快速浏览；
- `manifest.json`：资源清单与状态映射。

---

# 3. 状态清单

| 状态 | 文件名 | 典型用途 |
|---|---|---|
| Idle | `bunny_idle` | 首页待机、会话开始前 |
| Listen | `bunny_listen` | 系统播放标准语音、正在“听” |
| Speak | `bunny_speak` | 轮到孩子开口、录音前后 |
| Great | `bunny_great` | 明确答对、庆祝反馈 |
| TryAgain | `bunny_try_again` | 识别失败、温和鼓励重试 |
| Praise | `bunny_praise` | 会话完成、多次尝试后的鼓励通过 |

---

# 4. 导出与运行时建议

## 4.1 开发推荐

- 设计源保留 PNG；
- App 运行时优先加载 WebP；
- Compose 中统一经 `Painter` / `ImageBitmap` 使用；
- 图片不应被非等比拉伸；
- 使用 `contentScale = ContentScale.Fit`。

## 4.2 推荐显示尺寸

在 10.8 英寸 2560×1600 平板上：

- 首页主角色：280 ~ 420 dp 高；
- 练习页右侧角色：180 ~ 280 dp 高；
- 成功反馈角色：240 ~ 360 dp 高；
- 角落小角色（选择题页）：120 ~ 180 dp 高。

## 4.3 可接受动画方式

- scale in/out
- translateY（轻微跳动/浮动）
- alpha fade
- rotate（极小幅度）
- 粒子叠加（星星/闪光）

不建议：
- 高频闪烁
- 长时间震动
- 过强失败动画

---

# 5. 建议的状态到页面映射

| 页面/时机 | 角色状态 |
|---|---|
| Home | Idle |
| Category Select | Idle / Praise |
| Prompt Playing | Listen |
| Waiting for Child Speech | Speak |
| Recording | Speak |
| Evaluating | Listen |
| Correct Answer | Great |
| Retry Needed | TryAgain |
| Session Completed | Praise / Great |

---

# 6. 后续设计增强建议

如需进入正式设计稿或 Figma，建议补充：

1. 单独的星星特效素材；
2. 单独的汗滴/提示线特效素材；
3. 深色背景/浅色背景下的描边策略；
4. 角色名字与品牌故事；
5. 面向动画实现的关键帧草案。
