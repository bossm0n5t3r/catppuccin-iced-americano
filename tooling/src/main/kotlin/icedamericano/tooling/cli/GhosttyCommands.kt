package icedamericano.tooling.cli

import icedamericano.tooling.ghostty.GhosttyCheckResult
import icedamericano.tooling.ghostty.checkGhostty
import icedamericano.tooling.ghostty.generateGhostty
import java.io.PrintWriter
import java.nio.file.Path

internal fun runGhosttyGeneration(args: Array<String>, error: PrintWriter): Int {
    var palette = Path.of("palette/iced-americano.json")
    var output = Path.of("ports/ghostty/iced-americano")
    var release: String? = null
    var check = false
    var index = 1
    while (index < args.size) {
        val argument = args[index]
        when (val option = argument.substringBefore('=')) {
            "--check" -> {
                require(argument == "--check") { "--check does not take a value" }
                check = true
            }
            "--palette",
            "--output",
            "--release" -> {
                val value =
                    if ('=' in argument) argument.substringAfter('=')
                    else {
                        index++
                        require(index < args.size && !args[index].startsWith("--")) {
                            "$option requires a value"
                        }
                        args[index]
                    }
                when (option) {
                    "--palette" -> palette = Path.of(value)
                    "--output" -> output = Path.of(value)
                    else -> release = value
                }
            }
            else -> throw IllegalArgumentException("Unknown generation option: $argument")
        }
        index++
    }
    // The release version lives in gradle.properties; the Gradle tasks pass it in.
    val version = requireNotNull(release) { "--release is required" }
    if (!check) {
        generateGhostty(palette, output, version)
        return 0
    }
    return when (checkGhostty(palette, output, version)) {
        GhosttyCheckResult.CURRENT -> 0
        GhosttyCheckResult.MISSING -> {
            error.println("Missing generated theme: $output")
            1
        }
        GhosttyCheckResult.STALE -> {
            error.println("Stale generated theme: $output")
            1
        }
    }
}
