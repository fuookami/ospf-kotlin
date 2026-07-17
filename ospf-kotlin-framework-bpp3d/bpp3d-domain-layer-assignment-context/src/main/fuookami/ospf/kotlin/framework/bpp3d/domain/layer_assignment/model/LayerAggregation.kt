/**
 * Layer aggregation model.
 * 层聚合模型。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer

/**
 * Layer aggregation, manages layer sets during column generation.
 * 层聚合，管理列生成过程中的层集合。
 *
 * @property layersIteration 每次迭代新增的层 / layers added per iteration
 * @property layers 当前所有层 / all current layers
 * @property removedLayers 已移除的层 / removed layers
*/
class LayerAggregation(
    val layersIteration: MutableList<List<BinLayer>> = ArrayList(),
    val layers: MutableList<BinLayer> = ArrayList(),
    val removedLayers: MutableSet<BinLayer> = HashSet()
) {

    /**
     * Layers from the last iteration.
     * 最近一次迭代的层。
    */
    val lastIterationLayers: List<BinLayer> get() = layersIteration.lastOrNull { it.isNotEmpty() } ?: emptyList()

    /**
     * Add new columns (after deduplication).
     * 添加新列（去重后）。
     *
     * @param newLayers 新层列表 / new layer list
     * @return 去重后实际新增的层 / deduplicated newly added layers
    */
    suspend fun addColumns(newLayers: List<BinLayer>): List<BinLayer> {
        val thisUnduplicatedLayers = coroutineScope {
            val promises = ArrayList<Deferred<List<BinLayer>>>()
            for (layers in newLayers.groupBy { it.units.usize to it.depth }.values) {
                promises.add(async(Dispatchers.Default) {
                    val unduplicatedLayers = ArrayList<BinLayer>()
                    for (layer in layers) {
                        if (unduplicatedLayers.all { layer neq it }) {
                            unduplicatedLayers.add(layer)
                        }
                    }
                    unduplicatedLayers
                })
            }
            promises.flatMap { it.await() }
        }

        val unduplicatedLayers = coroutineScope {
            val promises = ArrayList<Deferred<List<BinLayer>>>()
            for (unduplicatedLayers in thisUnduplicatedLayers.groupBy { it.units.usize }.values) {
                promises.add(async(Dispatchers.Default) {
                    unduplicatedLayers.filter { layer ->
                        layers.all { layer neq it }
                    }
                })
            }
            promises.flatMap { it.await() }
        }

        ManualIndexed.flush<BinLayer>()
        for (layer in unduplicatedLayers) {
            if (layer.indexed) {
                layer.refreshIndex()
            } else {
                layer.setIndexed()
            }
        }
        layersIteration.add(unduplicatedLayers)
        layers.addAll(unduplicatedLayers)

        return unduplicatedLayers
    }

    /**
     * Remove a single layer.
     * 移除单个层。
     *
     * @param layer 要移除的层 / layer to remove
    */
    fun removeColumn(layer: BinLayer) {
        if (!removedLayers.contains(layer)) {
            removedLayers.add(layer)
            layers.remove(layer)
        }
    }

    /**
     * Remove multiple layers.
     * 移除多个层。
     *
     * @param layers 要移除的层列表 / layers to remove
    */
    fun removeColumns(layers: List<BinLayer>) {
        for (layer in layers) {
            removeColumn(layer)
        }
    }
}
