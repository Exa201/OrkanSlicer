#!/usr/bin/env python3
"""Verify the complete ZIP CRC table for each packaged Android APK."""

from pathlib import Path
import sys
import zipfile


def main() -> None:
    paths = [Path(value) for value in sys.argv[1:]]
    if not paths:
        raise SystemExit("Pass at least one APK path.")
    for path in paths:
        if not path.is_file():
            raise SystemExit(f"APK does not exist: {path}")
        with zipfile.ZipFile(path) as package:
            corrupt = package.testzip()
            if corrupt is not None:
                raise SystemExit(f"Corrupt ZIP entry in {path}: {corrupt}")
        print(f"ZIP CRC OK: {path}")


if __name__ == "__main__":
    main()
