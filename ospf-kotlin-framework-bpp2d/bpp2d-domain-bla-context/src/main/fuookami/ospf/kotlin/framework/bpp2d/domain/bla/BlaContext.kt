/**
 * BLA 领域上下文。 / BLA domain context.
 *
 * 对应 BPP3D 的 `BLAContext`：向应用层暴露二维底部向上左对齐排样能力。
 * / Mirrors BPP3D `BLAContext`: exposes 2D bottom-up left-justified layout to the application layer.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.bla

import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.framework.bpp2d.domain.bla.service.Bla2dLayoutResult
import fuookami.ospf.kotlin.framework.bpp2d.domain.bla.service.BottomUpLeftJustifiedAlgorithm
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.Sheet2Shape

/**
 * BLA 领域上下文。 / BLA domain context.
 *
 * @param V 数值类型 / numeric type
*/
class BlaContext<V : FloatingNumber<V>> {

    /**
     * 在给定板材上执行二维左对齐下对齐排样。 / Run 2D left-bottom justified layout on the given sheet.
     *
     * @param sheet 板材形状 / sheet shape
     * @param items 物品与件数 / items and their amounts
     * @param config 算法配置 / algorithm configuration
     * @return 排样结果 / layout result
    */
    fun layout(
        sheet: Sheet2Shape<V>,
        items: List<Pair<ActualItem<V>, Int>>,
        config: BottomUpLeftJustifiedAlgorithm.Config<V> = BottomUpLeftJustifiedAlgorithm.Config()
    ): Ret<Bla2dLayoutResult<V>> {
        return BottomUpLeftJustifiedAlgorithm(
            sheet = sheet,
            config = config
        ).invoke(items)
    }
}
