/**
 * 物料装箱器测试。
 * Material packer test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.*

class MaterialPackerTest {
    private object CargoAttr : AbstractCargoAttribute

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
        widthMeter: FltX,
        materials: Map<Material<FltX>, UInt64>
    ): MaterialPackingProgramCandidate<FltX> {
        return MaterialPackingProgramCandidate(
            id = id,
            program = PackingProgram.innerPackage(
                shape = PackageShape(
                    width = widthMeter * Meter,
                    height = fltX(1.0) * Meter,
                    depth = fltX(1.0) * Meter,
                    weight = fltX(1.0) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = materials.map { (material, amount) -> Pair(material.key, amount) }.toMap()
            )
        )
    }

    @Test
    fun shouldSelectOneFiveAndOneTwoForSingleMaterialAmountDemand() = runBlocking {
        val material = material("M-1", FltX.one)
        val plan = MaterialPacker().plan(
            demands = listOf(
                MaterialPackingDemand(
                    material = material,
                    amount = UInt64(7)
                )
            ),
            candidates = listOf(
                candidate("pack-5", fltX(5.0), mapOf(material to UInt64(5))),
                candidate("pack-2", fltX(2.0), mapOf(material to UInt64(2)))
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
        val material = material("M-2", fltX(2.0))
        val plan = MaterialPacker().plan(
            demands = listOf(
                MaterialPackingDemand(
                    material = material,
                    weight = fltX(5.0) * Kilogram
                )
            ),
            candidates = listOf(
                candidate("pack-1", FltX.one, mapOf(material to UInt64.one))
            )
        )

        assertEquals(UInt64(3), plan.normalizedDemands[material.key])
        assertEquals(UInt64(3), plan.solveInfo.selectedPackageCount)
        assertEquals(MaterialPackingStatus.Optimal, plan.solveInfo.status)
    }

    @Test
    fun shouldConvertFltXWeightDemandToAmountByCeilRule() = runBlocking {
        val material = material("M-2X", fltX(2.0))
        val plan = MaterialPacker().plan(
            demands = listOf(
                MaterialPackingDemand(
                    material = material,
                    weight = FltX(5.0) * Kilogram
                )
            ),
            candidates = listOf(
                candidate("pack-1x", FltX.one, mapOf(material to UInt64.one))
            )
        )

        assertEquals(UInt64(3), plan.normalizedDemands[material.key])
        assertEquals(UInt64(3), plan.solveInfo.selectedPackageCount)
        assertEquals(MaterialPackingStatus.Optimal, plan.solveInfo.status)
    }

    @Test
    fun shouldPreferComboProgramWhenItNeedsLessPackages() = runBlocking {
        val materialA = material("M-3A", FltX.one)
        val materialB = material("M-3B", FltX.one)
        val plan = MaterialPacker().plan(
            demands = listOf(
                MaterialPackingDemand(materialA, UInt64.one),
                MaterialPackingDemand(materialB, UInt64.one)
            ),
            candidates = listOf(
                candidate(
                    id = "combo",
                    widthMeter = FltX.one,
                    materials = mapOf(
                        materialA to UInt64.one,
                        materialB to UInt64.one
                    )
                ),
                candidate("single-a", fltX(0.5), mapOf(materialA to UInt64.one)),
                candidate("single-b", fltX(0.5), mapOf(materialB to UInt64.one))
            )
        )

        val selected = plan.selections.associate { it.candidate.id to it.amount }
        assertEquals(UInt64.one, selected["combo"])
        assertTrue((selected["single-a"] ?: UInt64.zero) == UInt64.zero)
        assertTrue((selected["single-b"] ?: UInt64.zero) == UInt64.zero)
    }

    @Test
    fun shouldMarkUnfilledPackageAsPending() = runBlocking {
        val material = material("M-4", FltX.one)
        val plan = MaterialPacker().plan(
            demands = listOf(MaterialPackingDemand(material, UInt64(6))),
            candidates = listOf(candidate("pack-5", fltX(5.0), mapOf(material to UInt64(5))))
        )

        assertEquals(2, plan.packages.size)
        assertTrue(plan.packages.any { it.pending })
        assertTrue(plan.packages.any { !it.pending })
    }

    @Test
    fun shouldPreserveCylinderShapeSpecOnPackagedItems() = runBlocking {
        val material = material("M-CYLINDER", FltX.one)
        val radius = fltX(0.5) * Meter
        val plan = MaterialPacker().plan(
            demands = listOf(
                MaterialPackingDemand(
                    material = material,
                    amount = UInt64.one
                )
            ),
            candidates = listOf(
                MaterialPackingProgramCandidate(
                    id = "pack-cylinder",
                    program = PackingProgram.innerPackage(
                        shape = PackageShape(
                            width = fltX(1.0) * Meter,
                            height = fltX(1.2) * Meter,
                            depth = fltX(1.0) * Meter,
                            weight = fltX(1.0) * Kilogram,
                            packageType = PackageType.CartonContainer,
                            shapeSpec = PackageShapeSpec.VerticalCylinder(
                                radius = radius,
                                axis = Axis3.Y
                            )
                        ),
                        materials = mapOf(material.key to UInt64.one)
                    )
                )
            )
        )
        val shape = plan.packagedItems.single().item.packingShape

        assertTrue(shape is CylinderPackingShape3)
        assertEquals(Axis3.Y, shape.axis)
        assertEquals(0.5, shape.radius.value.toDouble(), 1e-10)
    }

    @Test
    fun shouldReturnInfeasibleWhenMaterialCannotBeCovered() = runBlocking {
        val demandMaterial = material("M-5A", FltX.one)
        val candidateMaterial = material("M-5B", FltX.one)
        val plan = MaterialPacker().plan(
            demands = listOf(MaterialPackingDemand(demandMaterial, UInt64.one)),
            candidates = listOf(candidate("pack-b", FltX.one, mapOf(candidateMaterial to UInt64.one)))
        )

        assertEquals(MaterialPackingStatus.Infeasible, plan.solveInfo.status)
        assertEquals(UInt64.one, plan.restMaterials[demandMaterial.key])
    }

    @Test
    fun shouldBreakTieByLowerVolumeWhenPackageCountAndSlackAreEqual() = runBlocking {
        val material = material("M-6", FltX.one)
        val plan = MaterialPacker().plan(
            demands = listOf(MaterialPackingDemand(material, UInt64(4))),
            candidates = listOf(
                candidate("high-volume", fltX(8.0), mapOf(material to UInt64(4u))),
                candidate("low-volume", fltX(4.0), mapOf(material to UInt64(4u)))
            ),
            objective = MaterialPackingObjectiveConfig(
                packageCountWeight = fltX(1_000_000.0),
                volumeWeight = fltX(1_000.0),
                slackWeight = FltX.one
            )
        )

        val selected = plan.selections.associate { it.candidate.id to it.amount }
        assertEquals(UInt64.one, selected["low-volume"])
        assertTrue((selected["high-volume"] ?: UInt64.zero) == UInt64.zero)
    }
}
