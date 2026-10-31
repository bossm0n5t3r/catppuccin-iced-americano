package icedamericano.tooling.palette

import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val expectedTokens =
    setOf(
        "rosewater",
        "flamingo",
        "pink",
        "mauve",
        "red",
        "maroon",
        "peach",
        "yellow",
        "green",
        "teal",
        "sky",
        "sapphire",
        "blue",
        "lavender",
        "text",
        "subtext1",
        "subtext0",
        "overlay2",
        "overlay1",
        "overlay0",
        "surface2",
        "surface1",
        "surface0",
        "base",
        "mantle",
        "crust",
    )
private val hexPattern = Regex("#[0-9a-fA-F]{6}")

private fun JsonElement?.objectAt(path: String): JsonObject =
    this as? JsonObject ?: throw IllegalArgumentException("$path must be an object")

private fun JsonElement?.stringAt(path: String): String {
    val value = this as? JsonPrimitive
    if (value == null || !value.isString) throw IllegalArgumentException("$path must be a string")
    return value.content
}

private fun colorValue(element: JsonElement?, path: String): String {
    val value = element as? JsonPrimitive
    if (value == null || !value.isString || !hexPattern.matches(value.content)) {
        throw PaletteValidationException("$path must be a six-digit HEX color")
    }
    return value.content
}

private fun historicalColor(element: JsonElement?, path: String): String {
    val value = element.stringAt(path)
    if (!hexPattern.matches(value))
        throw PaletteValidationException("$path must be a six-digit HEX color")
    return value
}

fun readPalette(path: Path): Palette = parsePalette(Files.readString(path))

internal fun parsePalette(text: String): Palette {
    val root =
        try {
                Json.parseToJsonElement(text)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Malformed palette JSON", e)
            }
            .objectAt("palette")
    val colorsObject = root["colors"].objectAt("colors")
    if (colorsObject.keys != expectedTokens) {
        val missing = expectedTokens - colorsObject.keys
        val extra = colorsObject.keys - expectedTokens
        throw PaletteValidationException("Color token mismatch: missing=$missing extra=$extra")
    }
    val colors = LinkedHashMap<String, String>(colorsObject.size)
    for ((token, value) in colorsObject) colors[token] = colorValue(value, "colors.$token")

    val warning = root["semantics"].objectAt("semantics")["warning"].objectAt("semantics.warning")
    val changes = root["changes_from_latte"].objectAt("changes_from_latte")
    val blue = changes["blue"].objectAt("changes_from_latte.blue")
    val mauve = changes["mauve"].objectAt("changes_from_latte.mauve")
    return Palette(
        name = root["name"].stringAt("name"),
        version = root["version"].stringAt("version"),
        status = root["status"].stringAt("status"),
        colors = Collections.unmodifiableMap(colors),
        warningForeground = warning["foreground"].stringAt("semantics.warning.foreground"),
        warningAccent = warning["accent"].stringAt("semantics.warning.accent"),
        latteBlue = historicalColor(blue["from"], "changes_from_latte.blue.from"),
        latteMauve = historicalColor(mauve["from"], "changes_from_latte.mauve.from"),
    )
}
