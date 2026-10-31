package icedamericano.tooling.cli

import icedamericano.tooling.palette.PaletteValidationException
import icedamericano.tooling.palette.readPalette
import icedamericano.tooling.validation.validatePalette
import java.io.IOException
import java.io.PrintWriter
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.system.exitProcess

private const val DEFAULT_PALETTE = "palette/iced-americano.json"

fun main(args: Array<String>) {
    exitProcess(runCli(args, PrintWriter(System.out, true), PrintWriter(System.err, true)))
}

internal fun runCli(args: Array<String>, output: PrintWriter, error: PrintWriter): Int {
    if (args.size !in 1..2 || (args[0] != "validate" && args[0] != "cvd")) {
        error.println("Usage: tooling <validate|cvd> [PATH_TO_PALETTE_JSON]")
        return 2
    }
    return try {
        val palette = readPalette(Path.of(args.getOrElse(1) { DEFAULT_PALETTE }))
        when (args[0]) {
            "validate" -> {
                val result = validatePalette(palette)
                output.printValidationReport(palette, result)
                if (result.passes) 0 else 1
            }
            else -> {
                output.printCvdReport(palette)
                0
            }
        }
    } catch (exception: PaletteValidationException) {
        error.println(exception.message)
        1
    } catch (exception: IOException) {
        error.println("Cannot read palette: ${exception.message}")
        2
    } catch (exception: InvalidPathException) {
        error.println("Invalid palette path: ${exception.message}")
        2
    } catch (exception: IllegalArgumentException) {
        error.println("Invalid palette: ${exception.message}")
        2
    } finally {
        output.flush()
        error.flush()
    }
}
