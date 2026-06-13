package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPoint3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.eq
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.geometry.Axis3
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
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun packageShape(type: PackageType = PackageType.CartonContainer): PackageShape<FltX> {
        return PackageShape(
            width = fltX(1.0) * Meter,
            height = fltX(1.0) * Meter,
            depth = fltX(1.0) * Meter,
            weight = fltX(0.1) * Kilogram,
            packageType = type
        )
    }

    private fun cylinderPackageShape(type: PackageType = PackageType.CartonContainer): PackageShape<FltX> {
        return PackageShape(
            width = fltX(1.0) * Meter,
            height = fltX(1.0) * Meter,
            depth = fltX(1.0) * Meter,
            weight = fltX(0.1) * Kilogram,
            packageType = type,
            shapeSpec = PackageShapeSpec.VerticalCylinder(
                radius = fltX(0.5) * Meter,
                axis = Axis3.Y
            )
        )
    }

    @Test
    fun actualItemWithoutPackageHasEmptyMaterialStatistics() {
        val item = ActualItem(
            id = "item-0",
            name = "item-0",
            width = fltX(1.0) * Meter,
            height = fltX(1.0) * Meter,
            depth = fltX(1.0) * Meter,
            weight = fltX(1.0) * Kilogram,
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
            weight = fltX(5.0) * Kilogram
        )
        val materialB = Material(
            no = MaterialNo("B"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "B",
            weight = fltX(3.0) * Kilogram
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
        assertTrue(item.materialWeights[materialA.key]!! eq (fltX(10.0) * Kilogram))
        assertTrue(item.materialWeights[materialB.key]!! eq (fltX(3.0) * Kilogram))
    }

    @Test
    fun patternedItemAndBinLayerStatisticsSupportMaterialAmountAndWeightModes() {
        val materialAForItem1 = Material(
            no = MaterialNo("A"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "A-1",
            weight = fltX(5.0) * Kilogram
        )
        val materialAForItem2 = Material(
            no = MaterialNo("A"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "A-2",
            weight = fltX(7.0) * Kilogram
        )
        val materialB = Material(
            no = MaterialNo("B"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "B",
            weight = fltX(3.0) * Kilogram
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
        assertTrue(patterned.materialWeights[materialAForItem1.key]!! eq (fltX(44.0) * Kilogram))
        assertTrue(patterned.materialWeights[materialB.key]!! eq (fltX(9.0) * Kilogram))

        val placements = listOf(
            itemPlacement3Of(
                item1.view(),
                QuantityPoint3(
                    x = fltX(0.0) * Meter,
                    y = fltX(0.0) * Meter,
                    z = fltX(0.0) * Meter
                )
            ),
            itemPlacement3Of(
                item1.view(),
                QuantityPoint3(
                    x = fltX(1.0) * Meter,
                    y = fltX(0.0) * Meter,
                    z = fltX(0.0) * Meter
                )
            ),
            itemPlacement3Of(
                item1.view(),
                QuantityPoint3(
                    x = fltX(2.0) * Meter,
                    y = fltX(0.0) * Meter,
                    z = fltX(0.0) * Meter
                )
            ),
            itemPlacement3Of(
                item2.view(),
                QuantityPoint3(
                    x = fltX(3.0) * Meter,
                    y = fltX(0.0) * Meter,
                    z = fltX(0.0) * Meter
                )
            ),
            itemPlacement3Of(
                item2.view(),
                QuantityPoint3(
                    x = fltX(4.0) * Meter,
                    y = fltX(0.0) * Meter,
                    z = fltX(0.0) * Meter
                )
            )
        )
        val layer = BinLayer(
            iteration = Int64(0),
            from = DemandStatisticsTest::class,
            shape = Container3Shape(
                width = fltX(10.0) * Meter,
                height = fltX(10.0) * Meter,
                depth = fltX(10.0) * Meter
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
                eq (fltX(44.0) * Kilogram)
        )
        assertTrue(
            ((materialWeightStatistics[Bpp3dDemandKey.Material(materialB.key)] as Bpp3dDemandValue.Weight).value)
                eq (fltX(9.0) * Kilogram)
        )
    }

    @Test
    fun cylinderItemStatisticsShouldKeepAmountAndMaterialSemantics() {
        val material = Material(
            no = MaterialNo("CYL-M"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "CYL-M",
            weight = fltX(2.0) * Kilogram
        )
        val cylinderItem = ActualItem(
            id = "cyl-item-1",
            name = "cyl-item-1",
            pack = Package.innerPackage(
                shape = cylinderPackageShape(),
                materials = mapOf(
                    material to UInt64(2)
                )
            ),
            width = fltX(1.0) * Meter,
            height = fltX(1.0) * Meter,
            depth = fltX(1.0) * Meter,
            weight = fltX(0.1) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("BC1"),
            packageAttribute = defaultPackageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = fltX(0.5) * Meter,
                axis = Axis3.Y
            )
        )

        val itemStatistics = cylinderItem.statistics(Bpp3dDemandMode.ItemAmount)
        assertEquals(
            UInt64.one,
            (itemStatistics[Bpp3dDemandKey.Item(cylinderItem)] as Bpp3dDemandValue.Amount).value
        )

        val materialAmountStatistics = cylinderItem.statistics(Bpp3dDemandMode.ItemMaterialAmount)
        assertEquals(
            UInt64(2),
            (materialAmountStatistics[Bpp3dDemandKey.Material(material.key)] as Bpp3dDemandValue.Amount).value
        )

        val materialWeightStatistics = cylinderItem.statistics(Bpp3dDemandMode.ItemMaterialWeight)
        assertTrue(
            ((materialWeightStatistics[Bpp3dDemandKey.Material(material.key)] as Bpp3dDemandValue.Weight).value)
                eq (fltX(4.0) * Kilogram)
        )

        val placements = listOf(
            itemPlacement3Of(
                view = cylinderItem.view(),
                position = QuantityPoint3(
                    x = fltX(0.0) * Meter,
                    y = fltX(0.0) * Meter,
                    z = fltX(0.0) * Meter
                )
            ),
            itemPlacement3Of(
                view = cylinderItem.view(),
                position = QuantityPoint3(
                    x = fltX(1.2) * Meter,
                    y = fltX(0.0) * Meter,
                    z = fltX(0.0) * Meter
                )
            )
        )
        val layer = BinLayer(
            iteration = Int64(0),
            from = DemandStatisticsTest::class,
            shape = Container3Shape(
                width = fltX(10.0) * Meter,
                height = fltX(10.0) * Meter,
                depth = fltX(10.0) * Meter
            ),
            units = placements
        )

        val layerMaterialAmount = layer.statistics(Bpp3dDemandMode.ItemMaterialAmount)
        assertEquals(
            UInt64(4),
            (layerMaterialAmount[Bpp3dDemandKey.Material(material.key)] as Bpp3dDemandValue.Amount).value
        )
        val layerMaterialWeight = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight)
        assertTrue(
            ((layerMaterialWeight[Bpp3dDemandKey.Material(material.key)] as Bpp3dDemandValue.Weight).value)
                eq (fltX(8.0) * Kilogram)
        )
    }
}

