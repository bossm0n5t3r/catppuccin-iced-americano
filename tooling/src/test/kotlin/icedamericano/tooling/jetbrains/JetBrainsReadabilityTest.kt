package icedamericano.tooling.jetbrains

import icedamericano.tooling.color.contrast
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.w3c.dom.Element

class JetBrainsReadabilityTest {
    private fun resource(name: String) =
        checkNotNull(javaClass.getResourceAsStream("/jetbrains/$name"))

    private fun JsonObject.at(vararg path: String): String {
        var value = this.getValue(path.first())
        for (key in path.drop(1)) value = value.jsonObject.getValue(key)
        return value.jsonPrimitive.content
    }

    private fun assertReadable(foreground: String, background: String, role: String) {
        val ratio = contrast(foreground, background)
        assertTrue(ratio >= 4.5, "$role: $foreground on $background has contrast $ratio")
    }

    @Test
    fun buttonAndLinkStatesHaveReadableText() {
        for (name in listOf("IcedAmericano.theme.json", "IcedAmericano-islands.theme.json")) {
            val theme =
                resource(name).bufferedReader().use {
                    Json.parseToJsonElement(it.readText()).jsonObject
                }
            val colors = theme.getValue("colors").jsonObject
            val ui = theme.getValue("ui").jsonObject
            fun color(vararg path: String) = colors.at(ui.at(*path))
            assertReadable(
                color("Label", "selectedForeground"),
                color("*", "selectionBackground"),
                "$name selected label",
            )
            for (state in listOf("off", "on")) {
                assertReadable(
                    color("ToggleButton", "${state}Foreground"),
                    color("ToggleButton", "${state}Background"),
                    "$name toggle $state",
                )
            }
            for (background in listOf("startBackground", "endBackground")) {
                assertReadable(
                    color("Button", "default", "foreground"),
                    color("Button", "default", background),
                    "$name default button $background",
                )
            }
            for ((foreground, background) in
                listOf(
                    "installForeground" to "installBackground",
                    "installForeground" to "installFocusedBackground",
                    "installFillForeground" to "installFillBackground",
                )) {
                assertReadable(
                    color("Plugins", "Button", foreground),
                    color("Plugins", "Button", background),
                    "$name plugin button $foreground/$background",
                )
            }
            for (state in listOf("pressedForeground", "visitedForeground")) {
                for (surface in listOf("base", "mantle", "crust")) {
                    assertReadable(
                        color("Link", state),
                        colors.at(surface),
                        "$name link $state on $surface",
                    )
                }
            }
        }
    }

    private fun Element.options(): List<Element> =
        (0 until childNodes.length)
            .mapNotNull { childNodes.item(it) as? Element }
            .filter { it.tagName == "option" }

    private fun attributes(): List<Element> =
        resource("IcedAmericano.xml").use { stream ->
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
            (document.getElementsByTagName("attributes").item(0) as Element).options()
        }

    private fun Element.values(): Map<String, String> {
        val value = getElementsByTagName("value").item(0) as? Element ?: return emptyMap()
        return value.options().associate { it.getAttribute("name") to it.getAttribute("value") }
    }

    @Test
    fun reviewedSyntaxRolesRemainReadableOnEditorBackground() {
        val attributes = attributes().associate { it.getAttribute("name") to it.values() }
        val background = "#" + attributes.getValue("TEXT").getValue("BACKGROUND")
        for (role in
            listOf(
                "DEFAULT_ATTRIBUTE",
                "DEFAULT_NUMBER",
                "DEFAULT_METADATA",
                "DEFAULT_STATIC_FIELD",
                "DEFAULT_CLASS_NAME",
                "DEFAULT_CLASS_REFERENCE",
                "DEFAULT_INTERFACE_NAME",
            )) {
            assertReadable("#" + attributes.getValue(role).getValue("FOREGROUND"), background, role)
        }
    }

    @Test
    fun optionalSyntaxStylesDoNotIntroduceItalics() {
        for (attribute in attributes()) {
            val name = attribute.getAttribute("name")
            // Explicit document formatting is not an optional syntax style.
            if (name == "REST.ITALIC") continue
            val style = attribute.values()["FONT_TYPE"]?.toInt() ?: 0
            assertEquals(0, style and 2, "$name must not introduce syntax italics")
        }
    }
}
