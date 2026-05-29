/**
 * 约束符号
 * Constraint sign
 */
package fuookami.ospf.kotlin.core.model.basic

import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 从比较运算创建约束符号时的无效输入异常。
 * Exception thrown when creating a constraint sign from an invalid comparison.
 *
 * @property sign 无效的比较运算 / The invalid comparison
 */
class InvalidConstraintSignFromComparison(
    sign: Comparison
) : Throwable() {
    override val message: String = "No matched constraint sign for comparison: $sign."
}

/**
 * 约束关系枚举，表示小于等于、等于、大于等于三种约束方向。
 * Constraint relation enumeration representing three constraint directions: less-equal, equal, greater-equal.
 */
enum class ConstraintRelation {
    LessEqual {
        override val reverse get() = GreaterEqual
        override fun <T : Ord<T>> operator(): Comparator<T> = { lhs, rhs -> lhs.compareTo(rhs) <= 0 }
        override fun toString() = "<="
    },
    Equal {
        override fun <T : Ord<T>> operator(): Comparator<T> = { lhs, rhs -> lhs.compareTo(rhs) == 0 }
        override fun toString() = "="
    },
    GreaterEqual {
        override val reverse get() = LessEqual
        override fun <T : Ord<T>> operator(): Comparator<T> = { lhs, rhs -> lhs.compareTo(rhs) >= 0 }
        override fun toString() = ">="
    };

    companion object {
        @Throws(InvalidConstraintSignFromComparison::class)
        operator fun invoke(comparison: Comparison) = when (comparison) {
            Comparison.LT -> LessEqual
            Comparison.LE -> LessEqual
            Comparison.EQ -> Equal
            Comparison.NE -> throw InvalidConstraintSignFromComparison(comparison)
            Comparison.GT -> GreaterEqual
            Comparison.GE -> GreaterEqual
        }
    }

    /**
     * 获取此约束关系的反转关系（如小于等于变为大于等于）。
     * Get the reverse of this constraint relation (e.g. less-equal becomes greater-equal).
     *
     * @return 反转后的约束关系，默认返回自身（等于的反转仍为等于） / The reversed constraint relation; defaults to self (equal reverses to equal)
     */
    open val reverse: ConstraintRelation get() = this

    /**
     * 获取此约束关系对应的比较器。
     * Get the comparator corresponding to this constraint relation.
     *
     * @param T 可比较的有序类型 / An ordered type
     * @return 比较器，对满足约束关系的 lhs 和 rhs 返回 true / A comparator that returns true when lhs and rhs satisfy this constraint relation
     */
    abstract fun <T : Ord<T>> operator(): Comparator<T>

    /**
     * 直接判断两个值是否满足此约束关系。
     * Directly check whether two values satisfy this constraint relation.
     *
     * @param T 可比较的有序类型 / An ordered type
     * @param lhs 左操作数 / The left-hand operand
     * @param rhs 右操作数 / The right-hand operand
     * @return 若 lhs 和 rhs 满足约束关系则返回 true / True if lhs and rhs satisfy this constraint relation
     */
    operator fun <T : Ord<T>> invoke(lhs: T, rhs: T) = this.operator<T>()(lhs, rhs)

    /**
     * 将此约束关系转换为通用比较枚举。
     * Convert this constraint relation to the generic comparison enumeration.
     *
     * @return 对应的 Comparison 枚举值 / The corresponding Comparison enum value
     */
    fun toComparison(): Comparison = when (this) {
        LessEqual -> Comparison.LE
        Equal -> Comparison.EQ
        GreaterEqual -> Comparison.GE
    }
}
