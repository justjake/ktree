package serialize

import NodeBuilder
import NodeBuilderBlock
import Tree
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class SerializableTree {
    abstract val children: List<Node>

    @Serializable
    @SerialName("root")
    data class Root(override val children: List<Node>) : SerializableTree()

    @Serializable
    @SerialName("node")
    data class Node(val cells: List<String>, override val children: List<Node>) : SerializableTree()
}

fun toSerializableTree(tree: Tree.Root): SerializableTree.Root = SerializableTree.Root(
    children = tree.children.map { toSerializableTree(it) }
)

fun toSerializableTree(tree: Tree.Node): SerializableTree.Node = SerializableTree.Node(
    cells = tree.cells,
    children = tree.children.map { toSerializableTree(it) }
)

private fun nodeBuilderBlock(serializableNode: SerializableTree.Node): NodeBuilderBlock {
    return fun NodeBuilder.() {
        cells(*serializableNode.cells.toTypedArray())
        serializableNode.children.forEach { childNode ->
            node(block = nodeBuilderBlock(childNode))
        }
    }
}

fun fromSerializableTree(serializableTree: SerializableTree): Tree {
    return when(serializableTree) {
        is SerializableTree.Root -> TreeBuilder.build {
            serializableTree.children.forEach { child -> node(block = nodeBuilderBlock(child)) }
        }
        is SerializableTree.Node -> NodeBuilder.build(block = nodeBuilderBlock(serializableTree))
    }
}

fun Tree.toJson(json: Json = Json) = json.encodeToString(this.toSerializableTree())
fun fromJson(jsonText: String, json: Json = Json) = json.decodeFromString<SerializableTree>(jsonText).toTree()

fun SerializableTree.toTree() = fromSerializableTree(this)

fun Tree.toSerializableTree() = when (this) {
    is Tree.Node -> toSerializableTree(this)
    is Tree.Root -> toSerializableTree(this)
}