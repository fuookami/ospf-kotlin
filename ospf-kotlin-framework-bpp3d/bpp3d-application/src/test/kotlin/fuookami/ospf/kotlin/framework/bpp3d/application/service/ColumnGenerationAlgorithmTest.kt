package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.intermediate.Cell
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.Linear
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Package
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerationRequest
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerationResult
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.DemandModeKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dDemandEntry
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits.DemandShadowPriceKey
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.quantities.quantity.times
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import kotlin.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ColumnGenerationAlgorithmTest {
    private object CargoAttr : fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute

    private fun packageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(Flt64.zero),
            hangingPolicy = AbsoluteHangingPolicy(Flt64.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(id: String, material: Material): ActualItem {
        val pack = Package.innerPackage(
            shape = PackageShape(
                width = 1.0 * Meter,
                height = 1.0 * Meter,
                depth = 1.0 * Meter,
                weight = 1.0 * Kilogram,
                packageType = PackageType.CartonContainer
            ),
            materials = mapOf(material to UInt64.one)
        )
        return ActualItem(
            id = id,
            name = id,
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute()
        )
    }

    private fun layerBin(items: List<ActualItem>): Bin<BinLayer> {
        val binType = BinType(
            width = 3.0 * Meter,
            height = 3.0 * Meter,
            depth = 3.0 * Meter,
            capacity = 100.0 * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-A"
        )
        val placements = items.mapIndexed { index, item ->
            QuantityPlacement3(
                view = item.view(Orientation.Upright),
                position = point3(x = index.toDouble() * Meter, y = 0.0 * Meter, z = 0.0 * Meter)
            )
        }
        val layer = BinLayer(
            iteration = Int64.zero,
            from = ColumnGenerationAlgorithmTest::class,
            bin = binType,
            shape = Container3Shape(binType),
            units = placements
        )
        return Bin(
            shape = binType,
            units = listOf(
                QuantityPlacement3(
                    view = layer.view(Orientation.Upright)!!,
                    position = point3()
                )
            )
        )
    }

    @Test
    fun columnGenerationAlgorithmShouldLiveInApplicationService() {
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<Double> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<Double>): List<Bpp3dLayerGenerationResult<Double>> {
                    return emptyList()
                }
            }
        )
        assertNotNull(algorithm)
    }

    @Test
    fun algorithmShouldStopWhenNoAcceptedColumns() = runBlocking {
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<Double> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<Double>): List<Bpp3dLayerGenerationResult<Double>> {
                    return emptyList()
                }
            }
        )

        val result = algorithm.solve(items = emptyList<Item>())
        assertEquals(0, result.iterationCount)
        assertEquals(0, result.columns.size)
        assertEquals(1, result.lpSolvedTimes)
        assertTrue(result.finalSolved)
    }

    @Test
    fun algorithmShouldInvokeFinalSolveAnalyzerAndHeartbeatCallbacks() = runBlocking {
        val finalSolveCounter = AtomicInteger(0)
        val analyzeCounter = AtomicInteger(0)
        val heartbeatCounter = AtomicInteger(0)
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<Double> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<Double>): List<Bpp3dLayerGenerationResult<Double>> {
                    return emptyList()
                }
            },
            solveFinalMilp = {
                finalSolveCounter.incrementAndGet()
            },
            analyzeSolution = {
                analyzeCounter.incrementAndGet()
            },
            onIterationHeartbeat = {
                heartbeatCounter.incrementAndGet()
            }
        )

        val result = algorithm.solve(items = emptyList<Item>())
        assertEquals(1, finalSolveCounter.get())
        assertEquals(1, analyzeCounter.get())
        assertEquals(0, heartbeatCounter.get())
        assertTrue(result.finalSolved)
    }

    @Test
    fun rmpSolverAndRequestBuilderShouldBePreferredWhenProvided() = runBlocking {
        val legacyRmpCounter = AtomicInteger(0)
        val rmpSolverCounter = AtomicInteger(0)
        val requestBuilderCounter = AtomicInteger(0)
        var observedMaxCandidates = -1
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<Double> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<Double>): List<Bpp3dLayerGenerationResult<Double>> {
                    observedMaxCandidates = request.maxCandidates
                    return emptyList()
                }
            },
            rmpSolver = ColumnGenerationRmpSolver {
                rmpSolverCounter.incrementAndGet()
                ColumnGenerationLpResult(
                    shadowPrices = emptyMap<DemandModeKey, Double>(),
                    objective = 42.0
                )
            },
            layerRequestBuilder = ColumnGenerationLayerRequestBuilder { state, items, _ ->
                requestBuilderCounter.incrementAndGet()
                Bpp3dLayerGenerationRequest(
                    iteration = state.iteration,
                    items = items,
                    existingLayers = state.columns,
                    shadowPrices = state.shadowPrices,
                    maxCandidates = 1
                )
            },
            solveRmpWithResult = {
                legacyRmpCounter.incrementAndGet()
                ColumnGenerationLpResult(emptyMap())
            }
        )

        val result = algorithm.solve(
            items = emptyList(),
            config = ColumnGenerationConfig(maxColumnsPerIteration = 99)
        )
        assertEquals(1, rmpSolverCounter.get())
        assertEquals(0, legacyRmpCounter.get())
        assertEquals(1, requestBuilderCounter.get())
        assertEquals(1, observedMaxCandidates)
        assertEquals(listOf(42.0), result.lpObjectives)
    }

    @Test
    fun finalSolverReturnedBinsShouldReachPackingAnalyzer() = runBlocking {
        val material = Material(
            no = MaterialNo("M-1"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-1",
            weight = 0.5 * Kilogram
        )
        val bin = layerBin(listOf(item("item-1", material)))
        val analyzer = ColumnGenerationPackingAnalyzer()
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<Flt64> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<Flt64>): List<Bpp3dLayerGenerationResult<Flt64>> {
                    return emptyList()
                }
            },
            finalMilpSolver = ColumnGenerationFinalSolver {
                ColumnGenerationFinalResult(
                    columns = listOf(bin.units.first().unit),
                    bins = listOf(bin)
                )
            },
            solutionAnalyzer = analyzer
        )

        val result = algorithm.solve(items = emptyList())
        val snapshot = analyzer.latest
        assertNotNull(snapshot)
        assertEquals(1, snapshot.bins.size)
        assertEquals("1", snapshot.schema.kpi["bin_count"])
        assertTrue(result.finalSolved)
    }

    @Test
    fun standardExecutorsShouldBridgeSolverToRmpAndFinal() = runBlocking {
        val material = Material(
            no = MaterialNo("M-2"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-2",
            weight = 0.5 * Kilogram
        )
        val actualItem = item("item-2", material)
        val seedBin = layerBin(listOf(actualItem))
        val rawSeedLayer = seedBin.units.first().unit
        val seedLayer = BinLayer(
            iteration = rawSeedLayer.iteration,
            from = rawSeedLayer.from,
            bin = seedBin.shape,
            shape = rawSeedLayer.shape,
            units = rawSeedLayer.units
        )
        val finalBin: Bin<BinLayer> = Bin(
            shape = seedBin.shape,
            units = emptyList<QuantityPlacement3<BinLayer>>(),
            batchNo = seedBin.batchNo
        )
        val demandValue = Flt64.one
        val demandEntries = listOf(
            Bpp3dDemandEntry(
                mode = Bpp3dDemandMode.ItemAmount,
                key = Bpp3dDemandKey.Item(actualItem),
                demand = demandValue,
                demandRange = ValueRange(
                    demandValue,
                    demandValue,
                    Interval.Closed,
                    Interval.Closed,
                    Flt64
                ).value!!
            )
        )
        val solver = object : ColumnGenerationSolver {
            override val name: String = "stub-cg-solver"

            override suspend fun solveMILP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<FeasibleSolverOutput<Flt64>> {
                val solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.one }
                val output: FeasibleSolverOutput<Flt64> = FeasibleSolverOutput(
                    obj = Flt64(9.0),
                    solution = solution,
                    time = Duration.ZERO,
                    possibleBestObj = Flt64(9.0),
                    gap = Flt64.zero
                )
                return Ok(
                    output
                )
            }

            override suspend fun solveLP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<ColumnGenerationSolver.LPResult> {
                val tagged = metaModel.constraints.first { it.args is DemandShadowPriceKey }
                val dual = linkedMapOf<Constraint<Flt64, Linear>, Flt64>(
                    fakeConstraint(tagged) to Flt64(7.0)
                )
                val solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.zero }
                val output: FeasibleSolverOutput<Flt64> = FeasibleSolverOutput(
                    obj = Flt64(11.0),
                    solution = solution,
                    time = Duration.ZERO,
                    possibleBestObj = Flt64(11.0),
                    gap = Flt64.zero
                )
                return Ok(
                    ColumnGenerationSolver.LPResult(
                        result = output,
                        dualSolution = dual
                    )
                )
            }
        }
        val executors = ColumnGenerationStandardExecutors.fromDemandEntries(
            solver = solver,
            itemDemands = listOf(Pair(actualItem, UInt64.one)),
            demandEntries = demandEntries,
            finalBins = listOf(finalBin)
        )

        var capturedRequest: Bpp3dLayerGenerationRequest<Flt64>? = null
        var analyzedState: ColumnGenerationState<Flt64>? = null
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<Flt64> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<Flt64>): List<Bpp3dLayerGenerationResult<Flt64>> {
                    capturedRequest = request
                    return emptyList()
                }
            },
            rmpSolver = executors.rmpSolver(),
            finalMilpSolver = executors.finalSolver(),
            layerRequestBuilder = executors.requestBuilder(),
            solutionAnalyzer = ColumnGenerationSolutionAnalyzer { state ->
                analyzedState = state
            },
            initialColumns = { listOf(seedLayer) }
        )

        val result = algorithm.solve(items = listOf(actualItem))
        val request = capturedRequest
        val finalState = analyzedState
        assertNotNull(request)
        assertNotNull(finalState)
        assertEquals(listOf(Flt64(11.0)), result.lpObjectives)
        assertEquals(1, result.columns.size)
        assertEquals(1, finalState.bins.size)
        assertTrue(request.demandEntries.any { it.mode is Bpp3dDemandMode.ItemAmount })
        assertNotNull(request.scoreByShadowPrice)
        val demandKey = DemandModeKey(
            mode = Bpp3dDemandMode.ItemAmount,
            key = Bpp3dDemandKey.Item(actualItem)
        )
        assertEquals(7.0, (request.shadowPrices[demandKey] ?: Flt64.zero).toDouble(), 1e-10)
    }

    private fun fakeConstraint(origin: fuookami.ospf.kotlin.core.model.mechanism.MathConstraint): Constraint<Flt64, Linear> {
        return object : Constraint<Flt64, Linear> {
            override val lhs: List<Cell<Flt64>> = emptyList()
            override val sign: ConstraintRelation = ConstraintRelation.Equal
            override val rhs: Flt64 = Flt64.zero
            override val lazy: Boolean = false
            override val name: String = "fake-dual"
            override val origin: fuookami.ospf.kotlin.core.model.mechanism.MathConstraint = origin
            override val from: Pair<fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol<*>, Boolean>? = null

            override fun isTrue(): Boolean? = true
            override fun isTrue(results: List<Flt64>): Boolean? = true
        }
    }
}
