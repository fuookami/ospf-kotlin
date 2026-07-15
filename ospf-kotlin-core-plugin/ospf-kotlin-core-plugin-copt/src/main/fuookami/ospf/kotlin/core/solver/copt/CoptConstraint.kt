/** COPT 约束符号映射 / COPT constraint sign mapping */
package fuookami.ospf.kotlin.core.solver.copt

import copt.COPT
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation

/** COPT 约束符号枚举，将内部约束关系映射为 COPT 约束符号 / COPT constraint sign enum, maps internal constraint relations to COPT constraint signs */
enum class CoptConstraintSign {
    /** 大于等于 / Greater than or equal */
    GreaterEqual {
        override fun toCoptConstraintSign(): Char {
            return COPT.GREATER_EQUAL
        }
    },
    /** 等于 / Equal */
    Equal {
        override fun toCoptConstraintSign(): Char {
            return COPT.EQUAL
        }
    },
    /** 小于等于 / Less than or equal */
    LessEqual {
        override fun toCoptConstraintSign(): Char {
            return COPT.LESS_EQUAL
        }
    };

    companion object {
        /**
         * 从内部约束关系创建 COPT 约束符号 / Create COPT constraint sign from internal constraint relation
         *
         * @param sign 内部约束关系 / internal constraint relation
         * @return COPT 约束符号 / COPT constraint sign
        */
        operator fun invoke(sign: ConstraintRelation): CoptConstraintSign {
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
     * 转换为 COPT 约束符号字符 / Convert to COPT constraint sign character
     *
     * @return COPT 约束符号字符 / COPT constraint sign character
    */
    abstract fun toCoptConstraintSign(): Char
}