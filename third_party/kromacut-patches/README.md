# Kromacut source for the bundled editor

The APK bundles an Android-adapted Kromacut 4.2.0 editor. Its upstream source is commit [`94874755426ee7f33b3684a5544e7fab0e2e73f7`](https://github.com/vycdev/Kromacut/tree/94874755426ee7f33b3684a5544e7fab0e2e73f7), corresponding to [release v4.2.0](https://github.com/vycdev/Kromacut/releases/tag/v4.2.0). The integration patch and the Android bridge are maintained in this directory. Kromacut is AGPL-3.0-only; the upstream license is preserved in `KROMACUT-LICENSE.txt` and in the bundled assets.

To reconstruct the adapted source, check out the exact upstream commit, apply `android-integration.patch` with `git apply`, then copy `src/orcaMobile.ts` to `src/lib/orcaMobile.ts` and copy the pnpm lock and workspace files to the checkout root. Install dependencies with pnpm and run `pnpm run build`. The generated offline web application is bundled in `app/src/main/assets/kromacut-editor/`.

The patch adapts the mobile layout, local asset routing and Android file-export bridge. It does not change Kromacut's upstream license or authorship.

