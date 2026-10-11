#!/usr/bin/env python3
"""Check that the Maker screen uses Compose and retains every existing tool route."""
from pathlib import Path
from collections import Counter
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/ru/ytkab0bp/slicebeam"


def check_german_android_resource_coverage() -> None:
    """Ensure German strings, arrays and plurals cover English resources safely."""
    source_path = ROOT / "app/src/main/res/values/strings.xml"
    german_path = ROOT / "app/src/main/res/values-de/strings.xml"
    source_root = ET.parse(source_path).getroot()
    german_root = ET.parse(german_path).getroot()
    supported_tags = {"string", "string-array", "plurals"}
    format_token = re.compile(r"%(?:\d+\$)?[-#+0,(<]*\d*(?:\.\d+)?[tT]?[a-zA-Z%]")

    def resource_map(root: ET.Element, locale: str) -> dict[tuple[str, str], ET.Element]:
        resources: dict[tuple[str, str], ET.Element] = {}
        for element in root:
            if element.tag not in supported_tags or element.attrib.get("translatable") == "false":
                continue
            name = element.attrib.get("name")
            if not name:
                raise AssertionError(f"Unnamed {element.tag} resource in {locale}")
            key = (element.tag, name)
            if key in resources:
                raise AssertionError(f"Duplicate {element.tag} resource {name} in {locale}")
            resources[key] = element
        return resources

    source_resources = resource_map(source_root, "values")
    german_resources = resource_map(german_root, "values-de")
    missing = sorted(set(source_resources) - set(german_resources))
    if missing:
        raise AssertionError("German translations are missing: " + ", ".join(f"{tag}/{name}" for tag, name in missing))

    def localized_text(element: ET.Element) -> str:
        return "".join(element.itertext())

    def assert_format_tokens(source: ET.Element, german: ET.Element, label: str) -> None:
        source_tokens = Counter(format_token.findall(localized_text(source)))
        german_tokens = Counter(format_token.findall(localized_text(german)))
        if source_tokens != german_tokens:
            raise AssertionError(
                f"German format placeholders differ for {label}: "
                f"expected {dict(source_tokens)}, found {dict(german_tokens)}"
            )

    for key, source in source_resources.items():
        german = german_resources[key]
        label = f"{key[0]}/{key[1]}"
        if key[0] == "string":
            assert_format_tokens(source, german, label)
        elif key[0] == "string-array":
            source_items = source.findall("item")
            german_items = german.findall("item")
            if len(source_items) != len(german_items):
                raise AssertionError(
                    f"German option count differs for {label}: "
                    f"expected {len(source_items)}, found {len(german_items)}"
                )
            for index, (source_item, german_item) in enumerate(zip(source_items, german_items)):
                assert_format_tokens(source_item, german_item, f"{label}[{index}]")
        else:
            source_items = {item.attrib.get("quantity"): item for item in source.findall("item")}
            german_items = {item.attrib.get("quantity"): item for item in german.findall("item")}
            if source_items.keys() != german_items.keys():
                raise AssertionError(
                    f"German plural categories differ for {label}: "
                    f"expected {sorted(source_items)}, found {sorted(german_items)}"
                )
            for quantity, source_item in source_items.items():
                assert_format_tokens(source_item, german_items[quantity], f"{label}[{quantity}]")


def check_multilingual_plural_contracts() -> None:
    """Keep localized count messages correct for English, German, and Russian."""
    required_plural_categories = {
        "values": {"one", "other"},
        "values-de": {"one", "other"},
        "values-ru": {"one", "few", "many", "other"},
    }
    names = (
        "MenuFileImportProfilesWarningsMore",
        "MenuFileImportProfilesImportedCount",
        "MenuFileImportProfilesSkippedCount",
        "GCodeIntegrityLayerMismatch",
        "GCodeIntegrityLayerStarts",
        "ProfileSettingsPending",
    )
    format_token = re.compile(r"%(?:\d+\$)?[-#+0,(<]*\d*(?:\.\d+)?[tT]?[a-zA-Z%]")
    source_root = ET.parse(ROOT / "app/src/main/res/values/strings.xml").getroot()
    source_resources = {
        element.attrib.get("name"): element
        for element in source_root.findall("plurals")
    }
    missing_source = sorted(set(names) - set(source_resources))
    if missing_source:
        raise AssertionError("Required English plural resources are missing: " + ", ".join(missing_source))

    for locale, categories in required_plural_categories.items():
        root = ET.parse(ROOT / f"app/src/main/res/{locale}/strings.xml").getroot()
        localized_resources = {
            element.attrib.get("name"): element
            for element in root.findall("plurals")
        }
        for name in names:
            source_items = {
                item.attrib.get("quantity"): item
                for item in source_resources[name].findall("item")
            }
            localized = localized_resources.get(name)
            if localized is None:
                raise AssertionError(f"Plural {name} is missing from {locale}")
            localized_items = {
                item.attrib.get("quantity"): item
                for item in localized.findall("item")
            }
            if set(localized_items) != categories:
                raise AssertionError(
                    f"Plural categories for {name} differ in {locale}: "
                    f"expected {sorted(categories)}, found {sorted(localized_items)}"
                )
            expected_tokens = Counter(format_token.findall("".join(source_items["other"].itertext())))
            for quantity, item in localized_items.items():
                actual_tokens = Counter(format_token.findall("".join(item.itertext())))
                if actual_tokens != expected_tokens:
                    raise AssertionError(
                        f"Plural placeholders differ for {name}/{quantity} in {locale}: "
                        f"expected {dict(expected_tokens)}, found {dict(actual_tokens)}"
                    )


def check_measurement_and_pending_ui_localization() -> None:
    """Keep the Compose measurement panel localized and scoped to its visible lifecycle."""
    measure_path = JAVA / "ui/workspace/MeasureModePanel.kt"
    measure_source = measure_path.read_text(encoding="utf-8")
    for resource in ("MeasureDeltaX", "MeasureDeltaY", "MeasureDeltaZ", "MeasureValueMillimeters"):
        if f"R.string.{resource}" not in measure_source:
            raise AssertionError(f"Measurement UI does not use the localized {resource} resource")
    if '"ΔX:"' in measure_source or '"ΔY:"' in measure_source or '"ΔZ:"' in measure_source:
        raise AssertionError("Measurement axis labels are hard-coded instead of using Android resources")
    if 'format.format(abs(pointA.x - pointB.x)) + " mm"' in measure_source:
        raise AssertionError("Measurement units are hard-coded instead of using the localized millimeter resource")
    for required in (
        "DisposableEffect(glView)",
        "SliceBeam.EVENT_BUS.registerListener(eventBridge)",
        "SliceBeam.EVENT_BUS.unregisterListener(eventBridge)",
    ):
        if required not in measure_source:
            raise AssertionError(f"Compose measurement panel is missing lifecycle behavior: {required}")
    if (JAVA / "view/MeasureModeView.kt").exists():
        raise AssertionError("The legacy measurement overlay should be removed after the Compose migration")

    pending_source = (JAVA / "ui/settings/ComposeProfileSettingsStatus.kt").read_text(encoding="utf-8")
    if "pluralStringResource(" not in pending_source or "R.plurals.ProfileSettingsPending" not in pending_source:
        raise AssertionError("Unsaved profile changes are not displayed with a localized plural resource")
    if "R.string.ProfileSettingsSaved" in pending_source or "if (state.pendingCount <= 0) return" not in pending_source:
        raise AssertionError("The profile screen should only show the status card while edits are pending")


def check_touch_regressions_use_the_production_gate() -> None:
    """Reject placeholder E2E assertions for the plate/model tap bug."""
    suite = (ROOT / "scripts/tests/E2ETestSuite.java").read_text(encoding="utf-8")
    if "import ru.ytkab0bp.slicebeam.view.TapGestureGate;" not in suite:
        raise AssertionError("Touch E2E regressions must exercise the production TapGestureGate")
    required_tests = (
        "testLongPressOnModelDoesNotOpenMenu",
        "testLongPressOnBedDoesNotOpenMenu",
        "testRepeatedBuildPlateTapsInTheSameSpotAreCoalesced",
    )
    for test_name in required_tests:
        if f"void {test_name}()" not in suite:
            raise AssertionError(f"The plate/model touch regression is missing: {test_name}")
    if "boolean menuTriggered = false;" in suite or "boolean bedMenuTriggered = false;" in suite:
        raise AssertionError("A touch regression uses a hard-coded false instead of the gesture gate")
    if "gate.finish(1, false, false" not in suite:
        raise AssertionError("Touch regression tests do not call the production tap acceptance logic")


def check_crash_recovery_screen_uses_compose() -> None:
    """Keep the recovery screen independent from the legacy view tree and profile theme."""
    activity = (JAVA / "SafeStartActivity.kt").read_text(encoding="utf-8")
    screen = (JAVA / "ui/SafeStartScreen.kt").read_text(encoding="utf-8")
    device_test = (ROOT / "app/src/androidTest/java/ru/ytkab0bp/slicebeam/SafeStartComposeDeviceTest.kt").read_text(encoding="utf-8")
    for contract in (
        "ComposeView(this)",
        "SafeStartScreen(",
        "Intent.createChooser(sendIntent, null)",
        'remove("crash")',
        "MainActivity::class.java",
    ):
        if contract not in activity:
            raise AssertionError(f"The crash recovery activity lost its recovery action: {contract}")
    for contract in (
        "fun SafeStartScreen(",
        "isSystemInDarkTheme()",
        "safeDrawingPadding()",
        "verticalScroll(rememberScrollState())",
        "onClick = onShare",
        "onClick = onRestart",
        "R.string.AppCrashedShare",
        "R.string.AppCrashedRestart",
    ):
        if contract not in screen:
            raise AssertionError(f"The Compose crash recovery screen is missing: {contract}")
    if any(token in screen for token in ("ThemesRepo", "SliceBeam.CONFIG", "PrinterConfig")):
        raise AssertionError("The crash recovery screen must not depend on profile or application theme loading")
    if "recoveryScreenRendersAfterStoredCrashWithoutOpeningWorkspace" not in device_test:
        raise AssertionError("The crash recovery route lacks an Android runtime regression test")


def check_main_activity_uses_one_back_dispatch_route() -> None:
    """Keep system back handling on AndroidX so gesture and key navigation share one route."""
    activity = (JAVA / "MainActivity.kt").read_text(encoding="utf-8")
    device_test = (ROOT / "app/src/androidTest/java/ru/ytkab0bp/slicebeam/MainActivityBackDeviceTest.kt").read_text(encoding="utf-8")
    result_registry_test = device_test
    if "onBackPressedDispatcher.addCallback(this" not in activity:
        raise AssertionError("MainActivity must register its root back behavior with AndroidX")
    if "override fun onBackPressed()" in activity:
        raise AssertionError("MainActivity must not bypass AndroidX with a deprecated back override")
    for contract in (
        "dispatcherBackShowsOneExitConfirmationWithoutFinishingActivity",
        "activity.onBackPressedDispatcher.onBackPressed()",
        "switchToSlot(0)",
        "exitConfirmationDialog",
        "Repeated back input must not stack a second exit dialog",
        "AppCloseConfirmTitle",
        "AppCloseConfirmMessage",
    ):
        if contract not in device_test:
            raise AssertionError(f"The Android back-dispatch regression test is missing: {contract}")
    navigation = (JAVA / "navigation/NavigationDelegate.kt").read_text(encoding="utf-8")
    if "if (slot == currentSlot && activeContainer.childCount > 0)" not in navigation:
        raise AssertionError("Selecting the active navigation slot must not reattach its existing view")
    setup = (JAVA / "SetupActivity.kt").read_text(encoding="utf-8")
    setup_test = (ROOT / "app/src/androidTest/java/ru/ytkab0bp/slicebeam/SetupBoostyComposeDeviceTest.kt").read_text(encoding="utf-8")
    if "onBackPressedDispatcher.addCallback(this" not in setup or "override fun onBackPressed()" in setup:
        raise AssertionError("The setup pager must use AndroidX back dispatch without a deprecated override")
    if "activity.onBackPressedDispatcher.onBackPressed()" not in setup_test:
        raise AssertionError("The setup pager lacks an AndroidX back-navigation device regression")

    if "ActivityResultContracts.StartActivityForResult()," not in activity or "registerActivityResultLaunchers()" not in activity:
        raise AssertionError("MainActivity must register lifecycle-aware request-code launchers")
    if "private fun dispatchActivityResult(requestCode:Int, resultCode:Int, data:Intent?)" not in activity:
        raise AssertionError("MainActivity must preserve the existing result-code dispatcher")
    if "override fun onActivityResult(" in activity or "startActivityForResult(" in activity:
        raise AssertionError("MainActivity still uses deprecated activity-result APIs")
    request_block = activity.split("private val ACTIVITY_RESULT_REQUEST_CODES = intArrayOf(", 1)[1].split(")", 1)[0]
    for request_code in (
        "REQUEST_CODE_OPEN_FILE",
        "REQUEST_CODE_EXPORT_GCODE",
        "REQUEST_CODE_IMPORT_PROFILES",
        "REQUEST_CODE_EXPORT_PROFILES",
        "REQUEST_CODE_EXPORT_3MF",
        "REQUEST_CODE_AI_GENERATOR_TAKE_PHOTO",
        "REQUEST_CODE_AI_GENERATOR_CHOOSE_PHOTO",
        "REQUEST_CODE_IMAGE_TO_STL",
        "REQUEST_CODE_SAVE_CONVERTED_FILE",
        "REQUEST_CODE_CONVERTER_ROOT_FIRST_LAUNCH",
        "REQUEST_CODE_CONVERTER_ROOT_SETTINGS",
        "REQUEST_CODE_CONVERTER_ROOT_SAVE",
        "KromacutEditorMenu.REQUEST_CODE_FILE",
    ):
        if request_code not in request_block:
            raise AssertionError(f"MainActivity has no lifecycle-aware launcher for {request_code}")
    legacy_result_sources = []
    for source_path in JAVA.rglob("*.kt"):
        source_text = source_path.read_text(encoding="utf-8")
        if "startActivityForResult(" in source_text or "override fun onActivityResult(" in source_text:
            legacy_result_sources.append(source_path.relative_to(JAVA).as_posix())
    if legacy_result_sources:
        raise AssertionError("Deprecated Activity Result APIs remain in app production Kotlin: " + ", ".join(legacy_result_sources))
    for request_code in (
        "REQUEST_CODE_OPEN_FILE",
        "REQUEST_CODE_EXPORT_GCODE",
        "REQUEST_CODE_IMPORT_PROFILES",
        "REQUEST_CODE_EXPORT_PROFILES",
        "REQUEST_CODE_EXPORT_3MF",
        "REQUEST_CODE_AI_GENERATOR_TAKE_PHOTO",
        "REQUEST_CODE_AI_GENERATOR_CHOOSE_PHOTO",
        "REQUEST_CODE_IMAGE_TO_STL",
        "REQUEST_CODE_SAVE_CONVERTED_FILE",
        "REQUEST_CODE_CONVERTER_ROOT_FIRST_LAUNCH",
        "REQUEST_CODE_CONVERTER_ROOT_SETTINGS",
        "REQUEST_CODE_CONVERTER_ROOT_SAVE",
        "KromacutEditorMenu.REQUEST_CODE_FILE",
    ):
        if request_code not in result_registry_test:
            raise AssertionError(f"The Android result-registry test does not verify {request_code}")
    for contract in (
        "ActivityResultContracts.StartActivityForResult()",
        "profileImportLauncher.launch(intent)",
        "SetupProfilePickerResult.shouldImport(result.resultCode, Activity.RESULT_OK, uri != null)",
    ):
        if contract not in setup:
            raise AssertionError(f"SetupActivity is missing its lifecycle-safe profile picker contract: {contract}")
    if "override fun onActivityResult(" in setup or "startActivityForResult(" in setup:
        raise AssertionError("SetupActivity still uses the deprecated activity-result callbacks")


def main() -> None:
    check_german_android_resource_coverage()
    check_multilingual_plural_contracts()
    check_measurement_and_pending_ui_localization()
    check_touch_regressions_use_the_production_gate()
    check_crash_recovery_screen_uses_compose()
    check_main_activity_uses_one_back_dispatch_route()
    sapil = ROOT / "SAPIL/sapil/src/main/java/ru/ytkab0bp/sapil"
    sapil_java = list(sapil.rglob("*.java"))
    sapil_kotlin = list(sapil.rglob("*.kt"))
    if sapil_java:
        raise AssertionError("SAPIL still contains Java sources: " + ", ".join(str(path) for path in sapil_java))
    if len(sapil_kotlin) != 12:
        raise AssertionError(f"SAPIL should contain the 12 migrated Kotlin sources, found {len(sapil_kotlin)}")

    migrated_java_sources = (
        "app/src/androidTest/java/ru/ytkab0bp/slicebeam/NativeSliceDeviceTest",
        "EventBus/app/src/main/java/ru/ytkab0bp/eventbus/demo/MainActivity",
        "EventBus/app/src/main/java/ru/ytkab0bp/eventbus/demo/events/InheritedEvent",
        "EventBus/app/src/main/java/ru/ytkab0bp/eventbus/demo/events/TestEvent",
        "EventBus/eventbus_api/src/main/java/ru/ytkab0bp/eventbus/Event",
        "EventBus/eventbus_api/src/main/java/ru/ytkab0bp/eventbus/EventBusCompatConsumer",
        "EventBus/eventbus_api/src/main/java/ru/ytkab0bp/eventbus/EventBusListenerImpl",
        "EventBus/eventbus_api/src/main/java/ru/ytkab0bp/eventbus/EventHandler",
        "EventBus/eventbus_processor/src/main/java/ru/ytkab0bp/eventbus_processor/EventBusProcessor",
        "EventBus/eventbus/src/main/java/ru/ytkab0bp/eventbus/EventBus",
        "SAPIL/demo/src/main/java/ru/ytkab0bp/sapil/MainActivity",
        "SAPIL/demo/src/main/java/ru/ytkab0bp/sapil/PlaceholderRepo",
    )
    for source in migrated_java_sources:
        java_path = ROOT / f"{source}.java"
        kotlin_path = ROOT / f"{source}.kt"
        if java_path.exists():
            raise AssertionError(f"Java source remains after the 12-class migration: {java_path}")
        if not kotlin_path.exists():
            raise AssertionError(f"Kotlin replacement for migrated Java source is missing: {kotlin_path}")

    settings_source = (ROOT / "settings.gradle").read_text(encoding="utf-8")
    for demo_project in ("eventbusdemo", "sapildemo"):
        if f":{demo_project}" not in settings_source:
            raise AssertionError(f"The grouped build does not include the migrated {demo_project} module")

    app_build_source = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
    if "tasks.named('preBuild').configure" not in app_build_source or "verifyPrebuiltNativeBundle" not in app_build_source:
        raise AssertionError("Gradle can package cached native libraries without verifying their source build and checksums")
    native_cache_helper = ROOT / "scripts/sync_native_cache.py"
    verification_script = (ROOT / "scripts/verify-and-package.ps1").read_text(encoding="utf-8")
    if not native_cache_helper.exists() or "sync_native_cache.py' 'check'" not in verification_script:
        raise AssertionError("The grouped workflow does not verify the ARM64 native cache before fast builds")

    rotation_test = ROOT / "app/src/androidTest/java/ru/ytkab0bp/slicebeam/MainActivityRotationDeviceTest.kt"
    verification_script = (ROOT / "scripts/verify-and-package.ps1").read_text(encoding="utf-8")
    if not rotation_test.exists():
        raise AssertionError("The ARM64 MainActivity rotation regression test is missing")
    rotation_test_source = rotation_test.read_text(encoding="utf-8")
    for contract in (
        "ActivityScenario.launch(MainActivity::class.java)",
        "SCREEN_ORIENTATION_LANDSCAPE", "expectedConfigurationOrientation", "assertEquals",
        "assertSame", "isAttachedToWindow",
        "waitForIdleSync()",
    ):
        if contract not in rotation_test_source:
            raise AssertionError(f"The Android 16 rotation test is missing: {contract}")
    if ":app:compileSliceTestAndroidTestKotlin" not in verification_script:
        raise AssertionError("The grouped verification does not compile Kotlin instrumentation tests")
    if ":eventbusdemo:assembleDebug" not in verification_script or ":sapildemo:assembleDebug" not in verification_script:
        raise AssertionError("The grouped APK build does not compile both Kotlin demo modules")

    main_activity = (JAVA / "MainActivity.kt").read_text(encoding="utf-8")
    lifecycle_contracts = (
        "delegate?.onResume()",
        "delegate?.onPause()",
        "delegate?.onDestroy()",
        "if (delegate?.onBackPressed() == true) return true",
        "delegate?.getCurrentFragment()?.onConfigurationChanged(newConfig)",
    )
    missing_lifecycle_contracts = [item for item in lifecycle_contracts if item not in main_activity]
    if missing_lifecycle_contracts:
        raise AssertionError("MainActivity lifecycle does not safely handle an uninitialized navigation delegate: " + ", ".join(missing_lifecycle_contracts))
    if "if (navigationDelegate != null)" in main_activity:
        raise AssertionError("MainActivity checks its throwing non-null navigation getter instead of the nullable delegate")

    safe_start = (JAVA / "SafeStartActivity.kt").read_text(encoding="utf-8")
    safe_start_screen = (JAVA / "ui/SafeStartScreen.kt").read_text(encoding="utf-8")
    for contract in (
        "WindowCompat.setDecorFitsSystemWindows(window, false)",
        "WindowInsetsControllerCompat(window, window.decorView)",
        "ComposeView(this)",
    ):
        if contract not in safe_start:
            raise AssertionError(f"Crash recovery lost its edge-to-edge Compose host: {contract}")
    if "safeDrawingPadding()" not in safe_start_screen:
        raise AssertionError("Crash recovery content does not keep its actions inside Android system safe areas")

    setup_activity = (JAVA / "SetupActivity.kt").read_text(encoding="utf-8")
    if "composeSetupRoot(this)" not in setup_activity or "SetupActivityRoot(" not in setup_activity:
        raise AssertionError("Setup activity background, title, and pager must share the Compose root.")
    setup_onboarding = (JAVA / "ui/setup/SetupOnboardingScreens.kt").read_text(encoding="utf-8")
    if "color = Color.White.copy(alpha = 0.88f)" not in setup_onboarding:
        raise AssertionError("The intro subtitle must remain readable over the dark branded startup artwork")

    legacy = JAVA / "fragment/MakerFragment.java"
    source_path = JAVA / "fragment/MakerFragment.kt"
    if legacy.exists():
        raise AssertionError("The Maker screen still has a Java implementation")
    if not source_path.exists():
        raise AssertionError("The Kotlin Maker screen is missing")

    source = source_path.read_text(encoding="utf-8")
    description_start = source.index("Text(stringResource(section.descriptionRes)")
    description_end = source.index("\n                }", description_start)
    description_layout = source[description_start:description_end]
    if "maxLines" in description_layout or "TextOverflow.Ellipsis" in description_layout:
        raise AssertionError("Maker category descriptions must wrap fully instead of hiding their ends")
    for resource_dir in ("values", "values-de"):
        resource_file = ROOT / f"app/src/main/res/{resource_dir}/strings.xml"
        resource_text = resource_file.read_text(encoding="utf-8")
        for key in ("MakerConvertersDescription", "MakerKromacutDescription", "MakerGeneratorsDescription"):
            match = re.search(rf'<string name="{key}">(.*?)</string>', resource_text)
            if not match or len(match.group(1)) > 70:
                raise AssertionError(f"{key} must have a concise description in {resource_dir}")
    bed_fragment = (JAVA / "fragment/BedFragment.kt").read_text(encoding="utf-8")
    printer_web_controls = (JAVA / "ui/workspace/PrinterWebControls.kt").read_text(encoding="utf-8")
    for contract in (
        "private var printerWebButton: ComposeView?",
        "private var printerWebCloseButton: ComposeView?",
        "printerWebAvailable = enable",
        "PrinterWebOpenButton(hostConfigured = printerWebAvailable, onOpen = ::openPrinterWebView)",
        "PrinterWebCloseButton(onClose = ::destroyPrinterWebView)",
        "printerWebThemeRevision++",
    ):
        if contract not in bed_fragment:
            raise AssertionError(f"Printer dashboard controls lost their Compose host or state route: {contract}")
    for contract in (
        "internal fun PrinterWebOpenButton(",
        "R.string.PrinterOpenWebDescription",
        "R.string.PrinterWebConfigureDescription",
        "R.string.PrinterWebShortLabel",
        "internal fun PrinterWebCloseButton(",
        "R.string.ClosePanel",
        "size(48.dp)",
        "contentDescription = description",
    ):
        if contract not in printer_web_controls:
            raise AssertionError(f"Compose printer dashboard controls lost an accessible UI contract: {contract}")
    if 'printerWebButton = TextView(ctx)' in bed_fragment or 'printerWebCloseButton = TextView(ctx)' in bed_fragment:
        raise AssertionError("The printer dashboard controls still use legacy native text buttons")
    selected_actions = (JAVA / "ui/workspace/SelectedObjectActionsCompose.kt").read_text(encoding="utf-8")
    selected_actions_test = (ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/ui/workspace/SelectedObjectQuickActionPolicyTest.kt").read_text(encoding="utf-8")
    for contract in (
        "selectedObjectBar = composeView",
        "BeamComposeTheme(themeRevision = revision)",
        "title = selectedObjectBarTitle",
        "val selectionRevision = selectedObjectBarSelectionRevision",
        "selectedObjectBarSelectionRevision++",
        "selectionKey = selectionRevision",
        "onAction = ::runObjectQuickAction",
        "selectedObjectBarVisible = visible",
        "selectedObjectBarThemeRevision++",
        "SelectedObjectQuickAction.MOVE",
        "SelectedObjectQuickAction.ROTATE",
        "SelectedObjectQuickAction.SCALE",
        "SelectedObjectQuickAction.DUPLICATE",
        "SelectedObjectQuickAction.MORE",
    ):
        if contract not in bed_fragment:
            raise AssertionError(f"Selected-object quick-action behavior is missing after Compose migration: {contract}")
    for contract in (
        "androidx.compose.runtime.key(selectionKey)",
        "SelectedObjectQuickActionPolicy.actions.forEach",
        "horizontalScroll(rememberScrollState())",
        "defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)",
        "clickable(role = Role.Button",
        "stringResource(action.labelRes)",
        "MaterialTheme.colorScheme.surfaceVariant",
    ):
        if contract not in selected_actions:
            raise AssertionError(f"Selected-object Compose action bar is missing: {contract}")
    if "private fun objectActionButton" in bed_fragment or "private lateinit var selectedObjectTitle" in bed_fragment:
        raise AssertionError("The selected-object toolbar still contains the legacy native view implementation")
    for contract in (
        "selectionToolbarKeepsEveryActionInTheExpectedOrder",
        "actionsKeepTheirLocalizedLabels",
    ):
        if contract not in selected_actions_test:
            raise AssertionError(f"Selected-object action regression coverage is missing: {contract}")
    plate_tabs = (JAVA / "ui/workspace/PlateTabsCompose.kt").read_text(encoding="utf-8")
    bed_menu_compose = (JAVA / "components/bed_menu/BedMenuComposeContent.kt").read_text(encoding="utf-8")
    plate_tabs_test = (ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/ui/workspace/PlateTabsPolicyTest.kt").read_text(encoding="utf-8")
    for contract in (
        "plateTabsScroll = ComposeView(ctx)",
        "PlateTabs(",
        "onSelect = { index -> if (index != currentPlateIndex) switchPlate(index) }",
        "onClose = { index -> removePlate(index) }",
        "onAdd = { addNewPlate() }",
        "clearSelectionForPlateSwitch()",
        "plateTabCount = platesModels.size",
        "PlateTabsPolicy.afterClose(platesModels.size, currentPlateIndex, plateIndex)",
        "platesModels.set(0, Model())",
        "projectPlateIds.clear()",
    ):
        if contract not in bed_fragment:
            raise AssertionError(f"Compose plate management route or empty-last-plate behavior is missing: {contract}")
    for contract in (
        "LazyColumn(",
        ".width(72.dp)",
        "Role.Tab",
        "PlateCloseDescription",
        "MenuAddPlate",
        "defaultMinSize(minHeight = 48.dp)",
        "listState.animateScrollToItem",
    ):
        if contract not in plate_tabs:
            raise AssertionError(f"Compose plate tab is missing an accessible touch interaction: {contract}")
    for contract in (
        ".heightIn(max = LocalConfiguration.current.screenHeightDp.dp)",
        ".verticalScroll(rememberScrollState())",
    ):
        if contract not in bed_menu_compose:
            raise AssertionError(f"The bed menu is missing a finite viewport constraint: {contract}")
    if "plateTabsContainer" in bed_fragment or "private fun addPlateTab(" in bed_fragment:
        raise AssertionError("The plate selector still uses the legacy native tab renderer")
    for contract in (
        "closingTheOnlyPlateKeepsOneEmptyPlateSelected",
        "closingTheActivePlateSelectsThePreviousPlateWhenPossible",
        "closingAnEarlierPlateKeepsTheSameActivePlateAfterIndexShift",
        "closingALaterPlateDoesNotChangeTheActiveIndex",
        "invalidPlateIndicesAreRejected",
    ):
        if contract not in plate_tabs_test:
            raise AssertionError(f"Plate close regression coverage is missing: {contract}")
    profile_status = (JAVA / "ui/workspace/WorkspaceProfileStatus.kt").read_text(encoding="utf-8")
    for contract in (
        "WorkspaceProfileStatus(",
        "profileStatusText = ComposeView(ctx)",
        "profileStatusThemeRevision++",
        "R.string.WorkspaceSettingsUnknown",
        "Role.Button",
    ):
        if contract not in bed_fragment + profile_status:
            raise AssertionError(f"Workspace printer/profile status was not migrated accessibly to Compose: {contract}")
    if 'profileStatusText = TextView(ctx)' in bed_fragment or '"â€“"' in bed_fragment:
        raise AssertionError("Workspace profile status still uses its legacy text view or a mis-encoded placeholder")
    for contract in (
        "Modifier\n            .fillMaxWidth()\n            .heightIn(min = 48.dp)",
        "MaterialTheme.colorScheme.surfaceVariant",
        "R.string.WorkspaceProfilesOpenLabel",
        "R.drawable.printer_outline_28",
        "R.drawable.dropdown_24",
        "title: String",
        "summary: String",
        "TextOverflow.Ellipsis",
        "maxLines = 1",
        "contentDescription = accessibilityDescription",
    ):
        if contract not in profile_status:
            raise AssertionError(f"Adaptive workspace profile header is missing: {contract}")
    if "BedProfileStatus" in bed_fragment or "BedProfileStatus" in profile_status:
        raise AssertionError("The multi-line workspace profile summary still occupies the model viewport")
    if "header.addView(profileStatusText" not in bed_fragment:
        raise AssertionError("The workspace header is missing its profile control")
    unfold_menu = (JAVA / "components/UnfoldMenu.kt").read_text(encoding="utf-8")
    for contract in (
        "snackbarsLayout = SnackbarsLayout(ctx)",
        "overlayLayout!!.addView(snackbarsLayout,",
    ):
        if contract not in bed_fragment:
            raise AssertionError(f"Workspace snackbar overlay is not retained on BedFragment: {contract}")
    if "snackbars!!." in unfold_menu or "glView!!." in unfold_menu:
        raise AssertionError("Unfold-menu animation must tolerate a detached or partially initialized workspace overlay")
    for contract in ("snackbars?.translationY", "glView?.translationX", "snackbars?.layoutParams as? ViewGroup.MarginLayoutParams"):
        if contract not in unfold_menu:
            raise AssertionError(f"Unfold-menu animation is missing its lifecycle-safe workspace guard: {contract}")
    snackbar_host = (JAVA / "view/SnackbarsLayout.kt").read_text(encoding="utf-8")
    snackbar_card = (JAVA / "ui/components/SnackbarCard.kt").read_text(encoding="utf-8")
    for contract in (
        "fun show(snackbar: Snackbar)", "fun dismiss(tag: String?)",
        "private inner class SnackbarView(context: Context) : FrameLayout(context)",
        "ThemeAwareComposeView(context)", "SnackbarCard(", "modelTag(): String? = snackbar.tag",
        "@JvmField var lifetime", "@JvmField var tag: String?",
        "@JvmField var buttonTitle: CharSequence?", "@JvmField var buttonClick: View.OnClickListener?",
        ".addEndListener", "enum class Type",
    ):
        if contract not in snackbar_host:
            raise AssertionError(f"Compose snackbar host lost a lifecycle/event contract: {contract}")
    for legacy_widget in ("LinearLayout(", "ImageView(", "ProgressBar(", "TextView("):
        if legacy_widget in snackbar_host:
            raise AssertionError(f"Snackbar card still builds its visual content with {legacy_widget}")
    for contract in (
        "Surface(", "CircularProgressIndicator(", "TextButton(",
        "LiveRegionMode.Polite", "detectHorizontalDragGestures", "VelocityTracker",
    ):
        if contract not in snackbar_card:
            raise AssertionError(f"Compose snackbar presentation is missing: {contract}")
    snackbar_policy_tests = (ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/view/SnackbarSwipePolicyTest.kt").read_text(encoding="utf-8")
    for contract in (
        "aFastRightFlingDismissesPastTheVisibleCard", "aFastLeftFlingDismissesPastTheVisibleCard",
        "aSlowSwipeReturnsToItsOriginalPosition", "persistentTaggedProgressCannotBeDismissedBySwiping",
    ):
        if contract not in snackbar_policy_tests:
            raise AssertionError(f"Snackbar swipe regression coverage is missing: {contract}")
    for contract in (
        "private var navigationView: ComposeView? = null",
        "private var selectedMenuNavigationSlot by mutableIntStateOf(0)",
        "private fun createMenuNavigationView(ctx: Context, bottomNavigation: Boolean): ComposeView",
        "AdaptiveWorkspaceNavigation(",
        "selectedSlot = selectedMenuNavigationSlot",
        "onSelected = { slot -> onMenuNavigationSelected(ctx, bottomNavigation, slot) }",
        "navigationView = createMenuNavigationView(ctx, bottomNavigation = portrait)",
        "configureMenuNavigation()",
    ):
        if contract not in bed_fragment:
            raise AssertionError(f"Adaptive Compose workspace navigation is missing its rotation-safe contract: {contract}")
    if "ThemeRailNavigationView" in bed_fragment or "ThemeBottomNavigationView" in bed_fragment:
        raise AssertionError("The workspace category navigation must not fall back to legacy Material navigation views")
    for contract in (
        "BedFragment.MenuCategory.CAMERA -> menu = CameraMenu()",
        "BedFragment.MenuCategory.TRANSFORM -> menu = TransformMenu()",
        "BedFragment.MenuCategory.OPTIONS -> menu = OptionsMenu()",
        "FILE(R.string.MenuFile", "CAMERA(R.string.MenuCamera", "OPTIONS(R.string.MenuOptions",
        "SLICE_AND_EXPORT(R.string.MenuSlice",
    ):
        if contract not in bed_fragment:
            raise AssertionError(f"SliceBeam-style five-area workspace menu is missing: {contract}")

    compose_theme = (JAVA / "ui/BeamComposeTheme.kt").read_text(encoding="utf-8")
    if "LocalBeamThemeRevision provides themeRevision" not in compose_theme or "MaterialTheme(colorScheme = colors, content = content)" not in compose_theme:
        raise AssertionError("Shared Compose theme is not connected to the app light/dark palette")
    if "LocalDensity provides appDensity" not in compose_theme or "LocalBeamTextScaleApplied provides true" not in compose_theme:
        raise AssertionError("Shared Compose theme is not applying the app-specific text scale")
    prefs_source = (JAVA / "utils/Prefs.kt").read_text(encoding="utf-8")
    if '.getFloat("ui_text_scale", 1.0f)' not in prefs_source or '.coerceIn(0.9f, 1.5f)' not in prefs_source:
        raise AssertionError("The standard-size default app text scale and expanded accessible range are missing")
    if "val variants = floatArrayOf(0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f)" not in (JAVA / "fragment/SettingsFragment.kt").read_text(encoding="utf-8"):
        raise AssertionError("The app settings do not offer enlarged menu text for Honor-sized phones")
    layout_policy = (JAVA / "ui/settings/ProfileSettingsMenuLayoutPolicy.kt").read_text(encoding="utf-8")
    if "screenWidthDp in 1 until STACKED_FILTER_MAX_WIDTH_DP" not in layout_policy or "textScale >= STACKED_FILTER_TEXT_SCALE" not in layout_policy:
        raise AssertionError("Profile filters are not switching to a readable stacked layout for phones and large text")
    if "CATEGORY_PANE_MIN_WIDTH_DP = 840" not in layout_policy or "textScale < CATEGORY_PANE_MAX_TEXT_SCALE" not in layout_policy:
        raise AssertionError("Expanded-width profile screens must switch to a two-pane category layout only at readable text sizes")
    profile_list = (JAVA / "fragment/ProfileSettingsComposeList.kt").read_text(encoding="utf-8")
    if "TabletProfileCategorySelector(" not in profile_list or "rowsForCategory(rows, selectedCategory.categoryIndex)" not in profile_list:
        raise AssertionError("Expanded-width profile screens are missing their category selector and matching detail pane")
    category_policy = (JAVA / "fragment/ProfileSettingsCategoryPanePolicy.kt").read_text(encoding="utf-8")
    if "rows.filter { it.isExpandable && !it.title.isNullOrBlank() }" not in category_policy or "row.categoryIndex == categoryIndex" not in category_policy:
        raise AssertionError("Tablet profile categories must retain their scoped setting rows")
    category_builder = (JAVA / "fragment/ProfileSettingsListBuilder.kt").read_text(encoding="utf-8")
    if "also { it.categoryIndex = activeCategoryIndex }" not in category_builder:
        raise AssertionError("Accordion child rows must retain the index of their parent category")
    filter_bar = (JAVA / "ui/settings/ComposeProfileFilterBar.kt").read_text(encoding="utf-8")
    if "ProfileSearchField(" not in filter_bar or "maxLines = if (stacked) 2 else 1" not in filter_bar:
        raise AssertionError("Profile search and level controls are missing their adaptive readable layout")
    if "ProfileSettingsFavoritesToggle" not in filter_bar:
        raise AssertionError("The favorites-only icon is missing an accessibility label")
    profile_status = (JAVA / "ui/settings/ComposeProfileSettingsStatus.kt").read_text(encoding="utf-8")
    if "if (state.pendingCount <= 0) return" not in profile_status:
        raise AssertionError("The profile screen should stay free of a saved-status banner when no edits are pending")
    bed_touch = (JAVA / "view/GLView.kt").read_text(encoding="utf-8")
    bed_renderer = (JAVA / "render/GLRenderer.kt").read_text(encoding="utf-8")
    bed_screen = (JAVA / "fragment/BedFragment.kt").read_text(encoding="utf-8")
    if "tryStartPrimeTowerDrag" not in bed_touch or "dragPrimeTower" not in bed_touch or "onPrimeTowerTap?.invoke()" not in bed_touch:
        raise AssertionError("The prime tower must support direct dragging and a tap for exact positioning")
    if "PrimeTowerPositionEditor(" not in bed_screen or "onPrimeTowerTap = { showPrimeTowerPositionDialogForCurrentPlate() }" not in bed_screen:
        raise AssertionError("The build-plate tower tap must open the active plate's X/Y editor")
    if "PrimeTowerDragLogic.position(" not in bed_renderer:
        raise AssertionError("Dragging the prime tower must use bed-bounded coordinates")
    option_theme = (JAVA / "ui/settings/ProfileOptionTheme.kt").read_text(encoding="utf-8")
    if "BeamComposeTheme(content = content)" not in option_theme:
        raise AssertionError("Profile option editors do not share the central Compose theme")
    for themed_screen in (JAVA / "fragment/MakerFragment.kt",):
        themed_source = themed_screen.read_text(encoding="utf-8")
        if "BeamComposeTheme {" not in themed_source:
            raise AssertionError(f"{themed_screen.name} does not use the shared Compose theme")
        if "darkColorScheme(" in themed_source or "lightColorScheme(" in themed_source:
            raise AssertionError(f"{themed_screen.name} still defines a duplicate Compose palette")
    profile_host = (JAVA / "fragment/ProfileListFragment.kt").read_text(encoding="utf-8")
    if "BeamComposeTheme(themeRevision = currentThemeRevision)" not in profile_host:
        raise AssertionError("The unified profile screen does not provide the shared theme to all Compose settings")
    for themed_screen in (
        JAVA / "ui/settings/ComposeProfileCategoryTabs.kt",
        JAVA / "ui/settings/ComposeProfileFilterBar.kt",
        JAVA / "ui/settings/ComposeProfileSettingsStatus.kt",
    ):
        themed_source = themed_screen.read_text(encoding="utf-8")
        if "MaterialTheme." not in themed_source:
            raise AssertionError(f"{themed_screen.name} does not inherit the shared Compose theme")
        if "BeamComposeTheme {" in themed_source or "darkColorScheme(" in themed_source or "lightColorScheme(" in themed_source:
            raise AssertionError(f"{themed_screen.name} defines a redundant nested Compose palette")

    navigation_source = (JAVA / "ui/navigation/AdaptiveWorkspaceNavigation.kt").read_text(encoding="utf-8")
    navigation_delegate = (JAVA / "navigation/MobileNavigationDelegate.kt").read_text(encoding="utf-8")
    for contract in (
        "Row(Modifier.fillMaxWidth().selectableGroup())", "Column(",
        "item.slot == selectedSlot", "accessibilityLabel", "maxLines = 1",
        "softWrap = false", "fontSize = if (compactRail) 10.sp else 11.sp",
        "Modifier.width(AdaptiveNavigationPolicy.NAVIGATION_RAIL_WIDTH_DP.dp)",
        ".selectable(selected = selected, role = Role.Tab",
    ):
        if contract not in navigation_source:
            raise AssertionError(f"Adaptive Compose navigation is missing: {contract}")
    for contract in (
        "ComposeView(ctx)", "ViewCompositionStrategy.DisposeOnDetachedFromWindow",
        "AdaptiveWorkspaceNavigation(", "navigationThemeRevision++", "selectedNavigationSlot",
        "lastPrimarySlot", "onConfigurationChanged(configuration: Configuration)",
        "ViewUtils.dp(AdaptiveNavigationPolicy.NAVIGATION_RAIL_WIDTH_DP)",
    ):
        if contract not in navigation_delegate:
            raise AssertionError(f"Adaptive navigation state/rotation handling is missing: {contract}")
    if "ThemeBottomNavigationView" in navigation_delegate or "ThemeRailNavigationView" in navigation_delegate:
        raise AssertionError("The main adaptive navigation is still using the legacy navigation views")
    for contract in ("getSlotNavigationTitle(slot)", "NavigationFilament"):
        if contract not in navigation_delegate and contract not in (JAVA / "navigation/DelegateSlotImpl.kt").read_text(encoding="utf-8"):
            raise AssertionError(f"Compact phone navigation label is missing: {contract}")
    bed_menu_navigation = (JAVA / "fragment/BedFragment.kt").read_text(encoding="utf-8")
    if "R.string.MenuToolbarNavigation" not in bed_menu_navigation:
        raise AssertionError("The editing workspace navigation needs its compact localized label")
    navigation_policy = (JAVA / "navigation/MobileNavigationPolicy.kt").read_text(encoding="utf-8")
    for contract in (
        "private val destinations = listOf(", "PREVIEW_SLOT,", "PRINT_SLOT,", "FILAMENT_SLOT,",
        "PRINTER_SLOT,", "APP_SLOT,", "selectedDestination(slot: Int)",
        "if (slot == MAKER_SLOT) APP_SLOT else slot",
    ):
        if contract not in navigation_policy:
            raise AssertionError(f"SliceBeam-style five-area navigation is missing: {contract}")
    if "WorkspaceHeader(" in navigation_delegate:
        raise AssertionError("The main workspace still spends vertical space on a separate header")
    settings_fragment = (JAVA / "fragment/SettingsFragment.kt").read_text(encoding="utf-8")
    for contract in ("onMaker = ::openMaker", "navigation.pushFragment(MakerFragment())"):
        if contract not in settings_fragment:
            raise AssertionError(f"The App area is missing its direct Maker route: {contract}")
    fragment_navigation = (JAVA / "navigation/NavigationDelegate.kt").read_text(encoding="utf-8")
    for contract in (
        "activeContainer.addView(newView)", "activeContainer.addView(restoredView)",
        "restoredView.translationX = (1f - value) * -restoredView.width * 0.75f",
        "restoredView.translationY = (1f - value) * -restoredView.height * 0.75f",
    ):
        if contract not in fragment_navigation:
            raise AssertionError(f"Nested screen navigation is missing its attached back-stack view: {contract}")

    for contract in (
        "class MakerFragment : Fragment()",
        "ComposeView(ctx)",
        "BeamComposeTheme",
        "verticalScroll(rememberScrollState())",
        "mutableStateOf(emptySet<Int>())",
        "onApplyTheme()",
        "switchToSlot(0)",
        "private fun activity(): MainActivity? = getContext() as? MainActivity",
    ):
        if contract not in source:
            raise AssertionError(f"Maker screen is missing a required Compose or navigation route: {contract}")

    preserved = (
        "ImageToStlConverter.pickImage", "ImageToStlConverter.pickLithophane", "FastenerGenerator.show", "KromacutEditorMenu",
        "TextTagGenerator.show", "UtilityBoxGenerator.show", "CableClipGenerator.show",
        "CableGrommetGenerator.show", "WallToolHolderGenerator.show", "PlanterPotGenerator.show",
        "PlantLabelSetGenerator.show", "PhoneStandGenerator.show", "PowerStripTrayGenerator.show",
    )
    missing = [route for route in preserved if route not in source]
    if missing:
        raise AssertionError("Maker actions were lost during migration: " + ", ".join(missing))
    for legacy_view in ("ScrollView(", "GradientDrawable(", "android.widget.LinearLayout"):
        if legacy_view in source:
            raise AssertionError(f"Legacy Maker view implementation remains: {legacy_view}")

    config_option_def = (JAVA / "slic3r/ConfigOptionDef.kt").read_text(encoding="utf-8")
    native_bridge = (ROOT / "app/src/main/jni/slicebeam/beam_native.cpp").read_text(encoding="utf-8")
    vector_editor = (JAVA / "ui/settings/ComposeProfileVectorOption.kt").read_text(encoding="utf-8")
    filament_catalog = (JAVA / "ui/settings/OrcaFilamentSettingsCatalog.kt").read_text(encoding="utf-8")
    for contract, content in (
        ("@JvmField var nullable: Boolean = false", config_option_def),
        ('GetFieldID(configOptionDefClass, "nullable", "Z")', native_bridge),
        ("SetBooleanField(cfgDef, nullableField, nCfgDef->nullable)", native_bridge),
        ("validate(currentValue, minimum, maximum, kind, nullable)", vector_editor),
        ('section("Ironing", "filament_ironing_flow", "filament_ironing_spacing", "filament_ironing_inset", "filament_ironing_speed")', filament_catalog),
    ):
        if contract not in content:
            raise AssertionError(f"Nullable filament vector support is missing: {contract}")

    converter_legacy = JAVA / "components/bed_menu/ConverterMenu.java"
    converter_path = JAVA / "components/bed_menu/ConverterMenu.kt"
    if converter_legacy.exists():
        raise AssertionError("The local converter menu still has a Java implementation")
    if not converter_path.exists():
        raise AssertionError("The Kotlin local converter menu is missing")
    converter_source = converter_path.read_text(encoding="utf-8")
    converter_routes = (
        "class ConverterMenu : BedMenu()", "ThemeAwareComposeView(ctx)",
        "BeamComposeTheme(themeRevision = themedView.themeRevision)",
        "BedMenuActionsContent(createActions(), portrait)", "ImageToStlConverter.pickImage",
        "KromacutEditorMenu()", "ImageToStlConverter.pickLithophane", "TextTagGenerator.show",
        "UtilityBoxGenerator.show", "CableClipGenerator.show", "CableGrommetGenerator.show",
        "WallToolHolderGenerator.show", "PlanterPotGenerator.show", "PlantLabelSetGenerator.show",
        "PhoneStandGenerator.show", "PowerStripTrayGenerator.show",
    )
    missing_converter_routes = [route for route in converter_routes if route not in converter_source]
    if missing_converter_routes:
        raise AssertionError("Converter actions were lost during migration: " + ", ".join(missing_converter_routes))
    if "ListBedMenu" in converter_source or "BedMenuItem" in converter_source:
        raise AssertionError("Converter menu still depends on legacy RecyclerView rows")
    responsive_menu_path = JAVA / "components/bed_menu/BedMenuComposeContent.kt"
    if not responsive_menu_path.exists():
        raise AssertionError("Shared responsive Compose bed-menu actions are missing")
    responsive_menu_source = responsive_menu_path.read_text(encoding="utf-8")
    for contract in (
        "data class BedComposeMenuAction(", "fun BedMenuActionsContent(",
        "horizontalScroll(rememberScrollState())", "verticalScroll(rememberScrollState())",
        "action.sectionBreakBefore", "role = Role.Switch", "stringResource(action.titleRes)",
    ):
        if contract not in responsive_menu_source:
            raise AssertionError(f"Shared responsive Compose menu is missing {contract}")
    if "BedMenuActionsContent(actions, portrait, multiRow = true)" not in (JAVA / "components/bed_menu/CameraMenu.kt").read_text(encoding="utf-8"):
        raise AssertionError("Camera menu no longer uses the shared responsive Compose action layout")

    converter_progress = (JAVA / "components/ConverterProgressDialog.kt").read_text(encoding="utf-8")
    progress_model = (JAVA / "components/ConverterProgressModel.kt").read_text(encoding="utf-8")
    for contract in (
        "val composeView = ComposeView(activity)", "BeamComposeTheme {", "ConverterProgressContent(displayState)",
        "LinearProgressIndicator(", "R.string.ConverterProgressElapsed", "R.string.ConverterProgressRemaining",
        "setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)",
    ):
        if contract not in converter_progress:
            raise AssertionError(f"Shared converter progress dialog is missing Compose behavior: {contract}")
    if "ProgressBar" in converter_progress or "TextView" in converter_progress:
        raise AssertionError("Shared converter progress dialog still uses legacy progress/text widgets")
    for contract in (
        "fun advanceStage(", "fun estimate(", "fun stlFileProgress(", "fun formatTime(",
    ):
        if contract not in progress_model:
            raise AssertionError(f"Shared converter progress calculation is missing {contract}")
    for language in ("values/strings.xml", "values-de/strings.xml", "values-ru/strings.xml"):
        resource_source = (ROOT / "app/src/main/res" / language).read_text(encoding="utf-8")
        for translated_label in (
            "ConverterProgressPreparing", "ConverterProgressPercent", "ConverterProgressElapsed",
            "ConverterProgressRemaining", "ConverterProgressComplete",
        ):
            if f'name="{translated_label}"' not in resource_source:
                raise AssertionError(f"The {language} translation is missing {translated_label}")

    litogen_generator = (JAVA / "components/LitoGenGenerator.kt").read_text(encoding="utf-8")
    litogen_dialog = (JAVA / "components/LitoGenOptionsDialog.kt").read_text(encoding="utf-8")
    litogen_logic = (JAVA / "components/LitoGenFormLogic.kt").read_text(encoding="utf-8")
    for contract in (
        "fun pick(activity: Activity)", "putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)",
        "images.size != 4 && images.size != 5", "ImageToStlConverter.createTopZ(",
        "LithophaneStlWriter.writeLitoGenCubeKit(", "ImageToStlConverter.requestSave(activity, output, \"stl\", bed)",
        "ImageToStlConverter.saveGeneratedStl(activity, output, bed)",
    ):
        if contract not in litogen_generator:
            raise AssertionError(f"LitoGEN lost a multi-image or STL workflow contract: {contract}")
    for contract in (
        "ComposeView(activity)", "BeamComposeTheme", "LitoGenOptionsContent(", "Slider(",
        "OutlinedTextField(", "stringResource(R.string.LithophaneGenerate)",
        "stringResource(R.string.LithophaneGenerateAndSave)", "ColorFilter.colorMatrix(",
        "ImageToStlConverter.decodeBitmap(activity.contentResolver, uri, 160)",
        "Modifier.weight(1f).verticalScroll(rememberScrollState())",
    ):
        if contract not in litogen_dialog:
            raise AssertionError(f"LitoGEN Compose form is missing a UI contract: {contract}")
    for legacy_control in ("LinearLayout(", "ScrollView(", "EditText(", "Spinner(", "SeekBar(", "TextView("):
        if legacy_control in litogen_dialog or legacy_control in litogen_generator:
            raise AssertionError(f"LitoGEN still uses legacy form controls: {legacy_control}")
    for contract in (
        "fun parseNumber(", "fun validate(", "LitoGenValidation.THICKNESS", "LitoGenValidation.DETAIL",
    ):
        if contract not in litogen_logic:
            raise AssertionError(f"LitoGEN form validation policy is missing {contract}")

    fastener_path = JAVA / "components/FastenerGenerator.kt"
    fastener_source = fastener_path.read_text(encoding="utf-8")
    fastener_contracts = (
        "fun show(activity: Activity, bed: BedFragment)", "BeamComposeTheme", "FastenerFormContent(",
        "SliceChoiceField(", "OutlinedTextField(", "FastenerFormLogic.validate(values)",
        "FastenerFormLogic.validateFitSample(values)", "FastenerStlWriter.writeBolt(",
        "FastenerStlWriter.writeThreadedNut(", "FastenerStlWriter.writeWasher(",
        "FastenerStlWriter.writeThreadFitCoupon(", "saveGeneratedStl(activity, output, bed)",
    )
    missing_fastener_contracts = [contract for contract in fastener_contracts if contract not in fastener_source]
    if missing_fastener_contracts:
        raise AssertionError("Fastener generator lost Compose controls or geometry routes: " + ", ".join(missing_fastener_contracts))
    for legacy_fastener_view in ("Spinner(", "EditText(", "ScrollView(", "android.widget.LinearLayout"):
        if legacy_fastener_view in fastener_source:
            raise AssertionError(f"Fastener generator still uses legacy form controls: {legacy_fastener_view}")

    generator_form_contracts = {
        "PlanterPotGenerator.kt": (
            "fun show(activity: Activity, bed: BedFragment)", "PlanterPotFormContent(",
            "PlanterPotFormLogic.validate(submitted)", "LithophaneStlWriter.writePlanterPot(",
            "ImageToStlConverter.saveGeneratedStl(activity, output, bed)",
        ),
        "PlanterPotForm.kt": (
            "PlanterPotFormValues", "PlanterPotIssue", "PlanterPotFormLogic",
            "Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())",
            "GeneratorDimensionField(", "Checkbox(", "R.string.ConverterConvertSave",
        ),
        "PhoneStandGenerator.kt": (
            "fun show(activity: Activity, bed: BedFragment)", "PhoneStandFormContent(",
            "PhoneStandFormLogic.validate(submitted)", "LithophaneStlWriter.writePhoneStand(",
            "ImageToStlConverter.saveGeneratedStl(activity, output, bed)",
        ),
        "PhoneStandForm.kt": (
            "PhoneStandFormValues", "PhoneStandIssue", "PhoneStandFormLogic",
            "Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())",
            "GeneratorDimensionField(", "R.string.ConverterConvertSave",
        ),
        "CableClipGenerator.kt": (
            "fun show(activity: Activity, bed: BedFragment)", "BeamComposeTheme", "CableClipFormContent(",
            "CableClipFormLogic.validate(submitted)", "LithophaneStlWriter.writeCableClip(",
            "ImageToStlConverter.saveGeneratedStl(activity, output, bed)",
        ),
        "CableClipForm.kt": (
            "CableClipFormValues", "CableClipIssue", "CableClipFormLogic", "Checkbox(",
            "GeneratorDimensionField(", "R.string.ConverterConvertSave",
        ),
        "CableGrommetGenerator.kt": (
            "fun show(activity: Activity, bed: BedFragment)", "BeamComposeTheme", "CableGrommetFormContent(",
            "CableGrommetFormLogic.validate(submitted)", "LithophaneStlWriter.writeCableGrommet(",
            "ImageToStlConverter.saveGeneratedStl(activity, output, bed)",
        ),
        "CableGrommetForm.kt": (
            "CableGrommetFormValues", "CableGrommetIssue", "CableGrommetFormLogic",
            "GeneratorDimensionField(", "R.string.ConverterConvertSave",
        ),
        "PlantLabelSetGenerator.kt": (
            "fun show(activity: Activity, bed: BedFragment)", "BeamComposeTheme", "PlantTagsFormContent(",
            "PlantTagsFormLogic.validate(submitted)", "TextTagGenerator.rasterize(",
            "LithophaneStlWriter.writeTextTagBatch(", "ImageToStlConverter.saveGeneratedStl(activity, output, bed)",
        ),
        "PlantTagsForm.kt": (
            "PlantTagsFormValues", "PlantTagsIssue", "PlantTagsFormLogic", "OutlinedTextField(",
            "GeneratorDimensionField(", "R.string.ConverterConvertSave",
        ),
        "PowerStripTrayGenerator.kt": (
            "fun show(activity: Activity, bed: BedFragment)", "BeamComposeTheme", "PowerStripFormContent(",
            "PowerStripFormLogic.validate(submitted)", "LithophaneStlWriter.writePowerStripTray(",
            "ImageToStlConverter.saveGeneratedStl(activity, output, bed)",
        ),
        "PowerStripForm.kt": (
            "PowerStripFormValues", "PowerStripIssue", "PowerStripFormLogic",
            "GeneratorDimensionField(", "R.string.ConverterConvertSave",
        ),
        "WallToolHolderGenerator.kt": (
            "fun show(activity: Activity, bed: BedFragment)", "BeamComposeTheme", "WallToolHolderFormContent(",
            "WallToolHolderFormLogic.validate(submitted)", "LithophaneStlWriter.writeWallToolHolder(",
            "ImageToStlConverter.saveGeneratedStl(activity, output, bed)",
        ),
        "WallToolHolderForm.kt": (
            "WallToolHolderFormValues", "WallToolHolderIssue", "WallToolHolderFormLogic",
            "KeyboardType.Number", "GeneratorDimensionField(", "R.string.ConverterConvertSave",
        ),
        "TextTagGenerator.kt": (
            "fun show(activity: Activity, bed: BedFragment)", "BeamComposeTheme", "TextTagFormContent(",
            "TextTagFormLogic.validate(submitted)", "TextTagFormLogic.normalized(submitted)",
            "fun rasterize(", "LithophaneStlWriter.writeTextTag(",
            "ImageToStlConverter.saveGeneratedStl(activity, output, bed)",
        ),
        "TextTagForm.kt": (
            "TextTagFormValues", "TextTagFormLogic", "OutlinedTextField(",
            "Checkbox(", "GeneratorDimensionField(", "R.string.TextTagPreviewPlaceholder",
        ),
        "UtilityBoxGenerator.kt": (
            "fun show(activity: Activity, bed: BedFragment)", "BeamComposeTheme", "UtilityBoxFormContent(",
            "UtilityBoxFormLogic.normalized(submitted)", "LithophaneStlWriter.writeUtilityBox(",
            "ImageToStlConverter.saveGeneratedStl(activity, output, bed)",
        ),
        "UtilityBoxForm.kt": (
            "UtilityBoxFormValues", "UtilityBoxFormLogic", "Checkbox(",
            "GeneratorDimensionField(", "R.string.ConverterConvertSave",
        ),
        "GeneratorDimensionField.kt": ("fun GeneratorDimensionField(", "OutlinedTextField(", "KeyboardType.Decimal"),
    }
    localized_form_errors = (
        "PlanterErrorBottom", "PlanterErrorTop", "PlanterErrorHeight", "PlanterErrorWall",
        "PlanterErrorBase", "PlanterErrorDrain", "PlanterErrorSaucerClearance", "PlanterErrorSaucerWall",
        "PlanterErrorSaucerHeight", "PlanterErrorSaucerBase", "PhoneStandErrorWidth",
        "PhoneStandErrorThickness", "PhoneStandErrorTilt", "PhoneStandErrorDepth",
        "PhoneStandErrorBackHeight", "PhoneStandErrorBase", "PhoneStandErrorLip",
        "PhoneStandErrorCableGap", "PhoneStandErrorCableDepth", "PhoneStandErrorSupportClearance",
        "CableClipErrorDiameter", "CableClipErrorClearance", "CableClipErrorWall", "CableClipErrorWidth",
        "CableClipErrorMouth", "CableClipErrorTabLength", "CableClipErrorScrewHole",
        "CableGrommetErrorDiameter", "CableGrommetErrorClearance", "CableGrommetErrorWall",
        "CableGrommetErrorFlange", "CableGrommetErrorFlangeThickness", "CableGrommetErrorTubeLength",
        "CableGrommetErrorScrewHole", "PlantTagsErrorCount", "PlantTagsErrorNameLength",
        "PlantTagsErrorWidth", "PlantTagsErrorHeight", "PlantTagsErrorFont", "PlantTagsErrorBase",
        "PlantTagsErrorRaised", "PlantTagsErrorHole", "PlantTagsErrorGap", "PlantTagsErrorPlateSize",
        "PowerStripErrorLength", "PowerStripErrorWidth", "PowerStripErrorHeight", "PowerStripErrorClearance",
        "PowerStripErrorBase", "PowerStripErrorStop", "PowerStripErrorNotch", "PowerStripErrorScrew",
        "WallToolHolderErrorDiameter", "WallToolHolderErrorClearance", "WallToolHolderErrorCount",
        "WallToolHolderErrorThickness", "WallToolHolderErrorScrew", "WallToolHolderErrorGuideHeight",
        "TextTagPreviewPlaceholder",
    )
    for form_filename, contracts in generator_form_contracts.items():
        form_source = (JAVA / "components" / form_filename).read_text(encoding="utf-8")
        missing = [contract for contract in contracts if contract not in form_source]
        if missing:
            raise AssertionError(f"{form_filename} lost Compose, validation or STL routes: " + ", ".join(missing))
        for legacy_control in ("LinearLayout(", "ScrollView(", "EditText(", "TextView(", "CheckBox("):
            if legacy_control in form_source:
                raise AssertionError(f"{form_filename} still uses legacy form controls: {legacy_control}")
    for language in ("values/strings.xml", "values-de/strings.xml"):
        resource_source = (ROOT / "app/src/main/res" / language).read_text(encoding="utf-8")
        missing = [label for label in localized_form_errors if f'name="{label}"' not in resource_source]
        if missing:
            raise AssertionError(f"The {language} generator forms are missing translations: " + ", ".join(missing))

    print_config_path = JAVA / "fragment/PrintConfigFragment.kt"
    print_config_source = print_config_path.read_text(encoding="utf-8")
    print_config_tools_path = JAVA / "fragment/PrintConfigToolsCompose.kt"
    print_config_tools = print_config_tools_path.read_text(encoding="utf-8")
    print_config_contracts = (
        "PrintConfigToolsBar(", "showPrimeTowerPositionDialog(context)", "showFlushVolumesDialog(context)",
        "override fun onResume()", "toolsThemeRevision++", "override fun BelowFilamentPaletteContent(modifier: Modifier)",
        'config.presets?.get("printer")?.let(config::findPrinter)',
        "PrintConfigToolsLogic.canSyncFilaments(printer?.get(\"print_host\"))", "key(revision)",
        "MmuFilamentSync.synchronizeAll(printer)", "filamentSyncing = true", "filamentSyncing = false",
        'applyOptionValue("flush_volumes_matrix", serialized)',
        'applyOptionValue("wipe_tower_x", serialized.first)',
        'applyOptionValue("wipe_tower_y", serialized.second)',
    )
    missing_print_config_contracts = [item for item in print_config_contracts if item not in print_config_source]
    if missing_print_config_contracts:
        raise AssertionError("Print-profile quick controls lost Compose or profile-update routes: " + ", ".join(missing_print_config_contracts))
    quick_tools_start = print_config_source.find("override fun BelowFilamentPaletteContent(modifier: Modifier)")
    quick_tools_end = print_config_source.find("private fun syncMmuFilaments(", quick_tools_start)
    if quick_tools_start < 0 or quick_tools_end < 0:
        raise AssertionError("Print-profile quick controls are missing their direct Compose host")
    quick_tools_host = print_config_source[quick_tools_start:quick_tools_end]
    for nested_host in ("ComposeView(", "setContent {", "BeamComposeTheme {"):
        if nested_host in quick_tools_host:
            raise AssertionError(f"Print-profile quick controls still have a nested Compose host: {nested_host}")
    for contract in (
        "fun PrintConfigToolsBar(", "fun FlushVolumesEditor(", "fun PrimeTowerPositionEditor(",
        "OutlinedTextField(", "Modifier.horizontalScroll(rememberScrollState())",
        "fun flushMatrix(", "fun serializeFlushMatrix(", "fun towerPositions(",
        "fun serializeTowerPositions(", "replace(',', '.')", "stringResource(R.string.PrintConfigFlushCell",
    ):
        if contract not in print_config_tools:
            raise AssertionError(f"Compose print-profile tools are missing {contract}")
    for legacy_print_config_view in ("LinearLayout(", "ScrollView(", "HorizontalScrollView(", "EditText(", "BeamButton("):
        if legacy_print_config_view in print_config_source:
            raise AssertionError(f"Print-profile quick controls still use legacy view: {legacy_print_config_view}")
    for language in ("values/strings.xml", "values-de/strings.xml"):
        resource_source = (ROOT / "app/src/main/res" / language).read_text(encoding="utf-8")
        for translated_label in (
            "PrintConfigFlushVolumesHelp", "PrintConfigFlushCell", "PrintConfigPrimeTowerPositionHelp",
            "PrintConfigPrimePlateNumber", "MmuSyncWorking",
        ):
            if f'name="{translated_label}"' not in resource_source:
                raise AssertionError(f"The {language} translation is missing {translated_label}")

    dropdown_view_path = JAVA / "view/ProfileDropdownMenu.kt"
    dropdown_view = dropdown_view_path.read_text(encoding="utf-8")
    profile_host = (JAVA / "fragment/ProfileListFragment.kt").read_text(encoding="utf-8")
    for contract in (
        "internal fun ProfileDropdownContent(", "DropdownMenu(", "RadioButton(",
        "R.string.SettingsDeleteProfile", "R.string.SettingsCloneProfile", "Role.Button",
    ):
        if contract not in dropdown_view:
            raise AssertionError(f"Compose profile selector is missing {contract}")
    for legacy_dropdown_view in ("override fun onDraw(", "StaticLayout(", "Canvas("):
        if legacy_dropdown_view in dropdown_view:
            raise AssertionError(f"Profile selector still uses its custom-drawn view: {legacy_dropdown_view}")
    for contract in (
        "return ComposeView(ctx).apply", "BeamComposeTheme(themeRevision = currentThemeRevision)",
        "ProfileDropdownContent(", "ProfileDropdownMenuPolicy.create(",
        "ProfileDropdownMenuPolicy.selectedIndex(", "dropdownItemsSnapshot.getOrNull(safeIndex)",
        "selectItem(selected)", "refreshProfileDropdownTitle()", "refreshPendingActions()",
        "deleteCurrentProfile()", "cloneCurrentProfile()", "onUpdateConfigItems()",
        "profileList.Content()", "profileFilterController?.Content(showToggle = !showFilamentPalette())",
        "categoryTabsController?.Content()", "profileFooter?.Content(isPortrait)",
    ):
        if contract not in profile_host:
            raise AssertionError(f"Unified Compose profile screen lost profile behavior: {contract}")
    for legacy_profile_host in (
        "ProfileDropdownController", "LinearLayout(", "ProfileSettingsComposeHost(",
        "profileList.view", "categoryTabsController!!.view", "createView(ctx)",
    ):
        if legacy_profile_host in profile_host:
            raise AssertionError(f"Profile screen still has a legacy layout bridge: {legacy_profile_host}")

    profile_list = (JAVA / "fragment/ProfileSettingsComposeList.kt").read_text(encoding="utf-8")
    for direct_list_contract in ("fun Content(modifier: Modifier = Modifier)", "LazyColumn(", "rememberLazyListState()"):
        if direct_list_contract not in profile_list:
            raise AssertionError(f"Profile list is not rendered directly in the shared Compose root: {direct_list_contract}")
    if "ComposeView(" in profile_list or "ProfileSettingsComposeHost" in profile_list:
        raise AssertionError("Profile list must not create another Android/Compose host inside the profile screen")

    palette_path = JAVA / "view/FilamentPaletteController.kt"
    palette_source = palette_path.read_text(encoding="utf-8")
    for contract in (
        "class FilamentPaletteController", "@Composable\n    fun Content(modifier: Modifier = Modifier)",
        "FilamentPaletteContent(", "horizontalScroll(rememberScrollState())",
        "FilamentSlotSheet(", "Prefs.getFilamentSlots()", "Prefs.setFilamentSlots(slots)",
        "FilamentPalettePolicy.canAddSlot", "FilamentPalettePolicy.canDeleteAt", "Role.Button",
    ):
        if contract not in palette_source:
            raise AssertionError(f"Compose filament palette is missing {contract}")
    for legacy_palette_host in ("FrameLayout", "ComposeView(", "IThemeView"):
        if legacy_palette_host in palette_source:
            raise AssertionError(f"Filament palette still has a nested Android host: {legacy_palette_host}")
    if "filamentPaletteController?.Content(" not in profile_host:
        raise AssertionError("Filament palette must render directly in the shared Compose profile root")
    if "BelowFilamentPaletteContent(" not in profile_host or "AndroidView(" in profile_host:
        raise AssertionError("Profile quick controls must render directly in the shared Compose profile root")
    for legacy_palette_view in ("HorizontalScrollView(", "LinearLayout(", "TextView(", "GradientDrawable("):
        if legacy_palette_view in palette_source:
            raise AssertionError(f"Filament palette still uses legacy controls: {legacy_palette_view}")
    for language in ("values/strings.xml", "values-de/strings.xml"):
        resource_source = (ROOT / "app/src/main/res" / language).read_text(encoding="utf-8")
        for translated_label in ("FilamentPaletteAdd", "FilamentPaletteSlotDescription"):
            if f'name="{translated_label}"' not in resource_source:
                raise AssertionError(f"The {language} translation is missing {translated_label}")
    print_config_tools_test = (ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/fragment/PrintConfigToolsLogicTest.kt").read_text(encoding="utf-8")
    if "filamentSyncRequiresAConfiguredPrinterHost()" not in print_config_tools_test:
        raise AssertionError("Print-profile sync visibility must be covered for missing and configured printer hosts")

    profile_rows = JAVA / "fragment/ProfileSettingsComposeList.kt"
    profile_row_source = profile_rows.read_text(encoding="utf-8")
    option_element = (JAVA / "fragment/ProfileOptionElement.kt").read_text(encoding="utf-8")
    option_presenter = (JAVA / "fragment/ProfileOptionComposePresenter.kt").read_text(encoding="utf-8")
    direct_policy = (JAVA / "fragment/ProfileOptionDirectComposePolicy.kt").read_text(encoding="utf-8")
    for contract in (
        "ProfileOptionDirectComposePolicy.Kind.BOOLEAN",
        "ProfileOptionDirectComposePolicy.Kind.NUMBER",
        "ProfileOptionDirectComposePolicy.Kind.ENUM",
        "ProfileOptionDirectComposePolicy.Kind.OPEN_ENUM",
        "ProfileOptionDirectComposePolicy.Kind.ENUM_VECTOR",
        "ProfileOptionDirectComposePolicy.Kind.VECTOR",
        "ProfileOptionDirectComposePolicy.Kind.POINT",
        "ProfileOptionDirectComposePolicy.Kind.COLOR",
        "ProfileOptionDirectComposePolicy.Kind.STRING_VECTOR",
        "ProfileOptionDirectComposePolicy.Kind.STRING",
        "ProfileOptionDirectComposePolicy.Kind.FALLBACK",
        "ComposeProfileBooleanOption.Content(",
        "ComposeProfileNumericOption.Content(",
        "ComposeProfileEnumOption.Content(",
        "ComposeProfileOpenOption.Content(",
        "ComposeProfileColorOption.Content(",
        "ComposeProfileEnumVectorOption.Content(",
        "ComposeProfileVectorOption.Content(",
        "ComposeProfilePointOption.Content(",
        "ComposeProfileStringVectorOption.Content(",
        "ComposeProfileStringOption.Content(",
        "ComposeProfileFallbackOption.Content(",
    ):
        if contract not in option_presenter:
            raise AssertionError(f"Common profile setting rows are missing direct Compose rendering: {contract}")
    for contract in (
        "fun usesDirectCompose()",
        "fun DirectContent(",
        "onEditorStateChanged: (Boolean) -> Unit = {},",
        "if (ProfileOptionComposePresenter.supportsDirect(definition)) return null",
    ):
        if contract not in option_element:
            raise AssertionError(f"Profile settings still allocate nested views for a direct Compose row: {contract}")
    for contract in (
        "ProfileSettingsRowRenderPolicy.usesDirectCompose(",
        "option.DirectContent(bindVersion, index, onEditorStateChanged)",
        "item.Content(index, onEditorStateChanged)",
    ):
        if contract not in profile_row_source:
            raise AssertionError(f"Profile settings lost a direct Compose renderer: {contract}")
    if "AndroidView(" in profile_row_source:
        raise AssertionError("Profile settings must not route rows through an AndroidView compatibility wrapper.")
    for contract in (
        "definition.type == ConfigOptionDef.ConfigOptionType.BOOLS",
        "integerOpenEnum || floatOpenEnum -> Kind.OPEN_ENUM",
        "definition.type == ConfigOptionDef.ConfigOptionType.ENUMS && enumChoices -> Kind.ENUM_VECTOR",
        "ProfileStringVectorCodec.parse(rawValue) != null -> Kind.STRING_VECTOR",
        "else -> Kind.FALLBACK",
    ):
        if contract not in direct_policy:
            raise AssertionError(f"Direct profile renderer policy is missing {contract}")

    file_menu_java = JAVA / "components/bed_menu/FileMenu.java"
    file_menu_path = JAVA / "components/bed_menu/FileMenu.kt"
    if file_menu_java.exists():
        raise AssertionError("The live File menu still has a Java implementation")
    if not file_menu_path.exists():
        raise AssertionError("The Kotlin File menu is missing")
    file_menu_source = file_menu_path.read_text(encoding="utf-8")
    object_actions_kt = JAVA / "components/bed_menu/FileMenuObjectActions.kt"
    if not object_actions_kt.exists():
        raise AssertionError("Kotlin object-action controller is missing")
    object_actions_source = object_actions_kt.read_text(encoding="utf-8")
    delegated_actions = (
        "showCopyMoveDialog", "showObjectsDialog", "showSelectedObjectActions",
        "showObjectFilamentSlotDialog", "showDuplicateDialog", "showMoveToPlateDialog",
    )
    for action in delegated_actions:
        if f"objectActions()?.{action}(view)" not in file_menu_source:
            raise AssertionError(f"The live File menu no longer delegates {action} to Kotlin")
        if f"fun {action}(view: View)" not in object_actions_source:
            raise AssertionError(f"Kotlin object actions are missing {action}")
    object_action_contracts = (
        "model?.getObjectName(index)", "MenuFileObjectNumber", "setSingleChoiceItems(objects, selected)",
        "SelectedObjectChangedEvent()", "coerceIn(1, 50)", "minOf(4, slots.size)",
        "setObjectBaseFilament(selected, filament)", "(0 until plateCount).filter { it != current }",
        "moveSelectedObjectToPlate(target)", "centerSelectedOnBed", "deleteSelectedModel", "arrange()",
    )
    missing_object_contracts = [contract for contract in object_action_contracts if contract not in object_actions_source]
    if missing_object_contracts:
        raise AssertionError("Object actions lost behavior during Kotlin migration: " + ", ".join(missing_object_contracts))

    file_menu_contracts = (
        "class FileMenu : ListBedMenu()", "Intent.EXTRA_ALLOW_MULTIPLE, true",
        "override fun onCreateView(ctx: Context, portrait: Boolean): ThemeAwareComposeView",
        "BedMenuActionsContent(composeActions.value, portrait)", "private fun createComposeActions",
        "R.string.WorkspaceAddModel", "R.string.WorkspaceObjects", "R.string.WorkspaceArrange",
        "internal fun createOptionsItems",
        "R.string.MenuFileCalibrations", "R.string.MenuFileImportProfiles",
        "R.string.MenuFileExportProfiles", "R.string.MenuFileExport3mf",
        "TransformScaleMenu(bed)", "OrientationPositionMenu(bed)", "OrientationRotationMenu(bed)",
        "@EventHandler(runOnMainThread = true)", "class AIGeneratorMenu : UnfoldMenu()",
        "MediaStore.ACTION_IMAGE_CAPTURE", "Intent.ACTION_GET_CONTENT",
        "AIGeneratorMenuContent(", "AIGeneratorActionRow(", "LinearProgressIndicator(",
        "remainingLabel = remainingLabel", "quotaProgress = quotaProgress",
        "NeedDismissAIGeneratorMenu", "CloudModelsRemainingCountUpdatedEvent",
        "SliceBeam.EVENT_BUS.registerListener(this)", "SliceBeam.EVENT_BUS.unregisterListener(this)",
        "if (total <= 0) 0f", "aiGenerationQuotaProgress(total, generated)",
    )
    missing_file_menu_contracts = [item for item in file_menu_contracts if item not in file_menu_source]
    if missing_file_menu_contracts:
        raise AssertionError("File-menu routes or AI-generator behavior are missing: " + ", ".join(missing_file_menu_contracts))
    transform_menu_source = (JAVA / "components/bed_menu/TransformMenuItems.kt").read_text(encoding="utf-8")
    if "R.string.ModelUndo" not in transform_menu_source or "R.string.ModelRedo" not in transform_menu_source:
        raise AssertionError("Undo and redo actions are missing from the edit menu")
    options_menu_path = JAVA / "components/bed_menu/OptionsMenu.kt"
    if not options_menu_path.exists():
        raise AssertionError("The direct Options destination is missing from the workspace menu")
    options_menu_source = options_menu_path.read_text(encoding="utf-8")
    for contract in (
        "class OptionsMenu : ListBedMenu()", "actionSource.createOptionsItems()",
        "BedMenuActionsContent(composeActions.value, portrait)",
        "fun onSelectionChanged(event: SelectedObjectChangedEvent)",
        "fun onObjectsChanged(event: ObjectsListChangedEvent)",
    ):
        if contract not in options_menu_source:
            raise AssertionError(f"The direct Options destination is missing: {contract}")
    for legacy_ai_view in ("FadeRecyclerView", "SimpleRecyclerAdapter", "PreferenceItem", "SegmentsView"):
        if legacy_ai_view in file_menu_source:
            raise AssertionError(f"The AI-generator submenu still uses the legacy {legacy_ai_view} UI")

    calibration_menus_path = JAVA / "components/bed_menu/CalibrationMenus.kt"
    if not calibration_menus_path.exists():
        raise AssertionError("Kotlin calibration menus are missing")
    calibration_menus_source = calibration_menus_path.read_text(encoding="utf-8")
    for legacy_nested_menu in (
        "public final class CalibrationsMenu",
        "public final static class CalibrationModelsMenu",
    ):
        if legacy_nested_menu in file_menu_source:
            raise AssertionError("Calibration UI remains embedded in the Java File menu")
    calibration_contracts = (
        "class CalibrationsMenu : UnfoldMenu()", "R.string.CalibrationPressureAdvance",
        "SliceBeam.PENDING_CALIB_MODE = 1", "SliceBeam.PENDING_CALIB_START = 0.0",
        "SliceBeam.PENDING_CALIB_END = 0.1", "SliceBeam.PENDING_CALIB_STEP = 0.002",
        "model.objectsCount == 0", "class CalibrationModelsMenu : UnfoldMenu()",
        "OrcaCube_v2.stl", "OrcaPlug_v2.stl", "BedFragment.ModelLoadCallback",
        "loadNextModel(bed, files, index + 1)", "SliceBeam.EVENT_BUS.registerListener(this)",
        "SliceBeam.EVENT_BUS.unregisterListener(this)",
    )
    missing_calibration_contracts = [item for item in calibration_contracts if item not in calibration_menus_source]
    if missing_calibration_contracts:
        raise AssertionError("Calibration behavior changed during Kotlin migration: " + ", ".join(missing_calibration_contracts))
    for contract in (
        "ThemeAwareComposeView(ctx)", "CalibrationsMenuContent(", "CalibrationModelsContent(",
        "@Composable\nprivate fun CalibrationsMenuContent(",
        "@Composable\nprivate fun CalibrationModelsContent(",
        "CalibrationBackBar(onBack)",
    ):
        if contract not in calibration_menus_source:
            raise AssertionError(f"Calibration Compose UI is missing {contract}")
    for legacy_view in ("FadeRecyclerView", "SimpleRecyclerAdapter", "RecyclerView", "createBackBar"):
        if legacy_view in calibration_menus_source:
            raise AssertionError(f"Calibration menus still use the legacy {legacy_view} view scaffolding")
    list_bed_menu_source = (JAVA / "components/bed_menu/ListBedMenu.kt").read_text(encoding="utf-8")
    for legacy_view in ("FadeRecyclerView", "SimpleRecyclerAdapter", "RecyclerView"):
        if legacy_view in list_bed_menu_source:
            raise AssertionError(f"ListBedMenu still owns the unused {legacy_view} scaffold")

    transform_menu_java = JAVA / "components/bed_menu/TransformMenu.java"
    transform_menu_path = JAVA / "components/bed_menu/TransformMenu.kt"
    if transform_menu_java.exists():
        raise AssertionError("The active Transform-menu coordinator still has a Java implementation")
    if not transform_menu_path.exists():
        raise AssertionError("The Kotlin Transform-menu coordinator is missing")
    transform_menu_source = transform_menu_path.read_text(encoding="utf-8")
    transform_items_path = JAVA / "components/bed_menu/TransformMenuItems.kt"
    if not transform_items_path.exists():
        raise AssertionError("Kotlin transform-menu item builder is missing")
    transform_items_source = transform_items_path.read_text(encoding="utf-8")
    if "composeActions.value = items.createItems()" not in transform_menu_source:
        raise AssertionError("The active Transform menu no longer delegates Compose item creation to Kotlin")
    if "BedMenuActionsContent(composeActions.value, portrait, multiRow = true)" not in transform_menu_source:
        raise AssertionError("The active Transform menu no longer renders its responsive Compose actions")
    if "menuItems?.onSelectionChanged()" not in transform_menu_source or "menuItems?.onObjectsChanged()" not in transform_menu_source:
        raise AssertionError("Transform selection and model events no longer update Compose action state")
    if "ListBedMenu" in transform_menu_source and "SimpleRecyclerAdapter" in transform_menu_source:
        raise AssertionError("Transform menu still renders native RecyclerView rows")
    transform_routes = (
        "MenuOrientationArrange", "MenuOrientationAutoOrient", "MenuToolbarMove", "MenuToolbarRotate",
        "MenuTransformScale", "ModelSimplify", "MenuOrientationFlatten", "MenuTransformCut",
        "MenuToolbarSplit", "MenuToolbarVariableLayerHeight", "MenuToolbarSupportPaint",
        "MenuToolbarSeamPaint", "MenuToolbarFuzzyPaint", "MenuToolbarColorPaint", "MenuToolbarEmboss",
        "MenuToolbarMeasure",
    )
    missing_transform_routes = [route for route in transform_routes if route not in transform_items_source]
    if missing_transform_routes:
        raise AssertionError("Transform-menu routes were lost during Kotlin migration: " + ", ".join(missing_transform_routes))
    transform_contracts = (
        "showPositionMenu.accept(it)", "showRotationMenu.accept(it)", "showScaleMenu.accept(it)",
        "showCutMenu.accept(it)", "enabled = availability.hasSelectedObject", "modelSelectionAvailability(",
        "renderer.setInFlattenMode(flattenEnabled)", "fun onSelectionChanged()",
        "selectedTriangleCount", "ratios[choice]", "coerceIn(4, triangles - 1)",
    )
    missing_transform_contracts = [contract for contract in transform_contracts if contract not in transform_items_source]
    if missing_transform_contracts:
        raise AssertionError("Transform-menu behavior changed during Kotlin migration: " + ", ".join(missing_transform_contracts))
    for contract in ("new OrientationPositionMenu(bed)", "new OrientationRotationMenu(bed)"):
        kotlin_contract = contract.replace("new ", "")
        if kotlin_contract not in transform_menu_source:
            raise AssertionError(f"Transform menu must pass its active bed to the Kotlin submenu: {contract}")
    if "TransformCutMenu(bed)" not in transform_menu_source:
        raise AssertionError("Transform menu no longer opens the Kotlin cut editor with its active bed")
    for event_contract in (
        "@EventHandler(runOnMainThread = true)",
        "fun onObjectsChanged(event: ObjectsListChangedEvent)",
        "fun onSelectionChanged(event: SelectedObjectChangedEvent)",
        "fun onFlattenModeReset(event: FlattenModeResetEvent)",
    ):
        if event_contract not in transform_menu_source:
            raise AssertionError(f"Kotlin Transform menu is missing its selection update route: {event_contract}")
    scale_menu_path = JAVA / "components/bed_menu/TransformScaleMenu.kt"
    if not scale_menu_path.exists():
        raise AssertionError("The Kotlin transform scale editor is missing")
    scale_menu_source = scale_menu_path.read_text(encoding="utf-8")
    scale_menu_contracts = (
        "class TransformScaleMenu(private val bed: BedFragment) : UnfoldMenu()",
        "fun setLinked(linked: Boolean)", "Prefs.isScaleInputInMM()", "Prefs.setScaleInputInMM",
        "Prefs.isScaleLinked()", "Prefs.setScaleLinked", "model.ensureOnBed(index)",
        "resolveTransformScaleValues(", "TransformScalePolicy.moveRuler(",
        "TransformScaleManualDialog(", "SteppedRuler(",
        "renderer.setSelectionScale(dx, dy, dz)",
        "SliceBeam.EVENT_BUS.registerListener(this)", "SliceBeam.EVENT_BUS.unregisterListener(this)",
        "fun onSelectedObjectChanged(event: SelectedObjectChangedEvent)",
    )
    missing_scale_contracts = [contract for contract in scale_menu_contracts if contract not in scale_menu_source]
    if missing_scale_contracts:
        raise AssertionError("Transform scale behavior changed during Kotlin migration: " + ", ".join(missing_scale_contracts))
    legacy_scale_widgets = (
        "PositionScrollView", "PreferenceSwitchItem", "BeamAlertDialogBuilder",
        "LinearLayout(", "TextView(", "EditText(", "setOnClickListener",
    )
    remaining_scale_widgets = [widget for widget in legacy_scale_widgets if widget in scale_menu_source]
    if remaining_scale_widgets:
        raise AssertionError("Transform scale editor still uses legacy Android widgets: " + ", ".join(remaining_scale_widgets))
    shared_ruler_path = JAVA / "components/bed_menu/SteppedRuler.kt"
    if not shared_ruler_path.exists():
        raise AssertionError("The shared Compose transform ruler is missing")
    shared_ruler_source = shared_ruler_path.read_text(encoding="utf-8")
    for contract in (
        "TransformScalePolicy.drag(", "TransformScalePolicy.settle(",
        "detectHorizontalDragGestures(", "ProgressBarRangeInfo(", "setProgress {",
    ):
        if contract not in shared_ruler_source:
            raise AssertionError(f"Shared Compose transform ruler is missing required behavior: {contract}")
    if "TransformScaleMenu(bed)" not in transform_menu_source:
        raise AssertionError("Transform menu no longer opens the Kotlin scale editor")
    file_menu_source = (JAVA / "components/bed_menu/FileMenu.kt").read_text(encoding="utf-8")
    if "TransformScaleMenu(bed)" not in file_menu_source:
        raise AssertionError("File menu no longer opens the Kotlin scale editor")
    file_menu_compose_contracts = (
        "override fun onCreateView(ctx: Context, portrait: Boolean): ThemeAwareComposeView",
        "BeamComposeTheme(themeRevision = themedView.themeRevision)",
        "BedMenuActionsContent(composeActions.value, portrait)",
        "onCreateItems(portrait).map { menuItem ->",
        "menuItem.checkedChangeListener?.onCheckedChanged(menuItem.isChecked)",
        "menuItem.clickListener?.invoke(anchor)",
        "sectionBreakBefore = menuItem.sectionBreakBefore",
        "fun onObjectsChanged(event: ObjectsListChangedEvent)",
        "fun onSelectionChanged(event: SelectedObjectChangedEvent)",
        "fun onFeaturedUpdated(event: CloudFeaturesUpdatedEvent)",
    )
    missing_file_menu_contracts = [contract for contract in file_menu_compose_contracts if contract not in file_menu_source]
    if missing_file_menu_contracts:
        raise AssertionError("The file menu lost its adaptive Compose/action refresh routes: " + ", ".join(missing_file_menu_contracts))
    if "adapter?.setItems(onCreateItems" in file_menu_source:
        raise AssertionError("The primary file-menu action strip still relies on the legacy RecyclerView adapter")
    list_bed_menu_source = (JAVA / "components/bed_menu/ListBedMenu.kt").read_text(encoding="utf-8")
    if any(token in list_bed_menu_source for token in ("RecyclerView", "SimpleRecyclerAdapter", "SimpleRecyclerItem")):
        raise AssertionError("The shared bed-menu base still exposes the legacy RecyclerView action model")
    for contract in (
        "SliceBeam.EVENT_BUS.registerListener(this)",
        "SliceBeam.EVENT_BUS.unregisterListener(this)",
        "protected abstract fun onCreateItems(portrait: Boolean): List<BedMenuItem>",
    ):
        if contract not in list_bed_menu_source:
            raise AssertionError(f"Shared bed-menu lifecycle or Compose action model was lost: {contract}")
    bed_fragment_source = (JAVA / "fragment/BedFragment.kt").read_text(encoding="utf-8")
    if "TransformScaleMenu(this)" not in bed_fragment_source:
        raise AssertionError("Selected-object quick actions no longer open the Kotlin scale editor")
    if "new OrientationMenu().new PositionMenu()" in transform_menu_source or "new OrientationMenu().new RotationMenu()" in transform_menu_source:
        raise AssertionError("Transform menu is constructing a detached orientation-menu wrapper")

    bed_menu_item_source = (JAVA / "components/bed_menu/BedMenuItem.kt").read_text(encoding="utf-8")
    if any(token in bed_menu_item_source for token in ("SimpleRecyclerItem", "BedMenuItemHolderView", "RecyclerView", "Canvas(")):
        raise AssertionError("Bed-menu actions still contain the removed legacy Android view implementation")
    if "fun onCheckedChanged(checked: Boolean)" not in bed_menu_item_source:
        raise AssertionError("Compose toggle callbacks must receive the new checked state directly")
    menu_item_test = (ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/components/bed_menu/BedMenuItemTest.kt").read_text(encoding="utf-8")
    if "actionModelKeepsEnableToggleAndSectionStateWithoutCreatingAndroidViews" not in menu_item_test:
        raise AssertionError("Compose menu action state lacks a unit regression test")

    cut_menu_path = JAVA / "components/bed_menu/TransformCutMenu.kt"
    if not cut_menu_path.exists():
        raise AssertionError("The Compose transform cut editor is missing")
    cut_menu_source = cut_menu_path.read_text(encoding="utf-8")
    cut_menu_contracts = (
        "class TransformCutMenu(private val bed: BedFragment) : UnfoldMenu()",
        "renderer.showCutPlane(true)", "renderer.showCutPlane(false)",
        "setCutPlane(", "renderer.setCutConnectorPlacement(false)",
        "setCutConnectorDefaults(",
        "getCutConnectorCount(cutObject)", "removeLastCutConnectorForObject(cutObject)",
        "clearCutConnectorsForObject(cutObject)", "model.cut(",
        "renderer.clearAllCutConnectorPreviews()", "override fun getRequestedSize(into: FrameLayout, portrait: Boolean)",
        "ThemeAwareComposeView(ctx)", "BeamComposeTheme(themeRevision = themedView.themeRevision)",
        "verticalScroll(rememberScrollState())", "Slider(", "CutValueSlider(",
    )
    missing_cut_contracts = [item for item in cut_menu_contracts if item not in cut_menu_source]
    if missing_cut_contracts:
        raise AssertionError("Compose cut editor is missing behavior: " + ", ".join(missing_cut_contracts))
    legacy_cut_widgets = ("PositionScrollView", "TextView(", "LinearLayout(", "ScrollView(ctx)", "setOnClickListener")
    remaining_cut_widgets = [item for item in legacy_cut_widgets if item in cut_menu_source]
    if remaining_cut_widgets:
        raise AssertionError("The cut editor still creates legacy Android controls: " + ", ".join(remaining_cut_widgets))
    cut_policy_test = ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/components/bed_menu/TransformCutPolicyTest.kt"
    if not cut_policy_test.exists():
        raise AssertionError("The Compose cut editor lacks bounds and unit-conversion regression tests")

    orientation_menu_java = JAVA / "components/bed_menu/OrientationMenu.java"
    orientation_menu_path = JAVA / "components/bed_menu/OrientationMenu.kt"
    if orientation_menu_java.exists():
        raise AssertionError("The orientation-menu coordinator still has a Java implementation")
    if not orientation_menu_path.exists():
        raise AssertionError("The Kotlin orientation-menu coordinator is missing")
    orientation_menu_source = orientation_menu_path.read_text(encoding="utf-8")
    orientation_items_path = JAVA / "components/bed_menu/OrientationMenuItems.kt"
    if not orientation_items_path.exists():
        raise AssertionError("Kotlin orientation-menu item builder is missing")
    orientation_items_source = orientation_items_path.read_text(encoding="utf-8")
    if "composeActions.value = items.createItems()" not in orientation_menu_source:
        raise AssertionError("The active orientation menu no longer builds its Compose actions in Kotlin")
    if "BedMenuActionsContent(composeActions.value, portrait)" not in orientation_menu_source:
        raise AssertionError("The active orientation menu no longer renders its responsive Compose actions")
    orientation_events = (
        "menuItems?.onFlattenModeReset()",
        "menuItems?.onObjectsChanged()",
        "menuItems?.onSelectionChanged()",
    )
    if any(route not in orientation_menu_source for route in orientation_events):
        raise AssertionError("Orientation state events are not delegated to the Kotlin controller")
    orientation_contracts = (
        "MenuOrientationArrange", "MenuOrientationAutoOrient", "MenuOrientationFlatten",
        "MenuOrientationPosition", "MenuOrientationRotation", "MenuOrientationArrangeFinished",
        "MenuOrientationAutoOrientDone", "renderer.resetFlattenMode()", "renderer.setInFlattenMode(flattenEnabled)",
        "renderer.model!!.autoOrient(selected)", "showPositionMenu.accept(anchor)", "showRotationMenu.accept(anchor)",
        "modelSelectionAvailability(", "enabled = availability.hasSelectedObject",
    )
    missing_orientation_contracts = [item for item in orientation_contracts if item not in orientation_items_source]
    if missing_orientation_contracts:
        raise AssertionError("Orientation-menu behavior changed during Kotlin migration: " + ", ".join(missing_orientation_contracts))
    if "BedMenuItem" in orientation_items_source or "SimpleRecyclerAdapter" in orientation_items_source:
        raise AssertionError("Orientation action controller still creates native RecyclerView rows")
    availability_path = JAVA / "components/bed_menu/ModelSelectionAvailability.kt"
    availability_test = ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/components/bed_menu/ModelSelectionAvailabilityTest.kt"
    if not availability_path.exists() or not availability_test.exists():
        raise AssertionError("Model and selection enablement policy or its unit tests are missing")
    availability_source = availability_path.read_text(encoding="utf-8")
    availability_test_source = availability_test.read_text(encoding="utf-8")
    for contract in (
        "canArrange = modelLoaded", "hasSelectedObject = modelLoaded && selectedObjectIndex != -1",
    ):
        if contract not in availability_source:
            raise AssertionError(f"Model/selection enablement policy is missing {contract}")
    for test in (
        "missingModelDisablesArrangeAndEverySelectionAction()",
        "loadedModelWithoutSelectionOnlyEnablesArrange()",
        "loadedModelWithSelectedObjectEnablesSelectionActions()",
    ):
        if test not in availability_test_source:
            raise AssertionError(f"Model/selection enablement regression test is missing {test}")

    orientation_transforms_path = JAVA / "components/bed_menu/OrientationTransformMenus.kt"
    if not orientation_transforms_path.exists():
        raise AssertionError("Kotlin position and rotation submenus are missing")
    orientation_transforms_source = orientation_transforms_path.read_text(encoding="utf-8")
    for contract in (
        "open class OrientationPositionMenu", "open class OrientationRotationMenu",
        "setSelectionTranslation", "setSelectionRotation", "model.translate(index,",
        "Math.toRadians(target.x)", "model.ensureOnBed(index)",
        "onLongClickTranslation(event: LongClickTranslationEvent)",
        "onSelectedObjectChanged(event: SelectedObjectChangedEvent)",
        "MenuOrientationRotationApply", "SteppedRuler(",
    ):
        if contract not in orientation_transforms_source:
            raise AssertionError(f"Kotlin position/rotation menu is missing required behavior: {contract}")
    for legacy_widget in ("PositionScrollView", "android.widget.EditText", "makeBackToolbar(ctx"):
        if legacy_widget in orientation_transforms_source:
            raise AssertionError(f"Position/rotation menu still depends on the legacy Android widget: {legacy_widget}")
    for compatibility_contract in (
        "inner class PositionMenu : OrientationPositionMenu",
        "inner class RotationMenu : OrientationRotationMenu",
        "requireNotNull(this@OrientationMenu.fragment)",
    ):
        if compatibility_contract not in orientation_menu_source:
            raise AssertionError(f"Java callers lost a nested-menu compatibility entry point: {compatibility_contract}")

    smart_slice_java = JAVA / "components/SmartSliceDialog.java"
    smart_slice_path = JAVA / "components/SmartSliceDialog.kt"
    if smart_slice_java.exists():
        raise AssertionError("The Smart Slice dialog still has a Java implementation")
    if not smart_slice_path.exists():
        raise AssertionError("The Kotlin Smart Slice dialog is missing")
    smart_slice_source = smart_slice_path.read_text(encoding="utf-8")
    smart_slice_contracts = (
        "@JvmStatic", "fun show(fragment: BedFragment)",
        "SliceChoiceField", "SliceBeam.applyProfileSelection(printer = printer)",
        "SmartSliceEngine.analyze(geometry, intent, configSnapshot)",
        "geometry.modelRevision != activeModel.geometryRevision",
        "SmartSliceEngine.apply(result)", "R.string.SmartSliceApplyAndSlice",
        "R.string.SmartSliceApplyEdit", "RejectedExecutionException",
        "R.string.SmartSliceBusy", "SmartSliceInputValues.resolve",
        "ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool",
    )
    missing_smart_slice_contracts = [contract for contract in smart_slice_contracts if contract not in smart_slice_source]
    if missing_smart_slice_contracts:
        raise AssertionError("Smart Slice behavior changed during Kotlin migration: " + ", ".join(missing_smart_slice_contracts))
    if any(widget in smart_slice_source for widget in ("android.widget.Spinner", "android.widget.ScrollView", "android.widget.LinearLayout")):
        raise AssertionError("Smart Slice still uses the legacy native form controls")
    quick_slice_path = JAVA / "components/bed_menu/QuickSliceDialog.kt"
    if not quick_slice_path.exists():
        raise AssertionError("The Quick Slice Kotlin dialog is missing")
    quick_slice_source = quick_slice_path.read_text(encoding="utf-8")
    for contract in ("SliceChoiceField", "QuickSliceProfileValues.resolve", "ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool"):
        if contract not in quick_slice_source:
            raise AssertionError(f"Quick Slice dialog is missing required behavior: {contract}")
    if any(widget in quick_slice_source for widget in ("android.widget.Spinner", "android.widget.ScrollView", "android.widget.LinearLayout")):
        raise AssertionError("Quick Slice still uses the legacy native form controls")
    workspace_profile_path = JAVA / "components/WorkspaceProfileSheet.kt"
    workspace_profile_java = JAVA / "components/WorkspaceProfileSheet.java"
    if workspace_profile_java.exists() or not workspace_profile_path.exists():
        raise AssertionError("The workspace profile sheet is missing its Kotlin implementation")
    workspace_profile_source = workspace_profile_path.read_text(encoding="utf-8")
    workspace_profile_contracts = (
        "SliceChoiceField(", "WorkspaceProfileSelectionLogic.resolve(",
        "SliceBeam.applyProfileSelection(printer, filament, process)",
        "onProfilesChanged?.run()",
        "ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool",
    )
    missing_workspace_profile = [item for item in workspace_profile_contracts if item not in workspace_profile_source]
    if missing_workspace_profile:
        raise AssertionError("Workspace profile sheet lost required behavior: " + ", ".join(missing_workspace_profile))
    if any(widget in workspace_profile_source for widget in ("android.widget.Spinner", "android.widget.ScrollView", "android.widget.LinearLayout")):
        raise AssertionError("Workspace profile sheet still uses legacy native form controls")
    slice_menu_java = JAVA / "components/bed_menu/SliceMenu.java"
    slice_menu_path = JAVA / "components/bed_menu/SliceMenu.kt"
    if slice_menu_java.exists():
        raise AssertionError("The live Slice menu still has a Java implementation")
    if not slice_menu_path.exists():
        raise AssertionError("The Kotlin Slice menu coordinator is missing")
    slice_menu = slice_menu_path.read_text(encoding="utf-8")
    if "private final static class InfoMenu" in slice_menu:
        raise AssertionError("The Slice info panel still has a Java implementation")
    if "private final static class LayersMenu" in slice_menu:
        raise AssertionError("The layer-range editor still has a Java implementation")
    if "SliceMenuItems.createBeforeSlice(bed, hasModel)" not in slice_menu:
        raise AssertionError("The pre-slice menu is not routed to its Kotlin item builder")
    if "SliceMenuItems.createAfterSlice(bed, this)" not in slice_menu:
        raise AssertionError("The post-slice menu is not routed to its Kotlin item builder")
    if "SlicePrintPreparationDialog.show(" not in slice_menu:
        raise AssertionError("The print-preparation dialog is not routed to its Kotlin controller")
    if "SlicePreparationTools.readUsedTools(gcode)" not in slice_menu:
        raise AssertionError("The print-tool scan is not routed to its Kotlin helper")
    slice_menu_contracts = (
        "class SliceMenu : ListBedMenu()", "MmuFilamentSync.synchronize(printer)",
        "SliceUploadController.upload(", "SliceBeam.applyProfileSelection(printer = printer.title)",
        "override fun onCreateView(ctx: Context, portrait: Boolean): ThemeAwareComposeView",
        "BedMenuActionsContent(composeActions.value, portrait)", "fun refreshInfoItems()",
    )
    missing_slice_menu = [item for item in slice_menu_contracts if item not in slice_menu]
    if missing_slice_menu:
        raise AssertionError("Slice menu coordinator behavior changed during Kotlin migration: " + ", ".join(missing_slice_menu))
    slice_info_path = JAVA / "components/bed_menu/SliceInfoMenu.kt"
    slice_info_values_path = JAVA / "components/bed_menu/SliceInfoValues.kt"
    if not slice_info_path.exists() or not slice_info_values_path.exists():
        raise AssertionError("Kotlin Slice info panel or its tested value helpers are missing")
    slice_info_source = slice_info_path.read_text(encoding="utf-8")
    slice_info_contracts = (
        "class SliceInfoMenu(private val parentMenu: SliceMenu) : UnfoldMenu()",
        "ThemeAwareComposeView(ctx)", "BeamComposeTheme(themeRevision = themeRevision)",
        "SliceInfoContent(", "SliceInfoTimeline(", "Canvas(", "SegmentsView.mapColor(role)",
        "R.array.GCodeColorSchemes", "GCodeViewer.OPTION_TYPE_SEAMS",
        "R.string.SliceSummaryTimeValue", "R.string.SliceSummaryWeightValue",
        "R.string.SliceSummaryLengthValue", "R.string.SliceSummaryLayersValue",
        "R.string.SliceSummaryCostValue", "toggleExtrusionRoleVisible(role)",
        "viewer.setLayersViewRange(range.first, range.second)",
        "sliceInfoSegmentValues(roleTimes, visibleRoles)",
    )
    missing_slice_info = [item for item in slice_info_contracts if item not in slice_info_source]
    if missing_slice_info:
        raise AssertionError("Slice info behavior changed during Kotlin migration: " + ", ".join(missing_slice_info))
    if "AndroidView(" in slice_info_source:
        raise AssertionError("The G-code timeline should be drawn directly by Compose")
    if any(legacy in slice_info_source for legacy in ("NestedScrollView(", "MaterialCheckBox(", "LinearLayout(ctx)")):
        raise AssertionError("Slice info's standard menu controls have not been fully moved into Compose")
    if "canToggleSliceRole(" not in slice_info_source:
        raise AssertionError("The Compose G-code role toggles must preserve the last visible role")
    slice_info_values_source = slice_info_values_path.read_text(encoding="utf-8")
    if "fun sliceInfoSegmentValues(" not in slice_info_values_source or "object SliceInfoFormatter" not in slice_info_values_source:
        raise AssertionError("Slice info segment and duration helpers are missing")
    if "fun refreshInfoItems()" not in slice_menu:
        raise AssertionError("Kotlin Slice info color changes cannot refresh the active Slice menu")

    camera_menu_path = JAVA / "components/bed_menu/CameraMenu.kt"
    camera_geometry_path = JAVA / "components/bed_menu/CameraPresetGeometry.kt"
    camera_test_path = ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/components/bed_menu/CameraPresetGeometryTest.kt"
    if not camera_menu_path.exists() or not camera_geometry_path.exists() or not camera_test_path.exists():
        raise AssertionError("Compose camera menu or its tested preset geometry is missing")
    camera_menu_source = camera_menu_path.read_text(encoding="utf-8")
    camera_geometry_source = camera_geometry_path.read_text(encoding="utf-8")
    camera_contracts = (
        "open class CameraMenu : BedMenu()", "ThemeAwareComposeView(ctx)",
        "BeamComposeTheme(themeRevision = themedView.themeRevision)", "CameraMenuContent(",
        "CameraPresetGeometry.resolve(", "setSingleChoiceItems(", "Prefs.setCameraControlMode(which)",
        "Prefs.setOrthoProjectionEnabled(enabled)", "renderer.updateProjection()", "requestRender()",
        "R.string.MenuCameraIsometric", "R.string.MenuCameraTop", "R.string.MenuCameraBottom",
        "R.string.MenuCameraFront", "R.string.MenuCameraBack", "R.string.MenuCameraLeft",
        "R.string.MenuCameraRight", "R.string.MenuCameraControlMode", "R.string.MenuCameraOrtho",
        "BedMenuActionsContent(actions, portrait, multiRow = true)",
    )
    missing_camera = [item for item in camera_contracts if item not in camera_menu_source]
    if missing_camera:
        raise AssertionError("Camera actions or responsive Compose behavior changed: " + ", ".join(missing_camera))
    if "ListBedMenu" in camera_menu_source or "BedMenuItem" in camera_menu_source:
        raise AssertionError("Camera menu still depends on legacy RecyclerView rows")
    for preset in ("ISOMETRIC", "TOP", "BOTTOM", "FRONT", "BACK", "LEFT", "RIGHT"):
        if f"CameraPreset.{preset}" not in camera_geometry_source:
            raise AssertionError(f"Camera preset geometry is missing {preset}")
    camera_test_source = camera_test_path.read_text(encoding="utf-8")
    for test_name in (
        "isometricViewTargetsTheBuildVolumeCenter", "topAndBottomViewsClearTheBuildVolume",
        "frontAndBackViewsUseTheCorrectBedEdges", "leftAndRightViewsUseTheCorrectBedEdges",
    ):
        if f"fun {test_name}()" not in camera_test_source:
            raise AssertionError(f"Camera preset geometry regression is missing {test_name}")

    slice_layers_path = JAVA / "components/bed_menu/SliceLayersMenu.kt"
    slice_layers_values_path = JAVA / "components/bed_menu/SliceLayersValues.kt"
    if not slice_layers_path.exists() or not slice_layers_values_path.exists():
        raise AssertionError("Kotlin Compose layer-range menu or its range normalization helper is missing")
    slice_layers_source = slice_layers_path.read_text(encoding="utf-8")
    slice_layers_contracts = (
        "class SliceLayersMenu : UnfoldMenu()", "ThemeAwareComposeView(ctx)", "BeamComposeTheme",
        "SliceLayersContent(", "Slider(", "layerSliderFraction(", "layerFromSliderFraction(",
        "viewer.applyPerformanceMode(Prefs.isPerformanceModeEnabled())",
        "viewer.setLayersViewRange(from.toLong() - 1L, to.toLong() - 1L)",
        "normalizeSliceLayerRange(viewer.getLayersCount(), range.first, range.second)",
        "cancelPendingApply()", "ViewUtils.postOnMainThread(callback, 50L)",
    )
    missing_slice_layers = [item for item in slice_layers_contracts if item not in slice_layers_source]
    if missing_slice_layers:
        raise AssertionError("Slice layer-range interaction or Compose migration is incomplete: " + ", ".join(missing_slice_layers))
    if any(legacy in slice_layers_source for legacy in ("AndroidView(", "PositionScrollView")):
        raise AssertionError("The slice layer-range controls must be rendered directly by Compose")
    slice_layers_values = slice_layers_values_path.read_text(encoding="utf-8")
    if any(helper not in slice_layers_values for helper in (
        "normalizeSliceLayerRange(", "layerFromSliderFraction(", "layerSliderFraction(",
        "withSliceLayerFrom(", "withSliceLayerTo(",
    )):
        raise AssertionError("Slice layer slider mapping or ordered range helpers are missing")
    theme_compose_path = JAVA / "ui/ThemeAwareComposeView.kt"
    theme_compose_source = theme_compose_path.read_text(encoding="utf-8") if theme_compose_path.exists() else ""
    if (
        "open class ThemeAwareComposeView(context: Context) : AbstractComposeView(context), IThemeView" not in theme_compose_source
        or "override fun Content()" not in theme_compose_source
        or "private var contentState:" not in theme_compose_source
        or "private val composeView = ComposeView(context)" in theme_compose_source
        or "FrameLayout(context)" in theme_compose_source
    ):
        raise AssertionError("Theme-aware screens must use a direct Compose root without an extra Android container")

    slice_items_path = JAVA / "components/bed_menu/SliceMenuItems.kt"
    if not slice_items_path.exists():
        raise AssertionError("The Kotlin Slice menu item builder is missing")
    slice_items_source = slice_items_path.read_text(encoding="utf-8")
    slice_items_contracts = (
        "object SliceMenuItems", "fun createBeforeSlice(fragment: BedFragment, hasModel: Boolean): List<BedComposeMenuAction>",
        "QuickSliceDialog.show(fragment, view)", "SmartSliceDialog.show(fragment)",
        "fun createAfterSlice(", "SliceInfoMenu(parentMenu)", "SliceLayersMenu()",
        "Intent.ACTION_CREATE_DOCUMENT", "Intent.ACTION_SEND_MULTIPLE",
        "Prefs.isPerformanceModeEnabled()", "toggleable = true", "R.string.MenuSliceSendToPrinter",
        "R.string.MenuSliceSendToPrinterAndPrint", "sendAction.send(selectedType, host!!, apiKey, printer, true)",
    )
    missing_slice_items = [item for item in slice_items_contracts if item not in slice_items_source]
    if missing_slice_items:
        raise AssertionError("Slice menu items or actions changed during Kotlin migration: " + ", ".join(missing_slice_items))

    preparation_path = JAVA / "components/bed_menu/SlicePrintPreparationDialog.kt"
    preparation_tools_path = JAVA / "components/bed_menu/SlicePreparationTools.kt"
    if not preparation_path.exists() or not preparation_tools_path.exists():
        raise AssertionError("Kotlin print-preparation UI or its helpers are missing")
    preparation_source = preparation_path.read_text(encoding="utf-8")
    preparation_contracts = (
        "object SlicePrintPreparationDialog", "ComposeView(context)", "BeamComposeTheme {",
        "PrintPreparationContent(", "R.string.PrintPrepareTitle",
        "R.string.PrintPrepareAutoLevelEnabled", "R.string.PrintPrepareAutoLevelUnavailable",
        "MmuFilamentAssignmentDialog.show", "fragment.startSlicing", "sendAction.send(",
        "R.string.PrintPrepareReslicing", "selectedPrinter.value", "mappingRevision.intValue++",
    )
    missing_preparation = [item for item in preparation_contracts if item not in preparation_source]
    if missing_preparation:
        raise AssertionError("Print-preparation actions or checks changed during Kotlin migration: " + ", ".join(missing_preparation))
    preparation_tools_source = preparation_tools_path.read_text(encoding="utf-8")
    for preparation_helper in (
        "fun isAnycubicKobra3Model(", "fun isSendTarget(",
        "fun hasAutomaticBedLeveling(", "fun didPrinterChange(", "fun readMaxTool(",
    ):
        if preparation_helper not in preparation_tools_source:
            raise AssertionError(f"Kotlin print preparation helper is missing: {preparation_helper}")
    for legacy_preparation_view in ("android.widget.LinearLayout", "android.widget.ScrollView", "android.widget.TextView"):
        if legacy_preparation_view in preparation_source:
            raise AssertionError("Print-preparation dialog still contains its legacy native-view implementation")
    preparation_test_path = ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/components/bed_menu/SlicePreparationToolsTest.kt"
    preparation_test_source = preparation_test_path.read_text(encoding="utf-8")
    for test_name in (
        "acceptsOnlyConfiguredPrinterUploadProtocolsAndNonEmptyHosts()",
        "detectsAutoLevelCommandInStartGcodeWithoutMatchingItsNameAsPartOfAnotherWord()",
        "printerSelectionChangeIsComparedToTheActiveProfileTitle()",
    ):
        if test_name not in preparation_test_source:
            raise AssertionError(f"Print-preparation state regression test is missing: {test_name}")

    upload_path = JAVA / "components/bed_menu/SliceUploadController.kt"
    if not upload_path.exists():
        raise AssertionError("The Kotlin post-slice upload controller is missing")
    upload_source = upload_path.read_text(encoding="utf-8")
    upload_contracts = (
        "object SliceUploadController", "setTimeout(300000)", "setConnectTimeout(15000)",
        "/api/files/local", "/server/files/upload", "/printer/print/start",
        'params.put("root", "gcodes")', 'params.put("select", print.toString())',
        "GCodeIntegrityValidator.validate(source)", "GCodeIntegrityValidator.validate(uploadFile!!)",
        'startsWith("anycubic kobra 3")', "MmuFilamentSync.getSelectedGates(",
        "AnycubicGCodeProcessor.rewrite(source, prepared, scan, options)",
        "updateSendProgress(fragment, progress, bytesWritten, totalSize)",
    )
    missing_upload = [item for item in upload_contracts if item not in upload_source]
    if missing_upload:
        raise AssertionError("Printer upload or G-code preparation behavior changed during Kotlin migration: " + ", ".join(missing_upload))

    slice_layers_path = JAVA / "components/bed_menu/SliceLayersMenu.kt"
    if not slice_layers_path.exists():
        raise AssertionError("The Kotlin layer-range editor is missing")
    slice_layers_source = slice_layers_path.read_text(encoding="utf-8")
    slice_layers_contracts = (
        "class SliceLayersMenu : UnfoldMenu()", "R.string.MenuSliceInfoLayers",
        "viewer.setLayersViewRange(from.toLong() - 1L, to.toLong() - 1L)",
        "viewer.applyPerformanceMode(Prefs.isPerformanceModeEnabled())",
        "fun cancelPendingApply()", "ViewUtils.removeCallbacks(it)",
        "finishLayerSliderInteraction()", "withSliceLayerFrom(", "withSliceLayerTo(",
        "override fun getRequestedSize(",
    )
    missing_slice_layers = [item for item in slice_layers_contracts if item not in slice_layers_source]
    if missing_slice_layers:
        raise AssertionError("Layer-range editing behavior changed during Kotlin migration: " + ", ".join(missing_slice_layers))
    slice_layers_values_test = ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/components/bed_menu/SliceLayersValuesTest.kt"
    slice_layers_test_source = slice_layers_values_test.read_text(encoding="utf-8")
    for test_name in (
        "sliderFractionsMapToOneBasedLayersAndClampToTheTrack()",
        "sliderFractionAndLayerRoundTripAcrossLargeLayerCounts()",
        "sliderEndpointsStayOrderedAndWithinThePrintableLayerRange()",
    ):
        if test_name not in slice_layers_test_source:
            raise AssertionError(f"Compose layer slider regression test is missing: {test_name}")

    kromacut_java = JAVA / "components/KromacutEditorMenu.java"
    kromacut_path = JAVA / "components/KromacutEditorMenu.kt"
    if kromacut_java.exists():
        raise AssertionError("The bundled Kromacut editor menu still has a Java implementation")
    if not kromacut_path.exists():
        raise AssertionError("The Kotlin Kromacut editor menu is missing")
    kromacut_source = kromacut_path.read_text(encoding="utf-8")
    kromacut_contracts = (
        "const val REQUEST_CODE_FILE = 15", "@JvmStatic", "fun handleActivityResult(",
        "WebViewAssetLoader.Builder()", "assets/kromacut-editor/index.html",
        "settings.javaScriptEnabled = true", "settings.allowFileAccess = false",
        "addJavascriptInterface(DownloadBridge(), \"OrcaMobile\")", "onShowFileChooser(",
        "ImageToStlConverter.saveGeneratedFile", "Base64.decode(base64, Base64.DEFAULT)",
        "override fun onDestroy()", "destroy()",
    )
    missing_kromacut_contracts = [item for item in kromacut_contracts if item not in kromacut_source]
    if missing_kromacut_contracts:
        raise AssertionError("Kromacut editor behavior was lost during Kotlin migration: " + ", ".join(missing_kromacut_contracts))

    cloud_java = JAVA / "components/CloudManageBottomSheet.java"
    cloud_path = JAVA / "components/CloudManageBottomSheet.kt"
    if cloud_java.exists():
        raise AssertionError("The cloud account sheet still has a Java implementation")
    if not cloud_path.exists():
        raise AssertionError("The Kotlin cloud account sheet is missing")
    cloud_source = cloud_path.read_text(encoding="utf-8")
    cloud_state = (JAVA / "components/CloudManageScreenState.kt").read_text(encoding="utf-8")
    cloud_contracts = (
        "class CloudManageBottomSheet(context: Context) : BottomSheetDialog(context)",
        "ComposeView(context)", "BeamComposeTheme {", "CloudManageContent(",
        "Switch(", "TextButton(", "CloudManageScreenState.resolve(",
        "SettingsCloudManageLoggedInAs", "CloudController.getUserFeatures()",
        "Prefs.isCloudProfileSyncEnabled()", "Prefs.setCloudProfileSyncEnabled(isChecked)",
        "CloudController.notifyDataChanged()", "NeedDismissSnackbarEvent(CloudController.CLOUD_SYNC_TAG)",
        "SettingsCloudManageSubscription", "CloudController.logout()", "dismiss()",
        "uri.scheme !in setOf(\"https\", \"http\")", "ActivityNotFoundException",
    )
    missing_cloud_contracts = [item for item in cloud_contracts if item not in cloud_source]
    if missing_cloud_contracts:
        raise AssertionError("Cloud account actions were lost during Kotlin migration: " + ", ".join(missing_cloud_contracts))
    if "RecyclerView" in cloud_source or "SimpleRecyclerAdapter" in cloud_source:
        raise AssertionError("Cloud management sheet still contains a RecyclerView implementation")
    for cloud_state_contract in (
        "data class CloudManageScreenState(", "it.level != -1 && it.level <= currentLevel",
        ".maxByOrNull { it.level }", "currentLevel >= features.syncRequiredLevel",
        "isLoading = true", "activeLevel?.manageUrl?.takeIf(String::isNotBlank)",
    ):
        if cloud_state_contract not in cloud_state:
            raise AssertionError(f"Cloud-management state lost an entitlement or loading rule: {cloud_state_contract}")
    cloud_state_test = (ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/components/CloudManageScreenStateTest.kt").read_text(encoding="utf-8")
    for cloud_test_contract in (
        "selectsHighestEligibleSubscriptionAndChecksSyncEntitlement()",
        "hidesSyncAndSubscriptionWhenAccountHasNoEligibleEntitlements()",
        "missingCloudDataShowsLoadingInsteadOfThrowing()",
    ):
        if cloud_test_contract not in cloud_state_test:
            raise AssertionError(f"Cloud-management entitlement regression test is missing: {cloud_test_contract}")
    for locale in ("values", "values-de", "values-ru"):
        cloud_strings = (ROOT / f"app/src/main/res/{locale}/strings.xml").read_text(encoding="utf-8")
        if 'name="CloudManageLinkOpenFailed"' not in cloud_strings:
            raise AssertionError(f"Cloud subscription-link failure message is missing from {locale}")

    color_chooser_java = JAVA / "components/ColorChooserSheet.java"
    color_chooser_path = JAVA / "components/ColorChooserSheet.kt"
    if color_chooser_java.exists():
        raise AssertionError("The color chooser still has a Java implementation")
    if not color_chooser_path.exists():
        raise AssertionError("The Kotlin color chooser is missing")
    color_chooser_source = color_chooser_path.read_text(encoding="utf-8")
    color_chooser_contracts = (
        "open class ColorChooserSheet(", "ComposeView(context)", "BeamComposeTheme {",
        "ColorChooserContent(", "setViewCompositionStrategy(",
        "ColorChooserPalette.commonColors()", "ColorChooserPalette.allColors()",
        "ColorChooserPalette.columnsForWidth(configuration.screenWidthDp)",
        "ColorPickerPopUp.OnPickColorListener", "callback?.onColorChosen(color or OPAQUE_ALPHA)",
        "rememberScrollState()", "ColorSwatchGrid(", "ColorUtils.calculateLuminance(opaqueColor)",
        "clickable(role = Role.Button, onClick = onClick)",
    )
    missing_color_chooser_contracts = [item for item in color_chooser_contracts if item not in color_chooser_source]
    if missing_color_chooser_contracts:
        raise AssertionError("Color chooser lost its Compose, palette, theme or callback behavior: " + ", ".join(missing_color_chooser_contracts))
    color_palette_source = color_chooser_source
    for contract in (
        "fun commonColors(): List<Int> = CssColors.COMMON.mapNotNull",
        "fun allColors(): List<Int> = CssColors.ALL.values.toList()",
        ".coerceIn(MIN_COLUMNS, MAX_COLUMNS)",
    ):
        if contract not in color_palette_source:
            raise AssertionError(f"Responsive color palette contract is missing: {contract}")
    color_palette_test = (ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/components/ColorChooserPaletteTest.kt").read_text(encoding="utf-8")
    for test_name in (
        "commonColorsRemainCompleteAndInTheDeclaredOrder()",
        "cssPaletteRemainsComprehensiveAndOpaque()",
        "swatchColumnsAdaptToPhoneAndTabletWidths()",
    ):
        if test_name not in color_palette_test:
            raise AssertionError(f"Color chooser palette regression test is missing: {test_name}")

    filament_slot_java = JAVA / "components/FilamentSlotSheet.java"
    filament_slot_path = JAVA / "components/FilamentSlotSheet.kt"
    if filament_slot_java.exists():
        raise AssertionError("The filament-slot editor still has a Java implementation")
    if not filament_slot_path.exists():
        raise AssertionError("The Kotlin filament-slot editor is missing")
    filament_slot_source = filament_slot_path.read_text(encoding="utf-8")
    filament_slot_contracts = (
        "open class FilamentSlotSheet(", "ComposeView(context)", "BeamComposeTheme {",
        "FilamentSlotContent(", "ColorChooserSheet(context, color)",
        "slot.color = selectedColor", "items(materialTypes)", "slot.type = option",
        "if (canDelete)", "onDelete = {", "listener?.onDelete()", "dismiss()",
    )
    missing_filament_slot_contracts = [item for item in filament_slot_contracts if item not in filament_slot_source]
    if missing_filament_slot_contracts:
        raise AssertionError("Filament-slot actions or color/material callbacks were lost: " + ", ".join(missing_filament_slot_contracts))
    for legacy_filament_slot_view in ("android.widget.LinearLayout", "GradientDrawable(", "BeamAlertDialogBuilder("):
        if legacy_filament_slot_view in filament_slot_source:
            raise AssertionError("Filament-slot editor still contains its legacy native-view implementation")

    preslice_source = (JAVA / "components/PreSliceFilamentDialog.kt").read_text(encoding="utf-8")
    for contract in (
        "ComposeView(context)", "BeamComposeTheme {", "PreSliceFilamentContent(",
        "PreSliceFilamentAssignments.initialSlotIndices(",
        "PreSliceFilamentAssignments.resolve(",
        "R.string.PreSliceAllObjectsMode", "R.string.PreSliceIndividualMode",
        "model?.getObjectName(objectIndex)", "callback.onSelected(result)",
    ):
        if contract not in preslice_source:
            raise AssertionError(f"Pre-slice filament routing lost its Compose UI or assignment behavior: {contract}")
    for legacy_preslice_view in ("android.widget.LinearLayout", "RadioGroup(", "ScrollView(context)"):
        if legacy_preslice_view in preslice_source:
            raise AssertionError("Pre-slice filament routing still contains its legacy native-view implementation")
    preslice_test = (ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/components/PreSliceFilamentAssignmentsTest.kt").read_text(encoding="utf-8")
    for test_name in (
        "appSlotsMapToZeroBasedGcodeToolsAndClampUnavailableTools()",
        "oneSlotModeCopiesTheSelectionAcrossEveryObjectWithoutMutatingInput()",
        "assignmentModeDetectsUniformAndIndividualObjectSlots()",
    ):
        if test_name not in preslice_test:
            raise AssertionError(f"Pre-slice filament assignment regression test is missing: {test_name}")

    mmu_assignment_source = (JAVA / "components/MmuFilamentAssignmentDialog.kt").read_text(encoding="utf-8")
    for contract in (
        "ComposeView(context)", "BeamComposeTheme {", "MmuAssignmentContent(",
        "MmuFilamentSync.loadedGates(printerTitle)",
        "MmuFilamentSync.getSelectedGates(printerTitle, toolCount)",
        "MmuFilamentSync.setSelectedGates(printerTitle, result)",
        "R.string.MmuAssignmentContinue",
        "callback.onSelected(result.clone())",
    ):
        if contract not in mmu_assignment_source:
            raise AssertionError(f"ACE gate assignment lost its Compose UI or saved mapping behavior: {contract}")
    for legacy_mmu_view in ("android.widget.LinearLayout", "ScrollView(context)", "TextView(context)"):
        if legacy_mmu_view in mmu_assignment_source:
            raise AssertionError("ACE gate assignment still contains its legacy native-view implementation")

    cloud_api_java = JAVA / "cloud/CloudAPI.java"
    cloud_api_path = JAVA / "cloud/CloudAPI.kt"
    if cloud_api_java.exists():
        raise AssertionError("The cloud API contract still has a Java implementation")
    if not cloud_api_path.exists():
        raise AssertionError("The Kotlin cloud API contract is missing")
    cloud_api_source = cloud_api_path.read_text(encoding="utf-8")
    cloud_api_contracts = (
        "interface CloudAPI : APIRunner", "@JvmField", "APILibrary.newRunner(",
        "https://api.beam3d.ru/v1/", "SliceBeam v${BuildConfig.VERSION_NAME}/${BuildConfig.VERSION_CODE}",
        "@ru.ytkab0bp.sapil.Method(\"login/begin\")",
        "@ru.ytkab0bp.sapil.Method(requestType = ru.ytkab0bp.sapil.RequestType.POST, value = \"sync/upload\")",
        "@ru.ytkab0bp.sapil.Method(requestType = ru.ytkab0bp.sapil.RequestType.POST, value = \"models/generate\")",
        "class LoginData", "class UserInfo", "class UserFeatures", "class SyncState",
    )
    missing_cloud_api_contracts = [item for item in cloud_api_contracts if item not in cloud_api_source]
    if missing_cloud_api_contracts:
        raise AssertionError("Cloud API routes or data contracts were lost during Kotlin migration: " + ", ".join(missing_cloud_api_contracts))

    cloud_controller_java = JAVA / "cloud/CloudController.java"
    cloud_controller_path = JAVA / "cloud/CloudController.kt"
    if cloud_controller_java.exists():
        raise AssertionError("The cloud sync and login coordinator still has a Java implementation")
    if not cloud_controller_path.exists():
        raise AssertionError("The Kotlin cloud sync and login coordinator is missing")
    cloud_controller_source = cloud_controller_path.read_text(encoding="utf-8")
    cloud_controller_contracts = (
        "open class CloudController", "companion object", "const val CLOUD_SYNC_TAG",
        "private const val OFFLINE_BOOTSTRAP = true", "@JvmStatic", "fun initCached()",
        "fun beginLogin()", "fun checkUserFeatures()", "fun notifyDataChanged()",
        "CloudAPI.INSTANCE.syncGetState(", "CloudAPI.INSTANCE.syncUpload(",
        "CloudSyncConflictChooseRemote", "CloudSyncConflictChooseLocal",
    )
    missing_cloud_controller_contracts = [item for item in cloud_controller_contracts if item not in cloud_controller_source]
    if missing_cloud_controller_contracts:
        raise AssertionError("Cloud coordinator behavior was lost during Kotlin migration: " + ", ".join(missing_cloud_controller_contracts))

    changelog_java = JAVA / "components/ChangeLogBottomSheet.java"
    changelog_path = JAVA / "components/ChangeLogBottomSheet.kt"
    if changelog_java.exists():
        raise AssertionError("The change-log sheet still has a Java implementation")
    if not changelog_path.exists():
        raise AssertionError("The Kotlin change-log sheet is missing")
    changelog_source = changelog_path.read_text(encoding="utf-8")
    changelog_contracts = (
        "class ChangeLogBottomSheet(context: Context) : BottomSheetDialog(context)",
        "ComposeView(context)", "BeamComposeTheme", "ChangeLogContent(",
        "loadReleaseNotes(context)", "ChangeLogContentResolver.localizedText(",
        "BeamServerData.isBoostyAvailable()",
        "supporters = data.boostySubscribers.shuffled()", "BeamServerDataUpdatedEvent",
        "SliceBeam.EVENT_BUS.registerListener(this)", "SliceBeam.EVENT_BUS.unregisterListener(this)",
        "BottomSheetBehavior.STATE_EXPANDED", "https://boosty.to/ytkab0bp",
    )
    missing_changelog_contracts = [item for item in changelog_contracts if item not in changelog_source]
    if missing_changelog_contracts:
        raise AssertionError("Change-log behavior was lost during Kotlin migration: " + ", ".join(missing_changelog_contracts))
    if any(token in changelog_source for token in ("ViewPager(", "ScrollView(", "TextView(", "BoostySubsView(")):
        raise AssertionError("Change-log content still uses the legacy Android view implementation.")
    for locale in ("values", "values-de", "values-ru"):
        strings = (ROOT / "app/src/main/res" / locale / "strings.xml").read_text(encoding="utf-8")
        if 'name="ChangelogSupportLinkOpenFailed"' not in strings:
            raise AssertionError(f"Localized change-log support-link error is missing from {locale}")
    changelog_ui = (JAVA / "ui/components/ChangeLogContent.kt").read_text(encoding="utf-8")
    for contract in (
        "fun ChangeLogContent(", "HorizontalPager(", "LazyColumn(",
        "R.string.ChangelogBoostyDescription",
        "R.string.IntroBoostySupport", "R.string.ChangelogNext", "R.string.ChangelogOK",
    ):
        if contract not in changelog_ui:
            raise AssertionError("Change-log Compose screen lost a page or action: " + contract)

    animator_java = JAVA / "recycler/CubicBezierItemAnimator.java"
    animator_path = JAVA / "recycler/CubicBezierItemAnimator.kt"
    if animator_java.exists():
        raise AssertionError("The profile-list item animator still has a Java implementation")
    if not animator_path.exists():
        raise AssertionError("The Kotlin cubic-bezier item animator is missing")
    animator_source = animator_path.read_text(encoding="utf-8")
    animator_contracts = (
        "class CubicBezierItemAnimator : SimpleItemAnimator()", "setAddDuration(ANIMATION_DURATION)",
        "setRemoveDuration(ANIMATION_DURATION)", "ViewUtils.CUBIC_INTERPOLATOR",
        "override fun runPendingAnimations()", "override fun animateRemove(",
        "override fun animateAdd(", "override fun animateMove(", "override fun animateChange(",
        "override fun endAnimation(", "override fun endAnimations()", "override fun isRunning()",
        "dispatchRemoveStarting(holder)", "dispatchAddStarting(holder)", "dispatchMoveStarting(holder)",
        "dispatchChangeStarting(oldHolder, true)", "dispatchChangeStarting(newHolder, false)",
        "ViewCompat.postOnAnimationDelayed", "override fun canReuseUpdatedViewHolder(",
        "payloads.isNotEmpty() || super.canReuseUpdatedViewHolder(viewHolder, payloads)",
        "const val ANIMATION_DURATION = 180L",
    )
    missing_animator_contracts = [item for item in animator_contracts if item not in animator_source]
    if missing_animator_contracts:
        raise AssertionError("Profile-list animation behavior changed during Kotlin migration: " + ", ".join(missing_animator_contracts))

    profile_fragment_source = (JAVA / "fragment/ProfileListFragment.kt").read_text(encoding="utf-8")
    profile_filter_source = (JAVA / "fragment/ProfileSettingsFilterController.kt").read_text(encoding="utf-8")
    empty_state_contracts = (
        "Box(modifier = Modifier.weight(1f).fillMaxWidth())", "private fun updateEmptyState()",
        "profileFilterController?.emptyStateMessage(getContext() ?: return)",
        "recyclerView?.setEmptyMessage(message)",
    )
    missing_empty_state = [item for item in empty_state_contracts if item not in profile_fragment_source]
    if missing_empty_state:
        raise AssertionError("Profile settings no longer explain empty filters: " + ", ".join(missing_empty_state))
    compose_profile_list = (JAVA / "fragment/ProfileSettingsComposeList.kt").read_text(encoding="utf-8")
    if "ProfileSettingsRowSaveableKey.create(entry.index, entry.value)" not in compose_profile_list or "option:$it" not in compose_profile_list:
        raise AssertionError("Compose profile rows must use stable Bundle-saveable keys, never ProfileOptionWrapper instances")
    if "ProfileSettingsComposeHost" in compose_profile_list or "ComposeView(" in compose_profile_list:
        raise AssertionError("Profile settings should share the screen's bounded Compose root without nested host views")
    compose_empty_state_contracts = (
        "private var emptyMessage by mutableStateOf<String?>(null)", "LazyColumn(",
        "if (rows.isEmpty() && emptyMessage != null)", "align(Alignment.Center)",
        "fun setEmptyMessage(message: CharSequence?)",
    )
    missing_compose_empty_state = [item for item in compose_empty_state_contracts if item not in compose_profile_list]
    if missing_compose_empty_state:
        raise AssertionError("Compose profile settings no longer explain empty filters: " + ", ".join(missing_compose_empty_state))
    profile_render_contracts = (
        "internal object ProfileSettingsRowRenderPolicy",
        "ProfileSettingsRowRenderPolicy.usesDirectCompose(",
        "option.DirectContent(bindVersion, index, onEditorStateChanged)",
        "item.Content(index, onEditorStateChanged)",
        "option.bindRowPosition(index)",
    )
    missing_profile_render = [item for item in profile_render_contracts if item not in compose_profile_list]
    if missing_profile_render:
        raise AssertionError("Profile settings rows must render directly through Compose: " + ", ".join(missing_profile_render))
    if "AndroidView(" in compose_profile_list:
        raise AssertionError("Profile settings rows must not be wrapped in legacy AndroidView fallbacks.")
    compose_item_contract = (JAVA / "fragment/ProfileSettingsComposeItem.kt").read_text(encoding="utf-8")
    if "interface ProfileSettingsComposeItem" not in compose_item_contract or "fun Content(rowPosition: Int" not in compose_item_contract:
        raise AssertionError("Shared profile rows must expose a typed composable content contract.")
    profile_option = (JAVA / "fragment/ProfileOptionElement.kt").read_text(encoding="utf-8")
    if "ProfileSettingsComposeItem?" not in profile_option or "fun bindRowPosition(position: Int)" not in profile_option:
        raise AssertionError("Profile options must bind their row position without creating nested ComposeViews.")
    compose_option_files = sorted((JAVA / "ui/settings").glob("ComposeProfile*Option.kt"))
    if not compose_option_files:
        raise AssertionError("Compose profile option implementations are missing.")
    for option_file in compose_option_files:
        option_source = option_file.read_text(encoding="utf-8")
        if "override fun Content(rowPosition: Int" not in option_source:
            raise AssertionError(f"{option_file.name} does not render as a shared Compose profile row.")
        if "SimpleRecyclerItem<ComposeView>" in option_source or "onCreateView(" in option_source:
            raise AssertionError(f"{option_file.name} still creates a nested Android/Compose view wrapper.")
    printer_config_source = (JAVA / "fragment/PrinterConfigFragment.kt").read_text(encoding="utf-8")
    if "ProfileActionItem(" not in printer_config_source or "PreferenceItem()" in printer_config_source:
        raise AssertionError("Printer web and filament-sync actions must use the direct Compose profile-row model")
    setting_items = (JAVA / "fragment/ProfileSettingItems.kt").read_text(encoding="utf-8")
    space_item = (JAVA / "recycler/SpaceItem.kt").read_text(encoding="utf-8")
    if "class ProfileActionItem(" not in setting_items or "ProfileSettingsComposeItem" not in setting_items:
        raise AssertionError("Printer actions must use the typed direct Compose profile-row model.")
    if "class SpaceItem" not in space_item or "ProfileSettingsComposeItem" not in space_item:
        raise AssertionError("Profile spacers must use the typed direct Compose row model.")
    cloud_header = (JAVA / "ui/settings/ComposeCloudProfileHeader.kt").read_text(encoding="utf-8")
    cloud_avatar_contracts = (
        "private fun CloudProfileAvatar(",
        "rememberGlideBitmap(avatarUrl, 26.dp, GlideBitmapShape.CIRCLE)",
    )
    missing_cloud_avatar = [item for item in cloud_avatar_contracts if item not in cloud_header]
    if missing_cloud_avatar or "AndroidView(" in cloud_header or "ImageView" in cloud_header:
        detail = ", ".join(missing_cloud_avatar) or "legacy Android image view remains"
        raise AssertionError("Cloud profile avatars must be Compose-rendered and cleared with composition: " + detail)
    profile_render_test = ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/fragment/ProfileSettingsRowRenderPolicyTest.kt"
    if not profile_render_test.exists():
        raise AssertionError("Compose profile-row direct/fallback rendering tests are missing")
    filter_message_contracts = (
        "fun emptyStateMessage(context: Context)", "R.string.ProfileSettingsEmptySearch",
        "R.string.ProfileSettingsEmptyFavorites", "R.string.ProfileSettingsEmptySimple",
        "R.string.ProfileSettingsEmptyGeneral",
    )
    missing_filter_messages = [item for item in filter_message_contracts if item not in profile_filter_source]
    if missing_filter_messages:
        raise AssertionError("Profile filter empty-state messages are missing: " + ", ".join(missing_filter_messages))
    german_strings = (ROOT / "app/src/main/res/values-de/strings.xml").read_text(encoding="utf-8")
    translated_empty_state_names = (
        "ProfileSettingsEmptySearch", "ProfileSettingsEmptyFavorites",
        "ProfileSettingsEmptySimple", "ProfileSettingsEmptyGeneral",
    )
    if any(f'name="{name}"' not in german_strings for name in translated_empty_state_names):
        raise AssertionError("German translations for profile filter empty states are incomplete")
    german_resources = ET.fromstring(german_strings)
    german_text = {
        item.attrib["name"]: "".join(item.itertext()).strip()
        for item in german_resources.findall("string")
    }
    german_ui_translations = {
        # "Maker" is the requested product-area name, not an untranslated fallback.
        "SlotMaker": "Maker",
        "MakerTitle": "Maker",
        "MenuFileCalibrationsModelsBox": "Würfel",
        "MenuOrientationRotation": "Drehung",
        "WorkspaceArrange": "Anordnen",
        "IntroStart": "Loslegen",
        "IntroEarlyAccess": "Vorabversion",
        "SettingsInterfaceThemeSystem": "Wie auf dem Gerät",
        "SettingsCloudManageFeatureEarlyAccess": "Frühzugang",
    }
    untranslated_ui_labels = [
        f"{name}={german_text.get(name)!r} (expected {expected!r})"
        for name, expected in german_ui_translations.items()
        if german_text.get(name) != expected
    ]
    if untranslated_ui_labels:
        raise AssertionError("German UI labels fell back to English: " + ", ".join(untranslated_ui_labels))

    settings_java = JAVA / "fragment/SettingsFragment.java"
    settings_path = JAVA / "fragment/SettingsFragment.kt"
    if settings_java.exists():
        raise AssertionError("App settings still have a Java screen implementation")
    if not settings_path.exists():
        raise AssertionError("The Kotlin app settings screen is missing")
    settings_source = settings_path.read_text(encoding="utf-8")
    settings_contracts = (
        "class SettingsFragment : Fragment()", "ComposeView(ctx)", "BeamComposeTheme",
        "SettingsContent(", "expandedSections by mutableStateOf(emptySet<Int>())",
        "SettingsInterfaceTheme", "SettingsInterfaceColor", "SettingsInterfaceResolutionScale",
        "ConverterStorage.getRootDisplayName(ctx)", "REQUEST_CODE_CONVERTER_ROOT_SETTINGS",
        "SetupActivity.EXTRA_ADD_PRINTER", "SetupActivity.EXTRA_ABOUT",
        "SetupActivity.EXTRA_CLOUD_PROFILE", "SetupActivity.EXTRA_BOOSTY_ONLY",
        "SliceBeam.EVENT_BUS.registerListener(this)", "SliceBeam.EVENT_BUS.unregisterListener(this)",
        "fun refreshConverterFolderSetting()", "fun onDataUpdated(event: BeamServerDataUpdatedEvent)",
        "fun onUserInfoUpdated(event: CloudUserInfoUpdatedEvent)",
    )
    missing_settings_contracts = [item for item in settings_contracts if item not in settings_source]
    if missing_settings_contracts:
        raise AssertionError("Compose app settings lost an existing action or lifecycle route: " + ", ".join(missing_settings_contracts))
    for setting_label in (
        "SettingsScreenDescription", "SettingsDevices", "SettingsCloudProfile",
        "SettingsCloudProfileDescription", "SettingsAddPrinterDescription",
        "SettingsAppInformation", "SettingsAboutDescription",
    ):
        if f'name="{setting_label}"' not in german_strings:
            raise AssertionError(f"German app-settings label is missing: {setting_label}")

    safe_start_java = JAVA / "SafeStartActivity.java"
    safe_start_path = JAVA / "SafeStartActivity.kt"
    if safe_start_java.exists():
        raise AssertionError("The crash-recovery screen still has a Java implementation")
    if not safe_start_path.exists():
        raise AssertionError("The Kotlin crash-recovery screen is missing")
    safe_start_source = safe_start_path.read_text(encoding="utf-8")
    safe_start_contracts = (
        "class SafeStartActivity : ComponentActivity()", "R.string.AppCrashedDesc",
        'Prefs.getPrefs()?.getString("crash", "").orEmpty()', "Intent.ACTION_SEND",
        "Intent.EXTRA_TEXT", "Intent.createChooser(sendIntent, null)",
        'Prefs.getPrefs()?.edit()?.remove("crash")?.apply()',
        "Intent(this@SafeStartActivity, MainActivity::class.java)",
    )
    missing_safe_start = [item for item in safe_start_contracts if item not in safe_start_source]
    if missing_safe_start:
        raise AssertionError("Crash-recovery actions changed during Kotlin migration: " + ", ".join(missing_safe_start))
    safe_start_screen_source = (JAVA / "ui/SafeStartScreen.kt").read_text(encoding="utf-8")
    for contract in ("R.string.AppCrashedShare", "R.string.AppCrashedRestart"):
        if contract not in safe_start_screen_source:
            raise AssertionError(f"Crash-recovery Compose actions lost their localized labels: {contract}")

    theme_java = JAVA / "theme/BeamTheme.java"
    theme_path = JAVA / "theme/BeamTheme.kt"
    if theme_java.exists():
        raise AssertionError("The shared light/dark palette still has a Java implementation")
    if not theme_path.exists():
        raise AssertionError("The Kotlin light/dark palette is missing")
    theme_source = theme_path.read_text(encoding="utf-8")
    theme_contracts = (
        "class BeamTheme", "@JvmField\n        val LIGHT", "@JvmField\n        val DARK",
        "var nameRes: Int = 0", "var colors: SparseIntArray = SparseIntArray()",
        "colors = LIGHT.colors.clone()", "Prefs.getAccentColor()",
        "R.attr.gcodeViewerPerimeter", "R.attr.gcodeViewerWipeTower",
        "android.R.attr.textColorPrimary", "android.R.attr.windowBackground",
        "R.attr.defaultBedColor", "R.attr.snackbarError",
    )
    missing_theme_contracts = [item for item in theme_contracts if item not in theme_source]
    if missing_theme_contracts:
        raise AssertionError("Theme colors or Java-visible fields changed during Kotlin migration: " + ", ".join(missing_theme_contracts))
    if theme_source.count("colors.put(") < 62:
        raise AssertionError("The light/dark palette lost color entries")

    themes_repo_java = JAVA / "theme/ThemesRepo.java"
    themes_repo_path = JAVA / "theme/ThemesRepo.kt"
    if themes_repo_java.exists():
        raise AssertionError("The shared theme resolver still has a Java implementation")
    if not themes_repo_path.exists():
        raise AssertionError("The Kotlin shared theme resolver is missing")
    themes_repo_source = themes_repo_path.read_text(encoding="utf-8")
    themes_repo_contracts = (
        "object ThemesRepo", "fun getCurrent(): BeamTheme", "fun resetSystemResolvedTheme()",
        "fun getColor(resourceId: Int)", "fun invalidate(activity: Activity)",
        "fun invalidateView(view: View?)", "if (view == null) return", "Prefs.ThemeMode.SYSTEM", "BeamTheme.DARK",
        "BeamTheme.LIGHT", "resolvedSystemMode = null", "activity.onApplyTheme()",
        "invalidateView(activity.window.decorView)", "view.adapter?.notifyDataSetChanged()",
    )
    missing_themes_repo = [item for item in themes_repo_contracts if item not in themes_repo_source]
    if missing_themes_repo:
        raise AssertionError("Theme resolution or invalidation behavior changed during Kotlin migration: " + ", ".join(missing_themes_repo))
    if themes_repo_source.count("@JvmStatic") != 5:
        raise AssertionError("Java callers need all five public ThemesRepo methods to remain static")

    event_contracts = {
        "CloudUserInfoUpdatedEvent": (
            "open class CloudUserInfoUpdatedEvent",
        ),
        "LongClickTranslationEvent": (
            "open class LongClickTranslationEvent(",
            "@JvmField val x: Double", "@JvmField val y: Double", "@JvmField val visual: Boolean",
        ),
        "NeedDismissSnackbarEvent": (
            "open class NeedDismissSnackbarEvent(@JvmField val tag: String?)",
        ),
        "NeedSnackbarEvent": (
            "open class NeedSnackbarEvent",
            "@JvmField val title: CharSequence?", "@JvmField var type: SnackbarsLayout.Type",
            "@JvmField var tag: String?", "@JvmField var buttonTitle: CharSequence?",
            "@JvmField var buttonClick: View.OnClickListener?",
            "constructor(type: SnackbarsLayout.Type, title: Int, vararg args: Any?)",
            "open fun tag(tag: String?): NeedSnackbarEvent",
            "open fun button(title: Int, click: View.OnClickListener?): NeedSnackbarEvent",
        ),
        "EmbossSurfaceClickedEvent": (
            "open class EmbossSurfaceClickedEvent(",
            "@JvmField val position: Vec3d", "@JvmField val normal: Vec3d",
        ),
        "MeasurePointsChangedEvent": (
            "open class MeasurePointsChangedEvent(",
            "@JvmField val pointA: Vec3d?", "@JvmField val pointB: Vec3d?",
        ),
        "SlicingProgressEvent": (
            "open class SlicingProgressEvent(",
            "@JvmField val progress: Int", "@JvmField val message: String?",
        ),
    }
    for event_name, contracts in event_contracts.items():
        java_event = JAVA / f"events/{event_name}.java"
        kotlin_event = JAVA / f"events/{event_name}.kt"
        if java_event.exists():
            raise AssertionError(f"{event_name} still has a Java implementation")
        if not kotlin_event.exists():
            raise AssertionError(f"{event_name} Kotlin implementation is missing")
        event_source = kotlin_event.read_text(encoding="utf-8")
        missing_event_contracts = [item for item in contracts if item not in event_source]
        if missing_event_contracts:
            raise AssertionError(f"{event_name} lost its Java event contract: " + ", ".join(missing_event_contracts))

    marker_events = (
        "BeamServerDataUpdatedEvent", "CloudFeaturesUpdatedEvent", "CloudLoginStateUpdatedEvent",
        "CloudModelsRemainingCountUpdatedEvent", "CloudSyncFinishedEvent", "FlattenModeResetEvent",
        "NeedDismissAIGeneratorMenu", "NeedDismissCalibrationsMenu", "ObjectsListChangedEvent",
        "SelectedObjectChangedEvent",
    )
    for event_name in marker_events:
        java_event = JAVA / f"events/{event_name}.java"
        kotlin_event = JAVA / f"events/{event_name}.kt"
        if java_event.exists():
            raise AssertionError(f"{event_name} still has a Java implementation")
        if not kotlin_event.exists():
            raise AssertionError(f"{event_name} Kotlin implementation is missing")
        if f"open class {event_name}" not in kotlin_event.read_text(encoding="utf-8"):
            raise AssertionError(f"{event_name} no longer preserves its extensible Java class contract")

    slice_progress_source = (JAVA / "components/SliceProgressBottomSheet.kt").read_text(encoding="utf-8")
    slice_progress_state = (JAVA / "components/SliceProgressState.kt").read_text(encoding="utf-8")
    for progress_contract in (
        "class SliceProgressBottomSheet(context: Context) : BottomSheetDialog(context)",
        "SliceProgressContent(progressState)", "LazyColumn(", "LinearProgressIndicator(",
        "LaunchedEffect(state.messages.size)", "BottomSheetBehavior.STATE_EXPANDED",
        "SliceBeam.EVENT_BUS.registerListener(eventBridge)",
        "SliceBeam.EVENT_BUS.unregisterListener(eventBridge)",
        "if (progressState.completed) dismiss()",
        "localizeProgressMessage(Slic3rLocalization.getString(message))",
    ):
        if progress_contract not in slice_progress_source:
            raise AssertionError(f"Compose slicing progress lost required behavior: {progress_contract}")
    if "RecyclerView" in slice_progress_source or "ProgressLinesAdapter" in slice_progress_source:
        raise AssertionError("The slicing progress dialog still has a RecyclerView presentation")
    for state_contract in (
        "val progressMessage = event.message.orEmpty()", "event.progress.coerceIn(0, 100)",
        "event.progress >= 100", "localizedMessage != messages.lastOrNull()",
    ):
        if state_contract not in slice_progress_state:
            raise AssertionError(f"Slicing progress state reducer lost behavior: {state_contract}")

    utility_contracts = {
        "slic3r/NativeEnumValueSanitizer": (
            "object NativeEnumValueSanitizer",
            "@JvmStatic",
            "fun normalize(value: String?, acceptedValues: Array<String>?, defaultValue: String?): String?",
            "acceptedValues.isNullOrEmpty()) return value",
        ),
        "slic3r/Slic3rUtils": (
            "open class Slic3rUtils",
            "private val NOZZLE_PATTERN: Pattern",
            "fun extractNozzle(name: String?): String?",
            "fun stripNozzleSuffix(name: String?): String?",
            "fun isPrinterCompatible(",
            "fun parseStringList(serialized: String?): List<String>",
            "fun calcViewNormalMatrix(viewMatrix: DoubleArray, worldMatrix: DoubleArray, normalMatrix: DoubleArray)",
            "fun unproject(",
            "class ConfigChecker(config: String?)",
            "@Throws(Slic3rRuntimeError::class)",
            "fun eval(condition: String?): String?",
        ),
        "slic3r/Slic3rLocalization": (
            "open class Slic3rLocalization",
            "constructor(key: String) : this(",
            "@JvmStatic\n        fun getString(key: String): String",
            "@JvmStatic\n        fun getInstance(key: String): Slic3rLocalization",
            "fun get(key: String): String",
            "StandardCharsets.UTF_8",
            "flushEntry()",
            "prusaSlicerNamePattern",
        ),
        "slic3r/GCodeProcessorResult": (
            "open class GCodeProcessorResult",
            "@JvmField\n    internal var pointer: Long",
            "open fun getUsedFilamentMM(@GCodeViewer.ExtrusionRole role: Int): Double",
            "open fun getUsedFilamentG(@GCodeViewer.ExtrusionRole role: Int): Double",
            "open fun getRecommendedName(): String",
            "open fun release()",
            "@Throws(Throwable::class)",
        ),
        "slic3r/PaintSession": (
            "open class PaintSession(model: Model, objIdx: Int, mode: Int)",
            "open fun isValid(): Boolean",
            "open fun raycast(origin: DoubleArray?, dir: DoubleArray?): DoubleArray?",
            "open fun brush(",
            "open fun bucket(",
            "open fun height(",
            "open fun clear()",
            "open fun commit()",
            "open fun buildOverlay(gl: GLModel, filamentIdx: Int): Int",
            "open fun end()",
        ),
        "utils/DebugUtils": (
            "open class DebugUtils",
            "@JvmStatic",
            "fun assertTrue(value: Boolean)",
            "fun assertFalse(value: Boolean)",
            'throw AssertionError("Assert failed")',
        ),
        "utils/VibrationUtils": (
            "open class VibrationUtils",
            "@JvmStatic",
            "fun init(context: Context?)",
            "fun hasAmplitudeControl(): Boolean",
            "Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator!!.hasAmplitudeControl()",
        ),
        "slic3r/SliceListener": (
            "fun interface SliceListener",
            "fun onProgress(progress: Int, text: String?)",
        ),
        "smartslice/SmartSliceIntent": (
            "class SmartSliceIntent(",
            "@JvmField val purpose: Purpose", "@JvmField val priority: Priority",
            "@JvmField val requestedWallMm: Double?", "@JvmField val requestedLayerMm: Double?",
            "enum class Purpose", "enum class Priority",
        ),
        "smartslice/SmartSliceResult": (
            "class SmartSliceResult internal constructor(@JvmField val geometry: GeometryAnalysis)",
            "@JvmField val changes: LinkedHashMap<String, String>",
            "@JvmField val reasons: MutableList<String>", "@JvmField val warnings: MutableList<String>",
            "fun summary(): String", "fun changeSummary(before: Map<String, String>?): String",
            "String.format(\n                Locale.GERMANY",
        ),
        "smartslice/GeometryAnalysis": (
            "class GeometryAnalysis private constructor(",
            "@JvmField val objectCount: Int",
            "@JvmField val footprintMm2: Double",
            "@JvmField val volumeEnvelopeMm3: Double",
            "@JvmStatic\n        fun analyze(model: Model?): GeometryAnalysis",
            "private val CACHE = WeakHashMap<Model, CachedAnalysis>()",
        ),
        "smartslice/SmartSliceEngine": (
            "object SmartSliceEngine",
            "@JvmStatic\n    fun analyze(model: Model?, intent: SmartSliceIntent): SmartSliceResult",
            "fun analyze(g: GeometryAnalysis, intent: SmartSliceIntent, cfg: ConfigObject): SmartSliceResult",
            "fun apply(result: SmartSliceResult)",
            "if (intent.purpose != SmartSliceIntent.Purpose.VASE)",
            "ProfileSettingsRepository.setPendingValues(ConfigObject.PROFILE_LIST_PRINT, result.changes)",
        ),
        "utils/ThreadLocalDoubleArray": (
            "open class ThreadLocalDoubleArray(private val size: Int) : ThreadLocal<DoubleArray?>()",
            "override fun initialValue(): DoubleArray? = DoubleArray(size)",
        ),
        "utils/FillBedPlanner": (
            "class FillBedPlanner private constructor()",
            "@JvmStatic",
            "fun copyAttemptsForLimit(currentObjectCount: Int, maxObjectCount: Int): Int",
        ),
        "utils/UploadFileValidator": (
            "class UploadFileValidator private constructor()",
            "@JvmStatic",
            "fun isReadyForUpload(file: File?): Boolean",
            "file != null && file.isFile && file.canRead() && file.length() > 0",
        ),
        "utils/RandomUtils": (
            "open class RandomUtils",
            "@JvmField",
            "val RANDOM = Random()",
            "fun randomf(min: Float, max: Float): Float",
            "fun randoml(min: Long, max: Long): Long",
        ),
        "slic3r/Slic3rRuntimeError": (
            "open class Slic3rRuntimeError : Exception",
            "constructor() : super()",
            "constructor(message: String?) : super(message)",
            "constructor(message: String?, cause: Throwable?) : super(message, cause)",
            "constructor(cause: Throwable?) : super(cause)",
        ),
        "utils/FilamentTypes": (
            "object FilamentTypes",
            "@JvmField",
            "val ALL = arrayOf(",
            '"PLA", "PLA-CF", "PETG"',
            '"PEEK", "PEEK-CF", "PEEK-GF"',
        ),
        "utils/FilamentPaletteSyncPolicy": (
            "object FilamentPaletteSyncPolicy",
            "@JvmStatic",
            "fun updateExisting(",
            "printerTools: List<FilamentSlot?>",
        ),
        "utils/GCodeToolMapper": (
            "object GCodeToolMapper",
            "@JvmStatic",
            "fun remapToolLine(line: String, outputTools: IntArray): String",
            "fun parseToolCommand(line: String): Int",
            "fun isFilamentMapLine(line: String): Boolean",
        ),
        "utils/GCodeIntegrityValidator": (
            "object GCodeIntegrityValidator",
            "enum class Failure",
            "class Result private constructor(",
            "@JvmField val failure: Failure",
            "fun isValid(): Boolean",
            "fun validate(file: File?): Result",
        ),
        "utils/PrinterWebOriginPolicy": (
            "object PrinterWebOriginPolicy",
            "@JvmStatic",
            "fun dashboardUrl(configuredHost: String?): String?",
            "fun allows(configuredOrigin: String?, targetUrl: String?): Boolean",
        ),
        "utils/LithophaneInput": (
            "object LithophaneInput",
            "@JvmStatic",
            "fun parseClamped(text: String?, fallback: Float, min: Float, max: Float): Float",
            "text.trim { it <= ' ' }.replace(',', '.')",
        ),
        "utils/LithophaneToneMapper": (
            "object LithophaneToneMapper",
            "@JvmStatic",
            "fun gray(",
            "fun thickness(gray: Float, base: Float, maximum: Float, mode: Int): Float",
            "java.lang.Math.max(0.4f, base)",
        ),
        "utils/FastenerStandards": (
            "object FastenerStandards",
            "@JvmStatic",
            "fun count(): Int = DIAMETERS.size",
            "fun coarsePitch(i: Int): Float",
            "fun threadClearance(fit: Int): Float",
            "fun clearanceBore(i: Int): Float",
        ),
        "utils/CssColors": (
            "object CssColors",
            "@JvmField",
            "val ALL: MutableMap<String, Int> = LinkedHashMap()",
            "val COMMON = arrayOf(",
            'put("rebeccapurple", 0x663399)',
        ),
        "utils/Vec3d": (
            "class Vec3d",
            "@JvmField var x: Double = 0.0",
            "constructor(x: Double, y: Double, z: Double)",
            "constructor(from: Vec3d)",
            "fun center(with: Vec3d): Vec3d",
            "fun distance(with: Vec3d): Double",
            "fun normalize(): Vec3d",
            "fun divide(vec: Vec3d): Vec3d",
            "fun crossProduct(with: Vec3d): Vec3d",
            "fun asArray(): FloatArray",
            "fun asDoubleArray(): DoubleArray",
        ),
    }
    for relative_name, contracts in utility_contracts.items():
        java_class = JAVA / f"{relative_name}.java"
        kotlin_class = JAVA / f"{relative_name}.kt"
        if java_class.exists():
            raise AssertionError(f"{relative_name} still has a Java implementation")
        if not kotlin_class.exists():
            raise AssertionError(f"{relative_name} Kotlin implementation is missing")
        source = kotlin_class.read_text(encoding="utf-8")
        missing = [contract for contract in contracts if contract not in source]
        if missing:
            raise AssertionError(f"{relative_name} lost its Java-callable contract: " + ", ".join(missing))

    boot_task_contracts = {
        "boot/BootTask": (
            "open class BootTask",
            "@JvmField val dependencies: List<Class<*>>",
            "@JvmField val run: Runnable",
            "@JvmField var workerThread: Boolean = false",
            "@JvmField var priority: Int = 0",
            "@JvmField var nonCritical: Boolean = false",
            "@JvmField var index: Int = 0",
            "constructor(run: Runnable)",
            "constructor(dependencies: List<Class<*>>, run: Runnable)",
            "open fun onWorker(): BootTask = onWorker(-20)",
        ),
        "boot/CloudCachedInitTask": (
            "open class CloudCachedInitTask : BootTask(",
            "Collections.singletonList(PrefsTask::class.java)",
            "CloudController.initCached()",
            "onWorker()",
        ),
        "boot/CloudInitTask": (
            "open class CloudInitTask : BootTask(",
            "PrefsTask::class.java",
            "TrueTimeTask::class.java",
            "LoadSlic3rConfigTask::class.java",
            "CloudCachedInitTask::class.java",
            "CloudController.init()",
            "nonCritical = true",
        ),
        "boot/EventBusTask": (
            "open class EventBusTask : BootTask(Runnable {",
            'EventBus.registerImpl("ru.ytkab0bp.slicebeam")',
            "onWorker()",
        ),
        "boot/BeamServerDataTask": (
            "open class BeamServerDataTask : BootTask(Runnable {",
            "BeamServerData(JSONObject(Prefs.getBeamServerData()))",
            "catch (_: JSONException)",
            "BeamServerData(JSONObject())",
            "onWorker()",
        ),
        "boot/LoadSlic3rConfigTask": (
            "open class LoadSlic3rConfigTask : BootTask(Runnable {",
            "SliceBeam.getCurrentConfigFile().delete()",
            "SliceBeam.CONFIG = Slic3rConfigWrapper(configFile)",
            "ensureKobra3V2Available()",
            "Could not install bundled Kobra 3 V2 profile",
            "onWorker()",
        ),
        "boot/AppBoot": (
            "open class AppBoot",
            "@JvmStatic",
            "fun run(tasks: List<BootTask>)",
            "CountDownLatch(criticalTaskCount)",
            "startupLatch.await(10, TimeUnit.MILLISECONDS)",
            "task.index = index",
            "tryRunTask(task, fromMain, isContinue = true)",
            "executor!!.submit(runnable)",
            "currentCompleted.size() == currentTasks.size",
        ),
        "boot/TrueTimeTask": (
            "open class TrueTimeTask : BootTask(Runnable {",
            "CountDownLatch(1)",
            "TrueTimeImpl(",
            "Dispatchers.IO",
            "override fun initializeSuccess(values: LongArray)",
            "initializationLatch.await(300, TimeUnit.MILLISECONDS)",
            "nonCritical = true",
            "onWorker()",
        ),
        "boot/PrefsTask": ("class PrefsTask : BootTask(Runnable { Prefs.init(SliceBeam.requireInstance()) })",),
        "boot/PrintConfigWarmupTask": (
            "class PrintConfigWarmupTask : BootTask(Runnable { PrintConfigDef.getInstance() })",
            "onWorker()",
        ),
        "boot/VibrationUtilsTask": (
            "class VibrationUtilsTask : BootTask(Runnable { VibrationUtils.init(SliceBeam.requireInstance()) })",
            "onWorker()",
        ),
        "boot/ClearModelCacheTask": (
            "class ClearModelCacheTask : BootTask(Runnable {",
            "cache.listFiles()?.forEach { it.delete() }",
            "nonCritical = true",
            "onWorker()",
        ),
        "boot/CheckUpdateJsonTask": (
            "class CheckUpdateJsonTask : BootTask(Runnable {",
            'SliceBeam.requireInstance().getAssets().open("update.json").close()',
            "SliceBeam.hasUpdateInfo = true",
            "SliceBeam.hasUpdateInfo = false",
            "onWorker()",
        ),
    }
    for relative_name, contracts in boot_task_contracts.items():
        java_class = JAVA / f"{relative_name}.java"
        kotlin_class = JAVA / f"{relative_name}.kt"
        if java_class.exists():
            raise AssertionError(f"{relative_name} still has a Java implementation")
        if not kotlin_class.exists():
            raise AssertionError(f"{relative_name} Kotlin implementation is missing")
        source = kotlin_class.read_text(encoding="utf-8")
        missing = [contract for contract in contracts if contract not in source]
        if missing:
            raise AssertionError(f"{relative_name} lost its boot behavior: " + ", ".join(missing))

    render_contracts = {
        "render/Camera": (
            "class Camera",
            "@JvmField var position",
            "@JvmField var origin",
            "@JvmField var up",
            "fun getViewModelMatrix(): DoubleArray",
            "fun calcScreenMovement",
            "fun rotateAround",
            "MathUtils.clamp(",
        ),
        "render/CoordAxes": (
            "class CoordAxes",
            "@JvmField var origin",
            "fun setStemLength",
            "fun render(",
            "GLShadersManager.SHADER_GOURAUD_LIGHT",
            "ThemesRepo.getColor",
            "fun release()",
        ),
        "slic3r/GLModel": (
            "open class GLModel internal constructor(",
            "@JvmField var pointer: Long",
            "@JvmField var hoverProgress: Float",
            "@JvmField var isHovering: Boolean",
            "open fun initFromPaint",
            "Native.glmodel_release(pointer)",
            "inner class MeshRaycaster",
            "open class HitResult(",
        ),
        "slic3r/Bed3D": (
            "open class Bed3D",
            "Native.bed_create(data)",
            "open fun configure(file: File)",
            "open fun getVolumeMin(): Vec3d?",
            "open fun getVolumeMax(): Vec3d?",
            "open fun render(",
            "Native.bed_release(pointer)",
            "protected open fun finalize()",
        ),
        "slic3r/Model": (
            "open class Model private constructor(@JvmField var pointer: Long)",
            "@JvmField\n    val key: String",
            "open val objectsCount: Int",
            "open val geometryRevision: Long",
            "open val requiredFilamentCount: Int",
            "open fun cut(", "Native.model_cut(", "Native.model_slice(",
            "restoreAutoBrimForNative(configPath)", "Native.model_release(pointer)",
            "protected open fun finalize()", "@JvmStatic\n        fun merge(vararg models: Model)",
        ),
        "slic3r/GLShaderProgram": (
            "open class GLShaderProgram(name: String)",
            "@JvmField\n    var pointer: Long = 0",
            "Native.shader_init_from_texts(name, fragment, vertex)",
            "setUniformMatrix3fv(name: String, value: DoubleArray)",
            "setUniformMatrix4fv(name: String, value: FloatArray)",
            "Native.shader_release(pointer)",
        ),
        "slic3r/GLShadersManager": (
            "open class GLShadersManager",
            "const val SHADER_BACKGROUND = \"background\"",
            "annotation class ShaderType",
            "shaders.getOrPut(key) { GLShaderProgram(key) }",
            "@JvmStatic\n        @Keep\n        private fun getCurrentShaderPointer(): Long",
            "for (manager in managers)",
            "if (program != null) return program.pointer",
        ),
    }
    for relative_name, contracts in render_contracts.items():
        java_class = JAVA / f"{relative_name}.java"
        kotlin_class = JAVA / f"{relative_name}.kt"
        if java_class.exists():
            raise AssertionError(f"{relative_name} still has a Java implementation")
        if not kotlin_class.exists():
            raise AssertionError(f"{relative_name} Kotlin implementation is missing")
        source = kotlin_class.read_text(encoding="utf-8")
        missing = [contract for contract in contracts if contract not in source]
        if missing:
            raise AssertionError(f"{relative_name} lost a camera/rendering behavior: " + ", ".join(missing))

    remaining_migration_contracts = {
        "MainActivity": (
            "class MainActivity:AppCompatActivity()",
            "val navigationDelegate:NavigationDelegate",
            "get() = delegate ?: throw IllegalStateException(\"Navigation is not initialized\")",
            "override fun onCreate(savedInstanceState:Bundle?)",
            "override fun onConfigurationChanged(@NonNull newConfig:Configuration)",
            "ActivityResultContracts.StartActivityForResult(),",
            "private fun dispatchActivityResult(requestCode:Int, resultCode:Int, data:Intent?)",
        ),
        "SetupActivity": (
            "class SetupActivity:AppCompatActivity()",
            "override fun onCreate(@Nullable savedInstanceState:Bundle?)",
            "ActivityResultLauncher<Intent>",
            "SetupProfilePickerResult.shouldImport(result.resultCode, Activity.RESULT_OK, uri != null)",
        ),
        "components/ImageToStlConverter": (
            "object ImageToStlConverter",
            "@JvmStatic\nfun pickImage(activity:Activity)",
            "@JvmStatic\nfun pickLithophane(activity:Activity)",
            "fun saveGeneratedFile(activity:Activity, output:File, extension:String, bed:BedFragment)",
        ),
        "fragment/BedFragment": (
            "class BedFragment:Fragment()",
            "TransformScaleMenu(this)",
            "override fun onCreateView(",
            "fun loadModel(",
            "interface ModelLoadCallback",
        ),
        "render/GLRenderer": (
            "class GLRenderer(glView:GLView):GLSurfaceView.Renderer",
            "override fun onSurfaceCreated(",
            "override fun onSurfaceChanged(",
            "override fun onDrawFrame(",
            "fun setSelectionScale(",
            "fun setCutPlane(",
        ),
        "utils/LithophaneStlWriter": (
            "object LithophaneStlWriter",
            "@JvmStatic\nfun writePanel(",
            "@JvmStatic\nfun writeLitoGenCubeKit(",
            "@JvmStatic\nfun writeCurvedPanel(",
        ),
        "view/GLView": (
            "class GLView(context:Context):GLSurfaceView(context), IThemeView",
            "GLRenderer(this)",
            "setPreserveEGLContextOnPause(true)",
            "tapGestureGate.finish(",
            "cancelled = action == MotionEvent.ACTION_CANCEL",
            "renderer.focusForDrag(longClickTargetObject)",
            "override fun surfaceDestroyed(holder:SurfaceHolder)",
            "override fun onApplyTheme()",
        ),
        "render/GLRenderer": (
            "@JvmOverloads fun setSelectedObject(selectedObject:Int, showActionBar:Boolean = true):Boolean",
            "SelectedObjectChangedEvent(showActionBar = false)",
            "setSelectedObject(i, showActionBar = false)",
        ),
        "fragment/BedFragment": (
            "if (event.showActionBar) {",
            "selectedObjectBarSelectionRevision++",
            "selectedObjectBar?.setVisibility(View.GONE)",
        ),
    }
    for relative_name, contracts in remaining_migration_contracts.items():
        java_class = JAVA / f"{relative_name}.java"
        kotlin_class = JAVA / f"{relative_name}.kt"
        if java_class.exists():
            raise AssertionError(f"{relative_name} still has a Java implementation")
        if not kotlin_class.exists():
            raise AssertionError(f"{relative_name} Kotlin implementation is missing")
        source = kotlin_class.read_text(encoding="utf-8")
        missing = [contract for contract in contracts if contract not in source]
        if missing:
            raise AssertionError(f"{relative_name} lost a required Java/lifecycle/render contract: " + ", ".join(missing))

    setup_source = (JAVA / "SetupActivity.kt").read_text(encoding="utf-8")
    setup_screen_source = (JAVA / "ui/setup/SetupOnboardingScreens.kt").read_text(encoding="utf-8")
    setup_backdrop_source = (JAVA / "ui/setup/SetupActivityBackdrop.kt").read_text(encoding="utf-8")
    setup_root_source = (JAVA / "ui/setup/SetupActivityRoot.kt").read_text(encoding="utf-8")
    if "SetupActivityBackdrop(" not in setup_root_source or "HorizontalPager(" not in setup_root_source or "rememberPagerState(" not in setup_root_source:
        raise AssertionError("Setup flow must use the Compose pager under the Compose artwork and title.")
    if "ViewPager2" in setup_source or "SimpleRecyclerAdapter" in setup_source or "fakeDragBy" in setup_source:
        raise AssertionError("Setup flow still depends on the legacy pager or recycler adapter.")
    if "pages = setupPages" not in setup_source or "pageCount = visibleSetupPageCount" not in setup_source:
        raise AssertionError("Setup Compose pager must receive its current page data and gated page count.")
    if "internal interface SetupComposePage" not in setup_root_source or "LaunchedEffect(item) { item.onPageCreated() }" not in setup_root_source or "item.Content()" not in setup_root_source:
        raise AssertionError("Setup pages must render directly in the Compose pager and retain first-bind refresh behavior.")
    if "AndroidView(" in setup_root_source or "composeSetupPage(" in setup_source:
        raise AssertionError("Setup pages must not be nested ComposeViews hosted through AndroidView.")
    if "onSaveInstanceState(outState: Bundle)" not in setup_source or "STATE_CURRENT_SETUP_PAGE" not in setup_source:
        raise AssertionError("Setup pager must preserve its active page across Activity recreation.")
    if ".onSizeChanged { onRootHeightChanged(it.height) }" not in setup_root_source or "onRootHeightChanged = { height ->" not in setup_source:
        raise AssertionError("Setup activity title positioning must continue to follow the measured root height.")
    if "backgroundAlpha = startupBackgroundAlpha" not in setup_source:
        raise AssertionError("Setup activity must preserve the existing animated artwork visibility state.")
    if "ImageView" in setup_source or "private lateinit var title: TextView" in setup_source:
        raise AssertionError("Setup activity artwork/title still has its legacy native view host.")
    if "internal fun SetupActivityBackdrop(" not in setup_backdrop_source or "Text(" not in setup_backdrop_source:
        raise AssertionError("Setup activity artwork and animated title must both use their Compose renderer.")
    if "ContentScale.Crop" not in setup_backdrop_source or "R.drawable.startup_background" not in setup_backdrop_source:
        raise AssertionError("Setup activity Compose artwork must preserve the full-screen cropped startup image.")
    if any(contract in setup_source for contract in (
        "androidx.recyclerview.widget.RecyclerView",
        "RecyclerView.LayoutParams",
        "SimpleRecyclerItem<ProfilesRepo",
        "class RepoHolderView",
        "SetupRepositoryRow(",
    )):
        raise AssertionError("Setup profile sources must remain data-only; the unused RecyclerView row must not return.")
    for contract in (
        "private class ProfilesRepo(",
        "name = \"OrcaSlicer\"",
        "checked = true",
        "localAssets = true",
        "if (!repo.checked) continue",
        "if (repo.localAssets)",
        "client.get(repo.indexUrl",
    ):
        if contract not in setup_source:
            raise AssertionError("Setup profile repository data flow is missing: " + contract)
    for contract in ("titleScale = titleScale", "titleTranslationYPx = titleTranslationYPx", "titleTopInsetPx = titleTopInsetPx", "titleHeightPx != height"):
        if contract not in setup_source:
            raise AssertionError("Setup activity Compose title lost its animation or safe-area state: " + contract)
    setup_page_sections = (
        ("AboutItem", "private inner class AboutItem", "private inner class IntroItem"),
        ("IntroItem", "private inner class IntroItem", "private inner class PresetChoiceItem"),
        ("PresetChoiceItem", "private inner class PresetChoiceItem", "private inner class ProfilesItem"),
        ("ProfilesItem", "private inner class ProfilesItem", "private inner class FilamentsItem"),
        ("FilamentsItem", "private inner class FilamentsItem", "private inner class BoostyItem"),
        ("BoostyItem", "private inner class BoostyItem", "private inner class FinishItem"),
        ("FinishItem", "private inner class FinishItem", "enum class AccentColors"),
    )
    for name, start_marker, end_marker in setup_page_sections:
        start = setup_source.index(start_marker)
        end = setup_source.index(end_marker, start + len(start_marker))
        section = setup_source[start:end]
        if "SetupComposePage" not in section or "@Composable" not in section or "override fun Content()" not in section or "override fun onPageCreated()" not in section:
            raise AssertionError(f"SetupActivity {name} page must render directly in Compose and preserve page binding.")
        legacy_views = [token for token in ("LinearLayout(", "TextView(", "BeamSwitch(", "RecyclerView.LayoutParams", "FadeRecyclerView", "ProfileItem(", "SimpleRecyclerAdapter") if token in section]
        if legacy_views:
            raise AssertionError(f"SetupActivity {name} still builds native layout views: " + ", ".join(legacy_views))
    setup_screen_contracts = (
        "fun SetupAboutScreen(versionName: String, onClose: () -> Unit)",
        "fun SetupIntroScreen(onNext: () -> Unit)",
        "fun SetupPresetSourceScreen(",
        "R.string.SetupPresetSource",
        "R.string.SetupBuiltInPresets",
        "R.string.SetupLoadProfileSubtitle",
        "R.string.SetupBrowse",
        "R.string.IntroNext",
        "role = Role.RadioButton",
        "role = Role.Button",
    )
    missing_setup_screen_contracts = [contract for contract in setup_screen_contracts if contract not in setup_screen_source]
    if missing_setup_screen_contracts:
        raise AssertionError("Setup onboarding Compose screens lost expected content or accessibility roles: " + ", ".join(missing_setup_screen_contracts))
    if "fun SetupFinishScreen(onFinish: () -> Unit)" not in setup_screen_source:
        raise AssertionError("Setup completion page is not implemented in Compose.")
    setup_boosty_screen = (JAVA / "ui/setup/SetupBoostyScreen.kt").read_text(encoding="utf-8")
    for contract in (
        "fun SetupBoostyScreen(",
        "LazyColumn(",
        "onOpenSupport: () -> Unit",
        "onContinue: () -> Unit",
        "R.string.IntroBoostySupport",
        "R.string.IntroBoostyTitle",
    ):
        if contract not in setup_boosty_screen:
            raise AssertionError("Setup supporter page lost its Compose behavior or scrollable list: " + contract)
    if "SetupBoostyScreen(" not in setup_source or "refreshSupporters()" not in setup_source:
        raise AssertionError("Setup supporter page must use Compose and refresh supporter data.")
    setup_cloud_screen = (JAVA / "ui/setup/SetupCloudProfileScreen.kt").read_text(encoding="utf-8")
    for contract in (
        "fun SetupCloudProfileScreen(",
        "SetupCloudSubscriptionPolicy.isSubscribed(",
        "SetupCloudSubscriptionPolicy.canOpenCheckout(",
        "SettingsCloudManageTermsOfService",
        "SettingsCloudManageButtonManage",
        "SettingsCloudManageButtonLogIn",
        "SettingsCloudManageLevelRedirectAlreadySubscribed",
        "SettingsCloudManageFeatureAIGeneratorDescription",
        "role = Role.Button",
    ):
        if contract not in setup_cloud_screen:
            raise AssertionError("Setup Cloud account Compose page lost a behavior contract: " + contract)
    setup_cloud_activity = setup_source[
        setup_source.index("private inner class CloudProfileItem"):
        setup_source.index("private inner class AboutItem")
    ]
    if "SetupComposePage" not in setup_cloud_activity or "override fun Content()" not in setup_cloud_activity or "onPageCreated()" not in setup_cloud_activity or "refreshState()" not in setup_cloud_activity:
        raise AssertionError("Setup Cloud account page must render directly in Compose and refresh from CloudController state.")
    if any(token in setup_cloud_activity for token in ("FadeRecyclerView", "CloudSubscriptionLevel", "LinearLayout(", "TextView(")):
        raise AssertionError("Setup Cloud account page still builds legacy Android view rows.")
    profile_selection_screen = (JAVA / "ui/setup/SetupProfileSelectionScreens.kt").read_text(encoding="utf-8")
    profile_selection_policy = (JAVA / "ui/setup/SetupProfileSelectionPolicy.kt").read_text(encoding="utf-8")
    for contract in (
        "fun SetupPrinterSelectionScreen(",
        "fun SetupFilamentSelectionScreen(",
        "SetupCustomProfileCard()",
        "SetupProfileOptionRow(",
        "rememberGlideBitmap(",
        "IntroFilamentsSelectAll",
        "IntroFilamentsDeselectAll",
        "IntroFilamentsNone",
        "toggleable(value = option.selected, role = Role.Switch",
    ):
        if contract not in profile_selection_screen:
            raise AssertionError(f"The Compose printer/filament setup screens lost required behavior: {contract}")
    for contract in (
        "fun toggle(selectedKeys: Set<String>, key: String, selected: Boolean)",
        "fun selectAll(keys: Collection<String>)",
        "fun canContinue(useCustomProfile: Boolean, selectedPrinterCount: Int)",
    ):
        if contract not in profile_selection_policy:
            raise AssertionError(f"The setup-selection policy is missing: {contract}")
    setup_selection_test = ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/ui/setup/SetupProfileSelectionPolicyTest.kt"
    if not setup_selection_test.exists():
        raise AssertionError("The setup printer/filament selection regression tests are missing")
    setup_selection_test_source = setup_selection_test.read_text(encoding="utf-8")
    for contract in ("togglingChangesOnlyTheRequestedProfile", "selectingAllRetainsStableUniqueKeys", "printerPageRequiresASelectionExceptForCustomProfiles"):
        if contract not in setup_selection_test_source:
            raise AssertionError(f"The setup selection regression test is missing: {contract}")
    if "SetupFinishScreen(onFinish = { finishSetup() })" not in setup_source or "private fun finishSetup()" not in setup_source:
        raise AssertionError("Setup completion no longer invokes the existing profile-save and launch flow.")

    setup_selection_source = profile_selection_screen
    if "AndroidView(" in setup_selection_source or "ImageView(" in setup_selection_source:
        raise AssertionError("Setup profile thumbnails must be rendered as Compose images, not embedded Android views.")
    glide_compose_source = (JAVA / "ui/components/GlideBitmap.kt").read_text(encoding="utf-8")
    for contract in (
        "DisposableEffect(context, model, sizePx, shape, cornerRadiusPx)",
        "CustomTarget<Bitmap>(sizePx, sizePx)",
        "requestManager.clear(target)",
        ".override(sizePx, sizePx)",
    ):
        if contract not in glide_compose_source:
            raise AssertionError(f"Compose image loading lost a bounded, lifecycle-aware request contract: {contract}")
    if "rememberGlideBitmap(avatarUrl, 26.dp, GlideBitmapShape.CIRCLE)" not in (
        JAVA / "ui/settings/ComposeCloudProfileHeader.kt"
    ).read_text(encoding="utf-8"):
        raise AssertionError("Cloud profile avatars are not using the shared Compose image loader.")

    app_tests = ROOT / "app/src/test/java"
    remaining_java = sorted(JAVA.rglob("*.java"))
    remaining_test_java = sorted(app_tests.rglob("*.java")) if app_tests.exists() else []
    if remaining_java:
        raise AssertionError("Java source files remain in the app module: " + ", ".join(str(path.relative_to(JAVA)) for path in remaining_java))
    if remaining_test_java:
        raise AssertionError(
            "Java unit-test sources remain in the app module: "
            + ", ".join(str(path.relative_to(app_tests)) for path in remaining_test_java)
        )
    android_view_bridges = [
        str(path.relative_to(JAVA))
        for path in JAVA.rglob("*.kt")
        if "AndroidView(" in path.read_text(encoding="utf-8")
    ]
    if android_view_bridges:
        raise AssertionError(
            "AndroidView interop bridges remain in app production sources: "
            + ", ".join(android_view_bridges)
        )

    kotlin_count = len(list(JAVA.rglob("*.kt")))
    kotlin_test_count = len(list(app_tests.rglob("*.kt"))) if app_tests.exists() else 0
    print(f"Kotlin migration audit passed: all {kotlin_count} app production sources and {kotlin_test_count} app unit tests are Kotlin, no AndroidView interop bridges remain, migrated Java contracts and lifecycle/render routes are present, early MainActivity lifecycle callbacks are null-safe, setup pages render directly inside the Compose pager, printer/filament selection pages use Compose, and every profile row uses typed direct Compose; shared theme, adaptive Compose navigation/header, native model wrapper, event payloads, utility/boot/render contracts, Maker/converter routes and printing flows are retained.")


if __name__ == "__main__":
    main()
