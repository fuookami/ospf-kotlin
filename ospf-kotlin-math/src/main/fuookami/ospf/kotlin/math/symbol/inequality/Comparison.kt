/**
 * 比较运算
 * Comparison Operations
 *
 * 定义不等式中的比较运算符枚举，包括小于、小于等于、等于、不等于、大于等于和大于。 * 提供运算符的符号表示、属性查询和反转操作。 * Defines the comparison operator enumeration for inequalities,
 * including less than, less than or equal, equal, not equal, greater than or equal, and greater than.
 * Provides symbol representation, property queries, and reversal operations.
 */
package fuookami.ospf.kotlin.math.symbol.inequality

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

/**
 * 比较运算符枚丌 * Comparison Operator Enumeration
 *
 * 表示不等式中的比较运算符。支持六种比较关系：
 * - [LT]: 小于 (<)
 * - [LE]: 小于等于 (<=)
 * - [EQ]: 等于 (=)
 * - [NE]: 不等二(!=)
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
    LT,
    LE,
    EQ,
    NE,
    GE,
    GT;

    /** 运算符的符号表示 / Symbol representation of the operator */
    val symbol: String
        get() = when (this) {
            LT -> "<"
            LE -> "<="
            EQ -> "="
            NE -> "!="
            GE -> ">="
            GT -> ">"
        }

    /** 是否为严格比较（不含等于）/ Whether this is a strict comparison (excludes equality) */
    val isStrict: Boolean
        get() = this == LT || this == GT || this == NE

    /** 是否包含等于关系 / Whether this includes equality */
    val includesEquality: Boolean
        get() = this == LE || this == EQ || this == GE

    /** 是否为小于类比较（LT或LE）/ Whether this is a less-than type comparison (LT or LE) */
    val isLessLike: Boolean
        get() = this == LT || this == LE

    /** 是否为大于类比较（GT或GE）/ Whether this is a greater-than type comparison (GT or GE) */
    val isGreaterLike: Boolean
        get() = this == GT || this == GE

    /**
     * 返回反转的比较运算符。
     * Returns the reversed comparison operator.
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
