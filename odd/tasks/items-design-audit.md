# Items design audit (phase 3, flows 02–05 + profile)

Objective: audit the Figma states of phase 3 (Items) against Material 3 Expressive (android-design skill) and Nielsen's heuristics, apply the corrections in Figma and archive the replaced screens. Code comes after design approval.
Branch: `docs/items-design-audit` (from `develop`). Delivery strategy: ask-on-risk, local commits only. Test runner: none (design and docs only; structural readback).

## Specs

- **S1 — Scope.** User approved the cut: 32 states of flows 02 Inicio (`355:3712`), 03 Búsqueda (`357:2541`), 04 Detalle (`359:5982`), 05 Publicar (`364:5082`): 2.01–2.05, 3.01–3.05, 3.07, 4.01, 4.02 (paused only), 4.03, 4.05–4.07, 5.01–5.11. Answer to the profiles gap: "Sumar 4 de perfil (Recomendado)" — profile states 11.01 (basic), 11.02, 11.05, 11.06 join phase 3 (36 states); favorites (heart, "guardados") hidden until post-MVP. Recorded in PLAN.md §8.
- **S2 — Audit.** User: "dale con la auditoria usando tus skills de sndroid design y euristicas de nielsen". Findings 1–20 (listed in `## Log` L3) approved: "apruebo todi".
- **S3 — Apply and archive.** User: "las interfaces que que queden reemplazadss 0ssalas a Archive, pars tener un regidtro documental del proyecto." Corrections go into the flows; every replaced screen is first copied to page "Archivo · Interfaces obsoletas" (`462:6114`) with the ` · obsoleta` suffix, one frame per flow, following the flow 01 convention.
- **S4 — Rationale.** Findings, changes and their grounds documented in `odd/audits/items-design.md` (same role as `odd/audits/flow-01-design.md`).
- **S5 — Pending audit.** Profile states 11.01, 11.02, 11.05, 11.06 and "Estados transversales" (`363:6134`) have not been audited yet; audit, then ask for approval before applying.

## Tasks

| ID | Specs | Route | Status | Evidence |
|---|---|---|---|---|
| T1 | S1 | inline | done | PLAN.md §8 |
| T2 | S2, S3 | inline (Figma) | done | Flujo 02: findings 10, 16; archive 468:405 |
| T3 | S2, S3 | inline (Figma) | done | Flujo 03: finding 18; archive 468:409 |
| T4 | S2, S3 | delegated (parallel units with T5, disjoint frames) | done, parent spot check + 2 fixes | Flujo 04: findings 8, 9, 13, 14; new 4.08 (470:6249), 4.09 (470:6346); archive 468:413 |
| T5 | S2, S3 | delegated (parallel units with T4) | done, parent spot check + 1 fix | Flujo 05: findings 6, 7, 11, 15, 17, 19, 20; archive 468:417 |
| T6 | S4 | inline | done | `odd/audits/items-design.md` |
| T7 | S5 | inline | audited, waiting for approval | P1–P5, T1–T3 in `odd/audits/items-design.md` |

## Log

- **L1 (2026-11-22, user):** "va  ahora que hemis terminad9 con eo flujo 1, que sigue en el plan?" → "que flujos del figma abarca ese flujo de losnitems?" → "si, rebisa esos edtados primero" → "si, apruebo. dale con la auditoria usando tus skills de sndroid design y euristicas de nielsen" → "apruebo  todi, las interfaces que que queden reemplazadss 0ssalas a Archive, pars tener un regidtro documental del proyecto."
- **L2:** Scope review: 3.06 map and 3.08 saved search to post-MVP; 4.02 reserved, 4.04 QR to phase 4; 4.02 paused stays. Later-phase elements are hidden, not shown disabled (H4).
- **L3 — Approved findings.** High (scope/plan): 1 Eco-Puntos also in 5.06, 5.08, 5.10; impact card and bell in Inicio are post-MVP. 2 Nav bar: phase 3 keeps Inicio, Buscar, Perfil. 3 Seller rating, "Raíz Verificada", reputation filter and "Mejor calificado" depend on phase 4: hide. 4 Profiles had no phase (resolved in S1). 5 Favorites have no list (resolved in S1: hidden). High (usability): 6 drop 5.02 camera permission: Photo Picker + `ACTION_IMAGE_CAPTURE` need none; sheet "Tomar foto / Elegir de la galería". 7 5.01 vs 5.05 contradiction: "Continuar" enabled, validate on tap. 8 4.06 loading has no back button. 9 Delete in 4.03 has no confirmation. 10 2.05 offline dims content: keep full contrast, disable only network actions. Medium: 11 "Estado" required in step 1 (search filters by it). 12 3.04 primary becomes "Quitar filtros"/"Ampliar distancia". 13 4.07 menu keeps only "Ocultar" with undo snackbar. 14 4.02 paused needs the owner view ("Pausada" label, "Reanudar") and pause feedback. 15 5.07 drops the map. 16 Inicio search pill opens Buscar with the keyboard (one search entry). 17 Photo reorder needs an accessible alternative ("Hacer portada") and remove. Low: 18 no chevrons in Recientes/suggestions. 19 Publicar step 1 uses close (X); "Guardar borrador" consistent across steps. 20 5.08 optional empty items must not read as pending.
- **L4 — Applying.** The Figma describes the whole product (flows 06–14 design later phases), so scope findings 1–5 and 12 go to PLAN.md and code only; usability findings change the mockups. Profiles answer (S1) added to PLAN.md §8.
- **L5 — Done in Figma.** 02: filters icon hidden in Inicio's search pill, 2.05 content back to full opacity. 03: trailing chevrons hidden in 3.01/3.02. 04 (writer): back button in 4.06, menu keeps Reportar/Ocultar, new 4.08 delete dialog and 4.09 paused owner view; parent fixed "Minka" → "Mink'a" and the toolbar drawn above the scrim in 4.08. 05 (writer): 5.02 becomes the photo source sheet, Continuar enabled, Estado required in step 1 (also on 5.04's background), remove buttons on photos, X on step 1, Guardar borrador on every step, map removed in 5.07, neutral icon in 5.08; parent hid the chevrons left in 5.07. Every modified state was cloned to the archive first (02: 4, 03: 2, 04: 3, 05: 9).
- **L6:** Profile (11.01, 11.02, 11.05, 11.06) and cross-cutting states audited: P1–P5, T1–T3 proposed in `odd/audits/items-design.md`; waiting for the user's approval.
