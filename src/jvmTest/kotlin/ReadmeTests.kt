import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import serialize.toJson
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotEquals

@ExperimentalSerializationApi
class ReadmeTests {
    private fun readResource(name: String) =
        javaClass.classLoader.getResourceAsStream(name)
    private val variableRegex = Regex("\\$([\\w_]+)")

    private val notation = TreeNotation.Spaces
    private fun parse(s: String) = notation.parse(s)
    private fun parseEqual(s: String) = notation.copy(overIndentBehavior = TreeNotation.OverIndentBehavior.EquallyIndentedChildrenAreSiblings).parse(s)

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

    private val author = NodeBuilder.build {
        cell("author")
        node("name", "Jake")
        node("email", "jake@example.com")
    }

    @Test
    fun testBuildReadme() {
        val readmeIn = readResource("README.in.md")!!.readAllBytes().decodeToString()
        val readme = readmeIn.replace(variableRegex) {
            val varname = it.groups[1]!!.value
            return@replace when (varname) {
                "KTREE" -> ktree
                "KTREE_OUTLINE" -> parse(ktree).toOutline()
                "KTREE_JSON" -> parse(ktree).toJson(json)
                "AUTHOR" -> notation.format(author)
                "AUTHOR_JSON" -> author.toJson(json)
                "OVERINDENTED" -> overindented
                "OVERINDENTED_STRICT_OUTLINE" -> parse(overindented).toOutline()
                "OVERINDENTED_STRICT_JSON" -> parse(overindented).toJson(json)
                "OVERINDENTED_EQUAL_OUTLINE" -> parseEqual(overindented).toOutline()
                "OVERINDENTED_EQUAL_JSON" -> parseEqual(overindented).toJson(json)
                else -> throw Error("Unknown replacement var: $varname")
            }
        }
        // Should actually be transformed
        assertNotEquals(readmeIn, readme)

        File("README.md").writeText(readme)
        println(readme)
    }

    @Test
    fun failingTestMaybe() {
        throw Error("crap!!!")
    }
}

