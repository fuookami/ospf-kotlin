@file:Suppress("unused")

package fuookami.ospf.kotlin.core.function

import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearConstraintInput
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearFlattenData
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.frontend.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.concept.Field
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
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class SatisfiedAmountInequalityFunction<T : Field<T>>(
    val inputs: List<LinearConstraintInput>,
    val amount: ValueRange<UInt64>? = null,
    val epsilon: Flt64 = Flt64(1e-6),
    override var name: String = "satisfied_amount",
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    /** Binary flags: one per input constraint. */
    private val flagVars: List<AbstractVariableItem<*, *>> by lazy {
        inputs.indices.map { i -> BinVar("${name}_u_$i") }
    }

    /** Single binary output when amount is specified. */
    private val amountFlagVar: AbstractVariableItem<*, *>? by lazy {
        if (amount != null) BinVar("${name}_y") else null
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
    val result: LinearPolynomial<T> by lazy {
        if (amount != null) {
            LinearPolynomial(
                listOf(LinearMonomial(oneOf<T>(), amountFlagVar!!)),
                zeroOf<T>()
            )
        } else {
            LinearPolynomial(
                flagVars.map { LinearMonomial(oneOf<T>(), it) },
                zeroOf<T>()
            )
        }
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        var count = 0
        for (input in inputs) {
            val satisfied = checkInputSatisfied(input, values) ?: return null
            if (satisfied) count++
        }
        val countUInt = UInt64(count.toLong())
        val result = if (amount != null) {
            if (amount.contains(countUInt)) Flt64.one else Flt64.zero
        } else {
            Flt64(count.toDouble())
        }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    /**
     * Check whether a single input constraint is satisfied given the current values.
     */
    private fun <U : Field<U>> checkInputSatisfied(
        input: LinearConstraintInput,
        values: Map<Symbol, U>
    ): Boolean? {
        var lhsValue = input.flattenData.constant.asFlt64()
        for (monomial in input.flattenData.monomials) {
            val symbolValue = values[monomial.symbol]
                ?: return null
            lhsValue += monomial.coefficient * symbolValue.asFlt64()
        }
        return input.sign.compare(lhsValue, Flt64.zero)
    }

    override fun register(model: AbstractLinearMetaModel): Try {
        // Register flag variables
        when (val result = model.add(flagVars)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        amountFlagVar?.let { y ->
            when (val result = model.add(y)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        val eps = epsilon
        val nInputs = inputs.size

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

                    // Convert LinearFlattenData to LinearPolynomial<Flt64>
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
                    model.addConstraint(
                        relation = fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality(
                            upperLhs, upperRhs, Comparison.LE, "${name}_i${i}_upper"
                        ),
                        name = "${name}_i${i}_upper"
                    ).takeUnless { it.ok }?.let { return it }

                    val lowerLhs = LinearPolynomial(
                        polyFlt64.monomials + listOf(LinearMonomial(-m, flag)),
                        polyFlt64.constant
                    )
                    val lowerRhs = LinearPolynomial(emptyList(), -m - eps)
                    model.addConstraint(
                        relation = fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality(
                            lowerLhs, lowerRhs, Comparison.GE, "${name}_i${i}_lower"
                        ),
                        name = "${name}_i${i}_lower"
                    ).takeUnless { it.ok }?.let { return it }
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
                    val poly = LinearPolynomial(listOf(LinearMonomial(oneOf<T>(), flag)), zeroOf<T>())
                    val rhs = LinearPolynomial(emptyList(), fixedValue)
                    model.addConstraint(
                        relation = fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality(
                            poly, rhs, Comparison.EQ, "${name}_i${i}_flag"
                        ),
                        name = "${name}_i${i}_flag"
                    ).takeUnless { it.ok }?.let { return it }
                }
            }
        }

        // Amount range constraint: lb <= sum(u) <= ub, with binary y indicator
        if (amount != null) {
            val y = amountFlagVar!!
            val sumPoly = LinearPolynomial(
                flagVars.map { LinearMonomial(oneOf<T>(), it) },
                zeroOf<T>()
            )
            val sumPolyFlt64 = sumPoly.asFlt64Poly()

            // sum(u) >= amount.lowerBound - n*(1-y)
            val lbPoly = LinearPolynomial(
                sumPolyFlt64.monomials + listOf(LinearMonomial(Flt64(nInputs), y)),
                sumPolyFlt64.constant
            )
            val lbRhs = LinearPolynomial(
                emptyList(),
                Flt64(amount.lowerBound!!.value.unwrap().toDouble()) + Flt64(nInputs.toDouble())
            )
            model.addConstraint(
                relation = fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality(
                    lbPoly, lbRhs, Comparison.GE, "${name}_amount_lb"
                ),
                name = "${name}_amount_lb"
            ).takeUnless { it.ok }?.let { return it }

            // sum(u) <= amount.upperBound + n*(1-y)
            val ubPoly = LinearPolynomial(
                sumPolyFlt64.monomials + listOf(LinearMonomial(-Flt64(nInputs), y)),
                sumPolyFlt64.constant
            )
            val ubRhs = LinearPolynomial(
                emptyList(),
                Flt64(amount.upperBound!!.value.unwrap().toDouble()) + Flt64(nInputs.toDouble())
            )
            model.addConstraint(
                relation = fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality(
                    ubPoly, ubRhs, Comparison.LE, "${name}_amount_ub"
                ),
                name = "${name}_amount_ub"
            ).takeUnless { it.ok }?.let { return it }
        }

        return ok
    }

    companion object {
        operator fun invoke(
            inputs: List<LinearConstraintInput>,
            amount: ValueRange<UInt64>? = null,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): SatisfiedAmountInequalityFunction<Flt64> = SatisfiedAmountInequalityFunction(
            inputs = inputs,
            amount = amount,
            epsilon = epsilon,
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
class AnyFunction<T : Field<T>>(
    inputs: List<LinearConstraintInput>,
    epsilon: Flt64 = Flt64(1e-6),
    override var name: String = "any",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<T>(
    inputs = inputs,
    amount = ValueRange(UInt64.one, UInt64(inputs.size.toLong())).value!!,
    epsilon = epsilon,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            inputs: List<LinearConstraintInput>,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): AnyFunction<Flt64> = AnyFunction(
            inputs = inputs,
            epsilon = epsilon,
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
class AllFunction<T : Field<T>>(
    inputs: List<LinearConstraintInput>,
    epsilon: Flt64 = Flt64(1e-6),
    override var name: String = "all",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<T>(
    inputs = inputs,
    amount = ValueRange(UInt64(inputs.size.toLong()), UInt64(inputs.size.toLong())).value!!,
    epsilon = epsilon,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            inputs: List<LinearConstraintInput>,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): AllFunction<Flt64> = AllFunction(
            inputs = inputs,
            epsilon = epsilon,
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
class AtLeastInequalityFunction<T : Field<T>>(
    inputs: List<LinearConstraintInput>,
    val k: UInt64,
    epsilon: Flt64 = Flt64(1e-6),
    override var name: String = "at_least",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<T>(
    inputs = inputs,
    amount = ValueRange(k, UInt64(inputs.size.toLong())).value!!,
    epsilon = epsilon,
    name = name,
    displayName = displayName
) {
    init {
        assert(k > UInt64.zero)
        assert(UInt64(inputs.size.toLong()) >= k)
    }

    companion object {
        operator fun invoke(
            inputs: List<LinearConstraintInput>,
            k: UInt64,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): AtLeastInequalityFunction<Flt64> = AtLeastInequalityFunction(
            inputs = inputs,
            k = k,
            epsilon = epsilon,
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
class NotAllFunction<T : Field<T>>(
    inputs: List<LinearConstraintInput>,
    epsilon: Flt64 = Flt64(1e-6),
    override var name: String = "not_all",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<T>(
    inputs = inputs,
    amount = if (inputs.size > 1) ValueRange(UInt64.one, UInt64(inputs.size - 1)).value!! else null,
    epsilon = epsilon,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            inputs: List<LinearConstraintInput>,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): NotAllFunction<Flt64> = NotAllFunction(
            inputs = inputs,
            epsilon = epsilon,
            name = name,
            displayName = displayName
        )
    }
}

/**
 * NumerableFunction: the count of satisfied inequalities must be within a specified range.
 */
class NumerableFunction<T : Field<T>>(
    inputs: List<LinearConstraintInput>,
    val amount: ValueRange<UInt64>,
    epsilon: Flt64 = Flt64(1e-6),
    override var name: String = "numerable",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<T>(
    inputs = inputs,
    amount = amount,
    epsilon = epsilon,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            inputs: List<LinearConstraintInput>,
            amount: ValueRange<UInt64>,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): NumerableFunction<Flt64> = NumerableFunction(
            inputs = inputs,
            amount = amount,
            epsilon = epsilon,
            name = name,
            displayName = displayName
        )
    }
}
