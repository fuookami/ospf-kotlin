/**
 * 物理量需求缩减成本测试。
 * Quantity demand reduced cost test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

class QuantityDemandReducedCostTest {
    private val cargo = object : AbstractCargoAttribute {}

    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    @Test
    fun itemReducedCostShouldUseActiveMaterialDemandEntriesOnly() {
        val material = QuantityMaterial(
            no = MaterialNo("M-RC-MODEL-G64"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-RC-MODEL-G64",
            weight = fltX(0.5) * Kilogram
        )
        val item = QuantityItem(
            id = "item-rc-model-g64",
            name = "item-rc-model-g64",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = fltX(1.0) * Meter,
                    height = fltX(1.0) * Meter,
                    depth = fltX(1.0) * Meter,
                    weight = fltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64(3))
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-RC-MODEL-G64"),
            packageAttribute = defaultPackageAttribute()
        )
        val materialKey = item.statistics(Bpp3dDemandMode.ItemMaterialAmount).keys.single()

        val reducedCost = item.reducedCost(
            demandEntries = listOf(Pair(Bpp3dDemandMode.ItemMaterialAmount, materialKey)),
            shadowPriceOf = { _, _ -> FltX(2.0) },
            amountToScalar = { amount -> FltX(amount.toULong().toDouble()) },
            zero = FltX.zero
        )

        assertEquals(6.0, reducedCost.toDouble(), 1e-10)
    }

    @Test
    fun layerReducedCostShouldSupportFltXDemandValues() {
        val material = QuantityMaterial(
            no = MaterialNo("M-RC-MODEL-GX"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-RC-MODEL-GX",
            weight = FltX(0.5) * Kilogram
        )
        val item = QuantityItem(
            id = "item-rc-model-gx",
            name = "item-rc-model-gx",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = FltX.one * Meter,
                    height = FltX.one * Meter,
                    depth = FltX.one * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64(2))
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-RC-MODEL-GX"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer = QuantityBinLayer(
            iteration = Int64.zero,
            from = QuantityDemandReducedCostTest::class,
            width = FltX(10.0) * Meter,
            height = FltX(10.0) * Meter,
            depth = FltX(10.0) * Meter,
            units = listOf(
                QuantityItemPlacement(item, FltX.zero * Meter, FltX.zero * Meter, FltX.zero * Meter),
                QuantityItemPlacement(item, FltX.one * Meter, FltX.zero * Meter, FltX.zero * Meter)
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
    fun reducedCostShouldSupportQuantityShadowPriceKeyMap() {
        val material = QuantityMaterial(
            no = MaterialNo("M-RC-MODEL-MAP"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-RC-MODEL-MAP",
            weight = fltX(0.5) * Kilogram
        )
        val item = QuantityItem(
            id = "item-rc-model-map",
            name = "item-rc-model-map",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = fltX(1.0) * Meter,
                    height = fltX(1.0) * Meter,
                    depth = fltX(1.0) * Meter,
                    weight = fltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64(2))
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-RC-MODEL-MAP"),
            packageAttribute = defaultPackageAttribute()
        )
        val materialKey = item.statistics(Bpp3dDemandMode.ItemMaterialAmount).keys.single()
        val demandKey = QuantityDemandShadowPriceKey(
            mode = Bpp3dDemandMode.ItemMaterialAmount,
            key = materialKey
        )
        val shadowPrices = mapOf(demandKey to FltX(3.0))

        val reducedCost = item.reducedCost(
            demandEntries = listOf(demandKey),
            shadowPrices = shadowPrices,
            amountToScalar = { amount -> FltX(amount.toULong().toDouble()) },
            zero = FltX.zero
        )

        assertEquals(6.0, reducedCost.toDouble(), 1e-10)
    }
}
