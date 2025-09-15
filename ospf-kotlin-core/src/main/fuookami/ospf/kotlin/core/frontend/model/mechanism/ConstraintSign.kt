package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*

private typealias InequalitySign = fuookami.ospf.kotlin.core.frontend.inequality.Sign

class InvalidConstraintSign(
    sign: InequalitySign
) : Throwable() {
    override val message: String = "No matched constraint sign of inequality sign: $sign."
}

enum class Sign {
    LessEqual {
        override val reverse get() = GreaterEqual
        override fun <T : Ord<T>> operator() = InequalitySign.LessEqual.operator<T>()
        override fun toString() = "<="
    },
    Equal {
        override fun <T : Ord<T>> operator() = InequalitySign.Equal.operator<T>()
        override fun toString() = "="
    },
    GreaterEqual {
        override val reverse get() = LessEqual
        override fun <T : Ord<T>> operator() = InequalitySign.GreaterEqual.operator<T>()
        override fun toString() = ">="
    };

    companion object {
        @Throws(InvalidConstraintSign::class)
        operator fun invoke(sign: InequalitySign) = when (sign) {
            InequalitySign.Less -> LessEqual
            InequalitySign.LessEqual -> LessEqual
            InequalitySign.Equal -> Equal
            InequalitySign.Unequal -> throw InvalidConstraintSign(sign)
            InequalitySign.Greater -> GreaterEqual
            InequalitySign.GreaterEqual -> GreaterEqual
        }
    }

    open val reverse: Sign get() = this

    abstract fun <T : Ord<T>> operator(): Comparator<T>
    operator fun <T : Ord<T>> invoke(lhs: T, rhs: T) = this.operator<T>()(lhs, rhs)
}
