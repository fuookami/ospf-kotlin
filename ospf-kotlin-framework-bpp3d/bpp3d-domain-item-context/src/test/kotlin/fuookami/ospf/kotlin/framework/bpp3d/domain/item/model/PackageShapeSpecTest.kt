package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.Millimeter
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CylinderPackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraQuantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.eq
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar

class PackageShapeSpecTest {
    private fun assertRadiusValues(
        actual: List<InfraQuantity>,
        expected: List<Double>
    ) {
        assertEquals(
            expected = expected.size,
            actual = actual.size
        )
        for ((index, expectedValue) in expected.withIndex()) {
            assertEquals(
                expected = expectedValue,
                actual = actual[index].value.toDouble(),
                absoluteTolerance = 1e-9
            )
        }
    }

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
                radiusMax = infraScalar(0.6) * Meter
            )
        )

        val packingShape = assertNotNull(shape.toPackingShapeOrNull())
        assertTrue(packingShape is CylinderPackingShape3)
        assertTrue(packingShape.radius eq (infraScalar(0.5) * Meter))
        assertTrue(packingShape.boundingWidth eq (infraScalar(1.0) * Meter))
        assertRadiusValues(
            actual = (shape.shapeSpec as PackageShapeSpec.VerticalCylinder).resolvedRadiusCandidates,
            expected = listOf(0.4, 0.5, 0.6)
        )
    }

    @Test
    fun verticalCylinderRadiusWeightFunctionKeyShouldRemainMetadataOnly() {
        val spec = PackageShapeSpec.VerticalCylinder(
            radius = infraScalar(0.5) * Meter,
            axis = Axis3.Y,
            radiusMin = infraScalar(0.4) * Meter,
            radiusMax = infraScalar(0.6) * Meter,
            radiusWeightFunctionKey = "continuous-radius-prototype"
        )

        assertEquals(
            expected = "continuous-radius-prototype",
            actual = spec.radiusWeightFunctionKey
        )
        assertRadiusValues(
            actual = spec.resolvedRadiusCandidates,
            expected = listOf(0.5)
        )
    }

    @Test
    fun verticalCylinderProductionShapeShouldUseSelectedContinuousRadiusResult() {
        val shape = PackageShape(
            width = infraScalar(1.0) * Meter,
            height = infraScalar(1.2) * Meter,
            depth = infraScalar(1.0) * Meter,
            weight = infraScalar(3.0) * Kilogram,
            packageType = PackageType.CartonContainer,
            shapeSpec = PackageShapeSpec.VerticalCylinder(
                radius = infraScalar(0.5) * Meter,
                axis = Axis3.Y,
                radiusMin = infraScalar(0.4) * Meter,
                radiusMax = infraScalar(0.6) * Meter,
                radiusWeightFunctionKey = "continuous-radius-prototype"
            )
        )

        val packingShape = assertNotNull(shape.toPackingShapeOrNull())

        assertTrue(packingShape is CylinderPackingShape3)
        assertTrue(packingShape.radius eq (infraScalar(0.5) * Meter))
        assertEquals(
            expected = "continuous-radius-prototype",
            actual = (shape.shapeSpec as PackageShapeSpec.VerticalCylinder).radiusWeightFunctionKey
        )
    }

    @Test
    fun verticalCylinderRadiusWeightFunctionKeyShouldRejectDiscreteRadiusCandidates() {
        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = infraScalar(0.5) * Meter,
                axis = Axis3.Y,
                radiusCandidates = listOf(
                    infraScalar(0.4) * Meter,
                    infraScalar(0.5) * Meter
                ),
                radiusWeightFunctionKey = "continuous-radius-prototype"
            )
        }

        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = infraScalar(0.5) * Meter,
                axis = Axis3.Y,
                radiusMin = infraScalar(0.4) * Meter,
                radiusMax = infraScalar(0.6) * Meter,
                radiusStep = infraScalar(0.1) * Meter,
                radiusWeightFunctionKey = "continuous-radius-prototype"
            )
        }
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

    @Test
    fun verticalCylinderShouldGenerateRadiusCandidatesFromDiameterInterval() {
        val spec = PackageShapeSpec.VerticalCylinder(
            radius = infraScalar(0.15) * Meter,
            axis = Axis3.Y,
            diameterMin = infraScalar(0.30) * Meter,
            diameterMax = infraScalar(0.36) * Meter,
            diameterStep = infraScalar(0.01) * Meter
        )

        assertRadiusValues(
            actual = spec.resolvedRadiusCandidates,
            expected = listOf(0.15, 0.155, 0.16, 0.165, 0.17, 0.175, 0.18)
        )
    }

    @Test
    fun verticalCylinderShouldGenerateMillimeterRadiusCandidatesFromDiameterInterval() {
        val spec = PackageShapeSpec.VerticalCylinder(
            radius = infraScalar(150.0) * Millimeter,
            axis = Axis3.Y,
            diameterMin = infraScalar(300.0) * Millimeter,
            diameterMax = infraScalar(360.0) * Millimeter,
            diameterStep = infraScalar(10.0) * Millimeter
        )

        assertRadiusValues(
            actual = spec.resolvedRadiusCandidates,
            expected = listOf(150.0, 155.0, 160.0, 165.0, 170.0, 175.0, 180.0)
        )
    }

    @Test
    fun verticalCylinderShouldGenerateRadiusCandidatesFromRadiusInterval() {
        val spec = PackageShapeSpec.VerticalCylinder(
            radius = infraScalar(0.15) * Meter,
            axis = Axis3.Y,
            radiusMin = infraScalar(0.15) * Meter,
            radiusMax = infraScalar(0.18) * Meter,
            radiusStep = infraScalar(0.005) * Meter
        )

        assertRadiusValues(
            actual = spec.resolvedRadiusCandidates,
            expected = listOf(0.15, 0.155, 0.16, 0.165, 0.17, 0.175, 0.18)
        )
    }

    @Test
    fun verticalCylinderShouldPreferExplicitRadiusCandidatesOverIntervals() {
        val spec = PackageShapeSpec.VerticalCylinder(
            radius = infraScalar(0.16) * Meter,
            axis = Axis3.Y,
            radiusCandidates = listOf(
                infraScalar(0.16) * Meter,
                infraScalar(0.15) * Meter,
                infraScalar(0.16) * Meter
            ),
            radiusMin = infraScalar(0.15) * Meter,
            radiusMax = infraScalar(0.18) * Meter,
            radiusStep = infraScalar(0.005) * Meter,
            diameterMin = infraScalar(0.30) * Meter,
            diameterMax = infraScalar(0.36) * Meter,
            diameterStep = infraScalar(0.01) * Meter
        )

        assertRadiusValues(
            actual = spec.resolvedRadiusCandidates,
            expected = listOf(0.15, 0.16)
        )
    }

    @Test
    fun verticalCylinderShouldRejectInvalidDynamicRadiusIntervals() {
        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = infraScalar(0.15) * Meter,
                axis = Axis3.Y,
                radiusMin = infraScalar(0.15) * Meter,
                radiusMax = infraScalar(0.18) * Meter,
                radiusStep = infraScalar(0.0) * Meter
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = infraScalar(0.15) * Meter,
                axis = Axis3.Y,
                diameterMin = infraScalar(0.30) * Meter,
                diameterMax = infraScalar(0.36) * Meter,
                diameterStep = infraScalar(-0.01) * Meter
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = infraScalar(0.15) * Meter,
                axis = Axis3.Y,
                radiusStep = infraScalar(0.005) * Meter
            )
        }
    }

    @Test
    fun verticalCylinderShouldRejectMixedRadiusAndDiameterIntervalsWithoutExplicitCandidates() {
        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = infraScalar(0.15) * Meter,
                axis = Axis3.Y,
                radiusMin = infraScalar(0.15) * Meter,
                radiusMax = infraScalar(0.18) * Meter,
                radiusStep = infraScalar(0.005) * Meter,
                diameterMin = infraScalar(0.30) * Meter,
                diameterMax = infraScalar(0.36) * Meter,
                diameterStep = infraScalar(0.01) * Meter
            )
        }
    }
}
