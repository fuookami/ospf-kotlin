package fuookami.ospf.kotlin.framework.model

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

open class ShadowPriceKey(
    val limit: KClass<*>
)

class ShadowPrice(
    val key: ShadowPriceKey,
    val price: Flt64
) {
}

typealias ShadowPriceExtractor<M> = (ShadowPriceMap<M>, Array<out Any?>) -> Flt64

open class ShadowPriceMap<M : ShadowPriceMap<M>> {
    val map: Map<ShadowPriceKey, ShadowPrice> by ::_map
    private val _map = HashMap<ShadowPriceKey, ShadowPrice>()
    private val _extractors = ArrayList<ShadowPriceExtractor<M>>()

    operator fun invoke(vararg args: Any?) = _extractors.sumOf(Flt64) { it(this, args) }

    operator fun get(key: ShadowPriceKey): ShadowPrice? = _map[key]

    fun put(price: ShadowPrice) {
        _map[price.key] = price
    }

    fun put(extractor: ShadowPriceExtractor<M>) {
        _extractors.add(extractor)
    }

    fun remove(key: ShadowPriceKey) {
        _map.remove(key)
    }
}

fun <Model : MetaModel<*>, Map : ShadowPriceMap<Map>> extractShadowPrice(
    shadowPriceMap: Map,
    pipelineList: CGPipelineList<Model, Map>,
    model: Model,
    shadowPrices: List<Flt64>
): Try<Error> {
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
    return Ok(success)
}
