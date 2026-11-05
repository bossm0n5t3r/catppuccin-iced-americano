package icedamericano.tooling.cli

import icedamericano.tooling.jetbrains.checkJetBrains
import icedamericano.tooling.palette.readPalette
import java.io.PrintWriter
import java.nio.file.Path

internal fun runJetBrainsCheck(args: Array<String>, error: PrintWriter): Int {
    var palette = Path.of("palette/iced-americano.json")
    var themes = Path.of("ports/jetbrains/src/main/resources/themes")
    var index = 1
    while (index < args.size) {
        val argument = args[index]
        when (val option = argument.substringBefore('=')) {
            "--palette",
            "--themes" -> {
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
                if (option == "--palette") palette = path else themes = path
            }
            else -> throw IllegalArgumentException("Unknown JetBrains check option: $argument")
        }
        index++
    }
    val problems = checkJetBrains(readPalette(palette), themes)
    problems.forEach(error::println)
    return if (problems.isEmpty()) 0 else 1
}
