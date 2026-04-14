package fuookami.ospf.kotlin.core.expression.bridge

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.unaryMinus
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.times
import fuookami.ospf.kotlin.math.symbol.polynomial.toMutable
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.to
import fuookami.ospf.kotlin.quantities.quantity.toFlt64
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.div
import fuookami.ospf.kotlin.quantities.unit.times

// polynomial plus monomial

operator fun LinearPolynomial<Flt64>.plus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials + rhs, constant)
}

operator fun LinearMonomial<Flt64>.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this) + rhs.monomials, rhs.constant)
}

operator fun LinearPolynomial<Flt64>.minus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials + (-rhs), constant)
}

operator fun LinearMonomial<Flt64>.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this) + rhs.monomials.map { -it }, -rhs.constant)
}

// polynomial plus variable

operator fun LinearPolynomial<Flt64>.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials + LinearMonomial(Flt64.one, rhs), constant)
}

operator fun LinearPolynomial<Flt64>.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials + LinearMonomial(-Flt64.one, rhs), constant)
}

operator fun AbstractVariableItem<*, *>.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)) + rhs.monomials, rhs.constant)
}

operator fun AbstractVariableItem<*, *>.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)) + rhs.monomials.map { -it }, -rhs.constant)
}

// polynomial plus symbol

operator fun LinearPolynomial<Flt64>.plus(rhs: LinearIntermediateSymbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials + LinearMonomial(Flt64.one, rhs), constant)
}

operator fun LinearPolynomial<Flt64>.minus(rhs: LinearIntermediateSymbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials + LinearMonomial(-Flt64.one, rhs), constant)
}

operator fun LinearIntermediateSymbol.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)) + rhs.monomials, rhs.constant)
}

operator fun LinearIntermediateSymbol.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)) + rhs.monomials.map { -it }, -rhs.constant)
}

// polynomial plus/minus polynomial

// polynomial scalar plus/minus

operator fun LinearPolynomial<Flt64>.plus(rhs: Flt64): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials, constant + rhs)
}

operator fun LinearPolynomial<Flt64>.minus(rhs: Flt64): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials, constant - rhs)
}

operator fun Flt64.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials, this + rhs.constant)
}

operator fun Flt64.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials.map { -it }, this - rhs.constant)
}

// polynomial div scalar

operator fun LinearPolynomial<Flt64>.div(rhs: Flt64): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials.map { LinearMonomial(it.coefficient / rhs, it.symbol) }, constant / rhs)
}

operator fun Int.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials, Flt64(this) + rhs.constant)
}

operator fun Double.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials, Flt64(this) + rhs.constant)
}

operator fun <T : RealNumber<T>> T.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials, this.toFlt64() + rhs.constant)
}

operator fun Int.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials.map { -it }, Flt64(this) - rhs.constant)
}

operator fun Double.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials.map { -it }, Flt64(this) - rhs.constant)
}

operator fun <T : RealNumber<T>> T.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials.map { -it }, this.toFlt64() - rhs.constant)
}

operator fun Int.times(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials.map { this * it }, Flt64(this) * rhs.constant)
}

operator fun Double.times(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials.map { this * it }, Flt64(this) * rhs.constant)
}

operator fun <T : RealNumber<T>> T.times(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials.map { this * it }, this.toFlt64() * rhs.constant)
}

// polynomial times unit

operator fun LinearPolynomial<Flt64>.times(rhs: PhysicalUnit): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(this, rhs)
}

// polynomial and quantity

@JvmName("polynomialTimesQuantity")
operator fun <T : RealNumber<T>> LinearPolynomial<Flt64>.times(rhs: Quantity<T>): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(this * rhs.value.toFlt64(), rhs.unit)
}

@JvmName("quantityTimesPolynomial")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: LinearPolynomial<Flt64>): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(this.value.toFlt64() * rhs, this.unit)
}

@JvmName("polynomialDivQuantity")
operator fun <T : RealNumber<T>> LinearPolynomial<Flt64>.div(rhs: Quantity<T>): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(this / rhs.value.toFlt64(), rhs.unit)
}

// quantity polynomial and constant

@JvmName("quantityPolynomialTimesInt")
operator fun Quantity<LinearPolynomial<Flt64>>.times(rhs: Int): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(this.value * Flt64(rhs), this.unit)
}

@JvmName("quantityPolynomialTimesDouble")
operator fun Quantity<LinearPolynomial<Flt64>>.times(rhs: Double): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(this.value * Flt64(rhs), this.unit)
}

@JvmName("quantityPolynomialTimesRealNumber")
operator fun <T : RealNumber<T>> Quantity<LinearPolynomial<Flt64>>.times(rhs: T): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(this.value * rhs.toFlt64(), this.unit)
}

@JvmName("intTimesQuantityPolynomial")
operator fun Int.times(rhs: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(Flt64(this) * rhs.value, rhs.unit)
}

@JvmName("doubleTimesQuantityPolynomial")
operator fun Double.times(rhs: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(Flt64(this) * rhs.value, rhs.unit)
}

@JvmName("realNumberTimesQuantityPolynomial")
operator fun <T : RealNumber<T>> T.times(rhs: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(this.toFlt64() * rhs.value, rhs.unit)
}

@JvmName("quantityPolynomialDivInt")
operator fun Quantity<LinearPolynomial<Flt64>>.div(rhs: Int): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(this.value / Flt64(rhs), this.unit)
}

@JvmName("quantityPolynomialDivDouble")
operator fun Quantity<LinearPolynomial<Flt64>>.div(rhs: Double): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(this.value / Flt64(rhs), this.unit)
}

@JvmName("quantityPolynomialDivRealNumber")
operator fun <T : RealNumber<T>> Quantity<LinearPolynomial<Flt64>>.div(rhs: T): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(this.value / rhs.toFlt64(), this.unit)
}

// quantity polynomial and quantity

@JvmName("quantityPolynomialPlusQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearPolynomial<Flt64>>.plus(rhs: Quantity<T>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(LinearPolynomial(this.value.monomials, this.value.constant + rhs.value.toFlt64()), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            val converted: Quantity<Flt64> = rhs.toFlt64().to(this.unit)!!
            Quantity(LinearPolynomial(this.value.monomials, this.value.constant + converted.value), this.unit)
        } else {
            val converted: Quantity<LinearPolynomial<Flt64>> = this.to(rhs.unit)!!
            Quantity(LinearPolynomial(converted.value.monomials, converted.value.constant + rhs.value.toFlt64()), rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPlusQuantityPolynomial")
operator fun <T : RealNumber<T>> Quantity<T>.plus(rhs: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(LinearPolynomial(rhs.value.monomials, this.value.toFlt64() + rhs.value.constant), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            val converted: Quantity<LinearPolynomial<Flt64>> = rhs.to(this.unit)!!
            Quantity(LinearPolynomial(converted.value.monomials, this.value.toFlt64() + converted.value.constant), this.unit)
        } else {
            val qtyFlt64: Quantity<Flt64> = this.toFlt64()
            val converted: Quantity<Flt64> = qtyFlt64.to(rhs.unit)!!
            Quantity(LinearPolynomial(rhs.value.monomials, converted.value + rhs.value.constant), rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPolynomialMinusQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearPolynomial<Flt64>>.minus(rhs: Quantity<T>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(LinearPolynomial(this.value.monomials, this.value.constant - rhs.value.toFlt64()), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            val converted: Quantity<Flt64> = rhs.toFlt64().to(this.unit)!!
            Quantity(LinearPolynomial(this.value.monomials, this.value.constant - converted.value), this.unit)
        } else {
            val converted: Quantity<LinearPolynomial<Flt64>> = this.to(rhs.unit)!!
            Quantity(LinearPolynomial(converted.value.monomials.map { -it }, converted.value.constant - rhs.value.toFlt64()), rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMinusQuantityPolynomial")
operator fun <T : RealNumber<T>> Quantity<T>.minus(rhs: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityPolynomialTimesQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearPolynomial<Flt64>>.times(rhs: Quantity<T>): Quantity<LinearPolynomial<Flt64>> {
    val scalar: Flt64 = rhs.value.toFlt64()
    val scaledMonomials = this.value.monomials.map { LinearMonomial(it.coefficient * scalar, it.symbol) }
    val scaledConstant = this.value.constant * scalar
    return Quantity(LinearPolynomial(scaledMonomials, scaledConstant), this.unit * rhs.unit)
}

@JvmName("quantityTimesQuantityPolynomial")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
    val scalar: Flt64 = this.value.toFlt64()
    val scaledMonomials = rhs.value.monomials.map { LinearMonomial(it.coefficient * scalar, it.symbol) }
    val scaledConstant = rhs.value.constant * scalar
    return Quantity(LinearPolynomial(scaledMonomials, scaledConstant), this.unit * rhs.unit)
}

@JvmName("quantityPolynomialDivQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearPolynomial<Flt64>>.div(rhs: Quantity<T>): Quantity<LinearPolynomial<Flt64>> {
    val scalar: Flt64 = rhs.value.toFlt64()
    val scaledMonomials = this.value.monomials.map { LinearMonomial(it.coefficient / scalar, it.symbol) }
    val scaledConstant = this.value.constant / scalar
    return Quantity(LinearPolynomial(scaledMonomials, scaledConstant), this.unit / rhs.unit)
}

// quantity polynomial and quantity variable

@JvmName("quantityPolynomialPlusQuantityVariable")
operator fun Quantity<LinearPolynomial<Flt64>>.plus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityVariablePlusQuantityPolynomial")
operator fun Quantity<AbstractVariableItem<*, *>>.plus(rhs: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityPolynomialMinusQuantityVariable")
operator fun Quantity<LinearPolynomial<Flt64>>.minus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityVariableMinusQuantityPolynomial")
operator fun Quantity<AbstractVariableItem<*, *>>.minus(rhs: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

// quantity polynomial and quantity symbol

@JvmName("quantityPolynomialPlusQuantitySymbol")
operator fun Quantity<LinearPolynomial<Flt64>>.plus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantitySymbolPlusQuantityPolynomial")
operator fun Quantity<LinearIntermediateSymbol>.plus(rhs: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityPolynomialMinusQuantitySymbol")
operator fun Quantity<LinearPolynomial<Flt64>>.minus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantitySymbolMinusQuantityPolynomial")
operator fun Quantity<LinearIntermediateSymbol>.minus(rhs: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

// quantity polynomial and quantity monomial

@JvmName("quantityPolynomialPlusQuantityMonomial")
operator fun Quantity<LinearPolynomial<Flt64>>.plus(rhs: Quantity<LinearMonomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityMonomialPlusQuantityPolynomial")
operator fun Quantity<LinearMonomial<Flt64>>.plus(rhs: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityPolynomialMinusQuantityMonomial")
operator fun Quantity<LinearPolynomial<Flt64>>.minus(rhs: Quantity<LinearMonomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityMonomialMinusQuantityPolynomial")
operator fun Quantity<LinearMonomial<Flt64>>.minus(rhs: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

// quantity polynomial and quantity polynomial

@JvmName("quantityPolynomialPlusQuantityPolynomial")
operator fun Quantity<LinearPolynomial<Flt64>>.plus(rhs: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        val merged = this.value.monomials + rhs.value.monomials
        val mergedConstant = this.value.constant + rhs.value.constant
        Quantity(LinearPolynomial(merged, mergedConstant), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            val converted: Quantity<LinearPolynomial<Flt64>> = rhs.to(this.unit)!!
            val merged = this.value.monomials + converted.value.monomials
            val mergedConstant = this.value.constant + converted.value.constant
            Quantity(LinearPolynomial(merged, mergedConstant), this.unit)
        } else {
            val converted: Quantity<LinearPolynomial<Flt64>> = this.to(rhs.unit)!!
            val merged = converted.value.monomials + rhs.value.monomials
            val mergedConstant = converted.value.constant + rhs.value.constant
            Quantity(LinearPolynomial(merged, mergedConstant), rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPolynomialMinusQuantityPolynomial")
operator fun Quantity<LinearPolynomial<Flt64>>.minus(rhs: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        val merged = this.value.monomials + rhs.value.monomials.map { -it }
        val mergedConstant = this.value.constant - rhs.value.constant
        Quantity(LinearPolynomial(merged, mergedConstant), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            val converted: Quantity<LinearPolynomial<Flt64>> = rhs.to(this.unit)!!
            val merged = this.value.monomials + converted.value.monomials.map { -it }
            val mergedConstant = this.value.constant - converted.value.constant
            Quantity(LinearPolynomial(merged, mergedConstant), this.unit)
        } else {
            val converted: Quantity<LinearPolynomial<Flt64>> = this.to(rhs.unit)!!
            val merged = converted.value.monomials + rhs.value.monomials.map { -it }
            val mergedConstant = converted.value.constant - rhs.value.constant
            Quantity(LinearPolynomial(merged, mergedConstant), rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// toMutable helper

fun LinearPolynomial<Flt64>.toMutableLinearPolynomial(): MutableLinearPolynomial<Flt64> {
    return this.toMutable()
}
