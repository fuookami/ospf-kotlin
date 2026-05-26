package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MaterialDemandEntriesTest {
    private val cargo = object : AbstractCargoAttribute {}

    @Test
    fun materialAmountDemandEntriesShouldNotCreateItemAmountMode() {
        val material = Material(
            no = MaterialNo("M-AMOUNT"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-AMOUNT",
            weight = 1.0 * Kilogram
        )

        val entries = demandEntriesFromMaterialAmounts(
            materials = listOf(Pair(material, UInt64(7)))
        )

        assertEquals(1, entries.size)
        assertEquals("ItemMaterialAmount", entries.single().mode::class.simpleName)
        assertEquals(7.0, entries.single().demand.toDouble(), 1e-10)
    }

    @Test
    fun materialWeightDemandEntriesShouldNotCreateItemAmountMode() {
        val material = Material(
            no = MaterialNo("M-WEIGHT"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-WEIGHT",
            weight = 1.0 * Kilogram
        )

        val entries = demandEntriesFromMaterialWeights(
            materials = listOf(Pair(material, 12.5 * Kilogram))
        )

        assertEquals(1, entries.size)
        assertEquals("ItemMaterialWeight", entries.single().mode::class.simpleName)
        assertEquals(12.5, entries.single().demand.toDouble(), 1e-10)
    }
}
