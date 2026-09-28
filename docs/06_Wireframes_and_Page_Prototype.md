# Kids Language Companion 页面原型与线框设计说明

> Version: 1.0  
> Scope: V1 low-fidelity prototype  
> Target frame: Android Tablet / 16:10 / 2560×1600

---

# 1. 用途说明

本文件用于：

- 为 Figma 建立首版 Frame 结构；
- 为开发确认页面层级与组件位置；
- 为 AI Agent 生成高保真草图提供文字基线。

本文件偏“低保真结构说明”，重点是信息布局和流程，不替代最终视觉稿。

---

# 2. Figma 建议页面结构

```text
Pages/
├─ 00_Cover
├─ 01_Design_Tokens
├─ 02_Components
├─ 03_Flow_Child_Mode
├─ 04_Flow_Parent_Mode
├─ 05_Screens_LowFi
└─ 06_Prototype_Links
```

---

# 3. 设计 Token 建议

## 3.1 Frame 基准

- Device class: Tablet 16:10
- Reference Frame A: 1280 × 800
- Reference Frame B: 1600 × 1000
- Final export behavior: responsive to actual dp size

## 3.2 基础区域

- Top Bar: 72 ~ 96 dp
- Primary Content Area: remaining height
- Bottom Action Area: 120 ~ 180 dp（练习页）
- Side Mascot Area: 20% ~ 28% width（跟读页）

## 3.3 推荐色彩方向

- 背景：低饱和浅色
- 主品牌色：蓝 / 青 / 黄系
- 成功：明亮黄 + 星星
- 重试：淡紫 / 淡蓝，不使用攻击性红色

---

# 4. 页面列表（V1）

## S01 Splash / App Launch

用途：短暂品牌展示与资源初始化。

```text
┌────────────────────────────────────────────┐
│                                            │
│                App Logo                    │
│                Bunny Idle                  │
│                                            │
│            Loading... (optional)           │
│                                            │
└────────────────────────────────────────────┘
```

要求：
- 1 ~ 2 秒内完成；
- 不强制可交互；
- 可在首次加载 ASR 模型时显示轻量提示。

---

## S02 Home

用途：儿童模式主入口。

```text
┌────────────────────────────────────────────┐
│  [Settings/Parent]                         │
│                                            │
│   Bunny Idle      [ 日本語 ] [ 中文 ]      │
│                    [ English ]             │
│                                            │
│                 [ 开始 Start ]             │
│                                            │
└────────────────────────────────────────────┘
```

结构建议：
- 左侧：大兔子角色；
- 右侧：语言入口；
- 底部或中下：开始按钮；
- 所有按钮要大而稀疏。

---

## S03 Category Select

用途：选择场景，如“打招呼 / 幼儿园 / 一起玩 / 吃饭 / 厕所”。

```text
┌────────────────────────────────────────────┐
│ [Back]   日语 / Japanese                   │
│                                            │
│ [问候] [幼儿园] [一起玩] [吃饭]            │
│ [喝水] [厕所] [身体不舒服] [回家]          │
│                                            │
│                Bunny Idle                  │
└────────────────────────────────────────────┘
```

要求：
- 每个卡片包含图标/插图；
- 文字只是辅助；
- 可后续分页或横向滚动。

---

## S04 Practice Mode Select

用途：同一分类下选择练习模式。

```text
┌────────────────────────────────────────────┐
│ [Back]  幼儿园                             │
│                                            │
│   [ 听一听，说一说 ]   [ 听一听，选图片 ]   │
│                                            │
│               Bunny Praise                 │
└────────────────────────────────────────────┘
```

---

## S05 Listen & Repeat

用途：核心跟读页。

```text
┌──────────────────────────────────────────────────────────┐
│ [Back]  3 / 10                              [Replay]    │
│                                                          │
│  ┌───────────────────────────┐   ┌───────────────────┐  │
│  │         场景图片           │   │ Bunny Listen /   │  │
│  │                           │   │ Speak            │  │
│  │  (例：两个孩子一起玩)      │   │                  │  │
│  └───────────────────────────┘   └───────────────────┘  │
│                                                          │
│        いっしょに あそぼう   (可按家长设置隐藏)         │
│                                                          │
│                    [   BIG MIC   ]                       │
│                   [ waveform area ]                      │
└──────────────────────────────────────────────────────────┘
```

交互阶段：
1. 页面进入 → `Listen`
2. 自动播放标准音频
3. 角色切换 `Speak`
4. 自动开始录音或点击 Mic 开始
5. 成功 → 反馈页 / overlay
6. 失败 → `TryAgain` + Replay

---

## S06 Feedback Overlay - Success

```text
┌────────────────────────────────────────────┐
│                                            │
│              Bunny Great                   │
│                                            │
│                 太棒了！                   │
│              ★ ★ ★  星星动画               │
│                                            │
└────────────────────────────────────────────┘
```

要求：
- 0.8 ~ 1.5 秒后自动下一题；
- 可点任意处跳过。

---

## S07 Feedback Overlay - Retry

```text
┌────────────────────────────────────────────┐
│                                            │
│             Bunny TryAgain                 │
│                                            │
│                再试一次                    │
│                                            │
│           [ 再听一次 ] [ 我再说 ]          │
│                                            │
└────────────────────────────────────────────┘
```

要求：
- 不出现大红叉；
- 文案短；
- 两个按钮都足够大。

---

## S08 Listen & Choose

```text
┌──────────────────────────────────────────────────────────┐
│ [Back]  2 / 10                               [Replay]   │
│                                                          │
│  Bunny Listen                                            │
│                                                          │
│          请听一听，然后选正确的图片                      │
│                                                          │
│   [ Image A ]      [ Image B ]      [ Image C ]         │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

要求：
- 图片是主角；
- 角色缩小放角落；
- 点击即判定，不需要提交按钮。

---

## S09 Session Complete

```text
┌────────────────────────────────────────────┐
│                                            │
│             Bunny Praise / Great           │
│                                            │
│                你完成啦！                  │
│              今日完成 10 / 10             │
│                                            │
│        [ 再玩一次 ]   [ 回到首页 ]         │
│                                            │
└────────────────────────────────────────────┘
```

---

## S10 Parent Gate

用途：防止儿童误入家长设置。

候选方案：
- 长按角落 2 秒；
- 简单算数题；
- 按住并滑动手势。

---

## S11 Parent Mode / Settings

建议项：
- 当前语言
- 是否显示文本
- 自动下一题
- 每次练习题数
- 音效音量
- 角色动画强度
- 诊断信息
- 清除进度

---

# 5. Figma 组件建议

```text
Components/
├─ Buttons/
│  ├─ PrimaryLarge
│  ├─ SecondaryLarge
│  ├─ IconCircleSpeaker
│  └─ IconCircleMic
├─ Cards/
│  ├─ LanguageCard
│  ├─ CategoryCard
│  └─ OptionImageCard
├─ Mascot/
│  ├─ BunnyIdle
│  ├─ BunnyListen
│  ├─ BunnySpeak
│  ├─ BunnyGreat
│  ├─ BunnyTryAgain
│  └─ BunnyPraise
└─ Feedback/
   ├─ SuccessOverlay
   └─ RetryOverlay
```

---

# 6. Prototype Link 建议流程

```text
Home
 → Category Select
   → Practice Mode Select
     → Listen & Repeat
       → Success Overlay → Next Question
       → Retry Overlay → Repeat Question
     → Session Complete
```

并建立另一条：

```text
Home
 → Category Select
   → Practice Mode Select
     → Listen & Choose
       → Success Overlay / Retry Overlay
     → Session Complete
```

---

# 7. Agent / Figma 落图提示建议

如果交给 AI 生成高保真设计稿，可基于以下要求：

- 目标用户为不识字幼儿；
- 横屏平板 16:10；
- 画面明亮、留白充足；
- 使用白色兔子吉祥物作为统一陪伴角色；
- 使用大图卡 + 大按钮；
- 跟读页中角色位于右侧，题目图片位于左侧，Mic 居底部中央；
- 成功反馈使用星星和庆祝动画；
- 失败反馈温和，不使用惩罚性红色元素。
