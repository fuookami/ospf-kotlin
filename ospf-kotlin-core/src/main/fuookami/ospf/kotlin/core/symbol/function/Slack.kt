@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 松弛变量函数符号 / Slack variable function symbol
 *
 * 提供 [SlackFunction]，为不等式引入正/负松弛变量。
 *
 * Provides [SlackFunction] for introducing positive/negative slack variables for inequalities.
 */

/**
 * 松弛变量函数 / Slack variable function
 *
 * 为不等式 x <= y 引入正/负松弛变量，使得 x + neg - pos = y。
 *
 * Introduces positive/negative slack variables for inequality x <= y, so that x + neg - pos = y.
 *
 * @property x 左侧多项式 / Left-hand side polynomial
 * @property y 右侧多项式 / Right-hand side polynomial
 * @property type 松弛变量类型 / Slack variable type
 * @property withNegative 是否包含负松弛 / Whether to include negative slack
 * @property withPositive 是否包含正松弛 / Whether to include positive slack
 * @property negVar 负松弛变量 / Negative slack variable
 * @property posVar 正松弛变量 / Positive slack variable
 */
class SlackFunction<V>(
    val x: LinearPolynomial<V>,
    val y: LinearPolynomial<V>,
    val type: VariableTypeKind = UContinuous,
    val withNegative: Boolean = true,
    val withPositive: Boolean = true,
    val threshold: Boolean = false,
    val constraint: Boolean = true,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    init {
        require(withNegative || withPositive) { "At least one of withNegative or withPositive must be true" }
    }

    internal val negVar: AbstractVariableItem<*, *>? by lazy {
        if (withNegative) createVariable("${name}_neg") else null
    }
    internal val posVar: AbstractVariableItem<*, *>? by lazy {
        if (withPositive) createVariable("${name}_pos") else null
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOfNotNull(negVar, posVar)

    override val resultPolynomial: LinearPolynomial<V> by lazy {
        val monomials = buildList {
            if (withNegative && negVar != null) {
                add(LinearMonomial(converter.one, negVar!!))
            }
            if (withPositive && posVar != null) {
                add(LinearMonomial(converter.one, posVar!!))
            }
        }
        LinearPolynomial(monomials, converter.zero)
    }

    val neg: LinearPolynomial<V>? by lazy {
        negVar?.let { v ->
            LinearPolynomial(listOf(LinearMonomial(converter.one, v)), converter.zero)
        }
    }

    val pos: LinearPolynomial<V>? by lazy {
        posVar?.let { v ->
            LinearPolynomial(listOf(LinearMonomial(converter.one, v)), converter.zero)
        }
    }

    private fun createVariable(baseName: String): AbstractVariableItem<*, *> {
        return if (type.isIntegerType) UIntVar(baseName) else URealVar(baseName)
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xValue = x.evaluateWith(values) ?: return null
        val yValue = y.evaluateWith(values) ?: return null
        val diff = xValue - yValue
        return if (withNegative && withPositive) {
            diff.abs()
        } else if (withNegative) {
            if (diff ls converter.zero) -diff else converter.zero
        } else if (withPositive) {
            if (diff gr converter.zero) diff else converter.zero
        } else {
            converter.zero
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        if (!constraint) {
            return ok
        }
        val one = converter.one
        val constraints = mutableListOf<LinearInequality<V>>()

        if (!threshold) {
            constraints += LinearInequality(polyX, y, Comparison.EQ, name)
        } else {
            if (withNegative && negVar != null) {
                val lhs = LinearPolynomial(x.monomials + LinearMonomial(one, negVar!!), x.constant)
                constraints += LinearInequality(lhs, y, Comparison.GE, "${name}_neg")
            } else if (withPositive && posVar != null) {
                val lhs = LinearPolynomial(x.monomials + LinearMonomial(-one, posVar!!), x.constant)
                constraints += LinearInequality(lhs, y, Comparison.LE, "${name}_pos")
            }
        }
        return addConstraints(model, constraints) ?: ok
    }
    val polyX: LinearPolynomial<V> by lazy {
        val unit = converter.one
        var result = LinearPolynomial(x.monomials.toMutableList(), x.constant)
        if (withNegative && negVar != null) {
            result = LinearPolynomial(result.monomials + LinearMonomial(unit, negVar!!), result.constant)
        }
        if (withPositive && posVar != null) {
            result = LinearPolynomial(result.monomials + LinearMonomial(-unit, posVar!!), result.constant)
        }
        result
    }

    companion object {
        /** Generic V-typed invoke: primary entry point with x and y polynomials. */
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            y: LinearPolynomial<V>,
            type: VariableTypeKind = UContinuous,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            constraint: Boolean = true,
            converter: IntoValue<V>,
            name: String? = null,
            displayName: String? = null
        ): SlackFunction<V> where V : RealNumber<V>, V : NumberField<V> {
            return SlackFunction(
                x = x,
                y = y,
                type = type,
                withNegative = withNegative,
                withPositive = withPositive,
                threshold = threshold,
                constraint = constraint,
                converter = converter,
                name = name ?: "",
                displayName = displayName
            )
        }

        /** Generic V-typed invoke with LinearIntermediateSymbol<V>. */
        operator fun <V> invoke(
            x: LinearIntermediateSymbol<V>,
            y: LinearPolynomial<V>,
            type: VariableTypeKind = UContinuous,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            constraint: Boolean = true,
            converter: IntoValue<V>,
            name: String? = null,
            displayName: String? = null
        ): SlackFunction<V> where V : RealNumber<V>, V : NumberField<V> {
            return invoke(
                x = x.polynomial,
                y = y,
                type = type,
                withNegative = withNegative,
                withPositive = withPositive,
                threshold = threshold,
                constraint = constraint,
                converter = converter,
                name = name,
                displayName = displayName
            )
        }

        /** Generic V-typed invoke with ToLinearPolynomial<V>. */
        operator fun <V> invoke(
            x: ToLinearPolynomial<V>,
            y: ToLinearPolynomial<V>,
            type: VariableTypeKind = UContinuous,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            constraint: Boolean = true,
            converter: IntoValue<V>,
            name: String? = null,
            displayName: String? = null
        ): SlackFunction<V> where V : RealNumber<V>, V : NumberField<V> {
            return invoke(
                x = x.toLinearPolynomial(),
                y = y.toLinearPolynomial(),
                type = type,
                withNegative = withNegative,
                withPositive = withPositive,
                threshold = threshold,
                constraint = constraint,
                converter = converter,
                name = name,
                displayName = displayName
            )
        }

    }
}
