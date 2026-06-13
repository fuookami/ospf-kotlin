/**
 * 包装方案适配器测试。
 * Package solution-like adapter test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class PackageSolutionLikeAdapterTest {
    private object CargoAttr : AbstractCargoAttribute

    private fun material(
        no: String,
        unitWeightKg: FltX
    ): Material<FltX> {
        return Material(
            no = MaterialNo(no),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = no,
            weight = unitWeightKg * Kilogram
        )
    }

    private fun shape(scale: FltX): PackageShape<FltX> {
        return PackageShape(
            width = scale * Meter,
            height = scale * Meter,
            depth = scale * Meter,
            weight = scale * Kilogram,
            packageType = PackageType.CartonContainer
        )
    }

    @Test
    fun materialQuantityKindsShouldMapToPackingProgramValues() {
        val materialAmount = material(
            no = "M-APS-AMOUNT",
            unitWeightKg = FltX.one
        )
        val materialWeight = material(
            no = "M-APS-WEIGHT",
            unitWeightKg = FltX.one
        )
        val materialBoth = material(
            no = "M-APS-BOTH",
            unitWeightKg = FltX.one
        )
        val node = PackageSolutionLikeNode(
            shape = shape(FltX.one),
            materialItems = listOf(
                PackageSolutionLikeMaterialItem(
                    material = materialAmount.key,
                    quantity = PackageSolutionLikeQuantity.Amount(UInt64(2))
                ),
                PackageSolutionLikeMaterialItem(
                    material = materialWeight.key,
                    quantity = PackageSolutionLikeQuantity.Weight(fltX(5.0) * Kilogram)
                ),
                PackageSolutionLikeMaterialItem(
                    material = materialBoth.key,
                    quantity = PackageSolutionLikeQuantity.AmountAndWeight(
                        amount = UInt64(3),
                        weight = fltX(7.0) * Kilogram
                    )
                )
            )
        )

        val program = node.toPackingProgram()
        assertEquals(UInt64(2), program.materialAmounts()[materialAmount.key])
        assertTrue(program.materialAmounts().containsKey(materialBoth.key))
        assertTrue(!program.materialAmounts().containsKey(materialWeight.key))
        val weightOnly = program.materialWeights()[materialWeight.key]?.value?.toDouble()
        val bothWeight = program.materialWeights()[materialBoth.key]?.value?.toDouble()
        assertNotNull(weightOnly)
        assertNotNull(bothWeight)
        assertEquals(5.0, weightOnly, 1e-10)
        assertEquals(7.0, bothWeight, 1e-10)
    }

    @Test
    fun childrenShouldMapToOuterPackingProgramAndAggregateMaterials() {
        val material = material(
            no = "M-APS-CHILD",
            unitWeightKg = FltX.one
        )
        val childByAmount = PackageSolutionLikeNode(
            shape = shape(FltX.one),
            materialItems = listOf(
                PackageSolutionLikeMaterialItem(
                    material = material.key,
                    quantity = PackageSolutionLikeQuantity.Amount(UInt64(2))
                )
            )
        )
        val childByWeight = PackageSolutionLikeNode(
            shape = shape(fltX(1.2)),
            materialItems = listOf(
                PackageSolutionLikeMaterialItem(
                    material = material.key,
                    quantity = PackageSolutionLikeQuantity.Weight(fltX(4.0) * Kilogram)
                )
            )
        )
        val parent = PackageSolutionLikeNode(
            shape = shape(fltX(2.0)),
            materialItems = listOf(
                PackageSolutionLikeMaterialItem(
                    material = material.key,
                    quantity = PackageSolutionLikeQuantity.AmountAndWeight(
                        amount = UInt64.one,
                        weight = fltX(1.0) * Kilogram
                    )
                )
            ),
            children = listOf(childByAmount, childByWeight)
        )

        val program = parent.toPackingProgram()
        assertEquals(PackageClassification.Outer, program.classification)
        assertEquals(2, program.packages?.size)
        assertEquals(UInt64(3), program.materialAmounts()[material.key])
        val aggregatedWeight = program.materialWeights()[material.key]?.value?.toDouble()
        assertNotNull(aggregatedWeight)
        assertEquals(5.0, aggregatedWeight, 1e-10)
    }

    @Test
    fun fltXMaterialQuantityShouldMapThroughPackageSolutionLikeAdapter() {
        val material = material(
            no = "M-APS-FLTX",
            unitWeightKg = FltX.one
        )
        val node = PackageSolutionLikeNode(
            shape = shape(FltX.one),
            materialItems = listOf(
                PackageSolutionLikeMaterialItem(
                    material = material.key,
                    quantity = PackageSolutionLikeQuantity.Weight(FltX(2.5) * Kilogram)
                )
            )
        )

        val program = node.toPackingProgram()
        assertTrue(program.materials[material.key]?.weight?.value is FltX)
        val fltXWeight = program.materialWeights()[material.key]?.value?.toDouble()
        assertNotNull(fltXWeight)
        assertEquals(2.5, fltXWeight, 1e-10)
    }
}
