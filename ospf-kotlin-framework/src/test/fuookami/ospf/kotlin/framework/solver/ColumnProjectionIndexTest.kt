package fuookami.ospf.kotlin.framework.solver

import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.variable.*

class ColumnProjectionIndexTest {
    @Test
    fun sparseTermsKeepDecisionOrderAndQueriesDoNotReadSourceMaps() {
        val variables = List(256) { BinVar("v$it") }
        val decisions = variables.mapIndexed { index, variable ->
            ColumnDecision(id = "d$index", blockId = "b${index / 64}", variable = variable)
        }
        var reads = 0
        val values = linkedMapOf<AbstractVariableItem<*, *>, Flt64>(
            variables[0] to Flt64(1e16),
            variables[2] to Flt64(-1e16),
            variables[1] to Flt64.one
        )
        val counted = object : Map<AbstractVariableItem<*, *>, Flt64> by values {
            override val entries: Set<Map.Entry<AbstractVariableItem<*, *>, Flt64>>
                get() {
                    ++reads
                    return values.entries
                }
        }
        val row = LinearModelRow(
            id = "ordered",
            sourceName = "ordered",
            coefficients = counted,
            rhs = Flt64(1e20),
            comparison = Comparison.LE
        )
        val empty = row.copy(id = "empty", coefficients = emptyMap())
        val mixed = row.copy(id = "mixed", coefficients = mapOf(variables[0] to Flt64.one, variables[65] to Flt64.one))
        val global = row.copy(id = "global", coefficients = mapOf(RealVar("global") to Flt64.one))
        val snapshot = LinearModelSnapshot(
            revision = LinearModelRevision(1, 1),
            variables = emptyList(),
            rows = listOf(row, empty, mixed, global),
            objective = counted,
            objectiveConstant = Flt64.zero,
            objectCategory = ObjectCategory.Minimum
        )
        val index = ColumnProjectionIndex(
            decisions = decisions,
            blockIds = setOf("b0", "b1", "b2", "b3", "unused"),
            snapshot = snapshot
        )
        val initialReads = reads
        assertTrue(index.initializedTermIndexes().isEmpty())
        values.clear()
        values[variables[3]] = Flt64.two
        repeat(100) {
            val terms = index.rowTerms.getValue("ordered").getValue("b0")
            assertEquals(listOf("d0", "d1", "d2"), terms.map { it.first.id })
            assertEquals(Flt64.zero, terms.fold(Flt64.zero) { sum, term -> sum + term.second })
            assertEquals(listOf("ordered", "empty"), index.localRows.getValue("b0").map { it.id })
            assertEquals(listOf("empty"), index.localRows.getValue("b1").map { it.id })
            assertEquals(listOf("empty"), index.localRows.getValue("unused").map { it.id })
        }
        assertEquals(setOf("rows"), index.initializedTermIndexes())
        assertEquals(listOf("d0", "d1", "d2"), index.objectiveTerms.getValue("b0").map { it.first.id })
        assertEquals(setOf("rows", "objective"), index.initializedTermIndexes())
        assertEquals(initialReads, reads)
    }

    @Test
    fun concurrentFirstTermReadersShareTheCapturedRevision() {
        val variable = BinVar("v")
        val coefficients = mutableMapOf<AbstractVariableItem<*, *>, Flt64>(variable to Flt64.one)
        val index = ColumnProjectionIndex(
            decisions = listOf(ColumnDecision(id = "d", blockId = "b", variable = variable)),
            blockIds = setOf("b"),
            snapshot = LinearModelSnapshot(
                revision = LinearModelRevision(11, 3),
                variables = emptyList(),
                rows = listOf(LinearModelRow(
                    id = "r",
                    sourceName = "r",
                    coefficients = coefficients,
                    rhs = Flt64.one,
                    comparison = Comparison.LE
                )),
                objective = coefficients,
                objectiveConstant = Flt64.zero,
                objectCategory = ObjectCategory.Minimum
            )
        )
        coefficients.clear()
        val pool = Executors.newFixedThreadPool(8)
        val barrier = CyclicBarrier(8)
        try {
            val results = pool.invokeAll(List(8) { Callable {
                barrier.await(10, TimeUnit.SECONDS)
                index.rowTerms to index.objectiveTerms
            } }, 30, TimeUnit.SECONDS).map { it.get() }
            results.forEach {
                assertSame(results.first().first, it.first)
                assertSame(results.first().second, it.second)
            }
            assertEquals(Flt64.one, index.rowTerms.getValue("r").getValue("b").single().second)
            assertEquals(Flt64.one, index.objectiveTerms.getValue("b").single().second)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun sparseMasterMatchesFullDecisionFoldWithRefinedResourcesAndLateLocalRows(): Unit = runBlocking {
        val source = LinearMetaModel(name = "sparse_source")
        val variables = List(16) { BinVar("v$it") }
        val global = RealVar("global")
        val decisions = variables.mapIndexed { index, variable ->
            ColumnDecision(id = "d$index", blockId = "b${index % 4}", variable = variable)
        }
        val columns = (0 until 4).flatMap { block ->
            List(3) { column ->
                ProjectedColumn(
                    id = "c${block}_$column",
                    blockId = "b$block",
                    values = decisions.filter { it.blockId == "b$block" }.associate { it.id to Flt64(column % 2) }
                )
            }
        }
        try {
            assertTrue(source.add(variables) is Ok)
            assertTrue(source.add(global) is Ok)
            val random = kotlin.random.Random(439)
            repeat(12) { row ->
                val terms = variables.shuffled(random).take(if (row % 3 == 0) 16 else 3)
                    .map { LinearMonomial(Flt64(random.nextInt(-3, 4)), it) }
                assertTrue(source.addConstraint(
                    relation = LinearPolynomial(terms, Flt64.zero) leq Flt64(100),
                    name = "r$row"
                ) is Ok)
            }
            assertTrue(source.minimize(LinearPolynomial(variables.map { LinearMonomial(Flt64.one, it) }, Flt64(3))) is Ok)
            val projection = ColumnProjection(
                source = source,
                decisions = decisions,
                blockIds = setOf("b0", "b1", "b2", "b3")
            )
            val original = assertNotNull(projection.snapshot().value)
            for (refined in listOf(emptySet(), setOf<AbstractVariableItem<*, *>>(variables[0], variables[1]))) {
                val master = assertNotNull(projection.master(
                    columns = columns,
                    phaseOne = false,
                    integer = false,
                    refinedResources = refined
                ).value)
                try {
                    val actual = assertNotNull(master.model.state.refresh().value)
                    for (row in original.rows) {
                        val projected = actual.rows.single { it.sourceName == row.id }
                        for (column in columns) {
                            val expected = decisions.filter { it.blockId == column.blockId && it.variable !in refined }
                                .fold(Flt64.zero) { sum, decision ->
                                    sum + (row.coefficients[decision.variable] ?: Flt64.zero) * column.values.getValue(decision.id)
                                }
                            assertEquals(expected, projected.coefficients[master.columnVariables.getValue(column.id)] ?: Flt64.zero)
                        }
                    }
                } finally {
                    master.model.close()
                }
            }
            projection.columnsChanged()
            val before = assertNotNull(projection.pricingRequest(blockId = "b0", phaseOne = false, seed = true).value)
            val localVariables = decisions.filter { it.blockId == "b0" }.map { it.variable }.toSet()
            val expectedLocal = original.rows.filter { localVariables.containsAll(it.coefficients.keys) }.map { it.id }
            projection.pricingModel(before).value!!.use { model ->
                assertEquals(expectedLocal, model.state.refresh().value!!.rows.map { it.sourceName })
            }
            assertTrue(source.state.registerRow(ConstraintId("late-local"), LinearPolynomial(variables[0]) leq Flt64.one) is Ok)
            assertFalse(projection.pricingModel(before).ok)
            val after = assertNotNull(projection.pricingRequest(blockId = "b0", phaseOne = false, seed = true).value)
            projection.pricingModel(after).value!!.use { model ->
                assertTrue(model.state.refresh().value!!.rows.any { it.sourceName == "late-local" })
            }
        } finally {
            source.close()
        }
    }

    @Test
    fun pricingBatchSharesRequestsAndModelsButMustBeRevalidatedAfterSourceEdits(): Unit = runBlocking {
        val source = LinearMetaModel(name = "pricing_batch")
        val first = BinVar("first")
        val second = BinVar("second")
        try {
            assertTrue(source.add(listOf(first, second)) is Ok)
            assertTrue(source.minimize(LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(Flt64.one, first),
                    LinearMonomial(Flt64(2), second)
                ),
                constant = Flt64.zero
            )) is Ok)
            val projection = ColumnProjection(
                source = source,
                decisions = listOf(
                    ColumnDecision(
                        id = "first",
                        blockId = "b0",
                        variable = first
                    ),
                    ColumnDecision(
                        id = "second",
                        blockId = "b1",
                        variable = second
                    )
                ),
                blockIds = setOf("b0", "b1")
            )
            val batch = assertNotNull(projection.preparePricingBatch(
                blockIds = listOf("b1", "b0"),
                phaseOne = false,
                seed = true
            ).value)
            assertEquals(listOf("b1", "b0"), batch.entries.map { it.request.blockId })
            assertTrue(batch.entries.all { it.request.revision == batch.revision })
            for (entry in batch.entries) {
                val local = assertNotNull(entry.model.state.refresh().value)
                assertEquals(emptyList(), local.rows)
            }
            assertTrue(projection.requireCurrent(batch) is Ok)

            val scalarRequest = assertNotNull(projection.pricingRequest(
                blockId = "b0",
                phaseOne = false,
                seed = true
            ).value)
            val scalarModel = assertNotNull(projection.pricingModel(scalarRequest).value)
            val extension = BinVar("extension")
            assertTrue(scalarModel.add(extension) is Ok)
            assertTrue(scalarModel.addConstraint(
                relation = LinearPolynomial(extension) leq Flt64.one,
                name = "extension_limit"
            ) is Ok)
            assertTrue(scalarModel.state.refresh().value!!.rows.any { it.sourceName == "extension_limit" })
            scalarModel.close()

            yield()
            assertTrue(source.state.registerRow(
                ConstraintId("external_after_pricing"),
                LinearPolynomial(first) leq Flt64.one
            ) is Ok)
            assertFalse(projection.requireCurrent(batch).ok)
            batch.entries.forEach { it.model.close() }
        } finally {
            source.close()
        }
    }
}
