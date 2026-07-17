/**
 * 列生成标准执行器。
 * Column generation standard executors.
*/
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import kotlin.math.roundToInt
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver

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
 * RMP 建模扩展上下文 / RMP modeling extension context
 *
 * @property state 当前列生成状态 / Current column-generation state
 * @property model RMP 元模型 / RMP meta model
 * @property assignment 不精确分配组件 / Imprecise assignment component
 * @property load 不精确装载组件 / Imprecise load component
 * @property demandConstraint 需求约束 / Demand constraint
 * @property continuousRadiusComponent 连续半径组件 / Continuous-radius component
 */
data class ColumnGenerationRmpContext(
    val state: ColumnGenerationState<FltX>,
    val model: LinearMetaModel<FltX>,
    val assignment: ImpreciseAssignment,
    val load: ImpreciseLoad,
    val demandConstraint: ItemDemandConstraint,
    val continuousRadiusComponent: ContinuousRadiusModelComponent
)

/** RMP 建模扩展 / RMP modeling extension */
fun interface ColumnGenerationRmpModelExtension {
    /**
     * 注册扩展变量、约束或目标 / Register extension variables, constraints, or objectives
     *
     * @param context RMP 上下文 / RMP context
     * @return 操作结果 / Operation result
     */
    fun register(context: ColumnGenerationRmpContext): Try
}

/**
 * RMP 扩展求解结果 / RMP extension solve result
 *
 * @property additionalShadowPrices 扩展影子价格 / Additional shadow prices
 * @property info 扩展审计信息 / Extension audit information
 */
data class ColumnGenerationRmpExtensionResult(
    val additionalShadowPrices: Map<String, FltX> = emptyMap(),
    val info: Map<String, String> = emptyMap()
)

/** RMP 对偶提取扩展 / RMP dual extraction extension */
fun interface ColumnGenerationRmpSolutionExtension {
    /**
     * 提取扩展对偶和审计信息 / Extract extension duals and audit information
     *
     * @param context RMP 上下文 / RMP context
     * @param dualSolution 元对偶解 / Meta dual solution
     * @return 扩展结果 / Extension result
     */
    suspend fun extract(
        context: ColumnGenerationRmpContext,
        dualSolution: MetaDualSolution
    ): Ret<ColumnGenerationRmpExtensionResult>
}

/**
 * 最终 MILP 建模扩展上下文 / Final MILP modeling extension context
 *
 * @property state 当前列生成状态 / Current column-generation state
 * @property model 最终 MILP 元模型 / Final MILP meta model
 * @property bins 候选箱子 / Candidate bins
 * @property assignment 精确分配组件 / Precise assignment component
 * @property load 精确装载组件 / Precise load component
 * @property continuousRadiusComponent 连续半径组件 / Continuous-radius component
 */
data class ColumnGenerationFinalModelContext(
    val state: ColumnGenerationState<FltX>,
    val model: LinearMetaModel<FltX>,
    val bins: List<Bin<BinLayer, FltX>>,
    val assignment: PreciseAssignment,
    val load: PreciseLoad,
    val continuousRadiusComponent: ContinuousRadiusModelComponent
)

/** 最终 MILP 建模扩展 / Final MILP modeling extension */
fun interface ColumnGenerationFinalModelExtension {
    /**
     * 注册最终模型扩展 / Register final-model extensions
     *
     * @param context 最终模型上下文 / Final-model context
     * @return 操作结果 / Operation result
     */
    fun register(context: ColumnGenerationFinalModelContext): Try
}

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
     * @param modelExtensions RMP 建模扩展 / RMP model extensions
     * @param solutionExtensions RMP 对偶提取扩展 / RMP solution extensions
     * @return RMP 求解器 / RMP solver
    */
    fun rmpSolver(
        modelExtensions: List<ColumnGenerationRmpModelExtension> = emptyList(),
        solutionExtensions: List<ColumnGenerationRmpSolutionExtension> = emptyList()
    ): ColumnGenerationRmpSolver<FltX> {
        return ColumnGenerationRmpSolver { state ->
            val artifactsResult = buildRmpArtifacts(state)
            if (artifactsResult is Failed) return@ColumnGenerationRmpSolver Failed(artifactsResult.error)
            if (artifactsResult is Fatal) return@ColumnGenerationRmpSolver Fatal(artifactsResult.errors)
            val artifacts = (artifactsResult as Ok).value

            val context = ColumnGenerationRmpContext(
                state = state,
                model = artifacts.model,
                assignment = artifacts.assignment,
                load = artifacts.load,
                demandConstraint = artifacts.demandConstraint,
                continuousRadiusComponent = artifacts.continuousRadiusComponent
            )
            for (extension in modelExtensions) {
                when (val result = extension.register(context)) {
                    is Ok -> {}
                    is Failed -> return@ColumnGenerationRmpSolver Failed(result.error)
                    is Fatal -> return@ColumnGenerationRmpSolver Fatal(result.errors)
                }
            }

            val lpResult = solver.solveLPAs(
                name = "${config.rmpSolveNamePrefix}-${state.iteration}",
                metaModel = artifacts.model,
                toLogModel = config.rmpToLogModel
            )
            if (lpResult is Failed) return@ColumnGenerationRmpSolver Failed(lpResult.error)
            if (lpResult is Fatal) return@ColumnGenerationRmpSolver Fatal(lpResult.errors)
            val solved = (lpResult as Ok).value

            val dualSolution = solved.dualSolution.toMeta()
            val shadowPriceMap = AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, FltX, Item>()
            val refreshResult = artifacts.demandConstraint.refresh(
                shadowPriceMap = shadowPriceMap,
                model = artifacts.model,
                shadowPrices = dualSolution
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
            val extensionResults = ArrayList<ColumnGenerationRmpExtensionResult>()
            for (extension in solutionExtensions) {
                when (val result = extension.extract(context, dualSolution)) {
                    is Ok -> extensionResults.add(result.value)
                    is Failed -> return@ColumnGenerationRmpSolver Failed(result.error)
                    is Fatal -> return@ColumnGenerationRmpSolver Fatal(result.errors)
                }
            }
            val additionalShadowPrices = LinkedHashMap<String, FltX>()
            val extensionInfo = LinkedHashMap<String, String>()
            for (result in extensionResults) {
                additionalShadowPrices.putAll(result.additionalShadowPrices)
                extensionInfo.putAll(result.info)
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
                ) + extensionInfo + artifacts.continuousRadiusComponent.info()
                    + artifacts.continuousRadiusComponent.modelScaleInfo()
                    + artifacts.continuousRadiusComponent.extractNativeResults(artifacts.model)
                        .mapKeys { (name, _) -> "continuous_radius_solver_selected_$name" }
                        .mapValues { (_, value) -> value.toString() },
                additionalShadowPrices = additionalShadowPrices
            ))
        }
    }

    /**
     * 创建最终 MILP 求解器。
     * Create final MILP solver.
     *
     * @param modelExtensions 最终 MILP 建模扩展 / Final MILP model extensions
     * @return 最终 MILP 求解器 / final MILP solver
    */
    fun finalSolver(
        modelExtensions: List<ColumnGenerationFinalModelExtension> = emptyList()
    ): ColumnGenerationFinalSolver<FltX> {
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

            val extensionContext = ColumnGenerationFinalModelContext(
                state = state,
                model = model,
                bins = bins,
                assignment = assignment,
                load = load,
                continuousRadiusComponent = continuousRadiusComponent
            )
            for (extension in modelExtensions) {
                when (val result = extension.register(extensionContext)) {
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
            model.setSolution(normalizeScalarSolution(solved.solution).value!!)
            val selectedBins = when (val result = collectSelectedBins(model, bins, state.columns, assignment)) {
                is Ok -> result.value
                is Failed -> return@ColumnGenerationFinalSolver Failed(result.error)
                is Fatal -> return@ColumnGenerationFinalSolver Fatal(result.errors)
            }
            when (val depthBoundary = config.depthBoundaryLayerOrientationPolicy?.ensureSatisfied(selectedBins) ?: ok) {
                is Ok -> {}
                is Failed -> return@ColumnGenerationFinalSolver Failed(depthBoundary.error)
                is Fatal -> return@ColumnGenerationFinalSolver Fatal(depthBoundary.errors)
            }
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

	    /** 获取层生成候选箱子类型 / Get layer generation candidate bin type
	     * @return 候选箱子类型 / candidate bin type
	    */
    private fun layerGenerationCandidateBin(): BinType<FltX>? {
        return finalBins.firstOrNull()?.type
    }

    /**
     * RMP 求解构件，包含线性元模型、货物需求约束和连续半径模型组件。
     * RMP solving artifacts, containing the linear meta model, item demand constraint and continuous radius model component.
     *
     * @property model 线性元模型 / linear meta model
     * @property demandConstraint 货物需求约束 / item demand constraint
     * @property continuousRadiusComponent 连续半径模型组件 / continuous radius model component
    */
    private data class RmpArtifacts(
        val model: LinearMetaModel<FltX>,
        val assignment: ImpreciseAssignment,
        val load: ImpreciseLoad,
        val demandConstraint: ItemDemandConstraint,
        val continuousRadiusComponent: ContinuousRadiusModelComponent,
    )

    /**
     * 构建 RMP 所需的模型构件。
     * Build the model artifacts required for RMP.
     *
     * @param state 列生成状态 / column generation state
     * @return RMP 构件 / RMP artifacts
    */
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
            assignment = assignment,
            load = load,
            demandConstraint = demandConstraint,
            continuousRadiusComponent = continuousRadiusComponent
        ))
    }

    /**
     * 从求解结果中收集选中的列。
     * Collect selected columns from the solve result.
     *
     * @param model 线性元模型 / linear meta model
     * @param columns 层列列表 / layer column list
     * @param assignment 精确分配 / precise assignment
     * @param binAmount 箱子数量 / bin amount
     * @return 选中的层列列表 / selected layer column list
    */
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

    /**
     * 从求解结果中收集选中的箱子。
     * Collect selected bins from the solve result.
     *
     * @param model 线性元模型 / linear meta model
     * @param bins 候选箱子列表 / candidate bin list
     * @param columns 层列列表 / layer column list
     * @param assignment 精确分配 / precise assignment
     * @return 选中的箱子列表 / selected bin list
    */
    private fun collectSelectedBins(
        model: LinearMetaModel<FltX>,
        bins: List<Bin<BinLayer, FltX>>,
        columns: List<BinLayer>,
        assignment: PreciseAssignment
    ): Ret<List<Bin<BinLayer, FltX>>> {
        val selected = ArrayList<Bin<BinLayer, FltX>>()
        for ((binIndex, baseBin) in bins.withIndex()) {
            val placements = ArrayList<QuantityPlacement3<BinLayer, FltX>>()
            var zCursor = (baseBin.shape.depth - baseBin.shape.depth)!!
            for ((columnIndex, column) in columns.withIndex()) {
                val raw = tokenValue(model, assignment.x[binIndex, columnIndex])
                val copies = raw.toDouble().roundToInt()
                if (copies <= 0) {
                    continue
                }
                repeat(copies) {
                    val layerCopy = column.copy()
                    when (val result = layerCopy.toLayerPlacement(z = zCursor)) {
                        is Ok -> placements.add(result.value)
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                    zCursor = (zCursor + layerCopy.depth)!!
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
        return Ok(selected)
    }

    /**
     * 当未提供最终箱子时，从列中生成回退最终箱子。
     * Generate fallback final bins from columns when no final bins are provided.
     *
     * @param columns 层列列表 / layer column list
     * @return 回退最终箱子列表 / fallback final bin list
    */
    private fun fallbackFinalBins(columns: List<BinLayer>): List<Bin<BinLayer, FltX>> {
        return columns.mapNotNull { layer ->
            val binShape = layer.bin ?: return@mapNotNull null
            layerBinOf(
                shape = binShape,
                units = emptyList()
            )
        }
    }

    /**
     * 从模型中提取变量对应的令牌值。
     * Extract the token value for a variable from the model.
     *
     * @param model 线性元模型 / linear meta model
     * @param variable 抽象变量项 / abstract variable item
     * @return 变量的 FltX 值 / FltX value of the variable
    */
    private fun tokenValue(
        model: LinearMetaModel<FltX>,
        variable: AbstractVariableItem<*, *>
    ): FltX {
        val token = model.tokens.find(variable) ?: return FltX.zero
        return token.doubleResult?.let { FltX(it) } ?: FltX.zero
    }

    /**
     * 标准化标量解决方案。
     * Normalize scalar solution.
     *
     * @param values 原始解值列表 / raw solution value list
     * @return 标准化后的 FltX 列表 / normalized FltX list
    */
    private fun normalizeScalarSolution(values: List<*>): Ret<List<FltX>> {
        return ok(values.mapIndexed { index, value ->
            when (value) {
                is FltX -> value
                is Number -> FltX(value.toDouble())
                else -> return Failed(ErrorCode.IllegalArgument, "unsupported solution value at $index: ${value?.javaClass?.name}")
            }
        })
    }

    /**
     * 创建新的线性元模型。
     * Create a new linear meta model.
     *
     * @param name 模型名称 / model name
     * @return 线性元模型 / linear meta model
    */
    private fun newModel(name: String): LinearMetaModel<FltX> {
        return LinearMetaModel(
            name = name,
            objectCategory = ObjectCategory.Minimum,
            configuration = MetaModelConfiguration(),
            converter = IntoValue.fromConverter(FltX)
        )
    }
}
