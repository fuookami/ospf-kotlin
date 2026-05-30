/** COPT 变量类型映射 / COPT variable type mapping */
package fuookami.ospf.kotlin.core.solver.copt

import copt.COPT
import fuookami.ospf.kotlin.core.variable.*

/** COPT 变量类型枚举，将内部变量类型映射为 COPT 变量类型 / COPT variable type enum, maps internal variable types to COPT variable types */
enum class CoptVariable {
    /** 二进制变量 / Binary variable */
    Binary {
        override fun toCoptVar() = COPT.BINARY
    },
    /** 整数变量 / Integer variable */
    Integer {
        override fun toCoptVar() = COPT.INTEGER
    },
    /** 连续变量 / Continuous variable */
    Continuous {
        override fun toCoptVar() = COPT.CONTINUOUS
    };

    companion object {
        /**
         * 从内部变量类型创建 COPT 变量类型 / Create COPT variable type from internal variable type
         *
         * @param type 内部变量类型 / internal variable type
         * @return COPT 变量类型 / COPT variable type
         */
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

    /**
     * 转换为 COPT 变量类型字符 / Convert to COPT variable type character
     *
     * @return COPT 变量类型字符 / COPT variable type character
     */
    abstract fun toCoptVar(): Char
}