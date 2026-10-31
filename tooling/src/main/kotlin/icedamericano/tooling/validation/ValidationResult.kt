package icedamericano.tooling.validation

data class ValidationResult(val required: List<ContrastPair>, val diagnostics: List<ContrastPair>) {
    val passes: Boolean
        get() = required.all { it.passes }
}
