package tl.jake.ktree

import tl.jake.ktree.parser.Parser

// Parse
fun TreeNotation.parser() = Parser(this)
fun TreeNotation.parse(text: String, filename: String = "<memory>", startIndex: Int = 0) =
    parser().parse(text, filename, startIndex)

// Format
fun TreeNotation.formatter() = Formatter(this)
fun TreeNotation.format(tree: Tree) = formatter().format(tree)

