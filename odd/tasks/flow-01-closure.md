# Flow 01 closure (dark mode removal + deferred findings)

Objective: remove dark mode from code and Figma, fix the actionable deferred findings of `odd/audits/flow-01-code.md` (D1–D4, D6–D8), run the UI suite on the emulator, then merge into `develop` locally.
Branch: `feat/onboarding-iam-11-cleanup` (from `feat/onboarding-iam-10-resume`); unit branches in worktrees under `../minka-android-worktrees/`. Delivery strategy: local merge into `develop`, no push. Test runner: JUnit 4 via `./gradlew testDebugUnitTest`; Compose UI via `./gradlew connectedDebugAndroidTest` on `Medium_Phone_API_37.0`.

## Specs

- **S1 — Dark mode removal.** User: "te doy autorizacion denborrar del codigo y del figma lo referentena dark mode". The app ships one light theme: no dark color scheme, no `values-night`, no `isSystemInDarkTheme`, no Light/Dark previews; the Figma Dark mockups and dark foundations are removed; PLAN.md, README and audits stop describing a dark theme.
- **S2 — Deferred findings.** User: "dejs esos 3 documentados y avamza con eo resto". Fix D1, D2, D3, D4, D6, D7, D8 of `odd/audits/flow-01-code.md`; D5, D9 and D10 stay documented as deferred with their reasons. Each fix ships with tests; `./gradlew assembleDebug testDebugUnitTest` green.
- **S3 — Delegation.** User: "lamza agentes codex con herdr psra que te ayuden, tu encsrgste de supervisarlos y asefurarte que den un resultado de calidad enndiseño y codigo." Units run as Codex agents in Herdr; the parent reviews each diff for design and code quality before merging it.
- **S4 — Delivery.** User: "una vez todo ello esre atendido procede a mergear con develop ennlocsl y dejarlo ready habiendolo probado ennelnemulador que todo edte ok." Full unit and Compose UI suites pass on the emulator and the app is smoke-tested on it before the local merge into `develop`; no push.

## Tasks

| ID | Specs | Route | Status | Evidence |
|---|---|---|---|---|
| T1 | S1, S2 (D1) | Codex `theme` (worktree `theme`) | pending | |
| T2 | S2 (D2, D8) | Codex `state` (worktree `state`) | pending | |
| T3 | S2 (D3, D4) | Codex `backend` (worktree `backend`) | pending | |
| T4 | S1 | inline (Figma) | done | Figma: `293:165` deleted, Color mode Dark removed, `293:5`, `382:5785`, `382:6018` |
| T5 | S2 (D6) | Codex, wave 2 after T1–T3 merge | pending | |
| T6 | S2 (D7) | Codex, wave 2 after T1–T3 merge | pending | |
| T7 | S4 | inline: UI suite, emulator smoke, docs, slice table, merge into `develop` | pending | |

## Log

- **L1 (2026-11-22, user):** "va, encsrgate de todo ello, te doy autorizacion denborrar del codigo y del figma lo referentena dark mode. lamza agentes codex con herdr psra que te ayuden, tu encsrgste de supervisarlos y asefurarte que den un resultado de calidad enndiseño y codigo. una vez todo ello esre atendido procede a mergear con develop ennlocsl y dejarlo ready habiendolo probado ennelnemulador que todo edte ok. dudas o preguntas?"
- **L2 (2026-11-22, user):** "sisi, dejs esos 3 documentados y avamza con eo resto" — D5, D9, D10 stay deferred.
- **L3:** Routes. Writer trigger (a): T1–T3 are independent units with disjoint main surfaces, each several files; they run in parallel worktrees so their Gradle builds do not share one `build/` dir. T1 merges dark removal with D1 because both rewrite the theme. T5/T6 wait for T1–T3: D6 touches the repositories and ViewModels that T2/T3 change. Baseline: 156 unit tests green at 8275526.
- **L4:** T4 (Figma `uMjLWyytKzdU4AgXuNBRZM`). The Dark mockups were already gone (user removed them earlier). Removed: Foundations "Panel Dark" (`293:165`) and the `Dark` mode of the `Color` variable collection (only `Light` remains); Foundations header (`293:5`) now states the single light theme and the 2026-11-22 decision; QR code (`382:5785`) and QR card (`382:6018`) descriptions no longer mention a Dark theme. Kept on purpose: the landing "Store badge / Theme=Dark" variant (the official black Google Play badge, 5 instances; a badge style, not an app theme) and "dark ink" wording in Button/Chip/Nav item (a color, not a mode). Archive pages untouched.
- **L5:** Supervision. `codex-theme` stopped to delegate to a model its own config named and `codex-backend` stopped at its edit surface (`ProfileRepository` lives in `features/onboarding/domain/Onboarding.kt`; callers in `OnboardingUseCases.kt`). Both told to work inline; backend surface extended with those two paths, limited in `OnboardingUseCases.kt` to the port-forced change because `codex-state` owns that file's return types (merge resolved by the parent).
