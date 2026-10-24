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
