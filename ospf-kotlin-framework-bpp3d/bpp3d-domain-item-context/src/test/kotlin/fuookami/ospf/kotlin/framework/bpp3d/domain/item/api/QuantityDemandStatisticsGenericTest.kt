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

    private fun assertKilogramQuantity(value: Quantity<*>, expected: InfraNumber) {
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
            deformationAttribute = LinearDeformationAttribute(InfraNumber.zero),
            hangingPolicy = AbsoluteHangingPolicy(InfraNumber.zero),
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
            weight = infraScalar(0.5) * Kilogram
        )
        val item = Item(
            id = "item-64",
            name = "item-64",
            pack = Package.innerPackage(
                shape = PackageShape(
                    width = infraScalar(1.0) * Meter,
                    height = infraScalar(1.0) * Meter,
                    depth = infraScalar(1.0) * Meter,
                    weight = infraScalar(0.2) * Kilogram,
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
            width = infraScalar(10.0) * Meter,
            height = infraScalar(10.0) * Meter,
            depth = infraScalar(10.0) * Meter,
            units = listOf(
                ItemPlacement(item, infraScalar(0.0) * Meter, infraScalar(0.0) * Meter, infraScalar(0.0) * Meter),
                ItemPlacement(item, infraScalar(1.0) * Meter, infraScalar(0.0) * Meter, infraScalar(0.0) * Meter)
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
        assertKilogramQuantity(scaledMaterialWeight.value, InfraNumber(3.0))

        val layerMaterialWeight = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight).values.single()
        assertTrue(layerMaterialWeight is GenericBpp3dDemandValue.Weight<*>)
        assertKilogramQuantity(layerMaterialWeight.value, InfraNumber(2.0))
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
        assertKilogramQuantity(scaledMaterialWeight.value, InfraNumber(4.0))

        val layerMaterialWeight = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight).values.single()
        assertTrue(layerMaterialWeight is GenericBpp3dDemandValue.Weight<*>)
        assertKilogramQuantity(layerMaterialWeight.value, InfraNumber(2.0))
    }

    @Test
    fun flt64AliasPathShouldRemainLegacyCompatible() {
        val material: InfraNumberMaterial = Material(
            no = MaterialNo("M-A"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-A",
            weight = infraScalar(0.4) * Kilogram
        )
        val item: InfraNumberItem = Item(
            id = "item-a",
            name = "item-a",
            pack = Package.innerPackage(
                shape = PackageShape(
                    width = infraScalar(1.0) * Meter,
                    height = infraScalar(1.0) * Meter,
                    depth = infraScalar(1.0) * Meter,
                    weight = infraScalar(0.1) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-A"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer: InfraNumberBinLayer = BinLayer(
            iteration = Int64.zero,
            from = QuantityDemandStatisticsGenericTest::class,
            width = infraScalar(2.0) * Meter,
            height = infraScalar(2.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            units = listOf(ItemPlacement(item, infraScalar(0.0) * Meter, infraScalar(0.0) * Meter, infraScalar(0.0) * Meter))
        )

        val modelLayer = layer.toModel()
        assertEquals(1, modelLayer.units.size)
        assertTrue(modelLayer.shape.width eq (infraScalar(2.0) * Meter))
        assertTrue(modelLayer.units.first().view.unit.weight eq (infraScalar(0.1) * Kilogram))
    }
}


