package fuookami.ospf.kotlin.core.frontend.expression.bridge

import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.unaryMinus
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.unaryMinus
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.to
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit

// quantity variable conversion

@JvmName("quantityVariableConversion")
fun Quantity<AbstractVariableItem<*, *>>.to(targetUnit: PhysicalUnit): Quantity<LinearMonomial<Flt64>>? {
    return unit.to(targetUnit)?.let {
        Quantity(it.value * this.value, this.unit)
    }
}

// quantity symbol conversion

@JvmName("quantitySymbolConversion")
fun Quantity<LinearIntermediateSymbol>.to(targetUnit: PhysicalUnit): Quantity<LinearMonomial<Flt64>>? {
    return unit.to(targetUnit)?.let {
        Quantity(it.value * this.value, this.unit)
    }
}

// quantity monomial conversion

@JvmName("quantityMonomialConversion")
fun Quantity<LinearMonomial<Flt64>>.to(targetUnit: PhysicalUnit): Quantity<LinearMonomial<Flt64>>? {
    return unit.to(targetUnit)?.let {
        Quantity(it.value * this.value, this.unit)
    }
}

// quantity polynomial conversion

fun Quantity<LinearPolynomial<Flt64>>.to(targetUnit: PhysicalUnit): Quantity<LinearPolynomial<Flt64>>? {
    return this.unit.to(targetUnit)?.let {
        Quantity(it.value * this.value, targetUnit)
    }
}

// unary minus for quantity types (monomial and polynomial only - variable and symbol are in their respective files)

@JvmName("unaryMinusQuantityMonomial")
operator fun Quantity<LinearMonomial<Flt64>>.unaryMinus(): Quantity<LinearMonomial<Flt64>> {
    return Quantity(-this.value, this.unit)
}

@JvmName("unaryMinusQuantityPolynomial")
operator fun Quantity<LinearPolynomial<Flt64>>.unaryMinus(): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(-this.value, this.unit)
}
