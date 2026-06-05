package fuookami.ospf.kotlin.framework.bpp3d.application.service

import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayerPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LayerBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.layerBinOf

class DepthBoundaryLayerOrientationPolicyTest {
    private val binType = BinType(
        width = infraScalar(4.0) * Meter,
        height = infraScalar(4.0) * Meter,
        depth = infraScalar(12.0) * Meter,
        capacity = infraScalar(100.0) * Kilogram,
        longitudinalBalance = null,
        lateralBalance = null,
        typeCode = "BIN-DEPTH-POLICY"
    )

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

        val exception = assertFailsWith<IllegalArgumentException> {
            DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCuboidOrientations = setOf(Orientation.Side)
            ).ensureSatisfied(listOf(bin))
        }

        assertTrue(exception.message?.contains("boundary=first") == true)
        assertTrue(exception.message?.contains("cuboid_orientation=Upright") == true)
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

        val exception = assertFailsWith<IllegalArgumentException> {
            DepthBoundaryLayerOrientationPolicy(
                lastLayerAllowedCuboidOrientations = setOf(Orientation.Side)
            ).ensureSatisfied(listOf(bin))
        }

        assertTrue(exception.message?.contains("boundary=last") == true)
        assertTrue(exception.message?.contains("cuboid_orientation=Upright") == true)
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

        val exception = assertFailsWith<IllegalArgumentException> {
            DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCuboidOrientations = setOf(Orientation.Upright),
                lastLayerAllowedCuboidOrientations = setOf(Orientation.Side)
            ).ensureSatisfied(listOf(bin))
        }

        assertTrue(exception.message?.contains("boundary=last") == true)
        assertTrue(exception.message?.contains("cuboid_orientation=Upright") == true)
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

        val exception = assertFailsWith<IllegalArgumentException> {
            DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCylinderAxes = setOf(Axis3.X)
            ).ensureSatisfied(listOf(bin))
        }

        assertTrue(exception.message?.contains("boundary=first") == true)
        assertTrue(exception.message?.contains("cylinder_axis=Y") == true)
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

        val cuboidException = assertFailsWith<IllegalArgumentException> {
            DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCylinderAxes = setOf(Axis3.Y),
                firstLayerAllowedCuboidOrientations = setOf(Orientation.Upright)
            ).ensureSatisfied(listOf(bin))
        }
        assertTrue(cuboidException.message?.contains("cuboid_orientation=Side") == true)

        val cylinderException = assertFailsWith<IllegalArgumentException> {
            DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCylinderAxes = setOf(Axis3.X),
                firstLayerAllowedCuboidOrientations = setOf(Orientation.Side)
            ).ensureSatisfied(listOf(bin))
        }
        assertTrue(cylinderException.message?.contains("cylinder_axis=Y") == true)
    }

    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(InfraNumber.zero),
            hangingPolicy = AbsoluteHangingPolicy(InfraNumber.zero),
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
            width = InfraNumber.one * Meter,
            height = InfraNumber.one * Meter,
            depth = InfraNumber.one * Meter,
            weight = InfraNumber.one * Kilogram,
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
            shape = Container3Shape(binType),
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
            shape = Container3Shape(binType),
            units = listOf(
                cuboid.toItemPlacement(orientation = cuboidOrientation),
                cylinder.toItemPlacement(orientation = Orientation.Upright)
            )
        )
    }

    private fun placedLayer(layer: BinLayer, z: Double): BinLayerPlacement {
        return layer.toLayerPlacementWithoutAxisGuard(
            z = infraScalar(z) * Meter
        )
    }

    private fun binOf(vararg placements: BinLayerPlacement): LayerBin {
        return layerBinOf(
            shape = binType,
            units = placements.toList()
        )
    }

    private fun cylinder(axis: Axis3): PackageShapeSpec.VerticalCylinder {
        return PackageShapeSpec.VerticalCylinder(
            radius = infraScalar(0.4) * Meter,
            axis = axis
        )
    }
}
