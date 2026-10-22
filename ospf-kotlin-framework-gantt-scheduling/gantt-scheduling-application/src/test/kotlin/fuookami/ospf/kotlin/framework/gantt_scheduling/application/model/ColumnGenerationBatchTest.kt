package fuookami.ospf.kotlin.framework.gantt_scheduling.application.model

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.framework.model.ColumnGenerationBatch
import fuookami.ospf.kotlin.framework.model.ColumnGenerationCompletion
import fuookami.ospf.kotlin.framework.model.ColumnGenerationScope

class ColumnGenerationBatchTest {
    private val scope = ColumnGenerationScope(
        request = "run-7:pricing-4",
        group = "aircraft-1|aircraft-2",
        branch = "fixed=aircraft-3|hidden=aircraft-4",
        global = true,
        model = "run-7",
        dual = "lp-iteration-12",
        executorSlots = setOf("aircraft-1@slot-2", "aircraft-2@slot-2")
    )

    @Test
    fun exhaustedEmptyBatchCertifiesOnlyItsExactFullScope() {
        val batch = ColumnGenerationBatch<String>(
            columns = emptyList(),
            completion = ColumnGenerationCompletion.Exhausted,
            scope = scope,
            noImprovingColumns = true
        )

        assertTrue(batch.certifies(scope))
        assertTrue(batch.certifiesNoImprovingColumns(scope))
        assertFalse(batch.certifies(scope.copy(request = "run-7:pricing-5")))
        assertFalse(batch.certifies(scope.copy(model = "run-8")))
        assertFalse(batch.certifies(scope.copy(dual = "lp-iteration-13")))
        assertFalse(batch.certifies(scope.copy(branch = "root")))
        assertFalse(batch.certifies(scope.copy(group = "aircraft-1")))
        assertFalse(batch.certifies(scope.copy(executorSlots = setOf("aircraft-1@slot-3"))))
        assertFalse(batch.certifies(scope.copy(global = false)))
    }

    @Test
    fun missingModelOrPricingIdentityCannotCertifyEvenMatchingScopes() {
        val incompleteScopes = listOf(
            scope.copy(request = ""),
            scope.copy(model = ""),
            scope.copy(dual = " "),
            scope.copy(branch = "")
        )
        for (incomplete in incompleteScopes) {
            val batch = ColumnGenerationBatch<String>(
                columns = emptyList(),
                completion = ColumnGenerationCompletion.Exhausted,
                scope = incomplete,
                noImprovingColumns = true
            )
            assertFalse(batch.certifies(incomplete))
            assertFalse(batch.certifiesNoImprovingColumns(incomplete))
        }
    }

    @Test
    fun emptyOutputDoesNotInferNoImprovementWithoutExplicitEvidence() {
        val batch = ColumnGenerationBatch<String>(
            columns = emptyList(),
            completion = ColumnGenerationCompletion.Exhausted,
            scope = scope
        )

        assertTrue(batch.certifies(scope))
        assertFalse(batch.certifiesNoImprovingColumns(scope))
    }

    @Test
    fun partialOrHeuristicBatchesCannotCertifyNoImprovingColumns() {
        val budgetLimited = ColumnGenerationBatch<String>(
            columns = emptyList(),
            completion = ColumnGenerationCompletion.BudgetLimit,
            scope = scope,
            noImprovingColumns = true
        )
        val heuristic = ColumnGenerationBatch<String>(
            columns = emptyList(),
            completion = ColumnGenerationCompletion.Heuristic,
            scope = scope,
            noImprovingColumns = true
        )
        val hasCandidates = ColumnGenerationBatch(
            columns = listOf("candidate"),
            completion = ColumnGenerationCompletion.Exhausted,
            scope = scope
        )
        val contradictoryOutputWithIndependentProof = ColumnGenerationBatch(
            columns = listOf("non-improving candidate"),
            completion = ColumnGenerationCompletion.Exhausted,
            scope = scope,
            noImprovingColumns = true
        )

        assertFalse(budgetLimited.certifiesNoImprovingColumns(scope))
        assertFalse(heuristic.certifiesNoImprovingColumns(scope))
        assertFalse(hasCandidates.certifiesNoImprovingColumns(scope))
        assertTrue(contradictoryOutputWithIndependentProof.certifiesNoImprovingColumns(scope))
    }

    @Test
    fun aggregateEvidenceCannotOverrideAnIncompleteExecutorOrSlotResult() {
        val firstScope = scope.copy(
            request = "${scope.request}:aircraft-1",
            group = "aircraft-1",
            executorSlots = setOf("aircraft-1@slot-2")
        )
        val secondScope = scope.copy(
            request = "${scope.request}:aircraft-2",
            group = "aircraft-2",
            executorSlots = setOf("aircraft-2@slot-2")
        )
        val first = ColumnGenerationBatch<String>(
            columns = emptyList(),
            completion = ColumnGenerationCompletion.Exhausted,
            scope = firstScope,
            noImprovingColumns = true
        )
        val incomplete = ColumnGenerationBatch<String>(
            columns = emptyList(),
            completion = ColumnGenerationCompletion.BudgetLimit,
            scope = secondScope,
            noImprovingColumns = true
        )
        val aggregate = ColumnGenerationBatch(
            columns = emptyList(),
            completion = ColumnGenerationCompletion.Exhausted,
            scope = scope,
            noImprovingColumns = true,
            children = listOf(first, incomplete)
        )

        assertFalse(aggregate.certifies(scope))
        assertFalse(aggregate.certifiesNoImprovingColumns(scope))
    }

    @Test
    fun fullyCertifiedChildrenCanSupportAnAggregateCertificate() {
        val firstScope = scope.copy(
            request = "${scope.request}:aircraft-1",
            group = "aircraft-1",
            executorSlots = setOf("aircraft-1@slot-2")
        )
        val secondScope = scope.copy(
            request = "${scope.request}:aircraft-2",
            group = "aircraft-2",
            executorSlots = setOf("aircraft-2@slot-2")
        )
        val children = listOf(firstScope, secondScope).map { childScope ->
            ColumnGenerationBatch<String>(
                columns = emptyList(),
                completion = ColumnGenerationCompletion.Exhausted,
                scope = childScope,
                noImprovingColumns = true
            )
        }
        val aggregate = ColumnGenerationBatch(
            columns = emptyList(),
            completion = ColumnGenerationCompletion.Exhausted,
            scope = scope,
            noImprovingColumns = true,
            children = children
        )

        assertTrue(aggregate.certifies(scope))
        assertTrue(aggregate.certifiesNoImprovingColumns(scope))
    }
}
