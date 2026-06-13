package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.intermediate.Cell
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayerPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LayerBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Package
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.asContainer3Shape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.continuousCylinderRadiusSolverPrototype
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.layerBinOf
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.QuantityBinLayer as QuantityBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.QuantityItem as QuantityItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.QuantityItemPlacement as QuantityItemPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.QuantityMaterial as QuantityMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.QuantityPackage as QuantityPackage
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.QuantityPackageShape as QuantityPackageShape
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
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.fltX
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.geometry.Axis3
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ColumnGenerationAlgorithmTest {
    private object CargoAttr : fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute

    private fun packageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(id: String, material: Material<FltX>): ActualItem {
        val pack = Package.innerPackage(
            shape = PackageShape(
                width = FltX.one * Meter,
                height = FltX.one * Meter,
                depth = FltX.one * Meter,
                weight = FltX.one * Kilogram,
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

    private fun continuousRadiusItem(id: String, material: Material<FltX>): ActualItem {
        val pack = Package.innerPackage(
            shape = PackageShape(
                width = FltX.one * Meter,
                height = fltX(1.2) * Meter,
                depth = FltX.one * Meter,
                weight = FltX.one * Kilogram,
                packageType = PackageType.CartonContainer,
                shapeSpec = PackageShapeSpec.VerticalCylinder(
                    radius = fltX(0.5) * Meter,
                    axis = Axis3.Y,
                    radiusMin = fltX(0.4) * Meter,
                    radiusMax = fltX(0.6) * Meter,
                    radiusWeightFunctionKey = "cg-radius-key"
                )
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
        demand: FltX
    ): Bpp3dDemandEntry<FltX> {
        return Bpp3dDemandEntry(
            mode = mode,
            key = key,
            demand = demand,
            demandRange = ValueRange(
                demand,
                demand,
                Interval.Closed,
                Interval.Closed,
                FltX
            ).value!!
        )
    }

    private fun layerBin(
        items: List<ActualItem>,
        typeCode: String = "BIN-A"
    ): LayerBin {
        val binType = BinType(
            width = fltX(3.0) * Meter,
            height = fltX(3.0) * Meter,
            depth = fltX(3.0) * Meter,
            capacity = fltX(100.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = typeCode
        )
        val placements = items.mapIndexed { index, item ->
            item.toItemPlacement(
                x = fltX(index.toDouble()) * Meter
            )
        }
        val layer = BinLayer(
            iteration = Int64.zero,
            from = ColumnGenerationAlgorithmTest::class,
            bin = binType,
            shape = Container3Shape(binType.asContainer3Shape()),
            units = placements
        )
        return layerBinOf(
            shape = binType,
            units = listOf(
                layer.toLayerPlacement()
            )
        )
    }

    @Test
    fun columnGenerationAlgorithmShouldLiveInApplicationService() {
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<FltX> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                    return emptyList()
                }
            }
        )
        assertNotNull(algorithm)
    }

    @Test
    fun algorithmShouldStopWhenNoAcceptedColumns() = runBlocking {
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<FltX> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
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
    fun algorithmShouldSupportQuantityItemEntryPoint() = runBlocking {
        val quantityMaterial = QuantityMaterial(
            no = MaterialNo("M-ALG-G"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-ALG-G",
            weight = FltX.one * Kilogram
        )
        val quantityItem = QuantityItem(
            id = "item-alg-g",
            name = "item-alg-g",
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
            batchNo = BatchNo("B-ALG-G"),
            packageAttribute = packageAttribute()
        )
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<FltX> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                    return emptyList()
                }
            }
        )

        val result = algorithm.solveQuantity(
            items = listOf(quantityItem),
            config = ColumnGenerationConfig(finalMilpEnabled = false)
        )

        assertEquals(1, result.lpSolvedTimes)
        assertEquals(0, result.columns.size)
        assertTrue(!result.finalSolved)
    }

    @Test
    fun algorithmShouldInvokeFinalSolveAnalyzerAndHeartbeatCallbacks() = runBlocking {
        val finalSolveCounter = AtomicInteger(0)
        val analyzeCounter = AtomicInteger(0)
        val heartbeatCounter = AtomicInteger(0)
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<FltX> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
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
            weight = FltX.one * Kilogram
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
            layerGenerator = object : Bpp3dLayerGenerator<FltX> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                    return if (request.iteration == 0) {
                        listOf(
                            Bpp3dLayerGenerationResult(
                                layer = generatedLayer,
                                reducedCost = FltX(-1.0),
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
                    objective = FltX(100.0) - fltX(state.iteration.toDouble()),
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
                    objective = FltX(77.0),
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
        val fallbackRmpCounter = AtomicInteger(0)
        val rmpSolverCounter = AtomicInteger(0)
        val requestBuilderCounter = AtomicInteger(0)
        var observedMaxCandidates = -1
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<FltX> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                    observedMaxCandidates = request.maxCandidates
                    return emptyList()
                }
            },
            rmpSolver = ColumnGenerationRmpSolver {
                rmpSolverCounter.incrementAndGet()
                ColumnGenerationLpResult(
                    shadowPrices = emptyMap<DemandModeKey, FltX>(),
                    objective = FltX(42.0)
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
                fallbackRmpCounter.incrementAndGet()
                ColumnGenerationLpResult(emptyMap())
            }
        )

        val result = algorithm.solve(
            items = emptyList(),
            config = ColumnGenerationConfig(maxColumnsPerIteration = 99)
        )
        assertEquals(1, rmpSolverCounter.get())
        assertEquals(0, fallbackRmpCounter.get())
        assertEquals(1, requestBuilderCounter.get())
        assertEquals(1, observedMaxCandidates)
        assertEquals(listOf(FltX(42.0)), result.lpObjectives)
    }

    @Test
    fun algorithmShouldPropagateContinuousRadiusSolverPrototypesToStates() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CG-RADIUS"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CG-RADIUS",
            weight = FltX.one * Kilogram
        )
        val item = continuousRadiusItem(
            id = "item-cg-radius",
            material = material
        )
        val rmpVariables = ArrayList<String>()
        val requestBuilderVariables = ArrayList<String>()
        var finalVariables = emptyList<String>()
        var analyzerVariables = emptyList<String>()
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<FltX> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                    return emptyList()
                }
            },
            rmpSolver = ColumnGenerationRmpSolver { state ->
                rmpVariables.addAll(state.continuousRadiusSolverPrototypes.map { it.variableName })
                ColumnGenerationLpResult(
                    shadowPrices = emptyMap(),
                    objective = FltX(1.0)
                )
            },
            finalMilpSolver = ColumnGenerationFinalSolver { state ->
                finalVariables = state.continuousRadiusSolverPrototypes.map { it.variableName }
                ColumnGenerationFinalResult(columns = state.columns)
            },
            solutionAnalyzer = ColumnGenerationSolutionAnalyzer { state ->
                analyzerVariables = state.continuousRadiusSolverPrototypes.map { it.variableName }
            },
            layerRequestBuilder = ColumnGenerationLayerRequestBuilder { state, items, cgConfig ->
                requestBuilderVariables.addAll(state.continuousRadiusSolverPrototypes.map { it.variableName })
                Bpp3dLayerGenerationRequest(
                    iteration = state.iteration,
                    items = items,
                    existingLayers = state.columns,
                    shadowPrices = state.shadowPrices,
                    maxCandidates = cgConfig.maxColumnsPerIteration
                )
            }
        )

        val result = algorithm.solve(
            items = listOf(item),
            config = ColumnGenerationConfig(iterationLimit = 1)
        )
        val expectedVariable = continuousRadiusSolverPrototypesFromItems(listOf(item)).single().variableName

        assertEquals(listOf(expectedVariable), rmpVariables)
        assertEquals(listOf(expectedVariable), requestBuilderVariables)
        assertEquals(listOf(expectedVariable), finalVariables)
        assertEquals(listOf(expectedVariable), analyzerVariables)
        assertEquals(listOf(FltX(1.0)), result.lpObjectives)
    }

    @Test
    fun finalSolverReturnedBinsShouldReachPackingAnalyzer() = runBlocking {
        val material = Material(
            no = MaterialNo("M-1"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-1",
            weight = FltX.half * Kilogram
        )
        val bin = layerBin(listOf(item("item-1", material)))
        val analyzer = ColumnGenerationPackingAnalyzer()
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<FltX> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
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
    fun packingAnalyzerShouldExposeContinuousRadiusSolverPrototypeAndRegistrationPlanKpi() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CG-RADIUS-KPI"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CG-RADIUS-KPI",
            weight = FltX.one * Kilogram
        )
        val item = continuousRadiusItem(
            id = "item-cg-radius-kpi",
            material = material
        )
        val bin = layerBin(listOf(item))
        val prototype = continuousRadiusSolverPrototypesFromItems(listOf(item)).single()
        val analyzer = ColumnGenerationPackingAnalyzer()

        analyzer.analyze(
            state = ColumnGenerationState(
                iteration = 0,
                columns = listOf(bin.units.first().unit),
                bins = listOf(bin),
                continuousRadiusSolverPrototypes = listOf(prototype)
            )
        )
        val snapshot = assertNotNull(analyzer.latest)

        assertEquals("1", snapshot.schema.kpi["continuous_radius_solver_prototype_count"])
        assertEquals(
            prototype.variableName,
            snapshot.schema.kpi["continuous_radius_solver_prototype_variables"]
        )
        assertEquals("1", snapshot.schema.kpi["continuous_radius_solver_registration_plan_count"])
        assertEquals(
            prototype.variableName,
            snapshot.schema.kpi["continuous_radius_solver_registration_plan_variables"]
        )
        assertTrue(
            snapshot.schema.kpi["continuous_radius_solver_registration_plan_selected_radii"]
                ?.contains(prototype.variableName) == true
        )
        assertEquals(
            prototype.variableName,
            snapshot.schema.kpi["continuous_radius_solver_registration_plan_production_ready_variables"]
        )
        // Production-ready variables are no longer blocked (solver-registerable)
        assertEquals(
            "",
            snapshot.schema.kpi["continuous_radius_solver_model_registration_blocked_variables"]
        )
        // Blocked reason is empty when no variables are blocked
        assertEquals(
            "",
            snapshot.schema.kpi["continuous_radius_solver_model_registration_blocked_reason"]
        )
    }

    @Test
    fun standardExecutorsShouldExposeContinuousRadiusSolverRegistrationPlan() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CG-RADIUS-VAR"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CG-RADIUS-VAR",
            weight = FltX.one * Kilogram
        )
        val item = continuousRadiusItem(
            id = "item-cg-radius-var",
            material = material
        )
        val seedBin = layerBin(listOf(item))
        val rawSeedLayer = seedBin.units.first().unit
        val seedLayer = BinLayer(
            iteration = rawSeedLayer.iteration,
            from = rawSeedLayer.from,
            bin = seedBin.type,
            shape = rawSeedLayer.shape,
            units = rawSeedLayer.units
        )
        val finalBin: LayerBin = layerBinOf(
            shape = seedBin.type,
            units = emptyList<BinLayerPlacement>(),
            batchNo = seedBin.batchNo
        )
        val prototype = continuousRadiusSolverPrototypesFromItems(listOf(item)).single()
        val demandEntries: List<Bpp3dDemandEntry<FltX>> = listOf(
            fixedDemandEntry(
                mode = Bpp3dDemandMode.ItemAmount,
                key = Bpp3dDemandKey.Item(item),
                demand = FltX.one
            )
        )
        val lpRadiusTokens = ArrayList<String>()
        val milpRadiusTokens = ArrayList<String>()
        val solver = object : ColumnGenerationSolver {
            override val name: String = "stub-cg-radius-variable-solver"

            override suspend fun solveMILP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<FeasibleSolverOutput<Flt64>> {
                for (token in metaModel.tokens.tokensInSolver) {
                    if (token.name == prototype.variableName) {
                        milpRadiusTokens.add(token.name)
                    }
                }
                return Ok(
                    FeasibleSolverOutput(
                        obj = Flt64(3.0),
                        solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.one },
                        time = Duration.ZERO,
                        possibleBestObj = Flt64(3.0),
                        gap = Flt64.zero
                    )
                )
            }

            override suspend fun solveLP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<ColumnGenerationSolver.LPResult> {
                for (token in metaModel.tokens.tokensInSolver) {
                    if (token.name == prototype.variableName) {
                        lpRadiusTokens.add(token.name)
                    }
                }
                val tagged = metaModel.constraints.first { it.args is DemandShadowPriceKey }
                return Ok(
                    lpResultOf(
                        result = FeasibleSolverOutput(
                            obj = Flt64(2.0),
                            solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.zero },
                            time = Duration.ZERO,
                            possibleBestObj = Flt64(2.0),
                            gap = Flt64.zero
                        ),
                        dualSolution = linkedMapOf(fakeConstraint(tagged) to Flt64.one)
                    )
                )
            }
        }
        val executors = ColumnGenerationStandardExecutors.fromDemandEntries(
            solver = solver,
            itemDemands = listOf(Pair(item, UInt64.one)),
            demandEntries = demandEntries,
            finalBins = listOf(finalBin)
        )
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<FltX> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                    return emptyList()
                }
            },
            rmpSolver = executors.rmpSolver(),
            finalMilpSolver = executors.finalSolver(),
            layerRequestBuilder = executors.requestBuilder(),
            initialColumns = { listOf(seedLayer) }
        )

        val result = algorithm.solve(items = listOf(item))
        val selectedRadiusValue = assertNotNull(prototype.initialRadius).value.toDouble()

        // Production-ready variables are now registered into the solver model
        assertTrue(lpRadiusTokens.contains(prototype.variableName))
        assertTrue(milpRadiusTokens.contains(prototype.variableName))
        assertEquals("1", result.lpInfos.single()["continuous_radius_solver_registration_plan_count"])
        assertEquals(
            prototype.variableName,
            result.lpInfos.single()["continuous_radius_solver_registration_plan_variables"]
        )
        assertTrue(
            result.lpInfos.single()["continuous_radius_solver_registration_plan_bounds"]
                ?.contains("$selectedRadiusValue m..$selectedRadiusValue m") == true
        )
        assertEquals(
            prototype.variableName,
            result.lpInfos.single()["continuous_radius_solver_registration_plan_production_ready_variables"]
        )
        // Production-ready variables are no longer blocked
        assertEquals(
            "",
            result.lpInfos.single()["continuous_radius_solver_model_registration_blocked_variables"]
        )
        // Blocked reason is empty when no variables are blocked
        assertEquals(
            "",
            result.lpInfos.single()["continuous_radius_solver_model_registration_blocked_reason"]
        )
        // Solver-selected radius is exposed in LP info
        assertNotNull(result.lpInfos.single()["continuous_radius_solver_selected_${prototype.variableName}"])
        assertEquals("1", result.finalInfo["continuous_radius_solver_registration_plan_count"])
        assertEquals(
            prototype.variableName,
            result.finalInfo["continuous_radius_solver_registration_plan_production_ready_variables"]
        )
        // Solver-selected radius is exposed in final info
        assertNotNull(result.finalInfo["continuous_radius_solver_selected_${prototype.variableName}"])
    }

    @Test
    fun standardExecutorsShouldRegisterIntervalContinuousRadiusViaPWLPath() = runBlocking {
        // Interval-only continuous radius (with radiusWeightFunctionKey) is now handled
        // via the PWL approximation path, not blocked. The solver will select the optimal
        // radius through piecewise-linear approximation of r².
        // 仅区间连续半径（有 radiusWeightFunctionKey）现在通过 PWL 近似路径处理，而非被 blocked。
        // solver 将通过 r² 的分段线性近似选择最优半径。
        val material = Material(
            no = MaterialNo("M-CG-RADIUS-INTERVAL"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CG-RADIUS-INTERVAL",
            weight = FltX.one * Kilogram
        )
        val item = item("item-cg-radius-interval-demand", material)
        val seedBin = layerBin(listOf(item))
        val rawSeedLayer = seedBin.units.first().unit
        val seedLayer = BinLayer(
            iteration = rawSeedLayer.iteration,
            from = rawSeedLayer.from,
            bin = seedBin.type,
            shape = rawSeedLayer.shape,
            units = rawSeedLayer.units
        )
        val finalBin: LayerBin = layerBinOf(
            shape = seedBin.type,
            units = emptyList<BinLayerPlacement>(),
            batchNo = seedBin.batchNo
        )
        val prototype = assertNotNull(
            continuousCylinderRadiusSolverPrototype(
                source = "ColumnGenerationState.item.interval",
                radiusWeightFunctionKey = "prefer-large-radius",
                axis = Axis3.Y,
                radiusMin = fltX(0.4) * Meter,
                radiusMax = fltX(0.6) * Meter
            )
        )
        val demandEntries: List<Bpp3dDemandEntry<FltX>> = listOf(
            fixedDemandEntry(
                mode = Bpp3dDemandMode.ItemAmount,
                key = Bpp3dDemandKey.Item(item),
                demand = FltX.one
            )
        )
        val lpRadiusTokens = ArrayList<String>()
        val milpRadiusTokens = ArrayList<String>()
        val solver = object : ColumnGenerationSolver {
            override val name: String = "stub-cg-radius-interval-solver"

            override suspend fun solveMILP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<FeasibleSolverOutput<Flt64>> {
                for (token in metaModel.tokens.tokensInSolver) {
                    if (token.name == prototype.variableName) {
                        milpRadiusTokens.add(token.name)
                    }
                }
                return Ok(
                    FeasibleSolverOutput(
                        obj = Flt64(5.0),
                        solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.zero },
                        time = Duration.ZERO,
                        possibleBestObj = Flt64(5.0),
                        gap = Flt64.zero
                    )
                )
            }

            override suspend fun solveLP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<ColumnGenerationSolver.LPResult> {
                for (token in metaModel.tokens.tokensInSolver) {
                    if (token.name == prototype.variableName) {
                        lpRadiusTokens.add(token.name)
                    }
                }
                val tagged = metaModel.constraints.first { it.args is DemandShadowPriceKey }
                return Ok(
                    lpResultOf(
                        result = FeasibleSolverOutput(
                            obj = Flt64(4.0),
                            solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.zero },
                            time = Duration.ZERO,
                            possibleBestObj = Flt64(4.0),
                            gap = Flt64.zero
                        ),
                        dualSolution = linkedMapOf(fakeConstraint(tagged) to Flt64.one)
                    )
                )
            }
        }
        val executors = ColumnGenerationStandardExecutors.fromDemandEntries(
            solver = solver,
            itemDemands = listOf(Pair(item, UInt64.one)),
            demandEntries = demandEntries,
            finalBins = listOf(finalBin)
        )
        val state = ColumnGenerationState<FltX>(
            iteration = 0,
            columns = listOf(seedLayer),
            continuousRadiusSolverPrototypes = listOf(prototype)
        )

        val lpResult = executors.rmpSolver().solve(state)
        val finalResult = executors.finalSolver().solve(state)

        assertEquals("1", lpResult.info["continuous_radius_solver_registration_plan_count"])
        assertEquals(
            prototype.variableName,
            lpResult.info["continuous_radius_solver_registration_plan_variables"]
        )
        assertTrue(
            lpResult.info["continuous_radius_solver_registration_plan_bounds"]
                ?.contains("0.4 m..0.6 m") == true
        )
        assertTrue(
            lpResult.info["continuous_radius_solver_registration_plan_gap_variables"]
                ?.contains("MissingSelectedRadius") == true
        )
        assertTrue(
            lpResult.info["continuous_radius_solver_registration_plan_gap_variables"]
                ?.contains("SolverNativeRadiusIntervalUnsupported") == true
        )
        assertEquals("", lpResult.info["continuous_radius_solver_registration_plan_production_ready_variables"])
        // Interval-only with key is now PWL-registerable, not blocked
        // 有 key 的仅区间半径现在可通过 PWL 注册，不再被 blocked
        assertEquals("", lpResult.info["continuous_radius_solver_model_registration_blocked_variables"])
        assertEquals("", lpResult.info["continuous_radius_solver_model_registration_blocked_reason"])
        // PWL variables are registered (contains segments and maxRelErr diagnostics)
        // PWL 变量已注册（包含段数和最大相对误差诊断）
        assertNotNull(lpResult.info["continuous_radius_solver_pwl_registered_variables"])
        assertTrue(
            (lpResult.info["continuous_radius_solver_pwl_registered_variables"] ?: "").isNotEmpty()
        )
        assertTrue(
            (lpResult.info["continuous_radius_solver_pwl_registered_variables"] ?: "").contains("segments=")
        )
        assertEquals("1", finalResult.info["continuous_radius_solver_registration_plan_count"])
        assertTrue(
            finalResult.info["continuous_radius_solver_registration_plan_gap_variables"]
                ?.contains("MissingSelectedRadius") == true
        )
    }

    @Test
    fun standardExecutorsShouldBlockIntervalContinuousRadiusWithoutWeightFunctionKey() = runBlocking {
        // Interval-only continuous radius without radiusWeightFunctionKey should remain blocked
        // because PWL path requires a key for production writeback.
        // 无 radiusWeightFunctionKey 的仅区间连续半径应保持 blocked，
        // 因为 PWL 路径需要 key 才能进行生产回写。
        val material = Material(
            no = MaterialNo("M-CG-RADIUS-INTERVAL-NO-KEY"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CG-RADIUS-INTERVAL-NO-KEY",
            weight = FltX.one * Kilogram
        )
        val item = item("item-cg-radius-interval-no-key-demand", material)
        val seedBin = layerBin(listOf(item))
        val rawSeedLayer = seedBin.units.first().unit
        val seedLayer = BinLayer(
            iteration = rawSeedLayer.iteration,
            from = rawSeedLayer.from,
            bin = seedBin.type,
            shape = rawSeedLayer.shape,
            units = rawSeedLayer.units
        )
        val finalBin: LayerBin = layerBinOf(
            shape = seedBin.type,
            units = emptyList<BinLayerPlacement>(),
            batchNo = seedBin.batchNo
        )
        // No radiusWeightFunctionKey → isPWLRegisterable is false → blocked
        // 无 radiusWeightFunctionKey → isPWLRegisterable 为 false → blocked
        val prototype = assertNotNull(
            continuousCylinderRadiusSolverPrototype(
                source = "ColumnGenerationState.item.interval-no-key",
                radiusWeightFunctionKey = null,
                axis = Axis3.Y,
                radiusMin = fltX(0.4) * Meter,
                radiusMax = fltX(0.6) * Meter
            )
        )
        val demandEntries: List<Bpp3dDemandEntry<FltX>> = listOf(
            fixedDemandEntry(
                mode = Bpp3dDemandMode.ItemAmount,
                key = Bpp3dDemandKey.Item(item),
                demand = FltX.one
            )
        )
        val lpRadiusTokens = ArrayList<String>()
        val milpRadiusTokens = ArrayList<String>()
        val solver = object : ColumnGenerationSolver {
            override val name: String = "stub-cg-radius-interval-no-key-solver"

            override suspend fun solveMILP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<FeasibleSolverOutput<Flt64>> {
                for (token in metaModel.tokens.tokensInSolver) {
                    if (token.name == prototype.variableName) {
                        milpRadiusTokens.add(token.name)
                    }
                }
                return Ok(
                    FeasibleSolverOutput(
                        obj = Flt64(5.0),
                        solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.zero },
                        time = Duration.ZERO,
                        possibleBestObj = Flt64(5.0),
                        gap = Flt64.zero
                    )
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
                for (token in metaModel.tokens.tokensInSolver) {
                    if (token.name == prototype.variableName) {
                        lpRadiusTokens.add(token.name)
                    }
                }
                return Ok(
                    lpResultOf(
                        result = FeasibleSolverOutput(
                            obj = Flt64(4.0),
                            solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.zero },
                            time = Duration.ZERO,
                            possibleBestObj = Flt64(4.0),
                            gap = Flt64.zero
                        ),
                        dualSolution = linkedMapOf(fakeConstraint(tagged) to Flt64.one)
                    )
                )
            }
        }
        val executors = ColumnGenerationStandardExecutors.fromDemandEntries(
            solver = solver,
            itemDemands = listOf(Pair(item, UInt64.one)),
            demandEntries = demandEntries,
            finalBins = listOf(finalBin)
        )
        val state = ColumnGenerationState<FltX>(
            iteration = 0,
            columns = listOf(seedLayer),
            continuousRadiusSolverPrototypes = listOf(prototype)
        )

        val lpResult = executors.rmpSolver().solve(state)
        val finalResult = executors.finalSolver().solve(state)

        // Without key, PWL is not registerable → radius variable not registered
        // 无 key 时 PWL 不可注册 → 半径变量不注册
        assertTrue(lpRadiusTokens.isEmpty())
        assertTrue(milpRadiusTokens.isEmpty())
        assertEquals("1", lpResult.info["continuous_radius_solver_registration_plan_count"])
        // Without key, the variable is blocked
        // 无 key 时变量被 blocked
        assertEquals(
            prototype.variableName,
            lpResult.info["continuous_radius_solver_model_registration_blocked_variables"]
        )
        assertTrue(
            lpResult.info["continuous_radius_solver_model_registration_blocked_reason"]
                ?.contains("core token-bound support") == true
        )
    }

    @Test
    fun standardExecutorsShouldBridgeSolverToRmpAndFinal() = runBlocking {
        val material = Material(
            no = MaterialNo("M-2"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-2",
            weight = FltX.half * Kilogram
        )
        val actualItem = item("item-2", material)
        val seedBin = layerBin(listOf(actualItem))
        val rawSeedLayer = seedBin.units.first().unit
        val seedLayer = BinLayer(
            iteration = rawSeedLayer.iteration,
            from = rawSeedLayer.from,
            bin = seedBin.type,
            shape = rawSeedLayer.shape,
            units = rawSeedLayer.units
        )
        val finalBin: LayerBin = layerBinOf(
            shape = seedBin.type,
            units = emptyList<BinLayerPlacement>(),
            batchNo = seedBin.batchNo
        )
        val demandValue = FltX.one
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
                    FltX
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

        var capturedRequest: Bpp3dLayerGenerationRequest<FltX>? = null
        var analyzedState: ColumnGenerationState<FltX>? = null
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<FltX> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
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
        assertEquals(listOf(FltX(11.0)), result.lpObjectives)
        assertEquals(1, result.columns.size)
        assertEquals(1, finalState.bins.size)
        assertTrue(request.demandEntries.any { it.mode is Bpp3dDemandMode.ItemAmount })
        assertNotNull(request.scoreByShadowPrice)
        val demandKey = DemandModeKey(
            mode = Bpp3dDemandMode.ItemAmount,
            key = Bpp3dDemandKey.Item(actualItem)
        )
        assertEquals(FltX(7.0), request.shadowPrices[demandKey] ?: FltX.zero)
    }

    @Test
    fun standardExecutorsFactoryShouldUseModelItemDemands() {
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
        val materialCache = LinkedHashMap<QuantityMaterial<FltX>, Material<FltX>>()
        val modelItem = quantityItem.toModel(materialCache, itemCache)
        val demandEntries: List<Bpp3dDemandEntry<FltX>> = listOf(
            fixedDemandEntry(
                mode = Bpp3dDemandMode.ItemAmount,
                key = Bpp3dDemandKey.Item(modelItem),
                demand = FltX.one
            )
        )
        val solver = object : ColumnGenerationSolver {
            override val name = "stub-quantity-factory-solver"

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
            itemDemands = listOf(Pair(modelItem, UInt64.one)),
            demandEntries = demandEntries
        )

        assertNotNull(executors)
    }

    @Test
    fun standardExecutorsFactoryShouldSupportQuantityItemDemands() {
        val quantityMaterial = QuantityMaterial(
            no = MaterialNo("M-QG"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-QG",
            weight = FltX.one * Kilogram
        )
        val quantityItem = QuantityItem(
            id = "item-qg",
            name = "item-qg",
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
            batchNo = BatchNo("B-QG"),
            packageAttribute = packageAttribute()
        )
        val demandEntries: List<Bpp3dDemandEntry<FltX>> = listOf(
            fixedDemandEntry(
                mode = Bpp3dDemandMode.Material,
                key = Bpp3dDemandKey.Material(
                    MaterialKey(
                        no = quantityMaterial.no,
                        type = quantityMaterial.type
                    )
                ),
                demand = FltX.one
            )
        )
        val solver = object : ColumnGenerationSolver {
            override val name = "stub-quantity-factory-entry-solver"

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

        val executors = ColumnGenerationStandardExecutors.fromQuantityDemandEntries(
            solver = solver,
            itemDemands = listOf(Pair(quantityItem, UInt64.one)),
            demandEntries = demandEntries
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

        val request = columnGenerationApplicationRequestFromQuantity(
            itemDemands = listOf(Pair(quantityItem, UInt64(3))),
            materialAmountDemands = listOf(Pair(quantityMaterial, UInt64(6))),
            materialWeightDemands = listOf(Pair(quantityMaterial, FltX(2.5) * Kilogram)),
            initialColumns = listOf(quantityInitialLayer)
        )

        assertEquals(1, request.itemDemands.size)
        assertEquals("item-rq", (request.itemDemands.first().first as ActualItem).id)
        assertEquals(UInt64(3), request.itemDemands.first().second)
        assertEquals(1, request.materialAmountDemands.size)
        assertEquals(UInt64(6), request.materialAmountDemands.first().second)
        assertEquals(1, request.materialWeightDemands.size)
        assertEquals(2.5, request.materialWeightDemands.first().second.value.toDouble(), 1e-9)
        assertTrue(request.materialAmountDemands.first().first === request.materialWeightDemands.first().first)
        assertEquals(1, request.initialColumns.size)
        assertEquals(1, request.initialColumns.first().units.size)
    }

    @Test
    fun applicationServiceShouldSupportModelDemandEntryPoint() = runBlocking {
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
        val request = ColumnGenerationQuantityApplicationRequest(
            itemDemands = listOf(Pair(quantityItem, UInt64.one)),
            initialColumns = listOf(quantityInitialLayer),
            generators = listOf(
                object : Bpp3dLayerGenerator<FltX> {
                    override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                        return emptyList()
                    }
                }
            ),
            cgConfig = ColumnGenerationConfig(finalMilpEnabled = false)
        )
        val response = service.solve(
            request = request
        )

        assertEquals(1, response.result.lpSolvedTimes)
        assertEquals(listOf(FltX(3.0)), response.result.lpObjectives)
        assertEquals(1, response.result.columns.size)
        assertEquals(1, response.result.columns.first().units.size)
        assertTrue(!response.result.finalSolved)
    }

    @Test
    fun applicationServiceShouldRejectFinalMilpSelectedBinsByDepthBoundaryPolicy() = runBlocking {
        val material = Material(
            no = MaterialNo("M-DEPTH-FINAL"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-DEPTH-FINAL",
            weight = FltX.one * Kilogram
        )
        val actualItem = item("item-depth-final", material)
        val seedBin = layerBin(listOf(actualItem))
        val seedLayer = seedBin.units.first().unit
        val finalBin = layerBinOf(
            shape = seedBin.type,
            units = emptyList<BinLayerPlacement>(),
            batchNo = seedBin.batchNo
        )
        val demandEntries = listOf(
            fixedDemandEntry(
                mode = Bpp3dDemandMode.ItemAmount,
                key = Bpp3dDemandKey.Item(actualItem),
                demand = FltX.one
            )
        )
        val solver = object : ColumnGenerationSolver {
            override val name: String = "stub-depth-boundary-final-solver"

            override suspend fun solveMILP(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<FeasibleSolverOutput<Flt64>> {
                val output = FeasibleSolverOutput(
                    obj = Flt64(5.0),
                    solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.one },
                    time = Duration.ZERO,
                    possibleBestObj = Flt64(5.0),
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
                val output = FeasibleSolverOutput(
                    obj = Flt64(3.0),
                    solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.zero },
                    time = Duration.ZERO,
                    possibleBestObj = Flt64(3.0),
                    gap = Flt64.zero
                )
                return Ok(
                    lpResultOf(
                        result = output,
                        dualSolution = linkedMapOf(fakeConstraint(tagged) to Flt64.one)
                    )
                )
            }
        }

        val exception = assertFailsWith<IllegalArgumentException> {
            ColumnGenerationApplicationService(solver).solve(
                request = ColumnGenerationApplicationRequest(
                    itemDemands = listOf(Pair(actualItem, UInt64.one)),
                    demandEntries = demandEntries,
                    initialColumns = listOf(seedLayer),
                    finalBins = listOf(finalBin),
                    generators = listOf(
                        object : Bpp3dLayerGenerator<FltX> {
                            override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                                return emptyList()
                            }
                        }
                    ),
                    depthBoundaryLayerOrientationPolicy = DepthBoundaryLayerOrientationPolicy(
                        firstLayerAllowedCuboidOrientations = setOf(Orientation.Side)
                    )
                )
            )
        }

        assertTrue(exception.message?.contains("boundary=first") == true)
        assertTrue(exception.message?.contains("cuboid_orientation=Upright") == true)
    }

    @Test
    fun applicationServiceShouldRejectConflictingDepthBoundaryPolicySources() = runBlocking {
        val material = Material(
            no = MaterialNo("M-DEPTH-CONFLICT"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-DEPTH-CONFLICT",
            weight = FltX.one * Kilogram
        )
        val actualItem = item("item-depth-conflict", material)
        val requestPolicy = DepthBoundaryLayerOrientationPolicy(
            firstLayerAllowedCuboidOrientations = setOf(Orientation.Upright)
        )
        val executorPolicy = DepthBoundaryLayerOrientationPolicy(
            firstLayerAllowedCuboidOrientations = setOf(Orientation.Side)
        )
        val solver = object : ColumnGenerationSolver {
            override val name: String = "stub-depth-boundary-conflict-solver"

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

        val exception = assertFailsWith<IllegalArgumentException> {
            ColumnGenerationApplicationService(solver).solve(
                request = ColumnGenerationApplicationRequest(
                    itemDemands = listOf(Pair(actualItem, UInt64.one)),
                    depthBoundaryLayerOrientationPolicy = requestPolicy,
                    executorConfig = ColumnGenerationStandardExecutorConfig(
                        depthBoundaryLayerOrientationPolicy = executorPolicy
                    )
                )
            )
        }

        assertTrue(exception.message?.contains("conflicts with executorConfig") == true)
    }

    @Test
    fun applicationServiceShouldBridgeExecutorsLayerGenerationAndPacking() = runBlocking {
        val material = Material(
            no = MaterialNo("M-3"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-3",
            weight = FltX.half * Kilogram
        )
        val actualItem = item("item-3", material)
        val seedBin = layerBin(listOf(actualItem))
        val seedLayer = seedBin.units.first().unit
        val finalBin: LayerBin = layerBinOf(
            shape = seedBin.type,
            units = emptyList<BinLayerPlacement>(),
            batchNo = seedBin.batchNo
        )
        val demandValue = FltX.one
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
                    FltX
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
                    object : Bpp3dLayerGenerator<FltX> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )

        assertTrue(response.result.finalSolved)
        assertEquals(listOf(FltX(11.0)), response.result.lpObjectives)
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
            (snapshot.demandModeShadowPriceTotals["item_amount"] ?: FltX.zero).toDouble() > 0.0
        )
    }

    @Test
    fun applicationServiceShouldSupportMaterialWeightOnlyDemandPackingFlow() = runBlocking {
        val material = Material(
            no = MaterialNo("M-4"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-4",
            weight = FltX.one * Kilogram
        )
        val actualItem = item("item-4", material)
        val seedBin = layerBin(listOf(actualItem))
        val seedLayer = seedBin.units.first().unit
        val finalBin: LayerBin = layerBinOf(
            shape = seedBin.type,
            units = emptyList<BinLayerPlacement>(),
            batchNo = seedBin.batchNo
        )
        val demandValue = FltX(2.0)
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
                    FltX
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
                    object : Bpp3dLayerGenerator<FltX> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )

        assertTrue(response.result.finalSolved)
        assertEquals(listOf(FltX(13.0)), response.result.lpObjectives)
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
            weight = FltX.one * Kilogram
        )
        val materialB = Material(
            no = MaterialNo("M-5B"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-5B",
            weight = fltX(2.0) * Kilogram
        )
        val itemA = item("item-5a", materialA)
        val itemB = item("item-5b", materialB)
        val seedBinA = layerBin(listOf(itemA))
        val seedBinB = layerBin(listOf(itemB))
        val seedLayerA = seedBinA.units.first().unit
        val seedLayerB = seedBinB.units.first().unit
        val finalBins = listOf(
            layerBinOf(
                shape = seedBinA.type,
                units = emptyList<BinLayerPlacement>(),
                batchNo = seedBinA.batchNo
            ),
            layerBinOf(
                shape = seedBinB.type,
                units = emptyList<BinLayerPlacement>(),
                batchNo = seedBinB.batchNo
            )
        )
        val demandEntries: List<Bpp3dDemandEntry<FltX>> = listOf(
            fixedDemandEntry(
                mode = Bpp3dDemandMode.ItemAmount,
                key = Bpp3dDemandKey.Item(itemA),
                demand = FltX(2.0)
            ),
            fixedDemandEntry(
                mode = Bpp3dDemandMode.ItemAmount,
                key = Bpp3dDemandKey.Item(itemB),
                demand = FltX.one
            ),
            fixedDemandEntry(
                mode = Bpp3dDemandMode.ItemMaterialWeight,
                key = Bpp3dDemandKey.Material(materialA.key),
                demand = FltX(2.0)
            ),
            fixedDemandEntry(
                mode = Bpp3dDemandMode.ItemMaterialWeight,
                key = Bpp3dDemandKey.Material(materialB.key),
                demand = FltX(4.0)
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
                    object : Bpp3dLayerGenerator<FltX> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )

        assertTrue(response.result.finalSolved)
        assertEquals(listOf(FltX(19.0)), response.result.lpObjectives)
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
                weight = FltX.one * Kilogram
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
            layerBinOf(
                shape = seedBin.type,
                units = emptyList<BinLayerPlacement>(),
                batchNo = seedBin.batchNo
            )
        }
        val demandEntries: List<Bpp3dDemandEntry<FltX>> = buildList {
            addAll(items.map { actualItem ->
                fixedDemandEntry(
                    mode = Bpp3dDemandMode.ItemAmount,
                    key = Bpp3dDemandKey.Item(actualItem),
                    demand = FltX.one
                )
            })
            addAll(materials.map { material ->
                fixedDemandEntry(
                    mode = Bpp3dDemandMode.ItemMaterialWeight,
                    key = Bpp3dDemandKey.Material(material.key),
                    demand = fltX(layerCount.toDouble() / materialCount.toDouble())
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
                    object : Bpp3dLayerGenerator<FltX> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )

        assertTrue(response.result.finalSolved)
        assertEquals(listOf(FltX(37.0)), response.result.lpObjectives)
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








