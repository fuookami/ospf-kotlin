package fuookami.ospf.kotlin.core.solver.copt

import copt.COPT
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation

enum class CoptConstraintSign {
    GreaterEqual {
        override fun toCoptConstraintSign(): Char {
            return COPT.GREATER_EQUAL
        }
    },
    Equal {
        override fun toCoptConstraintSign(): Char {
            return COPT.EQUAL
        }
    },
    LessEqual {
        override fun toCoptConstraintSign(): Char {
            return COPT.LESS_EQUAL
        }
    };

    companion object {
        operator fun invoke(sign: ConstraintRelation): CoptConstraintSign {
            return when (sign) {
                ConstraintRelation.GreaterEqual -> {
                    GreaterEqual
                }

                ConstraintRelation.Equal -> {
                    Equal
                }

                ConstraintRelation.LessEqual -> {
                    LessEqual
                }
            }
        }
    }

    abstract fun toCoptConstraintSign(): Char
}
