package tl.jake.ktree.cli

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tl.jake.ktree.TreeNotation

const val CommmandName = "ktree"
const val HashBang = "#!"
private val ShebangRegex = Regex("""^$HashBang\s*(\w+)(\s+(.*))?$""")

/**
 * Parse notation settings given as a #! line from input data.
 *
 * As an extension to the official tree notation, you may include
 * notation configuration on the first line as part of a #! that
 * contains the ktree CLI command that will properly parse the
 * file.
 */
fun TreeNotation.Companion.parseFromHashBang(data: String): Pair<TreeNotation, Int>? {
    val lines = data.lines()
    val match = ShebangRegex.find(lines.first()) ?: return null
    val args = if (match.groups.size > 2) match.groups[2]!!.value else ""
    val notation = Json { isLenient = true }.decodeFromString<TreeNotation>(args)
    return Pair(notation, lines.first().length + 1)
}

fun TreeNotation.toHashBang(commandName: String = CommmandName): String {
    val jsonText = Json.encodeToString(this)
    return "$HashBang $commandName $jsonText"
}