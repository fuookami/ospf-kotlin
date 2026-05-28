package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageClassification
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PackageSolutionLikeAdapterTest {
    private object CargoAttr : AbstractCargoAttribute

    private fun material(
        no: String,
        unitWeightKg: InfraNumber
    ): Material {
        return Material(
            no = MaterialNo(no),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = no,
            weight = unitWeightKg * Kilogram
        )
    }

    private fun shape(scale: InfraNumber): PackageShape {
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
            unitWeightKg = InfraNumber.one
        )
        val materialWeight = material(
            no = "M-APS-WEIGHT",
            unitWeightKg = InfraNumber.one
        )
        val materialBoth = material(
            no = "M-APS-BOTH",
            unitWeightKg = InfraNumber.one
        )
        val node = PackageSolutionLikeNode(
            shape = shape(InfraNumber.one),
            materialItems = listOf(
                PackageSolutionLikeMaterialItem(
                    material = materialAmount.key,
                    quantity = PackageSolutionLikeQuantity.Amount(UInt64(2))
                ),
                PackageSolutionLikeMaterialItem(
                    material = materialWeight.key,
                    quantity = PackageSolutionLikeQuantity.Weight(infraScalar(5.0) * Kilogram)
                ),
                PackageSolutionLikeMaterialItem(
                    material = materialBoth.key,
                    quantity = PackageSolutionLikeQuantity.AmountAndWeight(
                        amount = UInt64(3),
                        weight = infraScalar(7.0) * Kilogram
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
            unitWeightKg = InfraNumber.one
        )
        val childByAmount = PackageSolutionLikeNode(
            shape = shape(InfraNumber.one),
            materialItems = listOf(
                PackageSolutionLikeMaterialItem(
                    material = material.key,
                    quantity = PackageSolutionLikeQuantity.Amount(UInt64(2))
                )
            )
        )
        val childByWeight = PackageSolutionLikeNode(
            shape = shape(infraScalar(1.2)),
            materialItems = listOf(
                PackageSolutionLikeMaterialItem(
                    material = material.key,
                    quantity = PackageSolutionLikeQuantity.Weight(infraScalar(4.0) * Kilogram)
                )
            )
        )
        val parent = PackageSolutionLikeNode(
            shape = shape(infraScalar(2.0)),
            materialItems = listOf(
                PackageSolutionLikeMaterialItem(
                    material = material.key,
                    quantity = PackageSolutionLikeQuantity.AmountAndWeight(
                        amount = UInt64.one,
                        weight = infraScalar(1.0) * Kilogram
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
            unitWeightKg = InfraNumber.one
        )
        val node = PackageSolutionLikeNode(
            shape = shape(InfraNumber.one),
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
