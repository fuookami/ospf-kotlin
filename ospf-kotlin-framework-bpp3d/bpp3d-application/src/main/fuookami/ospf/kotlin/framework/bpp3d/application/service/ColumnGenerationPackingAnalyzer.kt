package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayerView
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LayerBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackingContext
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackingResult
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service.Packer
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service.PackingRendererAdapter
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.SchemaDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber

data class ColumnGenerationPackingSnapshot(
    val bins: List<LayerBin>,
    val packingResult: PackingResult,
    val schema: SchemaDTO,
    val demandModeShadowPriceTotals: Map<String, InfraNumber> = emptyMap(),
    val demandModeShadowPriceEntryCounts: Map<String, Int> = emptyMap()
)

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

class ColumnGenerationPackingAnalyzer(
    private val packer: Packer = Packer(),
    private val rendererAdapter: PackingRendererAdapter = PackingRendererAdapter(),
    private val contextBuilder: (ColumnGenerationState<*>) -> PackingContext = { state ->
        PackingContext(info = mapOf("cg_iteration" to state.iteration.toString()))
    }
) : ColumnGenerationSolutionAnalyzer<InfraNumber> {
    var latest: ColumnGenerationPackingSnapshot? = null
        private set

    override suspend fun analyze(state: ColumnGenerationState<InfraNumber>) {
        val bins: List<LayerBin> = if (state.bins.isNotEmpty()) {
            state.bins
        } else {
            state.columns.mapNotNull { layer ->
                val bin = layer.bin ?: return@mapNotNull null
                val placement: QuantityPlacement3<fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer> = QuantityPlacement3(
                    view = BinLayerView(layer.copy()),
                    position = point3()
                )
                Bin(
                    shape = bin,
                    units = listOf(placement)
                )
            }
        }
        val packingResult = packer.invoke(
            bins = bins,
            context = contextBuilder(state)
        )
        val schema = rendererAdapter.toSchema(packingResult)
        val demandModeShadowPriceTotals = LinkedHashMap<String, InfraNumber>()
        val demandModeShadowPriceEntryCounts = LinkedHashMap<String, Int>()
        for ((key, value) in state.shadowPrices) {
            val modeTag = demandModeTag(key.mode)
            demandModeShadowPriceTotals[modeTag] = (demandModeShadowPriceTotals[modeTag] ?: InfraNumber.zero) + value
            demandModeShadowPriceEntryCounts[modeTag] = (demandModeShadowPriceEntryCounts[modeTag] ?: 0) + 1
        }
        val schemaKpi = LinkedHashMap(schema.kpi)
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
    }
}

