/**
 * 编译操作
 * Compile Operations
 *
 * 提供将多项式编译为高效求值函数的便捷封装。
 * 编译后的函数可直接用于数值计算，避免每次求值时的符号解析开销。
 * Provides convenient wrappers for compiling polynomials into efficient evaluation functions.
 * Compiled functions can be used directly for numerical computation,
 * avoiding symbol parsing overhead during each evaluation.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial

fun LinearPolynomial<Flt64>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true
): (List<Flt64>) -> Flt64 {
    return compileEvalLinear(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun QuadraticPolynomial<Flt64>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): (List<Flt64>) -> Flt64 {
    return compileEvalQuadratic(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

fun CanonicalPolynomial<Flt64>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): (List<Flt64>) -> Flt64 {
    return compileEvalCanonical(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator,
        one = Flt64.one
    )
}

fun LinearPolynomial<Flt64>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true
): (List<Flt64>) -> List<Flt64> {
    return compileGradientLinear(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun QuadraticPolynomial<Flt64>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): (List<Flt64>) -> List<Flt64> {
    return compileGradientQuadratic(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

fun CanonicalPolynomial<Flt64>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): (List<Flt64>) -> List<Flt64> {
    return compileGradientCanonical(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}