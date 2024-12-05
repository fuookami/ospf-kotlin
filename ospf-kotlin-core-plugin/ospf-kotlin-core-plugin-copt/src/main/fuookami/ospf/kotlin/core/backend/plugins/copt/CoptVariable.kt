package fuookami.ospf.kotlin.core.backend.plugins.copt

import copt.*
import fuookami.ospf.kotlin.core.frontend.variable.*

enum class CoptVariable {
    Binary {
        override fun toCoptVar() = COPT.BINARY
    },
    Integer {
        override fun toCoptVar() = COPT.INTEGER
    },
    Continuous {
        override fun toCoptVar() = COPT.CONTINUOUS
    };

    companion object {
        operator fun invoke(type: VariableType<*>): CoptVariable {
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

    abstract fun toCoptVar(): Char
}
