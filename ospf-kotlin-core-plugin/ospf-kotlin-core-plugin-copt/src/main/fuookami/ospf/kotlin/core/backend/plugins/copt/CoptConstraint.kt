package fuookami.ospf.kotlin.core.intermediate_plugins.copt

import copt.COPT
import fuookami.ospf.kotlin.core.intermediate_model.Sign

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
        operator fun invoke(sign: Sign): CoptConstraintSign {
            return when (sign) {
                Sign.GreaterEqual -> {
                    GreaterEqual
                }

                Sign.Equal -> {
                    Equal
                }

                Sign.LessEqual -> {
                    LessEqual
                }
            }
        }
    }

    abstract fun toCoptConstraintSign(): Char
}
