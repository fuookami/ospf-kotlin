/**
 * ONNX 表达式中间表示 / ONNX expression intermediate representation
*/
package fuookami.ospf.kotlin.core.plugin.onnx.lowering

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*

/**
 * 降阶用的标量表达式 / Scalar expression used during lowering
 *
 * 只允许常数、线性与二次三种形态，次数上限由 [degree] 表达。 / Only constant, linear and quadratic forms are allowed; the degree ceiling is expressed by [degree].
*/
sealed interface OnnxExpr {

    /** 表达式次数 / Expression degree */
    val degree: Int

    /** 常数表达式 / Constant expression */
    data class Constant(val value: Flt64) : OnnxExpr {
        override val degree: Int
            get() = 0
    }

    /** 线性表达式 / Linear expression */
    data class Linear(val polynomial: LinearPolynomial<Flt64>) : OnnxExpr {
        override val degree: Int
            get() = if (polynomial.monomials.isEmpty()) 0 else 1
    }

    /** 二次表达式 / Quadratic expression */
    data class Quadratic(val polynomial: QuadraticPolynomial<Flt64>) : OnnxExpr {
        override val degree: Int
            get() = if (polynomial.monomials.any { it.isQuadratic }) 2 else 1
    }
}

/**
 * 表达式代数运算 / Expression algebra
 *
 * 所有运算在次数超限时返回失败，不会静默丢弃高阶项。 / Every operation fails when the degree limit is exceeded instead of silently dropping high-order terms.
*/
internal object OnnxExprAlgebra {

    /** 加法 / Addition */
    fun add(lhs: OnnxExpr, rhs: OnnxExpr, mode: OnnxDegreeMode, nodeName: String): OnnxExprResult {
        return when {
            lhs is OnnxExpr.Constant && rhs is OnnxExpr.Constant ->
                OnnxExprResult.Success(OnnxExpr.Constant(lhs.value + rhs.value))

            lhs is OnnxExpr.Constant && rhs is OnnxExpr.Linear ->
                OnnxExprResult.Success(OnnxExpr.Linear(rhs.polynomial + lhs.value))

            lhs is OnnxExpr.Linear && rhs is OnnxExpr.Constant ->
                OnnxExprResult.Success(OnnxExpr.Linear(lhs.polynomial + rhs.value))

            lhs is OnnxExpr.Linear && rhs is OnnxExpr.Linear ->
                OnnxExprResult.Success(OnnxExpr.Linear(lhs.polynomial + rhs.polynomial))

            else -> addQuadratic(lhs, rhs, mode, nodeName)
        }
    }

    /** 减法 / Subtraction */
    fun sub(lhs: OnnxExpr, rhs: OnnxExpr, mode: OnnxDegreeMode, nodeName: String): OnnxExprResult {
        return when {
            lhs is OnnxExpr.Constant && rhs is OnnxExpr.Constant ->
                OnnxExprResult.Success(OnnxExpr.Constant(lhs.value - rhs.value))

            lhs is OnnxExpr.Constant && rhs is OnnxExpr.Linear ->
                OnnxExprResult.Success(OnnxExpr.Linear(-rhs.polynomial + lhs.value))

            lhs is OnnxExpr.Linear && rhs is OnnxExpr.Constant ->
                OnnxExprResult.Success(OnnxExpr.Linear(lhs.polynomial - rhs.value))

            lhs is OnnxExpr.Linear && rhs is OnnxExpr.Linear ->
                OnnxExprResult.Success(OnnxExpr.Linear(lhs.polynomial - rhs.polynomial))

            else -> subQuadratic(lhs, rhs, mode, nodeName)
        }
    }

    /** 乘法 / Multiplication */
    fun mul(lhs: OnnxExpr, rhs: OnnxExpr, mode: OnnxDegreeMode, nodeName: String): OnnxExprResult {
        val degree = lhs.degree + rhs.degree
        if (degree > mode.maxDegree) {
            return OnnxExprResult.Failure(
                OnnxErrorFactory.degreeOverflow(
                    detail = "$mode 模式下乘积次数 $degree 超过上限 ${mode.maxDegree} / " +
                        "product degree $degree exceeds the $mode limit ${mode.maxDegree}",
                    nodeName = nodeName
                )
            )
        }
        return when {
            lhs is OnnxExpr.Constant && rhs is OnnxExpr.Constant ->
                OnnxExprResult.Success(OnnxExpr.Constant(lhs.value * rhs.value))

            lhs is OnnxExpr.Constant -> scale(rhs, lhs.value)
            rhs is OnnxExpr.Constant -> scale(lhs, rhs.value)
            lhs is OnnxExpr.Linear && rhs is OnnxExpr.Linear ->
                OnnxExprResult.Success(OnnxExpr.Quadratic(quadraticProduct(lhs.polynomial, rhs.polynomial)))
            else -> OnnxExprResult.Failure(
                OnnxErrorFactory.degreeOverflow(
                    detail = "乘积形态不受支持 / unsupported product form",
                    nodeName = nodeName
                )
            )
        }
    }

    /** 取负 / Negation */
    fun negate(expr: OnnxExpr): OnnxExpr {
        return when (expr) {
            is OnnxExpr.Constant -> OnnxExpr.Constant(-expr.value)
            is OnnxExpr.Linear -> OnnxExpr.Linear(-expr.polynomial)
            is OnnxExpr.Quadratic -> OnnxExpr.Quadratic(-expr.polynomial)
        }
    }

    /** 线性多项式加法（把二次输入提升到二次域）/ Polynomial addition promoting to the quadratic domain */
    private fun addQuadratic(lhs: OnnxExpr, rhs: OnnxExpr, mode: OnnxDegreeMode, nodeName: String): OnnxExprResult {
        val promoted = maxOf(lhs.degree, rhs.degree)
        if (promoted > mode.maxDegree) {
            return OnnxExprResult.Failure(
                OnnxErrorFactory.degreeOverflow(
                    detail = "$mode 模式不允许次数 $promoted / $mode mode does not allow degree $promoted",
                    nodeName = nodeName
                )
            )
        }
        return OnnxExprResult.Success(OnnxExpr.Quadratic(toQuadratic(lhs) + toQuadratic(rhs)))
    }

    /** 二次域减法 / Subtraction in the quadratic domain */
    private fun subQuadratic(lhs: OnnxExpr, rhs: OnnxExpr, mode: OnnxDegreeMode, nodeName: String): OnnxExprResult {
        val promoted = maxOf(lhs.degree, rhs.degree)
        if (promoted > mode.maxDegree) {
            return OnnxExprResult.Failure(
                OnnxErrorFactory.degreeOverflow(
                    detail = "$mode 模式不允许次数 $promoted / $mode mode does not allow degree $promoted",
                    nodeName = nodeName
                )
            )
        }
        return OnnxExprResult.Success(OnnxExpr.Quadratic(toQuadratic(lhs) - toQuadratic(rhs)))
    }

    /** 常数缩放 / Constant scaling */
    private fun scale(expr: OnnxExpr, factor: Flt64): OnnxExprResult {
        // 乘零直接折叠为常数零，避免在多项式里留下零系数单项式。 / Scaling by zero folds into a constant zero, keeping zero-coefficient monomials out of polynomials.
        if (factor.compareTo(Flt64.zero) == 0) {
            return OnnxExprResult.Success(OnnxExpr.Constant(Flt64.zero))
        }
        return when (expr) {
            is OnnxExpr.Constant -> OnnxExprResult.Success(OnnxExpr.Constant(expr.value * factor))
            is OnnxExpr.Linear -> OnnxExprResult.Success(OnnxExpr.Linear(expr.polynomial * factor))
            is OnnxExpr.Quadratic -> OnnxExprResult.Success(OnnxExpr.Quadratic(expr.polynomial * factor))
        }
    }

    /** 提升为二次多项式 / Promote into a quadratic polynomial */
    fun toQuadratic(expr: OnnxExpr): QuadraticPolynomial<Flt64> {
        return when (expr) {
            is OnnxExpr.Constant -> QuadraticPolynomial(emptyList(), expr.value)
            is OnnxExpr.Linear -> expr.polynomial.toQuadraticPolynomial()
            is OnnxExpr.Quadratic -> expr.polynomial
        }
    }

    /** 降为线性多项式，二次项非零时返回 null / Reduce into a linear polynomial, null when quadratic terms exist */
    fun toLinear(expr: OnnxExpr): LinearPolynomial<Flt64>? {
        return when (expr) {
            is OnnxExpr.Constant -> LinearPolynomial(emptyList(), expr.value)
            is OnnxExpr.Linear -> expr.polynomial
            is OnnxExpr.Quadratic -> if (expr.polynomial.monomials.any { it.isQuadratic }) {
                null
            } else {
                LinearPolynomial(
                    monomials = expr.polynomial.monomials.map { LinearMonomial(it.coefficient, it.symbol1) },
                    constant = expr.polynomial.constant
                )
            }
        }
    }

    /** 线性多项式乘积展开为二次多项式 / Expand a product of linear polynomials into a quadratic polynomial */
    private fun quadraticProduct(
        lhs: LinearPolynomial<Flt64>,
        rhs: LinearPolynomial<Flt64>
    ): QuadraticPolynomial<Flt64> {
        val monomials = mutableListOf<QuadraticMonomial<Flt64>>()
        for (left in lhs.monomials) {
            for (right in rhs.monomials) {
                monomials.add(
                    QuadraticMonomial(left.coefficient * right.coefficient, left.symbol, right.symbol)
                )
            }
            monomials.add(
                QuadraticMonomial(left.coefficient * rhs.constant, left.symbol)
            )
        }
        for (right in rhs.monomials) {
            monomials.add(
                QuadraticMonomial(right.coefficient * lhs.constant, right.symbol)
            )
        }
        return QuadraticPolynomial(monomials, lhs.constant * rhs.constant)
    }
}

/**
 * 表达式运算结果 / Expression operation result
*/
internal sealed interface OnnxExprResult {

    /** 成功 / Success */
    data class Success(val expr: OnnxExpr) : OnnxExprResult

    /** 失败 / Failure */
    data class Failure(
        val error: fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
    ) : OnnxExprResult
}
