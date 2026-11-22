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
| T3 | S3 | pass 1: 2 parallel read-only reviewers (architecture, presentation); passes 2–3 inline | in progress | |
| T4 | S4 | inline | pending | |

## Log

- **L1 (2026-11-22, user):** "va, termina de auditar todo el flujo 1, recuerdabquebtienes codegraph por si ocuoas buscar codigo. una vez que hayas realizado la auditoria aplica las correecciones y documenta los cambios y el sustento y deja en archivo las interfaces obsoletas, oara optimixar el trabajo puedes oanzar subagentes sonnet o haiku para tareas basicas de documentacion o que no requieran mucho criterio o inteligencia. una vez culmines con ello ponte con ls revision del codigo, documenta tus hallazgos, y realiza un ciclo de 2 o 3 revisiones para asegurartr que hayas logrado un buen analisis y podterior a ello procede a aplicar oas mejoras, deja todo commiteado en local."
- **L2:** Section A already audited and merged earlier today (see `odd/tasks/onboarding-iam.md` L21).
- **L3:** Design findings A1–A5, B1–B5, D1, F1 applied in Figma; components changed: Button `Style=Google` (456:6082/456:6090), Text field "Visibility toggle" (297:32). Replaced screens archived on page "Archivo · Interfaces obsoletas" (462:6115). Section A originals predate the archive (Figma version history).
