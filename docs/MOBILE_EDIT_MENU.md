# Mobile workspace edit menu

The compact Edit action surface no longer includes the generic “More actions” entry that opened a 12-action model dialog. The dialog-only actions are exposed directly in Edit: fill the plate, center the selection, reset rotation or scale, lay the model on a face, select all models, and delete all models. Delete all keeps an explicit confirmation step. Existing edit, transform, cut, paint, and profile workflows remain available.

The two plate-layout entries retain their separate menu locations and now have distinct labels: “Arrange all objects” and “Pack models on plate” (German: “Alle Objekte anordnen” and “Modelle auf Platte verteilen”). The Print, Filament, Printer, and App destinations already use the profile/category and grouped-settings layouts; this change leaves their existing navigation and settings behavior intact.

## Verification

- `:app:testDebugUnitTest` passed (500 tests).
- Kotlin compilation and Android resource processing passed as part of the test task.
- `:app:assembleDebug` is blocked by the missing native dependency `app/src/main/jniImports/oneTBB/lib/arm64-v8a/libtbb.a`.
