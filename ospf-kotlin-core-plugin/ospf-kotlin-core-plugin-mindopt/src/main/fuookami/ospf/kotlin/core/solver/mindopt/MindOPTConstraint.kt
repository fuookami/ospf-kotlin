/** MindOPT 约束符号映射 / MindOPT constraint sign mapping */
package fuookami.ospf.kotlin.core.solver.mindopt

import com.alibaba.damo.mindopt.MDO
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation

/** MindOPT 约束符号枚举，将内部约束关系映射为 MindOPT 约束符号 / MindOPT constraint sign enum, maps internal constraint relations to MindOPT constraint signs */
enum class MindOPTConstraintSign {
    /** 大于等于 / Greater than or equal */
    GreaterEqual {
        override fun toMindOPTConstraintSign(): Char {
            return MDO.GREATER_EQUAL
        }
    },
    /** 等于 / Equal */
    Equal {
        override fun toMindOPTConstraintSign(): Char {
            return MDO.EQUAL
        }
    },
    /** 小于等于 / Less than or equal */
    LessEqual {
        override fun toMindOPTConstraintSign(): Char {
            return MDO.LESS_EQUAL
        }
    };

    companion object {
        /**
         * 从内部约束关系创建 MindOPT 约束符号 / Create MindOPT constraint sign from internal constraint relation
         *
         * @param sign 内部约束关系 / internal constraint relation
         * @return MindOPT 约束符号 / MindOPT constraint sign
         */
        operator fun invoke(sign: ConstraintRelation): MindOPTConstraintSign {
            return when (sign) {
                ConstraintRelation.GreaterEqual -> {
                    GreaterEqual
                }

                ConstraintRelation.Equal -> {
                    Equal
                }

                ConstraintRelation.LessEqual -> {
                    LessEqual
                }
            }
        }
    }

    /**
     * 转换为 MindOPT 约束符号字符 / Convert to MindOPT constraint sign character
     *
     * @return MindOPT 约束符号字符 / MindOPT constraint sign character
     */
    abstract fun toMindOPTConstraintSign(): Char
}