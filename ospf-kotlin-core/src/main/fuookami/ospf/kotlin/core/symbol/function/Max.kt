@file:Suppress("unused")

/** 最大值函数符号 / Maximum function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionStructure
import fuookami.ospf.kotlin.core.model.intermediate.MaxStructure
import fuookami.ospf.kotlin.core.model.intermediate.generateMaxConstraints
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

private fun <V> applyExtremumBounds(
    resultVar: RealVar,
    polynomials: List<LinearPolynomial<V>>,
    converter: IntoValue<V>,
    minimum: Boolean
) where V : RealNumber<V>, V : NumberField<V> {
    val bounds = polynomials.map { it.finiteBounds(converter) }
    if (bounds.any { it == null }) {
        return
    }

    val finiteBounds = bounds.filterNotNull()
    val resultLower = if (minimum) {
        finiteBounds.map { it.lower }
            .reduce { acc, value -> if (value ls acc) value else acc }
    } else {
        finiteBounds.map { it.lower }
            .reduce { acc, value -> if (value gr acc) value else acc }
    }
    val resultUpper = if (minimum) {
        finiteBounds.map { it.upper }
            .reduce { acc, value -> if (value ls acc) value else acc }
    } else {
        finiteBounds.map { it.upper }
            .reduce { acc, value -> if (value gr acc) value else acc }
    }

    resultVar.range.geq(converter.fromValue(resultLower))
    resultVar.range.leq(converter.fromValue(resultUpper))
}

/**
 * 最大/最小值函数符号 / Max/Min function symbols
 *
 * 提供 [MaxFunction]、[MinFunction]、[MinMaxFunction]、[MaxMinFunction]，
 * 用于最大值和最小值的线性化建模。 / Provides [MaxFunction], [MinFunction], [MinMaxFunction], and [MaxMinFunction]
 * for linearized modeling of maximum and minimum values.
*/

// ========== Max Function ==========

/**
 * 最大值函数 / Maximum function
 *
 * y = max(p1, p2, ..., pn)，使用 Big-M 方法线性化。
 *
 * y = max(p1, p2, ..., pn), linearized using Big-M method.
 *
 * @property polynomials 输入线性多项式列表 / List of input linear polynomials
 * @property resultVar 结果变量 / Result variable
 * @param bigM Big-M 界限（默认从候选范围推导，失败时回退到 1e6）/ Big-M bound (inferred from candidate ranges by default, falls back to 1e6)
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
*/
class MaxFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "max",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val explicitBigM: V? = bigM
    private val n = polynomials.size

    init {
        require(n >= 1) { "MaxFunction requires at least one input polynomial" }
    }

    val resultVar: RealVar = RealVar("${name}_max")
    val selectorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_max_sel${it}") }

    init {
        applyExtremumBounds(resultVar, polynomials, converter, minimum = false)
    }

    private fun automaticBigMValues(bounds: List<LinearPolynomialBounds<V>?>): List<V> {
        if (bounds.all { it != null }) {
            val finiteBounds = bounds.map { it!! }
            var maximumUpper = finiteBounds.first().upper
            for (bound in finiteBounds.drop(1)) {
                if (bound.upper gr maximumUpper) {
                    maximumUpper = bound.upper
                }
            }
            return finiteBounds.map { bound ->
                ensurePositiveBigM(maximumUpper - bound.lower, converter)
            }
        }
        val fallback = polynomials.defaultBigM(converter)
        return List(n) { fallback }
    }

    private fun currentBigMValues(): List<V> {
        val explicit = explicitBigM
        if (explicit != null) {
            return List(n) { explicit }
        }
        return automaticBigMValues(polynomials.map { it.finiteBounds(converter) })
    }

    override fun deferredStructure(): DeferredFunctionStructure {
        val snapshotInputs = polynomials.map { input ->
            LinearPolynomial(
                monomials = input.monomials.map { LinearMonomial(it.coefficient, it.symbol) },
                constant = input.constant
            )
        }
        val capturedInputBounds = if (explicitBigM == null) {
            polynomials.map { it.finiteBounds(converter) }
                .takeIf { bounds -> bounds.all { it != null } }
        } else {
            null
        }
        return MaxStructure(
            inputs = snapshotInputs,
            resultVariable = resultVar,
            selectorVariables = selectorVars.toList(),
            bigMValues = capturedInputBounds?.let { automaticBigMValues(it) } ?: currentBigMValues(),
            converter = converter,
            name = name,
            capturedInputBounds = capturedInputBounds
        )
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + selectorVars

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun evaluate(values: Map<Symbol, V>): V? {
        var maxVal: V? = null
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            if (maxVal == null || v gr maxVal) {
                maxVal = v
            }
        }
        return maxVal
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        return when (val result = generateMaxConstraints(
            inputs = polynomials,
            resultVariable = resultVar,
            selectorVariables = selectorVars,
            bigMValues = currentBigMValues(),
            converter = converter,
            name = name
        )) {
            is Ok -> addConstraints(model, result.value)?.let { it } ?: ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }
    companion object {
        /** 创建 [MaxFunction] 实例。 / Create a [MaxFunction] instance. */
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String = "max",
            displayName: String? = null
        ): MaxFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MaxFunction(polynomials, bigM, converter, name, displayName)

        /** 从线性中间符号列表创建 [MaxFunction] 适配器。 / Create a [MaxFunction] adapter from linear intermediate symbols. */
        @JvmStatic
        @JvmName("fromSymbols")
        fun <V> fromSymbols(
            polynomials: List<LinearIntermediateSymbol<V>>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> = LinearFunctionSymbolAdapter(
            MaxFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                bigM = bigM,
                converter = converter,
                name = name,
                displayName = displayName
            ),
            converter = converter
        )
    }
}

// ========== Min Function ==========

/**
 * 最小值函数 / Minimum function
 *
 * y = min(p1, p2, ..., pn)，使用 Big-M 方法线性化。
 *
 * y = min(p1, p2, ..., pn), linearized using Big-M method.
 *
 * @property polynomials 输入线性多项式列表 / List of input linear polynomials
 * @property resultVar 结果变量 / Result variable
 * @param bigM Big-M 界限（默认从候选范围推导，失败时回退到 1e6）/ Big-M bound (inferred from candidate ranges by default, falls back to 1e6)
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
*/
class MinFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "min",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val explicitBigM: V? = bigM
    private val n = polynomials.size

    init {
        require(n >= 1) { "MinFunction requires at least one input polynomial" }
    }

    val resultVar: RealVar = RealVar("${name}_min")
    val selectorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_min_sel${it}") }

    init {
        applyExtremumBounds(resultVar, polynomials, converter, minimum = true)
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + selectorVars

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun deferredStructure(): DeferredFunctionStructure {
        val snapshotInputs = polynomials.map { input ->
            LinearPolynomial(
                monomials = input.monomials.map { LinearMonomial(it.coefficient, it.symbol) },
                constant = input.constant
            )
        }
        val capturedInputBounds = if (explicitBigM == null) {
            polynomials.map { it.finiteBounds(converter) }
                .takeIf { bounds -> bounds.all { it != null } }
        } else null
        val bigMValues = if (capturedInputBounds != null) {
            val lower = capturedInputBounds.map { it!!.lower }
                .reduce { acc, value -> if (value ls acc) value else acc }
            capturedInputBounds.map { ensurePositiveBigM(it!!.upper - lower, converter) }
        } else if (explicitBigM != null) {
            List(n) { explicitBigM }
        } else {
            List(n) { polynomials.defaultBigM(converter) }
        }
        return MaxStructure(
            inputs = snapshotInputs,
            resultVariable = resultVar,
            selectorVariables = selectorVars.toList(),
            bigMValues = bigMValues,
            converter = converter,
            name = name,
            capturedInputBounds = capturedInputBounds,
            minimum = true
        )
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        var minVal: V? = null
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            if (minVal == null || v ls minVal) {
                minVal = v
            }
        }
        return minVal
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val zero = converter.zero
        val one = converter.one
        val resultMon = LinearMonomial(one, resultVar)
        val allConstraints = mutableListOf<LinearInequality<V>>()
        val bounds = if (explicitBigM == null) {
            polynomials.map { it.finiteBounds(converter) }.takeIf { it.all { bound -> bound != null } }
        } else null
        val minLower = bounds?.map { it!!.lower }?.reduce { acc, value -> if (value ls acc) value else acc }
        for (i in polynomials.indices) {
            val poly = polynomials[i]
            val ubMonos = listOf(resultMon) + poly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
            allConstraints += LinearInequality(LinearPolynomial(ubMonos, -poly.constant), LinearPolynomial(emptyList(), zero), Comparison.LE)
        }
        for (i in polynomials.indices) {
            val poly = polynomials[i]
            val currentBigM = explicitBigM ?: if (bounds != null && minLower != null) {
                ensurePositiveBigM(bounds[i]!!.upper - minLower, converter)
            } else polynomials.defaultBigM(converter)
            val lbMonos = listOf(resultMon) + poly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(-currentBigM, selectorVars[i])
            allConstraints += LinearInequality(LinearPolynomial(lbMonos, -poly.constant), LinearPolynomial(emptyList(), -currentBigM), Comparison.GE)
        }
        allConstraints += LinearInequality(
            LinearPolynomial(selectorVars.map { LinearMonomial(one, it) }, zero),
            LinearPolynomial(emptyList(), one), Comparison.EQ
        )
        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        /** 创建 [MinFunction] 实例。 / Create a [MinFunction] instance. */
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String = "min",
            displayName: String? = null
        ): MinFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MinFunction(polynomials, bigM, converter, name, displayName)

        /** 从线性中间符号列表创建 [MinFunction] 适配器。 / Create a [MinFunction] adapter from linear intermediate symbols. */
        @JvmStatic
        @JvmName("fromSymbols")
        fun <V> fromSymbols(
            polynomials: List<LinearIntermediateSymbol<V>>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> = LinearFunctionSymbolAdapter(
            MinFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                bigM = bigM,
                converter = converter,
                name = name,
                displayName = displayName
            ),
            converter = converter
        )
    }
}
