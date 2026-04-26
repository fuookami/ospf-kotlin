package fuookami.ospf.kotlin.core.solver.cplex

import fuookami.ospf.kotlin.core.variable.*
import ilog.concert.IloNumVarType

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

    abstract fun toCplexVar(): IloNumVarType
}
