package fuookami.ospf.kotlin.framework.csp1d.application.model

import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.UIntVariable1

/**
 * CSP1D 方案分配，包含一组无符号整数决策变量。
 * Csp1d assignment, containing a set of unsigned integer decision variables.
 * @property x 无符号整数决策变量，维度为方案数量。
 * @property planCount 方案数量。
 */
class Csp1dAssignment(
    val x: UIntVariable1,
    val planCount: Int
) {
    companion object {
        /**
         * 创建一个 Csp1dAssignment 实例。
         * Create a Csp1dAssignment instance.
         * @param planCount 方案数量。
         * @return 新建的 Csp1dAssignment 实例。
         */
        fun create(planCount: Int): Csp1dAssignment {
            return Csp1dAssignment(
                x = UIntVariable1("x", Shape1(planCount)),
                planCount = planCount
            )
        }
    }

    operator fun get(index: Int) = x[index]

    /**
     * 将决策变量注册到线性元模型中。
     * Register decision variables to the linear meta model.
     * @param model 线性元模型。
     * @return 注册操作的结果。
     */
    fun register(model: LinearMetaModel<Flt64>): Try {
        return model.add(x)
    }
}
