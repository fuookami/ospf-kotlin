/**
 * 多项式解析器
 * Polynomial Parser
 *
 * 提供泛型 Ring<T> 多项式解析的公共入口函数。
 * Provides generic Ring<T> polynomial parsing public entry functions.
 */
package fuookami.ospf.kotlin.math.symbol.parser

import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.symbol.parse.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier
import fuookami.ospf.kotlin.math.symbol.Symbol

/**
 * 解析字符串为规范多项式（泛型类型版本）
 * Parses a string into a canonical polynomial (generic number type version)
 *
 * @param input 输入字符串 / Input string
 * @param numberParser 数值解析器 / Number parser
 * @param zero 类型零值 / Zero value of the type
 * @param one 类型单位值 / One value of the type
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param isZero 零值判断函数 / Zero-check function
 * @param symbolComparator 符号排序比较器 / Symbol ordering comparator
 * @return 解析后的规范多项式 / Parsed canonical polynomial
 */
fun <T> parse(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<CanonicalPolynomial<T>> where T : Ring<T> {
    return parseCanonical(input, numberParser, zero, one, symbolOf, isZero, symbolComparator)
}

/**
 * 解析字符串为线性多项式，若非线性则返回 null
 * Parses a string into a linear polynomial, returns null if not linear
 *
 * @param input 输入字符串 / Input string
 * @param numberParser 数值解析器 / Number parser
 * @param zero 类型零值 / Zero value of the type
 * @param one 类型单位值 / One value of the type
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param isZero 零值判断函数 / Zero-check function
 * @return 线性多项式或 null / Linear polynomial or null
 */
fun <T> parseLinearPolynomialOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): ParseResult<LinearPolynomial<T>?> where T : Ring<T> {
    return parseLinearOrNull(input, numberParser, zero, one, symbolOf, isZero)
}

/**
 * 解析字符串为二次多项式，若非二次则返回 null
 * Parses a string into a quadratic polynomial, returns null if not quadratic
 *
 * @param input 输入字符串 / Input string
 * @param numberParser 数值解析器 / Number parser
 * @param zero 类型零值 / Zero value of the type
 * @param one 类型单位值 / One value of the type
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @param isZero 零值判断函数 / Zero-check function
 * @param symbolComparator 符号排序比较器 / Symbol ordering comparator
 * @return 二次多项式或 null / Quadratic polynomial or null
 */
fun <T> parseQuadraticPolynomialOrNull(
    input: String,
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): ParseResult<QuadraticPolynomial<T>?> where T : Ring<T> {
    return parseQuadraticOrNull(input, numberParser, zero, one, symbolOf, isZero, symbolComparator)
}
