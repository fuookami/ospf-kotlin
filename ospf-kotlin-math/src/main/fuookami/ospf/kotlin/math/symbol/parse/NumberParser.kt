package fuookami.ospf.kotlin.math.symbol.parse

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64

fun interface NumberParser<T> {
    fun parse(text: String): T?
}

data object Flt64NumberParser : NumberParser<Flt64> {
    override fun parse(text: String): Flt64? {
        return text.toDoubleOrNull()?.let(::Flt64)
    }
}

data object Int64NumberParser : NumberParser<Int64> {
    override fun parse(text: String): Int64? {
        return text.toLongOrNull()?.let(::Int64)
    }
}
