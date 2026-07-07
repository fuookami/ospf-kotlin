/**
 * Hexaly variable type mapping
 * Hexaly 变量类型映射
 */
package fuookami.ospf.kotlin.core.solver.hexaly

import com.hexaly.optimizer.HxExpression
import com.hexaly.optimizer.HxModel
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.variable.*

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
         * @param model Hexaly model / 中文 Hexaly 模型
         * @param type internal variable type / 中文 内部变量类型
         * @param lb variable lower bound / 中文 变量下界
         * @param ub variable upper bound / 中文 变量上界
         * @return Hexaly variable / 中文 Hexaly 变量
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
     * Convert to Hexaly variable expression
     * 转换为 Hexaly 变量表达式
     *
     * @return Hexaly variable expression / 中文 Hexaly 变量表达式
     */
    fun toHexalyVariable(): HxExpression

    /**
     * Binary variable
     * 二进制变量
     *
     * @property model Hexaly model / 中文 Hexaly 模型
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
     * @property model Hexaly model / 中文 Hexaly 模型
     * @property lb variable lower bound / 中文 变量下界
     * @property ub variable upper bound / 中文 变量上界
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
     * @property model Hexaly model / 中文 Hexaly 模型
     * @property lb variable lower bound / 中文 变量下界
     * @property ub variable upper bound / 中文 变量上界
     */
    class Continuous(private val model: HxModel, private val lb: Flt64, private val ub: Flt64) : HexalyVariable {
        override fun toHexalyVariable(): HxExpression {
            return model.floatVar(lb.toDouble(), ub.toDouble())
        }
    }
}


