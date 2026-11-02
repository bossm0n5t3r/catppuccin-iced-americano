package icedamericano.tooling.site

import icedamericano.tooling.palette.Palette
import icedamericano.tooling.palette.readPalette
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

private fun String.html(): String =
    buildString(length) {
        for (character in this@html) {
            when (character) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#x27;")
                else -> append(character)
            }
        }
    }

internal fun renderSite(palette: Palette): String {
    val variables = StringBuilder()
    val cards = StringBuilder()
    val rows = StringBuilder()
    for ((token, value) in palette.colors) {
        val label = token.replaceFirstChar { it.uppercaseChar() }
        val change = palette.changesFromLatte[token]
        val original = change?.from ?: value
        val reason = change?.reason?.html() ?: "Unchanged from Catppuccin Latte."
        variables.append("      --$token: $value;\n")
        cards.append(
            "        <li class=\"color-card\"><div class=\"swatch\" style=\"background: var(--$token)\" aria-hidden=\"true\"></div>"
        )
        cards.append("<div class=\"color-details\"><h3>$label</h3><code>$value</code>")
        cards.append(
            "<span class=\"tag\">${if (change != null) "Modified" else "Unchanged"}</span></div></li>\n"
        )
        rows.append("          <tr><th scope=\"row\">$label</th>")
        rows.append(
            "<td><span class=\"mini-swatch\" style=\"background: $original\" aria-hidden=\"true\"></span><code>$original</code></td>"
        )
        rows.append(
            "<td><span class=\"mini-swatch\" style=\"background: var(--$token)\" aria-hidden=\"true\"></span><code>$value</code></td>"
        )
        rows.append("<td>$reason</td></tr>\n")
    }
    val replacements =
        mapOf(
            "variables" to variables.toString().removeSuffix("\n"),
            "cards" to cards.toString().removeSuffix("\n"),
            "rows" to rows.toString().removeSuffix("\n"),
            "version" to palette.version.html(),
            "status" to palette.status.html(),
            "color-count" to palette.colors.size.toString(),
            "change-count" to palette.changesFromLatte.size.toString(),
        )
    val template =
        checkNotNull(Palette::class.java.getResourceAsStream("/site.html")) {
                "Missing site template"
            }
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    return Regex("@@([a-z-]+)@@").replace(template) { match ->
        replacements.getValue(match.groupValues[1])
    }
}

internal fun generateSite(palettePath: Path, outputPath: Path, check: Boolean): Boolean {
    require(
        palettePath.toAbsolutePath().normalize() != outputPath.toAbsolutePath().normalize() &&
            (!Files.exists(outputPath) || !Files.isSameFile(palettePath, outputPath))
    ) {
        "Palette source and generated page must be different files"
    }
    val expected = renderSite(readPalette(palettePath))
    if (check) return Files.exists(outputPath) && Files.readString(outputPath) == expected
    outputPath.toAbsolutePath().parent?.let { Files.createDirectories(it) }
    Files.writeString(outputPath, expected)
    return true
}

fun main(args: Array<String>) {
    try {
        require(args.size in 2..3 && (args.size == 2 || args[2] == "--check")) {
            "Usage: SiteGenerator PALETTE OUTPUT [--check]"
        }
        if (!generateSite(Path.of(args[0]), Path.of(args[1]), args.size == 3)) {
            System.err.println(
                "Palette site is missing or stale; run ./gradlew :tooling:generateSite"
            )
            exitProcess(1)
        }
        println(if (args.size == 3) "Palette site is up to date." else "Generated ${args[1]}")
    } catch (exception: IllegalArgumentException) {
        System.err.println("Invalid input: ${exception.message}")
        exitProcess(2)
    } catch (exception: java.io.IOException) {
        System.err.println("I/O error: ${exception.message}")
        exitProcess(2)
    }
}
