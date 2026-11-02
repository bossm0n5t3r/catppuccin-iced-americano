package icedamericano.tooling.site

private val markerPattern =
    Regex(
        "(?m)^[ \\t]*(<!--|/\\*) palette:([a-z-]+):(start depth=([0-9]+)|end) (-->|\\*/)[ \\t]*(?:\\r?\\n|$)"
    )
private val markerPrefix = Regex("(?:<!--|/\\*)\\s*palette:")

private data class Region(val name: String, val from: Int, val until: Int, val depth: Int)

internal fun updateSiteRegions(html: String, content: Map<String, String>): String {
    val regions = mutableListOf<Region>()
    val seen = mutableSetOf<String>()
    var start: MatchResult? = null
    var markerCount = 0
    for (marker in markerPattern.findAll(html)) {
        markerCount++
        val name = marker.groupValues[2]
        require(name in content) { "Unknown palette region: $name" }
        val css = name == "colors"
        require(
            marker.groupValues[1] == (if (css) "/*" else "<!--") &&
                marker.groupValues[5] == (if (css) "*/" else "-->")
        ) {
            "Invalid comment syntax for palette region: $name"
        }
        if (marker.groupValues[3] != "end") {
            require(start == null && seen.add(name)) { "Nested or duplicate palette region: $name" }
            val depth = marker.groupValues[4].toIntOrNull()
            require(depth != null && depth in 0..32) {
                "Invalid palette depth: $name (expected 0..32)"
            }
            start = marker
        } else {
            val opening = start
            require(opening != null && opening.groupValues[2] == name) {
                "Unmatched palette region end: $name"
            }
            regions.add(
                Region(
                    name,
                    opening.range.last + 1,
                    marker.range.first,
                    opening.groupValues[4].toInt(),
                )
            )
            start = null
        }
    }
    require(
        start == null && seen == content.keys && markerCount == markerPrefix.findAll(html).count()
    ) {
        "Missing or malformed palette markers; required regions: ${content.keys}"
    }
    val newline = if ("\r\n" in html) "\r\n" else "\n"
    return buildString(html.length) {
        var offset = 0
        for (region in regions) {
            append(html, offset, region.from)
            val indent = "    ".repeat(region.depth)
            for (line in content.getValue(region.name).lineSequence()) {
                append(indent).append(line).append(newline)
            }
            offset = region.until
        }
        append(html, offset, html.length)
    }
}
