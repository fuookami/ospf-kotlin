package fuookami.ospf.kotlin.core.token

import kotlin.test.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.variable.*

class TokenSolverIndexTest {
    @Test
    fun coldDistinctQueriesBuildOneLinearIndexAndIgnoreSolverIndexGaps() {
        val variables = List(256) { CountingVariable(IdentifierGenerator.gen()) }
        val tokens = variables.mapIndexed { index, variable ->
            Token<Flt64>(
                variable = variable,
                solverIndex = (variables.size - index) * 3,
                refreshCallbacks = mutableMapOf()
            )
        }
        val list = TokenList(tokens.associateBy { it.key })
        try {
            val ordered = list.tokensInSolver
            variables.forEach { it.reads = 0 }
            for ((index, token) in ordered.withIndex()) {
                assertEquals(index, list.indexOf(token))
                assertEquals(index, list.indexOf(token.copy(solverIndex = -1)))
            }
            assertTrue(variables.fold(0) { count, variable -> count + variable.reads } < 16 * variables.size)
            assertNull(list.indexOf(Token<Flt64>(
                variable = RealVar("missing"),
                solverIndex = 0,
                refreshCallbacks = mutableMapOf()
            )))
        } finally {
            list.close()
        }
    }

    @Test
    fun structuralChangesAndRollbackInvalidatePositionsButNotImmutableCopies() {
        val list = ManualTokenList<Flt64>(checkTokenExisted = true)
        val variables = List(4) { RealVar("v$it") }
        assertTrue(list.add(variables.take(3)) is Ok)
        val copy = TokenList(list)
        try {
            assertEquals(2, list.indexOf(variables[2]))
            list.remove(variables[0])
            assertEquals(1, list.indexOf(variables[2]))
            assertEquals(2, list.find(variables[2])?.solverIndex)
            assertEquals(2, copy.indexOf(variables[2]))
            assertTrue(list.add(listOf(variables[3], variables[2])) is Failed)
            assertNull(list.indexOf(variables[3]))
            assertEquals(1, list.indexOf(variables[2]))
            assertTrue(list.add(variables[3]) is Ok)
            assertEquals(2, list.indexOf(variables[3]))
        } finally {
            copy.close()
            list.close()
        }
    }

    private class CountingVariable(private val id: UInt64) : AbstractVariableItem<Flt64, Continuous>(
        type = Continuous,
        name = "counted_$id",
        constants = Flt64
    ) {
        var reads = 0
        override val identifier: UInt64
            get() {
                ++reads
                return id
            }
        override val dimension = 0
        override val index = 0
        override val vectorView = intArrayOf(0)
    }
}
