/**
 * 多项式解析器
 * Polynomial Parser
 *
 * 提供泛型 Ring<T> 多项式解析的公共入口函数�? * Provides generic Ring<T> polynomial parsing public entry functions.
 */
package fuookami.ospf.kotlin.math.symbol.parser

import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.parse.NumberParser
import fuookami.ospf.kotlin.math.symbol.parse.parseCanonicalTyped
import fuookami.ospf.kotlin.math.symbol.parse.parseLinearTypedOrNull
import fuookami.ospf.kotlin.math.symbol.parse.parseQuadraticTypedOrNull
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier

fun <T> parseTyped(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    return parseCanonicalTyped(input, numberParser, zero, one, symbolOf, isZero, symbolComparator)
}

fun <T> parseLinearPolynomialTypedOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T>? where T : Ring<T> {
    return parseLinearTypedOrNull(input, numberParser, zero, one, symbolOf, isZero)
}

fun <T> parseQuadraticPolynomialTypedOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<T>? where T : Ring<T> {
    return parseQuadraticTypedOrNull(input, numberParser, zero, one, symbolOf, isZero, symbolComparator)
}
