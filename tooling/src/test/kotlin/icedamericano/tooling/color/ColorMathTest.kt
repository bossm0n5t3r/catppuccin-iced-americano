package icedamericano.tooling.color

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ColorMathTest {
    private fun assertVector(expected: Vector3, actual: Vector3, tolerance: Double = 1e-7) {
        assertEquals(expected.x, actual.x, tolerance)
        assertEquals(expected.y, actual.y, tolerance)
        assertEquals(expected.z, actual.z, tolerance)
    }

    @Test
    fun transferAndHex() {
        assertEquals(0.0, srgbToLinear(0.0))
        assertEquals(1.0, srgbToLinear(1.0))
        assertEquals(0.04045 / 12.92, srgbToLinear(0.04045), 1e-15)
        assertEquals(((0.04046 + 0.055) / 1.055).pow(2.4), srgbToLinear(0.04046), 1e-15)
        assertEquals(0.21404114048223255, srgbToLinear(0.5), 1e-15)
        assertEquals(Vector3(0.0, 0.0, 0.0), hexToLinear("#000000"))
        assertEquals(Vector3(1.0, 1.0, 1.0), hexToLinear("#FFFFFF"))
        assertEquals(Vector3(1.0, 0.0, 0.0), hexToLinear("#ff0000"))
        for (invalid in listOf("", "FFFFFF", "#fff", "#12+345", "#12345g", "#1234567", "#１２３４５６")) {
            assertFailsWith<IllegalArgumentException>(invalid) { hexToLinear(invalid) }
        }
    }

    @Test
    fun wcagLuminanceAndContrast() {
        assertEquals(0.0, luminance("#000000"), 1e-15)
        assertEquals(1.0, luminance("#FFFFFF"), 1e-15)
        assertEquals(0.2126, luminance("#FF0000"), 1e-15)
        assertEquals(21.0, contrast("#000000", "#FFFFFF"), 1e-12)
        assertEquals(contrast("#123456", "#ABCDEF"), contrast("#ABCDEF", "#123456"), 1e-15)
        assertEquals(1.0, contrast("#123456", "#123456"), 1e-15)
    }

    @Test
    fun machadoPrimaryColumnsAndClipping() {
        val red = Vector3(1.0, 0.0, 0.0)
        val green = Vector3(0.0, 1.0, 0.0)
        val blue = Vector3(0.0, 0.0, 1.0)
        assertVector(Vector3(0.152286, 0.114503, 0.0), simulate(red, Vision.PROTANOPIA))
        assertVector(Vector3(1.0, 0.786281, 0.0), simulate(green, Vision.PROTANOPIA))
        assertVector(Vector3(0.0, 0.099216, 1.0), simulate(blue, Vision.PROTANOPIA))
        assertVector(Vector3(0.367322, 0.280085, 0.0), simulate(red, Vision.DEUTERANOPIA))
        assertVector(Vector3(0.860646, 0.672501, 0.042940), simulate(green, Vision.DEUTERANOPIA))
        assertVector(Vector3(0.0, 0.047413, 0.968881), simulate(blue, Vision.DEUTERANOPIA))
        val rgb = Vector3(0.2, 0.4, 0.8)
        assertEquals(rgb, simulate(rgb, Vision.NORMAL))
        // A mixed input detects clipping the terms before summation instead of the result.
        assertVector(Vector3(0.2875960, 0.4167858, 0.8215756), simulate(rgb, Vision.PROTANOPIA))
    }

    @Test
    fun oklabReferenceColors() {
        assertVector(Vector3(0.0, 0.0, 0.0), linearToOklab(Vector3(0.0, 0.0, 0.0)))
        assertVector(Vector3(1.0, 0.0, 0.0), linearToOklab(Vector3(1.0, 1.0, 1.0)))
        assertVector(
            Vector3(0.62795536, 0.22486306, 0.12584630),
            linearToOklab(Vector3(1.0, 0.0, 0.0)),
        )
    }

    @Test
    fun distances() {
        for (vision in Vision.entries) {
            assertEquals(0.0, distance("#123456", "#123456", vision), 0.0)
            assertEquals(
                distance("#123456", "#ABCDEF", vision),
                distance("#ABCDEF", "#123456", vision),
                1e-15,
            )
            assertTrue(distance("#000000", "#FFFFFF", vision) > 0.99)
        }
        assertEquals(1.0, distance("#000000", "#FFFFFF"), 1e-7)
        assertEquals(
            distance("#FF0000", "#000000", Vision.NORMAL),
            distance("#FF0000", "#000000"),
            0.0,
        )
    }
}
