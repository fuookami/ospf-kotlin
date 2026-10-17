/**
 * 多项式构造辅助。 / Polynomial construction helpers.
 *
 * core 的 `sum` 在空单项式列表上会抛出 `IllegalStateException`。列生成在尚未加列时“负载为空”是正常状态，
 * 因此统一在此拦截，避免异常穿透到求解流程（见 `.rules/error-handling.md`）。
 * / The core `sum` throws `IllegalStateException` on an empty monomial list. An empty load before any column is
 * added is a normal state in column generation, so it is handled here to keep the exception out of the solving
 * flow (see `.rules/error-handling.md`).
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum

/**
 * 由单项式列表构造线性多项式，空列表返回零多项式。 / Build a linear polynomial from monomials; an empty list
 * yields the zero polynomial.
 *
 * @param monomials 单项式列表 / monomial list
 * @return 线性多项式 / linear polynomial
*/
internal fun safeLinearPolynomial(monomials: List<LinearMonomial<FltX>>): LinearPolynomial<FltX> {
    if (monomials.isEmpty()) {
        return LinearPolynomial(emptyList(), FltX.zero)
    }
    return sum(monomials)
}
/**
 * 过滤出非零系数的单项式。 / Filter out monomials whose coefficient is zero.
 *
 * 零系数单项式没有建模意义，且会让空负载判断失效，因此统一在此剔除。
 * / Zero-coefficient monomials carry no modelling meaning and would defeat the empty-load check, so they are
 * dropped here.
 *
 * @param monomials 单项式列表 / monomial list
 * @return 非零系数单项式列表 / monomial list with non-zero coefficients
*/
internal fun safeLinearMonomials(monomials: List<LinearMonomial<FltX>>): List<LinearMonomial<FltX>> {
    return monomials.filter { it.coefficient != FltX.zero }
}