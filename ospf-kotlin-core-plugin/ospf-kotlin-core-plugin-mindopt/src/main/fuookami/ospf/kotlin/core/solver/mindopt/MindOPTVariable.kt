package fuookami.ospf.kotlin.core.solver.mindopt

import com.alibaba.damo.mindopt.MDO
import fuookami.ospf.kotlin.core.variable.*

enum class MindOPTVariable {
    Binary {
        override fun toMindOPTVar() = MDO.BINARY
    },
    Integer {
        override fun toMindOPTVar() = MDO.INTEGER
    },
    Continuous {
        override fun toMindOPTVar() = MDO.CONTINUOUS
    };

    companion object {
        operator fun invoke(type: VariableType<*>): MindOPTVariable {
            return when (type) {
                is fuookami.ospf.kotlin.core.variable.Binary -> {
                    Binary
                }

                is Ternary, is BalancedTernary, is fuookami.ospf.kotlin.core.variable.Integer, is UInteger -> {
                    Integer
                }

                is Percentage, is fuookami.ospf.kotlin.core.variable.Continuous, is UContinuous -> {
                    Continuous
                }
            }
        }
    }

    abstract fun toMindOPTVar(): Char
}