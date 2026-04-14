package fuookami.ospf.kotlin.core.expression.bridge

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.to
import fuookami.ospf.kotlin.quantities.quantity.toFlt64
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.unit.div
import fuookami.ospf.kotlin.quantities.unit.times

// unary minus

operator fun LinearIntermediateSymbol.unaryMinus(): LinearMonomial<Flt64> {
    return -Flt64.one * this
}

@JvmName("unaryMinusQuantitySymbol")
operator fun Quantity<LinearIntermediateSymbol>.unaryMinus(): Quantity<LinearMonomial<Flt64>> {
    return Quantity(-this.value, this.unit)
}

// symbol and constant

operator fun LinearIntermediateSymbol.times(rhs: Int): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(rhs), this)
}

operator fun LinearIntermediateSymbol.times(rhs: Double): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(rhs), this)
}

operator fun <T : RealNumber<T>> LinearIntermediateSymbol.times(rhs: T): LinearMonomial<Flt64> {
    return LinearMonomial(rhs.toFlt64(), this)
}

operator fun Int.times(rhs: LinearIntermediateSymbol): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this), rhs)
}

operator fun Double.times(rhs: LinearIntermediateSymbol): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this), rhs)
}

operator fun <T : RealNumber<T>> T.times(rhs: LinearIntermediateSymbol): LinearMonomial<Flt64> {
    return LinearMonomial(this.toFlt64(), rhs)
}

operator fun LinearIntermediateSymbol.div(rhs: Int): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(rhs).reciprocal(), this)
}

operator fun LinearIntermediateSymbol.div(rhs: Double): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(rhs).reciprocal(), this)
}

operator fun <T : RealNumber<T>> LinearIntermediateSymbol.div(rhs: T): LinearMonomial<Flt64> {
    return LinearMonomial(rhs.toFlt64().reciprocal(), this)
}

// symbol and quantity

operator fun <T : RealNumber<T>> LinearIntermediateSymbol.times(rhs: Quantity<T>): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(rhs.value.toFlt64(), this), rhs.unit)
}

operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: LinearIntermediateSymbol): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(this.value.toFlt64(), rhs), this.unit)
}

operator fun <T : RealNumber<T>> LinearIntermediateSymbol.div(rhs: Quantity<T>): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(Flt64.one / rhs.value.toFlt64(), this), NoneUnit)
}

// quantity symbol and constant

@JvmName("quantitySymbolTimesInt")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: Int): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(Flt64(rhs), this.value), this.unit)
}

@JvmName("quantitySymbolTimesDouble")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: Double): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(Flt64(rhs), this.value), this.unit)
}

@JvmName("quantitySymbolTimesRealNumber")
operator fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.times(rhs: T): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(rhs.toFlt64(), this.value), this.unit)
}

@JvmName("intTimesQuantitySymbol")
operator fun Int.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(Flt64(this), rhs.value), rhs.unit)
}

@JvmName("doubleTimesQuantitySymbol")
operator fun Double.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(Flt64(this), rhs.value), rhs.unit)
}

@JvmName("realNumberTimesQuantitySymbol")
operator fun <T : RealNumber<T>> T.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(this.toFlt64(), rhs.value), rhs.unit)
}

@JvmName("quantitySymbolDivInt")
operator fun Quantity<LinearIntermediateSymbol>.div(rhs: Int): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(Flt64(rhs).reciprocal(), this.value), this.unit)
}

@JvmName("quantitySymbolDivDouble")
operator fun Quantity<LinearIntermediateSymbol>.div(rhs: Double): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(Flt64(rhs).reciprocal(), this.value), this.unit)
}

@JvmName("quantitySymbolDivRealNumber")
operator fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.div(rhs: T): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(rhs.toFlt64().reciprocal(), this.value), this.unit)
}

// quantity symbol and quantity

@JvmName("quantitySymbolTimesQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.times(rhs: Quantity<T>): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(rhs.value.toFlt64(), this.value), this.unit * rhs.unit)
}

@JvmName("quantityTimesQuantitySymbol")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(this.value.toFlt64(), rhs.value), this.unit * rhs.unit)
}

@JvmName("quantitySymbolDivQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.div(rhs: Quantity<T>): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(rhs.value.toFlt64().reciprocal(), this.value), this.unit / rhs.unit)
}

// symbol to polynomial (plus/minus with constants)

operator fun LinearIntermediateSymbol.plus(rhs: Int): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64(rhs))
}

operator fun LinearIntermediateSymbol.plus(rhs: Double): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64(rhs))
}

operator fun <T : RealNumber<T>> LinearIntermediateSymbol.plus(rhs: T): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), rhs.toFlt64())
}

operator fun LinearIntermediateSymbol.minus(rhs: Int): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), -Flt64(rhs))
}

operator fun LinearIntermediateSymbol.minus(rhs: Double): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), -Flt64(rhs))
}

operator fun <T : RealNumber<T>> LinearIntermediateSymbol.minus(rhs: T): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), -rhs.toFlt64())
}

operator fun Int.plus(rhs: LinearIntermediateSymbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, rhs)), Flt64(this))
}

operator fun Double.plus(rhs: LinearIntermediateSymbol): LinearPolynomial<Flt64> {
    return Flt64(this).plus(rhs)
}

operator fun <T : RealNumber<T>> T.plus(rhs: LinearIntermediateSymbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, rhs)), this.toFlt64())
}

operator fun Int.minus(rhs: LinearIntermediateSymbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(-Flt64.one, rhs)), Flt64(this))
}

operator fun Double.minus(rhs: LinearIntermediateSymbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(-Flt64.one, rhs)), Flt64(this))
}

operator fun <T : RealNumber<T>> T.minus(rhs: LinearIntermediateSymbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(-Flt64.one, rhs)), this.toFlt64())
}

// symbol and variable

operator fun LinearIntermediateSymbol.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this), LinearMonomial(Flt64.one, rhs)), Flt64.zero)
}

operator fun LinearIntermediateSymbol.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this), LinearMonomial(-Flt64.one, rhs)), Flt64.zero)
}

operator fun AbstractVariableItem<*, *>.plus(rhs: LinearIntermediateSymbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this), LinearMonomial(Flt64.one, rhs)), Flt64.zero)
}

operator fun AbstractVariableItem<*, *>.minus(rhs: LinearIntermediateSymbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this), LinearMonomial(-Flt64.one, rhs)), Flt64.zero)
}

// quantity symbol and quantity variable

@JvmName("quantitySymbolPlusQuantityVariable")
operator fun Quantity<LinearIntermediateSymbol>.plus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariablePlusQuantitySymbol")
operator fun Quantity<AbstractVariableItem<*, *>>.plus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantitySymbolMinusQuantityVariable")
operator fun Quantity<LinearIntermediateSymbol>.minus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableMinusQuantitySymbol")
operator fun Quantity<AbstractVariableItem<*, *>>.minus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// symbol and symbol

operator fun LinearIntermediateSymbol.plus(rhs: LinearIntermediateSymbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this), LinearMonomial(Flt64.one, rhs)), Flt64.zero)
}

operator fun LinearIntermediateSymbol.minus(rhs: LinearIntermediateSymbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this), LinearMonomial(-Flt64.one, rhs)), Flt64.zero)
}

// quantity symbol and quantity symbol

@JvmName("quantitySymbolPlusQuantitySymbol")
operator fun Quantity<LinearIntermediateSymbol>.plus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantitySymbolMinusQuantitySymbol")
operator fun Quantity<LinearIntermediateSymbol>.minus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// quantity symbol and quantity

@JvmName("quantitySymbolPlusQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.plus(rhs: Quantity<T>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.toFlt64().to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPlusQuantitySymbol")
operator fun <T : RealNumber<T>> Quantity<T>.plus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.toFlt64().unit.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantitySymbolMinusQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.minus(rhs: Quantity<T>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.toFlt64().to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMinusQuantitySymbol")
operator fun <T : RealNumber<T>> Quantity<T>.minus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.toFlt64().unit.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// symbol sum

@JvmName("sumLinearSymbols")
fun sum(
    symbols: Iterable<LinearIntermediateSymbol>,
    ctor: (LinearIntermediateSymbol) -> LinearMonomial<Flt64> = { LinearMonomial(Flt64.one, it) }
): LinearPolynomial<Flt64> {
    val monomials = ArrayList<LinearMonomial<Flt64>>()
    for (symbol in symbols) {
        monomials.add(ctor(symbol))
    }
    return LinearPolynomial(monomials = monomials, constant = Flt64.zero)
}

fun <T> sumSymbols(
    objs: Iterable<T>,
    extractor: (T) -> LinearIntermediateSymbol?
): LinearPolynomial<Flt64> {
    return sum(objs.mapNotNull(extractor))
}
