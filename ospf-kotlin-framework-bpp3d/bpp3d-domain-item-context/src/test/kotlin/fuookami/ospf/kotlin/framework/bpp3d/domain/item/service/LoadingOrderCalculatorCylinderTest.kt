package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.itemPlacement3Of
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPoint3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.fltX

class LoadingOrderCalculatorCylinderTest {
    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            packageMaxLayer = UInt64(10),
            maxHeight = fltX(10.0) * Meter,
            maxDepth = fltX(10.0) * Meter,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun cylinderItem(id: String): ActualItem {
        val radius = fltX(0.5) * Meter
        return ActualItem(
            id = id,
            name = id,
            width = radius + radius,
            height = fltX(1.0) * Meter,
            depth = radius + radius,
            weight = fltX(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = Axis3.Y
            )
        )
    }

    private fun cylinderItem(id: String, axis: Axis3): ActualItem {
        val radius = fltX(0.5) * Meter
        return ActualItem(
            id = id,
            name = id,
            width = radius + radius,
            height = fltX(1.0) * Meter,
            depth = radius + radius,
            weight = fltX(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = axis
            )
        )
    }

    @Test
    fun loadingOrderShouldUseRealCylinderFootprintOnBottomPlane() {
        val higherItem = cylinderItem("higher")
        val lowerItem = cylinderItem("lower")
        val placements = listOf(
            itemPlacement3Of(
                view = higherItem.view(Orientation.Upright),
                position = QuantityPoint3(
                    x = fltX(0.0) * Meter,
                    y = fltX(1.1) * Meter,
                    z = fltX(0.0) * Meter
                )
            ),
            itemPlacement3Of(
                view = lowerItem.view(Orientation.Upright),
                position = QuantityPoint3(
                    x = fltX(0.8) * Meter,
                    y = fltX(0.0) * Meter,
                    z = fltX(0.8) * Meter
                )
            )
        )

        val calculator = LoadingOrderCalculator(
            maxBlockDepth = null,
            sameTypeJudger = { lhs, rhs -> lhs.pattern == rhs.pattern }
        )
        val orderedIds = calculator(placements).map { (placement, _) ->
            (placement.unit as ActualItem).id
        }

        assertEquals(listOf("higher", "lower"), orderedIds)
    }

    @Test
    fun loadingOrderShouldAcceptHorizontalCylinderAxisForSequenceOnly() {
        val placements = listOf(
            itemPlacement3Of(
                view = cylinderItem(id = "cyl-x", axis = Axis3.X).view(Orientation.Upright),
                position = QuantityPoint3(
                    x = fltX(0.0) * Meter,
                    y = fltX(0.0) * Meter,
                    z = fltX(0.0) * Meter
                )
            )
        )

        val calculator = LoadingOrderCalculator(
            maxBlockDepth = null,
            sameTypeJudger = { lhs, rhs -> lhs.pattern == rhs.pattern }
        )

        val orderedIds = calculator(placements).map { (placement, _) ->
            (placement.unit as ActualItem).id
        }

        assertEquals(listOf("cyl-x"), orderedIds)
    }

    @Test
    fun loadingOrderSideFrontShouldUseShapeAwareBoundingForVerticalCylinder() {
        val radius = fltX(0.5) * Meter
        val frontFirst = ActualItem(
            id = "front-first",
            name = "front-first",
            // inflate cuboid dimensions on purpose to ensure Side plane would overlap if old cuboid projection were used
            width = fltX(4.0) * Meter,
            height = fltX(1.0) * Meter,
            depth = fltX(4.0) * Meter,
            weight = fltX(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-front-first"),
            packageAttribute = packageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = Axis3.Y
            )
        )
        val sideCandidate = ActualItem(
            id = "side-candidate",
            name = "side-candidate",
            width = fltX(4.0) * Meter,
            height = fltX(1.0) * Meter,
            depth = fltX(4.0) * Meter,
            weight = fltX(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-side-candidate"),
            packageAttribute = packageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = Axis3.Y
            )
        )

        val placements = listOf(
            itemPlacement3Of(
                view = frontFirst.view(Orientation.Upright),
                position = QuantityPoint3(
                    x = fltX(2.0) * Meter,
                    y = fltX(0.0) * Meter,
                    z = fltX(0.0) * Meter
                )
            ),
            itemPlacement3Of(
                view = sideCandidate.view(Orientation.Upright),
                position = QuantityPoint3(
                    x = fltX(0.0) * Meter,
                    y = fltX(0.0) * Meter,
                    z = fltX(0.4) * Meter
                )
            )
        )

        val calculator = LoadingOrderCalculator(
            maxBlockDepth = null,
            sameTypeJudger = { _, _ -> false }
        )
        val orderedIds = calculator(placements).map { (placement, _) ->
            (placement.unit as ActualItem).id
        }

        assertEquals(
            listOf("side-candidate", "front-first"),
            orderedIds
        )
    }
}
