/**
 * 列生成装箱分析器。
 * Column generation packing analyzer.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.SchemaDTO
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.DemandModeKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service.*

/**
 * 列生成装箱快照。
 * Column generation packing snapshot.
 *
 * @property bins 箱子列表 / bin list
 * @property packingResult 装箱结果 / packing result
 * @property schema 渲染方案 / rendering schema
 * @property demandModeShadowPriceTotals 需求模式影子价格总和 / demand mode shadow price totals
 * @property demandModeShadowPriceEntryCounts 需求模式影子价格条目数 / demand mode shadow price entry counts
 */
data class ColumnGenerationPackingSnapshot(
    val bins: List<Bin<BinLayer, FltX>>,
    val packingResult: PackingResult,
    val schema: SchemaDTO,
    val demandModeShadowPriceTotals: Map<String, FltX> = emptyMap(),
    val demandModeShadowPriceEntryCounts: Map<String, Int> = emptyMap()
)

/**
 * 需求模式标签。
 * Demand mode tag.
 *
 * @param mode 需求模式 / demand mode
 * @return 模式对应的标签字符串 / tag string corresponding to the mode
 */
private fun demandModeTag(mode: Bpp3dDemandMode): String {
    return when (mode) {
        is Bpp3dDemandMode.Item -> "item"
        is Bpp3dDemandMode.Material -> "material"
        is Bpp3dDemandMode.ItemAmount -> "item_amount"
        is Bpp3dDemandMode.ItemWeight -> "item_weight"
        is Bpp3dDemandMode.ItemMaterialAmount -> "material_amount"
        is Bpp3dDemandMode.ItemMaterialWeight -> "material_weight"
    }
}

/**
 * 为量纲已知坐标层构造分析用箱型。
 * Build an analysis bin type for quantity known-coordinate layers.
 *
 * @param layer 量纲层转换后的模型层 / model layer converted from a quantity layer
 * @param index 层序号 / layer index
 * @return 分析用箱型 / analysis bin type
 */
private fun knownCoordinateBinType(layer: BinLayer, index: Int): BinType<FltX> {
    return BinType(
        width = layer.shape.width,
        height = layer.shape.height,
        depth = layer.shape.depth,
        capacity = FltX.maximum * Kilogram,
        longitudinalBalance = null,
        lateralBalance = null,
        typeCode = binTypeIdOf("QUANTITY-KNOWN-COORDINATE-${index + 1}")
    )
}

/**
 * 列生成装箱分析器，在每次迭代后执行装箱分析。
 * Column generation packing analyzer, performs packing analysis after each iteration.
 *
 * @property packer 装箱器 / packer
 * @property rendererAdapter 渲染适配器 / renderer adapter
 * @property contextBuilder 上下文构建器 / context builder
 */
class ColumnGenerationPackingAnalyzer(
    private val packer: Packer = Packer(),
    private val rendererAdapter: PackingRendererAdapter = PackingRendererAdapter(),
    private val contextBuilder: (ColumnGenerationState<*>) -> PackingContext = { state ->
        PackingContext(info = mapOf("cg_iteration" to state.iteration.toString()))
    }
) : ColumnGenerationSolutionAnalyzer<FltX> {
    /**
     * 最近一次分析的装箱快照。
     * Latest packing snapshot from the most recent analysis.
     */
    var latest: ColumnGenerationPackingSnapshot? = null
        private set

    /**
     * 分析当前列生成状态并生成装箱快照。
     * Analyze current column generation state and generate packing snapshot.
     *
     * @param state 列生成状态 / column generation state
     */
    override suspend fun analyze(state: ColumnGenerationState<FltX>): Try {
        val bins: List<Bin<BinLayer, FltX>> = if (state.bins.isNotEmpty()) {
            state.bins
        } else {
            state.columns.mapNotNull { layer ->
                val bin = layer.bin ?: return@mapNotNull null
                val placement = when (val placementResult = layer.copy().toLayerPlacement()) {
                    is Ok -> placementResult.value
                    is Failed -> return Failed(placementResult.error)
                    is Fatal -> return Fatal(placementResult.errors)
                }
                layerBinOf(
                    shape = bin,
                    units = listOf(placement)
                )
            }
        }
        val packingResult = when (val result = packer.invoke(
            bins = bins,
            context = contextBuilder(state)
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val continuousRadiusSelectionResults = buildNativeContinuousRadiusSelectionResults(
            prototypes = state.continuousRadiusSolverPrototypes,
            solverResults = state.continuousRadiusSolverResults
        )
        // Build PWL continuous-radius selection results from opaque Map (if any).
        val pwlSelectionResults = buildPWLContinuousRadiusSelectionResults(
            prototypes = state.continuousRadiusSolverPrototypes,
            pwlContinuousRadiusResults = state.pwlContinuousRadiusResults
        )
        val allContinuousRadiusSelectionResults = continuousRadiusSelectionResults + pwlSelectionResults
        val schema = when (val result = rendererAdapter.toSchema(packingResult, allContinuousRadiusSelectionResults)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val demandModeShadowPriceTotals = LinkedHashMap<String, FltX>()
        val demandModeShadowPriceEntryCounts = LinkedHashMap<String, Int>()
        for ((key, value) in state.shadowPrices) {
            val modeTag = demandModeTag(key.mode)
            demandModeShadowPriceTotals[modeTag] = (demandModeShadowPriceTotals[modeTag] ?: FltX.zero) + value
            demandModeShadowPriceEntryCounts[modeTag] = (demandModeShadowPriceEntryCounts[modeTag] ?: 0) + 1
        }
        val schemaKpi = LinkedHashMap(schema.kpi)
        schemaKpi["continuous_radius_solver_prototype_count"] = state.continuousRadiusSolverPrototypes.size.toString()
        schemaKpi["continuous_radius_solver_prototype_variables"] = state.continuousRadiusSolverPrototypes.joinToString("|") { it.variableName }
        schemaKpi.putAll(
            continuousRadiusSolverVariableRegistrationPlan(
                prototypes = state.continuousRadiusSolverPrototypes
            ).info()
        )
        for ((variableName, solverValue) in state.continuousRadiusSolverResults) {
            schemaKpi["continuous_radius_solver_selected_$variableName"] = solverValue.toString()
        }
        for ((mode, total) in demandModeShadowPriceTotals) {
            schemaKpi["shadow_price_mode_${mode}_total"] = total.toString()
        }
        for ((mode, count) in demandModeShadowPriceEntryCounts) {
            schemaKpi["shadow_price_mode_${mode}_entry_count"] = count.toString()
        }
        latest = ColumnGenerationPackingSnapshot(
            bins = bins,
            packingResult = packingResult,
            schema = schema.copy(kpi = schemaKpi),
            demandModeShadowPriceTotals = demandModeShadowPriceTotals,
            demandModeShadowPriceEntryCounts = demandModeShadowPriceEntryCounts
        )
        return ok
    }
}

/**
 * 使用量纲层列表执行装箱分析。
 * Execute packing analysis with quantity layer list.
 *
 * @param T 量纲数值类型 / quantity numeric type
 * @param iteration 迭代序号 / iteration number
 * @param columns 量纲层列表 / quantity layer list
 * @param bins 最终箱子（可选） / final bins (optional)
 * @param depthBoundaryLayerOrientationPolicy 深度边界层轴向/朝向策略 / depth boundary layer axis/orientation policy
 * @param shadowPrices 影子价格（可选） / shadow prices (optional)
 * @param materialCache 物料缓存 / material cache
 * @param itemCache 货物缓存 / item cache
 */
suspend fun <T : FloatingNumber<T>> ColumnGenerationPackingAnalyzer.analyzeFromQuantity(
    iteration: Int,
    columns: List<QuantityBinLayer<T>>,
    bins: List<Bin<BinLayer, FltX>> = emptyList(),
    depthBoundaryLayerOrientationPolicy: DepthBoundaryLayerOrientationPolicy? = null,
    shadowPrices: Map<DemandModeKey, FltX> = emptyMap(),
    materialCache: MutableMap<QuantityMaterial<T>, Material<FltX>> = LinkedHashMap(),
    itemCache: MutableMap<QuantityItem<T>, ActualItem> = LinkedHashMap()
): Try {
    val modelColumns = columns.map { layer -> layer.toModel(materialCache, itemCache) }
    val resolvedBins = if (bins.isNotEmpty()) {
        bins
    } else {
        modelColumns.mapIndexed { index, modelLayer ->
            val bin = modelLayer.bin ?: knownCoordinateBinType(
                layer = modelLayer,
                index = index
            )
            layerBinOf(
                shape = bin,
                units = listOf(modelLayer.toKnownCoordinateLayerPlacement())
            )
        }
    }
    when (val result = depthBoundaryLayerOrientationPolicy?.ensureSatisfied(resolvedBins) ?: ok) {
        is Ok -> {}
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return analyze(
        state = ColumnGenerationState(
            iteration = iteration,
            columns = modelColumns,
            bins = resolvedBins,
            shadowPrices = shadowPrices
        )
    )
}
