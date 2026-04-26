package fuookami.ospf.kotlin.core.solver.gurobi11

import com.gurobi.gurobi.GRB
import fuookami.ospf.kotlin.core.variable.*

enum class GurobiVariable {
    Binary {
        override fun toGurobiVar() = GRB.BINARY
    },
    Integer {
        override fun toGurobiVar() = GRB.INTEGER
    },
    Continuous {
        override fun toGurobiVar() = GRB.CONTINUOUS
    };

    companion object {
        operator fun invoke(type: VariableType<*>): GurobiVariable {
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

    abstract fun toGurobiVar(): Char
}
