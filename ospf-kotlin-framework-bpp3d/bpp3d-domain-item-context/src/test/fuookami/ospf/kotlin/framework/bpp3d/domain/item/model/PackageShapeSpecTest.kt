/**
 * 包装形状规格测试。
 * Package shape spec test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

class PackageShapeSpecTest {
    private fun assertRadiusValues(
        actual: List<Quantity<FltX>>,
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
            width = FltX(1.0) * Meter,
            height = FltX(1.2) * Meter,
            depth = FltX(1.0) * Meter,
            weight = FltX(3.0) * Kilogram,
            packageType = PackageType.CartonContainer,
            shapeSpec = PackageShapeSpec.VerticalCylinder(
                radius = FltX(0.5) * Meter,
                axis = Axis3.Y,
                radiusCandidates = listOf(
                    FltX(0.4) * Meter,
                    FltX(0.5) * Meter,
                    FltX(0.6) * Meter
                ),
                radiusMin = FltX(0.4) * Meter,
                radiusMax = FltX(0.6) * Meter
            )
        )

        val packingShape = assertNotNull(shape.toPackingShapeOrNull())
        assertTrue(packingShape is CylinderPackingShape3)
        assertTrue(packingShape.radius eq (FltX(0.5) * Meter))
        assertTrue(packingShape.boundingWidth eq (FltX(1.0) * Meter))
        assertRadiusValues(
            actual = (shape.shapeSpec as PackageShapeSpec.VerticalCylinder).resolvedRadiusCandidates,
            expected = listOf(0.4, 0.5, 0.6)
        )
    }

    @Test
    fun verticalCylinderRadiusWeightFunctionKeyShouldRemainMetadataOnly() {
        val spec = PackageShapeSpec.VerticalCylinder(
            radius = FltX(0.5) * Meter,
            axis = Axis3.Y,
            radiusMin = FltX(0.4) * Meter,
            radiusMax = FltX(0.6) * Meter,
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
            width = FltX(1.0) * Meter,
            height = FltX(1.2) * Meter,
            depth = FltX(1.0) * Meter,
            weight = FltX(3.0) * Kilogram,
            packageType = PackageType.CartonContainer,
            shapeSpec = PackageShapeSpec.VerticalCylinder(
                radius = FltX(0.5) * Meter,
                axis = Axis3.Y,
                radiusMin = FltX(0.4) * Meter,
                radiusMax = FltX(0.6) * Meter,
                radiusWeightFunctionKey = "continuous-radius-prototype"
            )
        )

        val packingShape = assertNotNull(shape.toPackingShapeOrNull())

        assertTrue(packingShape is CylinderPackingShape3)
        assertTrue(packingShape.radius eq (FltX(0.5) * Meter))
        assertEquals(
            expected = "continuous-radius-prototype",
            actual = (shape.shapeSpec as PackageShapeSpec.VerticalCylinder).radiusWeightFunctionKey
        )
    }

    @Test
    fun verticalCylinderContinuousRadiusSelectionShouldExposeSelectedResult() {
        val spec = PackageShapeSpec.VerticalCylinder(
            radius = FltX(0.5) * Meter,
            axis = Axis3.Z,
            radiusMin = FltX(0.4) * Meter,
            radiusMax = FltX(0.6) * Meter,
            radiusWeightFunctionKey = "continuous-radius-prototype"
        )

        val selection = assertNotNull(spec.continuousRadiusSelectionResult())

        assertEquals("continuous-radius-prototype", selection.key)
        assertEquals(Axis3.Z, selection.axis)
        assertTrue(selection.selectedRadius eq (FltX(0.5) * Meter))
        assertTrue(selection.radiusMin!! eq (FltX(0.4) * Meter))
        assertTrue(selection.radiusMax!! eq (FltX(0.6) * Meter))
    }

    @Test
    fun selectedContinuousRadiusShouldExposeSolverPrototype() {
        val spec = PackageShapeSpec.VerticalCylinder(
            radius = FltX(0.5) * Meter,
            axis = Axis3.Z,
            radiusMin = FltX(0.4) * Meter,
            radiusMax = FltX(0.6) * Meter,
            radiusWeightFunctionKey = "continuous-radius-prototype"
        )

        val prototype = assertNotNull(spec.continuousRadiusSolverPrototype(source = "unit test"))

        assertEquals("continuous-radius-prototype", prototype.radiusWeightFunctionKey)
        assertEquals(Axis3.Z, prototype.axis)
        assertEquals("cylinder_radius_unit_test_continuous_radius_prototype_Z", prototype.variableName)
        assertTrue(prototype.radiusLowerBound!! eq (FltX(0.4) * Meter))
        assertTrue(prototype.radiusUpperBound!! eq (FltX(0.6) * Meter))
        assertTrue(prototype.initialRadius!! eq (FltX(0.5) * Meter))
        assertTrue(prototype.isProductionReady)
        assertEquals(emptyList(), prototype.gaps)
    }

    @Test
    fun selectedContinuousRadiusSolverPrototypeShouldRejectRadiusBelowBounds() {
        val exception = assertFailsWith<IllegalArgumentException> {
            continuousCylinderRadiusSolverPrototype(
                source = "unit test",
                radiusWeightFunctionKey = "continuous-radius-prototype",
                axis = Axis3.Y,
                selectedRadius = FltX(0.3) * Meter,
                radiusMin = FltX(0.4) * Meter,
                radiusMax = FltX(0.6) * Meter
            )
        }

        assertTrue(exception.message?.contains("greater than or equal to lower bound") == true)
    }

    @Test
    fun selectedContinuousRadiusSolverPrototypeShouldRejectRadiusAboveBounds() {
        val exception = assertFailsWith<IllegalArgumentException> {
            continuousCylinderRadiusSolverPrototype(
                source = "unit test",
                radiusWeightFunctionKey = "continuous-radius-prototype",
                axis = Axis3.Y,
                selectedRadius = FltX(0.7) * Meter,
                radiusMin = FltX(0.4) * Meter,
                radiusMax = FltX(0.6) * Meter
            )
        }

        assertTrue(exception.message?.contains("less than or equal to upper bound") == true)
    }

    @Test
    fun selectedContinuousRadiusShouldHaveNoOptimizationGap() {
        val report = continuousCylinderRadiusOptimizationGapReport(
            source = "test",
            radiusWeightFunctionKey = "continuous-radius-prototype",
            hasConcreteSelectedRadius = true,
            hasDiscreteRadiusCandidates = false,
            hasDiscreteRadiusStep = false,
            hasContinuousRadiusInterval = false,
            hasContinuousDiameterInterval = false
        )

        assertEquals(
            expected = null,
            actual = report
        )
    }

    @Test
    fun intervalOnlyContinuousRadiusShouldExposeSolverOptimizationGap() {
        val report = assertNotNull(
            continuousCylinderRadiusOptimizationGapReport(
                source = "test",
                radiusWeightFunctionKey = "continuous-radius-prototype",
                hasConcreteSelectedRadius = false,
                hasContinuousRadiusInterval = true
            )
        )

        assertEquals(
            expected = listOf(
                ContinuousCylinderRadiusOptimizationGap.MissingSelectedRadius,
                ContinuousCylinderRadiusOptimizationGap.SolverNativeRadiusIntervalUnsupported
            ),
            actual = report.gaps
        )
        assertTrue(report.message("row=1").contains("continuous-radius-prototype"))
        assertTrue(report.message("row=1").contains("symbolic radius variables"))
        assertTrue(report.message("row=1").contains("row=1"))
    }

    @Test
    fun intervalOnlyContinuousRadiusShouldExposeSolverPrototypeGap() {
        val prototype = assertNotNull(
            continuousCylinderRadiusSolverPrototype(
                source = "Gurobi CSV",
                radiusWeightFunctionKey = "continuous-radius-prototype",
                axis = Axis3.Y,
                radiusMin = FltX(0.15) * Meter,
                radiusMax = FltX(0.18) * Meter
            )
        )

        assertEquals("continuous-radius-prototype", prototype.radiusWeightFunctionKey)
        assertEquals("cylinder_radius_Gurobi_CSV_continuous_radius_prototype_Y", prototype.variableName)
        assertTrue(prototype.radiusLowerBound!! eq (FltX(0.15) * Meter))
        assertTrue(prototype.radiusUpperBound!! eq (FltX(0.18) * Meter))
        assertEquals(null, prototype.initialRadius)
        assertEquals(
            expected = listOf(
                ContinuousCylinderRadiusOptimizationGap.MissingSelectedRadius,
                ContinuousCylinderRadiusOptimizationGap.SolverNativeRadiusIntervalUnsupported
            ),
            actual = prototype.gaps
        )
        assertEquals(false, prototype.isProductionReady)
        assertTrue(prototype.messageSuffix().contains("solverPrototype"))
        assertTrue(prototype.messageSuffix().contains("productionReady=false"))
    }

    @Test
    fun verticalCylinderRadiusWeightFunctionKeyShouldRejectDiscreteRadiusCandidates() {
        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = FltX(0.5) * Meter,
                axis = Axis3.Y,
                radiusCandidates = listOf(
                    FltX(0.4) * Meter,
                    FltX(0.5) * Meter
                ),
                radiusWeightFunctionKey = "continuous-radius-prototype"
            )
        }

        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = FltX(0.5) * Meter,
                axis = Axis3.Y,
                radiusMin = FltX(0.4) * Meter,
                radiusMax = FltX(0.6) * Meter,
                radiusStep = FltX(0.1) * Meter,
                radiusWeightFunctionKey = "continuous-radius-prototype"
            )
        }
    }

    @Test
    fun verticalCylinderShouldRejectResolvedRadiusOutsideCandidates() {
        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = FltX(0.5) * Meter,
                axis = Axis3.Y,
                radiusCandidates = listOf(
                    FltX(0.4) * Meter,
                    FltX(0.6) * Meter
                )
            )
        }
    }

    @Test
    fun verticalCylinderShouldRejectInvalidRadiusRange() {
        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = FltX(0.5) * Meter,
                axis = Axis3.Y,
                radiusMin = FltX(0.6) * Meter,
                radiusMax = FltX(0.4) * Meter
            )
        }
    }

    @Test
    fun verticalCylinderShouldGenerateRadiusCandidatesFromDiameterInterval() {
        val spec = PackageShapeSpec.VerticalCylinder(
            radius = FltX(0.15) * Meter,
            axis = Axis3.Y,
            diameterMin = FltX(0.30) * Meter,
            diameterMax = FltX(0.36) * Meter,
            diameterStep = FltX(0.01) * Meter
        )

        assertRadiusValues(
            actual = spec.resolvedRadiusCandidates,
            expected = listOf(0.15, 0.155, 0.16, 0.165, 0.17, 0.175, 0.18)
        )
    }

    @Test
    fun verticalCylinderShouldGenerateMillimeterRadiusCandidatesFromDiameterInterval() {
        val spec = PackageShapeSpec.VerticalCylinder(
            radius = FltX(150.0) * Millimeter,
            axis = Axis3.Y,
            diameterMin = FltX(300.0) * Millimeter,
            diameterMax = FltX(360.0) * Millimeter,
            diameterStep = FltX(10.0) * Millimeter
        )

        assertRadiusValues(
            actual = spec.resolvedRadiusCandidates,
            expected = listOf(150.0, 155.0, 160.0, 165.0, 170.0, 175.0, 180.0)
        )
    }

    @Test
    fun verticalCylinderShouldGenerateRadiusCandidatesFromRadiusInterval() {
        val spec = PackageShapeSpec.VerticalCylinder(
            radius = FltX(0.15) * Meter,
            axis = Axis3.Y,
            radiusMin = FltX(0.15) * Meter,
            radiusMax = FltX(0.18) * Meter,
            radiusStep = FltX(0.005) * Meter
        )

        assertRadiusValues(
            actual = spec.resolvedRadiusCandidates,
            expected = listOf(0.15, 0.155, 0.16, 0.165, 0.17, 0.175, 0.18)
        )
    }

    @Test
    fun verticalCylinderShouldPreferExplicitRadiusCandidatesOverIntervals() {
        val spec = PackageShapeSpec.VerticalCylinder(
            radius = FltX(0.16) * Meter,
            axis = Axis3.Y,
            radiusCandidates = listOf(
                FltX(0.16) * Meter,
                FltX(0.15) * Meter,
                FltX(0.16) * Meter
            ),
            radiusMin = FltX(0.15) * Meter,
            radiusMax = FltX(0.18) * Meter,
            radiusStep = FltX(0.005) * Meter,
            diameterMin = FltX(0.30) * Meter,
            diameterMax = FltX(0.36) * Meter,
            diameterStep = FltX(0.01) * Meter
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
                radius = FltX(0.15) * Meter,
                axis = Axis3.Y,
                radiusMin = FltX(0.15) * Meter,
                radiusMax = FltX(0.18) * Meter,
                radiusStep = FltX(0.0) * Meter
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = FltX(0.15) * Meter,
                axis = Axis3.Y,
                diameterMin = FltX(0.30) * Meter,
                diameterMax = FltX(0.36) * Meter,
                diameterStep = FltX(-0.01) * Meter
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = FltX(0.15) * Meter,
                axis = Axis3.Y,
                radiusStep = FltX(0.005) * Meter
            )
        }
    }

    @Test
    fun verticalCylinderShouldRejectMixedRadiusAndDiameterIntervalsWithoutExplicitCandidates() {
        assertFailsWith<IllegalArgumentException> {
            PackageShapeSpec.VerticalCylinder(
                radius = FltX(0.15) * Meter,
                axis = Axis3.Y,
                radiusMin = FltX(0.15) * Meter,
                radiusMax = FltX(0.18) * Meter,
                radiusStep = FltX(0.005) * Meter,
                diameterMin = FltX(0.30) * Meter,
                diameterMax = FltX(0.36) * Meter,
                diameterStep = FltX(0.01) * Meter
            )
        }
    }
}
