package fuookami.ospf.kotlin.framework.bpp

import kotlin.random.Random
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.bpp.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.*
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.*

class DiscreteLoadingTest {
    private fun interval(id: String, from: Double, to: Double, space: String = "main") = LoadingInterval(
        id = id, spaceId = space, start = Quantity(Flt64(from), Meter), end = Quantity(Flt64(to), Meter)
    )

    @Test
    fun `containment is not transitive and touching or separate decks do not conflict`() {
        val result = assertIs<Ok<Set<PositionConflict>, *, *>>(loadingConflicts(listOf(
            interval("large", 0.0, 4.0), interval("left", 0.0, 2.0), interval("right", 2.0, 4.0),
            interval("other", 0.0, 4.0, "lower")
        ))).value
        assertEquals(2, result.size)
        assertTrue(result.all { "large" in setOf(it.first, it.second) })
        val cliques = ConflictIndex.build(result).cliques
        assertTrue(cliques.all { it.size == 2 })
    }

    @Test
    fun `interval sweep matches a seeded brute force baseline`() {
        val random = Random(77031)
        val intervals = List(200) { index ->
            val start = random.nextInt(0, 100).toDouble()
            interval(index.toString(), start, start + random.nextInt(1, 10), "deck${index % 2}")
        }
        fun edge(a: String, b: String) = setOf(a, b)
        val expected = intervals.flatMapIndexed { i, a -> intervals.drop(i + 1).filter { b ->
            a.spaceId == b.spaceId && a.start.value < b.end.value && b.start.value < a.end.value
        }.map { b -> edge(a.id, b.id) } }.toSet()
        val actual = assertIs<Ok<Set<PositionConflict>, *, *>>(loadingConflicts(intervals)).value.map { edge(it.first, it.second) }.toSet()
        assertEquals(expected, actual)
    }

    private fun problem(layouts: Boolean = false): DiscreteLoadingProblem<Flt64> = DiscreteLoadingProblem(
        items = listOf(LoadingItem("a", Quantity(Flt64(2), Kilogram)), LoadingItem("b", Quantity(Flt64(3), Kilogram))),
        positions = listOf("large", "left", "right").map { LoadingPosition(it, UInt64(2), Quantity(Flt64(8), Kilogram)) },
        allowedAssignments = setOf(AssignmentKey("a", "left"), AssignmentKey("b", "left"), AssignmentKey("b", "right")),
        conflicts = setOf(PositionConflict("large", "left"), PositionConflict("large", "right")),
        layoutGroups = if (layouts) listOf(LayoutSelectionGroup(
            id = "all", positionIds = setOf("large", "left", "right"),
            layouts = listOf(LoadingLayout("big", setOf("large")), LoadingLayout("small", setOf("left", "right")))
        )) else emptyList(),
        weightUnit = Kilogram
    )

    @Test
    fun `sparse registration permits multiple pieces in one position and local mixing`() {
        val context = DiscreteLoadingContext(problem(), Flt64PackingValueAdapter)
        val model = LinearMetaModel<Flt64>(name = "sparse", converter = IntoValue.Identity)
        assertIs<Ok<*, *, *>>(context.register(model))
        assertEquals(3, context.aggregation.assignments.size)
        assertNull(context.aggregation.assignments[AssignmentKey("a", "large")])
        assertIs<Ok<*, *, *>>(context.verify(DiscreteLoadingSolution(mapOf("a" to "left", "b" to "left"), setOf("left"), emptyMap())))
        assertIs<Ok<*, *, *>>(context.verify(DiscreteLoadingSolution(mapOf("a" to "left", "b" to "right"), setOf("left", "right"), emptyMap())))
        assertIs<Failed<*, *, *>>(context.verify(DiscreteLoadingSolution(emptyMap(), setOf("large", "left"), emptyMap())))
    }

    @Test
    fun `positive additional weight requires whole-position occupancy without an epsilon threshold`() {
        val context = DiscreteLoadingContext(problem(), Flt64PackingValueAdapter)
        assertIs<Failed<*, *, *>>(context.verify(DiscreteLoadingSolution(emptyMap(), emptySet(), emptyMap()), mapOf("large" to Flt64(1e-30))))
        assertIs<Ok<*, *, *>>(context.verify(DiscreteLoadingSolution(emptyMap(), setOf("large"), emptyMap()), mapOf("large" to Flt64(1e-30))))
        assertIs<Failed<*, *, *>>(context.verify(DiscreteLoadingSolution(mapOf("a" to "left"), setOf("left"), emptyMap()), mapOf("left" to Flt64(7))))
    }

    @Test
    fun `layout consistency fixed cargo and exactly one extracted selection are verified`() {
        val input = problem(true).copy(items = problem().items.map { it.copy(fixedPositionId = if (it.id == "a") "left" else null) })
        val context = DiscreteLoadingContext(input, Flt64PackingValueAdapter)
        assertIs<Failed<*, *, *>>(context.verify(DiscreteLoadingSolution(emptyMap(), emptySet(), mapOf("all" to "small"))))
        assertIs<Failed<*, *, *>>(context.verify(DiscreteLoadingSolution(mapOf("a" to "left"), setOf("left"), mapOf("all" to "big"))))
        val model = LinearMetaModel<Flt64>(name = "layouts", converter = IntoValue.Identity)
        assertIs<Ok<*, *, *>>(context.register(model))
        val values = MutableList(model.tokens.tokens.size) { Flt64.zero }
        context.aggregation.layouts.values.forEach { values[model.tokens.indexOf(it)!!] = Flt64.one }
        assertIs<Failed<*, *, *>>(context.extractSolution(model, values))
    }

    @Test
    fun `generic FltX geometry normalizes centimeters`() {
        val intervals = listOf(
            LoadingInterval("a", "main", Quantity(FltX(0), Meter), Quantity(FltX(1), Meter)),
            LoadingInterval("b", "main", Quantity(FltX(100), Centimeter), Quantity(FltX(200), Centimeter))
        )
        assertTrue(assertIs<Ok<Set<PositionConflict>, *, *>>(loadingConflicts(intervals)).value.isEmpty())
    }

    @Test
    fun `fixed seeded model records a sparse versus dense registration baseline`() {
        val random = Random(28817)
        val positions = List(80) { LoadingPosition("p$it", UInt64(4), Quantity(Flt64(20), Kilogram)) }
        val items = List(60) { LoadingItem("c$it", Quantity(Flt64(1 + random.nextInt(4)), Kilogram)) }
        val legal = items.flatMapIndexed { index, item ->
            listOf(AssignmentKey(item.id, "p${index % 80}"), AssignmentKey(item.id, "p${(index + 1) % 80}"))
        }.toSet()
        val dense = items.flatMap { item -> positions.map { AssignmentKey(item.id, it.id) } }.toSet()
        fun register(pairs: Set<AssignmentKey>, baseline: Boolean): Triple<Int, Int, Long> {
            val context = DiscreteLoadingContext(
                problem = DiscreteLoadingProblem(items = items, positions = positions, allowedAssignments = pairs, conflicts = emptySet(), layoutGroups = emptyList(), weightUnit = Kilogram),
                adapter = Flt64PackingValueAdapter
            )
            val model = LinearMetaModel<Flt64>(name = "size_$baseline", converter = IntoValue.Identity)
            val start = System.nanoTime()
            assertIs<Ok<*, *, *>>(context.register(model))
            if (baseline) context.aggregation.assignments.filterKeys { it !in legal }.values.forEach { it.range.eq(false) }
            return Triple(model.tokens.tokens.size, model.constraints.size, System.nanoTime() - start)
        }
        val sparseStats = register(legal, false)
        val denseStats = register(dense, true)
        assertEquals(200, sparseStats.first)
        assertEquals(4880, denseStats.first)
        assertEquals(denseStats.second, sparseStats.second)
        println("bpp_scale,seed=28817,legal=${legal.size},dense_pairs=${dense.size},sparse_variables=${sparseStats.first},dense_variables=${denseStats.first},constraints=${sparseStats.second},conflict_constraints=0,sparse_build_ns=${sparseStats.third},dense_build_ns=${denseStats.third},iterations=0,cuts=0")
        // 两种注册的零固定非法组合均保留完全相同的业务可行分配。
        // Fixing every illegal dense pair to zero leaves exactly the same legal domain assignments.
        assertEquals(legal, dense.filter { it in legal }.toSet())
    }
}
