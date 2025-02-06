package fuookami.ospf.kotlin.core.backend.plugins.mindopt

import com.alibaba.damo.mindopt.*
import fuookami.ospf.kotlin.core.frontend.variable.*

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
                is fuookami.ospf.kotlin.core.frontend.variable.Binary -> {
                    Binary
                }

                is Ternary, is BalancedTernary, is fuookami.ospf.kotlin.core.frontend.variable.Integer, is UInteger -> {
                    Integer
                }

                is Percentage, is fuookami.ospf.kotlin.core.frontend.variable.Continuous, is UContinuous -> {
                    Continuous
                }
            }
        }
    }

    abstract fun toMindOPTVar(): Char
}
