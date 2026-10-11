#!/usr/bin/env python3
"""Check that settings shown in mobile profile editors are native and persist in that profile."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/ru/ytkab0bp/slicebeam"
NATIVE = ROOT / "app/src/main/jni/libslic3r"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def string_set(pattern: str, source: str) -> set[str]:
    return set(re.findall(pattern, source))


def config_keys(source: str, name: str) -> set[str]:
    if name == "PRINT_CONFIG_KEYS":
        match = re.search(
            r"buildPrintConfigKeys\(\).*?arrayListOf\((.*?)\)\s*for \(key in OrcaProcessSettingsCatalog\.processKeys",
            source,
            re.S,
        )
    else:
        match = re.search(
            # The Kotlin port keeps the closing parenthesis on the last key's
            # line; stop at the first close so later profile lists are not
            # accidentally included in this profile's whitelist.
            r"\b" + re.escape(name) + r"\s*:\s*List<String>\s*=\s*Arrays\.asList\((.*?)\)",
            source,
            re.S,
        )
    if not match:
        raise AssertionError(f"Profile key list {name} was not found")
    return string_set(r'"([^"]+)"', match.group(1))


def uncommented(source: str) -> str:
    return "\n".join(line for line in source.splitlines() if not line.lstrip().startswith("//"))


def catalog_option_keys(source: str) -> list[str]:
    keys: list[str] = []
    for match in re.finditer(r'\bsection\("[^"]+"\s*(?:,\s*([^)]*))?\)', source):
        keys.extend(re.findall(r'"([^"]+)"', match.group(1) or ""))
    return keys


def main() -> None:
    wrapper = read(JAVA / "slic3r/Slic3rConfigWrapper.kt")
    config_object = read(JAVA / "config/ConfigObject.kt")
    schema_migrator = read(JAVA / "config/ProfileConfigSchemaMigrator.kt")
    load_warning_types = read(JAVA / "config/ProfileConfigLoadWarning.kt")
    import_validator = read(JAVA / "config/ProfileConfigImportValidator.kt")
    config_load_task = read(JAVA / "boot/LoadSlic3rConfigTask.kt")
    profile_import_ui = read(JAVA / "MainActivity.kt")
    profile_import_test = read(ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/config/ProfileConfigImportValidatorTest.kt")
    workspace_profile_sheet = read(JAVA / "components/WorkspaceProfileSheet.kt")
    main_activity = read(JAVA / "MainActivity.kt")
    slicebeam = read(JAVA / "SliceBeam.kt")
    schema_migration_test = read(ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/config/ProfileConfigSchemaMigratorTest.kt")
    for migration_contract in (
        "const val CURRENT_SCHEMA_VERSION = 1", "MARKER_PREFIX", "ConfigObject.migrateKey(key)",
        "createBackup(file, original, existingVersion)", "replaceWithRollback(file, upgraded)",
        "recoverInterruptedReplacement(file)",
    ):
        if migration_contract not in schema_migrator:
            raise AssertionError(f"Profile schema migration is missing: {migration_contract}")
    if "ProfileConfigSchemaMigrator.migrate(configFile)" not in config_load_task:
        raise AssertionError("Stored profiles are not migrated before the startup config is read")
    if "else -> null" not in wrapper or "ignoreUnknownSection" not in wrapper:
        raise AssertionError("Unknown profile sections can leak into a recognized profile during import")
    for warning_contract in (
        "data class MissingParent", "data class UnknownOption", "data class InvalidValue",
        "data object ValidationUnavailable", "INVALID_NUMBER", "OUT_OF_RANGE", "UNKNOWN_ENUM",
        "INVALID_BOOLEAN", "INVALID_POINT",
    ):
        if warning_contract not in load_warning_types:
            raise AssertionError(f"Profile import diagnostics are missing: {warning_contract}")
    for validation_contract in (
        "ProfileNumericValueValidator.validate", "ProfileVectorValueValidator.validate",
        "ProfilePointValueValidator.validate", "ProfileEnumOptionResolver.selectedIndex",
        "ConfigObject.migrateKey(rawKey)", "isOpenSelection(definition)",
    ):
        if validation_contract not in import_validator:
            raise AssertionError(f"Native profile value validation is missing: {validation_contract}")
    for import_contract in (
        "ProfileConfigLoadWarning.MissingParent", "ProfileConfigLoadWarning.UnknownOption",
        "ProfileConfigImportValidator.validate", "PrintConfigDef.getInstance().options",
        "ProfileConfigLoadWarning.InvalidValue", "ProfileConfigLoadWarning.ValidationUnavailable",
        "MenuFileImportProfilesWarningInvalidValue", "MenuFileImportProfilesWarningValidationUnavailable",
        "MenuFileImportProfilesWarningsTitle", "MenuFileImportProfilesConflictTitle",
        "MenuFileImportProfilesExisting", "applyImport(true)", "applyImport(false)",
        "MenuFileImportProfilesSkipExisting",
    ):
        if import_contract not in profile_import_ui:
            raise AssertionError(f"Profile import safety UI is missing: {import_contract}")
    for warning_resource in (
        "MenuFileImportProfilesWarningInvalidNumber", "MenuFileImportProfilesWarningOutOfRange",
        "MenuFileImportProfilesWarningUnknownEnum", "MenuFileImportProfilesWarningInvalidBoolean",
        "MenuFileImportProfilesWarningInvalidPoint", "MenuFileImportProfilesWarningsMore",
    ):
        for locale in ("values", "values-de", "values-ru"):
            strings = read(ROOT / f"app/src/main/res/{locale}/strings.xml")
            if f'name="{warning_resource}"' not in strings:
                raise AssertionError(f"Profile validation warning {warning_resource} is missing from {locale}")
    for validation_test in (
        "reportsInvalidAndOutOfRangeScalarValues", "acceptsNativeEnumsAndLegacyIndicesButReportsUnknownChoices",
        "validatesNumericVectorsAndAllowsBlankOptionalVectors", "validatesBooleanVectorsAndCoordinates",
        "permitsCustomOpenEnumValuesAndDoesNotModifyImportedProfiles",
    ):
        if validation_test not in profile_import_test:
            raise AssertionError(f"Profile value validation lacks regression coverage: {validation_test}")
    if "loadWarnings" not in config_load_task or "warning.toString()" not in config_load_task:
        raise AssertionError("Stored profile migration warnings are not retained in startup diagnostics")
    for migration_test in (
        "migratesLegacyProfileKeysAndKeepsUnknownConfigTextWithRecoverableBackup",
        "currentSchemaIsIdempotentAndDoesNotCreateAnotherBackup", "futureSchemaIsNeverDowngraded",
        "restoresInterruptedReplacementBeforeStartingMigration",
        "migratedProfilesLoadAndUnknownSectionsCannotLeakIntoPreviousProfile",
    ):
        if migration_test not in schema_migration_test:
            raise AssertionError(f"Profile schema migration is missing regression coverage: {migration_test}")
    migrations_match = re.search(
        r"private fun buildKeyMigration\(\).*?HashMap<String, String>\(\)\.apply\s*\{(.*?)\n\s*\}",
        config_object,
        re.S,
    )
    if not migrations_match:
        raise AssertionError("ConfigObject key migration table was not found")
    migrations = dict(re.findall(r'put\("([^"]+)",\s*"([^"]+)"\)', migrations_match.group(1)))
    if len(migrations) < 137:
        raise AssertionError(f"ConfigObject key migration table was truncated: found {len(migrations)}, expected at least 137 entries")
    reverse = {target: old for old, target in migrations.items()}
    if len(reverse) != len(migrations):
        raise AssertionError("Two legacy setting names map to the same Orca setting")

    filament_catalog = uncommented(read(JAVA / "ui/settings/OrcaFilamentSettingsCatalog.kt"))
    filament_catalog_option_keys = catalog_option_keys(filament_catalog)
    process_catalog = uncommented(read(JAVA / "ui/settings/OrcaProcessSettingsCatalog.kt"))
    process_catalog_option_keys = catalog_option_keys(process_catalog)
    printer_catalog = uncommented(read(JAVA / "ui/settings/OrcaPrinterSettingsCatalog.kt"))
    printer_catalog_option_keys = catalog_option_keys(printer_catalog)
    if "OrcaProcessSettingsCatalog.processKeys()" not in wrapper:
        raise AssertionError("Process profile keys are not connected to the shared Kotlin catalog")
    if "ConfigObject.printConfigKeyAliases()" not in wrapper:
        raise AssertionError("Legacy Orca process aliases are not added to the profile import whitelist")
    alias_match = re.search(
        r"PRINT_CONFIG_KEY_ALIASES\s*:\s*List<String>\s*=\s*Collections\.unmodifiableList\(\s*listOf\((.*?)\)\s*\)",
        config_object,
        re.S,
    )
    if not alias_match:
        raise AssertionError("ConfigObject's legacy process alias list was not found")
    process_aliases = string_set(r'"([^"]+)"', alias_match.group(1))
    orca_alias_match = re.search(
        r"ORCA_KEY_ALIASES\s*:\s*Map<String, String>\s*=\s*Collections\.unmodifiableMap\(\s*hashMapOf\((.*?)\)\s*\)",
        config_object,
        re.S,
    )
    if not orca_alias_match:
        raise AssertionError("ConfigObject's previous-Orca key alias table was not found")
    orca_aliases = dict(re.findall(r'"([^"]+)"\s+to\s+"([^"]+)"', orca_alias_match.group(1)))
    groups = {
        "FilamentConfigFragment.kt": config_keys(wrapper, "FILAMENT_CONFIG_KEYS"),
        "PrinterConfigFragment.kt": config_keys(wrapper, "PRINTER_CONFIG_KEYS"),
        "PrintConfigFragment.kt": config_keys(wrapper, "PRINT_CONFIG_KEYS") | set(process_catalog_option_keys) | process_aliases,
    }
    absent_from_route: list[str] = []
    visible_settings = 0
    for filename, profile_keys in groups.items():
        fragment = uncommented(read(JAVA / "fragment" / filename))
        if filename == "FilamentConfigFragment.kt":
            keys = set(filament_catalog_option_keys)
        elif filename == "PrintConfigFragment.kt":
            keys = set(process_catalog_option_keys)
        elif filename == "PrinterConfigFragment.kt":
            keys = set(printer_catalog_option_keys)
        else:
            keys = string_set(r'def\.options\.get\(\s*"([^"]+)"', fragment)
        visible_settings += len(keys)
        for key in sorted(keys):
            legacy = reverse.get(key, key)
            if key not in profile_keys and legacy not in profile_keys:
                absent_from_route.append(f"{filename}: {key}")

    native_cpp = uncommented(read(NATIVE / "PrintConfig.cpp"))
    native_hpp = uncommented(read(NATIVE / "PrintConfig.hpp"))
    preset_cpp = read(NATIVE / "Preset.cpp")
    native_keys = string_set(r'\bthis->add\(\s*"([^"]+)"', native_cpp)
    native_keys.update(string_set(r"\(\(ConfigOption\w+,\s*([a-zA-Z_][\w]*)\)\)", native_hpp))
    ignored_match = re.search(r"static std::set<std::string> ignore\s*=\s*\{(.*?)\n\s*\};", native_cpp, re.S)
    if ignored_match:
        native_keys.difference_update(string_set(r'"([^"]+)"', ignored_match.group(1)))
    for list_name in ("s_Preset_print_options", "s_Preset_filament_options", "s_Preset_printer_options", "s_Preset_machine_limits_options"):
        match = re.search(r"static std::vector<std::string>\s+" + list_name + r"\s*\{(.*?)\n\s*\};", preset_cpp, re.S)
        if match:
            native_keys.update(string_set(r'"([^"]+)"', uncommented(match.group(1))))
    native_keys.update(f"machine_max_{kind}_{axis}" for kind in ("speed", "acceleration", "jerk") for axis in "xyze")

    errors = []
    legacy_feature_migrations = {
        "perimeter_extruder": "outer_wall_filament_id",
        "wall_filament": "outer_wall_filament_id",
        "wall_filament_id": "outer_wall_filament_id",
        "outer_wall_filament": "outer_wall_filament_id",
        "inner_wall_filament": "inner_wall_filament_id",
        "infill_extruder": "sparse_infill_filament_id",
        "sparse_infill_filament": "sparse_infill_filament_id",
        "solid_infill_extruder": "internal_solid_filament_id",
        "solid_infill_filament": "internal_solid_filament_id",
        "top_solid_infill_filament": "top_surface_filament_id",
        "bottom_solid_infill_filament": "bottom_surface_filament_id",
        "top_one_perimeter_type": "only_one_wall_top",
        "top_one_wall_type": "only_one_wall_top",
    }
    print_keys = groups["PrintConfigFragment.kt"]
    for legacy, expected_current in legacy_feature_migrations.items():
        intermediate = migrations.get(legacy, legacy)
        current = orca_aliases.get(intermediate, intermediate)
        if current != expected_current:
            errors.append(f"Legacy process key {legacy} resolves to {current}, expected {expected_current}")
        if legacy not in print_keys:
            errors.append(f"Legacy process key {legacy} is not recognized by the profile importer")
        if expected_current not in native_keys:
            errors.append(f"Legacy process key {legacy} resolves to missing native option {expected_current}")
    value_migration = uncommented(config_object)
    if 'if (value == "1") return "0"' not in value_migration:
        errors.append("Old feature-filament default values are not converted from 1 to Default (0)")
    if 'return if (value == "none") "0" else "1"' not in value_migration:
        errors.append("Old single-top-wall enum values are not converted to the current boolean")

    absent_from_native = []
    for filename in groups:
        fragment = uncommented(read(JAVA / "fragment" / filename))
        if filename == "FilamentConfigFragment.kt":
            keys = set(filament_catalog_option_keys)
        elif filename == "PrintConfigFragment.kt":
            keys = set(process_catalog_option_keys)
        elif filename == "PrinterConfigFragment.kt":
            keys = set(printer_catalog_option_keys)
        else:
            keys = string_set(r'def\.options\.get\(\s*"([^"]+)"', fragment)
        for key in sorted(keys):
            canonical = migrations.get(key, key)
            if key not in native_keys and canonical not in native_keys:
                absent_from_native.append(f"{filename}: {key}")

    if absent_from_route:
        errors.append("Visible settings not saved with their selected profile:\n  " + "\n  ".join(absent_from_route))
    if absent_from_native:
        errors.append("Visible settings missing from the bundled native Orca schema:\n  " + "\n  ".join(absent_from_native))

    filament_keys = config_keys(wrapper, "FILAMENT_CONFIG_KEYS")
    printer_keys = config_keys(wrapper, "PRINTER_CONFIG_KEYS")
    if "pressure_advance" not in filament_keys or "pressure_advance" in printer_keys:
        errors.append("Pressure Advance must belong exclusively to the filament profile")
    if "z_offset" not in printer_keys or "z_offset" in filament_keys:
        errors.append("Z offset must belong exclusively to the printer profile")
    if migrations.get("pressure_advance") == "z_offset":
        errors.append("Pressure Advance must never migrate to Z offset")

    # The process editor used to show these filament-scoped controls a second time.
    # Keep the full adaptive PA family in FilamentConfigFragment and expose only one
    # generic bed-temperature pair; plate-specific values are synchronized on save.
    process_builder = uncommented(read(JAVA / "fragment/OrcaPrintSettingsBuilder.kt"))
    process_presentation = process_builder + "\n" + process_catalog
    profile_repository = uncommented(read(JAVA / "config/ProfileSettingsRepository.kt"))
    filament_fragment = uncommented(read(JAVA / "fragment/FilamentConfigFragment.kt"))
    filament_option_keys = set(filament_catalog_option_keys)
    filament_builder = uncommented(read(JAVA / "fragment/OrcaFilamentSettingsBuilder.kt"))
    pressure_advance_keys = (
        "enable_pressure_advance", "pressure_advance", "adaptive_pressure_advance",
        "adaptive_pressure_advance_model", "adaptive_pressure_advance_overhangs",
        "adaptive_pressure_advance_bridges",
    )
    for key in pressure_advance_keys:
        if f'"{key}"' in process_presentation:
            errors.append(f"Filament-scoped setting {key} is duplicated in the process editor")
        if key not in filament_option_keys:
            errors.append(f"Filament-scoped setting {key} is not shown in the filament editor")
    if "addedKeys.add(key)" not in process_builder:
        errors.append("Process settings do not suppress duplicate Orca keys across sections")
    if "OrcaProcessSettingsCatalog.categories()" not in process_builder:
        errors.append("Process settings are not rendered from the central Kotlin catalog")
    if "hasUniqueProcessKeys()" not in process_catalog:
        errors.append("Central process catalog is missing its unique-key invariant")
    profile_fragment = uncommented(read(JAVA / "fragment/ProfileListFragment.kt"))
    if (JAVA / "fragment/ProfileListFragment.java").exists():
        errors.append("Shared profile screen host is still implemented in Java")
    for profile_host_contract in (
        "abstract class ProfileListFragment : Fragment()", "override fun onCreateView(ctx: Context): View",
        "return ComposeView(ctx).apply", "ProfileDropdownContent(", "openProfileMenu()",
        "selectProfileFromMenu", "ProfileSettingsComposeList(", "profileList.Content()",
        "ProfileSettingsFooter.create(", "ProfileConfigValueController.stageValue(",
    ):
        if profile_host_contract not in profile_fragment:
            errors.append(f"Kotlin profile screen host is missing a lifecycle or interaction route: {profile_host_contract}")
    adaptive_navigation_policy = uncommented(read(JAVA / "ui/navigation/AdaptiveNavigationPolicy.kt"))
    if "fun isPortraitWindow(windowWidthDp: Int, windowHeightDp: Int, isPortrait: Boolean)" not in adaptive_navigation_policy:
        errors.append("Adaptive UI policy does not account for the current app-window shape")
    for adaptive_screen in (
        uncommented(read(JAVA / "fragment/ProfileListFragment.kt")),
        uncommented(read(JAVA / "navigation/MobileNavigationDelegate.kt")),
    ):
        if "AdaptiveNavigationPolicy.isPortraitWindow(" not in adaptive_screen:
            errors.append("Adaptive screen layout still bypasses the app-window shape policy")
    if "resources.displayMetrics" in profile_fragment or "widthPixels" in profile_fragment:
        errors.append("Profile layout still derives its shape from physical display metrics")
    bed_fragment = uncommented(read(JAVA / "fragment/BedFragment.kt"))
    if "usesCompactPortraitLayout(" not in bed_fragment or "AdaptiveNavigationPolicy.isPortraitWindow(" not in bed_fragment:
        errors.append("3D workspace layout does not use the shared app-window size policy")
    if "widthPixels < heightPixels" in bed_fragment or "widthPixels / dm.density" in bed_fragment:
        errors.append("3D workspace layout still derives breakpoints from physical display metrics")
    profile_item_contract = uncommented(read(JAVA / "fragment/ProfileListItem.kt"))
    config_object = uncommented(read(JAVA / "config/ConfigObject.kt"))
    if "interface ProfileListItem" not in profile_item_contract or "val title: String?" not in profile_item_contract or "fun isSelected(): Boolean" not in profile_item_contract:
        errors.append("Shared profile row contract is not defined by the Kotlin model")
    if "open class ConfigObject : ProfileListItem" not in config_object:
        errors.append("ConfigObject does not implement the Kotlin profile row contract")
    if "exposesProfileTitleAndSelectionThroughTheKotlinContract" not in read(ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/config/ConfigObjectProfileListItemTest.kt"):
        errors.append("Java ConfigObject to Kotlin profile row interoperability test is missing")
    if "public interface ProfileListItem" in profile_fragment:
        errors.append("Profile row contract is still nested in the Java profile host")
    for fragment_name in (
        "FilamentConfigFragment.kt", "PrintConfigFragment.kt", "PrinterConfigFragment.kt",
        "SettingsFragment.kt",
    ):
        source = uncommented(read(JAVA / "fragment" / fragment_name))
        if "ProfileListFragment.ProfileListItem" in source:
            errors.append(f"{fragment_name} still depends on the nested Java profile row contract")
    profile_value_controller = uncommented(read(JAVA / "fragment/ProfileConfigValueController.kt"))
    profile_dropdown_controller = profile_fragment
    profile_dropdown_view = uncommented(read(JAVA / "view/ProfileDropdownMenu.kt"))
    for repository_call in ("valueFor(", "setPendingValue(", "commitDraft("):
        if repository_call not in profile_repository:
            errors.append(f"ProfileSettingsRepository does not implement {repository_call}")
    if "ProfileConfigValueController.valueFor(" not in profile_fragment:
        errors.append("Profile editor option reads do not use the Kotlin profile value controller")
    if "ProfileConfigValueController.stageValue(" not in profile_fragment:
        errors.append("Profile editor updates bypass the Kotlin profile value controller")
    for value_contract in (
        "ProfileSettingsRepository.valueFor(profileType, profile, key, !ignorePending)",
        'if (guiType == ConfigOptionDef.GUIType.COLOR) ";" else ","',
        "parts[index] = editedValue", "ProfileSettingsRepository.setPendingValue(profileType, key, baseValue, value)",
        'if (key == "host_type") "octoprint" else defaultValue',
    ):
        if value_contract not in profile_value_controller:
            errors.append(f"Kotlin profile value controller lost existing value behavior: {value_contract}")
    if "ProfileConfigValueControllerTest" not in read(ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/fragment/ProfileConfigValueControllerTest.kt"):
        errors.append("Kotlin profile value controller regression tests are missing")
    for dropdown_contract in (
        "ProfileDropdownMenuPolicy.create(", "ProfileDropdownMenuPolicy.selectedIndex(",
        "dropdownItemsSnapshot.getOrNull(safeIndex)", "selectItem(selected)",
        "refreshProfileDropdownTitle()", "refreshPendingActions()",
        "deleteCurrentProfile()", "cloneCurrentProfile()", "onUpdateConfigItems()",
    ):
        if dropdown_contract not in profile_dropdown_controller:
            errors.append(f"Kotlin profile dropdown lost existing behavior: {dropdown_contract}")
    for fragment_name, selection_call in (
        ("PrinterConfigFragment.kt", "SliceBeam.applyProfileSelection(printer = item.title)"),
        ("FilamentConfigFragment.kt", "SliceBeam.applyProfileSelection(filament = item.title)"),
        ("PrintConfigFragment.kt", "SliceBeam.applyProfileSelection(process = item.title)"),
    ):
        selection_source = uncommented(read(JAVA / "fragment" / fragment_name))
        if selection_call not in selection_source or "): Boolean" not in selection_source:
            errors.append(f"{fragment_name} must persist profile selection before reporting success to the menu")
        for rollback_contract in (
            "override fun onApplyConfig(title: String): Boolean",
            "previousStored.values", "if (!saveConfigOrNotify())",
            "profiles.add(removedIndex, stored)",
        ):
            if rollback_contract not in selection_source:
                errors.append(f"{fragment_name} profile edits/deletion are missing rollback contract {rollback_contract}")
        if "config." not in selection_source or "Configs.remove(copy)" not in selection_source:
            errors.append(f"{fragment_name} cloned profiles must be removed again when persistence fails")
    for selection_contract in (
        "onPrinterSelected = ::selectPrinter", "private fun selectPrinter(index: Int)",
        "SliceBeam.applyProfileSelection(printer = printerName)",
        'selectedFilament.intValue = WorkspaceProfileSelectionLogic.selectedIndex(filamentNames, presets["filament"])',
        'selectedProcess.intValue = WorkspaceProfileSelectionLogic.selectedIndex(processNames, presets["print"])',
    ):
        if selection_contract not in uncommented(workspace_profile_sheet):
            errors.append(f"Workspace printer switching is missing immediate apply/update behavior: {selection_contract}")
    if "fun savePendingProfileSettings(): Boolean" not in slicebeam or "SliceBeam.savePendingProfileSettings()" not in main_activity:
        errors.append("Pending profile edits are not saved when the app leaves the foreground")
    if "testBackgroundSavePersistsPendingProfileEdits" not in read(ROOT / "scripts/tests/E2ETestSuite.java"):
        errors.append("Pending profile background-save regression coverage is missing")
    snapshot_source = uncommented(read(JAVA / "config/ProfileConfigSnapshot.kt"))
    for snapshot_contract in (
        "class ProfileConfigSnapshot", "fun restore()", "saved.forEach(ObjectState::restore)",
        "target.addAll(saved.map(ObjectState::target))", "config.presets = presets",
    ):
        if snapshot_contract not in snapshot_source:
            errors.append(f"Profile save rollback is missing its snapshot contract: {snapshot_contract}")
    for filename, rollback_contract in (
        ("MainActivity.kt", "snapshot.restore()"),
        ("SetupActivity.kt", "snapshot.restore()"),
        ("boot/LoadSlic3rConfigTask.kt", "config.printerConfigs.remove(v2)"),
    ):
        source = uncommented(read(JAVA / filename))
        if rollback_contract not in source:
            errors.append(f"{filename} does not restore in-memory profiles after a failed save")
    for fragment_name, profile_type in (
        ("PrinterConfigFragment.kt", "PROFILE_LIST_PRINTER"),
        ("FilamentConfigFragment.kt", "PROFILE_LIST_FILAMENT"),
        ("PrintConfigFragment.kt", "PROFILE_LIST_PRINT"),
    ):
        source = uncommented(read(JAVA / "fragment" / fragment_name))
        if f"copy.profileListType = ConfigObject.{profile_type}" not in source:
            errors.append(f"{fragment_name} cloned profiles must retain their profile category for active selection")
    if "ProfileSettingsRepository.applyPendingFor(profileType, profile)" not in profile_fragment:
        errors.append("Pending profile edits must remain staged until the saved profile is committed successfully")
    if "if (!onApplyConfig(normalizedTitle))" not in profile_fragment or "ProfileSettingsRepository.clearDraft(profileType)" not in profile_fragment:
        errors.append("Pending profile edits must only be cleared after a successful profile save")
    for dropdown_action in ("R.string.SettingsDeleteProfile", "R.string.SettingsCloneProfile", "menu.canDelete"):
        if dropdown_action not in profile_dropdown_view:
            errors.append(f"Compose profile dropdown lost its labeled action: {dropdown_action}")
    if "ProfileListSelection.selected(getItems(false))?.title" not in profile_fragment:
        errors.append("Initial and resumed profile titles do not use the Kotlin dropdown controller")
    if "getSelectedItem()" in profile_fragment:
        errors.append("Active profile row selection is still implemented in the Java host")
    profile_selection = uncommented(read(JAVA / "fragment/ProfileListSelection.kt"))
    if "ProfileListSelection.selected(getItems(false))?.title" not in profile_dropdown_controller:
        errors.append("Profile dropdown title refresh bypasses the Kotlin selection policy")
    if "items.firstOrNull { it.isSelected() }" not in profile_selection:
        errors.append("Kotlin profile selection does not preserve the first selected profile")
    selection_test = read(ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/fragment/ProfileListSelectionTest.kt")
    for selection_case in ("returnsTheSelectedProfileAndKeepsListOrder", "returnsNullWhenNoProfileIsSelected"):
        if selection_case not in selection_test:
            errors.append(f"Profile list selection is missing the {selection_case} regression test")
    slice_beam = uncommented(read(JAVA / "SliceBeam.kt"))
    if "ProfileSettingsRepository.applyPendingTo(singleObject)" not in slice_beam:
        errors.append("Unsaved profile edits are not overlaid through ProfileSettingsRepository before slicing")
    print_config_def = uncommented(read(JAVA / "slic3r/PrintConfigDef.kt"))
    native_config_bridge = uncommented(read(JAVA / "slic3r/OrcaConfigBridge.kt"))
    native_kotlin = uncommented(read(JAVA / "slic3r/Native.kt"))
    native_bridge_cpp = uncommented(read(ROOT / "app/src/main/jni/slicebeam/beam_native.cpp"))
    native_jvm_methods = re.findall(
        r"(?m)^\s*@JvmStatic\s*\n\s*(?:@Throws\([^\n]+\)\s*\n\s*)?external fun ([A-Za-z0-9_]+)\([^\n]*\): [A-Za-z0-9_?]+",
        native_kotlin,
    )
    if len(native_jvm_methods) != 122 or len(set(native_jvm_methods)) != 122:
        errors.append("The Kotlin JNI facade must expose all 122 distinct methods as Java statics")
    jni_symbols = set(re.findall(
        r"Java_ru_ytkab0bp_slicebeam_slic3r_Native_([A-Za-z0-9_]+)\s*\(", native_bridge_cpp
    ))
    missing_jni_symbols = [
        method for method in native_jvm_methods if method.replace("_", "_1") not in jni_symbols
    ]
    if missing_jni_symbols:
        errors.append("Kotlin JNI facade no longer matches native C++ methods: " + ", ".join(missing_jni_symbols))
    if "OrcaConfigBridge.loadPrintConfigDefinition()" not in print_config_def:
        errors.append("PrintConfigDef does not load its schema through the Kotlin JNI facade")
    if "Native.get_print_config_def(definition)" not in native_config_bridge:
        errors.append("Kotlin JNI facade does not call the existing native schema entry point")
    if "Native.get_print_config_def" in print_config_def:
        errors.append("PrintConfigDef bypasses the Kotlin JNI facade")
    if "external fun get_print_config_def(def: PrintConfigDef): Unit" not in native_kotlin:
        errors.append("The existing get_print_config_def JNI declaration was changed or removed")
    if "Java_ru_ytkab0bp_slicebeam_slic3r_Native_get_1print_1config_1def" not in native_bridge_cpp:
        errors.append("The existing native Orca schema implementation is missing")
    if "errorMessage(error)" not in native_config_bridge or "OrcaConfigLoadResult" not in native_config_bridge:
        errors.append("Kotlin JNI facade does not return a typed error result")
    model_kotlin = uncommented(read(JAVA / "slic3r/Model.kt"))
    model_bridge = uncommented(read(JAVA / "slic3r/OrcaModelBridge.kt"))
    bed_fragment = uncommented(read(JAVA / "fragment/BedFragment.kt"))
    if "OrcaModelBridge.load(path, plateId)" not in model_kotlin:
        errors.append("Model constructors do not load files through the Kotlin JNI facade")
    if "Native.model_read_from_file(path, baseName(source.name), plateId)" not in model_bridge:
        errors.append("Kotlin model facade does not call the existing native file importer")
    if "external fun model_read_from_file(path: String, baseName: String, plateId: Int): Long" not in native_kotlin:
        errors.append("The existing model_read_from_file JNI declaration was changed or removed")
    if "Java_ru_ytkab0bp_slicebeam_slic3r_Native_model_1read_1from_1file" not in native_bridge_cpp:
        errors.append("The existing native model importer implementation is missing")
    if "std::unique_ptr<ModelRef> ref" not in native_bridge_cpp or "return (jlong) (intptr_t) ref.release()" not in native_bridge_cpp:
        errors.append("Native model import does not release temporary ModelRef state on failure")
    if "env->ReleaseStringUTFChars(path, chars)" not in native_bridge_cpp or "env->ReleaseStringUTFChars(base_name, baseChars)" not in native_bridge_cpp:
        errors.append("Native model import does not release JNI filename buffers on failure")
    if "val firstPlate = Model(f, 1)" not in bed_fragment or "platesModels.set(0, firstPlate)" not in bed_fragment:
        errors.append("Multi-plate loading no longer propagates active-plate import errors")
    if "resetFailedMultiPlateImport(importGeneration)" not in bed_fragment:
        errors.append("Failed multi-plate imports do not restore a valid empty plate state")
    reset_body = bed_fragment.split("private fun resetFailedMultiPlateImport", 1)[1].split("private fun loadModelBlocking", 1)[0]
    if "ViewUtils.postOnMainThread({" not in reset_body or "importGeneration != modelImportGeneration" not in reset_body:
        errors.append("Failed multi-plate imports must reset shared UI state on the main thread and reject stale failures")
    if "modelLoadInProgress || newPlateIndex" not in bed_fragment:
        errors.append("Plate selection must be disabled while a model import is in progress")
    visible_bed_temperature_keys = filament_option_keys & {"hot_plate_temp", "hot_plate_temp_initial_layer", "cool_plate_temp", "eng_plate_temp", "textured_plate_temp", "supertack_plate_temp"}
    if visible_bed_temperature_keys != {"hot_plate_temp", "hot_plate_temp_initial_layer"}:
        errors.append("Filament editor must expose exactly one general bed-temperature pair")
    if "OrcaFilamentSettingsBuilder.build(this)" not in filament_fragment:
        errors.append("Filament settings do not use the Kotlin settings builder")
    for retained_behavior in (
        "options[\"$surface$stage\"]",
        "Slic3rUtils.isPrinterCompatible(",
        "Slic3rUtils.isCompatible(",
        "seenTitles.add(profile.title.orEmpty())",
        "printChecker?.release()",
        "printerChecker?.release()",
    ):
        if retained_behavior not in filament_fragment:
            errors.append(f"Kotlin filament profile screen lost required behavior: {retained_behavior}")
    if "OrcaFilamentSettingsCatalog.categories()" not in filament_builder:
        errors.append("Kotlin filament builder does not render from the central settings catalog")
    if ".requireContext()" in filament_builder or ".requireContext()" in process_builder:
        errors.append("Profile settings builders must use the app fragment's custom context accessor")
    if "hasUniqueFilamentKeys()" not in filament_catalog:
        errors.append("Central filament catalog is missing its unique-key invariant")
    if len(filament_catalog_option_keys) != len(filament_option_keys):
        errors.append("Central filament catalog contains duplicate Orca keys")
    if re.search(r'\bsection\("[^"]+"\s*\)', filament_catalog):
        errors.append("Central filament catalog contains an empty group")
    printer_fragment = uncommented(read(JAVA / "fragment/PrinterConfigFragment.kt"))
    process_fragment = uncommented(read(JAVA / "fragment/PrintConfigFragment.kt"))
    printer_builder = uncommented(read(JAVA / "fragment/OrcaPrinterSettingsBuilder.kt"))
    if "OrcaPrintSettingsBuilder.build(this)" not in process_fragment:
        errors.append("Process settings are not rendered from the Kotlin settings builder")
    for retained_behavior in (
        "Slic3rUtils.layerHeightFitsNozzle(",
        "Slic3rUtils.lineWidthFitsNozzle(",
        "seenTitles.add(profile.title.orEmpty())",
        "MmuFilamentSync.synchronizeAll(printer)",
        "flush_volumes_matrix",
        "wipe_tower_x",
        "wipe_tower_y",
    ):
        if retained_behavior not in process_fragment:
            errors.append(f"Kotlin process profile screen lost required behavior: {retained_behavior}")
    if "OrcaPrinterSettingsBuilder.build(this)" not in printer_fragment:
        errors.append("Printer settings are not rendered from the Kotlin catalog adapter")
    for retained_behavior in (
        "MmuFilamentSync.synchronizeAll(selected)",
        "PrinterWebOriginPolicy.dashboardUrl(selected.get(\"print_host\"))",
        "ProfileSettingsRepository.applyPendingFor(getProfileListType(), selected)",
        "SliceBeam.applyProfileSelection(printer = item.title)",
    ):
        if retained_behavior not in printer_fragment:
            errors.append(f"Kotlin printer profile screen lost required behavior: {retained_behavior}")
    if "OrcaPrinterSettingsCatalog.leadingCategories()" not in printer_builder or "OrcaPrinterSettingsCatalog.trailingCategories()" not in printer_builder:
        errors.append("Kotlin printer builder does not render both fixed setting groups")
    if "repeat(extruderCount)" not in printer_builder or "extruderCount == 1) -1 else index" not in printer_builder:
        errors.append("Kotlin printer builder does not preserve per-extruder option indexing")
    if "definition.options[key]" not in printer_builder or "fragment.createOptionElement(option, extruderIndex)" not in printer_builder:
        errors.append("Kotlin printer builder must render native option definitions for each extruder")
    if "hasUniqueFixedKeys()" not in printer_catalog or "hasUniqueExtruderKeys()" not in printer_catalog:
        errors.append("Central printer catalog is missing its unique-key invariants")
    if len(printer_catalog_option_keys) != len(set(printer_catalog_option_keys)):
        errors.append("Central printer catalog contains duplicate Orca keys")
    if re.search(r'\bsection\("[^"]+"\s*\)', printer_catalog):
        errors.append("Central printer catalog contains an empty group")
    acceleration_section = re.search(r'section\("Acceleration limits",\s*([^)]*)\)', printer_catalog)
    expected_acceleration_keys = {
        "machine_max_acceleration_x", "machine_max_acceleration_y", "machine_max_acceleration_z",
        "machine_max_acceleration_e", "machine_max_acceleration_extruding", "machine_max_acceleration_retracting",
    }
    if not acceleration_section or not expected_acceleration_keys.issubset(
        string_set(r'"([^"]+)"', acceleration_section.group(1))
    ):
        errors.append("Machine acceleration settings must be grouped under Acceleration limits")
    if 'section("Maximum feedrates"' in printer_catalog:
        errors.append("Machine acceleration settings must not be labelled as maximum feedrates")
    group_localizer = uncommented(read(JAVA / "ui/settings/OrcaProfileGroupLocalizer.kt"))
    group_translation_keys = re.findall(r'"([^"]+)"\s+to\s+"[^"]+"', group_localizer)
    if len(group_translation_keys) != len(set(group_translation_keys)):
        errors.append("German profile group labels contain duplicate source keys")
    if 'OrcaProfileGroupLocalizer.mobileGroup(value' not in uncommented(read(JAVA / "fragment/ProfileSettingsCategoryTabsController.kt")):
        errors.append("Mobile profile group labels do not use the Kotlin localizer")
    profile_settings_filter = uncommented(read(JAVA / "ui/settings/ProfileSettingsFilter.kt"))
    if "OrcaProfileGroupLocalizer.expandGermanSearch(" not in profile_settings_filter:
        errors.append("German profile search terms do not use the Kotlin localizer")
    if 'OrcaProfileGroupLocalizer.filamentGroup(source, language)' not in filament_builder:
        errors.append("Filament group labels do not use the shared Kotlin localizer")
    for source, translated in (("Acceleration limits", "Beschleunigungsgrenzen"), ("Pressure advance", "Druckvorschub")):
        if f'"{source}" to "{translated}"' not in group_localizer:
            errors.append(f"Shared German profile label is missing or inconsistent: {source}")

    # Profile drafts must stay private to the Kotlin repository. These guards catch the old
    # Java-side ConfigObject aliases that let UI, Smart Slice, and rendering mutate them directly.
    for filename in (
        "fragment/ProfileListFragment.kt", "fragment/PrintConfigFragment.kt",
        "fragment/PrinterConfigFragment.kt", "fragment/FilamentConfigFragment.kt", "render/GLRenderer.kt",
        "smartslice/SmartSliceEngine.kt", "SliceBeam.kt",
    ):
        source = uncommented(read(JAVA / filename))
        if re.search(r"\bdiffObject\b|\bLIVE_DIFF_(?:PRINTER|PRINT|FILAMENT)\b|\bliveDiffFor\s*\(", source):
            errors.append(f"{filename} exposes or accesses a mutable profile draft directly")
    for api in ("pendingEntries(", "pendingCountFor(", "setPendingValues(", "applyPendingFor("):
        if api not in profile_repository:
            errors.append(f"ProfileSettingsRepository is missing the snapshot/update API {api}")
    if "ProfileSettingsPendingSummary.build(profileType, profile)" not in profile_fragment:
        errors.append("Profile change summaries do not use a repository snapshot and shared value resolver")
    if "ProfileSettingsRepository.setPendingValues(ConfigObject.PROFILE_LIST_PRINT, result.changes)" not in read(JAVA / "smartslice/SmartSliceEngine.kt"):
        errors.append("Smart Slice changes bypass the profile repository")
    if "ProfileSettingsRepository.setPendingValues(ConfigObject.PROFILE_LIST_PRINT, pending)" not in read(JAVA / "render/GLRenderer.kt"):
        errors.append("Prime tower drag changes bypass the profile repository")
    if "ProfileSettingsRepository.applyPendingFor(getProfileListType(), selected)" not in printer_fragment:
        errors.append("Printer actions do not use the printer-only pending snapshot")
    profile_view_model = uncommented(read(JAVA / "ui/settings/ProfileSettingsViewModel.kt"))
    for state_api in (
        "selectedCategory(", "setSelectedCategory(", "collapsedGroup(", "setCollapsedGroup(",
        "categoryExpanded(", "setCategoryExpanded(",
        "searchQuery(", "setSearchQuery(", "favoritesOnly(", "setFavoritesOnly(",
    ):
        if state_api not in profile_view_model:
            errors.append(f"Profile view model does not retain rotation state through {state_api}")
    category_tab_controller = uncommented(read(JAVA / "fragment/ProfileSettingsCategoryTabsController.kt"))
    if "ProfileSettingsCategoryTabsController(" not in profile_fragment:
        errors.append("Profile category tabs are not connected to the Kotlin tab controller")
    if "viewModel.selectedCategory(profileType)" not in category_tab_controller:
        errors.append("Selected profile category is not restored when the screen is recreated")
    for tab_contract in (
        "categoryTabsController?.selectedIndex", "categoryTabsController!!.updateHeadings(",
        "controller.select(index)", "categoryTabsController?.resetSelection()",
        "themeRevision++",
    ):
        if tab_contract not in profile_fragment:
            errors.append(f"Kotlin profile host lost its category-tab route: {tab_contract}")
    for retained_tab_behavior in (
        "viewModel.setSelectedCategory(profileType, selectedIndex)",
        "OrcaProfileGroupLocalizer.mobileGroup(value, Locale.getDefault().language)",
        "selected.coerceIn(0, (sourceTitles.size - 1).coerceAtLeast(0))",
        "if (index < 0 || index >= sourceTitles.size) return false",
        "labels = sourceTitles.map(::localize)", "ProfileCategoryTabs(labels, selectedIndex)",
    ):
        if retained_tab_behavior not in category_tab_controller:
            errors.append(f"Kotlin profile category tabs lost existing behavior: {retained_tab_behavior}")
    if 'state.headings.map { it.title.orEmpty() }' not in profile_fragment:
        errors.append("Profile category tabs do not safely handle an untitled special heading")
    profile_list_builder = uncommented(read(JAVA / "fragment/ProfileSettingsListBuilder.kt"))
    if "profileSettingsViewModel!!.setCollapsedGroup(profileType, group, expanded)" not in profile_fragment:
        errors.append("Expanded and collapsed profile groups are not retained by the view model")
    if "viewModel.collapsedGroup(profileType, groupKey)" not in profile_list_builder:
        errors.append("Profile group state is not restored by the Kotlin list builder")
    if "ProfileSettingsListBuilder.buildAccordion(" not in profile_fragment:
        errors.append("SliceBeam-style profile categories are missing their accordion route")
    if "profileSettingsViewModel?.categoryExpanded(profileType, profileTitle, category)" not in profile_fragment:
        errors.append("Profile category expansion is not isolated and restored by profile")
    if "profileSettingsViewModel.setSearchQuery(getProfileListType(), query)" not in profile_fragment:
        profile_filter_controller = uncommented(read(JAVA / "fragment/ProfileSettingsFilterController.kt"))
        if "viewModel.setSearchQuery(profileType, updatedQuery)" not in profile_filter_controller:
            errors.append("Profile search text is not retained by the view model")
    if "profileSettingsViewModel.setFavoritesOnly(getProfileListType(), favorites)" not in profile_fragment:
        profile_filter_controller = uncommented(read(JAVA / "fragment/ProfileSettingsFilterController.kt"))
        if "viewModel.setFavoritesOnly(profileType, showFavoritesOnly)" not in profile_filter_controller:
            errors.append("The favorite-only filter is not retained by the view model")

    settings_fragment_path = JAVA / "fragment/SettingsFragment.kt"
    settings_fragment = uncommented(read(settings_fragment_path))
    if (JAVA / "fragment/SettingsFragment.java").exists():
        errors.append("App settings are still implemented by the Java SettingsFragment")
    for retained_behavior in (
        "class SettingsFragment : Fragment()", "ComposeView(ctx)", "BeamComposeTheme",
        "expandedSections by mutableStateOf(emptySet<Int>())", "BeamServerData.isCloudAvailable()",
        "Prefs.setThemeMode(which)", "Prefs.setAccentColor(color)",
        "SetupActivity.AccentColors.DEFAULT.color", "Prefs.setRenderScale(variants[which])",
        "ConverterStorage.createFolderPickerIntent()", "REQUEST_CODE_CONVERTER_ROOT_SETTINGS",
        "SetupActivity.EXTRA_ADD_PRINTER",
        "SetupActivity.EXTRA_ABOUT", "SetupActivity.EXTRA_BOOSTY_ONLY", "SliceBeam.getConfigFile().delete()",
        "@EventHandler(runOnMainThread = true)", "BeamServerDataUpdatedEvent", "CloudUserInfoUpdatedEvent",
        "SliceBeam.EVENT_BUS.registerListener(this)", "SliceBeam.EVENT_BUS.unregisterListener(this)",
        "fun refreshConverterFolderSetting()", "SetupActivity.EXTRA_CLOUD_PROFILE",
    ):
        if retained_behavior not in settings_fragment:
            errors.append(f"Kotlin app settings screen lost required behavior: {retained_behavior}")

    slot_factory_path = JAVA / "navigation/DelegateSlotImpl.kt"
    slot_factory = uncommented(read(slot_factory_path))
    if (JAVA / "navigation/DelegateSlotImpl.java").exists():
        errors.append("Navigation slot factory is still implemented by Java")
    for retained_destination in (
        "open fun getSlotCount(): Int = 6",
        "1 -> MakerFragment()", "2 -> PrintConfigFragment()", "3 -> FilamentConfigFragment()",
        "4 -> PrinterConfigFragment()", "5 -> SettingsFragment()", "else -> BedFragment()",
        "R.string.SlotMaker", "R.string.SlotPrintConfig", "R.string.SlotFilamentConfig",
        "R.string.SlotPrinterConfig", "R.string.SlotAppSettings", "R.string.SlotBed",
    ):
        if retained_destination not in slot_factory:
            errors.append(f"Kotlin navigation factory lost a destination or label: {retained_destination}")

    mobile_navigation_policy = uncommented(read(JAVA / "navigation/MobileNavigationPolicy.kt"))
    for retained_navigation_behavior in (
        "const val PREVIEW_SLOT = 0", "const val MAKER_SLOT = 1", "const val PRINT_SLOT = 2",
        "const val FILAMENT_SLOT = 3", "const val PRINTER_SLOT = 4", "const val APP_SLOT = 5",
        "PREVIEW_SLOT,", "PRINT_SLOT,", "FILAMENT_SLOT,", "PRINTER_SLOT,", "APP_SLOT,",
        "fun destinations(): List<Int>", "fun isDestination(slot: Int): Boolean",
        "fun selectedDestination(slot: Int): Int = if (slot == MAKER_SLOT) APP_SLOT else slot",
    ):
        if retained_navigation_behavior not in mobile_navigation_policy:
            errors.append(f"Kotlin mobile navigation lost a destination or selection rule: {retained_navigation_behavior}")
    navigation_policy_test = uncommented(read(ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/navigation/MobileNavigationPolicyTest.kt"))
    for retained_navigation_test in (
        "mainDestinationsFollowTheSliceBeamWorkspaceOrder",
        "makerRemainsAvailableAsAnAppSubsectionInsteadOfTakingAMainTab",
        "MobileNavigationPolicy.destinations()",
        "MobileNavigationPolicy.selectedDestination(MobileNavigationPolicy.MAKER_SLOT)",
    ):
        if retained_navigation_test not in navigation_policy_test:
            errors.append(f"Kotlin mobile navigation is missing regression coverage: {retained_navigation_test}")

    mobile_navigation_path = JAVA / "navigation/MobileNavigationDelegate.kt"
    mobile_navigation = uncommented(read(mobile_navigation_path))
    if (JAVA / "navigation/MobileNavigationDelegate.java").exists():
        errors.append("Adaptive mobile navigation is still implemented by the Java delegate")
    for retained_behavior in (
        "AdaptiveNavigationPolicy.usesBottomNavigation(",
        "createNavigationHost(ctx)", "ComposeView(ctx)", "AdaptiveWorkspaceNavigation(",
        "ViewCompositionStrategy.DisposeOnDetachedFromWindow", "updateSelectedNavigationSlot(",
        "navigationThemeRevision++", "MobileNavigationPolicy.destinations().map { slot ->",
        "accessibilityLabel = ctx.getString(getSlotTooltip(slot))", "const val SETTINGS_SLOT = 5",
        "lastPrimarySlot", "override fun onBackPressed()", "super.onBackPressed()",
        "fun onConfigurationChanged(configuration: Configuration)",
        "layout.removeView(navigation)", "content?.layoutParams",
        "fragment.getView() ?: fragment.onCreateView(ctx)",
    ):
        if retained_behavior not in mobile_navigation:
            errors.append(f"Kotlin adaptive navigation lost required behavior: {retained_behavior}")
    if "container.removeAllViews()" in mobile_navigation:
        errors.append("Adaptive navigation must preserve the active screen while changing layout")
    adaptive_navigation = uncommented(read(JAVA / "ui/navigation/AdaptiveWorkspaceNavigation.kt"))
    for retained_component in (
        "Row(Modifier.fillMaxWidth().selectableGroup())", "Column(",
        "Role.Tab", "maxLines = 1", "softWrap = false",
        "fontSize = if (compactRail) 10.sp else 11.sp", "accessibilityLabel",
    ):
        if retained_component not in adaptive_navigation:
            errors.append(f"Compose adaptive navigation is missing {retained_component}")
    workspace_header = uncommented(read(JAVA / "ui/navigation/WorkspaceHeader.kt"))
    for retained_component in (
        "fun WorkspaceHeader(", "IconButton(", "actionDescription: String",
        "heightIn(min = 64.dp)", "TextOverflow.Ellipsis",
    ):
        if retained_component not in workspace_header:
            errors.append(f"Compose workspace header is missing {retained_component}")

    settings_filter = profile_settings_filter
    profile_filter_test = uncommented(read(ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/ui/settings/ProfileSettingsFilterTest.kt"))
    profile_host = uncommented(read(JAVA / "fragment/ProfileListFragment.kt"))
    section_filter = uncommented(read(JAVA / "fragment/ProfileOptionSectionFilter.kt"))
    section_filter_test = uncommented(read(ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/fragment/ProfileOptionSectionFilterTest.kt"))
    if "controller.filter(unfilteredConfigItems, accordionCategories = useAccordionCategories())" not in profile_host:
        errors.append("Kotlin profile host does not delegate filtering to the Kotlin filter controller")
    if "ProfileOptionSectionFilter.filter(" not in profile_filter_controller:
        errors.append("Kotlin profile filter controller does not use the section-filter builder")
    for retained_filter_behavior in (
        "isSectionHeader.test(item)", "matches.test(item)", "visible.add(currentHeader)",
        "visible.addAll(matchingRows)",
    ):
        if retained_filter_behavior not in section_filter:
            errors.append(f"Kotlin profile section filter lost required ordering behavior: {retained_filter_behavior}")
    for test_case in ("preservesMatchingRowsAndDropsEmptySections", "returnsNoOrphanRowsWhenNoSectionHasMatches"):
        if test_case not in section_filter_test:
            errors.append(f"Kotlin profile section filter lacks regression coverage: {test_case}")
    if "hierarchicalFilterKeepsOnlyMatchingGroupsAndTheirParentCategories" not in section_filter_test:
        errors.append("Hierarchical profile filtering lacks regression coverage")
    option_presenter = uncommented(read(JAVA / "fragment/ProfileOptionComposePresenter.kt"))
    fallback_option = uncommented(read(JAVA / "ui/settings/ComposeProfileFallbackOption.kt"))
    fallback_validator = uncommented(read(JAVA / "config/ProfileFallbackValueValidator.kt"))
    profile_option_element = uncommented(read(JAVA / "fragment/ProfileOptionElement.kt"))
    profile_option_help = uncommented(read(JAVA / "fragment/ProfileOptionHelpController.kt"))
    profile_filter_controller = uncommented(read(JAVA / "fragment/ProfileSettingsFilterController.kt"))
    profile_footer = uncommented(read(JAVA / "fragment/ProfileSettingsFooter.kt"))
    profile_footer_ui = uncommented(read(JAVA / "ui/settings/ComposeProfileSettingsFooter.kt"))
    profile_name_validator = uncommented(read(JAVA / "config/ProfileNameValidator.kt"))
    fallback_validation_test = uncommented(read(ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/config/ProfileFallbackValueValidatorTest.kt"))
    presenter_call = profile_option_element.find("ProfileOptionComposePresenter.create(")
    if presenter_call < 0 or "ProfileOptionLegacyPresenter" in profile_option_element:
        errors.append("Profile option rows must use the unified Compose presenter with no legacy editor fallback")
    if "ComposeProfileFallbackOption.create(" not in option_presenter:
        errors.append("Unmapped Orca setting types are missing their Compose fallback editor")
    if "ProfileFallbackValueValidator.validate(definition, editedValue)" not in fallback_option:
        errors.append("Compose fallback option editor is not connected to bounded-value validation")
    if "fun validate(definition: ConfigOptionDef, input: String): Result" not in fallback_validator:
        errors.append("Compose fallback option editor has no Orca setting validator")
    if "fun createOptionElement(definition: ConfigOptionDef, elementIndex: Int)" not in profile_host:
        errors.append("Kotlin profile host does not provide the shared option model with edit callbacks")
    if "class OptionElement extends ProfileOptionElement" in profile_host:
        errors.append("Profile option rows are still wrapped in a nested Java type")
    if "controller.showOptionHelp(context, definition)" not in profile_host:
        errors.append("Orca option help and favorite actions are not routed through the profile filter controller")
    if "ProfileOptionHelpController.show(" not in profile_filter_controller:
        errors.append("Profile filter controller does not delegate Orca option help to Kotlin")
    for option_element_contract in (
        "open class ProfileOptionElement(", "definition == null", "SpaceItem(0, 0)",
        "return ProfileOptionComposePresenter.create(", "boundIndex = position",
        "refreshRow?.accept(boundIndex)", "open fun setVisibleInAdvanced()",
    ):
        if option_element_contract not in profile_option_element:
            errors.append(f"Kotlin profile option model lost required behavior: {option_element_contract}")
    for help_contract in (
        "definition.getFullLabel()", "definition.tooltip", "SettingsAddFavorite", "SettingsRemoveFavorite",
        "favoriteKeys.remove(definition.key)", "favoriteKeys.add(definition.key)",
        "putStringSet(favoritesPreferenceKey, HashSet(favoriteKeys))", "favoritesOnly.getAsBoolean()",
        "onFavoritesChanged.run()",
    ):
        if help_contract not in profile_option_help:
            errors.append(f"Kotlin profile option help lost favorite behavior: {help_contract}")
    for compose_option in (
        "ComposeProfileBooleanOption.create(", "ComposeProfileNumericOption.create(",
        "ComposeProfileEnumOption.create(", "ComposeProfileEnumVectorOption.create(",
        "ComposeProfileVectorOption.create(", "ComposeProfilePointOption.create(",
        "ComposeProfileColorOption.create(", "ComposeProfileStringVectorOption.create(",
        "ComposeProfileStringOption.create(", "ComposeProfileOpenOption.create(",
    ):
        if compose_option not in option_presenter:
            errors.append(f"Kotlin profile option presenter lost a Compose editor route: {compose_option}")
    for compose_contract in (
        "ProfileStringVectorCodec.parse(valueProvider.get().orEmpty())", "ProfileVectorValueValidator.Kind.INTS",
        "ProfilePointValueValidator.Kind.POINT3", "GUIType.I_ENUM_OPEN",
        "PrinterHostOctoPrint", "PrinterHostMoonr", "ColorChooserSheet(",
        "ColorChooserSheet.OnColorChosen", 'onChanged.accept(String.format("#%06X", color and 0xFFFFFF))',
    ):
        if compose_contract not in option_presenter:
            errors.append(f"Kotlin profile option presenter lost a setting type or behavior: {compose_contract}")
    for fallback_contract in (
        "combinedClickable(", "onChanged.accept(choice.value)",
        "OutlinedTextField(", "ProfileFallbackValueValidator.Result.OUT_OF_RANGE",
        "definition.key.endsWith(\"_gcode\")", "maxLines = maxLines",
        "KeyboardOptions(keyboardType = inputType)",
    ):
        if fallback_contract not in fallback_option:
            errors.append(f"Compose profile fallback lost existing editor behavior: {fallback_contract}")
    for fallback_case in (
        "enforcesScalarIntegerAndPercentRanges", "validatesNumericVectorsWithoutAcceptingEmptyEntries",
        "leavesFreeTextAndOrcaEnumValuesUntouched",
    ):
        if fallback_case not in fallback_validation_test:
            errors.append(f"Compose fallback value validator is missing regression coverage: {fallback_case}")
    if "AtomicReference<AlertDialog>" in profile_host:
        errors.append("Legacy profile option dialogs are still constructed in the Java host")
    if "ProfileSettingsFooter.create(" not in profile_host:
        errors.append("Profile Save/Reset controls are not built by the Kotlin footer")
    if "profileFooter?.refreshPending(getProfileListType())" not in profile_host:
        errors.append("Profile pending-action state is not delegated to the Kotlin footer")
    for footer_contract in (
        "class Controller internal constructor(", "@Composable\n        fun Content(portrait: Boolean)",
        "ComposeProfileSettingsFooter(", "pendingSummary.get()",
        "fun refreshPending(profileType: Int)",
        "ProfileSettingsRepository.pendingCountFor(profileType) > 0",
    ):
        if footer_contract not in profile_footer:
            errors.append(f"Kotlin profile footer lost a save/reset behavior: {footer_contract}")
    for footer_contract in (
        "fun ComposeProfileSettingsFooter(", "Button(", "IconButton(",
        "R.string.SettingsResetProfileAction", "pendingSummary()",
        "changeSummary.isNotBlank()", "heightIn(max = 360.dp)",
        "OutlinedTextField(", "isError = !isValid", "enabled = isValid",
        "R.string.SettingsProfileNameInvalid", "ProfileNameValidator.isValid(title)",
    ):
        if footer_contract not in profile_footer_ui:
            errors.append(f"Compose profile footer lost an accessibility or dialog behavior: {footer_contract}")
    if "fun isValid(value: String): Boolean = value.isNotBlank() && value.none(forbiddenCharacters::contains)" not in profile_name_validator:
        errors.append("Profile-name validation no longer uses Orca's forbidden-character set")
    profile_name_test = uncommented(read(ROOT / "app/src/test/java/ru/ytkab0bp/slicebeam/config/ProfileNameValidatorTest.kt"))
    for validation_case in (
        "acceptsNormalNamesAndRejectsBlankNames",
        "rejectsEveryCharacterForbiddenByOrcaConfigWrapper",
        "detectsDuplicateNamesWithoutRejectingTheCurrentProfileName",
    ):
        if validation_case not in profile_name_test:
            errors.append(f"Profile-name validator is missing regression coverage: {validation_case}")
    for setting_label in (
        "SettingsProfileName", "SettingsProfileNameInvalid", "SettingsResetProfileAction",
    ):
        for locale in ("values", "values-de", "values-ru"):
            strings = read(ROOT / f"app/src/main/res/{locale}/strings.xml")
            if f'name="{setting_label}"' not in strings:
                errors.append(f"Compose profile footer string {setting_label} is missing from {locale}")
    if "protected lateinit var resetButton" in profile_host or "protected lateinit var saveButton" in profile_host:
        errors.append("Profile host still exposes legacy footer button views")
    if "ProfileSettingsFilter.matches(" not in profile_filter_controller:
        errors.append("Profile option filtering does not use the central Kotlin filter policy")
    for filter_contract in (
        "ProfileFilterBar(", "Prefs.getPrefs()?.edit()?.putInt(PREF_SETTINGS_LEVEL, updatedLevel)?.apply()",
        "viewModel.setSearchQuery(profileType, updatedQuery)",
        "viewModel.setFavoritesOnly(profileType, showFavoritesOnly)",
        "ProfileSettingsFilter.matches(", "PREF_SETTING_FAVORITES",
        "if (!tabsEnabled || items.isEmpty()) return items",
        "Slic3rLocalization.getString(definition.getFullLabel())",
        "matches(definition, element.visibleInAdvanced, query, searchableText)",
    ):
        if filter_contract not in profile_filter_controller:
            errors.append(f"Kotlin profile filter controller lost state or persistence behavior: {filter_contract}")
    if "ProfileListFragment.SubHeader" in profile_host or "ProfileListFragment.SubHint" in profile_host:
        errors.append("Profile group rows are still declared as Java nested view classes")
    if "ProfileSettingsListBuilder.build(" not in profile_host:
        errors.append("Kotlin profile host is not connected to the settings-list builder")
    if "SparseArray<out List<ProfileOptionWrapper>>" not in profile_host:
        errors.append("Kotlin profile host does not accept the builder's read-only category projection")
    if "SparseArray<out List<ProfileOptionWrapper>>" not in profile_list_builder:
        errors.append("Kotlin settings-list builder exposes an incompatible category collection type")
    for builder_contract in (
        "element.simpleItem as? ProfileSubHeader", "onGroupToggle.accept(groupKey, !collapsed)",
        "onRebuild.run()", "hideUntilNextHeader && element.title == null",
    ):
        if builder_contract not in profile_list_builder:
            errors.append(f"Kotlin settings-list builder lost group behavior: {builder_contract}")
    profile_compose_list = uncommented(read(JAVA / "fragment/ProfileSettingsComposeList.kt"))
    if "ProfileSettingsComposeList(" not in profile_host:
        errors.append("Profile host does not use the Compose settings list")
    for legacy_profile_list in ("ProfileSettingsRecyclerAdapter.kt", "ProfileSettingsRecyclerView.kt"):
        if (JAVA / "fragment" / legacy_profile_list).exists():
            errors.append(f"Legacy RecyclerView profile list source is still present: {legacy_profile_list}")
    for list_contract in (
        "class ProfileSettingsComposeList(", "LazyColumn(", "itemsIndexed(rows",
        "wrapper.hasSpecialType()", "ComposeProfileCategoryRow(", "ComposeCloudProfileHeader(",
        "CloudController.getUserInfo()", "R.string.SettingsCloudLoading",
        "ProfileSettingsRowRenderPolicy.usesDirectCompose(",
        "option.DirectContent(bindVersion, index, onEditorStateChanged)",
        "item.Content(index, onEditorStateChanged)", "option.bindRowPosition(index)",
        "categoryRowsUpdater.update(categoryIndex, index + 1, nextExpanded)",
        "ColorUtils.setAlphaComponent(", "drawWithContent", "setOverlayAlpha(alpha: Float)",
        "fun refreshTheme()", "emptyMessage",
    ):
        if list_contract not in profile_compose_list:
            errors.append(f"Compose profile list lost existing list or row behavior: {list_contract}")
    profile_category_row = uncommented(read(JAVA / "ui/settings/ComposeProfileCategoryRow.kt"))
    for category_contract in (
        "fun ComposeProfileCategoryRow(", "animateFloatAsState(", "stateDescription = stateLabel",
        "Role.Button", "R.string.ProfileGroupStateExpanded", "R.string.ProfileGroupExpandAction",
        "ConfigurationCompat.getLocales(LocalConfiguration.current)", "onClickLabel = actionLabel.takeIf { expandable }",
    ):
        if category_contract not in profile_category_row:
            errors.append(f"Compose profile category row lost animation, accessibility, or locale behavior: {category_contract}")
    for host_contract in (
        "currentList.addAll(insertionIndex, rows)", "currentList.removeAll(rows)",
        "recyclerView?.submit(currentList)", "recyclerView?.refreshItem(position)",
    ):
        if host_contract not in profile_host:
            errors.append(f"Kotlin profile host lost category-list update behavior: {host_contract}")
    if "fun bindRowPosition(position: Int)" not in profile_option_element:
        errors.append("Kotlin profile adapter cannot bind row positions for direct Compose option rows")
    if "AndroidView(" in profile_compose_list:
        errors.append("Compose profile settings must not wrap rows in an AndroidView compatibility fallback")
    profile_items = uncommented(read(JAVA / "fragment/ProfileSettingItems.kt"))
    for retained_item in (
        "class ProfileSubHeader", "class ProfileSubHint", "ProfileSettingsComposeItem",
        "override fun Content(rowPosition: Int, onEditorStateChanged: (Boolean) -> Unit)",
        "ProfileSettingGroupHeader(title, collapsed, onClick)",
        "ProfileSettingGroupHint(java.lang.String.valueOf(provider.provide()))",
        "HorizontalDivider(",
        "OrcaProfileGroupLocalizer.mobileGroup(", "stateDescription = stateLabel",
        "R.string.ProfileGroupStateCollapsed", "R.string.ProfileGroupExpandAction",
        "R.drawable.dropdown_24",
    ):
        if retained_item not in profile_items:
            errors.append(f"Compose profile group row lost required behavior or accessibility: {retained_item}")
    if "ComposeView(" in profile_items or "AndroidView(" in profile_items:
        errors.append("Profile headings and hints must render directly from the shared Compose row contract")
    for group_resource in (
        "ProfileGroupStateExpanded", "ProfileGroupStateCollapsed",
        "ProfileGroupExpandAction", "ProfileGroupCollapseAction",
    ):
        for locale in ("values", "values-de", "values-ru"):
            strings = read(ROOT / f"app/src/main/res/{locale}/strings.xml")
            if f'name="{group_resource}"' not in strings:
                errors.append(f"Compose profile group accessibility string {group_resource} is missing from {locale}")
    profile_row_views = uncommented(read(JAVA / "ui/settings/ComposeCloudProfileHeader.kt"))
    profile_compose_list = uncommented(read(JAVA / "fragment/ProfileSettingsComposeList.kt"))
    for retained_row_behavior in (
        "fun ComposeCloudProfileHeader(", "private fun CloudProfileAvatar(",
        "rememberGlideBitmap(avatarUrl, 26.dp, GlideBitmapShape.CIRCLE)",
        "Role.Button", "TextOverflow.Ellipsis",
    ):
        if retained_row_behavior not in profile_row_views:
            errors.append(f"Compose cloud profile row lost required behavior: {retained_row_behavior}")
    glide_bitmap_loader = uncommented(read(JAVA / "ui/components/GlideBitmap.kt"))
    for retained_image_behavior in (
        "DisposableEffect(context, model, sizePx, shape, cornerRadiusPx)",
        "CustomTarget<Bitmap>(sizePx, sizePx)",
        "requestManager.clear(target)", "request.circleCrop()",
        "override fun onLoadCleared(placeholder: Drawable?)", "resource.asImageBitmap()",
        ".override(sizePx, sizePx)",
    ):
        if retained_image_behavior not in glide_bitmap_loader:
            errors.append(f"Shared Compose image loader lost a lifecycle or memory bound: {retained_image_behavior}")
    if "AndroidView(" in profile_row_views or "ImageView" in profile_row_views:
        errors.append("Compose cloud profile avatar has regressed to an embedded Android ImageView")
    for retained_row_behavior in (
        "ComposeCloudProfileHeader(", "CloudController.getUserInfo()",
        "R.string.SettingsCloudLoading", "R.string.SettingsCloudNotLoggedIn",
        "R.string.SettingsCloudTapToManage", "R.string.SettingsCloudTapToShowMore",
    ):
        if retained_row_behavior not in profile_compose_list:
            errors.append(f"Compose cloud profile row lost a state or route: {retained_row_behavior}")
    if (JAVA / "fragment/ProfileListRowViews.kt").exists():
        errors.append("The migrated legacy profile row view source is still present")
    if "private final static class CategoryHolderView" in profile_host or "private final static class CloudProfileHeaderView" in profile_host:
        errors.append("Shared profile row views are still declared inside the Java host")
    if "class OptionWrapper" in profile_host:
        errors.append("Profile adapter wrappers are still declared inside the Java host")
    profile_wrapper = uncommented(read(JAVA / "fragment/ProfileOptionWrapper.kt"))
    for retained_wrapper_behavior in (
        "class ProfileOptionWrapper(", "ProfileOptionElement?",
        "fun hasSpecialType(): Boolean",
        "fun fromOption(element: ProfileOptionElement)",
        "if (element.title != null)", "if (element.specialType != -1)",
        "wrapper.color = element.color", "wrapper.noTint = element.noTint",
        "wrapper.onClick = element.onClick",
    ):
        if retained_wrapper_behavior not in profile_wrapper:
            errors.append(f"Kotlin profile adapter wrapper lost required behavior: {retained_wrapper_behavior}")
    if "SimpleRecyclerItem" in profile_wrapper or "RecyclerView" in profile_wrapper:
        errors.append("Profile wrapper still inherits a RecyclerView presentation contract")
    if "ProfileOptionWrapper.fromOption(element)" not in profile_host:
        errors.append("Kotlin profile adapter does not use the Kotlin option-wrapper factory")
    if "createProfileOptionWrapper" in profile_host:
        errors.append("Profile option-wrapper factory still lives in the Java host")
    pending_summary = uncommented(read(JAVA / "fragment/ProfileSettingsPendingSummary.kt"))
    for summary_contract in (
        "object ProfileSettingsPendingSummary", "ProfileSettingsRepository.pendingEntries(profileType)",
        "PrintConfigDef.getInstance().options[key]", "ProfileConfigValueController.valueFor(",
        "Slic3rLocalization.getString(definition.getFullLabel())",
    ):
        if summary_contract not in pending_summary:
            errors.append(f"Kotlin pending profile summary lost required behavior: {summary_contract}")
    if "ProfileSettingsPendingSummary.build(profileType, profile)" not in profile_host:
        errors.append("Kotlin profile host does not delegate the save summary to Kotlin")
    for builder in ("OrcaPrintSettingsBuilder.kt", "OrcaFilamentSettingsBuilder.kt", "OrcaPrinterSettingsBuilder.kt"):
        source = uncommented(read(JAVA / "fragment" / builder))
        if "ProfileSubHeader(" not in source:
            errors.append(f"{builder} does not use the Kotlin group header component")
    for retained_filter_behavior in (
        """"layer_height""", """"filename_format""", "OrcaProfileGroupLocalizer.expandGermanSearch(",
        "favoritesOnly && !isFavorite", "normalizedQuery.split(WHITESPACE)",
    ):
        if retained_filter_behavior not in settings_filter:
            errors.append(f"Kotlin profile filter lost required behavior: {retained_filter_behavior}")
    for test_case in (
        "simpleModeShowsOnlyCuratedOptions", "standardModeHidesExpertOptionsButExpertModeKeepsThem",
        "favoritesFilterAndSearchTermsAreCombined", "missingDefinitionsOnlyShowAsAdvancedPlaceholdersWithoutSearch",
    ):
        if test_case not in profile_filter_test:
            errors.append(f"Kotlin profile filter is missing regression coverage: {test_case}")

    navigation_base_path = JAVA / "navigation/NavigationDelegate.kt"
    navigation_base = uncommented(read(navigation_base_path))
    if (JAVA / "navigation/NavigationDelegate.java").exists():
        errors.append("Shared screen-stack navigation is still implemented by the Java base class")
    for retained_behavior in (
        "protected val fragmentStack = SparseArray<Stack<Fragment>>()",
        "fun switchSlot(slot: Int, onInterfaceChange: Runnable)",
        "onCheckDelayForSlotChange()", "switchAnimation?.cancel()",
        "previousFragment.onResume()", "currentFragment.onDestroy()",
        "if (stack.peek() === currentFragment) stack.pop()",
        "fun pushFragment(fragment: Fragment)", "fragment?.onBackPressed() == true",
        "fun getContainerView(): FrameLayout? = container",
    ):
        if retained_behavior not in navigation_base:
            errors.append(f"Kotlin navigation base lost required behavior: {retained_behavior}")

    fragment_path = JAVA / "navigation/Fragment.kt"
    fragment_source = uncommented(read(fragment_path))
    if (JAVA / "navigation/Fragment.java").exists():
        errors.append("Navigation screen lifecycle is still implemented by the Java Fragment base")
    for retained_behavior in (
        "abstract class Fragment", "open fun onCreate()", "open fun onResume()",
        "open fun onPause()", "open fun onDestroy()", "abstract fun onCreateView(ctx: Context): View",
        "open fun onViewCreated(view: View)", "fun getView(): View?", "open fun onBackPressed(): Boolean",
        "fun interface SlotChangeCallback", "fun needDelay(callback: Runnable): Boolean",
    ):
        if retained_behavior not in fragment_source:
            errors.append(f"Kotlin screen lifecycle lost required behavior: {retained_behavior}")

    for key, label in (("pressure_advance", "Pressure advance"), ("z_offset", "Z offset")):
        definition = re.search(
            r'this->add\("' + re.escape(key) + r'".*?(?:\n\s*def = this->add\(|\Z)',
            native_cpp,
            re.S,
        )
        if not definition or f'def->label = L("{label}")' not in definition.group(0):
            errors.append(f"Native label for {key} does not match its setting key")

    gcode_writer = read(NATIVE / "GCodeWriter.cpp")
    gcode = read(NATIVE / "GCode.cpp")
    if "SET_PRESSURE_ADVANCE ADVANCE=" not in gcode_writer:
        errors.append("Native G-code writer no longer emits the Klipper Pressure Advance command")
    if "m_config.pressure_advance.get_at(new_filament_id)" not in gcode:
        errors.append("Native G-code generator does not read Pressure Advance from the active filament")
    if "print_z + m_config.z_offset.value" not in gcode:
        errors.append("Native G-code generator does not apply printer Z offset to layer coordinates")

    if errors:
        raise AssertionError("\n".join(errors))
    print(
        f"Profile setting route audit passed: {visible_settings} visible settings resolve to native definitions "
        f"and matching profile parsers; {len(group_translation_keys)} German group labels are unique."
    )


if __name__ == "__main__":
    main()
