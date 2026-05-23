package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.utils.functional.Order
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OrientationTest {
    private data class StubCuboid(
        override val width: QuantityFlt64,
        override val height: QuantityFlt64,
        override val depth: QuantityFlt64,
        override val weight: QuantityFlt64
    ) : AbstractCuboid

    private val unit = StubCuboid(
        width = 2.0 * Meter,
        height = 2.0 * Meter,
        depth = 3.0 * Meter,
        weight = 5.0 * Kilogram
    )

    @Test
    fun entriesShouldContainAllOrientations() {
        assertEquals(
            listOf(
                Orientation.Upright,
                Orientation.UprightRotated,
                Orientation.Side,
                Orientation.SideRotated,
                Orientation.Lie,
                Orientation.LieRotated
            ),
            Orientation.entries
        )
    }

    @Test
    fun eachOrientationShouldMapDimensionsCorrectly() {
        assertTrue(Orientation.Upright.width(unit) eq (2.0 * Meter))
        assertTrue(Orientation.Upright.height(unit) eq (2.0 * Meter))
        assertTrue(Orientation.Upright.depth(unit) eq (3.0 * Meter))

        assertTrue(Orientation.UprightRotated.width(unit) eq (3.0 * Meter))
        assertTrue(Orientation.UprightRotated.height(unit) eq (2.0 * Meter))
        assertTrue(Orientation.UprightRotated.depth(unit) eq (2.0 * Meter))

        assertTrue(Orientation.Side.width(unit) eq (2.0 * Meter))
        assertTrue(Orientation.Side.height(unit) eq (2.0 * Meter))
        assertTrue(Orientation.Side.depth(unit) eq (3.0 * Meter))

        assertTrue(Orientation.SideRotated.width(unit) eq (3.0 * Meter))
        assertTrue(Orientation.SideRotated.height(unit) eq (2.0 * Meter))
        assertTrue(Orientation.SideRotated.depth(unit) eq (2.0 * Meter))

        assertTrue(Orientation.Lie.width(unit) eq (2.0 * Meter))
        assertTrue(Orientation.Lie.height(unit) eq (3.0 * Meter))
        assertTrue(Orientation.Lie.depth(unit) eq (2.0 * Meter))

        assertTrue(Orientation.LieRotated.width(unit) eq (2.0 * Meter))
        assertTrue(Orientation.LieRotated.height(unit) eq (3.0 * Meter))
        assertTrue(Orientation.LieRotated.depth(unit) eq (2.0 * Meter))
    }

    @Test
    fun mergeShouldDeduplicateEquivalentShapes() {
        val merged = Orientation.merge(unit, Orientation.entries)
        assertEquals(
            listOf(
                Orientation.Upright,
                Orientation.UprightRotated,
                Orientation.Lie
            ),
            merged
        )
    }

    @Test
    fun serializerShouldRoundTripLegacyLabels() {
        for (orientation in Orientation.entries) {
            val encoded = Json.encodeToString(OrientationSerializer, orientation)
            assertEquals("\"${orientation.label}\"", encoded)
            val decoded = Json.decodeFromString(OrientationSerializer, encoded)
            assertEquals(orientation, decoded)
        }
        assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString(OrientationSerializer, "\"UnknownOrientation\"")
        }
    }

    @Test
    fun ordShouldCompareByCategoryThenRank() {
        assertTrue((Orientation.Upright ord Orientation.UprightRotated) is Order.Less)
        assertTrue((Orientation.Side ord Orientation.Lie) is Order.Less)
        assertTrue((Orientation.LieRotated ord Orientation.Lie) is Order.Greater)
    }
}
