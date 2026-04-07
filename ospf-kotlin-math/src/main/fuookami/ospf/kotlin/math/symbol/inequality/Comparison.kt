/**
 * 比较运算
 * Comparison Operations
 *
 * 定义不等式中的比较运算符枚举，包括小于、小于等于、等于、不等于、大于等于和大于。
 * 提供运算符的符号表示、属性查询和反转操作。
 * Defines the comparison operator enumeration for inequalities,
 * including less than, less than or equal, equal, not equal, greater than or equal, and greater than.
 * Provides symbol representation, property queries, and reversal operations.
 */
package fuookami.ospf.kotlin.math.symbol.inequality

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

/**
 * 比较运算符枚举
 * Comparison Operator Enumeration
 *
 * 表示不等式中的比较运算符。支持六种比较关系：
 * - [LT]: 小于 (<)
 * - [LE]: 小于等于 (<=)
 * - [EQ]: 等于 (=)
 * - [NE]: 不等于 (!=)
 * - [GE]: 大于等于 (>=)
 * - [GT]: 大于 (>)
 * Represents comparison operators in inequalities. Supports six comparison relations:
 * - [LT]: Less than (<)
 * - [LE]: Less than or equal (<=)
 * - [EQ]: Equal (=)
 * - [NE]: Not equal (!=)
 * - [GE]: Greater than or equal (>=)
 * - [GT]: Greater than (>)
 */
enum class Comparison {
    /**
     * 小于
     * Less than
     */
    LT,

    /**
     * 小于等于
     * Less than or equal
     */
    LE,

    /**
     * 等于
     * Equal
     */
    EQ,

    /**
     * 不等于
     * Not equal
     */
    NE,

    /**
     * 大于等于
     * Greater than or equal
     */
    GE,

    /**
     * 大于
     * Greater than
     */
    GT;

    /**
     * 运算符的符号表示
     * Symbol representation of the operator
     *
     * 返回运算符的字符串表示形式，如 "<"、"<="、"=" 等。
     * Returns the string representation of the operator, such as "<", "<=", "=", etc.
     */
    val symbol: String
        get() = when (this) {
            LT -> "<"
            LE -> "<="
            EQ -> "="
            NE -> "!="
            GE -> ">="
            GT -> ">"
        }

    /**
     * 是否为严格比较（不含等号）
     * Whether this is a strict comparison (excluding equality)
     *
     * 对于 LT、GT、NE 返回 true，表示不含等号的严格比较。
     * Returns true for LT, GT, NE, indicating strict comparison without equality.
     */
    val isStrict: Boolean
        get() = this == LT || this == GT || this == NE

    /**
     * 是否包含等号
     * Whether this includes equality
     *
     * 对于 LE、EQ、GE 返回 true，表示包含等号的非严格比较。
     * Returns true for LE, EQ, GE, indicating non-strict comparison including equality.
     */
    val includesEquality: Boolean
        get() = this == LE || this == EQ || this == GE

    /**
     * 是否为小于类比较
     * Whether this is a less-like comparison
     *
     * 对于 LT、LE 返回 true，表示左侧小于或小于等于右侧。
     * Returns true for LT, LE, indicating left side is less than or less than or equal to right side.
     */
    val isLessLike: Boolean
        get() = this == LT || this == LE

    /**
     * 是否为大于类比较
     * Whether this is a greater-like comparison
     *
     * 对于 GT、GE 返回 true，表示左侧大于或大于等于右侧。
     * Returns true for GT, GE, indicating left side is greater than or greater than or equal to right side.
     */
    val isGreaterLike: Boolean
        get() = this == GT || this == GE

    /**
     * 反转比较运算符
     * Reverses the comparison operator
     *
     * 返回相反方向的比较运算符。例如，LT 返回 GT，LE 返回 GE。
     * EQ 和 NE 返回自身。
     * Returns the comparison operator in the opposite direction.
     * For example, LT returns GT, LE returns GE.
     * EQ and NE return themselves.
     *
     * @return 反转后的比较运算符 / The reversed comparison operator
     */
    fun reverse(): Comparison {
        return when (this) {
            LT -> GT
            LE -> GE
            EQ -> EQ
            NE -> NE
            GE -> LE
            GT -> LT
        }
    }
}

