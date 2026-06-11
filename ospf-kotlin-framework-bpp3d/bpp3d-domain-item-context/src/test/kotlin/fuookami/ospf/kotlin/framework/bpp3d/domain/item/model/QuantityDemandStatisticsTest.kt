package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
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

class QuantityDemandStatisticsTest {
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
        val material = QuantityMaterial(
            no = MaterialNo("M-64"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-64",
            weight = infraScalar(0.5) * Kilogram
        )
        val item = QuantityItem(
            id = "item-64",
            name = "item-64",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
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
        val layer = QuantityBinLayer(
            iteration = Int64.zero,
            from = QuantityDemandStatisticsTest::class,
            width = infraScalar(10.0) * Meter,
            height = infraScalar(10.0) * Meter,
            depth = infraScalar(10.0) * Meter,
            units = listOf(
                QuantityItemPlacement(item, infraScalar(0.0) * Meter, infraScalar(0.0) * Meter, infraScalar(0.0) * Meter),
                QuantityItemPlacement(item, infraScalar(1.0) * Meter, infraScalar(0.0) * Meter, infraScalar(0.0) * Meter)
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
        assertKilogramQuantity(scaledMaterialWeight.value, InfraNumber(3.0))

        val layerMaterialWeight = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight).values.single()
        assertTrue(layerMaterialWeight is QuantityBpp3dDemandValue.Weight<*>)
        assertKilogramQuantity(layerMaterialWeight.value, InfraNumber(2.0))
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
            id = "item-x",
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
        assertKilogramQuantity(scaledMaterialWeight.value, InfraNumber(4.0))

        val layerMaterialWeight = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight).values.single()
        assertTrue(layerMaterialWeight is QuantityBpp3dDemandValue.Weight<*>)
        assertKilogramQuantity(layerMaterialWeight.value, InfraNumber(2.0))
    }
}
