/** Gurobi 约束符号映射 / Gurobi constraint sign mapping */
package fuookami.ospf.kotlin.core.solver.gurobi

import gurobi.GRB
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation

/** Gurobi 约束符号枚举，将内部约束关系映射为 Gurobi 约束符号 / Gurobi constraint sign enum, maps internal constraint relations to Gurobi constraint signs */
enum class GurobiConstraintSign {
    /** 大于等于 / Greater than or equal */
    GreaterEqual {
        override fun toGurobiConstraintSign(): Char {
            return GRB.GREATER_EQUAL
        }
    },
    /** 等于 / Equal */
    Equal {
        override fun toGurobiConstraintSign(): Char {
            return GRB.EQUAL
        }
    },
    /** 小于等于 / Less than or equal */
    LessEqual {
        override fun toGurobiConstraintSign(): Char {
            return GRB.LESS_EQUAL
        }
    };

    companion object {
        /**
         * 从内部约束关系创建 Gurobi 约束符号 / Create Gurobi constraint sign from internal constraint relation
         *
         * @param sign 内部约束关系 / internal constraint relation
         * @return Gurobi 约束符号 / Gurobi constraint sign
         */
        operator fun invoke(sign: ConstraintRelation): GurobiConstraintSign {
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
     * 转换为 Gurobi 约束符号字符 / Convert to Gurobi constraint sign character
     *
     * @return Gurobi 约束符号字符 / Gurobi constraint sign character
     */
    abstract fun toGurobiConstraintSign(): Char
}