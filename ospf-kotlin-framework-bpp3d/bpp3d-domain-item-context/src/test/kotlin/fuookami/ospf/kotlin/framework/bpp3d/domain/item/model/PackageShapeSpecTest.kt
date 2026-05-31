package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CylinderPackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.eq
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar

class PackageShapeSpecTest {
    @Test
    fun verticalCylinderShouldKeepResolvedRadiusForPackingShape() {
        val shape = PackageShape(
            width = infraScalar(1.0) * Meter,
            height = infraScalar(1.2) * Meter,
            depth = infraScalar(1.0) * Meter,
            weight = infraScalar(3.0) * Kilogram,
            packageType = PackageType.CartonContainer,
            shapeSpec = PackageShapeSpec.VerticalCylinder(
                radius = infraScalar(0.5) * Meter,
                axis = Axis3.Y,
                radiusCandidates = listOf(
                    infraScalar(0.4) * Meter,
                    infraScalar(0.5) * Meter,
                    infraScalar(0.6) * Meter
                ),
                radiusMin = infraScalar(0.4) * Meter,
                radiusMax = infraScalar(0.6) * Meter,
                radiusWeightFunctionKey = "mass-linear-v1"
            )
        )

        val packingShape = assertNotNull(shape.toPackingShapeOrNull())
        assertTrue(packingShape is CylinderPackingShape3)
        assertTrue(packingShape.radius eq (infraScalar(0.5) * Meter))
        assertTrue(packingShape.boundingWidth eq (infraScalar(1.0) * Meter))
    }

    @Test
    fun verticalCylinderShouldRejectResolvedRadiusOutsideCandidates() {
        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = infraScalar(0.5) * Meter,
                axis = Axis3.Y,
                radiusCandidates = listOf(
                    infraScalar(0.4) * Meter,
                    infraScalar(0.6) * Meter
                )
            )
        }
    }

    @Test
    fun verticalCylinderShouldRejectInvalidRadiusRange() {
        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = infraScalar(0.5) * Meter,
                axis = Axis3.Y,
                radiusMin = infraScalar(0.6) * Meter,
                radiusMax = infraScalar(0.4) * Meter
            )
        }
    }
}
