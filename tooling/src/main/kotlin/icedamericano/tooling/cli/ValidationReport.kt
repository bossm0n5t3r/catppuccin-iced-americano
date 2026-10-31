package icedamericano.tooling.cli

import icedamericano.tooling.palette.Palette
import icedamericano.tooling.validation.ContrastPair
import icedamericano.tooling.validation.ValidationResult
import java.io.PrintWriter
import java.util.Locale

internal fun PrintWriter.printValidationReport(
    palette: Palette,
    result: ValidationResult,
) {
    println("${palette.name} ${palette.version} | Base ${palette.colors.getValue("base")}")
    println("Required pairs: AA normal text, minimum 4.5:1")
    for (pair in result.required) {
        printContrastPair(pair, if (pair.passes) "PASS" else "FAIL")
    }
    println("Additional diagnostics: normal-text 4.5:1 reference, not additional failures")
    for (pair in result.diagnostics) {
        printContrastPair(pair, if (pair.passes) "MEETS" else "BELOW")
    }
    println(
        "Warning: foreground=${palette.warningForeground}, accent=${palette.warningAccent}; " +
            "the accent is not suitable for small normal text."
    )
    println("Reminder: contrast tests do not establish color-vision-deficiency accessibility.")
}

private fun PrintWriter.printContrastPair(pair: ContrastPair, state: String) {
    printf(
        Locale.ROOT,
        "%-12s on %-8s %5.2f:1  %s%n",
        pair.foreground,
        pair.background,
        pair.ratio,
        state,
    )
}
