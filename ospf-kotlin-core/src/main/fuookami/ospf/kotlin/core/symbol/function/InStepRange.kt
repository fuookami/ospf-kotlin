@file:Suppress("unused")

/** 阶梯范围函数符号 / Step range function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

private const val MAX_STEP_POINTS: Int = 4096

/**
 * 步进区间函数符号 / In-step-range function symbol
 *
 * 提供 [InStepRangeFunction]，计算 y = lb + floor((x - lb) / step) * step 的线性化建模。
 *
 * Provides [InStepRangeFunction] for linearized modeling of y = lb + floor((x - lb) / step) * step.
 */

/**
 * 步进区间函数：y = lb + floor((ub - lb) / step) * step。
 * In-Step-Range function: `y = lb + floor((ub - lb) / step) * step`.
 *
 * 查找满足以下条件的最大值 y： / Finds the largest value y such that:
 * - y >= lb
 * - y <= ub
 * - y = lb + n * step，其中 n 为 >= 0 的整数
 * - y = lb + n * step for some integer n >= 0
 *
 * 委托给 FloorFunction 进行商计算。
 * Delegates to FloorFunction for the quotient computation.
 *
 * @property lb 下界线性多项式 / the lower bound linear polynomial
 * @property ub 上界线性多项式 / the upper bound linear polynomial
 * @property step 步长（必须为正，默认 1）/ the step size (must be positive, default 1)
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class InStepRangeFunction<V>(
    val lb: LinearPolynomial<V>,
    val ub: LinearPolynomial<V>,
    val step: V,
    private val converter: IntoValue<V>,
    override var name: String = "inStepRange",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    init {
        require(step.isFinite() && step gr converter.zero) {
            "InStepRangeFunction requires a positive finite step"
        }
        require(
            lb.constant.isFinite() && ub.constant.isFinite() &&
                lb.monomials.all { it.coefficient.isFinite() } &&
                ub.monomials.all { it.coefficient.isFinite() }
        ) {
            "InStepRangeFunction bounds must contain finite values"
        }
        require(hasFiniteScaledDifference()) {
            "InStepRangeFunction scaled bounds must contain finite values"
        }
    }

    /** Validate the quotient expression before any helper is created. / 在创建辅助变量前校验商表达式。 */
    private fun hasFiniteScaledDifference(): Boolean {
        return try {
            val delta = ub.constant - lb.constant
            delta.isFinite() &&
                (delta / step).isFinite() &&
                ub.monomials.all { (it.coefficient / step).isFinite() } &&
                lb.monomials.all { (-it.coefficient / step).isFinite() }
        } catch (_: RuntimeException) {
            false
        }
    }

    private val diff: LinearPolynomial<V> by lazy {
        LinearPolynomial(
            ub.monomials.map { LinearMonomial(it.coefficient / step, it.symbol) } +
                lb.monomials.map { LinearMonomial(-it.coefficient / step, it.symbol) },
            (ub.constant - lb.constant) / step
        )
    }

    private val floorFunc: FloorFunction<V> by lazy {
        FloorFunction(
            x = diff,
            converter = converter,
            name = "${name}_q"
        )
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = floorFunc.helperVariables

    val result: LinearPolynomial<V> by lazy {
        val qResult = floorFunc.result
        val scaledMonomials = qResult.monomials.map {
            LinearMonomial(it.coefficient * step, it.symbol)
        }
        val scaledConstant = qResult.constant * step
        LinearPolynomial(
            scaledMonomials + lb.monomials,
            scaledConstant + lb.constant
        )
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val lbValue = lb.evaluateWith(values) ?: return null
        val ubValue = ub.evaluateWith(values) ?: return null
        if (!lbValue.isFinite() || !ubValue.isFinite()) {
            return null
        }
        if (lbValue gr ubValue) {
            return null
        }
        val lbFlt = converter.fromValue(lbValue)
        val ubFlt = converter.fromValue(ubValue)
        val stepFlt = converter.fromValue(step)
        val qFlt = ((ubFlt - lbFlt) / stepFlt).floor()
        return converter.intoValue(lbFlt + qFlt * stepFlt)
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return floorFunc.registerAuxiliaryTokens(tokens)
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        when (val result = floorFunc.registerConstraints(model)) {
            is Ok -> Unit
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return addConstraints(
            model,
            listOf(LinearInequality(lb, ub, Comparison.LE, "${name}_bounds"))
        ) ?: ok
    }

    override val resultPolynomial: LinearPolynomial<V>
        get() = result
    companion object {
        /**
         * 创建步进区间函数实例 / Create an in-step-range function instance
         *
         * @param lb 下界线性多项式 / lower bound linear polynomial
         * @param ub 上界线性多项式 / upper bound linear polynomial
         * @param step 步长 / step size
         * @param converter 值类型转换器 / value type converter
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return [InStepRangeFunction] 实例 / [InStepRangeFunction] instance
         */
        operator fun <V> invoke(
            lb: LinearPolynomial<V>,
            ub: LinearPolynomial<V>,
            step: V,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): InStepRangeFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            InStepRangeFunction(lb = lb, ub = ub, step = step, converter = converter, name = name, displayName = displayName)
    }
}

/**
 * 步进集合成员指示函数：输入值属于 `{lower + k * step}` 时结果为 1。
 * Stepped-set membership indicator: returns 1 exactly on `{lower + k * step}`.
 *
 * 此类独立于 [InStepRangeFunction]，后者返回上界以下最大的步进端点；两者共享正步长、有限边界和相同边界 epsilon 契约。
 * This class is intentionally separate from [InStepRangeFunction], whose result is the largest stepped endpoint below an upper bound; both implementations use the same positive-step, finite-bound contract and boundary epsilon.
 *
 * @property input 待判断的线性多项式 / input linear polynomial
 * @property lower 步进集合下界 / lower endpoint of the stepped set
 * @property upper 步进集合上界 / upper endpoint of the stepped set
 * @property step 正步长 / positive step size
 * @param bigM 可选显式 Big-M / optional explicit Big-M
 * @param converter 值类型转换器 / value type converter
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class InStepRangeIndicatorFunction<V>(
    val input: LinearPolynomial<V>,
    val lower: V,
    val upper: V,
    val step: V,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "inStepRangeIndicator",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultVariable, HasResultPolynomial<V>
        where V : RealNumber<V>, V : NumberField<V> {
    private val explicitBigM: V? = bigM
    private val effectiveBigM: V = explicitBigM ?: input.defaultBigM(converter)

    init {
        require(lower.isFinite() && upper.isFinite() && step.isFinite()) {
            "InStepRangeIndicatorFunction bounds and step must be finite"
        }
        require(lower leq upper) {
            "InStepRangeIndicatorFunction requires lower <= upper"
        }
        require(step gr converter.zero) {
            "InStepRangeIndicatorFunction requires a positive step"
        }
        require(
            input.constant.isFinite() && input.monomials.all { it.coefficient.isFinite() }
        ) {
            "InStepRangeIndicatorFunction input polynomial must contain finite values"
        }
        require(effectiveBigM.isFinite() && effectiveBigM gr converter.zero) {
            "InStepRangeIndicatorFunction Big-M must be positive and finite"
        }
    }

    private val stepPoints: List<V> = run {
        val lowerFlt = converter.fromValue(lower)
        val upperFlt = converter.fromValue(upper)
        val stepFlt = converter.fromValue(step)
        val estimate = (
            (upperFlt - lowerFlt + Flt64(NONZERO_TOLERANCE)) / stepFlt
        ).floor()
        require(estimate.isFinite() && estimate geq Flt64.zero) {
            "InStepRangeIndicatorFunction step-point count is not finite"
        }
        require(estimate leq Flt64(MAX_STEP_POINTS - 1)) {
            "InStepRangeIndicatorFunction creates too many step points"
        }
        val count = estimate.toInt64().toInt() + 1
        (0 until count).map { index ->
            converter.intoValue(lowerFlt + stepFlt * Flt64(index))
        }
    }

    private val pointVars: List<AbstractVariableItem<*, *>> =
        stepPoints.indices.map { i -> BinVar("${name}_step_pt$i") }
    private val sideVars: List<AbstractVariableItem<*, *>> =
        stepPoints.indices.map { i -> BinVar("${name}_step_side$i") }

    override val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_result")

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(
            listOf(LinearMonomial(converter.one, resultVar)), converter.zero
        )

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + pointVars + sideVars

    /** 步进点成员指示变量 / Step-point indicator variables used by the membership encoding. */
    val pointIndicatorVars: List<AbstractVariableItem<*, *>>
        get() = pointVars

    /** 区间外容差带辅助变量 / Outside-band helper variables used by the strict complement encoding. */
    val pointSideVars: List<AbstractVariableItem<*, *>>
        get() = sideVars

    /** 显式 Big-M；使用共享默认策略时为 null / Explicit Big-M, or null when the shared default policy is used. */
    val bigM: V?
        get() = explicitBigM

    override fun evaluate(values: Map<Symbol, V>): V? {
        val value = input.evaluateWith(values) ?: return null
        if (!value.isFinite()) {
            return null
        }
        val valueFlt = converter.fromValue(value)
        val lowerFlt = converter.fromValue(lower)
        val upperFlt = converter.fromValue(upper)
        val stepFlt = converter.fromValue(step)
        val eps = Flt64(NONZERO_TOLERANCE)
        if (valueFlt + eps ls lowerFlt || valueFlt gr upperFlt + eps) {
            return converter.zero
        }
        val offset = (valueFlt - lowerFlt) / stepFlt
        return if ((offset - offset.round()).abs() leq eps) {
            converter.one
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

    private fun pointConstraint(
        index: Int,
        point: V,
        pointCoefficient: V,
        sideCoefficient: V,
        comparison: Comparison,
        rhs: V,
        suffix: String
    ): LinearInequality<V> {
        val monomials = input.monomials.map {
            LinearMonomial(it.coefficient, it.symbol)
        }.toMutableList()
        monomials += LinearMonomial(pointCoefficient, pointVars[index])
        monomials += LinearMonomial(sideCoefficient, sideVars[index])
        return LinearInequality(
            LinearPolynomial(monomials, input.constant - point),
            LinearPolynomial(emptyList(), rhs),
            comparison,
            "${name}_pt${index}_$suffix"
        )
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val one = converter.one
        val zero = converter.zero
        val stepFlt = converter.fromValue(step)
        val eps = Flt64(NONZERO_TOLERANCE)
        val toleranceFlt = stepFlt.abs() * eps + eps
        val tolerance = converter.intoValue(toleranceFlt)
        val strictBoundary = converter.intoValue(toleranceFlt + eps)
        val bigM = effectiveBigM
        val allConstraints = mutableListOf<LinearInequality<V>>()
        for ((index, point) in stepPoints.withIndex()) {
            allConstraints += pointConstraint(
                index, point, bigM, zero, Comparison.LE, tolerance + bigM, "band_ub"
            )
            allConstraints += pointConstraint(
                index, point, -bigM, zero, Comparison.GE, -tolerance - bigM, "band_lb"
            )
            allConstraints += pointConstraint(
                index, point, bigM, -bigM, Comparison.GE,
                strictBoundary - bigM, "out_lb"
            )
            allConstraints += pointConstraint(
                index, point, -bigM, -bigM, Comparison.LE, -strictBoundary, "out_ub"
            )
            allConstraints += LinearInequality(
                LinearPolynomial(
                    listOf(
                        LinearMonomial(one, resultVar),
                        LinearMonomial(-one, pointVars[index])
                    ), zero
                ),
                LinearPolynomial(emptyList(), zero),
                Comparison.GE,
                "${name}_or_lb_$index"
            )
        }
        allConstraints += LinearInequality(
            LinearPolynomial(
                listOf(LinearMonomial(one, resultVar)) +
                    pointVars.map { LinearMonomial(-one, it) }, zero
            ),
            LinearPolynomial(emptyList(), zero),
            Comparison.LE,
            "${name}_or_ub"
        )

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        /**
         * 创建步进集合成员指示函数。 / Create a stepped-set membership indicator.
         *
         * @param input 待判断的线性多项式 / input linear polynomial
         * @param lower 步进集合下界 / lower endpoint of the stepped set
         * @param upper 步进集合上界 / upper endpoint of the stepped set
         * @param step 正步长 / positive step size
         * @param bigM 可选显式 Big-M / optional explicit Big-M
         * @param converter 值类型转换器 / value type converter
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return 步进集合成员指示函数 / [InStepRangeIndicatorFunction] instance
         */
        operator fun <V> invoke(
            input: LinearPolynomial<V>,
            lower: V,
            upper: V,
            step: V,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String = "inStepRangeIndicator",
            displayName: String? = null
        ): InStepRangeIndicatorFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            InStepRangeIndicatorFunction(
                input = input,
                lower = lower,
                upper = upper,
                step = step,
                bigM = bigM,
                converter = converter,
                name = name,
                displayName = displayName
            )
    }
}
