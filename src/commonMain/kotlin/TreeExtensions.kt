
// Transforms

/**
 * Call on the result of a when {} to use the when {} as an expression,
 * which forces it to be exhaustive.
 * https://proandroiddev.com/til-when-is-when-exhaustive-31d69f630a8b
 */
val <T> T.exhaustive: T
    get() = this

fun checkHasChild(parent: Tree, child: Tree.Node) {
    check(parent.children.contains(child)) { "Parent has child $child" }
}

/**
 * Remove a child
 */
fun Tree.removeChild(child: Tree.Node) {
    checkHasChild(this, child)

    child.parent = null
    when(this) {
        is Tree.Root -> children.remove(child)
        is Tree.Node -> children.remove(child)
    }.exhaustive
}


/**
 * At a child node at `index` in children
 */
fun Tree.addChild(index: Int, child: Tree.Node) {
    child.parent?.removeChild(child)
    child.parent = this
    if (index >= children.size) {
        (children as MutableList<Tree.Node>).add(child)
    } else {
        (children as MutableList<Tree.Node>).add(index, child)
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
    data class Replace(val sibling: Tree.Node): AddingAt()
    data class Index(val index: Int) : AddingAt()
    data class ReplaceIndex(val index: Int) : AddingAt()
}

/**
 * Add a child node
 */
fun Tree.addChild(child: Tree.Node, at: AddingAt = AddingAt.End) {
    when(at) {
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

/**
 * Detach this node from the tree.
 */
fun Tree.Node.detach() {
    this.parent?.removeChild(this)
}

/**
 * Move this node to be a child of `parent`.
 */
fun Tree.Node.moveTo(parent: Tree, at: AddingAt = AddingAt.End) {
    detach()
    parent.addChild(this, at)
}

/**
 * Clone this node
 */
fun Tree.Node.cloneWith(cells: Boolean = true, children: Boolean = true, block: NodeBuilderBlock? = null): Tree.Node {
    return NodeBuilder.build {
        if (cells) {
            cells(*this.cells.toTypedArray())
        }

        if (children) {
            this@cloneWith.children.forEach { cloneOf(it) }
        }

        if (block != null) {
            block(this)
        }
    }
}

// Transforms for working with child data
fun Tree.Node.hasPrefix(type: String, vararg dataPrefix: String): Boolean =
    typeCell == type &&
            dataCells.size >= dataPrefix.size &&
            dataCells.subList(0, dataPrefix.size) == dataPrefix.toList()


fun Tree.getChild(type: String, vararg dataPrefix: String): Tree.Node? = children.find {
    it.hasPrefix(type, *dataPrefix)
}

fun Tree.getChildData(type: String, vararg dataPrefix: String): Tree.Node? =
    getChild(type, *dataPrefix)?.dataNode(*dataPrefix)

fun Tree.setChildData(type: String, vararg dataPrefix: String, node: Tree.Node) {
    val newNode = when {
        node.hasPrefix(type, *dataPrefix) -> node
        else -> node.cloneWith(cells = false) {
            cell(type)
            cells(*dataPrefix)
            cells(*node.cells.toTypedArray())
        }
    }

    return when (val child = getChild(type, *dataPrefix)) {
        null -> this.addChild(newNode)
        else -> this.addChild(newNode, AddingAt.Replace(child))
    }
}

fun Tree.removeChild(type: String, vararg dataPrefix: String) {
    val child = getChild(type, *dataPrefix)
    if (child != null) {
        removeChild(child)
    }
}

// TODO: rename to cloneWithoutPrefix?
private fun Tree.Node.dataNode(vararg omitDataPrefix: String): Tree.Node = cloneWith(cells = false) {
    this@dataNode.dataCells.forEachIndexed { index, s ->
        if (omitDataPrefix.size > index && s != omitDataPrefix[index]) {
            cell(s)
        }
    }
}
