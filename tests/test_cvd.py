"""Numerical contracts for the published CVD and OKLab transformations."""

import unittest

from scripts.cvd import (
    distance,
    hex_to_linear,
    linear_to_oklab,
    simulate,
    srgb_to_linear,
)


class CvdTests(unittest.TestCase):
    def assertVectorAlmostEqual(self, actual, expected, places=7):
        self.assertEqual(len(actual), len(expected))
        for value, reference in zip(actual, expected):
            self.assertAlmostEqual(value, reference, places=places)

    def test_srgb_transfer_and_hex_endpoints(self):
        self.assertEqual(srgb_to_linear(0), 0)
        self.assertEqual(srgb_to_linear(1), 1)
        self.assertAlmostEqual(srgb_to_linear(0.04045), 0.04045 / 12.92)
        self.assertAlmostEqual(srgb_to_linear(0.5), 0.21404114048223255)
        self.assertEqual(hex_to_linear("#000000"), (0, 0, 0))
        self.assertEqual(hex_to_linear("#FFFFFF"), (1, 1, 1))
        self.assertEqual(hex_to_linear("#ff0000"), (1, 0, 0))
        for invalid in ("FFFFFF", "#fff", "#12+345", "#12345g", "#1234567"):
            with self.subTest(invalid=invalid), self.assertRaises(ValueError):
                hex_to_linear(invalid)

    def test_machado_primary_columns_and_clipping(self):
        # Columns of the authors' severity-1 matrices, clipped after multiplying.
        self.assertVectorAlmostEqual(
            simulate((1, 0, 0), "protanopia"), (0.152286, 0.114503, 0)
        )
        self.assertVectorAlmostEqual(
            simulate((0, 1, 0), "protanopia"), (1, 0.786281, 0)
        )
        self.assertVectorAlmostEqual(
            simulate((0, 0, 1), "protanopia"), (0, 0.099216, 1)
        )
        self.assertVectorAlmostEqual(
            simulate((1, 0, 0), "deuteranopia"), (0.367322, 0.280085, 0)
        )
        self.assertVectorAlmostEqual(
            simulate((0, 1, 0), "deuteranopia"), (0.860646, 0.672501, 0.042940)
        )
        self.assertVectorAlmostEqual(
            simulate((0, 0, 1), "deuteranopia"), (0, 0.047413, 0.968881)
        )
        self.assertEqual(simulate((0.2, 0.4, 0.8), "normal"), (0.2, 0.4, 0.8))
        with self.assertRaises(KeyError):
            simulate((0, 0, 0), "unknown")

    def test_oklab_black_white_and_red(self):
        # Ottosson's published linear-sRGB conversion, D65 white and red.
        self.assertVectorAlmostEqual(linear_to_oklab((0, 0, 0)), (0, 0, 0))
        self.assertVectorAlmostEqual(linear_to_oklab((1, 1, 1)), (1, 0, 0))
        self.assertVectorAlmostEqual(
            linear_to_oklab((1, 0, 0)), (0.62795536, 0.22486306, 0.12584630)
        )

    def test_distance_relationships(self):
        for vision in ("normal", "protanopia", "deuteranopia"):
            with self.subTest(vision=vision):
                self.assertEqual(distance("#123456", "#123456", vision), 0)
                self.assertAlmostEqual(
                    distance("#123456", "#ABCDEF", vision),
                    distance("#ABCDEF", "#123456", vision),
                )
                self.assertGreater(distance("#000000", "#FFFFFF", vision), 0.99)
        self.assertAlmostEqual(distance("#000000", "#FFFFFF"), 1, places=7)


if __name__ == "__main__":
    unittest.main()
