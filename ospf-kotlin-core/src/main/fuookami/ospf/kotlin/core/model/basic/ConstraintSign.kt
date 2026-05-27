/**
 * 约束符号
 * Constraint sign
 */
package fuookami.ospf.kotlin.core.model.basic

import fuookami.ospf.kotlin.utils.functional.Comparator
import fuookami.ospf.kotlin.utils.functional.Ord
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison

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

    open val reverse: ConstraintRelation get() = this

    abstract fun <T : Ord<T>> operator(): Comparator<T>
    operator fun <T : Ord<T>> invoke(lhs: T, rhs: T) = this.operator<T>()(lhs, rhs)

    fun toComparison(): Comparison = when (this) {
        LessEqual -> Comparison.LE
        Equal -> Comparison.EQ
        GreaterEqual -> Comparison.GE
    }
}
