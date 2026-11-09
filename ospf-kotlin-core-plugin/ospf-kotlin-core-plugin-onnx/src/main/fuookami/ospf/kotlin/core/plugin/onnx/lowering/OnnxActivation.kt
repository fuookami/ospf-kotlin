/**
 * ONNX 激活函数降阶 / ONNX activation lowering
*/
package fuookami.ospf.kotlin.core.plugin.onnx.lowering

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tanh
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.QuadraticExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.function.ClampFunction
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.PositivePartFunction
import fuookami.ospf.kotlin.core.symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.symbol.function.finiteBounds
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxNode

/**
 * 激活函数降阶 / Activation lowering
 *
 * 激活输入可以是仿射表达式，也可以是二次表达式：二次输入会先实现为中间变量
 * `aux == q(x)`，再对 `aux` 施加激活。线性模式下二次输入仍然拒绝。 / Activation inputs may be affine or quadratic: a quadratic input is first realized as an intermediate
 * variable `aux == q(x)` and the activation then applies to `aux`. Quadratic inputs are still rejected
 * under linear mode.
 *
 * `Relu` 与 `Clip` 复用核心的精确函数符号，`LeakyRelu` 与 `HardSigmoid` 用精确线性组合，
 * `Sigmoid`/`Tanh`/`Softplus`/`Elu` 使用可配置段数的分段线性近似。 / `Relu` and `Clip` reuse the core exact function symbols, `LeakyRelu` and `HardSigmoid` use exact
 * linear combinations, and `Sigmoid`/`Tanh`/`Softplus`/`Elu` use a configurable piecewise-linear
 * approximation.
*/

/**
 * 桥接后的仿射激活输入 / Affine activation input after bridging
 *
 * @property polynomial 仿射多项式 / affine polynomial
 * @property symbols 桥接生成的中间符号 / intermediate symbols produced by the bridge
*/
internal data class OnnxBridgedInput(
    val polynomial: LinearPolynomial<Flt64>,
    val symbols: List<IntermediateSymbol<Flt64>>
)

internal object OnnxActivation {

    /** 支持的激活算子 / Supported activation operators */
    val OPERATORS: Set<String> = setOf(
        "Relu",
        "Clip",
        "LeakyRelu",
        "HardSigmoid",
        "Sigmoid",
        "Tanh",
        "Softplus",
        "Elu"
    )

    /** 需要分段线性近似的算子 / Operators requiring a piecewise-linear approximation */
    val APPROXIMATED: Set<String> = setOf("Sigmoid", "Tanh", "Softplus", "Elu")

    /** 默认分段数 / Default segment count */
    const val DEFAULT_SEGMENTS: Int = 8

    /** 允许的分段数区间 / Allowed segment count range */
    val SEGMENT_RANGE: IntRange = 2..1024

    /**
     * 分段线性近似允许的最大输入范围绝对值。
     *
     * 默认变量范围是 ±Double.MAX_VALUE 这种"形式有限但实际无界"的区间，直接分段会得到无意义的近似。
     * 超过该阈值视为缺少有效范围，要求调用方通过 `inputBounds` 显式给出。 / Maximum absolute input bound allowed for a piecewise-linear approximation.
     *
     * The default variable range is a formally finite but practically unbounded interval such as
     * ±Double.MAX_VALUE; splitting it produces a meaningless approximation. Values beyond this
     * threshold are treated as missing bounds and require an explicit `inputBounds` override.
     */
    const val MAX_PIECEWISE_BOUND: Double = 1.0e6

    /** 值转换器 / Value converter */
    private val converter: IntoValue<Flt64> = IntoValue.Identity

    /**
     * 降阶一个激活节点 / Lower one activation node
     *
     * @param node 节点 / node
     * @param input 输入张量 / input tensor
     * @param context 降阶上下文 / lowering context
     * @param segments 分段线性近似的段数 / segment count of the piecewise-linear approximation
     * @return 降阶结果 / lowering result
     */
    fun lower(
        node: OnnxNode,
        input: OnnxTensorExpr,
        context: OnnxLoweringContext,
        segments: Int = DEFAULT_SEGMENTS
    ): OnnxShapeResult<OnnxTensorExpr> {
        if (segments !in SEGMENT_RANGE) {
            return OnnxShapeResult.Failure(
                OnnxErrorFactory.unsupportedAttribute(
                    attributeName = "segments",
                    detail = "分段数 $segments 超出允许区间 $SEGMENT_RANGE / segment count $segments is outside $SEGMENT_RANGE",
                    nodeName = node.name
                )
            )
        }
        val elements = ArrayList<OnnxExpr>(input.elements.size)
        for ((position, element) in input.elements.withIndex()) {
            val name = "${node.name}_$position"
            val bridged = when (element) {
                is OnnxExpr.Linear -> OnnxBridgedInput(element.polynomial, emptyList())
                is OnnxExpr.Constant -> OnnxBridgedInput(
                    LinearPolynomial(monomials = emptyList(), constant = element.value),
                    emptyList()
                )

                is OnnxExpr.Quadratic -> when (
                    val result = bridge(name, position, element.polynomial, node, context)
                ) {
                    is Ok -> result.value
                    is Failed -> {
                        val structured = result.error.value as? OnnxError
                        if (structured != null) {
                            return OnnxShapeResult.Failure(structured)
                        }
                        return OnnxShapeResult.Failure(
                            exprFailure(node, position, result.error.message ?: "bridge failed")
                        )
                    }

                    is Fatal -> return OnnxShapeResult.Failure(
                        exprFailure(node, position, result.errors.first().message ?: "bridge failed")
                    )
                }
            }
            val affine = bridged.polynomial
            val lowered = when (node.opType) {
                "Relu" -> relu(name, affine)
                "Clip" -> clip(node, name, affine)
                "LeakyRelu" -> leakyRelu(node, name, affine)
                "HardSigmoid" -> hardSigmoid(node, name, affine)
                "Sigmoid" -> approximated(name, affine, segments, ::sigmoid)
                "Tanh" -> approximated(name, affine, segments, ::hyperbolicTangent)
                "Softplus" -> approximated(name, affine, segments, ::softplus)
                "Elu" -> approximated(name, affine, segments, ::elu)
                else -> return OnnxShapeResult.Failure(
                    OnnxErrorFactory.unsupportedOperator(
                        opType = node.opType,
                        detail = "激活算子不在白名单内 / the activation operator is not whitelisted",
                        nodeName = node.name
                    )
                )
            }
            val produced = when (lowered) {
                is OnnxActivationResult.Success -> lowered
                is OnnxActivationResult.Failure -> return OnnxShapeResult.Failure(lowered.error)
            }
            context.symbols.addAll(bridged.symbols)
            context.symbols.addAll(produced.symbols)
            if (node.opType in APPROXIMATED && node.name !in context.approximatedActivations) {
                context.approximatedActivations.add(node.name)
            }
            // 激活结果以结果变量参与后续表达式。 / The activation result enters later expressions through its result variable.
            elements.add(OnnxExpr.Linear(produced.polynomial))
        }
        return OnnxShapeResult.Success(OnnxTensorExpr(input.shape, elements))
    }

    /**
     * 把二次输入桥接为仿射输入 / Bridge a quadratic input into an affine input
     *
     * 二次表达式先实现为中间变量 `aux == q(x)`，激活再作用于 `aux`。这一写法与把二次表达式
     * 直接展开进激活的 Big-M 约束在最优解上等价，且不会让约束规模按激活元素数平方膨胀。
     * A quadratic expression is realized as an intermediate variable `aux == q(x)` and the activation then
     * applies to `aux`. This is equivalent to inlining the quadratic into the activation's Big-M constraints at
     * the optimum, without letting the constraint count grow quadratically in the activation element count.
     *
     * @param name 结果名称 / result name
     * @param position 元素序号 / element position
     * @param polynomial 二次多项式 / quadratic polynomial
     * @param node 节点 / node
     * @param context 降阶上下文 / lowering context
     * @return 桥接结果 / bridge result
     */
    private fun bridge(
        name: String,
        position: Int,
        polynomial: QuadraticPolynomial<Flt64>,
        node: OnnxNode,
        context: OnnxLoweringContext
    ): Ret<OnnxBridgedInput> {
        // ponytail: 线性模式不做可行化，q(x) 与激活的组合本质上是非线性，硬塞进 LP 只会得到错误的最优解。
        // ponytail: linear mode gives up deliberately; composing q(x) with an activation is inherently nonlinear,
        // squeezing it into an LP would only produce a wrong optimum.
        if (context.mode != OnnxDegreeMode.Quadratic) {
            return failed(
                OnnxErrorFactory.unsupportedOperator(
                    opType = node.opType,
                    detail = "线性模式下二次输入无法送激活，元素 $position / " +
                        "a quadratic input cannot feed an activation under linear mode, element $position",
                    nodeName = node.name
                ).toError()
            )
        }
        val symbols = ArrayList<IntermediateSymbol<Flt64>>(1)
        val mutable = MutableQuadraticPolynomial<Flt64>(
            monomials = polynomial.monomials.toList(),
            constant = polynomial.constant
        )
        val symbol = QuadraticExpressionSymbol(
            _utilsPolynomial = mutable,
            category = fuookami.ospf.kotlin.math.symbol.Quadratic,
            name = "${name}_input"
        )
        // 中间变量必须能拿到有限范围，否则分段线性近似无法建立。
        // The intermediate variable needs a finite range, otherwise the piecewise-linear approximation cannot be built.
        val bounds = polynomial.finiteBounds(converter)
            ?: return failed(
                OnnxErrorFactory.missingBound(
                    detail = "二次激活输入的元素 $position 缺少有限范围 / " +
                        "quadratic activation input element $position has no finite bounds",
                    nodeName = node.name
                ).toError()
            )
        symbol.useIntermediateVariable = true
        val variable = symbol.intermediateVariable
            ?: return failed(
                OnnxErrorFactory.unsupportedOperator(
                    opType = node.opType,
                    detail = "二次中间变量未生成，元素 $position / the quadratic intermediate variable was not materialized, element $position",
                    nodeName = node.name
                ).toError()
            )
        @Suppress("UNCHECKED_CAST")
        val range = variable.range as ExpressionRange<Flt64>
        if (!range.geq(bounds.lower) || !range.leq(bounds.upper)) {
            return failed(
                OnnxErrorFactory.missingBound(
                    detail = "二次激活输入的取值范围 $bounds 与中间变量取值域不相交 / " +
                        "the range $bounds of the quadratic activation input does not intersect the intermediate variable range",
                    nodeName = node.name
                ).toError()
            )
        }
        symbols.add(symbol)
        val affine = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, variable)),
            constant = Flt64.zero
        )
        return ok(OnnxBridgedInput(affine, symbols))
    }

    /** 构造带元素上下文的失败 / Build a failure carrying element context */
    private fun exprFailure(node: OnnxNode, position: Int, detail: String): OnnxError {
        return OnnxErrorFactory.unsupportedOperator(
            opType = node.opType,
            detail = "$detail（元素 $position / element $position）",
            nodeName = node.name
        )
    }

    /** `Relu` 精确正部 / Exact positive part for `Relu` */
    private fun relu(name: String, input: LinearPolynomial<Flt64>): OnnxActivationResult {        return when (val result = PositivePartFunction(input, converter, name = name)) {
            is Ok -> OnnxActivationResult.Success.adapt(LinearFunctionSymbolAdapter(result.value, converter))
            is Failed -> OnnxActivationResult.Failure(boundFailure(name, result.error))
            is Fatal -> OnnxActivationResult.Failure(boundFailure(name, result.errors.first()))
        }
    }

    /** `Clip` 精确截断 / Exact clamp for `Clip` */
    private fun clip(node: OnnxNode, name: String, input: LinearPolynomial<Flt64>): OnnxActivationResult {
        val lower = node.attributes.floatOrNull("min")
        val upper = node.attributes.floatOrNull("max")
        if (lower == null && upper == null) {
            // 两侧都未指定时 Clip 是恒等映射。 / Clip is the identity map when neither side is specified.
            return OnnxActivationResult.Success.expression(name, input)
        }
        val bounds = input.finiteBounds(converter)
            ?: return OnnxActivationResult.Failure(boundFailure(name, null))
        val lowerValue = lower?.let { Flt64(it) } ?: bounds.lower
        val upperValue = upper?.let { Flt64(it) } ?: bounds.upper
        if (lowerValue.compareTo(upperValue) > 0) {
            return OnnxActivationResult.Failure(
                OnnxErrorFactory.unsupportedAttribute(
                    attributeName = "min",
                    detail = "Clip 下界 $lowerValue 大于上界 $upperValue / Clip lower bound $lowerValue exceeds upper $upperValue",
                    nodeName = node.name
                )
            )
        }
        return when (
            val result = ClampFunction(
                input = input,
                lower = lowerValue,
                upper = upperValue,
                converter = converter,
                name = name
            )
        ) {
            is Ok -> OnnxActivationResult.Success.adapt(LinearFunctionSymbolAdapter(result.value, converter))
            is Failed -> OnnxActivationResult.Failure(boundFailure(name, result.error))
            is Fatal -> OnnxActivationResult.Failure(boundFailure(name, result.errors.first()))
        }
    }

    /** `LeakyRelu` 精确表达 `x - (1-alpha)·relu(-x)` / Exact `LeakyRelu` as `x - (1-alpha)·relu(-x)` */
    private fun leakyRelu(node: OnnxNode, name: String, input: LinearPolynomial<Flt64>): OnnxActivationResult {
        val alpha = Flt64(node.attributes.float("alpha", 0.01))
        if (alpha.compareTo(Flt64.zero) < 0) {
            return OnnxActivationResult.Failure(
                OnnxErrorFactory.unsupportedAttribute(
                    attributeName = "alpha",
                    detail = "LeakyRelu 的 alpha 不得为负 / LeakyRelu alpha must not be negative",
                    nodeName = node.name
                )
            )
        }
        if (alpha.compareTo(Flt64.one) == 0) {
            return OnnxActivationResult.Success.expression(name, input)
        }
        val positivePart = when (val result = PositivePartFunction(-input, converter, name = "${name}_neg")) {
            is Ok -> result.value
            is Failed -> return OnnxActivationResult.Failure(boundFailure(name, result.error))
            is Fatal -> return OnnxActivationResult.Failure(boundFailure(name, result.errors.first()))
        }
        val slope = Flt64.one - alpha
        val positivePolynomial = positivePart.resultPolynomial
        val combined = LinearPolynomial(
            monomials = input.monomials + positivePolynomial.monomials.map {
                LinearMonomial(it.coefficient * slope, it.symbol)
            },
            constant = input.constant + positivePolynomial.constant * slope
        )
        // 正部函数符号必须一并注册，否则组合表达式引用不到结果变量。
        // The positive-part symbol must be registered as well, otherwise the combined expression cannot resolve its result variable.
        return OnnxActivationResult.Success.composite(
            symbols = listOf(LinearFunctionSymbolAdapter(positivePart, converter)),
            name = name,
            polynomial = combined
        )
    }

    /** `HardSigmoid` 精确表达 `clamp(alpha·x + beta, 0, 1)` / Exact `HardSigmoid` as `clamp(alpha·x + beta, 0, 1)` */
    private fun hardSigmoid(node: OnnxNode, name: String, input: LinearPolynomial<Flt64>): OnnxActivationResult {
        val alpha = Flt64(node.attributes.float("alpha", 0.2))
        val beta = Flt64(node.attributes.float("beta", 0.5))
        val shifted = LinearPolynomial(
            monomials = input.monomials.map { LinearMonomial(it.coefficient * alpha, it.symbol) },
            constant = input.constant * alpha + beta
        )
        return when (
            val result = ClampFunction(
                input = shifted,
                lower = Flt64.zero,
                upper = Flt64.one,
                converter = converter,
                name = name
            )
        ) {
            is Ok -> OnnxActivationResult.Success.adapt(LinearFunctionSymbolAdapter(result.value, converter))
            is Failed -> OnnxActivationResult.Failure(boundFailure(name, result.error))
            is Fatal -> OnnxActivationResult.Failure(boundFailure(name, result.errors.first()))
        }
    }

    /** 分段线性近似 / Piecewise-linear approximation */
    private fun approximated(
        name: String,
        input: LinearPolynomial<Flt64>,
        segments: Int,
        function: (Double) -> Double
    ): OnnxActivationResult {
        val bounds = input.finiteBounds(converter)
            ?: return OnnxActivationResult.Failure(boundFailure(name, null))
        val lower = bounds.lower.toDouble()
        val upper = bounds.upper.toDouble()
        if (!lower.isFinite() || !upper.isFinite()) {
            return OnnxActivationResult.Failure(boundFailure(name, null))
        }
        // 形式有限但实际无界的默认取值域不做分段近似。 / Formally finite but practically unbounded default ranges are not approximated.
        if (abs(lower) > MAX_PIECEWISE_BOUND || abs(upper) > MAX_PIECEWISE_BOUND) {
            return OnnxActivationResult.Failure(boundFailure(name, null))
        }
        if (lower == upper) {
            // 常量输入直接折叠。 / A constant input collapses to a constant.
            return OnnxActivationResult.Success.expression(
                name,
                LinearPolynomial(emptyList(), Flt64(function(lower)))
            )
        }
        val breakpoints = (0..segments).map { index ->
            Flt64(lower + (upper - lower) * index / segments)
        }
        val values = breakpoints.map { function(it.toDouble()) }
        val slopes = (0 until segments).map { index ->
            Flt64(
                (values[index + 1] - values[index]) /
                    (breakpoints[index + 1].toDouble() - breakpoints[index].toDouble())
            )
        }
        val intercepts = (0 until segments).map { index ->
            Flt64(values[index] - slopes[index].toDouble() * breakpoints[index].toDouble())
        }
        val function = UnivariateLinearPiecewiseFunction(
            x = input,
            breakpoints = breakpoints,
            slopes = slopes,
            intercepts = intercepts,
            m = null,
            converter = converter,
            name = name
        )
        return OnnxActivationResult.Success.adapt(LinearFunctionSymbolAdapter(function, converter))
    }
    /** 把核心失败映射为结构化错误 / Map a core failure into a structured error */
    private fun boundFailure(name: String, error: Error<ErrorCode>?): fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError {
        val detail = error?.message?.let { ": $it" } ?: ""
        return OnnxErrorFactory.missingBound(
            detail = "激活 '$name' 需要有限输入范围$detail / activation '$name' requires finite input bounds$detail",
            tensorName = name
        )
    }

    /** `Sigmoid` / Sigmoid */
    private fun sigmoid(value: Double): Double = 1.0 / (1.0 + exp(-value))

    /** `Tanh` / Hyperbolic tangent */
    private fun hyperbolicTangent(value: Double): Double = tanh(value)

    /** `Softplus` / Softplus */
    private fun softplus(value: Double): Double = ln(1.0 + exp(value))

    /** `Elu`（alpha 固定为 1）/ `Elu` with a fixed alpha of 1 */
    private fun elu(value: Double): Double = if (value > 0.0) value else exp(value) - 1.0

    /**
     * 供测试检查数值近似误差 / Expose the scalar activation values for approximation checks in tests
     *
     * @param kind 激活类型 / activation kind
     * @param value 自变量 / argument
     * @return 激活值 / activation value
     */
    internal fun evaluateActivation(kind: String, value: Double): Double {
        return when (kind) {
            "Sigmoid" -> sigmoid(value)
            "Tanh" -> kotlin.math.tanh(value)
            "Softplus" -> softplus(value)
            "Elu" -> elu(value)
            else -> value
        }
    }
}

/**
 * 激活降阶结果 / Activation lowering result
*/
internal sealed interface OnnxActivationResult {

    /** 成功 / Success */
    data class Success(
        val symbols: List<IntermediateSymbol<Flt64>>,
        val polynomial: LinearPolynomial<Flt64>
    ) : OnnxActivationResult {

        companion object {
            /**
             * 由核心函数符号适配器构造 / Build from a core function-symbol adapter
             *
             * @param adapter 适配器 / adapter
             * @return 结果 / result
             */
            fun adapt(adapter: LinearFunctionSymbolAdapter<Flt64>): Success {
                return Success(symbols = listOf(adapter), polynomial = adapter.polynomial)
            }

            /**
             * 由纯表达式符号构造 / Build from a pure expression symbol
             *
             * @param name 符号名 / symbol name
             * @param polynomial 结果多项式 / result polynomial
             * @return 结果 / result
             */
            fun expression(name: String, polynomial: LinearPolynomial<Flt64>): Success {
                return Success(symbols = listOf(expressionSymbol(name, polynomial)), polynomial = polynomial)
            }

            /**
             * 由辅助符号加表达式符号组合构造 / Build from helper symbols plus an expression symbol
             *
             * @param symbols 需要先行注册的辅助符号 / helper symbols that must be registered first
             * @param name 符号名 / symbol name
             * @param polynomial 结果多项式 / result polynomial
             * @return 结果 / result
             */
            fun composite(
                symbols: List<IntermediateSymbol<Flt64>>,
                name: String,
                polynomial: LinearPolynomial<Flt64>
            ): Success {
                return Success(
                    symbols = symbols + expressionSymbol(name, polynomial),
                    polynomial = polynomial
                )
            }

            /** 构造一个填充了真实多项式的表达式符号 / Build an expression symbol filled with the real polynomial */
            private fun expressionSymbol(
                name: String,
                polynomial: LinearPolynomial<Flt64>
            ): LinearIntermediateSymbol<Flt64> {
                val symbol: LinearIntermediateSymbol<Flt64> = LinearIntermediateSymbol.empty(
                    constants = Flt64,
                    name = name
                )
                val mutable = symbol.asMutable()
                for (monomial in polynomial.monomials) {
                    mutable.addMonomial(monomial)
                }
                mutable.setConstant(polynomial.constant)
                return symbol
            }
        }
    }

    /** 失败 / Failure */
    data class Failure(
        val error: fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
    ) : OnnxActivationResult
}
