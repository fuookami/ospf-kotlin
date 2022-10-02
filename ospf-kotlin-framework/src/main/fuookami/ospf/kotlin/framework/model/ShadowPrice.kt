package fuookami.ospf.kotlin.framework.model

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.utils.math.*

open class ShadowPriceKey(
    val limit: KClass<*>
)

class ShadowPrice(
    val key: ShadowPriceKey,
    val price: Flt64
) {
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
