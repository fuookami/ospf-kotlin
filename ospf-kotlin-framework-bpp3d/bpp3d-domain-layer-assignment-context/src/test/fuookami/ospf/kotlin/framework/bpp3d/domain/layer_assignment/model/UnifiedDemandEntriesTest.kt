/**
 * 统一需求条目测试。
 * Unified demand entries test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

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
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(id: String): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            width = FltX.one * Meter,
            height = FltX.one * Meter,
            depth = FltX.one * Meter,
            weight = FltX.one * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute()
        )
    }

    private fun material(no: String): Material<FltX> {
        return Material(
            no = MaterialNo(no),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = no,
            weight = FltX.one * Kilogram
        )
    }

    @Test
    fun itemDemandEntriesShouldChooseModeByUnitDomain() {
        val item = item("item-demand-domain")
        val amountEntries = demandEntriesFromItemDemands(
            items = listOf(
                Pair(item, Quantity(FltX(3), DiscreteCountUnit))
            )
        )
        val weightEntries = demandEntriesFromItemDemands(
            items = listOf(
                Pair(item, Quantity(FltX(2), Kilogram))
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
                Pair(material, Quantity(FltX(4), DiscreteCountUnit))
            )
        )
        val weightEntries = demandEntriesFromMaterialDemands(
            materials = listOf(
                Pair(material, Quantity(FltX(5), Kilogram))
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
                    quantity = Quantity(FltX(6), DiscreteCountUnit),
                    mode = Bpp3dDemandMode.ItemAmount
                )
            )
        )
        val materialEntries = demandEntriesFromLabeledMaterialDemands(
            materials = listOf(
                Bpp3dMaterialDemand(
                    material = material.key,
                    quantity = Quantity(FltX(7), Kilogram),
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
                        quantity = Quantity(FltX.one, DiscreteCountUnit),
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
                        quantity = Quantity(FltX.one, DiscreteCountUnit),
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
            demand = FltX.one,
            demandRange = ValueRange(
                FltX.one,
                FltX.one,
                Interval.Closed,
                Interval.Closed,
                FltX
            ).value!!,
            quantityUnit = Kilogram
        )
        val weightByMode = Bpp3dDemandEntry(
            mode = Bpp3dDemandMode.ItemWeight,
            key = Bpp3dDemandKey.Item(item),
            demand = FltX.one,
            demandRange = ValueRange(
                FltX.one,
                FltX.one,
                Interval.Closed,
                Interval.Closed,
                FltX
            ).value!!,
            quantityUnit = DiscreteCountUnit
        )

        assertEquals(Bpp3dDemandDomain.Continuous, amountByMode.quantityDomain)
        assertEquals(Bpp3dDemandDomain.Discrete, weightByMode.quantityDomain)
    }
}
