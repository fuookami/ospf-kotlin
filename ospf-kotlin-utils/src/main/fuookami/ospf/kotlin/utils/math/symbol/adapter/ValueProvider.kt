package fuookami.ospf.kotlin.utils.math.symbol.adapter

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*

fun interface ValueProvider {
    operator fun get(symbol: Symbol): Flt64?
}

class MapValueProvider(
    private val values: Map<Symbol, Flt64>
) : ValueProvider {
    override fun get(symbol: Symbol): Flt64? {
        return values[symbol]
    }
}

enum class MissingValuePolicy {
    ReturnNull,
    AsZero,
    Fail
}
