# OrcaSlicer Mobile — Feature Parity Audit

## Verified status (2026-10-16)

The original counts below are historical and must not be read as the current UI coverage.
The current source audit reports 887 parsed native keys and 490 visible setting rows routed
to native definitions (319 process, 100 filament, 71 printer; 483 distinct catalog keys).
The row count includes keys that appear in more than one editor. The profile route checker
also verifies that those rows reach matching profile parsers. Run
`scripts/check_profile_setting_routes.py` and `scripts/audit_orca_feature_gap_doc.py` to
recalculate the current figures; the latter only reconciles settings literally named in
this older curated list and is not a complete desktop-parity scanner.

As of this audit, the two safe, native-backed omissions `elefant_foot_layers_density` and
`infill_lock_depth` are now exposed in the existing Precision and Infill groups. We kept
the other candidates out of the UI where the core does not implement them (`adaptive_layer_height`),
where Orca sets them internally (`has_scarf_joint_seam`), where dependent sequence editing
would be absent (`first_layer_sequence_choice`, `other_layers_sequence_choice`), where the
native definition has no useful label/help (`elefant_foot_min_width`), or where the native
tooltip warns of a possible brim-to-model fusion (`brim_use_efc_outline`). Bed-type temperatures are exposed as six clearly named plate groups with separate first-layer
and other-layer values. The selected printer plate determines the values shown before slicing;
explicit Orca values are preserved, while missing legacy plate values are filled from the
high-temperature plate setting. The printer profile defaults to Orca’s configured bed type, with
the existing PEI/high-temperature fallback when no default is configured.

The calibration menu now has an explicit “save measured result” action for Orca modes whose
G-code changes correspond to a profile value: pressure advance, nozzle temperature, maximum
volumetric speed, outer-wall speed (VFA), and filament retraction length. The result is range-
checked and staged through the existing profile-edit repository; the user must evaluate the
printed test and review/save the profile change. No calibration command is sent to a printer.
`Calib_Flow_Rate` is declared in the bundled native enum but has no implementation use in its
G-code path, so the app does not offer a flow-rate test or pretend to save a result for it.

Audit of what the OrcaSlicer engine (now bundled via `app/src/main/jni/libslic3r`)
exposes vs. what the mobile UI actually surfaces. Goal: find features lost when the
project was ported from SliceBeam (Prusa engine) to the Orca engine.

## Historical baseline (2026-10-10)

The first audit counted **862** native keys and **253** UI keys and estimated 180–200 missing
FDM settings. That snapshot predates the current profile catalogs and is no longer a current
coverage estimate. It remains here only to explain earlier prioritization.

## Current method and counts

- Native option definitions are parsed from the current Orca `PrintConfig` and preset schema.
- Visible settings come from `OrcaProcessSettingsCatalog`, `OrcaFilamentSettingsCatalog`, and
  `OrcaPrinterSettingsCatalog`; the route checker also verifies profile parser routing.
- Model tools are inspected from the current model, edit, camera, and slice menus.

Current source audit: **887** parsed native keys and **490** visible setting rows routed to native
definitions (319 process, 100 filament, 71 printer; 483 distinct catalog keys). These counts do
not measure desktop parity: some native keys are runtime/internal, while model tools use separate
object-level APIs. Run `scripts/check_profile_setting_routes.py` and
`scripts/audit_orca_feature_gap_doc.py` to recalculate them.

> The lists below are curated to the settings OrcaSlicer **desktop actually shows in its tabs**.
> Pure-internal keys (`e_position`, `layer_z`, `print_time`, `inherits`, `*_settings_id`, CLI
> verbs like `cut`/`arrange`/`slice`) are intentionally excluded.

---

## 1. Process settings: current coverage and remaining keys

The process catalog currently contains 319 native keys. Jerk and acceleration, pressure advance,
bridge and flow controls, scarf seam, fuzzy skin, ironing, support, brim ears, timelapse, power-loss
recovery, and object labeling are represented in the current settings UI. These are completed
areas; do not re-add them as missing settings.

The route audit finds these native keys absent from profile catalogs: `adaptive_layer_height`,
`brim_use_efc_outline`, `elefant_foot_min_width`, `first_layer_sequence_choice`,
`has_scarf_joint_seam`, and `other_layers_sequence_choice`. `adaptive_layer_height` is handled by
the model layer-height editor instead of the process preset. Orca sets `has_scarf_joint_seam`
internally. Sequence choice needs its paired state/editor; `brim_use_efc_outline` has a native
warning about possible brim fusion; `elefant_foot_min_width` lacks useful native UI metadata.

---

## 2. Filament settings: current coverage and remaining keys

The filament catalog contains 100 native keys. It includes all six bed-surface temperature groups,
pressure advance, cooling, air filtration, chamber controls, physical properties, ironing overrides,
and tool-change settings. Printer capability gates such as `support_chamber_temp_control` and
`support_air_filtration` belong to the printer profile, not the filament profile.

`filament_adaptive_volumetric_speed` remains hidden because Orca marks it experimental/incomplete.
The audit script also reports `slice`, which is a phantom key in the older curated notes, not a
native filament option.

---

## 3. Printer settings: current coverage and remaining gaps

The printer catalog contains 71 native keys. Machine motion limits, Z-hop/retraction-lift settings,
bed type, custom G-code, physical host settings, and the chamber/filtration capability gates are
already exposed. The remaining multi-extruder printer fields are only partially represented; in
particular, Orca-specific AMS count and physical extruder mapping do not yet have an end-to-end
mobile editing and validation flow.

---

## 4. Missing model-editing tools (gizmos) and implemented tools

Current tools: **Move, Rotate, Scale, Mirror, Auto-orient, Lay-on-face (Flatten), Arrange**, plus
bed/object context actions (duplicate, fill-bed, center, delete, select-all).

Missing vs OrcaSlicer desktop:

| Tool | Status | Notes |
|------|--------|-------|
| **Cut** (plane, connectors, keep upper/lower) | ✅ | `TransformCutMenu`, `Model.cut`, native `CutUtils`; connector and plane controls are in the model menu |
| **Color / MMU painting** | ✅ | `PaintModeView` and native `PaintSession`; painted facet data is preserved in 3MF. Compound-volume hit identification remains limited |
| **Support painting** | ✅ | Native facet annotations and support paint mode; tested for touch state and commit |
| **Seam painting** | ✅ | Native facet annotations and seam paint mode |
| **Fuzzy-skin painting** | ✅ | Native facet annotations and fuzzy paint mode |
| **Text emboss / engrave** | ✅ | `EmbossModeView` and `Native.model_emboss_text`; engrave uses a negative volume |
| **SVG import / emboss** | ❌ | SVG guard/simplification exists, but there is no surface-placement SVG emboss workflow |
| **Measure** | ✅ | `MeasureModePanel` and renderer surface-point measurement |
| **Negative / Modifier / Support-blocker volumes** | ⚠️ | Native volume types, cut connectors and text engraving exist; no general UI to add/import, place, and edit these volumes |
| **Per-object / per-part setting overrides** | ❌ | No mobile editing/apply workflow for model-object or volume config overrides |
| **Variable / adaptive layer height** | ✅ | Model menu opens the layer-height profile editor and writes the native object layer-height profile; this does not use the commented-out `adaptive_layer_height` preset key |
| **Split to objects / parts** | ✅ | `splitSelectedObject()` invokes native `Model.split` |
| **Simplify mesh** | ✅ | Selected-object simplification with ratio/target controls and large-model safeguards |
| **Mesh boolean operations** | ❌ | MCUT is present in native dependencies, but no user-facing boolean tool is routed |

---

## 5. Prioritized recommendations

1. **Per-object / per-part setting overrides and modifier volumes** — largest remaining Orca workflow gap; needs a model-aware editor and explicit native application of per-object configuration.
2. **SVG surface emboss** — the native SVG and emboss code exists, but the app needs a bounded import, placement, depth, and add/engrave workflow.
3. **Mesh boolean operations** — MCUT is bundled; expose only after validating memory use, cancellation, and undo for mobile-sized meshes.
4. **Complex multi-volume painting** — extend raycast results with volume/facet ownership before claiming support for arbitrary compound 3MF objects.
5. **Remaining profile options** — `adaptive_layer_height` is commented out in the native preset schema; `brim_use_efc_outline` carries a fusion warning; sequence choice requires related setting state. Keep these hidden until their behavior can be represented safely.


### Completed high-priority settings

Do not add duplicate controls for jerk, pressure advance, flow ratios, internal bridges, scarf seam,
brim ears, timelapse, machine motion limits, or bed-surface temperatures. Printer capability options
`support_chamber_temp_control` and `support_air_filtration` are exposed under Printer → Capabilities
and recognized by the printer-profile import route. The native filament option
`filament_adaptive_volumetric_speed` remains hidden because Orca labels it experimental/incomplete;
per-object volumetric coefficients only work for profiles that already contain saved values.
