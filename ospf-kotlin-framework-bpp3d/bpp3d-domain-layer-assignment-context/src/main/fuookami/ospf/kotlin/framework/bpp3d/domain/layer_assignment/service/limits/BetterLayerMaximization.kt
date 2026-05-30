/**
 * 更优层最大化目标。
 * Better layer maximization objective.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseAssignment
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 更优层最大化目标，最大化层与箱子的匹配系数。
 * Better layer maximization objective, maximizes layer-bin matching coefficients.
 *
 * @property bins 箱子列表 / bin list
 * @property layers 层列表 / layer list
 * @property assignment 精确赋值 / precise assignment
 * @property coefficient 层与箱子的系数函数 / coefficient function for layer and bin
 * @property name 约束名称 / constraint name
 */
class BetterLayerMaximization(
    private val bins: List<Bin<BinLayer>>,
    private val layers: List<BinLayer>,
    private val assignment: PreciseAssignment,
    private val coefficient: (BinLayer, Bin<BinLayer>) -> InfraNumber,
    val name: String = "better_layer_maximization"
) {
    /**
     * 将目标添加到模型。
     * Add objective to model.
     *
     * @param model 元模型 / meta model
     * @return 操作结果 / operation result
     */
    fun invoke(model: MetaModel<InfraNumber>): Try {
        val linearModel = model as AbstractLinearMetaModel<InfraNumber>
        when (val result = linearModel.maximize(
            polynomial = sum(bins.flatMapIndexed { i, bin ->
                layers.mapIndexed { j, layer ->
                    LinearMonomial(coefficient(layer, bin), assignment.x[i, j])
                }
            }),
            name = "better layer"
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





