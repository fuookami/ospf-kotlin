package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.eq
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.quantity.times
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemandStatisticsTest {
    private val cargo = object : AbstractCargoAttribute {}

    private fun fixedRange(value: UInt64): ValueRange<UInt64> {
        return ValueRange(value, UInt64).value!!
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

    private fun packageShape(type: PackageType = PackageType.CartonContainer): PackageShape {
        return PackageShape(
            width = infraScalar(1.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(1.0) * Meter,
            weight = infraScalar(0.1) * Kilogram,
            packageType = type
        )
    }

    @Test
    fun actualItemWithoutPackageHasEmptyMaterialStatistics() {
        val item = ActualItem(
            id = "item-0",
            name = "item-0",
            width = infraScalar(1.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(1.0) * Meter,
            weight = infraScalar(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B0"),
            packageAttribute = defaultPackageAttribute()
        )

        assertTrue(item.materialAmounts.isEmpty())
        assertTrue(item.materialWeights.isEmpty())
    }

    @Test
    fun actualItemDerivesMaterialAmountAndWeightFromPackage() {
        val materialA = Material(
            no = MaterialNo("A"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "A",
            weight = infraScalar(5.0) * Kilogram
        )
        val materialB = Material(
            no = MaterialNo("B"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "B",
            weight = infraScalar(3.0) * Kilogram
        )
        val pack = Package.innerPackage(
            shape = packageShape(),
            materials = mapOf(
                materialA to UInt64(2),
                materialB to UInt64.one
            )
        )
        val item = ActualItem(
            id = "item-1",
            name = "item-1",
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B1"),
            packageAttribute = defaultPackageAttribute()
        )

        assertEquals(UInt64(2), item.materialAmounts[materialA.key])
        assertEquals(UInt64.one, item.materialAmounts[materialB.key])
        assertTrue(item.materialWeights[materialA.key]!! eq (infraScalar(10.0) * Kilogram))
        assertTrue(item.materialWeights[materialB.key]!! eq (infraScalar(3.0) * Kilogram))
    }

    @Test
    fun patternedItemAndBinLayerStatisticsSupportMaterialAmountAndWeightModes() {
        val materialAForItem1 = Material(
            no = MaterialNo("A"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "A-1",
            weight = infraScalar(5.0) * Kilogram
        )
        val materialAForItem2 = Material(
            no = MaterialNo("A"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "A-2",
            weight = infraScalar(7.0) * Kilogram
        )
        val materialB = Material(
            no = MaterialNo("B"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "B",
            weight = infraScalar(3.0) * Kilogram
        )

        val item1 = ActualItem(
            id = "item-1",
            name = "item-1",
            pack = Package.innerPackage(
                shape = packageShape(),
                materials = mapOf(
                    materialAForItem1 to UInt64(2),
                    materialB to UInt64.one
                )
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B1"),
            packageAttribute = defaultPackageAttribute()
        )
        val item2 = ActualItem(
            id = "item-2",
            name = "item-2",
            pack = Package.innerPackage(
                shape = packageShape(),
                materials = mapOf(
                    materialAForItem2 to UInt64.one
                )
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B2"),
            packageAttribute = defaultPackageAttribute()
        )

        val patterned = PatternedItem(
            actualItems = listOf(
                Triple(item1, UInt64(3), fixedRange(UInt64(3))),
                Triple(item2, UInt64(2), fixedRange(UInt64(2)))
            ),
            width = item1.width,
            height = item1.height,
            depth = item1.depth,
            weight = item1.weight,
            enabledOrientations = listOf(Orientation.Upright),
            packageAttribute = item1.packageAttribute
        )
        assertEquals(UInt64(8), patterned.materialAmounts[materialAForItem1.key])
        assertEquals(UInt64(3), patterned.materialAmounts[materialB.key])
        assertTrue(patterned.materialWeights[materialAForItem1.key]!! eq (infraScalar(44.0) * Kilogram))
        assertTrue(patterned.materialWeights[materialB.key]!! eq (infraScalar(9.0) * Kilogram))

        val placements = listOf(
            QuantityPlacement3(item1.view(), point3(x = infraScalar(0.0) * Meter)),
            QuantityPlacement3(item1.view(), point3(x = infraScalar(1.0) * Meter)),
            QuantityPlacement3(item1.view(), point3(x = infraScalar(2.0) * Meter)),
            QuantityPlacement3(item2.view(), point3(x = infraScalar(3.0) * Meter)),
            QuantityPlacement3(item2.view(), point3(x = infraScalar(4.0) * Meter))
        )
        val layer = BinLayer(
            iteration = Int64(0),
            from = DemandStatisticsTest::class,
            shape = Container3Shape(
                width = infraScalar(10.0) * Meter,
                height = infraScalar(10.0) * Meter,
                depth = infraScalar(10.0) * Meter
            ),
            units = placements
        )

        val itemAmountStatistics = layer.statistics(Bpp3dDemandMode.ItemAmount)
        val amountByItem = itemAmountStatistics.mapKeys {
            (it.key as Bpp3dDemandKey.Item).item
        }.mapValues {
            (it.value as Bpp3dDemandValue.Amount).value
        }
        assertTrue(layer.amounts == amountByItem)

        val materialAmountStatistics = layer.statistics(Bpp3dDemandMode.ItemMaterialAmount)
        assertEquals(
            UInt64(8),
            (materialAmountStatistics[Bpp3dDemandKey.Material(materialAForItem1.key)] as Bpp3dDemandValue.Amount).value
        )
        assertEquals(
            UInt64(3),
            (materialAmountStatistics[Bpp3dDemandKey.Material(materialB.key)] as Bpp3dDemandValue.Amount).value
        )

        val materialWeightStatistics = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight)
        assertTrue(
            ((materialWeightStatistics[Bpp3dDemandKey.Material(materialAForItem1.key)] as Bpp3dDemandValue.Weight).value)
                eq (infraScalar(44.0) * Kilogram)
        )
        assertTrue(
            ((materialWeightStatistics[Bpp3dDemandKey.Material(materialB.key)] as Bpp3dDemandValue.Weight).value)
                eq (infraScalar(9.0) * Kilogram)
        )
    }
}

