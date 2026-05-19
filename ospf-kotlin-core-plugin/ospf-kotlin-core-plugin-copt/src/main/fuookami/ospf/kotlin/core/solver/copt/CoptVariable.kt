package fuookami.ospf.kotlin.core.solver.copt

import copt.COPT
import fuookami.ospf.kotlin.core.variable.*

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

    abstract fun toCoptVar(): Char
}