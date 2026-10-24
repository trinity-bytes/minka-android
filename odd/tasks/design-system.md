# Design system (phase 1)

Objective: phase 1 of PLAN.md — the Minka design system in Compose, sourced from Figma `uMjLWyytKzdU4AgXuNBRZM`.
Branch: `feat/design-system` (tracker, from `develop`). Delivery strategy: ask-on-risk → chain strategy `feature-branch-chain`. Test runner: JUnit 4 via `./gradlew testDebugUnitTest`.

## Specs

- **S1 — Color.** PLAN.md phase 1 deliverable: "Tema completo desde Figma (Light/Dark)". Light and Dark color schemes come from the Figma Color variables (Light mockups `333:2`, Dark mockups `368:5490`), mapped to Material 3 roles; Minka accents with no M3 role (brand, lavender, amber, sky, coral, warning, info) are exposed as extended colors. The theme follows the system dark setting. The window background matches in both modes.
- **S2 — Typography.** "Fraunces + Inter": the Figma type scale (Display/Headline/Title Large in Fraunces; Title Medium/Small, Body and Label in Inter; plus `Label/Sticker`) as the M3 `Typography`, fonts bundled in the app.
- **S3 — Shapes, spacing, elevation.** "formas, espaciado": Figma radius tokens as M3 `Shapes` (extraSmall 6, small 10, medium 16, large 24, extraLarge 28) plus `MinkaShapes.control` (12), `MinkaShapes.bento` (38) and full (`CircleShape`); spacing `MinkaSpacing.s<n>` on the 4 dp grid (4, 8, 12, 16, 20, 24, 32, 40, 48, 56, 64); Figma elevation effects (sm, md, lg, xl, lift, modal) as ink-tinted drop shadows.
- **S4 — App icon.** "ícono de la app": adaptive launcher icon from the Figma Logomark (`331:2928`).
- **S5 — Base components.** User choice: "Base + los de fase 2 (Recommended)" — the custom components onboarding/IAM uses: Button (Filled, Ink, Tonal, Outlined, Text, Destructive), Text field, Checkbox row, Strength meter, Top app bar, Logo, Badge, Snackbar, Dialog. Material 3 components that only need the theme (Switch, Radio, Chip, Slider, Tabs, Navigation bar) are not wrapped. Everything else is built in the phase that first uses it.
- **S6 — Close.** "Cada fase cierra con `./gradlew assembleDebug testDebugUnitTest` en verde." PLAN.md progress marks phase 1 done.

## Tasks

| ID | Specs | Route | Status | Commit |
|---|---|---|---|---|
| T1 | S1 | inline | done (3 tests) | 20c3744 |
| T2 | S2 | inline | done (4 tests) | ce13817 |
| T3 | S3 | inline | pending | |
| T4 | S4 | inline | pending | |
| T5 | S5 (Button, Text field, Checkbox row, Strength meter) | inline | pending | |
| T6 | S5 (Top app bar, Logo, Badge, Snackbar, Dialog) | inline | pending | |
| T7 | S6 | inline | pending | |

Route evidence: sequential units sharing the theme files; no parallel writer reason.

Slices (feature-branch-chain; forecast ~1500 authored lines, fonts excluded):

| PR | Branch | Base | Tasks |
|---|---|---|---|
| tracker (draft) | `feat/design-system` | `develop` | feature document |
| 1 | `feat/design-system-01-foundations` | `feat/design-system` | T1–T3 |
| 2 | `feat/design-system-02-app-icon` | PR 1 | T4 |
| 3 | `feat/design-system-03-actions-input` | PR 2 | T5 |
| 4 | `feat/design-system-04-navigation-feedback` | PR 3 | T6, T7 |

## Log

- **L1 (2026-10-24, user):** "vamos a empezar con la ejecucion del plan. la primera etapa seria la del sistem de diseño no?"
- **L2 (2026-10-24, user):** scope of components answered "Base + los de fase 2 (Recommended)".
- **L3:** Figma Color/typography/radius/spacing/elevation variables read for both modes; Components page `291:1299` holds ~50 components in 8 groups.
- **L4 (2026-10-24, user):** delivery answered "feature-branch-chain (Recommended)".
- **L5:** Figma Forma/Espaciado/Elevación frames name the Compose API (`shapes.*`, `MinkaShapes.*`, `MinkaSpacing.s<n>`) and add spacing 40–64 and elevations sm and lift; S3 rewritten to match. T1 → 20c3744, T2 → ce13817 on `feat/design-system-01-foundations`.
