package fuookami.ospf.kotlin.core.token

import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.variable.*

class TokenColdReadConcurrencyTest {
    @Test
    fun observeSimultaneousFirstReadsOfOneFrozenStructure() {
        val reads = AtomicInteger()
        val variables = List(4096) { CountingVariable(IdentifierGenerator.gen(), reads) }
        val tokens = variables.mapIndexed { index, variable -> Token<Flt64>(
            variable = variable,
            solverIndex = index * 3,
            refreshCallbacks = mutableMapOf()
        ) }
        val owner = TokenList(tokens.associateBy { it.key })
        val ordered = owner.tokensInSolver
        reads.set(0)
        val pool = Executors.newFixedThreadPool(8)
        val start = CyclicBarrier(8)
        try {
            val results = pool.invokeAll(List(8) { Callable {
                start.await(10, TimeUnit.SECONDS)
                owner.indexOf(ordered.last())
            } }, 30, TimeUnit.SECONDS).map { it.get() }
            assertEquals(List(8) { tokens.lastIndex }, results)
            println("TOKEN_COLD_OBSERVATION tokens=${tokens.size} readers=8 identifierReads=${reads.get()}")
            assertTrue(reads.get() <= tokens.size + 16, "Only one cold position index should read the frozen token keys")
        } finally {
            pool.shutdownNow()
            owner.close()
        }
    }

    private class CountingVariable(
        private val id: UInt64,
        private val reads: AtomicInteger
    ) : AbstractVariableItem<Flt64, Continuous>(
        type = Continuous,
        name = "cold_$id",
        constants = Flt64
    ) {
        override val identifier: UInt64 get() {
            reads.incrementAndGet()
            return id
        }
        override val dimension = 0
        override val index = 0
        override val vectorView = intArrayOf(0)
    }
}
