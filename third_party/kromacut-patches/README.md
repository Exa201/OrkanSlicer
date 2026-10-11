# Kromacut source for the bundled editor

The APK bundles an Android-adapted Kromacut 4.0.0 editor. Its corresponding source is the upstream [Kromacut commit `d5d8c490e68b96a015fa29e2508dca53076ca69e`](https://github.com/vycdev/Kromacut/tree/d5d8c490e68b96a015fa29e2508dca53076ca69e) plus the files in this directory. Both upstream and the integration changes are AGPL-3.0-only.

To reconstruct the modified source, clone Kromacut, check out that exact commit, apply `android-integration.patch` with `git apply`, then copy this directory's `src/lib/orcaMobile.ts`, `pnpm-lock.yaml` and `pnpm-workspace.yaml` to the same relative paths in the checkout. Install dependencies according to upstream's `package.json` and build the web app with Vite. The resulting offline web assets are bundled in `app/src/main/assets/kromacut-editor/` in this repository.

This patch changes the editor layout and mobile bridge. It does not change Kromacut's original license or authorship.
