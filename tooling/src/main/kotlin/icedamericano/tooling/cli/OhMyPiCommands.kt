package icedamericano.tooling.cli

import icedamericano.tooling.ohmypi.OhMyPiCheckResult
import icedamericano.tooling.ohmypi.checkOhMyPi
import icedamericano.tooling.ohmypi.generateOhMyPi
import java.io.PrintWriter
import java.nio.file.Path

internal fun runOhMyPiGeneration(args: Array<String>, error: PrintWriter): Int {
    var palette = Path.of("palette/iced-americano.json")
    var output = Path.of("ports/oh-my-pi/iced-americano.json")
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
                            "$option requires a value"
                        }
                        args[index]
                    }
                if (option == "--palette") palette = Path.of(value) else output = Path.of(value)
            }
            else -> throw IllegalArgumentException("Unknown generation option: $argument")
        }
        index++
    }
    if (!check) {
        generateOhMyPi(palette, output)
        return 0
    }
    return when (checkOhMyPi(palette, output)) {
        OhMyPiCheckResult.CURRENT -> 0
        OhMyPiCheckResult.MISSING -> {
            error.println("Missing generated theme: $output")
            1
        }
        OhMyPiCheckResult.STALE -> {
            error.println("Stale generated theme: $output")
            1
        }
    }
}
