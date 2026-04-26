@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.monomial

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.number.Int64

operator fun Flt64.times(symbol: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(this, symbol)
}

operator fun UInt64.times(symbol: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(this.toFlt64(), symbol)
}

operator fun Int64.times(symbol: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(this.toFlt64(), symbol)
}

operator fun Int.times(symbol: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this.toDouble()), symbol)
}

operator fun Long.times(symbol: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this.toDouble()), symbol)
}

operator fun UInt.times(symbol: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this.toDouble()), symbol)
}

operator fun ULong.times(symbol: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this.toDouble()), symbol)
}

operator fun Float.times(symbol: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this.toDouble()), symbol)
}

operator fun Double.times(symbol: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this), symbol)
}
