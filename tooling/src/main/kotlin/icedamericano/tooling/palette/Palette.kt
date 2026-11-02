package icedamericano.tooling.palette

data class Palette(
    val name: String,
    val version: String,
    val status: String,
    val colors: Map<String, String>,
    val warningForeground: String,
    val warningAccent: String,
    val latteBlue: String,
    val latteMauve: String,
    val changesFromLatte: Map<String, LatteChange>,
)

data class LatteChange(val from: String, val reason: String)
