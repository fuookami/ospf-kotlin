package fuookami.ospf.kotlin.core.token

import kotlin.test.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.variable.RealVar

class TokenListAtomicityTest {
    @Test
    fun batchAddRollsBackAfterMiddleFailureForAutoAndManualLists() {
        for (auto in listOf(true, false)) {
            val tokenList = createTokenList(auto, checkTokenExisted = true)
            try {
                val existing = RealVar("existing_${auto}")
                val first = RealVar("first_${auto}")
                val last = RealVar("last_${auto}")

                assertTrue(tokenList.add(existing) is Ok)
                val existingToken = tokenList.find(existing)
                assertNotNull(existingToken)
                val existingCallback = existingToken.refreshCallbacks[tokenList]
                tokenList.setSolverSolution(mapOf(existing to Flt64.one))
                assertTrue(tokenList.cachedSolution)
                assertEquals(0, tokenList.indexOf(existingToken))
                assertEquals(listOf(existingToken), tokenList.tokensInSolver)

                val result = tokenList.add(listOf(first, existing, last))

                assertTrue(result is Failed, "${if (auto) "Auto" else "Manual"} list should fail on duplicate")
                assertEquals(1, tokenList.tokens.size)
                assertSame(existingToken, tokenList.find(existing))
                assertEquals(existingCallback, existingToken.refreshCallbacks[tokenList])
                assertTrue(tokenList.cachedSolution)
                assertEquals(0, tokenList.indexOf(existingToken))
                assertEquals(listOf(existingToken), tokenList.tokensInSolver)
                assertNull(tokenList.list[first.key])
                assertNull(tokenList.list[last.key])

                val next = RealVar("next_${auto}")
                assertTrue(tokenList.add(next) is Ok)
                assertEquals(1, tokenList.find(next)?.solverIndex)
            } finally {
                tokenList.close()
            }
        }
    }

    @Test
    fun batchAddWorksForAutoAndManualListsWhenExistenceCheckIsDisabled() {
        for (auto in listOf(true, false)) {
            val tokenList = createTokenList(auto, checkTokenExisted = false)
            try {
                val existing = RealVar("unchecked_existing_${auto}")
                val first = RealVar("unchecked_first_${auto}")
                val last = RealVar("unchecked_last_${auto}")

                assertTrue(tokenList.add(existing) is Ok)
                val existingToken = tokenList.find(existing)
                assertNotNull(existingToken)
                tokenList.setSolverSolution(mapOf(existing to Flt64.one))
                assertTrue(tokenList.cachedSolution)

                val result = tokenList.add(listOf(first, existing, last))

                assertTrue(result is Ok)
                assertEquals(3, tokenList.tokens.size)
                assertEquals(listOf(1, 2, 3), tokenList.tokensInSolver.map { it.solverIndex })
                val replacement = tokenList.list[existing.key]
                assertNotNull(replacement)
                assertNotSame(existingToken, replacement)
                assertNull(existingToken.refreshCallbacks[tokenList])
                assertNotNull(replacement.refreshCallbacks[tokenList])
                assertFalse(tokenList.cachedSolution)

                val next = RealVar("unchecked_next_${auto}")
                assertTrue(tokenList.add(next) is Ok)
                assertEquals(4, tokenList.find(next)?.solverIndex)
            } finally {
                tokenList.close()
            }
        }
    }

    private fun createTokenList(
        auto: Boolean,
        checkTokenExisted: Boolean
    ): MutableTokenList<Flt64> {
        return if (auto) {
            AutoTokenList(checkTokenExisted)
        } else {
            ManualTokenList(checkTokenExisted)
        }
    }
}
