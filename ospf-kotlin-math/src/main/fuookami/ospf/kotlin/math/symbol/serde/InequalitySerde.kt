/**
 * 不等式序列化数据模型
 * Inequality Serialization Data Models
 *
 * 定义规范、线性和二次不等式的序列化数据结构。
 * Defines serialization data structures for canonical, linear, and quadratic inequalities.
 */
package fuookami.ospf.kotlin.math.symbol.serde

import kotlinx.serialization.Serializable

/**
 * 规范不等式序列化数据
 * Canonical inequality serialization data
 *
 * @property lhs 左侧规范多项式数据 / Left-hand side canonical polynomial data
 * @property rhs 右侧规范多项式数据 / Right-hand side canonical polynomial data
 * @property comparison 比较运算符字符串 / Comparison operator string
 */
@Serializable
data class CanonicalInequalityData(
    val lhs: CanonicalPolynomialData,
    val rhs: CanonicalPolynomialData,
    val comparison: String
)

/**
 * 线性不等式序列化数据
 * Linear inequality serialization data
 *
 * @property lhs 左侧线性多项式数据 / Left-hand side linear polynomial data
 * @property rhs 右侧线性多项式数据 / Right-hand side linear polynomial data
 * @property comparison 比较运算符字符串 / Comparison operator string
 * @property name 不等式名称 / Inequality name
 * @property displayName 不等式显示名称 / Inequality display name
 */
@Serializable
data class LinearInequalityData(
    val lhs: LinearPolynomialData,
    val rhs: LinearPolynomialData,
    val comparison: String,
    val name: String = "",
    val displayName: String = ""
)

/**
 * 二次不等式序列化数据
 * Quadratic inequality serialization data
 *
 * @property lhs 左侧二次多项式数据 / Left-hand side quadratic polynomial data
 * @property rhs 右侧二次多项式数据 / Right-hand side quadratic polynomial data
 * @property comparison 比较运算符字符串 / Comparison operator string
 * @property name 不等式名称 / Inequality name
 * @property displayName 不等式显示名称 / Inequality display name
 */
@Serializable
data class QuadraticInequalityData(
    val lhs: QuadraticPolynomialData,
    val rhs: QuadraticPolynomialData,
    val comparison: String,
    val name: String = "",
    val displayName: String = ""
)
