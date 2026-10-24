package fuookami.ospf.kotlin.core.token

import java.util.concurrent.ConcurrentHashMap
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*

class DependencyLayersTest {
    @Test
    fun stableLayersMatchFullScanReferenceForRandomGraphsCyclesAndMissingDependencies() {
        val random = kotlin.random.Random(17)
        repeat(80) {
            val graph = (0 until 40).shuffled(random).associateWith { symbol ->
                (0 until 40).filter { random.nextInt(100) < 3 && it != symbol }.toSet() +
                    if (symbol == 5) setOf(-1) else emptySet()
            }
            val remaining = graph.toMutableMap()
            val completed = mutableSetOf<Int>()
            val reference = mutableListOf<List<Int>>()
            while (true) {
                val ready = remaining.filterValues { completed.containsAll(it) }.keys.toList()
                if (ready.isEmpty()) break
                reference.add(ready)
                completed.addAll(ready)
                ready.forEach { remaining.remove(it) }
            }
            assertEquals(reference, buildDependencyLayers(graph))
        }
    }

    @Test
    fun longChainReadsEachDependencyOnlyOnce() {
        var reads = 0
        val graph = (0 until 4_096).associateWith { index ->
            val base = if (index == 0) emptySet() else setOf(index - 1)
            object : Set<Int> by base {
                override fun iterator(): Iterator<Int> {
                    val delegate = base.iterator()
                    return object : Iterator<Int> {
                        override fun hasNext() = delegate.hasNext()
                        override fun next(): Int {
                            ++reads
                            return delegate.next()
                        }
                    }
                }
            }
        }
        val layers = buildDependencyLayers(graph)
        assertEquals(4_096, layers.size)
        assertEquals((0 until 4_096).toList(), layers.flatten())
        assertEquals(4_095, reads)
    }

    @Test
    fun productionRegistrationWaitsForDependenciesAndCachesPostPreparePolynomial() = runBlocking {
        for (concurrent in listOf(false, true)) {
            val prepared = ConcurrentHashMap.newKeySet<IntermediateSymbol<*>>()
            val symbols = mutableListOf<IntermediateSymbol<*>>()
            repeat(32) { index ->
                val required = if (index == 0) emptySet() else setOf(symbols[index - 1])
                val base = LinearExpressionSymbol(constant = Flt64.one, name = "layer_$index")
                val symbol = object : LinearIntermediateSymbol<Flt64> by base {
                    var current = Flt64.one
                    override val dependencies = required
                    override fun toLinearPolynomial(): LinearPolynomial<Flt64> {
                        return LinearPolynomial(emptyList(), current)
                    }
                    override fun prepare(
                        values: Map<Symbol, Flt64>?,
                        tokenTable: AbstractTokenTable<Flt64>,
                        converter: IntoValue<Flt64>
                    ): Flt64 {
                        assertTrue(prepared.containsAll(required))
                        current = Flt64.two
                        prepared.add(this)
                        return current
                    }
                }
                symbols.add(symbol)
            }
            val statuses = mutableListOf<ULong>()
            val callback = RegistrationStatusCallBack {
                statuses.add(it.readySymbolAmount.toULong())
                ok
            }
            if (concurrent) {
                val table = ConcurrentManualAddTokenTable<Flt64>(Linear, false)
                try {
                    assertTrue(table.add(symbols) is Ok)
                    assertTrue(symbols.register(tokenTable = table, callBack = callback) is Ok)
                    assertTrue(symbols.all { table.cachedLinearFlattenValue(it)?.constant == Flt64.two })
                } finally {
                    table.close()
                }
            } else {
                val table = ManualTokenTable<Flt64>(Linear, false)
                try {
                    assertTrue(table.add(symbols) is Ok)
                    assertTrue(symbols.register(tokenTable = table, callBack = callback) is Ok)
                    assertTrue(symbols.all { table.cachedLinearFlattenValue(it)?.constant == Flt64.two })
                } finally {
                    table.close()
                }
            }
            assertEquals(symbols.toSet(), prepared)
            assertEquals((0UL..32UL).toList(), statuses)
        }
    }
}
