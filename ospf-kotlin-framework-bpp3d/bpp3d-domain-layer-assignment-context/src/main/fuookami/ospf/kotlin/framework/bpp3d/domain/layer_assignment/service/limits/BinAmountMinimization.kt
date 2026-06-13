/**
 * 箱子数量最小化目标。
 * Bin amount minimization objective.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseAssignment
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 箱子数量最小化目标，最小化使用的箱子数量。
 * Bin amount minimization objective, minimizes the number of bins used.
 *
 * @property bins 箱子列表 / bin list
 * @property assignment 精确赋值 / precise assignment
 * @property coefficient 箱子系数函数 / bin coefficient function
 * @property name 约束名称 / constraint name
 */
class BinAmountMinimization(
    private val bins: List<Bin<BinLayer, FltX>>,
    private val assignment: PreciseAssignment,
    private val coefficient: (Bin<BinLayer, FltX>) -> FltX,
    val name: String = "bin_amount_minimization"
) {
    /**
     * 将目标添加到模型。
     * Add objective to model.
     *
     * @param model 元模型 / meta model
     * @return 操作结果 / operation result
     */
    fun invoke(model: MetaModel<FltX>): Try {
        val linearModel = model as AbstractLinearMetaModel<FltX>
        when (val result = linearModel.minimize(
            polynomial = sum(bins.mapIndexed { i, bin ->
                LinearMonomial(coefficient(bin), assignment.v[i])
            }),
            name = "bin amount"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }
}





