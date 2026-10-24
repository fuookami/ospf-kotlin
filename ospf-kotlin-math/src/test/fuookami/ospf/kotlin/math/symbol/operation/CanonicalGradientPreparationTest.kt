package fuookami.ospf.kotlin.math.symbol.operation

import kotlin.random.Random
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial

class CanonicalGradientPreparationTest {
    private data class Variable(override val name: String) : Symbol {
        override val displayName: String? get() = null
    }

    private fun <T : Ring<T>> originalGradient(
        polynomial: CanonicalPolynomial<T>,
        order: List<Symbol>,
        values: List<T>,
        zero: T
    ): List<T> {
        val gradient = MutableList(order.size) { zero }
        for (monomial in polynomial.monomials) {
            for ((target, amount) in monomial.powers) {
                var derivative = zero
                repeat(amount.toInt()) { derivative += monomial.coefficient }
                for ((symbol, power) in monomial.powers) {
                    repeat(if (symbol == target) power.toInt() - 1 else power.toInt()) {
                        derivative *= values[order.indexOf(symbol)]
                    }
                }
                gradient[order.indexOf(target)] += derivative
            }
        }
        return gradient
    }

    private fun <T : Ring<T>> checkNumeric(
        zero: T,
        fromInt: (Int) -> T,
        encode: (T) -> Any = { it }
    ) {
        val random = Random(70324)
        val symbols = List(5) { Variable("v$it") }
        val order = symbols.reversed()
        repeat(24) {
            val polynomial = CanonicalPolynomial(
                monomials = List(4) {
                    CanonicalMonomial(
                        coefficient = fromInt(random.nextInt(-5, 6)),
                        powers = symbols.shuffled(random).take(random.nextInt(0, 6))
                            .associateWith { Int32(random.nextInt(0, 6)) }
                    )
                },
                constant = fromInt(7)
            )
            val compiled = polynomial.compileGradientCanonical(
                order = order,
                combineTerms = false,
                zero = zero
            ).value!!
            repeat(3) {
                val values = order.map { fromInt(random.nextInt(-4, 5)) }
                val expected = originalGradient(
                    polynomial = polynomial,
                    order = order,
                    values = values,
                    zero = zero
                )
                assertEquals(expected.map(encode), compiled(values).map(encode))
            }
        }
    }

    @Test
    fun exactIntegerPowersAndPrefixSuffixProductsMatchLegacyIncludingOverflowAndZeros() {
        checkNumeric(zero = Int8.zero, fromInt = { Int8(it.toByte()) })
        checkNumeric(zero = Int16.zero, fromInt = { Int16(it.toShort()) })
        checkNumeric(zero = Int32.zero, fromInt = { Int32(it) })
        checkNumeric(zero = Int64.zero, fromInt = { Int64(it.toLong()) })
        checkNumeric(zero = IntX.zero, fromInt = { IntX(it.toLong()) })
        checkNumeric(zero = UInt8.zero, fromInt = { UInt8(it.toUByte()) })
        checkNumeric(zero = UInt16.zero, fromInt = { UInt16(it.toUShort()) })
        checkNumeric(zero = UInt32.zero, fromInt = { UInt32(it.toUInt()) })
        checkNumeric(zero = UInt64.zero, fromInt = { UInt64(it.toULong()) })
        checkNumeric(zero = UIntX.zero, fromInt = { UIntX(it.toUInt().toLong()) })
    }

    @Test
    fun floatingPrefixesRetainEveryDerivativeLeftFoldAndNonFiniteRawBits() {
        val samples = listOf(1e16, 1.0, -1e16, -0.0, 0.0, Double.POSITIVE_INFINITY, Double.NaN)
        val symbols = List(8) { Variable("f$it") }
        val powers = linkedMapOf<Symbol, Int32>().apply {
            symbols.forEachIndexed { index, symbol -> put(symbol, Int32(index % 3)) }
        }
        for (coefficient in samples) {
            val polynomial = CanonicalPolynomial(
                monomials = listOf(CanonicalMonomial(Flt64(coefficient), powers)),
                constant = Flt64.zero
            )
            val compiled = polynomial.compileGradientCanonical(
                order = symbols,
                combineTerms = false,
                zero = Flt64.zero
            ).value!!
            repeat(16) { shift ->
                val values = symbols.indices.map { Flt64(samples[(it + shift) % samples.size]) }
                val expected = originalGradient(
                    polynomial = polynomial,
                    order = symbols,
                    values = values,
                    zero = Flt64.zero
                )
                assertEquals(expected.map { it.toDouble().toRawBits() }, compiled(values).map { it.toDouble().toRawBits() })
            }
        }
        checkNumeric(
            zero = Flt32.zero,
            fromInt = { Flt32(it.toFloat() / 3f) },
            encode = { it.toFlt64().toDouble().toRawBits() }
        )
        checkNumeric(
            zero = FltX.zero,
            fromInt = { FltX(it.toDouble() / 3.0) },
            encode = { it.toString() }
        )
    }

    @Test
    fun highDegreeIntegersReadEachInputTwiceAndStrictCommonPowersShareLeftPrefixes() {
        val symbols = List(32) { Variable("p$it") }
        val exact = CanonicalPolynomial(
            monomials = listOf(CanonicalMonomial(Int64(7L), symbols.associateWith { Int32(1024) })),
            constant = Int64.zero
        )
        val integers = CountingValues(List(symbols.size) { Int64(2L) })
        val exactCompiled = exact.compileGradientCanonical(
            order = symbols,
            combineTerms = false,
            zero = Int64.zero
        ).value!!
        assertEquals(List(symbols.size) { Int64.zero }, exactCompiled(integers))
        assertEquals(symbols.size * 2, integers.reads)
        val strict = CanonicalPolynomial(
            monomials = listOf(CanonicalMonomial(Flt64(0.1), symbols.associateWith { Int32(2) })),
            constant = Flt64.zero
        )
        val rawValues = List(symbols.size) { Flt64(0.99 + it / 1000.0) }
        val originalValues = CountingValues(rawValues)
        val expected = originalGradient(
            polynomial = strict,
            order = symbols,
            values = originalValues,
            zero = Flt64.zero
        )
        val sharedValues = CountingValues(rawValues)
        val compiled = strict.compileGradientCanonical(
            order = symbols,
            combineTerms = false,
            zero = Flt64.zero
        ).value!!
        assertEquals(expected.map { it.toDouble().toRawBits() }, compiled(sharedValues).map { it.toDouble().toRawBits() })
        assertTrue(sharedValues.reads < originalValues.reads * 0.6)
    }

    @Test
    fun customRingRetainsRuntimeArithmeticOrderAndValidationFailures() {
        val events = ArrayList<String>()
        val symbols = listOf(Variable("x"), Variable("y"))
        val zero = Recorded(0, events)
        val polynomial = CanonicalPolynomial(
            monomials = listOf(CanonicalMonomial(Recorded(3, events), symbols.associateWith { Int32(2) })),
            constant = zero
        )
        val compiled = polynomial.compileGradientCanonical(
            order = symbols,
            combineTerms = false,
            zero = zero
        ).value!!
        assertTrue(events.isEmpty())
        val values = listOf(Recorded(4, events), Recorded(5, events))
        val expected = originalGradient(
            polynomial = polynomial,
            order = symbols,
            values = values,
            zero = zero
        ).map { it.value }
        val originalEvents = events.toList()
        events.clear()
        assertEquals(expected, compiled(values).map { it.value })
        assertEquals(originalEvents, events)
        assertFailsWith<IllegalArgumentException> { compiled(emptyList()) }
        assertIs<Failed<*, *, *>>(polynomial.compileGradientCanonical(order = listOf(symbols[0]), zero = zero))
        assertIs<Failed<*, *, *>>(polynomial.compileGradientCanonical(order = symbols + symbols[0], zero = zero))
        val negative = polynomial.copy(monomials = listOf(CanonicalMonomial(Recorded(3, events), mapOf(symbols[0] to Int32(-1)))))
        assertIs<Failed<*, *, *>>(negative.compileGradientCanonical(order = symbols, zero = zero))
    }

    private class CountingValues<T>(private val values: List<T>) : AbstractList<T>() {
        var reads = 0
        override val size get() = values.size
        override fun get(index: Int): T {
            ++reads
            return values[index]
        }
    }

    private data class Recorded(val value: Int, val events: MutableList<String>) : Ring<Recorded> {
        override fun plus(rhs: Recorded): Recorded {
            events.add("+$value,${rhs.value}")
            return Recorded(value + rhs.value, events)
        }
        override fun times(rhs: Recorded): Recorded {
            events.add("*$value,${rhs.value}")
            return Recorded(value * rhs.value, events)
        }
        override fun minus(rhs: Recorded) = Recorded(value - rhs.value, events)
        override fun unaryMinus() = Recorded(-value, events)
        override fun inc() = Recorded(value + 1, events)
        override fun dec() = Recorded(value - 1, events)
    }
}
