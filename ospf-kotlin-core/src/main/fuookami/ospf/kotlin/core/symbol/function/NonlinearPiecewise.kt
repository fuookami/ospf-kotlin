@file:Suppress("unused")

/** 单变量非线性函数的分段线性近似工厂 / Piecewise-linear approximation factories for univariate nonlinear functions. */
package fuookami.ospf.kotlin.core.symbol.function

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionStructure
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 单变量非线性函数的分段线性近似。
 * 求解器模型和 [evaluate] 都使用断点间的线性插值；[originalValue] 只用于计算原函数值，
 * 本类型不提供可证明的绝对误差上界。
 *
 * Piecewise-linear approximation of a univariate nonlinear function.
 * Both the solver model and [evaluate] use linear interpolation between breakpoints.
 * [originalValue] evaluates the source function separately; this type does not claim a certified absolute-error bound.
 *
 * @property x 输入线性表达式 / Input linear expression
 * @property breakpoints 有限且严格递增的输入断点 / Finite, strictly increasing input breakpoints
 * @property certifiedAbsoluteError 当前工厂未计算或证明的误差界 / Error bound not calculated or certified by this factory
 * @property name 函数名称 / Function name
 * @property displayName 可选显示名称 / Optional display name
 */
abstract class NonlinearPiecewiseApproximationFunction<V> internal constructor(
    xInput: LinearPolynomial<V>,
    breakpointsInput: List<Flt64>,
    private val converter: IntoValue<V>,
    private val sourceFunction: (Double) -> Double,
    private val piecewise: UnivariateLinearPiecewiseFunction<V>,
    override var name: String,
    override var displayName: String?
) : MathFunctionSymbol<V>, HasResultPolynomial<V>
        where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {

    val x: LinearPolynomial<V> = LinearPolynomial(xInput.monomials.toList(), xInput.constant)
    val breakpoints: List<Flt64> = breakpointsInput.toList()

    /** 此工厂未计算或证明绝对误差界。 / This factory does not calculate or prove an absolute-error bound. */
    val certifiedAbsoluteError: Flt64? get() = null

    override val resultPolynomial: LinearPolynomial<V>
        get() = piecewise.resultPolynomial

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = piecewise.helperVariables

    override fun deferredStructure(): DeferredFunctionStructure {
        return piecewise.deferredStructure()
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        return piecewise.evaluate(values)
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return piecewise.registerAuxiliaryTokens(tokens)
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        return piecewise.registerConstraints(model)
    }

    /**
     * 在近似定义域内计算原函数值，供调用方比较插值结果；不会参与模型求解。
     *
     * Evaluate the source function within the approximation domain for comparison; this is not used by the optimization model.
     *
     * @param input 输入值 / Input value
     * @return 原函数值，或输入越界/运算非有限时的结构化错误 / Source value, or a structured error for out-of-domain or non-finite evaluation
     */
    fun originalValueFlt64(input: Flt64): Ret<Flt64> {
        val lower = breakpoints.first()
        val upper = breakpoints.last()
        if (!input.isFinite() || input < lower || input > upper) {
            return Failed(
                ErrorCode.IllegalArgument,
                "原函数输入必须位于有限近似定义域内 / Source-function input must lie inside the finite approximation domain"
            )
        }
        return try {
            val value = sourceFunction(java.lang.Double.valueOf(input.toString()))
            if (!value.isFinite()) {
                Failed(
                    ErrorCode.IllegalArgument,
                    "原函数求值产生非有限值 / Source-function evaluation produced a non-finite value"
                )
            } else {
                ok(Flt64(value))
            }
        } catch (error: RuntimeException) {
            Failed(
                ErrorCode.IllegalArgument,
                "原函数求值失败：${error.message ?: error::class.simpleName} / Source-function evaluation failed: ${error.message ?: error::class.simpleName}"
            )
        }
    }

    /**
     * 将 V 输入转换到 Flt64 后计算原函数，并将结果转换回 V。
     *
     * Evaluate the source function after converting a V input to Flt64, then convert the result back to V.
     *
     * @param input 输入值 / Input value
     * @return 原函数值或结构化错误 / Source value or a structured error
     */
    fun originalValue(input: V): Ret<V> {
        return try {
            when (val value = originalValueFlt64(converter.fromValue(input))) {
                is Ok -> ok(converter.intoValue(value.value))
                is Failed -> Failed(value.error)
                is Fatal -> Fatal(value.errors)
            }
        } catch (error: RuntimeException) {
            Failed(
                ErrorCode.IllegalArgument,
                "原函数值类型转换失败：${error.message ?: error::class.simpleName} / Source-value conversion failed: ${error.message ?: error::class.simpleName}"
            )
        }
    }
}

private const val MAX_NONLINEAR_PWL_SEGMENTS: Int = 100_000

private fun <V> createNonlinearPiecewise(
    x: LinearPolynomial<V>,
    breakpoints: List<Flt64>,
    converter: IntoValue<V>,
    name: String,
    displayName: String?,
    domainError: (Double, Double) -> String?,
    sourceFunction: (Double) -> Double
): Ret<UnivariateLinearPiecewiseFunction<V>> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (name.isBlank()) {
            return Failed(ErrorCode.IllegalArgument, "函数名称不能为空 / Function name must not be blank")
        }
        if (breakpoints.size < 2 || breakpoints.size - 1 > MAX_NONLINEAR_PWL_SEGMENTS) {
            return Failed(
                ErrorCode.IllegalArgument,
                "分段数必须介于 1 与 $MAX_NONLINEAR_PWL_SEGMENTS 之间 / Segment count must be between 1 and $MAX_NONLINEAR_PWL_SEGMENTS"
            )
        }
        if (breakpoints.any { !it.isFinite() } || breakpoints.zipWithNext().any { (left, right) -> left >= right }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "输入断点必须为有限值且严格递增 / Input breakpoints must be finite and strictly increasing"
            )
        }
        val lower = java.lang.Double.valueOf(breakpoints.first().toString())
        val upper = java.lang.Double.valueOf(breakpoints.last().toString())
        domainError(lower, upper)?.let { message ->
            return Failed(ErrorCode.IllegalArgument, message)
        }

        val points = ArrayList<Point<Dim2, V>>(breakpoints.size)
        for (point in breakpoints) {
            val y = sourceFunction(java.lang.Double.valueOf(point.toString()))
            if (!y.isFinite()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "原函数在断点处产生非有限值 / Source function produced a non-finite value at a breakpoint"
                )
            }
            points += Point<Dim2, V>(converter.intoValue(point), converter.intoValue(Flt64(y)))
        }

        val capturedX = LinearPolynomial(x.monomials.toList(), x.constant)
        when (val result = UnivariateLinearPiecewiseFunction.fromPointsResult(
            x = capturedX,
            points = points,
            converter = converter,
            name = name,
            displayName = displayName
        )) {
            is Ok -> result
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    } catch (error: RuntimeException) {
        Failed(
            ErrorCode.IllegalArgument,
            "创建非线性分段近似失败：${error.message ?: error::class.simpleName} / Failed to create nonlinear piecewise approximation: ${error.message ?: error::class.simpleName}"
        )
    }
}

private fun uniformBreakpoints(lower: Flt64, upper: Flt64, segments: Int): Ret<List<Flt64>> {
    if (!lower.isFinite() || !upper.isFinite() || lower >= upper) {
        return Failed(
            ErrorCode.IllegalArgument,
            "有限定义域必须满足 lower < upper / A finite domain must satisfy lower < upper"
        )
    }
    if (segments <= 0 || segments > MAX_NONLINEAR_PWL_SEGMENTS) {
        return Failed(
            ErrorCode.IllegalArgument,
            "分段数必须介于 1 与 $MAX_NONLINEAR_PWL_SEGMENTS 之间 / Segment count must be between 1 and $MAX_NONLINEAR_PWL_SEGMENTS"
        )
    }
    return try {
        val lo = java.lang.Double.valueOf(lower.toString())
        val hi = java.lang.Double.valueOf(upper.toString())
        val step = (hi - lo) / segments
        if (!step.isFinite() || step <= 0.0) {
            Failed(ErrorCode.IllegalArgument, "定义域无法按指定段数均分 / Domain cannot be evenly divided into the requested number of segments")
        } else {
            val points = (0..segments).map { index ->
                if (index == segments) upper else Flt64(lo + step * index)
            }
            if (points.zipWithNext().any { (left, right) -> left >= right }) {
                Failed(
                    ErrorCode.IllegalArgument,
                    "均分后存在重复断点；请减少分段数 / Uniform sampling produced duplicate breakpoints; reduce the segment count"
                )
            } else {
                ok(points)
            }
        }
    } catch (error: RuntimeException) {
        Failed(
            ErrorCode.IllegalArgument,
            "生成等分断点失败：${error.message ?: error::class.simpleName} / Failed to generate uniform breakpoints: ${error.message ?: error::class.simpleName}"
        )
    }
}

private fun <T> withUniformBreakpoints(
    lower: Flt64,
    upper: Flt64,
    segments: Int,
    create: (List<Flt64>) -> Ret<T>
): Ret<T> {
    return when (val result = uniformBreakpoints(
        lower = lower,
        upper = upper,
        segments = segments
    )) {
        is Ok -> create(result.value)
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/** 指数函数分段线性近似工厂，模型结果采用折线插值。 / Exponential piecewise-linear approximation factory using interpolation in the model. */
class ExpFunction<V> private constructor(
    x: LinearPolynomial<V>,
    breakpoints: List<Flt64>,
    converter: IntoValue<V>,
    piecewise: UnivariateLinearPiecewiseFunction<V>,
    name: String,
    displayName: String?
) : NonlinearPiecewiseApproximationFunction<V>(
    xInput = x,
    breakpointsInput = breakpoints,
    converter = converter,
    sourceFunction = { exp(it) },
    piecewise = piecewise,
    name = name,
    displayName = displayName
)
        where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {

    companion object {
        /** 使用等距断点创建指数近似。 / Create an exponential approximation with uniformly spaced breakpoints. */
        fun <V> create(
            x: LinearPolynomial<V>,
            lower: Flt64,
            upper: Flt64,
            segments: Int = 16,
            converter: IntoValue<V>,
            name: String = "exp",
            displayName: String? = null
        ): Ret<ExpFunction<V>> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
            return withUniformBreakpoints(
                lower = lower,
                upper = upper,
                segments = segments,
                create = { points ->
                    create(
                        x = x,
                        breakpoints = points,
                        converter = converter,
                        name = name,
                        displayName = displayName
                    )
                }
            )
        }

        /** 使用显式断点创建指数近似；首末断点定义有限定义域。 / Create an exponential approximation from explicit breakpoints; the first and last points define its finite domain. */
        fun <V> create(
            x: LinearPolynomial<V>,
            breakpoints: List<Flt64>,
            converter: IntoValue<V>,
            name: String = "exp",
            displayName: String? = null
        ): Ret<ExpFunction<V>> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
            return when (val result = createNonlinearPiecewise(
                x = x,
                breakpoints = breakpoints,
                converter = converter,
                name = name,
                displayName = displayName,
                domainError = { _, _ -> null },
                sourceFunction = { exp(it) }
            )) {
                is Ok -> ok(
                    ExpFunction(
                        x = x,
                        breakpoints = breakpoints.toList(),
                        converter = converter,
                        piecewise = result.value,
                        name = name,
                        displayName = displayName
                    )
                )
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }
    }
}

/** 自然对数分段线性近似工厂，定义域必须严格大于零。 / Natural-log piecewise-linear approximation factory; domain must be strictly positive. */
class LogFunction<V> private constructor(
    x: LinearPolynomial<V>,
    breakpoints: List<Flt64>,
    converter: IntoValue<V>,
    piecewise: UnivariateLinearPiecewiseFunction<V>,
    name: String,
    displayName: String?
) : NonlinearPiecewiseApproximationFunction<V>(
    xInput = x,
    breakpointsInput = breakpoints,
    converter = converter,
    sourceFunction = { ln(it) },
    piecewise = piecewise,
    name = name,
    displayName = displayName
)
        where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {

    companion object {
        /** 使用等距断点创建自然对数近似。 / Create a natural-log approximation with uniformly spaced breakpoints. */
        fun <V> create(
            x: LinearPolynomial<V>,
            lower: Flt64,
            upper: Flt64,
            segments: Int = 16,
            converter: IntoValue<V>,
            name: String = "log",
            displayName: String? = null
        ): Ret<LogFunction<V>> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
            return withUniformBreakpoints(
                lower = lower,
                upper = upper,
                segments = segments,
                create = { points ->
                    create(
                        x = x,
                        breakpoints = points,
                        converter = converter,
                        name = name,
                        displayName = displayName
                    )
                }
            )
        }

        /** 使用显式断点创建自然对数近似；首末断点定义有限定义域。 / Create a natural-log approximation from explicit breakpoints; the first and last points define its finite domain. */
        fun <V> create(
            x: LinearPolynomial<V>,
            breakpoints: List<Flt64>,
            converter: IntoValue<V>,
            name: String = "log",
            displayName: String? = null
        ): Ret<LogFunction<V>> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
            return when (val result = createNonlinearPiecewise(
                x = x,
                breakpoints = breakpoints,
                converter = converter,
                name = name,
                displayName = displayName,
                domainError = { lower, _ ->
                    if (lower <= 0.0) "Log 定义域必须严格大于零 / Log domain must be strictly positive" else null
                },
                sourceFunction = { ln(it) }
            )) {
                is Ok -> ok(
                    LogFunction(
                        x = x,
                        breakpoints = breakpoints.toList(),
                        converter = converter,
                        piecewise = result.value,
                        name = name,
                        displayName = displayName
                    )
                )
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }
    }
}

/** 倒数函数分段线性近似工厂，定义域不得跨越零。 / Reciprocal piecewise-linear approximation factory; domain must not cross zero. */
class ReciprocalFunction<V> private constructor(
    x: LinearPolynomial<V>,
    breakpoints: List<Flt64>,
    converter: IntoValue<V>,
    piecewise: UnivariateLinearPiecewiseFunction<V>,
    name: String,
    displayName: String?
) : NonlinearPiecewiseApproximationFunction<V>(
    xInput = x,
    breakpointsInput = breakpoints,
    converter = converter,
    sourceFunction = { 1.0 / it },
    piecewise = piecewise,
    name = name,
    displayName = displayName
)
        where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {

    companion object {
        /** 使用等距断点创建倒数近似。 / Create a reciprocal approximation with uniformly spaced breakpoints. */
        fun <V> create(
            x: LinearPolynomial<V>,
            lower: Flt64,
            upper: Flt64,
            segments: Int = 16,
            converter: IntoValue<V>,
            name: String = "reciprocal",
            displayName: String? = null
        ): Ret<ReciprocalFunction<V>> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
            return withUniformBreakpoints(
                lower = lower,
                upper = upper,
                segments = segments,
                create = { points ->
                    create(
                        x = x,
                        breakpoints = points,
                        converter = converter,
                        name = name,
                        displayName = displayName
                    )
                }
            )
        }

        /** 使用显式断点创建倒数近似；首末断点定义有限定义域。 / Create a reciprocal approximation from explicit breakpoints; the first and last points define its finite domain. */
        fun <V> create(
            x: LinearPolynomial<V>,
            breakpoints: List<Flt64>,
            converter: IntoValue<V>,
            name: String = "reciprocal",
            displayName: String? = null
        ): Ret<ReciprocalFunction<V>> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
            return when (val result = createNonlinearPiecewise(
                x = x,
                breakpoints = breakpoints,
                converter = converter,
                name = name,
                displayName = displayName,
                domainError = { lower, upper ->
                    if (lower <= 0.0 && upper >= 0.0) "Reciprocal 定义域不得包含零 / Reciprocal domain must not contain zero" else null
                },
                sourceFunction = { 1.0 / it }
            )) {
                is Ok -> ok(
                    ReciprocalFunction(
                        x = x,
                        breakpoints = breakpoints.toList(),
                        converter = converter,
                        piecewise = result.value,
                        name = name,
                        displayName = displayName
                    )
                )
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }
    }
}

/**
 * 幂函数分段线性近似工厂；负底数只允许整数指数，负指数不得包含零。
 *
 * Power-function piecewise-linear approximation factory; negative bases require integer exponents and negative exponents exclude zero.
 *
 * @property exponent 幂指数 / Power exponent
 */
class PowerFunction<V> private constructor(
    x: LinearPolynomial<V>,
    breakpoints: List<Flt64>,
    converter: IntoValue<V>,
    val exponent: Flt64,
    piecewise: UnivariateLinearPiecewiseFunction<V>,
    name: String,
    displayName: String?
) : NonlinearPiecewiseApproximationFunction<V>(
    xInput = x,
    breakpointsInput = breakpoints,
    converter = converter,
    sourceFunction = { it.pow(java.lang.Double.valueOf(exponent.toString())) },
    piecewise = piecewise,
    name = name,
    displayName = displayName
)
        where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {

    companion object {
        /** 使用等距断点创建幂函数近似。 / Create a power approximation with uniformly spaced breakpoints. */
        fun <V> create(
            x: LinearPolynomial<V>,
            exponent: Flt64,
            lower: Flt64,
            upper: Flt64,
            segments: Int = 16,
            converter: IntoValue<V>,
            name: String = "power",
            displayName: String? = null
        ): Ret<PowerFunction<V>> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
            return withUniformBreakpoints(
                lower = lower,
                upper = upper,
                segments = segments,
                create = { points ->
                    create(
                        x = x,
                        exponent = exponent,
                        breakpoints = points,
                        converter = converter,
                        name = name,
                        displayName = displayName
                    )
                }
            )
        }

        /** 使用显式断点创建幂函数近似；首末断点定义有限定义域。 / Create a power approximation from explicit breakpoints; the first and last points define its finite domain. */
        fun <V> create(
            x: LinearPolynomial<V>,
            exponent: Flt64,
            breakpoints: List<Flt64>,
            converter: IntoValue<V>,
            name: String = "power",
            displayName: String? = null
        ): Ret<PowerFunction<V>> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
            if (!exponent.isFinite()) {
                return Failed(ErrorCode.IllegalArgument, "Power 指数必须为有限值 / Power exponent must be finite")
            }
            val integerExponent = java.lang.Double.valueOf(exponent.toString()) % 1.0 == 0.0
            return when (val result = createNonlinearPiecewise(
                x = x,
                breakpoints = breakpoints,
                converter = converter,
                name = name,
                displayName = displayName,
                domainError = { lower, upper ->
                    when {
                        lower < 0.0 && !integerExponent -> "Power 的负底数只允许整数指数 / Negative power bases require an integer exponent"
                        exponent < Flt64.zero && lower <= 0.0 && upper >= 0.0 -> "负指数的 Power 定义域不得包含零 / A negative power exponent requires a domain that excludes zero"
                        else -> null
                    }
                },
                sourceFunction = { it.pow(java.lang.Double.valueOf(exponent.toString())) }
            )) {
                is Ok -> ok(
                    PowerFunction(
                        x = x,
                        breakpoints = breakpoints.toList(),
                        converter = converter,
                        exponent = exponent,
                        piecewise = result.value,
                        name = name,
                        displayName = displayName
                    )
                )
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }
    }
}

/**
 * 平滑 logistic 函数的分段线性近似；旧 [SigmoidFunction] 仍表示 0/1 阶跃指示。
 *
 * Smooth logistic piecewise-linear approximation; legacy [SigmoidFunction] remains a 0/1 step indicator.
 *
 * @property steepness 曲线陡峭程度 / Curve steepness
 * @property midpoint 曲线中点 / Curve midpoint
 */
class LogisticApproximationFunction<V> private constructor(
    x: LinearPolynomial<V>,
    breakpoints: List<Flt64>,
    converter: IntoValue<V>,
    val steepness: Flt64,
    val midpoint: Flt64,
    piecewise: UnivariateLinearPiecewiseFunction<V>,
    name: String,
    displayName: String?
) : NonlinearPiecewiseApproximationFunction<V>(
    xInput = x,
    breakpointsInput = breakpoints,
    converter = converter,
    sourceFunction = { input ->
        val z = java.lang.Double.valueOf(steepness.toString()) * (input - java.lang.Double.valueOf(midpoint.toString()))
        if (z >= 0.0) 1.0 / (1.0 + exp(-z)) else exp(z) / (1.0 + exp(z))
    },
    piecewise = piecewise,
    name = name,
    displayName = displayName
) where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {

    companion object {
        /** 使用等距断点创建平滑 logistic 近似。 / Create a smooth logistic approximation with uniformly spaced breakpoints. */
        fun <V> create(
            x: LinearPolynomial<V>,
            lower: Flt64,
            upper: Flt64,
            segments: Int = 16,
            steepness: Flt64 = Flt64.one,
            midpoint: Flt64 = Flt64.zero,
            converter: IntoValue<V>,
            name: String = "logistic",
            displayName: String? = null
        ): Ret<LogisticApproximationFunction<V>> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
            return withUniformBreakpoints(
                lower = lower,
                upper = upper,
                segments = segments,
                create = { points ->
                    create(
                        x = x,
                        breakpoints = points,
                        steepness = steepness,
                        midpoint = midpoint,
                        converter = converter,
                        name = name,
                        displayName = displayName
                    )
                }
            )
        }

        /** 使用显式断点创建平滑 logistic 近似。 / Create a smooth logistic approximation from explicit breakpoints. */
        fun <V> create(
            x: LinearPolynomial<V>,
            breakpoints: List<Flt64>,
            steepness: Flt64 = Flt64.one,
            midpoint: Flt64 = Flt64.zero,
            converter: IntoValue<V>,
            name: String = "logistic",
            displayName: String? = null
        ): Ret<LogisticApproximationFunction<V>> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
            if (!steepness.isFinite() || steepness <= Flt64.zero || !midpoint.isFinite()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Logistic steepness 必须为正有限值且 midpoint 必须有限 / Logistic steepness must be finite and positive and midpoint must be finite"
                )
            }
            val source: (Double) -> Double = { input ->
                val z = java.lang.Double.valueOf(steepness.toString()) * (input - java.lang.Double.valueOf(midpoint.toString()))
                if (z >= 0.0) 1.0 / (1.0 + exp(-z)) else exp(z) / (1.0 + exp(z))
            }
            return when (val result = createNonlinearPiecewise(
                x = x,
                breakpoints = breakpoints,
                converter = converter,
                name = name,
                displayName = displayName,
                domainError = { _, _ -> null },
                sourceFunction = source
            )) {
                is Ok -> ok(
                    LogisticApproximationFunction(
                        x = x,
                        breakpoints = breakpoints.toList(),
                        converter = converter,
                        steepness = steepness,
                        midpoint = midpoint,
                        piecewise = result.value,
                        name = name,
                        displayName = displayName
                    )
                )
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }
    }
}
