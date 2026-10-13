# Changelog

## 0.8.210 (alpha)

- Expose the native Orca bridge line width and relative bridge angle settings together with the existing bridge controls; move bridge flow and density into the same group so they appear only once.
- Show bridge line width as linked nozzle-relative percent and millimeter fields, capped at 100% to match the native slicer's validation. Add a compact Auto (0) action to the paired line-width editor.
- Add German text for the bridge-width control and explanation, and verify the actual localization catalog through a unit test.

## 0.8.209 (alpha)

- Keep checking a printer's top-level error code when an HTTP-success response also contains a neutral `error: 0` or `error: false` field. A file parser error can no longer be mistaken for a successful upload in this case.
- Validate the configured printer host before sending, reject blank or malformed addresses, and build consistent API URLs for both OctoPrint and Moonraker, including trailing slashes and reverse-proxy paths.
- Add regression tests for mixed printer response fields and printer URL construction; the real printer acceptance tests remain open.

## 0.8.208 (alpha)

- Inspect successful printer API responses for embedded errors before reporting an upload or print as complete. This covers OctoPrint-compatible, Moonraker upload, and Moonraker print-start responses.
- Present compact server error text and codes, including Anycubic's unknown-filament and file-parser responses, instead of dumping an entire JSON response.
- Catch synchronous request setup failures so the upload progress dialog closes with an error rather than remaining stuck.

## 0.8.207 (alpha)

- Read only relevant 3MF settings entries, cap their count and expanded size, and reject duplicate settings entries before parsing to reduce memory pressure from large or malformed archives.
- Limit expanded plate metadata to 8 MiB. When project metadata cannot be read, keep loading the geometry and show a warning rather than silently dropping the project settings.
- Name missing inherited process and filament profiles in the import warning so users can review the active values before slicing.

## 0.8.206 (alpha)

- Reject a send attempt cleanly if the sliced result has become unavailable, and ask the user to slice again instead of crashing on a missing preview result.
- Reconcile the prioritized feature and UI list with the current implementation; retain real print and device checks as open acceptance work.

## 0.8.197 (alpha)

- Hide the settings search and filter controls until the search button beside the profile selector is tapped in Print, Filament and Printer menus.
- Clear an active query and favorites filter when closing search, so hidden filters cannot leave an incomplete settings list.
- Move the long own-filament slot explanation behind an info button, keeping material mismatch warnings visible in a compact form.
- Verify the settings menus in portrait and landscape on Honor Magic7 Pro.

## 0.8.196 (alpha)

- On 3MF import, prefer temperatures from the user's saved filament profile with the same material for each tool; keep the 3MF model colors and do not alter saved profiles.
- Resolve stale slot-to-profile mappings by material, so a PETG slot cannot silently inherit ABS temperatures from an old mapping.
- Test the effective first-layer temperatures in actual G-code on an Honor device.

## 0.8.195 (alpha)

- Apply the filament editor's general bed temperature to every Orca plate surface in the temporary slice config, preventing a hidden Cool Plate value from lowering a print to 35 °C.
- Show the actual first-layer and later nozzle/bed temperatures for each used 3MF tool before slicing.
- Provide a direct Filament-menu action in that dialog and ask for slot confirmation again after temperature or profile changes.
- Compare the material recorded in the sliced G-code with the loaded ACE gate before direct printing. Missing or mismatched material information offers upload-only instead of starting the print.
- Reproduced the 35/215/205 °C values in a previously uploaded G-code file; no live print was started for this check.

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
