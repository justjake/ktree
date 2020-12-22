import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import serialize.toJson
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotEquals

class ReadmeTests {
    private fun readResource(name: String) =
        javaClass.classLoader.getResourceAsStream(name)

    private val variableRegex = Regex("\\$([\\w_]+)")

    private fun parse(s: String) = TreeNotation.Spaces.parse(s)

    @ExperimentalSerializationApi
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    private val ktree = """
package ktree

author
 name Jake
 email jake@example.com

dependencies
 multiplatform >= 2
  resolved https://example.com/multiplatform
  checksum abcdef1234
    """.trimIndent()

    private val overindented = """
parent
   over-indented child 1
   over-indented child 2
       over-indented child 3
    """.trimIndent()

    @Test
    fun testBuildReadme() {
        val readmeIn = readResource("README.in.md")!!.readAllBytes().decodeToString()
        val readme = readmeIn.replace(variableRegex) {
            val varname = it.groups[1]!!.value
            return@replace when (varname) {
                "KTREE" -> ktree
                "KTREE_OUTLINE" -> parse(ktree).toOutline()
                "KTREE_JSON" -> parse(ktree).toJson(json)
                "OVERINDENTED" -> overindented
                "OVERINDENTED_OUTLINE" -> parse(overindented).toOutline()
                "OVERINDENTED_JSON" -> parse(overindented).toJson(json)
                else -> throw Error("Unknown replacement var: $varname")
            }
        }
        // Should actually be transformed
        assertNotEquals(readmeIn, readme)

        File("README.md").writeText(readme)
        println(readme)
    }
}

