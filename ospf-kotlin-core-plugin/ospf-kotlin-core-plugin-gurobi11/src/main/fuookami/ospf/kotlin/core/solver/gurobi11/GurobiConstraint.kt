package fuookami.ospf.kotlin.core.solver.gurobi11

import com.gurobi.gurobi.GRB
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation

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
        operator fun invoke(sign: ConstraintRelation): GurobiConstraintSign {
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

    abstract fun toGurobiConstraintSign(): Char
}
