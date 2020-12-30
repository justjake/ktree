package tl.jake.ktree

typealias NodeBuilderBlock = NodeBuilder.() -> Unit
typealias TreeBuilderBlock = TreeBuilder.() -> Unit

class TreeBuilder {
    constructor()
    constructor(block: TreeBuilderBlock) {
        run(block)
    }

    private val refs = mutableListOf<Ref<Tree.Root>>()
    private val delegatedNodeBuilder = NodeBuilder()

    val children: MutableList<NodeBuilder>
        get() = delegatedNodeBuilder.children

    fun ref(ref: Ref<Tree.Root>) = refs.add(ref)
    fun ref(block: (Tree.Root) -> Unit) = this.refs.add(Ref.Callback(block))

    fun node(vararg cells: String, block: NodeBuilderBlock? = null) =
        apply { delegatedNodeBuilder.node(*cells, block = block) }

    fun build(): Tree.Root {
        val root = Tree.Root(null)
        children.forEach {
            root.children.add(it.build(root))
        }
        refs.forEach { it.set(root) }
        return root
    }

    companion object {
        fun build(block: TreeBuilderBlock): Tree.Root {
            val builder = TreeBuilder(block)
            return builder.build()
        }
    }
}

class NodeBuilder {
    constructor()
    constructor(block: NodeBuilderBlock) {
        run(block)
    }

    val refs = mutableListOf<Ref<Tree.Node>>()
    val cells = mutableListOf<String>()
    val children = mutableListOf<NodeBuilder>()

    fun ref(ref: Ref<Tree.Node>) = this.refs.add(ref)
    fun ref(block: (Tree.Node) -> Unit) = this.refs.add(Ref.Callback(block))

    fun cells(vararg newCells: String): NodeBuilder = apply { cells.addAll(newCells) }
    fun cell(data: String): NodeBuilder = apply { cells.add(data) }

    fun node(vararg cells: String, block: NodeBuilderBlock? = null): NodeBuilder {
        val childBuilder = NodeBuilder().cells(*cells)
        if (block != null) {
            childBuilder.run(block)
        }
        children.add(childBuilder)
        return this
    }

    fun cloneOf(existing: Tree.Node): NodeBuilder {
        node(*existing.cells.toTypedArray()) {
            existing.children.forEach { existingChild -> cloneOf(existingChild) }
        }
        return this
    }

    fun build(parent: Tree?): Tree.Node {
        val indent = 1 + (parent?.indent ?: -1)
        val node = Tree.Node(parent = parent, astNode = null, indent = indent, cells = cells)
        children.forEach {
            node.children.add(it.build(node))
        }
        refs.forEach { it.set(node) }
        return node
    }

    companion object {
        fun build(block: NodeBuilderBlock): Tree.Node = NodeBuilder(block).build(null)
        fun build(vararg cells: String, block: NodeBuilderBlock? = null): Tree.Node = build {
            this.cells(*cells)
            if (block != null) block(this)
        }

        fun box() = Ref.Box<Tree.Node>(null)
    }
}

sealed class Ref<T> {
    data class Box<T>(var value: T?) : Ref<T>()
    data class Callback<T>(val update: (T) -> Unit) : Ref<T>()

    fun set(newValue: T) {
        return when (this) {
            is Box<T> -> this.value = newValue
            is Callback<T> -> update(newValue)
        }
    }
}
