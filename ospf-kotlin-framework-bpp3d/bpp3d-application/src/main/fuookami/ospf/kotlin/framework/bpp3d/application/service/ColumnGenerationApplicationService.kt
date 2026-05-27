@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LayerBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dDemandEntry
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.demandEntriesFromItems
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.BLLocalLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.BLGlobalLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.BlockLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.CirclePackingLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.HistoricalLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.LayerGenerationContext
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.PatternLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.PileLayerGenerator
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

data class ColumnGenerationApplicationRequest(
    val itemDemands: List<Pair<Item, UInt64>>,
    val demandEntries: List<Bpp3dDemandEntry>? = null,
    val initialColumns: List<BinLayer> = emptyList(),
    val finalBins: List<LayerBin> = emptyList(),
    val generators: List<Bpp3dLayerGenerator<Flt64>> = emptyList(),
    val cgConfig: ColumnGenerationConfig = ColumnGenerationConfig(),
    val executorConfig: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig()
)

data class ColumnGenerationApplicationResponse(
    val result: ColumnGenerationResult<Flt64>,
    val packingSnapshot: ColumnGenerationPackingSnapshot?
)

class ColumnGenerationApplicationService(
    private val solver: ColumnGenerationSolver
) {
    companion object {
        fun defaultLayerGenerators(): List<Bpp3dLayerGenerator<Flt64>> {
            return listOf(
                BlockLayerGenerator(),
                BLLocalLayerGenerator(),
                BLGlobalLayerGenerator(),
                PatternLayerGenerator(),
                PileLayerGenerator(),
                CirclePackingLayerGenerator(),
                HistoricalLayerGenerator()
            )
        }
    }

    suspend fun solve(
        request: ColumnGenerationApplicationRequest,
        packingAnalyzer: ColumnGenerationPackingAnalyzer? = null,
        solutionAnalyzer: ColumnGenerationSolutionAnalyzer<Flt64>? = null
    ): ColumnGenerationApplicationResponse {
        val entries = request.demandEntries ?: demandEntriesFromItems(request.itemDemands)
        val executors = ColumnGenerationStandardExecutors.fromDemandEntries(
            solver = solver,
            itemDemands = request.itemDemands,
            demandEntries = entries,
            finalBins = request.finalBins,
            config = request.executorConfig
        )
        val layerGenerator = LayerGenerationContext(
            generators = request.generators.ifEmpty { defaultLayerGenerators() }
        )
        val combinedAnalyzer = when {
            packingAnalyzer != null && solutionAnalyzer != null -> {
                ColumnGenerationSolutionAnalyzer<Flt64> { state ->
                    solutionAnalyzer.analyze(state)
                    packingAnalyzer.analyze(state)
                }
            }

            packingAnalyzer != null -> packingAnalyzer
            else -> solutionAnalyzer
        }

        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = layerGenerator,
            rmpSolver = executors.rmpSolver(),
            finalMilpSolver = executors.finalSolver(),
            layerRequestBuilder = executors.requestBuilder(),
            solutionAnalyzer = combinedAnalyzer,
            initialColumns = { request.initialColumns }
        )
        val result = algorithm.solve(
            items = request.itemDemands.map { it.first },
            config = request.cgConfig
        )
        return ColumnGenerationApplicationResponse(
            result = result,
            packingSnapshot = packingAnalyzer?.latest
        )
    }
}
