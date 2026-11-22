# Flow 01 audit (design + code)

Objective: audit Figma flow 01 (1.01–1.22) against Material 3 Expressive and Android conventions, apply the design corrections, then review and improve the code that implements it.
Branch: `feat/onboarding-iam-10-resume`. Delivery strategy: ask-on-risk, local commits only. Test runner: JUnit 4 via `./gradlew testDebugUnitTest`; Compose UI via `./gradlew connectedDebugAndroidTest`.

## Specs

- **S1 — Design audit.** User: "termina de auditar todo el flujo 1". Sections A–F of Figma file `uMjLWyytKzdU4AgXuNBRZM`, flow frame `333:6`, audited with the android-design skill (Material 3 Expressive).
- **S2 — Design corrections.** User: "una vez que hayas realizado la auditoria aplica las correecciones y documenta los cambios y el sustento y deja en archivo las interfaces obsoletas". Corrections go into the flow; the replaced screens are archived in Figma, not deleted; the findings, changes and their rationale are documented in `odd/audits/flow-01-design.md`.
- **S3 — Code review.** User: "ponte con ls revision del codigo, documenta tus hallazgos, y realiza un ciclo de 2 o 3 revisiones para asegurartr que hayas logrado un buen analisis". Findings in `odd/audits/flow-01-code.md`, refined over 2–3 review passes.
- **S4 — Code improvements.** User: "podterior a ello procede a aplicar oas mejoras, deja todo commiteado en local". Includes aligning the code with the S2 design corrections. Each change ships with tests; `./gradlew assembleDebug testDebugUnitTest` green.
- **S5 — Delegation.** User: "puedes oanzar subagentes sonnet o haiku para tareas basicas de documentacion o que no requieran mucho criterio o inteligencia".

## Tasks

| ID | Specs | Route | Status | Evidence |
|---|---|---|---|---|
| T1 | S1, S2 | inline (Figma) | done | Figma: sections B–F corrected, archive page `462:6114` |
| T2 | S2, S5 | delegated (sonnet, docs) | done | `odd/audits/flow-01-design.md` (129 lines), spot-checked |
| T3 | S3 | pass 1: 2 parallel read-only reviewers; pass 2 parent verification; pass 3 independent refuter | done | `odd/audits/flow-01-code.md`, f75d3e3 |
| T4a | S4 (C1, C6–C8, C16, C17) | delegated writer W1 (parallel units) | done; verifier: C16 partial (D10, accepted) | 020a52f |
| T4b | S4 (C2, C3, C5 forms, C9, C11–C15, C19) | delegated writer W2 (parallel units) | done | fb20463, 4188965 |
| T4c | S4 (C4, C5 welcome/setup, C10, C18) | delegated writer W3 (parallel units) | done | 3046209 |

## Log

- **L1 (2026-11-22, user):** "va, termina de auditar todo el flujo 1, recuerdabquebtienes codegraph por si ocuoas buscar codigo. una vez que hayas realizado la auditoria aplica las correecciones y documenta los cambios y el sustento y deja en archivo las interfaces obsoletas, oara optimixar el trabajo puedes oanzar subagentes sonnet o haiku para tareas basicas de documentacion o que no requieran mucho criterio o inteligencia. una vez culmines con ello ponte con ls revision del codigo, documenta tus hallazgos, y realiza un ciclo de 2 o 3 revisiones para asegurartr que hayas logrado un buen analisis y podterior a ello procede a aplicar oas mejoras, deja todo commiteado en local."
- **L2:** Section A already audited and merged earlier today (see `odd/tasks/onboarding-iam.md` L21).
- **L3:** Design findings A1–A5, B1–B5, D1, F1 applied in Figma; components changed: Button `Style=Google` (456:6082/456:6090), Text field "Visibility toggle" (297:32). Replaced screens archived on page "Archivo · Interfaces obsoletas" (462:6115). Section A originals predate the archive (Figma version history).
- **L4:** Baseline before code changes: 125 unit tests green. Shared pieces created by the parent before the parallel writers: `MinkaGoogleButton` (`core/designsystem/component/GoogleButton.kt`, `res/drawable/ic_google_g.xml`) and `AdaptiveLayout.kt` (`ExpandedWidthBreakpoint` 840dp, `FormMaxWidth` 480dp, `formColumnWidth()`, `MinkaAdaptiveBox`). Writer trigger: three independent units with disjoint surfaces, each several files.
- **L5:** Writers done. Parent follow-ups: `SignInWithGoogle` requires `BeginOnboarding` (writer left a nullable default for a test outside its surface), unused `splash_loading` removed, `MinkaTopAppBar` titles get `heading()`. Tip verified: `./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin` green, 145 unit tests (baseline 125). Compose UI tests compile but did not run: no device attached. Intermediate commits are grouped by unit; only the tip was built (units were written in parallel against one tree). RDD: `gentle-ai review assess` on f75d3e3..HEAD = high / review_due, but receipt-driven development is off (global), so no native review. Independent verifier launched for T4a (high-risk items 1–2: lockout, persisted mock format).
- **L6:** Verifier on T4a: C1, C6, C7, C8, C17 PASS, no regressions; C16 partial (crash window after the server's 201), accepted as D10 with reason. Probes added as regression tests; suite 156 green. Pending outside this feature: run the Compose UI tests on a device; deferred D1–D10.
