"""Consumer contracts for the generated, installable Ghostty theme."""

import copy
import json
import subprocess
import sys
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from scripts.generate_ghostty import render_theme

ROOT = Path(__file__).resolve().parents[1]


def parse_theme(theme: str) -> tuple[dict, dict]:
    scalars, ansi = {}, {}
    for line in theme.splitlines():
        if not line or line.startswith("#"):
            continue
        key, value = line.split(" = ")
        if key == "palette":
            index, color = value.split("=")
            index = int(index)
            if index in ansi:
                raise AssertionError(f"Duplicate ANSI index {index}")
            ansi[index] = color
        else:
            if key in scalars:
                raise AssertionError(f"Duplicate setting {key}")
            scalars[key] = value
    return scalars, ansi


class GhosttyGenerationTests(unittest.TestCase):
    def setUp(self) -> None:
        self.palette = json.loads(
            (ROOT / "palette/iced-americano.json").read_text(encoding="utf-8")
        )

    def cli(self, source: Path, output: Path, *flags: str):
        return subprocess.run(
            [
                sys.executable,
                "-B",
                "-m",
                "scripts.generate_ghostty",
                "--palette",
                str(source),
                "--output",
                str(output),
                *flags,
            ],
            cwd=ROOT,
            capture_output=True,
            text=True,
            check=False,
        )

    def test_production_terminal_roles(self) -> None:
        scalars, ansi = parse_theme(render_theme(self.palette))
        self.assertEqual(
            scalars,
            {
                "background": "#EFF1F5",
                "foreground": "#25334D",
                "cursor-color": "#1D62ED",
                "cursor-text": "#EFF1F5",
                "selection-background": "#BCC0CC",
                "selection-foreground": "#25334D",
            },
        )
        self.assertEqual(
            ansi,
            dict(
                enumerate(
                    (
                        "#25334D",
                        "#D20F39",
                        "#006F80",
                        "#DF8E1D",
                        "#1D62ED",
                        "#EA76CB",
                        "#179299",
                        "#454D65",
                        "#686B81",
                        "#D20F39",
                        "#006F80",
                        "#DF8E1D",
                        "#1D62ED",
                        "#EA76CB",
                        "#179299",
                        "#25334D",
                    )
                )
            ),
        )

    def test_invalid_inputs_identify_field_and_preserve_input(self) -> None:
        cases = [(None, "palette"), ([], "palette"), ({}, "palette.colors")]
        cases.extend(({"colors": value}, "palette.colors") for value in (None, [], 42))
        missing = copy.deepcopy(self.palette)
        del missing["colors"]["blue"]
        cases.append((missing, "palette.colors.blue"))
        for value in (None, 42, "#GGGGGG", "#123"):
            invalid = copy.deepcopy(self.palette)
            invalid["colors"]["blue"] = value
            cases.append((invalid, "palette.colors.blue"))
        for field in ("version", "status"):
            missing = copy.deepcopy(self.palette)
            del missing[field]
            cases.append((missing, f"palette.{field}"))
            for value in (None, 42, "", "  ", "bad\rvalue", "bad\nvalue"):
                invalid = copy.deepcopy(self.palette)
                invalid[field] = value
                cases.append((invalid, f"palette.{field}"))
        for palette, field in cases:
            with self.subTest(palette=palette):
                before = copy.deepcopy(palette)
                with self.assertRaises(ValueError) as raised:
                    render_theme(palette)
                self.assertIn(field, str(raised.exception))
                self.assertEqual(palette, before)

    def test_central_palette_change_updates_consumers(self) -> None:
        before = copy.deepcopy(self.palette)
        original = render_theme(self.palette)
        self.assertEqual(original.encode(), render_theme(self.palette).encode())
        self.assertEqual(self.palette, before)
        changed = copy.deepcopy(self.palette)
        changed["colors"]["blue"] = "#112233"
        scalars, ansi = parse_theme(render_theme(changed))
        self.assertEqual(scalars["cursor-color"], "#112233")
        self.assertEqual((ansi[4], ansi[12]), ("#112233", "#112233"))
        self.assertEqual((ansi[1], ansi[9]), ("#D20F39", "#D20F39"))
        changed["colors"]["blue"] = "#aabbcc"
        changed["colors"]["unused"] = None
        changed["semantics"] = None
        self.assertEqual(
            parse_theme(render_theme(changed))[0]["cursor-color"], "#AABBCC"
        )
        changed["version"] = "9.8.7"
        changed["status"] = "custom"
        theme = render_theme(changed)
        self.assertIn("v9.8.7; custom", theme)
        self.assertEqual(parse_theme(theme)[0]["cursor-color"], "#AABBCC")

    def test_cli_regeneration_stale_and_missing(self) -> None:
        with TemporaryDirectory() as directory:
            source = Path(directory) / "palette.json"
            output = Path(directory) / "nested/theme"
            source.write_text(json.dumps(self.palette), encoding="utf-8")
            missing = self.cli(source, output, "--check")
            self.assertEqual(missing.returncode, 1)
            self.assertIn(str(output), missing.stderr)
            self.assertFalse(output.parent.exists())
            self.assertEqual(self.cli(source, output).returncode, 0)
            self.assertEqual(output.read_bytes(), render_theme(self.palette).encode())
            self.assertEqual(self.cli(source, output, "--check").returncode, 0)
            old = output.read_bytes()
            self.palette["colors"]["blue"] = "#112233"
            source.write_text(json.dumps(self.palette), encoding="utf-8")
            stale = self.cli(source, output, "--check")
            self.assertEqual(stale.returncode, 1)
            self.assertIn(str(output), stale.stderr)
            self.assertEqual(output.read_bytes(), old)
            self.assertEqual(self.cli(source, output).returncode, 0)
            self.assertEqual(self.cli(source, output, "--check").returncode, 0)
            self.assertEqual(parse_theme(output.read_text())[1][4], "#112233")

    def test_cli_errors_preserve_existing_output(self) -> None:
        with TemporaryDirectory() as directory:
            source = Path(directory) / "palette.json"
            output = Path(directory) / "theme"
            output.write_bytes(b"existing output\n")
            invalid = copy.deepcopy(self.palette)
            invalid["colors"]["blue"] = "#GGGGGG"
            for content in (None, "{", json.dumps(invalid)):
                if content is not None:
                    source.write_text(content, encoding="utf-8")
                for flags in ((), ("--check",)):
                    result = self.cli(source, output, *flags)
                    self.assertEqual(result.returncode, 2, result.stderr)
                    self.assertEqual(output.read_bytes(), b"existing output\n")

    def test_cli_invalid_metadata_preserves_output(self) -> None:
        with TemporaryDirectory() as directory:
            source = Path(directory) / "palette.json"
            output = Path(directory) / "theme"
            output.write_bytes(b"existing output\n")
            for field in ("version", "status"):
                for value in (None, 42, "", "  ", "bad\rvalue", "bad\nvalue"):
                    for missing in (False, True) if value is None else (False,):
                        with self.subTest(field=field, value=value, missing=missing):
                            invalid = copy.deepcopy(self.palette)
                            if missing:
                                del invalid[field]
                            else:
                                invalid[field] = value
                            source.write_text(json.dumps(invalid), encoding="utf-8")
                            for flags in ((), ("--check",)):
                                result = self.cli(source, output, *flags)
                                self.assertEqual(result.returncode, 2, result.stderr)
                                self.assertIn(f"palette.{field}", result.stderr)
                                self.assertEqual(
                                    output.read_bytes(), b"existing output\n"
                                )

    def test_cli_rejects_input_aliases_and_directory_output(self) -> None:
        with TemporaryDirectory() as directory:
            source = Path(directory) / "palette.json"
            source.write_text(json.dumps(self.palette), encoding="utf-8")
            original = source.read_bytes()
            alias = Path(directory) / "alias"
            alias.hardlink_to(source)
            link = Path(directory) / "link"
            link.symlink_to(source)
            for output in (source, alias, link, Path(directory)):
                for flags in ((), ("--check",)):
                    result = self.cli(source, output, *flags)
                    self.assertEqual(result.returncode, 2, result.stderr)
                    self.assertEqual(source.read_bytes(), original)

    def test_installed_artifact_matches_source(self) -> None:
        self.assertEqual(
            (ROOT / "ports/ghostty/iced-americano").read_bytes(),
            render_theme(self.palette).encode("utf-8"),
        )


if __name__ == "__main__":
    unittest.main()
