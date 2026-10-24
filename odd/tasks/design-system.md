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
| T1 | S1 | inline | done (3 tests) | 91a67e3 |
| T2 | S2 | inline | done (4 tests) | 81f6018 |
| T3 | S3 | inline | done (4 tests) | f96149f |
| T4 | S4 | inline | done (verified on emulator) | 69a4812 |
| T5 | S5 (Button, Text field, Checkbox row, Strength meter) | inline | done (1 unit + 6 instrumented tests) | bb5eaae (dependency), f7dca84, 999c96c (fix) |
| T6 | S5 (Top app bar, Logo, Badge, Snackbar, Dialog) | inline | done (6 instrumented tests) | 9b165c1 |
| T7 | S6 | inline | done | last commit of slice 4 |

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
- **L6:** slice 1 closed: `./gradlew assembleDebug testDebugUnitTest` green, 11 tests; 581 authored lines (about 170 of tests), over the 400 budget as one cohesive foundations unit. Hashes after rebasing onto the tracker: T1 91a67e3, T2 81f6018, T3 f96149f. RDD off globally, so no native review.
- **L7:** T4 icon reference: Figma splash 1.01 (`334:6`) shows the logomark in on-brand ink on brand green; the adaptive icon uses that (solid background, launcher masks the shape) plus a monochrome layer. Verified on the API 37 emulator after fixing a malformed pathData that made PackageManager return the default icon. Light and dark themes and Fraunces rendering also checked on the emulator.
- **L8:** T5 needs `Icons.Rounded.*`, only in `material-icons-extended` (BOM 1.7.8): committed alone as bb5eaae (Risk item 5) and noted in PLAN.md stack. Components follow the Figma bindings: Filled uses `MinkaTheme.colors.brand` (constant in both modes), Ink inverse surface, Tonal secondary container. Known deviations: M3 Checkbox keeps its 2 dp corner (Figma 6 dp) and the unfocused error border is 1 dp (Figma 2 dp). Instrumented tests 6/6 on the API 37 emulator; light and dark snapshots compared against Figma.
- **L9:** 999c96c: text fields fill with `MinkaTheme.colors.raised` (Figma binds `--color-card`, #172F22 in dark), not container-lowest. T6: dialog uses `MinkaTheme.colors.overlay` and the modal shadow via BasicAlertDialog; app bar actions use onSurface as in Figma; `scrollBehavior` left out because its experimental type would force opt-in on every caller. Logo drawables extracted with an XML parser (white, tinted in Compose). Instrumented 12/12; light/dark snapshots compared against Figma.
- **L10:** phase closed: `./gradlew assembleDebug testDebugUnitTest` green (12 unit tests), `connectedDebugAndroidTest` 12/12 on the API 37 emulator, `lintDebug` with no errors (new warnings: VectorPath and VectorRaster on the logotype). PLAN.md section 5 documents the design system API and marks phase 1 done; README lists the UI test command. Slice sizes: 1 = 581, 2 = 46, 3 = 568 (+fix), 4 = ~540 authored lines. Push and PRs are pending the user's decision.
