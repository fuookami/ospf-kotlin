/**
 * ONNX 数据类型映射 / ONNX data type mapping
*/
package fuookami.ospf.kotlin.core.plugin.onnx.graph

/**
 * 支持的 ONNX 数据类型 / Supported ONNX data types
 *
 * 浮点类型参与算术表达式，整数类型既可作为控制张量、也可作为整数算术的输入；
 * 所有类型在读入时统一转换为 `Double` 参与表达式代数。 / Floating types take part in arithmetic expressions; integer types serve as control tensors as well as
 * integer arithmetic inputs. Every type is converted to `Double` on read before entering the expression
 * algebra.
 *
 * @property protoId `TensorProto.DataType` 枚举值 / `TensorProto.DataType` enum value
 * @property displayName 可读名称 / Readable name
*/
enum class OnnxDataType(val protoId: Int, val displayName: String) {
    /** 32 位浮点 / 32-bit float */
    Float32(1, "Float32"),

    /** 8 位无符号整数 / 8-bit unsigned integer */
    UInt8(2, "UInt8"),

    /** 8 位有符号整数 / 8-bit signed integer */
    Int8(3, "Int8"),

    /** 16 位无符号整数 / 16-bit unsigned integer */
    UInt16(4, "UInt16"),

    /** 16 位有符号整数 / 16-bit signed integer */
    Int16(5, "Int16"),

    /** 32 位有符号整数 / 32-bit signed integer */
    Int32(6, "Int32"),

    /** 64 位有符号整数 / 64-bit signed integer */
    Int64(7, "Int64"),

    /** 布尔 / Boolean */
    Bool(9, "Bool"),

    /** 16 位浮点 / 16-bit float */
    Float16(10, "Float16"),

    /** 64 位浮点 / 64-bit float */
    Double(11, "Double"),

    /** 32 位无符号整数 / 32-bit unsigned integer */
    UInt32(12, "UInt32"),

    /** 64 位无符号整数 / 64-bit unsigned integer */
    UInt64(13, "UInt64"),

    /** 16 位脑浮点 / 16-bit brain float */
    BFloat16(16, "BFloat16");

    /** 是否为参与算术的浮点类型 / Whether this is an arithmetic floating type */
    val isFloating: Boolean
        get() = when (this) {
            Float32, Float16, Double, BFloat16 -> true
            else -> false
        }

    /** 是否为整数控制类型 / Whether this is an integer control type */
    val isInteger: Boolean
        get() = when (this) {
            UInt8, Int8, UInt16, Int16, Int32, Int64, UInt32, UInt64 -> true
            Bool -> false
            else -> false
        }

    /** 每个元素占用的字节数，用于 `raw_data` 解码 / Bytes per element, used for `raw_data` decoding */
    val byteWidth: Int
        get() = when (this) {
            Int8, UInt8, Bool -> 1
            Float16, BFloat16, Int16, UInt16 -> 2
            Float32, Int32, UInt32 -> 4
            Double, Int64, UInt64 -> 8
        }

    /** 对应的 ONNX Runtime Java 类型名 / Corresponding ONNX Runtime Java type name */
    val ortName: String
        get() = when (this) {
            Float32 -> "FLOAT"
            Float16 -> "FLOAT16"
            BFloat16 -> "BFLOAT16"
            Double -> "DOUBLE"
            Int8 -> "INT8"
            UInt8 -> "UINT8"
            Int16 -> "INT16"
            UInt16 -> "UINT16"
            Int32 -> "INT32"
            UInt32 -> "UINT32"
            Int64 -> "INT64"
            UInt64 -> "UINT64"
            Bool -> "BOOL"
        }

    companion object {
        /**
         * 按 `TensorProto.DataType` 值查找 / Look up by `TensorProto.DataType` value
         *
         * @param protoId 协议值 / protocol value
         * @return 匹配类型，不支持时返回 null / matching type, or null when unsupported
         */
        fun of(protoId: Int): OnnxDataType? {
            return entries.firstOrNull { it.protoId == protoId }
        }

        /**
         * 获取协议值的可读名称 / Get the readable name of a protocol value
         *
         * @param protoId 协议值 / protocol value
         * @return 可读名称 / readable name
         */
        fun displayNameOf(protoId: Int): String {
            return of(protoId)?.displayName ?: "unsupported($protoId)"
        }
    }
}
