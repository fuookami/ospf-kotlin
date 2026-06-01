/**
 * 体积最小化目标。
 * Volume minimization objective.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.ImpreciseAssignment
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
class VolumeMinimization<
        Args : AbstractBPP3DShadowPriceArguments<T>,
        T : Cuboid<T>
        >(
    private val assignment: ImpreciseAssignment,
    private val coefficient: InfraNumber,
    override val name: String = "volume_minimization",
) : CGPipeline<Args, AbstractLinearMetaModel<InfraNumber>, AbstractBPP3DShadowPriceMap<Args, T>> {
    companion object {
        /**
         * 创建 Item 专用体积最小化目标，隐藏调用侧 `T : Cuboid<T>` 泛型暴露。
         * Build item-only volume minimization objective and hide caller-side `T : Cuboid<T>` generic exposure.
         */
        fun <Args : AbstractBPP3DShadowPriceArguments<Item>> forItem(
            assignment: ImpreciseAssignment,
            coefficient: InfraNumber,
            name: String = "volume_minimization"
        ): ItemVolumeMinimization<Args> {
            return VolumeMinimization(
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

/** Item 专用体积最小化目标。Item-only volume minimization objective. */
typealias ItemVolumeMinimization<Args> = VolumeMinimization<Args, Item>



