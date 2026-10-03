# 横屏手机启动页裁切修复

> Author / Agent: Codex  
> Date: 2026-10-03  
> Status: Native rendering regression verified; device acceptance pending

本记录增量补充 docs/48 的手机布局适配。此前主页与练习页已适配，启动页仍固定使用 320dp 兔子和大标题；794×361dp 横屏下兔子占据几乎全部高度，标题及标语被挤出视口。

新增启动页独立渲染回归用例，在修改前确认失败：Bottom clipped: Kids Language Companion。修复前截图见 assets/60_Splash_Layout/before.png，显示底部文字不可见。

启动页复用现有紧凑横屏判定，兔子调整为 160dp，缩小标题和间距；内容增加边距，文字居中并占据可用宽度。非紧凑窗口保留 320dp 兔子及原字体等级。启动等待仍为 1200ms，导航与角色动画逻辑不变。

修改后默认及两倍字体测试通过，逐行检查文字完整、无省略或越界，并检查兔子图像布局保持正方形。原生渲染截图为 assets/60_Splash_Layout/after.png 和 double-font.png；这不是模拟器或新真机截图。

完整 test assembleDebug lintDebug --offline 通过：Debug 215 + Release 185 = 400 项零失败/错误。

另审查了启动转场：导航在进入首页时从返回栈移除 splash，并使用淡入/淡出。上次平板 UI 树同时出现启动页与主页节点，与转场期间重叠一致，现有证据不足以认定持久残留；本轮不据此修改转场。真实闪帧与后台行为仍需解锁平板后确认。

未安装或发布本次 APK；平板仍锁屏。此修复不代表手机全部页面、角色细节动画或整体产品验收完成。
