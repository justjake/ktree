package tl.jake.ktree.cli

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import tl.jake.ktree.InputFile
import tl.jake.ktree.TreeNotation
import tl.jake.ktree.parse
import tl.jake.ktree.serialize.toJson

/**
 * Read tree notation on STDIN (or from specified file) and write out it's
 * JSON encoding.
 */
fun main(args: Array<String>) {
    when (args.size) {
        0 -> jsonEncodeFile(TreeNotation(), InputFile("STDIN", readStdin()))
        1 -> jsonEncodeFile(TreeNotation(), InputFile(args[0], readFile(args[0])))
        2 -> jsonEncodeFile(Json.decodeFromString(args[0]), InputFile(args[1], readFile(args[1])))
    }
}

fun jsonEncodeFile(notation: TreeNotation, inputFile: InputFile) {
    val tree = notation.parse(inputFile.content, inputFile.filename)
    writeOut(tree.toJson(Json { prettyPrint = true }) + "\n")
    tree.allWarnings().forEach { warning ->
        writeError("ktree: ${warning.toString()}\n")
    }
}

expect fun readStdin(): String
expect fun readFile(filename: String): String
expect fun writeOut(data: String)
expect fun writeError(data: String)