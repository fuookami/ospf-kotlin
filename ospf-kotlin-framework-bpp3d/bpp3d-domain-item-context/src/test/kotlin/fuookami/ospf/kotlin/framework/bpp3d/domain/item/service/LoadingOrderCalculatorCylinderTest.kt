package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
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
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.placement3Of
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3

class LoadingOrderCalculatorCylinderTest {
    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            packageMaxLayer = UInt64(10),
            maxHeight = infraScalar(10.0) * Meter,
            maxDepth = infraScalar(10.0) * Meter,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(InfraNumber.zero),
            hangingPolicy = AbsoluteHangingPolicy(InfraNumber.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun cylinderItem(id: String): ActualItem {
        val radius = infraScalar(0.5) * Meter
        return ActualItem(
            id = id,
            name = id,
            width = radius + radius,
            height = infraScalar(1.0) * Meter,
            depth = radius + radius,
            weight = infraScalar(1.0) * Kilogram,
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
        val radius = infraScalar(0.5) * Meter
        return ActualItem(
            id = id,
            name = id,
            width = radius + radius,
            height = infraScalar(1.0) * Meter,
            depth = radius + radius,
            weight = infraScalar(1.0) * Kilogram,
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
            placement3Of(
                view = higherItem.view(Orientation.Upright),
                position = point3(
                    x = infraScalar(0.0) * Meter,
                    y = infraScalar(1.1) * Meter,
                    z = infraScalar(0.0) * Meter
                )
            ),
            placement3Of(
                view = lowerItem.view(Orientation.Upright),
                position = point3(
                    x = infraScalar(0.8) * Meter,
                    y = infraScalar(0.0) * Meter,
                    z = infraScalar(0.8) * Meter
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
    fun loadingOrderShouldRejectNonVerticalCylinderAxis() {
        val placements = listOf(
            placement3Of(
                view = cylinderItem(id = "cyl-x", axis = Axis3.X).view(Orientation.Upright),
                position = point3(x = infraScalar(0.0) * Meter)
            )
        )

        val calculator = LoadingOrderCalculator(
            maxBlockDepth = null,
            sameTypeJudger = { lhs, rhs -> lhs.pattern == rhs.pattern }
        )

        val error = assertFailsWith<IllegalArgumentException> {
            calculator(placements)
        }
        assertTrue(error.message?.contains("only Axis3.Y is allowed") == true)
    }

    @Test
    fun loadingOrderSideFrontShouldUseShapeAwareBoundingForVerticalCylinder() {
        val radius = infraScalar(0.5) * Meter
        val frontFirst = ActualItem(
            id = "front-first",
            name = "front-first",
            // inflate cuboid dimensions on purpose to ensure Side plane would overlap if old cuboid projection were used
            width = infraScalar(4.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(4.0) * Meter,
            weight = infraScalar(1.0) * Kilogram,
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
            width = infraScalar(4.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(4.0) * Meter,
            weight = infraScalar(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-side-candidate"),
            packageAttribute = packageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = Axis3.Y
            )
        )

        val placements = listOf(
            placement3Of(
                view = frontFirst.view(Orientation.Upright),
                position = point3(
                    x = infraScalar(2.0) * Meter,
                    y = infraScalar(0.0) * Meter,
                    z = infraScalar(0.0) * Meter
                )
            ),
            placement3Of(
                view = sideCandidate.view(Orientation.Upright),
                position = point3(
                    x = infraScalar(0.0) * Meter,
                    y = infraScalar(0.0) * Meter,
                    z = infraScalar(0.4) * Meter
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
