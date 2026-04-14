package fuookami.ospf.kotlin.core.expression.bridge

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.unaryMinus
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.divAssign
import fuookami.ospf.kotlin.math.symbol.polynomial.timesAssign
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.to
import fuookami.ospf.kotlin.quantities.quantity.toFlt64

// MutableLinearPolynomial<Flt64> bridge operators

operator fun MutableLinearPolynomial<Flt64>.plusAssign(rhs: AbstractVariableItem<*, *>) {
    addMonomial(LinearMonomial(Flt64.one, rhs))
}

@JvmName("plusAssignVariables")
operator fun MutableLinearPolynomial<Flt64>.plusAssign(rhs: Iterable<AbstractVariableItem<*, *>>) {
    rhs.forEach { addMonomial(LinearMonomial(Flt64.one, it)) }
}

operator fun MutableLinearPolynomial<Flt64>.plusAssign(rhs: LinearIntermediateSymbol) {
    addMonomial(LinearMonomial(Flt64.one, rhs))
}

@JvmName("plusAssignSymbols")
operator fun MutableLinearPolynomial<Flt64>.plusAssign(rhs: Iterable<LinearIntermediateSymbol>) {
    rhs.forEach { addMonomial(LinearMonomial(Flt64.one, it)) }
}

operator fun MutableLinearPolynomial<Flt64>.minusAssign(rhs: AbstractVariableItem<*, *>) {
    addMonomial(LinearMonomial(-Flt64.one, rhs))
}

@JvmName("minusAssignVariables")
operator fun MutableLinearPolynomial<Flt64>.minusAssign(rhs: Iterable<AbstractVariableItem<*, *>>) {
    rhs.forEach { addMonomial(LinearMonomial(-Flt64.one, it)) }
}

operator fun MutableLinearPolynomial<Flt64>.minusAssign(rhs: LinearIntermediateSymbol) {
    addMonomial(LinearMonomial(-Flt64.one, rhs))
}

@JvmName("minusAssignSymbols")
operator fun MutableLinearPolynomial<Flt64>.minusAssign(rhs: Iterable<LinearIntermediateSymbol>) {
    rhs.forEach { addMonomial(LinearMonomial(-Flt64.one, it)) }
}

// Quantity MutableLinearPolynomial operators - use addMonomial/addConstant directly

@JvmName("quantityPolynomialPlusAssignQuantityVariable")
fun Quantity<MutableLinearPolynomial<Flt64>>.plusAssign(rhs: Quantity<AbstractVariableItem<*, *>>) {
    val converted: Quantity<LinearMonomial<Flt64>> = rhs.to(this.unit) ?: return
    value.addMonomial(converted.value)
}

@JvmName("quantityPolynomialPlusAssignQuantityVariables")
fun Quantity<MutableLinearPolynomial<Flt64>>.plusAssign(rhs: Iterable<Quantity<AbstractVariableItem<*, *>>>) {
    for (item in rhs) {
        val converted: Quantity<LinearMonomial<Flt64>> = item.to(this.unit) ?: continue
        value.addMonomial(converted.value)
    }
}

@JvmName("quantityPolynomialPlusAssignQuantitySymbol")
fun Quantity<MutableLinearPolynomial<Flt64>>.plusAssign(rhs: Quantity<LinearIntermediateSymbol>) {
    val converted: Quantity<LinearMonomial<Flt64>> = rhs.to(this.unit) ?: return
    value.addMonomial(converted.value)
}

@JvmName("quantityPolynomialPlusAssignQuantitySymbols")
fun Quantity<MutableLinearPolynomial<Flt64>>.plusAssign(rhs: Iterable<Quantity<LinearIntermediateSymbol>>) {
    for (item in rhs) {
        val converted: Quantity<LinearMonomial<Flt64>> = item.to(this.unit) ?: continue
        value.addMonomial(converted.value)
    }
}

@JvmName("quantityPolynomialPlusAssignQuantityMonomial")
fun Quantity<MutableLinearPolynomial<Flt64>>.plusAssign(rhs: Quantity<LinearMonomial<Flt64>>) {
    val converted: Quantity<LinearMonomial<Flt64>> = rhs.to(this.unit) ?: return
    value.addMonomial(converted.value)
}

@JvmName("quantityPolynomialPlusAssignQuantityPolynomial")
fun Quantity<MutableLinearPolynomial<Flt64>>.plusAssign(rhs: Quantity<LinearPolynomial<Flt64>>) {
    val converted: Quantity<LinearPolynomial<Flt64>> = rhs.to(this.unit) ?: return
    converted.value.monomials.forEach { value.addMonomial(it) }
    value.addConstant(converted.value.constant)
}

@JvmName("quantityPolynomialPlusAssignQuantity")
fun <V : RealNumber<V>> Quantity<MutableLinearPolynomial<Flt64>>.plusAssign(rhs: Quantity<V>) {
    val qtyFlt64: Quantity<Flt64> = rhs.toFlt64()
    val converted: Quantity<Flt64> = qtyFlt64.to(this.unit) ?: return
    value.addConstant(converted.value)
}

@JvmName("quantityPolynomialMinusAssignQuantityVariable")
fun Quantity<MutableLinearPolynomial<Flt64>>.minusAssign(rhs: Quantity<AbstractVariableItem<*, *>>) {
    val converted: Quantity<LinearMonomial<Flt64>> = rhs.to(this.unit) ?: return
    value.addMonomial(-converted.value)
}

@JvmName("quantityPolynomialMinusAssignQuantityVariables")
fun Quantity<MutableLinearPolynomial<Flt64>>.minusAssign(rhs: Iterable<Quantity<AbstractVariableItem<*, *>>>) {
    for (item in rhs) {
        val converted: Quantity<LinearMonomial<Flt64>> = item.to(this.unit) ?: continue
        value.addMonomial(-converted.value)
    }
}

@JvmName("quantityPolynomialMinusAssignQuantitySymbol")
fun Quantity<MutableLinearPolynomial<Flt64>>.minusAssign(rhs: Quantity<LinearIntermediateSymbol>) {
    val converted: Quantity<LinearMonomial<Flt64>> = rhs.to(this.unit) ?: return
    value.addMonomial(-converted.value)
}

@JvmName("quantityPolynomialMinusAssignQuantitySymbols")
fun Quantity<MutableLinearPolynomial<Flt64>>.minusAssign(rhs: Iterable<Quantity<LinearIntermediateSymbol>>) {
    for (item in rhs) {
        val converted: Quantity<LinearMonomial<Flt64>> = item.to(this.unit) ?: continue
        value.addMonomial(-converted.value)
    }
}

@JvmName("quantityPolynomialMinusAssignQuantityMonomial")
fun Quantity<MutableLinearPolynomial<Flt64>>.minusAssign(rhs: Quantity<LinearMonomial<Flt64>>) {
    val converted: Quantity<LinearMonomial<Flt64>> = rhs.to(this.unit) ?: return
    value.addMonomial(-converted.value)
}

@JvmName("quantityPolynomialMinusAssignQuantityPolynomial")
fun Quantity<MutableLinearPolynomial<Flt64>>.minusAssign(rhs: Quantity<LinearPolynomial<Flt64>>) {
    val converted: Quantity<LinearPolynomial<Flt64>> = rhs.to(this.unit) ?: return
    converted.value.monomials.forEach { value.addMonomial(-it) }
    value.addConstant(-converted.value.constant)
}

@JvmName("quantityPolynomialMinusAssignQuantity")
fun <V : RealNumber<V>> Quantity<MutableLinearPolynomial<Flt64>>.minusAssign(rhs: Quantity<V>) {
    val qtyFlt64: Quantity<Flt64> = rhs.toFlt64()
    val converted: Quantity<Flt64> = qtyFlt64.to(this.unit) ?: return
    value.addConstant(-converted.value)
}

@JvmName("quantityPolynomialTimesAssign")
fun <V : RealNumber<V>> Quantity<MutableLinearPolynomial<Flt64>>.timesAssign(rhs: V) {
    val scalar: Flt64 = rhs.toFlt64()
    value.timesAssign(scalar)
}

@JvmName("quantityPolynomialDivAssign")
fun <V : RealNumber<V>> Quantity<MutableLinearPolynomial<Flt64>>.divAssign(rhs: V) {
    val scalar: Flt64 = rhs.toFlt64()
    value.divAssign(scalar)
}
