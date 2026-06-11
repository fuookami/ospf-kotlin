/**
 * 保守半径 envelope 测试。
 * Conservative radius envelope tests.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.geometry.Axis3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConservativeRadiusEnvelopeTest {

    @Test
    fun testEnvelopeProperties() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(5.0)
        )
        assertEquals(5.0, envelope.envelopeRadius.toDouble(), 1e-10)
        assertEquals(10.0, envelope.envelopeDiameter.toDouble(), 1e-10)
    }

    @Test
    fun testVerticalCylinderFootprint() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(3.0)
        )
        val height = infraScalar(10.0)
        // Vertical cylinder (axis=Y): width=diameter, depth=diameter
        assertEquals(6.0, envelope.footprintWidth(Axis3.Y, height).toDouble(), 1e-10)
        assertEquals(6.0, envelope.footprintDepth(Axis3.Y, height).toDouble(), 1e-10)
        assertEquals(10.0, envelope.footprintWidth(Axis3.X, height).toDouble(), 1e-10)
        assertEquals(6.0, envelope.footprintDepth(Axis3.X, height).toDouble(), 1e-10)
    }

    @Test
    fun testHorizontalCylinderFootprint() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(3.0)
        )
        val height = infraScalar(10.0)
        // X-axis horizontal cylinder: width=height, depth=diameter
        assertEquals(10.0, envelope.footprintWidth(Axis3.X, height).toDouble(), 1e-10)
        assertEquals(6.0, envelope.footprintDepth(Axis3.X, height).toDouble(), 1e-10)
        // Z-axis horizontal cylinder: depth=height, width=diameter
        assertEquals(6.0, envelope.footprintWidth(Axis3.Z, height).toDouble(), 1e-10)
        assertEquals(10.0, envelope.footprintDepth(Axis3.Z, height).toDouble(), 1e-10)
    }

    @Test
    fun testBoundingDimensions() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(3.0)
        )
        val height = infraScalar(10.0)
        // Vertical: bounding box is width=diameter x height=cylinder_height x depth=diameter
        assertEquals(6.0, envelope.boundingWidth(Axis3.Y, height).toDouble(), 1e-10)
        assertEquals(10.0, envelope.boundingHeight(Axis3.Y, height).toDouble(), 1e-10)
        assertEquals(6.0, envelope.boundingDepth(Axis3.Y, height).toDouble(), 1e-10)
        // X horizontal: bounding box is width=cylinder_height x height=diameter x depth=diameter
        assertEquals(10.0, envelope.boundingWidth(Axis3.X, height).toDouble(), 1e-10)
        assertEquals(6.0, envelope.boundingHeight(Axis3.X, height).toDouble(), 1e-10)
        assertEquals(6.0, envelope.boundingDepth(Axis3.X, height).toDouble(), 1e-10)
    }

    @Test
    fun testSupportCoverageRadius() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(3.0)
        )
        assertEquals(3.0, envelope.supportCoverageRadius().toDouble(), 1e-10)
    }

    @Test
    fun testCollisionMargin() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(3.0)
        )
        assertEquals(6.0, envelope.collisionMargin().toDouble(), 1e-10)
    }

    // ===== Real geometry tests =====

    @Test
    fun testRealFootprintVertical() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(3.0)
        )
        val height = infraScalar(10.0)
        val actualRadius = infraScalar(2.5)
        // Vertical: real footprint width = 2 * actualRadius = 5.0
        assertEquals(5.0, envelope.realFootprintWidth(Axis3.Y, height, actualRadius).toDouble(), 1e-10)
        assertEquals(5.0, envelope.realFootprintDepth(Axis3.Y, height, actualRadius).toDouble(), 1e-10)
    }

    @Test
    fun testRealBoundingVertical() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(3.0)
        )
        val height = infraScalar(10.0)
        val actualRadius = infraScalar(2.5)
        // Vertical: real bounding height = cylinder_height = 10
        assertEquals(10.0, envelope.realBoundingHeight(Axis3.Y, height, actualRadius).toDouble(), 1e-10)
        // Real bounding width = 2 * actualRadius = 5.0
        assertEquals(5.0, envelope.realBoundingWidth(Axis3.Y, height, actualRadius).toDouble(), 1e-10)
    }

    @Test
    fun testEnvelopeConservativeOverActual() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(3.0)
        )
        val height = infraScalar(10.0)
        val actualRadius = infraScalar(2.5)
        // Conservative footprint should always be >= real footprint
        assertTrue(
            envelope.footprintWidth(Axis3.Y, height).toDouble() >= envelope.realFootprintWidth(Axis3.Y, height, actualRadius).toDouble(),
            "Conservative footprint width should be >= real footprint width"
        )
        assertTrue(
            envelope.footprintDepth(Axis3.Y, height).toDouble() >= envelope.realFootprintDepth(Axis3.Y, height, actualRadius).toDouble(),
            "Conservative footprint depth should be >= real footprint depth"
        )
        assertTrue(
            envelope.boundingHeight(Axis3.Y, height).toDouble() >= envelope.realBoundingHeight(Axis3.Y, height, actualRadius).toDouble(),
            "Conservative bounding height should be >= real bounding height"
        )
    }

    // ===== Validation tests =====

    @Test
    fun testIsRadiusValidWithinBounds() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(3.0)
        )
        assertTrue(envelope.isRadiusValid(infraScalar(2.0)))
        assertTrue(envelope.isRadiusValid(infraScalar(2.5)))
        assertTrue(envelope.isRadiusValid(infraScalar(3.0)))
    }

    @Test
    fun testIsRadiusInvalidOutsideBounds() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(3.0)
        )
        assertTrue(!envelope.isRadiusValid(infraScalar(1.5)))
        assertTrue(!envelope.isRadiusValid(infraScalar(3.5)))
    }

    // ===== Edge case tests =====

    @Test
    fun testEqualBounds() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(3.0),
            rMax = infraScalar(3.0)
        )
        assertEquals(3.0, envelope.envelopeRadius.toDouble(), 1e-10)
        assertEquals(6.0, envelope.envelopeDiameter.toDouble(), 1e-10)
        assertTrue(envelope.isRadiusValid(infraScalar(3.0)))
        assertTrue(!envelope.isRadiusValid(infraScalar(2.99)))
    }

    @Test
    fun testRequiresPositiveRMin() {
        try {
            ConservativeRadiusEnvelope(
                rMin = infraScalar(0.0),
                rMax = infraScalar(3.0)
            )
            throw AssertionError("Should have thrown for non-positive rMin")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testRequiresRMaxGreaterOrEqualRMin() {
        try {
            ConservativeRadiusEnvelope(
                rMin = infraScalar(4.0),
                rMax = infraScalar(3.0)
            )
            throw AssertionError("Should have thrown for rMax < rMin")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }
}
