import java.io.File

/**
 * Parse file from disk
 */
fun Parser.parseFile(filename: String): Tree.Root {
    // TODO: consider buffered reading & incremental parsing
    val text = File(filename).readText()
    return this.parse(text, filename)
}