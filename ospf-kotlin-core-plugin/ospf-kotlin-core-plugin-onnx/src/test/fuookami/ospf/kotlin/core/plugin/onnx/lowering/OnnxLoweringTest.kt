/**
 * ONNX 降阶测试 / ONNX lowering tests
*/
package fuookami.ospf.kotlin.core.plugin.onnx.lowering

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.plugin.onnx.*
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
import fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxImportMode
import fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxImportOptions
import fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxFunctionalModel

/**
 * 线性/二次算子降阶与失败路径测试 / Linear and quadratic lowering plus failure-path tests
*/
class OnnxLoweringTest {

    /** 变量及其有限范围 / Variables with finite ranges */
    private fun variables(vararg names: String): Map<String, RealVar> {
        return names.associateWith { name ->
            RealVar(name).also { variable ->
                variable.range.geq(Flt64(-10.0))
                variable.range.leq(Flt64(10.0))
            }
        }
    }

    /** 取输出符号的多项式文本 / Read the polynomial of an output symbol */
    private fun linearOutputModel(
        bytes: ByteArray,
        bindings: Map<String, List<Symbol>>,
        options: OnnxImportOptions = OnnxImportOptions()
    ): OnnxFunctionalModel {
        return OnnxFunctionalModel.load(bytes, bindings, options).valueOrFail()
    }

    @Test
    fun gemmReluGemmLoweringProducesLinearOutput() {
        val x0 = RealVar("x0").also {
            it.range.geq(Flt64(-1.0))
            it.range.leq(Flt64(1.0))
        }
        val x1 = RealVar("x1").also {
            it.range.geq(Flt64(-1.0))
            it.range.leq(Flt64(1.0))
        }
        val bytes = model(
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

        val model = linearOutputModel(bytes, mapOf("x" to listOf(x0, x1)))
        assertEquals(OnnxImportMode.Linear, model.report.mode)
        assertEquals(3, model.report.nodes)
        // Relu 是精确表达，不属于分段线性近似。 / Relu is exact and is not a piecewise-linear approximation.
        assertTrue(model.report.approximatedActivations.isEmpty())
        // Relu 逐元素作用在 1×2 张量上，得到 2 个激活符号，再加 1 个输出符号。
        // Relu acts element-wise on the 1×2 tensor, giving 2 activation symbols plus 1 output symbol.
        assertEquals(3, model.symbols.size)

        val output = model.linearOutput("y", 0).valueOrFail()
        // y = relu(x0 + 0.5·x1) + relu(-x0 + 2·x1)
        assertEquals(2, output.polynomial.monomials.size)
        assertTrue(model.outputs("y").valueOrFail().size == 1)
    }

    @Test
    fun broadcastAddSubMulAreSupported() {
        val x0 = RealVar("x0")
        val x1 = RealVar("x1")
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(2L, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(2L, 2L))),
            nodes = listOf(
                node("Add", listOf("x", "b"), listOf("s"), name = "add"),
                node("Sub", listOf("s", "b"), listOf("d"), name = "sub"),
                node("Mul", listOf("d", "b"), listOf("y"), name = "mul")
            ),
            initializers = listOf(floatTensor("b", listOf(2L), listOf(1.0f, 2.0f)))
        ).toByteArray()

        val model = linearOutputModel(
            bytes = bytes,
            bindings = mapOf("x" to listOf(x0, x1, x0, x1))
        )
        // 每步都广播 1×2 到 2×2：y[i][j] = ((x[i][j] + b[j]) - b[j]) * b[j] = x[i][j] * b[j]
        val y = model.outputs("y").valueOrFail()
        assertEquals(4, y.size)
        val first = model.linearOutput("y", 0).valueOrFail()
        assertEquals(1.0, first.polynomial.monomials.single().coefficient.toDouble())
        val second = model.linearOutput("y", 1).valueOrFail()
        assertEquals(2.0, second.polynomial.monomials.single().coefficient.toDouble())
    }

    @Test
    fun matmulAndTransposeAndReshapeAreSupported() {
        val x0 = RealVar("x0")
        val x1 = RealVar("x1")
        val x2 = RealVar("x2")
        val x3 = RealVar("x3")
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(2L, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(2L, 2L))),
            nodes = listOf(
                node("Transpose", listOf("x"), listOf("t"), name = "transpose") {
                    addAttribute(intsAttr("perm", listOf(1L, 0L)))
                },
                node("MatMul", listOf("t", "w"), listOf("m"), name = "matmul"),
                node("Reshape", listOf("m", "shape"), listOf("y"), name = "reshape")
            ),
            initializers = listOf(
                floatTensor("w", listOf(2L, 2L), listOf(1.0f, 0.0f, 0.0f, 1.0f)),
                longTensor("shape", listOf(2L), listOf(2L, 2L))
            )
        ).toByteArray()

        val model = linearOutputModel(
            bytes = bytes,
            bindings = mapOf("x" to listOf(x0, x1, x2, x3))
        )
        // x = [[x0, x1], [x2, x3]]，转置后 [[x0, x2], [x1, x3]]，乘单位阵不变。
        val first = model.linearOutput("y", 0).valueOrFail()
        assertEquals("x0", first.polynomial.monomials.single().symbol.name)
        val second = model.linearOutput("y", 1).valueOrFail()
        assertEquals("x2", second.polynomial.monomials.single().symbol.name)
    }

    @Test
    fun concatSplitGatherReduceSumAreSupported() {
        val variables = (0 until 4).map { RealVar("x$it") }
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 4L))),
            outputs = listOf(floatValueInfo("s", listOf(1L, 1L))),
            nodes = listOf(
                node("Split", listOf("x", "split"), listOf("a", "b"), name = "split") {
                    addAttribute(intAttr("axis", 1L))
                },
                node("Concat", listOf("a", "b"), listOf("c"), name = "concat") {
                    addAttribute(intAttr("axis", 1L))
                },
                node("Gather", listOf("c", "indices"), listOf("g"), name = "gather") {
                    addAttribute(intAttr("axis", 1L))
                },
                node("ReduceSum", listOf("g", "axes"), listOf("s"), name = "reduce") {
                    addAttribute(intAttr("keepdims", 1L))
                }
            ),
            initializers = listOf(
                longTensor("split", listOf(2L), listOf(2L, 2L)),
                longTensor("indices", listOf(2L), listOf(0L, 3L)),
                longTensor("axes", listOf(1L), listOf(1L))
            )
        ).toByteArray()

        val model = linearOutputModel(
            bytes = bytes,
            bindings = mapOf("x" to variables)
        )
        // gather 得到 [x0, x3]，reduce 求和 => x0 + x3
        val output = model.linearOutput("s", 0).valueOrFail()
        assertEquals(2, output.polynomial.monomials.size)
        assertEquals(setOf("x0", "x3"), output.polynomial.monomials.map { it.symbol.name }.toSet())
    }

    @Test
    fun flattenProducesExpectedShape() {
        val variables = (0 until 4).map { RealVar("x$it") }
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(2L, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(4L))),
            nodes = listOf(
                node("Flatten", listOf("x"), listOf("f"), name = "flatten") {
                    addAttribute(intAttr("axis", 0L))
                },
                node("Reshape", listOf("f", "shape"), listOf("y"), name = "reshape")
            ),
            initializers = listOf(longTensor("shape", listOf(1L), listOf(4L)))
        ).toByteArray()

        val model = linearOutputModel(bytes, mapOf("x" to variables))
        val outputs = model.outputs("y").valueOrFail()
        assertEquals(4, outputs.size)
        assertEquals("x0", model.linearOutput("y", 0).valueOrFail().polynomial.monomials.single().symbol.name)
        assertEquals("x3", model.linearOutput("y", 3).valueOrFail().polynomial.monomials.single().symbol.name)
    }

    @Test
    fun quadraticModeAllowsSymbolProduct() {
        val x0 = RealVar("x0").also {
            it.range.geq(Flt64(-5.0))
            it.range.leq(Flt64(5.0))
        }
        val x1 = RealVar("x1").also {
            it.range.geq(Flt64(-5.0))
            it.range.leq(Flt64(5.0))
        }
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(node("Mul", listOf("x", "x"), listOf("y"), name = "square"))
        ).toByteArray()

        val model = linearOutputModel(
            bytes = bytes,
            bindings = mapOf("x" to listOf(x0, x1)),
            options = OnnxImportOptions(mode = OnnxImportMode.Quadratic)
        )
        val output = model.quadraticOutput("y", 0).valueOrFail()
        assertTrue(output.polynomial.monomials.any { it.isQuadratic })
    }

    @Test
    fun linearModeRejectsSymbolProduct() {
        val x0 = RealVar("x0")
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L))),
            nodes = listOf(node("Mul", listOf("x", "x"), listOf("y"), name = "square"))
        ).toByteArray()

        val result = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x0)))
        assertIs<OnnxError.DegreeOverflow>(result.structuredError())
    }

    @Test
    fun unknownOperatorFails() {
        val x0 = RealVar("x0")
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L))),
            nodes = listOf(node("Cos", listOf("x"), listOf("y"), name = "cos"))
        ).toByteArray()

        val result = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x0)))
        assertIs<OnnxError.UnsupportedOperator>(result.structuredError())
    }

    @Test
    fun dynamicShapeFails() {
        val x0 = RealVar("x0")
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(null, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L))),
            nodes = listOf(node("Identity", listOf("x"), listOf("y"), name = "identity"))
        ).toByteArray()

        val result = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x0)))
        assertIs<OnnxError.DynamicShape>(result.structuredError())
    }

    @Test
    fun shapeMismatchFails() {
        val variables = (0 until 2).map { RealVar("x$it") }
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 3L))),
            nodes = listOf(
                node("Add", listOf("x", "b"), listOf("y"), name = "add")
            ),
            initializers = listOf(floatTensor("b", listOf(3L), listOf(1.0f, 2.0f, 3.0f)))
        ).toByteArray()

        val result = OnnxFunctionalModel.load(bytes, mapOf("x" to variables))
        assertIs<OnnxError.ShapeMismatch>(result.structuredError())
    }

    @Test
    fun bindingCountMismatchFails() {
        val x0 = RealVar("x0")
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 2L))),
            nodes = listOf(node("Identity", listOf("x"), listOf("y"), name = "identity"))
        ).toByteArray()

        val result = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x0)))
        assertIs<OnnxError.ShapeMismatch>(result.structuredError())
    }

    @Test
    fun missingBindingFails() {
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L))),
            nodes = listOf(node("Identity", listOf("x"), listOf("y"), name = "identity"))
        ).toByteArray()

        val result = OnnxFunctionalModel.load(bytes, emptyMap())
        assertIs<OnnxError.ShapeMismatch>(result.structuredError())
    }

    @Test
    fun batchedMatMulIsSupported() {
        // 形状 [2, 2, 2] × [2, 2, 2]：批量维 2 个独立矩阵相乘。
        // Shape [2, 2, 2] × [2, 2, 2]: two independent matrices per batch entry.
        val variables = (0 until 8).map { RealVar("x$it") }
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(2L, 2L, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(2L, 2L, 2L))),
            nodes = listOf(node("MatMul", listOf("x", "w"), listOf("y"), name = "matmul")),
            initializers = listOf(
                floatTensor("w", listOf(2L, 2L), listOf(1.0f, 0.0f, 0.0f, 1.0f))
            )
        ).toByteArray()

        val model = linearOutputModel(bytes, mapOf("x" to variables))
        val outputs = model.outputs("y").valueOrFail()
        assertEquals(8, outputs.size)
        // 乘单位阵后元素顺序不变。 / Multiplying by the identity keeps the element order.
        for (index in 0 until 8) {
            val output = model.linearOutput("y", index).valueOrFail()
            assertEquals("x$index", output.polynomial.monomials.single().symbol.name)
        }
    }

    @Test
    fun batchedMatMulBroadcastsBatchDimensions() {
        // [1, 2, 2] × [3, 2, 2]：左侧批量维广播到 3。 / [1, 2, 2] × [3, 2, 2]: the left batch dimension broadcasts to 3.
        val variables = (0 until 4).map { RealVar("x$it") }
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 2L, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(3L, 2L, 2L))),
            nodes = listOf(node("MatMul", listOf("x", "w"), listOf("y"), name = "matmul")),
            initializers = listOf(
                floatTensor(
                    "w",
                    listOf(3L, 2L, 2L),
                    listOf(
                        1.0f, 0.0f, 0.0f, 1.0f,
                        2.0f, 0.0f, 0.0f, 2.0f,
                        3.0f, 0.0f, 0.0f, 3.0f
                    )
                )
            )
        ).toByteArray()

        val model = linearOutputModel(bytes, mapOf("x" to variables))
        assertEquals(12, model.outputs("y").valueOrFail().size)
        // 第二个批次放大 2 倍：y[1][0][0] = 2·x0。 / The second batch scales by 2: y[1][0][0] = 2·x0.
        val secondBatchFirst = model.linearOutput("y", 4).valueOrFail()
        assertEquals(2.0, secondBatchFirst.polynomial.monomials.single().coefficient.toDouble())
        val thirdBatchFirst = model.linearOutput("y", 8).valueOrFail()
        assertEquals(3.0, thirdBatchFirst.polynomial.monomials.single().coefficient.toDouble())
    }

    @Test
    fun batchedMatMulRejectsIncompatibleBatchDimensions() {
        val variables = (0 until 8).map { RealVar("x$it") }
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(2L, 2L, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(3L, 2L, 2L))),
            nodes = listOf(node("MatMul", listOf("x", "w"), listOf("y"), name = "matmul")),
            initializers = listOf(
                floatTensor("w", listOf(3L, 2L, 2L), List(12) { 1.0f })
            )
        ).toByteArray()

        val result = OnnxFunctionalModel.load(bytes, mapOf("x" to variables))
        assertIs<OnnxError.ShapeMismatch>(result.structuredError())
    }

    @Test
    fun unknownOutputFailsSelection() {
        val variables = (0 until 2).map { RealVar("x$it") }
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 2L))),
            nodes = listOf(node("Identity", listOf("x"), listOf("y"), name = "identity"))
        ).toByteArray()

        val model = linearOutputModel(bytes, mapOf("x" to variables))
        assertIs<OnnxError.OutputSelection>(model.linearOutput("not_y", 0).structuredError())
        assertIs<OnnxError.OutputSelection>(model.linearOutput("y", 5).structuredError())
    }

    @Test
    fun quadraticOutputRejectedInLinearMode() {
        val variables = (0 until 2).map { RealVar("x$it") }
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 2L))),
            nodes = listOf(node("Identity", listOf("x"), listOf("y"), name = "identity"))
        ).toByteArray()

        val model = linearOutputModel(bytes, mapOf("x" to variables))
        assertIs<OnnxError.OutputSelection>(model.quadraticOutput("y", 0).structuredError())
    }

    @Test
    fun evaluateUsesExpressionsNotRuntime() {
        val x0 = RealVar("x0")
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(node("Identity", listOf("x"), listOf("y"), name = "identity"))
        ).toByteArray()

        val model = linearOutputModel(bytes, mapOf("x" to listOf<Symbol>(x0)))
        val output = model.linearOutput("y", 0).valueOrFail()
        // 没有推理句柄参与，纯表达式求值。 / No inference handle participates; this is pure expression evaluation.
        assertEquals("x0", output.polynomial.monomials.single().symbol.name)
        assertEquals(1.0, output.polynomial.monomials.single().coefficient.toDouble())
    }
}
