package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.LinearPolynomialBounds
import fuookami.ospf.kotlin.core.symbol.function.finiteBounds
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/** 求解器无关的函数结构快照。 / Solver-independent function structure snapshot. */
interface DeferredFunctionStructure

/**
 * 结构节点拥有的连续约束区域。 / A contiguous constraint region owned by a structure.
 *
 * @property structure 所属节点 / Owning structure
 * @property firstConstraintIndex 本模型层的起始行号 / First row index in this model layer
 * @property constraintCount 区域行数 / Number of rows
 */
data class DeferredFunctionConstraintRegion(
    val structure: DeferredFunctionStructure,
    val firstConstraintIndex: Int,
    val constraintCount: Int
) {
    val lastConstraintIndex: Int
        get() = firstConstraintIndex + constraintCount
}

/**
 * 分段线性函数的结构与 fallback 证明快照。 / Piecewise structure and fallback proof snapshot.
 *
 * @property input 输入多项式 / Input polynomial
 * @property breakpoints 严格有序的断点 / Strictly ordered breakpoints
 * @property slopes 分段斜率 / Segment slopes
 * @property intercepts 分段截距 / Segment intercepts
 * @property resultVariable 对外结果变量 / Externally visible result variable
 * @property selectorVariables fallback 选择变量 / Fallback selector variables
 * @property usage 使用语境 / Usage context
 * @property explicitM 可选的显式 Big-M / Optional explicit Big-M
 * @property converter 求解器边界值转换 / Solver-boundary value conversion
 * @property name 函数名称 / Function name
 */
data class UnivariateLinearPiecewiseStructure<V>(
    val input: LinearPolynomial<V>,
    val breakpoints: List<V>,
    val slopes: List<V>,
    val intercepts: List<V>,
    val resultVariable: AbstractVariableItem<*, *>,
    val selectorVariables: List<AbstractVariableItem<*, *>>,
    val usage: FunctionUsageSummary = FunctionUsageSummary(),
    val explicitM: V? = null,
    val converter: IntoValue<V>? = null,
    val name: String = "piecewise",
    /** Automatic Big-M input bounds captured at snapshot creation; null means explicit M.
     * 在快照创建时捕获的自动 Big-M 输入范围；null 表示使用显式 M。 */
    val capturedInputBounds: Ret<LinearPolynomialBounds<V>>? = null
) : DeferredFunctionStructure where V : RealNumber<V>, V : NumberField<V> {
    internal fun validateFallbackInputBounds(): Try {
        if (explicitM != null) {
            return ok
        }
        val valueConverter = converter ?: return Failed(
            ErrorCode.IllegalArgument,
            "分段线性函数快照缺少值类型转换器。 / The piecewise snapshot has no value converter."
        )
        val captured = when (val bounds = capturedInputBounds) {
            is Ok -> bounds.value
            is Failed -> return Failed(bounds.error)
            is Fatal -> return Fatal(bounds.errors)
            null -> return Failed(
                ErrorCode.IllegalArgument,
                "分段线性函数快照缺少输入范围证明。 / The piecewise snapshot has no input bounds proof."
            )
        }
        val current = when (val bounds = capturePwlInputBounds(input, valueConverter)) {
            is Ok -> bounds.value
            is Failed -> return Failed(bounds.error)
            is Fatal -> return Fatal(bounds.errors)
        }
        if (current.lower.compareTo(captured.lower) < 0 || current.upper.compareTo(captured.upper) > 0) {
            return Failed(
                ErrorCode.IllegalArgument,
                "函数 $name 的输入范围超出已捕获的 Big-M 证明。 / Input bounds for $name exceed the captured Big-M proof."
            )
        }
        return ok
    }

    /** Convert this immutable snapshot into solver-neutral Flt64 fallback rows. / 将此不可变快照转换为与求解器无关的 Flt64 fallback 约束行。 */
    internal fun materializeFallbackConstraints(): Ret<DeferredFunctionFallbackConstraints> {
        val valueConverter = converter ?: return Failed(
            ErrorCode.IllegalArgument,
            "分段线性函数快照缺少值类型转换器。 / The piecewise snapshot has no value converter."
        )
        val inputBounds = if (explicitM == null) {
            capturedInputBounds ?: return Failed(
                ErrorCode.IllegalArgument,
                "分段线性函数快照缺少自动 Big-M 输入范围。 / The piecewise snapshot has no captured input bounds for automatic Big-M."
            )
        } else {
            null
        }
        return when (val result = generateUnivariateLinearPiecewiseConstraints(
            input = input,
            breakpoints = breakpoints,
            slopes = slopes,
            intercepts = intercepts,
            explicitM = explicitM,
            converter = valueConverter,
            resultVariable = resultVariable,
            selectorVariables = selectorVariables,
            name = name,
            inputBounds = inputBounds
        )) {
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
            is Ok -> convertPwlConstraintsToFlt64(result.value, valueConverter)
        }
    }
}

/** ABS helper range captured before later model restrictions. / ABS helper 后续模型限制前捕获的范围。 */
data class AbsHelperBoundsSnapshot(
    val lower: Flt64?,
    val upper: Flt64?
)

/**
 * ABS 函数的结构与 fallback 证明快照。 / Absolute-value structure and fallback proof snapshot.
 *
 * @property input 输入线性多项式 / Input linear polynomial
 * @property resultVariable 对外结果变量 / Externally visible result variable
 * @property positiveVariable 正部 helper / Positive-part helper
 * @property negativeVariable 负部 helper / Negative-part helper
 * @property signVariable 正负部选择变量 / Positive/negative selector
 * @property positiveBigM 正部 Big-M / Positive-part Big-M
 * @property negativeBigM 负部 Big-M / Negative-part Big-M
 * @property converter 值类型转换器 / Value converter
 * @property name 函数名称 / Function name
 */
data class AbsStructure<V>(
    val input: LinearPolynomial<V>,
    val resultVariable: AbstractVariableItem<*, *>,
    val positiveVariable: AbstractVariableItem<*, *>,
    val negativeVariable: AbstractVariableItem<*, *>,
    val signVariable: AbstractVariableItem<*, *>,
    val positiveBigM: V,
    val negativeBigM: V,
    val converter: IntoValue<V>,
    val name: String = "abs",
    val usage: FunctionUsageSummary = FunctionUsageSummary(),
    val capturedHelperBounds: Map<VariableItemKey, AbsHelperBoundsSnapshot> = emptyMap()
) : DeferredFunctionStructure where V : RealNumber<V>, V : NumberField<V> {
    val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(positiveVariable, negativeVariable, signVariable)

    internal fun inputVariableKeyOrNull(): VariableItemKey? {
        return try {
            val monomial = input.monomials.singleOrNull() ?: return null
            if (converter.fromValue(monomial.coefficient).compareTo(Flt64.one) != 0 ||
                converter.fromValue(input.constant).compareTo(Flt64.zero) != 0
            ) {
                return null
            }
            (monomial.symbol as? AbstractVariableItem<*, *>)?.key
        } catch (_: RuntimeException) {
            null
        }
    }

    internal fun solverBigMOrNull(): Pair<Flt64, Flt64>? {
        return try {
            converter.fromValue(positiveBigM) to converter.fromValue(negativeBigM)
        } catch (_: RuntimeException) {
            null
        }
    }
}

/**
 * MAX 函数的结构与 fallback 快照。 / MAX function structure and fallback snapshot.
 *
 * @property inputs 输入线性多项式 / Input linear polynomials
 * @property resultVariable 对外结果变量 / Externally visible result variable
 * @property selectorVariables 候选选择变量 / Candidate selector variables
 * @property bigMValues 每个候选对应的 Big-M / Big-M value for each candidate
 * @property converter 值类型转换器 / Value converter
 * @property name 函数名称 / Function name
 * @property usage 使用语境 / Usage context
 */
data class MaxStructure<V>(
    val inputs: List<LinearPolynomial<V>>,
    val resultVariable: AbstractVariableItem<*, *>,
    val selectorVariables: List<AbstractVariableItem<*, *>>,
    val bigMValues: List<V>,
    val converter: IntoValue<V>,
    val name: String = "max",
    val usage: FunctionUsageSummary = FunctionUsageSummary(),
    val capturedInputBounds: List<LinearPolynomialBounds<V>?>? = null,
    val minimum: Boolean = false
) : DeferredFunctionStructure where V : RealNumber<V>, V : NumberField<V> {
    val helperVariables: List<AbstractVariableItem<*, *>>
        get() = selectorVariables

    internal fun validateFallbackInputBounds(): Try {
        val captured = capturedInputBounds ?: return ok
        if (captured.size != inputs.size) {
            return Failed(
                ErrorCode.IllegalArgument,
                "MAX 快照输入范围证明数量不匹配。 / MAX snapshot input-bound proof count is inconsistent."
            )
        }
        return try {
            for ((index, original) in captured.withIndex()) {
                if (original == null) {
                    continue
                }
                val current = inputs[index].finiteBounds(converter) ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "MAX 输入范围证明在物化时丢失。 / MAX input-bound proof is unavailable during materialization."
                )
                if (current.lower.compareTo(original.lower) < 0 ||
                    current.upper.compareTo(original.upper) > 0
                ) {
                    return Failed(
                        ErrorCode.IllegalArgument,
                        "MAX 输入范围超出已捕获的 Big-M 证明。 / MAX input bounds exceed the captured Big-M proof."
                    )
                }
            }
            ok
        } catch (error: RuntimeException) {
            Failed(
                ErrorCode.IllegalArgument,
                "校验 MAX 输入范围证明失败：${error.message ?: error::class.simpleName} / Failed to validate MAX input-bound proof: ${error.message ?: error::class.simpleName}"
            )
        }
    }
}

/** 半连续变量的结构与 fallback 快照。 / Semi-continuous variable structure and fallback snapshot. */
data class SemiStructure<V>(
    val lowerBound: V,
    val upperBound: V,
    val resultVariable: AbstractVariableItem<*, *>,
    val indicatorVariable: AbstractVariableItem<*, *>,
    val converter: IntoValue<V>,
    val name: String = "semi",
    val usage: FunctionUsageSummary = FunctionUsageSummary()
) : DeferredFunctionStructure where V : RealNumber<V>, V : NumberField<V> {
    val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(indicatorVariable)

    internal fun materializeFallbackConstraints(): Ret<DeferredFunctionFallbackConstraints> {
        return try {
            val zero = converter.zero
            val one = converter.one
            val result = listOf(
                LinearInequality(
                    lhs = LinearPolynomial(
                        listOf(
                            fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(one, resultVariable),
                            fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(-upperBound, indicatorVariable)
                        ),
                        zero
                    ),
                    rhs = LinearPolynomial(emptyList(), zero),
                    comparison = fuookami.ospf.kotlin.math.symbol.inequality.Comparison.LE,
                    name = "${name}_semi_upper"
                ),
                LinearInequality(
                    lhs = LinearPolynomial(
                        listOf(
                            fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(one, resultVariable),
                            fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(-lowerBound, indicatorVariable)
                        ),
                        zero
                    ),
                    rhs = LinearPolynomial(emptyList(), zero),
                    comparison = fuookami.ospf.kotlin.math.symbol.inequality.Comparison.GE,
                    name = "${name}_semi_lower"
                )
            )
            when (val converted = convertPwlConstraintsToFlt64(result, converter)) {
                is Ok -> converted
                is Failed -> Failed(converted.error)
                is Fatal -> Fatal(converted.errors)
            }
        } catch (error: RuntimeException) {
            Failed(ErrorCode.IllegalArgument, "生成半连续 fallback 失败：${error.message} / Failed to generate semi-continuous fallback")
        }
    }
}

internal fun DeferredFunctionStructure.resultVariableOrNull(): AbstractVariableItem<*, *>? {
    return when (this) {
        is UnivariateLinearPiecewiseStructure<*> -> resultVariable
        is AbsStructure<*> -> resultVariable
        is MaxStructure<*> -> resultVariable
        is SemiStructure<*> -> resultVariable
        is BinaryLogicStructure<*> -> resultVariable
        is IndicatorStructure<*> -> resultVariable
        is MaskingStructure<*> -> resultVariable
        else -> null
    }
}

internal fun DeferredFunctionStructure.inputOrNull(): LinearPolynomial<*>? {
    return when (this) {
        is UnivariateLinearPiecewiseStructure<*> -> input
        is AbsStructure<*> -> input
        is MaxStructure<*> -> inputs.singleOrNull()
        is IndicatorStructure<*> -> input
        is MaskingStructure<*> -> value.input
        is BinaryLogicStructure<*> -> null
        else -> null
    }
}

internal fun DeferredFunctionStructure.inputPolynomialsOrEmpty(): List<LinearPolynomial<*>> {
    return when (this) {
        is UnivariateLinearPiecewiseStructure<*> -> listOf(input)
        is AbsStructure<*> -> listOf(input)
        is MaxStructure<*> -> inputs
        is IndicatorStructure<*> -> listOf(input) + listOfNotNull(conditionalValue?.input, impliedCondition?.input, conjunction?.condition?.input, difference?.condition?.input)
        is MaskingStructure<*> -> listOf(value.input, maskPolynomial) + listOfNotNull(maskDefinition)
        is BinaryLogicStructure<*> -> emptyList()
        else -> emptyList()
    }
}

internal fun DeferredFunctionStructure.helperVariablesOrEmpty(): List<AbstractVariableItem<*, *>> {
    return when (this) {
        is UnivariateLinearPiecewiseStructure<*> -> selectorVariables
        is AbsStructure<*> -> helperVariables
        is MaxStructure<*> -> helperVariables
        is SemiStructure<*> -> helperVariables
        is BinaryLogicStructure<*> -> emptyList()
        else -> emptyList()
    }
}

internal fun DeferredFunctionStructure.supportsDeferredFallback(): Boolean {
    return this is UnivariateLinearPiecewiseStructure<*> ||
        this is AbsStructure<*> ||
        this is MaxStructure<*> ||
        this is SemiStructure<*> ||
        this is BinaryLogicStructure<*> ||
        this is IndicatorStructure<*> || this is MaskingStructure<*>
}

/** 函数展开策略；默认保持 EAGER。 / Function expansion policy, defaulting to EAGER. */
enum class FunctionExpansionPolicy {
    EAGER,
    DEFERRED_NATIVE_FIRST,
    AUTO
}

/** 原生接口需要区分的输入形状。 / Input shapes distinguished by native interfaces. */
enum class FunctionInputShape {
    SingleVariable,
    LinearExpression,
    QuadraticExpression,
    Unknown
}

/** 分段连接处的连续性。 / Continuity at segment boundaries. */
enum class PiecewiseContinuity {
    Continuous,
    Discontinuous,
    Unknown
}

/** 插件可探测的原生数学接口。 / Native mathematical interfaces detectable by plugins. */
enum class FunctionNativeCapability {
    PiecewiseLinearObjective,
    PiecewiseLinearConstraint,
    SOS1,
    SOS2,
    Indicator,
    GeneralAbs,
    GeneralMinMax,
    SemiContinuous,
    BinaryLogic
}

/** 函数结果的使用位置。 / Usage locations of a function result. */
enum class FunctionUsageLocation {
    ObjectiveOnly,
    ConstraintOnly,
    ObjectiveAndConstraint,
    Nested,
    External,
    Unused
}

/**
 * 原生展开所需的使用语境。 / Usage context required for native lowering.
 *
 * @property inObjective 出现在有效目标项中 / Appears in an effective objective term
 * @property inConstraint 出现在外部约束中 / Appears in an external constraint
 * @property nestedAsInput 被另一个结构引用 / Referenced by another structure
 * @property externallyReferenced 存在显式引用或初值 / Has an explicit reference or initial value
 * @property inputShape 输入形状 / Input shape
 * @property continuity 连续性 / Continuity
 */
data class FunctionUsageSummary(
    val inObjective: Boolean = false,
    val inConstraint: Boolean = false,
    val nestedAsInput: Boolean = false,
    val externallyReferenced: Boolean = false,
    val inputShape: FunctionInputShape = FunctionInputShape.Unknown,
    val continuity: PiecewiseContinuity = PiecewiseContinuity.Unknown
) {
    val location: FunctionUsageLocation
        get() = when {
            nestedAsInput -> FunctionUsageLocation.Nested
            externallyReferenced -> FunctionUsageLocation.External
            inObjective && inConstraint -> FunctionUsageLocation.ObjectiveAndConstraint
            inObjective -> FunctionUsageLocation.ObjectiveOnly
            inConstraint -> FunctionUsageLocation.ConstraintOnly
            else -> FunctionUsageLocation.Unused
        }

    val requiresResultVariable: Boolean
        get() = inConstraint || nestedAsInput || externallyReferenced || (inObjective && inConstraint)

    fun supportsObjectiveOnlyNative(): Boolean =
        location == FunctionUsageLocation.ObjectiveOnly &&
            !nestedAsInput &&
            !externallyReferenced &&
            inputShape == FunctionInputShape.SingleVariable
}

/**
 * 当前 SDK 的能力快照。 / Capability snapshot of the current SDK.
 *
 * @property solver 求解器标识 / Solver identifier
 * @property version SDK 版本 / SDK version
 * @property supported 已确认的接口 / Confirmed interfaces
 */
data class FunctionSolverCapabilities(
    val solver: String,
    val version: String? = null,
    val supported: Set<FunctionNativeCapability> = emptySet()
) {
    fun supports(capability: FunctionNativeCapability): Boolean = capability in supported
}

/**
 * 原生展开的资格决策，不代表已经执行。 / Native eligibility decision, not execution status.
 *
 * @property useNative 是否满足候选条件 / Whether candidate conditions are met
 * @property capability 选中的接口 / Selected interface
 * @property reason 决策原因 / Decision reason
 */
data class FunctionLoweringDecision(
    val useNative: Boolean,
    val capability: FunctionNativeCapability? = null,
    val reason: String
)

/**
 * 检查策略与语境的基本条件；数学及版本限制由插件继续校验。 / Check basic policy and usage conditions; plugins validate semantics and versions.
 *
 * @param policy 展开策略 / Expansion policy
 * @param usage 使用语境 / Usage context
 * @param capabilities 当前 SDK 能力 / Current SDK capabilities
 * @param nativeCapability 目标原生接口 / Target native interface
 * @return 基本资格决策 / Basic eligibility decision
 */
fun decideFunctionLowering(
    policy: FunctionExpansionPolicy,
    usage: FunctionUsageSummary,
    capabilities: FunctionSolverCapabilities,
    nativeCapability: FunctionNativeCapability
): FunctionLoweringDecision {
    if (policy == FunctionExpansionPolicy.EAGER) {
        return FunctionLoweringDecision(false, reason = "eager policy")
    }
    if (usage.location == FunctionUsageLocation.Unused) {
        return FunctionLoweringDecision(false, reason = "function result is unused")
    }
    if (nativeCapability == FunctionNativeCapability.PiecewiseLinearObjective &&
        !usage.supportsObjectiveOnlyNative()
    ) {
        return FunctionLoweringDecision(false, reason = "objective-only native interface is incompatible with usage")
    }
    if (!capabilities.supports(nativeCapability)) {
        return FunctionLoweringDecision(false, reason = "solver capability is unavailable")
    }
    return FunctionLoweringDecision(true, nativeCapability, "native capability is compatible")
}

/**
 * 汇总尚未规范化的多项式引用。 / Summarize references in polynomials before normalization.
 *
 * @param resultVariable 函数结果变量 / Function result variable
 * @param objectivePolynomials 目标多项式 / Objective polynomials
 * @param constraints 原始不等式 / Original inequalities
 * @param nestedAsInput 已知嵌套标记 / Known nesting flag
 * @param externallyReferenced 已知外部引用标记 / Known external-reference flag
 * @param inputShape 输入形状 / Input shape
 * @param continuity 连续性 / Continuity
 * @return 引用摘要 / Reference summary
 */
fun <V> summarizeFunctionUsage(
    resultVariable: AbstractVariableItem<*, *>,
    objectivePolynomials: Iterable<LinearPolynomial<V>> = emptyList(),
    constraints: Iterable<LinearInequality<V>> = emptyList(),
    nestedAsInput: Boolean = false,
    externallyReferenced: Boolean = false,
    inputShape: FunctionInputShape = FunctionInputShape.Unknown,
    continuity: PiecewiseContinuity = PiecewiseContinuity.Unknown
): FunctionUsageSummary where V : RealNumber<V>, V : NumberField<V> {
    fun LinearPolynomial<V>.referencesResult(): Boolean = monomials.any {
        it.symbol === resultVariable || it.symbol == resultVariable
    }

    return FunctionUsageSummary(
        inObjective = objectivePolynomials.any { it.referencesResult() },
        inConstraint = constraints.any { it.lhs.referencesResult() || it.rhs.referencesResult() },
        nestedAsInput = nestedAsInput,
        externallyReferenced = externallyReferenced,
        inputShape = inputShape,
        continuity = continuity
    )
}
