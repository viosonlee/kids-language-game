# Kids Language Companion 高保真 Figma 设计规格书

> Version: 1.0  
> Scope: Child Mode + Parent Mode / Android Tablet  
> Target Device: Android 10, 10.8-inch, 2560×1600, 16:10  
> Reference Frame: 1600×1000 px for Figma composition, implementation remains dp-based  
> Related: `03_UI_UX_Design.md`, `04_Mascot_Design.md`, `05_Mascot_Asset_Spec.md`, `06_Wireframes_and_Page_Prototype.md`

---

# 1. 设计目标

本设计稿应呈现为一个面向幼儿的“语言陪伴游戏”，而不是传统学习软件。

高保真视觉目标：

- 第一眼即可感知“可爱、安全、愿意玩”；
- 不识字也能理解主流程；
- 兔子吉祥物是主要陪伴者，但不抢夺练习内容本身；
- 页面视觉节奏轻快，反馈明确；
- 日语、中文、英语三种语言共用同一套 UI 结构；
- 高饱和色仅用于关键动作与成功反馈，背景保持柔和；
- 避免复杂阴影、过度拟物、过度动画。

---

# 2. Figma 文件结构

建议 Figma 文件使用以下 Page：

```text
00_Cover
01_Foundations
02_Components
03_Mascot
04_Child_Flow
05_Parent_Flow
06_Prototype
07_Dev_Handoff
```

## 2.1 00_Cover

包含：

- App 暂定名称：Kids Language Companion
- 核心描述：Listen · Speak · Understand
- Bunny Mascot 主视觉
- 目标设备信息
- 文档版本

---

# 3. Design Foundations

## 3.1 Color System

### Primary

| Token | Value | 用途 |
|---|---:|---|
| `Primary/500` | `#4DA3FF` | 主按钮、主交互 |
| `Primary/600` | `#338DEB` | Pressed / Strong |
| `Primary/100` | `#DCEEFF` | 浅背景块 |

### Accent

| Token | Value | 用途 |
|---|---:|---|
| `Accent/Yellow` | `#FFC94A` | 星星、奖励、强调 |
| `Accent/Orange` | `#FF9E42` | 兔子背包 / 次级亮点 |
| `Accent/Green` | `#66C56A` | 正向选择、完成 |
| `Accent/Purple` | `#A98AF8` | Try Again / 次交互 |
| `Accent/Pink` | `#FF88B7` | 情绪性装饰 |

### Neutral

| Token | Value | 用途 |
|---|---:|---|
| `Neutral/0` | `#FFFFFF` | 卡片背景 |
| `Neutral/50` | `#F8FAFC` | 页面背景 |
| `Neutral/100` | `#F0F4F8` | 区块背景 |
| `Neutral/300` | `#C9D3DD` | 边界 / Disabled |
| `Neutral/700` | `#425466` | 次级文字 |
| `Neutral/900` | `#1E293B` | 主要文字 |

### Feedback

| Token | Value | 用途 |
|---|---:|---|
| `Success/Bg` | `#FFF6CE` | 成功 Overlay |
| `Retry/Bg` | `#F2EDFF` | 重试 Overlay |
| `Info/Bg` | `#EAF6FF` | 提示区域 |

原则：失败场景不使用纯红色背景与大红叉。

---

## 3.2 Background

Child Mode 推荐：

- 主背景：`#F8FAFC`
- 可使用非常轻微的云朵 / 圆点 / 弧形装饰；
- 装饰透明度 5%~12%；
- 不允许背景复杂到影响题图识别。

Parent Mode 推荐：

- 更偏 Material 风格；
- 使用更克制的灰白背景；
- 降低卡通装饰。

---

# 4. Typography

不绑定需额外分发的字体文件，优先使用 Android 系统可用字体与 Noto 系列回退。

建议栈：

```text
Japanese: Noto Sans JP / sans-serif
Chinese: Noto Sans SC / sans-serif
English: Roboto / sans-serif
```

## 4.1 Child Mode Type Scale

| Token | Size | Weight | 用途 |
|---|---:|---:|---|
| `Display/Large` | 44 sp | 700 | 完成页大标题 |
| `Title/Large` | 34 sp | 700 | 页面主标题 |
| `Title/Medium` | 28 sp | 700 | 卡片标题 |
| `Body/Large` | 26 sp | 600 | 目标句子 |
| `Body/Medium` | 22 sp | 500 | 简短说明 |
| `Label/Large` | 22 sp | 700 | 大按钮 |
| `Label/Medium` | 18 sp | 600 | 次级按钮 |

原则：

- Child Mode 不使用 16sp 以下功能性文字；
- 按钮文字最多两行；
- 文本不是完成任务的唯一信息源。

---

# 5. Spacing / Radius / Elevation

## 5.1 Spacing

基础间距 Token：

```text
4 / 8 / 12 / 16 / 24 / 32 / 40 / 48 / 64 dp
```

页面级主要间距：

- 主区块之间：32~48 dp
- 卡片间距：24 dp
- 主按钮与正文：32 dp
- 边缘安全区：32~48 dp

## 5.2 Radius

| Component | Radius |
|---|---:|
| Primary Button | 28 dp |
| Large Card | 32 dp |
| Image Option Card | 28 dp |
| Dialog | 32 dp |
| Icon Button | 50% / Circle |

## 5.3 Shadow / Elevation

- 大卡片：2~4 dp
- Primary CTA：4~6 dp
- Overlay：8~12 dp

原则：阴影轻柔，避免“游戏商城”风格。

---

# 6. Layout Grid

Reference Frame: 1600×1000。

建议：

- 页面外边距：48 px（Figma Reference）
- 12-column grid
- Gutter: 24 px
- 顶部工具区：96 px
- 底部交互区：160~200 px

Compose 实现时应转换为响应式 dp 布局，不直接复制 px。

---

# 7. Mascot Component

## 7.1 Variants

创建 Figma Component Set：`Mascot/Bunny`

Variants：

```text
State = Idle | Listen | Speak | Great | TryAgain | Praise
Size = Small | Medium | Large
```

推荐 Size：

- Small: 160×160
- Medium: 260×260
- Large: 360×360

## 7.2 Motion Notes

Figma Prototype 中可模拟：

- Idle: 轻微浮动
- Listen: 左右 2~3° 摆动
- Speak: 102% → 100% 呼吸式缩放
- Great: Y -40 + scale 110% + stars
- TryAgain: Y +12 + 轻微左右摆动
- Praise: 105% pop-in

---

# 8. Component Library

## 8.1 Button/PrimaryLarge

尺寸：

- Height: 96 dp
- Min width: 280 dp
- Horizontal padding: 40 dp
- Radius: 28 dp

States：

- Default
- Pressed
- Disabled
- Loading

## 8.2 Button/IconCircle

用于 Speaker / Mic / Back。

- Small: 72×72 dp
- Large Mic: 140~168 dp
- Touch target 不小于 72 dp

Mic 推荐视觉：

- 主色圆形底
- 中央白色 Mic icon
- Recording 时外环脉冲

## 8.3 Card/Language

- 360×180 dp 推荐
- Icon / Flag-like visual + language name
- 大圆角
- Selected 状态增加边框 + 轻微 scale

## 8.4 Card/Category

- 300×220 dp
- 图片 / 场景插图占上部 65%
- 标题占下部 35%

## 8.5 Card/OptionImage

- 360×260 dp
- 图片全幅
- Selected 正确：Green ring + check sparkle
- Selected 错误：Purple soft ring，不显示大红叉

---

# 9. Screen Specifications

## S01 Splash

### Layout

- Center: Bunny Idle Large
- Bottom Center: App wordmark
- Background: soft neutral + subtle dots

### Animation

- Bunny scale 0.96 → 1.0 / 300ms
- Wordmark fade in / 250ms

---

## S02 Home

### Composition

```text
┌────────────────────────────────────────────────────────────┐
│ Parent Gate                                         Sound │
│                                                            │
│   [ Bunny Idle Large ]      今天想学什么？                  │
│                             [ 日本語 ]                      │
│                             [ 中文 ]                        │
│                             [ English ]                     │
│                                                            │
│                             [   开始   ]                    │
└────────────────────────────────────────────────────────────┘
```

### Layout Ratio

- Left mascot area: 42%
- Right interaction area: 58%

### Notes

- 语言卡只保留一个主视觉 + 名称；
- 默认最近使用语言可预选；
- CTA 永远位于固定位置。

---

## S03 Category Select

### Composition

Top:

- Back
- Language label
- Optional small Bunny Praise

Body:

- 2×4 category cards
- 每卡大场景图 + 简短标题

推荐场景：

- 问候
- 幼儿园
- 一起玩
- 吃饭
- 喝水
- 厕所
- 不舒服
- 回家

---

## S04 Practice Mode Select

两个主卡：

### Card A: Listen & Repeat

视觉：

- Speaker icon → Bunny Speak → Mic

### Card B: Listen & Choose

视觉：

- Speaker icon → 3 images → tap

Child Mode 中，图示比文字更重要。

---

## S05 Listen & Repeat

### High-fidelity Layout

```text
┌────────────────────────────────────────────────────────────┐
│ ←          3 / 10                              🔊 Replay    │
│                                                            │
│ ┌────────────────────────────┐  ┌────────────────────────┐ │
│ │                            │  │                        │ │
│ │       Scene Image          │  │    Bunny Listen       │ │
│ │                            │  │                        │ │
│ └────────────────────────────┘  └────────────────────────┘ │
│                                                            │
│              いっしょに あそぼう                           │
│                                                            │
│                   [      MIC      ]                        │
│                   ~ waveform ~                             │
└────────────────────────────────────────────────────────────┘
```

### Sizing

- Image area: 760×480 reference
- Mascot area: 360×420 reference
- Mic: 160 dp
- Sentence: 26~30 sp

### State variants

1. PromptPlaying → Bunny Listen
2. ReadyToSpeak → Bunny Speak
3. Recording → Bunny Speak + Mic Pulse
4. Evaluating → Bunny Listen + subtle loading dots
5. Passed → Success Overlay
6. Retry → Retry Overlay

---

## S06 Success Overlay

### Full-screen Feedback

- Background: `Success/Bg`
- Center: Bunny Great Large
- Stars: 5~8 particles
- Text: “太棒了！” / “できた！” / “Great!” based on UI language

Motion:

- Bunny: jump 320ms
- Stars: burst 450ms
- Hold: 600~900ms
- Exit: 200ms

总时长建议 < 1.5s。

---

## S07 Retry Overlay

- Background: `Retry/Bg`
- Center: Bunny TryAgain Medium/Large
- Text: “再试一次”
- CTA A: 再听一次
- CTA B: 我再说

不使用错误图标。

---

## S08 Listen & Choose

### Layout

- Top: Back / Progress / Replay
- Small Bunny Listen in top-right
- Center: 3 option cards
- No submit button

选中后立即反馈。

---

## S09 Session Complete

- Bunny Praise / Great Large
- 3 stars / stickers
- Completed count
- CTA: 再玩一次
- Secondary: 回到首页

不显示分数百分比。

---

## S10 Parent Gate

建议高保真设计：

- 长按家长图标 2 秒后出现半透明 Dialog
- Dialog 中出现 3 个大数字卡
- 指示“请选择 3 + 2 的答案”

仅作为儿童误触防护，不作为真正安全认证。

---

## S11 Parent Settings

视觉风格可以更“成人化”：

- Sectioned Settings
- Switch / Slider / Dropdown
- Less mascot usage

Sections：

1. Language
2. Practice
3. Audio & Animation
4. Text Visibility
5. Progress
6. Diagnostics

---

# 10. Responsive Rules

虽然目标设备固定为 10.8 英寸，仍应保持响应式：

- Width < 1000dp：减小左右分栏比例，Mascot 可缩小；
- Width >= 1200dp：增加内容最大宽度，不无限拉伸卡片；
- Height < 700dp：减少顶部留白；
- 不依赖绝对位置；
- 练习页始终保证 Mic 在视觉中心偏下位置。

---

# 11. Accessibility / Preschool Usability

- 关键图标配合声音反馈；
- 主要触摸区域 >= 72dp；
- 不依赖红/绿颜色单独传递结果；
- 不使用长文本；
- 音效与动画可关闭；
- 失败不可造成阻塞；
- 页面层级最多 3 层进入练习。

---

# 12. Figma Prototype Flow

```text
Splash
 → Home
 → Category Select
 → Practice Mode
 → Listen & Repeat
   → Success → Next
   → Retry → Repeat
 → Session Complete
```

以及：

```text
Home
 → Category Select
 → Practice Mode
 → Listen & Choose
   → Success/Retry
 → Session Complete
```

---

# 13. Dev Handoff Checklist

Figma Dev Handoff 必须明确：

- 所有颜色 Token 名称；
- Typography Token；
- Component variants；
- Mascot state 名称；
- 每个页面的 state diagram；
- 最小点击区域；
- Overlay animation timing；
- 图片裁切规则；
- Asset 文件名；
- Android 对应资源路径。

---

# 14. 高保真设计完成标准

设计稿进入开发前应至少包含：

- 11 个主屏幕；
- 6 个吉祥物状态；
- 2 种反馈 Overlay；
- Button / Card / Icon 基础组件；
- 全套颜色与 Typography Token；
- Listen & Repeat 全状态 Variant；
- Listen & Choose 全状态 Variant；
- 一条完整 Child Mode Prototype 流；
- Parent Settings 高保真页面；
- Android Dev Handoff 标注。
