package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageClassification
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackageSolutionLikeAdapterTest {
    private object CargoAttr : AbstractCargoAttribute

    private fun material(
        no: String,
        unitWeightKg: Flt64
    ): Material {
        return Material(
            no = MaterialNo(no),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = no,
            weight = unitWeightKg * Kilogram
        )
    }

    private fun shape(scale: Flt64): PackageShape {
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
            unitWeightKg = Flt64.one
        )
        val materialWeight = material(
            no = "M-APS-WEIGHT",
            unitWeightKg = Flt64.one
        )
        val materialBoth = material(
            no = "M-APS-BOTH",
            unitWeightKg = Flt64.one
        )
        val node = PackageSolutionLikeNode(
            shape = shape(Flt64.one),
            materialItems = listOf(
                PackageSolutionLikeMaterialItem(
                    material = materialAmount.key,
                    quantity = PackageSolutionLikeQuantity.Amount(UInt64(2))
                ),
                PackageSolutionLikeMaterialItem(
                    material = materialWeight.key,
                    quantity = PackageSolutionLikeQuantity.Weight(5.0 * Kilogram)
                ),
                PackageSolutionLikeMaterialItem(
                    material = materialBoth.key,
                    quantity = PackageSolutionLikeQuantity.AmountAndWeight(
                        amount = UInt64(3),
                        weight = 7.0 * Kilogram
                    )
                )
            )
        )

        val program = node.toPackingProgram()
        assertEquals(UInt64(2), program.materialAmounts()[materialAmount.key])
        assertTrue(program.materialAmounts().containsKey(materialBoth.key))
        assertTrue(!program.materialAmounts().containsKey(materialWeight.key))
        assertEquals(Flt64(5.0), program.materialWeights()[materialWeight.key]?.value)
        assertEquals(Flt64(7.0), program.materialWeights()[materialBoth.key]?.value)
    }

    @Test
    fun childrenShouldMapToOuterPackingProgramAndAggregateMaterials() {
        val material = material(
            no = "M-APS-CHILD",
            unitWeightKg = Flt64.one
        )
        val childByAmount = PackageSolutionLikeNode(
            shape = shape(Flt64.one),
            materialItems = listOf(
                PackageSolutionLikeMaterialItem(
                    material = material.key,
                    quantity = PackageSolutionLikeQuantity.Amount(UInt64(2))
                )
            )
        )
        val childByWeight = PackageSolutionLikeNode(
            shape = shape(Flt64(1.2)),
            materialItems = listOf(
                PackageSolutionLikeMaterialItem(
                    material = material.key,
                    quantity = PackageSolutionLikeQuantity.Weight(4.0 * Kilogram)
                )
            )
        )
        val parent = PackageSolutionLikeNode(
            shape = shape(Flt64(2.0)),
            materialItems = listOf(
                PackageSolutionLikeMaterialItem(
                    material = material.key,
                    quantity = PackageSolutionLikeQuantity.AmountAndWeight(
                        amount = UInt64.one,
                        weight = 1.0 * Kilogram
                    )
                )
            ),
            children = listOf(childByAmount, childByWeight)
        )

        val program = parent.toPackingProgram()
        assertEquals(PackageClassification.Outer, program.classification)
        assertEquals(2, program.packages?.size)
        assertEquals(UInt64(3), program.materialAmounts()[material.key])
        assertEquals(Flt64(5.0), program.materialWeights()[material.key]?.value)
    }

    @Test
    fun fltXMaterialQuantityShouldMapThroughPackageSolutionLikeAdapter() {
        val material = material(
            no = "M-APS-FLTX",
            unitWeightKg = Flt64.one
        )
        val node = PackageSolutionLikeNode(
            shape = shape(Flt64.one),
            materialItems = listOf(
                PackageSolutionLikeMaterialItem(
                    material = material.key,
                    quantity = PackageSolutionLikeQuantity.Weight(FltX(2.5) * Kilogram)
                )
            )
        )

        val program = node.toPackingProgram()
        assertTrue(program.materials[material.key]?.weight?.value is FltX)
        assertEquals(Flt64(2.5), program.materialWeights()[material.key]?.value)
    }
}
