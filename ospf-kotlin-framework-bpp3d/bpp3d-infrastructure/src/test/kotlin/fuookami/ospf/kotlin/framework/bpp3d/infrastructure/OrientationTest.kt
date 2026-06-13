package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
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
        override val width: Quantity<FltX>,
        override val height: Quantity<FltX>,
        override val depth: Quantity<FltX>,
        override val weight: Quantity<FltX>
    ) : AbstractCuboid<FltX>

    private data class StubCuboidX(
        override val width: Quantity<FltX>,
        override val height: Quantity<FltX>,
        override val depth: Quantity<FltX>,
        override val weight: Quantity<FltX>
    ) : AbstractCuboid<FltX>

    private val unit = StubCuboid(
        width = fltX(2.0) * Meter,
        height = fltX(2.0) * Meter,
        depth = fltX(3.0) * Meter,
        weight = fltX(5.0) * Kilogram
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
        assertTrue(Orientation.Upright.width(unit) eq (fltX(2.0) * Meter))
        assertTrue(Orientation.Upright.height(unit) eq (fltX(2.0) * Meter))
        assertTrue(Orientation.Upright.depth(unit) eq (fltX(3.0) * Meter))

        assertTrue(Orientation.UprightRotated.width(unit) eq (fltX(3.0) * Meter))
        assertTrue(Orientation.UprightRotated.height(unit) eq (fltX(2.0) * Meter))
        assertTrue(Orientation.UprightRotated.depth(unit) eq (fltX(2.0) * Meter))

        assertTrue(Orientation.Side.width(unit) eq (fltX(2.0) * Meter))
        assertTrue(Orientation.Side.height(unit) eq (fltX(2.0) * Meter))
        assertTrue(Orientation.Side.depth(unit) eq (fltX(3.0) * Meter))

        assertTrue(Orientation.SideRotated.width(unit) eq (fltX(3.0) * Meter))
        assertTrue(Orientation.SideRotated.height(unit) eq (fltX(2.0) * Meter))
        assertTrue(Orientation.SideRotated.depth(unit) eq (fltX(2.0) * Meter))

        assertTrue(Orientation.Lie.width(unit) eq (fltX(2.0) * Meter))
        assertTrue(Orientation.Lie.height(unit) eq (fltX(3.0) * Meter))
        assertTrue(Orientation.Lie.depth(unit) eq (fltX(2.0) * Meter))

        assertTrue(Orientation.LieRotated.width(unit) eq (fltX(2.0) * Meter))
        assertTrue(Orientation.LieRotated.height(unit) eq (fltX(3.0) * Meter))
        assertTrue(Orientation.LieRotated.depth(unit) eq (fltX(2.0) * Meter))
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
    fun serializerShouldRoundTripStableLabels() {
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

    @Test
    fun orientationDimensionMappingShouldSupportFltX() {
        val unit = StubCuboidX(
            width = Quantity(FltX(2.0), Meter),
            height = Quantity(FltX(2.0), Meter),
            depth = Quantity(FltX(3.0), Meter),
            weight = Quantity(FltX(5.0), Kilogram)
        )

        assertTrue(Orientation.Upright.width(unit) eq Quantity(FltX(2.0), Meter))
        assertTrue(Orientation.UprightRotated.width(unit) eq Quantity(FltX(3.0), Meter))
        assertTrue(Orientation.Lie.height(unit) eq Quantity(FltX(3.0), Meter))
        assertEquals(
            listOf(
                Orientation.Upright,
                Orientation.UprightRotated,
                Orientation.Lie
            ),
            Orientation.merge(unit, Orientation.entries)
        )
    }
}

