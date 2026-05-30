/** MindOPT 变量类型映射 / MindOPT variable type mapping */
package fuookami.ospf.kotlin.core.solver.mindopt

import com.alibaba.damo.mindopt.MDO
import fuookami.ospf.kotlin.core.variable.*

/** MindOPT 变量类型枚举，将内部变量类型映射为 MindOPT 变量类型 / MindOPT variable type enum, maps internal variable types to MindOPT variable types */
enum class MindOPTVariable {
    /** 二进制变量 / Binary variable */
    Binary {
        override fun toMindOPTVar() = MDO.BINARY
    },
    /** 整数变量 / Integer variable */
    Integer {
        override fun toMindOPTVar() = MDO.INTEGER
    },
    /** 连续变量 / Continuous variable */
    Continuous {
        override fun toMindOPTVar() = MDO.CONTINUOUS
    };

    companion object {
        /**
         * 从内部变量类型创建 MindOPT 变量类型 / Create MindOPT variable type from internal variable type
         *
         * @param type 内部变量类型 / internal variable type
         * @return MindOPT 变量类型 / MindOPT variable type
         */
        operator fun invoke(type: VariableType<*>): MindOPTVariable {
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
     * 转换为 MindOPT 变量类型字符 / Convert to MindOPT variable type character
     *
     * @return MindOPT 变量类型字符 / MindOPT variable type character
     */
    abstract fun toMindOPTVar(): Char
}