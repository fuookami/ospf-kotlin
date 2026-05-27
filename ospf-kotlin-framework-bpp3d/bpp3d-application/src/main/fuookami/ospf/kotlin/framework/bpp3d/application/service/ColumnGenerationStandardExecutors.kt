@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModelConfiguration
import fuookami.ospf.kotlin.core.model.mechanism.toMeta
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BPP3DShadowPriceArguments
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BPP3DShadowPriceMap
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayerView
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LayerBin
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
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits.DemandConstraint
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits.DemandShadowPriceKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits.VolumeMinimization
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerationRequest
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.DemandModeKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.LayerGenerationDemandEntry
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.shadowPriceAwareLayerScore
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraZero
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.quantities.quantity.minus
import fuookami.ospf.kotlin.quantities.quantity.plus
import kotlin.math.roundToInt

data class ColumnGenerationStandardExecutorConfig(
    val rmpSolveNamePrefix: String = "bpp3d-rmp",
    val finalSolveNamePrefix: String = "bpp3d-final",
    val rmpToLogModel: Boolean = false,
    val finalToLogModel: Boolean = false,
    val rmpVolumeCoefficient: Flt64 = Flt64.one,
    val finalBinAmountCoefficient: Flt64 = Flt64.one,
    val enableFinalBinDepthConstraint: Boolean = true,
    val enableFinalBinCapacityConstraint: Boolean = true,
    val enableShadowPriceAwareRequestScore: Boolean = true,
    val integralityTolerance: Flt64 = Flt64(1e-6)
)

class ColumnGenerationStandardExecutors(
    private val solver: ColumnGenerationSolver,
    private val itemDemands: List<Pair<Item, UInt64>>,
    private val demandEntries: List<Bpp3dDemandEntry> = demandEntriesFromItems(itemDemands),
    private val finalBins: List<LayerBin> = emptyList(),
    private val config: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig()
) {
    companion object {
        fun fromDemandEntries(
            solver: ColumnGenerationSolver,
            itemDemands: List<Pair<Item, UInt64>>,
            demandEntries: List<Bpp3dDemandEntry>,
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
    }

    fun rmpSolver(): ColumnGenerationRmpSolver<Flt64> {
        return ColumnGenerationRmpSolver { state ->
            val artifacts = buildRmpArtifacts(state)
            val solved = ensureRet(
                solver.solveLP(
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
                    shadowPrices = solved.dualSolution.toMeta()
                ),
                stage = "refresh demand shadow prices"
            )
            val shadowPrices = LinkedHashMap<DemandModeKey, Flt64>()
            for ((key, value) in shadowPriceMap.map) {
                val demandKey = key as? DemandShadowPriceKey ?: continue
                shadowPrices[DemandModeKey(demandKey.mode, demandKey.key, demandKey.quantityUnit)] = value.price
                if (demandKey.quantityUnit != null) {
                    shadowPrices.putIfAbsent(
                        DemandModeKey(demandKey.mode, demandKey.key),
                        value.price
                    )
                }
            }
            ColumnGenerationLpResult(
                shadowPrices = shadowPrices,
                objective = solved.obj,
                info = mapOf(
                    "solver" to solver.name,
                    "model" to artifacts.model.name,
                    "lp_time_ms" to solved.time.inWholeMilliseconds.toString(),
                    "lp_gap" to solved.gap.toString(),
                    "lp_objective" to solved.obj.toString()
                )
            )
        }
    }

    fun finalSolver(): ColumnGenerationFinalSolver<Flt64> {
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

            val demandConstraint = DemandConstraint<BPP3DShadowPriceArguments, Item>(
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

            val solved = ensureRet(
                solver.solveMILP(
                name = "${config.finalSolveNamePrefix}-${state.iteration}",
                metaModel = model,
                toLogModel = config.finalToLogModel
                ),
                stage = "solve MILP"
            )
            model.setSolution(normalizeFlt64Solution(solved.solution))
            val selectedBins = collectSelectedBins(model, bins, state.columns, assignment)
            val selectedColumns = collectSelectedColumns(
                model = model,
                columns = state.columns,
                assignment = assignment,
                binAmount = bins.size
            )
            ColumnGenerationFinalResult(
                columns = if (selectedColumns.isNotEmpty()) selectedColumns else state.columns,
                bins = selectedBins,
                objective = solved.obj,
                info = mapOf(
                    "solver" to solver.name,
                    "model" to model.name,
                    "milp_time_ms" to solved.time.inWholeMilliseconds.toString(),
                    "milp_gap" to solved.gap.toString(),
                    "milp_objective" to solved.obj.toString(),
                    "selected_bin_count" to selectedBins.size.toString(),
                    "selected_layer_count" to selectedColumns.size.toString()
                )
            )
        }
    }

    fun requestBuilder(): ColumnGenerationLayerRequestBuilder<Flt64> {
        val requestDemandEntries = demandEntries.map {
            LayerGenerationDemandEntry(it.mode, it.key, it.quantityUnit)
        }
        return ColumnGenerationLayerRequestBuilder { state, items, cgConfig ->
            Bpp3dLayerGenerationRequest(
                iteration = state.iteration,
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

    private data class RmpArtifacts(
        val model: LinearMetaModel<Flt64>,
        val demandConstraint: DemandConstraint<BPP3DShadowPriceArguments, Item>
    )

    private suspend fun buildRmpArtifacts(
        state: ColumnGenerationState<Flt64>
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

        val demandConstraint = DemandConstraint<BPP3DShadowPriceArguments, Item>(
            load = load,
            demandEntries = demandEntries
        )
        demandConstraint.register(model)
        ensureTry(demandConstraint.invoke(model), "build rmp demand constraint")

        val volumeMinimization = VolumeMinimization<BPP3DShadowPriceArguments, Item>(
            assignment = assignment,
            coefficient = config.rmpVolumeCoefficient
        )
        volumeMinimization.register(model)
        ensureTry(volumeMinimization.invoke(model), "build rmp objective")

        return RmpArtifacts(
            model = model,
            demandConstraint = demandConstraint
        )
    }

    private fun collectSelectedColumns(
        model: LinearMetaModel<Flt64>,
        columns: List<BinLayer>,
        assignment: PreciseAssignment,
        binAmount: Int
    ): List<BinLayer> {
        val selected = ArrayList<BinLayer>()
        for ((columnIndex, column) in columns.withIndex()) {
            var amount = Flt64.zero
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
        model: LinearMetaModel<Flt64>,
        bins: List<LayerBin>,
        columns: List<BinLayer>,
        assignment: PreciseAssignment
    ): List<LayerBin> {
        val selected = ArrayList<LayerBin>()
        for ((binIndex, baseBin) in bins.withIndex()) {
            val placements = ArrayList<QuantityPlacement3<BinLayer>>()
            var zCursor = baseBin.shape.depth - baseBin.shape.depth
            for ((columnIndex, column) in columns.withIndex()) {
                val raw = tokenValue(model, assignment.x[binIndex, columnIndex])
                val copies = raw.toDouble().roundToInt()
                if (copies <= 0) {
                    continue
                }
                repeat(copies) {
                    val layerCopy = column.copy()
                    placements.add(
                        QuantityPlacement3(
                            view = BinLayerView(layerCopy),
                            position = point3(z = zCursor)
                        )
                    )
                    zCursor = zCursor + layerCopy.depth
                }
            }
            if (placements.isNotEmpty()) {
                selected.add(
                    Bin(
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
            Bin(
                shape = binShape,
                units = emptyList()
            )
        }
    }

    private fun tokenValue(
        model: LinearMetaModel<Flt64>,
        variable: AbstractVariableItem<*, *>
    ): Flt64 {
        return model.tokens.find(variable)?.result ?: Flt64.zero
    }

    private fun normalizeFlt64Solution(values: List<*>): List<Flt64> {
        return values.mapIndexed { index, value ->
            when (value) {
                is Flt64 -> value
                is Number -> Flt64(value.toDouble())
                else -> throw IllegalStateException("unsupported solution value at $index: ${value?.javaClass?.name}")
            }
        }
    }

    private fun newModel(name: String): LinearMetaModel<Flt64> {
        return LinearMetaModel(
            name = name,
            objectCategory = ObjectCategory.Minimum,
            configuration = MetaModelConfiguration(),
            converter = IntoValue.Identity
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
