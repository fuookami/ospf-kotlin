/**
 * ONNX 图解析测试 / ONNX graph parser tests
*/
package fuookami.ospf.kotlin.core.plugin.onnx.graph

import kotlin.test.*
import ai.onnx.proto.OnnxMl
import shade.com.google.protobuf.ByteString
import fuookami.ospf.kotlin.core.plugin.onnx.*
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError

/**
 * 图解析与常量解码测试 / Graph parsing and constant decoding tests
*/
class OnnxGraphParserTest {

    @Test
    fun graphParsesNodesAndInitializers() {
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(2L, 3L))),
            outputs = listOf(floatValueInfo("y", listOf(2L, 3L))),
            nodes = listOf(
                node("Identity", listOf("x"), listOf("y"), name = "identity")
            ),
            initializers = listOf(floatTensor("w", listOf(2L, 3L), List(6) { 1.0f }))
        ).toByteArray()

        val parsed = parseOnnxGraph(bytes)
        assertIs<OnnxParseResult.Success>(parsed)
        val graph = parsed.graph
        assertEquals(TEST_OPSET, graph.opsetVersion)
        assertEquals(listOf("x"), graph.inputs.map { it.name })
        assertEquals(listOf(2L, 3L), graph.inputs.single().staticShape)
        assertEquals(listOf("y"), graph.outputs.map { it.name })
        assertEquals(1, graph.nodes.size)
        assertEquals("Identity", graph.nodes.single().opType)
        assertEquals(6, graph.initializers.getValue("w").elementCount)
    }

    @Test
    fun initializerBackedInputIsNotAGraphInput() {
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L)), floatValueInfo("w", listOf(1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L))),
            nodes = listOf(node("Add", listOf("x", "w"), listOf("y"))),
            initializers = listOf(floatTensor("w", listOf(1L), listOf(2.0f)))
        ).toByteArray()

        val parsed = parseOnnxGraph(bytes)
        assertIs<OnnxParseResult.Success>(parsed)
        assertEquals(listOf("x"), parsed.graph.inputs.map { it.name })
    }

    @Test
    fun constantNodeIsFoldedIntoInitializers() {
        val constantNode = node(
            opType = "Constant",
            inputs = emptyList(),
            outputs = listOf("c"),
            name = "const"
        ) {
            addAttribute(
                OnnxMl.AttributeProto.newBuilder()
                    .setName("value")
                    .setType(OnnxMl.AttributeProto.AttributeType.TENSOR)
                    .setT(floatTensor("c_value", listOf(1L), listOf(3.0f)))
            )
        }
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L))),
            nodes = listOf(constantNode, node("Add", listOf("x", "c"), listOf("y")))
        ).toByteArray()

        val parsed = parseOnnxGraph(bytes)
        assertIs<OnnxParseResult.Success>(parsed)
        assertEquals(1, parsed.graph.nodes.size)
        assertEquals("Add", parsed.graph.nodes.single().opType)
        assertEquals(3.0, parsed.graph.initializers.getValue("c").doubleAt(0))
    }

    @Test
    fun corruptBytesFailWithLoadFailed() {
        val parsed = parseOnnxGraph(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        assertIs<OnnxParseResult.Failure>(parsed)
        assertIs<OnnxError.LoadFailed>(parsed.error)
    }

    @Test
    fun unsupportedOpsetFails() {
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L))),
            outputs = listOf(floatValueInfo("x", listOf(1L))),
            nodes = listOf(node("Identity", listOf("x"), listOf("x"))),
            opset = 12L
        ).toByteArray()

        val parsed = parseOnnxGraph(bytes)
        assertIs<OnnxParseResult.Failure>(parsed)
        assertIs<OnnxError.UnsupportedOpset>(parsed.error)
        assertEquals(12L, parsed.error.version)
    }

    @Test
    fun customDomainFails() {
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L))),
            outputs = listOf(floatValueInfo("x", listOf(1L))),
            nodes = listOf(node("Identity", listOf("x"), listOf("x"))),
            extraOpsets = listOf("com.example" to 1L)
        ).toByteArray()

        val parsed = parseOnnxGraph(bytes)
        assertIs<OnnxParseResult.Failure>(parsed)
        assertIs<OnnxError.UnsupportedOpset>(parsed.error)
        assertEquals("com.example", parsed.error.domain)
    }

    @Test
    fun unavailableNodeInputFails() {
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L))),
            nodes = listOf(node("Add", listOf("x", "missing"), listOf("y")))
        ).toByteArray()

        val parsed = parseOnnxGraph(bytes)
        assertIs<OnnxParseResult.Failure>(parsed)
        assertIs<OnnxError.InvalidModel>(parsed.error)
    }

    @Test
    fun duplicatedOutputTensorFails() {
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L))),
            nodes = listOf(
                node("Identity", listOf("x"), listOf("y")),
                node("Identity", listOf("x"), listOf("y"))
            )
        ).toByteArray()

        val parsed = parseOnnxGraph(bytes)
        assertIs<OnnxParseResult.Failure>(parsed)
        assertIs<OnnxError.InvalidModel>(parsed.error)
    }

    @Test
    fun unsupportedDataTypeFails() {
        val valueInfo = OnnxMl.ValueInfoProto.newBuilder()
            .setName("x")
            .setType(
                OnnxMl.TypeProto.newBuilder().setTensorType(
                    OnnxMl.TypeProto.Tensor.newBuilder().setElemType(14)
                )
            )
            .build()
        val bytes = model(
            inputs = listOf(valueInfo),
            outputs = listOf(floatValueInfo("x", listOf(1L))),
            nodes = listOf(node("Identity", listOf("x"), listOf("x")))
        ).toByteArray()

        val parsed = parseOnnxGraph(bytes)
        assertIs<OnnxParseResult.Failure>(parsed)
        assertIs<OnnxError.UnsupportedDType>(parsed.error)
    }

    @Test
    fun rawDataTensorIsDecoded() {
        val buffer = java.nio.ByteBuffer.allocate(16).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buffer.putFloat(1.5f)
        buffer.putFloat(-2.5f)
        buffer.putFloat(0.0f)
        buffer.putFloat(4.0f)
        val raw = OnnxMl.TensorProto.newBuilder()
            .setName("w")
            .setDataType(1)
            .addDims(2)
            .addDims(2)
            .setRawData(ByteString.copyFrom(buffer.array()))
            .build()

        val decoded = decodeTensorConstant(raw)
        val constant = decoded.value
        assertNotNull(constant)
        assertEquals(listOf(2, 2), constant.shape)
        assertEquals(listOf(1.5, -2.5, 0.0, 4.0), constant.doubles.toList())
    }

    @Test
    fun int64ControlTensorIsDecoded() {
        val decoded = decodeTensorConstant(longTensor("axes", listOf(2L), listOf(0L, 1L)))
        val constant = decoded.value
        assertNotNull(constant)
        assertEquals(OnnxDataType.Int64, constant.dtype)
        assertEquals(listOf(0L, 1L), constant.longs.toList())
    }

    @Test
    fun unsupportedTensorDataTypeFails() {
        val raw = OnnxMl.TensorProto.newBuilder()
            .setName("w")
            .setDataType(2)
            .addDims(1)
            .build()

        val decoded = decodeTensorConstant(raw)
        assertNull(decoded.value)
        assertNotNull(decoded.detail)
    }
}
