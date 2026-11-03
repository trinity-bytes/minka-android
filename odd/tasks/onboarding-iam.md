# Onboarding + IAM (phase 2)

Objective: phase 2 of PLAN.md — Figma flow 01 (states 1.01–1.22) over an in-app Ktor mock API, with the session kept in DataStore.
Branch: `feat/onboarding-iam` (from `develop`). Delivery strategy: ask-on-risk → chain strategy `feature-branch-chain`. Test runner: JUnit 4 via `./gradlew testDebugUnitTest`; Compose UI via `./gradlew connectedDebugAndroidTest`.

## Specs

- **S1 — Scope.** PLAN.md phase 2 deliverable: "Flujo 1.01–1.22 de Figma, sesión en DataStore". Figma file `uMjLWyytKzdU4AgXuNBRZM`, flow frame `333:6` (Light) and `368:5494` (Dark).
- **S2 — Mock API.** User: "la decision es usar un mock api local, asi que hasta un futuro indefinido la ap no tendra backend" and "vamos con ktor". The app talks HTTP through a Ktor `HttpClient` whose engine is a `MockEngine` living inside the app: one handler per route, JSON bodies with kotlinx.serialization, mock server state persisted on the device. Swapping to a real server would mean changing the engine and base URL only. PLAN.md drops the Supabase/Ktor backend decision and the "Backend real" phase.
- **S3 — IAM domain.** Hexagonal per PLAN.md §2: `features/auth/{domain,application,infrastructure,presentation}`. Ports in `domain` (Kotlin only); the Ktor adapter in `infrastructure`; ViewModels depend on use cases only. Rules taken from the Figma notes:
  - 1.08: "Tras 5 intentos fallidos. Diálogo con cuenta regresiva y salida a recuperar."
  - 1.10: "Un solo campo; acepta correo o teléfono como el registro."
  - 1.11: "Reenviar se habilita al terminar la cuenta regresiva." Link "Vence en 30 minutos."
  - 1.12: "medidor de fuerza en vivo y términos con versión (se guardan versión y fecha)."
  - 1.14: "Código de 6 dígitos por correo o SMS. 5 intentos, reenvío con temporizador y contacto con soporte."
  - 1.15: "Al agotar los 5 intentos se pide un código nuevo."
- **S4 — Session.** "sesión en DataStore": the session token and onboarding progress persist in DataStore Preferences; 1.01 "El indicador de carga aparece mientras se restaura la sesión."
- **S5 — Screens.** Every state 1.01–1.22 in Light and Dark, built with the phase 1 design system. Distinct states of one screen are one composable driven by an immutable `UiState`.
  - A: 1.01 Splash, 1.02–1.04 Bienvenida 1/3–3/3.
  - B: 1.05 vacío ("Ingresar" deshabilitado hasta completar ambos campos), 1.06 error ("Validación al salir del campo; mensaje bajo cada campo"), 1.07 cargando ("El botón muestra el indicador de carga; campos bloqueados"), 1.08 cuenta bloqueada, 1.09 Google ("Hoja inferior para elegir la cuenta. Al elegir, pasa a 1.07").
  - C: 1.10 Recuperar contraseña, 1.11 enviado.
  - D: 1.12 datos, 1.13 errores ("el botón espera a que todo sea válido"), 1.14 verificar, 1.15 código incorrecto.
  - E: 1.16 distrito ("Sin selección no se avanza"), 1.17 permiso de ubicación ("Basta la aproximada"), 1.18 denegada ("Snackbar con salida a Ajustes; el flujo sigue con la elección manual"), 1.19 intereses ("Se puede omitir"), 1.20 notificaciones ("luego el diálogo del sistema (Android 13+)"), 1.21 todo listo ("lleva a Inicio").
  - F: 1.22 sin conexión ("Aparece en cualquier paso que necesite red; conserva lo escrito").
- **S6 — Close.** "Cada fase cierra con `./gradlew assembleDebug testDebugUnitTest` en verde." PLAN.md progress marks phase 2 done.
- **Assumptions (to confirm when reached):** Google access (1.09) is a mock account picker served by the mock API, with no Google SDK. The mock verification code and seed demo account are documented in README.

## Tasks

| ID | Specs | Route | Status | Commit |
|---|---|---|---|---|
| T1 | S2 | inline | done (3 tests) | 296b81b (dependencies), c3930b4 |
| T2 | S3, S4 | inline | done (16 tests) | 5e93de0 (domain, session), dafa2fc (adapter, mock server) |
| T3 | S5 A | delegated (context backstop) | done (6 unit + 4 UI tests, UI not run: no emulator) | d59f4ce |
| T4 | S5 B | inline | pending | |
| T5 | S5 C | inline | pending | |
| T6 | S5 D | inline | pending | |
| T7 | S5 E | inline | pending | |
| T8 | S5 F, S6 | inline | pending | |

Route evidence: sequential units sharing navigation and the auth module; no parallel writer reason.

Slices (feature-branch-chain):

| PR | Branch | Base | Tasks | Size |
|---|---|---|---|---|
| tracker (draft) | `feat/onboarding-iam` | `develop` | feature document | — |
| 1 | `feat/onboarding-iam-01-network` | tracker | T1 | ~250 |
| 2 | `feat/onboarding-iam-02-auth-domain` | PR 1 | T2 domain, session store, use cases | 364 |
| 3 | `feat/onboarding-iam-03-auth-mock-api` | PR 2 | T2 Ktor adapter, mock auth server, Hilt, README | 664 (size:exception: adapter and server are tested together) |
| 4 | `feat/onboarding-iam-04-welcome` | PR 3 | T3 | ~1150 incl. 3 photos and vector shapes (size:exception: one screen flow) |

## Log

- **L1 (2026-11-03, user):** "va" (start phase 2), then "si, de hecho la decision es usar un mock api local, asi que hasta un futuro indefinido la ap no tendra backend", then "vamos con ktor".
- **L2:** versions checked on Maven: Ktor 3.6.1, DataStore 1.3.0. Forecast ~3000 authored lines → chain strategy question before slice 1 closes.
- **L3 (2026-11-03, user):** delivery answered "feature-branch-chain (Recommended)".
- **L4:** Figma 1.08 dialog: "Por seguridad bloqueamos el acceso tras 5 intentos fallidos. Vuelve a intentarlo en 14:59 o recupera tu contraseña." → 15-minute lock. 1.12/1.13: terms "Acepto los términos y la privacidad (v1.0)"; errors "Este correo ya está registrado", "Las contraseñas no coinciden", "Debes aceptar los términos".
- **L5:** T1 + T2 green: `./gradlew assembleDebug testDebugUnitTest` 31 tests. Mock decisions: unknown contacts get the same `invalid_credentials` answer and password reset always answers 202 (no account discovery); demo account `ana.quispe@gmail.com` / `987654321` / `Minka2026!`, code `482915`, resend after 60 s (README). Risk: item 5 (new dependencies, 296b81b); independent verifier deferred to the feature end per verification timing. RDD off globally.
- **L6:** T3 by a delegated writer (d59f4ce): 37 unit tests green, UI tests compile, not run (no emulator). Deviations: M3 1.4.0 in BOM 2026.09.00 has no `MaterialShapes`/Expressive `LoadingIndicator` → Figma vector shapes (`PathShape`) and a spinning/pulsing shape; "Omitir" hidden on step 3; `SignInRoute(withGoogle)`; mint accent local (#D7F5E3 light, primaryContainer dark). Open: dark Filled buttons use primaryContainer in Figma vs brand in `MinkaButton`; sticker letter spacing `6.sp` in theme should be 6 % (`0.06.em`); no `mint` theme token.
- **L7 (2026-11-03, user):** "lo reviso la siguiene sesion. me lo recuerdas porfa" — pending decision for next session: dark-mode Filled buttons, primary-container (Figma dark mockups) or brand green in both modes (Components page, current `MinkaButton`). Next task after it: T4 (1.05–1.09) on a new slice branch from `feat/onboarding-iam-04-welcome`.
- **L8 (2026-11-03, user):** "seria el primary, prque el dark, aparte de ser muy apagado, nos desviaria del branding. corrigelo priumero en el figma. de paso corrige las pantallas 1.17, 1.20, 5.02, 7.05 en sus versionas light y dark" — Filled buttons stay brand green in both themes (`MinkaButton` already did). Figma: Button `Style=Filled, State=Enabled` (296:36) rebound `primary/container` → `brand/brand` (+ `brand/on-brand`); the dialogs of 1.17, 1.20, 5.02, 7.05 (Light and Dark) were flow children of the screen auto-layout, now absolute and centered. Then "si, aprobado y continua con el resto de cosas": `MinkaColors.mint` added from Figma `brand/mint` (#D7F5E3 / #0E4A28; dark changes from primaryContainer #146236), sticker tracking moved to the theme as `0.06.em`. 38 unit tests green.
