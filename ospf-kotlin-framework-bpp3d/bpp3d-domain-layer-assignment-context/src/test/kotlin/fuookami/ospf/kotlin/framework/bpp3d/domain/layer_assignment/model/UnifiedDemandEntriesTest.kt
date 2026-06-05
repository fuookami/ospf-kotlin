package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
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
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.QuantityUnit
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.UnitConversionRule
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UnifiedDemandEntriesTest {
    private object CargoAttr : AbstractCargoAttribute

    private object DiscreteCountUnit : PhysicalUnit() {
        @Suppress("unused")
        fun getDomain(): String = "Discrete"

        override val name = "count"
        override val symbol = "cnt"
        override val quantity = QuantityUnit(name = "count", symbol = "cnt").quantity
        override val conversionRule = UnitConversionRule.Linear(Scale())
    }

    private fun packageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(InfraNumber.zero),
            hangingPolicy = AbsoluteHangingPolicy(InfraNumber.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(id: String): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            width = InfraNumber.one * Meter,
            height = InfraNumber.one * Meter,
            depth = InfraNumber.one * Meter,
            weight = InfraNumber.one * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute()
        )
    }

    private fun material(no: String): Material<InfraNumber> {
        return Material(
            no = MaterialNo(no),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = no,
            weight = InfraNumber.one * Kilogram
        )
    }

    @Test
    fun itemDemandEntriesShouldChooseModeByUnitDomain() {
        val item = item("item-demand-domain")
        val amountEntries = demandEntriesFromItemDemands(
            items = listOf(
                Pair(item, Quantity(InfraNumber(3), DiscreteCountUnit))
            )
        )
        val weightEntries = demandEntriesFromItemDemands(
            items = listOf(
                Pair(item, Quantity(InfraNumber(2), Kilogram))
            )
        )

        assertEquals(Bpp3dDemandMode.Item::class, amountEntries.single().mode::class)
        assertEquals(Bpp3dDemandDomain.Discrete, amountEntries.single().quantityDomain)
        assertEquals(3.0, amountEntries.single().demand.toDouble(), 1e-10)

        assertEquals(Bpp3dDemandMode.Item::class, weightEntries.single().mode::class)
        assertEquals(Bpp3dDemandDomain.Continuous, weightEntries.single().quantityDomain)
        assertEquals(2.0, weightEntries.single().demand.toDouble(), 1e-10)
    }

    @Test
    fun materialDemandEntriesShouldChooseModeByUnitDomain() {
        val material = material("material-demand-domain")
        val amountEntries = demandEntriesFromMaterialDemands(
            materials = listOf(
                Pair(material, Quantity(InfraNumber(4), DiscreteCountUnit))
            )
        )
        val weightEntries = demandEntriesFromMaterialDemands(
            materials = listOf(
                Pair(material, Quantity(InfraNumber(5), Kilogram))
            )
        )

        assertEquals(Bpp3dDemandMode.Material::class, amountEntries.single().mode::class)
        assertEquals(Bpp3dDemandDomain.Discrete, amountEntries.single().quantityDomain)
        assertEquals(4.0, amountEntries.single().demand.toDouble(), 1e-10)

        assertEquals(Bpp3dDemandMode.Material::class, weightEntries.single().mode::class)
        assertEquals(Bpp3dDemandDomain.Continuous, weightEntries.single().quantityDomain)
        assertEquals(5.0, weightEntries.single().demand.toDouble(), 1e-10)
    }

    @Test
    fun labeledDemandEntriesShouldKeepDemandMode() {
        val item = item("item-labeled-mode")
        val material = material("material-labeled-mode")
        val itemEntries = demandEntriesFromLabeledItemDemands(
            items = listOf(
                Bpp3dItemDemand(
                    item = item,
                    quantity = Quantity(InfraNumber(6), DiscreteCountUnit),
                    mode = Bpp3dDemandMode.ItemAmount
                )
            )
        )
        val materialEntries = demandEntriesFromLabeledMaterialDemands(
            materials = listOf(
                Bpp3dMaterialDemand(
                    material = material.key,
                    quantity = Quantity(InfraNumber(7), Kilogram),
                    mode = Bpp3dDemandMode.ItemMaterialWeight
                )
            )
        )

        assertEquals(Bpp3dDemandMode.ItemAmount::class, itemEntries.single().mode::class)
        assertEquals(Bpp3dDemandMode.ItemMaterialWeight::class, materialEntries.single().mode::class)
    }

    @Test
    fun labeledDemandEntriesShouldRejectIncompatibleMode() {
        val item = item("item-invalid-mode")
        val material = material("material-invalid-mode")

        assertFailsWith<IllegalArgumentException> {
            demandEntriesFromLabeledItemDemands(
                items = listOf(
                    Bpp3dItemDemand(
                        item = item,
                        quantity = Quantity(InfraNumber.one, DiscreteCountUnit),
                        mode = Bpp3dDemandMode.Material
                    )
                )
            )
        }

        assertFailsWith<IllegalArgumentException> {
            demandEntriesFromLabeledMaterialDemands(
                materials = listOf(
                    Bpp3dMaterialDemand(
                        material = material.key,
                        quantity = Quantity(InfraNumber.one, DiscreteCountUnit),
                        mode = Bpp3dDemandMode.Item
                    )
                )
            )
        }
    }

    @Test
    fun quantityDomainShouldDependOnUnitOnly() {
        val item = item("item-domain-only")
        val amountByMode = Bpp3dDemandEntry(
            mode = Bpp3dDemandMode.ItemAmount,
            key = Bpp3dDemandKey.Item(item),
            demand = InfraNumber.one,
            demandRange = ValueRange(
                InfraNumber.one,
                InfraNumber.one,
                Interval.Closed,
                Interval.Closed,
                InfraNumber
            ).value!!,
            quantityUnit = Kilogram
        )
        val weightByMode = Bpp3dDemandEntry(
            mode = Bpp3dDemandMode.ItemWeight,
            key = Bpp3dDemandKey.Item(item),
            demand = InfraNumber.one,
            demandRange = ValueRange(
                InfraNumber.one,
                InfraNumber.one,
                Interval.Closed,
                Interval.Closed,
                InfraNumber
            ).value!!,
            quantityUnit = DiscreteCountUnit
        )

        assertEquals(Bpp3dDemandDomain.Continuous, amountByMode.quantityDomain)
        assertEquals(Bpp3dDemandDomain.Discrete, weightByMode.quantityDomain)
    }
}



