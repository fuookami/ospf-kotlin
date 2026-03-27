package fuookami.ospf.kotlin.utils.math.symbol.parser

class ParseError(
    message: String,
    val position: Int
) : IllegalArgumentException("$message at position $position")
