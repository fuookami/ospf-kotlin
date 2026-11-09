/**
 * ONNX 张量元信息与数据 / ONNX tensor metadata and data
*/
package fuookami.ospf.kotlin.core.plugin.onnx.inference

import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxDataType

/**
 * ONNX 张量元信息 / ONNX tensor metadata
 *
 * @property name 张量名 / tensor name
 * @property dtype 数据类型 / data type
 * @property shape 形状，动态维度为 null 元素 / shape, with null entries for dynamic dimensions
*/
data class OnnxTensorInfo(
    val name: String,
    val dtype: OnnxDataType,
    val shape: List<Long?>
) {
    /** 元素个数，动态维度存在时返回 null / element count, or null when a dynamic dimension exists */
    val elementCount: Int?
        get() = if (shape.any { it == null }) {
            null
        } else {
            shape.fold(1L) { acc, dim -> acc * dim!! }.let {
                if (it > Int.MAX_VALUE) null else it.toInt()
            }
        }
}

/**
 * 按名称传入/返回的 ONNX 张量数据 / ONNX tensor data passed in and out by name
 *
 * 数据始终按 row-major 展平，并保留 shape 与 dtype。 / Data is always flattened in row-major order, keeping shape and dtype.
 *
 * @property dtype 数据类型 / data type
 * @property shape 静态形状 / static shape
 * @property data 行主序数据 / row-major data
*/
data class OnnxTensorData(
    val dtype: OnnxDataType,
    val shape: List<Int>,
    val data: DoubleArray
) {
    init {
        require(shape.all { it >= 0 }) { "张量形状不得含负维度 / tensor shape must not contain negative dimensions" }
    }

    /** 元素个数 / Element count */
    val elementCount: Int
        get() = shape.fold(1) { acc, dim -> acc * dim }

    /**
     * 校验数据长度与形状一致 / Validate that the data length matches the shape
     *
     * @return 是否一致 / whether they match
     */
    fun isConsistent(): Boolean {
        return data.size == elementCount
    }
}
