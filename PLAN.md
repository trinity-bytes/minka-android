# Plan de la app Android de Mink’a

Plan vivo de la app nativa de Mink’a (trueque comunitario y economía circular en Lima). Proyecto hobby: continúa el trabajo presentado en el curso de la UPC, sin requisitos académicos.

- **Plataforma:** Android, Kotlin, Jetpack Compose + Material 3 (Expressive).
- **Diseño:** archivo de Figma `uMjLWyytKzdU4AgXuNBRZM` (mink-a-project).
- **Landing:** repositorio `minka-landing`; su llamado a la acción es instalar esta app.

## 1. Alcance

### MVP: el ciclo del trueque

El MVP cubre solo el ciclo que le da sentido a Mink’a:

> Onboarding/auth → publicar objeto → explorar/buscar → detalle → proponer intercambio → chat → confirmar con QR

| Feature | Qué incluye en el MVP |
|---|---|
| `onboarding` | Splash, carrusel, permisos, estado sin conexión |
| `iam` | Registro, inicio de sesión, verificación por código, recuperar contraseña, cierre de sesión |
| `items` | Publicar objeto (fotos, categoría, estado, descripción), explorar, buscar y filtrar, detalle |
| `exchanges` | Proponer intercambio, aceptar/rechazar, confirmar la entrega con QR |
| `chat` | Lista de conversaciones y conversación por intercambio |
| `profiles` | Perfil propio y ajeno (datos básicos, objetos publicados) |

### Después del MVP

Gamificación, comunidad, dashboard, notificaciones push, reportes y el resto de ajustes. Están diseñados en Figma, pero no se construyen hasta que el ciclo central funcione.

## 2. Arquitectura

**Hexagonal, organizada por feature** (screaming architecture): la estructura de paquetes dice qué hace la app, no qué framework usa.

```text
com.trinitybytes.minka
├── MinkaApplication.kt        ← @HiltAndroidApp
├── MainActivity.kt            ← única Activity
├── navigation/                ← NavHost y rutas de nivel superior
├── core/
│   ├── designsystem/          ← tema (color, tipografía, formas) y componentes reutilizables
│   ├── domain/                ← tipos compartidos (resultados, errores)
│   ├── di/                    ← módulos de Hilt transversales
│   ├── network/               ← cliente Ktor y el mock API que corre dentro de la app
│   └── database/              ← Room (caché local)
└── features/
    └── <feature>/
        ├── domain/            ← modelos y puertos (interfaces de repositorio). Kotlin puro.
        ├── application/       ← casos de uso
        ├── infrastructure/    ← adaptadores: API (Ktor), rutas del mock API, Room, DataStore
        └── presentation/      ← pantallas Compose + ViewModel
```

Reglas:

1. `domain` no depende de Android ni de ninguna otra capa.
2. `presentation` solo conoce `application` y `domain`; nunca un adaptador concreto.
3. Hilt une los puertos con su adaptador. Los adaptadores hablan HTTP; si algún día hay servidor, se cambia el engine de Ktor, no el dominio ni la UI.
4. Cada pantalla expone un `UiState` inmutable (`StateFlow`) y recibe eventos: flujo de datos unidireccional.
5. Un solo módulo de Gradle al principio. Si los límites entre paquetes se mantienen limpios, extraer módulos después es mecánico.

## 3. Stack

Versiones fijadas en `gradle/libs.versions.toml`, alineadas con las que ya compilan en este equipo.

| Necesidad | Elección | Estado |
|---|---|---|
| Build | AGP 9 + Kotlin 2.4 (Kotlin integrado en AGP), Gradle 9.6, catálogo de versiones | En el scaffold |
| UI | Compose BOM + Material 3 | En el scaffold |
| Íconos | Material Icons Round (`material-icons-extended`, versión del BOM). Pesa varios MB sin minificar: activar R8 en release antes de subir a Play | Fase 1 |
| Navegación | Navigation Compose con rutas tipadas (kotlinx.serialization) | En el scaffold |
| Inyección de dependencias | Hilt + KSP | En el scaffold |
| Concurrencia | Coroutines + Flow | En el scaffold |
| Persistencia | Room + DataStore | Se agrega con la primera feature que lo necesite |
| Red | Ktor Client + `MockEngine` + kotlinx.serialization | Fase 2 |
| Imágenes | Coil 3 | Se agrega con `items` |
| QR | Generación + CameraX/ML Kit para escanear | Se agrega con `exchanges` |
| Tests | JUnit 4, kotlinx-coroutines-test, Turbine, Compose UI test | JUnit y coroutines-test en el scaffold |

**Compatibilidad mínima: Android 8.0 (`minSdk = 26`).** La landing lo promete ("Requiere Android 8.0 o superior"), así que no se sube sin actualizar la landing. Toda API más nueva que el nivel 26 va detrás de un chequeo de versión (`Build.VERSION.SDK_INT`) o de una alternativa de AndroidX; el lint `NewApi` lo detecta. `compileSdk` y `targetSdk` (37) son independientes: no cambian qué dispositivos pueden instalar la app.

## 4. Backend

**No hay backend, por tiempo indefinido.** La app habla con un **mock API que corre dentro de ella**.

1. Los adaptadores de `infrastructure` usan un `HttpClient` de Ktor con DTOs y JSON reales.
2. El engine del cliente es un `MockEngine` (`core/network/MockApi`): cada feature registra sus rutas (`MockRoute`) con multibinding de Hilt, y cada ruta responde JSON con el mismo formato de error (`ApiError`, con un `code` estable).
3. El estado del "servidor" (cuentas, publicaciones, intercambios) se guarda en el dispositivo, así que sobrevive a reinicios de la app.
4. Las respuestas tardan unos 600 ms para que los estados de carga del diseño se vean.
5. Si algún día aparece un servidor, se cambia el engine y la URL base; dominio, casos de uso y UI no se tocan.

## 5. Diseño

Fuente de verdad: Figma `uMjLWyytKzdU4AgXuNBRZM`.

| Tablero | Nodo |
|---|---|
| Mapa de pantallas y estados + wireframes v2 | `307:80` |
| Mockups v1 · Light (134 pantallas) | `333:2` |

- La página **Foundations** (`291:1298`) manda: sus paneles de color fijan el mapeo exacto a roles de Material 3 en claro, y las descripciones de **Components** (`291:1299`) nombran la API de Compose.
- Colores: `MaterialTheme.colorScheme` para los roles de Material 3 y `MinkaTheme.colors` para lo que no tiene rol (marca, acentos, `raised`, `overlay`). La aplicación ofrece un único tema claro sin color dinámico. El modo oscuro se descartó el 22-11-2026 porque su costo supera su valor. Compose BOM 2026.11.01 incluye Material 3 1.5.0 estable y requiere compilar con el SDK 37.1.
- Tipografía: Fraunces (display, headline y Title/Large, con `SOFT 0`, `WONK 1` y tamaño óptico por estilo) e Inter (resto), como fuentes variables empaquetadas. `MinkaTheme.typography.sticker` para el estilo sin lugar en Material 3.
- Formas, espaciado y elevación: `MaterialTheme.shapes`, `MinkaShapes` (`control`, `bento`, `full`), `MinkaSpacing.s4`…`s64` y las sombras teñidas de `MinkaElevation` con `Modifier.minkaShadow`.
- Íconos: Material Icons Round (`Icons.Rounded.*`).
- Componentes en `core/designsystem/component`: `MinkaButton` (6 estilos), `MinkaTextField`, `MinkaCheckboxRow`, `MinkaStrengthMeter`, `MinkaTopAppBar`, `MinkaLogo`, `MinkaBadge`, `MinkaSnackbarHost` y `MinkaDialog`. Los componentes de Material 3 que solo necesitan el tema (Switch, Radio, Chip, Slider, Tabs, Navigation bar) se usan directo. El resto de la librería se construye en la fase que lo usa por primera vez.
- Textos de UI en español; código, nombres e identificadores en inglés.

## 6. Calidad

- **Test-first** en dominio, casos de uso y ViewModels: primero el test en rojo, después la implementación.
- **Fakes en lugar de mocks**: los mismos fakes de `infrastructure` sirven para los tests.
- Tests de UI con Compose para los flujos críticos (registro, publicar, proponer intercambio).
- Cada fase cierra con `./gradlew assembleDebug testDebugUnitTest` en verde.

## 7. Publicación en Google Play

- Cuenta de desarrollador: pago único.
- Una **cuenta personal nueva** tiene que hacer una **prueba cerrada con al menos 12 testers durante 14 días seguidos** antes de pedir acceso a producción. Conviene reclutar testers desde la fase 3.
- El `applicationId` (`com.trinitybytes.minka`) se puede cambiar libremente **antes** de la primera subida a Play; después queda fijo.
- Mientras no haya ficha en Play, la landing puede ofrecer el enlace de la prueba cerrada.

## 8. Fases

| Fase | Objetivo | Entregable |
|---|---|---|
| 0 | Scaffold | Proyecto que compila: Hilt, Compose, navegación, tema base, este plan |
| 1 | Sistema de diseño | Tema completo desde Figma (Light), Fraunces + Inter, formas, espaciado, componentes base, ícono de la app |
| 2 | Onboarding + IAM sobre el mock API | Flujo 1.01–1.22 de Figma, sesión en DataStore |
| 3 | Items sobre el mock API | Publicar, explorar, buscar, detalle; caché en Room |
| 4 | Exchanges + chat sobre el mock API | Proponer, aceptar, chatear, confirmar con QR |
| 5 | Prueba cerrada | Release firmado con R8, prueba cerrada en Play |
| 6 | Post-MVP | Perfil completo, notificaciones, gamificación, comunidad |

#### Alcance de la fase 3 (aprobado el 2026-11-22)

36 estados de Figma: Inicio 2.01–2.05, Búsqueda 3.01–3.05 y 3.07, Detalle 4.01, 4.02 (solo pausada), 4.03, 4.05–4.07, Publicar 5.01–5.11 y Perfil 11.01 (básico), 11.02, 11.05 y 11.06. El perfil entra en esta fase porque Publicar necesita dónde retomar borradores y pausar publicaciones, y el detalle enlaza al perfil del dueño.

- Pasan a la fase 4: 4.02 reservada ("Avisarme si se libera"), 4.04 código QR del objeto, proponer desde 4.01, propuestas recibidas en 4.03, "descargar QR" en 4.07, la pestaña Intercambios y todo lo que depende de trueques cerrados: reputación del dueño, filtro "Reputación mínima" y orden "Mejor calificado".
- Pasan a post-MVP: 3.06 resultados en mapa, 3.08 búsqueda guardada, Eco-Puntos (4.01, 5.06, 5.08, 5.10), "reportar" en 4.07, la tarjeta de impacto y la campana de Inicio, la pestaña Comunidad, el sello "Raíz Verificada" y los favoritos (corazón y contador "guardados").
- En la fase 3, los elementos que dependen de una fase posterior se ocultan hasta que esa fase los construya.

### Progreso

- [x] Fase 0 — Scaffold
- [x] Fase 1 — Sistema de diseño
- [x] Fase 2 — Onboarding + IAM
- [ ] Fase 3 — Items
- [ ] Fase 4 — Exchanges + chat
- [ ] Fase 5 — Prueba cerrada
- [ ] Fase 6 — Post-MVP

## 9. Decisiones abiertas

1. Dominio de la landing (por ejemplo `minkaapp.com`); afecta los enlaces profundos (App Links).
2. `applicationId` definitivo antes de la primera subida a Play.
