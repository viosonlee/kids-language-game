# 平板覆盖安装与初步布局检查

> Author / Agent: Codex  
> Date: 2026-10-03  
> Status: Partial device verification

本记录增量补充 docs/54：重新连接后 ADB 已识别 HUAWEI MRX-W09，Android 10，物理屏幕 1600×2560、密度 400；应用横屏显示 2560×1600。

将 debug 应用从 1.0.0/code 1 通过 `adb install -r` 升级到 1.0.2/code 3，返回 Success。安装前备份应用私有文件，升级后、首次启动前比对 user_settings.preferences_pb 字节一致；未清除数据。此比对只证明设置文件保留，不等于全部数据库迁移验收。

成功启动并通过触控从首页进入场景选择、玩法选择；三页未观察到文字裁切或按钮异常换行。截图保存在 assets/55_Tablet_Review。兔子素材内仍含 Idle/Praise 英文标识，素材与动画验收仍未完成。

尚未验证实际发声的中英文识别、完整答题闭环、诊断页、结果弹窗、后台/锁屏及长时间稳定性。不得据此次安装和三页检查认定整体实机验收通过。未发布公开 APK。
