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
    child.parent?.let { it.removeChild(child) }
    child.parent = this
    (children as MutableList<Tree.Node>).add(index, child)
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