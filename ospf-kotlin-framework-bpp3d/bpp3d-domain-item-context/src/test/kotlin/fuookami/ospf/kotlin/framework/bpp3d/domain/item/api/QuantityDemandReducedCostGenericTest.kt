package fuookami.ospf.kotlin.framework.bpp3d.domain.item.api

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class QuantityDemandReducedCostGenericTest {
    private val cargo = object : AbstractCargoAttribute {}

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
    fun itemReducedCostShouldUseActiveMaterialDemandEntriesOnly() {
        val material = Material(
            no = MaterialNo("M-RC-G64"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-RC-G64",
            weight = 0.5 * Kilogram
        )
        val item = Item(
            id = "item-rc-g64",
            name = "item-rc-g64",
            pack = Package.innerPackage(
                shape = PackageShape(
                    width = 1.0 * Meter,
                    height = 1.0 * Meter,
                    depth = 1.0 * Meter,
                    weight = 0.2 * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64(3))
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-RC-G64"),
            packageAttribute = defaultPackageAttribute()
        )
        val materialKey = item.statistics(Bpp3dDemandMode.ItemMaterialAmount).keys.single()

        val reducedCost = item.reducedCost(
            demandEntries = listOf(Pair(Bpp3dDemandMode.ItemMaterialAmount, materialKey)),
            shadowPriceOf = { _, _ -> Flt64(2.0) },
            amountToScalar = { amount -> Flt64(amount.toULong().toDouble()) },
            zero = Flt64.zero
        )

        assertEquals(6.0, reducedCost.toDouble(), 1e-10)
    }

    @Test
    fun layerReducedCostShouldSupportFltXDemandValues() {
        val material = Material(
            no = MaterialNo("M-RC-GX"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-RC-GX",
            weight = FltX(0.5) * Kilogram
        )
        val item = Item(
            id = "item-rc-gx",
            name = "item-rc-gx",
            pack = Package.innerPackage(
                shape = PackageShape(
                    width = FltX.one * Meter,
                    height = FltX.one * Meter,
                    depth = FltX.one * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64(2))
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-RC-GX"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer = BinLayer(
            iteration = Int64.zero,
            from = QuantityDemandReducedCostGenericTest::class,
            width = FltX(10.0) * Meter,
            height = FltX(10.0) * Meter,
            depth = FltX(10.0) * Meter,
            units = listOf(
                ItemPlacement(item, FltX.zero * Meter, FltX.zero * Meter, FltX.zero * Meter),
                ItemPlacement(item, FltX.one * Meter, FltX.zero * Meter, FltX.zero * Meter)
            )
        )
        val materialKey = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight).keys.single()

        val reducedCost = layer.reducedCost(
            demandEntries = listOf(Pair(Bpp3dDemandMode.ItemMaterialWeight, materialKey)),
            shadowPriceOf = { _, _ -> FltX(1.5) },
            amountToScalar = { amount -> FltX(amount.toULong().toDouble()) },
            zero = FltX.zero
        )

        assertEquals(3.0, reducedCost.toDouble(), 1e-10)
    }

    @Test
    fun reducedCostShouldSupportGenericShadowPriceKeyMap() {
        val material = Material(
            no = MaterialNo("M-RC-MAP"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-RC-MAP",
            weight = 0.5 * Kilogram
        )
        val item = Item(
            id = "item-rc-map",
            name = "item-rc-map",
            pack = Package.innerPackage(
                shape = PackageShape(
                    width = 1.0 * Meter,
                    height = 1.0 * Meter,
                    depth = 1.0 * Meter,
                    weight = 0.2 * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64(2))
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-RC-MAP"),
            packageAttribute = defaultPackageAttribute()
        )
        val materialKey = item.statistics(Bpp3dDemandMode.ItemMaterialAmount).keys.single()
        val demandKey = GenericDemandShadowPriceKey(
            mode = Bpp3dDemandMode.ItemMaterialAmount,
            key = materialKey
        )
        val shadowPrices = mapOf(demandKey to Flt64(3.0))

        val reducedCost = item.reducedCost(
            demandEntries = listOf(demandKey),
            shadowPrices = shadowPrices,
            amountToScalar = { amount -> Flt64(amount.toULong().toDouble()) },
            zero = Flt64.zero
        )

        assertEquals(6.0, reducedCost.toDouble(), 1e-10)
    }
}
