# Flow 01 design audit: "Flujo 01 · Onboarding y acceso"

- Date: 2026-11-22. Figma file `uMjLWyytKzdU4AgXuNBRZM`, page "App · Mockups", flow frame `333:6` (screens 1.01–1.22, sections A–F).
- Scope: all screens of the flow. Dark mode is out of scope by product decision (2026-11-22); it is not a finding.
- Method: android-design skill (Material 3 Expressive + Android conventions): rank goals, emphasis, Android conventions, adaptive layouts, accessibility. All findings below were applied in Figma.

## Summary

| ID | Screen(s) | Severity | Finding | Change applied | Code impact |
|---|---|---|---|---|---|
| A1 | 1.01 | high | Custom splash after the mandatory Android 12+ system splash shows two splashes | 1.01 is now the system SplashScreen spec | No `installSplashScreen` yet; in-app splash composable becomes obsolete |
| A2 | 1.02, 1.04 | high | No medium/expanded layout | New row "Ventanas ≥840dp" in section A | Adaptive two-pane welcome layout |
| A3 | 1.04 | low | "Omitir" on the last step, which already shows the access group | Removed | Remove the button on the last step |
| A4 | 1.04, 1.05–1.09 | medium | "Continuar con Google" was a text-only button, no official G mark | Button variants `Style=Google` (Enabled/Disabled) | Use official G mark in the Google button |
| A5 | 1.04 | low | 200% font-scale behavior undocumented | Caption states the rule | Verify scroll/shrink behavior at 200% |
| B1 | 1.06 | high | Sign-in validated password length | Password field no longer in error; caption updated | `SignInViewModel.kt:214` |
| B2 | 1.05–1.09, 1.12, 1.13, 1.22 | high | Password fields had no show/hide control | Text field property "Visibility toggle" | `SignInScreen.kt`, `RegisterScreen.kt` |
| B3 | 1.08 | medium | Dialog used a filled "Entendido" | Text button next to "Recuperar" | Dialog actions as text buttons |
| B4 | 1.09 | medium | Custom "Elige una cuenta" sheet drawn by the app | Documented as system sheet (Credential Manager) | Use Credential Manager; mock API simulates it |
| B5 | 1.05 (all forms B–E) | medium | No expanded layout for forms | New row "Ventanas ≥840dp" in section B | Constrain form width to 480dp |
| D1 | 1.12, 1.13 | medium | Terms copy exposed internal version; documents unreachable | Copy "Acepto los términos y la privacidad" with links | `RegisterScreen.kt:236`; link destinations needed |
| F1 | 1.22 | high | "Sin conexión" replaced the form with a full-screen error card | Form stays visible; snackbar with "Reintentar" | `SignInScreen.kt:121`; likely register, verify code, recovery |

## Findings and rationale

### A1 Splash (1.01)
- Problem: Android 12+ always shows a system splash. A custom splash afterwards means two splashes; the system splash cannot show a wordmark or a loader.
- Rule: use the platform SplashScreen API; keep it brief and brand-minimal.
- Figma: 1.01 is now the system SplashScreen spec: only the Cookie icon in the 192dp safe zone, no wordmark, no loader. It stays on screen via `setKeepOnScreenCondition` while the session restores from DataStore.
- Code: add `installSplashScreen` (androidx.core:core-splashscreen); the in-app splash composable becomes obsolete.

### A2 Expanded layout for welcome (1.02, 1.04)
- Problem: no medium/expanded layout.
- Rule: never stretch phone content; say what extra space reveals.
- Figma: new row "Ventanas ≥840dp" in section A (`456:6144`): two panes, photo left, text and actions right, bottom-aligned.
- Code: window-size-class based two-pane welcome.

### A3 "Omitir" on last step (1.04)
- Problem: the last step already shows the access group, so skipping is meaningless.
- Rule: do not offer an action that leads nowhere new.
- Figma: "Omitir" removed from 1.04.
- Code: hide the skip action on the last step.

### A4 Google button (1.04, 1.05–1.09)
- Problem: "Continuar con Google" was text-only. Google sign-in branding requires the official G mark; text-only buttons belong in action groups.
- Rule: Google identity branding guidelines; M3 button anatomy.
- Figma: new Button component variants `Style=Google`: Enabled `456:6082`, Disabled `456:6090` (outlined, official G via "Show icon"). Used in 1.04 and 1.05–1.09.
- Code: outlined button with the official G asset.

### A5 Font scale (1.04)
- Problem: behavior at large font scale was unspecified.
- Rule: layouts must survive 200% font scale; critical actions stay reachable.
- Figma: caption states: the photo shrinks first, then the screen scrolls; access buttons always stay visible.
- Code: implement and test at fontScale 2.0.

### B1 Sign-in password validation (1.06)
- Problem: sign-in validated password length ("Mínimo 8 caracteres").
- Rule: sign-in must not enforce the registration policy. It leaks the policy and blocks older passwords. Wrong credentials get one generic error.
- Figma: 1.06 password field no longer in error. Caption: "Validación al salir del campo solo para el formato del correo o teléfono. La contraseña no se valida por largo al iniciar sesión: credenciales incorrectas muestran un error genérico. Los campos de contraseña tienen el botón mostrar/ocultar."
- Code: `SignInViewModel.kt:214` returns `SignInFieldError.ShortPassword` using `MIN_PASSWORD_LENGTH`; remove for sign-in.

### B2 Password visibility toggle (1.05–1.09, 1.12, 1.13, 1.22)
- Problem: no show/hide control on password fields.
- Rule: Android convention and accessibility; reduces typos without a confirm field.
- Figma: Text field component (`297:32`) gains boolean property "Visibility toggle" (trailing visibility icon, `surface/on-surface-variant`). Enabled on every password field in 1.05–1.09, 1.12, 1.13 and 1.22. In State=Error the error icon is hidden when the toggle is on.
- Code: `SignInScreen.kt` and `RegisterScreen.kt` use `PasswordVisualTransformation` with no toggle.

### B3 Dialog actions (1.08)
- Problem: "Cuenta bloqueada" dialog used a filled "Entendido".
- Rule: M3 dialogs use text buttons for actions.
- Figma: "Entendido" is now a text button next to "Recuperar".
- Code: align dialog actions.

### B4 Account chooser (1.09)
- Problem: the design drew a custom "Elige una cuenta" sheet.
- Rule: on Android, Google account selection is the system Credential Manager sheet; apps do not draw it.
- Figma: caption "Hoja del sistema (Credential Manager): la app no la dibuja; aquí solo se documenta. En el mock API se simula. Al elegir, pasa a 1.07." The visual is kept as documentation only.
- Code: no custom sheet; use Credential Manager when the real API arrives.

### B5 Expanded forms (sections B–E)
- Problem: form screens had no expanded layout.
- Rule: do not stretch forms across wide windows.
- Figma: new row "Ventanas ≥840dp" in section B (`462:6835`) with 1.05 expanded (`462:6841`): top app bar and form together in a centered 480dp column (+16dp margins). The pattern applies to every form in the flow.
- Code: shared max-width (480dp) container for forms.

### D1 Terms copy (1.12, 1.13)
- Problem: "Acepto los términos y la privacidad (v1.0)" exposed an internal version; the documents were not reachable.
- Rule: show user-facing copy only; legal documents must be reachable from the acceptance.
- Figma: "Acepto los términos y la privacidad" with "términos" and "privacidad" styled as links (primary + underline). The version is still stored with the acceptance date, not shown.
- Code: `RegisterScreen.kt:236` copy; the links need destinations.

### F1 Offline state (1.22)
- Problem: "Sin conexión" replaced the whole screen with a full-screen error card, hiding the form while claiming "Lo que escribiste se guardó".
- Rule: keep user input visible; use a snackbar for transient errors with a retry action; reserve full-screen errors for steps with nothing to show.
- Figma: 1.22 keeps the form visible (registration form example) with snackbar "Sin conexión. Lo escrito sigue aquí." and action "Reintentar". Caption: "Al enviar sin red, la pantalla no cambia: lo escrito queda visible y un snackbar ofrece Reintentar. La pantalla completa de error solo se usa cuando un paso no puede mostrar nada sin red (p. ej. la lista de distritos)."
- Code: `SignInScreen.kt:121` swaps to `MinkaOfflineScreen` when `uiState.isOffline`; same pattern likely in register, verify code and recovery.

## Flow 01 implementation alignment (2026-11-22)

- `MinkaTopAppBar` uses Material 3 `LargeFlexibleTopAppBar` with the existing collapsing behavior (ab72e56); the approved light theme and Inter/Fraunces typography remain unchanged.
- The activity keeps transparent system bars and `adjustResize`. Flow screens and app bars use safe-content insets for system bars, keyboard, and gesture regions; scaffold padding is consumed without a second IME inset (78184cc). This is a code-level check: visual and keyboard/gesture verification on a device remains pending.

## Considered, not changed

| Item | Reason |
|---|---|
| 1.05/1.13/1.14 disabled primary button until the form is valid | Explicit Figma rule; recommend a later revisit (disabled buttons hide why) |
| 1.12 "Confirmar contraseña" | Product decision; the visibility toggle makes it redundant. Open decision |
| 1.19 selected chips with ink (near-black) fill | Competes with the primary CTA; kept as brand choice |
| 1.11/1.11b blue confirmation cookie | Expressive confirmation; verify it is a theme token, not an ad-hoc color |
| Decoration density on welcome photos (disc + blob + sticker) | Taste; kept |
| Passkeys first (Android sign-in convention) | Out of scope while the app runs on the mock API |
| Dark mode | Dropped by product decision |

## Archive

- Page "Archivo · Interfaces obsoletas" (`462:6114`), frame "Flujo 01 · reemplazado en la auditoría (2026-11-22)" (`462:6115`): copies of 1.05, 1.06, 1.07, 1.08, 1.09, 1.12, 1.13, 1.22 before the changes, each named "… · obsoleta".
- Section A originals were edited before the archive existed: recover them from Figma version history (2026-11-22, before 09:30).
- The intermediate proposal frame "Review · Tramo A propuesto (2026-11-22)" (`453:5986`) stays on "App · Mockups" as history.

## Figma node reference

| Node ID | Name / purpose |
|---|---|
| `333:6` | Flow frame "Flujo 01 · Onboarding y acceso" |
| `456:6144` | Section A row "Ventanas ≥840dp" (A2) |
| `456:6082` | Button `Style=Google`, Enabled (A4) |
| `456:6090` | Button `Style=Google`, Disabled (A4) |
| `297:32` | Text field component, property "Visibility toggle" (B2) |
| `462:6835` | Section B row "Ventanas ≥840dp" (B5) |
| `462:6841` | 1.05 expanded (B5) |
| `462:6114` | Page "Archivo · Interfaces obsoletas" |
| `462:6115` | Frame "Flujo 01 · reemplazado en la auditoría (2026-11-22)" |
| `453:5986` | Frame "Review · Tramo A propuesto (2026-11-22)" |
