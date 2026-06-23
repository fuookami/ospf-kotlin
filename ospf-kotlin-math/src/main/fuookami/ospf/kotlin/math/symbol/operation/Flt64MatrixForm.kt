/**
 * Flt64 矩阵形式快捷函数
 * Flt64 Matrix Form Convenience Functions
 *
 * 提供 Flt64 多项式与 Double 数组矩阵形式之间的转换。
 * 封装通用矩阵形式运算，使用 Double 数组表示系数矩阵和向量。
 * Provides conversion between Flt64 polynomials and Double array matrix forms.
 * Wraps generic matrix form operations using Double arrays for coefficient matrices and vectors.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret

/**
 * Flt64 线性多项式矩阵形式
 * Flt64 linear polynomial matrix form
 *
 * 使用 Double 数组表示系数向量。
 * Uses Double array for coefficient vector.
 *
 * @property c 系数向量 / Coefficient vector
 * @property d 常数项 / Constant term
 * @property order 符号顺序 / Symbol order
 */
data class Flt64LinearMatrixForm(
    val c: DoubleArray,
    val d: Flt64,
    val order: List<Symbol>
)

/**
 * Flt64 二次多项式矩阵形式
 * Flt64 quadratic polynomial matrix form
 *
 * 使用 Double 数组表示系数矩阵和向量。
 * Uses Double arrays for coefficient matrix and vectors.
 *
 * @property q 二次项系数矩阵 / Quadratic coefficient matrix
 * @property c 一次项系数向量 / Linear coefficient vector
 * @property d 常数项 / Constant term
 * @property order 符号顺序 / Symbol order
 */
data class Flt64QuadraticMatrixForm(
    val q: Array<DoubleArray>,
    val c: DoubleArray,
    val d: Flt64,
    val order: List<Symbol>
)

/**
 * 将 Flt64 线性多项式转换为 Double 数组矩阵形式
 * Convert a Flt64 linear polynomial to Double array matrix form
 *
 * @param order 符号顺序 / Symbol order
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @return Flt64 线性矩阵形式 / Flt64 linear matrix form
 */
fun LinearPolynomial<Flt64>.toFlt64MatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true
): Ret<Flt64LinearMatrixForm> {
    val result: Ret<LinearMatrixForm<Flt64>> = this.toMatrixForm(
        order = order,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { value: Flt64 -> value == Flt64.zero }
    )
    val form = when (result) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    return Ok(Flt64LinearMatrixForm(
        c = form.c.map { it.toDouble() }.toDoubleArray(),
        d = form.d,
        order = form.order
    ))
}

/**
 * 从 Double 数组矩阵形式还原 Flt64 线性多项式
 * Reconstruct a Flt64 linear polynomial from Double array matrix form
 *
 * @param c 系数向量 / Coefficient vector
 * @param d 常数项 / Constant term
 * @param order 符号顺序 / Symbol order
 * @return 线性多项式 / Linear polynomial
 */
fun flt64LinearPolynomialFromMatrixForm(
    c: DoubleArray,
    d: Flt64,
    order: List<Symbol>
): LinearPolynomial<Flt64> {
    return linearPolynomialFromMatrixForm(
        c = c.map { Flt64(it) },
        d = d,
        order = order,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 从 Flt64 线性矩阵形式还原多项式（便捷重载）
 * Reconstruct a polynomial from Flt64 linear matrix form (convenience overload)
 *
 * @param form Flt64 线性矩阵形式 / Flt64 linear matrix form
 * @return 线性多项式 / Linear polynomial
 */
fun flt64LinearPolynomialFromMatrixForm(form: Flt64LinearMatrixForm): LinearPolynomial<Flt64> {
    return flt64LinearPolynomialFromMatrixForm(
        c = form.c,
        d = form.d,
        order = form.order
    )
}

/**
 * 将 Flt64 二次多项式转换为 Double 数组矩阵形式
 * Convert a Flt64 quadratic polynomial to Double array matrix form
 *
 * @param order 符号顺序 / Symbol order
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @return Flt64 二次矩阵形式 / Flt64 quadratic matrix form
 */
fun QuadraticPolynomial<Flt64>.toFlt64MatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true
): Ret<Flt64QuadraticMatrixForm> {
    val result: Ret<QuadraticMatrixForm<Flt64>> = this.toMatrixForm(
        order = order,
        zero = Flt64.zero,
        splitOffDiagonal = { coefficient: Flt64 ->
            val half = coefficient / Flt64.two
            half to half
        },
        combineTerms = combineTerms,
        isZero = { value: Flt64 -> value == Flt64.zero }
    )
    val form = when (result) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    return Ok(Flt64QuadraticMatrixForm(
        q = form.q.map { row -> row.map { it.toDouble() }.toDoubleArray() }.toTypedArray(),
        c = form.c.map { it.toDouble() }.toDoubleArray(),
        d = form.d,
        order = form.order
    ))
}

/**
 * 将 Flt64 规范多项式转换为 Double 数组矩阵形式（需为二次以下）
 * Convert a Flt64 canonical polynomial to Double array matrix form (must be at most quadratic)
 *
 * @param order 符号顺序 / Symbol order
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return Flt64 二次矩阵形式 / Flt64 quadratic matrix form
 */
fun CanonicalPolynomial<Flt64>.toFlt64MatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<Flt64QuadraticMatrixForm> {
    val result: Ret<QuadraticMatrixForm<Flt64>> = this.toMatrixForm(
        order = order,
        zero = Flt64.zero,
        splitOffDiagonal = { coefficient: Flt64 ->
            val half = coefficient / Flt64.two
            half to half
        },
        combineTerms = combineTerms,
        isZero = { value: Flt64 -> value == Flt64.zero },
        symbolComparator = symbolComparator
    )
    val form = when (result) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    return Ok(Flt64QuadraticMatrixForm(
        q = form.q.map { row -> row.map { it.toDouble() }.toDoubleArray() }.toTypedArray(),
        c = form.c.map { it.toDouble() }.toDoubleArray(),
        d = form.d,
        order = form.order
    ))
}

/**
 * 从 Double 数组矩阵形式还原 Flt64 二次多项式
 * Reconstruct a Flt64 quadratic polynomial from Double array matrix form
 *
 * @param q 二次项系数矩阵 / Quadratic coefficient matrix
 * @param c 一次项系数向量 / Linear coefficient vector
 * @param d 常数项 / Constant term
 * @param order 符号顺序 / Symbol order
 * @return 二次多项式 / Quadratic polynomial
 */
fun flt64QuadraticPolynomialFromMatrixForm(
    q: Array<DoubleArray>,
    c: DoubleArray,
    d: Flt64,
    order: List<Symbol>
): QuadraticPolynomial<Flt64> {
    return quadraticPolynomialFromMatrixForm(
        q = q.map { row -> row.map { Flt64(it) } },
        c = c.map { Flt64(it) },
        d = d,
        order = order,
        zero = Flt64.zero,
        isZero = { value: Flt64 -> value == Flt64.zero },
        mergeOffDiagonal = { lhs: Flt64, rhs: Flt64 -> lhs + rhs }
    )
}

/**
 * 从 Flt64 二次矩阵形式还原多项式（便捷重载）
 * Reconstruct a polynomial from Flt64 quadratic matrix form (convenience overload)
 *
 * @param form Flt64 二次矩阵形式 / Flt64 quadratic matrix form
 * @return 二次多项式 / Quadratic polynomial
 */
fun flt64QuadraticPolynomialFromMatrixForm(form: Flt64QuadraticMatrixForm): QuadraticPolynomial<Flt64> {
    return flt64QuadraticPolynomialFromMatrixForm(
        q = form.q,
        c = form.c,
        d = form.d,
        order = form.order
    )
}
