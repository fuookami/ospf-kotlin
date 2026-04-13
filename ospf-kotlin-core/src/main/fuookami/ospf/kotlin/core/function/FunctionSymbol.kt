@file:Suppress("unused")

package fuookami.ospf.kotlin.core.function

import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearFlattenData
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Base interface for math-symbol-based function symbols.
 * Each function symbol creates helper variables and generates linear constraints.
 *
 * @param T the numeric type (must implement Field). Currently only Flt64 is supported for register().
 */
interface MathFunctionSymbol<T : Field<T>> {
    val name: String
    var displayName: String?

    /**
     * Helper variables created by this function (e.g. pos/neg slack variables).
     * Exposed so the framework can reference them in objectives.
     */
    val helperVariables: List<AbstractVariableItem<*, *>>

    /**
     * Evaluate this function symbol given resolved symbol values.
     */
    fun evaluate(values: Map<Symbol, T>): T?

    /**
     * Register this function symbol with the model.
     * Note: Currently only supports T=Flt64 since the solver layer uses Flt64 constraints.
     */
    fun register(model: AbstractLinearMetaModel): Try
}

/**
 * Evaluate a math LinearPolynomial given a map of Symbol -> T values.
 * Returns null if any symbol in the polynomial is missing from the map.
 */
@Suppress("UNCHECKED_CAST")
fun <T : Field<T>> LinearPolynomial<T>.evaluate(values: Map<Symbol, T>): T? {
    val monomialsWithValues = monomials.mapNotNull { mono ->
        val sv = values[mono.symbol] ?: return null
        mono.coefficient * sv
    }
    var sum: T? = null
    for (term in monomialsWithValues) {
        sum = if (sum == null) term else sum + term
    }
    return (sum ?: constant) as T
}

/** Internal helper: cast T to Flt64 for constraint generation. Only valid when T=Flt64. */
@Suppress("UNCHECKED_CAST")
internal fun <T : Field<T>> T.asFlt64(): Flt64 = this as Flt64

/** Internal helper: get zero for type T. */
internal fun <T : Field<T>> zeroOf(): T = Flt64.zero as T

/** Internal helper: get one for type T. */
internal fun <T : Field<T>> oneOf(): T = Flt64.one as T

/** Internal helper: check if T is near zero. */
internal fun <T : Field<T>> T.isNearZero(tolerance: Double = NONZERO_TOLERANCE): Boolean {
    val d = this.asFlt64().toDouble()
    return d <= tolerance && d >= -tolerance
}

/** Internal helper: check if T is nonzero. */
internal fun <T : Field<T>> T.isNonZero(tolerance: Double = NONZERO_TOLERANCE): Boolean {
    val d = this.asFlt64().toDouble()
    return d > tolerance || d < -tolerance
}

/** Internal helper: convert LinearPolynomial<T> to LinearPolynomial<Flt64> for constraint generation. */
@Suppress("UNCHECKED_CAST")
internal fun <T : Field<T>> LinearPolynomial<T>.asFlt64Poly(): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(it.coefficient.asFlt64(), it.symbol) },
        constant.asFlt64()
    )
}

// ========== Flt64 type aliases for backward compatibility ==========

typealias Flt64MathFunctionSymbol = MathFunctionSymbol<Flt64>

typealias Flt64AndFunction = AndFunction<Flt64>
typealias Flt64OrFunction = OrFunction<Flt64>
typealias Flt64NotFunction = NotFunction<Flt64>
typealias Flt64XorFunction = XorFunction<Flt64>
typealias Flt64IfFunction = IfFunction<Flt64>
typealias Flt64BinaryzationFunction = BinaryzationFunction<Flt64>
typealias Flt64MaskingFunction = MaskingFunction<Flt64>
typealias Flt64MaskingRangeFunction = MaskingRangeFunction<Flt64>
typealias Flt64MaxFunction = MaxFunction<Flt64>
typealias Flt64MinFunction = MinFunction<Flt64>
typealias Flt64MinMaxFunction = MinMaxFunction<Flt64>
typealias Flt64MaxMinFunction = MaxMinFunction<Flt64>
typealias Flt64SlackFunction = SlackFunction<Flt64>
