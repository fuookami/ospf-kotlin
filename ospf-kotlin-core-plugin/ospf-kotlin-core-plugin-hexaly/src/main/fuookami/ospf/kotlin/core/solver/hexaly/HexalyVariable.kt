/**
 * Hexaly variable type mapping
 * Hexaly 变量类型映射
*/
package fuookami.ospf.kotlin.core.solver.hexaly

import com.hexaly.optimizer.HxExpression
import com.hexaly.optimizer.HxModel
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.variable.BalancedTernary as BalancedTernaryType
import fuookami.ospf.kotlin.core.variable.Binary as BinaryType
import fuookami.ospf.kotlin.core.variable.Continuous as ContinuousType
import fuookami.ospf.kotlin.core.variable.Integer as IntegerType
import fuookami.ospf.kotlin.core.variable.Percentage as PercentageType
import fuookami.ospf.kotlin.core.variable.Ternary as TernaryType
import fuookami.ospf.kotlin.core.variable.UContinuous as UContinuousType
import fuookami.ospf.kotlin.core.variable.UInteger as UIntegerType
import fuookami.ospf.kotlin.core.variable.VariableType

/**
 * Hexaly variable type sealed interface, maps internal variable types to Hexaly variable expressions
 * Hexaly 变量类型密封接口，将内部变量类型映射为 Hexaly 变量表达式
*/
sealed interface HexalyVariable {
    companion object {
        /**
         * Create Hexaly variable from internal variable type
         * 从内部变量类型创建 Hexaly 变量
         *
         * @param model 中文 Hexaly 模型 / Hexaly model
         * @param type 中文 内部变量类型 / internal variable type
         * @param lb 中文 变量下界 / variable lower bound
         * @param ub 中文 变量上界 / variable upper bound
         * @return 中文 Hexaly 变量 / Hexaly variable
        */
        operator fun invoke(model: HxModel, type: VariableType<*>, lb: Flt64, ub: Flt64): HexalyVariable {
            return when (type) {
                is BinaryType -> {
                    Binary(model)
                }

                is TernaryType, is BalancedTernaryType, is IntegerType, is UIntegerType -> {
                    Integer(model, lb, ub)
                }

                is PercentageType, is ContinuousType, is UContinuousType -> {
                    Continuous(model, lb, ub)
                }
            }
        }
    }

    /**
     * Convert to Hexaly variable expression
     * 转换为 Hexaly 变量表达式
     *
     * @return 中文 Hexaly 变量表达式 / Hexaly variable expression
    */
    fun toHexalyVariable(): HxExpression

    /**
     * Binary variable
     * 二进制变量
     *
     * @property model 中文 Hexaly 模型 / Hexaly model
    */
    class Binary(private val model: HxModel) : HexalyVariable {
        override fun toHexalyVariable(): HxExpression {
            return model.boolVar()
        }
    }

    /**
     * Integer variable
     * 整数变量
     *
     * @property model 中文 Hexaly 模型 / Hexaly model
     * @property lb 中文 变量下界 / variable lower bound
     * @property ub 中文 变量上界 / variable upper bound
    */
    class Integer(private val model: HxModel, private val lb: Flt64, private val ub: Flt64) : HexalyVariable {
        override fun toHexalyVariable(): HxExpression {
            return model.intVar(lb.toDouble().toLong(), ub.toDouble().toLong())
        }
    }

    /**
     * Continuous variable
     * 连续变量
     *
     * @property model 中文 Hexaly 模型 / Hexaly model
     * @property lb 中文 变量下界 / variable lower bound
     * @property ub 中文 变量上界 / variable upper bound
    */
    class Continuous(private val model: HxModel, private val lb: Flt64, private val ub: Flt64) : HexalyVariable {
        override fun toHexalyVariable(): HxExpression {
            return model.floatVar(lb.toDouble(), ub.toDouble())
        }
    }
}


