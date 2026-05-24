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
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.eq
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuantityDemandStatisticsGenericTest {
    private val cargo = object : AbstractCargoAttribute {}

    private fun assertKilogramQuantity(value: Quantity<*>, expected: Double) {
        assertEquals(Kilogram, value.unit)
        val actual = when (val scalar = value.value) {
            is Flt64 -> scalar.toDouble()
            is FltX -> scalar.toDouble()
            else -> error("Unsupported scalar type: $scalar")
        }
        assertEquals(expected, actual, 1e-10)
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
    fun flt64StatisticsShouldCoverAmountMaterialAmountAndMaterialWeightModes() {
        val material = Material(
            no = MaterialNo("M-64"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-64",
            weight = 0.5 * Kilogram
        )
        val item = Item(
            id = "item-64",
            name = "item-64",
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
            batchNo = BatchNo("B-64"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer = BinLayer(
            iteration = Int64.zero,
            from = QuantityDemandStatisticsGenericTest::class,
            width = 10.0 * Meter,
            height = 10.0 * Meter,
            depth = 10.0 * Meter,
            units = listOf(
                ItemPlacement(item, 0.0 * Meter, 0.0 * Meter, 0.0 * Meter),
                ItemPlacement(item, 1.0 * Meter, 0.0 * Meter, 0.0 * Meter)
            )
        )

        val scaledItemAmount = item.statistics(Bpp3dDemandMode.ItemAmount, UInt64(3)).values.single()
        assertTrue(scaledItemAmount is GenericBpp3dDemandValue.Amount<*>)
        assertEquals(UInt64(3), scaledItemAmount.value)

        val scaledMaterialAmount = item.statistics(Bpp3dDemandMode.ItemMaterialAmount, UInt64(3)).values.single()
        assertTrue(scaledMaterialAmount is GenericBpp3dDemandValue.Amount<*>)
        assertEquals(UInt64(6), scaledMaterialAmount.value)

        val scaledMaterialWeight = item.statistics(Bpp3dDemandMode.ItemMaterialWeight, UInt64(3)).values.single()
        assertTrue(scaledMaterialWeight is GenericBpp3dDemandValue.Weight<*>)
        assertKilogramQuantity(scaledMaterialWeight.value, 3.0)

        val layerMaterialWeight = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight).values.single()
        assertTrue(layerMaterialWeight is GenericBpp3dDemandValue.Weight<*>)
        assertKilogramQuantity(layerMaterialWeight.value, 2.0)
    }

    @Test
    fun fltXStatisticsShouldCoverAmountMaterialAmountAndMaterialWeightModes() {
        val material = Material(
            no = MaterialNo("M-X"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-X",
            weight = FltX(0.5) * Kilogram
        )
        val item = Item(
            id = "item-x",
            name = "item-x",
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
            batchNo = BatchNo("B-X"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer = BinLayer(
            iteration = Int64.zero,
            from = QuantityDemandStatisticsGenericTest::class,
            width = FltX(10.0) * Meter,
            height = FltX(10.0) * Meter,
            depth = FltX(10.0) * Meter,
            units = listOf(
                ItemPlacement(item, FltX.zero * Meter, FltX.zero * Meter, FltX.zero * Meter),
                ItemPlacement(item, FltX.one * Meter, FltX.zero * Meter, FltX.zero * Meter)
            )
        )

        val scaledItemAmount = item.statistics(Bpp3dDemandMode.ItemAmount, UInt64(4)).values.single()
        assertTrue(scaledItemAmount is GenericBpp3dDemandValue.Amount<*>)
        assertEquals(UInt64(4), scaledItemAmount.value)

        val scaledMaterialAmount = item.statistics(Bpp3dDemandMode.ItemMaterialAmount, UInt64(4)).values.single()
        assertTrue(scaledMaterialAmount is GenericBpp3dDemandValue.Amount<*>)
        assertEquals(UInt64(8), scaledMaterialAmount.value)

        val scaledMaterialWeight = item.statistics(Bpp3dDemandMode.ItemMaterialWeight, UInt64(4)).values.single()
        assertTrue(scaledMaterialWeight is GenericBpp3dDemandValue.Weight<*>)
        assertKilogramQuantity(scaledMaterialWeight.value, 4.0)

        val layerMaterialWeight = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight).values.single()
        assertTrue(layerMaterialWeight is GenericBpp3dDemandValue.Weight<*>)
        assertKilogramQuantity(layerMaterialWeight.value, 2.0)
    }

    @Test
    fun flt64AliasPathShouldRemainLegacyCompatible() {
        val material: Flt64Material = Material(
            no = MaterialNo("M-A"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-A",
            weight = 0.4 * Kilogram
        )
        val item: Flt64Item = Item(
            id = "item-a",
            name = "item-a",
            pack = Package.innerPackage(
                shape = PackageShape(
                    width = 1.0 * Meter,
                    height = 1.0 * Meter,
                    depth = 1.0 * Meter,
                    weight = 0.1 * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-A"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer: Flt64BinLayer = BinLayer(
            iteration = Int64.zero,
            from = QuantityDemandStatisticsGenericTest::class,
            width = 2.0 * Meter,
            height = 2.0 * Meter,
            depth = 2.0 * Meter,
            units = listOf(ItemPlacement(item, 0.0 * Meter, 0.0 * Meter, 0.0 * Meter))
        )

        val legacyLayer = layer.toLegacy()
        assertEquals(1, legacyLayer.units.size)
        assertTrue(legacyLayer.shape.width eq (2.0 * Meter))
        assertTrue(legacyLayer.units.first().unit.weight eq (0.1 * Kilogram))
    }
}
