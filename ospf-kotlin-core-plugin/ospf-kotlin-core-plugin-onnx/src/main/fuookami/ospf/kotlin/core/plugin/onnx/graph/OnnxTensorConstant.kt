/**
 * ONNX 常量张量解码 / ONNX constant tensor decoding
*/
package fuookami.ospf.kotlin.core.plugin.onnx.graph

import java.nio.ByteBuffer
import java.nio.ByteOrder
import ai.onnx.proto.OnnxMl

/**
 * 解码后的常量张量 / Decoded constant tensor
 *
 * 浮点类型数据写入 [doubles]，整数类型数据写入 [longs]，两者都以行主序展平。 / Floating data is stored in [doubles], integer data in [longs]; both are flattened in
 * row-major order.
 *
 * @property name 张量名（可空） / tensor name (nullable)
 * @property dtype 数据类型 / data type
 * @property shape 静态形状 / static shape
 * @property doubles 浮点数据 / floating data
 * @property longs 整数数据 / integer data
*/
data class OnnxTensorConstant(
    val name: String?,
    val dtype: OnnxDataType,
    val shape: List<Int>,
    val doubles: DoubleArray,
    val longs: LongArray
) {
    /** 元素个数 / Element count */
    val elementCount: Int
        get() = shape.fold(1) { acc, dim -> acc * dim }

    /**
     * 读取第 [index] 个元素为浮点 / Read element [index] as a float
     *
     * @param index 展平索引 / flattened index
     * @return 浮点值 / floating value
     */
    fun doubleAt(index: Int): Double {
        return if (dtype.isFloating) doubles[index] else longs[index].toDouble()
    }

    /**
     * 读取第 [index] 个元素为整数 / Read element [index] as an integer
     *
     * @param index 展平索引 / flattened index
     * @return 整数值 / integer value
     */
    fun longAt(index: Int): Long {
        return if (dtype.isFloating) doubles[index].toLong() else longs[index]
    }
}

/**
 * 张量解码结果 / Tensor decoding result
 *
 * @property value 解码结果，失败时为 null / decoded value, null on failure
 * @property detail 失败原因 / failure reason
*/
internal data class OnnxTensorDecodeResult(val value: OnnxTensorConstant?, val detail: String?)

/**
 * 解码 `TensorProto` 为 [OnnxTensorConstant] / Decode `TensorProto` into [OnnxTensorConstant]
 *
 * 支持 `float_data`/`double_data`/`int32_data`/`int64_data` 与 `raw_data` 两种存储形式。 / Supports both `float_data`/`double_data`/`int32_data`/`int64_data` and `raw_data` storage.
 *
 * @param proto 协议张量 / protocol tensor
 * @return 解码结果 / decoding result
 */
internal fun decodeTensorConstant(proto: OnnxMl.TensorProto): OnnxTensorDecodeResult {
    val name = if (proto.hasName()) proto.name else null
    val dtype = OnnxDataType.of(proto.dataType)
        ?: return OnnxTensorDecodeResult(
            value = null,
            detail = "张量 '${name ?: "<anonymous>"}' 的数据类型 ${OnnxDataType.displayNameOf(proto.dataType)} 不受支持 / " +
                "tensor '${name ?: "<anonymous>"}' uses unsupported dtype ${OnnxDataType.displayNameOf(proto.dataType)}"
        )

    val shape = mutableListOf<Int>()
    for (dim in proto.dimsList) {
        if (dim < 0) {
            return OnnxTensorDecodeResult(
                value = null,
                detail = "张量 '${name ?: "<anonymous>"}' 含负维度 $dim / tensor '${name ?: "<anonymous>"}' has negative dimension $dim"
            )
        }
        if (dim > Int.MAX_VALUE) {
            return OnnxTensorDecodeResult(
                value = null,
                detail = "张量 '${name ?: "<anonymous>"}' 维度 $dim 超出可表示范围 / " +
                    "tensor '${name ?: "<anonymous>"}' dimension $dim exceeds the representable range"
            )
        }
        shape.add(dim.toInt())
    }
    val elementCount = shape.fold(1) { acc, dim -> acc * dim }

    val raw = proto.rawData
    val doubles: DoubleArray
    val longs: LongArray
    if (!raw.isEmpty) {
        val buffer = raw.asReadOnlyByteBuffer().order(ByteOrder.LITTLE_ENDIAN)
        if (buffer.remaining() < elementCount * dtype.byteWidth) {
            return OnnxTensorDecodeResult(
                value = null,
                detail = "张量 '${name ?: "<anonymous>"}' 的 raw_data 长度不足 / raw_data of tensor '${name ?: "<anonymous>"}' is too short"
            )
        }
        if (dtype.isFloating) {
            doubles = DoubleArray(elementCount)
            for (index in 0 until elementCount) {
                doubles[index] = when (dtype) {
                    OnnxDataType.Float32 -> buffer.getFloat().toDouble()
                    OnnxDataType.Double -> buffer.getDouble()
                    OnnxDataType.Float16 -> halfBitsToDouble(buffer.getShort().toInt() and 0xFFFF)
                    OnnxDataType.BFloat16 -> bfloat16BitsToDouble(buffer.getShort().toInt() and 0xFFFF)
                    else -> error("unreachable")
                }
            }
            longs = LongArray(0)
        } else {
            longs = LongArray(elementCount)
            for (index in 0 until elementCount) {
                longs[index] = when (dtype) {
                    OnnxDataType.UInt8, OnnxDataType.Bool -> (buffer.get().toInt() and 0xFF).toLong()
                    OnnxDataType.Int8 -> buffer.get().toLong()
                    OnnxDataType.UInt16 -> (buffer.getShort().toInt() and 0xFFFF).toLong()
                    OnnxDataType.Int16 -> buffer.getShort().toLong()
                    OnnxDataType.Int32 -> buffer.getInt().toLong()
                    OnnxDataType.UInt32 -> (buffer.getInt().toLong() and 0xFFFFFFFFL)
                    OnnxDataType.Int64 -> buffer.getLong()
                    OnnxDataType.UInt64 -> buffer.getLong()
                    else -> error("unreachable")
                }
            }
            doubles = DoubleArray(0)
        }
    } else {
        // 浮点类型优先读 float_data，其余读 int32_data/int64_data；半精度以位模式存放在 int32_data 中。
        // Floating types read float_data first; the rest read int32_data/int64_data; half precision stores bit patterns in int32_data.
        when (dtype) {
            OnnxDataType.Float32 -> {
                val data = proto.floatDataList
                if (data.size != elementCount) {
                    return OnnxTensorDecodeResult(
                        value = null,
                        detail = "张量 '${name ?: "<anonymous>"}' 的 float_data 数量 ${data.size} 与形状元素数 $elementCount 不符 / " +
                            "float_data size ${data.size} of tensor '${name ?: "<anonymous>"}' does not match element count $elementCount"
                    )
                }
                doubles = DoubleArray(elementCount) { data[it].toDouble() }
                longs = LongArray(0)
            }

            OnnxDataType.Double -> {
                val data = proto.doubleDataList
                if (data.size != elementCount) {
                    return OnnxTensorDecodeResult(
                        value = null,
                        detail = "张量 '${name ?: "<anonymous>"}' 的 double_data 数量 ${data.size} 与形状元素数 $elementCount 不符 / " +
                            "double_data size ${data.size} of tensor '${name ?: "<anonymous>"}' does not match element count $elementCount"
                    )
                }
                doubles = DoubleArray(elementCount) { data[it] }
                longs = LongArray(0)
            }

            OnnxDataType.Float16, OnnxDataType.BFloat16 -> {
                val data = proto.int32DataList
                if (data.size != elementCount) {
                    return OnnxTensorDecodeResult(
                        value = null,
                        detail = "张量 '${name ?: "<anonymous>"}' 的 int32_data 数量 ${data.size} 与形状元素数 $elementCount 不符 / " +
                            "int32_data size ${data.size} of tensor '${name ?: "<anonymous>"}' does not match element count $elementCount"
                    )
                }
                doubles = DoubleArray(elementCount) { index ->
                    val bits = data[index] and 0xFFFF
                    if (dtype == OnnxDataType.Float16) {
                        halfBitsToDouble(bits)
                    } else {
                        bfloat16BitsToDouble(bits)
                    }
                }
                longs = LongArray(0)
            }

            OnnxDataType.Int64, OnnxDataType.UInt64 -> {
                val data = proto.int64DataList
                if (data.size != elementCount) {
                    return OnnxTensorDecodeResult(
                        value = null,
                        detail = "张量 '${name ?: "<anonymous>"}' 的 int64_data 数量 ${data.size} 与形状元素数 $elementCount 不符 / " +
                            "int64_data size ${data.size} of tensor '${name ?: "<anonymous>"}' does not match element count $elementCount"
                    )
                }
                longs = LongArray(elementCount) { data[it] }
                doubles = DoubleArray(0)
            }

            else -> {
                val data = proto.int32DataList
                if (data.size != elementCount) {
                    return OnnxTensorDecodeResult(
                        value = null,
                        detail = "张量 '${name ?: "<anonymous>"}' 的 int32_data 数量 ${data.size} 与形状元素数 $elementCount 不符 / " +
                            "int32_data size ${data.size} of tensor '${name ?: "<anonymous>"}' does not match element count $elementCount"
                    )
                }
                longs = LongArray(elementCount) { index ->
                    when (dtype) {
                        OnnxDataType.UInt8, OnnxDataType.Bool -> (data[index] and 0xFF).toLong()
                        OnnxDataType.Int8 -> (data[index].toByte()).toLong()
                        OnnxDataType.UInt16 -> (data[index] and 0xFFFF).toLong()
                        OnnxDataType.Int16 -> (data[index].toShort()).toLong()
                        OnnxDataType.UInt32 -> (data[index].toLong() and 0xFFFFFFFFL)
                        else -> data[index].toLong()
                    }
                }
                doubles = DoubleArray(0)
            }
        }
    }

    return OnnxTensorDecodeResult(
        value = OnnxTensorConstant(
            name = name,
            dtype = dtype,
            shape = shape,
            doubles = doubles,
            longs = longs
        ),
        detail = null
    )
}
