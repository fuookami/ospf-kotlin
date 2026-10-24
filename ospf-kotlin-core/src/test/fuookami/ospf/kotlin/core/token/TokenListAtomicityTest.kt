package fuookami.ospf.kotlin.core.token

import kotlin.test.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.variable.RealVar

class TokenListAtomicityTest {
    @Test
    fun checkedBatchRestoresNewKeyJournalAfterDuplicateOrIteratorFailure() {
        for (auto in listOf(false, true)) {
            for (duplicate in listOf(false, true)) {
                val list = createTokenList(auto, checkTokenExisted = true)
                try {
                    val existing = RealVar("checked-existing")
                    val added = RealVar("checked-added")
                    assertTrue(list.add(existing) is Ok)
                    val original = assertNotNull(list.find(existing))
                    original.setResult(Flt64.one)
                    val callback = original.refreshCallbacks[list]
                    var temporary: Token<Flt64>? = null
                    val broken = Iterable {
                        object : Iterator<RealVar> {
                            var position = 0
                            override fun hasNext(): Boolean = true
                            override fun next(): RealVar {
                                if (position++ == 0) return added
                                temporary = list.list[added.key]
                                if (duplicate) return added
                                error("expected iterator failure")
                            }
                        }
                    }
                    assertTrue(list.add(broken) is Failed)
                    assertSame(original, list.list[existing.key])
                    assertSame(callback, original.refreshCallbacks[list])
                    assertNull(list.list[added.key])
                    assertNull(assertNotNull(temporary).refreshCallbacks[list])
                    assertTrue(list.cachedSolution)
                    assertEquals(listOf(original), list.tokensInSolver)
                    assertTrue(list.add(added) is Ok)
                    assertEquals(1, list.find(added)?.solverIndex)
                    assertEquals(1, list.indexOf(assertNotNull(list.find(added))))
                } finally {
                    list.close()
                }
            }
        }
    }

    @Test
    fun runtimeFailureRestoresRepeatedReplacementsAndCallbacks() {
        for (auto in listOf(false, true)) {
            val list = createTokenList(auto, checkTokenExisted = false)
            val existing = RealVar("replace")
            val added = RealVar("added")
            assertTrue(list.add(existing) is Ok)
            val token = assertNotNull(list.find(existing))
            token.setResult(Flt64.one)
            val callback = token.refreshCallbacks[list]
            val broken = Iterable {
                object : Iterator<RealVar> {
                    var position = 0
                    override fun hasNext(): Boolean = true
                    override fun next(): RealVar {
                        return when (position++) {
                            0, 1 -> existing
                            2 -> added
                            else -> error("expected iterator failure")
                        }
                    }
                }
            }
            try {
                assertTrue(list.add(broken) is Failed)
                assertEquals(1, list.tokens.size)
                assertSame(token, list.list[existing.key])
                assertSame(callback, token.refreshCallbacks[list])
                assertEquals(Flt64.one, token.result)
                assertTrue(list.cachedSolution)
                assertNull(list.list[added.key])
                assertTrue(list.add(added) is Ok)
                assertEquals(1, list.find(added)?.solverIndex)
            } finally {
                list.close()
            }
        }
    }

    @Test
    fun addingNewKeysDoesNotSnapshotUnrelatedCallbacks() {
        val callbacks = List(128) { CountingCallbacks() }
        val sourceTokens = callbacks.mapIndexed { index, map ->
            Token<Flt64>(
                variable = RealVar("source_$index"),
                solverIndex = index,
                refreshCallbacks = map
            )
        }
        val source = TokenList(sourceTokens.associateBy { it.key })
        val list = ManualTokenList(source, checkTokenExisted = false)
        try {
            callbacks.forEach { it.reads = 0 }
            repeat(128) { assertTrue(list.add(RealVar("new_$it")) is Ok) }
            assertEquals(0, callbacks.fold(0) { count, map -> count + map.reads })
            assertEquals(256, list.tokens.size)
        } finally {
            list.close()
            source.close()
        }
    }

    private class CountingCallbacks : HashMap<AbstractTokenList<Flt64>, (Boolean) -> Unit>() {
        var reads = 0
        override fun get(key: AbstractTokenList<Flt64>): ((Boolean) -> Unit)? {
            ++reads
            return super.get(key)
        }
    }

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
