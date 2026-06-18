/**
 * 列生成标准执行器。
 * Column generation standard executors.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import kotlin.math.roundToInt
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver

/**
 * 将对偶解映射转换为 MetaDualSolution（未经类型检查）。
 * Convert dual solution map to MetaDualSolution (unchecked).
 */
private fun dualSolutionToMetaUnchecked(dualSolution: Map<*, *>): MetaDualSolution {
    val constraints = LinkedHashMap<MathConstraint, Any>()
    val symbols = LinkedHashMap<IntermediateSymbol<*>, MutableList<Pair<Constraint<*, *>, Any>>>()

    for ((key, value) in dualSolution) {
        val constraint = key as? Constraint<*, *> ?: continue
        val dual = value ?: continue
        constraint.origin?.let { constraints[it] = dual }
        constraint.from?.let { (symbol, _) ->
            symbols.getOrPut(symbol) { mutableListOf() }.add(constraint to dual)
        }
    }

    val ctor = MetaDualSolution::class.java.declaredConstructors
        .firstOrNull { it.parameterCount == 2 }
        ?: throw IllegalStateException("MetaDualSolution constructor is unavailable")
    @Suppress("UNCHECKED_CAST")
    return ctor.newInstance(
        constraints,
        symbols.mapValues { it.value.toList() }
    ) as MetaDualSolution
}

/**
 * 从求解结果中提取对偶解映射。
 * Extract dual solution map from solve result.
 */
private fun extractDualSolutionMap(result: Any): Map<*, *> {
    return runCatching {
        result.javaClass.methods
            .firstOrNull { it.name == "getDualSolution" && it.parameterCount == 0 }
            ?.invoke(result) as? Map<*, *>
    }.getOrNull() ?: emptyMap<Any?, Any?>()
}

/**
 * 列生成标准执行器配置。
 * Column generation standard executor configuration.
 *
 * @property rmpSolveNamePrefix RMP 求解名称前缀 / RMP solve name prefix
 * @property finalSolveNamePrefix 最终求解名称前缀 / final solve name prefix
 * @property rmpToLogModel RMP 是否输出模型日志 / whether to log RMP model
 * @property finalToLogModel 最终求解是否输出模型日志 / whether to log final model
 * @property rmpVolumeCoefficient RMP 体积系数 / RMP volume coefficient
 * @property finalBinAmountCoefficient 最终箱子数量系数 / final bin amount coefficient
 * @property enableFinalBinDepthConstraint 是否启用最终箱子深度约束 / whether to enable final bin depth constraint
 * @property enableFinalBinCapacityConstraint 是否启用最终箱子容量约束 / whether to enable final bin capacity constraint
 * @property enableShadowPriceAwareRequestScore 是否启用影子价格感知请求评分 / whether to enable shadow price aware request score
 * @property integralityTolerance 整性容差 / integrality tolerance
 * @property depthBoundaryLayerOrientationPolicy 深度边界层轴向/朝向硬约束 / hard axis/orientation constraints for depth boundary layers
 */
data class ColumnGenerationStandardExecutorConfig(
    val rmpSolveNamePrefix: String = "bpp3d-rmp",
    val finalSolveNamePrefix: String = "bpp3d-final",
    val rmpToLogModel: Boolean = false,
    val finalToLogModel: Boolean = false,
    val rmpVolumeCoefficient: FltX = FltX.one,
    val finalBinAmountCoefficient: FltX = FltX.one,
    val enableFinalBinDepthConstraint: Boolean = true,
    val enableFinalBinCapacityConstraint: Boolean = true,
    val enableShadowPriceAwareRequestScore: Boolean = true,
    val integralityTolerance: FltX = FltX(1e-6),
    val depthBoundaryLayerOrientationPolicy: DepthBoundaryLayerOrientationPolicy? = null
)

/**
 * 列生成标准执行器，提供 RMP 求解器、最终 MILP 求解器和请求构建器。
 * Column generation standard executors, provides RMP solver, final MILP solver and request builder.
 *
 * @property solver 列生成求解器 / column generation solver
 * @property itemDemands 货物需求 / item demands
 * @property demandEntries 需求条目 / demand entries
 * @property finalBins 最终箱子 / final bins
 * @property config 执行器配置 / executor config
 */
class ColumnGenerationStandardExecutors(
    private val solver: ColumnGenerationSolver,
    private val itemDemands: List<Pair<Item, UInt64>>,
    private val demandEntries: List<Bpp3dDemandEntry<FltX>> = demandEntriesFromItems(itemDemands),
    private val finalBins: List<Bin<BinLayer, FltX>> = emptyList(),
    private val config: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig()
) {
    companion object {
        /**
         * 从需求条目创建执行器。
         * Create executor from demand entries.
         *
         * @param solver 列生成求解器 / column generation solver
         * @param itemDemands 货物需求 / item demands
         * @param demandEntries 需求条目 / demand entries
         * @param finalBins 最终箱子 / final bins
         * @param config 执行器配置 / executor config
         * @return 执行器实例 / executor instance
         */
        fun fromDemandEntries(
            solver: ColumnGenerationSolver,
            itemDemands: List<Pair<Item, UInt64>>,
            demandEntries: List<Bpp3dDemandEntry<FltX>>,
            finalBins: List<Bin<BinLayer, FltX>> = emptyList(),
            config: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig()
        ): ColumnGenerationStandardExecutors {
            return ColumnGenerationStandardExecutors(
                solver = solver,
                itemDemands = itemDemands,
                demandEntries = demandEntries,
                finalBins = finalBins,
                config = config
            )
        }

        /**
         * 从量纲需求条目创建执行器。
         * Create executor from quantity demand entries.
         *
         * @param T 量纲数值类型 / quantity numeric type
         * @param solver 列生成求解器 / column generation solver
         * @param itemDemands 量纲货物需求 / quantity item demands
         * @param demandEntries 需求条目 / demand entries
         * @param finalBins 最终箱子 / final bins
         * @param config 执行器配置 / executor config
         * @param materialCache 物料缓存 / material cache
         * @param itemCache 货物缓存 / item cache
         * @return 执行器实例 / executor instance
         */
        fun <T : FloatingNumber<T>> fromQuantityDemandEntries(
            solver: ColumnGenerationSolver,
            itemDemands: List<Pair<QuantityItem<T>, UInt64>>,
            demandEntries: List<Bpp3dDemandEntry<FltX>>,
            finalBins: List<Bin<BinLayer, FltX>> = emptyList(),
            config: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig(),
            materialCache: MutableMap<QuantityMaterial<T>, Material<FltX>> = LinkedHashMap(),
            itemCache: MutableMap<QuantityItem<T>, ActualItem> = LinkedHashMap()
        ): ColumnGenerationStandardExecutors {
            val modelItemDemands = itemDemands.map { (item, amount) ->
                Pair(item.toModel(materialCache, itemCache), amount)
            }
            return fromDemandEntries(
                solver = solver,
                itemDemands = modelItemDemands,
                demandEntries = demandEntries,
                finalBins = finalBins,
                config = config
            )
        }

    }

    /**
     * 创建 RMP 求解器。
     * Create RMP solver.
     *
     * @return RMP 求解器 / RMP solver
     */
    fun rmpSolver(): ColumnGenerationRmpSolver<FltX> {
        return ColumnGenerationRmpSolver { state ->
            val artifactsResult = buildRmpArtifacts(state)
            if (artifactsResult is Failed) return@ColumnGenerationRmpSolver Failed(artifactsResult.error)
            if (artifactsResult is Fatal) return@ColumnGenerationRmpSolver Fatal(artifactsResult.errors)
            val artifacts = (artifactsResult as Ok).value

            val lpResult = solver.solveLPAs(
                name = "${config.rmpSolveNamePrefix}-${state.iteration}",
                metaModel = artifacts.model,
                toLogModel = config.rmpToLogModel
            )
            if (lpResult is Failed) return@ColumnGenerationRmpSolver Failed(lpResult.error)
            if (lpResult is Fatal) return@ColumnGenerationRmpSolver Fatal(lpResult.errors)
            val solved = (lpResult as Ok).value

            val shadowPriceMap = AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, FltX, Item>()
            val refreshResult = artifacts.demandConstraint.refresh(
                shadowPriceMap = shadowPriceMap,
                model = artifacts.model,
                shadowPrices = dualSolutionToMetaUnchecked(extractDualSolutionMap(solved))
            )
            if (refreshResult is Failed) return@ColumnGenerationRmpSolver Failed(refreshResult.error)
            if (refreshResult is Fatal) return@ColumnGenerationRmpSolver Fatal(refreshResult.errors)
            val shadowPrices = LinkedHashMap<DemandModeKey, FltX>()
            for ((key, value) in shadowPriceMap.map) {
                val demandKey = key as? DemandShadowPriceKey ?: continue
                val price = FltX(value.price.toDouble())
                shadowPrices[DemandModeKey(demandKey.mode, demandKey.key, demandKey.quantityUnit)] = price
                if (demandKey.quantityUnit != null) {
                    shadowPrices.putIfAbsent(
                        DemandModeKey(demandKey.mode, demandKey.key),
                        price
                    )
                }
            }
            Ok(ColumnGenerationLpResult(
                shadowPrices = shadowPrices,
                objective = FltX(solved.obj.toDouble()),
                info = mapOf(
                    "solver" to solver.name,
                    "model" to artifacts.model.name,
                    "lp_time_ms" to solved.time.inWholeMilliseconds.toString(),
                    "lp_gap" to solved.gap.toString(),
                    "lp_objective" to solved.obj.toString(),
                    "continuous_radius_solver_prototype_count" to state.continuousRadiusSolverPrototypes.size.toString(),
                    "continuous_radius_solver_prototype_variables" to state.continuousRadiusSolverPrototypes.joinToString("|") { it.variableName }
                ) + artifacts.continuousRadiusComponent.info()
                    + artifacts.continuousRadiusComponent.modelScaleInfo()
                    + artifacts.continuousRadiusComponent.extractNativeResults(artifacts.model)
                        .mapKeys { (name, _) -> "continuous_radius_solver_selected_$name" }
                        .mapValues { (_, value) -> value.toString() }
            ))
        }
    }

    /**
     * 创建最终 MILP 求解器。
     * Create final MILP solver.
     *
     * @return 最终 MILP 求解器 / final MILP solver
     */
    fun finalSolver(): ColumnGenerationFinalSolver<FltX> {
        return ColumnGenerationFinalSolver { state ->
            val bins = if (finalBins.isNotEmpty()) {
                finalBins
            } else {
                fallbackFinalBins(state.columns)
            }
            if (bins.isEmpty()) {
                return@ColumnGenerationFinalSolver Ok(ColumnGenerationFinalResult(
                    columns = state.columns,
                    bins = emptyList(),
                    objective = null,
                    info = mapOf("skipped" to "no_final_bins")
                ))
            }

            val model = newModel("${config.finalSolveNamePrefix}-${state.iteration}")
            val continuousRadiusComponent = ContinuousRadiusModelComponent(
                prototypes = state.continuousRadiusSolverPrototypes
            )
            when (val result = continuousRadiusComponent.register(model)) {
                is Ok -> {}
                is Failed -> return@ColumnGenerationFinalSolver Failed(result.error)
                is Fatal -> return@ColumnGenerationFinalSolver Fatal(result.errors)
            }
            val assignment = PreciseAssignment(
                bins = bins,
                layers = state.columns
            )
            when (val result = assignment.register(model)) {
                is Ok -> {}
                is Failed -> return@ColumnGenerationFinalSolver Failed(result.error)
                is Fatal -> return@ColumnGenerationFinalSolver Fatal(result.errors)
            }

            val load = PreciseLoad(
                demandEntries = demandEntries,
                layers = state.columns,
                assignment = assignment,
                overEnabled = false,
                lessEnabled = true
            )
            when (val result = load.register(model)) {
                is Ok -> {}
                is Failed -> return@ColumnGenerationFinalSolver Failed(result.error)
                is Fatal -> return@ColumnGenerationFinalSolver Fatal(result.errors)
            }

            val demandConstraint = itemDemandConstraint(
                load = load,
                demandEntries = demandEntries
            )
            demandConstraint.register(model)
            when (val result = demandConstraint.invoke(model)) {
                is Ok -> {}
                is Failed -> return@ColumnGenerationFinalSolver Failed(result.error)
                is Fatal -> return@ColumnGenerationFinalSolver Fatal(result.errors)
            }

            val capacity = PreciseLoadCapacity(
                bins = bins,
                layers = state.columns,
                assignment = assignment
            )
            when (val result = capacity.register(model)) {
                is Ok -> {}
                is Failed -> return@ColumnGenerationFinalSolver Failed(result.error)
                is Fatal -> return@ColumnGenerationFinalSolver Fatal(result.errors)
            }
            if (config.enableFinalBinDepthConstraint) {
                when (val result = BinDepthConstraint(
                    bins = bins,
                    capacity = capacity
                ).invoke(model)) {
                    is Ok -> {}
                    is Failed -> return@ColumnGenerationFinalSolver Failed(result.error)
                    is Fatal -> return@ColumnGenerationFinalSolver Fatal(result.errors)
                }
            }
            if (config.enableFinalBinCapacityConstraint) {
                when (val result = BinCapacityConstraint(
                    bins = bins,
                    capacity = capacity
                ).invoke(model)) {
                    is Ok -> {}
                    is Failed -> return@ColumnGenerationFinalSolver Failed(result.error)
                    is Fatal -> return@ColumnGenerationFinalSolver Fatal(result.errors)
                }
            }

            when (val result = BinAmountMinimization(
                bins = bins,
                assignment = assignment,
                coefficient = { config.finalBinAmountCoefficient }
            ).invoke(model)) {
                is Ok -> {}
                is Failed -> return@ColumnGenerationFinalSolver Failed(result.error)
                is Fatal -> return@ColumnGenerationFinalSolver Fatal(result.errors)
            }

            val milpResult = solver.solveMILPAs(
                name = "${config.finalSolveNamePrefix}-${state.iteration}",
                metaModel = model,
                toLogModel = config.finalToLogModel
            )
            if (milpResult is Failed) return@ColumnGenerationFinalSolver Failed(milpResult.error)
            if (milpResult is Fatal) return@ColumnGenerationFinalSolver Fatal(milpResult.errors)
            val solved = (milpResult as Ok).value
            model.setSolution(normalizeScalarSolution(solved.solution))
            val selectedBins = collectSelectedBins(model, bins, state.columns, assignment)
            config.depthBoundaryLayerOrientationPolicy?.ensureSatisfied(selectedBins)
            val selectedColumns = collectSelectedColumns(
                model = model,
                columns = state.columns,
                assignment = assignment,
                binAmount = bins.size
            )
            Ok(ColumnGenerationFinalResult(
                columns = if (selectedColumns.isNotEmpty()) selectedColumns else state.columns,
                bins = selectedBins,
                objective = FltX(solved.obj.toDouble()),
                info = mapOf(
                    "solver" to solver.name,
                    "model" to model.name,
                    "milp_time_ms" to solved.time.inWholeMilliseconds.toString(),
                    "milp_gap" to solved.gap.toString(),
                    "milp_objective" to solved.obj.toString(),
                    "selected_bin_count" to selectedBins.size.toString(),
                    "selected_layer_count" to selectedColumns.size.toString(),
                    "continuous_radius_solver_prototype_count" to state.continuousRadiusSolverPrototypes.size.toString(),
                    "continuous_radius_solver_prototype_variables" to state.continuousRadiusSolverPrototypes.joinToString("|") { it.variableName }
                ) + continuousRadiusComponent.info()
                    + continuousRadiusComponent.modelScaleInfo()
                    + continuousRadiusComponent.extractNativeResults(model)
                        .mapKeys { (name, _) -> "continuous_radius_solver_selected_$name" }
                        .mapValues { (_, value) -> value.toString() },
                pwlContinuousRadiusResults = continuousRadiusComponent.extractPWLResults(model)
            ))
        }
    }

    /**
     * 创建层请求构建器。
     * Create layer request builder.
     *
     * @return 层请求构建器 / layer request builder
     */
    fun requestBuilder(): ColumnGenerationLayerRequestBuilder<FltX> {
        val requestDemandEntries = demandEntries.map {
            LayerGenerationDemandEntry(it.mode, it.key, it.quantityUnit)
        }
        val candidateBin = layerGenerationCandidateBin()
        return ColumnGenerationLayerRequestBuilder { state, items, cgConfig ->
            bpp3dLayerGenerationRequest(
                iteration = state.iteration,
                bin = candidateBin,
                items = items,
                existingLayers = state.columns,
                demandEntries = requestDemandEntries,
                shadowPrices = state.shadowPrices,
                scoreByShadowPrice = if (config.enableShadowPriceAwareRequestScore) {
                    shadowPriceAwareLayerScore(shadowPriceToScalar = { it })
                } else {
                    null
                },
                maxCandidates = cgConfig.maxColumnsPerIteration
            )
        }
    }

    private fun layerGenerationCandidateBin(): BinType<FltX>? {
        return finalBins.firstOrNull()?.type
    }

    private data class RmpArtifacts(
        val model: LinearMetaModel<FltX>,
        val demandConstraint: ItemDemandConstraint,
        val continuousRadiusComponent: ContinuousRadiusModelComponent,
    )

    private suspend fun buildRmpArtifacts(
        state: ColumnGenerationState<FltX>
    ): Ret<RmpArtifacts> {
        val model = newModel("${config.rmpSolveNamePrefix}-${state.iteration}")
        val aggregation = LayerAggregation()
        val assignment = ImpreciseAssignment(
            items = itemDemands.toMap(),
            aggregation = aggregation
        )
        when (val result = assignment.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val load = ImpreciseLoad(
            demandEntries = demandEntries,
            assignment = assignment,
            overEnabled = false,
            lessEnabled = true
        )
        when (val result = load.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (state.columns.isNotEmpty()) {
            val added = when (val result = assignment.addColumns(
                iteration = UInt64.zero,
                newLayers = state.columns,
                model = model
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (added.isNotEmpty()) {
                when (val result = load.addColumns(
                    iteration = UInt64.zero,
                    newLayers = added,
                    model = model
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }

        val demandConstraint = itemDemandConstraint(
            load = load,
            demandEntries = demandEntries
        )
        demandConstraint.register(model)
        when (val result = demandConstraint.invoke(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val volumeMinimization = itemVolumeMinimization(
            assignment = assignment,
            coefficient = config.rmpVolumeCoefficient
        )
        volumeMinimization.register(model)
        when (val result = volumeMinimization.invoke(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val continuousRadiusComponent = ContinuousRadiusModelComponent(
            prototypes = state.continuousRadiusSolverPrototypes
        )
        when (val result = continuousRadiusComponent.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return Ok(RmpArtifacts(
            model = model,
            demandConstraint = demandConstraint,
            continuousRadiusComponent = continuousRadiusComponent
        ))
    }

    private fun collectSelectedColumns(
        model: LinearMetaModel<FltX>,
        columns: List<BinLayer>,
        assignment: PreciseAssignment,
        binAmount: Int
    ): List<BinLayer> {
        val selected = ArrayList<BinLayer>()
        for ((columnIndex, column) in columns.withIndex()) {
            var amount = FltX.zero
            for (binIndex in 0 until binAmount) {
                amount += tokenValue(model, assignment.x[binIndex, columnIndex])
            }
            if (amount.toDouble() > config.integralityTolerance.toDouble()) {
                selected.add(column)
            }
        }
        return selected
    }

    private fun collectSelectedBins(
        model: LinearMetaModel<FltX>,
        bins: List<Bin<BinLayer, FltX>>,
        columns: List<BinLayer>,
        assignment: PreciseAssignment
    ): List<Bin<BinLayer, FltX>> {
        val selected = ArrayList<Bin<BinLayer, FltX>>()
        for ((binIndex, baseBin) in bins.withIndex()) {
            val placements = ArrayList<QuantityPlacement3<BinLayer, FltX>>()
            var zCursor = baseBin.shape.depth - baseBin.shape.depth
            for ((columnIndex, column) in columns.withIndex()) {
                val raw = tokenValue(model, assignment.x[binIndex, columnIndex])
                val copies = raw.toDouble().roundToInt()
                if (copies <= 0) {
                    continue
                }
                repeat(copies) {
                    val layerCopy = column.copy()
                    placements.add(layerCopy.toLayerPlacement(z = zCursor))
                    zCursor = zCursor + layerCopy.depth
                }
            }
            if (placements.isNotEmpty()) {
                selected.add(
                    layerBinOf(
                        shape = baseBin.type,
                        units = placements,
                        batchNo = baseBin.batchNo
                    )
                )
            }
        }
        return selected
    }

    private fun fallbackFinalBins(columns: List<BinLayer>): List<Bin<BinLayer, FltX>> {
        return columns.mapNotNull { layer ->
            val binShape = layer.bin ?: return@mapNotNull null
            layerBinOf(
                shape = binShape,
                units = emptyList()
            )
        }
    }

    private fun tokenValue(
        model: LinearMetaModel<FltX>,
        variable: AbstractVariableItem<*, *>
    ): FltX {
        val token = model.tokens.find(variable) ?: return FltX.zero
        return token.doubleResult?.let { FltX(it) } ?: FltX.zero
    }

    private fun normalizeScalarSolution(values: List<*>): List<FltX> {
        return values.mapIndexed { index, value ->
            when (value) {
                is FltX -> value
                is Number -> FltX(value.toDouble())
                else -> throw IllegalStateException("unsupported solution value at $index: ${value?.javaClass?.name}")
            }
        }
    }

    private fun newModel(name: String): LinearMetaModel<FltX> {
        return LinearMetaModel(
            name = name,
            objectCategory = ObjectCategory.Minimum,
            configuration = MetaModelConfiguration(),
            converter = IntoValue.fromConverter(FltX)
        )
    }
}
