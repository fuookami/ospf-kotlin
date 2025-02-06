package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.utils.concept.ManualIndexed

class LayerAggregation(
    val layersIteration: MutableList<List<BinLayer>> = ArrayList(),
    val layers: MutableList<BinLayer> = ArrayList(),
    val removedLayers: MutableSet<BinLayer> = HashSet()
) {
    val lastIterationLayers: List<BinLayer> get() = layersIteration.lastOrNull { it.isNotEmpty() } ?: emptyList()

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
            layer.setIndexed()
        }
        layersIteration.add(unduplicatedLayers)
        layers.addAll(unduplicatedLayers)

        return unduplicatedLayers
    }

    fun removeColumn(layer: BinLayer)  {
        if (!removedLayers.contains(layer)) {
            removedLayers.add(layer)
            layers.remove(layer)
        }
    }

    fun removeColumns(layers: List<BinLayer>)  {
        for (layer in layers) {
            removeColumn(layer)
        }
    }
}
