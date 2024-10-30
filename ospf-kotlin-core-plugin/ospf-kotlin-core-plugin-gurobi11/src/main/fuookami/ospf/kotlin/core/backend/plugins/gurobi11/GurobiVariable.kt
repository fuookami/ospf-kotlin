package fuookami.ospf.kotlin.core.backend.plugins.gurobi11

import com.gurobi.gurobi.*
import fuookami.ospf.kotlin.core.frontend.variable.*

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

    abstract fun toGurobiVar(): Char
}
