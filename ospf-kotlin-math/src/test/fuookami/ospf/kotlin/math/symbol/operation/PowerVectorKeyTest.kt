package fuookami.ospf.kotlin.math.symbol.operation

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.number.Int32

class PowerVectorKeyTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun denseModeShouldCreateCorrectKey() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")

        val symbolList: List<Symbol> = listOf(x, y, z)
        val symbolIndex: Map<Symbol, Int> = symbolList.indices.associateBy { symbolList[it] }

        val powers: Map<Symbol, Int32> = mapOf(x to Int32(2), y to Int32(3))
        val key = PowerVectorKey.create(powers, symbolIndex, 3)

        assertTrue(key.isDense)
        assertFalse(key.isSparse)

        // Verify reconstruction
        val reconstructed = key.toPowers(symbolList)
        assertEquals(Int32(2), reconstructed[x])
        assertEquals(Int32(3), reconstructed[y])
        assertEquals(null, reconstructed[z])  // z has no power
    }

    @Test
    fun sparseModeShouldCreateCorrectKey() {
        val symbols: List<Symbol> = (0..10).map { TestSymbol("s$it") }
        val symbolList = symbols.toList()
        val symbolIndex: Map<Symbol, Int> = symbolList.indices.associateBy { symbolList[it] }

        // Only 2 symbols have powers, totalSymbols = 11 -> sparse mode
        val powers: Map<Symbol, Int32> = mapOf(
            symbols[2] to Int32(5),
            symbols[7] to Int32(3)
        )
        val key = PowerVectorKey.create(powers, symbolIndex, 11)

        assertTrue(key.isSparse)
        assertFalse(key.isDense)

        // Verify reconstruction
        val reconstructed = key.toPowers(symbolList)
        assertEquals(Int32(5), reconstructed[symbols[2]])
        assertEquals(Int32(3), reconstructed[symbols[7]])
        assertEquals(2, reconstructed.size)
    }

    @Test
    fun denseKeysWithSamePowersShouldBeEqual() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolList: List<Symbol> = listOf(x, y)
        val symbolIndex: Map<Symbol, Int> = symbolList.indices.associateBy { symbolList[it] }

        val key1 = PowerVectorKey.create(mapOf<Symbol, Int32>(x to Int32(2), y to Int32(3)), symbolIndex, 2)
        val key2 = PowerVectorKey.create(mapOf<Symbol, Int32>(y to Int32(3), x to Int32(2)), symbolIndex, 2)

        assertEquals(key1, key2)
        assertEquals(key1.hashCode(), key2.hashCode())
    }

    @Test
    fun sparseKeysWithSamePowersShouldBeEqual() {
        val symbols: List<Symbol> = (0..10).map { TestSymbol("s$it") }
        val symbolList = symbols.toList()
        val symbolIndex: Map<Symbol, Int> = symbolList.indices.associateBy { symbolList[it] }

        // Create keys with different insertion order but same powers
        val key1 = PowerVectorKey.create(
            mapOf<Symbol, Int32>(symbols[5] to Int32(1), symbols[2] to Int32(3)),
            symbolIndex, 11
        )
        val key2 = PowerVectorKey.create(
            mapOf<Symbol, Int32>(symbols[2] to Int32(3), symbols[5] to Int32(1)),
            symbolIndex, 11
        )

        assertEquals(key1, key2)
        assertEquals(key1.hashCode(), key2.hashCode())
    }

    @Test
    fun keysWithDifferentPowersShouldNotBeEqual() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolList: List<Symbol> = listOf(x, y)
        val symbolIndex: Map<Symbol, Int> = symbolList.indices.associateBy { symbolList[it] }

        val key1 = PowerVectorKey.create(mapOf<Symbol, Int32>(x to Int32(2)), symbolIndex, 2)
        val key2 = PowerVectorKey.create(mapOf<Symbol, Int32>(x to Int32(3)), symbolIndex, 2)

        assertFalse(key1 == key2)
    }

    @Test
    fun emptyPowersShouldWork() {
        val x = TestSymbol("x")
        val symbolList: List<Symbol> = listOf(x)
        val symbolIndex: Map<Symbol, Int> = symbolList.indices.associateBy { symbolList[it] }

        val key = PowerVectorKey.create(emptyMap(), symbolIndex, 1)

        val reconstructed = key.toPowers(symbolList)
        assertTrue(reconstructed.isEmpty())
    }

    @Test
    fun singleSymbolShouldWork() {
        val x = TestSymbol("x")
        val symbolList: List<Symbol> = listOf(x)
        val symbolIndex: Map<Symbol, Int> = symbolList.indices.associateBy { symbolList[it] }

        val key = PowerVectorKey.create(mapOf<Symbol, Int32>(x to Int32(5)), symbolIndex, 1)

        assertTrue(key.isDense)
        val reconstructed = key.toPowers(symbolList)
        assertEquals(Int32(5), reconstructed[x])
    }

    @Test
    fun modeSelectionShouldFollowThreshold() {
        val symbols: List<Symbol> = (0..9).map { TestSymbol("s$it") }
        val symbolList = symbols.toList()
        val symbolIndex: Map<Symbol, Int> = symbolList.indices.associateBy { symbolList[it] }

        // 6 out of 10 symbols have powers (sparsity = 0.6 >= 0.5) -> dense mode
        val densePowers: Map<Symbol, Int32> = (0..5).associate { symbols[it] to Int32(1) }
        val denseKey = PowerVectorKey.create(densePowers, symbolIndex, 10)
        assertTrue(denseKey.isDense)

        // 2 out of 10 symbols have powers (sparsity = 0.2 < 0.5) -> sparse mode
        val sparsePowers: Map<Symbol, Int32> = mapOf(symbols[0] to Int32(1), symbols[5] to Int32(2))
        val sparseKey = PowerVectorKey.create(sparsePowers, symbolIndex, 10)
        assertTrue(sparseKey.isSparse)
    }

    @Test
    fun smallSymbolSetShouldAlwaysUseDenseMode() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")
        val symbolList: List<Symbol> = listOf(x, y, z)
        val symbolIndex: Map<Symbol, Int> = symbolList.indices.associateBy { symbolList[it] }

        // Even with low sparsity (1/3), small set uses dense mode
        val key = PowerVectorKey.create(mapOf<Symbol, Int32>(x to Int32(1)), symbolIndex, 3)
        assertTrue(key.isDense)
    }
}
