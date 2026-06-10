@file:Suppress("DEPRECATION")

/**
 * 列生成标准执行器。
 * Column generation standard executors.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModelConfiguration
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.MathConstraint
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BPP3DShadowPriceArguments
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BPP3DShadowPriceMap
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayerPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LayerBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.layerBinOf
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dDemandEntry
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.ImpreciseAssignment
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.ImpreciseLoad
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.LayerAggregation
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseAssignment
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseLoad
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseLoadCapacity
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.demandEntriesFromItems
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits.BinAmountMinimization
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits.BinCapacityConstraint
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits.BinDepthConstraint
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits.ItemDemandConstraint
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits.DemandShadowPriceKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits.itemDemandConstraint
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits.itemVolumeMinimization
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.DemandModeKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.LayerGenerationDemandEntry
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.bpp3dLayerGenerationRequest
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.shadowPriceAwareLayerScore
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraZero
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.quantities.quantity.minus
import fuookami.ospf.kotlin.quantities.quantity.plus
import kotlin.math.roundToInt

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
    val rmpVolumeCoefficient: InfraNumber = InfraNumber.one,
    val finalBinAmountCoefficient: InfraNumber = InfraNumber.one,
    val enableFinalBinDepthConstraint: Boolean = true,
    val enableFinalBinCapacityConstraint: Boolean = true,
    val enableShadowPriceAwareRequestScore: Boolean = true,
    val integralityTolerance: InfraNumber = InfraNumber(1e-6),
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
    private val demandEntries: List<Bpp3dDemandEntry<InfraNumber>> = demandEntriesFromItems(itemDemands),
    private val finalBins: List<LayerBin> = emptyList(),
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
            demandEntries: List<Bpp3dDemandEntry<InfraNumber>>,
            finalBins: List<LayerBin> = emptyList(),
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
         * 从泛型需求条目创建执行器。
         * Create executor from generic demand entries.
         *
         * @param T 泛型数值类型 / generic numeric type
         * @param solver 列生成求解器 / column generation solver
         * @param itemDemands 泛型货物需求 / generic item demands
         * @param demandEntries 需求条目 / demand entries
         * @param finalBins 最终箱子 / final bins
         * @param config 执行器配置 / executor config
         * @param materialCache 物料缓存 / material cache
         * @param itemCache 货物缓存 / item cache
         * @return 执行器实例 / executor instance
         */
        fun <T : FloatingNumber<T>> fromGenericDemandEntries(
            solver: ColumnGenerationSolver,
            itemDemands: List<Pair<GenericItem<T>, UInt64>>,
            demandEntries: List<Bpp3dDemandEntry<InfraNumber>>,
            finalBins: List<LayerBin> = emptyList(),
            config: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig(),
            materialCache: MutableMap<GenericMaterial<T>, Material<InfraNumber>> = LinkedHashMap(),
            itemCache: MutableMap<GenericItem<T>, ActualItem> = LinkedHashMap()
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
    fun rmpSolver(): ColumnGenerationRmpSolver<InfraNumber> {
        return ColumnGenerationRmpSolver { state ->
            val artifacts = buildRmpArtifacts(state)
            val solved = ensureRet(
                solver.solveLPV(
                name = "${config.rmpSolveNamePrefix}-${state.iteration}",
                metaModel = artifacts.model,
                toLogModel = config.rmpToLogModel
                ),
                stage = "solve LP"
            )
            val shadowPriceMap = BPP3DShadowPriceMap()
            ensureTry(
                artifacts.demandConstraint.refresh(
                    shadowPriceMap = shadowPriceMap,
                    model = artifacts.model,
                    shadowPrices = dualSolutionToMetaUnchecked(extractDualSolutionMap(solved))
                ),
                stage = "refresh demand shadow prices"
            )
            val shadowPrices = LinkedHashMap<DemandModeKey, InfraNumber>()
            for ((key, value) in shadowPriceMap.map) {
                val demandKey = key as? DemandShadowPriceKey ?: continue
                val price = InfraNumber(value.price.toDouble())
                shadowPrices[DemandModeKey(demandKey.mode, demandKey.key, demandKey.quantityUnit)] = price
                if (demandKey.quantityUnit != null) {
                    shadowPrices.putIfAbsent(
                        DemandModeKey(demandKey.mode, demandKey.key),
                        price
                    )
                }
            }
            ColumnGenerationLpResult(
                shadowPrices = shadowPrices,
                objective = InfraNumber(solved.obj.toDouble()),
                info = mapOf(
                    "solver" to solver.name,
                    "model" to artifacts.model.name,
                    "lp_time_ms" to solved.time.inWholeMilliseconds.toString(),
                    "lp_gap" to solved.gap.toString(),
                    "lp_objective" to solved.obj.toString(),
                    "continuous_radius_solver_prototype_count" to state.continuousRadiusSolverPrototypes.size.toString(),
                    "continuous_radius_solver_prototype_variables" to state.continuousRadiusSolverPrototypes.joinToString("|") { it.variableName }
                ) + artifacts.continuousRadiusVariablePlan.info()
                    + extractContinuousRadiusSolverResults(artifacts.model, artifacts.continuousRadiusVariables)
                        .mapKeys { (name, _) -> "continuous_radius_solver_selected_$name" }
                        .mapValues { (_, value) -> value.toString() }
            )
        }
    }

    /**
     * 创建最终 MILP 求解器。
     * Create final MILP solver.
     *
     * @return 最终 MILP 求解器 / final MILP solver
     */
    fun finalSolver(): ColumnGenerationFinalSolver<InfraNumber> {
        return ColumnGenerationFinalSolver { state ->
            val bins = if (finalBins.isNotEmpty()) {
                finalBins
            } else {
                fallbackFinalBins(state.columns)
            }
            if (bins.isEmpty()) {
                return@ColumnGenerationFinalSolver ColumnGenerationFinalResult(
                    columns = state.columns,
                    bins = emptyList(),
                    objective = null,
                    info = mapOf("skipped" to "no_final_bins")
                )
            }

            val model = newModel("${config.finalSolveNamePrefix}-${state.iteration}")
            val finalContinuousRadiusVariables = continuousRadiusSolverVariables(
                prototypes = state.continuousRadiusSolverPrototypes
            )
            val continuousRadiusVariablePlan = continuousRadiusSolverVariableRegistrationPlan(
                prototypes = state.continuousRadiusSolverPrototypes,
                solverVariables = finalContinuousRadiusVariables
            )
            val assignment = PreciseAssignment(
                bins = bins,
                layers = state.columns
            )
            ensureTry(assignment.register(model), "register precise assignment")

            val load = PreciseLoad(
                demandEntries = demandEntries,
                layers = state.columns,
                assignment = assignment,
                overEnabled = false,
                lessEnabled = true
            )
            ensureTry(load.register(model), "register precise load")

            val demandConstraint = itemDemandConstraint(
                load = load,
                demandEntries = demandEntries
            )
            demandConstraint.register(model)
            ensureTry(demandConstraint.invoke(model), "build precise demand constraint")

            val capacity = PreciseLoadCapacity(
                bins = bins,
                layers = state.columns,
                assignment = assignment
            )
            ensureTry(capacity.register(model), "register precise capacity")
            if (config.enableFinalBinDepthConstraint) {
                ensureTry(
                    BinDepthConstraint(
                        bins = bins,
                        capacity = capacity
                    ).invoke(model),
                    "build final bin depth constraint"
                )
            }
            if (config.enableFinalBinCapacityConstraint) {
                ensureTry(
                    BinCapacityConstraint(
                        bins = bins,
                        capacity = capacity
                    ).invoke(model),
                    "build final bin capacity constraint"
                )
            }

            ensureTry(
                BinAmountMinimization(
                    bins = bins,
                    assignment = assignment,
                    coefficient = { config.finalBinAmountCoefficient }
                ).invoke(model),
                "build final objective"
            )

            registerContinuousRadiusVariables(model, finalContinuousRadiusVariables)

            val solved = ensureRet(
                solver.solveMILPV(
                name = "${config.finalSolveNamePrefix}-${state.iteration}",
                metaModel = model,
                toLogModel = config.finalToLogModel
                ),
                stage = "solve MILP"
            )
            model.setSolution(normalizeScalarSolution(solved.solution))
            val selectedBins = collectSelectedBins(model, bins, state.columns, assignment)
            config.depthBoundaryLayerOrientationPolicy?.ensureSatisfied(selectedBins)
            val selectedColumns = collectSelectedColumns(
                model = model,
                columns = state.columns,
                assignment = assignment,
                binAmount = bins.size
            )
            ColumnGenerationFinalResult(
                columns = if (selectedColumns.isNotEmpty()) selectedColumns else state.columns,
                bins = selectedBins,
                objective = InfraNumber(solved.obj.toDouble()),
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
                ) + continuousRadiusVariablePlan.info()
                    + extractContinuousRadiusSolverResults(model, finalContinuousRadiusVariables)
                        .mapKeys { (name, _) -> "continuous_radius_solver_selected_$name" }
                        .mapValues { (_, value) -> value.toString() }
            )
        }
    }

    /**
     * 创建层请求构建器。
     * Create layer request builder.
     *
     * @return 层请求构建器 / layer request builder
     */
    fun requestBuilder(): ColumnGenerationLayerRequestBuilder<InfraNumber> {
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

    private fun layerGenerationCandidateBin(): BinType? {
        return finalBins.firstOrNull()?.shape
    }

    private data class RmpArtifacts(
        val model: LinearMetaModel<InfraNumber>,
        val demandConstraint: ItemDemandConstraint,
        val continuousRadiusVariablePlan: ContinuousRadiusSolverVariableRegistrationPlan,
        val continuousRadiusVariables: List<ContinuousRadiusSolverVariable>
    )

    private suspend fun buildRmpArtifacts(
        state: ColumnGenerationState<InfraNumber>
    ): RmpArtifacts {
        val model = newModel("${config.rmpSolveNamePrefix}-${state.iteration}")
        val aggregation = LayerAggregation()
        val assignment = ImpreciseAssignment(
            items = itemDemands.toMap(),
            aggregation = aggregation
        )
        ensureTry(assignment.register(model), "register imprecise assignment")

        val load = ImpreciseLoad(
            demandEntries = demandEntries,
            assignment = assignment,
            overEnabled = false,
            lessEnabled = true
        )
        ensureTry(load.register(model), "register imprecise load")

        if (state.columns.isNotEmpty()) {
            val added = ensureRet(
                assignment.addColumns(
                    iteration = UInt64.zero,
                    newLayers = state.columns,
                    model = model
                ),
                "add rmp columns"
            )
            if (added.isNotEmpty()) {
                ensureRet(
                    load.addColumns(
                        iteration = UInt64.zero,
                        newLayers = added,
                        model = model
                    ),
                    "refresh rmp load columns"
                )
            }
        }

        val demandConstraint = itemDemandConstraint(
            load = load,
            demandEntries = demandEntries
        )
        demandConstraint.register(model)
        ensureTry(demandConstraint.invoke(model), "build rmp demand constraint")

        val volumeMinimization = itemVolumeMinimization(
            assignment = assignment,
            coefficient = config.rmpVolumeCoefficient
        )
        volumeMinimization.register(model)
        ensureTry(volumeMinimization.invoke(model), "build rmp objective")

        val continuousRadiusVariables = continuousRadiusSolverVariables(
            prototypes = state.continuousRadiusSolverPrototypes
        )
        registerContinuousRadiusVariables(model, continuousRadiusVariables)

        val continuousRadiusVariablePlan = continuousRadiusSolverVariableRegistrationPlan(
            prototypes = state.continuousRadiusSolverPrototypes,
            solverVariables = continuousRadiusVariables
        )

        return RmpArtifacts(
            model = model,
            demandConstraint = demandConstraint,
            continuousRadiusVariablePlan = continuousRadiusVariablePlan,
            continuousRadiusVariables = continuousRadiusVariables
        )
    }

    /**
     * 注册连续半径 solver 变量及其约束到模型。
     * Register continuous-radius solver variables and their constraints into the model.
     */
    private fun registerContinuousRadiusVariables(
        model: LinearMetaModel<InfraNumber>,
        solverVariables: List<ContinuousRadiusSolverVariable>
    ) {
        for (solverVar in solverVariables) {
            val proto = solverVar.prototype
            model.add(solverVar.variable)
            // Lower bound: 1*x >= lowerBound
            proto.radiusLowerBound?.let { lb ->
                val lhs = LinearPolynomial(
                    listOf(LinearMonomial(InfraNumber.one, solverVar.variable)),
                    InfraNumber.zero
                )
                val rhs = LinearPolynomial(emptyList(), lb.value)
                ensureTry(
                    model.addConstraint(
                        relation = LinearInequality(lhs, rhs, Comparison.GE),
                        name = "${proto.variableName}_lb"
                    ),
                    "register continuous radius lower bound for ${proto.variableName}"
                )
            }
            // Upper bound: 1*x <= upperBound
            proto.radiusUpperBound?.let { ub ->
                val lhs = LinearPolynomial(
                    listOf(LinearMonomial(InfraNumber.one, solverVar.variable)),
                    InfraNumber.zero
                )
                val rhs = LinearPolynomial(emptyList(), ub.value)
                ensureTry(
                    model.addConstraint(
                        relation = LinearInequality(lhs, rhs, Comparison.LE),
                        name = "${proto.variableName}_ub"
                    ),
                    "register continuous radius upper bound for ${proto.variableName}"
                )
            }
            // Target: 1*x == initialRadius (for production-ready variables with a concrete selected radius)
            if (proto.isProductionReady) {
                proto.initialRadius?.let { ir ->
                    val lhs = LinearPolynomial(
                        listOf(LinearMonomial(InfraNumber.one, solverVar.variable)),
                        InfraNumber.zero
                    )
                    val rhs = LinearPolynomial(emptyList(), ir.value)
                    ensureTry(
                        model.addConstraint(
                            relation = LinearInequality(lhs, rhs, Comparison.EQ),
                            name = "${proto.variableName}_target"
                        ),
                        "register continuous radius target for ${proto.variableName}"
                    )
                }
            }
        }
    }

    /**
     * 从模型中提取连续半径 solver 变量的求解结果。
     * Extract continuous-radius solver variable results from the model.
     */
    private fun extractContinuousRadiusSolverResults(
        model: LinearMetaModel<InfraNumber>,
        solverVariables: List<ContinuousRadiusSolverVariable>
    ): Map<String, InfraNumber> {
        val results = LinkedHashMap<String, InfraNumber>()
        for (solverVar in solverVariables) {
            val proto = solverVar.prototype
            val token = model.tokens.find(solverVar.variable) ?: continue
            val value = token.doubleResult?.let { InfraNumber(it) } ?: continue
            results[proto.variableName] = value
        }
        return results
    }

    private fun collectSelectedColumns(
        model: LinearMetaModel<InfraNumber>,
        columns: List<BinLayer>,
        assignment: PreciseAssignment,
        binAmount: Int
    ): List<BinLayer> {
        val selected = ArrayList<BinLayer>()
        for ((columnIndex, column) in columns.withIndex()) {
            var amount = InfraNumber.zero
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
        model: LinearMetaModel<InfraNumber>,
        bins: List<LayerBin>,
        columns: List<BinLayer>,
        assignment: PreciseAssignment
    ): List<LayerBin> {
        val selected = ArrayList<LayerBin>()
        for ((binIndex, baseBin) in bins.withIndex()) {
            val placements = ArrayList<BinLayerPlacement>()
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
                        shape = baseBin.shape,
                        units = placements,
                        batchNo = baseBin.batchNo
                    )
                )
            }
        }
        return selected
    }

    private fun fallbackFinalBins(columns: List<BinLayer>): List<LayerBin> {
        return columns.mapNotNull { layer ->
            val binShape = layer.bin ?: return@mapNotNull null
            layerBinOf(
                shape = binShape,
                units = emptyList()
            )
        }
    }

    private fun tokenValue(
        model: LinearMetaModel<InfraNumber>,
        variable: AbstractVariableItem<*, *>
    ): InfraNumber {
        val token = model.tokens.find(variable) ?: return InfraNumber.zero
        return token.doubleResult?.let { InfraNumber(it) } ?: InfraNumber.zero
    }

    private fun normalizeScalarSolution(values: List<*>): List<InfraNumber> {
        return values.mapIndexed { index, value ->
            when (value) {
                is InfraNumber -> value
                is Number -> InfraNumber(value.toDouble())
                else -> throw IllegalStateException("unsupported solution value at $index: ${value?.javaClass?.name}")
            }
        }
    }

    private fun newModel(name: String): LinearMetaModel<InfraNumber> {
        return LinearMetaModel(
            name = name,
            objectCategory = ObjectCategory.Minimum,
            configuration = MetaModelConfiguration(),
            converter = IntoValue.fromConverter(InfraNumber)
        )
    }

    private fun ensureTry(result: Try, stage: String) {
        when (result) {
            is Ok -> {}
            is Failed -> throw IllegalStateException("$stage failed: ${result.error}")
            is Fatal -> throw IllegalStateException("$stage fatal: ${result.errors}")
        }
    }

    private fun <T> ensureRet(result: Ret<T>, stage: String): T {
        return when (result) {
            is Ok -> result.value
            is Failed -> throw IllegalStateException("$stage failed: ${result.error}")
            is Fatal -> throw IllegalStateException("$stage fatal: ${result.errors}")
        }
    }
}


