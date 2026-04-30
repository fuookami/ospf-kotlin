/**
 * 多项式与不等式解析 API
 * Polynomial and Inequality Parsing API
 *
 * 提供 Ret 包装的多项式和不等式解析函数，支持 Flt64 和泛型 Ring<T> 类型。
 * Provides Ret-wrapped parsing functions for polynomials and inequalities, supporting Flt64 and generic Ring<T> types.
 */
package fuookami.ospf.kotlin.math.symbol.parser

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier
import fuookami.ospf.kotlin.math.symbol.parse.NumberParser
import fuookami.ospf.kotlin.math.symbol.parse.ParseResult
import fuookami.ospf.kotlin.math.symbol.parse.parseCanonicalRet
import fuookami.ospf.kotlin.math.symbol.parse.parseCanonicalTypedRet
import fuookami.ospf.kotlin.math.symbol.parse.parseLinearRet
import fuookami.ospf.kotlin.math.symbol.parse.parseLinearTypedRetOrNull
import fuookami.ospf.kotlin.math.symbol.parse.parseLinearInequalityTypedRetOrNull
import fuookami.ospf.kotlin.math.symbol.parse.parseQuadraticRet
import fuookami.ospf.kotlin.math.symbol.parse.parseQuadraticTypedRetOrNull
import fuookami.ospf.kotlin.math.symbol.parse.parseLinearInequalityRet
import fuookami.ospf.kotlin.math.symbol.parse.parseQuadraticInequalityRet

fun <T> parseCanonical(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<CanonicalPolynomial<T>> where T : Ring<T> {
    return parseCanonicalTypedRet(input, numberParser, zero, one, symbolOf, isZero, symbolComparator)
}

fun parseCanonical(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<CanonicalPolynomial<Flt64>> {
    return parseCanonicalRet(input, symbolOf, symbolComparator)
}

fun <T> parseLinear(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): ParseResult<LinearPolynomial<T>> where T : Ring<T> {
    return parseLinearTypedRetOrNull(input, numberParser, zero, one, symbolOf, isZero)
}

fun parseLinear(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): ParseResult<LinearPolynomial<Flt64>> {
    return parseLinearRet(input, symbolOf)
}

fun <T> parseQuadratic(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticPolynomial<T>> where T : Ring<T> {
    return parseQuadraticTypedRetOrNull(input, numberParser, zero, one, symbolOf, isZero, symbolComparator)
}

fun parseQuadratic(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticPolynomial<Flt64>> {
    return parseQuadraticRet(input, symbolOf, symbolComparator)
}

fun <T> parseLinearInequality(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): ParseResult<LinearInequality<T>> where T : Ring<T> {
    return parseLinearInequalityTypedRetOrNull(input, numberParser, zero, one, symbolOf, isZero)
}

fun parseLinearInequality(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): ParseResult<LinearInequality<Flt64>> {
    return parseLinearInequalityRet(input, symbolOf)
}

fun parseQuadraticInequality(
    input: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticInequality> {
    return parseQuadraticInequalityRet(input, symbolOf, symbolComparator)
}
