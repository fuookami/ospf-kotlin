package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.math.*

val Collection<*>.defaultConcurrentAmount: UInt64
    get() = UInt64(
        maxOf(
            minOf(
                Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                Runtime.getRuntime().availableProcessors()
            ),
            1
        )
    )
