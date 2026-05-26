package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayerView
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LayerBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackingContext
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackingResult
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service.Packer
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service.PackingRendererAdapter
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.SchemaDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.math.algebra.number.Flt64

data class ColumnGenerationPackingSnapshot(
    val bins: List<LayerBin>,
    val packingResult: PackingResult,
    val schema: SchemaDTO
)

class ColumnGenerationPackingAnalyzer(
    private val packer: Packer = Packer(),
    private val rendererAdapter: PackingRendererAdapter = PackingRendererAdapter(),
    private val contextBuilder: (ColumnGenerationState<*>) -> PackingContext = { state ->
        PackingContext(info = mapOf("cg_iteration" to state.iteration.toString()))
    }
) : ColumnGenerationSolutionAnalyzer<Flt64> {
    var latest: ColumnGenerationPackingSnapshot? = null
        private set

    override suspend fun analyze(state: ColumnGenerationState<Flt64>) {
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
        latest = ColumnGenerationPackingSnapshot(
            bins = bins,
            packingResult = packingResult,
            schema = schema
        )
    }
}
