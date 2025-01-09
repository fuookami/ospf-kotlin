package fuookami.ospf.kotlin.framework.model

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

open class ShadowPriceKey(
    val limit: KClass<*>
)

data class ShadowPrice(
    val key: ShadowPriceKey,
    val price: Flt64
) {
    override fun toString(): String {
        return "$key: $price"
    }
}

typealias ShadowPriceExtractor<Args, M> = (AbstractShadowPriceMap<Args, M>, Args) -> Flt64

abstract class AbstractShadowPriceMap<in Args : Any, in M : AbstractShadowPriceMap<Args, M>> {
    val map: Map<ShadowPriceKey, ShadowPrice> by ::_map
    private val _map = HashMap<ShadowPriceKey, ShadowPrice>()
    private val _extractors = ArrayList<ShadowPriceExtractor<Args, M>>()

    open operator fun invoke(arg: Args) = _extractors.sumOf { it(this, arg) }

    operator fun get(key: ShadowPriceKey): ShadowPrice? = _map[key]

    fun put(price: ShadowPrice) {
        _map[price.key] = price
    }

    fun put(extractor: ShadowPriceExtractor<@UnsafeVariance Args, @UnsafeVariance M>) {
        _extractors.add(extractor)
    }

    fun remove(key: ShadowPriceKey) {
        _map.remove(key)
    }

    fun shrink() {
        _map.entries.removeIf { it.value.price eq Flt64.zero }
    }
}

fun <Args : Any, Model : MetaModel, Map : AbstractShadowPriceMap<Args, Map>> extractShadowPrice(
    shadowPriceMap: Map,
    pipelineList: CGPipelineList<Args, Model, Map>,
    model: Model,
    shadowPrices: List<Flt64>
): Try {
    for (pipeline in pipelineList) {
        when (val ret = pipeline.refresh(shadowPriceMap, model, shadowPrices)) {
            is Ok -> {}
            is Failed -> {
                return Failed(ret.error)
            }
        }
        val extractor = pipeline.extractor() ?: continue
        shadowPriceMap.put(extractor)
    }
    return ok
}
