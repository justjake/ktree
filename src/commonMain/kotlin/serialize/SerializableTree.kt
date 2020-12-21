package serialize

import NodeBuilder
import NodeBuilderBlock
import Tree
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SerializableTree(val cells: List<String> = listOf(), val children: List<SerializableTree> = listOf())

fun Tree.toSerializableTree(): SerializableTree = when(this) {
    is Tree.Root -> SerializableTree(children = children.map { it.toSerializableTree() })
    is Tree.Node -> SerializableTree(cells, children.map { it.toSerializableTree() } )
}
fun fromSerializableTree(serializableTree: SerializableTree): Tree = NodeBuilder.build(block = nodeBuilderBlock(serializableTree))

private fun nodeBuilderBlock(serializableNode: SerializableTree): NodeBuilderBlock {
    return fun NodeBuilder.() {
        cells(*serializableNode.cells.toTypedArray())
        serializableNode.children.forEach { childNode ->
            node(block = nodeBuilderBlock(childNode))
        }
    }
}

fun Tree.toJson(json: Json = Json) = json.encodeToString(this.toSerializableTree())
fun fromJson(jsonText: String, json: Json = Json) = json.decodeFromString<SerializableTree>(jsonText).toTree()

fun SerializableTree.toTree() = fromSerializableTree(this)