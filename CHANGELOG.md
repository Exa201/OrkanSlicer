# Changelog

## 0.8.215 (alpha)

- Require explicit confirmation before slicing when an ACE slot's material differs from the model filament, while preserving manual slot assignments.
- Recheck material compatibility after assigning slots in the print dialog; a mismatched job can be uploaded without starting a print.
- Rebuild the active model mesh when the simplified-preview setting changes, so it takes effect immediately without altering slicing geometry.
- Add a regression test proving a second 3MF project does not retain colors, temperatures, edits or ACE mappings from the first.
- Allow two-line navigation labels for larger Android font settings; check the Honor layout at 130% system font size.
- Recheck the Kobra status endpoint; it still returns HTML rather than usable printer status JSON, so physical print verification remains open.

## 0.8.214 (alpha)

- Release native raycast trees and bed meshes when previews and build plates are recreated; this prevents retained memory after repeated model and printer changes.
- Simplify the displayed mesh of large, unpainted active models in performance mode while keeping the original geometry for slicing and export. Painting switches back to exact geometry.
- Explain when a printer API address returns an HTML page instead of confirming a file upload or print start; never report such a response as a started print.
- Reserve space for both actions in the pre-slice filament dialog so large-font phone layouts can scroll the content without hiding Cancel.
- Check the user Mewtwo 3MF on Honor and audit existing two-color G-code; the older Bulbasaur sample is incomplete and is explicitly excluded from print verification.
- Fix the Kotlin static JNI binding found by the Honor large-model test; retest a closed 259,200-triangle STL and the three-color Mewtwo project on device.

## 0.8.213 (alpha)

- Show the full selected printer name on the workspace card and allow longer profile names in the print, filament and printer selectors.
- Compact the filament-slot labels so more T-slots are visible on a phone without losing the material name.
- Add a regression case for two logical 3MF filaments with six nozzle-temperature variants, matching a user project layout.
- Validate an existing multicolor G-code sample for layer completeness, final extrusion and end order; inspect the printer and Honor portrait/landscape menus without starting a physical print.
- Save GPU memory in the default performance mode by drawing inactive plates as simple model bounds; rebuild their lightweight previews after an app switch without losing plate positions.
- Use 70% viewport resolution and a 25-layer initial G-code preview on Android low-RAM devices while preserving the complete slicing model and exported G-code. Drop obsolete object meshes after deletions.

## 0.8.212 (alpha)

- Validate OctoPrint file-upload responses instead of treating any `files` or `action` field as a successful print.
- Confirm the returned local file path and reject unrelated folder acknowledgements or unsafe paths.
- Distinguish a received file from one that OctoPrint is still processing (`done`).
- Respect `effectiveSelect` and `effectivePrint`: a requested print is reported as started only when OctoPrint confirms it.
- Show clear German and English messages when a file is selected but not started, or when the print start remains unconfirmed; add protocol regression tests.
- Refresh the in-app release notes, which still showed version 0.8.202 on startup.

## 0.8.211 (alpha)

- Treat printer responses with `success: false` as rejected requests, even when HTTP and the top-level code report success.
- Require a confirmed Moonraker upload before starting a print. Use the file path acknowledged by Moonraker rather than assuming the requested filename was stored unchanged.
- Respect `print_started` and `print_queued` upload flags, avoiding a duplicate start request and showing the actual printer state.
- Require an explicit `ok` response to the Moonraker print-start request before reporting that the print started. Unknown responses ask the user to check the printer state instead of silently claiming success.
- Add regression cases for malformed acknowledgements, unsafe paths, queued jobs and print-start responses.

## 0.8.210 (alpha)

- Expose the native Orca bridge line width and relative bridge angle settings together with the existing bridge controls; move bridge flow and density into the same group so they appear only once.
- Show bridge line width as linked nozzle-relative percent and millimeter fields, capped at 100% to match the native slicer's validation. Add a compact Auto (0) action to the paired line-width editor.
- Add German text for the bridge-width control and explanation, and verify the actual localization catalog through a unit test.
- Run project-settings regression checks against the bundled two-color 3MF and a real Orca Benchy 3MF; confirm filament colors and temperatures remain separate while printer overrides are ignored.

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
