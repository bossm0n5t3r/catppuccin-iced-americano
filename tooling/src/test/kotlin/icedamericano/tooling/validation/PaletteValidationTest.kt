package icedamericano.tooling.validation

import icedamericano.tooling.palette.Palette
import icedamericano.tooling.palette.PaletteValidationException
import icedamericano.tooling.palette.parsePalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class PaletteValidationTest {
    private fun fixture(): JsonObject =
        Json.parseToJsonElement(
            checkNotNull(javaClass.getResourceAsStream("/iced-americano.json"))
                .bufferedReader()
                .use { it.readText() }
        ) as JsonObject

    private fun withColor(root: JsonObject, token: String, value: JsonElement?): JsonObject {
        val colors = (root.getValue("colors") as JsonObject).toMutableMap()
        if (value == null) colors.remove(token) else colors[token] = value
        return JsonObject(root + ("colors" to JsonObject(colors)))
    }

    private fun parse(root: JsonObject): Palette = parsePalette(root.toString())

    @Test
    fun unroundedNormalTextBoundary() {
        val whiteBase = withColor(fixture(), "base", JsonPrimitive("#FFFFFF"))
        for ((color, expected) in listOf("#767676" to true, "#777777" to false)) {
            val result =
                validatePalette(parse(withColor(whiteBase, "subtext0", JsonPrimitive(color))))
            assertEquals(expected, result.passes, color)
            assertEquals(
                expected,
                result.required
                    .single { it.foreground == "subtext0" && it.background == "base" }
                    .passes,
            )
        }
    }

    @Test
    fun darkerSurfacesAreDiagnosticsOnly() {
        val result = validatePalette(parse(fixture()))
        assertTrue(result.passes)
        for (foreground in listOf("blue", "subtext0", "red")) {
            assertTrue(
                result.required
                    .single { it.foreground == foreground && it.background == "base" }
                    .passes
            )
            assertFalse(
                result.diagnostics
                    .single { it.foreground == foreground && it.background == "crust" }
                    .passes
            )
        }
    }

    @Test
    fun requiredBaseAndSelectionRegressionsFail() {
        val root = fixture()
        for ((token, color) in listOf("blue" to "#EFF1F5", "surface1" to "#25334D")) {
            assertFalse(
                validatePalette(parse(withColor(root, token, JsonPrimitive(color)))).passes,
                token,
            )
        }
    }

    @Test
    fun lowContrastYellowDoesNotFailValidation() {
        val root = fixture()
        val base = (root.getValue("colors") as JsonObject).getValue("base")
        val result = validatePalette(parse(withColor(root, "yellow", base)))
        assertTrue(result.passes)
        assertFalse(
            result.diagnostics
                .single { it.foreground == "yellow" && it.background == "base" }
                .passes
        )
    }

    @Test
    fun tokenAndHexFailuresHaveValidationException() {
        val root = fixture()
        val invalid =
            listOf(
                withColor(root, "blue", null),
                withColor(root, "extra", JsonPrimitive("#123456")),
                withColor(root, "blue", JsonPrimitive("#GGGGGG")),
                withColor(root, "blue", JsonPrimitive("#12345")),
                withColor(root, "blue", JsonPrimitive(123456)),
            )
        for (document in invalid) assertFailsWith<PaletteValidationException> { parse(document) }
        assertEquals(
            "#abcdef",
            parse(withColor(root, "blue", JsonPrimitive("#abcdef"))).colors["blue"],
        )
    }

    @Test
    fun malformedJsonAndSchemaFail() {
        assertFailsWith<IllegalArgumentException> { parsePalette("{") }
        assertFailsWith<IllegalArgumentException> { parsePalette("[]") }
        val root = fixture()
        for (document in
            listOf(
                JsonObject(root - "colors"),
                JsonObject(root + ("name" to JsonPrimitive(42))),
                JsonObject(root + ("semantics" to JsonPrimitive("invalid"))),
                JsonObject(root - "changes_from_latte"),
            )) {
            val error = assertFailsWith<IllegalArgumentException> { parse(document) }
            assertFalse(error is PaletteValidationException)
        }
    }
}
