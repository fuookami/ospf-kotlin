/**
 * 多项式矩阵形式
 * Polynomial Matrix Form
 *
 * 提供将多项式转换为矩阵形式（c/d 向量和 Q 矩阵）的功能。
 * 支持线性和二次多项式的矩阵表示与反向还原。
 * Provides conversion of polynomials to matrix form (c/d vectors and Q matrix).
 * Supports matrix representation and reverse reconstruction for linear and quadratic polynomials.
*/
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try

/**
 * 线性多项式矩阵形式
 * Linear polynomial matrix form
 *
 * 表示 c^T * x + d 的矩阵形式。
 * Represents the matrix form c^T * x + d.
 *
 * @property c 系数向量 / Coefficient vector
 * @property d 常数项 / Constant term
 * @property order 符号顺序 / Symbol order
*/
data class LinearMatrixForm<T>(
    val c: List<T>,
    val d: T,
    val order: List<Symbol>
) where T : Ring<T>

/**
 * 二次多项式矩阵形式
 * Quadratic polynomial matrix form
 *
 * 表示 x^T * Q * x + c^T * x + d 的矩阵形式。
 * Represents the matrix form x^T * Q * x + c^T * x + d.
 *
 * @property q 二次项系数矩阵 / Quadratic coefficient matrix
 * @property c 一次项系数向量 / Linear coefficient vector
 * @property d 常数项 / Constant term
 * @property order 符号顺序 / Symbol order
*/
data class QuadraticMatrixForm<T>(
    val q: List<List<T>>,
    val c: List<T>,
    val d: T,
    val order: List<Symbol>
) where T : Ring<T>

/**
 * 验证符号顺序列表无重复符号
 * Validate that the symbol order list contains no duplicate symbols
 *
 * @param order 符号顺序列表 / Symbol order list
 * @throws IllegalArgumentException 若存在重复符号 / If duplicate symbols exist
*/
private fun validateOrder(order: List<Symbol>) {
    require(order.toSet().size == order.size) {
        "Symbol order contains duplicated symbols."
    }
}

/**
 * Validate symbol order and return a result type.
 * 验证符号顺序并返回结果类型。
 *
 * @param order the symbol order list to validate / 要验证的符号顺序列表
 * @return ok if valid, Failed if duplicate symbols exist / 验证通过返回 ok，存在重复符号返回 Failed
*/
private fun validateOrderRet(order: List<Symbol>): Try {
    return if (order.toSet().size == order.size) {
        ok
    } else {
        Failed(ErrorCode.IllegalArgument, "Symbol order contains duplicated symbols.")
    }
}

/**
 * Require that a symbol index exists in the given map.
 * 要求符号索引在给定映射中存在。
 *
 * @param symbol the symbol to look up / 要查找的符号
 * @param indexOfSymbol the map from symbol to index / 符号到索引的映射
 * @return the index of the symbol, or Failed if not found / 符号的索引，未找到则返回 Failed
*/
private fun requireSymbolIndex(
    symbol: Symbol,
    indexOfSymbol: Map<Symbol, Int>
): Ret<Int> {
    return indexOfSymbol[symbol]
        ?.let { Ok(it) }
        ?: Failed(ErrorCode.DataNotFound, "Symbol ${symbol.name} not found in order.")
}

/**
 * 验证线性矩阵形式的维度一致性
 * Validate dimension consistency of linear matrix form
 *
 * @param c 系数向量 / Coefficient vector
 * @param order 符号顺序列表 / Symbol order list
 * @throws IllegalArgumentException 若维度不匹配 / If dimensions mismatch
*/
private fun <T> validateLinearMatrixDimensions(c: List<T>, order: List<Symbol>) {
    require(c.size == order.size) {
        "Linear matrix form dimension mismatch: c.size=${c.size}, order.size=${order.size}."
    }
}

/**
 * 验证二次矩阵形式的维度一致性
 * Validate dimension consistency of quadratic matrix form
 *
 * @param q 二次项系数矩阵 / Quadratic coefficient matrix
 * @param c 一次项系数向量 / Linear coefficient vector
 * @param order 符号顺序列表 / Symbol order list
 * @throws IllegalArgumentException 若维度不匹配 / If dimensions mismatch
*/
private fun <T> validateQuadraticMatrixDimensions(q: List<List<T>>, c: List<T>, order: List<Symbol>) {
    val n = order.size
    require(q.size == n) {
        "Quadratic matrix form dimension mismatch: q.size=${q.size}, order.size=$n."
    }
    require(q.all { it.size == n }) {
        "Quadratic matrix form requires square q with size order.size=$n."
    }
    require(c.size == n) {
        "Quadratic matrix form dimension mismatch: c.size=${c.size}, order.size=$n."
    }
}

/**
 * 将线性多项式转换为矩阵形式
 * Convert a linear polynomial to matrix form
 *
 * @param order 符号顺序 / Symbol order
 * @param zero 零值 / Zero value
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param isZero 判断零的函数 / Function to check if value is zero
 * @return 线性矩阵形式 / Linear matrix form
*/
fun <T> LinearPolynomial<T>.toMatrixForm(
    order: List<Symbol>,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero }
): Ret<LinearMatrixForm<T>> where T : Ring<T> {
    when (val result = validateOrderRet(order)) {
        is Ok -> {}
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val source = if (combineTerms) {
        this.combineLinearTerms(zero = zero, isZero = isZero)
    } else {
        this
    }
    val c = MutableList(order.size) { zero }
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    for (monomial in source.monomials) {
        val i = when (val result = requireSymbolIndex(monomial.symbol, indexOfSymbol)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        c[i] = c[i] + monomial.coefficient
    }
    return Ok(LinearMatrixForm(c = c, d = source.constant, order = order))
}

/**
 * 从矩阵形式还原线性多项式
 * Reconstruct a linear polynomial from matrix form
 *
 * @param c 系数向量 / Coefficient vector
 * @param d 常数项 / Constant term
 * @param order 符号顺序 / Symbol order
 * @param zero 零值 / Zero value
 * @param isZero 判断零的函数 / Function to check if value is zero
 * @return 线性多项式 / Linear polynomial
*/
fun <T> linearPolynomialFromMatrixForm(
    c: List<T>,
    d: T,
    order: List<Symbol>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T> where T : Ring<T> {
    validateOrder(order)
    validateLinearMatrixDimensions(c, order)
    val monomials = ArrayList<LinearMonomial<T>>(order.size)
    for (i in order.indices) {
        if (!isZero(c[i])) {
            monomials.add(LinearMonomial(coefficient = c[i], symbol = order[i]))
        }
    }
    return LinearPolynomial(
        monomials = monomials,
        constant = d
    ).combineLinearTerms(zero, isZero)
}

/**
 * 从线性矩阵形式还原多项式（便捷重载）
 * Reconstruct a polynomial from linear matrix form (convenience overload)
 *
 * @param form 线性矩阵形式 / Linear matrix form
 * @param zero 零值 / Zero value
 * @param isZero 判断零的函数 / Function to check if value is zero
 * @return 线性多项式 / Linear polynomial
*/
fun <T> linearPolynomialFromMatrixForm(
    form: LinearMatrixForm<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T> where T : Ring<T> {
    return linearPolynomialFromMatrixForm(
        c = form.c,
        d = form.d,
        order = form.order,
        zero = zero,
        isZero = isZero
    )
}

/**
 * 将二次多项式转换为矩阵形式
 * Convert a quadratic polynomial to matrix form
 *
 * @param order 符号顺序 / Symbol order
 * @param zero 零值 / Zero value
 * @param splitOffDiagonal 非对角项拆分函数 / Off-diagonal split function
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param isZero 判断零的函数 / Function to check if value is zero
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 二次矩阵形式 / Quadratic matrix form
*/
fun <T> QuadraticPolynomial<T>.toMatrixForm(
    order: List<Symbol>,
    zero: T,
    splitOffDiagonal: (T) -> Pair<T, T>,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticMatrixForm<T>> where T : Ring<T> {
    when (val result = validateOrderRet(order)) {
        is Ok -> {}
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val source = if (combineTerms) {
        this.combineQuadraticTerms(zero = zero, isZero = isZero, symbolComparator = symbolComparator)
    } else {
        this
    }
    val n = order.size
    val q = MutableList(n) { MutableList(n) { zero } }
    val c = MutableList(n) { zero }
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    for (monomial in source.monomials) {
        if (monomial.isQuadratic) {
            val symbol2 = monomial.symbol2!!
            val i = when (val result = requireSymbolIndex(monomial.symbol1, indexOfSymbol)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val j = when (val result = requireSymbolIndex(symbol2, indexOfSymbol)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (i == j) {
                q[i][j] = q[i][j] + monomial.coefficient
            } else {
                val (left, right) = splitOffDiagonal(monomial.coefficient)
                q[i][j] = q[i][j] + left
                q[j][i] = q[j][i] + right
            }
        } else {
            val i = when (val result = requireSymbolIndex(monomial.symbol1, indexOfSymbol)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            c[i] = c[i] + monomial.coefficient
        }
    }
    return Ok(QuadraticMatrixForm(q = q, c = c, d = source.constant, order = order))
}

/**
 * 将规范多项式转换为矩阵形式（需为二次以下）
 * Convert a canonical polynomial to matrix form (must be at most quadratic)
 *
 * @param order 符号顺序 / Symbol order
 * @param zero 零值 / Zero value
 * @param splitOffDiagonal 非对角项拆分函数 / Off-diagonal split function
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param isZero 判断零的函数 / Function to check if value is zero
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 二次矩阵形式 / Quadratic matrix form
 * @throws IllegalArgumentException 若多项式超过二次 / If polynomial exceeds quadratic
*/
fun <T> CanonicalPolynomial<T>.toMatrixForm(
    order: List<Symbol>,
    zero: T,
    splitOffDiagonal: (T) -> Pair<T, T>,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticMatrixForm<T>> where T : Ring<T> {
    when (val result = validateOrderRet(order)) {
        is Ok -> {}
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val source = if (combineTerms) {
        this.combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
    } else {
        this
    }
    val quadratic = source.toQuadraticPolynomialOrNull(
        zero = zero,
        isZero = isZero,
        symbolComparator = symbolComparator
    ) ?: return Failed(ErrorCode.IllegalArgument, "Canonical polynomial is not quadratic.")
    return quadratic.toMatrixForm(
        order = order,
        zero = zero,
        splitOffDiagonal = splitOffDiagonal,
        combineTerms = false,
        isZero = isZero,
        symbolComparator = symbolComparator
    )
}

/**
 * 从矩阵形式还原二次多项式
 * Reconstruct a quadratic polynomial from matrix form
 *
 * @param q 二次项系数矩阵 / Quadratic coefficient matrix
 * @param c 一次项系数向量 / Linear coefficient vector
 * @param d 常数项 / Constant term
 * @param order 符号顺序 / Symbol order
 * @param zero 零值 / Zero value
 * @param isZero 判断零的函数 / Function to check if value is zero
 * @param mergeOffDiagonal 非对角项合并函数 / Off-diagonal merge function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 二次多项式 / Quadratic polynomial
*/
fun <T> quadraticPolynomialFromMatrixForm(
    q: List<List<T>>,
    c: List<T>,
    d: T,
    order: List<Symbol>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    mergeOffDiagonal: (T, T) -> T = { lhs, rhs -> lhs + rhs },
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticPolynomial<T> where T : Ring<T> {
    validateOrder(order)
    validateQuadraticMatrixDimensions(q, c, order)
    val monomials = ArrayList<QuadraticMonomial<T>>(order.size * (order.size + 1) / 2 + order.size)
    for (i in order.indices) {
        if (!isZero(q[i][i])) {
            monomials.add(
                QuadraticMonomial(
                    coefficient = q[i][i],
                    symbol1 = order[i],
                    symbol2 = order[i]
                )
            )
        }
        if (!isZero(c[i])) {
            monomials.add(
                QuadraticMonomial(
                    coefficient = c[i],
                    symbol1 = order[i],
                    symbol2 = null
                )
            )
        }
        for (j in (i + 1) until order.size) {
            val coefficient = mergeOffDiagonal(q[i][j], q[j][i])
            if (!isZero(coefficient)) {
                monomials.add(
                    QuadraticMonomial(
                        coefficient = coefficient,
                        symbol1 = order[i],
                        symbol2 = order[j]
                    )
                )
            }
        }
    }
    return QuadraticPolynomial(
        monomials = monomials,
        constant = d
    ).combineQuadraticTerms(
        zero = zero,
        isZero = isZero,
        symbolComparator = symbolComparator
    )
}

/**
 * 从二次矩阵形式还原多项式（便捷重载）
 * Reconstruct a polynomial from quadratic matrix form (convenience overload)
 *
 * @param form 二次矩阵形式 / Quadratic matrix form
 * @param zero 零值 / Zero value
 * @param isZero 判断零的函数 / Function to check if value is zero
 * @param mergeOffDiagonal 非对角项合并函数 / Off-diagonal merge function
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 二次多项式 / Quadratic polynomial
*/
fun <T> quadraticPolynomialFromMatrixForm(
    form: QuadraticMatrixForm<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    mergeOffDiagonal: (T, T) -> T = { lhs, rhs -> lhs + rhs },
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticPolynomial<T> where T : Ring<T> {
    return quadraticPolynomialFromMatrixForm(
        q = form.q,
        c = form.c,
        d = form.d,
        order = form.order,
        zero = zero,
        isZero = isZero,
        mergeOffDiagonal = mergeOffDiagonal,
        symbolComparator = symbolComparator
    )
}
