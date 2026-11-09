/**
 * ONNX Runtime 推理封装 / ONNX Runtime inference wrapper
*/
package fuookami.ospf.kotlin.core.plugin.onnx.inference

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.DoubleBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer
import java.nio.ShortBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import ai.onnxruntime.NodeInfo
import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OnnxValue
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxDataType
import fuookami.ospf.kotlin.core.plugin.onnx.graph.bfloat16BitsToDouble
import fuookami.ospf.kotlin.core.plugin.onnx.graph.doubleToBFloat16Bits
import fuookami.ospf.kotlin.core.plugin.onnx.graph.doubleToHalfBits
import fuookami.ospf.kotlin.core.plugin.onnx.graph.halfBitsToDouble

/**
 * ONNX Runtime 推理句柄 / ONNX Runtime inference handle
 *
 * 持有 `OrtSession`；`OrtEnvironment` 是 JVM 级单例，句柄不会关闭它。 / Holds the `OrtSession`; the `OrtEnvironment` is a JVM-level singleton and is never closed here.
 *
 * 每次推理创建的 tensor 与 result 都在调用结束前释放；捕获的运行时异常统一转换为
 * [OnnxError.InferenceFailed]。 / Tensors and results created per inference are released before the call returns; captured
 * runtime exceptions are uniformly converted to [OnnxError.InferenceFailed].
 *
 * 实例在关闭前可被多线程并发推理。 / An instance may be used for concurrent inference until it is closed.
*/
class OnnxInference private constructor(
    private val environment: OrtEnvironment,
    private val session: OrtSession
) : AutoCloseable {

    private val closed = AtomicBoolean(false)

    /** 输入元信息 / Input metadata */
    val inputInfos: List<OnnxTensorInfo> = readInfos(session.inputInfo)

    /** 输出元信息 / Output metadata */
    val outputInfos: List<OnnxTensorInfo> = readInfos(session.outputInfo)

    /** 是否已关闭 / Whether the handle is closed */
    val isClosed: Boolean
        get() = closed.get()

    /**
     * 按名称推理 / Run inference by tensor names
     *
     * @param inputs 输入名到张量数据的映射 / map from input name to tensor data
     * @return 输出名到张量数据的映射 / map from output name to tensor data
     */
    fun evaluate(inputs: Map<String, OnnxTensorData>): Ret<Map<String, OnnxTensorData>> {
        if (closed.get()) {
            return failed(OnnxError.ResourceClosed("inference handle is closed").toError())
        }
        val expectedNames = inputInfos.map { it.name }.toSet()
        val missing = expectedNames - inputs.keys
        if (missing.isNotEmpty()) {
            return failed(
                OnnxError.BindingMismatch("缺少输入 ${missing.sorted()} / missing inputs ${missing.sorted()}").toError()
            )
        }
        val unexpected = inputs.keys - expectedNames
        if (unexpected.isNotEmpty()) {
            return failed(
                OnnxError.BindingMismatch("多余的输入 ${unexpected.sorted()} / unexpected inputs ${unexpected.sorted()}").toError()
            )
        }

        val tensors = LinkedHashMap<String, OnnxTensor>()
        return try {
            for (info in inputInfos) {
                val data = inputs.getValue(info.name)
                if (info.dtype.isFloating && !data.dtype.isFloating) {
                    return failed(
                        OnnxError.BindingMismatch(
                            detail = "输入 '${info.name}' 需要 ${info.dtype.displayName}，实际为 ${data.dtype.displayName} / " +
                                "input '${info.name}' expects ${info.dtype.displayName}, got ${data.dtype.displayName}",
                            tensorName = info.name
                        ).toError()
                    )
                }
                if (!data.isConsistent()) {
                    return failed(
                        OnnxError.BindingMismatch(
                            detail = "输入 '${info.name}' 的数据长度 ${data.data.size} 与形状 ${data.shape} 不符 / " +
                                "data length ${data.data.size} of input '${info.name}' does not match shape ${data.shape}",
                            tensorName = info.name
                        ).toError()
                    )
                }
                val expectedShape = info.elementCount
                if (expectedShape != null && expectedShape != data.elementCount) {
                    return failed(
                        OnnxError.BindingMismatch(
                            detail = "输入 '${info.name}' 的元素数 ${data.elementCount} 与模型声明 $expectedShape 不符 / " +
                                "element count ${data.elementCount} of input '${info.name}' does not match declared $expectedShape",
                            tensorName = info.name
                        ).toError()
                    )
                }
                tensors[info.name] = createTensor(info.dtype, data)
            }

            val result = session.run(tensors)
            try {
                val outputs = LinkedHashMap<String, OnnxTensorData>()
                for (info in outputInfos) {
                    val value = result.get(info.name).orElse(null)
                        ?: return failed(
                            OnnxError.InferenceFailed("模型未返回输出 '${info.name}' / model did not return output '${info.name}'").toError()
                        )
                    outputs[info.name] = readTensor(value)
                }
                ok(outputs)
            } finally {
                result.close()
            }
        } catch (e: OrtException) {
            failed(OnnxError.InferenceFailed(e.message ?: "OrtException").toError())
        } catch (e: Exception) {
            failed(OnnxError.InferenceFailed(e.message ?: e::class.simpleName ?: "unknown error").toError())
        } finally {
            for (tensor in tensors.values) {
                try {
                    tensor.close()
                } catch (_: Exception) {
                    // 释放失败不影响结果返回。 / Release failures do not affect the returned result.
                }
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                session.close()
            } catch (_: Exception) {
                // 关闭异常不再上抛，符合项目不抛异常的约定。 / Close failures are not rethrown, per the project's no-throw convention.
            }
        }
    }

    /** 按模型声明创建张量 / Create a tensor following the declared dtype */
    private fun createTensor(dtype: OnnxDataType, data: OnnxTensorData): OnnxTensor {
        val shape = data.shape.map { it.toLong() }.toLongArray()
        return when (dtype) {
            OnnxDataType.Double -> {
                val buffer = DoubleBuffer.allocate(data.data.size)
                buffer.put(data.data)
                buffer.flip()
                OnnxTensor.createTensor(environment, buffer, shape)
            }

            OnnxDataType.Float32 -> {
                val buffer = FloatBuffer.allocate(data.data.size)
                for (value in data.data) {
                    buffer.put(value.toFloat())
                }
                buffer.flip()
                OnnxTensor.createTensor(environment, buffer, shape)
            }

            // 半精度必须按短整型缓冲区承载位模式，并显式声明类型。 / Half precision carries bit patterns in a short buffer and must declare the type explicitly.
            OnnxDataType.Float16 -> {
                val buffer = ShortBuffer.allocate(data.data.size)
                for (value in data.data) {
                    buffer.put((doubleToHalfBits(value) and 0xFFFF).toShort())
                }
                buffer.flip()
                OnnxTensor.createTensor(environment, buffer, shape, OnnxJavaType.FLOAT16)
            }

            OnnxDataType.BFloat16 -> {
                val buffer = ShortBuffer.allocate(data.data.size)
                for (value in data.data) {
                    buffer.put((doubleToBFloat16Bits(value) and 0xFFFF).toShort())
                }
                buffer.flip()
                OnnxTensor.createTensor(environment, buffer, shape, OnnxJavaType.BFLOAT16)
            }

            OnnxDataType.Int64, OnnxDataType.UInt64 -> {
                val buffer = LongBuffer.allocate(data.data.size)
                for (value in data.data) {
                    buffer.put(value.toLong())
                }
                buffer.flip()
                OnnxTensor.createTensor(environment, buffer, shape)
            }

            OnnxDataType.Int32, OnnxDataType.UInt32 -> {
                val buffer = IntBuffer.allocate(data.data.size)
                for (value in data.data) {
                    buffer.put(value.toInt())
                }
                buffer.flip()
                OnnxTensor.createTensor(environment, buffer, shape)
            }

            OnnxDataType.Int16, OnnxDataType.UInt16 -> {
                val buffer = ShortBuffer.allocate(data.data.size)
                for (value in data.data) {
                    buffer.put(value.toInt().toShort())
                }
                buffer.flip()
                OnnxTensor.createTensor(environment, buffer, shape)
            }

            OnnxDataType.Int8, OnnxDataType.UInt8 -> {
                val buffer = ByteBuffer.allocateDirect(data.data.size)
                for (value in data.data) {
                    buffer.put(value.toInt().toByte())
                }
                buffer.flip()
                OnnxTensor.createTensor(environment, buffer, shape)
            }

            OnnxDataType.Bool -> {
                val buffer = ByteBuffer.allocateDirect(data.data.size)
                for (value in data.data) {
                    buffer.put(if (value != 0.0) 1 else 0)
                }
                buffer.flip()
                OnnxTensor.createTensor(environment, buffer, shape)
            }
        }
    }

    /** 把输出张量读成行主序 [OnnxTensorData] / Read an output tensor into row-major [OnnxTensorData] */
    private fun readTensor(value: OnnxValue): OnnxTensorData {
        val tensor = value as OnnxTensor
        val info = tensor.info as TensorInfo
        val shape = info.shape.map { it.toInt() }
        return when (info.type) {
            OnnxJavaType.DOUBLE -> {
                val buffer = tensor.doubleBuffer
                OnnxTensorData(
                    dtype = OnnxDataType.Double,
                    shape = shape,
                    data = DoubleArray(buffer.remaining()).also { buffer.get(it) }
                )
            }

            OnnxJavaType.FLOAT -> {
                val buffer = tensor.floatBuffer
                val floats = FloatArray(buffer.remaining()).also { buffer.get(it) }
                OnnxTensorData(
                    dtype = OnnxDataType.Float32,
                    shape = shape,
                    data = DoubleArray(floats.size) { floats[it].toDouble() }
                )
            }

            OnnxJavaType.FLOAT16 -> {
                val buffer = tensor.shortBuffer
                val shorts = ShortArray(buffer.remaining()).also { buffer.get(it) }
                OnnxTensorData(
                    dtype = OnnxDataType.Float16,
                    shape = shape,
                    data = DoubleArray(shorts.size) { halfBitsToDouble(shorts[it].toInt() and 0xFFFF) }
                )
            }

            OnnxJavaType.BFLOAT16 -> {
                val buffer = tensor.shortBuffer
                val shorts = ShortArray(buffer.remaining()).also { buffer.get(it) }
                OnnxTensorData(
                    dtype = OnnxDataType.BFloat16,
                    shape = shape,
                    data = DoubleArray(shorts.size) { bfloat16BitsToDouble(shorts[it].toInt() and 0xFFFF) }
                )
            }

            OnnxJavaType.INT64 -> {
                val buffer = tensor.longBuffer
                val longs = LongArray(buffer.remaining()).also { buffer.get(it) }
                OnnxTensorData(
                    dtype = OnnxDataType.Int64,
                    shape = shape,
                    data = DoubleArray(longs.size) { longs[it].toDouble() }
                )
            }

            OnnxJavaType.INT32 -> {
                val buffer = tensor.intBuffer
                val ints = IntArray(buffer.remaining()).also { buffer.get(it) }
                OnnxTensorData(
                    dtype = OnnxDataType.Int32,
                    shape = shape,
                    data = DoubleArray(ints.size) { ints[it].toDouble() }
                )
            }

            OnnxJavaType.INT16 -> {
                val buffer = tensor.shortBuffer
                val shorts = ShortArray(buffer.remaining()).also { buffer.get(it) }
                OnnxTensorData(
                    dtype = OnnxDataType.Int16,
                    shape = shape,
                    data = DoubleArray(shorts.size) { shorts[it].toDouble() }
                )
            }

            OnnxJavaType.INT8 -> {
                val buffer = tensor.byteBuffer
                OnnxTensorData(
                    dtype = OnnxDataType.Int8,
                    shape = shape,
                    data = DoubleArray(buffer.remaining()) { buffer.get().toDouble() }
                )
            }

            OnnxJavaType.UINT8 -> {
                val buffer = tensor.byteBuffer
                OnnxTensorData(
                    dtype = OnnxDataType.UInt8,
                    shape = shape,
                    data = DoubleArray(buffer.remaining()) { (buffer.get().toInt() and 0xFF).toDouble() }
                )
            }

            OnnxJavaType.BOOL -> {
                val buffer = tensor.byteBuffer
                OnnxTensorData(
                    dtype = OnnxDataType.Bool,
                    shape = shape,
                    data = DoubleArray(buffer.remaining()) { if (buffer.get().toInt() != 0) 1.0 else 0.0 }
                )
            }

            else -> {
                // 未知 Java 类型统一按 32 位浮点读取，与旧行为保持一致。
                // Unknown Java types fall back to 32-bit float reading, matching previous behaviour.
                val buffer = tensor.floatBuffer
                val floats = FloatArray(buffer.remaining()).also { buffer.get(it) }
                OnnxTensorData(
                    dtype = OnnxDataType.Float32,
                    shape = shape,
                    data = DoubleArray(floats.size) { floats[it].toDouble() }
                )
            }
        }
    }

    companion object {

        /**
         * 从文件加载 / Load from a file
         *
         * @param path 模型路径 / model path
         * @return 推理句柄 / inference handle
         */
        fun load(path: Path): Ret<OnnxInference> {
            if (!Files.isRegularFile(path)) {
                return failed(OnnxError.LoadFailed("not a regular file: $path").toError())
            }
            return create {
                val options = OrtSession.SessionOptions()
                try {
                    val session = environment().createSession(path.toString(), options)
                    OnnxInference(environment(), session)
                } finally {
                    options.close()
                }
            }
        }

        /**
         * 从字节数组加载 / Load from a byte array
         *
         * @param bytes 模型字节 / model bytes
         * @return 推理句柄 / inference handle
         */
        fun load(bytes: ByteArray): Ret<OnnxInference> {
            if (bytes.isEmpty()) {
                return failed(OnnxError.LoadFailed("empty model bytes").toError())
            }
            return create {
                val options = OrtSession.SessionOptions()
                try {
                    // ORT 要求直接缓冲区承载模型字节。 / ORT requires a direct buffer to carry the model bytes.
                    val buffer = java.nio.ByteBuffer.allocateDirect(bytes.size)
                    buffer.put(bytes)
                    buffer.flip()
                    val session = environment().createSession(buffer, options)
                    OnnxInference(environment(), session)
                } finally {
                    options.close()
                }
            }
        }

        /**
         * 从输入流加载 / Load from an input stream
         *
         * 流会被完整读取，但不会关闭；关闭责任属于调用方。 / The stream is fully read but never closed; closing is the caller's responsibility.
         *
         * @param stream 输入流 / input stream
         * @return 推理句柄 / inference handle
         */
        fun load(stream: InputStream): Ret<OnnxInference> {
            val bytes = try {
                stream.readBytes()
            } catch (e: Exception) {
                return failed(
                    OnnxError.LoadFailed("failed to read stream: ${e.message ?: e::class.simpleName}").toError()
                )
            }
            return load(bytes)
        }

        /** JVM 级共享环境 / JVM-level shared environment */
        private fun environment(): OrtEnvironment {
            return OrtEnvironment.getEnvironment()
        }

        /** 统一的加载边界，捕获 ORT 异常 / Unified loading boundary capturing ORT exceptions */
        private inline fun create(block: () -> OnnxInference): Ret<OnnxInference> {
            return try {
                ok(block())
            } catch (e: OrtException) {
                failed(OnnxError.LoadFailed(e.message ?: "OrtException").toError())
            } catch (e: Exception) {
                failed(
                    OnnxError.LoadFailed(e.message ?: e::class.simpleName ?: "unknown error").toError()
                )
            }
        }

        /** 读取会话输入输出元信息 / Read session input/output metadata */
        private fun readInfos(infos: Map<String, NodeInfo>): List<OnnxTensorInfo> {
            return infos.map { (name, nodeInfo) ->
                val tensorInfo = nodeInfo.info as? TensorInfo
                OnnxTensorInfo(
                    name = name,
                    dtype = when (tensorInfo?.type) {
                        OnnxJavaType.FLOAT -> OnnxDataType.Float32
                        OnnxJavaType.DOUBLE -> OnnxDataType.Double
                        OnnxJavaType.FLOAT16 -> OnnxDataType.Float16
                        OnnxJavaType.BFLOAT16 -> OnnxDataType.BFloat16
                        OnnxJavaType.INT64 -> OnnxDataType.Int64
                        OnnxJavaType.INT32 -> OnnxDataType.Int32
                        OnnxJavaType.INT16 -> OnnxDataType.Int16
                        OnnxJavaType.INT8 -> OnnxDataType.Int8
                        OnnxJavaType.UINT8 -> OnnxDataType.UInt8
                        OnnxJavaType.BOOL -> OnnxDataType.Bool
                        // ORT 没有暴露无符号 32/64 位类型；统一按有符号同宽类型处理。
                        // ORT exposes no unsigned 32/64-bit types; the signed type of the same width is used.
                        else -> OnnxDataType.Int64
                    },
                    // ORT 用 -1 表示动态维度。 / ORT uses -1 for dynamic dimensions.
                    shape = tensorInfo?.shape?.map { if (it < 0) null else it }?.toList() ?: emptyList()
                )
            }
        }
    }
}
