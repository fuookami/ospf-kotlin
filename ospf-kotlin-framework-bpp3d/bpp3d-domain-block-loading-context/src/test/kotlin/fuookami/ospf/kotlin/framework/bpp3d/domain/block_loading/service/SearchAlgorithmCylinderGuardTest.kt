package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
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
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.fltX

class SearchAlgorithmCylinderGuardTest {
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

    private fun cylinderItem(id: String, axis: Axis3 = Axis3.Y): ActualItem {
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

    private fun shape(): Container3Shape {
        return Container3Shape(
            width = fltX(2.0) * Meter,
            height = fltX(2.0) * Meter,
            depth = fltX(2.0) * Meter
        )
    }

    @Test
    fun depthFirstSearchShouldRejectCylinderItemsInCuboidPath() {
        val algorithm = DepthFirstSearchAlgorithm(
            config = DepthFirstSearchAlgorithm.Config()
        )

        for (axis in listOf(Axis3.X, Axis3.Y, Axis3.Z)) {
            val error = assertFailsWith<IllegalArgumentException> {
                algorithm(
                    items = mapOf(cylinderItem(id = "dfs-cylinder-$axis", axis = axis) to UInt64.one),
                    shape = shape(),
                    blockTable = emptyList()
                )
            }

            assertTrue(error.message?.contains("DFS/MLHS space-splitting path is cuboid-only") == true)
        }
    }

    @Test
    fun multiLayerHeuristicSearchShouldRejectCylinderItemsInCuboidPath() {
        val algorithm = MultiLayerHeuristicSearchAlgorithm(
            config = MultiLayerHeuristicSearchAlgorithm.Config()
        )

        for (axis in listOf(Axis3.X, Axis3.Y, Axis3.Z)) {
            val error = assertFailsWith<IllegalArgumentException> {
                algorithm(
                    items = mapOf(cylinderItem(id = "mlhs-cylinder-$axis", axis = axis) to UInt64.one),
                    shape = shape(),
                    blockTable = emptyList()
                )
            }

            assertTrue(error.message?.contains("DFS/MLHS space-splitting path is cuboid-only") == true)
        }
    }
}
