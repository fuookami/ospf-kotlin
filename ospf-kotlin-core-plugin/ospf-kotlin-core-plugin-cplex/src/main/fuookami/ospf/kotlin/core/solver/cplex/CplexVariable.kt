/** CPLEX 变量类型映射 / CPLEX variable type mapping */
package fuookami.ospf.kotlin.core.solver.cplex

import ilog.concert.IloNumVarType
import fuookami.ospf.kotlin.core.variable.*

/** CPLEX 变量类型枚举，将内部变量类型映射为 CPLEX 变量类型 / CPLEX variable type enum, maps internal variable types to CPLEX variable types */
enum class CplexVariable {
    /** 二进制变量 / Binary variable */
    Binary {
        override fun toCplexVar(): IloNumVarType = IloNumVarType.Bool
    },
    /** 整数变量 / Integer variable */
    Integer {
        override fun toCplexVar(): IloNumVarType = IloNumVarType.Int
    },
    /** 连续变量 / Continuous variable */
    Continuous {
        override fun toCplexVar(): IloNumVarType = IloNumVarType.Float
    };

    companion object {
        /**
         * 从内部变量类型创建 CPLEX 变量类型 / Create CPLEX variable type from internal variable type
         *
         * @param type 内部变量类型 / internal variable type
         * @return CPLEX 变量类型 / CPLEX variable type
         */
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

    /**
     * 转换为 CPLEX 变量类型 / Convert to CPLEX variable type
     *
     * @return CPLEX 变量类型 / CPLEX variable type
     */
    abstract fun toCplexVar(): IloNumVarType
}