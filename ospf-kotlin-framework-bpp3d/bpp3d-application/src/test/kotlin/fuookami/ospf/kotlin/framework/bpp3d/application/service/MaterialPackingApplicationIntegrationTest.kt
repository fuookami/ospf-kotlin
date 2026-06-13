package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.intermediate.Cell
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LayerBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Package
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackingProgram
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackingProgramMaterialValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.asContainer3Shape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.layerBinOf
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits.DemandShadowPriceKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerationRequest
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerationResult
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.toLayerGenerationItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingProgramCandidate
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.fltX
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration

class MaterialPackingApplicationIntegrationTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun enableCompanionFallback() {
            System.setProperty("ospf.kotlin.math.enableCompanionReflectionFallback", "true")
        }
    }

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

    private fun material(no: String, unitWeightKg: FltX): Material<FltX> {
        return Material(
            no = MaterialNo(no),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = no,
            weight = unitWeightKg * Kilogram
        )
    }

    private fun candidate(
        id: String,
        material: Material<FltX>,
        amountPerPackage: UInt64
    ): MaterialPackingProgramCandidate<FltX> {
        return MaterialPackingProgramCandidate(
            id = id,
            itemName = id,
            program = PackingProgram.innerPackage(
                shape = PackageShape(
                    width = fltX(1.0) * Meter,
                    height = fltX(1.0) * Meter,
                    depth = fltX(1.0) * Meter,
                    weight = fltX(1.0) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material.key to amountPerPackage)
            )
        )
    }

    private fun seedItem(id: String, material: Material<FltX>): ActualItem {
        val pack = Package.innerPackage(
            shape = PackageShape(
                width = fltX(1.0) * Meter,
                height = fltX(1.0) * Meter,
                depth = fltX(1.0) * Meter,
                weight = fltX(1.0) * Kilogram,
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

    private fun seedLayer(
        item: ActualItem,
        binDepthMeter: Double = 3.0
    ): BinLayer {
        val binType = BinType(
            width = fltX(3.0) * Meter,
            height = fltX(3.0) * Meter,
            depth = fltX(binDepthMeter) * Meter,
            capacity = fltX(100.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-SEED"
        )
        return BinLayer(
            iteration = fuookami.ospf.kotlin.math.algebra.number.Int64.zero,
            from = MaterialPackingApplicationIntegrationTest::class,
            bin = binType,
            shape = Container3Shape(binType.asContainer3Shape()),
            units = listOf(
                item.toItemPlacement()
            )
        )
    }

    private fun finalBinOf(layer: BinLayer): LayerBin {
        val binShape = layer.bin!!
        return layerBinOf(
            shape = binShape,
            units = emptyList()
        )
    }

    private inner class FixedValueSolver(private val milpValue: Flt64) : ColumnGenerationSolver {
        override val name: String = "fixed-value-solver"

        override suspend fun solveMILP(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<FeasibleSolverOutput<Flt64>> {
            val solution = List(metaModel.tokens.tokensInSolver.size) { milpValue }
            return Ok(
                FeasibleSolverOutput(
                    obj = Flt64(10.0),
                    solution = solution,
                    time = Duration.ZERO,
                    possibleBestObj = Flt64(10.0),
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
            val dual = linkedMapOf<Constraint<Flt64, Linear>, Flt64>()
            metaModel.constraints
                .filter { constraint -> constraint.args is DemandShadowPriceKey }
                .forEachIndexed { index, constraint ->
                    dual[fakeConstraint(constraint)] = Flt64(index.toDouble() + 1.0)
                }
            return Ok(
                lpResultOf(
                    result = FeasibleSolverOutput(
                        obj = Flt64(5.0),
                        solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.zero },
                        time = Duration.ZERO,
                        possibleBestObj = Flt64(5.0),
                        gap = Flt64.zero
                    ),
                    dualSolution = dual
                )
            )
        }
    }

    @Test
    fun materialAmountOnlyShouldBePackedThenEnterCgFlow() = runBlocking {
        val material = material("M-A1", FltX.one)
        val seed = seedItem("seed-a1", material)
        val layer = seedLayer(seed)
        val service = ColumnGenerationApplicationService(FixedValueSolver(milpValue = Flt64(3.0)))

        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = emptyList(),
                materialAmountDemands = listOf(Pair(material, UInt64(3))),
                materialPackingCandidates = listOf(candidate("pack-a1", material, UInt64.one)),
                initialColumns = listOf(layer),
                finalBins = listOf(finalBinOf(layer)),
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

        val plan = response.materialPackingPlan
        assertNotNull(plan)
        assertEquals(UInt64(3), plan.solveInfo.selectedPackageCount)
        assertEquals(UInt64(3), plan.normalizedDemands[material.key])
        assertTrue(response.result.finalSolved)
    }

    @Test
    fun materialWeightOnlyShouldBePackedThenEnterCgFlow() = runBlocking {
        val material = material("M-W1", FltX(2.0))
        val seed = seedItem("seed-w1", material)
        val layer = seedLayer(seed)
        val service = ColumnGenerationApplicationService(FixedValueSolver(milpValue = Flt64(3.0)))

        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = emptyList(),
                materialWeightDemands = listOf(Pair(material, fltX(5.0) * Kilogram)),
                materialPackingCandidates = listOf(candidate("pack-w1", material, UInt64.one)),
                initialColumns = listOf(layer),
                finalBins = listOf(finalBinOf(layer)),
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

        val plan = response.materialPackingPlan
        assertNotNull(plan)
        assertEquals(UInt64(3), plan.normalizedDemands[material.key])
        assertEquals(UInt64(3), plan.solveInfo.selectedPackageCount)
        assertTrue(response.result.finalSolved)
    }

    @Test
    fun materialPackingSummaryShouldMatchFinalPackingAnalyzerSummary() = runBlocking {
        val material = material("M-S1", FltX.one)
        val seed = seedItem("seed-s1", material)
        val layer = seedLayer(seed, binDepthMeter = 4.0)
        val service = ColumnGenerationApplicationService(FixedValueSolver(milpValue = Flt64(4.0)))

        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = emptyList(),
                materialAmountDemands = listOf(Pair(material, UInt64(4))),
                materialPackingCandidates = listOf(candidate("pack-s1", material, UInt64.one)),
                initialColumns = listOf(layer),
                finalBins = listOf(finalBinOf(layer)),
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

        val plan = response.materialPackingPlan
        assertNotNull(plan)
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        val materialSummary = snapshot.packingResult.materialSummary.associate { entry ->
            entry.material to entry.amount
        }
        assertEquals(plan.normalizedDemands[material.key], materialSummary[material.key])
    }

    @Test
    fun layerGenerationProgramDemandShouldWorkWithoutMaterialPacker() = runBlocking {
        val material = material("M-PROGRAM-DIRECT", FltX(2.0))
        val candidate = MaterialPackingProgramCandidate(
            id = "program-direct",
            program = PackingProgram.innerPackageWithMaterialValues(
                shape = PackageShape(
                    width = fltX(1.0) * Meter,
                    height = fltX(1.0) * Meter,
                    depth = fltX(1.0) * Meter,
                    weight = fltX(1.0) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(
                    material.key to PackingProgramMaterialValue(
                        amount = UInt64(2)
                    )
                )
            )
        )
        val programItem = candidate.toLayerGenerationItem(
            sequence = 1,
            materialCatalog = mapOf(material.key to material)
        ) as ActualItem
        val layer = seedLayer(programItem)
        val service = ColumnGenerationApplicationService(FixedValueSolver(milpValue = Flt64.one))

        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = emptyList(),
                materialAmountDemands = listOf(Pair(material, UInt64(2))),
                layerGenerationProgramDemands = listOf(Pair(candidate, UInt64.one)),
                programMaterialCatalog = mapOf(material.key to material),
                initialColumns = listOf(layer),
                finalBins = listOf(finalBinOf(layer)),
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
        assertEquals(null, response.materialPackingPlan)
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        val materialSummary = snapshot.packingResult.materialSummary.associate { entry ->
            entry.material to entry.amount
        }
        assertEquals(UInt64(2), materialSummary[material.key])
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






