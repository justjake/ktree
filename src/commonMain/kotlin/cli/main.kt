package tl.jake.ktree.cli

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import tl.jake.ktree.parser.InputFile
import tl.jake.ktree.TreeNotation
import tl.jake.ktree.parse
import tl.jake.ktree.serialize.toJson

fun main(args: Array<String>) = parseArguments(args).perform()

class CLIError(message: String) : Error(message)

sealed class CLIAction {
    abstract fun perform()

    /**
     * Parse `inputFile` using either the supplied notation, the hash-bang
     * notation in the file, or
     */
    data class ConvertTreeNotationToCanonicalJson(
        val notation: TreeNotation?,
        val inputFile: InputFile
    ) : CLIAction() {
        override fun perform() {
            val (parsedNotation, startIndex) = TreeNotation.parseFromHashBang(inputFile.content)
                ?: Pair(
                    TreeNotation(), 0
                )
            val tree = (notation ?: parsedNotation).parse(
                inputFile.content,
                inputFile.filename,
                startIndex
            )
            writeOut(tree.toJson(Json { prettyPrint = true }) + "\n")
            tree.allWarnings().forEach { warning ->
                writeError("$CommmandName: ${warning}\n")
            }
        }
    }
}

fun parseArguments(args: Array<String>): CLIAction =
    when (args.size) {
        0 -> CLIAction.ConvertTreeNotationToCanonicalJson(null, InputFile("STDIN", readStdin()))
        1 -> CLIAction.ConvertTreeNotationToCanonicalJson(
            null,
            InputFile(args[0], readFile(args[0]))
        )
        2 -> CLIAction.ConvertTreeNotationToCanonicalJson(
            Json.decodeFromString(args[0]),
            InputFile(args[1], readFile(args[1]))
        )
        else -> throw CLIError("Unexpected number of arguments: ${args.size}")
    }

internal expect fun readStdin(): String
internal expect fun readFile(filename: String): String
internal expect fun writeOut(data: String)
internal expect fun writeError(data: String)