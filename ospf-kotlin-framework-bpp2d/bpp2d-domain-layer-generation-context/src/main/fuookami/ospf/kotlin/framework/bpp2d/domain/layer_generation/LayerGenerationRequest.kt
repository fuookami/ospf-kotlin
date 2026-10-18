/**
 * 层生成请求与结果。 / Layer generation request and result.
 *
 * 对应 BPP3D 的 `Bpp3dLayerGenerationRequest` / `Bpp3dLayerGenerationResult`：候选列生成的输入与输出。
 * 二维侧把「箱」换为「板材」，把「层」保持为层。
 * / Mirrors BPP3D `Bpp3dLayerGenerationRequest` / `Bpp3dLayerGenerationResult`: the input and output of
 * candidate column generation. 2D replaces bins with sheets while keeping layers.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation

import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 需求模式键。 / Demand mode key.
 *
 * @property mode 需求模式 / demand mode
 * @property key 需求键 / demand key
 * @property quantityUnit 量纲单位 / quantity unit
*/
data class DemandModeKey(
    val mode: String,
    val key: String,
    val quantityUnit: PhysicalUnit? = null
)

/**
 * 层生成需求项。 / Layer generation demand entry.
 *
 * @property mode 需求模式 / demand mode
 * @property key 需求键 / demand key
 * @property quantityUnit 量纲单位 / quantity unit
*/
data class LayerGenerationDemandEntry(
    val mode: String,
    val key: String,
    val quantityUnit: PhysicalUnit? = null
)

/**
 * 层生成请求。 / Layer generation request.
 *
 * @param V 数值类型 / numeric type
 * @property iteration 当前迭代 / current iteration
 * @property sheet 主板材类型 / primary sheet type
 * @property sheets 参与候选生成的全部板材类型；为空时退化为仅 `sheet` / all sheet types taking part in
 * candidate generation; empty means only `sheet`
 * @property items 参与排样的物品与件数 / items and amounts taking part in the layout
 * @property existingLayers 已有层，供历史生成器回放 / existing layers replayed by the historical generator
 * @property demandEntries 需求项 / demand entries
 * @property shadowPrices 影子价格 / shadow prices
 * @property scoreByShadowPrice 影子价格评分函数 / shadow-price scoring function
 * @property kerf 刀缝间距，null 表示无刀缝 / kerf spacing, null meaning no kerf
 * @property timeLimit 时间限制 / time limit
 * @property maxCandidates 最大候选数 / maximum candidate count
*/
data class Bpp2dLayerGenerationRequest<V : FloatingNumber<V>>(
    val iteration: Int,
    val sheet: SheetType<V>? = null,
    val sheets: List<SheetType<V>> = emptyList(),
    val items: List<Pair<ActualItem<V>, Int>> = emptyList(),
    val existingLayers: List<Layer<V>> = emptyList(),
    val demandEntries: List<LayerGenerationDemandEntry> = emptyList(),
    val shadowPrices: Map<DemandModeKey, V> = emptyMap(),
    val scoreByShadowPrice: ((Layer<V>, Bpp2dLayerGenerationRequest<V>) -> V)? = null,
    val kerf: Quantity<V>? = null,
    val timeLimit: Duration = ZERO,
    val maxCandidates: Int = 256
)

/**
 * 层生成结果。 / Layer generation result.
 *
 * @param V 数值类型 / numeric type
 * @property layer 候选层 / candidate layer
 * @property reducedCost 检验数 / reduced cost
 * @property score 评分 / score
 * @property source 生成来源 / generation source
*/
data class Bpp2dLayerGenerationResult<V : FloatingNumber<V>>(
    val layer: Layer<V>,
    val reducedCost: V? = null,
    val score: V? = null,
    val source: String
)

/**
 * 层生成器接口。 / Layer generator interface.
 *
 * @param V 数值类型 / numeric type
*/
interface Bpp2dLayerGenerator<V : FloatingNumber<V>> {

    /**
     * 生成候选层。 / Generate candidate layers.
     *
     * @param request 层生成请求 / layer generation request
     * @return 候选层结果列表 / candidate layer result list
    */
    fun generate(request: Bpp2dLayerGenerationRequest<V>): List<Bpp2dLayerGenerationResult<V>>
}

/**
 * 判断请求中是否含圆形件。 / Check whether the request contains any circle item.
 *
 * @param V 数值类型 / numeric type
 * @return 是否含圆形件 / whether any circle item is present
*/
fun <V : FloatingNumber<V>> Bpp2dLayerGenerationRequest<V>.hasCircleItems(): Boolean {
    return items.any { (item, _) -> item.isCircleItem() }
}

/**
 * 判断请求中是否含矩形件。 / Check whether the request contains any rectangle item.
 *
 * @param V 数值类型 / numeric type
 * @return 是否含矩形件 / whether any rectangle item is present
*/
fun <V : FloatingNumber<V>> Bpp2dLayerGenerationRequest<V>.hasRectangleItems(): Boolean {
    return items.any { (item, _) -> !item.isCircleItem() }
}

/**
 * 读取请求的刀缝间距。 / Read the kerf spacing of the request.
 *
 * 未设置刀缝时返回与样本同单位的零间距；调用方总是持有一个形状，因此不会出现无法构造零值的情况。
 * / When the kerf is unset, a zero spacing in the sample's unit is returned. Callers always hold a shape, so a zero
 * value can always be built.
 *
 * @param V 数值类型 / numeric type
 * @param sample 样本量值，用于提供单位 / sample quantity providing the unit
 * @return 刀缝间距 / kerf spacing
*/
fun <V : FloatingNumber<V>> Bpp2dLayerGenerationRequest<V>.kerfSpacing(sample: Quantity<V>): Quantity<V> {
    val explicit = kerf
    return if (explicit != null && quantityIsPositive(explicit)) explicit else quantityZeroOf(sample)
}