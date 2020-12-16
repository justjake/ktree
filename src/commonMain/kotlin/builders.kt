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
    val cells = mutableListOf<String>()
    val children = mutableListOf<NodeBuilder>()

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

    fun cloned(node: Tree.Node): NodeBuilder {
        TODO("Implement deep clone (and shallow clone)")
    }

    fun build(parent: Tree?): Tree.Node {
        val indent = 1 + (parent?.indent ?: -1)
        val node = Tree.Node.create(parent, null, indent, cells)
        children.forEach {
            node.children.add(it.build(node))
        }
        return node
    }
}