# Items design audit (phase 3)

Audit of the Figma states in phase 3 (flows 02 Inicio, 03 Búsqueda, 04 Detalle, 05 Publicar), done on 2026-11-22 with the android-design skill (Material 3 Expressive) and Nielsen's ten usability heuristics (H1–H10). The user approved every finding. Task tracking: `odd/tasks/items-design-audit.md`.

## How the findings were applied

The Figma file describes the **whole product**: flows 06–14 already design exchanges, Eco-Puntos, community and moderation. Removing later-phase elements from the mockups would erase that design, so findings are applied where they belong:

- **Scope findings (1–5)** change `PLAN.md` §8 and the phase 3 code: later-phase elements are hidden in the build until their phase ships. The Figma is untouched for them.
- **Usability findings (6–20)** change the Figma mockups. Every modified state was first copied to page "Archivo · Interfaces obsoletas" (`462:6114`), one container per flow, with the ` · obsoleta` suffix.

## Scope findings (PLAN.md and code)

| # | Finding | Decision |
|---|---|---|
| 1 | Eco-Puntos appears in 4.01, 5.06, 5.08 and 5.10; Inicio shows the monthly impact card and the notifications bell. All are post-MVP. | Hidden in phase 3 and the MVP. |
| 2 | The navigation bar has five tabs; Comunidad is post-MVP and Intercambios is phase 4. | Phase 3 shows Inicio, Buscar, Perfil; phase 4 adds Intercambios. |
| 3 | Owner rating ("4,8 de 5 · 23 trueques"), "Raíz Verificada", the "Reputación mínima" filter and the "Mejor calificado" sort need closed exchanges. | Hidden until phase 4 (rating) and post-MVP (verification). |
| 4 | `profiles` is MVP scope but no phase built it; drafts are resumed from "Perfil › Mis publicaciones". | Profile states 11.01 (basic), 11.02, 11.05 and 11.06 join phase 3. |
| 5 | Favorites (heart in 4.01, "guardados" in 4.03) have no list anywhere. | Hidden until post-MVP. |
| 12 | 3.04's primary action, "Guardar búsqueda", depends on saved searches (post-MVP). | In the build, the primary action is "Quitar filtros" until saved searches ship. The mockup keeps the full-product version. |

## Usability findings (Figma)

| # | State | Heuristic | Problem | Change |
|---|---|---|---|---|
| 10 | 2.05 | H1, contrast | Cached content was dimmed to 50 %, yet the message says "Ves lo último guardado". Dimmed text fails 4.5:1. | Content at full contrast; only the search bar (network) stays disabled. Room cache backs this state. |
| 16 | 2.02–2.05 | H4 | Two different search entries: Inicio's pill had a filters icon, Buscar's field has a microphone. | Inicio's pill drops the filters icon and opens Buscar with the keyboard ready (one search entry, one set of controls). |
| 18 | 3.01, 3.02, 5.07 | Android conventions | Recent searches, suggestions and the 5.07 location rows used trailing chevrons, an iOS habit. | Chevrons removed; the whole row is the target. |
| 8 | 4.06 | H3 | The loading skeleton had no back button: the user was trapped while the listing loaded. | Back button over the photo skeleton. |
| 13 | 4.07 | H8, H5 | The "más" menu repeated "Compartir" (already in the top bar) and offered "Descargar QR" on someone else's listing (the QR is the owner's handover code). | Menu keeps "Reportar publicación" and "Ocultar"; hiding shows an undo snackbar. |
| 9 | 4.03 → 4.08 | H5 | "Borrar" deleted without confirmation. | New state 4.08: confirmation dialog that also offers pausing as the reversible alternative. |
| 14 | 4.02 → 4.09 | H1 | Only the "Reservado" variant existed; the owner's paused view and the pause feedback were missing. | New state 4.09: "Pausada" tag, "Reanudar" in the toolbar, snackbar with undo. |
| 6 | 5.02 | Android conventions, H7 | A camera permission dialog before choosing photos. The system Photo Picker and `ACTION_IMAGE_CAPTURE` need no runtime permission. | 5.02 becomes a sheet "Tomar foto / Elegir de la galería"; no permission prompt. |
| 7 | 5.01, 5.05 | H4, H9 | 5.01 disabled "Continuar" while 5.05 said errors appear "al tocar Continuar". A disabled button never explains what is missing. | "Continuar" is always enabled and validates on tap. |
| 11 | 5.01, 5.04, 5.05 | Data consistency | "Estado" was optional (inside "Más detalles") but search filters and shows results by condition; listings without it would vanish from filtered results. | "Estado" is required in step 1; the sheet keeps description and estimated value. |
| 17 | 5.03 | Accessibility, H3 | Reordering photos relied on long-press only and there was no way to remove a photo. | Remove button on each photo; tapping a photo offers "Hacer portada" or "Quitar". |
| 19 | 5.01–5.08 | Android conventions, H4 | Publicar opens full screen from the FAB but step 1 used a back arrow; "Guardar borrador" existed only in step 1. | Step 1 uses a close (X) icon; "Guardar borrador" sits next to the main action in every step. |
| 15 | 5.07 | Scope, performance | A map image needed the maps SDK that 3.06 was postponed for. | District and meeting point rows without the map. |
| 20 | 5.08 | H1 | Empty optional rows used the same empty circle as pending items, so they read as required. | Neutral icon and "Agregar" for empty optional rows. |

## Kept as designed

5.10 as the flow's hero moment, the wavy step progress, the 2.01 and 3.03 skeletons, and the 5.05 field errors.

## Proposed, pending approval: profile and cross-cutting states

| # | State | Heuristic | Problem | Proposed change |
|---|---|---|---|---|
| P1 | 11.05 | H6, H3 | 5.11 says drafts are resumed from "Perfil › Mis publicaciones", but the list has no drafts. | "Borradores" filter and draft rows with "Continuar". |
| P2 | 11.05 | H7, H1 | 11.06 says paused listings are reactivated "desde la lista", but rows have no action. | Overflow button per row: Editar, Pausar/Reanudar, Borrar. |
| P3 | 11.05 | M3 components | The status filter looks like tabs (one filled pill, the rest plain text); with "Borradores" it no longer fits. | Scrollable filter chips. |
| P4 | 11.06 vs 4.09 | H4, H5 | Pausing asks for confirmation in 11.06 but uses an undo snackbar in 4.09. Pausing is reversible, so it needs no dialog. | Pause directly with a snackbar that explains the effect and offers "Deshacer"; 11.06 becomes that snackbar. |
| P5 | 11.01 | Android conventions | List rows with chevrons (same as finding 18). | Remove chevrons. |
| T1 | X.01 vs 1.22 | H4 | Two full-screen offline designs: X.01 uses its own layout, 1.22 the "Error state" component. | X.01 reuses the 1.22 "Error state". |
| T2 | X.04 | H3 | Updated terms block the app with no way out for someone who does not accept. | Secondary action "Ahora no" that signs out. |
| T3 | X.03 | H4 | The background is an old Inicio (no logo, "Cerca de ti en Miraflores", filters icon in the search pill). | Background updated to the current 2.02. |

Scope only (code, no Figma change): 11.01 Eco-Puntos, "Logros e impacto", "Mis intercambios" and reviews; 11.02 reviews tab, rating, "Reportar perfil" and "Bloquear"; X.01's low-data tip (Ajustes is post-MVP). X.05 (forced update) belongs to phase 5.
