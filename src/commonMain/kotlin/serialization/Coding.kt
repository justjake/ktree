/**
 * Serialize Kotlin values to Tree Notation.
 * See https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/formats.md#custom-formats-experimental
 */
package tl.jake.ktree.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import tl.jake.ktree.NodeBuilder
import tl.jake.ktree.Tree
import tl.jake.ktree.toOutline

// TODO: re-work null handling. Just encoding as "null" means that
// String? fields are ambiguous because { value: null } and { value: "null" }
// encode identically.
internal const val NULL = "null"
internal const val LIST_TYPE = "-"
internal const val JSON_QUOTE = '"'
internal const val STRING_CONTAINING_NULL = "$JSON_QUOTE$NULL$JSON_QUOTE"
internal val json = Json

internal fun escapeString(str: String): String {
    if (str.isEmpty()) return str
    // TODO this null handling sucks
    if (str == NULL) return STRING_CONTAINING_NULL
    val jsonString = json.encodeToString(str)
    return jsonString.substring(1 until jsonString.length - 1)
}

internal fun unescapeString(str: String): String {
    if (str.isEmpty()) return str
    if (str == STRING_CONTAINING_NULL) return NULL
    val jsonString = '"' + str + '"'
    return json.decodeFromString(jsonString)
}

@ExperimentalSerializationApi
fun <T> encodeToTree(serializer: SerializationStrategy<T>, value: T): Tree.Node {
    val encoder = KtreeEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.node
}

@ExperimentalSerializationApi
inline fun <reified T> encodeToTree(value: T) = encodeToTree(serializer(), value)

@ExperimentalSerializationApi
open class KtreeEncoder(val node: Tree.Node = NodeBuilder.build()) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return when (descriptor.kind) {
            is StructureKind.LIST -> KtreeListEncoder(node)
            is StructureKind -> KtreeMapEncoder(node)
            else -> KtreeEncoder(node)
        }
    }

    override fun encodeNull() = pushCell(NULL)
    override fun encodeBoolean(value: Boolean): Unit = encodeToString(value)
    override fun encodeByte(value: Byte): Unit = encodeToString(value)
    override fun encodeShort(value: Short): Unit = encodeToString(value)
    override fun encodeInt(value: Int): Unit = encodeToString(value)
    override fun encodeLong(value: Long): Unit = encodeToString(value)
    override fun encodeFloat(value: Float): Unit = encodeToString(value)
    override fun encodeDouble(value: Double): Unit = encodeToString(value)
    override fun encodeChar(value: Char): Unit = encodeEscapedString(value)
    override fun encodeString(value: String): Unit = encodeEscapedString(value)
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int): Unit =
        encodeEscapedString(enumDescriptor.getElementName(index))

    private fun encodeEscapedString(value: Char) = encodeEscapedString(value.toString())
    private fun encodeEscapedString(value: String) = pushCell(escapeString(value))
    private fun encodeToString(value: Any) = pushCell(value.toString())
    protected open fun pushCell(value: String) {
        node.cells.add(value)
    }
}

@ExperimentalSerializationApi
class KtreeListEncoder(root: Tree.Node) : KtreeEncoder(root) {
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        // TODO: was a new child created already by a call to encodeElement?
        //       It should have been.
        val child = node.children.last()
        val encoder = KtreeEncoder(child)
        return encoder.beginStructure(descriptor)
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        newChild()
        return true
    }

    override fun pushCell(value: String) {
        node.children.last().cells.add(value)
    }

    private fun newChild() {
        node.addChild(NodeBuilder.build(LIST_TYPE))
    }
}

@ExperimentalSerializationApi
class KtreeMapEncoder(root: Tree.Node) : KtreeEncoder(root) {
    private var isKey = true

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        val child: Tree.Node? = when (descriptor.kind) {
            is StructureKind.MAP -> {
                if (isKey) {
                    isKey = false
                    NodeBuilder.build()
                } else {
                    isKey = true
                    null
                }
            }
            else -> NodeBuilder.build(escapeString(descriptor.getElementName(index)))
        }

        if (child != null) node.addChild(child)

        return true
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        // TODO: was a new child created already by a call to encodeElement?
        //       It should have been.
        val child = node.children.last()
        val encoder = KtreeEncoder(child)
        return encoder.beginStructure(descriptor)
    }

    override fun pushCell(value: String) {
        node.children.last().cells.add(value)
    }
}

@ExperimentalSerializationApi
fun <T> decodeFromTree(node: Tree.Node, deserializer: DeserializationStrategy<T>): T {
    val decoder = KtreeDecoder(node)
    return decoder.decodeSerializableValue(deserializer)
}

@ExperimentalSerializationApi
inline fun <reified T> decodeFromTree(node: Tree.Node): T = decodeFromTree(node, serializer())
@ExperimentalSerializationApi
inline fun <reified T> decodeFromTree(node: Tree.Root): T = decodeFromTree(node.toNode(), serializer())

@ExperimentalSerializationApi
open class KtreeDecoder(val node: Tree.Node) : AbstractDecoder() {
    protected var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun decodeNotNullMark(): Boolean = cell() != NULL
    override fun decodeBoolean(): Boolean = cell().toBoolean()
    override fun decodeByte(): Byte = cell().toByte()
    override fun decodeShort(): Short = cell().toShort()
    override fun decodeInt(): Int = cell().toInt()
    override fun decodeLong(): Long = cell().toLong()
    override fun decodeFloat(): Float = cell().toFloat()
    override fun decodeDouble(): Double = cell().toDouble()
    // TODO: should this be .toInt().toChar()?
    override fun decodeChar(): Char = unescapeString(cell()).toCharArray().first()
    override fun decodeString(): String = unescapeString(cell())
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val name = unescapeString(cell())
        return enumDescriptor.getElementIndex(name)
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return node.children.size
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        when(descriptor.kind) {
            is StructureKind.LIST -> KtreeListDecoder(node)
            is StructureKind -> KtreeMapDecoder(node)
            else -> KtreeDecoder(node)
        }

    override fun decodeSequentially(): Boolean = true

    protected open fun cell() = node.cells[elementIndex]
}


@ExperimentalSerializationApi
class KtreeListDecoder(node: Tree.Node) : KtreeDecoder(node) {
    // Does this even make sense??
    override fun cell() = node.dataCells[elementIndex]
    fun child() = node.children[elementIndex]

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val result = KtreeDecoder(child()).beginStructure(descriptor)
        elementIndex++
        return result
    }
}

@ExperimentalSerializationApi
class KtreeMapDecoder(node: Tree.Node) : KtreeDecoder(node) {
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (node.children.isEmpty() || elementIndex >= node.children.size) {
            return CompositeDecoder.DECODE_DONE
        }

        return when (descriptor.kind) {
            is StructureKind.MAP -> elementIndex
            else -> {
                val child = node.children[elementIndex]
                val name = unescapeString(child.typeCell!!)
                println("[${node.toOutline()}] decodeElementIndex: name $name")
                return descriptor.getElementIndex(name)
            }
        }.also { println("decodeElementIndex: $it, elementIndex=$elementIndex") }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        KtreeDecoder(child()).beginStructure(descriptor)

    override fun decodeSequentially() = false

    fun child(): Tree.Node {
        val node = node.children[elementIndex]
        elementIndex++
        return node
    }

    override fun cell() = child().dataCells.first()
}
