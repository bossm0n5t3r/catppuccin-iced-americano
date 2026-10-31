package icedamericano.tooling.validation

import icedamericano.tooling.color.contrast
import icedamericano.tooling.palette.Palette

private val requiredPairs =
    listOf(
        "text" to "base",
        "subtext1" to "base",
        "subtext0" to "base",
        "blue" to "base",
        "red" to "base",
        "green" to "base",
        "mauve" to "base",
        "text" to "surface1",
        "base" to "blue",
    )
private val diagnosticText =
    listOf(
        "text",
        "subtext1",
        "subtext0",
        "blue",
        "red",
        "green",
        "mauve",
        "yellow",
        "pink",
        "teal",
    )
private val diagnosticBackgrounds = listOf("base", "mantle", "crust")

fun validatePalette(palette: Palette): ValidationResult {
    fun pair(foreground: String, background: String) =
        ContrastPair(
            foreground,
            background,
            contrast(palette.colors.getValue(foreground), palette.colors.getValue(background)),
        )
    val required = requiredPairs.map { (foreground, background) -> pair(foreground, background) }
    val diagnostics = buildList {
        for (foreground in diagnosticText) {
            for (background in diagnosticBackgrounds) {
                if (requiredPairs.none { it.first == foreground && it.second == background }) {
                    add(pair(foreground, background))
                }
            }
        }
    }
    return ValidationResult(required, diagnostics)
}
