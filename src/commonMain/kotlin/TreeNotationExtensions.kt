fun TreeNotation.parser() = Parser(this)

fun TreeNotation.parse(text: String, filename: String = "<memory>") =
    parser().parse(text, filename)

fun TreeNotation.formatter() = Formatter(this)

fun TreeNotation.format(tree: Tree) = formatter().format(tree)
