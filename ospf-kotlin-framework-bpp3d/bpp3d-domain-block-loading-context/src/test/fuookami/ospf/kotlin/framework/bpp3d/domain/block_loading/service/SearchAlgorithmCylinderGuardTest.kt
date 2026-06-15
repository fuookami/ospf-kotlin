/**
 * 搜索算法圆柱守卫测试。
 * Search algorithm cylinder guard test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class SearchAlgorithmCylinderGuardTest {
    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            packageMaxLayer = UInt64(10),
            maxHeight = FltX(10.0) * Meter,
            maxDepth = FltX(10.0) * Meter,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun cylinderItem(id: String, axis: Axis3 = Axis3.Y): ActualItem {
        val radius = FltX(0.5) * Meter
        return ActualItem(
            id = id,
            name = id,
            width = radius + radius,
            height = FltX(1.0) * Meter,
            depth = radius + radius,
            weight = FltX(1.0) * Kilogram,
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
            width = FltX(2.0) * Meter,
            height = FltX(2.0) * Meter,
            depth = FltX(2.0) * Meter
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
