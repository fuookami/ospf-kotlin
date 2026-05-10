@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintInput
import fuookami.ospf.kotlin.core.model.mechanism.compare
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.token.LinearFlattenData

private val flt64Converter = object : IntoValue<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/**
 * Satisfied Amount function: counts how many inequalities in a list are satisfied.
 *
 * Given a list of linear constraints, this function:
 * - Creates a binary flag `u[i]` for each constraint indicating satisfaction
 * - Returns `y = sum(u[i])` as the count of satisfied constraints
 *
 * When `amount` is specified, returns a binary indicator:
 * - `y = 1` if the count of satisfied constraints is within `amount` range
 * - `y = 0` otherwise
 *
 * The constraint satisfaction is encoded using the PCT (Percentage) formulation:
 * For each constraint, 3 percentage variables [k0, k1, k2] are created to interpolate
 * between [lowerBound, 0, upperBound], with a binary flag indicating whether 0 is in range.
 *
 * @param inputs list of constraint inputs to check
 * @param amount optional range of satisfied count; if null, returns raw count
 * @param epsilon tolerance for boundary checks
 * @param converter value type converter for V-typed constants and Flt64 <-> V conversion
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
open class SatisfiedAmountInequalityFunction<V>(
    val inputs: List<LinearConstraintInput>,
    open val amount: ValueRange<UInt64>? = null,
    val epsilon: V,
    val converter: IntoValue<V>,
    override var name: String = "satisfied_amount",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    /** Binary flags: one per input constraint. */
    private val flagVars: List<AbstractVariableItem<*, *>> by lazy {
        inputs.indices.map { i -> BinVar("${name}_u_$i") }
    }

    /** Single binary output when amount is specified. */
    private val amountFlagVar: AbstractVariableItem<*, *>? by lazy {
        val currentAmount = amount
        if (currentAmount != null) BinVar("${name}_y") else null
    }

    override val helperVariables: List<AbstractVariableItem<*, *>> by lazy {
        buildList {
            addAll(flagVars)
            amountFlagVar?.let { add(it) }
        }
    }

    /**
     * Result: sum of satisfied constraint flags.
     * If amount is specified, this is a binary indicator (0 or 1).
     */
    val result: LinearPolynomial<V> by lazy {
        val currentAmount = amount
        if (currentAmount != null) {
            LinearPolynomial(
                listOf(LinearMonomial(converter.one, amountFlagVar!!)),
                converter.zero
            )
        } else {
            LinearPolynomial(
                flagVars.map { LinearMonomial(converter.one, it) },
                converter.zero
            )
        }
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        var count = 0
        for (input in inputs) {
            val satisfied = checkInputSatisfied(input, values) ?: return null
            if (satisfied) count++
        }
        val countUInt = UInt64(count)
        val currentAmount = amount
        val resultFlt64 = if (currentAmount != null) {
            if (currentAmount.contains(countUInt)) Flt64.one else Flt64.zero
        } else {
            Flt64(count.toDouble())
        }
        return converter.intoValue(resultFlt64)
    }

    /**
     * Check whether a single input constraint is satisfied given the current values.
     */
    private fun checkInputSatisfied(
        input: LinearConstraintInput,
        values: Map<Symbol, V>
    ): Boolean? {
        var lhsValue = input.flattenData.constant
        for (monomial in input.flattenData.monomials) {
            val symbolValue = values[monomial.symbol]
                ?: return null
            lhsValue += monomial.coefficient * converter.fromValue(symbolValue)
        }
        return input.sign.compare(lhsValue, Flt64.zero)
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val eps = converter.fromValue(epsilon)
        val nInputs = inputs.size
        val constraints = mutableListOf<fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>>()

        for ((i, input) in inputs.withIndex()) {
            val lb = input.lowerBound
            val ub = input.upperBound
            val flag = flagVars[i]

            if (lb != null && ub != null) {
                val inRange = Flt64.zero geq lb && Flt64.zero leq ub
                if (inRange) {
                    // Simple encoding: when 0 is within [lb, ub],
                    // flag = 1 means constraint satisfied (lhs ~ 0 within bounds)
                    // flag = 0 means violated
                    // Use Big-M: lhs <= M*flag and lhs >= -M*flag when flag=1
                    val m = maxOf(lb.abs(), ub.abs(), Flt64(1e6))

                    // Convert LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> to LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>
                    val polyFlt64 = LinearPolynomial(
                        input.flattenData.monomials.map { m2 -> LinearMonomial(m2.coefficient, m2.symbol) },
                        input.flattenData.constant
                    )

                    // When flag=1: lhs <= epsilon and lhs >= -epsilon (satisfied)
                    // When flag=0: lhs <= M and lhs >= -M (no constraint)
                    val upperLhs = LinearPolynomial(
                        polyFlt64.monomials + listOf(LinearMonomial(m, flag)),
                        polyFlt64.constant
                    )
                    val upperRhs = LinearPolynomial(emptyList(), m + eps)
                    constraints += fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                        upperLhs, upperRhs, Comparison.LE, "${name}_i${i}_upper"
                    )

                    val lowerLhs = LinearPolynomial(
                        polyFlt64.monomials + listOf(LinearMonomial(-m, flag)),
                        polyFlt64.constant
                    )
                    val lowerRhs = LinearPolynomial(emptyList(), -m - eps)
                    constraints += fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                        lowerLhs, lowerRhs, Comparison.GE, "${name}_i${i}_lower"
                    )
                } else {
                    // When 0 is NOT within [lb, ub], the constraint is trivially satisfied or violated
                    val triviallySatisfied = when (input.sign) {
                        Comparison.LE, Comparison.LT -> ub ls Flt64.zero
                        Comparison.GE, Comparison.GT -> lb gr Flt64.zero
                        Comparison.EQ -> false
                        Comparison.NE -> true
                    }
                    // Fix flag to the trivial value
                    val fixedValue = if (triviallySatisfied) Flt64.one else Flt64.zero
                    val poly = LinearPolynomial(listOf(LinearMonomial(Flt64.one, flag)), Flt64.zero)
                    val rhs = LinearPolynomial(emptyList(), fixedValue)
                    constraints += fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                        poly, rhs, Comparison.EQ, "${name}_i${i}_flag"
                    )
                }
            }
        }

        // Amount range constraint: lb <= sum(u) <= ub, with binary y indicator
        val currentAmount = amount
        if (currentAmount != null) {
            val y = amountFlagVar!!
            val sumPoly = LinearPolynomial(
                flagVars.map { LinearMonomial(converter.one, it) },
                converter.zero
            )
            val sumPolyFlt64 = sumPoly.asFlt64Poly(converter)

            // sum(u) >= amount.lowerBound - n*(1-y)
            val lbPoly = LinearPolynomial(
                sumPolyFlt64.monomials + listOf(LinearMonomial(Flt64(nInputs), y)),
                sumPolyFlt64.constant
            )
            val lbRhs = LinearPolynomial(
                emptyList(),
                Flt64(currentAmount.lowerBound.value.unwrap().toLong().toDouble()) + Flt64(nInputs.toDouble())
            )
            constraints += fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                lbPoly, lbRhs, Comparison.GE, "${name}_amount_lb"
            )

            // sum(u) <= amount.upperBound + n*(1-y)
            val ubPoly = LinearPolynomial(
                sumPolyFlt64.monomials + listOf(LinearMonomial(-Flt64(nInputs), y)),
                sumPolyFlt64.constant
            )
            val ubRhs = LinearPolynomial(
                emptyList(),
                Flt64(currentAmount.upperBound.value.unwrap().toLong().toDouble()) + Flt64(nInputs.toDouble())
            )
            constraints += fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                ubPoly, ubRhs, Comparison.LE, "${name}_amount_ub"
            )
        }

        return addConstraints(model, constraints, converter) ?: ok
    }
    companion object {
        operator fun <V> invoke(
            inputs: List<LinearConstraintInput>,
            amount: ValueRange<UInt64>? = null,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): SatisfiedAmountInequalityFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            SatisfiedAmountInequalityFunction(
                inputs = inputs,
                amount = amount,
                epsilon = converter.intoValue(epsilon),
                converter = converter,
                name = name,
                displayName = displayName
            )

        operator fun invoke(
            inputs: List<LinearConstraintInput>,
            amount: ValueRange<UInt64>? = null,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): SatisfiedAmountInequalityFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = SatisfiedAmountInequalityFunction(
            inputs = inputs,
            amount = amount,
            epsilon = epsilon,
            converter = flt64Converter,
            name = name,
            displayName = displayName
        )
    }
}

/**
 * AnyFunction: at least one inequality must be satisfied.
 *
 * Alias: `amount = [1, n]`
 */
class AnyFunction<V>(
    inputs: List<LinearConstraintInput>,
    epsilon: V,
    converter: IntoValue<V>,
    override var name: String = "any",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<V>(
    inputs = inputs,
    amount = ValueRange(UInt64.one, UInt64(inputs.size)).value!!,
    epsilon = epsilon,
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        operator fun <V> invoke(
            inputs: List<LinearConstraintInput>,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): AnyFunction<V> where V : RealNumber<V>, V : NumberField<V> = AnyFunction(
            inputs = inputs,
            epsilon = converter.intoValue(epsilon),
            converter = converter,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            inputs: List<LinearConstraintInput>,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): AnyFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = AnyFunction(
            inputs = inputs,
            epsilon = epsilon,
            converter = flt64Converter,
            name = name,
            displayName = displayName
        )
    }
}

/**
 * AllFunction: all inequalities must be satisfied.
 *
 * Alias: `amount = [n, n]`
 */
class AllFunction<V>(
    inputs: List<LinearConstraintInput>,
    epsilon: V,
    converter: IntoValue<V>,
    override var name: String = "all",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<V>(
    inputs = inputs,
    amount = ValueRange(UInt64(inputs.size), UInt64(inputs.size)).value!!,
    epsilon = epsilon,
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        operator fun <V> invoke(
            inputs: List<LinearConstraintInput>,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): AllFunction<V> where V : RealNumber<V>, V : NumberField<V> = AllFunction(
            inputs = inputs,
            epsilon = converter.intoValue(epsilon),
            converter = converter,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            inputs: List<LinearConstraintInput>,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): AllFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = AllFunction(
            inputs = inputs,
            epsilon = epsilon,
            converter = flt64Converter,
            name = name,
            displayName = displayName
        )
    }
}

/**
 * AtLeastInequalityFunction: at least k inequalities must be satisfied.
 *
 * Alias: `amount = [k, n]`
 */
class AtLeastInequalityFunction<V>(
    inputs: List<LinearConstraintInput>,
    val k: UInt64,
    epsilon: V,
    converter: IntoValue<V>,
    override var name: String = "at_least",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<V>(
    inputs = inputs,
    amount = ValueRange(k, UInt64(inputs.size)).value!!,
    epsilon = epsilon,
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    init {
        assert(k > UInt64.zero)
        assert(UInt64(inputs.size) >= k)
    }

    companion object {
        operator fun <V> invoke(
            inputs: List<LinearConstraintInput>,
            k: UInt64,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): AtLeastInequalityFunction<V> where V : RealNumber<V>, V : NumberField<V> = AtLeastInequalityFunction(
            inputs = inputs,
            k = k,
            epsilon = converter.intoValue(epsilon),
            converter = converter,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            inputs: List<LinearConstraintInput>,
            k: UInt64,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): AtLeastInequalityFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = AtLeastInequalityFunction(
            inputs = inputs,
            k = k,
            epsilon = epsilon,
            converter = flt64Converter,
            name = name,
            displayName = displayName
        )
    }
}

/**
 * NotAllFunction: not all inequalities can be satisfied simultaneously.
 *
 * Alias: `amount = [1, n-1]`
 */
class NotAllFunction<V>(
    inputs: List<LinearConstraintInput>,
    epsilon: V,
    converter: IntoValue<V>,
    override var name: String = "not_all",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<V>(
    inputs = inputs,
    amount = if (inputs.size > 1) ValueRange(UInt64.one, UInt64(inputs.size - 1)).value!! else null,
    epsilon = epsilon,
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        operator fun <V> invoke(
            inputs: List<LinearConstraintInput>,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): NotAllFunction<V> where V : RealNumber<V>, V : NumberField<V> = NotAllFunction(
            inputs = inputs,
            epsilon = converter.intoValue(epsilon),
            converter = converter,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            inputs: List<LinearConstraintInput>,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): NotAllFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = NotAllFunction(
            inputs = inputs,
            epsilon = epsilon,
            converter = flt64Converter,
            name = name,
            displayName = displayName
        )
    }
}

/**
 * NumerableFunction: the count of satisfied inequalities must be within a specified range.
 */
class NumerableFunction<V>(
    inputs: List<LinearConstraintInput>,
    override val amount: ValueRange<UInt64>,
    epsilon: V,
    converter: IntoValue<V>,
    override var name: String = "numerable",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<V>(
    inputs = inputs,
    amount = amount,
    epsilon = epsilon,
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        operator fun <V> invoke(
            inputs: List<LinearConstraintInput>,
            amount: ValueRange<UInt64>,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): NumerableFunction<V> where V : RealNumber<V>, V : NumberField<V> = NumerableFunction(
            inputs = inputs,
            amount = amount,
            epsilon = converter.intoValue(epsilon),
            converter = converter,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            inputs: List<LinearConstraintInput>,
            amount: ValueRange<UInt64>,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): NumerableFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = NumerableFunction(
            inputs = inputs,
            amount = amount,
            epsilon = epsilon,
            converter = flt64Converter,
            name = name,
            displayName = displayName
        )
    }
}
