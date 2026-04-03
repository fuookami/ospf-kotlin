package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.operator.Order

// ============================================================================
// 一元二次多项式系数提取 / Univariate quadratic polynomial coefficient extraction
// ============================================================================

/**
 * 一元二次多项式系数
 * Univariate quadratic polynomial coefficients
 *
 * 表示形式：ax² + bx + c
 * Representation: ax² + bx + c
 */
data class QuadraticCoefficients<T>(
    val a: T,  // 二次项系数 / Quadratic coefficient
    val b: T,  // 一次项系数 / Linear coefficient
    val c: T,  // 常数项 / Constant term
    val symbol: Symbol  // 变量符号 / Variable symbol
)

/**
 * 从 QuadraticPolynomial 提取一元二次多项式系数
 * Extract univariate quadratic polynomial coefficients from QuadraticPolynomial
 *
 * 要求多项式只包含一个变量符号。
 * Requires the polynomial to contain only one variable symbol.
 *
 * @return 系数元组 (a, b, c, symbol)，如果多项式不是单变量的则返回 null
 * @return Coefficient tuple (a, b, c, symbol), or null if not univariate
 */
fun <T> QuadraticPolynomial<T>.extractUnivariateCoefficients(): QuadraticCoefficients<T>?
    where T : Field<T>, T : RealNumber<T> {

    // 收集所有符号 / Collect all symbols
    val symbols = mutableSetOf<Symbol>()
    for (m in monomials) {
        symbols.add(m.symbol1)
        if (m.symbol2 != null && m.symbol2 != m.symbol1) {
            symbols.add(m.symbol2)
        }
    }

    // 必须只有一个变量 / Must have exactly one variable
    if (symbols.size != 1) return null

    val symbol = symbols.first()
    var a = constant.constants.zero  // x² 系数
    var b = constant.constants.zero  // x 系数
    val c = constant                 // 常数项

    for (m in monomials) {
        val isQuadratic = m.symbol2 != null
        if (isQuadratic) {
            // 二次项 x² / Quadratic term x²
            a = a + m.coefficient
        } else {
            // 一次项 x / Linear term x
            b = b + m.coefficient
        }
    }

    return QuadraticCoefficients(a, b, c, symbol)
}

// ============================================================================
// 求根 / Root finding
// ============================================================================

/**
 * 多项式的根
 * Roots of a polynomial
 */
data class PolynomialRoots<T>(
    val roots: List<T>,
    val discriminant: T,
    val isReal: Boolean
)

/**
 * 一元二次多项式求根（使用求根公式）
 * Find roots of univariate quadratic polynomial (using quadratic formula)
 *
 * 公式 / Formula: x = (-b ± √(b² - 4ac)) / 2a
 *
 * @param coefficients 二次多项式系数 / Quadratic polynomial coefficients
 * @return 根的列表（0、1 或 2 个实根）/ List of roots (0, 1, or 2 real roots)
 */
fun <T> solveQuadratic(coefficients: QuadraticCoefficients<T>): PolynomialRoots<T>
    where T : Field<T>, T : FloatingNumber<T> {

    val (a, b, c, _) = coefficients
    val constants = a.constants

    // 特殊情况：a = 0，退化为线性方程 / Special case: a = 0, degenerates to linear
    if (a eq constants.zero) {
        if (b eq constants.zero) {
            // 无解或无穷多解 / No solution or infinite solutions
            return PolynomialRoots(emptyList(), constants.zero, true)
        }
        // 线性方程 bx + c = 0，解为 x = -c/b
        // Linear equation bx + c = 0, solution is x = -c/b
        return PolynomialRoots(listOf(-c / b), constants.zero, true)
    }

    // 计算判别式 Δ = b² - 4ac
    // Calculate discriminant Δ = b² - 4ac
    val discriminant = b * b - (constants.two * constants.two) * a * c

    // 判断判别式符号 / Determine discriminant sign
    when (discriminant ord constants.zero) {
        is Order.Less -> {
            // 无实根 / No real roots
            return PolynomialRoots(emptyList(), discriminant, false)
        }
        is Order.Equal -> {
            // 一个重根 / One repeated root
            val root = -b / (constants.two * a)
            return PolynomialRoots(listOf(root), discriminant, true)
        }
        else -> {
            // 两个不同的实根 / Two distinct real roots
            @Suppress("UNCHECKED_CAST")
            val sqrtD = discriminant.sqrt() as T
            val twoA = constants.two * a
            val root1 = (-b + sqrtD) / twoA
            val root2 = (-b - sqrtD) / twoA
            return PolynomialRoots(listOf(root1, root2), discriminant, true)
        }
    }
}

/**
 * 一元二次多项式求根（便捷方法）
 * Find roots of univariate quadratic polynomial (convenience method)
 */
fun <T> QuadraticPolynomial<T>.solve(): PolynomialRoots<T>?
    where T : Field<T>, T : FloatingNumber<T> {

    // 处理边界情况：无单项式 / Handle edge case: no monomials
    if (monomials.isEmpty()) {
        val constants = constant.constants
        // 零多项式或常数多项式 / Zero polynomial or constant polynomial
        return PolynomialRoots(emptyList(), constants.zero, true)
    }

    val coefficients = extractUnivariateCoefficients() ?: return null
    return solveQuadratic(coefficients)
}

// ============================================================================
// 因式分解 / Factorization
// ============================================================================

/**
 * 因式分解结果
 * Factorization result
 *
 * 表示形式：a(x - r₁)(x - r₂)
 * Representation: a(x - r₁)(x - r₂)
 */
data class QuadraticFactorization<T>(
    val leadingCoefficient: T,  // 首项系数 / Leading coefficient
    val factors: List<LinearFactor<T>>,  // 一次因式 / Linear factors
    val symbol: Symbol
)

/**
 * 一次因式 (x - root)
 * Linear factor (x - root)
 */
data class LinearFactor<T>(
    val root: T  // 根 / Root
)

/**
 * 一元二次多项式因式分解
 * Factorize univariate quadratic polynomial
 *
 * 将 ax² + bx + c 分解为 a(x - r₁)(x - r₂) 的形式。
 * Factorizes ax² + bx + c into the form a(x - r₁)(x - r₂).
 *
 * @param coefficients 二次多项式系数 / Quadratic polynomial coefficients
 * @return 因式分解结果，如果无法分解则返回 null
 * @return Factorization result, or null if cannot factorize
 */
fun <T> factorizeQuadratic(coefficients: QuadraticCoefficients<T>): QuadraticFactorization<T>?
    where T : Field<T>, T : FloatingNumber<T> {

    val (a, b, c, symbol) = coefficients
    val constants = a.constants

    // 特殊情况：a = 0，退化为线性 / Special case: a = 0, degenerates to linear
    if (a eq constants.zero) {
        if (b eq constants.zero) {
            return null  // 常数，无法因式分解 / Constant, cannot factorize
        }
        // 线性因式 bx + c = b(x + c/b)
        // Linear factor bx + c = b(x + c/b)
        val root = -c / b
        return QuadraticFactorization(b, listOf(LinearFactor(root)), symbol)
    }

    // 求根 / Find roots
    val roots = solveQuadratic(coefficients)

    if (!roots.isReal || roots.roots.isEmpty()) {
        return null  // 无实根，无法在实数域因式分解 / No real roots, cannot factorize over reals
    }

    // 构造因式 / Build factors
    val factors = roots.roots.map { LinearFactor(it) }

    return QuadraticFactorization(a, factors, symbol)
}

/**
 * 一元二次多项式因式分解（便捷方法）
 * Factorize univariate quadratic polynomial (convenience method)
 */
fun <T> QuadraticPolynomial<T>.factorize(): QuadraticFactorization<T>?
    where T : Field<T>, T : FloatingNumber<T> {

    // 处理边界情况：无单项式 / Handle edge case: no monomials
    if (monomials.isEmpty()) {
        return null  // 常数无法因式分解 / Constant cannot be factorized
    }

    val coefficients = extractUnivariateCoefficients() ?: return null
    return factorizeQuadratic(coefficients)
}

// ============================================================================
// 因式展开 / Factor expansion
// ============================================================================

/**
 * 将因式分解结果展开回多项式
 * Expand factorization result back to polynomial
 */
fun <T> QuadraticFactorization<T>.expand(): QuadraticPolynomial<T>
    where T : Field<T>, T : RealNumber<T> {

    val constants = leadingCoefficient.constants

    return when (factors.size) {
        0 -> QuadraticPolynomial(emptyList(), leadingCoefficient)
        1 -> {
            // 线性因式：a(x - r) = ax - ar
            val root = factors[0].root
            val linearMonomial = LinearMonomial(leadingCoefficient, symbol)
            val constant = -leadingCoefficient * root
            QuadraticPolynomial(
                listOf(QuadraticMonomial.linear(leadingCoefficient, symbol)),
                constant
            )
        }
        2 -> {
            // 二次因式：a(x - r₁)(x - r₂) = a(x² - (r₁+r₂)x + r₁r₂)
            val r1 = factors[0].root
            val r2 = factors[1].root

            // x² 系数：a
            val quadraticMonomial = QuadraticMonomial.quadratic(leadingCoefficient, symbol, symbol)

            // x 系数：-a(r₁ + r₂)
            val linearCoeff = -leadingCoefficient * (r1 + r2)
            val linearMonomial = QuadraticMonomial.linear(linearCoeff, symbol)

            // 常数项：a * r₁ * r₂
            val constant = leadingCoefficient * r1 * r2

            QuadraticPolynomial(listOf(quadraticMonomial, linearMonomial), constant)
        }
        else -> QuadraticPolynomial(emptyList(), constants.zero)
    }
}