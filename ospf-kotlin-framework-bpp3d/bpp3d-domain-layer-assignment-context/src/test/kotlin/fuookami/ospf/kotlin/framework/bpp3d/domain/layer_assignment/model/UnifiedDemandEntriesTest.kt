package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.QuantityUnit
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UnifiedDemandEntriesTest {
    private object CargoAttr : AbstractCargoAttribute

    private object DiscreteCountUnit : PhysicalUnit() {
        @Suppress("unused")
        fun getDomain(): String = "Discrete"

        override val name = "count"
        override val symbol = "cnt"
        override val quantity = QuantityUnit(name = "count", symbol = "cnt").quantity
        override val scale = Scale()
    }

    private fun packageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(Flt64.zero),
            hangingPolicy = AbsoluteHangingPolicy(Flt64.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(id: String): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            width = Flt64.one * Meter,
            height = Flt64.one * Meter,
            depth = Flt64.one * Meter,
            weight = Flt64.one * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute()
        )
    }

    private fun material(no: String): Material {
        return Material(
            no = MaterialNo(no),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = no,
            weight = Flt64.one * Kilogram
        )
    }

    @Test
    fun itemDemandEntriesShouldChooseModeByUnitDomain() {
        val item = item("item-demand-domain")
        val amountEntries = demandEntriesFromItemDemands(
            items = listOf(
                Pair(item, Quantity(Flt64(3), DiscreteCountUnit))
            )
        )
        val weightEntries = demandEntriesFromItemDemands(
            items = listOf(
                Pair(item, Quantity(Flt64(2), Kilogram))
            )
        )

        assertEquals(Bpp3dDemandMode.Item::class, amountEntries.single().mode::class)
        assertEquals(Bpp3dDemandDomain.Discrete, amountEntries.single().quantityDomain)
        assertEquals(Flt64(3), amountEntries.single().demand)

        assertEquals(Bpp3dDemandMode.Item::class, weightEntries.single().mode::class)
        assertEquals(Bpp3dDemandDomain.Continuous, weightEntries.single().quantityDomain)
        assertEquals(Flt64(2), weightEntries.single().demand)
    }

    @Test
    fun materialDemandEntriesShouldChooseModeByUnitDomain() {
        val material = material("material-demand-domain")
        val amountEntries = demandEntriesFromMaterialDemands(
            materials = listOf(
                Pair(material, Quantity(Flt64(4), DiscreteCountUnit))
            )
        )
        val weightEntries = demandEntriesFromMaterialDemands(
            materials = listOf(
                Pair(material, Quantity(Flt64(5), Kilogram))
            )
        )

        assertEquals(Bpp3dDemandMode.Material::class, amountEntries.single().mode::class)
        assertEquals(Bpp3dDemandDomain.Discrete, amountEntries.single().quantityDomain)
        assertEquals(Flt64(4), amountEntries.single().demand)

        assertEquals(Bpp3dDemandMode.Material::class, weightEntries.single().mode::class)
        assertEquals(Bpp3dDemandDomain.Continuous, weightEntries.single().quantityDomain)
        assertEquals(Flt64(5), weightEntries.single().demand)
    }
}
