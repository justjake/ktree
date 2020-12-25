package tl.jake.ktree

import tl.jake.ktree.nodeFS.readFile
import kotlin.js.Promise

/**
 * Parse file from disk
 */
fun Parser.parseFile(filename: String): Promise<Tree.Root> =
    readFile(filename, "utf-8").then {
        this.parse(it, filename)
    }
