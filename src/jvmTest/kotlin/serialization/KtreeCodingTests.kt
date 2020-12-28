package tl.jake.ktree.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import tl.jake.ktree.*
import kotlin.test.Test
import kotlin.test.assertEquals

enum class Color(val rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF)
}

@Serializable
data class Frob(
    val thingy: Bob,
    val cats: List<Bob>,
    val friend: Bob?,
//    val color: Color,
)
@Serializable
data class Bob(val name: String)

@ExperimentalSerializationApi
class KtreeCodingTests {

    fun nativeExample() = Frob(
        Bob("Jesse"),
        listOf(Bob("Holly"), Bob("Folly")),
        null,
//        Color.GREEN,
    )
    fun treeExample() = NodeBuilder.build {
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
//        node("color", "GREEN")
    }

    @Test
    fun `test encode`() {
        val tree = encodeToTree(nativeExample())
        val expected = treeExample()
        assertTreesHaveSameContent(expected, tree)
        assertEquals(stringify(expected), stringify(tree))
        println(stringify(tree))
    }

    @Test
    fun `test encode map`() {
        @Serializable
        data class Cow(val name: String)

        val cowsByName = mapOf<String, Cow>(
            "omlette" to Cow("comlette"),
            "cheese" to Cow("velveeta")
        )

        val tree = encodeToTree(cowsByName)
        val expected = NodeBuilder.build {
            node("omlette") {
                node("name", "comlette")
            }
            node("cheese") {
                node("name", "velveeta")
            }
        }
        assertEquals(stringify(expected), stringify(tree))
        assertTreesHaveSameContent(tree, expected)
    }

    @Test
    fun `test decode`() {
        val native = decodeFromTree<Frob>(treeExample())
        val expected = nativeExample()
        println(native)
        assertEquals(expected, native)
    }

    private fun stringify(node: Tree.Node): String = TreeNotation.Spaces.format(node)
}