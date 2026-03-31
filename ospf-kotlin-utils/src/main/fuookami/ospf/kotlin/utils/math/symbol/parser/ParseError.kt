package fuookami.ospf.kotlin.utils.math.symbol.parser

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

class ParseError(
    message: String,
    val position: Int
) : IllegalArgumentException("$message at position $position")

