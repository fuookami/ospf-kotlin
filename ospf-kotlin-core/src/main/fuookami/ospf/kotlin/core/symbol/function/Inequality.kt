@file:Suppress("unused")

/** 不等式函数符号 / Inequality function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.intermediate.ZeroBand
import fuookami.ospf.kotlin.core.model.intermediate.IndicatorStructure
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*

/**
 * 不等式满足指示函数符号 / Inequality satisfaction indicator function symbol
 *
 * 提供 [InequalityFunction]，判断不等式是否满足并返回二值指示变量。 / Provides [InequalityFunction] for checking inequality satisfaction and returning a binary indicator.
 */

/**
 * 不等式满足指示函数。 / Inequality satisfaction indicator function.
 *
 * 给定线性表达式和比较类型，返回： / Given a linear expression and a comparison type, returns:
 * - 1 若不等式满足 / if the inequality is satisfied
 * - 0 若不等式违反 / if the inequality is violated
 *
 * @property lhs 左侧线性多项式 / the left-hand side linear polynomial
 * @property rhs 右侧常数值 / the right-hand side constant value
 * @property sign 比较类型 / the comparison type
 * @param converter 值类型转换器 / value type converter
 * @param bigM Big-M 界限（默认从 lhs-rhs 范围推导，失败时回退到 1e6）/ Big-M bound (inferred from lhs-rhs range by default, falls back to 1e6)
 * @param tolerance LE/GE 的正间隔及 EQ/NE 的零容差（默认 1e-6）；EQ/NE 要求 0 <= tolerance < strictBoundary / positive LE/GE gap and EQ/NE zero tolerance (default 1e-6); EQ/NE require 0 <= tolerance < strictBoundary
 * @param strictBoundary LT/GT 真分支最小差值及 EQ/NE 带外边界（默认 0.5）；这些关系在间隔内 evaluate 返回 null / minimum LT/GT true-branch difference and EQ/NE outside-band boundary (default 0.5); evaluate returns null inside these relations' gaps
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class InequalityFunction<V>(
    val lhs: LinearPolynomial<V>,
    val rhs: V,
    val sign: Comparison,
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "ineq",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: LinearPolynomial(lhs.monomials, lhs.constant - rhs).defaultBigM(converter)
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))

    private val flagVar: AbstractVariableItem<*, *> by lazy { BinVar("${name}_flag") }
    private val sideVar: AbstractVariableItem<*, *> by lazy { BinVar("${name}_side") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = if (sign == Comparison.EQ || sign == Comparison.NE) {
            listOf(flagVar, sideVar)
        } else {
            listOf(flagVar)
        }

    val result: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, flagVar)), converter.zero)
    }

    override fun deferredStructure(): IndicatorStructure<V>? {
        if (sign == Comparison.EQ || sign == Comparison.NE) {
            return IndicatorStructure(
                input = LinearPolynomial(lhs.monomials.toList(), lhs.constant - rhs),
                resultVariable = flagVar,
                bigM = bigM,
                tolerance = strictBoundary,
                converter = converter,
                name = "${name}_${if (sign == Comparison.EQ) "eq" else "ne"}",
                positiveOnZero = sign == Comparison.EQ,
                zeroBand = ZeroBand(tolerance, sideVar)
            )
        }
        val input = if (sign == Comparison.LT || sign == Comparison.GE) {
            LinearPolynomial(lhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }, rhs - lhs.constant)
        } else {
            LinearPolynomial(lhs.monomials.toList(), lhs.constant - rhs)
        }
        return IndicatorStructure(
            input = input,
            resultVariable = flagVar,
            bigM = bigM,
            tolerance = if (sign == Comparison.LE || sign == Comparison.GE) tolerance else strictBoundary,
            converter = converter,
            name = name,
            positiveOnZero = sign == Comparison.LE || sign == Comparison.GE
        )
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val lhsValue = lhs.evaluateWith(values) ?: return null
        if (sign == Comparison.EQ || sign == Comparison.NE) {
            if (!validEqualityBand() || !isUsableConditionBound(lhsValue, converter) ||
                !isUsableConditionBound(rhs, converter)
            ) {
                return null
            }
            val distance = (lhsValue - rhs).abs()
            if (!isUsableConditionBound(distance, converter)) {
                return null
            }
            val equal = when {
                distance.compareTo(tolerance) <= 0 -> true
                distance.compareTo(strictBoundary) >= 0 -> false
                else -> return null
            }
            return if (equal == (sign == Comparison.EQ)) converter.one else converter.zero
        }
        return when (val classification = classify(
            d = lhsValue - rhs,
            relation = sign,
            strictBoundary = if (sign == Comparison.LE || sign == Comparison.GE) tolerance else strictBoundary
        )) {
            is Ok -> when (classification.value) {
                TruthValue.True -> converter.one
                TruthValue.False -> converter.zero
                TruthValue.Undefined -> null
            }
            is Failed, is Fatal -> null
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        when (val constraints = checkedConstraints()) {
            is Ok -> Unit
            is Failed -> return Failed(constraints.error)
            is Fatal -> return Fatal(constraints.errors)
        }
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val constraints = when (val result = checkedConstraints()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        addConstraints(model, constraints)?.let { return it }
        return ok
    }

    private fun checkedConstraints(): Ret<List<LinearInequality<V>>> {
        return try {
            if (!isUsableConditionBound(bigM, converter) || bigM.compareTo(converter.zero) <= 0 ||
                !isUsableConditionBound(rhs, converter) || !hasUsableConditionSolverValues(lhs, converter)
            ) {
                return Failed(ErrorCode.IllegalArgument, "比较输入或 Big-M 无效。 / Invalid comparison input or Big-M.")
            }
            if ((sign == Comparison.LE || sign == Comparison.GE) &&
                (!isUsableConditionBound(tolerance, converter) || tolerance.compareTo(converter.zero) <= 0)
            ) {
                return Failed(ErrorCode.IllegalArgument, "非严格比较需要正有限 tolerance。 / Non-strict comparisons require positive finite tolerance.")
            }
            val constraints = when (val result = buildConstraints()) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            for (constraint in constraints) {
                val difference = LinearPolynomial(
                    monomials = constraint.lhs.monomials + constraint.rhs.monomials.map {
                        LinearMonomial(-it.coefficient, it.symbol)
                    },
                    constant = constraint.lhs.constant - constraint.rhs.constant
                )
                if (!hasUsableConditionSolverValues(difference, converter)) {
                    return Failed(ErrorCode.IllegalArgument, "比较约束展平后数值无效。 / Invalid flattened comparison constraint values.")
                }
            }
            Ok(constraints)
        } catch (_: RuntimeException) {
            Failed(ErrorCode.IllegalArgument, "构造比较约束失败。 / Failed to build comparison constraints.")
        }
    }

    private fun buildConstraints(): Ret<List<LinearInequality<V>>> {
        return deferredStructure()?.generateConstraints()
            ?: Failed(ErrorCode.IllegalArgument, "比较关系不支持展开。 / Unsupported comparison lowering.")
    }

    private fun validEqualityBand(): Boolean {
        return isUsableConditionBound(tolerance, converter) &&
            isUsableConditionBound(strictBoundary, converter) &&
            tolerance.compareTo(converter.zero) >= 0 &&
            strictBoundary.compareTo(tolerance) > 0
    }

    companion object {
        /**
         * 创建不等式满足指示函数实例 / Create an inequality function instance
         *
         * @param lhs 左侧线性多项式 / left-hand side linear polynomial
         * @param rhs 右侧常数值 / right-hand side constant value
         * @param sign 比较类型 / comparison type
         * @param converter 值类型转换器 / value type converter
         * @param bigM Big-M 界限 / Big-M bound
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return [InequalityFunction] 实例 / [InequalityFunction] instance
         */
        operator fun <V> invoke(
            lhs: LinearPolynomial<V>,
            rhs: V,
            sign: Comparison,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): InequalityFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            InequalityFunction(lhs = lhs, rhs = rhs, sign = sign, converter = converter, bigM = bigM, name = name, displayName = displayName)
    }
}
