/**
 * 掩码函数符号 / Masking function symbols
 *
 * 提供 [MaskingFunction]、[MaskingWithPolyMaskFunction]、[MaskingRangeFunction]，
 * 实现 y = x * mask 的线性化建模（mask 为二值变量）。
 *
 * Provides [MaskingFunction], [MaskingWithPolyMaskFunction], and [MaskingRangeFunction]
 * for linearized modeling of y = x * mask (where mask is binary).
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.eq
import fuookami.ospf.kotlin.core.model.mechanism.geq
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.token.AbstractTokenList
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.SolverBoundaryCasts
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.core.variable.URealVar

/**
 * Masking function: y = x * mask where mask is binary.
 * When mask=1, y=x. When mask=0, y=0.
 */
class MaskingFunction<V>(
    val input: LinearPolynomial<V>,
    val mask: AbstractVariableItem<*, *>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_masking")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val maskValue = values[mask] ?: return converter.zero
        if (maskValue eq converter.zero) return converter.zero
        return input.evaluateWith(values)
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val one = converter.one
        val zero = converter.zero
        val resultIdx = LinearMonomial(one, resultVar)
        val mD = bigM
        val inputPoly = input

        val constraints = mutableListOf<LinearInequality<V>>()

        // c1: y - x + M*mask <= M + x_const  =>  y - x <= M*(1 - mask) + x_const
        val c1LhsMonos = inputPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(one, resultVar)
        val c1Lhs = LinearPolynomial(c1LhsMonos, -inputPoly.constant)
        val c1RhsPoly = LinearPolynomial(listOf(LinearMonomial(mD, mask)), mD)
        constraints += LinearInequality(c1Lhs, c1RhsPoly, Comparison.LE, "${name}_masking_eq_ub")

        // c2: y - x >= -M*mask + x_const  =>  y - x >= -M*mask + x_const
        val c2LhsMonos = inputPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(one, resultVar)
        val c2Lhs = LinearPolynomial(c2LhsMonos, -inputPoly.constant)
        val c2RhsPoly = LinearPolynomial(listOf(LinearMonomial(-mD, mask)), zero)
        constraints += LinearInequality(c2Lhs, c2RhsPoly, Comparison.GE, "${name}_masking_eq_lb")

        // c3: y - M*mask <= 0
        val c3Lhs = LinearPolynomial(listOf(resultIdx, LinearMonomial(-mD, mask)), zero)
        val c3Rhs = LinearPolynomial(emptyList(), zero)
        constraints += LinearInequality(c3Lhs, c3Rhs, Comparison.LE, "${name}_masking_zero_ub")

        // c4: y + M*mask >= 0
        val c4Lhs = LinearPolynomial(listOf(resultIdx, LinearMonomial(mD, mask)), zero)
        val c4Rhs = LinearPolynomial(emptyList(), zero)
        constraints += LinearInequality(c4Lhs, c4Rhs, Comparison.GE, "${name}_masking_zero_lb")

        return addConstraints(model, constraints) ?: ok
    }
    companion object {
        operator fun <V> invoke(
            input: LinearPolynomial<V>,
            mask: AbstractVariableItem<*, *>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): MaskingFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MaskingFunction(input, mask, bigM, converter, name, displayName)

        fun <V> withMaskVarName(
            input: LinearPolynomial<V>,
            maskVarName: String,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> {
            val maskVar = BinVar("${name}_${maskVarName}")
            return LinearFunctionSymbolAdapter(
                MaskingFunction(input, maskVar, bigM, converter, name, displayName),
                converter = converter
            )
        }

        /**
         * 类型化符号工厂：将线性中间符号转换为线性多项式。
         * Typed symbol factory: converts a linear intermediate symbol to a linear polynomial.
         */
        @JvmStatic
        @JvmName("fromLinearIntermediateSymbol")
        fun <V> fromLinearIntermediateSymbol(
            x: LinearIntermediateSymbol<V>,
            mask: AbstractVariableItem<*, *>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> = LinearFunctionSymbolAdapter(
            MaskingFunction(
                input = x.toLinearPolynomial(),
                mask = mask,
                bigM = bigM,
                converter = converter,
                name = name,
                displayName = displayName
            ),
            converter = converter
        )

        /**
         * 类型化多项式工厂：接受 input 与 mask 的线性多项式视图。
         * Typed polynomial factory: accepts linear-polynomial views for input and mask.
         */
        @JvmStatic
        @JvmName("fromLinearPolynomials")
        fun <V> fromLinearPolynomials(
            x: ToLinearPolynomial<V>,
            mask: ToLinearPolynomial<V>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): MaskingWithPolyMaskFunction<V> where V : RealNumber<V>, V : NumberField<V> = MaskingWithPolyMaskFunction(
            input = x.toLinearPolynomial(),
            maskPoly = mask.toLinearPolynomial(),
            bigM = bigM,
            converter = converter,
            name = name,
            displayName = displayName
        )
    }
}

/**
 * Masking function variant where mask is a polynomial instead of a single variable.
 * y = x * mask where mask is a polynomial expression.
 * Creates an internal variable `m` with constraint m = maskPoly, then applies
 * standard Big-M masking constraints with m as the binary mask.
 */
class MaskingWithPolyMaskFunction<V>(
    val input: LinearPolynomial<V>,
    val maskPoly: LinearPolynomial<V>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : LinearIntermediateSymbol<V>, MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    val maskVar: AbstractVariableItem<*, *> = URealVar("${name}_mask_var")
    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_result")

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(maskVar, resultVar)

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Linear
    override val cached: Boolean get() = false
    override val dependencies: Set<IntermediateSymbol<*>> get() = emptySet()
    override val discrete: Boolean get() = false
    override val range: ExpressionRange<V> get() = SolverBoundaryCasts.fullExpressionRange()

    override fun flush(force: Boolean) {}
    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val typedValues = values?.let { SolverBoundaryCasts.mapValues(it, converter) }
        return if (typedValues.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(typedValues, tokenTable, converter, false)
        }
    }
    override fun toRawString(unfold: UInt64): String = name

    override val polynomial: LinearPolynomial<V> get() = LinearPolynomial(emptyList(), converter.zero)
    override fun asMutable(): MutableLinearPolynomial<V> = MutableLinearPolynomial(emptyList(), converter.zero)

    internal fun evaluate(tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? = null
    internal fun evaluate(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? = null
    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?, zeroIfNone: Boolean): Flt64? {
        return delegate().evaluate(SolverBoundaryCasts.mapValues(values, converter))?.let { converter.fromValue(it) }
    }

    // V-typed evaluate overrides
    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        return if (values.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(values, tokenTable, converter, false)
        }
    }
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val values = LinkedHashMap<Symbol, V>(tokenTable.tokensInSolver.size)
        for (token in tokenTable.tokensInSolver) {
            val tokenValue = token.result ?: if (zeroIfNone) converter.zero else return null
            values[token.variable] = tokenValue
        }
        return delegate().evaluate(values)
    }
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val values = LinkedHashMap<Symbol, V>(tokenTable.tokensInSolver.size)
        for ((index, token) in tokenTable.tokensInSolver.withIndex()) {
            val value = if (index < results.size) {
                results[index]
            } else {
                if (!zeroIfNone) {
                    return null
                }
                converter.zero
            }
            values[token.variable] = value
        }
        return delegate().evaluate(values)
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return delegate().evaluate(values)
    }
    internal fun evaluateSolver(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val typedResults = results.map { converter.intoValue(it) }
        return evaluate(typedResults, tokenTable, converter, zeroIfNone)
    }
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val v = delegate().evaluate(SolverBoundaryCasts.mapValues(values, converter)) ?: return null
        return converter.intoValue(converter.fromValue(v))
    }

    private fun delegate(): MathFunctionSymbol<V> = this

    override fun evaluate(values: Map<Symbol, V>): V? {
        val maskValue = maskPoly.evaluateWith(values) ?: return converter.zero
        if (maskValue eq converter.zero) return converter.zero
        return input.evaluateWith(values)
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val one = converter.one
        val zero = converter.zero
        val inputPoly = input
        val maskPolyF = maskPoly
        val mF = bigM

        val constraints = mutableListOf<LinearInequality<V>>()

        // maskPoly = maskVar constraint
        val maskMonos = maskPolyF.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(one, maskVar)
        constraints += LinearInequality(
            LinearPolynomial(monomials = maskMonos, constant = -maskPolyF.constant),
            LinearPolynomial(monomials = emptyList(), constant = zero), Comparison.EQ, "${name}_mask_eq")

        // c1: result - input <= M*(1 - mask_var_normalized)
        val c1Monos = inputPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(one, resultVar) +
            LinearMonomial(mF, maskVar)
        constraints += LinearInequality(
            LinearPolynomial(monomials = c1Monos, constant = -inputPoly.constant),
            LinearPolynomial(monomials = emptyList(), constant = mF), Comparison.LE, "${name}_ub")

        // c2: result - input >= -M*mask_var
        val c2Monos = inputPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(one, resultVar) +
            LinearMonomial(mF, maskVar)
        constraints += LinearInequality(
            LinearPolynomial(monomials = c2Monos, constant = -inputPoly.constant),
            LinearPolynomial(monomials = emptyList(), constant = zero), Comparison.GE, "${name}_lb")

        // c3: result <= M*mask_var
        val c3Monos = listOf(LinearMonomial(one, resultVar), LinearMonomial(-mF, maskVar))
        constraints += LinearInequality(
            LinearPolynomial(monomials = c3Monos, constant = zero),
            LinearPolynomial(monomials = emptyList(), constant = zero), Comparison.LE, "${name}_zero_ub")

        // c4: result >= -M*mask_var
        val c4Monos = listOf(LinearMonomial(one, resultVar), LinearMonomial(mF, maskVar))
        constraints += LinearInequality(
            LinearPolynomial(monomials = c4Monos, constant = zero),
            LinearPolynomial(monomials = emptyList(), constant = zero), Comparison.GE, "${name}_zero_lb")

        return addConstraints(model, constraints) ?: ok
    }}

/**
 * Masking range function: y in [lower*mask, upper*mask].
 * When mask=0, y=0. When mask=1, y in [lower, upper].
 */
class MaskingRangeFunction<V>(
    val mask: LinearPolynomial<V>,
    val lower: V,
    val upper: V,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    init {
        require(lower ls upper || lower eq upper) {
            "MaskingRange lower bound must be <= upper bound"
        }
    }

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_mask_range")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val maskValue = mask.evaluateWith(values) ?: return converter.zero
        if (maskValue eq converter.zero) return converter.zero

        val yValue = values[resultVar] ?: return converter.zero
        val lowerCandidate = lower * maskValue
        val upperCandidate = upper * maskValue
        val lowerBound: V
        val upperBound: V
        if (lowerCandidate ls upperCandidate || lowerCandidate eq upperCandidate) {
            lowerBound = lowerCandidate
            upperBound = upperCandidate
        } else {
            lowerBound = upperCandidate
            upperBound = lowerCandidate
        }
        return when {
            yValue ls lowerBound -> lowerBound
            yValue gr upperBound -> upperBound
            else -> yValue
        }
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val one = converter.one
        val zero = converter.zero
        val resultMon = LinearMonomial(one, resultVar)
        val maskPoly = mask
        val lowerValue = lower
        val upperValue = upper

        val constraints = mutableListOf<LinearInequality<V>>()

        // Upper: y - upper*mask <= 0
        val upperMonos = listOf(resultMon) +
            maskPoly.monomials.map { LinearMonomial(it.coefficient * -upperValue, it.symbol) }
        val upperLhs = LinearPolynomial(upperMonos, maskPoly.constant * -upperValue)
        val upperRhs = LinearPolynomial(emptyList(), zero)
        constraints += LinearInequality(upperLhs, upperRhs, Comparison.LE, "${name}_masking_range_ub")

        // Lower: y - lower*mask >= 0
        val lowerMonos = listOf(resultMon) +
            maskPoly.monomials.map { LinearMonomial(it.coefficient * -lowerValue, it.symbol) }
        val lowerLhs = LinearPolynomial(lowerMonos, maskPoly.constant * -lowerValue)
        val lowerRhs = LinearPolynomial(emptyList(), zero)
        constraints += LinearInequality(lowerLhs, lowerRhs, Comparison.GE, "${name}_masking_range_lb")

        return addConstraints(model, constraints) ?: ok
    }
    companion object {
        operator fun <V> invoke(
            mask: LinearPolynomial<V>,
            lower: V,
            upper: V,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): MaskingRangeFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MaskingRangeFunction(mask, lower, upper, converter, name, displayName)
    }
}
