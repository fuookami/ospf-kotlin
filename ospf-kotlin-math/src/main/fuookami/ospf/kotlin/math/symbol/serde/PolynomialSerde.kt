/**
 * 多项式序列化数据模型
 * Polynomial Serialization Data Models
 *
 * 定义规范、线性和二次多项式及其单项式的序列化数据结构。
 * Defines serialization data structures for canonical, linear, and quadratic polynomials and their monomials.
 */
package fuookami.ospf.kotlin.math.symbol.serde

import kotlinx.serialization.Serializable

/**
 * 规范多项式序列化数据
 * Canonical polynomial serialization data
 *
 * @property monomials 规范单项式数据列表 / List of canonical monomial data
 * @property constant 常数项（Double）/ Constant term (Double)
 */
@Serializable
data class CanonicalPolynomialData(
    val monomials: List<CanonicalMonomialData> = emptyList(),
    val constant: Double
)

/**
 * 规范单项式序列化数据
 * Canonical monomial serialization data
 *
 * @property coefficient 系数（Double）/ Coefficient (Double)
 * @property powers 符号幂次映射（符号名 -> 指数）/ Symbol powers map (symbol name -> exponent)
 */
@Serializable
data class CanonicalMonomialData(
    val coefficient: Double,
    val powers: Map<String, Int> = emptyMap()
)

/**
 * 线性多项式序列化数据
 * Linear polynomial serialization data
 *
 * @property monomials 线性单项式数据列表 / List of linear monomial data
 * @property constant 常数项（Double）/ Constant term (Double)
 */
@Serializable
data class LinearPolynomialData(
    val monomials: List<LinearMonomialData> = emptyList(),
    val constant: Double
)

/**
 * 线性单项式序列化数据
 * Linear monomial serialization data
 *
 * @property coefficient 系数（Double）/ Coefficient (Double)
 * @property symbol 符号名称 / Symbol name
 */
@Serializable
data class LinearMonomialData(
    val coefficient: Double,
    val symbol: String
)

/**
 * 二次多项式序列化数据
 * Quadratic polynomial serialization data
 *
 * @property monomials 二次单项式数据列表 / List of quadratic monomial data
 * @property constant 常数项（Double）/ Constant term (Double)
 */
@Serializable
data class QuadraticPolynomialData(
    val monomials: List<QuadraticMonomialData> = emptyList(),
    val constant: Double
)

/**
 * 二次单项式序列化数据
 * Quadratic monomial serialization data
 *
 * @property coefficient 系数（Double）/ Coefficient (Double)
 * @property symbol1 第一个符号名称 / First symbol name
 * @property symbol2 第二个符号名称（线性项时为 null）/ Second symbol name (null for linear terms)
 */
@Serializable
data class QuadraticMonomialData(
    val coefficient: Double,
    val symbol1: String,
    val symbol2: String? = null
)
