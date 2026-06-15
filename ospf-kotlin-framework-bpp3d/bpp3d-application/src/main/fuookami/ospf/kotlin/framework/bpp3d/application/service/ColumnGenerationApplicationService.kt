/**
 * 列生成应用服务。
 * Column generation application service.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver

/**
 * 物料装箱混合需求策略。
 * Material packing mixed demand policy.
 */
enum class MaterialPackingMixedDemandPolicy {
    /** 拒绝混合需求 / reject mixed demands */
    Reject,
    /** 替换货物需求 / replace item demands */
    ReplaceItemDemands,
    /** 合并需求 / merge demands */
    Merge
}

private fun <T : FloatingNumber<T>> toFltXQuantity(
    quantity: Quantity<T>
): Quantity<FltX> {
    return Quantity(FltX(quantity.value.toString().toDouble()), quantity.unit)
}

/**
 * 从泛型输入构建列生成应用请求。
 * Build column generation application request from quantity inputs.
 *
 * @param T 量纲数值类型 / quantity numeric type
 * @param itemDemands 货物需求列表 / item demand list
 * @param materialAmountDemands 物料数量需求 / material amount demands
 * @param materialWeightDemands 物料重量需求 / material weight demands
 * @param materialPackingCandidates 物料装箱候选方案 / material packing candidates
 * @param layerGenerationProgramDemands 层生成程序需求 / layer generation program demands
 * @param programMaterialCatalog 程序物料目录 / program material catalog
 * @param materialPackingObjectiveConfig 物料装箱目标配置 / material packing objective config
 * @param mixedDemandPolicy 混合需求策略 / mixed demand policy
 * @param demandEntries 自定义需求条目（可选） / custom demand entries (optional)
 * @param initialColumns 初始列 / initial columns
 * @param finalBins 最终箱子 / final bins
 * @param generators 层生成器列表 / layer generator list
 * @param cgConfig 列生成配置 / column generation config
 * @param depthBoundaryLayerOrientationPolicy 深度边界层轴向/朝向策略 / depth boundary layer axis/orientation policy
 * @param executorConfig 执行器配置 / executor config
 * @param materialCache 物料缓存 / material cache
 * @param itemCache 货物缓存 / item cache
 * @return 列生成应用请求 / column generation application request
 */
fun <T : FloatingNumber<T>> columnGenerationApplicationRequestFromQuantity(
    itemDemands: List<Pair<QuantityItem<T>, UInt64>>,
    materialAmountDemands: List<Pair<QuantityMaterial<T>, UInt64>> = emptyList(),
    materialWeightDemands: List<Pair<QuantityMaterial<T>, Quantity<T>>> = emptyList(),
    materialPackingCandidates: List<MaterialPackingProgramCandidate<FltX>> = emptyList(),
    layerGenerationProgramDemands: List<Pair<MaterialPackingProgramCandidate<FltX>, UInt64>> = emptyList(),
    programMaterialCatalog: Map<MaterialKey, QuantityMaterial<T>> = emptyMap(),
    materialPackingObjectiveConfig: MaterialPackingObjectiveConfig = MaterialPackingObjectiveConfig(),
    mixedDemandPolicy: MaterialPackingMixedDemandPolicy = MaterialPackingMixedDemandPolicy.Reject,
    demandEntries: List<Bpp3dDemandEntry<FltX>>? = null,
    initialColumns: List<QuantityBinLayer<T>> = emptyList(),
    finalBins: List<Bin<BinLayer, FltX>> = emptyList(),
    generators: List<Bpp3dLayerGenerator<FltX>> = emptyList(),
    cgConfig: ColumnGenerationConfig = ColumnGenerationConfig(),
    depthBoundaryLayerOrientationPolicy: DepthBoundaryLayerOrientationPolicy? = null,
    executorConfig: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig(),
    materialCache: MutableMap<QuantityMaterial<T>, Material<FltX>> = LinkedHashMap(),
    itemCache: MutableMap<QuantityItem<T>, ActualItem> = LinkedHashMap()
): ColumnGenerationApplicationRequest {
    val modelItemDemands = itemDemands.map { (item, amount) ->
        Pair(item.toModel(materialCache, itemCache), amount)
    }
    val modelMaterialAmountDemands = materialAmountDemands.map { (material, amount) ->
        Pair(materialCache.getOrPut(material) { material.toModel() }, amount)
    }
    val modelMaterialWeightDemands = materialWeightDemands.map { (material, weight) ->
        Pair(
            materialCache.getOrPut(material) { material.toModel() },
            toFltXQuantity(weight)
        )
    }
    val modelProgramMaterialCatalog = programMaterialCatalog.mapValues { (_, material) ->
        materialCache.getOrPut(material) { material.toModel() }
    }
    val modelInitialColumns = initialColumns.map { layer ->
        layer.toModel(materialCache, itemCache)
    }
    return ColumnGenerationApplicationRequest(
        itemDemands = modelItemDemands,
        materialAmountDemands = modelMaterialAmountDemands,
        materialWeightDemands = modelMaterialWeightDemands,
        materialPackingCandidates = materialPackingCandidates,
        layerGenerationProgramDemands = layerGenerationProgramDemands,
        programMaterialCatalog = modelProgramMaterialCatalog,
        materialPackingObjectiveConfig = materialPackingObjectiveConfig,
        mixedDemandPolicy = mixedDemandPolicy,
        demandEntries = demandEntries,
        initialColumns = modelInitialColumns,
        finalBins = finalBins,
        generators = generators,
        cgConfig = cgConfig,
        depthBoundaryLayerOrientationPolicy = depthBoundaryLayerOrientationPolicy,
        executorConfig = executorConfig
    )
}

/**
 * 列生成应用请求。
 * Column generation application request.
 *
 * @property itemDemands 货物需求列表 / item demand list
 * @property materialAmountDemands 物料数量需求 / material amount demands
 * @property materialWeightDemands 物料重量需求 / material weight demands
 * @property materialPackingCandidates 物料装箱候选方案 / material packing candidates
 * @property layerGenerationProgramDemands 层生成程序需求 / layer generation program demands
 * @property programMaterialCatalog 程序物料目录 / program material catalog
 * @property materialPackingObjectiveConfig 物料装箱目标配置 / material packing objective config
 * @property mixedDemandPolicy 混合需求策略 / mixed demand policy
 * @property demandEntries 自定义需求条目（可选） / custom demand entries (optional)
 * @property initialColumns 初始列 / initial columns
 * @property finalBins 最终箱子 / final bins
 * @property generators 层生成器列表 / layer generator list
 * @property cgConfig 列生成配置 / column generation config
 * @property depthBoundaryLayerOrientationPolicy 深度边界层轴向/朝向策略 / depth boundary layer axis/orientation policy
 * @property executorConfig 执行器配置 / executor config
 */
data class ColumnGenerationApplicationRequest(
    val itemDemands: List<Pair<Item, UInt64>>,
    val materialAmountDemands: List<Pair<Material<FltX>, UInt64>> = emptyList(),
    val materialWeightDemands: List<Pair<Material<FltX>, Quantity<FltX>>> = emptyList(),
    val materialPackingCandidates: List<MaterialPackingProgramCandidate<FltX>> = emptyList(),
    val layerGenerationProgramDemands: List<Pair<MaterialPackingProgramCandidate<FltX>, UInt64>> = emptyList(),
    val programMaterialCatalog: Map<MaterialKey, Material<FltX>> = emptyMap(),
    val materialPackingObjectiveConfig: MaterialPackingObjectiveConfig = MaterialPackingObjectiveConfig(),
    val mixedDemandPolicy: MaterialPackingMixedDemandPolicy = MaterialPackingMixedDemandPolicy.Reject,
    val demandEntries: List<Bpp3dDemandEntry<FltX>>? = null,
    val initialColumns: List<BinLayer> = emptyList(),
    val finalBins: List<Bin<BinLayer, FltX>> = emptyList(),
    val generators: List<Bpp3dLayerGenerator<FltX>> = emptyList(),
    val cgConfig: ColumnGenerationConfig = ColumnGenerationConfig(),
    val depthBoundaryLayerOrientationPolicy: DepthBoundaryLayerOrientationPolicy? = null,
    val executorConfig: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig()
)

/**
 * 列生成量纲应用请求。
 * Column generation quantity application request.
 *
 * @param T 量纲数值类型 / quantity numeric type
 * @property itemDemands 货物需求列表 / item demand list
 * @property materialAmountDemands 物料数量需求 / material amount demands
 * @property materialWeightDemands 物料重量需求 / material weight demands
 * @property materialPackingCandidates 物料装箱候选方案 / material packing candidates
 * @property layerGenerationProgramDemands 层生成程序需求 / layer generation program demands
 * @property programMaterialCatalog 程序物料目录 / program material catalog
 * @property materialPackingObjectiveConfig 物料装箱目标配置 / material packing objective config
 * @property mixedDemandPolicy 混合需求策略 / mixed demand policy
 * @property demandEntries 自定义需求条目（可选） / custom demand entries (optional)
 * @property initialColumns 初始列 / initial columns
 * @property finalBins 最终箱子 / final bins
 * @property generators 层生成器列表 / layer generator list
 * @property cgConfig 列生成配置 / column generation config
 * @property depthBoundaryLayerOrientationPolicy 深度边界层轴向/朝向策略 / depth boundary layer axis/orientation policy
 * @property executorConfig 执行器配置 / executor config
 */
data class ColumnGenerationQuantityApplicationRequest<T : FloatingNumber<T>>(
    val itemDemands: List<Pair<QuantityItem<T>, UInt64>>,
    val materialAmountDemands: List<Pair<QuantityMaterial<T>, UInt64>> = emptyList(),
    val materialWeightDemands: List<Pair<QuantityMaterial<T>, Quantity<T>>> = emptyList(),
    val materialPackingCandidates: List<MaterialPackingProgramCandidate<FltX>> = emptyList(),
    val layerGenerationProgramDemands: List<Pair<MaterialPackingProgramCandidate<FltX>, UInt64>> = emptyList(),
    val programMaterialCatalog: Map<MaterialKey, QuantityMaterial<T>> = emptyMap(),
    val materialPackingObjectiveConfig: MaterialPackingObjectiveConfig = MaterialPackingObjectiveConfig(),
    val mixedDemandPolicy: MaterialPackingMixedDemandPolicy = MaterialPackingMixedDemandPolicy.Reject,
    val demandEntries: List<Bpp3dDemandEntry<FltX>>? = null,
    val initialColumns: List<QuantityBinLayer<T>> = emptyList(),
    val finalBins: List<Bin<BinLayer, FltX>> = emptyList(),
    val generators: List<Bpp3dLayerGenerator<FltX>> = emptyList(),
    val cgConfig: ColumnGenerationConfig = ColumnGenerationConfig(),
    val depthBoundaryLayerOrientationPolicy: DepthBoundaryLayerOrientationPolicy? = null,
    val executorConfig: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig()
)

/**
 * 量纲应用请求转模型应用请求。
 * Convert quantity application request into model application request.
 *
 * @param T 量纲数值类型 / quantity numeric type
 * @param materialCache 物料缓存 / material cache
 * @param itemCache 货物缓存 / item cache
 * @return 模型应用请求 / model application request
 */
fun <T : FloatingNumber<T>> ColumnGenerationQuantityApplicationRequest<T>.toModelRequest(
    materialCache: MutableMap<QuantityMaterial<T>, Material<FltX>> = LinkedHashMap(),
    itemCache: MutableMap<QuantityItem<T>, ActualItem> = LinkedHashMap()
): ColumnGenerationApplicationRequest {
    return columnGenerationApplicationRequestFromQuantity(
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
        finalBins = finalBins,
        generators = generators,
        cgConfig = cgConfig,
        depthBoundaryLayerOrientationPolicy = depthBoundaryLayerOrientationPolicy,
        executorConfig = executorConfig,
        materialCache = materialCache,
        itemCache = itemCache
    )
}

/**
 * 列生成应用响应。
 * Column generation application response.
 *
 * @property result 列生成结果 / column generation result
 * @property packingSnapshot 装箱快照（可选） / packing snapshot (optional)
 * @property materialPackingPlan 物料装箱计划（可选） / material packing plan (optional)
 */
data class ColumnGenerationApplicationResponse(
    val result: ColumnGenerationResult<FltX>,
    val packingSnapshot: ColumnGenerationPackingSnapshot?,
    val materialPackingPlan: MaterialPackingPlan? = null
)

/**
 * 列生成应用服务，编排物料装箱和列生成流程。
 * Column generation application service, orchestrates material packing and column generation.
 *
 * @property solver 列生成求解器 / column generation solver
 * @property materialPackingSolverExecutor 物料装箱求解器执行器 / material packing solver executor
 */
class ColumnGenerationApplicationService(
    private val solver: ColumnGenerationSolver,
    private val materialPackingSolverExecutor: MaterialPackingSolverExecutor = ExhaustiveMaterialPackingSolverExecutor()
) {
    companion object {
        /**
         * 获取默认层生成器列表。
         * Get default layer generator list.
         *
         * @return 层生成器列表 / layer generator list
         */
        fun defaultLayerGenerators(): List<Bpp3dLayerGenerator<FltX>> {
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

    /**
     * 执行列生成求解。
     * Execute column generation solving.
     *
     * @param request 应用请求 / application request
     * @param packingAnalyzer 装箱分析器（可选） / packing analyzer (optional)
     * @param solutionAnalyzer 解分析器（可选） / solution analyzer (optional)
     * @return 应用响应 / application response
     */
    suspend fun solve(
        request: ColumnGenerationApplicationRequest,
        packingAnalyzer: ColumnGenerationPackingAnalyzer? = null,
        solutionAnalyzer: ColumnGenerationSolutionAnalyzer<FltX>? = null
    ): ColumnGenerationApplicationResponse {
        val hasMaterialDemands = request.materialAmountDemands.isNotEmpty() || request.materialWeightDemands.isNotEmpty()
        val shouldRunMaterialPacking = hasMaterialDemands && request.materialPackingCandidates.isNotEmpty()
        val materialPackingPlan = if (shouldRunMaterialPacking) {
            val materialDemands = ArrayList<MaterialPackingDemand<FltX>>()
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
        val programCandidateItemDemands = layerGenerationItemDemandsFromPrograms(
            programDemands = request.layerGenerationProgramDemands,
            materialCatalog = materialCatalog
        )
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
            config = resolveExecutorConfig(request)
        )
        val layerGenerator = LayerGenerationContext(
            generators = request.generators.ifEmpty { defaultLayerGenerators() }
        )
        val combinedAnalyzer = when {
            packingAnalyzer != null && solutionAnalyzer != null -> {
                ColumnGenerationSolutionAnalyzer<FltX> { state ->
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

    /**
     * 执行量纲列生成求解。
     * Execute quantity column generation solving.
     *
     * @param request 量纲应用请求 / quantity application request
     * @param packingAnalyzer 装箱分析器（可选） / packing analyzer (optional)
     * @param solutionAnalyzer 解分析器（可选） / solution analyzer (optional)
     * @return 应用响应 / application response
     */
    suspend fun <T : FloatingNumber<T>> solve(
        request: ColumnGenerationQuantityApplicationRequest<T>,
        packingAnalyzer: ColumnGenerationPackingAnalyzer? = null,
        solutionAnalyzer: ColumnGenerationSolutionAnalyzer<FltX>? = null
    ): ColumnGenerationApplicationResponse {
        return solve(
            request = request.toModelRequest(),
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
    ): Map<MaterialKey, Material<FltX>> {
        val catalog = LinkedHashMap<MaterialKey, Material<FltX>>()
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

    private fun resolveExecutorConfig(
        request: ColumnGenerationApplicationRequest
    ): ColumnGenerationStandardExecutorConfig {
        val requestPolicy = request.depthBoundaryLayerOrientationPolicy
            ?: return request.executorConfig
        val executorPolicy = request.executorConfig.depthBoundaryLayerOrientationPolicy
        require(executorPolicy == null || executorPolicy == requestPolicy) {
            "depthBoundaryLayerOrientationPolicy conflicts with executorConfig.depthBoundaryLayerOrientationPolicy."
        }
        return request.executorConfig.copy(
            depthBoundaryLayerOrientationPolicy = requestPolicy
        )
    }
}
