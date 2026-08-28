@file:Suppress("unused")

/** 条件函数符号 / If condition function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*

/**
 * 条件函数符号 / If function symbol
 *
 * 提供 [IfFunction]，实现按比较关系选择 0/1 的线性化建模。
 *
 * Provides [IfFunction] for relation-aware linearized modeling of a binary result.
*/

/**
 * 条件函数：满足 [relation] 时 y = 1，否则 y = 0。
 * If function: `y = 1` when [relation] is satisfied, and `y = 0` otherwise.
 *
 * 使用条件范围驱动的关系指示约束。 / Uses range-driven relation-indicator constraints.
 *
 * @property condition 条件线性多项式 / the condition linear polynomial
 * @property relation 条件比较关系，默认 [Comparison.GT] / condition comparison relation, defaulting to [Comparison.GT]
 * @property strictBoundary 真、假分支之间的业务间隔 / business gap between the true and false branches
 * @property delta 离散步长，缺省取 [strictBoundary] / discrete step, defaulting to [strictBoundary]
 * @param converter 值类型转换器 / value type converter
 * @param bigM 旧版 Big-M 参数，仅用于源码兼容；不能替代范围 / legacy Big-M parameter kept for source compatibility; it cannot replace bounds
 * @param tolerance 兼容旧调用的容差（默认 [NONZERO_TOLERANCE]）/ legacy tolerance (default [NONZERO_TOLERANCE])
 * @param bounds 条件多项式的显式有限范围（兼容名称）/ explicit finite bounds of the condition polynomial (compatibility name)
 * @param conditionBounds 条件多项式的显式有限范围 / explicit finite bounds of the condition polynomial
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
*/
class IfFunction<V>(
    val condition: LinearPolynomial<V>,
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "if",
    override var displayName: String? = null,
    val relation: Comparison = Comparison.GT,
    bounds: ConditionBounds<V>? = null,
    conditionBounds: ConditionBounds<V>? = null,
    delta: V? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val explicitBigM: V? = bigM
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    val strictBoundary: V = strictBoundary ?: this.tolerance
    private val requestedDelta: V? = delta
    val delta: V = requestedDelta ?: this.strictBoundary
    private val declaredConditionBounds: ConditionBounds<V>? = conditionBounds ?: bounds

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_if")
    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_if_nz")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, indicatorVar)

    override val resultPolynomial: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)
    }

    val result: LinearPolynomial<V> get() = resultPolynomial

    private fun isUsableBound(value: V): Boolean {
        return isUsableConditionBound(value, converter)
    }

    private fun usableBounds(bounds: ConditionBounds<V>): ConditionBounds<V>? {
        return if (isUsableBound(bounds.lower) &&
            isUsableBound(bounds.upper) &&
            bounds.lower.compareTo(bounds.upper) <= 0
        ) {
            bounds
        } else {
            null
        }
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        return when (val classification = classify(values)) {
            is Ok -> when (classification.value) {
                TruthValue.True -> converter.one
                TruthValue.False -> converter.zero
                TruthValue.Undefined -> null
            }
            is Failed, is Fatal -> null
        }
    }

    /**
     * 对给定符号值执行统一三值条件判定。 / Classify the condition using the shared three-valued semantics.
     *
     * @param values 符号到值的映射 / mapping from symbols to values
     * @return 条件判定结果；缺失输入或非法关系时失败 / condition result; failure for missing input or an invalid relation
    */
    fun classify(values: Map<Symbol, V>): Ret<TruthValue> {
        return try {
            val condValue = condition.evaluateWith(values)
                ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "条件输入缺失，无法判定。 / A condition input is missing, so the condition cannot be classified."
                )
            classifyDiscreteCondition(
                d = condValue,
                relation = relation,
                delta = delta,
                strictBoundary = strictBoundary
            )
        } catch (error: RuntimeException) {
            val detail = error.message?.takeIf { it.isNotBlank() }
                ?: error::class.simpleName
                ?: "unknown runtime error"
            Failed(
                ErrorCode.IllegalArgument,
                "条件判定失败：$detail / Condition classification failed: $detail"
            )
        }
    }

    private fun resolveBounds(): Ret<ConditionBounds<V>> {
        declaredConditionBounds?.let { bounds ->
            return usableBounds(bounds)?.let { Ok(it) } ?: Failed(
                ErrorCode.IllegalArgument,
                "显式条件范围必须为有限且有序的 solver 范围。 / Explicit condition bounds must be finite and ordered for the solver."
            )
        }
        val inferred = try {
            condition.finiteBounds(converter)
        } catch (_: RuntimeException) {
            null
        }?.let {
            usableBounds(ConditionBounds(it.lower, it.upper))
        }
        inferred?.let { return Ok(it) }
        return Failed(
            ErrorCode.IllegalArgument,
            "条件范围缺失：请迁移调用并提供 ConditionBounds；旧版 bigM 不能替代范围。 / Condition bounds are missing: migrate the call to provide ConditionBounds; the legacy bigM cannot replace them."
        )
    }

    private fun precheck(): Ret<ConditionBounds<V>> {
        explicitBigM?.let { value ->
            if (!isUsableConditionBound(value, converter)) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "bigM 必须为有限且可编码的 solver 值；它仍不能替代条件范围。 / bigM must be finite and solver-representable; it still cannot replace the condition bounds."
                )
            }
            val zero = value - value
            if (value.compareTo(zero) <= 0) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "bigM 必须大于 0；它仍不能替代条件范围。 / bigM must be greater than 0; it still cannot replace the condition bounds."
                )
            }
        }
        if (!hasUsableConditionFlattenedValues(condition, converter)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "条件多项式的系数和常数必须为有限且可编码的 solver 值。 / Condition polynomial coefficients and constant must be finite and solver-representable."
            )
        }
        when (val validation = validateDiscreteConditionParameters(
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        if (!isUsableConditionBound(strictBoundary, converter) ||
            !isUsableConditionBound(delta, converter)
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "strictBoundary 和 delta 必须是有限且可编码的 solver 值。 / strictBoundary and delta must be finite solver-representable values."
            )
        }
        val resolved = when (val result = resolveBounds()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return when (val validation = normalizeDiscreteCondition(
            poly = condition,
            bounds = resolved,
            relation = relation,
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> Ok(resolved)
            is Failed -> Failed(validation.error)
            is Fatal -> Fatal(validation.errors)
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        when (val validation = precheck()) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 构建条件指示约束列表。 / Build the list of condition indicator constraints.
     *
     * @return 线性不等式约束列表 / list of linear inequality constraints
    */
    private fun buildConstraints(
        bounds: ConditionBounds<V>
    ): Ret<List<LinearInequality<V>>> {
        val zero = converter.zero
        val one = converter.one
        val allConstraints = mutableListOf<LinearInequality<V>>()

        val normalized = when (val result = normalizeDiscreteCondition(
            poly = condition,
            relation = relation,
            bounds = bounds,
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        normalized.fixedValue?.let { fixedValue ->
            val value = if (fixedValue == TruthValue.True) one else zero
            return Ok(
                listOf(
                    fixedVariableEquality(
                        variable = indicatorVar,
                        value = value,
                        zero = zero,
                        one = one,
                        name = "${name}_if_fold_indicator"
                    ),
                    fixedVariableEquality(
                        variable = resultVar,
                        value = value,
                        zero = zero,
                        one = one,
                        name = "${name}_if_fold_result"
                    )
                )
            )
        }

        val indicatorConstraints = when (val result = relationIndicatorConstraints(
            poly = normalized.polynomial,
            indicator = indicatorVar,
            relation = Comparison.GT,
            bounds = normalized.bounds,
            strictBoundary = strictBoundary,
            namePrefix = "${name}_if"
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        allConstraints += indicatorConstraints

        // Keep result = indicator for the stable public result contract. / 保持 result = indicator，稳定公开结果契约。
        allConstraints += LinearInequality(
            LinearPolynomial(
                listOf(LinearMonomial(one, resultVar), LinearMonomial(-one, indicatorVar)),
                zero
            ),
            LinearPolynomial(emptyList(), zero),
            Comparison.EQ, "${name}_if_eq"
        )

        return Ok(allConstraints)
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val bounds = when (val validation = precheck()) {
            is Ok -> validation.value
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        val constraints = when (val result = buildConstraints(bounds)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        addConstraints(model, constraints)?.let { return it }
        return ok
    }

    companion object {
        /**
         * 创建条件函数实例 / Create an if function instance
         * @param condition 条件线性多项式 / condition linear polynomial
         * @param converter 值类型转换器 / value type converter
         * @param bigM Big-M 界限 / Big-M bound
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @param relation 条件比较关系 / condition comparison relation
         * @param tolerance 兼容旧调用的容差 / legacy tolerance
         * @param strictBoundary 真、假分支之间的业务间隔 / business gap between true and false branches
         * @param bounds 条件多项式的显式有限范围（兼容名称）/ explicit finite bounds of the condition polynomial (compatibility name)
         * @param conditionBounds 条件多项式的显式有限范围 / explicit finite bounds of the condition polynomial
         * @param delta 离散步长 / discrete step
         * @return [IfFunction] 实例 / [IfFunction] instance
        */
        operator fun <V> invoke(
            condition: LinearPolynomial<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null,
            relation: Comparison = Comparison.GT,
            tolerance: V? = null,
            strictBoundary: V? = null,
            bounds: ConditionBounds<V>? = null,
            conditionBounds: ConditionBounds<V>? = null,
            delta: V? = null
        ): IfFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            IfFunction(
                condition = condition,
                converter = converter,
                bigM = bigM,
                tolerance = tolerance,
                strictBoundary = strictBoundary,
                name = name,
                displayName = displayName,
                relation = relation,
                bounds = bounds,
                conditionBounds = conditionBounds,
                delta = delta
            )

        /**
         * 约束输入工厂：从约束输入提取条件多项式。 / Constraint-input factory: extracts the condition polynomial from the constraint input.
         * @param inequality 约束输入 / constraint input
         * @param converter 值类型转换器 / value type converter
         * @param bigM Big-M 界限 / Big-M bound
         * @param tolerance 零容差 / zero tolerance
         * @param strictBoundary 严格边界值 / strict boundary value
         * @param bounds 条件多项式的显式有限范围（兼容名称）/ explicit finite bounds of the condition polynomial (compatibility name)
         * @param conditionBounds 条件多项式的显式有限范围 / explicit finite bounds of the condition polynomial
         * @param delta 离散步长 / discrete step
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return 包装后的线性函数符号适配器 / wrapped linear function symbol adapter
        */
        fun <V> from(
            inequality: LinearConstraintInput<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            tolerance: V? = null,
            strictBoundary: V? = null,
            name: String,
            displayName: String? = null,
            bounds: ConditionBounds<V>? = null,
            conditionBounds: ConditionBounds<V>? = null,
            delta: V? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> {
            val conditionPoly = LinearPolynomial(
                inequality.flattenData.monomials.map { LinearMonomial(it.coefficient, it.symbol) },
                inequality.flattenData.constant
            )
            return LinearFunctionSymbolAdapter(
                IfFunction(
                    condition = conditionPoly,
                    converter = converter,
                    bigM = bigM,
                    tolerance = tolerance,
                    strictBoundary = strictBoundary,
                    relation = inequality.sign,
                    bounds = bounds,
                    conditionBounds = conditionBounds,
                    delta = delta,
                    name = name,
                    displayName = displayName
                ),
                converter = converter
            )
        }
    }
}
