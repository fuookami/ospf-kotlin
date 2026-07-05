/**
 * 物理量需求统计测试。
 * Quantity demand statistics test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

class QuantityDemandStatisticsTest {
    private val cargo = object : AbstractCargoAttribute {}

    private fun assertKilogramQuantity(value: Quantity<*>, expected: FltX) {
        assertEquals(Kilogram, value.unit)
        val actual = when (val scalar = value.value) {
            is FltX -> scalar.toDouble()
            is Number -> scalar.toDouble()
            else -> error("Unsupported scalar type: $scalar")
        }
        assertEquals(expected.toDouble(), actual, 1e-10)
    }

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
    fun flt64StatisticsShouldCoverAmountMaterialAmountAndMaterialWeightModes() {
        val material = QuantityMaterial(
            no = MaterialNo("M-64"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-64",
            weight = FltX(0.5) * Kilogram
        )
        val item = QuantityItem(
            id = itemIdOf("item-64"),
            name = "item-64",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = FltX(1.0) * Meter,
                    height = FltX(1.0) * Meter,
                    depth = FltX(1.0) * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64(2))
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-64"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer = QuantityBinLayer(
            iteration = Int64.zero,
            from = QuantityDemandStatisticsTest::class,
            width = FltX(10.0) * Meter,
            height = FltX(10.0) * Meter,
            depth = FltX(10.0) * Meter,
            units = listOf(
                QuantityItemPlacement(item, FltX(0.0) * Meter, FltX(0.0) * Meter, FltX(0.0) * Meter),
                QuantityItemPlacement(item, FltX(1.0) * Meter, FltX(0.0) * Meter, FltX(0.0) * Meter)
            )
        )

        val scaledItemAmount = item.statistics(Bpp3dDemandMode.ItemAmount, UInt64(3)).values.single()
        assertTrue(scaledItemAmount is QuantityBpp3dDemandValue.Amount<*>)
        assertEquals(UInt64(3), scaledItemAmount.value)

        val scaledMaterialAmount = item.statistics(Bpp3dDemandMode.ItemMaterialAmount, UInt64(3)).values.single()
        assertTrue(scaledMaterialAmount is QuantityBpp3dDemandValue.Amount<*>)
        assertEquals(UInt64(6), scaledMaterialAmount.value)

        val scaledMaterialWeight = item.statistics(Bpp3dDemandMode.ItemMaterialWeight, UInt64(3)).values.single()
        assertTrue(scaledMaterialWeight is QuantityBpp3dDemandValue.Weight<*>)
        assertKilogramQuantity(scaledMaterialWeight.value, FltX(3.0))

        val layerMaterialWeight = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight).values.single()
        assertTrue(layerMaterialWeight is QuantityBpp3dDemandValue.Weight<*>)
        assertKilogramQuantity(layerMaterialWeight.value, FltX(2.0))
    }

    @Test
    fun fltXStatisticsShouldCoverAmountMaterialAmountAndMaterialWeightModes() {
        val material = QuantityMaterial(
            no = MaterialNo("M-X"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-X",
            weight = FltX(0.5) * Kilogram
        )
        val item = QuantityItem(
            id = itemIdOf("item-x"),
            name = "item-x",
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
            batchNo = BatchNo("B-X"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer = QuantityBinLayer(
            iteration = Int64.zero,
            from = QuantityDemandStatisticsTest::class,
            width = FltX(10.0) * Meter,
            height = FltX(10.0) * Meter,
            depth = FltX(10.0) * Meter,
            units = listOf(
                QuantityItemPlacement(item, FltX.zero * Meter, FltX.zero * Meter, FltX.zero * Meter),
                QuantityItemPlacement(item, FltX.one * Meter, FltX.zero * Meter, FltX.zero * Meter)
            )
        )

        val scaledItemAmount = item.statistics(Bpp3dDemandMode.ItemAmount, UInt64(4)).values.single()
        assertTrue(scaledItemAmount is QuantityBpp3dDemandValue.Amount<*>)
        assertEquals(UInt64(4), scaledItemAmount.value)

        val scaledMaterialAmount = item.statistics(Bpp3dDemandMode.ItemMaterialAmount, UInt64(4)).values.single()
        assertTrue(scaledMaterialAmount is QuantityBpp3dDemandValue.Amount<*>)
        assertEquals(UInt64(8), scaledMaterialAmount.value)

        val scaledMaterialWeight = item.statistics(Bpp3dDemandMode.ItemMaterialWeight, UInt64(4)).values.single()
        assertTrue(scaledMaterialWeight is QuantityBpp3dDemandValue.Weight<*>)
        assertKilogramQuantity(scaledMaterialWeight.value, FltX(4.0))

        val layerMaterialWeight = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight).values.single()
        assertTrue(layerMaterialWeight is QuantityBpp3dDemandValue.Weight<*>)
        assertKilogramQuantity(layerMaterialWeight.value, FltX(2.0))
    }
}
