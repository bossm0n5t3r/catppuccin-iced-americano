package icedamericano.tooling.jetbrains

import icedamericano.tooling.palette.Palette
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.w3c.dom.Element
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException

internal val uiThemeFiles = listOf("IcedAmericano.theme.json", "IcedAmericano-islands.theme.json")
internal const val EDITOR_SCHEME_FILE = "IcedAmericano.xml"

// Attribute options that hold style flags rather than colors.
private val nonColorOptions = setOf("FONT_TYPE", "EFFECT_TYPE")

/**
 * Checks the hand-maintained JetBrains theme files against the palette without modifying them.
 * Every color must be a palette color or match its registered derivation; returns the problems
 * found, or an empty list when the files are current.
 */
fun checkJetBrains(palette: Palette, themeDirectory: Path): List<String> {
    val colors = palette.colors.mapValues { it.value.lowercase() }
    val problems = mutableListOf<String>()
    for (name in uiThemeFiles) {
        val values = uiThemeColors(Json.parseToJsonElement(read(themeDirectory, name)))
        for ((token, expected) in colors) {
            val actual = values["colors.$token"]
            if (actual != expected)
                problems += "$name: colors.$token is $actual, expected $expected"
        }
        problems += checkColors(name, values, uiThemeDerivations, colors)
    }
    val scheme = editorSchemeColors(read(themeDirectory, EDITOR_SCHEME_FILE))
    problems += checkColors(EDITOR_SCHEME_FILE, scheme, editorSchemeDerivations, colors)
    return problems
}

private fun read(directory: Path, name: String): String = Files.readString(directory.resolve(name))

private fun checkColors(
    file: String,
    values: Map<String, String>,
    derivations: Map<String, Derivation>,
    colors: Map<String, String>,
): List<String> {
    val problems = mutableListOf<String>()
    for ((path, derivation) in derivations) {
        val expected = derivation.resolve(colors)
        when (val actual = values[path]) {
            null -> problems += "$file: missing derived color $path ($derivation)"
            expected -> {}
            else -> problems += "$file: $path is $actual, expected $expected ($derivation)"
        }
    }
    val paletteColors = colors.values.toSet()
    for ((path, actual) in values) {
        if (path !in derivations && actual !in paletteColors) {
            problems +=
                "$file: $path uses $actual, which is not a palette color or registered derivation"
        }
    }
    return problems
}

private fun uiThemeColors(document: JsonElement): Map<String, String> {
    val values = linkedMapOf<String, String>()
    fun walk(element: JsonElement, path: String) {
        when (element) {
            is JsonObject ->
                for ((key, child) in element) walk(child, if (path.isEmpty()) key else "$path.$key")
            is JsonPrimitive ->
                if (element.isString && element.content.startsWith("#")) {
                    values[path] = element.content.lowercase()
                }
            else -> {}
        }
    }
    walk(document, "")
    return values
}

private fun Element.options(): List<Element> =
    (0 until childNodes.length)
        .mapNotNull { childNodes.item(it) as? Element }
        .filter { it.tagName == "option" }

private fun editorSchemeColors(text: String): Map<String, String> {
    val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    // Report parse failures as invalid input instead of the parser's default stderr output.
    builder.setErrorHandler(
        object : ErrorHandler {
            override fun warning(exception: SAXParseException) {}

            override fun error(exception: SAXParseException) = throw exception

            override fun fatalError(exception: SAXParseException) = throw exception
        }
    )
    val document =
        try {
            builder.parse(text.byteInputStream(Charsets.UTF_8))
        } catch (exception: SAXException) {
            throw IllegalArgumentException("$EDITOR_SCHEME_FILE: ${exception.message}", exception)
        }
    fun section(tag: String) =
        requireNotNull(document.getElementsByTagName(tag).item(0) as? Element) {
            "$EDITOR_SCHEME_FILE: missing <$tag> section"
        }
    val values = linkedMapOf<String, String>()
    fun put(path: String, value: String) {
        if (value.isNotEmpty()) values[path] = "#" + value.lowercase()
    }
    for (option in section("colors").options()) {
        put("colors.${option.getAttribute("name")}", option.getAttribute("value"))
    }
    for (attribute in section("attributes").options()) {
        val value = attribute.getElementsByTagName("value").item(0) as? Element ?: continue
        for (option in value.options()) {
            val key = option.getAttribute("name")
            if (key in nonColorOptions) continue
            put("attributes.${attribute.getAttribute("name")}.$key", option.getAttribute("value"))
        }
    }
    return values
}
