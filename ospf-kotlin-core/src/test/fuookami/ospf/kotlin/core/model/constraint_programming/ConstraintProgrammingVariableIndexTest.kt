package fuookami.ospf.kotlin.core.model.constraint_programming

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingCheckpointCodec
import fuookami.ospf.kotlin.core.solver.constraint_programming.FakeConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.VariableId

/** 批量变量查找保持首匹配语义且只遍历一次输入。 / Batch lookups preserve first-match semantics and traverse input once. */
class ConstraintProgrammingVariableIndexTest {
    @Test
    fun batchIndexPreservesFirstDuplicateAndRegistrationOrder() {
        val first = variable("same")
        val second = first.copy(name = "second")
        val last = variable("last")
        val snapshot = snapshot(listOf(first, second, last))
        val indexed = snapshot.indexVariables()
        assertSame(first, indexed[first.id])
        assertEquals(listOf(first.id, last.id), indexed.keys.toList())
        assertNull(indexed[VariableId("missing")])
        assertSame(snapshot.variable(first.id), indexed[first.id])
    }

    @Test
    fun batchIndexesDoNotCacheExternallyMutableInputsAcrossCallsOrCopies() {
        val values = mutableListOf(variable("old"))
        val snapshot = snapshot(values)
        val old = snapshot.indexVariables()
        values.clear()
        values.add(variable("new"))
        assertTrue(VariableId("old") in old)
        assertNull(snapshot.indexVariables()[VariableId("old")])
        assertNotNull(snapshot.indexVariables()[VariableId("new")])
        val copy = snapshot.copy(variables = listOf(variable("copy")))
        assertEquals(setOf(VariableId("copy")), copy.indexVariables().keys)
    }

    @Test
    fun checkpointCaptureAndRestoreTraverseVariablesLinearly() {
        val variables = CountingList(List(256) { variable("v$it") })
        val snapshot = snapshot(variables)
        val values = variables.associate { it.id to Int64.one }
        variables.reads = 0
        val captured = ConstraintProgrammingCheckpointCodec.capture(
            snapshot = snapshot,
            descriptor = FakeConstraintProgrammingSolver().descriptor,
            checkpointId = "indexed-incumbent",
            createdAtEpochMs = 0L,
            incumbent = ConstraintProgrammingSolution(values = values)
        )
        val envelope = assertNotNull(captured.value, captured.toString())
        assertTrue(variables.reads < variables.size * 32, "capture reads: ${variables.reads}")
        variables.reads = 0
        val restored = ConstraintProgrammingCheckpointCodec.restore(
            envelope = envelope,
            snapshot = snapshot,
            expectedConfigurationFingerprint = null,
            expectedSolverFingerprint = envelope.solverFingerprint,
            allowLegacyConfigurationFingerprint = true
        )
        assertEquals(values, assertNotNull(restored.value, restored.toString()).incumbent!!.values)
        assertTrue(variables.reads < variables.size * 32, "restore reads: ${variables.reads}")
    }

    @Test
    fun checkpointStillRejectsUnknownMissingAndOutOfDomainValues() {
        val snapshot = snapshot(listOf(variable("value")))
        for (values in listOf(
            emptyMap(),
            mapOf(VariableId("unknown") to Int64.one),
            mapOf(VariableId("value") to Int64(2))
        )) {
            val result = ConstraintProgrammingCheckpointCodec.capture(
                snapshot = snapshot,
                descriptor = FakeConstraintProgrammingSolver().descriptor,
                checkpointId = "invalid-incumbent",
                createdAtEpochMs = 0L,
                incumbent = ConstraintProgrammingSolution(values = values)
            )
            assertTrue(result.failed)
        }
    }

    private fun variable(id: String) = ConstraintProgrammingVariableSnapshot(
        id = VariableId(id),
        name = id,
        typeName = "Integer",
        domain = IntegerDomain.boolean
    )

    private fun snapshot(variables: List<ConstraintProgrammingVariableSnapshot>) = ConstraintProgrammingModelSnapshot(
        name = "indexed-snapshot",
        objectCategory = ObjectCategory.Minimum,
        variables = variables,
        intervals = emptyList(),
        expressions = emptyList(),
        constraints = emptyList(),
        objectives = emptyList(),
        constraintGroups = emptyList()
    )

    private class CountingList<T>(private val values: List<T>) : AbstractList<T>() {
        var reads = 0
        override val size get() = values.size

        override fun get(index: Int): T {
            reads++
            return values[index]
        }
    }
}
