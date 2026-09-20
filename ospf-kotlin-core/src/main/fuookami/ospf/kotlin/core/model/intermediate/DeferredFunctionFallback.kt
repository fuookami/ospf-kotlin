package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.LinearPolynomialBounds
import fuookami.ospf.kotlin.core.symbol.function.ensurePositiveBigM
import fuookami.ospf.kotlin.core.symbol.function.finiteBounds
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.gr
import fuookami.ospf.kotlin.math.symbol.inequality.ls
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

data class DeferredFunctionFallbackConstraints(
    val constraints: List<LinearInequality<Flt64>>
)

/**
 * Materialize supported piecewise-linear snapshots without a solver SDK.
 * 在不依赖求解器 SDK 的情况下 materialize 已支持的分段线性快照。
 */
object PwlFallbackMaterializer : DeferredFunctionFallbackMaterializer {
    override fun materialize(structure: DeferredFunctionStructure): Ret<DeferredFunctionFallbackConstraints> {
        return when (structure) {
            is UnivariateLinearPiecewiseStructure<*> -> structure.materializeFallbackConstraints()
            else -> Failed(
                ErrorCode.IllegalArgument,
                "不支持的延迟函数 fallback 结构：${structure::class.simpleName}。 / Unsupported deferred function fallback structure: ${structure::class.simpleName}."
            )
        }
    }
}

/** Materialize an ABS snapshot without a solver SDK. / 在不依赖求解器 SDK 的情况下物化 ABS 快照。 */
object AbsFallbackMaterializer : DeferredFunctionFallbackMaterializer {
    override fun materialize(structure: DeferredFunctionStructure): Ret<DeferredFunctionFallbackConstraints> {
        return when (structure) {
            is AbsStructure<*> -> structure.materializeAbsFallbackConstraints()
            else -> Failed(
                ErrorCode.IllegalArgument,
                "不支持的 ABS fallback 结构：${structure::class.simpleName}。 / Unsupported ABS fallback structure: ${structure::class.simpleName}."
            )
        }
    }
}

/** Materialize a MAX snapshot without a solver SDK. / 在不依赖求解器 SDK 的情况下物化 MAX 快照。 */
object MaxFallbackMaterializer : DeferredFunctionFallbackMaterializer {
    override fun materialize(structure: DeferredFunctionStructure): Ret<DeferredFunctionFallbackConstraints> {
        return when (structure) {
            is MaxStructure<*> -> structure.materializeMaxFallbackConstraints()
            else -> Failed(
                ErrorCode.IllegalArgument,
                "不支持的 MAX fallback 结构：${structure::class.simpleName}。 / Unsupported MAX fallback structure: ${structure::class.simpleName}."
            )
        }
    }
}

/** Materialize a semi-continuous snapshot without a solver SDK. / 在不依赖求解器 SDK 的情况下物化半连续快照。 */
object SemiFallbackMaterializer : DeferredFunctionFallbackMaterializer {
    override fun materialize(structure: DeferredFunctionStructure): Ret<DeferredFunctionFallbackConstraints> {
        return when (structure) {
            is SemiStructure<*> -> structure.materializeFallbackConstraints()
            else -> Failed(
                ErrorCode.IllegalArgument,
                "不支持的半连续 fallback 结构：${structure::class.simpleName}。 / Unsupported semi-continuous fallback structure: ${structure::class.simpleName}."
            )
        }
    }
}

/** Materializer for pure binary logic snapshots. / 纯二值逻辑快照物化器。 */
object BinaryLogicFallbackMaterializer : DeferredFunctionFallbackMaterializer {
    override fun materialize(structure: DeferredFunctionStructure): Ret<DeferredFunctionFallbackConstraints> {
        return when (structure) {
            is BinaryLogicStructure<*> -> structure.materializeFallbackConstraints()
            else -> Failed(ErrorCode.IllegalArgument, "不支持的二值逻辑 fallback 结构。 / Unsupported binary logic fallback structure.")
        }
    }
}

/** Materializer for the core deferred function set. / core 延迟函数集合的物化器。 */
object CoreDeferredFunctionFallbackMaterializer : DeferredFunctionFallbackMaterializer {
    override fun materialize(structure: DeferredFunctionStructure): Ret<DeferredFunctionFallbackConstraints> {
        return when (structure) {
            is UnivariateLinearPiecewiseStructure<*> -> PwlFallbackMaterializer.materialize(structure)
            is AbsStructure<*> -> AbsFallbackMaterializer.materialize(structure)
            is MaxStructure<*> -> MaxFallbackMaterializer.materialize(structure)
            is SemiStructure<*> -> SemiFallbackMaterializer.materialize(structure)
            is BinaryLogicStructure<*> -> BinaryLogicFallbackMaterializer.materialize(structure)
            is IndicatorStructure<*> -> structure.materializeFallbackConstraints()
            is MaskingStructure<*> -> structure.materializeFallbackConstraints()
            else -> Failed(
                ErrorCode.IllegalArgument,
                "不支持的延迟函数 fallback 结构：${structure::class.simpleName}。 / Unsupported deferred function fallback structure: ${structure::class.simpleName}."
            )
        }
    }
}

private data class PwlOutputBounds<V>(
    val lower: V,
    val upper: V
) where V : RealNumber<V>, V : NumberField<V>

private fun <V> pwlGenerationFailure(message: String): Ret<V> {
    return Failed(ErrorCode.IllegalArgument, message)
}

private fun <V> absGenerationFailure(message: String): Ret<V> {
    return Failed(ErrorCode.IllegalArgument, message)
}

private fun <V> maxGenerationFailure(message: String): Ret<V> {
    return Failed(ErrorCode.IllegalArgument, message)
}

private fun <V> isUsableAbsValue(value: V, converter: IntoValue<V>): Boolean
    where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!value.isFinite() || value.constants.nan?.let { value.compareTo(it) == 0 } == true) {
            return false
        }
        val converted = converter.fromValue(value)
        converted.isFinite() &&
            converted != Flt64.nan &&
            converted.compareTo(Flt64.minimum) > 0 &&
            converted.compareTo(Flt64.maximum) < 0
    } catch (_: RuntimeException) {
        false
    }
}

/** Generate the four original ABS rows for both eager and deferred paths. /
 * 为 EAGER 与 deferred 路径生成原 ABS 四行约束。 */
internal fun <V> generateAbsConstraints(
    input: LinearPolynomial<V>,
    resultVariable: AbstractVariableItem<*, *>,
    positiveVariable: AbstractVariableItem<*, *>,
    negativeVariable: AbstractVariableItem<*, *>,
    signVariable: AbstractVariableItem<*, *>,
    positiveBigM: V,
    negativeBigM: V,
    converter: IntoValue<V>,
    name: String
): Ret<List<LinearInequality<V>>> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        val keys = listOf(
            resultVariable.key,
            positiveVariable.key,
            negativeVariable.key,
            signVariable.key
        )
        if (keys.toSet().size != keys.size) {
            return absGenerationFailure(
                "ABS 结果变量与辅助变量必须具有不同键。 / ABS result and helper variables must have distinct keys."
            )
        }
        if (!isUsableAbsValue(input.constant, converter) ||
            input.monomials.any { !isUsableAbsValue(it.coefficient, converter) } ||
            !isUsableAbsValue(positiveBigM, converter) ||
            !isUsableAbsValue(negativeBigM, converter)
        ) {
            return absGenerationFailure(
                "ABS 输入或 Big-M 含非有限、不可表示值。 / ABS input or Big-M contains a non-finite or unrepresentable value."
            )
        }
        if (positiveBigM.compareTo(converter.zero) < 0 || negativeBigM.compareTo(converter.zero) < 0) {
            return absGenerationFailure(
                "ABS Big-M 必须非负。 / ABS Big-M values must be nonnegative."
            )
        }

        val zero = converter.zero
        val one = converter.one
        val constraints = ArrayList<LinearInequality<V>>(4)
        constraints += LinearInequality(
            lhs = LinearPolynomial(
                listOf(
                    LinearMonomial(one, resultVariable),
                    LinearMonomial(-one, positiveVariable),
                    LinearMonomial(-one, negativeVariable)
                ),
                constant = zero
            ),
            rhs = LinearPolynomial(emptyList(), zero),
            comparison = Comparison.EQ,
            name = "${name}_abs_result"
        )
        constraints += LinearInequality(
            lhs = LinearPolynomial(
                input.monomials.map { LinearMonomial(it.coefficient, it.symbol) } + listOf(
                    LinearMonomial(-one, positiveVariable),
                    LinearMonomial(one, negativeVariable)
                ),
                constant = input.constant
            ),
            rhs = LinearPolynomial(emptyList(), zero),
            comparison = Comparison.EQ,
            name = "${name}_abs_decompose"
        )
        constraints += LinearInequality(
            lhs = LinearPolynomial(
                listOf(
                    LinearMonomial(one, positiveVariable),
                    LinearMonomial(-positiveBigM, signVariable)
                ),
                constant = zero
            ),
            rhs = LinearPolynomial(emptyList(), zero),
            comparison = Comparison.LE,
            name = "${name}_abs_pos_ub"
        )
        constraints += LinearInequality(
            lhs = LinearPolynomial(
                listOf(
                    LinearMonomial(one, negativeVariable),
                    LinearMonomial(negativeBigM, signVariable)
                ),
                constant = zero
            ),
            rhs = LinearPolynomial(emptyList(), negativeBigM),
            comparison = Comparison.LE,
            name = "${name}_abs_neg_ub"
        )
        Ok(constraints)
    } catch (error: RuntimeException) {
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown runtime error"
        absGenerationFailure(
            "生成 ABS 约束失败：$detail / Failed to generate ABS constraints: $detail"
        )
    }
}

/**
 * Generate the exact MAX rows shared by eager registration and deferred fallback.
 * 为 EAGER 注册与 deferred fallback 生成共享的精确 MAX 约束行。
 */
internal fun <V> generateMaxConstraints(
    inputs: List<LinearPolynomial<V>>,
    resultVariable: AbstractVariableItem<*, *>,
    selectorVariables: List<AbstractVariableItem<*, *>>,
    bigMValues: List<V>,
    converter: IntoValue<V>,
    name: String
): Ret<List<LinearInequality<V>>> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (inputs.isEmpty() ||
            selectorVariables.size != inputs.size ||
            bigMValues.size != inputs.size
        ) {
            return maxGenerationFailure(
                "MAX 输入、选择变量和 Big-M 数量必须一致且非空。 / MAX inputs, selectors, and Big-M values must have matching non-empty sizes."
            )
        }
        val keys = listOf(resultVariable.key) + selectorVariables.map { it.key }
        if (keys.toSet().size != keys.size) {
            return maxGenerationFailure(
                "MAX 结果变量与选择变量必须具有不同键。 / MAX result and selector variables must have distinct keys."
            )
        }
        if (inputs.any { !isUsablePwlPolynomial(it, converter) } ||
            bigMValues.any {
                !isUsablePwlSolverValue(it, converter) || it.compareTo(converter.zero) < 0
            }
        ) {
            return maxGenerationFailure(
                "MAX 输入或 Big-M 含非有限、不可表示值。 / MAX inputs or Big-M values contain non-finite or unrepresentable values."
            )
        }

        val zero = converter.zero
        val one = converter.one
        val resultMonomial = LinearMonomial(one, resultVariable)
        val constraints = ArrayList<LinearInequality<V>>(inputs.size * 2 + 1)

        for ((index, input) in inputs.withIndex()) {
            constraints += LinearInequality(
                lhs = LinearPolynomial(
                    monomials = listOf(resultMonomial) + input.monomials.map {
                        LinearMonomial(-it.coefficient, it.symbol)
                    },
                    constant = -input.constant
                ),
                rhs = LinearPolynomial(emptyList(), zero),
                comparison = Comparison.GE,
                name = "${name}_max_lb_$index"
            )
        }
        for ((index, input) in inputs.withIndex()) {
            val bigM = bigMValues[index]
            constraints += LinearInequality(
                lhs = LinearPolynomial(
                    monomials = listOf(resultMonomial) + input.monomials.map {
                        LinearMonomial(-it.coefficient, it.symbol)
                    } + LinearMonomial(bigM, selectorVariables[index]),
                    constant = -input.constant
                ),
                rhs = LinearPolynomial(emptyList(), bigM),
                comparison = Comparison.LE,
                name = "${name}_max_ub_$index"
            )
        }
        constraints += LinearInequality(
            lhs = LinearPolynomial(
                monomials = selectorVariables.map { LinearMonomial(one, it) },
                constant = zero
            ),
            rhs = LinearPolynomial(emptyList(), one),
            comparison = Comparison.EQ,
            name = "${name}_max_select_one"
        )
        Ok(constraints)
    } catch (error: RuntimeException) {
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown runtime error"
        maxGenerationFailure(
            "生成 MAX 约束失败：$detail / Failed to generate MAX constraints: $detail"
        )
    }
}

private fun <V> AbsStructure<V>.materializeAbsFallbackConstraints(): Ret<DeferredFunctionFallbackConstraints>
    where V : RealNumber<V>, V : NumberField<V> {
    return when (val result = generateAbsConstraints(
        input = input,
        resultVariable = resultVariable,
        positiveVariable = positiveVariable,
        negativeVariable = negativeVariable,
        signVariable = signVariable,
        positiveBigM = positiveBigM,
        negativeBigM = negativeBigM,
        converter = converter,
        name = name
    )) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Ok -> convertAbsConstraintsToFlt64(result.value, converter)
    }
}

private fun <V> MaxStructure<V>.materializeMaxFallbackConstraints(): Ret<DeferredFunctionFallbackConstraints>
    where V : RealNumber<V>, V : NumberField<V> {
    when (val validation = validateFallbackInputBounds()) {
        is Ok -> {}
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
    }
    val result = if (!minimum) {
        generateMaxConstraints(
            inputs = inputs,
            resultVariable = resultVariable,
            selectorVariables = selectorVariables,
            bigMValues = bigMValues,
            converter = converter,
            name = name
        )
    } else {
        generateMinConstraints(
            inputs = inputs,
            resultVariable = resultVariable,
            selectorVariables = selectorVariables,
            bigMValues = bigMValues,
            converter = converter,
            name = name
        )
    }
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Ok -> convertMaxConstraintsToFlt64(result.value, converter)
    }
}

private fun <V> generateMinConstraints(
    inputs: List<LinearPolynomial<V>>,
    resultVariable: AbstractVariableItem<*, *>,
    selectorVariables: List<AbstractVariableItem<*, *>>,
    bigMValues: List<V>,
    converter: IntoValue<V>,
    name: String
): Ret<List<LinearInequality<V>>> where V : RealNumber<V>, V : NumberField<V> {
    if (inputs.isEmpty() || inputs.size != selectorVariables.size || inputs.size != bigMValues.size) {
        return Failed(ErrorCode.IllegalArgument, "MIN 输入、选择变量与 Big-M 数量不一致。 / MIN input, selector, and Big-M counts must match.")
    }
    val zero = converter.zero
    val one = converter.one
    val constraints = ArrayList<LinearInequality<V>>(2 * inputs.size + 1)
    for ((index, input) in inputs.withIndex()) {
        constraints += LinearInequality(
            LinearPolynomial(
                listOf(LinearMonomial(one, resultVariable)) + input.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
                -input.constant
            ),
            LinearPolynomial(emptyList(), zero),
            Comparison.LE,
            "${name}_min_upper_$index"
        )
        constraints += LinearInequality(
            LinearPolynomial(
                listOf(LinearMonomial(one, resultVariable)) + input.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                    LinearMonomial(-bigMValues[index], selectorVariables[index]),
                -input.constant
            ),
            LinearPolynomial(emptyList(), -bigMValues[index]),
            Comparison.GE,
            "${name}_min_lower_$index"
        )
    }
    constraints += LinearInequality(
        LinearPolynomial(selectorVariables.map { LinearMonomial(one, it) }, zero),
        LinearPolynomial(emptyList(), one),
        Comparison.EQ,
        "${name}_min_select"
    )
    return Ok(constraints)
}

private fun isUsablePwlSolverFlt64(value: Flt64): Boolean {
    return try {
        value.isFinite() &&
            value != Flt64.nan &&
            value.compareTo(Flt64.minimum) > 0 &&
            value.compareTo(Flt64.maximum) < 0
    } catch (_: RuntimeException) {
        false
    }
}

private fun <V> isUsablePwlSolverValue(
    value: V,
    converter: IntoValue<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!value.isFinite() || value.constants.nan?.let { value.compareTo(it) == 0 } == true) {
            return false
        }
        isUsablePwlSolverFlt64(converter.fromValue(value))
    } catch (_: RuntimeException) {
        false
    }
}

private fun <V> isUsablePwlPolynomial(
    polynomial: LinearPolynomial<V>,
    converter: IntoValue<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return isUsablePwlSolverValue(polynomial.constant, converter) &&
        polynomial.monomials.all { isUsablePwlSolverValue(it.coefficient, converter) }
}

private fun <V> isUsablePwlConstraint(
    constraint: LinearInequality<V>,
    converter: IntoValue<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return isUsablePwlPolynomial(constraint.lhs, converter) &&
        isUsablePwlPolynomial(constraint.rhs, converter)
}

private fun <V> isUsablePwlBounds(
    bounds: LinearPolynomialBounds<V>,
    converter: IntoValue<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!isUsablePwlSolverValue(bounds.lower, converter) ||
            !isUsablePwlSolverValue(bounds.upper, converter) ||
            bounds.lower.compareTo(bounds.upper) > 0
        ) {
            return false
        }
        val freeBound = Flt64.decimalPrecision.reciprocal()
        val lower = converter.fromValue(bounds.lower)
        val upper = converter.fromValue(bounds.upper)
        isUsablePwlSolverFlt64(lower) &&
            isUsablePwlSolverFlt64(upper) &&
            lower.compareTo(-freeBound) > 0 &&
            upper.compareTo(freeBound) < 0
    } catch (_: RuntimeException) {
        false
    }
}

/** Capture finite input bounds for a deferred automatic Big-M snapshot. /
 * 为延迟自动 Big-M 快照捕获有限输入范围。 */
internal fun <V> capturePwlInputBounds(
    input: LinearPolynomial<V>,
    converter: IntoValue<V>
): Ret<LinearPolynomialBounds<V>> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        val bounds = input.finiteBounds(converter) ?: return pwlGenerationFailure(
            "创建分段线性函数快照时缺少可证明的有限输入范围。 / No provable finite input range was available when creating the piecewise snapshot."
        )
        if (!isUsablePwlBounds(bounds, converter)) {
            return pwlGenerationFailure(
                "创建分段线性函数快照时输入范围为非有限或 solver 哨兵范围。 / The input range was non-finite or a solver sentinel when creating the piecewise snapshot."
            )
        }
        Ok(bounds)
    } catch (error: RuntimeException) {
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown runtime error"
        pwlGenerationFailure(
            "创建分段线性函数快照时计算输入范围失败：$detail / Failed to capture piecewise input bounds: $detail"
        )
    }
}

private fun <V> resolvePwlBigM(
    candidate: V,
    explicit: Boolean,
    converter: IntoValue<V>
): Ret<V> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!isUsablePwlSolverValue(candidate, converter) ||
            (explicit && candidate.compareTo(converter.zero) <= 0)
        ) {
            return pwlGenerationFailure(
                "分段线性函数的 Big-M 必须为有限、可表示且为正值。 / Piecewise Big-M must be finite, representable, and positive."
            )
        }
        val normalized = ensurePositiveBigM(candidate, converter)
        if (!isUsablePwlSolverValue(normalized, converter) || normalized.compareTo(converter.zero) <= 0) {
            return pwlGenerationFailure(
                "分段线性函数的 Big-M 规范化后不可用。 / The normalized piecewise Big-M is not usable."
            )
        }
        Ok(normalized)
    } catch (error: RuntimeException) {
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown runtime error"
        pwlGenerationFailure(
            "校验分段线性函数 Big-M 失败：$detail / Failed to validate piecewise Big-M: $detail"
        )
    }
}

private fun <V> calculatePwlOutputBounds(
    breakpoints: List<V>,
    slopes: List<V>,
    intercepts: List<V>,
    converter: IntoValue<V>
): Ret<PwlOutputBounds<V>> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (breakpoints.size < 2 ||
            slopes.size != breakpoints.size - 1 ||
            intercepts.size != breakpoints.size - 1
        ) {
            return pwlGenerationFailure(
                "分段线性函数的断点、斜率和截距数量不匹配。 / Piecewise breakpoints, slopes, and intercept counts are inconsistent."
            )
        }
        if (breakpoints.any { !isUsablePwlSolverValue(it, converter) } ||
            slopes.any { !isUsablePwlSolverValue(it, converter) } ||
            intercepts.any { !isUsablePwlSolverValue(it, converter) }
        ) {
            return pwlGenerationFailure(
                "分段线性函数的断点、斜率和截距必须为可表示的有限值。 / Piecewise breakpoints, slopes, and intercepts must be finite representable values."
            )
        }
        for (index in 0 until breakpoints.size - 1) {
            if (breakpoints[index].compareTo(breakpoints[index + 1]) >= 0) {
                return pwlGenerationFailure(
                    "分段线性函数的断点必须严格递增。 / Piecewise breakpoints must be strictly increasing."
                )
            }
        }

        val outputValues = (0 until breakpoints.size - 1).flatMap { index ->
            listOf(
                slopes[index] * breakpoints[index] + intercepts[index],
                slopes[index] * breakpoints[index + 1] + intercepts[index]
            )
        }
        if (outputValues.isEmpty() || outputValues.any { !isUsablePwlSolverValue(it, converter) }) {
            return pwlGenerationFailure(
                "分段线性函数的端点计算产生非有限或无界输出。 / Evaluating piecewise endpoints produced a non-finite or unbounded output."
            )
        }

        var lower = outputValues.first()
        var upper = outputValues.first()
        for (value in outputValues.drop(1)) {
            if (value.compareTo(lower) < 0) {
                lower = value
            }
            if (value.compareTo(upper) > 0) {
                upper = value
            }
        }
        Ok(PwlOutputBounds(lower = lower, upper = upper))
    } catch (error: RuntimeException) {
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown runtime error"
        pwlGenerationFailure(
            "计算分段线性函数输出范围失败：$detail / Failed to calculate piecewise output bounds: $detail"
        )
    }
}

/**
 * Generate all fallback rows for a univariate piecewise-linear snapshot.
 * 为单变量分段线性快照生成完整 fallback 约束行。
 *
 * This function is pure with respect to the model: it only validates the snapshot and
 * constructs rows, so eager registration and deferred materialization share identical math.
 * 此函数不修改模型，只校验快照并构造约束，因此 EAGER 注册与延迟 materializer 共享同一数学实现。
 */
internal fun <V> generateUnivariateLinearPiecewiseConstraints(
    input: LinearPolynomial<V>,
    breakpoints: List<V>,
    slopes: List<V>,
    intercepts: List<V>,
    explicitM: V?,
    converter: IntoValue<V>,
    resultVariable: AbstractVariableItem<*, *>,
    selectorVariables: List<AbstractVariableItem<*, *>>,
    name: String,
    inputBounds: Ret<LinearPolynomialBounds<V>>? = null
): Ret<List<LinearInequality<V>>> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        val numSegments = breakpoints.size - 1
        if (breakpoints.size < 2 ||
            slopes.size != numSegments ||
            intercepts.size != numSegments ||
            selectorVariables.size != numSegments
        ) {
            return pwlGenerationFailure(
                "分段线性函数快照的结构数量不匹配。 / Piecewise snapshot dimensions are inconsistent."
            )
        }
        if (!isUsablePwlPolynomial(input, converter)) {
            return pwlGenerationFailure(
                "分段线性函数的输入多项式包含非有限或不可表示值。 / The piecewise input polynomial contains a non-finite or unrepresentable value."
            )
        }
        val outputBounds = when (val result = calculatePwlOutputBounds(
            breakpoints = breakpoints,
            slopes = slopes,
            intercepts = intercepts,
            converter = converter
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val explicitBigM = if (explicitM == null) {
            null
        } else {
            when (val result = resolvePwlBigM(explicitM, explicit = true, converter = converter)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        val xBounds: LinearPolynomialBounds<V>?
        val fallbackM: V?
        if (explicitBigM == null) {
            xBounds = if (inputBounds == null) {
                try {
                    input.finiteBounds(converter)
                } catch (error: RuntimeException) {
                    val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown runtime error"
                    return pwlGenerationFailure(
                        "计算分段线性函数输入范围失败：$detail / Failed to calculate piecewise input bounds: $detail"
                    )
                }
            } else {
                when (val result = inputBounds) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            if (xBounds == null) {
                return pwlGenerationFailure(
                    "分段线性函数缺少可证明的有限输入范围，不能自动推导 Big-M。 / The piecewise function has no provable finite input range for automatic Big-M inference."
                )
            }
            if (!isUsablePwlBounds(xBounds, converter)) {
                return pwlGenerationFailure(
                    "分段线性函数的输入范围为非有限或 solver 哨兵范围。 / The piecewise input range is non-finite or a solver sentinel range."
                )
            }
            fallbackM = when (val result = resolvePwlBigM(xBounds.absMax, explicit = false, converter = converter)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        } else {
            xBounds = null
            fallbackM = null
        }

        val zero = converter.zero
        val one = converter.one
        val constraints = ArrayList<LinearInequality<V>>(1 + numSegments * 4)
        val sumMonomials = selectorVariables.map { LinearMonomial(one, it) }
        constraints += LinearInequality(
            lhs = LinearPolynomial(sumMonomials, zero),
            rhs = LinearPolynomial(emptyList(), one),
            comparison = Comparison.EQ,
            name = "${name}_select_one"
        )

        for (index in 0 until numSegments) {
            val selector = selectorVariables[index]
            val breakpointLower = breakpoints[index]
            val breakpointUpper = breakpoints[index + 1]
            val slope = slopes[index]
            val intercept = intercepts[index]
            val bigM = if (explicitBigM != null) {
                explicitBigM
            } else if (xBounds != null) {
                val lineAtLower = slope * xBounds.lower + intercept
                val lineAtUpper = slope * xBounds.upper + intercept
                val lineLower = if (lineAtLower ls lineAtUpper) lineAtLower else lineAtUpper
                val lineUpper = if (lineAtLower gr lineAtUpper) lineAtLower else lineAtUpper
                val xLowerRelax = if (breakpointLower gr xBounds.lower) breakpointLower - xBounds.lower else zero
                val xUpperRelax = if (xBounds.upper gr breakpointUpper) xBounds.upper - breakpointUpper else zero
                val eqUpperRelax = (outputBounds.upper - lineLower).abs()
                val eqLowerRelax = (outputBounds.lower - lineUpper).abs()
                val candidate = listOf(
                    xLowerRelax,
                    xUpperRelax,
                    eqUpperRelax,
                    eqLowerRelax,
                    fallbackM ?: return pwlGenerationFailure(
                        "分段线性函数无法解析自动 Big-M。 / The piecewise function cannot resolve an automatic Big-M."
                    )
                ).reduce { current, value -> if (value gr current) value else current }
                when (val result = resolvePwlBigM(candidate, explicit = false, converter = converter)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            } else {
                fallbackM ?: return pwlGenerationFailure(
                    "分段线性函数无法解析自动 Big-M。 / The piecewise function cannot resolve an automatic Big-M."
                )
            }

            val inputMonomials = input.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
            constraints += LinearInequality(
                lhs = LinearPolynomial(
                    monomials = inputMonomials + LinearMonomial(-bigM, selector),
                    constant = input.constant + bigM
                ),
                rhs = LinearPolynomial(emptyList(), breakpointLower),
                comparison = Comparison.GE,
                name = "${name}_seg_${index}_lb"
            )
            constraints += LinearInequality(
                lhs = LinearPolynomial(
                    monomials = inputMonomials + LinearMonomial(bigM, selector),
                    constant = input.constant
                ),
                rhs = LinearPolynomial(emptyList(), breakpointUpper + bigM),
                comparison = Comparison.LE,
                name = "${name}_seg_${index}_ub"
            )

            val negatedInputMonomials = input.monomials.map {
                LinearMonomial(-it.coefficient * slope, it.symbol)
            }
            val equationConstant = -slope * input.constant - intercept
            constraints += LinearInequality(
                lhs = LinearPolynomial(
                    monomials = listOf(LinearMonomial(one, resultVariable)) +
                        negatedInputMonomials + LinearMonomial(bigM, selector),
                    constant = equationConstant
                ),
                rhs = LinearPolynomial(emptyList(), bigM),
                comparison = Comparison.LE,
                name = "${name}_seg_${index}_eq_ub"
            )
            constraints += LinearInequality(
                lhs = LinearPolynomial(
                    monomials = listOf(LinearMonomial(one, resultVariable)) +
                        negatedInputMonomials + LinearMonomial(-bigM, selector),
                    constant = equationConstant
                ),
                rhs = LinearPolynomial(emptyList(), -bigM),
                comparison = Comparison.GE,
                name = "${name}_seg_${index}_eq_lb"
            )
        }

        if (constraints.any { !isUsablePwlConstraint(it, converter) }) {
            return pwlGenerationFailure(
                "分段线性函数生成了非有限或不可表示的约束。 / The piecewise function generated a non-finite or unrepresentable constraint."
            )
        }
        Ok(constraints)
    } catch (error: RuntimeException) {
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown runtime error"
        pwlGenerationFailure(
            "生成分段线性函数约束失败：$detail / Failed to generate piecewise constraints: $detail"
        )
    }
}

internal fun <V> convertPwlConstraintsToFlt64(
    constraints: List<LinearInequality<V>>,
    converter: IntoValue<V>
): Ret<DeferredFunctionFallbackConstraints> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        fun convertPolynomial(polynomial: LinearPolynomial<V>): LinearPolynomial<Flt64> {
            return LinearPolynomial(
                monomials = polynomial.monomials.map {
                    LinearMonomial(converter.fromValue(it.coefficient), it.symbol)
                },
                constant = converter.fromValue(polynomial.constant)
            )
        }

        val converted = constraints.map { constraint ->
            LinearInequality(
                lhs = convertPolynomial(constraint.lhs),
                rhs = convertPolynomial(constraint.rhs),
                comparison = constraint.comparison,
                name = constraint.name,
                displayName = constraint.displayName
            )
        }
        if (converted.any { constraint ->
                !isUsablePwlPolynomial(constraint.lhs, IntoValue.Identity) ||
                    !isUsablePwlPolynomial(constraint.rhs, IntoValue.Identity)
            }
        ) {
            return pwlGenerationFailure(
                "约束无法转换为有限 Flt64。 / Constraints cannot be converted to finite Flt64 values."
            )
        }
        Ok(DeferredFunctionFallbackConstraints(converted))
    } catch (error: RuntimeException) {
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown runtime error"
        pwlGenerationFailure(
            "转换约束失败：$detail / Failed to convert constraints: $detail"
        )
    }
}

internal fun <V> convertAbsConstraintsToFlt64(
    constraints: List<LinearInequality<V>>,
    converter: IntoValue<V>
): Ret<DeferredFunctionFallbackConstraints> where V : RealNumber<V>, V : NumberField<V> {
    return convertPwlConstraintsToFlt64(constraints, converter)
}

internal fun <V> convertMaxConstraintsToFlt64(
    constraints: List<LinearInequality<V>>,
    converter: IntoValue<V>
): Ret<DeferredFunctionFallbackConstraints> where V : RealNumber<V>, V : NumberField<V> {
    return convertPwlConstraintsToFlt64(constraints, converter)
}

fun interface DeferredFunctionFallbackMaterializer {
    fun materialize(structure: DeferredFunctionStructure): Ret<DeferredFunctionFallbackConstraints>
}

interface DeferredFunctionFallbackTarget {
    val constraintCount: Int

    fun append(constraints: List<LinearInequality<Flt64>>): Try

    fun rollback(constraintCount: Int): Try
}

fun materializeDeferredFunctionFallbacks(
    structures: List<DeferredFunctionStructure>,
    target: DeferredFunctionFallbackTarget,
    materializer: DeferredFunctionFallbackMaterializer
): Ret<List<DeferredFunctionConstraintRegion>> {
    val checkpoint = target.constraintCount
    val regions = ArrayList<DeferredFunctionConstraintRegion>(structures.size)
    for (structure in structures) {
        val constraints = when (val result = materializer.materialize(structure)) {
            is Ok -> result.value.constraints
            is Failed -> {
                val rollback = target.rollback(checkpoint)
                return if (rollback is Ok) Failed(result.error) else Fatal(rollbackErrors(listOf(result.error), rollback))
            }
            is Fatal -> {
                val rollback = target.rollback(checkpoint)
                return if (rollback is Ok) Fatal(result.errors) else Fatal(rollbackErrors(result.errors, rollback))
            }
        }
        val first = target.constraintCount
        when (val appended = target.append(constraints)) {
            is Ok -> regions += DeferredFunctionConstraintRegion(structure, first, constraints.size)
            is Failed -> {
                val rollback = target.rollback(checkpoint)
                return if (rollback is Ok) Failed(appended.error) else Fatal(rollbackErrors(listOf(appended.error), rollback))
            }
            is Fatal -> {
                val rollback = target.rollback(checkpoint)
                return if (rollback is Ok) Fatal(appended.errors) else Fatal(rollbackErrors(appended.errors, rollback))
            }
        }
    }
    return Ok(regions)
}

private fun rollbackErrors(
    primaryErrors: List<fuookami.ospf.kotlin.utils.error.Error<ErrorCode>>,
    rollback: Try
): List<fuookami.ospf.kotlin.utils.error.Error<ErrorCode>> {
    val errors = ArrayList(primaryErrors)
    when (rollback) {
        is Failed -> errors += rollback.error
        is Fatal -> errors += rollback.errors
        is Ok -> {}
    }
    return errors
}
