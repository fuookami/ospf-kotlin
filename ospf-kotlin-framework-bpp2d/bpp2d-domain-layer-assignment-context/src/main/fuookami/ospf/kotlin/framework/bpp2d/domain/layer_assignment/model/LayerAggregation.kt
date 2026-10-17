/**
 * 层聚合。 / Layer aggregation.
 *
 * 对应 BPP3D 的 `LayerAggregation`：管理列生成过程中的候选层集合，负责加列去重与移除。
 * / Mirrors BPP3D `LayerAggregation`: manages the candidate layer set during column generation, handling
 * deduplicated column addition and removal.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model

import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer

/**
 * 层聚合，管理列生成中的层集合。 / Layer aggregation managing the layer set during column generation.
 *
 * @param V 数值类型 / numeric type
 * @property layersIteration 每次迭代新增的层 / layers added per iteration
 * @property layers 当前全部层 / all current layers
 * @property removedLayers 已移除的层 / removed layers
*/
class LayerAggregation<V : fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber<V>>(
    val layersIteration: MutableList<List<Layer<V>>> = ArrayList(),
    val layers: MutableList<Layer<V>> = ArrayList(),
    val removedLayers: MutableSet<Layer<V>> = HashSet()
) {

    /** 最近一次迭代的层 / layers from the last iteration */
    val lastIterationLayers: List<Layer<V>>
        get() = layersIteration.lastOrNull { it.isNotEmpty() } ?: emptyList()

    /**
     * 添加新列并去重。 / Add new columns with deduplication.
     *
     * @param newLayers 新层列表 / new layer list
     * @return 去重后实际新增的层 / deduplicated newly added layers
    */
    fun addColumns(newLayers: List<Layer<V>>): List<Layer<V>> {
        val unduplicated = ArrayList<Layer<V>>()
        for (layer in newLayers) {
            if (unduplicated.any { it == layer }) {
                continue
            }
            if (layers.any { it == layer }) {
                continue
            }
            unduplicated.add(layer)
        }
        ManualIndexed.flush<Layer<V>>()
        for (layer in unduplicated) {
            // 同一批列对象会在多次 RMP 装配中重复登记，因此已索引的层只能刷新索引。
            // The same column objects are re-registered across RMP builds, so already indexed layers are only refreshed.
            if (layer.indexed) {
                layer.refreshIndex()
            } else {
                layer.setIndexed()
            }
        }
        layersIteration.add(unduplicated)
        layers.addAll(unduplicated)
        return unduplicated
    }

    /**
     * 移除单个列。 / Remove a single column.
     *
     * @param layer 要移除的层 / layer to remove
    */
    fun removeColumn(layer: Layer<V>) {
        if (!removedLayers.contains(layer)) {
            removedLayers.add(layer)
            layers.remove(layer)
        }
    }

    /**
     * 移除多个列。 / Remove multiple columns.
     *
     * @param layers 要移除的层列表 / layers to remove
    */
    fun removeColumns(layers: List<Layer<V>>) {
        for (layer in layers) {
            removeColumn(layer)
        }
    }
}

/**
 * 把件数换算为无符号整数。 / Convert a count to an unsigned integer.
 *
 * @param value 件数 / count
 * @return 无符号整数 / unsigned integer
*/
internal fun countOf(value: Int): UInt64 {
    return if (value <= 0) UInt64.zero else UInt64(value.toULong())
}
