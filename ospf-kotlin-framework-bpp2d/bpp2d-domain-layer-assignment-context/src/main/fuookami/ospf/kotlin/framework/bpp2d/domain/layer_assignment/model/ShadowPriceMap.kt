/**
 * 影子价格映射。 / Shadow price map.
 *
 * 对应 BPP3D 的 `ShadowPriceMap`：为需求约束的影子价格提供类型化键与映射，供列生成的检验数计算读取。
 * / Mirrors BPP3D `ShadowPriceMap`: typed keys and a map for demand-constraint shadow prices, read by the
 * reduced-cost computation of column generation.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.model.AbstractShadowPriceMap
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ItemId

/**
 * 需求影子价格键。 / Demand shadow price key.
 *
 * @property mode 需求种类 / demand kind
 * @property itemId 物品编号 / item identifier
*/
data class Bpp2dDemandShadowPriceKey(
    val mode: Bpp2dAssignmentDemandMode,
    val itemId: ItemId
) : ShadowPriceKey(Bpp2dDemandShadowPriceKey::class)

/**
 * 需求影子价格映射参数。 / Demand shadow price map argument.
 *
 * @property layer 参与检验数计算的候选层 / candidate layer taking part in the reduced-cost computation
*/
data class Bpp2dShadowPriceArguments(
    val layer: fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer<fuookami.ospf.kotlin.math.algebra.number.FltX>
)

/**
 * 二维装箱影子价格映射。 / 2D packing shadow price map.
*/
class Bpp2dShadowPriceMap : AbstractShadowPriceMap<Bpp2dShadowPriceArguments, Bpp2dShadowPriceMap>()
