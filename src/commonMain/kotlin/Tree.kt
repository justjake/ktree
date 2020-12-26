package tl.jake.ktree

/**
 * A tree, as described by Tree Notation.
 */
sealed class Tree {
    abstract val children: List<Node>
    abstract val parent: Tree?
    abstract val astNode: AST?
    abstract val indent: Int
    abstract val warnings: List<Warning>
    abstract val depth: Int

    override fun toString(): String = recursiveToString(0)

    protected fun recursiveToString(indent: Int): String {
        val spaces = " ".repeat(indent)
        val c = when {
            children.isEmpty() -> "[]"
            else -> children.joinToString(
                separator = "\n",
                prefix = "[\n",
                postfix = "\n$spaces]"
            ) { it.recursiveToString(indent + 1) }
        }
        return when (this) {
            is Root -> "${spaces}Tree.Root(children=$c)"
            is Node -> "${spaces}Tree.Node(indent=$indent, cells=$cells, children=$c)"
        }
    }

    fun allWarnings(): List<Warning> {
        return warnings + children.flatMap { it.allWarnings() }
    }

    fun ancestors() = sequence<Tree> {
        var node = this@Tree
        while (node.parent != null) {
            yield(node.parent!!)
            node = node.parent!!
        }
    }

    /**
     * A tree Root never has a parent, and is un-indented.
     */
    data class Root(
        override val astNode: AST.File?,
        override val children: MutableList<Node> = mutableListOf<Node>()
    ) : Tree() {
        override val parent: Tree? = null
        override val indent = -1
        override val depth = -1
        override val warnings = mutableListOf<Warning>()
        override fun toString(): String {
            return recursiveToString(0)
        }
    }

    data class Node(
        override var parent: Tree?,
        val cells: MutableList<String>,
        override val indent: Int,
        override val astNode: AST.TreeNode? = null,
        override val children: MutableList<Node> = mutableListOf<Node>()
    ) : Tree() {
        override val warnings = mutableListOf<Warning>()
        override val depth: Int
            get() = parent.let {
                when (it) {
                    null -> 0
                    else -> 1 + it.depth
                }
            }

        var typeCell: String?
            get() = cells.firstOrNull()
            set(value) = when (value) {
                null -> cells.clear()
                else -> cells[0] = value
            }

        var dataCells: List<String>
            get() = when {
                cells.size > 1 -> cells.subList(1, cells.size)
                else -> listOf()
            }
            set(value) {
                if (cells.size == 0) {
                    cells.add("")
                }

                value.forEachIndexed { index, s -> cells[index + 1] = s }
                if (cells.size > value.size + 1) {
                    cells.subList(value.size + 1, cells.size).clear()
                }
            }
    }
}