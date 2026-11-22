# Mink’a Android

App nativa de Mink’a: trueque comunitario y economía circular al alcance de tu comunidad.

El plan completo (alcance, arquitectura, stack, backend y fases) está en [PLAN.md](PLAN.md).

## Requisitos

- JDK 21 (Gradle usa un toolchain compatible).
- Android SDK con la plataforma 37 (`ANDROID_HOME` configurado, o `sdk.dir` en `local.properties`).
- Android Studio (opcional, recomendado).

## Comandos

```bash
./gradlew assembleDebug          # compila el APK de debug
./gradlew testDebugUnitTest      # tests unitarios
./gradlew connectedDebugAndroidTest  # tests de UI de Compose (necesita un dispositivo o emulador)
./gradlew installDebug           # instala en un dispositivo o emulador conectado
```

## Mock API

Profile endpoints require a bearer session token and use `/me/profile` rather than client-supplied user IDs. Signing out revokes the mock session when online; local sign-out still succeeds offline. Mock credentials use salted password hashes.

La app no tiene backend: responde un mock API que corre dentro de ella (Ktor `MockEngine`, ver [PLAN.md](PLAN.md#4-backend)). Lo que se crea en la app (cuentas, etc.) queda guardado en el dispositivo; para empezar de cero, borra los datos de la app.

| Para | Usa |
|---|---|
| Iniciar sesión | `ana.quispe@gmail.com` o `987654321`, contraseña `Minka2026!` |
| Código de verificación del registro | `482915` (el mock no envía correos ni SMS) |
| Bloqueo de cuenta | 5 contraseñas incorrectas seguidas bloquean el acceso 15 minutos |
| Recuperar contraseña | Cualquier correo o teléfono válido recibe la misma respuesta (no revela si la cuenta existe); no se envía ningún enlace real y se puede reenviar a los 60 segundos |
| "Usar mi ubicación" (perfil) | Pide solo la ubicación aproximada y elige el distrito de Lima más cercano a ella; si el dispositivo no tiene ninguna, preselecciona Miraflores. Distritos e intereses salen del mock |
| Retomar el perfil | Si cierras la app a mitad del perfil (distrito, intereses, notificaciones), al volver abre el paso pendiente; las cuentas que solo inician sesión van directo a Inicio |
| Sin conexión (1.22) | Solo en builds de debug: `adb shell run-as com.trinitybytes.minka touch files/mock_offline` hace que el mock responda como si no hubiera red, en cualquier paso; `adb shell run-as com.trinitybytes.minka rm files/mock_offline` la devuelve y "Reintentar" repite lo último con lo que escribiste |

## Estructura

```text
app/src/main/java/com/trinitybytes/minka
├── core/          ← sistema de diseño y piezas transversales
├── features/      ← una carpeta por feature: domain, application, infrastructure, presentation
└── navigation/    ← NavHost y rutas
```

## Convenciones

- Commits con [Conventional Commits](https://www.conventionalcommits.org/).
- Ramas con Git Flow: `main` (releases) y `develop` (integración); features en `feat/*`.
- Textos de UI en español; código e identificadores en inglés.
