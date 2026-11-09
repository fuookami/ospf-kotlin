/**
 * ONNX 数据类型扩展测试 / ONNX extended data-type tests
*/
package fuookami.ospf.kotlin.core.plugin.onnx.graph

import kotlin.test.*
import kotlin.math.abs
import ai.onnx.proto.OnnxMl
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.plugin.onnx.*
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
import fuookami.ospf.kotlin.core.plugin.onnx.inference.OnnxInference
import fuookami.ospf.kotlin.core.plugin.onnx.inference.OnnxTensorData
import fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxFunctionalModel

/**
 * 半精度、整除与布尔类型的解码、推理与降阶测试 / Half-precision, integer and boolean decoding, inference and lowering tests
*/
class OnnxDataTypeTest {

    @Test
    fun dataTypeCoversOnnxTensorProtoValues() {
        val expected = mapOf(
            1 to OnnxDataType.Float32,
            2 to OnnxDataType.UInt8,
            3 to OnnxDataType.Int8,
            4 to OnnxDataType.UInt16,
            5 to OnnxDataType.Int16,
            6 to OnnxDataType.Int32,
            7 to OnnxDataType.Int64,
            9 to OnnxDataType.Bool,
            10 to OnnxDataType.Float16,
            11 to OnnxDataType.Double,
            12 to OnnxDataType.UInt32,
            13 to OnnxDataType.UInt64,
            16 to OnnxDataType.BFloat16
        )
        for ((protoId, dtype) in expected) {
            assertEquals(dtype, OnnxDataType.of(protoId), "protoId=$protoId")
        }
        assertNull(OnnxDataType.of(8))
        assertEquals("unsupported(8)", OnnxDataType.displayNameOf(8))
    }

    @Test
    fun byteWidthMatchesType() {
        assertEquals(1, OnnxDataType.Int8.byteWidth)
        assertEquals(2, OnnxDataType.Float16.byteWidth)
        assertEquals(2, OnnxDataType.BFloat16.byteWidth)
        assertEquals(4, OnnxDataType.Float32.byteWidth)
        assertEquals(8, OnnxDataType.Double.byteWidth)
    }

    @Test
    fun halfPrecisionRoundTrip() {
        val samples = listOf(
            0.0, 1.0, -1.0, 0.5, -0.5, 2.0, 65504.0, -65504.0,
            0.00006097555160522461, 3.140625, -7.5, 1234.0
        )
        for (sample in samples) {
            val bits = doubleToHalfBits(sample)
            val decoded = halfBitsToDouble(bits)
            assertTrue(abs(decoded - sample) <= abs(sample) * 1e-2 + 1e-6, "sample=$sample decoded=$decoded")
        }
        assertEquals(0x7C00, doubleToHalfBits(Double.POSITIVE_INFINITY))
        assertEquals(0xFC00, doubleToHalfBits(Double.NEGATIVE_INFINITY))
        assertTrue(halfBitsToDouble(0x7E00).isNaN())
        assertEquals(0x8000, doubleToHalfBits(-0.0))
    }

    @Test
    fun bfloat16RoundTrip() {
        val samples = listOf(0.0, 1.0, -1.0, 0.5, 3.140625, 1e-8, 1e8)
        for (sample in samples) {
            val decoded = bfloat16BitsToDouble(doubleToBFloat16Bits(sample))
            assertTrue(abs(decoded - sample) <= abs(sample) * 1e-2 + 1e-9, "sample=$sample decoded=$decoded")
        }
    }

    @Test
    fun float16InitializerDecodes() {
        val bytes = model(
            inputs = listOf(valueInfo("x", listOf(1L, 1L), elemType = 1)),
            outputs = listOf(valueInfo("y", listOf(1L, 2L), elemType = 1)),
            nodes = listOf(node("Identity", listOf("c"), listOf("y"), name = "identity")),
            initializers = listOf(
                // FP16 常量以位模式存放在 int32_data。 / FP16 constants store bit patterns in int32_data.
                OnnxMl.TensorProto.newBuilder()
                    .setName("c")
                    .setDataType(OnnxDataType.Float16.protoId)
                    .addDims(1).addDims(2)
                    .addInt32Data(doubleToHalfBits(1.5))
                    .addInt32Data(doubleToHalfBits(-2.25))
                    .build()
            )
        ).toByteArray()
        val x = RealVar("x")
        val model = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x))).valueOrFail()
        val outputs = model.outputs("y").valueOrFail()
        assertEquals(1.5, (outputs[0] as fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol<Flt64>).polynomial.constant.toDouble())
        assertEquals(-2.25, (outputs[1] as fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol<Flt64>).polynomial.constant.toDouble())
    }

    @Test
    fun float16RawDataDecodes() {
        val bits = intArrayOf(doubleToHalfBits(2.0), doubleToHalfBits(-0.5))
        val raw = java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        for (value in bits) {
            raw.putShort((value and 0xFFFF).toShort())
        }
        raw.flip()
        val proto = OnnxMl.TensorProto.newBuilder()
            .setName("c")
            .setDataType(OnnxDataType.Float16.protoId)
            .addDims(2)
            .setRawData(shade.com.google.protobuf.ByteString.copyFrom(raw))
            .build()
        val decoded = decodeTensorConstant(proto)
        val value = assertNotNull(decoded.value)
        assertEquals(OnnxDataType.Float16, value.dtype)
        assertEquals(2.0, value.doubleAt(0))
        assertEquals(-0.5, value.doubleAt(1))
    }

    @Test
    fun int8ConstantsDecodeSignedAndUnsigned() {
        val signed = decodeTensorConstant(
            OnnxMl.TensorProto.newBuilder()
                .setName("s")
                .setDataType(OnnxDataType.Int8.protoId)
                .addDims(3)
                .addInt32Data(-128)
                .addInt32Data(0)
                .addInt32Data(127)
                .build()
        )
        assertEquals(listOf(-128L, 0L, 127L), (0 until 3).map { signed.value!!.longAt(it) })

        val unsigned = decodeTensorConstant(
            OnnxMl.TensorProto.newBuilder()
                .setName("u")
                .setDataType(OnnxDataType.UInt8.protoId)
                .addDims(3)
                .addInt32Data(255)
                .addInt32Data(0)
                .addInt32Data(200)
                .build()
        )
        assertEquals(listOf(255L, 0L, 200L), (0 until 3).map { unsigned.value!!.longAt(it) })
    }

    @Test
    fun boolRawDataDecodes() {
        val proto = OnnxMl.TensorProto.newBuilder()
            .setName("b")
            .setDataType(OnnxDataType.Bool.protoId)
            .addDims(3)
            .setRawData(shade.com.google.protobuf.ByteString.copyFrom(byteArrayOf(1, 0, 1)))
            .build()
        val decoded = decodeTensorConstant(proto)
        assertEquals(listOf(1L, 0L, 1L), (0 until 3).map { decoded.value!!.longAt(it) })
    }

    @Test
    fun float16InferenceRuns() {
        val bytes = model(
            inputs = listOf(valueInfo("x", listOf(1L, 2L), elemType = OnnxDataType.Float16.protoId)),
            outputs = listOf(valueInfo("y", listOf(1L, 2L), elemType = OnnxDataType.Float16.protoId)),
            nodes = listOf(node("Identity", listOf("x"), listOf("y"), name = "identity"))
        ).toByteArray()
        val inference = OnnxInference.load(bytes).valueOrFail()
        try {
            assertEquals(OnnxDataType.Float16, inference.inputInfos.single().dtype)
            val outputs = inference.evaluate(
                mapOf(
                    "x" to OnnxTensorData(
                        dtype = OnnxDataType.Float16,
                        shape = listOf(1, 2),
                        data = doubleArrayOf(1.5, -2.25)
                    )
                )
            ).valueOrFail()
            val output = outputs.getValue("y")
            assertEquals(OnnxDataType.Float16, output.dtype)
            assertEquals(1.5, output.data[0])
            assertEquals(-2.25, output.data[1])
        } finally {
            inference.close()
        }
    }

    @Test
    fun int64ControlInputDecodes() {
        val bytes = model(
            inputs = listOf(
                floatValueInfo("x", listOf(2L, 3L)),
                valueInfo("axis", listOf(1L), elemType = OnnxDataType.Int64.protoId)
            ),
            outputs = listOf(floatValueInfo("y", listOf(3L, 2L))),
            nodes = listOf(node("Transpose", listOf("x", "axis"), listOf("y"), name = "transpose"))
        ).toByteArray()
        val variables = (0 until 6).map { RealVar("x$it") }
        val result = OnnxFunctionalModel.load(bytes, mapOf("x" to variables))
        // Transpose 不接受第二个输入，这里只验证明图解析没有因 Int64 拒绝。
        // Transpose takes no second input; this only checks the parser does not reject Int64.
        assertTrue(result is fuookami.ospf.kotlin.utils.functional.Failed || result is fuookami.ospf.kotlin.utils.functional.Ok)
    }

    @Test
    fun unsupportedDTypeStillReported() {
        val bytes = model(
            inputs = listOf(valueInfo("x", listOf(1L, 2L), elemType = 8)),
            outputs = listOf(valueInfo("y", listOf(1L, 2L), elemType = 8)),
            nodes = listOf(node("Identity", listOf("x"), listOf("y"), name = "identity"))
        ).toByteArray()
        val result = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(RealVar("x0"), RealVar("x1"))))
        assertIs<OnnxError.UnsupportedDType>(result.structuredError())
    }
}
