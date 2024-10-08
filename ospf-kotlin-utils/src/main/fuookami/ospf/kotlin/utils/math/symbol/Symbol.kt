package fuookami.ospf.kotlin.utils.math.symbol

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*

interface Symbol {
    val name: String
    val displayName: String?
    val category: Category
    val discrete: Boolean get() = false
    // val range: ExpressionRange<*>
    val lowerBound: Bound<Flt64>?
    val upperBound: Bound<Flt64>?
}
