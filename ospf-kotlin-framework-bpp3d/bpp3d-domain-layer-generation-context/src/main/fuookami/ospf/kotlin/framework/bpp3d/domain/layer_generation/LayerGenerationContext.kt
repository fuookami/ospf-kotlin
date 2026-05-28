@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation

import fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service.ComplexBlockGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service.SimpleBlockGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Block
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ItemPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.group
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.statistics
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.toConcreteMode
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.math.floor
import kotlin.math.sqrt

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
    val scoreByShadowPrice: ((BinLayer, Bpp3dLayerGenerationRequest<V>) -> InfraNumber)? = null,
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
    val numericScore: InfraNumber? = null,
    val source: String
)

/**
 * Demand mode and demand key pair.
 * 需求模式与需求键的组合键。
 */
data class DemandModeKey(
    val mode: Bpp3dDemandMode,
    val key: Bpp3dDemandKey,
    val quantityUnit: PhysicalUnit? = null
)

/**
 * Demand entry used by layer generation context.
 * layer generation 使用的需求项。
 */
data class LayerGenerationDemandEntry(
    val mode: Bpp3dDemandMode,
    val key: Bpp3dDemandKey,
    val quantityUnit: PhysicalUnit? = null
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

private fun resolveDemandDomainDiscrete(unit: PhysicalUnit?): Boolean {
    val domainRaw = runCatching {
        unit?.javaClass?.methods
            ?.firstOrNull { it.name == "getDomain" && it.parameterCount == 0 }
            ?.invoke(unit)
            ?.toString()
    }.getOrNull()
    return domainRaw.equals("Discrete", ignoreCase = true)
}

fun <V> shadowPriceAwareLayerScore(
    shadowPriceToScalar: (V) -> InfraNumber,
    demandValueToScalar: (Bpp3dDemandValue) -> InfraNumber = { demand ->
        when (demand) {
            is Bpp3dDemandValue.Amount -> InfraNumber(demand.value.toULong().toDouble())
            is Bpp3dDemandValue.Weight -> demand.value.value
        }
    }
): (BinLayer, Bpp3dLayerGenerationRequest<V>) -> InfraNumber {
    return { layer, request ->
        val activeEntries = if (request.demandEntries.isNotEmpty()) {
            request.demandEntries.map { DemandModeKey(it.mode, it.key, it.quantityUnit) }
        } else {
            request.shadowPrices.keys
        }
        var total = InfraNumber.zero
        for (entry in activeEntries) {
            val shadowPrice = request.shadowPrices[entry] ?: continue
            val concreteMode = entry.mode.toConcreteMode(
                isDiscrete = resolveDemandDomainDiscrete(entry.quantityUnit)
            )
            val demand = layer.statistics(concreteMode)[entry.key] ?: continue
            total += shadowPriceToScalar(shadowPrice) * demandValueToScalar(demand)
        }
        total
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
        .sortedByDescending { it.numericScore ?: InfraNumber.negativeInfinity }
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

private suspend fun <V> mapBlockLoadingToLayers(
    request: Bpp3dLayerGenerationRequest<V>,
    source: String,
    sourceClass: Class<*>,
    useGlobalSearch: Boolean
): List<Bpp3dLayerGenerationResult<V>> {
    val bin = request.bin ?: return emptyList()
    val groupedItems = request.items.group()
    if (groupedItems.isEmpty()) {
        return emptyList()
    }

    val binShape = Container3Shape(bin)
    val simpleBlocks = SimpleBlockGenerator(
        config = SimpleBlockGenerator.Config(
            mergeAsPatternBlock = false,
            withRotation = true,
            withRemainder = false
        )
    ).invoke(
        items = groupedItems,
        space = binShape,
        patterns = emptyList(),
        restWeight = bin.capacity.value
    )
    if (simpleBlocks.isEmpty()) {
        return emptyList()
    }

    val complexBlocks = ComplexBlockGenerator(
        config = ComplexBlockGenerator.Config(
            withX = true,
            withY = true,
            withZ = false
        )
    ).invoke(
        items = groupedItems,
        space = binShape,
        simpleBlocks = simpleBlocks,
        restWeight = bin.capacity.value
    )

    val blockTable = (simpleBlocks + complexBlocks)
        .filter { binShape.enabled(it) }
        .distinct()
    if (blockTable.isEmpty()) {
        return emptyList()
    }

    val orderedBlocks = if (useGlobalSearch) {
        blockTable.sortedWith(
            compareByDescending<Block> { block -> block.volume.value }
                .thenByDescending { block -> block.weight.value }
        )
    } else {
        blockTable.sortedWith(
            compareByDescending<Block> { block -> block.units.size }
                .thenByDescending { block -> block.volume.value }
        )
    }

    val iteration = Int64(request.iteration.toLong())
    return orderedBlocks.asSequence()
        .mapNotNull { block ->
            val units = block.dump()
            if (units.isEmpty()) {
                return@mapNotNull null
            }
            BinLayer(
                iteration = iteration,
                from = sourceClass.kotlin,
                bin = bin,
                shape = Container3Shape(bin),
                units = units
            )
        }
        .distinct()
        .take(request.maxCandidates)
        .map { layer ->
            Bpp3dLayerGenerationResult<V>(
                layer = layer,
                source = source
            )
        }
        .toList()
}

private suspend fun <V> mapItemsToPileLayers(
    request: Bpp3dLayerGenerationRequest<V>,
    source: String,
    sourceClass: Class<*>
): List<Bpp3dLayerGenerationResult<V>> {
    val bin = request.bin ?: return emptyList()
    val binShape = Container3Shape(bin)
    val iteration = Int64(request.iteration.toLong())
    val layers = LinkedHashSet<BinLayer>()
    for (item in request.items) {
        val orientation = pickOrientation(item, request.bin) ?: continue
        val itemView = item.view(orientation)
        val maxByBinHeight = (bin.height.value / itemView.height.value).floor().toUInt64()
        if (maxByBinHeight <= UInt64.one) {
            continue
        }

        val placements = ArrayList<ItemPlacement3>()
        for (index in UInt64.zero until maxByBinHeight) {
            val placement = ItemPlacement3(
                view = itemView,
                position = point3(y = itemView.height * legacyScalar(index.toULong().toDouble()))
            )
            val isEnabled = item.packageAttribute.enabledStackingOn(
                item = placement,
                bottomItems = placements,
                space = binShape
            )
            if (!isEnabled) {
                break
            }
            placements.add(placement)
        }
        if (placements.size <= 1) {
            continue
        }

        layers.add(
            BinLayer(
                iteration = iteration,
                from = sourceClass.kotlin,
                bin = bin,
                shape = binShape,
                units = placements
            )
        )
        if (layers.size >= request.maxCandidates) {
            break
        }
    }

    val generated = layers
        .map { layer ->
            Bpp3dLayerGenerationResult<V>(
                layer = layer,
                source = source
            )
        }
    return rankByShadowScore(request, generated)
}

private suspend fun <V> mapItemsToCirclePackingLayers(
    request: Bpp3dLayerGenerationRequest<V>,
    sourceClass: Class<*>
): List<Bpp3dLayerGenerationResult<V>> {
    val bin = request.bin ?: return emptyList()
    val iteration = Int64(request.iteration.toLong())
    val binWidth = bin.width.value.toDouble()
    val binDepth = bin.depth.value.toDouble()
    if (binWidth <= 0.0 || binDepth <= 0.0) {
        return emptyList()
    }

    val candidates = ArrayList<Triple<BinLayer, String, Int>>()
    val items = request.items
        .filter { item ->
            item.packageType.category != fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageCategory.Pallet
        }
        .distinct()
    for (item in items) {
        val orientation = pickOrientation(item, request.bin) ?: continue
        val itemView = item.view(orientation)
        val diameter = if (itemView.width.value.toDouble() >= itemView.depth.value.toDouble()) {
            itemView.width
        } else {
            itemView.depth
        }
        val diameterValue = diameter.value.toDouble()
        if (diameterValue <= 0.0 || diameterValue > binWidth || diameterValue > binDepth) {
            continue
        }

        val rectCols = floor(binWidth / diameterValue).toInt()
        val rectRows = floor(binDepth / diameterValue).toInt()
        if (rectCols > 0 && rectRows > 0) {
            val placements = ArrayList<ItemPlacement3>(rectCols * rectRows)
            for (row in 0 until rectRows) {
                val z = diameter * legacyScalar(row.toDouble())
                for (col in 0 until rectCols) {
                    val x = diameter * legacyScalar(col.toDouble())
                    placements.add(
                        ItemPlacement3(
                            view = itemView,
                            position = point3(x = x, z = z)
                        )
                    )
                }
            }
            if (placements.isNotEmpty()) {
                candidates.add(
                    Triple(
                        BinLayer(
                            iteration = iteration,
                            from = sourceClass.kotlin,
                            bin = bin,
                            shape = Container3Shape(bin),
                            units = placements
                        ),
                        "circle-packing-rect",
                        placements.size
                    )
                )
            }
        }

        val hexRowStepScale = sqrt(3.0) / 2.0
        val hexRowStep = diameter * legacyScalar(hexRowStepScale)
        val hexRowStepValue = hexRowStep.value.toDouble()
        if (hexRowStepValue > 0.0) {
            val placements = ArrayList<ItemPlacement3>()
            var row = 0
            while (true) {
                val z = hexRowStep * legacyScalar(row.toDouble())
                if (z.value.toDouble() + diameterValue > binDepth) {
                    break
                }
                val offset = if (row % 2 == 0) 0.0 else 0.5
                var col = 0
                while (true) {
                    val xScale = col.toDouble() + offset
                    val x = diameter * legacyScalar(xScale)
                    if (x.value.toDouble() + diameterValue > binWidth) {
                        break
                    }
                    placements.add(
                        ItemPlacement3(
                            view = itemView,
                            position = point3(x = x, z = z)
                        )
                    )
                    col += 1
                }
                row += 1
            }
            if (placements.isNotEmpty()) {
                candidates.add(
                    Triple(
                        BinLayer(
                            iteration = iteration,
                            from = sourceClass.kotlin,
                            bin = bin,
                            shape = Container3Shape(bin),
                            units = placements
                        ),
                        "circle-packing-hex",
                        placements.size
                    )
                )
            }
        }
    }

    val ranked = candidates
        .sortedWith(
            compareByDescending<Triple<BinLayer, String, Int>> { it.third }
                .thenByDescending { it.second == "circle-packing-hex" }
        )
        .take(request.maxCandidates)
        .map { (layer, source, packed) ->
            Bpp3dLayerGenerationResult<V>(
                layer = layer,
                numericScore = InfraNumber(packed.toDouble()),
                source = source
            )
        }
    return rankByShadowScore(request, ranked)
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
            val byBlockLoading = mapBlockLoadingToLayers(
                request = it,
                source = "block",
                sourceClass = BlockLayerGenerator::class.java,
                useGlobalSearch = false
            )
            if (byBlockLoading.isNotEmpty()) {
                byBlockLoading
            } else {
                val sortedItems = it.items.sortedByDescending { item -> item.volume.value }
                mapItemsToLayers(it, "block", BlockLayerGenerator::class.java, sortedItems)
            }
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
            val byBlockLoading = mapBlockLoadingToLayers(
                request = it,
                source = "bl-local",
                sourceClass = BLLocalLayerGenerator::class.java,
                useGlobalSearch = false
            )
            if (byBlockLoading.isNotEmpty()) {
                byBlockLoading
            } else {
                val sortedItems = it.items.sortedWith(
                    compareByDescending<Item> { item -> item.width.value * item.depth.value }
                        .thenByDescending { item -> item.height.value }
                )
                mapItemsToLayers(it, "bl-local", BLLocalLayerGenerator::class.java, sortedItems)
            }
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
            val byBlockLoading = mapBlockLoadingToLayers(
                request = it,
                source = "bl-global",
                sourceClass = BLGlobalLayerGenerator::class.java,
                useGlobalSearch = true
            )
            if (byBlockLoading.isNotEmpty()) {
                byBlockLoading
            } else {
                val sortedItems = it.items.sortedWith(
                    compareByDescending<Item> { item -> item.weight.value }
                        .thenByDescending { item -> item.volume.value }
                )
                mapItemsToLayers(it, "bl-global", BLGlobalLayerGenerator::class.java, sortedItems)
            }
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
            val groupedByPattern = it.items.groupBy { item -> item.pattern }
            val byBlockLoading = if (it.bin != null) {
                groupedByPattern.values.flatMap { groupItems ->
                    mapBlockLoadingToLayers(
                        request = it.copy(items = groupItems),
                        source = "pattern",
                        sourceClass = PatternLayerGenerator::class.java,
                        useGlobalSearch = false
                    )
                }
            } else {
                emptyList()
            }
            if (byBlockLoading.isNotEmpty()) {
                byBlockLoading
            } else {
            val picked = LinkedHashMap<Any, Item>()
            for (item in it.items) {
                picked.putIfAbsent(item.pattern, item)
            }
                mapItemsToLayers(it, "pattern", PatternLayerGenerator::class.java, picked.values.toList())
            }
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
            val byPile = if (it.bin != null) {
                mapItemsToPileLayers(
                    request = it,
                    source = "pile",
                    sourceClass = PileLayerGenerator::class.java
                )
            } else {
                emptyList()
            }
            if (byPile.isNotEmpty()) {
                byPile
            } else {
                val sortedItems = it.items.sortedWith(
                    compareByDescending<Item> { item -> item.maxLayer.toLong() }
                        .thenByDescending { item -> item.maxHeight.value }
                )
                mapItemsToLayers(it, "pile", PileLayerGenerator::class.java, sortedItems)
            }
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
            val packed = if (it.bin != null) {
                mapItemsToCirclePackingLayers(
                    request = it,
                    sourceClass = CirclePackingLayerGenerator::class.java
                )
            } else {
                emptyList()
            }
            if (packed.isNotEmpty()) {
                packed
            } else {
                val preferred = it.items.filter { item ->
                    item.packageType.category != fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageCategory.Pallet
                }
                val fallback = if (preferred.isNotEmpty()) preferred else it.items
                mapItemsToLayers(it, "circle-packing", CirclePackingLayerGenerator::class.java, fallback)
            }
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


