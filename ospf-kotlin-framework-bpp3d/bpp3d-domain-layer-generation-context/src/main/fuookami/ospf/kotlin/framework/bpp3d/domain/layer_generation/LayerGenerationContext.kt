@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ItemPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.statistics
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.math.algebra.number.Int64
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

/**
 * Layer generation request.
 * 层生成请求。
 */
data class Bpp3dLayerGenerationRequest<V>(
    val iteration: Int,
    val bin: BinType? = null,
    val items: List<Item>,
    val existingLayers: List<BinLayer> = emptyList(),
    val demandEntries: List<LayerGenerationDemandEntry> = emptyList(),
    val shadowPrices: Map<DemandModeKey, V> = emptyMap(),
    val scoreByShadowPrice: ((BinLayer, Bpp3dLayerGenerationRequest<V>) -> Double)? = null,
    val timeLimit: Duration = ZERO,
    val maxCandidates: Int = 256
)

/**
 * Layer generation result.
 * 层生成结果。
 */
data class Bpp3dLayerGenerationResult<V>(
    val layer: BinLayer,
    val reducedCost: V? = null,
    val score: V? = null,
    val numericScore: Double? = null,
    val source: String
)

/**
 * Demand mode and demand key pair.
 * 需求模式与需求键的组合键。
 */
data class DemandModeKey(
    val mode: Bpp3dDemandMode,
    val key: Bpp3dDemandKey
)

/**
 * Demand entry used by layer generation context.
 * layer generation 使用的需求项。
 */
data class LayerGenerationDemandEntry(
    val mode: Bpp3dDemandMode,
    val key: Bpp3dDemandKey
)

/**
 * Unified delegated layer generator interface for BPP3D.
 * BPP3D 统一委托式层生成接口。
 */
interface Bpp3dLayerGenerator<V> {
    suspend fun generate(
        request: Bpp3dLayerGenerationRequest<V>
    ): List<Bpp3dLayerGenerationResult<V>>
}

fun <V> shadowPriceAwareLayerScore(
    shadowPriceToDouble: (V) -> Double,
    demandValueToDouble: (Bpp3dDemandValue) -> Double = { demand ->
        when (demand) {
            is Bpp3dDemandValue.Amount -> demand.value.toULong().toDouble()
            is Bpp3dDemandValue.Weight -> demand.value.value.toDouble()
        }
    }
): (BinLayer, Bpp3dLayerGenerationRequest<V>) -> Double {
    return { layer, request ->
        val activeEntries = if (request.demandEntries.isNotEmpty()) {
            request.demandEntries.map { DemandModeKey(it.mode, it.key) }
        } else {
            request.shadowPrices.keys
        }
        activeEntries.sumOf { entry ->
            val shadowPrice = request.shadowPrices[entry] ?: return@sumOf 0.0
            val demand = layer.statistics(entry.mode)[entry.key] ?: return@sumOf 0.0
            shadowPriceToDouble(shadowPrice) * demandValueToDouble(demand)
        }
    }
}

private fun <V> rankByShadowScore(
    request: Bpp3dLayerGenerationRequest<V>,
    generated: List<Bpp3dLayerGenerationResult<V>>
): List<Bpp3dLayerGenerationResult<V>> {
    val score = request.scoreByShadowPrice ?: return generated
    return generated
        .map { result ->
            val thisScore = result.numericScore ?: score(result.layer, request)
            result.copy(numericScore = thisScore)
        }
        .sortedByDescending { it.numericScore ?: Double.NEGATIVE_INFINITY }
}

private suspend fun <V> delegatedOrDefault(
    request: Bpp3dLayerGenerationRequest<V>,
    delegate: (suspend (Bpp3dLayerGenerationRequest<V>) -> List<Bpp3dLayerGenerationResult<V>>)?,
    fallback: suspend (Bpp3dLayerGenerationRequest<V>) -> List<Bpp3dLayerGenerationResult<V>>
): List<Bpp3dLayerGenerationResult<V>> {
    val generated = delegate?.invoke(request) ?: fallback(request)
    return rankByShadowScore(request, generated)
        .distinctBy { it.layer }
        .take(request.maxCandidates)
}

private fun pickOrientation(item: Item, bin: BinType?): Orientation? {
    return item.enabledOrientations.firstOrNull { orientation ->
        bin?.enabled(item, orientation) ?: true
    }
}

private fun buildLayer(
    request: Bpp3dLayerGenerationRequest<*>,
    source: Class<*>,
    item: Item
): BinLayer? {
    val orientation = pickOrientation(item, request.bin) ?: return null
    val placement = ItemPlacement3(item.view(orientation), point3())
    val iteration = Int64(request.iteration.toLong())
    val bin = request.bin
    return if (bin == null) {
        BinLayer(
            iteration = iteration,
            from = source.kotlin,
            shape = Container3Shape(
                width = placement.width,
                height = placement.height,
                depth = placement.depth
            ),
            units = listOf(placement)
        )
    } else {
        BinLayer(
            iteration = iteration,
            from = source.kotlin,
            bin = bin,
            shape = Container3Shape(bin),
            units = listOf(placement)
        )
    }
}

private fun <V> mapItemsToLayers(
    request: Bpp3dLayerGenerationRequest<V>,
    source: String,
    sourceClass: Class<*>,
    orderedItems: List<Item>
): List<Bpp3dLayerGenerationResult<V>> {
    return rankByShadowScore(
        request = request,
        generated = orderedItems.asSequence()
        .mapNotNull { item -> buildLayer(request, sourceClass, item) }
        .distinct()
        .take(request.maxCandidates)
        .map { layer ->
            Bpp3dLayerGenerationResult<V>(
                layer = layer,
                source = source
            )
        }
        .toList()
    )
}

/**
 * Fallback generator reading pre-built layers from request.
 * 从请求中读取已有层作为兜底生成器。
 */
class HistoricalLayerGenerator<V>(
    private val delegate: (suspend (Bpp3dLayerGenerationRequest<V>) -> List<Bpp3dLayerGenerationResult<V>>)? = null
) : Bpp3dLayerGenerator<V> {
    override suspend fun generate(request: Bpp3dLayerGenerationRequest<V>): List<Bpp3dLayerGenerationResult<V>> {
        return delegatedOrDefault(request, delegate) {
            it.existingLayers.map { layer ->
                Bpp3dLayerGenerationResult(
                    layer = layer,
                    source = "historical"
                )
            }
        }
    }
}

/**
 * Placeholder block-based generator.
 * 基于块装载的占位实现。
 */
class BlockLayerGenerator<V>(
    private val delegate: (suspend (Bpp3dLayerGenerationRequest<V>) -> List<Bpp3dLayerGenerationResult<V>>)? = null
) : Bpp3dLayerGenerator<V> {
    override suspend fun generate(request: Bpp3dLayerGenerationRequest<V>): List<Bpp3dLayerGenerationResult<V>> {
        return delegatedOrDefault(request, delegate) {
            val sortedItems = it.items.sortedByDescending { item -> item.volume.value }
            mapItemsToLayers(it, "block", BlockLayerGenerator::class.java, sortedItems)
        }
    }
}

/**
 * Placeholder BL local generator.
 * BL 局部占位实现。
 */
class BLLocalLayerGenerator<V>(
    private val delegate: (suspend (Bpp3dLayerGenerationRequest<V>) -> List<Bpp3dLayerGenerationResult<V>>)? = null
) : Bpp3dLayerGenerator<V> {
    override suspend fun generate(request: Bpp3dLayerGenerationRequest<V>): List<Bpp3dLayerGenerationResult<V>> {
        return delegatedOrDefault(request, delegate) {
            val sortedItems = it.items.sortedWith(
                compareByDescending<Item> { item -> item.width.value * item.depth.value }
                    .thenByDescending { item -> item.height.value }
            )
            mapItemsToLayers(it, "bl-local", BLLocalLayerGenerator::class.java, sortedItems)
        }
    }
}

/**
 * Placeholder BL global generator.
 * BL 全局占位实现。
 */
class BLGlobalLayerGenerator<V>(
    private val delegate: (suspend (Bpp3dLayerGenerationRequest<V>) -> List<Bpp3dLayerGenerationResult<V>>)? = null
) : Bpp3dLayerGenerator<V> {
    override suspend fun generate(request: Bpp3dLayerGenerationRequest<V>): List<Bpp3dLayerGenerationResult<V>> {
        return delegatedOrDefault(request, delegate) {
            val sortedItems = it.items.sortedWith(
                compareByDescending<Item> { item -> item.weight.value }
                    .thenByDescending { item -> item.volume.value }
            )
            mapItemsToLayers(it, "bl-global", BLGlobalLayerGenerator::class.java, sortedItems)
        }
    }
}

/**
 * Placeholder pattern-based generator.
 * 基于 pattern 的占位实现。
 */
class PatternLayerGenerator<V>(
    private val delegate: (suspend (Bpp3dLayerGenerationRequest<V>) -> List<Bpp3dLayerGenerationResult<V>>)? = null
) : Bpp3dLayerGenerator<V> {
    override suspend fun generate(request: Bpp3dLayerGenerationRequest<V>): List<Bpp3dLayerGenerationResult<V>> {
        return delegatedOrDefault(request, delegate) {
            val picked = LinkedHashMap<Any, Item>()
            for (item in it.items) {
                picked.putIfAbsent(item.pattern, item)
            }
            mapItemsToLayers(it, "pattern", PatternLayerGenerator::class.java, picked.values.toList())
        }
    }
}

/**
 * Placeholder pile-based generator.
 * 基于 pile 的占位实现。
 */
class PileLayerGenerator<V>(
    private val delegate: (suspend (Bpp3dLayerGenerationRequest<V>) -> List<Bpp3dLayerGenerationResult<V>>)? = null
) : Bpp3dLayerGenerator<V> {
    override suspend fun generate(request: Bpp3dLayerGenerationRequest<V>): List<Bpp3dLayerGenerationResult<V>> {
        return delegatedOrDefault(request, delegate) {
            val sortedItems = it.items.sortedWith(
                compareByDescending<Item> { item -> item.maxLayer.toLong() }
                    .thenByDescending { item -> item.maxHeight.value }
            )
            mapItemsToLayers(it, "pile", PileLayerGenerator::class.java, sortedItems)
        }
    }
}

/**
 * Placeholder circle-packing generator.
 * 圆密排占位实现。
 */
class CirclePackingLayerGenerator<V>(
    private val delegate: (suspend (Bpp3dLayerGenerationRequest<V>) -> List<Bpp3dLayerGenerationResult<V>>)? = null
) : Bpp3dLayerGenerator<V> {
    override suspend fun generate(request: Bpp3dLayerGenerationRequest<V>): List<Bpp3dLayerGenerationResult<V>> {
        return delegatedOrDefault(request, delegate) {
            val preferred = it.items.filter { item ->
                item.packageType.category != fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageCategory.Pallet
            }
            val fallback = if (preferred.isNotEmpty()) preferred else it.items
            mapItemsToLayers(it, "circle-packing", CirclePackingLayerGenerator::class.java, fallback)
        }
    }
}

/**
 * Composite delegated layer generation context.
 * 组合式委托层生成上下文。
 */
class LayerGenerationContext<V>(
    private val generators: List<Bpp3dLayerGenerator<V>> = listOf(HistoricalLayerGenerator())
) : Bpp3dLayerGenerator<V> {
    override suspend fun generate(request: Bpp3dLayerGenerationRequest<V>): List<Bpp3dLayerGenerationResult<V>> {
        if (generators.isEmpty()) {
            return emptyList()
        }
        val all = ArrayList<Bpp3dLayerGenerationResult<V>>()
        for (generator in generators) {
            all += generator.generate(request)
            if (all.size >= request.maxCandidates) {
                break
            }
        }
        return all
            .distinctBy { it.layer }
            .take(request.maxCandidates)
    }
}
