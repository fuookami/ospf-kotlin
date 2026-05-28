/**
 * 最大/最小值函数符号 / Max/Min function symbols
 *
 * 提供 [MaxFunction]、[MinFunction]、[MinMaxFunction]、[MaxMinFunction]，
 * 用于最大值和最小值的线性化建模。
 *
 * Provides [MaxFunction], [MinFunction], [MinMaxFunction], and [MaxMinFunction]
 * for linearized modeling of maximum and minimum values.
 */
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*

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
 */
class MaxFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "max",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val n = polynomials.size

    init {
        require(n >= 1) { "MaxFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_max")
    val selectorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_max_sel${it}") }

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
        val zero = converter.zero
        val one = converter.one
        val resultMon = LinearMonomial(one, resultVar)
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // result >= poly[i] for each i
        for (i in polynomials.indices) {
            val poly = polynomials[i]
            val lbMonos = listOf(resultMon) + poly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
            allConstraints += LinearInequality(
                LinearPolynomial(lbMonos, -poly.constant),
                LinearPolynomial(emptyList(), zero), Comparison.GE)
        }

        // result - poly[i] + M*sel[i] <= M
        for (i in polynomials.indices) {
            val poly = polynomials[i]
            val ubMonos = listOf(resultMon) +
                poly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(bigM, selectorVars[i])
            allConstraints += LinearInequality(
                LinearPolynomial(ubMonos, -poly.constant),
                LinearPolynomial(emptyList(), bigM), Comparison.LE)
        }

        // sum(sel[i]) = 1
        val selMonos = selectorVars.map { LinearMonomial(one, it) }
        allConstraints += LinearInequality(
            LinearPolynomial(selMonos, zero),
            LinearPolynomial(emptyList(), one), Comparison.EQ)

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String = "max",
            displayName: String? = null
        ): MaxFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MaxFunction(polynomials, bigM, converter, name, displayName)

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
 */
class MinFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "min",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val n = polynomials.size

    init {
        require(n >= 1) { "MinFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_min")
    val selectorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_min_sel${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + selectorVars

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

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

        // result <= poly[i] for each i
        for (i in polynomials.indices) {
            val poly = polynomials[i]
            val ubMonos = listOf(resultMon) + poly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
            allConstraints += LinearInequality(
                LinearPolynomial(ubMonos, -poly.constant),
                LinearPolynomial(emptyList(), zero), Comparison.LE)
        }

        // result - poly[i] + M*sel[i] >= 0
        for (i in polynomials.indices) {
            val poly = polynomials[i]
            val lbMonos = listOf(resultMon) +
                poly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(bigM, selectorVars[i])
            allConstraints += LinearInequality(
                LinearPolynomial(lbMonos, -poly.constant),
                LinearPolynomial(emptyList(), zero), Comparison.GE)
        }

        // sum(sel[i]) = 1
        val selMonos = selectorVars.map { LinearMonomial(one, it) }
        allConstraints += LinearInequality(
            LinearPolynomial(selMonos, zero),
            LinearPolynomial(emptyList(), one), Comparison.EQ)

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String = "min",
            displayName: String? = null
        ): MinFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MinFunction(polynomials, bigM, converter, name, displayName)

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
