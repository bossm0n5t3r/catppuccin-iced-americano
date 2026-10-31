package icedamericano.tooling.cli

import icedamericano.tooling.ghostty.GhosttyCheckResult
import icedamericano.tooling.ghostty.checkGhostty
import icedamericano.tooling.ghostty.generateGhostty
import java.io.PrintWriter
import java.nio.file.Path

internal fun runGhosttyGeneration(args: Array<String>, error: PrintWriter): Int {
    var palette = Path.of("palette/iced-americano.json")
    var output = Path.of("ports/ghostty/iced-americano")
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
            "--output" -> {
                val value =
                    if ('=' in argument) argument.substringAfter('=')
                    else {
                        index++
                        require(index < args.size && !args[index].startsWith("--")) {
                            "$option requires a path"
                        }
                        args[index]
                    }
                val path = Path.of(value)
                if (option == "--palette") palette = path else output = path
            }
            else -> throw IllegalArgumentException("Unknown generation option: $argument")
        }
        index++
    }
    if (!check) {
        generateGhostty(palette, output)
        return 0
    }
    return when (checkGhostty(palette, output)) {
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
