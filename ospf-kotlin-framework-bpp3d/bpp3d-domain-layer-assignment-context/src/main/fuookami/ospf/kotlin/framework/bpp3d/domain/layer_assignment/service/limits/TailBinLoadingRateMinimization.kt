/**
 * Tail bin loading rate minimization objective.
 * 尾箱装载率最小化目标。
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.*

/**
 * Tail bin loading rate minimization objective, minimizes tail bin loading rate.
 * 尾箱装载率最小化目标，最小化尾箱的装载率。
 *
 * @property bins 箱子列表 / bin list
 * @property assignment 精确赋值 / precise assignment
 * @property capacity 容量符号 / capacity symbols
 * @property coefficient 箱子系数函数 / bin coefficient function
 * @property name 约束名称 / constraint name
 */
class TailBinLoadingRateMinimization(
    private val bins: List<Bin<BinLayer, FltX>>,
    private val assignment: PreciseAssignment,
    private val capacity: Capacity,
    private val coefficient: (Bin<BinLayer, FltX>) -> FltX,
    val name: String = "tail_bin_loading_rate_minimization"
) {
    /**
     * Add objective to model.
     * 将目标添加到模型。
     *
     * @param model 元模型 / meta model
     * @return 操作结果 / operation result
     */
    fun invoke(model: MetaModel<FltX>): Try {
        val linearModel = model as AbstractLinearMetaModel<FltX>
        when (val result = linearModel.minimize(
            polynomial = sum(bins.mapIndexed { i, bin ->
                LinearMonomial(coefficient(bin), capacity.tailLoadingRate[i])
            }),
            name = "tail bin loading rate"
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
