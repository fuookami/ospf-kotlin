package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.functional.Comparator
import fuookami.ospf.kotlin.utils.functional.Ord
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison

class InvalidConstraintSignFromComparison(
    sign: Comparison
) : Throwable() {
    override val message: String = "No matched constraint sign for comparison: $sign."
}

enum class Sign {
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
        operator fun invoke(sign: Comparison) = when (sign) {
            Comparison.LT -> LessEqual
            Comparison.LE -> LessEqual
            Comparison.EQ -> Equal
            Comparison.NE -> throw InvalidConstraintSignFromComparison(sign)
            Comparison.GT -> GreaterEqual
            Comparison.GE -> GreaterEqual
        }
    }

    open val reverse: Sign get() = this

    abstract fun <T : Ord<T>> operator(): Comparator<T>
    operator fun <T : Ord<T>> invoke(lhs: T, rhs: T) = this.operator<T>()(lhs, rhs)
}
