/**
 * ONNX 符号注册与生命周期测试 / ONNX symbol registration and lifecycle tests
*/
package fuookami.ospf.kotlin.core.plugin.onnx.model

import kotlin.test.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.token.ManualTokenTable
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.core.plugin.onnx.*

/**
 * 拓扑注册、幂等性与输出符号真实性测试 / Topological registration, idempotency and output-symbol authenticity tests
*/
class OnnxRegistrationTest {

    /** 变量及有限范围 / Variable with a finite range */
    private fun bounded(name: String, lower: Double, upper: Double): RealVar {
        return RealVar(name).also {
            it.range.geq(Flt64(lower))
            it.range.leq(Flt64(upper))
        }
    }

    /** 两层线性网络 + Relu / Two-layer linear network plus Relu */
    private fun mlpBytes(): ByteArray {
        return model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(
                node("Gemm", listOf("x", "w1", "b1"), listOf("h"), name = "gemm1"),
                node("Relu", listOf("h"), listOf("a"), name = "relu"),
                node("Gemm", listOf("a", "w2", "b2"), listOf("y"), name = "gemm2")
            ),
            initializers = listOf(
                floatTensor("w1", listOf(2L, 2L), listOf(1.0f, -1.0f, 0.5f, 2.0f)),
                floatTensor("b1", listOf(2L), listOf(0.0f, 0.0f)),
                floatTensor("w2", listOf(2L, 1L), listOf(1.0f, 1.0f)),
                floatTensor("b2", listOf(1L), listOf(0.0f))
            )
        ).toByteArray()
    }

    @Test
    fun symbolsRegisterIntoTokenTableInTopologicalOrder() {
        val x0 = bounded("x0", -1.0, 1.0)
        val x1 = bounded("x1", -1.0, 1.0)
        val model = OnnxFunctionalModel.load(mlpBytes(), mapOf("x" to listOf<Symbol>(x0, x1))).valueOrFail()

        val tokens = AutoTokenTable<Flt64>(category = Linear, checkTokenExists = false)
        tokens.add(x0)
        tokens.add(x1)
        assertTrue(model.registerInto(tokens) is Ok)

        // 激活符号与输出符号都进入令牌表。 / Both activation and output symbols enter the token table.
        assertEquals(3, model.symbols.size)
        assertEquals(3, tokens.symbols.size)
        for (symbol in model.symbols) {
            assertTrue(tokens.symbols.any { it === symbol }, "missing symbol ${symbol.name}")
        }
    }

    @Test
    fun repeatedRegistrationIsIdempotent() {
        val x0 = bounded("x0", -1.0, 1.0)
        val x1 = bounded("x1", -1.0, 1.0)
        val model = OnnxFunctionalModel.load(mlpBytes(), mapOf("x" to listOf<Symbol>(x0, x1))).valueOrFail()

        val tokens = AutoTokenTable<Flt64>(category = Linear, checkTokenExists = false)
        tokens.add(x0)
        tokens.add(x1)
        assertTrue(model.registerInto(tokens) is Ok)
        val afterFirst = tokens.symbols.size
        assertTrue(model.registerInto(tokens) is Ok)
        assertEquals(afterFirst, tokens.symbols.size)
    }

    @Test
    fun registrationRequiresMutableTokenTable() {
        val x0 = bounded("x0", -1.0, 1.0)
        val x1 = bounded("x1", -1.0, 1.0)
        val model = OnnxFunctionalModel.load(mlpBytes(), mapOf("x" to listOf<Symbol>(x0, x1))).valueOrFail()

        val manual = ManualTokenTable<Flt64>(category = Linear, checkTokenExists = false)
        manual.add(x0)
        manual.add(x1)
        assertTrue(model.registerInto(manual) is Ok)
    }

    @Test
    fun outputPolynomialIsRealNotZero() {
        val x0 = bounded("x0", -1.0, 1.0)
        val x1 = bounded("x1", -1.0, 1.0)
        val model = OnnxFunctionalModel.load(mlpBytes(), mapOf("x" to listOf<Symbol>(x0, x1))).valueOrFail()

        val output = model.linearOutput("y", 0).valueOrFail()
        // 结果多项式必须引用真实符号，不能用零多项式掩盖黑盒。
        // The result polynomial must reference real symbols; a zero polynomial must not hide a black box.
        assertTrue(output.polynomial.monomials.isNotEmpty())
        assertTrue(output.polynomial.monomials.all { it.coefficient.compareTo(Flt64.zero) != 0 })
    }

    @Test
    fun symbolListContainsNoDuplicates() {
        val x0 = bounded("x0", -1.0, 1.0)
        val x1 = bounded("x1", -1.0, 1.0)
        val model = OnnxFunctionalModel.load(mlpBytes(), mapOf("x" to listOf<Symbol>(x0, x1))).valueOrFail()

        val identities = model.symbols.map { System.identityHashCode(it) }
        assertEquals(identities.size, identities.distinct().size)
    }

    @Test
    fun reportCapturesModeAndSegments() {
        val x0 = bounded("x0", -2.0, 2.0)
        val x1 = bounded("x1", -2.0, 2.0)
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 2L))),
            nodes = listOf(node("Sigmoid", listOf("x"), listOf("y"), name = "sigmoid"))
        ).toByteArray()
        val options = OnnxImportOptions(
            mode = OnnxImportMode.Quadratic,
            activationApproximation = OnnxActivationApproximation(segments = 16)
        )
        val model = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x0, x1)), options).valueOrFail()
        assertEquals(OnnxImportMode.Quadratic, model.report.mode)
        assertEquals(16, model.report.segments)
        assertEquals(listOf("sigmoid"), model.report.approximatedActivations)
    }

    @Test
    fun loadFromPathAndStreamWorks() {
        val bytes = mlpBytes()
        val path = java.nio.file.Files.createTempFile("ospf-onnx-model-", ".onnx")
        try {
            java.nio.file.Files.write(path, bytes)
            val x0 = bounded("x0", -1.0, 1.0)
            val x1 = bounded("x1", -1.0, 1.0)
            val bindings = mapOf("x" to listOf<Symbol>(x0, x1))

            val fromPath = OnnxFunctionalModel.load(path, bindings).valueOrFail()
            assertEquals(3, fromPath.symbols.size)

            val stream = java.io.ByteArrayInputStream(bytes)
            val fromStream = OnnxFunctionalModel.load(stream, bindings).valueOrFail()
            assertEquals(3, fromStream.symbols.size)
            // 流不被关闭。 / The stream stays open.
            assertTrue(stream.read() == -1)
        } finally {
            java.nio.file.Files.deleteIfExists(path)
        }
    }

    @Test
    fun missingFileFails() {
        val result = OnnxFunctionalModel.load(
            java.nio.file.Paths.get("no-such-model.onnx"),
            mapOf("x" to listOf<Symbol>(bounded("x0", -1.0, 1.0)))
        )
        assertIs<fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.LoadFailed>(result.structuredError())
    }
}
