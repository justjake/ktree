typealias NodeBuilderBlock = NodeBuilder.() -> Unit

class TreeBuilder {
    private val delegatedNodeBuilder = NodeBuilder()
    val children: MutableList<NodeBuilder>
        get() = delegatedNodeBuilder.children

    fun node(vararg cells: String, block: NodeBuilderBlock? = null) =
        apply { delegatedNodeBuilder.node(*cells, block = block) }

    fun build(): Tree.Root {
        val root = Tree.Root(null)
        children.forEach {
            root.children.add(it.build(root))
        }
        return root
    }

    companion object {
        fun build(block: TreeBuilder.() -> Unit): Tree.Root {
            val builder = TreeBuilder()
            builder.run(block)
            return builder.build()
        }
    }
}

class NodeBuilder {
    var refs = mutableListOf<Ref<Tree.Node>>()
    val cells = mutableListOf<String>()
    val children = mutableListOf<NodeBuilder>()

    fun ref(ref: Ref<Tree.Node>) {
        this.refs.add(ref)
    }

    fun ref(block: (Tree.Node) -> Unit) {
        this.refs.add(Ref.Callback(block))
    }

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
        val node = Tree.Node(parent=parent, astNode = null, indent = indent, cells = cells)
        children.forEach {
            node.children.add(it.build(node))
        }
        return node
    }

    companion object {
        fun build(vararg cells: String, block: NodeBuilderBlock? = null): Tree.Node {
            val builder = NodeBuilder()
            builder.cells(*cells)
            block?.let { builder.run(it) }
            return builder.build(null)
        }

        fun box() = Ref.Box<Tree.Node>(null)
    }
}

sealed class Ref<T> {
    data class Box<T>(var value: T?) : Ref<T>()
    data class Callback<T>(val update: (T) -> Unit) : Ref<T>()

    fun set(newValue: T) {
        return when(this) {
            is Box<T> -> this.value = newValue
            is Callback<T> -> update(newValue)
        }
    }
}
