package icedamericano.tooling.color

import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

fun srgbToLinear(channel: Double): Double =
    if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

fun hexToLinear(color: String): Vector3 {
    require(color.length == 7 && color[0] == '#') { "Expected #RRGGBB" }
    fun digit(index: Int): Int {
        return when (val c = color[index]) {
            in '0'..'9' -> c - '0'
            in 'a'..'f' -> c - 'a' + 10
            in 'A'..'F' -> c - 'A' + 10
            else -> throw IllegalArgumentException("Expected #RRGGBB")
        }
    }
    return Vector3(
        srgbToLinear((digit(1) * 16 + digit(2)) / 255.0),
        srgbToLinear((digit(3) * 16 + digit(4)) / 255.0),
        srgbToLinear((digit(5) * 16 + digit(6)) / 255.0),
    )
}

fun luminance(color: String): Double {
    val rgb = hexToLinear(color)
    return rgb.x * 0.2126 + rgb.y * 0.7152 + rgb.z * 0.0722
}

fun contrast(first: String, second: String): Double {
    val a = luminance(first)
    val b = luminance(second)
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)
}

// The linear-light result is clipped to the sRGB gamut, not gamut mapped.
fun simulate(rgb: Vector3, vision: Vision): Vector3 =
    when (vision) {
        Vision.NORMAL -> rgb
        Vision.PROTANOPIA -> PROTANOPIA_MATRIX.transformClamped(rgb, 0.0, 1.0)
        Vision.DEUTERANOPIA -> DEUTERANOPIA_MATRIX.transformClamped(rgb, 0.0, 1.0)
    }

// Björn Ottosson's linear-sRGB-to-OKLab conversion:
// https://bottosson.github.io/posts/oklab/#converting-from-linear-srgb-to-oklab
fun linearToOklab(rgb: Vector3): Vector3 {
    val r = rgb.x
    val g = rgb.y
    val b = rgb.z
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    return Vector3(
        0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
        1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
        0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
    )
}

fun distance(first: String, second: String, vision: Vision = Vision.NORMAL): Double {
    val a = linearToOklab(simulate(hexToLinear(first), vision))
    val b = linearToOklab(simulate(hexToLinear(second), vision))
    val x = a.x - b.x
    val y = a.y - b.y
    val z = a.z - b.z
    return sqrt(x * x + y * y + z * z)
}
