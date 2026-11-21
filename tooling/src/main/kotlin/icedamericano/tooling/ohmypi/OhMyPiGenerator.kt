package icedamericano.tooling.ohmypi

import icedamericano.tooling.ghostty.ansiPalette
import icedamericano.tooling.ghostty.ansiPaletteTokens
import icedamericano.tooling.jetbrains.Darken
import icedamericano.tooling.jetbrains.Derivation
import icedamericano.tooling.jetbrains.Mix
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

const val OH_MY_PI_THEME_NAME = "iced-americano"

// Every palette token, in Catppuccin order, becomes a theme variable.
private val varTokens =
    listOf(
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

// Port-specific colors derived from palette tokens, emitted as extra theme
// variables in order, so a derivation can use earlier ones. Backgrounds tint
// Base with Blue and Red like the JetBrains selection and banner colors.
// Chromatic tokens that fall below 4.5:1 keep their OKLCH hue and darken until
// they reach 4.5:1 on Crust, the darkest surface they appear on (the status
// line), so they also pass on Base and Mantle. Blue, the accent, darkens to
// 4.5:1 on the Blue-tinted selection instead. Types match the JetBrains port.
internal val derivedVars: Map<String, Derivation> =
    linkedMapOf(
        "selection" to Mix("blue", "base", 0.15),
        "toolPending" to Mix("blue", "base", 0.08),
        "toolError" to Mix("red", "base", 0.08),
        "blueInk" to Darken("blue", "selection", 4.5),
        "sapphireInk" to Darken("sapphire", "crust", 4.5),
        "skyInk" to Darken("sky", "crust", 4.5),
        "pinkInk" to Darken("pink", "crust", 4.5),
        "peachInk" to Darken("peach", "crust", 4.5),
        "yellowInk" to Darken("yellow", "crust", 4.5),
        "typeName" to Darken("yellow", "base", 4.8),
    )

// Blue-black mapping: Text for content, Subtext for secondary text, Blue as the
// accent (as in the JetBrains port), Mauve for keywords and labels, and the
// dark-roast Rosewater for inline code. Syntax roles follow the JetBrains
// editor scheme.
private val colorRoles =
    listOf(
        "accent" to "blueInk",
        "border" to "surface2",
        "borderAccent" to "blueInk",
        "borderMuted" to "surface1",
        "success" to "green",
        "error" to "red",
        "warning" to "yellowInk",
        "muted" to "subtext0",
        "dim" to "overlay1",
        "text" to "text",
        "thinkingText" to "subtext0",
        "selectedBg" to "selection",
        "userMessageBg" to "mantle",
        "userMessageText" to "text",
        "customMessageBg" to "surface0",
        "customMessageText" to "text",
        "customMessageLabel" to "mauve",
        "toolPendingBg" to "toolPending",
        "toolSuccessBg" to "mantle",
        "toolErrorBg" to "toolError",
        "toolTitle" to "blueInk",
        "toolOutput" to "subtext1",
        "mdHeading" to "blueInk",
        "mdLink" to "blueInk",
        "mdLinkUrl" to "subtext0",
        "mdCode" to "rosewater",
        "mdCodeBlock" to "text",
        "mdCodeBlockBorder" to "surface2",
        "mdQuote" to "subtext0",
        "mdQuoteBorder" to "surface2",
        "mdHr" to "surface2",
        "mdListBullet" to "blueInk",
        "toolDiffAdded" to "green",
        "toolDiffRemoved" to "red",
        "toolDiffContext" to "subtext1",
        "link" to "blueInk",
        "syntaxComment" to "subtext0",
        "syntaxKeyword" to "mauve",
        "syntaxFunction" to "blueInk",
        "syntaxVariable" to "text",
        "syntaxString" to "green",
        "syntaxNumber" to "blueInk",
        "syntaxType" to "typeName",
        "syntaxOperator" to "skyInk",
        "syntaxPunctuation" to "overlay2",
        "thinkingOff" to "surface1",
        "thinkingMinimal" to "overlay1",
        "thinkingLow" to "sapphireInk",
        "thinkingMedium" to "blueInk",
        "thinkingHigh" to "mauve",
        "thinkingXhigh" to "pinkInk",
        "bashMode" to "green",
        "statusLineBg" to "crust",
        "statusLineSep" to "surface2",
        "statusLineModel" to "blueInk",
        "statusLinePath" to "text",
        "statusLineGitClean" to "green",
        "statusLineGitDirty" to "yellowInk",
        "statusLineContext" to "subtext1",
        "statusLineSpend" to "subtext1",
        "statusLineStaged" to "green",
        "statusLineDirty" to "peachInk",
        "statusLineUntracked" to "skyInk",
        "statusLineOutput" to "mauve",
        "statusLineCost" to "red",
        "statusLineSubagents" to "mauve",
        "pythonMode" to "yellowInk",
        "btwMode" to "mauve",
    )
private val exportRoles = listOf("pageBg" to "base", "cardBg" to "mantle", "infoBg" to "surface0")
private val terminalRoles =
    listOf(
        "background" to "base",
        "foreground" to "text",
        "chrome" to "mantle",
        "widget" to "mantle",
    )
private val hexPattern = Regex("#[0-9a-fA-F]{6}")
private val requiredTokens = (varTokens + ansiPaletteTokens).distinct()

@OptIn(ExperimentalSerializationApi::class)
private val prettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

// Resolves derivedVars in order; each result is available to later derivations.
internal fun resolveDerivedVars(colors: Map<String, String>): Map<String, String> {
    val known = colors.toMutableMap()
    return derivedVars.mapValues { (name, derivation) ->
        derivation.resolve(known).uppercase().also { known[name] = it }
    }
}

private fun roles(roles: List<Pair<String, String>>): JsonObject =
    JsonObject(roles.associate { (key, token) -> key to JsonPrimitive(token) })

internal fun renderOhMyPiTheme(document: JsonElement): String {
    val palette =
        document as? JsonObject ?: throw IllegalArgumentException("palette must be an object")
    val colors =
        palette["colors"] as? JsonObject
            ?: throw IllegalArgumentException("palette.colors must be an object")
    val validated = requiredTokens.associateWith { token ->
        val field = "palette.colors.$token"
        val element = colors[token] ?: throw IllegalArgumentException("$field is required")
        val value = element as? JsonPrimitive
        require(value != null && value.isString) { "$field must be a #RRGGBB string" }
        require(hexPattern.matches(value.content)) { "$field must be #RRGGBB" }
        value.content.uppercase()
    }
    // ANSI colors reuse the Ghostty contrast mixes so both terminal ports match.
    val ansi = JsonArray(ansiPalette(validated).map(::JsonPrimitive))
    val theme =
        JsonObject(
            mapOf(
                "name" to JsonPrimitive(OH_MY_PI_THEME_NAME),
                "vars" to
                    JsonObject(
                        varTokens.associateWith { JsonPrimitive(validated[it]) } +
                            resolveDerivedVars(validated).mapValues { JsonPrimitive(it.value) }
                    ),
                "colors" to roles(colorRoles),
                "export" to roles(exportRoles),
                "terminal" to JsonObject(roles(terminalRoles) + ("ansi" to ansi)),
            )
        )
    return prettyJson.encodeToString(JsonObject.serializer(), theme) + "\n"
}

private fun expectedTheme(palettePath: Path, outputPath: Path): ByteArray {
    val document = Json.parseToJsonElement(Files.readString(palettePath))
    val expected = renderOhMyPiTheme(document).toByteArray(Charsets.UTF_8)
    val sameNormalizedPath =
        palettePath.toAbsolutePath().normalize() == outputPath.toAbsolutePath().normalize()
    val sameExistingFile = Files.exists(outputPath) && Files.isSameFile(palettePath, outputPath)
    require(!sameNormalizedPath && !sameExistingFile) {
        "output must not overwrite input palette: $outputPath"
    }
    if (Files.isDirectory(outputPath)) throw IOException("output is a directory: $outputPath")
    return expected
}

fun generateOhMyPi(palettePath: Path, outputPath: Path) {
    val expected = expectedTheme(palettePath, outputPath)
    outputPath.toAbsolutePath().parent?.let { Files.createDirectories(it) }
    Files.write(outputPath, expected)
}

fun checkOhMyPi(palettePath: Path, outputPath: Path): OhMyPiCheckResult {
    val expected = expectedTheme(palettePath, outputPath)
    val actual =
        try {
            Files.readAllBytes(outputPath)
        } catch (_: NoSuchFileException) {
            return OhMyPiCheckResult.MISSING
        }
    return if (actual.contentEquals(expected)) OhMyPiCheckResult.CURRENT
    else OhMyPiCheckResult.STALE
}

enum class OhMyPiCheckResult {
    CURRENT,
    MISSING,
    STALE,
}
