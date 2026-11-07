#!/usr/bin/env python3
"""Reconcile the historical Orca feature-gap notes with today's mobile setting catalogs."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from check_profile_setting_routes import JAVA, NATIVE, catalog_option_keys, read, uncommented  # noqa: E402


def native_options() -> set[str]:
    cpp = uncommented(read(NATIVE / "PrintConfig.cpp"))
    hpp = uncommented(read(NATIVE / "PrintConfig.hpp"))
    preset_cpp = read(NATIVE / "Preset.cpp")
    keys = set(re.findall(r'\bthis->add\(\s*"([^"]+)"', cpp))
    keys.update(re.findall(r"\(\(ConfigOption\w+,\s*([a-zA-Z_][\w]*)\)\)", hpp))
    ignored = re.search(r"static std::set<std::string> ignore\s*=\s*\{(.*?)\n\s*\};", cpp, re.S)
    if ignored:
        keys.difference_update(re.findall(r'"([^"]+)"', ignored.group(1)))
    for list_name in (
        "s_Preset_print_options",
        "s_Preset_filament_options",
        "s_Preset_printer_options",
        "s_Preset_machine_limits_options",
    ):
        match = re.search(
            r"static std::vector<std::string>\s+" + list_name + r"\s*\{(.*?)\n\s*\};",
            preset_cpp,
            re.S,
        )
        if match:
            keys.update(re.findall(r'"([^"]+)"', uncommented(match.group(1))))
    keys.update(f"machine_max_{kind}_{axis}" for kind in ("speed", "acceleration", "jerk") for axis in "xyze")
    return keys


def section(source: str, heading: str, next_heading: str) -> str:
    start = source.index(heading) + len(heading)
    end = source.index(next_heading, start)
    return source[start:end]


def documented_native_keys(source: str, native: set[str]) -> set[str]:
    candidates = {
        key
        for key in native
        if re.search(r"(?<![a-z0-9_])" + re.escape(key) + r"(?![a-z0-9_])", source)
    }
    for prefix in re.findall(r"\b([a-z][a-z0-9_]*_)\*", source):
        candidates.update(key for key in native if key.startswith(prefix))
    return candidates


def main() -> None:
    audit = read(ROOT / "docs/ORCA_FEATURE_AUDIT.md")
    catalogs = {
        "print": set(catalog_option_keys(read(JAVA / "ui/settings/OrcaProcessSettingsCatalog.kt"))),
        "filament": set(catalog_option_keys(read(JAVA / "ui/settings/OrcaFilamentSettingsCatalog.kt"))),
        "printer": set(catalog_option_keys(read(JAVA / "ui/settings/OrcaPrinterSettingsCatalog.kt"))),
    }
    all_visible = set.union(*catalogs.values())
    native = native_options()
    sections = (
        ("print", "## 1. Process settings: current coverage and remaining keys", "## 2. Filament settings: current coverage and remaining keys"),
        ("filament", "## 2. Filament settings: current coverage and remaining keys", "## 3. Printer settings: current coverage and remaining gaps"),
        ("printer", "## 3. Printer settings: current coverage and remaining gaps", "## 4. Missing model-editing tools"),
    )

    print(f"Native option keys parsed: {len(native)}")
    print(f"Profile catalog keys: {len(all_visible)} (process {len(catalogs['print'])}, filament {len(catalogs['filament'])}, printer {len(catalogs['printer'])})")
    print("This is a focused reconciliation of native keys named in the audit sections, not a full Orca desktop parity scan.")
    for profile, heading, next_heading in sections:
        listed = documented_native_keys(section(audit, heading, next_heading), native)
        already_anywhere = listed & all_visible
        assigned_here = listed & catalogs[profile]
        elsewhere = already_anywhere - assigned_here
        still_not_in_a_profile_catalog = listed - all_visible
        print(f"\n{profile}: {len(listed)} native keys mentioned")
        print(f"  already shown in intended profile: {len(assigned_here)}")
        print(f"  shown in another profile catalog: {len(elsewhere)}" + (f" ({', '.join(sorted(elsewhere))})" if elsewhere else ""))
        print(f"  not in any profile catalog: {len(still_not_in_a_profile_catalog)}")
        if still_not_in_a_profile_catalog:
            print("    " + ", ".join(sorted(still_not_in_a_profile_catalog)))


if __name__ == "__main__":
    main()
