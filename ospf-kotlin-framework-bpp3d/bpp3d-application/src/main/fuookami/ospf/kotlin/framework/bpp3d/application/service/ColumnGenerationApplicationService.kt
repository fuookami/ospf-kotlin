@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.LegacyQuantity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LayerBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingDemand
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingObjectiveConfig
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingPlan
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingProgramCandidate
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingStatus
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service.ExhaustiveMaterialPackingSolverExecutor
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service.MaterialPacker
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service.MaterialPackingSolverExecutor
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

enum class MaterialPackingMixedDemandPolicy {
    Reject,
    ReplaceItemDemands,
    Merge
}

data class ColumnGenerationApplicationRequest(
    val itemDemands: List<Pair<Item, UInt64>>,
    val materialAmountDemands: List<Pair<Material, UInt64>> = emptyList(),
    val materialWeightDemands: List<Pair<Material, LegacyQuantity>> = emptyList(),
    val materialPackingCandidates: List<MaterialPackingProgramCandidate> = emptyList(),
    val materialPackingObjectiveConfig: MaterialPackingObjectiveConfig = MaterialPackingObjectiveConfig(),
    val mixedDemandPolicy: MaterialPackingMixedDemandPolicy = MaterialPackingMixedDemandPolicy.Reject,
    val demandEntries: List<Bpp3dDemandEntry>? = null,
    val initialColumns: List<BinLayer> = emptyList(),
    val finalBins: List<LayerBin> = emptyList(),
    val generators: List<Bpp3dLayerGenerator<Flt64>> = emptyList(),
    val cgConfig: ColumnGenerationConfig = ColumnGenerationConfig(),
    val executorConfig: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig()
)

data class ColumnGenerationApplicationResponse(
    val result: ColumnGenerationResult<Flt64>,
    val packingSnapshot: ColumnGenerationPackingSnapshot?,
    val materialPackingPlan: MaterialPackingPlan? = null
)

class ColumnGenerationApplicationService(
    private val solver: ColumnGenerationSolver,
    private val materialPackingSolverExecutor: MaterialPackingSolverExecutor = ExhaustiveMaterialPackingSolverExecutor()
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
        val hasMaterialDemands = request.materialAmountDemands.isNotEmpty() || request.materialWeightDemands.isNotEmpty()
        val materialPackingPlan = if (hasMaterialDemands) {
            val materialDemands = ArrayList<MaterialPackingDemand>()
            materialDemands.addAll(
                request.materialAmountDemands.map { (material, amount) ->
                    MaterialPackingDemand(
                        material = material,
                        amount = amount
                    )
                }
            )
            materialDemands.addAll(
                request.materialWeightDemands.map { (material, weight) ->
                    MaterialPackingDemand(
                        material = material,
                        weight = weight
                    )
                }
            )
            MaterialPacker(materialPackingSolverExecutor).plan(
                demands = materialDemands,
                candidates = request.materialPackingCandidates,
                objective = request.materialPackingObjectiveConfig
            )
        } else {
            null
        }
        if (materialPackingPlan != null) {
            if (materialPackingPlan.solveInfo.status != MaterialPackingStatus.Optimal ||
                materialPackingPlan.restMaterials.values.any { it != UInt64.zero }
            ) {
                throw IllegalStateException("material packing infeasible: ${materialPackingPlan.solveInfo.rawStatus ?: "unknown"}")
            }
        }

        val packagedItemDemands = materialPackingPlan?.packagedItems?.map { packagedItem ->
            Pair(packagedItem.item as Item, packagedItem.amount)
        } ?: emptyList()
        val resolvedItemDemands = when {
            packagedItemDemands.isEmpty() -> request.itemDemands
            request.itemDemands.isEmpty() -> packagedItemDemands
            request.mixedDemandPolicy == MaterialPackingMixedDemandPolicy.ReplaceItemDemands -> packagedItemDemands
            request.mixedDemandPolicy == MaterialPackingMixedDemandPolicy.Merge -> mergeItemDemands(request.itemDemands, packagedItemDemands)
            else -> throw IllegalArgumentException("both itemDemands and materialDemands are present, set mixedDemandPolicy explicitly.")
        }
        val entries = request.demandEntries ?: demandEntriesFromItems(resolvedItemDemands)
        val executors = ColumnGenerationStandardExecutors.fromDemandEntries(
            solver = solver,
            itemDemands = resolvedItemDemands,
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
            items = resolvedItemDemands.map { it.first },
            config = request.cgConfig
        )
        return ColumnGenerationApplicationResponse(
            result = result,
            packingSnapshot = packingAnalyzer?.latest,
            materialPackingPlan = materialPackingPlan
        )
    }

    private fun mergeItemDemands(
        base: List<Pair<Item, UInt64>>,
        extra: List<Pair<Item, UInt64>>
    ): List<Pair<Item, UInt64>> {
        val merged = LinkedHashMap<Item, UInt64>()
        for ((item, amount) in base) {
            merged[item] = (merged[item] ?: UInt64.zero) + amount
        }
        for ((item, amount) in extra) {
            merged[item] = (merged[item] ?: UInt64.zero) + amount
        }
        return merged.toList()
    }
}
