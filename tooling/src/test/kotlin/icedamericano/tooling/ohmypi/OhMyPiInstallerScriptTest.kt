package icedamericano.tooling.ohmypi

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir

class OhMyPiInstallerScriptTest {
    @TempDir lateinit var directory: Path
    private lateinit var script: Path
    private lateinit var source: Path
    private lateinit var home: Path
    private lateinit var bin: Path
    private lateinit var log: Path

    @BeforeEach
    fun prepareInstaller() {
        val port = Files.createDirectories(directory.resolve("port"))
        script = port.resolve("install.sh")
        checkNotNull(javaClass.getResourceAsStream("/oh-my-pi/install.sh")).use {
            Files.copy(it, script)
        }
        source = Files.writeString(port.resolve("iced-americano.json"), "{}\n")
        home = Files.createDirectories(directory.resolve("home space"))
        bin = Files.createDirectories(directory.resolve("bin"))
        log = directory.resolve("omp.log")
        // A stand-in omp that records the agent directory and arguments it receives.
        val omp =
            Files.writeString(
                bin.resolve("omp"),
                "#!/bin/sh\nprintf '%s|%s\\n' \"\$PI_CODING_AGENT_DIR\" \"\$*\" >> \"\$OMP_LOG\"\n",
            )
        Files.setPosixFilePermissions(omp, PosixFilePermissions.fromString("rwxr-xr-x"))
    }

    private fun run(
        vararg args: String,
        agentDir: String? = null,
        path: String = "$bin:/usr/bin:/bin",
    ): Int {
        val process =
            ProcessBuilder(listOf("/bin/bash", script.toString()) + args)
                .directory(directory.toFile())
                .redirectErrorStream(true)
        val environment = process.environment()
        environment["HOME"] = home.toString()
        environment["PATH"] = path
        environment["OMP_LOG"] = log.toString()
        environment.remove("PI_CODING_AGENT_DIR")
        if (agentDir != null) environment["PI_CODING_AGENT_DIR"] = agentDir
        val child = process.start()
        val output = child.inputStream.bufferedReader().use { it.readText() }
        val code = child.waitFor()
        if (code != 0) System.err.print(output)
        return code
    }

    private fun calls(): List<String> =
        if (Files.exists(log)) Files.readAllLines(log) else emptyList()

    private fun defaultAgent(): Path = home.resolve(".omp/agent")

    private fun target(agent: Path = defaultAgent()): Path =
        agent.resolve("themes/iced-americano.json")

    @Test
    fun linksThemeAndSetsLightThemeInDefaultAgentDirectory() {
        assertEquals(0, run())
        assertTrue(Files.isSymbolicLink(target()))
        assertTrue(Files.isSameFile(source, target()))
        assertEquals(listOf("${defaultAgent()}|config set theme.light iced-americano"), calls())
        assertEquals(0, run())
        assertTrue(Files.isSameFile(source, target()))
    }

    @Test
    fun darkAlsoSetsDarkTheme() {
        assertEquals(0, run("--dark"))
        assertEquals(
            listOf(
                "${defaultAgent()}|config set theme.light iced-americano",
                "${defaultAgent()}|config set theme.dark iced-americano",
            ),
            calls(),
        )
    }

    @Test
    fun agentDirectoryComesFromOptionThenEnvironmentAndExpandsHome() {
        val fromEnvironment = home.resolve("env agent")
        assertEquals(0, run(agentDir = fromEnvironment.toString()))
        assertTrue(Files.isSameFile(source, target(fromEnvironment)))
        val fromOption = home.resolve("option agent")
        assertEquals(0, run("--agent-dir=~/option agent", agentDir = fromEnvironment.toString()))
        assertTrue(Files.isSameFile(source, target(fromOption)))
        assertEquals(
            listOf(
                "$fromEnvironment|config set theme.light iced-americano",
                "$fromOption|config set theme.light iced-americano",
            ),
            calls(),
        )
    }

    @Test
    fun replacesRegularAndBrokenThemeLinks() {
        Files.createDirectories(target().parent)
        Files.writeString(target(), "old theme")
        assertEquals(0, run())
        assertTrue(Files.isSymbolicLink(target()))
        Files.delete(target())
        Files.createSymbolicLink(target(), directory.resolve("missing"))
        assertEquals(0, run())
        assertTrue(Files.isSameFile(source, target()))
    }

    @Test
    fun refusesInvalidSetupsBeforeChangingFiles() {
        assertNotEquals(0, run(path = "/usr/bin:/bin"))
        assertFalse(Files.exists(defaultAgent()))
        for (args in
            listOf(
                arrayOf("--agent-dir"),
                arrayOf("--agent-dir", ""),
                arrayOf("--agent-dir="),
                arrayOf("--unknown"),
            )) {
            assertNotEquals(0, run(*args))
            assertFalse(Files.exists(defaultAgent()))
        }
        Files.createDirectories(target())
        assertNotEquals(0, run())
        assertTrue(Files.isDirectory(target()))
        Files.delete(target())
        // A themes directory that resolves to the port must not replace the source.
        val aliased = directory.resolve("aliased")
        Files.createDirectories(aliased)
        Files.createSymbolicLink(aliased.resolve("themes"), source.parent)
        assertNotEquals(0, run("--agent-dir", aliased.toString()))
        assertEquals("{}\n", Files.readString(source))
        Files.delete(source)
        assertNotEquals(0, run())
        assertFalse(Files.exists(target()))
        assertEquals(emptyList(), calls())
    }
}
