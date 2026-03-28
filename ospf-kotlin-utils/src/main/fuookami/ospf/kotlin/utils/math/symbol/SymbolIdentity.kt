package fuookami.ospf.kotlin.utils.math.symbol

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

interface IdentifiedSymbol {
    val symbolId: String
}

fun Symbol.identity(): String {
    return when (this) {
        is IdentifiedSymbol -> symbolId
        else -> "${name}#${System.identityHashCode(this)}"
    }
}

val defaultSymbolComparator: Comparator<Symbol> = Comparator { lhs, rhs ->
    val byName = lhs.name.compareTo(rhs.name)
    if (byName != 0) {
        byName
    } else {
        lhs.identity().compareTo(rhs.identity())
    }
}



