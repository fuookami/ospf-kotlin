/**
 * 列生成标准执行器。 / Column generation standard executors.
 *
 * 对应 BPP3D 的 `ColumnGenerationStandardExecutors`：提供 RMP 求解器、最终 MILP 求解器与层请求构建器的标准
 * 装配。模型装配全部委托给 `LayerAssignmentContext`，求解调用委托给 `ColumnGenerationSolver`。
 * / Mirrors BPP3D `ColumnGenerationStandardExecutors`: provides the standard assembly of the RMP solver, the
 * final MILP solver and the layer request builder. Model assembly is delegated to `LayerAssignmentContext` and
 * solving is delegated to `ColumnGenerationSolver`.
*/
package fuookami.ospf.kotlin.framework.bpp2d.application.service

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.core.model.mechanism.MetaModelConfiguration
import fuookami.ospf.kotlin.core.model.mechanism.toMeta
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ItemId
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Sheet
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetLayer
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetType
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.LayerAssignmentContext
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.Bpp2dLayerGenerationRequest
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.DemandModeKey
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver

/**
 * 标准执行器配置。 / Standard executor configuration.
 *
 * @property rmpSolveNamePrefix RMP 求解名称前缀 / RMP solve name prefix
 * @property finalSolveNamePrefix 最终求解名称前缀 / final solve name prefix
 * @property rmpToLogModel RMP 是否输出模型日志 / whether to log the RMP model
 * @property finalToLogModel 最终求解是否输出模型日志 / whether to log the final model
 * @property rmpAreaCoefficient RMP 面积系数 / RMP area coefficient
 * @property finalSheetAmountCoefficient 最终板材数量系数 / final sheet amount coefficient
 * @property enableFinalCapacityConstraint 是否启用板材容量约束 / whether the sheet capacity constraint is enabled
 * @property integralityTolerance 整性容差 / integrality tolerance
*/
data class ColumnGenerationStandardExecutorConfig(
    val rmpSolveNamePrefix: String = "bpp2d-rmp",
    val finalSolveNamePrefix: String = "bpp2d-final",
    val rmpToLogModel: Boolean = false,
    val finalToLogModel: Boolean = false,
    val rmpAreaCoefficient: FltX = FltX.one,
    val finalSheetAmountCoefficient: FltX = FltX.one,
    val enableFinalCapacityConstraint: Boolean = true,
    val integralityTolerance: FltX = FltX(1e-6)
)

/**
 * 列生成标准执行器。 / Column generation standard executors.
 *
 * @property solver 列生成求解器 / column generation solver
 * @property items 物品件数需求 / item amount demands
 * @property itemCatalogue 物品目录，供候选生成使用 / item catalogue used by candidate generation
 * @property demandEntries 需求项列表 / demand entry list
 * @property sheetTypes 板材类型列表 / sheet type list
 * @property finalSheets 预置的最终板材 / pre-provided final sheets
 * @property config 执行器配置 / executor config
*/
class ColumnGenerationStandardExecutors(
    private val solver: ColumnGenerationSolver,
    private val items: Map<ItemId, UInt64>,
    private val itemCatalogue: Map<ItemId, ActualItem<FltX>>,
    private val demandEntries: List<Bpp2dAssignmentDemandEntry>,
    private val sheetTypes: List<SheetType<FltX>>,
    private val finalSheets: List<Sheet<FltX>> = emptyList(),
    private val config: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig()
) {

    /**
     * 创建 RMP 求解器。 / Create the RMP solver.
     *
     * @return RMP 求解器 / RMP solver
    */
    fun rmpSolver(): ColumnGenerationRmpSolver {
        return ColumnGenerationRmpSolver { state ->
            val model = newModel("${config.rmpSolveNamePrefix}-${state.iteration}")
            val context = LayerAssignmentContext(
                items = items,
                aggregation = LayerAggregation()
            )
            val artifacts = when (
                val result = context.registerRmp(
                    model = model,
                    demandEntries = demandEntries
                )
            ) {
                is Ok -> result.value
                is Failed -> return@ColumnGenerationRmpSolver Failed(result.error)
                is Fatal -> return@ColumnGenerationRmpSolver Fatal(result.errors)
            }

            if (state.columns.isNotEmpty()) {
                when (
                    val result = context.addColumns(
                        assignment = artifacts.assignment,
                        load = artifacts.load,
                        iteration = UInt64.zero,
                        newLayers = state.columns,
                        model = model
                    )
                ) {
                    is Ok -> {}
                    is Failed -> return@ColumnGenerationRmpSolver Failed(result.error)
                    is Fatal -> return@ColumnGenerationRmpSolver Fatal(result.errors)
                }
            }

            when (val result = context.registerAreaMinimization(artifacts.assignment, model)) {
                is Ok -> {}
                is Failed -> return@ColumnGenerationRmpSolver Failed(result.error)
                is Fatal -> return@ColumnGenerationRmpSolver Fatal(result.errors)
            }

            val lpResult = solver.solveLPAs(
                name = "${config.rmpSolveNamePrefix}-${state.iteration}",
                metaModel = model,
                toLogModel = config.rmpToLogModel
            )
            if (lpResult is Failed) return@ColumnGenerationRmpSolver Failed(lpResult.error)
            if (lpResult is Fatal) return@ColumnGenerationRmpSolver Fatal(lpResult.errors)
            val solved = (lpResult as Ok).value

            val shadowPriceMap = Bpp2dShadowPriceMap()
            val dualSolution: MetaDualSolution = solved.dualSolution.toMeta()
            val refreshResult = artifacts.demandConstraint.refresh(
                shadowPriceMap = shadowPriceMap,
                model = model,
                shadowPrices = dualSolution
            )
            if (refreshResult is Failed) return@ColumnGenerationRmpSolver Failed(refreshResult.error)
            if (refreshResult is Fatal) return@ColumnGenerationRmpSolver Fatal(refreshResult.errors)

            val shadowPrices = LinkedHashMap<DemandModeKey, FltX>()
            for ((key, value) in shadowPriceMap.map) {
                val demandKey = key as? Bpp2dDemandShadowPriceKey ?: continue
                shadowPrices[
                    DemandModeKey(
                        mode = demandKey.mode.name,
                        key = demandKey.itemId.value
                    )
                ] = FltX(value.price.toDouble())
            }

            Ok(
                ColumnGenerationLpResult(
                    shadowPrices = shadowPrices,
                    objective = solved.result.solution?.objective ?: FltX.zero,
                    info = mapOf(
                        "solver" to solver.name,
                        "model" to model.name,
                        "lp_time_ms" to (solved.result.statistics.solveTime?.inWholeMilliseconds ?: 0L).toString(),
                        "lp_gap" to (solved.result.statistics.gap ?: Flt64Zero()).toString(),
                        "lp_objective" to (solved.result.solution?.objective ?: FltX.zero).toString(),
                        "column_count" to state.columns.size.toString()
                    )
                )
            )
        }
    }

    /**
     * 创建最终 MILP 求解器。 / Create the final MILP solver.
     *
     * @return 最终 MILP 求解器 / final MILP solver
    */
    fun finalSolver(): ColumnGenerationFinalSolver {
        return ColumnGenerationFinalSolver { state ->
            if (state.columns.isEmpty()) {
                return@ColumnGenerationFinalSolver Ok(
                    ColumnGenerationFinalResult(
                        columns = state.columns,
                        sheets = emptyList(),
                        objective = null,
                        info = mapOf("skipped" to "no_columns")
                    )
                )
            }

            val model = newModel("${config.finalSolveNamePrefix}-${state.iteration}")
            val context = LayerAssignmentContext(items = items)
            val artifacts = when (
                val result = context.registerFinal(
                    model = model,
                    layers = state.columns,
                    demandEntries = demandEntries,
                    enableCapacityConstraint = config.enableFinalCapacityConstraint
                )
            ) {
                is Ok -> result.value
                is Failed -> return@ColumnGenerationFinalSolver Failed(result.error)
                is Fatal -> return@ColumnGenerationFinalSolver Fatal(result.errors)
            }

            when (
                val result = context.registerSheetAmountMinimization(
                    assignment = artifacts.assignment,
                    layers = state.columns,
                    model = model,
                    coefficient = config.finalSheetAmountCoefficient
                )
            ) {
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

            // 求解结果必须回写到模型令牌，否则按变量取值收集板材会得到全零。
            // The solve result must be written back into the model tokens, otherwise collecting sheets by variable
            // value yields zeros.
            when (val normalized = normalizeScalarSolution(solved.values)) {
                is Ok -> model.setSolution(normalized.value)
                is Failed -> return@ColumnGenerationFinalSolver Failed(normalized.error)
                is Fatal -> return@ColumnGenerationFinalSolver Fatal(normalized.errors)
            }

            // 每列的份数就是该列要产出的板材数量；一份对应一块板材。
            // The copies of a column are the sheets it produces; one copy equals one sheet.
            val copies = state.columns.mapIndexed { index, _ ->
                tokenValue(model, artifacts.assignment.y[index])
            }
            val selectedPicks = state.columns.indices
                .filter { copies[it] > config.integralityTolerance }
                .map { Triple(it, state.columns[it], copies[it]) }
            val selectedColumns = selectedPicks.map { it.second }
            val selectedSheets = buildSheets(
                picks = selectedPicks,
                fallbackType = sheetTypes.firstOrNull { it.isMain } ?: sheetTypes.firstOrNull()
            )

            Ok(
                ColumnGenerationFinalResult(
                    columns = if (selectedColumns.isNotEmpty()) selectedColumns else state.columns,
                    sheets = selectedSheets,
                    objective = solved.solution?.objective ?: FltX.zero,
                    info = mapOf(
                        "solver" to solver.name,
                        "model" to model.name,
                        "milp_time_ms" to (solved.statistics.solveTime?.inWholeMilliseconds ?: 0L).toString(),
                        "milp_gap" to (solved.statistics.gap ?: Flt64Zero()).toString(),
                        "milp_objective" to (solved.solution?.objective ?: FltX.zero).toString(),
                        "selected_sheet_count" to selectedSheets.size.toString(),
                        "selected_layer_count" to selectedColumns.size.toString()
                    )
                )
            )
        }
    }

    /**
     * 创建层请求构建器。 / Create the layer request builder.
     *
     * @return 层请求构建器 / layer request builder
    */
    fun requestBuilder(): ColumnGenerationLayerRequestBuilder {
        val candidateSheet = sheetTypes.firstOrNull { it.isMain } ?: sheetTypes.firstOrNull()
        // 候选生成需要物品清单；缺少物品时生成器无法产出任何候选。
        // Candidate generation needs the item list; without it the generators can produce nothing.
        val generationItems = buildGenerationItems()
        return ColumnGenerationLayerRequestBuilder { state, cgConfig ->
            Bpp2dLayerGenerationRequest(
                iteration = state.iteration,
                sheet = candidateSheet,
                items = generationItems,
                existingLayers = state.columns,
                shadowPrices = state.shadowPrices,
                maxCandidates = cgConfig.maxColumnsPerIteration
            )
        }
    }

    /**
     * 由列推导回退板材：每种列所属板材类型各取一块，并为每列各提供一块候选板材。
     * / Derive fallback sheets from the columns: one per distinct sheet type plus one per column.
     *
     * @param columns 列列表 / column list
     * @return 候选板材列表 / candidate sheet list
    */
    private fun fallbackFinalSheets(columns: List<Layer<FltX>>): List<Sheet<FltX>> {
        val result = ArrayList<Sheet<FltX>>()
        val defaultType = sheetTypes.firstOrNull { it.isMain } ?: sheetTypes.firstOrNull()
        for (column in columns) {
            val type = column.sheet ?: defaultType ?: continue
            result.add(
                Sheet(
                    type = type,
                    layers = listOf(
                        SheetLayer(
                            type = type,
                            shape = fuookami.ospf.kotlin.framework.bpp2d.infrastructure.Sheet2Shape(
                                width = type.width,
                                height = type.height
                            ),
                            layers = listOf(column)
                        )
                    )
                )
            )
        }
        return result
    }

    /**
     *
     * 每一份列对应一块板材，块内只包含该列对应的一个层。
     * / Each copy of a column corresponds to one sheet containing that single layer.
     *
     * @param picks 列下标、层与份数三元组 / triples of column index, layer and copies
     * @param fallbackType 缺省板材类型 / fallback sheet type
     * @return 板材列表 / sheet list
    */
    private fun buildSheets(
        picks: List<Triple<Int, Layer<FltX>, FltX>>,
        fallbackType: SheetType<FltX>?
    ): List<Sheet<FltX>> {
        val result = ArrayList<Sheet<FltX>>()
        for ((_, layer, copies) in picks) {
            val type = layer.sheet ?: fallbackType ?: continue
            val count = copies.toDouble().toInt().coerceAtLeast(1)
            repeat(count) {
                result.add(
                    Sheet(
                        type = type,
                        layers = listOf(
                            SheetLayer(
                                type = type,
                                shape = Sheet2Shape(
                                    width = layer.shape.width,
                                    height = layer.shape.height
                                ),
                                layers = listOf(layer)
                            )
                        )
                    )
                )
            }
        }
        return result
    }

    private fun tokenValue(
        model: LinearMetaModel<FltX>,
        variable: fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>
    ): FltX {
        val token = model.tokens.find(variable) ?: return FltX.zero
        return token.doubleResult?.let { FltX(it) } ?: FltX.zero
    }

    /**
     * 创建新的线性元模型。 / Create a new linear meta model.
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

    /**
     * 零 `Flt64` 常量，用于缺省统计值。 / Zero `Flt64` constant used for missing statistics.
     *
     * @return 零值 / zero value
    */
    private fun Flt64Zero(): fuookami.ospf.kotlin.math.algebra.number.Flt64 {
        return fuookami.ospf.kotlin.math.algebra.number.Flt64.zero
    }

    /**
     * 标准化求解器标量解。 / Normalize the solver scalar solution.
     *
     * @param values 原始解值列表 / raw solution value list
     * @return 标准化后的 `FltX` 列表或失败 / normalized `FltX` list, or failure
    */
    private fun normalizeScalarSolution(values: List<*>): Ret<List<FltX>> {
        val normalized = ArrayList<FltX>(values.size)
        for ((index, value) in values.withIndex()) {
            when (value) {
                is FltX -> normalized.add(value)
                is Number -> normalized.add(FltX(value.toDouble()))
                else -> return Failed(
                    ErrorCode.IllegalArgument,
                    "不支持的解值类型 / unsupported solution value at $index: ${value?.javaClass?.name}"
                )
            }
        }
        return ok(normalized)
    }

    /**
     * 由物品件数需求与物品目录构造候选生成输入。 / Build the candidate generation input from the item demands and
     * the item catalogue.
     *
     * @return 物品与件数列表 / item and amount list
    */
    private fun buildGenerationItems(): List<Pair<ActualItem<FltX>, Int>> {
        val result = ArrayList<Pair<ActualItem<FltX>, Int>>(items.size)
        for ((itemId, amount) in items) {
            val item = itemCatalogue[itemId] ?: continue
            result.add(item to amount.toULong().toInt())
        }
        return result
    }
}
