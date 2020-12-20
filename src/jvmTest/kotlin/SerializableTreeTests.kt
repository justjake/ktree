import kotlinx.serialization.json.Json
import serialize.*
import kotlin.test.Test

class SerializableTreeTests {
    val tree = TreeBuilder.build {
        node("doctype", "html5")
        node("head") {
            node("title", "ktree documentation")
        }
        node("body") {
            node("article") {
                node("p", "very cool docs.")
                node("p", "more docs here.")
            }
        }
    }

    @Test
    fun testRoundTrip() {
        assertTreesHaveSameContent(tree, fromSerializableTree(toSerializableTree(tree)))
        assertTreesHaveSameContent(tree, tree.toSerializableTree().toTree())
    }

    @Test
    fun testJsonRoundTrip() {
        print(tree.toJson(Json { prettyPrint = true }))
        assertTreesHaveSameContent(tree, fromJson(tree.toJson()))
    }

    @Test
    fun readmeExample() {
        val text = """
            parent
               over-indented child 1
               over-indented child 2
                   over-indented child 3

        """.trimIndent()
        print(TreeNotation.Spaces.parse(text).toJson(Json { prettyPrint = true }))
    }
}