/**
 * Encode Kotlin types to Tree Notation.
 * Decode tree notation to Kotlin types.
 * See https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/formats.md#custom-formats-experimental
 */
package tl.jake.ktree.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import tl.jake.ktree.*

enum class Symbol(val value: String) {
    Null("null"),
    List("-"),
    MultilineString("|"),
    Escape("\\");

    fun escaped() =  Escape.value + this.value

}

const val KEY = "key"
const val VALUE = "value"


internal val json = Json

internal fun escapeString(str: String): String {
    if (str.isEmpty()) return str

    val encoded = json.encodeToString(str).let {
        it.substring(1 until it.length - 1)
    }

    return when {
        encoded == Symbol.Null.value -> Symbol.Null.escaped()
        encoded == Symbol.List.value -> Symbol.List.escaped()
        encoded == Symbol.MultilineString.value -> Symbol.MultilineString.escaped()
        encoded.startsWith(Symbol.Escape.value) -> Symbol.Escape.value + encoded
        else -> encoded
    }
}

internal fun unescapeString(str: String): String {
    if (str.isEmpty()) return str

    val encoded = if (str.startsWith(Symbol.Escape.value)) {
        str.substring(1 until str.length)
    } else {
        str
    }

    val jsonString = '"' + encoded + '"'
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
            is StructureKind.MAP -> KtreeMapEncoder(node, serializersModule)
            is StructureKind -> KtreeClassEncoder(node)
            else -> KtreeEncoder(node)
        }
    }

    override fun encodeNull() = pushCell(Symbol.Null.value)
    override fun encodeBoolean(value: Boolean): Unit = encodeToString(value)
    override fun encodeByte(value: Byte): Unit = encodeToString(value)
    override fun encodeShort(value: Short): Unit = encodeToString(value)
    override fun encodeInt(value: Int): Unit = encodeToString(value)
    override fun encodeLong(value: Long): Unit = encodeToString(value)
    override fun encodeFloat(value: Float): Unit = encodeToString(value)
    override fun encodeDouble(value: Double): Unit = encodeToString(value)
    override fun encodeChar(value: Char): Unit = encodeToString(value)
    override fun encodeString(value: String): Unit = encodeToString(value)
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int): Unit =
        encodeEscapedString(enumDescriptor.getElementName(index))

    private fun encodeToString(value: Any) = encodeEscapedString(value.toString())
    private fun encodeEscapedString(value: String) = pushCell(escapeString(value))
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
        node.addChild(NodeBuilder.build(Symbol.List.value))
    }
}

@ExperimentalSerializationApi
class KtreeClassEncoder(root: Tree.Node) : KtreeEncoder(root) {
    var target = root

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        if (shouldEncodeInline(descriptor, index)) {
            target = node
            return true
        }

        val newChild = if (shouldEncodeAnonymously(descriptor, index)) {
            NodeBuilder.build()
        } else {
            NodeBuilder.build(escapeString(descriptor.getElementName(index)))
        }

        node.addChild(newChild)
        target = newChild
        return true
    }

    private inline fun <reified T> SerialDescriptor.getAnnotation(index: Int): T? {
        val annotations = getElementAnnotations(index)
        return annotations.find { it is T } as T?
    }

    private fun shouldEncodeInline(descriptor: SerialDescriptor, index: Int): Boolean {
        val childDescriptor = descriptor.getElementDescriptor(index)
        return descriptor.getAnnotation<Inline>(index) != null && childDescriptor.kind is PrimitiveKind
    }

    private fun shouldEncodeAnonymously(descriptor: SerialDescriptor, index: Int): Boolean {
        return descriptor.getAnnotation<Anonymous>(index) != null
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val encoder = KtreeEncoder(target)
        return encoder.beginStructure(descriptor)
    }

    override fun pushCell(value: String) {
        target.cells.add(value)
    }
}

@ExperimentalSerializationApi
class KtreeMapEncoder(val root: Tree.Node, override val serializersModule: SerializersModule) : AbstractEncoder() {
    sealed class State {
        object Initial : State()
        data class EncodeKeyNext(val keyEncoder: KtreeEncoder, val valueEncoder: KtreeEncoder) : State()
        data class EncodeValueNext(val valueEncoder: KtreeEncoder) : State()
    }
    var state: State = State.Initial

    override fun encodeNull() = nextEncoder().encodeNull()
    override fun encodeBoolean(value: Boolean) = nextEncoder().encodeBoolean(value)
    override fun encodeByte(value: Byte) = nextEncoder().encodeByte(value)
    override fun encodeShort(value: Short) = nextEncoder().encodeShort(value)
    override fun encodeInt(value: Int) = nextEncoder().encodeInt(value)
    override fun encodeLong(value: Long) = nextEncoder().encodeLong(value)
    override fun encodeFloat(value: Float) = nextEncoder().encodeFloat(value)
    override fun encodeDouble(value: Double) = nextEncoder().encodeDouble(value)
    override fun encodeChar(value: Char) = nextEncoder().encodeChar(value)
    override fun encodeString(value: String) = nextEncoder().encodeString(value)
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = nextEncoder().encodeEnum(enumDescriptor, index)

    private fun nextEncoder(): KtreeEncoder {
        return when (val startingState = state) {
            is State.Initial -> throw Error("invalid state - can't encode without key or value state")
            is State.EncodeKeyNext -> {
                state = State.EncodeValueNext(startingState.valueEncoder)
                startingState.keyEncoder
            }
            is State.EncodeValueNext -> {
                state = State.Initial
                startingState.valueEncoder
            }
        }
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        if (state == State.Initial) {
            state = if (descriptor.getElementDescriptor(index).kind is PrimitiveKind) {
                val newChild = NodeBuilder.build()
                root.addChild(newChild)
                State.EncodeKeyNext(KtreeEncoder(newChild), KtreeEncoder(newChild))
            } else {
                lateinit var keyNode: Tree.Node
                lateinit var valueNode: Tree.Node
                val newChild = NodeBuilder.build(Symbol.List.value) {
                    node(KEY) { ref { keyNode = it } }
                    node(VALUE) { ref { valueNode = it } }
                }
                root.addChild(newChild)
                State.EncodeKeyNext(KtreeEncoder(keyNode), KtreeEncoder(valueNode))
            }
        }
        return true
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return nextEncoder().beginStructure(descriptor)
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

    override fun decodeNotNullMark(): Boolean = cellOrNull() != Symbol.Null.value
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
            is StructureKind.LIST -> KtreeListDecoder(node, serializersModule)
            is StructureKind.MAP -> KtreeMapDecoder(node, serializersModule)
            is StructureKind -> KtreeClassDecoder(node)
            else -> KtreeDecoder(node)
        }

    override fun decodeSequentially(): Boolean = true

    protected open fun cellOrNull() = node.cells.getOrNull(elementIndex)
    protected fun cell(): String {
        val maybeCell = cellOrNull()
        check(maybeCell != null) { "Cell $elementIndex must not be null in $node" }
        return maybeCell
    }
}

@ExperimentalSerializationApi
class KtreeListDecoder(val root: Tree.Node, override val serializersModule: SerializersModule) : AbstractDecoder() {
    var elementIndex = 0
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = elementIndex

    override fun decodeNotNullMark() = decoder().decodeNotNullMark()
    override fun decodeNull() = nextDecoder().decodeNull()
    override fun decodeBoolean() = nextDecoder().decodeBoolean()
    override fun decodeByte() = nextDecoder().decodeByte()
    override fun decodeShort() = nextDecoder().decodeShort()
    override fun decodeInt() = nextDecoder().decodeInt()
    override fun decodeLong() = nextDecoder().decodeLong()
    override fun decodeFloat() = nextDecoder().decodeFloat()
    override fun decodeDouble() = nextDecoder().decodeDouble()
    override fun decodeChar() = nextDecoder().decodeChar()
    override fun decodeString() = nextDecoder().decodeString()
    override fun decodeEnum(enumDescriptor: SerialDescriptor) = nextDecoder().decodeEnum(enumDescriptor)
    override fun beginStructure(descriptor: SerialDescriptor) = nextDecoder().beginStructure(descriptor)

    private fun decoder(): KtreeDecoder {
        val child = root.children[elementIndex]
        return if (child.typeCell == Symbol.List.value) KtreeDecoder(child.cloneData(Symbol.List.value)) else KtreeDecoder(child)
    }

    private fun nextDecoder() = decoder().also { elementIndex++ }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return root.children.size
    }

    override fun decodeSequentially(): Boolean = true
}

@ExperimentalSerializationApi
class KtreeMapDecoder(val root: Tree.Node, override val serializersModule: SerializersModule) : AbstractDecoder() {
    var elementIndex = 0
    val size = root.children.size * 2
    var elementDecoder: KtreeDecoder? = null

    override fun decodeNotNullMark() = nextDecoder().decodeNotNullMark()
    override fun decodeNull() = nextDecoder().decodeNull()
    override fun decodeBoolean() = nextDecoder().decodeBoolean()
    override fun decodeByte() = nextDecoder().decodeByte()
    override fun decodeShort() = nextDecoder().decodeShort()
    override fun decodeInt() = nextDecoder().decodeInt()
    override fun decodeLong() = nextDecoder().decodeLong()
    override fun decodeFloat() = nextDecoder().decodeFloat()
    override fun decodeDouble() = nextDecoder().decodeDouble()
    override fun decodeChar() = nextDecoder().decodeChar()
    override fun decodeString() = nextDecoder().decodeString()
    override fun decodeEnum(enumDescriptor: SerialDescriptor) = nextDecoder().decodeEnum(enumDescriptor)
    override fun beginStructure(descriptor: SerialDescriptor) = nextDecoder().beginStructure(descriptor)

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        check(descriptor.kind is StructureKind.MAP) { "Can only decode MAP, was ${descriptor.kind}" }
        if (elementIndex >= size) {
            return CompositeDecoder.DECODE_DONE
        }

        // Update decoder
        val kv = getKVNodeForIndex(elementIndex)
        if (elementDecoder?.node !== kv) {
            elementDecoder = KtreeDecoder(kv)
        }
        return elementIndex++
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return root.children.size * 2
    }

    private fun getKVNodeForIndex(index: Int): Tree.Node {
        val child = root.children[index / 2]
        return when (child.typeCell) {
            // - { key: $key, value: $value }
            Symbol.List.value -> {
                val name = if (index % 2 == 0) KEY else VALUE
                val kv = child.getChildData(name)
                checkNotNull(kv) { "KV map element should have child $name" }
                kv
            }
            // { $key: $value }
            else -> child
        }
    }

    private fun nextDecoder(): KtreeDecoder {
        val decoder = elementDecoder
        checkNotNull(decoder) { "Should have populated decoder by the time nextDecoder is called" }
        return decoder
    }
}

@ExperimentalSerializationApi
class KtreeClassDecoder(node: Tree.Node) : KtreeDecoder(node) {
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (node.children.isEmpty() || elementIndex >= node.children.size) {
            return CompositeDecoder.DECODE_DONE
        }

        return when (descriptor.kind) {
            is StructureKind.MAP -> elementIndex
            else -> {
                val child = node.children[elementIndex]
                val name = unescapeString(child.typeCell!!)
                return descriptor.getElementIndex(name)
            }
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        KtreeDecoder(child()).beginStructure(descriptor)

    override fun decodeSequentially() = false

    fun child(): Tree.Node {
        val node = node.children[elementIndex]
        elementIndex++
        return node
    }

    override fun cellOrNull() = child().dataCells.firstOrNull()
}
