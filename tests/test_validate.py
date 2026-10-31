"""Contrast boundaries and required-versus-diagnostic palette contracts."""

import contextlib
import io
import json
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from scripts.validate import contrast, luminance, main

ROOT = Path(__file__).resolve().parents[1]


class ContrastTests(unittest.TestCase):
    def setUp(self) -> None:
        self.palette = json.loads(
            (ROOT / "palette/iced-americano.json").read_text(encoding="utf-8")
        )

    def validate(self) -> tuple[int, str]:
        with TemporaryDirectory() as directory:
            path = Path(directory) / "palette.json"
            path.write_text(json.dumps(self.palette), encoding="utf-8")
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                result = main(path)
            return result, output.getvalue()

    def test_wcag_endpoints_and_symmetry(self) -> None:
        self.assertEqual(luminance("#000000"), 0)
        self.assertEqual(luminance("#FFFFFF"), 1)
        self.assertEqual(contrast("#000000", "#FFFFFF"), 21)
        self.assertEqual(contrast("#25334D", "#25334D"), 1)
        self.assertEqual(contrast("#25334D", "#EFF1F5"), contrast("#EFF1F5", "#25334D"))

    def test_unrounded_normal_text_boundary(self) -> None:
        self.palette["colors"]["base"] = "#FFFFFF"
        for color, expected in (("#767676", 0), ("#777777", 1)):
            with self.subTest(color=color):
                self.palette["colors"]["subtext0"] = color
                self.assertEqual(self.validate()[0], expected)

    def test_darker_surfaces_are_diagnostic_not_universal_requirements(self) -> None:
        colors = self.palette["colors"]
        for foreground in ("blue", "subtext0", "red"):
            self.assertGreaterEqual(contrast(colors[foreground], colors["base"]), 4.5)
            self.assertLess(contrast(colors[foreground], colors["crust"]), 4.5)
        self.assertEqual(self.validate()[0], 0)

    def test_required_base_and_selection_regressions_fail(self) -> None:
        for token, value in (("blue", "#EFF1F5"), ("surface1", "#25334D")):
            with self.subTest(token=token):
                original = self.palette["colors"][token]
                self.palette["colors"][token] = value
                self.assertEqual(self.validate()[0], 1)
                self.palette["colors"][token] = original

    def test_low_contrast_accent_is_not_a_normal_text_failure(self) -> None:
        self.palette["colors"]["yellow"] = self.palette["colors"]["base"]
        self.assertEqual(self.validate()[0], 0)

    def test_existing_token_and_hex_checks_remain_errors(self) -> None:
        self.palette["colors"]["blue"] = "#GGGGGG"
        self.assertEqual(self.validate()[0], 1)
        del self.palette["colors"]["blue"]
        self.assertEqual(self.validate()[0], 1)


if __name__ == "__main__":
    unittest.main()
