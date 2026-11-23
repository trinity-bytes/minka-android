# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/); versiones con [SemVer](https://semver.org/lang/es/).

## [0.1.0] - 2026-11-22

Primera versión interna: fases 0 a 3 de `PLAN.md`, sobre el mock API local (sin backend).

### Agregado

- Base del proyecto: Kotlin, Jetpack Compose, Material 3 Expressive, Hilt, Ktor con `MockEngine` y DataStore.
- Sistema de diseño desde Figma (tema claro, Fraunces e Inter, formas expresivas, componentes base e ícono de la app).
- Onboarding e IAM (Figma 1.01–1.22): bienvenida, registro con verificación de código, inicio de sesión con bloqueo por intentos, recuperación de contraseña, configuración de distrito, intereses y notificaciones, y la sesión en DataStore.
- Navegación con Navigation 3: barra de navegación en teléfonos y riel desde 840dp, con historial propio por pestaña (Inicio, Buscar, Perfil).
- Publicaciones con caché en Room: Inicio con la grilla cercana y el modo sin conexión, Búsqueda con filtros y lista y detalle lado a lado en pantallas anchas, Detalle con las acciones del dueño (pausar, reanudar, borrar), Publicar en tres pasos con Photo Picker y cámara sin permisos, borradores y Perfil con Mis publicaciones.

[0.1.0]: https://github.com/trinity-bytes/minka-android/releases/tag/v0.1.0
