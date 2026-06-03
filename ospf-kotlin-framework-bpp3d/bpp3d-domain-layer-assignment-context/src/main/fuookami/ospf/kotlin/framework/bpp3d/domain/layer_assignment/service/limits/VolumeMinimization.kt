/**
 * 体积最小化目标。
 * Volume minimization objective.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.ImpreciseAssignment
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BPP3DShadowPriceArguments
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractBPP3DShadowPriceArguments
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractBPP3DShadowPriceMap
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Cuboid
import fuookami.ospf.kotlin.framework.model.CGPipeline
import fuookami.ospf.kotlin.framework.model.ShadowPriceExtractor
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution

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
        Args : AbstractBPP3DShadowPriceArguments<T>,
        T : Cuboid<T>
        >(
    private val assignment: ImpreciseAssignment,
    private val coefficient: InfraNumber,
    override val name: String = "volume_minimization",
) : CGPipeline<Args, AbstractLinearMetaModel<InfraNumber>, AbstractBPP3DShadowPriceMap<Args, T>> {
    companion object {
        /**
         * 创建 Item 专用体积最小化目标，不暴露 `T : Cuboid<T>` 泛型约束。
         * Build item-only volume minimization objective without exposing `T : Cuboid<T>` generic constraint.
         */
        fun forItem(
            assignment: ImpreciseAssignment,
            coefficient: InfraNumber,
            name: String = "volume_minimization"
        ): ItemVolumeMinimization {
            return itemVolumeMinimization(
                assignment = assignment,
                coefficient = coefficient,
                name = name
            )
        }
    }

    /**
     * 将目标添加到模型。
     * Add objective to model.
     *
     * @param model 线性元模型 / linear meta model
     * @return 操作结果 / operation result
     */
    override fun invoke(model: AbstractLinearMetaModel<InfraNumber>): Try {
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
 * Build item-only volume minimization objective so business callers avoid the generic base entry.
 *
 * @param assignment 不精确赋值 / imprecise assignment
 * @param coefficient 体积系数 / volume coefficient
 * @param name 管道名称 / pipeline name
 * @return Item 专用体积最小化目标 / item-only volume minimization objective
 */
fun itemVolumeMinimization(
    assignment: ImpreciseAssignment,
    coefficient: InfraNumber,
    name: String = "volume_minimization"
): ItemVolumeMinimization {
    return ItemVolumeMinimization(
        assignment = assignment,
        coefficient = coefficient,
        name = name
    )
}

/**
 * Item 专用体积最小化目标，不暴露 `T : Cuboid<T>` 泛型约束。
 * Item-only volume minimization objective, does not expose `T : Cuboid<T>` generic constraint.
 *
 * @property assignment 不精确赋值 / imprecise assignment
 * @property coefficient 体积系数 / volume coefficient
 * @property name 管道名称 / pipeline name
 */
class ItemVolumeMinimization(
    private val assignment: ImpreciseAssignment,
    private val coefficient: InfraNumber,
    override val name: String = "volume_minimization"
) : VolumeMinimization<BPP3DShadowPriceArguments, Item>(
    assignment = assignment,
    coefficient = coefficient,
    name = name
)



