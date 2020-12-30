package tl.jake.ktree.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tl.jake.ktree.*
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals

enum class Color(val rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF)
}

@Serializable
sealed class Animal {
    @Serializable
    data class Bird(val wingspan: Int) : Animal()

    @Serializable
    data class Mammal(val furLength: Int) : Animal()

    @Serializable
    @SerialName("Lazard")
    data class Lizard(val lazyness: Int) : Animal()
}

@Serializable
data class ContainsPolymorph(
    val poly: List<Animal>
)

@Serializable
data class Frob(
    val thingy: Bob,
    val cats: List<Bob>,
    val friend: Bob?,
    val color: Color,
)

@Serializable
data class Bob(val name: String)

@Serializable
sealed class TestStatement

@Serializable
@SerialName("test")
data class IntegrationTest(
    @Inline val name: String,
    val input: TestData,
    val output: TestData,
) : TestStatement()

@ExperimentalSerializationApi
@Serializable
data class TestData(
    @Inline val type: Type,
    @Anonymous val content: String
) {
    @Serializable
    enum class Type {
        @SerialName("json")
        Json(),

        @SerialName("tree notation")
        TreeNotation(),
    }
}

@ExperimentalSerializationApi
class KtreeCodingTests {
    /* Helpers */
    private fun stringify(node: Tree.Node): String = TreeNotation.Spaces.format(node)

    private inline fun <reified T> T.assertEncodesTo(expected: Tree.Node) {
        val tree = encodeToTree<T>(this)
        assertEquals(stringify(expected), stringify(tree))
        assertTreesHaveSameContent(expected, tree)
        println(stringify(tree))
    }

    private inline fun <reified T> Tree.assertDecodesTo(expected: T) {
        val value: T = when (this) {
            is Tree.Root -> decodeFromTree(this)
            is Tree.Node -> decodeFromTree(this)
        }
        if (expected is List<*> && value is List<*>) {
            expected.forEachIndexed { index, expectedChild ->
                val child = value[index]
                val expectedClass =
                    if (expectedChild != null) expectedChild::class.toString() else "null"
                val childClass = if (child != null) child::class.toString() else "null"
                assertEquals(expectedChild, child, "$index in $value")
            }
        }
        assertEquals(expected, value)
        assertEquals(expected.prettyToString(), value.prettyToString())
        println(value.prettyToString())
    }

    /* Cases */

    private fun nativeExample() = Frob(
        Bob("Jesse"),
        listOf(Bob("Holly"), Bob("Folly")),
        null,
        Color.GREEN,
    )

    private fun treeExample() = NodeBuilder.build {
        node("thingy") {
            node("name", "Jesse")
        }
        node("cats") {
            node("-") {
                node("name", "Holly")
            }
            node("-") {
                node("name", "Folly")
            }
        }
        node("friend", "null")
        node("color", "GREEN")
    }

    @Test
    fun `test encode`() = nativeExample().assertEncodesTo(treeExample())
    @Test
    fun `test decode`() = treeExample().assertDecodesTo(nativeExample())

    private fun mapNative() = mapOf<String, Bob>(
        "omlette" to Bob("comlette"),
        "cheese" to Bob("velveeta")
    )

    private fun mapTree() = NodeBuilder.build {
        node("omlette") {
            node("name", "comlette")
        }
        node("cheese") {
            node("name", "velveeta")
        }
    }

    @Test
    fun `encode map`() = mapNative().assertEncodesTo(mapTree())
    @Test
    fun `decode map`() = mapTree().assertDecodesTo(mapNative())

    private fun objMapNative() = mapOf<Bob, Bob?>(
        Bob("key foo") to Bob("value foo"),
        Bob("key bar") to Bob("value bar"),
        Bob("key null") to null
    )

    private fun objMapTree() = NodeBuilder.build {
        node("-") {
            node("key") {
                node("name", "key foo")
            }
            node("value") {
                node("name", "value foo")
            }
        }

        node("-") {
            node("key") {
                node("name", "key bar")
            }
            node("value") {
                node("name", "value bar")
            }
        }

        node("-") {
            node("key") {
                node("name", "key null")
            }
            node("value", "null")
        }
    }

    @Test
    fun `encode object map`() = objMapNative().assertEncodesTo(objMapTree())
    @Test
    fun `decode object map`() = objMapTree().assertDecodesTo(objMapNative())

    private fun polyNative() = ContainsPolymorph(
        listOf(
            Animal.Bird(3),
            Animal.Lizard(0),
            Animal.Mammal(1),
        )
    )

    private fun polyTree() = NodeBuilder.build {
        node("poly") {
            node("-", "tl.jake.ktree.serialization.Animal.Bird") {
                node("wingspan", 3.toString())
            }
            node("-", "Lazard") {
                node("lazyness", 0.toString())
            }
            node("-", "tl.jake.ktree.serialization.Animal.Mammal") {
                node("furLength", 1.toString())
            }
        }
    }

    @Test
    fun `encode poly`() = polyNative().assertEncodesTo(polyTree())
    @Test
    fun `decode poly`() = polyTree().assertDecodesTo(polyNative())

    private fun annotationsNative(): TestStatement = IntegrationTest(
        "basic json output",
        TestData(
            TestData.Type.TreeNotation, """
            parent cell1 cell2
             child ccell1 ccell2
        """.trimIndent()
        ),
        TestData(
            TestData.Type.Json, """
            {"cells": ["cell1","cell2"], "children": [{ cells: ["ccell1", "ccell2"]}]}
        """.trimIndent()
        )
    )

    // Inspiration for multi-line string marker | is from YAML, read more: https://yaml-multiline.info/
    // In Ktree, this means "all child blocks shall be treated as a single string".
    // Cells text is joined using the default cell break symbol "\t", and node lines are
    // joined by the default node break symbol "\n".
    private fun annotationsTree() = """
        test${"\t"}basic json output
        ${"\t"}input${"\t"}tree notation
        ${"\t\t"}parent cell1 cell2\n child ccell1 ccell2
        ${"\t"}output${"\t"}json
        ${"\t\t"}{\"cells\": [\"cell1\",\"cell2\"], \"children\": [{ cells: [\"ccell1\", \"ccell2\"]}]}
    """.trimIndent().let { TreeNotation().parse(it).children[0] }

    @Test
    fun `encode annotations`() = annotationsNative().assertEncodesTo(annotationsTree())
    @Test
    fun `decode annotations`() = annotationsTree().assertDecodesTo(annotationsNative())

    private fun simpleListNative() = listOf("foo", "bar", "baz", null, "null")
    private fun simpleListTree() = NodeBuilder.build {
        node("-", "foo")
        node("-", "bar")
        node("-", "baz")
        node("-", "null")
        node("-", "\\null")
    }

    @Test
    fun `encode simple list`() = simpleListNative().assertEncodesTo(simpleListTree())
    @Test
    fun `decode simple list`() = simpleListTree().assertDecodesTo(simpleListNative())
}