/**
 * ONNX 动态形状具体化测试 / ONNX dynamic-shape materialization tests
*/
package fuookami.ospf.kotlin.core.plugin.onnx.model

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.ManualTokenTable
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.plugin.onnx.*
import fuookami.ospf.kotlin.core.plugin.onnx.callback.OnnxCallbackAdapter
import fuookami.ospf.kotlin.core.plugin.onnx.callback.OnnxInputBinding
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
import fuookami.ospf.kotlin.core.plugin.onnx.inference.OnnxInference

/**
 * 动态维度经 inputShapes 覆盖后可参与推理与降阶 / Dynamic dimensions become usable for inference and lowering once inputShapes overrides them
*/
class OnnxDynamicShapeTest {

    /** 动态批量维的恒等模型 / Identity model with a dynamic batch dimension */
    private fun dynamicBytes(): ByteArray {
        return model(
            inputs = listOf(floatValueInfo("x", listOf(null, 2L))),
            outputs = listOf(floatValueInfo("y", listOf(null, 2L))),
            nodes = listOf(node("Identity", listOf("x"), listOf("y"), name = "identity"))
        ).toByteArray()
    }

    @Test
    fun inferenceExposesDynamicDimensionsAsNull() {
        val inference = OnnxInference.load(dynamicBytes()).valueOrFail()
        try {
            assertEquals(listOf(null, 2L), inference.inputInfos.single().shape)
            assertNull(inference.inputInfos.single().elementCount)
        } finally {
            inference.close()
        }
    }

    @Test
    fun inferenceRunsWithConcreteShape() {
        val inference = OnnxInference.load(dynamicBytes()).valueOrFail()
        try {
            val outputs = inference.evaluate(
                mapOf(
                    "x" to fuookami.ospf.kotlin.core.plugin.onnx.inference.OnnxTensorData(
                        dtype = fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxDataType.Float32,
                        shape = listOf(3, 2),
                        data = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
                    )
                )
            ).valueOrFail()
            assertEquals(listOf(3, 2), outputs.getValue("y").shape)
            assertEquals(listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), outputs.getValue("y").data.toList())
        } finally {
            inference.close()
        }
    }

    @Test
    fun loweringMaterializesDynamicInput() {
        val variables = (0 until 6).map { RealVar("x$it") }
        val model = OnnxFunctionalModel.load(
            source = dynamicBytes(),
            inputBindings = mapOf("x" to variables),
            options = OnnxImportOptions(inputShapes = mapOf("x" to listOf(3L, 2L)))
        ).valueOrFail()

        assertEquals(listOf(3L, 2L), model.inputInfos.single().shape)
        assertEquals(6, model.outputs("y").valueOrFail().size)
        for (index in 0 until 6) {
            assertEquals("x$index", model.linearOutput("y", index).valueOrFail().polynomial.monomials.single().symbol.name)
        }
    }

    @Test
    fun loweringWithoutOverrideFailsWithDynamicShape() {
        val variables = (0 until 6).map { RealVar("x$it") }
        val result = OnnxFunctionalModel.load(dynamicBytes(), mapOf("x" to variables))
        assertIs<OnnxError.DynamicShape>(result.structuredError())
    }

    @Test
    fun shapeOverrideConflictingWithStaticDimensionFails() {
        val variables = (0 until 6).map { RealVar("x$it") }
        val result = OnnxFunctionalModel.load(
            source = dynamicBytes(),
            inputBindings = mapOf("x" to variables),
            options = OnnxImportOptions(inputShapes = mapOf("x" to listOf(3L, 4L)))
        )
        assertIs<OnnxError.ShapeMismatch>(result.structuredError())
    }

    @Test
    fun shapeOverrideWithWrongRankFails() {
        val variables = (0 until 6).map { RealVar("x$it") }
        val result = OnnxFunctionalModel.load(
            source = dynamicBytes(),
            inputBindings = mapOf("x" to variables),
            options = OnnxImportOptions(inputShapes = mapOf("x" to listOf(6L)))
        )
        assertIs<OnnxError.ShapeMismatch>(result.structuredError())
    }

    @Test
    fun shapeOverrideForUnknownInputFails() {
        val variables = (0 until 6).map { RealVar("x$it") }
        val result = OnnxFunctionalModel.load(
            source = dynamicBytes(),
            inputBindings = mapOf("x" to variables),
            options = OnnxImportOptions(inputShapes = mapOf("nope" to listOf(3L, 2L)))
        )
        assertIs<OnnxError.BindingMismatch>(result.structuredError())
    }

    @Test
    fun optionsRejectNonPositiveOverride() {
        assertFailsWith<IllegalArgumentException> {
            OnnxImportOptions(inputShapes = mapOf("x" to listOf(0L, 2L)))
        }
    }

    @Test
    fun boundsRejectInvertedOrNonFiniteRange() {
        // 上下界颠倒必须拒绝 / An inverted range must be rejected
        assertFailsWith<IllegalArgumentException> {
            OnnxBounds(lower = Flt64(3.0), upper = Flt64(-3.0))
        }
        // 无穷与 NaN 都不是有效的有限范围 / Infinity and NaN are not valid finite bounds
        assertFailsWith<IllegalArgumentException> {
            OnnxBounds(lower = Flt64.negativeInfinity, upper = Flt64(1.0))
        }
        assertFailsWith<IllegalArgumentException> {
            OnnxBounds(lower = Flt64.zero, upper = Flt64.nan)
        }
        // 合法范围照常构造 / A valid range constructs normally
        val bounds = OnnxBounds(lower = Flt64(-3.0), upper = Flt64(3.0))
        assertEquals(-3.0, bounds.lower.toDouble())
        assertEquals(3.0, bounds.upper.toDouble())
    }

    @Test
    fun callbackRunsWithConcreteShapeOverride() {
        val table = ManualTokenTable<Flt64>(category = Linear, checkTokenExists = false)
        val variables = (0 until 6).map { RealVar("x$it") }
        for (variable in variables) {
            table.add(variable)
        }
        val inference = OnnxInference.load(dynamicBytes()).valueOrFail()
        try {
            // 未给覆盖时构造失败。 / Construction fails without an override.
            val missing = OnnxCallbackAdapter.create(
                inference = inference,
                tokens = table,
                bindings = listOf(OnnxInputBinding(inputName = "x", variables = variables)),
                outputName = "y",
                elementIndex = 0,
                converter = IntoValue.Identity
            )
            assertIs<OnnxError.DynamicShape>(missing.structuredError())

            // 给了覆盖后可用。 / Usable once the override is given.
            val adapter = OnnxCallbackAdapter.create(
                inference = inference,
                tokens = table,
                bindings = listOf(OnnxInputBinding(inputName = "x", variables = variables)),
                outputName = "y",
                elementIndex = 4,
                converter = IntoValue.Identity,
                inputShapes = mapOf("x" to listOf(3L, 2L))
            ).valueOrFail()
            val solution = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).map { Flt64(it) }
            assertEquals(5.0, adapter.evaluate(solution).valueOrFail().toDouble())
        } finally {
            inference.close()
        }
    }
}
