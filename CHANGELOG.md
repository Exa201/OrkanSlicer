# Changelog

## 0.8.268 (alpha)

- Show file and profile import failures in the Compose workspace dialog while the Bed workspace is active.
- Preserve the platform alert fallback outside the workspace and leave profile import, model loading, and cleanup behavior unchanged.
- Add Honor coverage for the actual unsupported-file `loadFile` route and Compose dismissal.

## 0.8.267 (alpha)

- Host the GL-triggered prime-tower position editor in the exclusive Compose workspace dialog.
- Keep the active plate offset, pending profile edits, render refresh, Cancel behavior, and keyboard-safe scrolling.
- Add Honor coverage for Cancel, Android Back, and explicit Apply through the BedFragment workspace route.

## 0.8.266 (alpha)

- Show slice preparation, printer upload/start, G-code integrity, and Anycubic mapping failures in the shared Compose workspace dialog.
- Preserve main-thread delivery, upload cleanup, integrity validation, and all existing print-start safeguards.
- Add Honor coverage for the real slice preparation failure route and Compose Back dismissal.

## 0.8.265 (alpha)

- Show Kromacut file-picker and export failures in the existing Compose tool panel.
- Keep the active WebView, file chooser callback, temporary-file cleanup, and generated-file handoff unchanged.
- Allow long error details to scroll inside the dialog and add Honor UI coverage.

## 0.8.264 (alpha)

- Move the app-settings reset confirmation and converter-folder-picker error to Compose.
- Preserve the explicit reset confirmation boundary; Cancel and Back cannot reset settings.
- Add Honor UI coverage for reset confirmation and folder-picker error dismissal.

## 0.8.263 (alpha)

- Render MMU filament-sync errors in the print-profile screen with the existing Compose message dialog.
- Keep sync callbacks on the UI thread and ignore results from a profile screen that has already been destroyed.

## 0.8.262 (alpha)

- Move process, printer, and filament profile option-help dialogs and their favorite actions into Compose.
- Replace the app-settings theme, render-scale, and text-scale choice dialogs with one scrollable Compose selection component.
- Keep preference persistence, theme invalidation, and selection/cancel behavior; add Honor interaction coverage for help favorites and long settings lists.

## 0.8.261 (alpha)

- Move the print-profile flush-volume matrix and prime-tower position editors into Compose dialogs hosted by the profile screen.
- Preserve input validation, locale-aware decimals, per-plate coordinates, pending profile updates, and explicit Apply/Cancel behavior.
- Add Honor coverage for both dialog validation and actual profile-screen actions without persisting cancelled edits.

## 0.8.260 (alpha)

- Move Smart Expert Slice setup, cancellable analysis progress, and result actions into the exclusive Compose workspace dialog host.
- Keep GL geometry analysis, background profile analysis, model-revision checks, cancellation, and explicit apply/slice actions intact.
- Use an adaptive action flow for the result dialog and migrate MMU synchronization failure feedback to the workspace message dialog.
- Add Honor UI tests for validation, Cancel, Back, progress cancellation, and result dismissal without running analysis or slicing.

## 0.8.259 (alpha)

- Move Quick Slice into the exclusive Compose workspace dialog host; keep all profile validation and persistence behind the explicit Slice action.
- Use the workspace message dialog for calibration-model load errors.
- Test invalid quick-slice values, corrected values, Cancel, and Android Back without starting a slice.

## 0.8.258 (alpha)

- Move snackbar state and rendering into the Compose workspace instead of attaching a separate Android overlay view.
- Preserve tagged progress replacement, timed dismissal, swipe-to-dismiss policy, and actionable snackbar callbacks.
- Position workspace notifications around the navigation and tool panels without shrinking the build-plate viewport.
- Add controller-state unit coverage and Honor Compose UI tests for tagged progress, button context, and expiry.

## 0.8.257 (alpha)

- Render generic workspace confirmation and message dialogs from the exclusive Compose workspace panel.
- Focus the duplicate-count field on open and handle Android Back by hiding the IME before dismissing the dialog.

## 0.8.256 (alpha)

- Remove the unused AndroidView fallback and legacy View-based overlay from the workspace tool host; tool routes now require Compose content.
- Remove the unreferenced legacy WebViewMenu. Kromacut's active embedded WebView, editor, file chooser, and download flow remain intact.
- Keep real Compose tool lifecycle, Back cleanup, responsive panel, and outside-dismiss device coverage on the production workspace host.

## 0.8.255 (alpha)

- Profile-settings favorites filter now announces its enabled/disabled state to screen readers in English, German, and Russian.
- Added Honor coverage for the favorites toggle, search input, and profile-level picker at 320 dp and 150% text scale.

## 0.8.254 (alpha)

- App-setting sections now announce their expanded or collapsed state to screen readers.
- Added Honor coverage for settings section state, touch size, and an actual theme-setting action across 320/360/400 dp portrait and 640×320 dp landscape at 100/130/150% Compose font scales.

## 0.8.253 (alpha)

- Maker section controls now announce whether they expand or directly open a specific action.
- Maker generator rows meet the 48 dp touch minimum and are tested at 320 dp width with 150% text scale.

## 0.8.252 (alpha)

- The workspace menu host now requires all six primary destinations to provide Compose content directly; it no longer silently falls back to mounting an Android menu view.
- Unknown workspace destinations fail explicitly instead of being routed to the Project menu.

## 0.8.251 (alpha)

- The build-plate picker Cancel action now has a Material 3 touch target that meets the 48 dp accessibility minimum.
- Build-plate selection cards expose distinct TalkBack labels and are verified through 1/3/6/10/20-plate UI fixtures.

## 0.8.250 (alpha)

- Added a labeled, accessible 48 dp control to reveal the remaining ACE physical slots when the printer slot row overflows; it does not alter hardware slot mappings.

## 0.8.249 (alpha)

- Render project settings, profile status, filament access, plate selection, and printer-web access directly in `WorkspaceShell` Compose content instead of separate AndroidView-wrapped ComposeViews.
- Keep the GLView host stable across workspace navigation and verify that it is not recreated while switching all six main-menu routes on Honor.

## 0.8.248 (alpha)

- Render all six workspace main-menu destinations directly in the shared Compose `WorkspaceShell`; retain legacy view rendering only as a compatibility fallback.
- Tie list-menu EventBus listeners to the active Compose destination lifecycle and verify Project, Model, Camera, Edit, Tools and Slice navigation on Honor without allocating legacy menu views.

## 0.8.247 (alpha)

- Render the camera and model-edit menus directly in the Compose `WorkspaceShell` instead of allocating their legacy menu views.
- Preserve transform-menu model-selection updates with a Compose-scoped event-bus subscription; verify both routes and legacy-view absence on Honor.

## 0.8.246 (alpha)

- Complete the plate-switch callback after activating an already-loaded plate, on the GL thread, matching lazy-loaded plate completion.
- Test switching between both loaded plates in the real two-plate 3MF workspace flow.
- Exercise filament rows and two-row tool actions at 320/360/400 dp with 100/130/150% Compose font scales; verify reachability and 48 dp touch bounds on Honor.

## 0.8.245 (alpha)

- Ensure every measurement-panel exit path releases the renderer's measurement state and points.
- Verify loaded-model paint cancellation and measurement-mode cleanup on Honor PTP-N49 / Android 16.

- Render the model context menu through Compose workspace overlay state instead of attaching a separate `ComposeView` host from `BedFragment`; preserve exclusive-panel and Back dismissal behavior.
- Verify Compose overlay content replacement and cleanup in `WorkspaceController` tests and the Honor context-menu dismissal test. Keep the existing dialog hosts until focused-input Back behavior is proven on device.
- Render the Kromacut close toolbar in the Compose workspace route while retaining its embedded WebView, bundled asset loading, file chooser, and download bridge. Remove the unused second `MainActivity` UnfoldMenu/back-state host.
- Verify all ten direct Compose workspace tool routes on Honor, including Kromacut's attached WebView and its restricted file-access settings.
- Load the bundled two-color 3MF on Honor, start a GL paint session, cancel with Android Back, and verify the renderer exits paint mode without changing the model.

## 0.8.244 (alpha)

- Move workspace menu route transitions into `WorkspaceShell` with Compose `AnimatedContent`; keep the GL renderer host stable.
- Render transform, cut, calibration, slice-information, layer-preview, and AI-generator tool routes directly in the Compose workspace panel while preserving callbacks, GL-thread operations, and Android activity-result flows.
- Verify nine Compose tool routes on Honor for attach, active composition, and Back cleanup; keep Kromacut's required WebView/editor integration on its existing route.

## 0.8.243 (alpha)

- Add per-object names, T0-based tool, material and color to the model list, with direct filament assignment.
- Preserve painted facet assignments across OrkanSlicer 3MF export and import. Accept both `paint_color` and `slic3rpe:mmu_segmentation`, and keep paint selectors aligned with the annotated volume mesh.
- Add a device regression test for paint, export, import and restored filament assignments.
- Continue the Compose workspace migration with shared panels and dialogs, clearer model actions, and responsive navigation. This is an incremental migration; legacy tool hosts remain.
- Warn before exporting a project with multiple plates because the current 3MF export includes only the active plate.
- Virtualize the compact filament palette and verify scrolling and selection with 1, 4, 8, 16 and 24 UI entries. The 24-entry case is synthetic; supported ACE capacity is unchanged.
- Keep fixed T0/T1/T2/T3-to-ACE slot mapping, including empty slots, and retain Moonraker upload/print-start safeguards.

## 0.8.242 (alpha)

- Separate first-layer and other-layer temperatures for six Orca bed surfaces.
- Select the active bed surface in printer settings and preview its temperatures before slicing.
- Preserve existing surface values and backfill only missing legacy values.

## 0.8.241 (alpha)

- Preserve painted facet assignments when reopening an OrkanSlicer 3MF; the Orca/Bambu importer now accepts both supported paint attribute names.
- Keep paint selection indices aligned with the annotated mesh for multi-volume objects.
- Verify painting and 3MF export/import on the Honor device.

## 0.8.240 (alpha)

- Commit color, support, seam, and fuzzy painting before refreshing the workspace model history, so undo and redo operate on the completed painted model.
- Make paint tools and filament swatches at least 48 dp tall, expose their active state and accessible action labels, and announce whether painting is paused or active.

## 0.8.239 (alpha)

- Open the complete filament palette from the compact workspace color button. Phones use an expanded bottom sheet; wide windows use a side panel.
- Reuse the existing filament and ACE slot controller for palette editing, then open the existing model assignment flow from the panel.
- Clarify compact model actions: filling the plate makes copies, while arranging repositions the models already on the plate. Keep the model action grid in two rows.

## 0.8.232 (alpha)

- Keep the saved filament temperatures and pressure-advance values aligned with their physical ACE gates when an empty gate makes the model palette compact (for example, ACE 1, 3 and 4). Changing a model tool's gate now selects the corresponding saved palette profile instead of treating the gate number as a palette index.
- Preserve the physical gate order of an explicit full sync independently of later tool assignments and keep that order aligned when a palette entry is deleted.
- When importing a 3MF with the user's own filament settings, reject an obsolete slot profile whose material differs from the slot's current material. Verify a real three-color 3MF import and complete slice on the Honor test app.

## 0.8.231 (alpha)

- Refresh the Kobra 3 V2 ACE state once more immediately before preparing an upload. A failed final check stops the transfer.
- Compare the used gates, their printer-tool mapping, material and color with the state approved in the send dialog. If any used slot changed while the dialog was open, stop and ask the user to review the assignment again.
- Exercise the final-refresh behavior against simulated printer responses on an Android device, including a slot becoming empty and a color changing while the slot remains loaded.

## 0.8.230 (alpha)

- Warn before printing when several used model tools point to the same physical ACE gate: the dialog now shows the reduced count of printable filament colors and offers direct assignment editing, upload only, or explicit confirmation for an intentional shared-slot print.
- Verify the current Kobra 3 V2 ACE state read-only; its second slot remains empty. The printer's Rinkhals version is not exposed by the available status API, so a physical three-color print with that gap still requires firmware verification and supervised testing.

## 0.8.229 (alpha)

- Show the complete physical ACE slot row in the final send dialog, so a gap remains visible immediately before upload or print.
- Recheck every used model color against the freshly synchronized, loaded and firmware-routable ACE slots before sending. If a selected slot became empty or lost its tool mapping, stop and offer to edit the assignment.
- Exercise the complete three-color Anycubic G-code rewrite with an empty second gate: tool selections route to T0/T2/T3, metadata keeps physical positions, and the final layer and end command remain present.

## 0.8.228 (alpha)

- Show every physical ACE gate after filament sync, including an empty gate between loaded ones. The overview distinguishes printer tool numbers from the slicer's compact model-color palette and shows each model color's ACE assignment.
- Keep empty gates unavailable for assignment while retaining their positions. Regression checks cover ACE gates `1,0,1,1` and the corresponding T0→ACE 1, T1→ACE 3, T2→ACE 4 G-code routing.

## 0.8.227 (alpha)

- Stop large-model paint sessions before full-resolution mesh and facet overlays are allocated. Offer an explicit simplification choice; cancel keeps the print geometry unchanged.
- Apply the same early choice to embossed text. The renderer also refuses an oversized paint session when invoked through another route.
- Keep the surface-tool threshold in one place and test the suggested reduction for a dense KromaCut-size model.
- Retry a consented reduction when the native simplifier remains above the safe threshold; report honestly if it still cannot reach the limit. Ignore queued simplification after its model is no longer active.

## 0.8.226 (alpha)

- Keep large models selectable while preventing approximate box hits from placing paint, embossed text, or measurement points on the wrong surface. Show a localized explanation when precise surface tools need a simpler model.
- Clear cached selection data whenever a GL model is rebuilt for another mesh.
- Give plate cards more room and use the scrollable grid for large system fonts; keep narrow navigation labels whole, shorten the German 3MF save label, and allow long printer names another line when text is enlarged.

## 0.8.225 (alpha)

- Avoid the native face-neighbour raycast index when selecting dense KromaCut and other models above 100,000 triangles. Taps use the model's current approximate bounds; slicing geometry stays unchanged.
- Verify the large-model tap path on Xiaomi 13 after centering a real KromaCut 3MF model (about 1.38 million triangles): repeated taps on the model and empty plate no longer crash. Keep the plate-picker improvements from 0.8.224.

## 0.8.224 (alpha)

- Use the correct singular and plural object labels in the build-plate picker.
- Refresh the inexpensive plate footprints whenever the picker opens, so a moved or scaled model does not leave an old miniature behind.

## 0.8.223 (alpha)

- Refresh the plate object count and invalidate its thumbnail cache only after a model import actually commits to the active native model. This fixes a stale "0 objects" label observed after keeping the original geometry of a 502,002-triangle STL.
- Recheck the large-model import consent on Honor with a closed 518,400-triangle STL. Both "keep original" and "simplify" render the model and show one object in the plate picker. A cold restart opens an empty plate while retaining the selected printer.

## 0.8.222 (alpha)

- Keep the compact plate picker usable on narrow and short screens: show the circular layout only when it fits, otherwise use the scrollable grid. Generate its miniature model footprints from inexpensive approximate bounds without changing slice geometry.
- Move duplicate, move-to-plate, delete and further model actions to the front of the Edit menu. Ask before deleting the selected model.
- Share menu and navigation sizing tokens, create one-row phone menus lazily, and choose readable text on the selected accent color in both themes.
- Audit the existing import simplification gate and plate state handling; retain their confirmation and preservation behavior. Add a plate layout regression test and document remaining device and low-memory checks.

## 0.8.221 (alpha)

- Ask for the target printer after opening a 3MF and let the user independently choose file print settings, filament settings, or an exactly matching installed printer profile. Own profiles are the default; raw machine commands and connection details from the file are never imported.
- Keep model colors visible when the user retains own filament values. A selected file filament profile, including temperatures and pressure advance, is applied only to the current model; saved profiles remain unchanged.
- Clear the previous project's temporary settings before showing the new choice, including when the dialog is cancelled. Add selection and reader regression tests.

## 0.8.220 (alpha)

- Require the Moonraker upload acknowledgement to identify the same filename that the app sent; an unrelated stored path can no longer be started.
- Give Moonraker print-start requests a separate 30-second timeout with no automatic retry. On a missing start confirmation, direct the user to check printer status before resending because the first request may already have started a print.
- Bound printer error-response parsing to 256 KiB and inspect only a short prefix when identifying an HTML page from the wrong API endpoint.
- Exercise the actual workspace's lazy switch between two 3MF plates on Android, including each plate's own object and filament.
- Recheck the Kobra host read-only: Moonraker and OctoPrint-compatible information endpoints respond, the last job is complete, and ACE slot 2 remains empty. No firmware or physical print was changed.

## 0.8.219 (alpha)

- Bound QIDI Q2 tool metadata to 16 filaments and reject malformed or out-of-range tool numbers before allocating package metadata. Stop retaining embedded thumbnail text during the first G-code pass.
- Verify every QIDI package preview, not only the primary image, and limit the checksum field before reading it.
- Keep an unfinished OctoPrint upload in the processing state even if a print was requested; do not claim a final print result before the server finishes processing.
- Count real 3MF plate elements while ignoring XML comments and CDATA. Verify separate objects and filament assignments on a generated two-plate project with the native Android reader.
- Recheck main navigation and open screens at 130% system font size on Honor; collect a device memory baseline. Live QIDI/OctoPrint printing and the physical Kobra multicolor run remain open.

## 0.8.218 (alpha)

- After a printer switch in the send dialog, mark the previous slice's filament and G-code values as stale and label the action "Slice again and check" until a new slice is ready.
- Ignore orphan 3MF thumbnail images when counting build plates; use declared plate elements or actual plate data, with a mobile-safe upper bound.
- Validate complete QIDI preview PNG data and CRCs before creating or accepting a print package, with a size limit for embedded thumbnails.
- Report an OctoPrint-confirmed print start even when the app requested upload-only, so unexpected remote behavior is visible rather than silently labelled as an upload.
- Recheck the Kobra ACE state read-only, add regressions for phantom plates and broken thumbnails, and run Android device checks. Physical multicolor printing and firmware changes remain open.

## 0.8.217 (alpha)

- Inspect the real G-code tool order in the print dialog, including three-color jobs whose first-use order differs from the logical tool numbers.
- Warn before starting an Anycubic ACE print when an empty physical slot precedes a selected slot. Older Rinkhals firmware can map sparse slots to the wrong color; the dialog offers upload-only or an explicit firmware-checked start.
- Show the configured physical slot even when it is empty, instead of silently displaying another loaded slot in the mapping summary.
- After switching printers in the send dialog, activate the selected profile, slice again, and reopen preparation with the new file and refreshed slot state before any upload.
- Add regression checks for the observed three-color Kobra assignment, tool order, contiguous slots and one-color jobs. A physical print remains to be verified after the printer firmware is corrected.

## 0.8.216 (alpha)

- Preserve and apply the selected own-filament pressure advance when importing a 3MF project, alongside own temperature settings. Editing a positive PA value enables it for that filament; the enable switch can still be turned off explicitly.
- Inspect executable pressure-advance commands in the finished G-code before upload. The print dialog distinguishes slicer-generated filament values from custom start-code commands and shows the selected printer host.
- Verify on the Kobra 3 V2 Moonraker endpoint that Klipper accepts `SET_PRESSURE_ADVANCE` and reports the changed value; restore the previous live value after the read-only workflow check. A physical print was not started.

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
