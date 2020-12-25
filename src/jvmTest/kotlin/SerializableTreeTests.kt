package tl.jake.ktree

import kotlinx.serialization.json.Json
import tl.jake.ktree.serialize.*
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
    fun `verify round-trip to serializable tree`() {
        assertTreesHaveSameContent(tree, fromSerializableTree(tree.toSerializableTree()))
        assertTreesHaveSameContent(tree, tree.toSerializableTree().toTree())
    }

    @Test
    fun `verify round-trip to json`() {
        print(tree.toJson(Json { prettyPrint = true }))
        assertTreesHaveSameContent(tree, fromJson(tree.toJson()))
    }
}