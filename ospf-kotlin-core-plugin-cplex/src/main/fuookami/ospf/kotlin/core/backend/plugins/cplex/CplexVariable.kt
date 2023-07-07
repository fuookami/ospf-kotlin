package fuookami.ospf.kotlin.core.backend.plugins.cplex

import ilog.concert.*
import fuookami.ospf.kotlin.core.frontend.variable.*

enum class CplexVariable {
    Binary {
        override fun toCplexVar(): IloNumVarType = IloNumVarType.Bool
    },
    Integer {
        override fun toCplexVar(): IloNumVarType = IloNumVarType.Int
    },
    Continuous {
        override fun toCplexVar(): IloNumVarType = IloNumVarType.Float
    };

    companion object {
        operator fun invoke(type: VariableType<*>): CplexVariable {
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

    abstract fun toCplexVar(): IloNumVarType
}
