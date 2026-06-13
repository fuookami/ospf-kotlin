/**
 * 体积最小化目标。
 * Volume minimization objective.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.ImpreciseAssignment
import fuookami.ospf.kotlin.framework.model.CGPipeline

/**
 * 体积最小化目标，最小化总体积使用。
 * Volume minimization objective, minimizes total volume usage.
 *
 * @param Args 影子价格参数类型 / shadow price arguments type
 * @param T 立方体类型 / cuboid type
 * @property assignment 不精确赋值 / imprecise assignment
 * @property coefficient 体积系数 / volume coefficient
 * @property name 管道名称 / pipeline name
 */
open class VolumeMinimization<
        Args : AbstractBPP3DShadowPriceArguments<FltX, T>,
        T : Cuboid<T, FltX>
        > protected constructor(
    private val assignment: ImpreciseAssignment,
    private val coefficient: FltX,
    override val name: String = "volume_minimization",
) : CGPipeline<Args, AbstractLinearMetaModel<FltX>, AbstractBPP3DShadowPriceMap<Args, FltX, T>> {
    /**
     * 将目标添加到模型。
     * Add objective to model.
     *
     * @param model 线性元模型 / linear meta model
     * @return 操作结果 / operation result
     */
    override fun invoke(model: AbstractLinearMetaModel<FltX>): Try {
        when (val result = model.minimize(
            monomial = LinearMonomial(coefficient, assignment.volume),
            name = "volume"
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

/**
 * 创建 Item 专用体积最小化目标，供业务调用侧避开泛型基类入口。
 * Build item-only volume minimization objective so business callers avoid the quantity-polymorphic base entry.
 *
 * @param assignment 不精确赋值 / imprecise assignment
 * @param coefficient 体积系数 / volume coefficient
 * @param name 管道名称 / pipeline name
 * @return Item 专用体积最小化目标 / item-only volume minimization objective
 */
fun itemVolumeMinimization(
    assignment: ImpreciseAssignment,
    coefficient: FltX,
    name: String = "volume_minimization"
): ItemVolumeMinimization {
    return ItemVolumeMinimization(
        assignment = assignment,
        coefficient = coefficient,
        name = name
    )
}

/**
 * Item 专用体积最小化目标，不暴露底层泛型 cuboid 约束。
 * Item-only volume minimization objective, does not expose the underlying quantity-polymorphic cuboid constraint.
 *
 * @property assignment 不精确赋值 / imprecise assignment
 * @property coefficient 体积系数 / volume coefficient
 * @property name 管道名称 / pipeline name
 */
class ItemVolumeMinimization(
    private val assignment: ImpreciseAssignment,
    private val coefficient: FltX,
    override val name: String = "volume_minimization"
) : VolumeMinimization<BPP3DShadowPriceArguments, Item>(
    assignment = assignment,
    coefficient = coefficient,
    name = name
)
