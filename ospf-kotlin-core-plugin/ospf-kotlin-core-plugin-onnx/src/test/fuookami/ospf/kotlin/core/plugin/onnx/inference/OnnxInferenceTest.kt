/**
 * ONNX 推理测试 / ONNX inference tests
*/
package fuookami.ospf.kotlin.core.plugin.onnx.inference

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.*
import fuookami.ospf.kotlin.core.plugin.onnx.*
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxDataType

/**
 * 推理句柄的加载、元信息、求值与生命周期测试 / Loading, metadata, evaluation and lifecycle tests of the inference handle
*/
class OnnxInferenceTest {

    /** 单层线性模型 y = x * w + b / Single-layer linear model y = x * w + b */
    private fun linearModelBytes(dtype: OnnxDataType): ByteArray {
        val valueInfo = if (dtype == OnnxDataType.Double) {
            doubleValueInfo("x", listOf(2L, 2L))
        } else {
            floatValueInfo("x", listOf(2L, 2L))
        }
        val weight = if (dtype == OnnxDataType.Double) {
            doubleTensor("w", listOf(2L, 2L), listOf(1.0, 2.0, 3.0, 4.0))
        } else {
            floatTensor("w", listOf(2L, 2L), listOf(1.0f, 2.0f, 3.0f, 4.0f))
        }
        val bias = if (dtype == OnnxDataType.Double) {
            doubleTensor("b", listOf(2L), listOf(0.5, -0.5))
        } else {
            floatTensor("b", listOf(2L), listOf(0.5f, -0.5f))
        }
        return model(
            inputs = listOf(valueInfo),
            outputs = listOf(valueInfo.renamed("y")),
            nodes = listOf(node("Gemm", listOf("x", "w", "b"), listOf("y"), name = "gemm")),
            initializers = listOf(weight, bias)
        ).toByteArray()
    }

    @Test
    fun float32InferenceFlattensRowMajor() {
        val handle = OnnxInference.load(linearModelBytes(OnnxDataType.Float32)).valueOrFail()
        try {
            assertEquals(listOf("x"), handle.inputInfos.map { info -> info.name })
            assertEquals(listOf("y"), handle.outputInfos.map { info -> info.name })
            assertEquals(listOf(2L, 2L), handle.inputInfos.single().shape)

            val outputs = handle.evaluate(
                mapOf(
                    "x" to OnnxTensorData(
                        dtype = OnnxDataType.Float32,
                        shape = listOf(2, 2),
                        data = doubleArrayOf(1.0, 0.0, 0.0, 1.0)
                    )
                )
            ).valueOrFail()
            val y = outputs.getValue("y")
            assertEquals(listOf(2, 2), y.shape)
            assertEquals(OnnxDataType.Float32, y.dtype)
            // X 为单位阵，故 X·W = W，再加偏置 [0.5, -0.5] 广播：[[1.5,1.5],[3.5,3.5]]
            // X is the identity matrix, so X·W = W; adding bias [0.5, -0.5] broadcast gives [[1.5,1.5],[3.5,3.5]]
            assertEquals(listOf(1.5, 1.5, 3.5, 3.5), y.data.toList().map { round(it) })
        } finally {
            handle.close()
        }
    }

    @Test
    fun float64InferenceIsSupported() {
        val handle = OnnxInference.load(linearModelBytes(OnnxDataType.Double)).valueOrFail()
        try {
            assertEquals(OnnxDataType.Double, handle.inputInfos.single().dtype)
            val outputs = handle.evaluate(
                mapOf(
                    "x" to OnnxTensorData(
                        dtype = OnnxDataType.Double,
                        shape = listOf(2, 2),
                        data = doubleArrayOf(2.0, 0.0, 0.0, 2.0)
                    )
                )
            ).valueOrFail()
            val y = outputs.getValue("y")
            assertEquals(OnnxDataType.Double, y.dtype)
            assertEquals(listOf(2.5, 3.5, 6.5, 7.5), y.data.toList().map { round(it) })
        } finally {
            handle.close()
        }
    }

    @Test
    fun loadFromPathByteArrayAndStream() {
        val bytes = linearModelBytes(OnnxDataType.Float32)
        val path = Files.createTempFile("ospf-onnx-", ".onnx")
        try {
            Files.write(path, bytes)
            OnnxInference.load(path).valueOrFail().close()
            OnnxInference.load(bytes).valueOrFail().close()

            val stream = ByteArrayInputStream(bytes)
            OnnxInference.load(stream).valueOrFail().close()
            // 调用方持有的流不被关闭。 / The caller-owned stream stays open.
            assertTrue(stream.read() == -1)
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun missingFileFails() {
        val result = OnnxInference.load(Paths.get("does-not-exist.onnx"))
        assertIs<OnnxError.LoadFailed>(result.structuredError())
    }

    @Test
    fun emptyBytesFail() {
        assertIs<OnnxError.LoadFailed>(OnnxInference.load(ByteArray(0)).structuredError())
    }

    @Test
    fun corruptBytesFail() {
        assertIs<OnnxError.LoadFailed>(OnnxInference.load(byteArrayOf(9, 9, 9, 9)).structuredError())
    }

    @Test
    fun closedHandleReportsResourceClosed() {
        val handle = OnnxInference.load(linearModelBytes(OnnxDataType.Float32)).valueOrFail()
        handle.close()
        assertTrue(handle.isClosed)
        assertIs<OnnxError.ResourceClosed>(handle.evaluate(emptyMap()).structuredError())
        // 重复关闭是幂等的。 / Repeated close is idempotent.
        handle.close()
    }

    @Test
    fun unknownInputNameFails() {
        val handle = OnnxInference.load(linearModelBytes(OnnxDataType.Float32)).valueOrFail()
        try {
            val result = handle.evaluate(
                mapOf("not_x" to OnnxTensorData(OnnxDataType.Float32, listOf(2, 2), DoubleArray(4)))
            )
            assertIs<OnnxError.BindingMismatch>(result.structuredError())
        } finally {
            handle.close()
        }
    }

    @Test
    fun inconsistentShapeFails() {
        val handle = OnnxInference.load(linearModelBytes(OnnxDataType.Float32)).valueOrFail()
        try {
            val result = handle.evaluate(
                mapOf("x" to OnnxTensorData(OnnxDataType.Float32, listOf(2, 3), DoubleArray(6)))
            )
            assertIs<OnnxError.BindingMismatch>(result.structuredError())
        } finally {
            handle.close()
        }
    }

    @Test
    fun concurrentEvaluationIsConsistent() {
        val handle = OnnxInference.load(linearModelBytes(OnnxDataType.Float32)).valueOrFail()
        try {
            val expected = listOf(1.5, 1.5, 3.5, 3.5)
            val failures = java.util.concurrent.ConcurrentLinkedQueue<Throwable>()
            val threads = (1..8).map {
                Thread {
                    try {
                        repeat(8) {
                            val outputs = handle.evaluate(
                                mapOf(
                                    "x" to OnnxTensorData(
                                        OnnxDataType.Float32,
                                        listOf(2, 2),
                                        doubleArrayOf(1.0, 0.0, 0.0, 1.0)
                                    )
                                )
                            ).valueOrFail()
                            assertEquals(expected, outputs.getValue("y").data.toList().map { round(it) })
                        }
                    } catch (e: Throwable) {
                        failures.add(e)
                    }
                }
            }
            threads.forEach { it.start() }
            threads.forEach { it.join() }
            assertTrue(failures.isEmpty(), "concurrent evaluation failures: ${failures.firstOrNull()}")
        } finally {
            handle.close()
        }
    }

    /** 归一到 1e-5，抹掉 Float32 表示误差 / Normalize to 1e-5, hiding Float32 representation error */
    private fun round(value: Double): Double {
        return Math.round(value * 100000.0) / 100000.0
    }
}
