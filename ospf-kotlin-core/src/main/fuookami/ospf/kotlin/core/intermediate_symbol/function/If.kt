@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.ExpressionRange
import fuookami.ospf.kotlin.core.intermediate_model.*
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.token.LegacyAbstractTokenTable
import fuookami.ospf.kotlin.core.token.LinearFlattenDataF64
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.token.AbstractTokenListF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as MathLinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as MathLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * If-Then implication function: result = 1 if (premise => consequence), else 0.
 *
 * Uses InequalityFunction-style indicator variables for premise and consequence,
 * then links them: premise_indicator <= consequence_indicator (in constraint mode).
 */
class IfFunction<T : Field<T>>(
    val premise: MathLinearInequality,
    val consequence: MathLinearInequality,
    bigM: T? = null,
    val constraintMode: Boolean = true,
    override var name: String,
    override var displayName: String? = null
) : LinearIntermediateSymbol<Flt64>, MathFunctionSymbol<T> {
    private val bigM: T = bigM ?: Flt64(BIG_M_DEFAULT) as T

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_if_then")
    val premiseIndicator: AbstractVariableItem<*, *> = BinVar("${name}_premise")
    val consequenceIndicator: AbstractVariableItem<*, *> = BinVar("${name}_consequence")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, premiseIndicator, consequenceIndicator)

    override fun evaluate(values: Map<Symbol, T>): T? {
        val premiseHolds = checkInequality(premise, values) ?: return null
        val consequenceHolds = checkInequality(consequence, values) ?: return null
        return if (!premiseHolds || consequenceHolds) oneOf<T>() else zeroOf<T>()
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionF64): Try {
        val vars = helperVariables
        return if (vars.isNotEmpty()) tokens.add(vars) else ok
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        when (val r = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val allConstraints = mutableListOf<MathLinearInequality>()

        // Build premise indicator constraints
        allConstraints += simpleIndicatorConstraints(premise, premiseIndicator, bigM.asFlt64(), "${name}_premise")

        // Build consequence indicator constraints
        allConstraints += simpleIndicatorConstraints(consequence, consequenceIndicator, bigM.asFlt64(), "${name}_consequence")

        val premMon = MathLinearMonomial(Flt64.one, premiseIndicator)
        val consMon = MathLinearMonomial(Flt64.one, consequenceIndicator)
        val resultMon = MathLinearMonomial(Flt64.one, resultVar)

        if (constraintMode) {
            // premise_indicator - consequence_indicator <= 0
            allConstraints += MathLinearInequality(
                MathLinearPolynomial(listOf(premMon, MathLinearMonomial(Flt64(-1.0), consequenceIndicator)), Flt64.zero),
                MathLinearPolynomial(emptyList(), Flt64.zero), Comparison.LE, "${name}_if_then")

            // result = 1
            allConstraints += MathLinearInequality(
                MathLinearPolynomial(listOf(resultMon), Flt64.zero),
                MathLinearPolynomial(emptyList(), Flt64.one), Comparison.EQ, "${name}_if_then_result")
        } else {
            // result + premise_indicator >= 1
            allConstraints += MathLinearInequality(
                MathLinearPolynomial(listOf(resultMon, premMon), Flt64.zero),
                MathLinearPolynomial(emptyList(), Flt64.one), Comparison.GE, "${name}_if_then_value_lb1")

            // result - consequence_indicator >= 0
            allConstraints += MathLinearInequality(
                MathLinearPolynomial(listOf(resultMon, MathLinearMonomial(Flt64(-1.0), consequenceIndicator)), Flt64.zero),
                MathLinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_if_then_value_lb2")

            // result + premise_indicator - consequence_indicator <= 1
            allConstraints += MathLinearInequality(
                MathLinearPolynomial(listOf(resultMon, premMon, MathLinearMonomial(Flt64(-1.0), consequenceIndicator)), Flt64.zero),
                MathLinearPolynomial(emptyList(), Flt64.one), Comparison.LE, "${name}_if_then_value_ub")
        }

        return addConstraints(model, allConstraints) ?: ok
    }

    // LinearIntermediateSymbol members
    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Linear
    override val cached: Boolean get() = false
    override val dependencies: Set<IntermediateSymbol<*>> get() = emptySet()
    override val discrete: Boolean get() = false
    override val range: ExpressionRange<Flt64> get() = ExpressionRange()

    override fun flush(force: Boolean) {}
    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: LegacyAbstractTokenTable): Flt64? = null
    override fun toRawString(unfold: UInt64): String = name

    override val flattenedMonomials: LinearFlattenDataF64 get() = LinearFlattenDataF64(emptyList(), Flt64.zero)

    override fun toMathLinearInequality(): MathLinearInequality {
        return MathLinearInequality(MathLinearPolynomial(emptyList(), Flt64.zero), MathLinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    override fun toMathQuadraticInequality(): MathQuadraticInequality {
        return MathQuadraticInequality(fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), Flt64.zero), fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    override fun evaluate(tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenListF64?, zeroIfNone: Boolean): Flt64? =
        (this as MathFunctionSymbol<Flt64>).evaluate(values)

    companion object {
        operator fun invoke(
            premise: MathLinearInequality,
            consequence: MathLinearInequality,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): IfFunction<Flt64> = IfFunction(premise, consequence, bigM, constraintMode = true, name, displayName)

        fun indicator(
            premise: MathLinearInequality,
            consequence: MathLinearInequality,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): IfFunction<Flt64> = IfFunction(premise, consequence, bigM, constraintMode = false, name, displayName)

        /**
         * Factory: accept legacy LinearConstraintInput for framework compatibility.
         * Converts the input relation into premise/consequence inequalities.
         * For legacy single-inequality input, premise is always true (vacuous),
         * so the result indicates whether the input inequality holds.
         */
        @JvmStatic
        operator fun invoke(
            input: fuookami.ospf.kotlin.core.intermediate_model.LinearConstraintInput,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): IfFunction<Flt64> {
            // LinearConstraintInput.sign is already math.symbol.inequality.Comparison
            val comp = input.sign
            // Reconstruct linear inequality from flattenData
            val monos = input.flattenData.monomials.map {
                MathLinearMonomial(it.coefficient, it.symbol)
            }
            val lhs = MathLinearPolynomial(monos, input.flattenData.constant)
            val rhs = MathLinearPolynomial(emptyList(), Flt64.zero)
            val premiseIneq = MathLinearInequality(lhs, rhs, comp, "${name}_premise")
            // Vacuous consequence: 0 <= 0 (always true)
            val consequenceIneq = MathLinearInequality(
                MathLinearPolynomial(emptyList(), Flt64.zero),
                MathLinearPolynomial(emptyList(), Flt64.zero),
                Comparison.LE,
                "${name}_consequence"
            )
            return IfFunction(premiseIneq, consequenceIneq, bigM, constraintMode = true, name, displayName)
        }
    }
}

private fun <T : Field<T>> checkInequality(ineq: MathLinearInequality, values: Map<Symbol, T>): Boolean? {
    val lhsVal = evalPoly(ineq.lhs, values) ?: return null
    val rhsVal = ineq.rhs.constant.asFlt64().toDouble()
    val eps = 1e-10
    return when (ineq.comparison) {
        Comparison.LE -> lhsVal.asFlt64().toDouble() <= rhsVal + eps
        Comparison.GE -> lhsVal.asFlt64().toDouble() + eps >= rhsVal
        Comparison.EQ -> kotlin.math.abs(lhsVal.asFlt64().toDouble() - rhsVal) <= eps
        Comparison.LT, Comparison.GT, Comparison.NE -> false
    }
}

private fun <T : Field<T>> evalPoly(poly: MathLinearPolynomial<Flt64>, values: Map<Symbol, T>): T? {
    var sum: T? = null
    for (m in poly.monomials) {
        val sv = values[m.symbol] ?: return null
        val term = (m.coefficient.toDouble() * sv.asFlt64().toDouble()).let {
            @Suppress("UNCHECKED_CAST")
            Flt64(it) as T
        }
        sum = if (sum == null) term else sum + term
    }
    val constTerm = (poly.constant.toDouble()).let {
        @Suppress("UNCHECKED_CAST")
        Flt64(it) as T
    }
    return sum?.let { it + constTerm } ?: constTerm
}
