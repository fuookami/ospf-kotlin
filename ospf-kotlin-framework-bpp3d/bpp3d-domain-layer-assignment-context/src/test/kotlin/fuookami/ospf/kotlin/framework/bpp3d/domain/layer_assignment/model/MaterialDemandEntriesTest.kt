package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.UnitConversionRule
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MaterialDemandEntriesTest {
    private enum class MockDomain {
        Discrete,
        Continuous
    }

    private class DomainAwareMassUnit(
        private val marker: MockDomain
    ) : PhysicalUnit() {
        @Suppress("unused")
        fun getDomain(): String = marker.name

        override val name: String = "domain-aware-mass"
        override val symbol: String = "dam"
        override val quantity = Kilogram.quantity
        override val conversionRule = Kilogram.conversionRule
    }

    private val cargo = object : AbstractCargoAttribute {}

    @Test
    fun materialAmountDemandEntriesShouldNotCreateItemAmountMode() {
        val material = Material(
            no = MaterialNo("M-AMOUNT"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-AMOUNT",
            weight = infraScalar(1.0) * Kilogram
        )

        val entries = demandEntriesFromMaterialAmounts(
            materials = listOf(Pair(material, UInt64(7)))
        )

        assertEquals(1, entries.size)
        assertEquals("Material", entries.single().mode::class.simpleName)
        assertEquals(7.0, entries.single().demand.toDouble(), 1e-10)
        assertEquals(Bpp3dDemandDomain.Discrete, entries.single().quantityDomain)
    }

    @Test
    fun materialWeightDemandEntriesShouldNotCreateItemAmountMode() {
        val material = Material(
            no = MaterialNo("M-WEIGHT"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-WEIGHT",
            weight = infraScalar(1.0) * Kilogram
        )

        val entries = demandEntriesFromMaterialWeights(
            materials = listOf(Pair(material, infraScalar(12.5) * Kilogram))
        )

        assertEquals(1, entries.size)
        assertEquals("Material", entries.single().mode::class.simpleName)
        assertEquals(12.5, entries.single().demand.toDouble(), 1e-10)
        assertEquals(Bpp3dDemandDomain.Continuous, entries.single().quantityDomain)
    }

    @Test
    fun materialWeightDemandDomainShouldReadFromUnitInsteadOfDimension() {
        val material = Material(
            no = MaterialNo("M-DOMAIN"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-DOMAIN",
            weight = infraScalar(1.0) * Kilogram
        )
        val discreteMassUnit = DomainAwareMassUnit(MockDomain.Discrete)

        val entries = demandEntriesFromMaterialWeights(
            materials = listOf(Pair(material, infraScalar(5.0) * discreteMassUnit))
        )

        assertEquals(1, entries.size)
        assertEquals(Bpp3dDemandDomain.Discrete, entries.single().quantityDomain)
        assertEquals(discreteMassUnit, entries.single().quantityUnit)
    }
}


