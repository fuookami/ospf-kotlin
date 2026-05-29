@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LayerBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
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
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.demandEntriesFromMaterialAmounts
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.demandEntriesFromMaterialWeights
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.BLLocalLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.BLGlobalLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.BlockLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.CirclePackingLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.HistoricalLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.LayerGenerationContext
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.PatternLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.PileLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Item as QuantityItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.BinLayer as QuantityBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Material as QuantityMaterial
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

enum class MaterialPackingMixedDemandPolicy {
    Reject,
    ReplaceItemDemands,
    Merge
}

data class ColumnGenerationApplicationRequest(
    val itemDemands: List<Pair<Item, UInt64>>,
    val materialAmountDemands: List<Pair<Material, UInt64>> = emptyList(),
    val materialWeightDemands: List<Pair<Material, Quantity<InfraNumber>>> = emptyList(),
    val materialPackingCandidates: List<MaterialPackingProgramCandidate> = emptyList(),
    val layerGenerationProgramDemands: List<Pair<MaterialPackingProgramCandidate, UInt64>> = emptyList(),
    val programMaterialCatalog: Map<MaterialKey, Material> = emptyMap(),
    val materialPackingObjectiveConfig: MaterialPackingObjectiveConfig = MaterialPackingObjectiveConfig(),
    val mixedDemandPolicy: MaterialPackingMixedDemandPolicy = MaterialPackingMixedDemandPolicy.Reject,
    val demandEntries: List<Bpp3dDemandEntry>? = null,
    val initialColumns: List<BinLayer> = emptyList(),
    val finalBins: List<LayerBin> = emptyList(),
    val generators: List<Bpp3dLayerGenerator<InfraNumber>> = emptyList(),
    val cgConfig: ColumnGenerationConfig = ColumnGenerationConfig(),
    val executorConfig: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig()
) {
    companion object {
        fun <V : FloatingNumber<V>> fromQuantityDemands(
            itemDemands: List<Pair<QuantityItem<V>, UInt64>>,
            materialAmountDemands: List<Pair<QuantityMaterial<V>, UInt64>> = emptyList(),
            materialWeightDemands: List<Pair<QuantityMaterial<V>, Quantity<V>>> = emptyList(),
            materialPackingCandidates: List<MaterialPackingProgramCandidate> = emptyList(),
            layerGenerationProgramDemands: List<Pair<MaterialPackingProgramCandidate, UInt64>> = emptyList(),
            programMaterialCatalog: Map<MaterialKey, Material> = emptyMap(),
            materialPackingObjectiveConfig: MaterialPackingObjectiveConfig = MaterialPackingObjectiveConfig(),
            mixedDemandPolicy: MaterialPackingMixedDemandPolicy = MaterialPackingMixedDemandPolicy.Reject,
            demandEntries: List<Bpp3dDemandEntry>? = null,
            initialColumns: List<BinLayer> = emptyList(),
            quantityInitialColumns: List<QuantityBinLayer<V>> = emptyList(),
            finalBins: List<LayerBin> = emptyList(),
            generators: List<Bpp3dLayerGenerator<InfraNumber>> = emptyList(),
            cgConfig: ColumnGenerationConfig = ColumnGenerationConfig(),
            executorConfig: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig(),
            materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap(),
            itemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap()
        ): ColumnGenerationApplicationRequest {
            val slices = toDemandSlices(
                itemDemands = itemDemands,
                materialAmountDemands = materialAmountDemands,
                materialWeightDemands = materialWeightDemands,
                initialColumns = initialColumns,
                quantityInitialColumns = quantityInitialColumns,
                materialCache = materialCache,
                itemCache = itemCache
            )
            return ColumnGenerationApplicationRequest(
                itemDemands = slices.itemDemands,
                materialAmountDemands = slices.materialAmountDemands,
                materialWeightDemands = slices.materialWeightDemands,
                materialPackingCandidates = materialPackingCandidates,
                layerGenerationProgramDemands = layerGenerationProgramDemands,
                programMaterialCatalog = programMaterialCatalog,
                materialPackingObjectiveConfig = materialPackingObjectiveConfig,
                mixedDemandPolicy = mixedDemandPolicy,
                demandEntries = demandEntries,
                initialColumns = slices.initialColumns,
                finalBins = finalBins,
                generators = generators,
                cgConfig = cgConfig,
                executorConfig = executorConfig
            )
        }
    }
}

data class ColumnGenerationApplicationResponse(
    val result: ColumnGenerationResult<InfraNumber>,
    val packingSnapshot: ColumnGenerationPackingSnapshot?,
    val materialPackingPlan: MaterialPackingPlan? = null
)

class ColumnGenerationApplicationService(
    private val solver: ColumnGenerationSolver,
    private val materialPackingSolverExecutor: MaterialPackingSolverExecutor = ExhaustiveMaterialPackingSolverExecutor()
) {
    companion object {
        fun defaultLayerGenerators(): List<Bpp3dLayerGenerator<InfraNumber>> {
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
        solutionAnalyzer: ColumnGenerationSolutionAnalyzer<InfraNumber>? = null
    ): ColumnGenerationApplicationResponse {
        val hasMaterialDemands = request.materialAmountDemands.isNotEmpty() || request.materialWeightDemands.isNotEmpty()
        val shouldRunMaterialPacking = hasMaterialDemands && request.materialPackingCandidates.isNotEmpty()
        val materialPackingPlan = if (shouldRunMaterialPacking) {
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
        val materialCatalog = buildProgramMaterialCatalog(request)
        val programCandidateItemDemands = request.layerGenerationProgramDemands.mapIndexed { index, entry ->
            val candidate = entry.first
            val amount = entry.second
            Pair(
                candidate.toLayerGenerationItem(
                    sequence = index + 1,
                    materialCatalog = materialCatalog
                ),
                amount
            )
        }
        val baseItemDemands = mergeItemDemands(
            base = request.itemDemands,
            extra = programCandidateItemDemands
        )
        val resolvedItemDemands = when {
            packagedItemDemands.isEmpty() -> baseItemDemands
            baseItemDemands.isEmpty() -> packagedItemDemands
            request.mixedDemandPolicy == MaterialPackingMixedDemandPolicy.ReplaceItemDemands -> packagedItemDemands
            request.mixedDemandPolicy == MaterialPackingMixedDemandPolicy.Merge -> mergeItemDemands(baseItemDemands, packagedItemDemands)
            else -> throw IllegalArgumentException("both itemDemands and materialDemands are present, set mixedDemandPolicy explicitly.")
        }
        val entries = request.demandEntries ?: if (hasMaterialDemands && !shouldRunMaterialPacking) {
            demandEntriesFromItems(resolvedItemDemands) +
                    demandEntriesFromMaterialAmounts(request.materialAmountDemands) +
                    demandEntriesFromMaterialWeights(request.materialWeightDemands)
        } else {
            demandEntriesFromItems(resolvedItemDemands)
        }
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
                ColumnGenerationSolutionAnalyzer<InfraNumber> { state ->
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

    suspend fun <V : FloatingNumber<V>> solveQuantityDemands(
        itemDemands: List<Pair<QuantityItem<V>, UInt64>>,
        materialAmountDemands: List<Pair<QuantityMaterial<V>, UInt64>> = emptyList(),
        materialWeightDemands: List<Pair<QuantityMaterial<V>, Quantity<V>>> = emptyList(),
        materialPackingCandidates: List<MaterialPackingProgramCandidate> = emptyList(),
        layerGenerationProgramDemands: List<Pair<MaterialPackingProgramCandidate, UInt64>> = emptyList(),
        programMaterialCatalog: Map<MaterialKey, Material> = emptyMap(),
        materialPackingObjectiveConfig: MaterialPackingObjectiveConfig = MaterialPackingObjectiveConfig(),
        mixedDemandPolicy: MaterialPackingMixedDemandPolicy = MaterialPackingMixedDemandPolicy.Reject,
        demandEntries: List<Bpp3dDemandEntry>? = null,
        initialColumns: List<BinLayer> = emptyList(),
        quantityInitialColumns: List<QuantityBinLayer<V>> = emptyList(),
        finalBins: List<LayerBin> = emptyList(),
        generators: List<Bpp3dLayerGenerator<InfraNumber>> = emptyList(),
        cgConfig: ColumnGenerationConfig = ColumnGenerationConfig(),
        executorConfig: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig(),
        packingAnalyzer: ColumnGenerationPackingAnalyzer? = null,
        solutionAnalyzer: ColumnGenerationSolutionAnalyzer<InfraNumber>? = null,
        materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap(),
        itemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap()
    ): ColumnGenerationApplicationResponse {
        return solve(
            request = ColumnGenerationApplicationRequest.fromQuantityDemands(
                itemDemands = itemDemands,
                materialAmountDemands = materialAmountDemands,
                materialWeightDemands = materialWeightDemands,
                materialPackingCandidates = materialPackingCandidates,
                layerGenerationProgramDemands = layerGenerationProgramDemands,
                programMaterialCatalog = programMaterialCatalog,
                materialPackingObjectiveConfig = materialPackingObjectiveConfig,
                mixedDemandPolicy = mixedDemandPolicy,
                demandEntries = demandEntries,
                initialColumns = initialColumns,
                quantityInitialColumns = quantityInitialColumns,
                finalBins = finalBins,
                generators = generators,
                cgConfig = cgConfig,
                executorConfig = executorConfig,
                materialCache = materialCache,
                itemCache = itemCache
            ),
            packingAnalyzer = packingAnalyzer,
            solutionAnalyzer = solutionAnalyzer
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

    private fun buildProgramMaterialCatalog(
        request: ColumnGenerationApplicationRequest
    ): Map<MaterialKey, Material> {
        val catalog = LinkedHashMap<MaterialKey, Material>()
        for ((material, _) in request.materialAmountDemands) {
            catalog.putIfAbsent(material.key, material)
        }
        for ((material, _) in request.materialWeightDemands) {
            catalog.putIfAbsent(material.key, material)
        }
        for ((key, material) in request.programMaterialCatalog) {
            catalog.putIfAbsent(key, material)
        }
        return catalog
    }
}


