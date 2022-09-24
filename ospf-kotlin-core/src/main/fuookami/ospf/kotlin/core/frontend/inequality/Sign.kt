package fuookami.ospf.kotlin.core.frontend.inequality

import fuookami.ospf.kotlin.utils.functional.Comparator
import fuookami.ospf.kotlin.utils.operator.Ord

enum class Sign {
    Less {
        override fun toString() = "<"

        override fun <T : Ord<T>> operator(): Comparator<T> = { lhs, rhs -> lhs ls rhs }
    },
    LessEqual {
        override fun toString() = "<="

        override fun <T : Ord<T>> operator(): Comparator<T> = { lhs, rhs -> lhs leq rhs }
    },
    Greater {
        override fun toString() = ">"

        override fun <T : Ord<T>> operator(): Comparator<T> = { lhs, rhs -> lhs gr rhs }
    },
    GreaterEqual {
        override fun toString() = ">="

        override fun <T : Ord<T>> operator(): Comparator<T> = { lhs, rhs -> lhs geq rhs }
    },
    Equal {
        override fun toString() = "="

        override fun <T : Ord<T>> operator(): Comparator<T> = { lhs, rhs -> lhs eq rhs }
    },
    Unequal {
        override fun toString() = "!="

        override fun <T : Ord<T>> operator(): Comparator<T> = { lhs, rhs -> lhs neq rhs }
    };

    abstract fun <T : Ord<T>> operator(): Comparator<T>;
}
