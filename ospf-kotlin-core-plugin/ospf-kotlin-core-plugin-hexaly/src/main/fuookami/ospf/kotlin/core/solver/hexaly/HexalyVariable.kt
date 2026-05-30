/** Hexaly 变量类型映射 / Hexaly variable type mapping */
package fuookami.ospf.kotlin.core.solver.hexaly

import com.hexaly.optimizer.HxExpression
import com.hexaly.optimizer.HxModel
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/** Hexaly 变量类型密封接口，将内部变量类型映射为 Hexaly 变量表达式 / Hexaly variable type sealed interface, maps internal variable types to Hexaly variable expressions */
sealed interface HexalyVariable {
    companion object {
        /**
         * 从内部变量类型创建 Hexaly 变量 / Create Hexaly variable from internal variable type
         *
         * @param model Hexaly 模型 / Hexaly model
         * @param type 内部变量类型 / internal variable type
         * @param lb 变量下界 / variable lower bound
         * @param ub 变量上界 / variable upper bound
         * @return Hexaly 变量 / Hexaly variable
         */
        operator fun invoke(model: HxModel, type: VariableType<*>, lb: Flt64, ub: Flt64): HexalyVariable {
            return when (type) {
                is fuookami.ospf.kotlin.core.variable.Binary -> {
                    Binary(model)
                }

                is Ternary, is BalancedTernary, is fuookami.ospf.kotlin.core.variable.Integer, is UInteger -> {
                    Integer(model, lb, ub)
                }

                is Percentage, is fuookami.ospf.kotlin.core.variable.Continuous, is UContinuous -> {
                    Continuous(model, lb, ub)
                }
            }
        }
    }

    /**
     * 转换为 Hexaly 变量表达式 / Convert to Hexaly variable expression
     *
     * @return Hexaly 变量表达式 / Hexaly variable expression
     */
    fun toHexalyVariable(): HxExpression

    /** 二进制变量 / Binary variable */
    class Binary(private val model: HxModel) : HexalyVariable {
        override fun toHexalyVariable(): HxExpression {
            return model.boolVar()
        }
    }

    /** 整数变量 / Integer variable */
    class Integer(private val model: HxModel, private val lb: Flt64, private val ub: Flt64) : HexalyVariable {
        override fun toHexalyVariable(): HxExpression {
            return model.intVar(lb.toDouble().toLong(), ub.toDouble().toLong())
        }
    }

    /** 连续变量 / Continuous variable */
    class Continuous(private val model: HxModel, private val lb: Flt64, private val ub: Flt64) : HexalyVariable {
        override fun toHexalyVariable(): HxExpression {
            return model.floatVar(lb.toDouble(), ub.toDouble())
        }
    }
}


