@file:Suppress("unused")
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.functional.Ret

/**
 * Flt64 编译快捷函数
 * Flt64 Compile Convenience Functions
 *
 * 提供 Flt64 多项式的编译求值和梯度快捷函数。
 * 封装通用编译运算，自动填入 Flt64 的零值和一值。
 * Provides convenience compile-evaluation and gradient functions for Flt64 polynomials.
 * Wraps generic compile operations with Flt64 zero/one constants.
 */

/**
 * 编译线性多项式为求值函数
 * Compile a linear polynomial into an evaluation function
 *
 * @param order 符号顺序 / Symbol order
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @return 编译后的求值函数 / Compiled evaluation function
 */
fun LinearPolynomial<Flt64>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true
): Ret<(List<Flt64>) -> Flt64> {
    return compileEvalLinear(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 编译二次多项式为求值函数
 * Compile a quadratic polynomial into an evaluation function
 *
 * @param order 符号顺序 / Symbol order
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 编译后的求值函数 / Compiled evaluation function
 */
fun QuadraticPolynomial<Flt64>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): Ret<(List<Flt64>) -> Flt64> {
    return compileEvalQuadratic(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

/**
 * 编译规范多项式为求值函数
 * Compile a canonical polynomial into an evaluation function
 *
 * @param order 符号顺序 / Symbol order
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 编译后的求值函数 / Compiled evaluation function
 */
fun CanonicalPolynomial<Flt64>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): Ret<(List<Flt64>) -> Flt64> {
    return compileEvalCanonical(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator,
        one = Flt64.one
    )
}

/**
 * 编译线性多项式为梯度函数
 * Compile a linear polynomial's gradient function
 *
 * @param order 符号顺序 / Symbol order
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @return 编译后的梯度函数 / Compiled gradient function
 */
fun LinearPolynomial<Flt64>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true
): Ret<(List<Flt64>) -> List<Flt64>> {
    return compileGradientLinear(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 编译二次多项式为梯度函数
 * Compile a quadratic polynomial's gradient function
 *
 * @param order 符号顺序 / Symbol order
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 编译后的梯度函数 / Compiled gradient function
 */
fun QuadraticPolynomial<Flt64>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): Ret<(List<Flt64>) -> List<Flt64>> {
    return compileGradientQuadratic(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

/**
 * 编译规范多项式为梯度函数
 * Compile a canonical polynomial's gradient function
 *
 * @param order 符号顺序 / Symbol order
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 编译后的梯度函数 / Compiled gradient function
 */
fun CanonicalPolynomial<Flt64>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): Ret<(List<Flt64>) -> List<Flt64>> {
    return compileGradientCanonical(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}
