/** Gurobi 11 变量类型映射 / Gurobi 11 variable type mapping */
package fuookami.ospf.kotlin.core.solver.gurobi11

import com.gurobi.gurobi.GRB
import fuookami.ospf.kotlin.core.variable.*

/** Gurobi 11 变量类型枚举，将内部变量类型映射为 Gurobi 变量类型 / Gurobi 11 variable type enum, maps internal variable types to Gurobi variable types */
enum class GurobiVariable {
    /** 二进制变量 / Binary variable */
    Binary {
        override fun toGurobiVar() = GRB.BINARY
    },
    /** 整数变量 / Integer variable */
    Integer {
        override fun toGurobiVar() = GRB.INTEGER
    },
    /** 连续变量 / Continuous variable */
    Continuous {
        override fun toGurobiVar() = GRB.CONTINUOUS
    };

    companion object {
        /**
         * 从内部变量类型创建 Gurobi 变量类型 / Create Gurobi variable type from internal variable type
         *
         * @param type 内部变量类型 / internal variable type
         * @return Gurobi 变量类型 / Gurobi variable type
         */
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

    /**
     * 转换为 Gurobi 变量类型字符 / Convert to Gurobi variable type character
     *
     * @return Gurobi 变量类型字符 / Gurobi variable type character
     */
    abstract fun toGurobiVar(): Char
}