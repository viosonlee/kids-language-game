# Code review follow-up: session lifecycle, bundled ASR, category layout

> Author / Agent: Codex
> Date: 2026-09-28
> Status: In progress; not a full product acceptance report
> Scope: Additive review against docs/01_Product_Requirements.md and docs/15_Toy_Optimization_and_Zero_ADB_Spec.md.

## Confirmed defects and changes

| Finding | Requirement | Correction | Evidence |
|---|---|---|---|
| Chinese and English models absent from APK; Huawei MRX-W09 has no system RecognitionService | FR-APP-002, FR-LANG-003, FR-SPEAK-004 | Bundle official small CN 0.22 and EN-US 0.15 models; record sources and SHA-256 in assets/models/manifest.json | ADB query-services returned No services found; device private model directory contained only Japanese before update; BundledModelsTest checks all three archives |
| Any nonempty directory accepted as an ASR model; interrupted unzip could poison future launches | NFR-REL-003, docs/15 section 2 | Validate required model files, stage extraction, reject path traversal, publish only complete model directory | OfflineModelArchiveTest covers valid, incomplete and unsafe archives |
| Speaker could finish before ASR model initialization and enable recording early | NFR-PERF-005 | Keep Loading state until model load finishes and show progress indicator | RepeatViewModelTest covers premature speaker/record interaction and load failure |
| Fixed 150dp category card clips Chinese titles; grid clips shadows | NFR-COMP-003, NFR-UX-004 | Use content-driven height with 176dp minimum, center multiline text, give grid content shadow padding | Device screenshot verification pending |
| Choose session outlives screen, praise overlaps next prompt, stale option taps accepted | FR-AUDIO-004, NFR-REL-004 | Navigation-owned ViewModel, resume/pause cleanup, single feedback job, item-specific option IDs | ChooseViewModelTest |
| Choose ignores auto-next setting and has no repeated-error assistance | FR-PARENT-002, FR-LISTEN-005 | Honor autoNext, show large forward button, replay prompt after errors, highlight after second error, assist on third | ChooseViewModelTest |

## Remaining review scope

The broad review remains active. The table above is not proof of completion of all documentation requirements.

- Repeat mode still needs auto-next preference and resume-position audit.
- Review audio/TTS cancellation ownership, SoundPool settings and lifecycle, diagnostic-screen cleanup, model switching concurrency.
- Review language-specific content/category selection, image/meaning ambiguity, scoring normalization/aliases, invalid content handling.
- Parent preferences and per-item/language/date progress currently cover less than the PRD; establish implementation and tests for missing settings/statistics.
- Review complete navigation/replay flows, long phrases and font scaling, offline cold start and language switches on tablet.
- A real child-observation session from PRD section 21.3 cannot be replaced with adult/automated tests; it remains a user observation task after technical fixes.

## Workspace and deployment

Existing mascot PNG/WebP changes and current_screen.png were already present or modified by another task. This review does not claim ownership of them.
The first attempted tablet update failed because the default local build used a different debug certificate. The installed APK certificate matches C:/Users/viosonlee/.android/debug.keystore. Build signing overrides are used locally; no key or machine-specific signing configuration is committed.
