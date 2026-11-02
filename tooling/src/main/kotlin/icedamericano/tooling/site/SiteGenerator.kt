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

private fun siteTemplate(): String =
    checkNotNull(Palette::class.java.getResourceAsStream("/site.html")) { "Missing site template" }
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }

private fun StringBuilder.appendColorCell(background: String, value: String) {
    val cell =
        """<td><span class="mini-swatch" style="background: $background" aria-hidden="true"></span><code>$value</code></td>"""
    append("    ")
    // Match the existing table layout: short cells wrap attributes; longer cells wrap </td>.
    if (cell.length <= 112) {
        val split = cell.indexOf(" aria-hidden")
        append(cell, 0, split).append("\n              ")
        append(cell, split + 1, cell.length).append('\n')
    } else {
        append(cell, 0, cell.length - "</td>".length).append("\n    </td>\n")
    }
}

private fun StringBuilder.appendReasonCell(reason: String) {
    if (reason.length <= 96) {
        append("    <td>").append(reason).append("</td>\n")
        return
    }
    append("    <td>")
    var start = 0
    while (reason.length - start > 96) {
        val split = reason.lastIndexOf(' ', start + 96)
        if (split <= start) break
        append(reason, start, split).append("\n        ")
        start = split + 1
    }
    append(reason, start, reason.length).append("\n    </td>\n")
}

internal fun renderSite(palette: Palette, html: String = siteTemplate()): String {
    val variables = StringBuilder()
    val cards = StringBuilder()
    val rows = StringBuilder()
    for ((token, value) in palette.colors) {
        val label = token.replaceFirstChar { it.uppercaseChar() }
        val change = palette.changesFromLatte[token]
        val original = change?.from ?: value
        val reason = change?.reason?.html() ?: "Unchanged from Catppuccin Latte."
        variables.append("--$token: $value;\n")
        cards
            .append(
                """
            <li class="color-card">
                <div class="swatch" style="background: var(--$token)" aria-hidden="true"></div>
                <div class="color-details">
                    <h3>$label</h3>
                    <code>$value</code>
                    <span class="tag">${if (change != null) "Modified" else "Unchanged"}</span>
                </div>
            </li>
            """
                    .trimIndent()
            )
            .append('\n')
        rows.append("<tr>\n    <th scope=\"row\">$label</th>\n")
        rows.appendColorCell(original, original)
        rows.appendColorCell("var(--$token)", value)
        rows.appendReasonCell(reason)
        rows.append("</tr>\n")
    }
    return updateSiteRegions(
        html,
        mapOf(
            "colors" to variables.toString().removeSuffix("\n"),
            "metadata" to
                """<p class="eyebrow">Unofficial Catppuccin Latte derivative · v${palette.version.html()} · ${palette.status.html()}</p>""",
            "color-count" to
                """
                <p class="section-note">All ${palette.colors.size} colors, directly from the shared palette. Every chip has a token name and
                    selectable HEX value; modified tokens are labeled explicitly.</p>
                """
                    .trimIndent(),
            "cards" to cards.toString().removeSuffix("\n"),
            "change-count" to
                """
                <p class="section-note">${palette.changesFromLatte.size} deliberate changes. Everything else retains the original Catppuccin Latte value.
                    Compare the original and current colors side by side.</p>
                """
                    .trimIndent(),
            "rows" to rows.toString().removeSuffix("\n"),
        ),
    )
}

internal fun generateSite(palettePath: Path, outputPath: Path, check: Boolean): Boolean {
    require(
        palettePath.toAbsolutePath().normalize() != outputPath.toAbsolutePath().normalize() &&
            (!Files.exists(outputPath) || !Files.isSameFile(palettePath, outputPath))
    ) {
        "Palette source and generated page must be different files"
    }
    val palette = readPalette(palettePath)
    val actual =
        if (Files.exists(outputPath)) Files.readString(outputPath)
        else if (check) return false else siteTemplate()
    val expected = renderSite(palette, actual)
    if (check) return actual == expected
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
