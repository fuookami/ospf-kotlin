package fuookami.ospf.kotlin.math.symbol

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.dimension.*

class SymbolDimensionTest {
    @Test
    fun `dimensionedSymbol_shouldCarryDimensionInfo`() {
        val x = DimensionedSymbol(
            name = "x",
            quantity = Length,
            preferredUnit = Meter
        )

        assertEquals("x", x.name)
        assertEquals(Length, x.quantity)
        assertEquals(Meter, x.preferredUnit)
    }

    @Test
    fun `dimensionedSymbol_canAddTo_shouldCheckDimensionCompatibility`() {
        val x = DimensionedSymbol("x", quantity = Length, preferredUnit = Meter)
        val y = DimensionedSymbol("y", quantity = Length, preferredUnit = Centimeter)
        val z = DimensionedSymbol("z", quantity = Time, preferredUnit = Second)

        // Same dimension: can add
        assert(x.canAddTo(y))
        // Different dimension: cannot add
        assert(!x.canAddTo(z))
    }

    @Test
    fun `dimensionedSymbol_multiplyDivide_shouldComputeResultDimension`() {
        val x = DimensionedSymbol("x", quantity = Length, preferredUnit = Meter)
        val y = DimensionedSymbol("y", quantity = Time, preferredUnit = Second)

        // Length * Time = L·T (not Velocity, which is a named quantity)
        val lengthTime = x.multiplyWith(y)
        // Verify it has both L and T in its dimension
        assert(lengthTime.quantities.any { it.dimension.dimensionName == "length" })
        assert(lengthTime.quantities.any { it.dimension.dimensionName == "time" })

        // Length / Time = L/T (dimensionally equivalent to Velocity)
        val lengthDivTime = x.divideBy(y)
        // Verify dimension symbol contains L and T
        val symbol = lengthDivTime.dimensionSymbol()
        assert(symbol.contains("L"))
        assert(symbol.contains("T"))
    }

    @Test
    fun `symbolDimensionRegistry_shouldRejectInvalidAddSub`() {
        val registry = SymbolDimensionRegistry()

        val x = DimensionedSymbol("x", quantity = Length, preferredUnit = Meter)
        val y = DimensionedSymbol("y", quantity = Time, preferredUnit = Second)

        registry.register(x)
        registry.register(y)

        // x(m) + y(s) 应该失败
        assertThrows<DimensionMismatchException> {
            registry.validateAddSubDimension(listOf(x, y))
        }
    }

    @Test
    fun `symbolDimensionRegistry_shouldInferResultDimension`() {
        val registry = SymbolDimensionRegistry()

        val x = DimensionedSymbol("x", quantity = Length, preferredUnit = Meter)
        val y = DimensionedSymbol("y", quantity = Time, preferredUnit = Second)

        registry.register(x)
        registry.register(y)

        // x(m) * y(s) 应该得到 L·T
        val result = registry.inferDimension(x, y, Operation.Multiply)
        assertTrue(result is Ok, "inferDimension should succeed for multiply")
        val dimension = (result as Ok).value
        // Verify it has both L and T
        assert(dimension.quantities.any { it.dimension.dimensionName == "length" })
        assert(dimension.quantities.any { it.dimension.dimensionName == "time" })

        // x(m) / y(s) 应该得到 L/T (dimensionally velocity)
        val result2 = registry.inferDimension(x, y, Operation.Divide)
        assertTrue(result2 is Ok, "inferDimension should succeed for divide")
        val dimension2 = (result2 as Ok).value
        val symbol = dimension2.dimensionSymbol()
        assert(symbol.contains("L"))
        assert(symbol.contains("T"))
    }

    @Test
    fun `symbolDimensionRegistry_shouldAllowValidAddSub`() {
        val registry = SymbolDimensionRegistry()

        val x = DimensionedSymbol("x", quantity = Length, preferredUnit = Meter)
        val y = DimensionedSymbol("y", quantity = Length, preferredUnit = Centimeter)

        registry.register(x)
        registry.register(y)

        // x(m) + y(cm) 应该成功 (相同量纲)
        registry.validateAddSubDimension(listOf(x, y))
    }
}
