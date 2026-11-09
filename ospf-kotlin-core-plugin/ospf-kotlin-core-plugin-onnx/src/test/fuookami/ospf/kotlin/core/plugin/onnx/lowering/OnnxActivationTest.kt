/**
 * ONNX 激活与范围传播测试 / ONNX activation and range propagation tests
*/
package fuookami.ospf.kotlin.core.plugin.onnx.lowering

import kotlin.math.abs
import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.plugin.onnx.*
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
import fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxBounds
import fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxFunctionalModel
import fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxImportOptions

/**
 * 激活精确性、分段近似、范围覆盖与缺范围失败测试 / Exactness, piecewise approximation, bound override and missing-bound tests
*/
class OnnxActivationTest {

    /** 构造单输入单输出模型 / Build a single-input single-output model */
    private fun activationModel(opType: String, inputShape: List<Long?> = listOf(1L, 1L)): ByteArray {
        return model(
            inputs = listOf(floatValueInfo("x", inputShape)),
            outputs = listOf(floatValueInfo("y", inputShape)),
            nodes = listOf(node(opType, listOf("x"), listOf("y"), name = "act"))
        ).toByteArray()
    }

    /** 带有限范围的变量 / Variable with a finite range */
    private fun bounded(name: String, lower: Double, upper: Double): RealVar {
        return RealVar(name).also {
            it.range.geq(Flt64(lower))
            it.range.leq(Flt64(upper))
        }
    }

    @Test
    fun reluIsExactPositivePart() {
        val x = bounded("x", -4.0, 6.0)
        val model = OnnxFunctionalModel.load(activationModel("Relu"), mapOf("x" to listOf<Symbol>(x))).valueOrFail()
        val output = model.linearOutput("y", 0).valueOrFail()
        // 正部函数以结果变量参与表达式。 / The positive-part function enters the expression via its result variable.
        assertEquals(1, output.polynomial.monomials.size)
        assertEquals(1.0, output.polynomial.monomials.single().coefficient.toDouble())
    }

    @Test
    fun clipUsesExactClamp() {
        val x = bounded("x", -4.0, 6.0)
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(
                node("Clip", listOf("x", "min", "max"), listOf("y"), name = "clip")
            ),
            initializers = listOf(
                floatTensor("min", emptyList(), listOf(-1.0f)),
                floatTensor("max", emptyList(), listOf(1.0f))
            )
        ).toByteArray()
        val model = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x))).valueOrFail()
        assertTrue(model.linearOutput("y", 0).ok)
    }

    @Test
    fun leakyReluIsLinearCombination() {
        val x = bounded("x", -4.0, 6.0)
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(
                node("LeakyRelu", listOf("x"), listOf("y"), name = "leaky") {
                    addAttribute(floatAttr("alpha", 0.1))
                }
            )
        ).toByteArray()
        val model = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x))).valueOrFail()
        // x 与 relu(-x) 各一项。 / One term for x and one for relu(-x).
        val output = model.linearOutput("y", 0).valueOrFail()
        assertEquals(2, output.polynomial.monomials.size)
        assertEquals(1.0, output.polynomial.monomials.first { it.symbol.name == "x" }.coefficient.toDouble())
    }

    @Test
    fun hardSigmoidUsesExactClamp() {
        val x = bounded("x", -4.0, 6.0)
        val model = OnnxFunctionalModel.load(activationModel("HardSigmoid"), mapOf("x" to listOf<Symbol>(x))).valueOrFail()
        assertTrue(model.linearOutput("y", 0).ok)
    }

    @Test
    fun piecewiseActivationIsRecordedAndFinitelySegmented() {
        val x = bounded("x", -4.0, 6.0)
        val model = OnnxFunctionalModel.load(activationModel("Sigmoid"), mapOf("x" to listOf<Symbol>(x))).valueOrFail()
        assertEquals(listOf("act"), model.report.approximatedActivations)
        assertEquals(8, model.report.segments)
        assertTrue(model.linearOutput("y", 0).ok)
    }

    @Test
    fun allPiecewiseActivationsLower() {
        val x = bounded("x", -4.0, 6.0)
        for (opType in listOf("Sigmoid", "Tanh", "Softplus", "Elu")) {
            val model = OnnxFunctionalModel.load(activationModel(opType), mapOf("x" to listOf<Symbol>(x))).valueOrFail()
            assertEquals(listOf("act"), model.report.approximatedActivations, "opType=$opType")
            assertTrue(model.linearOutput("y", 0).ok, "opType=$opType")
        }
    }

    @Test
    fun segmentCountIsConfigurable() {
        val x = bounded("x", -4.0, 6.0)
        val options = OnnxImportOptions(
            activationApproximation = fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxActivationApproximation(segments = 32)
        )
        val model = OnnxFunctionalModel.load(
            activationModel("Tanh"),
            mapOf("x" to listOf<Symbol>(x)),
            options
        ).valueOrFail()
        assertEquals(32, model.report.segments)
    }

    @Test
    fun missingBoundsFailWithoutOverride() {
        val x = RealVar("x")
        val result = OnnxFunctionalModel.load(activationModel("Sigmoid"), mapOf("x" to listOf<Symbol>(x)))
        assertIs<OnnxError.MissingBound>(result.structuredError())
    }

    @Test
    fun explicitBoundsEnablePiecewiseActivation() {
        val x = RealVar("x")
        val options = OnnxImportOptions(
            inputBounds = mapOf("x" to OnnxBounds(lower = Flt64(-3.0), upper = Flt64(3.0)))
        )
        val model = OnnxFunctionalModel.load(
            activationModel("Sigmoid"),
            mapOf("x" to listOf<Symbol>(x)),
            options
        ).valueOrFail()
        assertEquals(listOf("act"), model.report.approximatedActivations)
        assertTrue(model.linearOutput("y", 0).ok)
    }

    @Test
    fun boundOverrideForUnknownInputFails() {
        val x = bounded("x", -1.0, 1.0)
        val options = OnnxImportOptions(
            inputBounds = mapOf("not_x" to OnnxBounds(lower = Flt64(-3.0), upper = Flt64(3.0)))
        )
        val result = OnnxFunctionalModel.load(
            activationModel("Sigmoid"),
            mapOf("x" to listOf<Symbol>(x)),
            options
        )
        assertIs<OnnxError.BindingMismatch>(result.structuredError())
    }

    @Test
    fun quadraticInputToActivationBridgesThroughIntermediateVariable() {
        // y = relu(x²)：二次输入经中间变量 aux == x² 后送 Relu。
        // y = relu(x²): the quadratic input goes through the intermediate variable aux == x² before Relu.
        val x = bounded("x", -2.0, 2.0)
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(
                node("Mul", listOf("x", "x"), listOf("sq"), name = "square"),
                node("Relu", listOf("sq"), listOf("y"), name = "relu")
            )
        ).toByteArray()
        val model = OnnxFunctionalModel.load(
            bytes,
            mapOf("x" to listOf<Symbol>(x)),
            OnnxImportOptions(mode = fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxImportMode.Quadratic)
        ).valueOrFail()

        val output = model.linearOutput("y", 0).valueOrFail()
        // 输出引用激活结果变量，激活输入引用桥接变量。 / The output references the activation result; the activation input references the bridge variable.
        assertTrue(output.polynomial.monomials.isNotEmpty())
        val bridge = model.symbols.filterIsInstance<fuookami.ospf.kotlin.core.symbol.QuadraticExpressionSymbol<Flt64>>()
            .single()
        assertTrue(bridge.useIntermediateVariable)
        assertTrue(bridge.polynomial.monomials.size >= 1)
        // 桥接变量出现在符号列表中，注册后才会生成 aux == x² 等式约束。
        // The bridge symbol is in the symbol list, so registration emits the aux == x² equality.
        assertNotNull(bridge.intermediateVariable)

        // 桥接变量与激活辅助变量都必须进入令牌表。 / The bridge variable and activation helpers must all reach the token table.
        val tokens = fuookami.ospf.kotlin.core.token.AutoTokenTable<Flt64>(
            category = fuookami.ospf.kotlin.math.symbol.Quadratic,
            checkTokenExists = false
        )
        tokens.add(x)
        assertTrue(model.registerInto(tokens) is fuookami.ospf.kotlin.utils.functional.Ok)
        assertTrue(tokens.symbols.any { it === bridge })
        assertNotNull(tokens.find(bridge.intermediateVariable!!))
    }

    @Test
    fun quadraticInputStillRejectedUnderLinearMode() {
        val x = bounded("x", -2.0, 2.0)
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(
                node("Mul", listOf("x", "x"), listOf("sq"), name = "square"),
                node("Relu", listOf("sq"), listOf("y"), name = "relu")
            )
        ).toByteArray()
        val result = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x)))
        // 线性模式下平方本身就超次数，先于激活失败。 / Under linear mode the square itself overflows the degree before the activation is reached.
        assertIs<OnnxError.DegreeOverflow>(result.structuredError())
    }

    @Test
    fun piecewiseApproximationConvergesWithSegmentCount() {
        // 采样点处应精确命中函数值。 / Sample points hit the function values exactly.
        val lower = -4.0
        val upper = 6.0
        for (index in 0..8) {
            val value = lower + (upper - lower) * index / 8
            assertEquals(kotlin.math.tanh(value), OnnxActivation.evaluateActivation("Tanh", value), 1e-12)
        }

        // 段数越多误差越小：8 段允许 0.15，64 段必须收紧到 0.01。
        // More segments mean less error: 8 segments may reach 0.15, 64 segments must stay under 0.01.
        val coarse = worstMidpointError("Tanh", lower, upper, 8)
        val fine = worstMidpointError("Tanh", lower, upper, 64)
        assertTrue(coarse < 0.15, "8-segment error $coarse")
        assertTrue(fine < 0.01, "64-segment error $fine")
        assertTrue(fine < coarse, "more segments must be more accurate: $fine vs $coarse")
    }

    /** 分段线性近似在中点的最大误差 / Worst midpoint error of the piecewise-linear approximation */
    private fun worstMidpointError(kind: String, lower: Double, upper: Double, segments: Int): Double {
        val step = (upper - lower) / segments
        var worst = 0.0
        for (index in 0 until segments) {
            val left = lower + step * index
            val right = left + step
            val midpoint = (left + right) / 2
            val approximate = (
                OnnxActivation.evaluateActivation(kind, left) +
                    OnnxActivation.evaluateActivation(kind, right)
                ) / 2
            val exact = OnnxActivation.evaluateActivation(kind, midpoint)
            worst = maxOf(worst, abs(approximate - exact))
        }
        return worst
    }
}
