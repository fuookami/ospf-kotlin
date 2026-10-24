package fuookami.ospf.kotlin.framework.bpp1d.application

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.framework.bpp.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp1d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp1d.domain.pattern_assignment.*

class PatternIncrementalRefreshTest {
    private fun problem() = PackingProblem(
        items = listOf("a", "b").map { id ->
            Item(
                id = id,
                length = Quantity(Flt64.one, Meter),
                weight = Quantity(Flt64.one, Kilogram),
                amount = UInt64(20)
            )
        },
        binTypes = listOf("small", "large").mapIndexed { index, id ->
            BinType(
                id = id,
                length = Quantity(Flt64(10), Meter),
                maxWeight = Quantity(Flt64(10), Kilogram),
                cost = Flt64(index + 3),
                inventory = UInt64(20)
            )
        },
        spacing = Quantity(Flt64.zero, Meter),
        objective = PackingObjective.Cost
    )

    private fun context(
        problem: PackingProblem<Flt64> = problem(),
        adapter: PackingValueAdapter<Flt64> = Flt64PackingValueAdapter,
        extensions: List<PatternModelingExtension<Flt64>> = emptyList()
    ) = PatternAssignmentContext(
        problem = problem,
        adapter = adapter,
        model = LinearMetaModel<Flt64>(name = "incremental-pattern", converter = IntoValue.Identity),
        stage = PackingModelStage.LP,
        extensions = extensions
    )

    private fun assertBase(context: PatternAssignmentContext<Flt64>) {
        val patterns = context.patterns
        fun assertTerms(actual: List<LinearMonomial<Flt64>>, expected: List<LinearMonomial<Flt64>>) {
            assertEquals(expected.map { it.coefficient.toDouble().toRawBits() }, actual.map { it.coefficient.toDouble().toRawBits() })
            assertEquals(expected.map { it.symbol }, actual.map { it.symbol })
        }
        for ((id, symbol) in context.aggregation.demands) {
            assertTerms(symbol.asMutable().monomials, patterns.map { pattern ->
                (pattern.amounts[id] ?: UInt64.zero).toFlt64() * context.aggregation.variables.getValue(pattern.key)
            })
        }
        for ((id, symbol) in context.aggregation.inventories) {
            assertTerms(symbol.asMutable().monomials, patterns.filter { it.binTypeId == id }.map { pattern ->
                Flt64.one * context.aggregation.variables.getValue(pattern.key)
            })
        }
        assertTerms(context.aggregation.objective.asMutable().monomials, patterns.map { pattern ->
            context.cost(pattern) * context.aggregation.variables.getValue(pattern.key)
        })
    }

    @Test
    fun `many small batches append only new terms and removal rebuilds then resumes appending`() {
        val context = context()
        assertIs<Ok<*, *, *>>(context.register(emptyList()))
        val columns = (1..6).map { amount -> PackingPattern<Flt64>("small", mapOf("a" to UInt64(amount))) }
        for (pattern in columns) {
            val previous = context.aggregation.demands.getValue("a").asMutable().monomials.firstOrNull()
            assertIs<Ok<*, *, *>>(context.addColumns(listOf(pattern)))
            if (previous != null) assertSame(previous, context.aggregation.demands.getValue("a").asMutable().monomials.first())
            assertBase(context)
        }
        val beforeRemoval = context.aggregation.demands.getValue("a").asMutable().monomials[1]
        assertIs<Ok<*, *, *>>(context.removeColumns(setOf(columns.first().key)))
        assertNotSame(beforeRemoval, context.aggregation.demands.getValue("a").asMutable().monomials.first())
        assertBase(context)
        val retained = context.aggregation.demands.getValue("a").asMutable().monomials.first()
        assertIs<Ok<*, *, *>>(context.addColumns(listOf(columns.first())))
        assertSame(retained, context.aggregation.demands.getValue("a").asMutable().monomials.first())
        assertBase(context)
        assertEquals(columns.size, context.model.objectiveSnapshot().flatMap { it.linearTerms() }.size)
        context.model.close()
    }

    @Test
    fun `built in adapters detect mutable history public symbols variables and bin costs`() {
        val bins = problem().binTypes.toMutableList()
        val context = context(problem = problem().copy(binTypes = bins))
        val amounts = linkedMapOf("a" to UInt64.one)
        val first = PackingPattern<Flt64>("small", amounts)
        assertIs<Ok<*, *, *>>(context.register(listOf(first)))
        val originalVariable = context.aggregation.variables.getValue(first.key)
        val demand = context.aggregation.demands.getValue("a")
        val term = demand.asMutable().monomials.first()
        amounts["a"] = UInt64(7)
        context.aggregation.variables[first.key] = originalVariable
        assertIs<Ok<*, *, *>>(context.addColumns(emptyList()))
        assertNotSame(term, demand.asMutable().monomials.first())
        assertBase(context)
        demand.asMutable() += Flt64(999) * originalVariable
        demand.asMutable().setConstant(Flt64(3))
        assertIs<Ok<*, *, *>>(context.addColumns(emptyList()))
        assertEquals(Flt64(3), demand.asMutable().constant)
        assertBase(context)
        val objectiveTerm = context.aggregation.objective.asMutable().monomials.first()
        bins[0] = bins[0].copy(cost = Flt64(8))
        assertIs<Ok<*, *, *>>(context.addColumns(emptyList()))
        assertNotSame(objectiveTerm, context.aggregation.objective.asMutable().monomials.first())
        assertBase(context)
        val second = PackingPattern<Flt64>("large", mapOf("b" to UInt64.one))
        assertIs<Ok<*, *, *>>(context.addColumns(listOf(second)))
        context.aggregation.demands["a"] = LinearExpressionSymbol(Flt64.zero, name = "replacement")
        context.aggregation.variables[first.key] = context.aggregation.variables.getValue(second.key)
        assertIs<Ok<*, *, *>>(context.addColumns(emptyList()))
        assertBase(context)
        context.model.close()
    }

    @Test
    fun `custom adapters retain complete reads and mutable history and expression behavior`() {
        var factor = Flt64.one
        var costReads = 0
        val adapter = object : PackingValueAdapter<Flt64> by Flt64PackingValueAdapter {
            override fun toSolver(value: Flt64): Flt64 {
                costReads++
                return value * factor
            }
        }
        val context = context(adapter = adapter)
        val amounts = linkedMapOf("a" to UInt64.one)
        val first = PackingPattern<Flt64>("small", amounts)
        assertIs<Ok<*, *, *>>(context.register(listOf(first)))
        val originalKey = first.key
        val originalVariable = context.aggregation.variables.getValue(originalKey)
        for (amount in 2..4) {
            costReads = 0
            assertIs<Ok<*, *, *>>(context.addColumns(listOf(PackingPattern("large", mapOf("b" to UInt64(amount))))))
            assertEquals(context.patterns.size + 1, costReads)
            assertBase(context)
        }
        val demand = context.aggregation.demands.getValue("a")
        val oldTerm = demand.asMutable().monomials.first()
        amounts["a"] = UInt64(7)
        context.aggregation.variables[first.key] = originalVariable
        assertIs<Ok<*, *, *>>(context.addColumns(emptyList()))
        assertNotSame(oldTerm, demand.asMutable().monomials.first())
        assertBase(context)
        demand.asMutable() += Flt64(999) * originalVariable
        demand.asMutable().setConstant(Flt64(3))
        assertIs<Ok<*, *, *>>(context.addColumns(emptyList()))
        assertEquals(Flt64(3), demand.asMutable().constant)
        assertBase(context)
        val oldObjectiveTerm = context.aggregation.objective.asMutable().monomials.first()
        factor = Flt64.two
        assertIs<Ok<*, *, *>>(context.addColumns(emptyList()))
        assertNotSame(oldObjectiveTerm, context.aggregation.objective.asMutable().monomials.first())
        assertBase(context)
        context.aggregation.demands["a"] = LinearExpressionSymbol(Flt64.zero, name = "replacement")
        context.aggregation.variables[first.key] = context.aggregation.variables.getValue(context.patterns.last().key)
        assertIs<Ok<*, *, *>>(context.addColumns(emptyList()))
        assertBase(context)
        context.model.close()
    }

    @Test
    fun `throwing custom adapter retains legacy partial update and recovery boundary`() {
        var throwOnSecond = false
        var reads = 0
        val adapter = object : PackingValueAdapter<Flt64> by Flt64PackingValueAdapter {
            override fun toSolver(value: Flt64): Flt64 {
                if (throwOnSecond && ++reads == 2) throw IllegalStateException("test conversion failure")
                return value
            }
        }
        val context = context(adapter = adapter)
        val amounts = linkedMapOf("a" to UInt64.one)
        val first = PackingPattern<Flt64>("small", amounts)
        val second = PackingPattern<Flt64>("large", mapOf("b" to UInt64.one))
        assertIs<Ok<*, *, *>>(context.register(listOf(first, second)))
        val variable = context.aggregation.variables.getValue(first.key)
        amounts["a"] = UInt64(7)
        context.aggregation.variables[first.key] = variable
        val oldObjective = context.aggregation.objective.asMutable().monomials
        throwOnSecond = true
        assertFailsWith<IllegalStateException> { context.addColumns(emptyList()) }
        assertEquals(Flt64(7), context.aggregation.demands.getValue("a").asMutable().monomials.first().coefficient)
        assertEquals(oldObjective, context.aggregation.objective.asMutable().monomials)
        throwOnSecond = false
        assertIs<Ok<*, *, *>>(context.addColumns(emptyList()))
        assertBase(context)
        context.model.close()
    }

    @Test
    fun `extension refresh and partial registration failures do not accumulate stale or duplicate terms`() {
        var refreshes = 0
        var failNext = false
        val extension = object : PatternModelingExtension<Flt64> {
            override fun register(context: PatternAssignmentContext<Flt64>): Try = ok

            override fun refresh(context: PatternAssignmentContext<Flt64>): Try {
                refreshes++
                assertBase(context)
                val variable = context.aggregation.variables.values.first()
                context.aggregation.demands.getValue("a").asMutable() += Flt64(123) * variable
                return if (failNext) Failed(ErrorCode.ApplicationError, "expected test failure") else ok
            }
        }
        val context = context(extensions = listOf(extension))
        val first = PackingPattern<Flt64>("small", mapOf("a" to UInt64.one))
        val second = PackingPattern<Flt64>("small", mapOf("a" to UInt64.two))
        assertIs<Ok<*, *, *>>(context.register(listOf(first)))
        assertIs<Failed<*, *, *>>(context.addColumns(listOf(second, PackingPattern("missing", mapOf("a" to UInt64.one)))))
        assertEquals(1, context.aggregation.demands.getValue("a").asMutable().monomials.size)
        failNext = true
        assertIs<Failed<*, *, *>>(context.addColumns(emptyList()))
        failNext = false
        assertIs<Ok<*, *, *>>(context.addColumns(emptyList()))
        assertEquals(2, refreshes)
        assertEquals(3, context.aggregation.demands.getValue("a").asMutable().monomials.size)
        assertEquals(2, context.model.objectiveSnapshot().flatMap { it.linearTerms() }.size)
        context.model.close()
    }
}
