package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackingProgram
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackingProgramMaterialValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingProgramCandidate
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackingProgramLayerCandidateAdapterTest {
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

    private fun shape(): PackageShape {
        return PackageShape(
            width = 1.0 * Meter,
            height = 1.0 * Meter,
            depth = 1.0 * Meter,
            weight = 1.0 * Kilogram,
            packageType = PackageType.CartonContainer
        )
    }

    @Test
    fun amountOnlyShouldDeriveWeightWhenCatalogProvided() {
        val material = material(
            no = "M-LAYER-AMOUNT",
            unitWeightKg = Flt64(2.0)
        )
        val candidate = MaterialPackingProgramCandidate(
            id = "candidate-amount",
            program = PackingProgram.innerPackage(
                shape = shape(),
                materials = mapOf(material.key to UInt64(3))
            )
        )

        val item = candidate.toLayerGenerationItem(
            sequence = 1,
            materialCatalog = mapOf(material.key to material)
        )

        assertEquals(UInt64(3), item.materialAmounts[material.key])
        assertEquals(Flt64(6.0), item.materialWeights[material.key]?.value)
    }

    @Test
    fun weightOnlyShouldNotBackfillAmount() {
        val material = material(
            no = "M-LAYER-WEIGHT",
            unitWeightKg = Flt64(2.0)
        )
        val candidate = MaterialPackingProgramCandidate(
            id = "candidate-weight",
            program = PackingProgram.innerPackageWithMaterialValues(
                shape = shape(),
                materials = mapOf(
                    material.key to PackingProgramMaterialValue(
                        weight = 5.0 * Kilogram
                    )
                )
            )
        )

        val item = candidate.toLayerGenerationItem(sequence = 1)
        assertTrue(item.materialAmounts.isEmpty())
        assertEquals(Flt64(5.0), item.materialWeights[material.key]?.value)
    }

    @Test
    fun amountAndWeightShouldKeepExplicitWeight() {
        val material = material(
            no = "M-LAYER-BOTH",
            unitWeightKg = Flt64(2.0)
        )
        val candidate = MaterialPackingProgramCandidate(
            id = "candidate-both",
            program = PackingProgram.innerPackageWithMaterialValues(
                shape = shape(),
                materials = mapOf(
                    material.key to PackingProgramMaterialValue(
                        amount = UInt64(3),
                        weight = 9.0 * Kilogram
                    )
                )
            )
        )

        val item = candidate.toLayerGenerationItem(
            sequence = 1,
            materialCatalog = mapOf(material.key to material)
        )
        assertEquals(UInt64(3), item.materialAmounts[material.key])
        assertEquals(Flt64(9.0), item.materialWeights[material.key]?.value)
    }
}
