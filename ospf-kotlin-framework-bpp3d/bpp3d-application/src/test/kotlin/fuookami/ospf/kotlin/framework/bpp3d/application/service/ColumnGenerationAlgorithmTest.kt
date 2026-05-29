package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.intermediate.Cell
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.math.symbol.Linear
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
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Item as QuantityItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.BinLayer as QuantityBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.ItemPlacement as QuantityItemPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Material as QuantityMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Package as QuantityPackage
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.PackageShape as QuantityPackageShape
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
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
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
            deformationAttribute = LinearDeformationAttribute(InfraNumber.zero),
            hangingPolicy = AbsoluteHangingPolicy(InfraNumber.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(id: String, material: Material): ActualItem {
        val pack = Package.innerPackage(
            shape = PackageShape(
                width = InfraNumber.one * Meter,
                height = InfraNumber.one * Meter,
                depth = InfraNumber.one * Meter,
                weight = InfraNumber.one * Kilogram,
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

    private fun fixedDemandEntry(
        mode: Bpp3dDemandMode,
        key: Bpp3dDemandKey,
        demand: InfraNumber
    ): Bpp3dDemandEntry {
        return Bpp3dDemandEntry(
            mode = mode,
            key = key,
            demand = demand,
            demandRange = ValueRange(
                demand,
                demand,
                Interval.Closed,
                Interval.Closed,
                InfraNumber
            ).value!!
        )
    }

    private fun layerBin(
        items: List<ActualItem>,
        typeCode: String = "BIN-A"
    ): Bin<BinLayer> {
        val binType = BinType(
            width = infraScalar(3.0) * Meter,
            height = infraScalar(3.0) * Meter,
            depth = infraScalar(3.0) * Meter,
            capacity = infraScalar(100.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = typeCode
        )
        val placements = items.mapIndexed { index, item ->
            QuantityPlacement3(
                view = item.view(Orientation.Upright),
                position = point3(x = infraScalar(index.toDouble()) * Meter, y = infraScalar(0.0) * Meter, z = infraScalar(0.0) * Meter)
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
            layerGenerator = object : Bpp3dLayerGenerator<InfraNumber> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<InfraNumber>): List<Bpp3dLayerGenerationResult<InfraNumber>> {
                    return emptyList()
                }
            }
        )
        assertNotNull(algorithm)
    }

    @Test
    fun algorithmShouldStopWhenNoAcceptedColumns() = runBlocking {
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<InfraNumber> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<InfraNumber>): List<Bpp3dLayerGenerationResult<InfraNumber>> {
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
            layerGenerator = object : Bpp3dLayerGenerator<InfraNumber> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<InfraNumber>): List<Bpp3dLayerGenerationResult<InfraNumber>> {
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
    fun algorithmShouldCompleteLpSpAddColumnAndFinalMilpFlow() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CG-FLOW"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CG-FLOW",
            weight = InfraNumber.one * Kilogram
        )
        val seedItem = item(
            id = "item-cg-flow-seed",
            material = material
        )
        val generatedItem = item(
            id = "item-cg-flow-generated",
            material = material
        )
        val seedLayer = layerBin(listOf(seedItem)).units.first().unit
        val generatedLayer = layerBin(listOf(generatedItem)).units.first().unit
        val rmpCallStates = ArrayList<Pair<Int, Int>>()
        var finalSolverColumnCount = -1
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<InfraNumber> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<InfraNumber>): List<Bpp3dLayerGenerationResult<InfraNumber>> {
                    return if (request.iteration == 0) {
                        listOf(
                            Bpp3dLayerGenerationResult(
                                layer = generatedLayer,
                                reducedCost = InfraNumber(-1.0),
                                source = "test-add-column"
                            )
                        )
                    } else {
                        emptyList()
                    }
                }
            },
            rmpSolver = ColumnGenerationRmpSolver { state ->
                rmpCallStates.add(Pair(state.iteration, state.columns.size))
                ColumnGenerationLpResult(
                    shadowPrices = emptyMap(),
                    objective = InfraNumber(100.0) - infraScalar(state.iteration.toDouble()),
                    info = mapOf(
                        Pair("solver", "stub-lp"),
                        Pair("iteration", state.iteration.toString())
                    )
                )
            },
            finalMilpSolver = ColumnGenerationFinalSolver { state ->
                finalSolverColumnCount = state.columns.size
                ColumnGenerationFinalResult(
                    columns = state.columns,
                    objective = InfraNumber(77.0),
                    info = mapOf(Pair("solver", "stub-final"))
                )
            },
            initialColumns = { listOf(seedLayer) }
        )

        val result = algorithm.solve(
            items = listOf(seedItem, generatedItem),
            config = ColumnGenerationConfig(
                iterationLimit = 4,
                maxColumnsPerIteration = 16
            )
        )

        assertEquals(1, result.iterationCount)
        assertEquals(2, result.lpSolvedTimes)
        assertTrue(result.finalSolved)
        assertEquals(2, result.columns.size)
        assertEquals(
            listOf(Pair(0, 1), Pair(1, 2)),
            rmpCallStates
        )
        assertEquals(2, finalSolverColumnCount)
        assertEquals(2, result.lpInfos.size)
        assertEquals("stub-final", result.finalInfo["solver"])
    }

    @Test
    fun rmpSolverAndRequestBuilderShouldBePreferredWhenProvided() = runBlocking {
        val legacyRmpCounter = AtomicInteger(0)
        val rmpSolverCounter = AtomicInteger(0)
        val requestBuilderCounter = AtomicInteger(0)
        var observedMaxCandidates = -1
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<InfraNumber> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<InfraNumber>): List<Bpp3dLayerGenerationResult<InfraNumber>> {
                    observedMaxCandidates = request.maxCandidates
                    return emptyList()
                }
            },
            rmpSolver = ColumnGenerationRmpSolver {
                rmpSolverCounter.incrementAndGet()
                ColumnGenerationLpResult(
                    shadowPrices = emptyMap<DemandModeKey, InfraNumber>(),
                    objective = InfraNumber(42.0)
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
        assertEquals(listOf(InfraNumber(42.0)), result.lpObjectives)
    }

    @Test
    fun finalSolverReturnedBinsShouldReachPackingAnalyzer() = runBlocking {
        val material = Material(
            no = MaterialNo("M-1"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-1",
            weight = InfraNumber.half * Kilogram
        )
        val bin = layerBin(listOf(item("item-1", material)))
        val analyzer = ColumnGenerationPackingAnalyzer()
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<InfraNumber> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<InfraNumber>): List<Bpp3dLayerGenerationResult<InfraNumber>> {
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
            weight = InfraNumber.half * Kilogram
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
        val demandValue = InfraNumber.one
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
                    InfraNumber
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
                return Ok(lpResultOf(output, dual))
            }
        }
        val executors = ColumnGenerationStandardExecutors.fromDemandEntries(
            solver = solver,
            itemDemands = listOf(Pair(actualItem, UInt64.one)),
            demandEntries = demandEntries,
            finalBins = listOf(finalBin)
        )

        var capturedRequest: Bpp3dLayerGenerationRequest<InfraNumber>? = null
        var analyzedState: ColumnGenerationState<InfraNumber>? = null
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<InfraNumber> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<InfraNumber>): List<Bpp3dLayerGenerationResult<InfraNumber>> {
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
        assertEquals(listOf(InfraNumber(11.0)), result.lpObjectives)
        assertEquals(1, result.columns.size)
        assertEquals(1, finalState.bins.size)
        assertTrue(request.demandEntries.any { it.mode is Bpp3dDemandMode.ItemAmount })
        assertNotNull(request.scoreByShadowPrice)
        val demandKey = DemandModeKey(
            mode = Bpp3dDemandMode.ItemAmount,
            key = Bpp3dDemandKey.Item(actualItem)
        )
        assertEquals(InfraNumber(7.0), request.shadowPrices[demandKey] ?: InfraNumber.zero)
    }

    @Test
    fun standardExecutorsFactoryShouldSupportQuantityItemDemands() {
        val quantityMaterial = QuantityMaterial(
            no = MaterialNo("M-Q"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-Q",
            weight = FltX.one * Kilogram
        )
        val quantityItem = QuantityItem(
            id = "item-q",
            name = "item-q",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = FltX.one * Meter,
                    height = FltX.one * Meter,
                    depth = FltX.one * Meter,
                    weight = FltX.one * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(quantityMaterial to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-Q"),
            packageAttribute = packageAttribute()
        )
        val itemCache = LinkedHashMap<QuantityItem<FltX>, ActualItem>()
        val materialCache = LinkedHashMap<QuantityMaterial<FltX>, Material>()
        val modelItem = quantityItem.toModel(materialCache, itemCache)
        val demandEntries = listOf(
            fixedDemandEntry(
                mode = Bpp3dDemandMode.ItemAmount,
                key = Bpp3dDemandKey.Item(modelItem),
                demand = InfraNumber.one
            )
        )
        val solver = object : ColumnGenerationSolver {
            override val name = "stub-generic-factory-solver"

            override suspend fun solveMILP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<FeasibleSolverOutput<Flt64>> {
                error("not used in this test")
            }

            override suspend fun solveLP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<ColumnGenerationSolver.LPResult> {
                error("not used in this test")
            }
        }

        val executors = ColumnGenerationStandardExecutors.fromDemandEntries(
            solver = solver,
            itemDemands = listOf(Pair(quantityItem, UInt64.one)),
            demandEntries = demandEntries,
            itemCache = itemCache,
            materialCache = materialCache
        )

        assertNotNull(executors)
    }

    @Test
    fun applicationRequestFactoryShouldSupportQuantityDemands() {
        val quantityMaterial = QuantityMaterial(
            no = MaterialNo("M-RQ"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-RQ",
            weight = FltX(0.5) * Kilogram
        )
        val quantityItem = QuantityItem(
            id = "item-rq",
            name = "item-rq",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = FltX.one * Meter,
                    height = FltX.one * Meter,
                    depth = FltX.one * Meter,
                    weight = FltX.one * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(quantityMaterial to UInt64(2))
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-RQ"),
            packageAttribute = packageAttribute()
        )
        val quantityInitialLayer = QuantityBinLayer(
            iteration = Int64.zero,
            from = ColumnGenerationAlgorithmTest::class,
            width = FltX(3) * Meter,
            height = FltX(3) * Meter,
            depth = FltX(3) * Meter,
            units = listOf(
                QuantityItemPlacement(
                    item = quantityItem,
                    x = FltX.zero * Meter,
                    y = FltX.zero * Meter,
                    z = FltX.zero * Meter,
                    orientation = Orientation.Upright
                )
            )
        )

        val request = ColumnGenerationApplicationRequest.fromQuantityDemands(
            itemDemands = listOf(Pair(quantityItem, UInt64(3))),
            materialAmountDemands = listOf(Pair(quantityMaterial, UInt64(6))),
            materialWeightDemands = listOf(Pair(quantityMaterial, FltX(2.5) * Kilogram)),
            quantityInitialColumns = listOf(quantityInitialLayer)
        )

        assertEquals(1, request.itemDemands.size)
        assertEquals("item-rq", (request.itemDemands.first().first as ActualItem).id)
        assertEquals(UInt64(3), request.itemDemands.first().second)
        assertEquals(1, request.materialAmountDemands.size)
        assertEquals(UInt64(6), request.materialAmountDemands.first().second)
        assertEquals(1, request.materialWeightDemands.size)
        assertEquals(2.5, request.materialWeightDemands.first().second.value.toDouble(), 1e-9)
        assertEquals(1, request.initialColumns.size)
        assertEquals(1, request.initialColumns.first().units.size)
    }

    @Test
    fun applicationServiceShouldSupportQuantityDemandEntryPoint() = runBlocking {
        val quantityMaterial = QuantityMaterial(
            no = MaterialNo("M-SQ"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-SQ",
            weight = FltX.one * Kilogram
        )
        val quantityItem = QuantityItem(
            id = "item-sq",
            name = "item-sq",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = FltX.one * Meter,
                    height = FltX.one * Meter,
                    depth = FltX.one * Meter,
                    weight = FltX.one * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(quantityMaterial to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-SQ"),
            packageAttribute = packageAttribute()
        )
        val quantityInitialLayer = QuantityBinLayer(
            iteration = Int64.zero,
            from = ColumnGenerationAlgorithmTest::class,
            width = FltX(3) * Meter,
            height = FltX(3) * Meter,
            depth = FltX(3) * Meter,
            units = listOf(
                QuantityItemPlacement(
                    item = quantityItem,
                    x = FltX.zero * Meter,
                    y = FltX.zero * Meter,
                    z = FltX.zero * Meter,
                    orientation = Orientation.Upright
                )
            )
        )
        val solver = object : ColumnGenerationSolver {
            override val name: String = "stub-cg-quantity-entry-solver"

            override suspend fun solveMILP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<FeasibleSolverOutput<Flt64>> {
                error("final milp should be disabled in this test")
            }

            override suspend fun solveLP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<ColumnGenerationSolver.LPResult> {
                val output = FeasibleSolverOutput(
                    obj = Flt64(3.0),
                    solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.zero },
                    time = Duration.ZERO,
                    possibleBestObj = Flt64(3.0),
                    gap = Flt64.zero
                )
                return Ok(lpResultOf(output, emptyMap<Constraint<Flt64, Linear>, Flt64>()))
            }
        }
        val service = ColumnGenerationApplicationService(solver)
        val response = service.solveQuantityDemands(
            itemDemands = listOf(Pair(quantityItem, UInt64.one)),
            quantityInitialColumns = listOf(quantityInitialLayer),
            generators = listOf(
                object : Bpp3dLayerGenerator<InfraNumber> {
                    override suspend fun generate(request: Bpp3dLayerGenerationRequest<InfraNumber>): List<Bpp3dLayerGenerationResult<InfraNumber>> {
                        return emptyList()
                    }
                }
            ),
            cgConfig = ColumnGenerationConfig(finalMilpEnabled = false)
        )

        assertEquals(1, response.result.lpSolvedTimes)
        assertEquals(listOf(InfraNumber(3.0)), response.result.lpObjectives)
        assertEquals(1, response.result.columns.size)
        assertEquals(1, response.result.columns.first().units.size)
        assertTrue(!response.result.finalSolved)
    }

    @Test
    fun applicationServiceShouldBridgeExecutorsLayerGenerationAndPacking() = runBlocking {
        val material = Material(
            no = MaterialNo("M-3"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-3",
            weight = InfraNumber.half * Kilogram
        )
        val actualItem = item("item-3", material)
        val seedBin = layerBin(listOf(actualItem))
        val seedLayer = seedBin.units.first().unit
        val finalBin: Bin<BinLayer> = Bin(
            shape = seedBin.shape,
            units = emptyList<QuantityPlacement3<BinLayer>>(),
            batchNo = seedBin.batchNo
        )
        val demandValue = InfraNumber.one
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
                    InfraNumber
                ).value!!
            )
        )
        val solver = object : ColumnGenerationSolver {
            override val name: String = "stub-cg-service-solver"

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
                return Ok(output)
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
                return Ok(lpResultOf(output, dual))
            }
        }
        val service = ColumnGenerationApplicationService(solver)
        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = listOf(Pair(actualItem, UInt64.one)),
                demandEntries = demandEntries,
                initialColumns = listOf(seedLayer),
                finalBins = listOf(finalBin),
                generators = listOf(
                    object : Bpp3dLayerGenerator<InfraNumber> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<InfraNumber>): List<Bpp3dLayerGenerationResult<InfraNumber>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )

        assertTrue(response.result.finalSolved)
        assertEquals(listOf(InfraNumber(11.0)), response.result.lpObjectives)
        assertEquals(1, response.result.columns.size)
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(1, snapshot.bins.size)
        assertEquals("1", snapshot.schema.kpi["bin_count"])
        val itemAmountEntryCount = snapshot.schema.kpi["shadow_price_mode_item_amount_entry_count"]?.toInt()
        assertNotNull(itemAmountEntryCount)
        assertTrue(itemAmountEntryCount >= 1)
        assertEquals(
            itemAmountEntryCount,
            snapshot.demandModeShadowPriceEntryCounts["item_amount"]
        )
        val itemAmountTotal = snapshot.schema.kpi["shadow_price_mode_item_amount_total"]?.toDouble()
        assertNotNull(itemAmountTotal)
        assertTrue(itemAmountTotal > 0.0)
        assertTrue(
            (snapshot.demandModeShadowPriceTotals["item_amount"] ?: InfraNumber.zero).toDouble() > 0.0
        )
    }

    @Test
    fun applicationServiceShouldSupportMaterialWeightOnlyDemandPackingFlow() = runBlocking {
        val material = Material(
            no = MaterialNo("M-4"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-4",
            weight = InfraNumber.one * Kilogram
        )
        val actualItem = item("item-4", material)
        val seedBin = layerBin(listOf(actualItem))
        val seedLayer = seedBin.units.first().unit
        val finalBin: Bin<BinLayer> = Bin(
            shape = seedBin.shape,
            units = emptyList<QuantityPlacement3<BinLayer>>(),
            batchNo = seedBin.batchNo
        )
        val demandValue = InfraNumber(2.0)
        val demandEntries = listOf(
            Bpp3dDemandEntry(
                mode = Bpp3dDemandMode.ItemMaterialWeight,
                key = Bpp3dDemandKey.Material(material.key),
                demand = demandValue,
                demandRange = ValueRange(
                    demandValue,
                    demandValue,
                    Interval.Closed,
                    Interval.Closed,
                    InfraNumber
                ).value!!
            )
        )
        val solver = object : ColumnGenerationSolver {
            override val name: String = "stub-cg-material-weight-solver"

            override suspend fun solveMILP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<FeasibleSolverOutput<Flt64>> {
                val solution = List(metaModel.tokens.tokensInSolver.size) { Flt64(2.0) }
                val output: FeasibleSolverOutput<Flt64> = FeasibleSolverOutput(
                    obj = Flt64(17.0),
                    solution = solution,
                    time = Duration.ZERO,
                    possibleBestObj = Flt64(17.0),
                    gap = Flt64.zero
                )
                return Ok(output)
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
                    fakeConstraint(tagged) to Flt64(5.0)
                )
                val solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.zero }
                val output: FeasibleSolverOutput<Flt64> = FeasibleSolverOutput(
                    obj = Flt64(13.0),
                    solution = solution,
                    time = Duration.ZERO,
                    possibleBestObj = Flt64(13.0),
                    gap = Flt64.zero
                )
                return Ok(lpResultOf(output, dual))
            }
        }
        val service = ColumnGenerationApplicationService(solver)
        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = listOf(Pair(actualItem, UInt64(2))),
                demandEntries = demandEntries,
                initialColumns = listOf(seedLayer),
                finalBins = listOf(finalBin),
                generators = listOf(
                    object : Bpp3dLayerGenerator<InfraNumber> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<InfraNumber>): List<Bpp3dLayerGenerationResult<InfraNumber>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )

        assertTrue(response.result.finalSolved)
        assertEquals(listOf(InfraNumber(13.0)), response.result.lpObjectives)
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(1, snapshot.bins.size)
        assertEquals(2, snapshot.bins.single().units.size)
        assertEquals(2, snapshot.packingResult.aggregation.bins.single().items.size)
        assertEquals(1, snapshot.packingResult.materialSummary.size)
        assertEquals(UInt64(2), snapshot.packingResult.materialSummary.single().amount)
        assertEquals("1", snapshot.schema.kpi["bin_count"])
        assertEquals("1", snapshot.schema.kpi["material_count"])
    }

    @Test
    fun applicationServiceShouldKeepPackingConsistentForMultiMaterialMultiBinScenario() = runBlocking {
        val materialA = Material(
            no = MaterialNo("M-5A"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-5A",
            weight = InfraNumber.one * Kilogram
        )
        val materialB = Material(
            no = MaterialNo("M-5B"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-5B",
            weight = infraScalar(2.0) * Kilogram
        )
        val itemA = item("item-5a", materialA)
        val itemB = item("item-5b", materialB)
        val seedBinA = layerBin(listOf(itemA))
        val seedBinB = layerBin(listOf(itemB))
        val seedLayerA = seedBinA.units.first().unit
        val seedLayerB = seedBinB.units.first().unit
        val finalBins = listOf(
            Bin(
                shape = seedBinA.shape,
                units = emptyList<QuantityPlacement3<BinLayer>>(),
                batchNo = seedBinA.batchNo
            ),
            Bin(
                shape = seedBinB.shape,
                units = emptyList<QuantityPlacement3<BinLayer>>(),
                batchNo = seedBinB.batchNo
            )
        )
        val demandEntries = listOf(
            fixedDemandEntry(
                mode = Bpp3dDemandMode.ItemAmount,
                key = Bpp3dDemandKey.Item(itemA),
                demand = InfraNumber(2.0)
            ),
            fixedDemandEntry(
                mode = Bpp3dDemandMode.ItemAmount,
                key = Bpp3dDemandKey.Item(itemB),
                demand = InfraNumber.one
            ),
            fixedDemandEntry(
                mode = Bpp3dDemandMode.ItemMaterialWeight,
                key = Bpp3dDemandKey.Material(materialA.key),
                demand = InfraNumber(2.0)
            ),
            fixedDemandEntry(
                mode = Bpp3dDemandMode.ItemMaterialWeight,
                key = Bpp3dDemandKey.Material(materialB.key),
                demand = InfraNumber(4.0)
            )
        )
        val solver = object : ColumnGenerationSolver {
            override val name: String = "stub-cg-multi-bin-solver"

            override suspend fun solveMILP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<FeasibleSolverOutput<Flt64>> {
                val solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.one }
                val output: FeasibleSolverOutput<Flt64> = FeasibleSolverOutput(
                    obj = Flt64(23.0),
                    solution = solution,
                    time = Duration.ZERO,
                    possibleBestObj = Flt64(23.0),
                    gap = Flt64.zero
                )
                return Ok(output)
            }

            override suspend fun solveLP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<ColumnGenerationSolver.LPResult> {
                val taggedConstraints = metaModel.constraints
                    .filter { constraint -> constraint.args is DemandShadowPriceKey }
                val dual = linkedMapOf<Constraint<Flt64, Linear>, Flt64>()
                taggedConstraints.forEachIndexed { index, constraint ->
                    dual[fakeConstraint(constraint)] = Flt64((index + 1).toDouble())
                }
                val solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.zero }
                val output: FeasibleSolverOutput<Flt64> = FeasibleSolverOutput(
                    obj = Flt64(19.0),
                    solution = solution,
                    time = Duration.ZERO,
                    possibleBestObj = Flt64(19.0),
                    gap = Flt64.zero
                )
                return Ok(lpResultOf(output, dual))
            }
        }

        val service = ColumnGenerationApplicationService(solver)
        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = listOf(
                    Pair(itemA, UInt64(2)),
                    Pair(itemB, UInt64.one)
                ),
                demandEntries = demandEntries,
                initialColumns = listOf(seedLayerA, seedLayerB),
                finalBins = finalBins,
                generators = listOf(
                    object : Bpp3dLayerGenerator<InfraNumber> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<InfraNumber>): List<Bpp3dLayerGenerationResult<InfraNumber>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )

        assertTrue(response.result.finalSolved)
        assertEquals(listOf(InfraNumber(19.0)), response.result.lpObjectives)
        assertEquals(2, response.result.columns.size)
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(2, snapshot.bins.size)
        assertEquals(2, snapshot.bins.sumOf { bin -> bin.units.size })
        assertEquals(2, snapshot.packingResult.aggregation.bins.size)
        assertEquals(2, snapshot.packingResult.aggregation.bins.sumOf { bin -> bin.items.size })
        assertEquals(2, snapshot.packingResult.materialSummary.size)
        val materialSummary = snapshot.packingResult.materialSummary.associate { entry ->
            entry.material to entry.amount
        }
        assertEquals(UInt64.one, materialSummary[materialA.key])
        assertEquals(UInt64.one, materialSummary[materialB.key])
        assertEquals("2", snapshot.schema.kpi["bin_count"])
        assertEquals("2", snapshot.schema.kpi["material_count"])
    }

    @Test
    fun applicationServiceShouldKeepPackingConsistentForLargeMaterialBatchScenario() = runBlocking {
        val materialCount = 4
        val layerCount = 24
        val materials = (0 until materialCount).map { index ->
            Material(
                no = MaterialNo("M-6-$index"),
                type = MaterialType.RawMaterial,
                cargo = CargoAttr,
                name = "M-6-$index",
                weight = InfraNumber.one * Kilogram
            )
        }
        val items = (0 until layerCount).map { index ->
            item(
                id = "item-6-$index",
                material = materials[index % materialCount]
            )
        }
        val seedBins = items.mapIndexed { index, actualItem ->
            layerBin(
                items = listOf(actualItem),
                typeCode = "BIN-6-$index"
            )
        }
        val seedLayers = seedBins.map { seedBin ->
            seedBin.units.first().unit
        }
        val finalBins = seedBins.map { seedBin ->
            Bin(
                shape = seedBin.shape,
                units = emptyList<QuantityPlacement3<BinLayer>>(),
                batchNo = seedBin.batchNo
            )
        }
        val demandEntries = buildList {
            addAll(items.map { actualItem ->
                fixedDemandEntry(
                    mode = Bpp3dDemandMode.ItemAmount,
                    key = Bpp3dDemandKey.Item(actualItem),
                    demand = InfraNumber.one
                )
            })
            addAll(materials.map { material ->
                fixedDemandEntry(
                    mode = Bpp3dDemandMode.ItemMaterialWeight,
                    key = Bpp3dDemandKey.Material(material.key),
                    demand = infraScalar(layerCount.toDouble() / materialCount.toDouble())
                )
            })
        }
        val solver = object : ColumnGenerationSolver {
            override val name: String = "stub-cg-large-batch-solver"

            override suspend fun solveMILP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<FeasibleSolverOutput<Flt64>> {
                val solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.one }
                val output: FeasibleSolverOutput<Flt64> = FeasibleSolverOutput(
                    obj = Flt64(61.0),
                    solution = solution,
                    time = Duration.ZERO,
                    possibleBestObj = Flt64(61.0),
                    gap = Flt64.zero
                )
                return Ok(output)
            }

            override suspend fun solveLP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<ColumnGenerationSolver.LPResult> {
                val taggedConstraints = metaModel.constraints
                    .filter { constraint -> constraint.args is DemandShadowPriceKey }
                val dual = linkedMapOf<Constraint<Flt64, Linear>, Flt64>()
                taggedConstraints.forEachIndexed { index, constraint ->
                    dual[fakeConstraint(constraint)] = Flt64((index + 1).toDouble())
                }
                val solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.zero }
                val output: FeasibleSolverOutput<Flt64> = FeasibleSolverOutput(
                    obj = Flt64(37.0),
                    solution = solution,
                    time = Duration.ZERO,
                    possibleBestObj = Flt64(37.0),
                    gap = Flt64.zero
                )
                return Ok(lpResultOf(output, dual))
            }
        }

        val service = ColumnGenerationApplicationService(solver)
        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = items.map { actualItem -> Pair(actualItem, UInt64.one) },
                demandEntries = demandEntries,
                initialColumns = seedLayers,
                finalBins = finalBins,
                generators = listOf(
                    object : Bpp3dLayerGenerator<InfraNumber> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<InfraNumber>): List<Bpp3dLayerGenerationResult<InfraNumber>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )

        assertTrue(response.result.finalSolved)
        assertEquals(listOf(InfraNumber(37.0)), response.result.lpObjectives)
        assertEquals(layerCount, response.result.columns.size)
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(layerCount, snapshot.bins.size)
        assertEquals(layerCount, snapshot.bins.sumOf { bin -> bin.units.size })
        assertEquals(layerCount, snapshot.packingResult.aggregation.bins.size)
        assertEquals(layerCount, snapshot.packingResult.aggregation.bins.sumOf { bin -> bin.items.size })
        assertEquals(materialCount, snapshot.packingResult.materialSummary.size)
        val materialSummary = snapshot.packingResult.materialSummary.associate { entry ->
            entry.material to entry.amount
        }
        val expectedMaterialAmount = UInt64(layerCount / materialCount)
        for (material in materials) {
            assertEquals(expectedMaterialAmount, materialSummary[material.key])
        }
        assertEquals(layerCount.toString(), snapshot.schema.kpi["bin_count"])
        assertEquals(materialCount.toString(), snapshot.schema.kpi["material_count"])
    }

    @Suppress("UNCHECKED_CAST")
    private fun lpResultOf(
        result: FeasibleSolverOutput<Flt64>,
        dualSolution: Map<*, *>
    ): ColumnGenerationSolver.LPResult {
        val constructor = ColumnGenerationSolver.LPResult::class.java.declaredConstructors
            .first { it.parameterCount == 2 }
        constructor.isAccessible = true
        return constructor.newInstance(result, dualSolution) as ColumnGenerationSolver.LPResult
    }

    private fun fakeConstraint(origin: fuookami.ospf.kotlin.core.model.mechanism.MathConstraint): Constraint<Flt64, Linear> {
        return object : Constraint<Flt64, Linear> {
            override val lhs: List<Cell<Flt64>> = emptyList()
            override val sign: ConstraintRelation = ConstraintRelation.Equal
            override val rhs: Flt64 = Flt64.zero
            override val lazy: Boolean = false
            override val name: String = "fake-dual"
            override val origin: fuookami.ospf.kotlin.core.model.mechanism.MathConstraint = origin
            override val from: Pair<IntermediateSymbol<*>, Boolean>? = null

            override fun isTrue(): Boolean? = true
            override fun isTrue(results: List<Flt64>): Boolean? = true
        }
    }
}







