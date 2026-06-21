/**
 * 深度边界层方向策略测试。
 * Depth boundary layer orientation policy test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class DepthBoundaryLayerOrientationPolicyTest {
    private val binType = BinType(
        width = FltX(4.0) * Meter,
        height = FltX(4.0) * Meter,
        depth = FltX(12.0) * Meter,
        capacity = FltX(100.0) * Kilogram,
        longitudinalBalance = null,
        lateralBalance = null,
        typeCode = "BIN-DEPTH-POLICY"
    )

    private fun assertPolicyFailed(block: () -> Try): String {
        val result = block()
        assertTrue(result is Failed)
        return result.error.message ?: ""
    }

    @Test
    fun depthBoundaryPolicyShouldNotRestrictWhenDisabled() {
        val bin = binOf(
            placedLayer(
                layer = layer("first", Orientation.Side),
                z = 0.0
            ),
            placedLayer(
                layer = layer("last", Orientation.Upright),
                z = 3.0
            )
        )

        DepthBoundaryLayerOrientationPolicy().ensureSatisfied(listOf(bin))
    }

    @Test
    fun depthBoundaryPolicyShouldUseDepthCoordinateOrder() {
        val bin = binOf(
            placedLayer(
                layer = layer("late", Orientation.Upright),
                z = 5.0
            ),
            placedLayer(
                layer = layer("early", Orientation.Side),
                z = 1.0
            )
        )

        DepthBoundaryLayerOrientationPolicy(
            firstLayerAllowedCuboidOrientations = setOf(Orientation.Side)
        ).ensureSatisfied(listOf(bin))
    }

    @Test
    fun depthBoundaryPolicyShouldRejectFirstCuboidOrientation() {
        val bin = binOf(
            placedLayer(
                layer = layer("first-reject", Orientation.Upright),
                z = 0.0
            ),
            placedLayer(
                layer = layer("last-free", Orientation.Side),
                z = 2.0
            )
        )

        val message = assertPolicyFailed {
            DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCuboidOrientations = setOf(Orientation.Side)
            ).ensureSatisfied(listOf(bin))
        }

        assertTrue(message.contains("boundary=first"))
        assertTrue(message.contains("cuboid_orientation=Upright"))
    }

    @Test
    fun depthBoundaryPolicyShouldRejectLastCuboidOrientation() {
        val bin = binOf(
            placedLayer(
                layer = layer("first-free", Orientation.Side),
                z = 0.0
            ),
            placedLayer(
                layer = layer("last-reject", Orientation.Upright),
                z = 2.0
            )
        )

        val message = assertPolicyFailed {
            DepthBoundaryLayerOrientationPolicy(
                lastLayerAllowedCuboidOrientations = setOf(Orientation.Side)
            ).ensureSatisfied(listOf(bin))
        }

        assertTrue(message.contains("boundary=last"))
        assertTrue(message.contains("cuboid_orientation=Upright"))
    }

    @Test
    fun depthBoundaryPolicyShouldApplyFirstAndLastToSingleLayer() {
        val bin = binOf(
            placedLayer(
                layer = layer("single-pass", Orientation.Upright),
                z = 0.0
            )
        )

        DepthBoundaryLayerOrientationPolicy(
            firstLayerAllowedCuboidOrientations = setOf(Orientation.Upright, Orientation.Side),
            lastLayerAllowedCuboidOrientations = setOf(Orientation.Upright)
        ).ensureSatisfied(listOf(bin))
    }

    @Test
    fun depthBoundaryPolicyShouldRejectSingleLayerConflict() {
        val bin = binOf(
            placedLayer(
                layer = layer("single-reject", Orientation.Upright),
                z = 0.0
            )
        )

        val message = assertPolicyFailed {
            DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCuboidOrientations = setOf(Orientation.Upright),
                lastLayerAllowedCuboidOrientations = setOf(Orientation.Side)
            ).ensureSatisfied(listOf(bin))
        }

        assertTrue(message.contains("boundary=last"))
        assertTrue(message.contains("cuboid_orientation=Upright"))
    }

    @Test
    fun depthBoundaryPolicyShouldApplyPerBin() {
        val validBin = binOf(
            placedLayer(
                layer = layer("bin-0-first-valid", Orientation.Side),
                z = 0.0
            ),
            placedLayer(
                layer = layer("bin-0-last-free", Orientation.Upright),
                z = 2.0
            )
        )
        val invalidBin = binOf(
            placedLayer(
                layer = layer("bin-1-first-invalid", Orientation.Upright),
                z = 0.0
            ),
            placedLayer(
                layer = layer("bin-1-last-free", Orientation.Side),
                z = 2.0
            )
        )

        val message = assertPolicyFailed {
            DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCuboidOrientations = setOf(Orientation.Side)
            ).ensureSatisfied(listOf(validBin, invalidBin))
        }

        assertTrue(message.contains("bin=1"))
        assertTrue(message.contains("boundary=first"))
        assertTrue(message.contains("cuboid_orientation=Upright"))
    }

    @Test
    fun depthBoundaryPolicyShouldRejectEmptyConfiguredSets() {
        val exception = assertFailsWith<IllegalArgumentException> {
            DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCylinderAxes = emptySet()
            )
        }

        assertTrue(exception.message?.contains("firstLayerAllowedCylinderAxes") == true)
    }

    @Test
    fun depthBoundaryPolicyShouldCheckCylinderAxis() {
        val bin = binOf(
            placedLayer(
                layer = layer(
                    id = "cylinder-first",
                    orientation = Orientation.Upright,
                    shapeSpec = cylinder(Axis3.Y)
                ),
                z = 0.0
            )
        )
        DepthBoundaryLayerOrientationPolicy(
            firstLayerAllowedCylinderAxes = setOf(Axis3.X, Axis3.Y)
        ).ensureSatisfied(listOf(bin))

        val message = assertPolicyFailed {
            DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCylinderAxes = setOf(Axis3.X)
            ).ensureSatisfied(listOf(bin))
        }

        assertTrue(message.contains("boundary=first"))
        assertTrue(message.contains("cylinder_axis=Y"))
    }

    @Test
    fun depthBoundaryPolicyShouldCheckHorizontalCylinderAxesOnDepthBoundaries() {
        val bin = binOf(
            placedLayer(
                layer = layer(
                    id = "cylinder-x-first",
                    orientation = Orientation.Upright,
                    shapeSpec = cylinder(Axis3.X)
                ),
                z = 0.0
            ),
            placedLayer(
                layer = layer(
                    id = "cylinder-z-last",
                    orientation = Orientation.Upright,
                    shapeSpec = cylinder(Axis3.Z)
                ),
                z = 2.0
            )
        )

        DepthBoundaryLayerOrientationPolicy(
            firstLayerAllowedCylinderAxes = setOf(Axis3.X),
            lastLayerAllowedCylinderAxes = setOf(Axis3.Z)
        ).ensureSatisfied(listOf(bin))

        val firstMessage = assertPolicyFailed {
            DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCylinderAxes = setOf(Axis3.Y),
                lastLayerAllowedCylinderAxes = setOf(Axis3.Z)
            ).ensureSatisfied(listOf(bin))
        }
        assertTrue(firstMessage.contains("boundary=first"))
        assertTrue(firstMessage.contains("cylinder_axis=X"))

        val lastMessage = assertPolicyFailed {
            DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCylinderAxes = setOf(Axis3.X),
                lastLayerAllowedCylinderAxes = setOf(Axis3.Y)
            ).ensureSatisfied(listOf(bin))
        }
        assertTrue(lastMessage.contains("boundary=last"))
        assertTrue(lastMessage.contains("cylinder_axis=Z"))
    }

    @Test
    fun depthBoundaryPolicyShouldCheckMixedBoundaryLayerByShapeKind() {
        val bin = binOf(
            placedLayer(
                layer = mixedLayer(
                    id = "mixed-first",
                    cuboidOrientation = Orientation.Side,
                    cylinderAxis = Axis3.Y
                ),
                z = 0.0
            )
        )

        DepthBoundaryLayerOrientationPolicy(
            firstLayerAllowedCylinderAxes = setOf(Axis3.Y),
            firstLayerAllowedCuboidOrientations = setOf(Orientation.Side)
        ).ensureSatisfied(listOf(bin))

        val cuboidMessage = assertPolicyFailed {
            DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCylinderAxes = setOf(Axis3.Y),
                firstLayerAllowedCuboidOrientations = setOf(Orientation.Upright)
            ).ensureSatisfied(listOf(bin))
        }
        assertTrue(cuboidMessage.contains("cuboid_orientation=Side"))

        val cylinderMessage = assertPolicyFailed {
            DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCylinderAxes = setOf(Axis3.X),
                firstLayerAllowedCuboidOrientations = setOf(Orientation.Side)
            ).ensureSatisfied(listOf(bin))
        }
        assertTrue(cylinderMessage.contains("cylinder_axis=Y"))
    }

    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(
        id: String,
        enabledOrientations: List<Orientation>,
        shapeSpec: PackageShapeSpec? = null
    ): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            width = FltX.one * Meter,
            height = FltX.one * Meter,
            depth = FltX.one * Meter,
            weight = FltX.one * Kilogram,
            enabledOrientations = enabledOrientations,
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute(),
            shapeSpecOverride = shapeSpec
        )
    }

    private fun layer(
        id: String,
        orientation: Orientation,
        shapeSpec: PackageShapeSpec? = null
    ): BinLayer {
        val item = item(
            id = id,
            enabledOrientations = listOf(Orientation.Upright, Orientation.Side),
            shapeSpec = shapeSpec
        )
        return BinLayer(
            iteration = Int64.zero,
            from = DepthBoundaryLayerOrientationPolicyTest::class,
            bin = binType,
            shape = Container3Shape(binType.asContainer3Shape()),
            units = listOf(item.toItemPlacement(orientation = orientation))
        )
    }

    private fun mixedLayer(
        id: String,
        cuboidOrientation: Orientation,
        cylinderAxis: Axis3
    ): BinLayer {
        val cuboid = item(
            id = "$id-cuboid",
            enabledOrientations = listOf(Orientation.Upright, Orientation.Side)
        )
        val cylinder = item(
            id = "$id-cylinder",
            enabledOrientations = listOf(Orientation.Upright),
            shapeSpec = cylinder(cylinderAxis)
        )
        return BinLayer(
            iteration = Int64.zero,
            from = DepthBoundaryLayerOrientationPolicyTest::class,
            bin = binType,
            shape = Container3Shape(binType.asContainer3Shape()),
            units = listOf(
                cuboid.toItemPlacement(orientation = cuboidOrientation),
                cylinder.toItemPlacement(orientation = Orientation.Upright)
            )
        )
    }

    private fun placedLayer(layer: BinLayer, z: Double): QuantityPlacement3<BinLayer, FltX> {
        return layer.toKnownCoordinateLayerPlacement(
            z = FltX(z) * Meter
        )
    }

    private fun binOf(vararg placements: QuantityPlacement3<BinLayer, FltX>): Bin<BinLayer, FltX> {
        return layerBinOf(
            shape = binType,
            units = placements.toList()
        )
    }

    private fun cylinder(axis: Axis3): PackageShapeSpec.VerticalCylinder {
        return PackageShapeSpec.VerticalCylinder(
            radius = FltX(0.4) * Meter,
            axis = axis
        )
    }
}
