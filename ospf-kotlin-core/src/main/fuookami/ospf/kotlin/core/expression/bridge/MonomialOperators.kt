package fuookami.ospf.kotlin.core.expression.bridge

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.unaryMinus
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.to
import fuookami.ospf.kotlin.quantities.quantity.toFlt64

// monomial and constant

operator fun Int.times(rhs: LinearMonomial<Flt64>): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this) * rhs.coefficient, rhs.symbol)
}

operator fun Double.times(rhs: LinearMonomial<Flt64>): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this) * rhs.coefficient, rhs.symbol)
}

operator fun <T : RealNumber<T>> T.times(rhs: LinearMonomial<Flt64>): LinearMonomial<Flt64> {
    return LinearMonomial(this.toFlt64() * rhs.coefficient, rhs.symbol)
}

// monomial and monomial

operator fun LinearMonomial<Flt64>.plus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this, rhs), Flt64.zero)
}

operator fun LinearMonomial<Flt64>.minus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this, -rhs), Flt64.zero)
}

// monomial and variable

operator fun LinearMonomial<Flt64>.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this, LinearMonomial(Flt64.one, rhs)), Flt64.zero)
}

operator fun LinearMonomial<Flt64>.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this, LinearMonomial(-Flt64.one, rhs)), Flt64.zero)
}

operator fun AbstractVariableItem<*, *>.plus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this), rhs), Flt64.zero)
}

operator fun AbstractVariableItem<*, *>.minus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this), -rhs), Flt64.zero)
}

// monomial and symbol

operator fun LinearMonomial<Flt64>.plus(rhs: LinearIntermediateSymbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this, LinearMonomial(Flt64.one, rhs)), Flt64.zero)
}

operator fun LinearMonomial<Flt64>.minus(rhs: LinearIntermediateSymbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this, LinearMonomial(-Flt64.one, rhs)), Flt64.zero)
}

operator fun LinearIntermediateSymbol.plus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this), rhs), Flt64.zero)
}

operator fun LinearIntermediateSymbol.minus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this), -rhs), Flt64.zero)
}

// monomial plus/minus constant

operator fun LinearMonomial<Flt64>.plus(rhs: Int): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this), Flt64(rhs))
}

operator fun LinearMonomial<Flt64>.plus(rhs: Double): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this), Flt64(rhs))
}

operator fun <T : RealNumber<T>> LinearMonomial<Flt64>.plus(rhs: T): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this), rhs.toFlt64())
}

operator fun LinearMonomial<Flt64>.minus(rhs: Int): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this), -Flt64(rhs))
}

operator fun LinearMonomial<Flt64>.minus(rhs: Double): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this), -Flt64(rhs))
}

operator fun <T : RealNumber<T>> LinearMonomial<Flt64>.minus(rhs: T): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this), -rhs.toFlt64())
}

operator fun Int.plus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(rhs), Flt64(this))
}

operator fun Double.plus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(rhs), Flt64(this))
}

operator fun <T : RealNumber<T>> T.plus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(rhs), this.toFlt64())
}

operator fun Int.minus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(-rhs), Flt64(this))
}

operator fun Double.minus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(-rhs), Flt64(this))
}

operator fun <T : RealNumber<T>> T.minus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(-rhs), this.toFlt64())
}

// quantity monomial and quantity

@JvmName("quantityMonomialPlusQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearMonomial<Flt64>>.plus(rhs: Quantity<T>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            val qtyFlt64: Quantity<Flt64> = rhs.toFlt64()
            val converted: Quantity<Flt64> = qtyFlt64.to(this.unit)!!
            Quantity(LinearPolynomial(listOf(this.value), converted.value), this.unit)
        } else {
            val converted: Quantity<LinearMonomial<Flt64>> = this.to(rhs.unit)!!
            Quantity(LinearPolynomial(listOf(converted.value), rhs.toFlt64().value), rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPlusQuantityMonomial")
operator fun <T : RealNumber<T>> Quantity<T>.plus(rhs: Quantity<LinearMonomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            val converted: Quantity<LinearMonomial<Flt64>> = rhs.to(this.unit)!!
            Quantity(LinearPolynomial(listOf(converted.value), this.toFlt64().value), this.unit)
        } else {
            val qtyFlt64: Quantity<Flt64> = this.toFlt64()
            val converted: Quantity<Flt64> = qtyFlt64.to(rhs.unit)!!
            Quantity(LinearPolynomial(listOf(rhs.value), converted.value), rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMonomialMinusQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearMonomial<Flt64>>.minus(rhs: Quantity<T>): Quantity<LinearPolynomial<Flt64>> {
    return if (this.unit == rhs.unit) {
        Quantity(LinearPolynomial(listOf(this.value), -rhs.toFlt64().value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            val qtyFlt64: Quantity<Flt64> = rhs.toFlt64()
            val converted: Quantity<Flt64> = qtyFlt64.to(this.unit)!!
            Quantity(LinearPolynomial(listOf(this.value), -converted.value), this.unit)
        } else {
            val converted: Quantity<LinearMonomial<Flt64>> = this.to(rhs.unit)!!
            Quantity(LinearPolynomial(listOf(converted.value), -rhs.toFlt64().value), rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMinusQuantityMonomial")
operator fun <T : RealNumber<T>> Quantity<T>.minus(rhs: Quantity<LinearMonomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

// quantity monomial and quantity variable

@JvmName("quantityMonomialPlusQuantityVariable")
operator fun Quantity<LinearMonomial<Flt64>>.plus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityVariablePlusQuantityMonomial")
operator fun Quantity<AbstractVariableItem<*, *>>.plus(rhs: Quantity<LinearMonomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityMonomialMinusQuantityVariable")
operator fun Quantity<LinearMonomial<Flt64>>.minus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityVariableMinusQuantityMonomial")
operator fun Quantity<AbstractVariableItem<*, *>>.minus(rhs: Quantity<LinearMonomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

// quantity monomial and quantity symbol

@JvmName("quantityMonomialPlusQuantitySymbol")
operator fun Quantity<LinearMonomial<Flt64>>.plus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantitySymbolPlusQuantityMnomial")
operator fun Quantity<LinearIntermediateSymbol>.plus(rhs: Quantity<LinearMonomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityMonomialMinusQuantitySymbol")
operator fun Quantity<LinearMonomial<Flt64>>.minus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantitySymbolMinusQuantityMonomial")
operator fun Quantity<LinearIntermediateSymbol>.minus(rhs: Quantity<LinearMonomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

// quantity monomial and quantity monomial

@JvmName("quantityMonomialPlusQuantityMonomial")
operator fun Quantity<LinearMonomial<Flt64>>.plus(rhs: Quantity<LinearMonomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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

@JvmName("quantityMonomialMinusQuantityMonomial")
operator fun Quantity<LinearMonomial<Flt64>>.minus(rhs: Quantity<LinearMonomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
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
