package fuookami.ospf.kotlin.core.solver.mindopt

import com.alibaba.damo.mindopt.MDO
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation

enum class MindOPTConstraintSign {
    GreaterEqual {
        override fun toMindOPTConstraintSign(): Char {
            return MDO.GREATER_EQUAL
        }
    },
    Equal {
        override fun toMindOPTConstraintSign(): Char {
            return MDO.EQUAL
        }
    },
    LessEqual {
        override fun toMindOPTConstraintSign(): Char {
            return MDO.LESS_EQUAL
        }
    };

    companion object {
        operator fun invoke(sign: ConstraintRelation): MindOPTConstraintSign {
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

    abstract fun toMindOPTConstraintSign(): Char
}