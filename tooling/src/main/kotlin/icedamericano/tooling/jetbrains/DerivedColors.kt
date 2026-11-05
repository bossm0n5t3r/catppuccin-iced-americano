package icedamericano.tooling.jetbrains

import kotlin.math.roundToInt

// Colors the JetBrains port computes from palette tokens, keyed by location. Formulas follow the
// Catppuccin for JetBrains Latte templates with Blue as the accent.
internal sealed interface Derivation {
    fun resolve(colors: Map<String, String>): String
}

internal data class Mix(val color: String, val with: String, val amount: Double) : Derivation {
    override fun resolve(colors: Map<String, String>) =
        whiskersMix(colors.getValue(color), colors.getValue(with), amount)

    override fun toString() = "mix($color, $with, $amount)"
}

internal data class Opacity(val color: String, val opacity: Double) : Derivation {
    override fun resolve(colors: Map<String, String>) =
        colors.getValue(color).lowercase() + byteHex(ratioByte(opacity.toFloat()))

    override fun toString() = "$color with opacity $opacity"
}

private fun ratioByte(value: Float): Int = (value.coerceIn(0f, 1f) * 255f).roundToInt()

private fun byteHex(value: Int): String = "%02x".format(value)

// Reproduces Catppuccin Whiskers' `mix` filter (farver 3 RGBA mix for opaque colors): the
// amount is rounded to a whole percent and every ratio is quantized to 1/255, so results can
// differ by one from plain per-channel rounding.
internal fun whiskersMix(first: String, second: String, amount: Double): String {
    val percent = (amount * 100.0).coerceIn(0.0, 100.0).roundToInt()
    val weight = ratioByte(percent / 100f)
    fun scaled(channel: Int, ratio: Int) = ratioByte((channel / 255f) * (ratio / 255f))
    return buildString(7) {
        append('#')
        for (index in 1..5 step 2) {
            val a = first.substring(index, index + 2).toInt(16)
            val b = second.substring(index, index + 2).toInt(16)
            append(byteHex(minOf(255, scaled(a, weight) + scaled(b, 255 - weight))))
        }
    }
}

private fun fileColor(color: String) = Mix(color, "base", 0.15)

internal val uiThemeDerivations: Map<String, Derivation> =
    mapOf(
        "colors.hoverBackground" to Mix("overlay2", "base", 0.3),
        "colors.selectionBackground" to Mix("overlay2", "base", 0.2),
        "colors.searchMatchBackground" to Opacity("blue", 0.3),
        "colors.gitLogBackground" to Opacity("blue", 0.05),
        "colors.dragAndDropBackground" to Opacity("blue", 0.15),
        "colors.tabBackground" to Mix("blue", "base", 0.15),
        "ui.Banner.errorBackground" to Opacity("red", 0.1),
        "ui.Banner.errorBorderColor" to Opacity("red", 0.15),
        "ui.Banner.informativeBackground" to Opacity("blue", 0.1),
        "ui.Banner.informativeBorderColor" to Opacity("blue", 0.1),
        "ui.Banner.warningBackground" to Opacity("peach", 0.1),
        "ui.Banner.warningBorderColor" to Opacity("peach", 0.1),
        "ui.EditorTabs.inactiveColoredFileBackground" to Opacity("base", 0.5),
        "ui.FileColor.Gray" to fileColor("overlay0"),
        "ui.FileColor.Blue" to fileColor("blue"),
        "ui.FileColor.Green" to fileColor("green"),
        "ui.FileColor.Orange" to fileColor("peach"),
        "ui.FileColor.Rose" to fileColor("red"),
        "ui.FileColor.Violet" to fileColor("lavender"),
        "ui.FileColor.Yellow" to fileColor("yellow"),
        "icons.ColorPalette.Checkbox.Background.Selected" to Mix("base", "surface0", 0.2),
    )

private fun selection(color: String, amount: Double = 0.4) = Mix(color, "base", amount)

private fun rainbow(color: String) = Mix("text", color, 0.6)

internal val editorSchemeDerivations: Map<String, Derivation> =
    mapOf(
        "colors.CARET_ROW_COLOR" to Mix("surface1", "base", 0.15),
        "colors.FILESTATUS_UNKNOWN" to Mix("peach", "base", 0.9),
        "colors.FILESTATUS_changelistConflict" to Mix("red", "base", 0.9),
        "colors.INLINE_REFACTORING_SETTINGS_DEFAULT" to Mix("surface1", "base", 0.65),
        "colors.INLINE_REFACTORING_SETTINGS_FOCUSED" to Mix("surface2", "base", 0.55),
        "colors.INLINE_REFACTORING_SETTINGS_HOVERED" to Mix("surface2", "base", 0.55),
        "colors.SELECTION_BACKGROUND" to Mix("surface2", "base", 0.4),
        "colors.VCS_ANNOTATIONS_COLOR_1" to Mix("blue", "base", 0.4),
        "colors.VCS_ANNOTATIONS_COLOR_2" to Mix("blue", "base", 0.32),
        "colors.VCS_ANNOTATIONS_COLOR_3" to Mix("blue", "base", 0.26),
        "colors.VCS_ANNOTATIONS_COLOR_4" to Mix("blue", "base", 0.18),
        "colors.VCS_ANNOTATIONS_COLOR_5" to Mix("blue", "base", 0.1),
        "attributes.BLOCK_TERMINAL_CURRENT_SEARCH_ENTRY.BACKGROUND" to Mix("red", "base", 0.3),
        "attributes.BLOCK_TERMINAL_SEARCH_ENTRY.BACKGROUND" to Mix("blue", "base", 0.3),
        "attributes.CodeWithMe.USER_1_SELECTION.BACKGROUND" to selection("red", 0.2),
        "attributes.CodeWithMe.USER_2_SELECTION.BACKGROUND" to selection("peach"),
        "attributes.CodeWithMe.USER_3_SELECTION.BACKGROUND" to selection("yellow"),
        "attributes.CodeWithMe.USER_4_SELECTION.BACKGROUND" to selection("green"),
        "attributes.CodeWithMe.USER_5_SELECTION.BACKGROUND" to selection("sapphire"),
        "attributes.CodeWithMe.USER_6_SELECTION.BACKGROUND" to selection("mauve"),
        "attributes.DIFF_CONFLICT.BACKGROUND" to Mix("red", "base", 0.25),
        "attributes.DIFF_CONFLICT.ERROR_STRIPE_COLOR" to Mix("red", "base", 0.2),
        "attributes.DIFF_DELETED.BACKGROUND" to Mix("red", "base", 0.25),
        "attributes.DIFF_DELETED.ERROR_STRIPE_COLOR" to Mix("red", "base", 0.2),
        "attributes.DIFF_INSERTED.BACKGROUND" to Mix("green", "base", 0.25),
        "attributes.DIFF_INSERTED.ERROR_STRIPE_COLOR" to Mix("green", "base", 0.2),
        "attributes.DIFF_MODIFIED.BACKGROUND" to Mix("blue", "base", 0.25),
        "attributes.DIFF_MODIFIED.ERROR_STRIPE_COLOR" to Mix("blue", "base", 0.2),
        "attributes.EXECUTIONPOINT_ATTRIBUTES.BACKGROUND" to Mix("yellow", "base", 0.15),
        "attributes.SEARCH_RESULT_ATTRIBUTES.BACKGROUND" to Mix("blue", "base", 0.3),
        "attributes.TAB_SELECTED.BACKGROUND" to Mix("overlay2", "base", 0.2),
        "attributes.TAB_SELECTED_INACTIVE.BACKGROUND" to Mix("overlay2", "base", 0.2),
        "attributes.TEXT_SEARCH_RESULT_ATTRIBUTES.BACKGROUND" to Mix("blue", "base", 0.3),
        "attributes.RAINBOW_COLOR0.FOREGROUND" to rainbow("red"),
        "attributes.RAINBOW_COLOR1.FOREGROUND" to rainbow("yellow"),
        "attributes.RAINBOW_COLOR2.FOREGROUND" to rainbow("green"),
        "attributes.RAINBOW_COLOR3.FOREGROUND" to rainbow("blue"),
        "attributes.RAINBOW_COLOR4.FOREGROUND" to rainbow("mauve"),
    )
