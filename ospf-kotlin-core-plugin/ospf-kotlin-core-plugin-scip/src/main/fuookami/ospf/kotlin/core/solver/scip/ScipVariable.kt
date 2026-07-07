/** SCIP 变量类型映射 / SCIP variable type mapping */
package fuookami.ospf.kotlin.core.solver.scip

import jscip.SCIP_Vartype
import fuookami.ospf.kotlin.core.variable.*

/** SCIP 变量类型枚举，将内部变量类型映射为 SCIP 变量类型 / SCIP variable type enum, maps internal variable types to SCIP variable types */
enum class ScipVariable {
    /** 二进制变量 / Binary variable */
    Binary {
        override fun toSCIPVar(): SCIP_Vartype = SCIP_Vartype.SCIP_VARTYPE_BINARY
    },
    /** 整数变量 / Integer variable */
    Integer {
        override fun toSCIPVar(): SCIP_Vartype = SCIP_Vartype.SCIP_VARTYPE_INTEGER
    },
    /** 连续变量 / Continuous variable */
    Continuous {
        override fun toSCIPVar(): SCIP_Vartype = SCIP_Vartype.SCIP_VARTYPE_CONTINUOUS
    };

    /** Companion object providing variable type conversion / 伴生对象，提供变量类型转换 */
    companion object {
        /**
         * 从内部变量类型创建 SCIP 变量类型 / Create SCIP variable type from internal variable type
         *
         * @param type 内部变量类型 / internal variable type
         * @return SCIP 变量类型 / SCIP variable type
         */
        operator fun invoke(type: VariableType<*>): ScipVariable {
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
     * 转换为 SCIP 变量类型 / Convert to SCIP variable type
     *
     * @return SCIP 变量类型 / SCIP variable type
     */
    abstract fun toSCIPVar(): SCIP_Vartype
}