# Mink'a Android

Barter app for Android: Kotlin, Jetpack Compose, Material 3 Expressive, Hilt, Ktor `MockEngine` (no backend), DataStore, Room (phase 3). Plan and phase scope: `PLAN.md`. Feature tracking: `odd/tasks/`. Design rationale: `odd/audits/`. Figma file `uMjLWyytKzdU4AgXuNBRZM` describes the whole product; phase scope lives in `PLAN.md` §8 and in code.

## Android skills by default (MANDATORY)

Load these skills with the Skill tool before the matching work, without waiting to be asked, and pass their `SKILL.md` paths to any subagent doing that work:

| Work | Skills |
|---|---|
| Any screen, component, theme or visual change in Compose | `android-design` (load first; it owns design decisions) |
| Layouts for more than one window size, navigation rail/bar, list-detail, foldables | `adaptive`, plus `navigation-3` when multi-pane navigation is involved |
| Navigation graph, back stack, deep links | `navigation-3`, `navigation-event` |
| Insets, system bars, keyboard (IME) handling | `edge-to-edge` |
| Recomposition, lists, scrolling performance | `jetpack-compose-performance` |
| Unit and Compose UI tests, test setup | `testing-setup` |
| Emulator, installing, screenshots, Compose previews, SDK | `android-cli` |
| Permissions, manifest components, intents (photo picker, camera intent) | `android-permissions-security`, `android-intent-security` |
| ANR risks, main-thread work | `anr-audit-and-fix` |
| Release build, R8, app size | `r8-analyzer` |
| Profiling traces, jank, startup | `android-profiler` |

## CodeGraph first (MANDATORY)

The project has a CodeGraph index (`.codegraph/`, git-ignored). For structural questions (where something lives, call flow, dependencies, references, impact of a change), use CodeGraph before Grep/Glob/Read sweeps:

- Prefer the `codegraph_explore` MCP tool when available; otherwise the read-only CLI: `codegraph query`, `explore`, `node`, `callers`, `callees`, `impact`, `affected`, `files`, `status`.
- Run `codegraph sync -q /mnt/files/Projects/minka-android` before CLI reads and after editing code; if sync fails, say so and fall back to normal search.
- Before changing shared code (design system components, use cases, repositories), run `codegraph impact` on it.
- Tell subagents to do the same.

## Project rules

- Light theme only: never add dark mode back (removed from code and Figma on 2026-11-22).
- Hexagonal per feature: `features/<name>/{domain,application,infrastructure,presentation}`; ViewModels depend on use cases only.
- Every phase closes with `./gradlew assembleDebug testDebugUnitTest` green; Compose UI tests run with `./gradlew connectedDebugAndroidTest` on the API 37 emulator (cold boot with `-gpu swiftshader_indirect` if the snapshot crashes).
- Adaptive helpers already exist in `core/designsystem/component/AdaptiveLayout.kt` (`ExpandedWidthBreakpoint` 840dp, `FormMaxWidth` 480dp); reuse them.

## Phase 3 (Items) starting point

- Scope: 36 Figma states listed in `PLAN.md` §8; later-phase elements are hidden in the build, not drawn disabled.
- Design audit done: `odd/tasks/items-design-audit.md`, `odd/audits/items-design.md`. Expanded (≥840dp) layouts exist for 2.02, 3.05, 4.01, 5.03, 5.08, 11.05; component "Navigation rail" `477:192`.
- Open decision before coding: search list-detail with `ListDetailPaneScaffold` on Navigation Compose 2.10, or migrate to Navigation 3 scenes.
- Mandatory before closing phase 3: check 200 % font scale on the emulator (listing cards, long "Busca:" chips).
