package fuookami.ospf.kotlin.core.token

import kotlin.test.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.testing.*

class GenericTokenCacheTest {
    @Test
    fun cacheSolverIfNotCachedShouldRespectSolutionAndFixedValueKeysForFourNumberTypes() {
        runCacheCase(GenericNumberCases.flt64)
        runCacheCase(GenericNumberCases.rtn64)
        runCacheCase(GenericNumberCases.fltX)
        runCacheCase(GenericNumberCases.rtnX)
    }

    private fun <V> runCacheCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val tokenTable = AutoTokenTable<V>(Linear, false)
        val cacheKey = "${numberCase.name}_cache_key"
        val solutionKey = listOf(Flt64(1.0), Flt64(2.0))

        var solutionComputeCalls = 0
        val cachedSolution1 = tokenTable.cacheSolverIfNotCached(
            cacheKey = cacheKey,
            solution = solutionKey,
            value = {
                ++solutionComputeCalls
                value(numberCase, 7.0)
            },
            converter = numberCase.converter
        )
        val cachedSolution2 = tokenTable.cacheSolverIfNotCached(
            cacheKey = cacheKey,
            solution = solutionKey,
            value = {
                ++solutionComputeCalls
                value(numberCase, 8.0)
            },
            converter = numberCase.converter
        )

        assertEquals(1, solutionComputeCalls, "${numberCase.name}: solution cache should compute once")
        assertEquals(Flt64(7.0), toFlt64(numberCase, assertNotNull(cachedSolution1)), "${numberCase.name}: first solution cache value mismatch")
        assertEquals(Flt64(7.0), toFlt64(numberCase, assertNotNull(cachedSolution2)), "${numberCase.name}: second solution cache read mismatch")
        assertTrue(tokenTable.cachedSolver(cacheKey, solutionKey, numberCase.converter) == true, "${numberCase.name}: solution cache flag should be true")

        val symbol = LinearExpressionSymbol(constant = Flt64.one, name = "${numberCase.name.lowercase()}_cache_symbol")
        val fixedValuesA = mapOf<Symbol, Flt64>(symbol to Flt64(3.0))
        val fixedValuesB = mapOf<Symbol, Flt64>(symbol to Flt64(4.0))

        var fixedComputeCallsA = 0
        var fixedComputeCallsB = 0
        val fixedCachedA1 = tokenTable.cacheSolverIfNotCached(
            cacheKey = cacheKey,
            fixedValues = fixedValuesA,
            value = {
                ++fixedComputeCallsA
                value(numberCase, 11.0)
            },
            converter = numberCase.converter
        )
        val fixedCachedA2 = tokenTable.cacheSolverIfNotCached(
            cacheKey = cacheKey,
            fixedValues = fixedValuesA,
            value = {
                ++fixedComputeCallsA
                value(numberCase, 12.0)
            },
            converter = numberCase.converter
        )
        val fixedCachedB = tokenTable.cacheSolverIfNotCached(
            cacheKey = cacheKey,
            fixedValues = fixedValuesB,
            value = {
                ++fixedComputeCallsB
                value(numberCase, 13.0)
            },
            converter = numberCase.converter
        )

        assertEquals(1, fixedComputeCallsA, "${numberCase.name}: fixed-values(A) cache should compute once")
        assertEquals(1, fixedComputeCallsB, "${numberCase.name}: fixed-values(B) cache should compute once")
        assertEquals(Flt64(11.0), toFlt64(numberCase, assertNotNull(fixedCachedA1)), "${numberCase.name}: fixed-values(A) first cached value mismatch")
        assertEquals(Flt64(11.0), toFlt64(numberCase, assertNotNull(fixedCachedA2)), "${numberCase.name}: fixed-values(A) second cached value mismatch")
        assertEquals(Flt64(13.0), toFlt64(numberCase, assertNotNull(fixedCachedB)), "${numberCase.name}: fixed-values(B) cached value mismatch")
        assertEquals(
            Flt64(7.0),
            toFlt64(numberCase, assertNotNull(tokenTable.cachedSolverValue(cacheKey, solutionKey, numberCase.converter))),
            "${numberCase.name}: fixed-value cache should not overwrite solution cache"
        )
    }

    private fun <V> value(
        numberCase: GenericNumberCase<V>,
        raw: Double
    ): V where V : RealNumber<V>, V : NumberField<V> = numberCase.converter.intoValue(Flt64(raw))

    private fun <V> toFlt64(
        numberCase: GenericNumberCase<V>,
        value: V
    ): Flt64 where V : RealNumber<V>, V : NumberField<V> = numberCase.converter.fromValue(value)
}
