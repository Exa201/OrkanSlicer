# Changelog

## 0.8.194 (alpha)

- Preserve an imported 3MF model's T1/part-color assignment by default instead of silently flattening it to one slot before slicing.
- Use one filament-assignment dialog from the model menu and pre-slice flow. A confirmed assignment updates the model preview and is reused for slicing unless the model, printer or palette changes.
- Show temporary 3MF filament materials in Quick Slice and the workspace header instead of the unrelated saved filament preset.
- Show only tools actually selected in the G-code in the printer preparation dialog; a T1-only file no longer displays an unused T0 mapping.
- Write the selected ACE gate's physical color and material into Anycubic vendor metadata. Keep the sliced temperature commands unchanged.
- If a direct print uses a different material from the selected ACE slot, offer upload-only and require the user to correct the filament settings and re-slice before printing.
- Tested a large two-filament 3MF import, 400,000-triangle reduction, Kobra 3 V2 slicing, complete G-code, and upload-only path on an Honor Magic7 Pro. The uploaded file was retrieved from the printer and passed G-code integrity checks. No physical print was started because the model requests PLA while the connected ACE contains PETG/ABS.

## Earlier alpha development

This fork includes the earlier OrcaMobile/SliceBeam Kotlin UI migration, model and 3MF workflows, maker tools, printer integrations, and Android 16 compatibility work. The upstream project history and retained credits document their origins.
