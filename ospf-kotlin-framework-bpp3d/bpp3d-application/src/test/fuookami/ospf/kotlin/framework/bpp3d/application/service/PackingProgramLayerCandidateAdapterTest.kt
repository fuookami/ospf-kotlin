/**
 * 装箱方案层候选适配器测试。
 * Packing program layer candidate adapter test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.toLayerGenerationItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingProgramCandidate

class PackingProgramLayerCandidateAdapterTest {
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

    private fun shape(): PackageShape<FltX> {
        return PackageShape(
            width = FltX(1.0) * Meter,
            height = FltX(1.0) * Meter,
            depth = FltX(1.0) * Meter,
            weight = FltX(1.0) * Kilogram,
            packageType = PackageType.CartonContainer
        )
    }

    @Test
    fun amountOnlyShouldDeriveWeightWhenCatalogProvided() {
        val material = material(
            no = "M-LAYER-AMOUNT",
            unitWeightKg = FltX(2.0)
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
        val derivedWeight = item.materialWeights[material.key]?.value?.toDouble()
        assertNotNull(derivedWeight)
        assertEquals(6.0, derivedWeight, 1e-10)
    }

    @Test
    fun weightOnlyShouldNotBackfillAmount() {
        val material = material(
            no = "M-LAYER-WEIGHT",
            unitWeightKg = FltX(2.0)
        )
        val candidate = MaterialPackingProgramCandidate(
            id = "candidate-weight",
            program = PackingProgram.innerPackageWithMaterialValues(
                shape = shape(),
                materials = mapOf(
                    material.key to PackingProgramMaterialValue(
                        weight = FltX(5.0) * Kilogram
                    )
                )
            )
        )

        val item = candidate.toLayerGenerationItem(sequence = 1)
        assertTrue(item.materialAmounts.isEmpty())
        assertEquals(FltX(5.0), item.materialWeights[material.key]?.value)
    }

    @Test
    fun amountAndWeightShouldKeepExplicitWeight() {
        val material = material(
            no = "M-LAYER-BOTH",
            unitWeightKg = FltX(2.0)
        )
        val candidate = MaterialPackingProgramCandidate(
            id = "candidate-both",
            program = PackingProgram.innerPackageWithMaterialValues(
                shape = shape(),
                materials = mapOf(
                    material.key to PackingProgramMaterialValue(
                        amount = UInt64(3),
                        weight = FltX(9.0) * Kilogram
                    )
                )
            )
        )

        val item = candidate.toLayerGenerationItem(
            sequence = 1,
            materialCatalog = mapOf(material.key to material)
        )
        assertEquals(UInt64(3), item.materialAmounts[material.key])
        assertEquals(FltX(9.0), item.materialWeights[material.key]?.value)
    }
}
