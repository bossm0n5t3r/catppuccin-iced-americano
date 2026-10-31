package icedamericano.tooling.validation

data class ContrastPair(val foreground: String, val background: String, val ratio: Double) {
    val passes: Boolean
        get() = ratio >= 4.5
}
