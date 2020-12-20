import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val example = """
    project Tree parser in Kotlin
    
    author
     name Jake
     email jake@example.com
    
    version 0.0.0
    
""".trimIndent()

val useSpaces = NotationSettings.Spaces

fun treeToString(tree: Tree) = Printer(useSpaces).printToString(tree)
fun parsedExample() = Parser(useSpaces).parse(example)
fun buildExample() = TreeBuilder.build {
    node("project", "Tree parser in Kotlin")
    node()
    node("author") {
        node("name", "Jake")
        node("email", "jake@example.com")
    }
    node()
    node("version", "0.0.0")
}

fun assertTreesHaveSameContent(expected: Tree, actual: Tree, prefix: String = "") {
    // Cells
    if (expected is Tree.Node) {
        assertEquals(expected.cells, (actual as Tree.Node).cells, "$prefix.cells")
    }

    // Children
    assertEquals(expected.children.size, actual.children.size, "$prefix.children.size")
    actual.children.forEachIndexed { index, node ->
        val expectedNode = expected.children[index]
        assertTreesHaveSameContent(expectedNode, node, "$prefix.children[$index]")
    }
}

class ParserTests {
    @Test
    fun testLex() {
        val parser = Parser(useSpaces)
        val dummyPos = Position(0, 0, 0)
        val dummySpan = Span(dummyPos, dummyPos)
        val tokens = parser.lex("human Jake\n favorite food tofu\n")
        val expectedTokens = listOf(
            Token(Token.Type.Text, "human", dummySpan),
            Token(Token.Type.WordBreakOrEdge, " ", dummySpan),
            Token(Token.Type.Text, "Jake", dummySpan),
            Token(Token.Type.NodeBreak, "\n", dummySpan),
            Token(Token.Type.WordBreakOrEdge, " ", dummySpan),
            Token(Token.Type.Text, "favorite", dummySpan),
            Token(Token.Type.WordBreakOrEdge, " ", dummySpan),
            Token(Token.Type.Text, "food", dummySpan),
            Token(Token.Type.WordBreakOrEdge, " ", dummySpan),
            Token(Token.Type.Text, "tofu", dummySpan),
            Token(Token.Type.NodeBreak, "\n", dummySpan),
        )
        assertEquals(expectedTokens.size, tokens.size)
        tokens.forEachIndexed { index, token ->
            val expectedToken = expectedTokens[index]
            assertEquals(expectedToken.type, token.type, "[$index] type matches")
            assertEquals(expectedToken.string, token.string, "[$index] string matches")
        }
    }

    @Test
    fun testAst() {
        val parser = Parser(useSpaces)
        val dummyPos = Position(0, 0, 0)
        val dummySpan = Span(dummyPos, dummyPos)
        val tokens = listOf(
            Token(Token.Type.Text, "human", dummySpan),
            Token(Token.Type.WordBreakOrEdge, " ", dummySpan),
            Token(Token.Type.Text, "Jake", dummySpan),
            Token(Token.Type.NodeBreak, "\n", dummySpan),
            Token(Token.Type.WordBreakOrEdge, " ", dummySpan),
            Token(Token.Type.Text, "favorite", dummySpan),
            Token(Token.Type.WordBreakOrEdge, " ", dummySpan),
            Token(Token.Type.Text, "food", dummySpan),
            Token(Token.Type.WordBreakOrEdge, " ", dummySpan),
            Token(Token.Type.Text, "tofu", dummySpan),
            Token(Token.Type.NodeBreak, "\n", dummySpan),
        )
        val inputFile = InputFile("test", "")
        val expectedFile = AST.File(
            span = dummySpan,
            raw = inputFile,
            nodes = listOf(
                // human Jake
                AST.TreeNode(
                    span = dummySpan,
                    indent = 0,
                    edgeTokens = listOf(),
                    words = listOf(
                        AST.Word(
                            dummySpan,
                            listOf(tokens[0]),
                            tokens[0].string
                        ),
                        AST.Word(
                            dummySpan,
                            listOf(tokens[2]),
                            tokens[2].string,
                        )
                    )
                ),
                //  favorite food tofu
                AST.TreeNode(
                    span = dummySpan,
                    indent = 1,
                    edgeTokens = listOf(tokens[4]),
                    words = listOf(
                        AST.Word(dummySpan, listOf(tokens[5]), tokens[5].string),
                        AST.Word(dummySpan, listOf(tokens[7]), tokens[7].string),
                        AST.Word(dummySpan, listOf(tokens[9]), tokens[9].string),
                    )
                )
            )
        )
        val file = parser.ast(inputFile, tokens)

        assertEquals(expectedFile.nodes.size, file.nodes.size, "total nodes match")
        file.nodes.forEachIndexed { index, treeNode ->
            val expectedTreeNode = expectedFile.nodes[index]
            assertEquals(expectedTreeNode.indent, treeNode.indent, "[$index]: indent equal")
            assertEquals(expectedTreeNode.edgeTokens, treeNode.edgeTokens, "[$index]: edge tokens equal")
            assertEquals(expectedTreeNode.words, treeNode.words, "[$index]: words equal")
        }
    }

    @Test
    fun testTreeParser() {
        val parser = Parser(useSpaces)
        val dummyPos = Position(0, 0, 0)
        val dummySpan = Span(dummyPos, dummyPos)
        val tokens = listOf(
            Token(Token.Type.Text, "human", dummySpan),
            Token(Token.Type.WordBreakOrEdge, " ", dummySpan),
            Token(Token.Type.Text, "Jake", dummySpan),
            Token(Token.Type.NodeBreak, "\n", dummySpan),
            Token(Token.Type.WordBreakOrEdge, " ", dummySpan),
            Token(Token.Type.Text, "favorite", dummySpan),
            Token(Token.Type.WordBreakOrEdge, " ", dummySpan),
            Token(Token.Type.Text, "food", dummySpan),
            Token(Token.Type.WordBreakOrEdge, " ", dummySpan),
            Token(Token.Type.Text, "tofu", dummySpan),
            Token(Token.Type.NodeBreak, "\n", dummySpan),
        )
        val inputFile = InputFile("test", "")
        val ast = parser.ast(inputFile, tokens)

        val expectedTree = TreeBuilder.build {
            node("human", "Jake") {
                node("favorite", "food", "tofu")
            }
        }
        val tree = parser.tree(ast)

        assertTreesHaveSameContent(expectedTree, tree)
    }

    @Test
    fun testParser() {
        val tree = parsedExample()
        assertEquals(treeToString(buildExample()), treeToString(parsedExample()))
    }

    @Test
    fun testOverIndentWarnings() {
        val text = """
            package 
              id com.example.bad
              version 2.0
        """.trimIndent()
        fun assertHasOverindentedWarning(node: Tree.Node, parent: Tree, msg: String) {
            assertEquals(node.warnings.size, 1, "$msg: has one warning")
            val warning = node.warnings.first()
            assertTrue("$msg: Is overindent warning") { warning is Warning.ChildNodeOverIndented }
            if (warning is Warning.ChildNodeOverIndented) {
                assertEquals(2, warning.indent, msg)
                assertEquals(0, warning.parentIndent, msg)
                assertEquals(warning.node, node, msg)
                assertEquals(warning.parent, parent, msg)
            }
        }

        val tree = Parser(useSpaces).parse(text)
        val packageNode = tree.children[0]

        val warnings = mutableListOf<Warning>()
        assertEquals(packageNode.children.size, 2)
        packageNode.children.forEachIndexed { index, node ->
            val warning = node.warnings.first()
            warnings.add(warning)
            assertHasOverindentedWarning(node, packageNode, "packageNode.children[$index]")
        }

        assertEquals(warnings, tree.allWarnings())
    }

    @Test
    fun testPrinter() {
        val tree = buildExample()
        val printer = Printer(useSpaces)
        val str = printer.printToString(tree)
        assertEquals(example, str)
    }

    @Test
    fun testOverindentedSiblingsParentedToSameNode() {
        val text = """
            package
              id com.example.bad
              version 2.0
        """.trimIndent()
        val tree = Parser(useSpaces).parse(text)
        val expected = TreeBuilder.build {
            node("package") {
                node("", "id", "com.example.bad")
                node("", "version", "2.0")
            }
        }
        print(treeToString(tree))
        print(treeToString(expected))
        assertTreesHaveSameContent(expected, tree)
    }
}
