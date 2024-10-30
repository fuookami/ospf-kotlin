package fuookami.ospf.kotlin.core.backend.plugins.gurobi11

import com.gurobi.gurobi.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

enum class GurobiConstraintSign {
    GreaterEqual {
        override fun toGurobiConstraintSign(): Char {
            return GRB.GREATER_EQUAL
        }
    },
    Equal {
        override fun toGurobiConstraintSign(): Char {
            return GRB.EQUAL
        }
    },
    LessEqual {
        override fun toGurobiConstraintSign(): Char {
            return GRB.LESS_EQUAL
        }
    };

    companion object {
        operator fun invoke(sign: Sign): GurobiConstraintSign {
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

    abstract fun toGurobiConstraintSign(): Char
}
