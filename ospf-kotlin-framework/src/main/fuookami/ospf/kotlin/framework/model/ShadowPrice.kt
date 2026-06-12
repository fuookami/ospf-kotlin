/**
 * 影子价格模型
 * Shadow Price Model
 *
 * 定义影子价格键、价格和映射抽象，用于列生成和 Benders 分解的对偶信息管理。
 * Defines shadow price keys, prices, and map abstractions for managing dual information
 * in column generation and Benders decomposition.
 */
package fuookami.ospf.kotlin.framework.model

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.core.model.mechanism.toMeta
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol

/**
 * 影子价格键
 * Shadow price key
 *
 * @property limit 约束限制类型 / Constraint limit type
 */
open class ShadowPriceKey(
    val limit: KClass<*>
)

/**
 * 影子价格
 * Shadow price
 *
 * @property key 影子价格键 / Shadow price key
 * @property price 影子价格值 / Shadow price value
 */
data class ShadowPrice(
    val key: ShadowPriceKey,
    val price: Flt64
) {
    override fun toString(): String {
        return "$key: $price"
    }
}

/**
 * 影子价格提取器函数类型
 * Shadow price extractor function type
 *
 * @param Args 参数类型 / Argument type
 * @param M 映射类型 / Map type
 */
typealias ShadowPriceExtractor<Args, M> = (AbstractShadowPriceMap<Args, M>, Args) -> Flt64

/**
 * 抽象影子价格映射
 * Abstract shadow price map
 *
 * @param Args 参数类型 / Argument type
 * @param M 映射自身类型 / Map self type
 */
abstract class AbstractShadowPriceMap<in Args : Any, in M : AbstractShadowPriceMap<Args, M>> {
    /** 影子价格映射表 / Shadow price map */
    val map: Map<ShadowPriceKey, ShadowPrice> by ::_map
    private val _map = HashMap<ShadowPriceKey, ShadowPrice>()
    private val _extractors = ArrayList<ShadowPriceExtractor<Args, M>>()

    /**
     * 通过参数计算影子价格总和
     * Calculate shadow price sum via argument
     *
     * @param arg 参数 / Argument
     * @return 影子价格总和 / Shadow price sum
     */
    open operator fun invoke(arg: Args) = _extractors.sumOf { it(this, arg) }

    /**
     * 按键获取影子价格
     * Get shadow price by key
     *
     * @param key 影子价格键 / Shadow price key
     * @return 影子价格，不存在时返回 null / Shadow price, null if not found
     */
    operator fun get(key: ShadowPriceKey): ShadowPrice? = _map[key]

    /**
     * 按键设置影子价格
     * Set shadow price by key
     *
     * @param key 影子价格键 / Shadow price key
     * @param value 影子价格 / Shadow price
     */
    operator fun set(key: ShadowPriceKey, value: ShadowPrice) {
        _map[key] = value
    }

    /**
     * 放置影子价格
     * Put shadow price
     *
     * @param price 影子价格 / Shadow price
     */
    fun put(price: ShadowPrice) {
        _map[price.key] = price
    }

    /**
     * 放置或累加影子价格
     * Put or add shadow price
     *
     * @param price 影子价格 / Shadow price
     */
    fun putOrAdd(price: ShadowPrice) {
        _map[price.key] = ShadowPrice(price.key, (_map[price.key]?.price ?: Flt64.zero) + price.price)
    }

    /**
     * 注册影子价格提取器
     * Register shadow price extractor
     *
     * @param extractor 影子价格提取器 / Shadow price extractor
     */
    fun put(extractor: ShadowPriceExtractor<@UnsafeVariance Args, @UnsafeVariance M>) {
        _extractors.add(extractor)
    }

    /**
     * 按键移除影子价格
     * Remove shadow price by key
     *
     * @param key 影子价格键 / Shadow price key
     */
    fun remove(key: ShadowPriceKey) {
        _map.remove(key)
    }

    /**
     * 收缩：移除零值影子价格
     * Shrink: remove zero-value shadow prices
     */
    fun shrink() {
        _map.entries.removeIf { it.value.price eq Flt64.zero }
    }
}

/**
 * 从管线列表提取影子价格
 * Extract shadow prices from pipeline list
 *
 * @param shadowPriceMap 目标影子价格映射 / Target shadow price map
 * @param pipelineList 列生成管线列表 / Column generation pipeline list
 * @param model 元模型 / Meta model
 * @param shadowPrices 约束影子价格映射 / Constraint shadow price map
 * @param Args 参数类型 / Argument type
 * @param Model 模型类型 / Model type
 * @param Map 映射类型 / Map type
 * @return 操作结果 / Operation result
 */
fun <
        Args : Any,
        Model : MetaModel<*>,
        Map : AbstractShadowPriceMap<Args, Map>
        > extractShadowPrice(
    shadowPriceMap: Map,
    pipelineList: CGPipelineList<Args, Model, Map>,
    model: Model,
    shadowPrices: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
): Try {
    for (pipeline in pipelineList) {
        when (val ret = pipeline.refresh(
            shadowPriceMap = shadowPriceMap,
            model = model,
            shadowPrices = shadowPrices.toMeta()
        )) {
            is Ok -> {}
            is Failed -> {
                return Failed(ret.error)
            }

            is Fatal -> {
                return Fatal(ret.errors)
            }
        }
        val extractor = pipeline.extractor() ?: continue
        shadowPriceMap.put(extractor)
    }
    return ok
}

/**
 * 刷新中间符号的影子价格
 * Refresh shadow prices of intermediate symbol
 *
 * @param shadowPriceMap 目标影子价格映射 / Target shadow price map
 * @param shadowPrices 对偶解 / Dual solution
 * @param Args 参数类型 / Argument type
 * @param Map 映射类型 / Map type
 * @return 操作结果 / Operation result
 */
fun <
        Args : Any,
        Map : AbstractShadowPriceMap<Args, Map>
        > IntermediateSymbol<*>.refresh(
    shadowPriceMap: Map,
    shadowPrices: MetaDualSolution
): Try {
    when (val args = this.args) {
        is ShadowPriceKey -> {
            shadowPrices.symbols[this]?.sumOf { it.second }?.let {
                shadowPriceMap.putOrAdd(ShadowPrice(args, it))
            }
        }
    }

    return ok
}
