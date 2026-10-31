package icedamericano.tooling.cli

import icedamericano.tooling.color.Vision
import icedamericano.tooling.color.contrast
import icedamericano.tooling.color.distance
import icedamericano.tooling.palette.Palette
import java.io.PrintWriter
import java.util.Locale

internal fun PrintWriter.printCvdReport(palette: Palette) {
    val colors = palette.colors
    println("${palette.name} ${palette.version} | ${palette.status}")
    println(
        "Euclidean OKLab distances (L 0-1 scale); Machado severity 1, " +
            "linear-sRGB channel clipping."
    )
    println("Model estimates only; no accessibility threshold or certification.")
    println(
        "Fixed current partners: mauve ${colors.getValue("mauve")}, " +
            "green ${colors.getValue("green")}, red ${colors.getValue("red")}."
    )
    printf(
        Locale.ROOT,
        "%-23s %-7s %-7s  %7s  %7s  %7s%n",
        "Pair / variant",
        "first",
        "second",
        "normal",
        "protan",
        "deutan",
    )
    printBluePairs("Latte", palette.latteBlue, colors)
    printBluePairs("previous", "#193B83", colors)
    printBluePairs("current", colors.getValue("blue"), colors)
    printDistancePair(
        "red-green / current",
        colors.getValue("red"),
        colors.getValue("green"),
    )
    println("\nMauve change: fixed current Blue, Green and Red")
    printMauvePairs("Latte", palette.latteMauve, colors)
    printMauvePairs("current", colors.getValue("mauve"), colors)
}

private fun PrintWriter.printBluePairs(
    label: String,
    blue: String,
    colors: Map<String, String>,
) {
    printDistancePair("blue-mauve / $label", blue, colors.getValue("mauve"))
    printDistancePair("blue-green / $label", blue, colors.getValue("green"))
}

private fun PrintWriter.printMauvePairs(
    label: String,
    mauve: String,
    colors: Map<String, String>,
) {
    printDistancePair("mauve-blue / $label", mauve, colors.getValue("blue"))
    printDistancePair("mauve-green / $label", mauve, colors.getValue("green"))
    printDistancePair("mauve-red / $label", mauve, colors.getValue("red"))
    printf(
        Locale.ROOT,
        "Mauve %s WCAG contrast (normal vision): base %.2f:1  mantle %.2f:1  crust %.2f:1%n",
        mauve,
        contrast(mauve, colors.getValue("base")),
        contrast(mauve, colors.getValue("mantle")),
        contrast(mauve, colors.getValue("crust")),
    )
}

private fun PrintWriter.printDistancePair(label: String, first: String, second: String) {
    printf(
        Locale.ROOT,
        "%-23s %s %s  %.5f  %.5f  %.5f%n",
        label,
        first,
        second,
        distance(first, second, Vision.NORMAL),
        distance(first, second, Vision.PROTANOPIA),
        distance(first, second, Vision.DEUTERANOPIA),
    )
}
