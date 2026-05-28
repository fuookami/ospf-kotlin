/**
 * 多项式转换接双
 * Polynomial Conversion Interfaces
 *
 * 定义可转换为线性、二次、规范多项式的统一接口。
 * 讌variable、symbol、monomial、polynomial 等类型实现对应接口，
 * 以统一模型层的 addObject / minimize / maximize / addConstraint 入口。
 *
 * 接口继承关系（同阶自动实现），
 * - ToLinearPolynomial extends TryToLinearPolynomial
 *   （非空版实现可空版：toLinearPolynomialOrNull() = toLinearPolynomial()，
 * - ToQuadraticPolynomial extends TryToQuadraticPolynomial
 *   （非空版实现可空版：toQuadraticPolynomialOrNull() = toQuadraticPolynomial()，
 * - ToCanonicalPolynomial extends TryToCanonicalPolynomial
 *   （非空版实现可空版：toCanonicalPolynomialOrNull() = toCanonicalPolynomial()，
 *
 * Defines unified interfaces for conversion to linear, quadratic, and canonical polynomials.
 * Enables variable, symbol, monomial, and polynomial types to implement corresponding interfaces,
 * unifying model layer entry points for addObject / minimize / maximize / addConstraint.
 *
 * Interface inheritance (same-level auto-implementation):
 * - ToLinearPolynomial extends TryToLinearPolynomial
 *   (non-null version implements nullable: toLinearPolynomialOrNull() = toLinearPolynomial())
 * - ToQuadraticPolynomial extends TryToQuadraticPolynomial
 *   (non-null version implements nullable: toQuadraticPolynomialOrNull() = toQuadraticPolynomial())
 * - ToCanonicalPolynomial extends TryToCanonicalPolynomial
 *   (non-null version implements nullable: toCanonicalPolynomialOrNull() = toCanonicalPolynomial())
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring

/**
 * 可尝试转换为线性多项式（降阶）
 * Try-convertible to linear polynomial (demotion)
 *
 * 降阶转换可能失败（如二次多项式含交叉项时无法转为线性），
 * 因此返回可空类型。这是独立的降阶接口。
 * 调用方可使用 ConvertOps.kt 中的带参数扩展函数进行精确控制。
 * Demotion conversion may fail (e.g., quadratic polynomial with cross terms
 * cannot convert to linear), so the return type is nullable.
 * Callers can use parameterized extension functions in ConvertOps.kt for precise control.
 */
interface TryToLinearPolynomial<T : Ring<T>> {
    fun toLinearPolynomialOrNull(): LinearPolynomial<T>?
}

/**
 * 可尝试转换为二次多项式（降阶，
 * Try-convertible to quadratic polynomial (demotion)
 *
 * 降阶转换可能失败（如规范多项式含三次以上项时无法转为二次），
 * 因此返回可空类型。
 * Demotion conversion may fail (e.g., canonical polynomial with degree > 2
 * cannot convert to quadratic), so the return type is nullable.
 */
interface TryToQuadraticPolynomial<T : Ring<T>> {
    fun toQuadraticPolynomialOrNull(): QuadraticPolynomial<T>?
}

/**
 * 可尝试转换为规范多项弌
 * Try-convertible to canonical polynomial
 *
 * 规范型是最高形式，通常升阶不会失败。
 * 此接口为完整性保留，未来可能有需覌symbolComparator 等参数的转换场景。
 * Canonical is the highest form; promotion typically never fails.
 * This interface is kept for completeness; future scenarios may require
 * conversions with symbolComparator or other parameters.
 */
interface TryToCanonicalPolynomial<T : Ring<T>> {
    fun toCanonicalPolynomialOrNull(): CanonicalPolynomial<T>?
}

/**
 * 可转换为线性多项式
 * Convertible to linear polynomial
 *
 * 任何实现了此接口的类型都可以转换为线性多项式，
 * 从而统一作为线性模型的目标函数或约束的输入。
 * 继承 TryToLinearPolynomial，用 toLinearPolynomial() 实现 toLinearPolynomialOrNull()。
 * Any type implementing this interface can be converted to a linear polynomial,
 * serving as a unified input for linear model objectives or constraints.
 * Extends TryToLinearPolynomial, implementing toLinearPolynomialOrNull() via toLinearPolynomial().
 */
interface ToLinearPolynomial<T : Ring<T>> : TryToLinearPolynomial<T> {
    fun toLinearPolynomial(): LinearPolynomial<T>
    override fun toLinearPolynomialOrNull(): LinearPolynomial<T> = toLinearPolynomial()
}

/**
 * 可转换为二次多项弌
 * Convertible to quadratic polynomial
 *
 * 任何实现了此接口的类型都可以转换为二次多项式，
 * 从而统一作为二次模型的目标函数或约束的输入。
 * 继承 TryToQuadraticPolynomial，用 toQuadraticPolynomial() 实现 toQuadraticPolynomialOrNull()。
 * Any type implementing this interface can be converted to a quadratic polynomial,
 * serving as a unified input for quadratic model objectives or constraints.
 * Extends TryToQuadraticPolynomial, implementing toQuadraticPolynomialOrNull() via toQuadraticPolynomial().
 */
interface ToQuadraticPolynomial<T : Ring<T>> : TryToQuadraticPolynomial<T> {
    fun toQuadraticPolynomial(): QuadraticPolynomial<T>
    override fun toQuadraticPolynomialOrNull(): QuadraticPolynomial<T> = toQuadraticPolynomial()
}

/**
 * 可转换为规范多项弌
 * Convertible to canonical polynomial
 *
 * 任何实现了此接口的类型都可以转换为规范多项式，
 * 规范多项式是最通用的多项式表示形式，支持任意次数。
 * 继承 TryToCanonicalPolynomial，用 toCanonicalPolynomial() 实现 toCanonicalPolynomialOrNull()。
 * Any type implementing this interface can be converted to a canonical polynomial,
 * which is the most general polynomial representation supporting any degree.
 * Extends TryToCanonicalPolynomial, implementing toCanonicalPolynomialOrNull() via toCanonicalPolynomial().
 */
interface ToCanonicalPolynomial<T : Ring<T>> : TryToCanonicalPolynomial<T> {
    fun toCanonicalPolynomial(): CanonicalPolynomial<T>
    override fun toCanonicalPolynomialOrNull(): CanonicalPolynomial<T> = toCanonicalPolynomial()
}
