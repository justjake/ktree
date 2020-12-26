package tl.jake.ktree

import tl.jake.ktree.parser.AST
import tl.jake.ktree.parser.Warning

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

    fun ancestors(): Sequence<Tree> = sequence {
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
        override val astNode: AST.FileNode?,
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

    /**
     * A tree Node.
     */
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

        /**
         * Detach this node from the tree.
         */
        fun detach() {
            this.parent?.removeChild(this)
        }

        /**
         * Move this node to be a child of `parent`.
         */
        fun moveTo(parent: Tree, at: AddingAt = AddingAt.End) {
            detach()
            parent.addChild(this, at)
        }

        /**
         * Clone this node
         */
        fun cloneWith(
            cells: Boolean = true,
            children: Boolean = true,
            block: NodeBuilderBlock? = null
        ): Node {
            return NodeBuilder.build {
                if (cells) {
                    cells(*this.cells.toTypedArray())
                }

                if (children) {
                    this@Node.children.forEach { cloneOf(it) }
                }

                if (block != null) {
                    block(this)
                }
            }
        }
    }

    /**
     * Remove a child
     */
    fun removeChild(child: Tree.Node) {
        checkHasChild(this, child)

        child.parent = null
        when (this) {
            is Root -> children.remove(child)
            is Node -> children.remove(child)
        }.exhaustive
    }


    /**
     * At a child node at `index` in children
     */
    fun addChild(index: Int, child: Tree.Node) {
        child.parent?.removeChild(child)
        child.parent = this
        if (index >= children.size) {
            (children as MutableList<Tree.Node>).add(child)
        } else {
            (children as MutableList<Tree.Node>).add(index, child)
        }
    }

    /**
     * Add a child node
     */
    fun addChild(child: Tree.Node, at: AddingAt = AddingAt.End) {
        when (at) {
            is AddingAt.Start -> addChild(0, child)
            is AddingAt.End -> addChild(children.size, child)
            is AddingAt.Before -> {
                val index = children.indexOf(at.sibling)
                addChild(if (index >= 0) index else 0, child)
            }
            is AddingAt.After -> {
                val index = children.indexOf(at.sibling)
                addChild(if (index >= 0) index + 1 else children.size, child)
            }
            is AddingAt.Index -> addChild(at.index, child)
            is AddingAt.Replace -> {
                addChild(children.indexOf(at.sibling), child)
                removeChild(at.sibling)
            }
            is AddingAt.ReplaceIndex -> {
                val existing = children[at.index]
                removeChild(existing)
                addChild(at.index, child)
            }
        }.exhaustive
    }
}

/**
 * Position to add a node at
 */
sealed class AddingAt {
    object Start : AddingAt()
    object End : AddingAt()
    data class Before(val sibling: Tree.Node) : AddingAt()
    data class After(val sibling: Tree.Node) : AddingAt()
    data class Replace(val sibling: Tree.Node) : AddingAt()
    data class Index(val index: Int) : AddingAt()
    data class ReplaceIndex(val index: Int) : AddingAt()
}

fun checkHasChild(parent: Tree, child: Tree.Node) {
    check(parent.children.contains(child)) { "Parent has child $child" }
}