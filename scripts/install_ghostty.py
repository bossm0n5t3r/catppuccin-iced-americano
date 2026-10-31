"""Install the Ghostty theme symlink and select it without replacing other settings."""

import argparse
import os
import re
import sys
from pathlib import Path

THEME_NAME = "iced-americano"
THEME_SOURCE = Path(__file__).resolve().parents[1] / "ports" / "ghostty" / THEME_NAME
THEME_SETTING = re.compile(r"^\s*theme\s*=")


def default_config(config_dir: Path) -> Path:
    """Prefer existing XDG settings, including the legacy config filename."""
    candidates = [config_dir / "config.ghostty", config_dir / "config"]
    if sys.platform == "darwin":
        mac_dir = Path.home() / "Library/Application Support/com.mitchellh.ghostty"
        candidates.extend([mac_dir / "config.ghostty", mac_dir / "config"])
    return next((path for path in candidates if path.is_file()), candidates[0])


def install(config: Path, config_dir: Path) -> None:
    """Replace the installed theme and keep exactly one active theme setting."""
    if not THEME_SOURCE.is_file():
        raise FileNotFoundError(f"Missing generated theme: {THEME_SOURCE}")
    target = config_dir / "themes" / THEME_NAME
    if target.is_dir() and not target.is_symlink():
        raise IsADirectoryError(f"Theme destination is a directory: {target}")
    if config.resolve() in (THEME_SOURCE, target.resolve()):
        raise ValueError("Config must not be the theme file")

    original = config.read_text(encoding="utf-8") if config.exists() else ""
    lines = []
    found = False
    for line in original.splitlines(keepends=True):
        if THEME_SETTING.match(line):
            if not found:
                lines.append(f"theme = {THEME_NAME}\n")
                found = True
        else:
            lines.append(line)
    updated = "".join(lines)
    if not found:
        if updated and not updated.endswith("\n"):
            updated += "\n"
        updated += f"theme = {THEME_NAME}\n"

    target.parent.mkdir(parents=True, exist_ok=True)
    if target.is_symlink() or target.exists():
        target.unlink()
    target.symlink_to(THEME_SOURCE)
    if updated != original:
        config.parent.mkdir(parents=True, exist_ok=True)
        config.write_text(updated, encoding="utf-8")
    print(f"Theme: {target} -> {THEME_SOURCE}")
    print(f"Config: {config} (theme = {THEME_NAME})")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, help="Ghostty config file to update")
    args = parser.parse_args()
    config_home = Path(os.environ.get("XDG_CONFIG_HOME") or Path.home() / ".config")
    config_dir = config_home.expanduser() / "ghostty"
    config = args.config.expanduser() if args.config else default_config(config_dir)
    try:
        install(config, config_dir)
    except (OSError, ValueError) as exc:
        parser.error(str(exc))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
