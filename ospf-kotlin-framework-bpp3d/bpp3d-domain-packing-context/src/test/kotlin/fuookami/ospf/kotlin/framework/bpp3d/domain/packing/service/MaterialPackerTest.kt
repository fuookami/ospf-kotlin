package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackingProgram
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingDemand
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingObjectiveConfig
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingProgramCandidate
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingStatus
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaterialPackerTest {
    private object CargoAttr : AbstractCargoAttribute

    private fun material(no: String, unitWeightKg: InfraNumber): Material<InfraNumber> {
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
        widthMeter: InfraNumber,
        materials: Map<Material<InfraNumber>, UInt64>
    ): MaterialPackingProgramCandidate<InfraNumber> {
        return MaterialPackingProgramCandidate(
            id = id,
            program = PackingProgram.innerPackage(
                shape = PackageShape(
                    width = widthMeter * Meter,
                    height = infraScalar(1.0) * Meter,
                    depth = infraScalar(1.0) * Meter,
                    weight = infraScalar(1.0) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = materials.map { (material, amount) -> Pair(material.key, amount) }.toMap()
            )
        )
    }

    @Test
    fun shouldSelectOneFiveAndOneTwoForSingleMaterialAmountDemand() = runBlocking {
        val material = material("M-1", InfraNumber.one)
        val plan = MaterialPacker().plan(
            demands = listOf(
                MaterialPackingDemand(
                    material = material,
                    amount = UInt64(7)
                )
            ),
            candidates = listOf(
                candidate("pack-5", infraScalar(5.0), mapOf(material to UInt64(5))),
                candidate("pack-2", infraScalar(2.0), mapOf(material to UInt64(2)))
            )
        )

        val selected = plan.selections.associate { it.candidate.id to it.amount }
        assertEquals(UInt64.one, selected["pack-5"])
        assertEquals(UInt64.one, selected["pack-2"])
        assertEquals(MaterialPackingStatus.Optimal, plan.solveInfo.status)
        assertTrue(plan.restMaterials.isEmpty())
    }

    @Test
    fun shouldConvertWeightDemandToAmountByCeilRule() = runBlocking {
        val material = material("M-2", infraScalar(2.0))
        val plan = MaterialPacker().plan(
            demands = listOf(
                MaterialPackingDemand(
                    material = material,
                    weight = infraScalar(5.0) * Kilogram
                )
            ),
            candidates = listOf(
                candidate("pack-1", InfraNumber.one, mapOf(material to UInt64.one))
            )
        )

        assertEquals(UInt64(3), plan.normalizedDemands[material.key])
        assertEquals(UInt64(3), plan.solveInfo.selectedPackageCount)
        assertEquals(MaterialPackingStatus.Optimal, plan.solveInfo.status)
    }

    @Test
    fun shouldConvertFltXWeightDemandToAmountByCeilRule() = runBlocking {
        val material = material("M-2X", infraScalar(2.0))
        val plan = MaterialPacker().plan(
            demands = listOf(
                MaterialPackingDemand(
                    material = material,
                    weight = FltX(5.0) * Kilogram
                )
            ),
            candidates = listOf(
                candidate("pack-1x", InfraNumber.one, mapOf(material to UInt64.one))
            )
        )

        assertEquals(UInt64(3), plan.normalizedDemands[material.key])
        assertEquals(UInt64(3), plan.solveInfo.selectedPackageCount)
        assertEquals(MaterialPackingStatus.Optimal, plan.solveInfo.status)
    }

    @Test
    fun shouldPreferComboProgramWhenItNeedsLessPackages() = runBlocking {
        val materialA = material("M-3A", InfraNumber.one)
        val materialB = material("M-3B", InfraNumber.one)
        val plan = MaterialPacker().plan(
            demands = listOf(
                MaterialPackingDemand(materialA, UInt64.one),
                MaterialPackingDemand(materialB, UInt64.one)
            ),
            candidates = listOf(
                candidate(
                    id = "combo",
                    widthMeter = InfraNumber.one,
                    materials = mapOf(
                        materialA to UInt64.one,
                        materialB to UInt64.one
                    )
                ),
                candidate("single-a", infraScalar(0.5), mapOf(materialA to UInt64.one)),
                candidate("single-b", infraScalar(0.5), mapOf(materialB to UInt64.one))
            )
        )

        val selected = plan.selections.associate { it.candidate.id to it.amount }
        assertEquals(UInt64.one, selected["combo"])
        assertTrue((selected["single-a"] ?: UInt64.zero) == UInt64.zero)
        assertTrue((selected["single-b"] ?: UInt64.zero) == UInt64.zero)
    }

    @Test
    fun shouldMarkUnfilledPackageAsPending() = runBlocking {
        val material = material("M-4", InfraNumber.one)
        val plan = MaterialPacker().plan(
            demands = listOf(MaterialPackingDemand(material, UInt64(6))),
            candidates = listOf(candidate("pack-5", infraScalar(5.0), mapOf(material to UInt64(5))))
        )

        assertEquals(2, plan.packages.size)
        assertTrue(plan.packages.any { it.pending })
        assertTrue(plan.packages.any { !it.pending })
    }

    @Test
    fun shouldReturnInfeasibleWhenMaterialCannotBeCovered() = runBlocking {
        val demandMaterial = material("M-5A", InfraNumber.one)
        val candidateMaterial = material("M-5B", InfraNumber.one)
        val plan = MaterialPacker().plan(
            demands = listOf(MaterialPackingDemand(demandMaterial, UInt64.one)),
            candidates = listOf(candidate("pack-b", InfraNumber.one, mapOf(candidateMaterial to UInt64.one)))
        )

        assertEquals(MaterialPackingStatus.Infeasible, plan.solveInfo.status)
        assertEquals(UInt64.one, plan.restMaterials[demandMaterial.key])
    }

    @Test
    fun shouldBreakTieByLowerVolumeWhenPackageCountAndSlackAreEqual() = runBlocking {
        val material = material("M-6", InfraNumber.one)
        val plan = MaterialPacker().plan(
            demands = listOf(MaterialPackingDemand(material, UInt64(4))),
            candidates = listOf(
                candidate("high-volume", infraScalar(8.0), mapOf(material to UInt64(4u))),
                candidate("low-volume", infraScalar(4.0), mapOf(material to UInt64(4u)))
            ),
            objective = MaterialPackingObjectiveConfig(
                packageCountWeight = infraScalar(1_000_000.0),
                volumeWeight = infraScalar(1_000.0),
                slackWeight = InfraNumber.one
            )
        )

        val selected = plan.selections.associate { it.candidate.id to it.amount }
        assertEquals(UInt64.one, selected["low-volume"])
        assertTrue((selected["high-volume"] ?: UInt64.zero) == UInt64.zero)
    }
}

