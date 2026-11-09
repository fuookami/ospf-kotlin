/**
 * ONNX 回调适配测试 / ONNX call-back adapter tests
*/
package fuookami.ospf.kotlin.core.plugin.onnx.callback

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.ManualTokenTable
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.plugin.onnx.*
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxDataType
import fuookami.ospf.kotlin.core.plugin.onnx.inference.OnnxInference

/**
 * 回调目标值、约束比较、绑定校验与失败路径测试 / Objective, constraint, binding validation and failure-path tests
*/
class OnnxCallbackAdapterTest {

    /** 单输出线性模型 y = x·w + b / Single-output linear model y = x·w + b */
    private fun linearModelBytes(): ByteArray {
        return model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 2L))),
            nodes = listOf(node("Gemm", listOf("x", "w", "b"), listOf("y"), name = "gemm")),
            initializers = listOf(
                floatTensor("w", listOf(2L, 2L), listOf(1.0f, 0.0f, 0.0f, 2.0f)),
                floatTensor("b", listOf(2L), listOf(0.0f, 0.0f))
            )
        ).toByteArray()
    }

    /** 建令牌表并注册变量，返回 (令牌表, 变量) / Build a token table with registered variables */
    private fun tokenTable(vararg names: String): Pair<ManualTokenTable<Flt64>, List<RealVar>> {
        // 手动表不会自动登记未注册变量，正好用来验证绑定校验。 / A manual table does not auto-register stray variables, which is exactly what binding validation needs.
        val table = ManualTokenTable<Flt64>(category = Linear, checkTokenExists = false)
        val variables = names.map { RealVar(it) }
        for (variable in variables) {
            table.add(variable)
        }
        return table to variables
    }

    @Test
    fun objectiveExtractorReadsSelectedElement() {
        val (tokens, variables) = tokenTable("x0", "x1")
        val inference = OnnxInference.load(linearModelBytes()).valueOrFail()
        try {
            val adapter = OnnxCallbackAdapter.create(
                inference = inference,
                tokens = tokens,
                bindings = listOf(OnnxInputBinding(inputName = "x", variables = variables)),
                outputName = "y",
                elementIndex = 1,
                converter = IntoValue.Identity
            ).valueOrFail()

            val extractor = adapter.objectiveExtractor()
            // y = [x0, 2*x1]，取索引 1 得 2*x1 / y = [x0, 2*x1]; index 1 gives 2*x1
            assertEquals(6.0, extractor(listOf(Flt64(3.0), Flt64(3.0)))?.toDouble())
            assertEquals(0.0, extractor(listOf(Flt64(5.0), Flt64(0.0)))?.toDouble())
            assertEquals(6.0, adapter.evaluate(listOf(Flt64(3.0), Flt64(3.0))).valueOrFail().toDouble())
        } finally {
            inference.close()
        }
    }

    @Test
    fun constraintExtractorHonoursComparisonAndTolerance() {
        val (tokens, variables) = tokenTable("x0", "x1")
        val inference = OnnxInference.load(linearModelBytes()).valueOrFail()
        try {
            val adapter = OnnxCallbackAdapter.create(
                inference = inference,
                tokens = tokens,
                bindings = listOf(OnnxInputBinding(inputName = "x", variables = variables)),
                outputName = "y",
                elementIndex = 0,
                converter = IntoValue.Identity
            ).valueOrFail()

            // y[0] = x0
            val ge = adapter.constraintExtractor(Comparison.GE, Flt64(5.0))
            assertEquals(true, ge(listOf(Flt64(5.0), Flt64(0.0))))
            assertEquals(false, ge(listOf(Flt64(4.9), Flt64(0.0))))

            val gt = adapter.constraintExtractor(Comparison.GT, Flt64(5.0))
            assertEquals(false, gt(listOf(Flt64(5.0), Flt64(0.0))))
            assertEquals(true, gt(listOf(Flt64(5.1), Flt64(0.0))))

            val le = adapter.constraintExtractor(Comparison.LE, Flt64(5.0))
            assertEquals(true, le(listOf(Flt64(5.0), Flt64(0.0))))
            assertEquals(false, le(listOf(Flt64(5.1), Flt64(0.0))))

            val eq = adapter.constraintExtractor(Comparison.EQ, Flt64(5.0), Flt64(0.25))
            assertEquals(true, eq(listOf(Flt64(5.2), Flt64(0.0))))
            assertEquals(false, eq(listOf(Flt64(5.3), Flt64(0.0))))
        } finally {
            inference.close()
        }
    }

    @Test
    fun missingBindingFailsAtConstruction() {
        val (tokens, _) = tokenTable("x0", "x1")
        val inference = OnnxInference.load(linearModelBytes()).valueOrFail()
        try {
            val result = OnnxCallbackAdapter.create(
                inference = inference,
                tokens = tokens,
                bindings = emptyList(),
                outputName = "y",
                elementIndex = 0,
                converter = IntoValue.Identity
            )
            assertIs<OnnxError.BindingMismatch>(result.structuredError())
        } finally {
            inference.close()
        }
    }

    @Test
    fun wrongVariableCountFailsAtConstruction() {
        val (tokens, variables) = tokenTable("x0", "x1")
        val inference = OnnxInference.load(linearModelBytes()).valueOrFail()
        try {
            val result = OnnxCallbackAdapter.create(
                inference = inference,
                tokens = tokens,
                bindings = listOf(OnnxInputBinding(inputName = "x", variables = variables.take(1))),
                outputName = "y",
                elementIndex = 0,
                converter = IntoValue.Identity
            )
            assertIs<OnnxError.BindingMismatch>(result.structuredError())
        } finally {
            inference.close()
        }
    }

    @Test
    fun unregisteredVariableFailsAtConstruction() {
        val (tokens, variables) = tokenTable("x0")
        val stray = RealVar("x_missing")
        val inference = OnnxInference.load(linearModelBytes()).valueOrFail()
        try {
            val result = OnnxCallbackAdapter.create(
                inference = inference,
                tokens = tokens,
                bindings = listOf(OnnxInputBinding(inputName = "x", variables = listOf(variables.single(), stray))),
                outputName = "y",
                elementIndex = 0,
                converter = IntoValue.Identity
            )
            assertIs<OnnxError.BindingMismatch>(result.structuredError())
        } finally {
            inference.close()
        }
    }

    @Test
    fun unknownOutputNameFailsAtConstruction() {
        val (tokens, variables) = tokenTable("x0", "x1")
        val inference = OnnxInference.load(linearModelBytes()).valueOrFail()
        try {
            val result = OnnxCallbackAdapter.create(
                inference = inference,
                tokens = tokens,
                bindings = listOf(OnnxInputBinding(inputName = "x", variables = variables)),
                outputName = "not_y",
                elementIndex = 0,
                converter = IntoValue.Identity
            )
            assertIs<OnnxError.OutputSelection>(result.structuredError())
        } finally {
            inference.close()
        }
    }

    @Test
    fun outOfRangeElementIndexFailsAtConstruction() {
        val (tokens, variables) = tokenTable("x0", "x1")
        val inference = OnnxInference.load(linearModelBytes()).valueOrFail()
        try {
            val result = OnnxCallbackAdapter.create(
                inference = inference,
                tokens = tokens,
                bindings = listOf(OnnxInputBinding(inputName = "x", variables = variables)),
                outputName = "y",
                elementIndex = 5,
                converter = IntoValue.Identity
            )
            assertIs<OnnxError.OutputSelection>(result.structuredError())
        } finally {
            inference.close()
        }
    }

    @Test
    fun closedInferenceFailsAtConstruction() {
        val (tokens, variables) = tokenTable("x0", "x1")
        val inference = OnnxInference.load(linearModelBytes()).valueOrFail()
        inference.close()

        val result = OnnxCallbackAdapter.create(
            inference = inference,
            tokens = tokens,
            bindings = listOf(OnnxInputBinding(inputName = "x", variables = variables)),
            outputName = "y",
            elementIndex = 0,
            converter = IntoValue.Identity
        )
        assertIs<OnnxError.ResourceClosed>(result.structuredError())
    }

    @Test
    fun runtimeInferenceFailureReturnsNullFromExtractors() {
        val (tokens, variables) = tokenTable("x0", "x1")
        val inference = OnnxInference.load(linearModelBytes()).valueOrFail()
        val adapter = OnnxCallbackAdapter.create(
            inference = inference,
            tokens = tokens,
            bindings = listOf(OnnxInputBinding(inputName = "x", variables = variables)),
            outputName = "y",
            elementIndex = 0,
            converter = IntoValue.Identity
        ).valueOrFail()
        // 关闭句柄模拟运行时推理失败。 / Closing the handle simulates a runtime inference failure.
        inference.close()

        assertIs<OnnxError.ResourceClosed>(adapter.evaluate(listOf(Flt64(1.0), Flt64(1.0))).structuredError())
        assertNull(adapter.objectiveExtractor()(listOf(Flt64(1.0), Flt64(1.0))))
        assertNull(adapter.constraintExtractor(Comparison.GE, Flt64(0.0))(listOf(Flt64(1.0), Flt64(1.0))))
    }

    @Test
    fun shortSolutionVectorFailsEvaluation() {
        val (tokens, variables) = tokenTable("x0", "x1")
        val inference = OnnxInference.load(linearModelBytes()).valueOrFail()
        try {
            val adapter = OnnxCallbackAdapter.create(
                inference = inference,
                tokens = tokens,
                bindings = listOf(OnnxInputBinding(inputName = "x", variables = variables)),
                outputName = "y",
                elementIndex = 0,
                converter = IntoValue.Identity
            ).valueOrFail()

            assertIs<OnnxError.BindingMismatch>(adapter.evaluate(listOf(Flt64(1.0))).structuredError())
        } finally {
            inference.close()
        }
    }

    @Test
    fun doublePrecisionModelIsSupported() {
        val bytes = model(
            inputs = listOf(doubleValueInfo("x", listOf(1L, 2L))),
            outputs = listOf(doubleValueInfo("y", listOf(1L, 2L))),
            nodes = listOf(node("Gemm", listOf("x", "w", "b"), listOf("y"), name = "gemm")),
            initializers = listOf(
                doubleTensor("w", listOf(2L, 2L), listOf(1.0, 0.0, 0.0, 2.0)),
                doubleTensor("b", listOf(2L), listOf(1.0, 1.0))
            )
        ).toByteArray()
        val (tokens, variables) = tokenTable("x0", "x1")
        val inference = OnnxInference.load(bytes).valueOrFail()
        try {
            assertEquals(OnnxDataType.Double, inference.inputInfos.single().dtype)
            val adapter = OnnxCallbackAdapter.create(
                inference = inference,
                tokens = tokens,
                bindings = listOf(OnnxInputBinding(inputName = "x", variables = variables)),
                outputName = "y",
                elementIndex = 1,
                converter = IntoValue.Identity
            ).valueOrFail()
            // y[1] = 2*x1 + 1
            assertEquals(7.0, adapter.evaluate(listOf(Flt64(0.0), Flt64(3.0))).valueOrFail().toDouble())
        } finally {
            inference.close()
        }
    }
}
