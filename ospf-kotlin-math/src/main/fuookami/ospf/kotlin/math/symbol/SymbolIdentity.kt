/**
 * 符号标识
 * Symbol Identity
 *
 * 提供符号的唯一标识机制，用于在符号计算过程中区分不同的符号实例。
 * 即使两个符号具有相同的名称，它们也可以通过唯一标识进行区分。
 * Provides unique identification mechanism for symbols,
 * used to distinguish different symbol instances during symbolic computation.
 * Even if two symbols have the same name, they can be distinguished by unique identifiers.
 */
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

