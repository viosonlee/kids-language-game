# Kids Language Companion V1 开发任务清单

> Purpose: Agent / Human implementation backlog

---

# Milestone M0 — Project Skeleton

- [ ] M0-01 Create Android Kotlin Compose project
- [ ] M0-02 Configure minSdk 29
- [ ] M0-03 Landscape-first window behavior
- [ ] M0-04 Navigation skeleton
- [ ] M0-05 Theme tokens
- [ ] M0-06 Add mascot assets
- [ ] M0-07 Verify Android 10 tablet launch

Exit criteria: all screens reachable with placeholder content.

---

# Milestone M1 — High Fidelity UI Shell

- [ ] M1-01 Splash screen
- [ ] M1-02 Home screen
- [ ] M1-03 Category screen
- [ ] M1-04 Mode select screen
- [ ] M1-05 Repeat screen static states
- [ ] M1-06 Choose screen static states
- [ ] M1-07 Success overlay
- [ ] M1-08 Retry overlay
- [ ] M1-09 Session complete screen
- [ ] M1-10 Parent settings shell

Exit criteria: Figma spec can be represented in Compose without business logic.

---

# Milestone M2 — Content / Audio

- [ ] M2-01 Define PracticeItem JSON schema
- [ ] M2-02 Implement ContentRepository
- [ ] M2-03 Add 20 Japanese PoC items
- [ ] M2-04 Add prompt audio mapping
- [ ] M2-05 Add image mapping
- [ ] M2-06 Missing asset fallback

Exit criteria: 20 Japanese items load and display offline.

---

# Milestone M3 — ASR PoC

- [ ] M3-01 SpeechRecognizerEngine interface
- [ ] M3-02 Vosk adapter
- [ ] M3-03 Japanese model packaging/loading
- [ ] M3-04 AudioRecord capture
- [ ] M3-05 Recognize 20 target phrases
- [ ] M3-06 Build evaluation log screen for development
- [ ] M3-07 Test with real child voice

Exit criteria: enough evidence to accept Vosk or decide replacement.

---

# Milestone M4 — Repeat Mode

- [ ] M4-01 PracticeSessionController
- [ ] M4-02 PromptPlaying state
- [ ] M4-03 Recording state
- [ ] M4-04 Recognizing state
- [ ] M4-05 Evaluation rules
- [ ] M4-06 Pass flow
- [ ] M4-07 Retry flow
- [ ] M4-08 SoftPass flow
- [ ] M4-09 Auto next
- [ ] M4-10 Mascot state binding

Exit criteria: child can complete a 10-item Repeat session offline.

---

# Milestone M5 — Choose Mode

- [ ] M5-01 Distractor selection
- [ ] M5-02 3-image answer layout
- [ ] M5-03 Immediate feedback
- [ ] M5-04 Retry behavior
- [ ] M5-05 Session complete integration

Exit criteria: child can complete a 10-item Choose session without reading text.

---

# Milestone M6 — Persistence / Parent Mode

- [ ] M6-01 DataStore settings
- [ ] M6-02 Room progress
- [ ] M6-03 Parent gate
- [ ] M6-04 Text visibility control
- [ ] M6-05 Sound toggle
- [ ] M6-06 Animation toggle
- [ ] M6-07 Session length setting
- [ ] M6-08 Reset progress

---

# Milestone M7 — Three-language Expansion

- [ ] M7-01 Japanese ≥60 items
- [ ] M7-02 Chinese ≥30 items
- [ ] M7-03 English ≥30 items
- [ ] M7-04 Chinese ASR model integration
- [ ] M7-05 English ASR model integration
- [ ] M7-06 Language switching model unload/load

---

# Milestone M8 — Stabilization

- [ ] M8-01 Offline regression
- [ ] M8-02 Android 10 lifecycle test
- [ ] M8-03 Memory profiling
- [ ] M8-04 Audio conflict test
- [ ] M8-05 Permission-denied behavior
- [ ] M8-06 Long-session test
- [ ] M8-07 Child usability observation
- [ ] M8-08 Final bug fix pass

Exit criteria: V1 release gate satisfied.
