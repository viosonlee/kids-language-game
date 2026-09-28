# UI readability, feedback overlay and rabbit launcher icon

> Author / Agent: Codex
> Date: 2026-09-28
> Status: Implemented; device visual acceptance pending
> Scope: Additive implementation notes for docs/03_UI_UX_Design.md and docs/05_Mascot_Asset_Spec.md.

## Readability

- Shared Typography now defines font metrics without fixing text color. Material buttons, dialog actions and outlined controls inherit their component content color instead of labelLarge forcing white text onto a pale surface.
- Added Primary700 (#1766B5) and Primary800 (#12518F) for filled text buttons. Both maintain at least 4.5:1 contrast with white. Lavender controls use dark foreground text.
- Primary and secondary large buttons use one-line measured labels, matching base label sizes, a content-driven minimum height, and available-width fitting. The mode choices remove redundant parenthetical wording. Long completion headings are centered and can wrap independently of buttons; the completion page can scroll at large font sizes.

## Full-screen result feedback

The practice page now has a full-size root for overlays and a separately padded child for ordinary controls. The result scrim fills the root and only fades; scaling no longer shrinks the backdrop and exposes pale margins. This follows the user's request to cover the full screen with the translucent backdrop.

## Launcher icon

The icon uses the actual bunny_idle.png artwork, cropped above the Idle label and placed on #DCEEFF. Ears, expression, blue hoodie and character proportions are preserved. The foreground fits the Android adaptive icon safe region; launcher masks supply the final circular or rounded outline. Five legacy density variants are also generated.

- Reproducible generation: powershell -File scripts/create_app_icon.ps1
- Adaptive foreground: app/src/main/res/drawable-nodpi/ic_launcher_foreground.png
- Adaptive resource: app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
- Largest legacy preview: app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
- Manifest points both icon and roundIcon to @mipmap/ic_launcher.

The built-in image generation tool was unavailable. No image-generation API or CLI was invoked; this is a deterministic composition of the existing mascot artwork, not newly AI-painted artwork. An optional API alternative was presented to the user; the default local-artwork approach was used while independent UI repairs proceeded.

## Verification

UiContrastTest checks white-on-blue, dark-on-pale/disabled colors and the absence of fixed typography colors. Build/test and device verification results are recorded append-only in WORKLOG.md. Unit checks do not replace device screenshots or large-font interaction testing.
