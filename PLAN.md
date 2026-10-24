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
│   ├── network/               ← cliente HTTP (cuando exista backend)
│   └── database/              ← Room (caché local)
└── features/
    └── <feature>/
        ├── domain/            ← modelos y puertos (interfaces de repositorio). Kotlin puro.
        ├── application/       ← casos de uso
        ├── infrastructure/    ← adaptadores: fakes, API, Room
        └── presentation/      ← pantallas Compose + ViewModel
```

Reglas:

1. `domain` no depende de Android ni de ninguna otra capa.
2. `presentation` solo conoce `application` y `domain`; nunca un adaptador concreto.
3. Hilt une los puertos con su adaptador. Cambiar de fake a backend real es cambiar un binding, no tocar dominio ni UI.
4. Cada pantalla expone un `UiState` inmutable (`StateFlow`) y recibe eventos: flujo de datos unidireccional.
5. Un solo módulo de Gradle al principio. Si los límites entre paquetes se mantienen limpios, extraer módulos después es mecánico.

## 3. Stack

Versiones fijadas en `gradle/libs.versions.toml`, alineadas con las que ya compilan en este equipo.

| Necesidad | Elección | Estado |
|---|---|---|
| Build | AGP 9 + Kotlin 2.4 (Kotlin integrado en AGP), Gradle 9.6, catálogo de versiones | En el scaffold |
| UI | Compose BOM + Material 3 | En el scaffold |
| Navegación | Navigation Compose con rutas tipadas (kotlinx.serialization) | En el scaffold |
| Inyección de dependencias | Hilt + KSP | En el scaffold |
| Concurrencia | Coroutines + Flow | En el scaffold |
| Persistencia | Room + DataStore | Se agrega con la primera feature que lo necesite |
| Red | Retrofit + OkHttp + kotlinx.serialization | Se agrega con el backend real |
| Imágenes | Coil 3 | Se agrega con `items` |
| QR | Generación + CameraX/ML Kit para escanear | Se agrega con `exchanges` |
| Tests | JUnit 4, kotlinx-coroutines-test, Turbine, Compose UI test | JUnit y coroutines-test en el scaffold |

**Compatibilidad mínima: Android 8.0 (`minSdk = 26`).** La landing lo promete ("Requiere Android 8.0 o superior"), así que no se sube sin actualizar la landing. Toda API más nueva que el nivel 26 va detrás de un chequeo de versión (`Build.VERSION.SDK_INT`) o de una alternativa de AndroidX; el lint `NewApi` lo detecta. `compileSdk` y `targetSdk` (37) son independientes: no cambian qué dispositivos pueden instalar la app.

## 4. Backend

**Contract-first, con fakes primero.**

1. Cada feature define sus puertos en `domain`. El primer adaptador es un **fake en memoria** dentro de `infrastructure`, con datos de ejemplo realistas.
2. Todo el MVP se puede construir y probar sin servidor.
3. En paralelo se escribe el contrato de la API (OpenAPI) a partir de los puertos.
4. Cuando llegue el backend real, se agrega el adaptador remoto y se cambia el binding de Hilt.

**Decisión abierta: qué backend real.**

| Opción | A favor | En contra |
|---|---|---|
| Supabase | Auth, Postgres, almacenamiento de fotos y tiempo real (chat) ya resueltos; cliente oficial para Kotlin | Dependencia de un proveedor; se programa menos backend |
| Backend propio en Ktor + PostgreSQL | Kotlin de punta a punta; control total; aprendizaje de backend | Chat en tiempo real, fotos y auth se construyen a mano |

Se decide antes de la fase 4. Hasta entonces, los fakes no fuerzan ninguna de las dos.

## 5. Diseño

Fuente de verdad: Figma `uMjLWyytKzdU4AgXuNBRZM`.

| Tablero | Nodo |
|---|---|
| Mapa de pantallas y estados + wireframes v2 | `307:80` |
| Mockups v1 · Light (134 pantallas) | `333:2` |
| Mockups v1 · Dark | `368:5490` |

- Colores: variables de Figma (colección Color, modos Light/Dark) con roles de Material 3 y sintaxis ANDROID. El scaffold arranca con el esquema claro tomado de los tokens de la landing; el esquema oscuro se carga desde Figma en la fase 1.
- Tipografía: Fraunces (display) e Inter (texto).
- Íconos: Material Icons Round (`Icons.Rounded.*`).
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
| 1 | Sistema de diseño | Tema completo desde Figma (Light/Dark), Fraunces + Inter, formas, espaciado, componentes base, ícono de la app |
| 2 | Onboarding + IAM con fakes | Flujo 1.01–1.22 de Figma, sesión en DataStore |
| 3 | Items con fakes | Publicar, explorar, buscar, detalle; caché en Room |
| 4 | Exchanges + chat con fakes | Proponer, aceptar, chatear, confirmar con QR. Decisión de backend |
| 5 | Backend real | Adaptadores remotos, contrato OpenAPI, prueba cerrada en Play |
| 6 | Post-MVP | Perfil completo, notificaciones, gamificación, comunidad |

### Progreso

- [x] Fase 0 — Scaffold
- [ ] Fase 1 — Sistema de diseño
- [ ] Fase 2 — Onboarding + IAM
- [ ] Fase 3 — Items
- [ ] Fase 4 — Exchanges + chat
- [ ] Fase 5 — Backend real
- [ ] Fase 6 — Post-MVP

## 9. Decisiones abiertas

1. Backend real: Supabase o Ktor propio (antes de la fase 4).
2. Dominio de la landing (por ejemplo `minkaapp.com`); afecta los enlaces profundos (App Links).
3. `applicationId` definitivo antes de la primera subida a Play.
