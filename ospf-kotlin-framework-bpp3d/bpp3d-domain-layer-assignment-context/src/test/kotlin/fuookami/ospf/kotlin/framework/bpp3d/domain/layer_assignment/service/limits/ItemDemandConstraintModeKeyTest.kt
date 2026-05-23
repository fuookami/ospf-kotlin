package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BPP3DShadowPriceArguments
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BPP3DShadowPriceMap
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Package
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dDemandEntry
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.DefaultBpp3dSolverValueAdapter
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Load
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ItemDemandConstraintModeKeyTest {
    private val cargo = object : AbstractCargoAttribute {}

    private fun fixedRange(value: Flt64): ValueRange<Flt64> {
        return ValueRange(value, value, Interval.Closed, Interval.Closed, Flt64).value!!
    }

    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(Flt64.zero),
            hangingPolicy = AbsoluteHangingPolicy(Flt64.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    @Test
    fun extractorShouldSeparateShadowPriceByModeAndKey() {
        val material = Material(
            no = MaterialNo("A"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "A",
            weight = 5.0 * Kilogram
        )
        val item = ActualItem(
            id = "item-1",
            name = "item-1",
            pack = Package.innerPackage(
                shape = PackageShape(
                    width = 1.0 * Meter,
                    height = 1.0 * Meter,
                    depth = 1.0 * Meter,
                    weight = 0.1 * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64(2))
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B1"),
            packageAttribute = defaultPackageAttribute()
        )

        val amountKey = ItemDemandShadowPriceKey(Bpp3dDemandMode.ItemAmount, Bpp3dDemandKey.Item(item))
        val materialAmountKey = ItemDemandShadowPriceKey(Bpp3dDemandMode.ItemMaterialAmount, Bpp3dDemandKey.Material(material.key))
        val materialWeightKey = ItemDemandShadowPriceKey(Bpp3dDemandMode.ItemMaterialWeight, Bpp3dDemandKey.Material(material.key))

        assertNotEquals(amountKey, materialAmountKey)
        assertNotEquals(materialAmountKey, materialWeightKey)
        assertNotEquals(amountKey, materialWeightKey)

        val load = object : Load {
            override val demandEntries: List<Bpp3dDemandEntry> = listOf(
                Bpp3dDemandEntry(
                    mode = Bpp3dDemandMode.ItemAmount,
                    key = Bpp3dDemandKey.Item(item),
                    demand = Flt64.one,
                    demandRange = fixedRange(Flt64.one)
                ),
                Bpp3dDemandEntry(
                    mode = Bpp3dDemandMode.ItemMaterialAmount,
                    key = Bpp3dDemandKey.Material(material.key),
                    demand = Flt64(2.0),
                    demandRange = fixedRange(Flt64(2.0))
                ),
                Bpp3dDemandEntry(
                    mode = Bpp3dDemandMode.ItemMaterialWeight,
                    key = Bpp3dDemandKey.Material(material.key),
                    demand = Flt64(10.0),
                    demandRange = fixedRange(Flt64(10.0))
                )
            )
            override val demandValueAdapter = DefaultBpp3dSolverValueAdapter
            override val load: LinearIntermediateSymbols1<Flt64>
                get() = error("not used in this test")
            override val overLoad: LinearIntermediateSymbols1<Flt64>
                get() = error("not used in this test")
            override val lessLoad: LinearIntermediateSymbols1<Flt64>
                get() = error("not used in this test")
            override val overEnabled: Boolean = true
            override val lessEnabled: Boolean = true
        }

        val constraint = ItemDemandConstraint<BPP3DShadowPriceArguments, Item>(load)
        val extractor = constraint.extractor()!!

        val map = BPP3DShadowPriceMap()
        map[amountKey] = ShadowPrice(amountKey, Flt64(2.0))
        map[materialAmountKey] = ShadowPrice(materialAmountKey, Flt64(3.0))
        map[materialWeightKey] = ShadowPrice(materialWeightKey, Flt64(4.0))

        val reducedCost = extractor(map, BPP3DShadowPriceArguments(item))

        assertEquals(48.0, reducedCost.toDouble(), 1e-10)
    }
}
