package fuookami.ospf.kotlin.math.symbol

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

@JvmInline
value class SymbolId(val value: String) {
    override fun toString(): String = value
}

interface IdentifiedSymbol {
    val symbolId: String
}

interface OwnedSymbolLike : Symbol {
    val id: SymbolId
}

data class OwnedSymbol(
    override val id: SymbolId,
    override val name: String,
    override val displayName: String? = null
) : OwnedSymbolLike {
    constructor(symbol: Symbol, id: SymbolId = symbol.stableId()) : this(
        id = id,
        name = symbol.name,
        displayName = symbol.displayName
    )
}

fun Symbol.stableId(): SymbolId {
    return when (this) {
        is OwnedSymbolLike -> id
        is IdentifiedSymbol -> SymbolId(symbolId)
        else -> SymbolId("${name}#${System.identityHashCode(this)}")
    }
}

fun Symbol.owned(id: SymbolId = stableId()): OwnedSymbol {
    return OwnedSymbol(this, id)
}

fun Symbol.identity(): String {
    return stableId().value
}

val defaultSymbolComparator: Comparator<Symbol> = Comparator { lhs, rhs ->
    val byName = lhs.name.compareTo(rhs.name)
    if (byName != 0) {
        byName
    } else {
        lhs.identity().compareTo(rhs.identity())
    }
}

