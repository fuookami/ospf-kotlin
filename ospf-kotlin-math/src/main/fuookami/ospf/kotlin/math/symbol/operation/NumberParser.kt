package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.parse.NumberParser

data object Flt64NumberParser : NumberParser<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    override fun parse(text: String): Flt64? {
        return text.toDoubleOrNull()?.let(::Flt64)
    }
}