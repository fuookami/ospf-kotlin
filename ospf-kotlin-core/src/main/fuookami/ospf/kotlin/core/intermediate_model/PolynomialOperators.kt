@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomialCell
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomialCell
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomialSymbolUnit
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import kotlin.collections.sumOf
import kotlin.collections.ArrayList
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomialSymbol
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*

// Re-export expression.monomial and expression.polynomial operators for framework migration.
// Framework code should import from intermediate_model instead of core.expression.
// Note: Kotlin does not support function re-export, so we re-implement the same logic.
// These are deprecated and will be removed when expression directory is deleted (E7).

// --- Monomial operators for AbstractVariableItem ---

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.unaryMinus(): LinearMonomial {
    return -Flt64.one * this
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.times(rhs: Int): LinearMonomial {
    return LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.times(rhs: Double): LinearMonomial {
    return LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.times(rhs: T): LinearMonomial {
    return LinearMonomial(rhs.toFlt64(), LinearMonomialSymbol(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Int.times(rhs: AbstractVariableItem<*, *>): LinearMonomial {
    return LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Double.times(rhs: AbstractVariableItem<*, *>): LinearMonomial {
    return LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> T.times(rhs: AbstractVariableItem<*, *>): LinearMonomial {
    return LinearMonomial(this.toFlt64(), LinearMonomialSymbol(rhs))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.div(rhs: Int): LinearMonomial {
    return LinearMonomial(Flt64(rhs).reciprocal(), LinearMonomialSymbol(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.div(rhs: Double): LinearMonomial {
    return LinearMonomial(Flt64(rhs).reciprocal(), LinearMonomialSymbol(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.div(rhs: T): LinearMonomial {
    return LinearMonomial(rhs.toFlt64().reciprocal(), LinearMonomialSymbol(this))
}

// --- Monomial operators for LinearIntermediateSymbol ---

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.unaryMinus(): LinearMonomial {
    return -Flt64.one * this
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.times(rhs: Int): LinearMonomial {
    return LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.times(rhs: Double): LinearMonomial {
    return LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> LinearIntermediateSymbol.times(rhs: T): LinearMonomial {
    return LinearMonomial(rhs.toFlt64(), LinearMonomialSymbol(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Int.times(rhs: LinearIntermediateSymbol): LinearMonomial {
    return LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Double.times(rhs: LinearIntermediateSymbol): LinearMonomial {
    return LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> T.times(rhs: LinearIntermediateSymbol): LinearMonomial {
    return LinearMonomial(this.toFlt64(), LinearMonomialSymbol(rhs))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.div(rhs: Int): LinearMonomial {
    return LinearMonomial(Flt64(rhs).reciprocal(), LinearMonomialSymbol(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.div(rhs: Double): LinearMonomial {
    return LinearMonomial(Flt64(rhs).reciprocal(), LinearMonomialSymbol(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> LinearIntermediateSymbol.div(rhs: T): LinearMonomial {
    return LinearMonomial(rhs.toFlt64().reciprocal(), LinearMonomialSymbol(this))
}

// --- Quantity monomial operators ---

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.times(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.value.toFlt64(), LinearMonomialSymbol(this)), rhs.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: AbstractVariableItem<*, *>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(this.value.toFlt64(), LinearMonomialSymbol(rhs)), this.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("quantityVariableTimesInt")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: Int): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this.value)), this.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("quantityVariableTimesDouble")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: Double): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this.value)), this.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("quantityVariableTimesRealNumber")
operator fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.times(rhs: T): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.toFlt64(), LinearMonomialSymbol(this.value)), this.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("intTimesQuantityVariable")
operator fun Int.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs.value)), rhs.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("doubleTimesQuantityVariable")
operator fun Double.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs.value)), rhs.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("realNumberTimesQuantityVariable")
operator fun <T : RealNumber<T>> T.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(this.toFlt64(), LinearMonomialSymbol(rhs.value)), rhs.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("quantitySymbolTimesInt")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: Int): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this.value)), this.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("quantitySymbolTimesDouble")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: Double): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this.value)), this.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("quantitySymbolTimesRealNumber")
operator fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.times(rhs: T): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.toFlt64(), LinearMonomialSymbol(this.value)), this.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("intTimesQuantitySymbol")
operator fun Int.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs.value)), rhs.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("doubleTimesQuantitySymbol")
operator fun Double.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs.value)), rhs.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("realNumberTimesQuantitySymbol")
operator fun <T : RealNumber<T>> T.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(this.toFlt64(), LinearMonomialSymbol(rhs.value)), rhs.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("quantityVariableTimesQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.times(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.value.toFlt64(), LinearMonomialSymbol(this.value)), this.unit * rhs.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("quantityTimesQuantityVariable")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(this.value.toFlt64(), LinearMonomialSymbol(rhs.value)), this.unit * rhs.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("quantitySymbolTimesQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.times(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.value.toFlt64(), LinearMonomialSymbol(this.value)), this.unit * rhs.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("quantityTimesQuantitySymbol")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(this.value.toFlt64(), LinearMonomialSymbol(rhs.value)), this.unit * rhs.unit)
}

// --- Quadratic monomial operators (variable × variable) ---

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.times(rhs: AbstractVariableItem<*, *>): QuadraticMonomial {
    return QuadraticMonomial(this, rhs)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("quantityVariableTimesVariable")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: AbstractVariableItem<*, *>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.value, rhs), this.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("variableTimesQuantityVariable")
operator fun AbstractVariableItem<*, *>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this, rhs.value), rhs.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("quantityVariableTimesQuantityVariable")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.value, rhs.value), this.unit * rhs.unit)
}

// --- Quadratic monomial scalar operators ---

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Int.times(rhs: QuadraticMonomial): QuadraticMonomial {
    return QuadraticMonomial(Flt64(this) * rhs.coefficient, rhs.symbol)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Double.times(rhs: QuadraticMonomial): QuadraticMonomial {
    return QuadraticMonomial(Flt64(this) * rhs.coefficient, rhs.symbol)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> T.times(rhs: QuadraticMonomial): QuadraticMonomial {
    return QuadraticMonomial(this.toFlt64() * rhs.coefficient, rhs.symbol)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("quantityTimesQuadraticMonomial")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: QuadraticMonomial): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.value.toFlt64() * rhs.coefficient, rhs.symbol), this.unit)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
@JvmName("quadraticMonomialTimesQuantity")
operator fun <T : RealNumber<T>> QuadraticMonomial.times(rhs: Quantity<T>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.coefficient * rhs.value.toFlt64(), this.symbol), rhs.unit)
}

// --- LinearPolynomial operators (AbstractLinearPolynomial) ---

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractLinearPolynomial<*>.plus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(this.monomials + rhs.monomials, this.constant + rhs.constant)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractLinearPolynomial<*>.minus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(this.monomials + rhs.monomials.map { -it }, this.constant - rhs.constant)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractLinearPolynomial<*>.times(rhs: Int): LinearPolynomial {
    val f = Flt64(rhs)
    return LinearPolynomial(this.monomials.map { it.times(f) }, this.constant * f)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractLinearPolynomial<*>.times(rhs: Double): LinearPolynomial {
    val f = Flt64(rhs)
    return LinearPolynomial(this.monomials.map { it.times(f) }, this.constant * f)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.times(rhs: T): LinearPolynomial {
    val f = rhs.toFlt64()
    return LinearPolynomial(this.monomials.map { it.times(f) }, this.constant * f)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Int.times(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    val f = Flt64(this)
    return LinearPolynomial(rhs.monomials.map { it.times(f) }, rhs.constant * f)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Double.times(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    val f = Flt64(this)
    return LinearPolynomial(rhs.monomials.map { it.times(f) }, rhs.constant * f)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> T.times(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    val f = this.toFlt64()
    return LinearPolynomial(rhs.monomials.map { it.times(f) }, rhs.constant * f)
}

// --- AbstractVariableItem polynomial operators ---

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.plus(rhs: Int): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))), Flt64(rhs))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.plus(rhs: Double): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))), Flt64(rhs))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.plus(rhs: T): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))), rhs.toFlt64())
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.minus(rhs: Int): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))), -Flt64(rhs))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.minus(rhs: Double): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))), -Flt64(rhs))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.minus(rhs: T): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))), -rhs.toFlt64())
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Int.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Double.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> T.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), this.toFlt64())
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Int.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(listOf(-LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Double.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(listOf(-LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> T.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(listOf(-LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), this.toFlt64())
}

// --- LinearIntermediateSymbol polynomial operators ---

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.plus(rhs: Int): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))), Flt64(rhs))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.plus(rhs: Double): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))), Flt64(rhs))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> LinearIntermediateSymbol.plus(rhs: T): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))), rhs.toFlt64())
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.minus(rhs: Int): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))), -Flt64(rhs))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.minus(rhs: Double): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))), -Flt64(rhs))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> LinearIntermediateSymbol.minus(rhs: T): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))), -rhs.toFlt64())
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Int.plus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Double.plus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> T.plus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), this.toFlt64())
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Int.minus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(listOf(-LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun Double.minus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(listOf(-LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64(this))
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> T.minus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(listOf(-LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), this.toFlt64())
}

// --- Cross-type symbol/variable polynomial operators ---

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this)), LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64.zero)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this)), -LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64.zero)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this)), LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64.zero)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this)), -LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64.zero)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.plus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this)), LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64.zero)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.minus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this)), -LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64.zero)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.plus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this)), LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64.zero)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.minus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this)), -LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), Flt64.zero)
}

// --- Variable/Symbol + Polynomial cross-type operators ---

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.plus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))) + rhs.monomials, Flt64.zero + rhs.constant)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractVariableItem<*, *>.minus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))) + rhs.monomials.map { -it }, Flt64.zero - rhs.constant)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractLinearPolynomial<*>.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(this.monomials + listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), this.constant + Flt64.zero)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractLinearPolynomial<*>.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(this.monomials + listOf(-LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), this.constant - Flt64.zero)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.plus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))) + rhs.monomials, Flt64.zero + rhs.constant)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun LinearIntermediateSymbol.minus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(this))) + rhs.monomials.map { -it }, Flt64.zero - rhs.constant)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractLinearPolynomial<*>.plus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(this.monomials + listOf(LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), this.constant + Flt64.zero)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractLinearPolynomial<*>.minus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(this.monomials + listOf(-LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs))), this.constant - Flt64.zero)
}

// --- RealNumber + Polynomial cross-type operators ---

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> T.plus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(rhs.monomials, this.toFlt64() + rhs.constant)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun <T : RealNumber<T>> T.minus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(rhs.monomials.map { -it }, this.toFlt64() - rhs.constant)
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractLinearPolynomial<*>.plus(rhs: RealNumber<*>): LinearPolynomial {
    return LinearPolynomial(this.monomials, this.constant + rhs.toFlt64())
}

@Deprecated("Will be removed in E7. Use intermediate_model operator imports instead.")
operator fun AbstractLinearPolynomial<*>.minus(rhs: RealNumber<*>): LinearPolynomial {
    return LinearPolynomial(this.monomials, this.constant - rhs.toFlt64())
}

/**
 * Transitional typealiases for framework code migrating off core.expression.
 *
 * These allow framework code to import types from intermediate_model instead of expression.
 * Will be removed when expression directory is deleted (E7).
 */
// Type re-exports removed - use direct imports from intermediate_model package instead.
// e.g. import fuookami.ospf.kotlin.core.intermediate_model.LinearPolynomial

/**
 * Re-export of sum() convenience functions from core.expression.
 *
 * This is a transitional compatibility layer. Framework code can use `import ...intermediate_model.sum`
 * instead of importing from core.expression.
 *
 * Will be removed when expression directory is deleted.
 */

@Deprecated("Will be removed in E7. Use kotlin's built-in sumOf or fold instead.")
@JvmName("sumVariables")
fun sum(
    items: Iterable<AbstractVariableItem<*, *>>,
    ctor: (AbstractVariableItem<*, *>) -> LinearMonomial = { LinearMonomial(it) }
): LinearPolynomial = items.fold(LinearPolynomial()) { acc, item -> acc + ctor(item) }

@Deprecated("Will be removed in E7. Use kotlin's built-in sumOf or fold instead.")
@JvmName("sumLinearSymbols")
fun sum(
    symbols: Iterable<LinearIntermediateSymbol>,
    ctor: (LinearIntermediateSymbol) -> LinearMonomial = { LinearMonomial(it) }
): LinearPolynomial = symbols.fold(LinearPolynomial()) { acc, sym -> acc + ctor(sym) }

@Deprecated("Will be removed in E7. Use kotlin's built-in sumOf or fold instead.")
@JvmName("sumLinearMonomials")
fun sum(monomials: Iterable<LinearMonomial>): LinearPolynomial =
    monomials.fold(LinearPolynomial()) { acc, mono -> acc + mono }

@Deprecated("Will be removed in E7. Use kotlin's built-in sumOf or fold instead.")
@JvmName("sumLinearPolynomials")
fun sum(polynomials: Iterable<AbstractLinearPolynomial<*>>): LinearPolynomial {
    val allMonomials = ArrayList<LinearMonomial>()
    var constant = Flt64.zero
    for (p in polynomials) {
        allMonomials.addAll(p.monomials)
        constant += p.constant
    }
    return LinearPolynomial(monomials = allMonomials, constant = constant)
}

// ========== qsum re-exports ==========

@Deprecated("Will be removed in E7. Use kotlin's built-in sumOf or fold instead.")
@JvmName("qsumQuadraticSymbols")
fun qsum(
    symbols: Iterable<fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol>,
    ctor: (fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol) -> QuadraticMonomial = { QuadraticMonomial(it) }
): QuadraticPolynomial = symbols.fold(QuadraticPolynomial()) { acc, sym -> acc + ctor(sym) }

@Deprecated("Will be removed in E7. Use kotlin's built-in sumOf or fold instead.")
@JvmName("qsumQuadraticMonomials")
fun qsum(monomials: Iterable<QuadraticMonomial>): QuadraticPolynomial =
    monomials.fold(QuadraticPolynomial()) { acc, mono -> acc + mono }

@Deprecated("Will be removed in E7. Use kotlin's built-in sumOf or fold instead.")
@JvmName("qsumQuadraticPolynomials")
fun qsum(polynomials: Iterable<AbstractQuadraticPolynomial<*>>): QuadraticPolynomial {
    val allMonomials = ArrayList<QuadraticMonomial>()
    var constant = Flt64.zero
    for (p in polynomials) {
        allMonomials.addAll(p.monomials)
        constant += p.constant
    }
    return QuadraticPolynomial(monomials = allMonomials, constant = constant)
}
