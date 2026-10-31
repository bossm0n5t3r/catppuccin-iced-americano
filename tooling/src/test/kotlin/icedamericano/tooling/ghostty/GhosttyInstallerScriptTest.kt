package icedamericano.tooling.ghostty

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir

class GhosttyInstallerScriptTest {
    @TempDir lateinit var directory: Path
    private lateinit var script: Path
    private lateinit var source: Path
    private lateinit var home: Path
    private lateinit var xdg: Path

    @BeforeEach
    fun prepareInstaller() {
        val port = Files.createDirectories(directory.resolve("port"))
        script = port.resolve("install.sh")
        checkNotNull(javaClass.getResourceAsStream("/install.sh")).use { Files.copy(it, script) }
        source = Files.writeString(port.resolve("iced-americano"), "background = #EFF1F5\n")
        home = Files.createDirectories(directory.resolve("home space"))
        xdg = home.resolve("xdg")
    }

    private fun run(vararg args: String, xdgValue: String = xdg.toString()): Int {
        val process =
            ProcessBuilder(listOf("/bin/bash", script.toString()) + args)
                .directory(directory.toFile())
                .redirectErrorStream(true)
        process.environment()["HOME"] = home.toString()
        process.environment()["XDG_CONFIG_HOME"] = xdgValue
        process.environment()["PATH"] = "/usr/bin:/bin"
        val child = process.start()
        val output = child.inputStream.bufferedReader().use { it.readText() }
        val code = child.waitFor()
        if (code != 0) System.err.print(output)
        return code
    }

    private fun write(path: Path, contents: String): Path {
        Files.createDirectories(path.parent)
        return Files.writeString(path, contents)
    }

    private fun target(): Path = xdg.resolve("ghostty/themes/iced-americano")

    @Test
    fun installsFromAnyWorkingDirectoryAndReinstallPreservesOtherSettings() {
        val config =
            write(
                xdg.resolve("ghostty/config.ghostty"),
                "# theme = ignored\nfont-family = mono\n theme=old\ntheme = duplicate\nlast = value",
            )
        assertEquals(0, run())
        val expected = "# theme = ignored\nfont-family = mono\ntheme = iced-americano\nlast = value"
        assertEquals(expected, Files.readString(config))
        assertTrue(Files.isSymbolicLink(target()))
        assertTrue(Files.isSameFile(source, target()))
        assertEquals(0, run())
        assertEquals(expected, Files.readString(config))
    }

    @Test
    fun handlesEmptyUnterminatedAndMixedLineEndings() {
        val config = xdg.resolve("ghostty/config.ghostty")
        for ((before, expected) in
            listOf(
                "" to "theme = iced-americano\n",
                "font = mono" to "font = mono\ntheme = iced-americano\n",
                "font = mono\r\n theme=old\r\n" to "font = mono\ntheme = iced-americano\n",
                "font = mono\rtheme=old\r" to "font = mono\ntheme = iced-americano\n",
                "font = mono\r\ntheme = iced-americano\r\n" to
                    "font = mono\r\ntheme = iced-americano\r\n",
            )) {
            write(config, before)
            assertEquals(0, run())
            assertEquals(expected, Files.readString(config))
        }
    }

    @Test
    fun discoversConfigWithXdgBeforeMacOsAndFallsBackToHome() {
        val mac =
            write(
                home.resolve("Library/Application Support/com.mitchellh.ghostty/config.ghostty"),
                "mac = yes\n",
            )
        assertEquals(0, run())
        val expected =
            if (System.getProperty("os.name").startsWith("Mac")) mac
            else xdg.resolve("ghostty/config.ghostty")
        assertTrue(Files.readString(expected).contains("theme = iced-americano\n"))
        val legacy = write(xdg.resolve("ghostty/config"), "legacy = yes\n")
        Files.deleteIfExists(xdg.resolve("ghostty/config.ghostty"))
        assertEquals(0, run())
        assertEquals("legacy = yes\ntheme = iced-americano\n", Files.readString(legacy))
        val primary = write(xdg.resolve("ghostty/config.ghostty"), "primary = yes\n")
        assertEquals(0, run())
        assertEquals("primary = yes\ntheme = iced-americano\n", Files.readString(primary))
        Files.delete(mac)
        assertEquals(0, run(xdgValue = ""))
        assertTrue(Files.isSymbolicLink(home.resolve(".config/ghostty/themes/iced-americano")))
    }

    @Test
    fun explicitConfigExpandsHomeAndPreservesConfigSymlink() {
        val realConfig = write(home.resolve("dotfiles/config"), "font = mono\n")
        val configLink = home.resolve("linked-config")
        Files.createSymbolicLink(configLink, realConfig)
        assertEquals(0, run("--config", "~/linked-config", xdgValue = "~/custom"))
        assertTrue(Files.isSymbolicLink(configLink))
        assertEquals("font = mono\ntheme = iced-americano\n", Files.readString(realConfig))
        assertTrue(Files.isSymbolicLink(home.resolve("custom/ghostty/themes/iced-americano")))
    }

    @Test
    fun replacesRegularAndBrokenThemeLinks() {
        write(target(), "old theme")
        assertEquals(0, run())
        assertTrue(Files.isSymbolicLink(target()))
        Files.delete(target())
        Files.createSymbolicLink(target(), directory.resolve("missing"))
        assertEquals(0, run())
        assertTrue(Files.isSameFile(source, target()))
    }

    @Test
    fun rejectsConfigAliasesBeforeChangingFiles() {
        val hardlink = directory.resolve("hardlink")
        Files.createLink(hardlink, source)
        val sourceLink = directory.resolve("source-link")
        Files.createSymbolicLink(sourceLink, source)
        val missingTargetLink = directory.resolve("target-link")
        Files.createSymbolicLink(missingTargetLink, target())
        for (config in listOf(source, sourceLink, hardlink, target(), missingTargetLink)) {
            assertNotEquals(0, run("--config", config.toString()))
            assertEquals("background = #EFF1F5\n", Files.readString(source))
            assertFalse(Files.exists(target()))
        }
    }

    @Test
    fun refusesDirectoriesAndMissingSourceWithoutChangingConfig() {
        val config = write(xdg.resolve("ghostty/config.ghostty"), "font = mono\n")
        Files.createDirectories(target())
        assertNotEquals(0, run())
        assertTrue(Files.isDirectory(target()))
        assertEquals("font = mono\n", Files.readString(config))
        Files.delete(target())
        Files.delete(source)
        assertNotEquals(0, run())
        assertFalse(Files.exists(target()))
        assertEquals("font = mono\n", Files.readString(config))
    }
}
