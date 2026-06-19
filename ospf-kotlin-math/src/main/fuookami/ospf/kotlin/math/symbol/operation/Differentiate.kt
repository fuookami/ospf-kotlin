@file:Suppress("unused")
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * Flt64 微分快捷函数
 * Flt64 Differentiation Convenience Functions
 *
 * 提供 Flt64 多项式的导数、梯度和海森矩阵快捷函数。
 * 封装通用微分运算，自动填入 Flt64 的零值。
 * Provides Flt64 polynomial derivative, gradient, and Hessian convenience functions.
 * Wraps generic differentiation operations with Flt64 zero constant.
 */

/**
 * 计算 Flt64 线性单项式对指定符号的导数
 * Compute the derivative of a Flt64 linear monomial with respect to a symbol
 *
 * @param symbol 微分变量 / Differentiation variable
 * @return 导数值 / Derivative value
 */
fun LinearMonomial<Flt64>.derivative(symbol: Symbol): Flt64 {
    return derivativeLinear(symbol, zero = Flt64.zero)
}

/**
 * 计算 Flt64 线性多项式对指定符号的导数
 * Compute the derivative of a Flt64 linear polynomial with respect to a symbol
 *
 * @param symbol 微分变量 / Differentiation variable
 * @return 导数值 / Derivative value
 */
fun LinearPolynomial<Flt64>.derivative(symbol: Symbol): Flt64 {
    return derivativeLinear(symbol, zero = Flt64.zero)
}

/**
 * 计算 Flt64 线性多项式的梯度
 * Compute the gradient of a Flt64 linear polynomial
 *
 * @param order 符号顺序 / Symbol order
 * @return 梯度值列表 / Gradient value list
 */
fun LinearPolynomial<Flt64>.gradient(order: List<Symbol>): List<Flt64> {
    return gradientLinear(order, zero = Flt64.zero)
}

/**
 * 计算 Flt64 二次单项式对指定符号的导数
 * Compute the derivative of a Flt64 quadratic monomial with respect to a symbol
 *
 * @param symbol 微分变量 / Differentiation variable
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @return 导数（线性多项式） / Derivative (linear polynomial)
 */
fun QuadraticMonomial<Flt64>.derivative(
    symbol: Symbol,
    combineTerms: Boolean = true
): LinearPolynomial<Flt64> {
    return derivativeQuadratic(
        symbol = symbol,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 计算 Flt64 二次多项式对指定符号的导数
 * Compute the derivative of a Flt64 quadratic polynomial with respect to a symbol
 *
 * @param symbol 微分变量 / Differentiation variable
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @return 导数（线性多项式） / Derivative (linear polynomial)
 */
fun QuadraticPolynomial<Flt64>.derivative(
    symbol: Symbol,
    combineTerms: Boolean = true
): LinearPolynomial<Flt64> {
    return derivativeQuadratic(
        symbol = symbol,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 计算 Flt64 二次多项式的梯度
 * Compute the gradient of a Flt64 quadratic polynomial
 *
 * @param order 符号顺序 / Symbol order
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @return 梯度（线性多项式列表） / Gradient (list of linear polynomials)
 */
fun QuadraticPolynomial<Flt64>.gradient(
    order: List<Symbol>,
    combineTerms: Boolean = true
): List<LinearPolynomial<Flt64>> {
    return gradientQuadratic(
        order = order,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 计算 Flt64 二次多项式的海森矩阵
 * Compute the Hessian matrix of a Flt64 quadratic polynomial
 *
 * @param order 符号顺序 / Symbol order
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @return 海森矩阵（二维 Double 数组） / Hessian matrix (2D Double array)
 */
fun QuadraticPolynomial<Flt64>.hessian(
    order: List<Symbol>,
    combineTerms: Boolean = true
): Array<DoubleArray> {
    val firstOrderDerivatives = gradient(order, combineTerms)
    return Array(order.size) { i ->
        DoubleArray(order.size) { j ->
            firstOrderDerivatives[i].derivative(order[j]).toDouble()
        }
    }
}

/**
 * 计算 Flt64 规范单项式对指定符号的导数
 * Compute the derivative of a Flt64 canonical monomial with respect to a symbol
 *
 * @param symbol 微分变量 / Differentiation variable
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @return 导数（规范多项式） / Derivative (canonical polynomial)
 */
fun CanonicalMonomial<Flt64>.derivative(
    symbol: Symbol,
    combineTerms: Boolean = true
): CanonicalPolynomial<Flt64> {
    return derivativeCanonical(
        symbol = symbol,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 计算 Flt64 规范多项式对指定符号的导数
 * Compute the derivative of a Flt64 canonical polynomial with respect to a symbol
 *
 * @param symbol 微分变量 / Differentiation variable
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @return 导数（规范多项式） / Derivative (canonical polynomial)
 */
fun CanonicalPolynomial<Flt64>.derivative(
    symbol: Symbol,
    combineTerms: Boolean = true
): CanonicalPolynomial<Flt64> {
    return derivativeCanonical(
        symbol = symbol,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 计算 Flt64 规范多项式的梯度
 * Compute the gradient of a Flt64 canonical polynomial
 *
 * @param order 符号顺序 / Symbol order
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @return 梯度（规范多项式列表） / Gradient (list of canonical polynomials)
 */
fun CanonicalPolynomial<Flt64>.gradient(
    order: List<Symbol>,
    combineTerms: Boolean = true
): List<CanonicalPolynomial<Flt64>> {
    return gradientCanonical(
        order = order,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 计算 Flt64 规范多项式的海森矩阵
 * Compute the Hessian matrix of a Flt64 canonical polynomial
 *
 * @param order 符号顺序 / Symbol order
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 海森矩阵（二维 Double 数组） / Hessian matrix (2D Double array)
 */
fun CanonicalPolynomial<Flt64>.hessian(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<Array<DoubleArray>> {
    val source = if (combineTerms) {
        this.combineTerms(symbolComparator)
    } else {
        this
    }
    return when (val quadratic = source.toQuadraticPolynomialRet(symbolComparator)) {
        is Ok -> {
            Ok(quadratic.value.hessian(order = order, combineTerms = false))
        }

        is Failed -> {
            Failed(
                ErrorCode.IllegalArgument,
                "Cannot compute canonical hessian for non-quadratic polynomial: ${quadratic.error.message}"
            )
        }

        is Fatal -> {
            Fatal(
                ErrorCode.IllegalArgument,
                "Cannot compute canonical hessian for non-quadratic polynomial: ${quadratic.errors.joinToString { it.message }}"
            )
        }
    }
}
