package fuookami.ospf.kotlin.math.symbol.parser

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.symbol.parse.Flt64NumberParser
import fuookami.ospf.kotlin.math.symbol.parse.Int64NumberParser
import fuookami.ospf.kotlin.math.symbol.parse.NumberParser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NumberParserIntegrationTest {
    @Test
    fun flt64ParserParsesInteger() {
        val parser = Flt64NumberParser
        val result = parser.parse("42")
        assertEquals(Flt64(42.0), result)
    }

    @Test
    fun flt64ParserParsesDecimal() {
        val parser = Flt64NumberParser
        val result = parser.parse("3.14")
        assertEquals(Flt64(3.14), result)
    }

    @Test
    fun flt64ParserParsesNegative() {
        val parser = Flt64NumberParser
        val result = parser.parse("-7.5")
        assertEquals(Flt64(-7.5), result)
    }

    @Test
    fun flt64ParserReturnsNullForInvalid() {
        val parser = Flt64NumberParser
        val result = parser.parse("abc")
        assertNull(result)
    }

    @Test
    fun int64ParserParsesInteger() {
        val parser = Int64NumberParser
        val result = parser.parse("42")
        assertEquals(Int64(42), result)
    }

    @Test
    fun int64ParserParsesNegative() {
        val parser = Int64NumberParser
        val result = parser.parse("-7")
        assertEquals(Int64(-7), result)
    }

    @Test
    fun int64ParserReturnsNullForDecimal() {
        val parser = Int64NumberParser
        val result = parser.parse("3.14")
        assertNull(result)
    }

    @Test
    fun int64ParserReturnsNullForInvalid() {
        val parser = Int64NumberParser
        val result = parser.parse("abc")
        assertNull(result)
    }
}
