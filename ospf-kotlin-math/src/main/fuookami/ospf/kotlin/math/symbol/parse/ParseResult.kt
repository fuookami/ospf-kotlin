package fuookami.ospf.kotlin.math.symbol.parse

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.functional.Ret

typealias ParseResult<T> = Ret<T>

enum class ParseIssueType {
    Lexical,
    Syntax,
    Conversion,
    Semantic,
    Unknown
}

data class ParseIssue(
    val type: ParseIssueType,
    val message: String,
    val input: String? = null,
    val position: Int? = null
)
