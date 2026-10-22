package fuookami.ospf.kotlin.framework.bpp2d.domain.discrete_loading

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.Box2Geometry
import fuookami.ospf.kotlin.framework.bpp.infrastructure.Flt64PackingValueAdapter
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.DiscreteLoadingContext
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.*

class DiscreteGeometryTest {
    @Test
    fun `shared exact 2D geometry permits disjoint lateral positions and separate decks`() {
        fun box(id: String, x: Int, y: Int, width: Int, height: Int, space: String = "main") = LoadingRectangle(
            id = id, spaceId = space,
            bounds = Box2Geometry.of(
                x = Quantity(Flt64(x), Meter), y = Quantity(Flt64(y), Meter),
                width = Quantity(Flt64(width), Meter), height = Quantity(Flt64(height), Meter)
            )
        )
        val rectangles = listOf(box("large", 0, 0, 4, 2), box("left", 0, 0, 4, 1), box("right", 0, 1, 4, 1), box("lower", 0, 0, 4, 2, "lower"))
        val input = DiscreteLoadingProblem<Flt64>(
            items = emptyList(), positions = rectangles.map { LoadingPosition(it.id, UInt64.one, Quantity(Flt64(10), Kilogram)) },
            allowedAssignments = emptySet(), conflicts = emptySet(), layoutGroups = emptyList(), weightUnit = Kilogram
        )
        val context = assertIs<Ok<DiscreteLoadingContext<Flt64>, *, *>>(Bpp2dDiscreteLoading.context(problem = input, geometry = rectangles, adapter = Flt64PackingValueAdapter)).value
        assertEquals(setOf(setOf("large", "left"), setOf("large", "right")), context.problem.conflicts.map { setOf(it.first, it.second) }.toSet())
    }

    @Test
    fun `mixed quantity units are normalized before exact geometry`() {
        val rectangle = LoadingRectangle(
            id = "a", spaceId = "main",
            bounds = Box2Geometry.of(
                x = Quantity(Flt64.zero, Meter), y = Quantity(Flt64.zero, Centimeter),
                width = Quantity(Flt64(100), Centimeter), height = Quantity(Flt64.one, Meter)
            )
        )
        val other = rectangle.copy(id = "b", bounds = Box2Geometry.of(
            x = Quantity(Flt64.one, Meter), y = Quantity(Flt64.zero, Meter),
            width = Quantity(Flt64.one, Meter), height = Quantity(Flt64.one, Meter)
        ))
        val input = DiscreteLoadingProblem<Flt64>(
            items = emptyList(), positions = listOf("a", "b").map { LoadingPosition(it, UInt64.one, Quantity(Flt64.one, Kilogram)) },
            allowedAssignments = emptySet(), conflicts = emptySet(), layoutGroups = emptyList(), weightUnit = Kilogram
        )
        val context = assertIs<Ok<DiscreteLoadingContext<Flt64>, *, *>>(Bpp2dDiscreteLoading.context(problem = input, geometry = listOf(rectangle, other), adapter = Flt64PackingValueAdapter)).value
        assertTrue(context.problem.conflicts.isEmpty())
    }
}
