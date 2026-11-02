package icedamericano.tooling.site

import icedamericano.tooling.palette.parsePalette
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir

class SiteGeneratorTest {
    @TempDir lateinit var directory: Path

    private fun fixture(): JsonObject =
        Json.parseToJsonElement(
            checkNotNull(javaClass.getResourceAsStream("/iced-americano.json"))
                .bufferedReader()
                .use { it.readText() }
        ) as JsonObject

    @Test
    fun metadataIsEscapedAndNotReinterpretedAsTemplateSyntax() {
        val root = fixture()
        val changes = root.getValue("changes_from_latte") as JsonObject
        val blue = changes.getValue("blue") as JsonObject
        val reason = "<script>alert(\"blue\")</script> & 'safe' @@status@@"
        val modified =
            JsonObject(
                root +
                    mapOf(
                        "version" to JsonPrimitive("<beta> & \"1\""),
                        "changes_from_latte" to
                            JsonObject(
                                changes +
                                    ("blue" to
                                        JsonObject(blue + ("reason" to JsonPrimitive(reason))))
                            ),
                    )
            )
        val html = renderSite(parsePalette(modified.toString()))
        assertTrue(html.contains("v&lt;beta&gt; &amp; &quot;1&quot;"))
        assertTrue(
            html.contains(
                "&lt;script&gt;alert(&quot;blue&quot;)&lt;/script&gt; &amp; &#x27;safe&#x27; @@status@@"
            )
        )
        assertFalse(html.contains("<script>"))
    }

    @Test
    fun staleAndMissingChecksPreserveOutputUntilExplicitGeneration() {
        val source = directory.resolve("palette.json")
        val output = directory.resolve("nested/index.html")
        val root = fixture()
        Files.writeString(source, root.toString())
        assertFalse(generateSite(source, output, check = true))
        assertFalse(Files.exists(output.parent))
        generateSite(source, output, check = false)
        assertTrue(generateSite(source, output, check = true))
        val previous = Files.readString(output)
        val colors = root.getValue("colors") as JsonObject
        Files.writeString(
            source,
            JsonObject(
                    root + ("colors" to JsonObject(colors + ("blue" to JsonPrimitive("#112233"))))
                )
                .toString(),
        )
        assertFalse(generateSite(source, output, check = true))
        assertEquals(previous, Files.readString(output))
        generateSite(source, output, check = false)
        assertTrue(Files.readString(output).contains("--blue: #112233;"))
        assertTrue(generateSite(source, output, check = true))
    }
}
