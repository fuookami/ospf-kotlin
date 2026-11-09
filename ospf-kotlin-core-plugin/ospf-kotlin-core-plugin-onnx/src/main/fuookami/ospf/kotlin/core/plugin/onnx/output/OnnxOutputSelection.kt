/**
 * ONNX 输出聚合 / ONNX output aggregation
*/
package fuookami.ospf.kotlin.core.plugin.onnx.output

/**
 * 输出聚合方式 / Output aggregation mode
 *
 * [Element] 按展平索引取单个标量，其余模式把整个输出张量折叠为一个标量。 / [Element] selects one scalar by flattened index; every other mode folds the whole output tensor into
 * a single scalar.
*/
enum class OnnxAggregation {
    /** 取展平索引处的单个元素 / take the element at the flattened index */
    Element,

    /** 求和 / sum */
    Sum,

    /** 求平均 / mean */
    Mean,

    /** 取第一个元素 / first element */
    First,

    /** 取最后一个元素 / last element */
    Last,

    /** 取最大值 / maximum */
    Max,

    /** 取最小值 / minimum */
    Min;

    /** 是否需要整个张量参与 / Whether the whole tensor participates */
    val isFold: Boolean
        get() = this != Element
}

/**
 * 输出选择 / Output selection
 *
 * @property outputName 输出名称 / output name
 * @property aggregation 聚合方式 / aggregation mode
 * @property elementIndex 展平索引，仅在 [OnnxAggregation.Element] 下生效 / flattened index, only used with [OnnxAggregation.Element]
*/
data class OnnxOutputSelection(
    val outputName: String,
    val aggregation: OnnxAggregation = OnnxAggregation.Element,
    val elementIndex: Int = 0
) {
    init {
        require(elementIndex >= 0) {
            "输出索引不得为负 / output index must not be negative"
        }
    }

    companion object {
        /**
         * 取单个元素的选择 / Selection of a single element
         *
         * @param outputName 输出名称 / output name
         * @param elementIndex 展平索引 / flattened index
         * @return 输出选择 / output selection
         */
        fun element(outputName: String, elementIndex: Int = 0): OnnxOutputSelection {
            return OnnxOutputSelection(
                outputName = outputName,
                aggregation = OnnxAggregation.Element,
                elementIndex = elementIndex
            )
        }

        /**
         * 折叠整个张量的选择 / Selection folding the whole tensor
         *
         * @param outputName 输出名称 / output name
         * @param aggregation 折叠方式 / folding mode
         * @return 输出选择 / output selection
         */
        fun fold(outputName: String, aggregation: OnnxAggregation): OnnxOutputSelection {
            require(aggregation.isFold) {
                "fold 只接受折叠方式 / fold accepts folding modes only"
            }
            return OnnxOutputSelection(outputName = outputName, aggregation = aggregation)
        }
    }
}
