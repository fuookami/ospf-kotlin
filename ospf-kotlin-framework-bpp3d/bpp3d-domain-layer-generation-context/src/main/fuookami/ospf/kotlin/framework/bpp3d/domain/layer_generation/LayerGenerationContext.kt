/**
 * Layer generation context.
 * 层生成上下文。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation

import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.minus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.quantity.div
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingProgramCandidate

/**
 * Layer generation request.
 * 层生成请求。
 *
 * @property iteration The current iteration number.
 * @property bin The target bin type, or null if unspecified.
 * @property items The list of items to pack.
 * @property existingLayers Pre-existing layers to consider.
 * @property demandEntries Demand entries for the generation.
 * @property shadowPrices Shadow prices mapped by demand mode key.
 * @property scoreByShadowPrice Optional scoring function using shadow prices.
 * @property timeLimit Time limit for generation.
 * @property maxCandidates Maximum number of candidate layers.
*/
data class Bpp3dLayerGenerationRequest<V>(
    val iteration: Int,
    val bin: BinType<FltX>? = null,
    val items: List<Item>,
    val existingLayers: List<BinLayer> = emptyList(),
    val demandEntries: List<LayerGenerationDemandEntry> = emptyList(),
    val shadowPrices: Map<DemandModeKey, V> = emptyMap(),
    val scoreByShadowPrice: ((BinLayer, Bpp3dLayerGenerationRequest<V>) -> FltX)? = null,
    val timeLimit: Duration = ZERO,
    val maxCandidates: Int = 256
)

/**
 * layerPoint3.
 * layerPoint3。
 * @param x x-coordinate quantity / X 坐标量
 * @param y y-coordinate quantity / Y 坐标量
 * @param z z-coordinate quantity / Z 坐标量
 * @param unit packing unit to check / 待检查的装箱单元
 * @return 3D quantity point / 三维量纲点
*/
private fun layerPoint3(
    x: Quantity<FltX>? = null,
    y: Quantity<FltX>? = null,
    z: Quantity<FltX>? = null,
    unit: PhysicalUnit? = null
): QuantityPoint3<FltX> {
    val actualUnit = unit ?: x?.unit ?: y?.unit ?: z?.unit ?: Meter
    val zero = Quantity(FltX.zero, actualUnit)
    return point3(
        x = x ?: zero,
        y = y ?: zero,
        z = z ?: zero
    )
}

/**
 * Layer generation result.
 * 层生成结果。
*/
data class Bpp3dLayerGenerationResult<V>(
    val layer: BinLayer,
    val reducedCost: V? = null,
    val score: V? = null,
    val numericScore: FltX? = null,
    val source: String
)

fun <V> bpp3dLayerGenerationRequest(
    iteration: Int,
    bin: BinType<FltX>? = null,
    items: List<Item>,
    existingLayers: List<BinLayer> = emptyList(),
    demandEntries: List<LayerGenerationDemandEntry> = emptyList(),
    shadowPrices: Map<DemandModeKey, V> = emptyMap(),
    scoreByShadowPrice: ((BinLayer, Bpp3dLayerGenerationRequest<V>) -> FltX)? = null,
    timeLimit: Duration = ZERO,
    maxCandidates: Int = 256
): Bpp3dLayerGenerationRequest<V> {
    return Bpp3dLayerGenerationRequest(
        iteration = iteration,
        bin = bin,
        items = items,
        existingLayers = existingLayers,
        demandEntries = demandEntries,
        shadowPrices = shadowPrices,
        scoreByShadowPrice = scoreByShadowPrice,
        timeLimit = timeLimit,
        maxCandidates = maxCandidates
    )
}

fun <T : FloatingNumber<T>, V> bpp3dLayerGenerationRequestFromQuantity(
    iteration: Int,
    bin: BinType<FltX>? = null,
    items: List<QuantityItem<T>>,
    existingLayers: List<QuantityBinLayer<T>> = emptyList(),
    demandEntries: List<LayerGenerationDemandEntry> = emptyList(),
    shadowPrices: Map<DemandModeKey, V> = emptyMap(),
    scoreByShadowPrice: ((BinLayer, Bpp3dLayerGenerationRequest<V>) -> FltX)? = null,
    timeLimit: Duration = ZERO,
    maxCandidates: Int = 256
): Bpp3dLayerGenerationRequest<V> {
    val materialCache = LinkedHashMap<QuantityMaterial<T>, Material<FltX>>()
    val itemCache = LinkedHashMap<QuantityItem<T>, ActualItem>()
    return bpp3dLayerGenerationRequest(
        iteration = iteration,
        bin = bin,
        items = items.map { it.toModel(materialCache, itemCache) },
        existingLayers = existingLayers.map { it.toModel(materialCache, itemCache) },
        demandEntries = demandEntries,
        shadowPrices = shadowPrices,
        scoreByShadowPrice = scoreByShadowPrice,
        timeLimit = timeLimit,
        maxCandidates = maxCandidates
    )
}

fun <V> bpp3dLayerGenerationRequestFromProgramDemands(
    iteration: Int,
    bin: BinType<FltX>? = null,
    items: List<Item> = emptyList(),
    programDemands: List<Pair<MaterialPackingProgramCandidate<*>, UInt64>>,
    programMaterialCatalog: Map<MaterialKey, Material<FltX>> = emptyMap(),
    existingLayers: List<BinLayer> = emptyList(),
    demandEntries: List<LayerGenerationDemandEntry> = emptyList(),
    shadowPrices: Map<DemandModeKey, V> = emptyMap(),
    scoreByShadowPrice: ((BinLayer, Bpp3dLayerGenerationRequest<V>) -> FltX)? = null,
    timeLimit: Duration = ZERO,
    maxCandidates: Int = 256
): Bpp3dLayerGenerationRequest<V> {
    return bpp3dLayerGenerationRequest(
        iteration = iteration,
        bin = bin,
        items = items + layerGenerationItemsFromPrograms(
            programDemands = programDemands,
            materialCatalog = programMaterialCatalog
        ),
        existingLayers = existingLayers,
        demandEntries = demandEntries,
        shadowPrices = shadowPrices,
        scoreByShadowPrice = scoreByShadowPrice,
        timeLimit = timeLimit,
        maxCandidates = maxCandidates
    )
}

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

/**
 * generate.
 * generate。
 * @param request layer generation request / 层生成请求
 * @return list of layer generation results / 层生成结果列表
*/
    suspend fun generate(
        request: Bpp3dLayerGenerationRequest<V>
    ): List<Bpp3dLayerGenerationResult<V>>
}

/**
 * 使用泛型输入执行层生成。
 * Execute layer generation with quantity inputs.
 *
 * @param T 量纲数值类型 / quantity numeric type
 * @param iteration 当前迭代 / current iteration
 * @param bin 目标箱型 / target bin type
 * @param items 量纲货物列表 / quantity item list
 * @param existingLayers 已有层列表 / existing layer list
 * @param demandEntries 需求条目 / demand entries
 * @param shadowPrices 影子价格 / shadow prices
 * @param scoreByShadowPrice 影子价格评分函数 / shadow-price score function
 * @param timeLimit 时间限制 / time limit
 * @param maxCandidates 最大候选数 / max candidate amount
 * @return 层生成结果列表 / layer generation result list
*/
suspend fun <V, T : FloatingNumber<T>> Bpp3dLayerGenerator<V>.generateFromQuantity(
    iteration: Int,
    bin: BinType<FltX>? = null,
    items: List<QuantityItem<T>>,
    existingLayers: List<QuantityBinLayer<T>> = emptyList(),
    demandEntries: List<LayerGenerationDemandEntry> = emptyList(),
    shadowPrices: Map<DemandModeKey, V> = emptyMap(),
    scoreByShadowPrice: ((BinLayer, Bpp3dLayerGenerationRequest<V>) -> FltX)? = null,
    timeLimit: Duration = ZERO,
    maxCandidates: Int = 256
): List<Bpp3dLayerGenerationResult<V>> {
    return generate(
        request = bpp3dLayerGenerationRequestFromQuantity(
            iteration = iteration,
            bin = bin,
            items = items,
            existingLayers = existingLayers,
            demandEntries = demandEntries,
            shadowPrices = shadowPrices,
            scoreByShadowPrice = scoreByShadowPrice,
            timeLimit = timeLimit,
            maxCandidates = maxCandidates
        )
    )
}

suspend fun <V> Bpp3dLayerGenerator<V>.generateFromProgramDemands(
    iteration: Int,
    bin: BinType<FltX>? = null,
    items: List<Item> = emptyList(),
    programDemands: List<Pair<MaterialPackingProgramCandidate<*>, UInt64>>,
    programMaterialCatalog: Map<MaterialKey, Material<FltX>> = emptyMap(),
    existingLayers: List<BinLayer> = emptyList(),
    demandEntries: List<LayerGenerationDemandEntry> = emptyList(),
    shadowPrices: Map<DemandModeKey, V> = emptyMap(),
    scoreByShadowPrice: ((BinLayer, Bpp3dLayerGenerationRequest<V>) -> FltX)? = null,
    timeLimit: Duration = ZERO,
    maxCandidates: Int = 256
): List<Bpp3dLayerGenerationResult<V>> {
    return generate(
        request = bpp3dLayerGenerationRequestFromProgramDemands(
            iteration = iteration,
            bin = bin,
            items = items,
            programDemands = programDemands,
            programMaterialCatalog = programMaterialCatalog,
            existingLayers = existingLayers,
            demandEntries = demandEntries,
            shadowPrices = shadowPrices,
            scoreByShadowPrice = scoreByShadowPrice,
            timeLimit = timeLimit,
            maxCandidates = maxCandidates
        )
    )
}

/**
 * resolveDemandDomainDiscrete.
 * resolveDemandDomainDiscrete。
 * @param unit packing unit to check / 待检查的装箱单元
 * @return whether the unit domain is discrete / 单位域是否为离散
*/
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
    shadowPriceToScalar: (V) -> FltX,
    demandValueToScalar: (Bpp3dDemandValue) -> FltX = { demand ->
        when (demand) {
            is Bpp3dDemandValue.Amount -> FltX(demand.value.toULong().toDouble())
            is Bpp3dDemandValue.Weight -> demand.value.value
        }
    }
): (BinLayer, Bpp3dLayerGenerationRequest<V>) -> FltX {
    return { layer, request ->
        val activeEntries = if (request.demandEntries.isNotEmpty()) {
            request.demandEntries.map { DemandModeKey(it.mode, it.key, it.quantityUnit) }
        } else {
            request.shadowPrices.keys
        }
        var total = FltX.zero
        for (entry in activeEntries) {
            val shadowPrice = request.shadowPrices[entry] ?: continue
            val concreteMode = entry.mode.toConcreteMode(
                key = entry.key,
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
        .sortedByDescending { it.numericScore ?: FltX.negativeInfinity }
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

/**
 * pickOrientation.
 * pickOrientation。
 * @param item item to evaluate / 待评估的货物
 * @param bin target bin type, or null / 目标箱型，或 null
 * @return first enabled orientation, or null / 第一个可用姿态，或 null
*/
private fun pickOrientation(item: Item, bin: BinType<FltX>?): Orientation? {
    return item.enabledOrientations.firstOrNull { orientation ->
        bin?.enabled(item, orientation) ?: true
    }
}

/**
 * Builds layer.
 * 构建Layer。
 * @param request layer generation request / 层生成请求
 * @param source source cylinder shape / 源圆柱形状
 * @param item item to evaluate / 待评估的货物
 * @return single-item bin layer, or null / 单货物箱层，或 null
*/
private fun buildLayer(
    request: Bpp3dLayerGenerationRequest<*>,
    source: Class<*>,
    item: Item
): BinLayer? {
    val orientation = pickOrientation(item, request.bin) ?: return null
    val placement = QuantityPlacement3<Item, FltX>(item.view(orientation), point3FltX())
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
            shape = Container3Shape(bin.asContainer3Shape()),
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
    for (item in orderedItems) {
        when (val axisCheck = requireVerticalCylinderAxis(
            shape = item.packingShape,
            path = CylinderCapabilityPath.DefaultLayerCandidate
        )) {
            is Ok -> {}
            is Failed -> return emptyList()
            is Fatal -> return emptyList()
        }
    }
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

    val binShape = Container3Shape(bin.asContainer3Shape())
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
                shape = Container3Shape(bin.asContainer3Shape()),
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
    val binShape = Container3Shape(bin.asContainer3Shape())
    val iteration = Int64(request.iteration.toLong())
    val layers = LinkedHashSet<BinLayer>()
    for (item in request.items) {
        val orientation = pickOrientation(item, request.bin) ?: continue
        when (val supportCheck = requireUprightVerticalCylinderSupport(
            shape = item.packingShape,
            orientation = orientation,
            path = CylinderCapabilityPath.PileSupportCandidate
        )) {
            is Ok -> {}
            is Failed -> continue
            is Fatal -> continue
        }
        val itemView = item.view(orientation)
        val maxByBinHeight = (bin.height.value / itemView.height.value).floor().toUInt64()
        if (maxByBinHeight <= UInt64.one) {
            continue
        }

        val placements = ArrayList<QuantityPlacement3<Item, FltX>>()
        for (index in UInt64.zero until maxByBinHeight) {
            val placement = QuantityPlacement3<Item, FltX>(
                view = itemView,
                position = layerPoint3(y = itemView.height * FltX(index.toULong().toDouble()))
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

/**
 * CirclePackingCandidateItemView class.
 * CirclePackingCandidateItemView类。
*/
private class CirclePackingCandidateItemView(
    unit: Item,
    orientation: Orientation,
    override val placementPackingShape: CylinderPackingShape3
) : ItemView(unit, orientation) {
    override val width get() = placementPackingShape.boundingWidth
    override val height get() = placementPackingShape.boundingHeight
    override val depth get() = placementPackingShape.boundingDepth

    override fun copy(): ItemView {
        return CirclePackingCandidateItemView(
            unit = unit,
            orientation = orientation,
            placementPackingShape = placementPackingShape
        )
    }

    override fun hashCode(): Int {
        var result = unit.hashCode()
        result = 31 * result + orientation.hashCode()
        result = 31 * result + placementPackingShape.axis.hashCode()
        result = 31 * result + placementPackingShape.radius.value.toDouble().hashCode()
        result = 31 * result + placementPackingShape.boundingHeight.value.toDouble().hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is CirclePackingCandidateItemView) {
            return false
        }
        return unit == other.unit
                && orientation == other.orientation
                && placementPackingShape.axis == other.placementPackingShape.axis
                && placementPackingShape.radius.value.toDouble() == other.placementPackingShape.radius.value.toDouble()
                && placementPackingShape.boundingHeight.value.toDouble() == other.placementPackingShape.boundingHeight.value.toDouble()
    }
}

/**
 * CirclePackingItemCandidate data class.
 * CirclePackingItemCandidate数据类。
*/
private data class CirclePackingItemCandidate(
    val view: ItemView,
    val sourceRadius: Quantity<FltX>? = null
)

/**
 * CirclePackingLayerCandidate data class.
 * CirclePackingLayerCandidate数据类。
*/
private data class CirclePackingLayerCandidate(
    val layer: BinLayer,
    val source: String,
    val packed: Int,
    val volume: Double
)

/**
 * cylinderAxisLength.
 * cylinderAxisLength。
 * @param source source cylinder shape / 源圆柱形状
 * @param axis cylinder axis direction / 圆柱轴向
 * @return axis-aligned length of the cylinder / 圆柱沿该轴的长度
*/
private fun cylinderAxisLength(
    source: CylinderPackingShape3,
    axis: Axis3
): Quantity<FltX> {
    return when (axis) {
        Axis3.X -> source.boundingWidth
        Axis3.Y -> source.boundingHeight
        Axis3.Z -> source.boundingDepth
    }
}

/**
 * cylinderCandidateShape.
 * cylinderCandidateShape。
 * @param source source cylinder shape / 源圆柱形状
 * @param radius candidate radius / 候选半径
 * @param axis cylinder axis direction / 圆柱轴向
 * @return new cylinder packing shape with the given radius / 具有给定半径的新圆柱装载形状
*/
private fun cylinderCandidateShape(
    source: CylinderPackingShape3,
    radius: Quantity<FltX>,
    axis: Axis3
): CylinderPackingShape3 {
    return CylinderPackingShape3(
        cylinder = object : AbstractCylinder<FltX> {
            override val radius = radius
            override val height = cylinderAxisLength(
                source = source,
                axis = axis
            )
            override val axis = axis
            override val weight = source.weight
        }
    )
}

/**
 * circlePackingItemCandidates.
 * circlePackingItemCandidates。
 * @param item item to evaluate / 待评估的货物
 * @param bin target bin type, or null / 目标箱型，或 null
 * @return list of circle-packing item candidates / 圆密排货物候选列表
*/
private fun circlePackingItemCandidates(
    item: Item,
    bin: BinType<FltX>?
): List<CirclePackingItemCandidate> {
    if (bin != null && (item.weight leq bin.capacity) != true) {
        return emptyList()
    }
    requireConcreteCirclePackingRadiusMetadata(item)
    return when (val shape = item.packingShape) {
        is CylinderPackingShape3 -> {
            if (!item.enabledOrientations.contains(Orientation.Upright)) {
                return emptyList()
            }
            val spec = item.packingShapeSpec as? PackageShapeSpec.VerticalCylinder
            val radii = spec?.resolvedRadiusCandidates ?: listOf(shape.radius)
            val dynamic = radii.size > 1
            radii.map { radius ->
                val candidateShape = cylinderCandidateShape(
                    source = shape,
                    radius = radius,
                    axis = shape.axis
                )
                CirclePackingItemCandidate(
                    view = CirclePackingCandidateItemView(
                        unit = item,
                        orientation = Orientation.Upright,
                        placementPackingShape = candidateShape
                    ),
                    sourceRadius = if (dynamic) radius else null
                )
            }
        }

        else -> {
            val orientation = pickOrientation(item, bin) ?: return emptyList()
            listOf(CirclePackingItemCandidate(view = item.view(orientation)))
        }
    }
}

/**
 * requireConcreteCirclePackingRadiusMetadata.
 * requireConcreteCirclePackingRadiusMetadata。
 * @param item item to evaluate / 待评估的货物
*/
private fun requireConcreteCirclePackingRadiusMetadata(item: Item) {
    item.packingShapeSpec?.let { spec ->
        requireConcreteCylinderRadiusProductionMetadata(
            spec = spec,
            source = CylinderCapabilityPath.CirclePackingCandidate.source
        )!!
    }
}

/**
 * circlePackingSource.
 * circlePackingSource。
 * @param pattern source pattern name / 来源模式名称
 * @param candidate circle packing item candidate / 圆密排货物候选
 * @return source string with optional suffix / 带可选后缀的来源字符串
*/
private fun circlePackingSource(
    pattern: String,
    candidate: CirclePackingItemCandidate
): String {
    val radius = candidate.sourceRadius ?: return pattern
    return "$pattern-r=${radius.value.toDouble()}"
}

/**
 * circlePackingSource.
 * circlePackingSource。
 * @param pattern source pattern name / 来源模式名称
 * @param candidate circle packing item candidate / 圆密排货物候选
 * @param axis cylinder axis direction / 圆柱轴向
 * @return source string with optional suffix / 带可选后缀的来源字符串
*/
private fun circlePackingSource(
    pattern: String,
    candidate: CirclePackingItemCandidate,
    axis: Axis3?
): String {
    val axisSuffix = axis?.let { "-axis=${it.name.lowercase()}" } ?: ""
    return circlePackingSource(pattern, candidate) + axisSuffix
}

/**
 * circlePackingVolume.
 * circlePackingVolume。
 * @param placements item placements to sum volume for / 待求和体积的货物放置列表
 * @return total actual volume / 实际总体积
*/
private fun circlePackingVolume(placements: List<QuantityPlacement3<Item, FltX>>): Double {
    return placements.map { placement -> placement.resolvedPackingShape().actualVolume.value.toDouble() }.sum()
}

/**
 * Check if a support view can fully support a horizontal cylinder.
 * 检查支撑视图是否能够完全支撑水平圆柱。
 *
 * @param supportView 支撑视图 / support view
 * @param cylinderView 圆柱视图 / cylinder view
 * @return 是否能够完全支撑 / whether full support is possible
*/
private fun canFullySupportHorizontalCylinder(
    supportView: ItemView,
    cylinderView: ItemView
): Boolean {
    return supportView.width.value.toDouble() + 1e-7 >= cylinderView.width.value.toDouble()
            && supportView.depth.value.toDouble() + 1e-7 >= cylinderView.depth.value.toDouble()
}

/**
 * Generate single hanging support placements for a horizontal cylinder.
 * 为水平圆柱生成单悬挂支撑放置方案。
 *
 * @param supportView 支撑视图 / support view
 * @param cylinderView 圆柱视图 / cylinder view
 * @param axis 圆柱轴向 / cylinder axis
 * @return 放置方案列表，如果无法支撑则返回 null / placement list, or null if unsupported
*/
private fun horizontalCylinderSingleHangingSupportPlacements(
    supportView: ItemView,
    cylinderView: ItemView,
    axis: Axis3
): List<QuantityPlacement3<Item, FltX>>? {
    if (axis == Axis3.Y) {
        return null
    }
    val supportAxisSpanValue = horizontalCylinderSupportAxisSpan(
        view = supportView,
        axis = axis
    ).value.toDouble()
    val cylinderAxisSpanValue = horizontalCylinderSupportAxisSpan(
        view = cylinderView,
        axis = axis
    ).value.toDouble()
    if (supportAxisSpanValue + 1e-7 < cylinderAxisSpanValue) {
        return null
    }

    val supportRadialSpan = horizontalCylinderSupportRadialSpan(
        view = supportView,
        axis = axis
    )
    val cylinderRadialSpan = horizontalCylinderSupportRadialSpan(
        view = cylinderView,
        axis = axis
    )
    val supportRadialSpanValue = supportRadialSpan.value.toDouble()
    val cylinderRadialSpanValue = cylinderRadialSpan.value.toDouble()
    if (supportRadialSpanValue <= 0.0 || supportRadialSpanValue + 1e-7 >= cylinderRadialSpanValue) {
        return null
    }

    val radialOffset = ((cylinderRadialSpan - supportRadialSpan)!! * FltX(0.5))!!
    val supportPosition = when (axis) {
        Axis3.X -> layerPoint3(z = radialOffset)
        Axis3.Y -> point3FltX()
        Axis3.Z -> layerPoint3(x = radialOffset)
    }
    return listOf(
        QuantityPlacement3<Item, FltX>(
            view = supportView,
            position = supportPosition
        ),
        QuantityPlacement3<Item, FltX>(
            view = cylinderView,
            position = layerPoint3(y = supportView.height)
        )
    )
}

/**
 * Calculate the number of repeated hanging supports needed for a horizontal cylinder.
 * 计算水平圆柱所需的重复悬挂支撑数量。
 *
 * @param supportView 支撑视图 / support view
 * @param cylinderView 圆柱视图 / cylinder view
 * @param axis 圆柱轴向 / cylinder axis
 * @return 支撑数量，如果无法支撑则返回 null / support count, or null if unsupported
*/
private fun horizontalCylinderRepeatedHangingSupportCount(
    supportView: ItemView,
    cylinderView: ItemView,
    axis: Axis3
): Int? {
    if (axis == Axis3.Y) {
        return null
    }

    val supportRadialSpan = horizontalCylinderSupportRadialSpan(
        view = supportView,
        axis = axis
    ).value.toDouble()
    val cylinderRadialSpan = horizontalCylinderSupportRadialSpan(
        view = cylinderView,
        axis = axis
    ).value.toDouble()
    if (supportRadialSpan <= 0.0 || supportRadialSpan + 1e-7 >= cylinderRadialSpan) {
        return null
    }

    val supportAxisSpan = horizontalCylinderSupportAxisSpan(
        view = supportView,
        axis = axis
    ).value.toDouble()
    val cylinderAxisSpan = horizontalCylinderSupportAxisSpan(
        view = cylinderView,
        axis = axis
    ).value.toDouble()
    if (supportAxisSpan <= 0.0 || cylinderAxisSpan <= 0.0) {
        return null
    }

    val count = ceil(cylinderAxisSpan / supportAxisSpan).toInt()
    if (count <= 1 || count > 16) {
        return null
    }
    return count
}

/**
 * Generate repeated hanging support placements for a horizontal cylinder with a given count.
 * 为水平圆柱生成指定数量的重复悬挂支撑放置方案。
 *
 * @param supportView 支撑视图 / support view
 * @param cylinderView 圆柱视图 / cylinder view
 * @param supportCount 支撑物数量 / support count
 * @param axis 圆柱轴向 / cylinder axis
 * @return 放置方案列表 / placement list
*/
private fun horizontalCylinderRepeatedHangingSupportPlacements(
    supportView: ItemView,
    cylinderView: ItemView,
    supportCount: Int,
    axis: Axis3
): List<QuantityPlacement3<Item, FltX>> {
    val supportRadialSpan = horizontalCylinderSupportRadialSpan(
        view = supportView,
        axis = axis
    )
    val cylinderRadialSpan = horizontalCylinderSupportRadialSpan(
        view = cylinderView,
        axis = axis
    )
    val supportAxisSpan = horizontalCylinderSupportAxisSpan(
        view = supportView,
        axis = axis
    )
    val radialOffset = ((cylinderRadialSpan - supportRadialSpan)!! * FltX(0.5))!!
    val placements = ArrayList<QuantityPlacement3<Item, FltX>>(supportCount + 1)
    for (index in 0 until supportCount) {
        val axisOffset = supportAxisSpan * FltX(index.toDouble())
        val position = when (axis) {
            Axis3.X -> layerPoint3(x = axisOffset, z = radialOffset)
            Axis3.Y -> point3FltX()
            Axis3.Z -> layerPoint3(x = radialOffset, z = axisOffset)
        }
        placements.add(
            QuantityPlacement3<Item, FltX>(
                view = supportView,
                position = position
            )
        )
    }
    placements.add(
        QuantityPlacement3<Item, FltX>(
            view = cylinderView,
            position = layerPoint3(y = supportView.height)
        )
    )
    return placements
}

/**
 * Generate repeated hanging support placements from a list of support views for a horizontal cylinder.
 * 从支撑视图列表中为水平圆柱生成重复悬挂支撑放置方案。
 *
 * @param supportViews 支撑视图列表 / support view list
 * @param cylinderView 圆柱视图 / cylinder view
 * @param axis 圆柱轴向 / cylinder axis
 * @return 放置方案列表，如果无法支撑则返回 null / placement list, or null if unsupported
*/
private fun horizontalCylinderRepeatedHangingSupportPlacements(
    supportViews: List<ItemView>,
    cylinderView: ItemView,
    axis: Axis3
): List<QuantityPlacement3<Item, FltX>>? {
    if (axis == Axis3.Y) {
        return null
    }

    val cylinderAxisSpan = horizontalCylinderSupportAxisSpan(
        view = cylinderView,
        axis = axis
    )
    val cylinderRadialSpan = horizontalCylinderSupportRadialSpan(
        view = cylinderView,
        axis = axis
    )
    val cylinderAxisSpanValue = cylinderAxisSpan.value.toDouble()
    val cylinderRadialSpanValue = cylinderRadialSpan.value.toDouble()
    if (cylinderAxisSpanValue <= 0.0 || cylinderRadialSpanValue <= 0.0) {
        return null
    }

    for (heightReference in supportViews) {
        val heightValue = heightReference.height.value.toDouble()
        val referenceAxisSpan = horizontalCylinderSupportAxisSpan(
            view = heightReference,
            axis = axis
        )
        val referenceRadialSpan = horizontalCylinderSupportRadialSpan(
            view = heightReference,
            axis = axis
        )
        val referenceAxisSpanValue = referenceAxisSpan.value.toDouble()
        val referenceRadialSpanValue = referenceRadialSpan.value.toDouble()
        if (referenceAxisSpanValue <= 0.0
            || referenceRadialSpanValue <= 0.0
            || referenceRadialSpanValue + 1e-7 >= cylinderRadialSpanValue
        ) {
            continue
        }

        val supportCount = ceil(cylinderAxisSpanValue / referenceAxisSpanValue).toInt()
        if (supportCount <= 1 || supportCount > 16) {
            continue
        }
        val eligibleSupports = supportViews
            .filter { supportView ->
                val supportAxisSpanValue = horizontalCylinderSupportAxisSpan(
                    view = supportView,
                    axis = axis
                ).value.toDouble()
                val supportRadialSpanValue = horizontalCylinderSupportRadialSpan(
                    view = supportView,
                    axis = axis
                ).value.toDouble()
                abs(supportView.height.value.toDouble() - heightValue) <= 1e-7
                        && abs(supportAxisSpanValue - referenceAxisSpanValue) <= 1e-7
                        && abs(supportRadialSpanValue - referenceRadialSpanValue) <= 1e-7
            }
            .distinctBy { supportView -> supportView.unit }
        if (eligibleSupports.size < supportCount) {
            continue
        }

        val radialOffset = ((cylinderRadialSpan - referenceRadialSpan)!! * FltX(0.5))!!
        val placements = ArrayList<QuantityPlacement3<Item, FltX>>(supportCount + 1)
        var axisOffset = (cylinderAxisSpan * FltX(0.0))!!
        for (supportView in eligibleSupports.take(supportCount)) {
            val position = when (axis) {
                Axis3.X -> layerPoint3(x = axisOffset, z = radialOffset)
                Axis3.Y -> point3FltX()
                Axis3.Z -> layerPoint3(x = radialOffset, z = axisOffset)
            }
            placements.add(
                QuantityPlacement3<Item, FltX>(
                    view = supportView,
                    position = position
                )
            )
            axisOffset = (axisOffset + referenceAxisSpan)!!
        }
        placements.add(
            QuantityPlacement3<Item, FltX>(
                view = cylinderView,
                position = layerPoint3(y = heightReference.height)
            )
        )
        return placements
    }
    return null
}

/**
 * Get the axis-span length of a view for horizontal cylinder support calculation.
 * 获取水平圆柱支撑计算中视图的轴方向跨度长度。
 *
 * @param view 货物视图 / item view
 * @param axis 轴向 / axis
 * @return 轴方向跨度 / axis span length
*/
private fun horizontalCylinderSupportAxisSpan(
    view: ItemView,
    axis: Axis3
): Quantity<FltX> {
    return when (axis) {
        Axis3.X -> view.width
        Axis3.Y -> view.height
        Axis3.Z -> view.depth
    }
}

/**
 * Get the radial-span length of a view for horizontal cylinder support calculation.
 * 获取水平圆柱支撑计算中视图的径向跨度长度。
 *
 * @param view 货物视图 / item view
 * @param axis 圆柱轴向 / cylinder axis
 * @return 径向跨度 / radial span length
*/
private fun horizontalCylinderSupportRadialSpan(
    view: ItemView,
    axis: Axis3
): Quantity<FltX> {
    return when (axis) {
        Axis3.X -> view.depth
        Axis3.Y -> view.height
        Axis3.Z -> view.width
    }
}

/**
 * Calculate the number of repeated supports needed for a horizontal cylinder.
 * 计算水平圆柱所需的重复支撑数量。
 *
 * @param supportView 支撑视图 / support view
 * @param cylinderView 圆柱视图 / cylinder view
 * @param axis 圆柱轴向 / cylinder axis
 * @return 支撑数量，如果无法支撑则返回 null / support count, or null if unsupported
*/
private fun horizontalCylinderRepeatedSupportCount(
    supportView: ItemView,
    cylinderView: ItemView,
    axis: Axis3
): Int? {
    val supportRadialSpan = horizontalCylinderSupportRadialSpan(
        view = supportView,
        axis = axis
    ).value.toDouble()
    val cylinderRadialSpan = horizontalCylinderSupportRadialSpan(
        view = cylinderView,
        axis = axis
    ).value.toDouble()
    if (supportRadialSpan + 1e-7 < cylinderRadialSpan) {
        return null
    }

    val supportAxisSpan = horizontalCylinderSupportAxisSpan(
        view = supportView,
        axis = axis
    ).value.toDouble()
    val cylinderAxisSpan = horizontalCylinderSupportAxisSpan(
        view = cylinderView,
        axis = axis
    ).value.toDouble()
    if (supportAxisSpan <= 0.0 || cylinderAxisSpan <= 0.0) {
        return null
    }

    val count = ceil(cylinderAxisSpan / supportAxisSpan).toInt()
    if (count <= 1 || count > 16) {
        return null
    }
    return count
}

/**
 * Generate repeated support placements for horizontal cylinder.
 * 为水平圆柱生成重复支撑放置方案。
 *
 * @param supportView 支撑视图 / support view
 * @param cylinderView 圆柱视图 / cylinder view
 * @param supportCount 支撑物数量 / support count
 * @param axis 圆柱轴向 / cylinder axis
 * @return 放置方案列表 / placement list
*/
private fun horizontalCylinderRepeatedSupportPlacements(
    supportView: ItemView,
    cylinderView: ItemView,
    supportCount: Int,
    axis: Axis3
): List<QuantityPlacement3<Item, FltX>> {
    val placements = ArrayList<QuantityPlacement3<Item, FltX>>(supportCount + 1)
    val supportAxisSpan = horizontalCylinderSupportAxisSpan(
        view = supportView,
        axis = axis
    )
    for (index in 0 until supportCount) {
        val axisOffset = supportAxisSpan * FltX(index.toDouble())
        val position = when (axis) {
            Axis3.X -> layerPoint3(x = axisOffset)
            Axis3.Y -> point3FltX()
            Axis3.Z -> layerPoint3(z = axisOffset)
        }
        placements.add(
            QuantityPlacement3<Item, FltX>(
                view = supportView,
                position = position
            )
        )
    }
    placements.add(
        QuantityPlacement3<Item, FltX>(
            view = cylinderView,
            position = layerPoint3(y = supportView.height)
        )
    )
    return placements
}

/**
 * Generate heterogeneous support placements for horizontal cylinder.
 * 为水平圆柱生成异构支撑放置方案。
 *
 * @param supportViews 支撑视图列表 / support view list
 * @param cylinderView 圆柱视图 / cylinder view
 * @param axis 圆柱轴向 / cylinder axis
 * @return 放置方案列表，如果无法支撑则返回 null / placement list, or null if unsupported
*/
private fun horizontalCylinderHeterogeneousSupportPlacements(
    supportViews: List<ItemView>,
    cylinderView: ItemView,
    axis: Axis3
): List<QuantityPlacement3<Item, FltX>>? {
    val cylinderAxisSpan = horizontalCylinderSupportAxisSpan(
        view = cylinderView,
        axis = axis
    )
    val cylinderAxisSpanValue = cylinderAxisSpan.value.toDouble()
    if (cylinderAxisSpanValue <= 0.0) {
        return null
    }
    val cylinderRadialSpanValue = horizontalCylinderSupportRadialSpan(
        view = cylinderView,
        axis = axis
    ).value.toDouble()

    for (heightReference in supportViews) {
        val heightValue = heightReference.height.value.toDouble()
        val eligibleSupports = supportViews
            .filter { supportView ->
                val supportAxisSpanValue = horizontalCylinderSupportAxisSpan(
                    view = supportView,
                    axis = axis
                ).value.toDouble()
                val supportRadialSpanValue = horizontalCylinderSupportRadialSpan(
                    view = supportView,
                    axis = axis
                ).value.toDouble()
                !canFullySupportHorizontalCylinder(
                    supportView = supportView,
                    cylinderView = cylinderView
                )
                        && abs(supportView.height.value.toDouble() - heightValue) <= 1e-7
                        && supportAxisSpanValue > 0.0
                        && supportRadialSpanValue + 1e-7 >= cylinderRadialSpanValue
            }
            .distinctBy { supportView -> supportView.unit }
            .sortedByDescending { supportView ->
                horizontalCylinderSupportAxisSpan(
                    view = supportView,
                    axis = axis
                ).value.toDouble()
            }
        if (eligibleSupports.size < 2) {
            continue
        }

        val supportPlacements = ArrayList<QuantityPlacement3<Item, FltX>>()
        var coveredAxisSpan = 0.0
        var axisOffset = (cylinderAxisSpan * FltX(0.0))!!
        for (supportView in eligibleSupports) {
            val supportAxisSpan = horizontalCylinderSupportAxisSpan(
                view = supportView,
                axis = axis
            )
            val position = when (axis) {
                Axis3.X -> layerPoint3(x = axisOffset)
                Axis3.Y -> point3FltX()
                Axis3.Z -> layerPoint3(z = axisOffset)
            }
            supportPlacements.add(
                QuantityPlacement3<Item, FltX>(
                    view = supportView,
                    position = position
                )
            )
            coveredAxisSpan += supportAxisSpan.value.toDouble()
            axisOffset = (axisOffset + supportAxisSpan)!!
            if (coveredAxisSpan + 1e-7 >= cylinderAxisSpanValue) {
                supportPlacements.add(
                    QuantityPlacement3<Item, FltX>(
                        view = cylinderView,
                        position = layerPoint3(y = heightReference.height)
                    )
                )
                return supportPlacements
            }
        }
    }
    return null
}

/**
 * Validate geometry validity for stacked circle packing layer.
 * 验证圆密排堆叠层的几何合法性。
 *
 * @param binShape 容器形状 / bin container shape
 * @param placements 放置物列表 / placement list
 * @return 几何是否有效 / whether the geometry is valid
*/
private fun circlePackingStackedLayerIsGeometryValid(
    binShape: Container3Shape,
    placements: List<QuantityPlacement3<Item, FltX>>
): Boolean {
    val shapePlacements = placements.map { placement ->
        val shape = placement.resolvedPackingShape()
        if (!binShape.enabled(shape, placement.absolutePosition)) {
            return false
        }
        placement.asShapePlacement3 { shape }
    }
    for (lhsIndex in shapePlacements.indices) {
        for (rhsIndex in (lhsIndex + 1) until shapePlacements.size) {
            if (shapePlacements[lhsIndex].overlapped(shapePlacements[rhsIndex]).value == true) {
                return false
            }
        }
    }
    return true
}

/**
 * Generate supported stack candidates for horizontal cylinder items.
 * 为水平圆柱货物生成支撑堆叠候选层。
 *
 * @param sourceClass 源类 / source class
 * @param iteration 迭代次数 / iteration number
 * @param bin 目标箱型 / target bin type
 * @param binShape 容器形状 / bin container shape
 * @param items 所有货物列表 / all item list
 * @param cylinderItem 圆柱货物 / cylinder item
 * @param cylinderCandidate 圆柱候选 / cylinder candidate
 * @param cylinderShape 圆柱形状 / cylinder shape
 * @return 候选层列表 / candidate layer list
*/
private suspend fun horizontalCylinderSupportedStackCandidates(
    sourceClass: Class<*>,
    iteration: Int64,
    bin: BinType<FltX>,
    binShape: Container3Shape,
    items: List<Item>,
    cylinderItem: Item,
    cylinderCandidate: CirclePackingItemCandidate,
    cylinderShape: CylinderPackingShape3
): List<CirclePackingLayerCandidate> {
    val candidates = ArrayList<CirclePackingLayerCandidate>()
    val supportCandidates = items
        .filter { supportItem ->
            supportItem != cylinderItem && supportItem.packingShape !is CylinderPackingShape3
        }
        .flatMap { supportItem -> circlePackingItemCandidates(supportItem, bin) }
    val distinctRepeatedHangingSupportPlacements = horizontalCylinderRepeatedHangingSupportPlacements(
        supportViews = supportCandidates.map { supportCandidate -> supportCandidate.view },
        cylinderView = cylinderCandidate.view,
        axis = cylinderShape.axis
    )
    if (distinctRepeatedHangingSupportPlacements != null
        && circlePackingStackedLayerIsGeometryValid(
            binShape = binShape,
            placements = distinctRepeatedHangingSupportPlacements
        )
    ) {
        val distinctRepeatedHangingCylinderPlacement = distinctRepeatedHangingSupportPlacements.last()
        if (distinctRepeatedHangingCylinderPlacement.enabledStackingOn(
                bottomItems = distinctRepeatedHangingSupportPlacements.dropLast(1),
                space = binShape
            )
        ) {
            candidates.add(
                CirclePackingLayerCandidate(
                    layer = BinLayer(
                        iteration = iteration,
                        from = sourceClass.kotlin,
                        bin = bin,
                        shape = binShape,
                        units = distinctRepeatedHangingSupportPlacements
                    ),
                    source = circlePackingSource(
                        pattern = "circle-packing-horizontal-hanging-support-multi",
                        candidate = cylinderCandidate,
                        axis = cylinderShape.axis
                    ),
                    packed = distinctRepeatedHangingSupportPlacements.size,
                    volume = circlePackingVolume(distinctRepeatedHangingSupportPlacements)
                )
            )
        }
    }
    for (supportCandidate in supportCandidates) {
        val supportView = supportCandidate.view
        if (canFullySupportHorizontalCylinder(
                supportView = supportView,
                cylinderView = cylinderCandidate.view
            )
        ) {
            val supportPlacement = QuantityPlacement3<Item, FltX>(
                view = supportView,
                position = point3FltX()
            )
            val cylinderPlacement = QuantityPlacement3<Item, FltX>(
                view = cylinderCandidate.view,
                position = layerPoint3(y = supportView.height)
            )
            val placements = listOf(supportPlacement, cylinderPlacement)
            if (circlePackingStackedLayerIsGeometryValid(
                    binShape = binShape,
                    placements = placements
                )
                && cylinderPlacement.enabledStackingOn(
                    bottomItems = listOf(supportPlacement),
                    space = binShape
                )
            ) {
                candidates.add(
                    CirclePackingLayerCandidate(
                        layer = BinLayer(
                            iteration = iteration,
                            from = sourceClass.kotlin,
                            bin = bin,
                            shape = binShape,
                            units = placements
                        ),
                        source = circlePackingSource(
                            pattern = "circle-packing-horizontal-supported-stack",
                            candidate = cylinderCandidate,
                            axis = cylinderShape.axis
                        ),
                        packed = placements.size,
                        volume = circlePackingVolume(placements)
                    )
                )
            }
        }

        val hangingSupportPlacements = horizontalCylinderSingleHangingSupportPlacements(
            supportView = supportView,
            cylinderView = cylinderCandidate.view,
            axis = cylinderShape.axis
        )
        if (hangingSupportPlacements != null
            && circlePackingStackedLayerIsGeometryValid(
                binShape = binShape,
                placements = hangingSupportPlacements
            )
        ) {
            val hangingCylinderPlacement = hangingSupportPlacements.last()
            if (hangingCylinderPlacement.enabledStackingOn(
                    bottomItems = hangingSupportPlacements.dropLast(1),
                    space = binShape
                )
            ) {
                candidates.add(
                    CirclePackingLayerCandidate(
                        layer = BinLayer(
                            iteration = iteration,
                            from = sourceClass.kotlin,
                            bin = bin,
                            shape = binShape,
                            units = hangingSupportPlacements
                        ),
                        source = circlePackingSource(
                            pattern = "circle-packing-horizontal-hanging-support",
                            candidate = cylinderCandidate,
                            axis = cylinderShape.axis
                        ),
                        packed = hangingSupportPlacements.size,
                        volume = circlePackingVolume(hangingSupportPlacements)
                    )
                )
            }
        }

        val repeatedHangingSupportCount = horizontalCylinderRepeatedHangingSupportCount(
            supportView = supportView,
            cylinderView = cylinderCandidate.view,
            axis = cylinderShape.axis
        )
        if (repeatedHangingSupportCount != null) {
            val multiHangingSupportPlacements = horizontalCylinderRepeatedHangingSupportPlacements(
                supportView = supportView,
                cylinderView = cylinderCandidate.view,
                supportCount = repeatedHangingSupportCount,
                axis = cylinderShape.axis
            )
            if (circlePackingStackedLayerIsGeometryValid(
                    binShape = binShape,
                    placements = multiHangingSupportPlacements
                )
            ) {
                val multiHangingCylinderPlacement = multiHangingSupportPlacements.last()
                if (multiHangingCylinderPlacement.enabledStackingOn(
                        bottomItems = multiHangingSupportPlacements.dropLast(1),
                        space = binShape
                    )
                ) {
                    candidates.add(
                        CirclePackingLayerCandidate(
                            layer = BinLayer(
                                iteration = iteration,
                                from = sourceClass.kotlin,
                                bin = bin,
                                shape = binShape,
                                units = multiHangingSupportPlacements
                            ),
                            source = circlePackingSource(
                                pattern = "circle-packing-horizontal-hanging-support-multi",
                                candidate = cylinderCandidate,
                                axis = cylinderShape.axis
                            ),
                            packed = multiHangingSupportPlacements.size,
                            volume = circlePackingVolume(multiHangingSupportPlacements)
                        )
                    )
                }
            }
        }

        val repeatedSupportCount = horizontalCylinderRepeatedSupportCount(
            supportView = supportView,
            cylinderView = cylinderCandidate.view,
            axis = cylinderShape.axis
        ) ?: continue
        val multiSupportPlacements = horizontalCylinderRepeatedSupportPlacements(
            supportView = supportView,
            cylinderView = cylinderCandidate.view,
            supportCount = repeatedSupportCount,
            axis = cylinderShape.axis
        )
        if (!circlePackingStackedLayerIsGeometryValid(
                binShape = binShape,
                placements = multiSupportPlacements
            )
        ) {
            continue
        }
        val multiSupportCylinderPlacement = multiSupportPlacements.last()
        if (!multiSupportCylinderPlacement.enabledStackingOn(
                bottomItems = multiSupportPlacements.dropLast(1),
                space = binShape
            )
        ) {
            continue
        }
        candidates.add(
            CirclePackingLayerCandidate(
                layer = BinLayer(
                    iteration = iteration,
                    from = sourceClass.kotlin,
                    bin = bin,
                    shape = binShape,
                    units = multiSupportPlacements
                ),
                source = circlePackingSource(
                    pattern = "circle-packing-horizontal-supported-stack-multi",
                    candidate = cylinderCandidate,
                    axis = cylinderShape.axis
                ),
                packed = multiSupportPlacements.size,
                volume = circlePackingVolume(multiSupportPlacements)
            )
        )
    }

    val heterogeneousSupportPlacements = horizontalCylinderHeterogeneousSupportPlacements(
        supportViews = supportCandidates.map { supportCandidate -> supportCandidate.view },
        cylinderView = cylinderCandidate.view,
        axis = cylinderShape.axis
    )
    if (heterogeneousSupportPlacements != null
        && circlePackingStackedLayerIsGeometryValid(
            binShape = binShape,
            placements = heterogeneousSupportPlacements
        )
    ) {
        val heterogeneousCylinderPlacement = heterogeneousSupportPlacements.last()
        if (heterogeneousCylinderPlacement.enabledStackingOn(
                bottomItems = heterogeneousSupportPlacements.dropLast(1),
                space = binShape
            )
        ) {
            candidates.add(
                CirclePackingLayerCandidate(
                    layer = BinLayer(
                        iteration = iteration,
                        from = sourceClass.kotlin,
                        bin = bin,
                        shape = binShape,
                        units = heterogeneousSupportPlacements
                    ),
                    source = circlePackingSource(
                        pattern = "circle-packing-horizontal-supported-stack-heterogeneous",
                        candidate = cylinderCandidate,
                        axis = cylinderShape.axis
                    ),
                    packed = heterogeneousSupportPlacements.size,
                    volume = circlePackingVolume(heterogeneousSupportPlacements)
                )
            )
        }
    }
    return candidates
}

private suspend fun <V> mapItemsToCirclePackingLayers(
    request: Bpp3dLayerGenerationRequest<V>,
    sourceClass: Class<*>
): List<Bpp3dLayerGenerationResult<V>> {
    val bin = request.bin ?: return emptyList()
    val iteration = Int64(request.iteration.toLong())
    val binShape = Container3Shape(bin.asContainer3Shape())
    val binWidth = bin.width.value.toDouble()
    val binDepth = bin.depth.value.toDouble()
    if (binWidth <= 0.0 || binDepth <= 0.0) {
        return emptyList()
    }
    for (item in request.items) {
        requireConcreteCirclePackingRadiusMetadata(item)
    }

    val candidates = ArrayList<CirclePackingLayerCandidate>()
    val items = request.items
        .filter { item ->
            item.packageType.category != fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageCategory.Pallet
        }
        .distinct()
    for (item in items) {
        requireConcreteCirclePackingRadiusMetadata(item)
        for (candidate in circlePackingItemCandidates(item, request.bin)) {
            val itemView = candidate.view
            val candidateShape = itemView.placementPackingShape as? CylinderPackingShape3
            if (candidateShape != null && candidateShape.axis != Axis3.Y) {
                val cellWidth = itemView.width
                val cellDepth = itemView.depth
                val cellWidthValue = cellWidth.value.toDouble()
                val cellDepthValue = cellDepth.value.toDouble()
                if (cellWidthValue <= 0.0
                    || cellDepthValue <= 0.0
                    || cellWidthValue > binWidth
                    || cellDepthValue > binDepth
                ) {
                    continue
                }

                val cols = floor(binWidth / cellWidthValue).toInt()
                val rows = floor(binDepth / cellDepthValue).toInt()
                if (cols <= 0 || rows <= 0) {
                    continue
                }
                val placements = ArrayList<QuantityPlacement3<Item, FltX>>(cols * rows)
                for (row in 0 until rows) {
                    val z = cellDepth * FltX(row.toDouble())
                    for (col in 0 until cols) {
                        val x = cellWidth * FltX(col.toDouble())
                        placements.add(
                            QuantityPlacement3<Item, FltX>(
                                view = itemView,
                                position = layerPoint3(x = x, z = z)
                            )
                        )
                    }
                }
                if (placements.size > 1 && circlePackingLayerIsGeometryValid(binShape, placements)) {
                    candidates.add(
                        CirclePackingLayerCandidate(
                            layer = BinLayer(
                                iteration = iteration,
                                from = sourceClass.kotlin,
                                bin = bin,
                                shape = binShape,
                                units = placements
                            ),
                            source = circlePackingSource(
                                pattern = "circle-packing-horizontal-grid",
                                candidate = candidate,
                                axis = candidateShape.axis
                            ),
                            packed = placements.size,
                            volume = circlePackingVolume(placements)
                        )
                    )
                }
                if (placements.isNotEmpty()) {
                    val singlePlacement = listOf(placements.first())
                    if (circlePackingLayerIsGeometryValid(binShape, singlePlacement)) {
                        candidates.add(
                            CirclePackingLayerCandidate(
                                layer = BinLayer(
                                    iteration = iteration,
                                    from = sourceClass.kotlin,
                                    bin = bin,
                                    shape = binShape,
                                    units = singlePlacement
                                ),
                                source = circlePackingSource(
                                    pattern = "circle-packing-horizontal-grid-single",
                                    candidate = candidate,
                                    axis = candidateShape.axis
                                ),
                                packed = singlePlacement.size,
                                volume = circlePackingVolume(singlePlacement)
                            )
                        )
                    }
                }
                candidates.addAll(
                    horizontalCylinderSupportedStackCandidates(
                        sourceClass = sourceClass,
                        iteration = iteration,
                        bin = bin,
                        binShape = binShape,
                        items = items,
                        cylinderItem = item,
                        cylinderCandidate = candidate,
                        cylinderShape = candidateShape
                    )
                )
                continue
            }

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
                val placements = ArrayList<QuantityPlacement3<Item, FltX>>(rectCols * rectRows)
                for (row in 0 until rectRows) {
                    val z = diameter * FltX(row.toDouble())
                    for (col in 0 until rectCols) {
                        val x = diameter * FltX(col.toDouble())
                        placements.add(
                            QuantityPlacement3<Item, FltX>(
                                view = itemView,
                                position = layerPoint3(x = x, z = z)
                            )
                        )
                    }
                }
                if (placements.isNotEmpty() && circlePackingLayerIsGeometryValid(binShape, placements)) {
                    candidates.add(
                        CirclePackingLayerCandidate(
                            layer = BinLayer(
                                iteration = iteration,
                                from = sourceClass.kotlin,
                                bin = bin,
                                shape = binShape,
                                units = placements
                            ),
                            source = circlePackingSource("circle-packing-rect", candidate),
                            packed = placements.size,
                            volume = circlePackingVolume(placements)
                        )
                    )
                }
            }

            val hexRowStepScale = sqrt(3.0) / 2.0
            val hexRowStep = (diameter * FltX(hexRowStepScale))!!
            val hexRowStepValue = hexRowStep.value.toDouble()
            if (hexRowStepValue > 0.0) {
                val placements = ArrayList<QuantityPlacement3<Item, FltX>>()
                var row = 0
                while (true) {
                    val z = (hexRowStep * FltX(row.toDouble()))!!
                    if (z.value.toDouble() + diameterValue > binDepth) {
                        break
                    }
                    val offset = if (row % 2 == 0) 0.0 else 0.5
                    var col = 0
                    while (true) {
                        val xScale = col.toDouble() + offset
                        val x = (diameter * FltX(xScale))!!
                        if (x.value.toDouble() + diameterValue > binWidth) {
                            break
                        }
                        placements.add(
                            QuantityPlacement3<Item, FltX>(
                                view = itemView,
                                position = layerPoint3(x = x, z = z)
                            )
                        )
                        col += 1
                    }
                    row += 1
                }
                if (placements.isNotEmpty() && circlePackingLayerIsGeometryValid(binShape, placements)) {
                    candidates.add(
                        CirclePackingLayerCandidate(
                            layer = BinLayer(
                                iteration = iteration,
                                from = sourceClass.kotlin,
                                bin = bin,
                                shape = binShape,
                                units = placements
                            ),
                            source = circlePackingSource("circle-packing-hex", candidate),
                            packed = placements.size,
                            volume = circlePackingVolume(placements)
                        )
                    )
                }
            }
        }
    }

    val ranked = candidates
        .sortedWith(
            compareByDescending<CirclePackingLayerCandidate> { it.packed }
                .thenByDescending { it.volume }
                .thenByDescending { it.source.startsWith("circle-packing-hex") }
        )
        .take(request.maxCandidates)
        .map { candidate ->
            Bpp3dLayerGenerationResult<V>(
                layer = candidate.layer,
                numericScore = FltX(candidate.packed.toDouble()),
                source = candidate.source
            )
        }
    return rankByShadowScore(request, ranked)
}

/**
 * Validate geometry for circle packing layer placements.
 * 验证圆密排层中放置物的几何合法性。
 *
 * @param binShape 容器形状 / bin container shape
 * @param placements 放置物列表 / placement list
 * @return 几何是否有效 / whether the geometry is valid
*/
private fun circlePackingLayerIsGeometryValid(
    binShape: Container3Shape,
    placements: List<QuantityPlacement3<Item, FltX>>
): Boolean {
    val shapePlacements = placements.map { placement ->
        val shape = placement.resolvedPackingShape()
        if (!binShape.enabled(shape, placement.absolutePosition)) {
            return false
        }
        placement.asShapePlacement3 { shape }
    }
    for (lhsIndex in shapePlacements.indices) {
        for (rhsIndex in (lhsIndex + 1) until shapePlacements.size) {
            val overlap = shapePlacements[lhsIndex].footprintOverlapArea(shapePlacements[rhsIndex])
            if (overlap.toDouble() > 1e-7) {
                return false
            }
        }
    }
    return true
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
            for (item in it.items) {
                requireConcreteCirclePackingRadiusMetadata(item)
                requireAxisAwareCylinderCandidate(
                    shape = item.packingShape,
                    path = CylinderCapabilityPath.CirclePackingCandidate
                )!!
            }
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
        val selectedGenerators = if (request.items.any { item ->
                val shape = item.packingShape
                shape is CylinderPackingShape3 && shape.axis != Axis3.Y
            }
        ) {
            generators.filter { generator ->
                generator is CirclePackingLayerGenerator<*> || generator is HistoricalLayerGenerator<*>
            }
        } else {
            generators
        }
        val all = ArrayList<Bpp3dLayerGenerationResult<V>>()
        for (generator in selectedGenerators) {
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
