#!/usr/bin/env python3
"""Report whether the tracked official OrcaSlicer nightly changed."""

from pathlib import Path
import re
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[1]
LOCK_FILE = ROOT / "ORCA_NIGHTLY_LOCK.md"
REMOTE_REF = "orcaslicer-nightly/main"


def git(*args: str) -> str:
    return subprocess.check_output(["git", "-C", str(ROOT), *args], text=True).strip()


def main() -> int:
    lock = LOCK_FILE.read_text(encoding="utf-8")
    match = re.search(r"^- Commit: `([0-9a-f]{40})`$", lock, re.MULTILINE)
    if not match:
        print("Nightly lock is missing a valid commit hash.", file=sys.stderr)
        return 2

    locked = match.group(1)
    git("fetch", "--depth=1", "orcaslicer-nightly", "main")
    current = git("rev-parse", REMOTE_REF)
    print(f"Locked nightly:  {locked}")
    print(f"Current nightly: {current}")

    if current == locked:
        print("Nightly is unchanged.")
        return 0

    print("Nightly changed. Review the engine and Android-port deltas before updating the lock.")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
