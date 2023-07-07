package fuookami.ospf.kotlin.framework.model

import kotlin.reflect.*
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
    override fun toString() = "$key: $price"
}

typealias Extractor<M> = (ShadowPriceMap<M>, Array<out Any?>) -> Flt64

open class ShadowPriceMap<M : ShadowPriceMap<M>>(
    val map: MutableMap<ShadowPriceKey, ShadowPrice> = HashMap(),
    private val extractors: MutableList<Extractor<M>> = ArrayList()
) {
    operator fun invoke(vararg args: Any?) = Flt64(extractors.sumOf { it(this, args).toDouble() })

    operator fun get(key: ShadowPriceKey): ShadowPrice? = map[key]

    fun put(price: ShadowPrice) {
        map[price.key] = price
    }

    fun put(extractor: Extractor<M>) {
        extractors.add(extractor)
    }

    fun remove(key: ShadowPriceKey) {
        map.remove(key)
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
