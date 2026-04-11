package fuookami.ospf.kotlin.core.frontend.expression.bridge

import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.div
import fuookami.ospf.kotlin.math.symbol.polynomial.times
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.to
import fuookami.ospf.kotlin.quantities.quantity.toFlt64
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.div
import fuookami.ospf.kotlin.quantities.unit.times

// unary minus

operator fun AbstractVariableItem<*, *>.unaryMinus(): LinearMonomial<Flt64> {
    return -Flt64.one * this
}

@JvmName("unaryMinusQuantityVariable")
operator fun Quantity<AbstractVariableItem<*, *>>.unaryMinus(): Quantity<LinearMonomial<Flt64>> {
    return Quantity(-this.value, this.unit)
}

// variable and constant

operator fun AbstractVariableItem<*, *>.times(rhs: Int): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(rhs), this)
}

operator fun AbstractVariableItem<*, *>.times(rhs: Double): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(rhs), this)
}

operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.times(rhs: T): LinearMonomial<Flt64> {
    return LinearMonomial(rhs.toFlt64(), this)
}

operator fun Int.times(rhs: AbstractVariableItem<*, *>): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this), rhs)
}

operator fun Double.times(rhs: AbstractVariableItem<*, *>): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this), rhs)
}

operator fun <T : RealNumber<T>> T.times(rhs: AbstractVariableItem<*, *>): LinearMonomial<Flt64> {
    return LinearMonomial(this.toFlt64(), rhs)
}

operator fun AbstractVariableItem<*, *>.div(rhs: Int): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(rhs).reciprocal(), this)
}

operator fun AbstractVariableItem<*, *>.div(rhs: Double): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(rhs).reciprocal(), this)
}

operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.div(rhs: T): LinearMonomial<Flt64> {
    return LinearMonomial(rhs.toFlt64().reciprocal(), this)
}

// variable and quantity

operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.times(rhs: Quantity<T>): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(rhs.value.toFlt64(), this), rhs.unit)
}

operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: AbstractVariableItem<*, *>): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(this.value.toFlt64(), rhs), this.unit)
}

operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.div(rhs: Quantity<T>): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(Flt64.one / rhs.value.toFlt64(), this), NoneUnit)
}

// quantity variable and constant

@JvmName("quantityVariableTimesInt")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: Int): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(Flt64(rhs), this.value), this.unit)
}

@JvmName("quantityVariableTimesDouble")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: Double): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(Flt64(rhs), this.value), this.unit)
}

@JvmName("quantityVariableTimesRealNumber")
operator fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.times(rhs: T): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(rhs.toFlt64(), this.value), this.unit)
}

@JvmName("intTimesQuantityVariable")
operator fun Int.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(Flt64(this), rhs.value), rhs.unit)
}

@JvmName("doubleTimesQuantityVariable")
operator fun Double.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(Flt64(this), rhs.value), rhs.unit)
}

@JvmName("realNumberTimesQuantityVariable")
operator fun <T : RealNumber<T>> T.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(this.toFlt64(), rhs.value), rhs.unit)
}

@JvmName("quantityVariableDivInt")
operator fun Quantity<AbstractVariableItem<*, *>>.div(rhs: Int): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(Flt64(rhs).reciprocal(), this.value), this.unit)
}

@JvmName("quantityVariableDivDouble")
operator fun Quantity<AbstractVariableItem<*, *>>.div(rhs: Double): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(Flt64(rhs).reciprocal(), this.value), this.unit)
}

@JvmName("quantityVariableDivRealNumber")
operator fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.div(rhs: T): Quantity<LinearMonomial<Flt64>> {
    return Quantity(LinearMonomial(rhs.toFlt64().reciprocal(), this.value), this.unit)
}

// quantity variable and quantity

@JvmName("quantityVariableTimesQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.times(rhs: Quantity<T>): Quantity<LinearMonomial<Flt64>> {
    val scalar: Flt64 = rhs.value.toFlt64()
    return Quantity(LinearMonomial(scalar, this.value), this.unit * rhs.unit)
}

@JvmName("quantityTimesQuantityVariable")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearMonomial<Flt64>> {
    val scalar: Flt64 = this.value.toFlt64()
    return Quantity(LinearMonomial(scalar, rhs.value), this.unit * rhs.unit)
}

@JvmName("quantityVariableDivQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.div(rhs: Quantity<T>): Quantity<LinearMonomial<Flt64>> {
    val scalar: Flt64 = Flt64.one / rhs.value.toFlt64()
    return Quantity(LinearMonomial(scalar, this.value), this.unit / rhs.unit)
}

// monomial and unit

operator fun LinearMonomial<Flt64>.times(rhs: PhysicalUnit): Quantity<LinearMonomial<Flt64>> {
    return Quantity(this, rhs)
}

// monomial and quantity

@JvmName("quantityTimesMonomial")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: LinearMonomial<Flt64>): Quantity<LinearMonomial<Flt64>> {
    val scalar: Flt64 = this.value.toFlt64()
    return Quantity(LinearMonomial(scalar * rhs.coefficient, rhs.symbol), this.unit)
}

@JvmName("monomialTimesQuantity")
operator fun <T : RealNumber<T>> LinearMonomial<Flt64>.times(rhs: Quantity<T>): Quantity<LinearMonomial<Flt64>> {
    val scalar: Flt64 = rhs.value.toFlt64()
    return Quantity(LinearMonomial(this.coefficient * scalar, this.symbol), rhs.unit)
}

@JvmName("monomialDivQuantity")
operator fun <T : RealNumber<T>> LinearMonomial<Flt64>.div(rhs: Quantity<T>): Quantity<LinearMonomial<Flt64>> {
    val scalar: Flt64 = rhs.value.toFlt64()
    return Quantity(LinearMonomial(this.coefficient / scalar, this.symbol), rhs.unit)
}

// quantity monomial and quantity

@JvmName("quantityTimesQuantityMonomial")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: Quantity<LinearMonomial<Flt64>>): Quantity<LinearMonomial<Flt64>> {
    val scalar: Flt64 = this.value.toFlt64()
    return Quantity(LinearMonomial(scalar * rhs.value.coefficient, rhs.value.symbol), this.unit * rhs.unit)
}

@JvmName("quantityMonomialTimesQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearMonomial<Flt64>>.times(rhs: Quantity<T>): Quantity<LinearMonomial<Flt64>> {
    val scalar: Flt64 = rhs.value.toFlt64()
    return Quantity(LinearMonomial(this.value.coefficient * scalar, this.value.symbol), this.unit * rhs.unit)
}

@JvmName("quantityMonomialDivQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearMonomial<Flt64>>.div(rhs: Quantity<T>): Quantity<LinearMonomial<Flt64>> {
    val scalar: Flt64 = rhs.value.toFlt64()
    return Quantity(LinearMonomial(this.value.coefficient / scalar, this.value.symbol), this.unit / rhs.unit)
}

// variable to polynomial (plus/minus with constants)

operator fun AbstractVariableItem<*, *>.plus(rhs: Int): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64(rhs))
}

operator fun AbstractVariableItem<*, *>.plus(rhs: Double): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64(rhs))
}

operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.plus(rhs: T): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), rhs.toFlt64())
}

operator fun AbstractVariableItem<*, *>.minus(rhs: Int): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), -Flt64(rhs))
}

operator fun AbstractVariableItem<*, *>.minus(rhs: Double): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), -Flt64(rhs))
}

operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.minus(rhs: T): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), -rhs.toFlt64())
}

operator fun Int.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, rhs)), Flt64(this))
}

operator fun Double.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, rhs)), Flt64(this))
}

operator fun <T : RealNumber<T>> T.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, rhs)), this.toFlt64())
}

operator fun Int.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(-Flt64.one, rhs)), Flt64(this))
}

operator fun Double.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(-Flt64.one, rhs)), Flt64(this))
}

operator fun <T : RealNumber<T>> T.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(-Flt64.one, rhs)), this.toFlt64())
}

// variable and variable

operator fun AbstractVariableItem<*, *>.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this), LinearMonomial(Flt64.one, rhs)), Flt64.zero)
}

operator fun AbstractVariableItem<*, *>.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this), LinearMonomial(-Flt64.one, rhs)), Flt64.zero)
}

// quantity variable and quantity

@JvmName("quantityVariablePlusQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.plus(rhs: Quantity<T>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityPlusQuantityVariable")
operator fun <T : RealNumber<T>> Quantity<T>.plus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityVariableMinusQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.minus(rhs: Quantity<T>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityMinusQuantityVariable")
operator fun <T : RealNumber<T>> Quantity<T>.minus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial<Flt64>> {
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

// quantity variable and quantity variable

@JvmName("quantityVariablePlusQuantityVariable")
operator fun Quantity<AbstractVariableItem<*, *>>.plus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityVariableMinusQuantityVariable")
operator fun Quantity<AbstractVariableItem<*, *>>.minus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial<Flt64>> {
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

// variable to quantity polynomial

@JvmName("sumVariables")
fun sum(
    items: Iterable<AbstractVariableItem<*, *>>,
    ctor: (AbstractVariableItem<*, *>) -> LinearMonomial<Flt64> = { LinearMonomial(Flt64.one, it) }
): LinearPolynomial<Flt64> {
    val monomials = ArrayList<LinearMonomial<Flt64>>()
    for (item in items) {
        monomials.add(ctor(item))
    }
    return LinearPolynomial(monomials = monomials, constant = Flt64.zero)
}

fun <T> sumVars(
    objs: Iterable<T>,
    extractor: (T) -> AbstractVariableItem<*, *>?
): LinearPolynomial<Flt64> {
    return sum(objs.mapNotNull(extractor))
}
