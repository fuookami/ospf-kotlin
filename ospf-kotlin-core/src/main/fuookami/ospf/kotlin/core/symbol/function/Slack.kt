@file:Suppress("unused")

/** 松弛变量函数符号 / Slack variable function symbol */
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
 * @property threshold 是否使用阈值约束 / Whether to use threshold constraints
 * @property constraint 是否添加约束 / Whether to add constraints
 * @property negVar 负松弛变量 / Negative slack variable
 * @property posVar 正松弛变量 / Positive slack variable
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
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

    /**
     * 根据变量类型创建辅助变量。
     * Create an auxiliary variable based on the variable type.
     *
     * @param baseName 变量基础名称 / base variable name
     * @return 创建的变量项 / the created variable item
     */
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
        /**
         * 通用 V 类型调用：使用 x 和 y 多项式的主入口点。
         * Generic V-generic invoke: primary entry point with x and y polynomials.
         *
         * @param x 左侧多项式 / left-hand side polynomial
         * @param y 右侧多项式 / right-hand side polynomial
         * @param type 松弛变量类型 / slack variable type
         * @param withNegative 是否包含负松弛 / whether to include negative slack
         * @param withPositive 是否包含正松弛 / whether to include positive slack
         * @param threshold 是否使用阈值约束 / whether to use threshold constraints
         * @param constraint 是否添加约束 / whether to add constraints
         * @param converter 值类型转换器 / value type converter
         * @param name 此函数的唯一名称 / unique name for this function
         * @param displayName 可选的人类可读显示名称 / optional human-readable display name
         * @return 松弛函数实例 / slack function instance
         */
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

        /**
         * 使用 LinearIntermediateSymbol<V> 的通用 V 类型调用。
         * Generic V-generic invoke with LinearIntermediateSymbol<V>.
         *
         * @param x 左侧线性中间符号 / left-hand side linear intermediate symbol
         * @param y 右侧多项式 / right-hand side polynomial
         * @param type 松弛变量类型 / slack variable type
         * @param withNegative 是否包含负松弛 / whether to include negative slack
         * @param withPositive 是否包含正松弛 / whether to include positive slack
         * @param threshold 是否使用阈值约束 / whether to use threshold constraints
         * @param constraint 是否添加约束 / whether to add constraints
         * @param converter 值类型转换器 / value type converter
         * @param name 此函数的唯一名称 / unique name for this function
         * @param displayName 可选的人类可读显示名称 / optional human-readable display name
         * @return 松弛函数实例 / slack function instance
         */
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

        /**
         * 使用 ToLinearPolynomial<V> 的通用 V 类型调用。
         * Generic V-generic invoke with ToLinearPolynomial<V>.
         *
         * @param x 左侧可转换为线性多项式的对象 / left-hand side convertible to linear polynomial
         * @param y 右侧可转换为线性多项式的对象 / right-hand side convertible to linear polynomial
         * @param type 松弛变量类型 / slack variable type
         * @param withNegative 是否包含负松弛 / whether to include negative slack
         * @param withPositive 是否包含正松弛 / whether to include positive slack
         * @param threshold 是否使用阈值约束 / whether to use threshold constraints
         * @param constraint 是否添加约束 / whether to add constraints
         * @param converter 值类型转换器 / value type converter
         * @param name 此函数的唯一名称 / unique name for this function
         * @param displayName 可选的人类可读显示名称 / optional human-readable display name
         * @return 松弛函数实例 / slack function instance
         */
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
