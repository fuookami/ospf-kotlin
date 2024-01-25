package fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context.model.*

class Aggregation(
    val edgeBandwidth: EdgeBandwidth,
    val serviceBandwidth: ServiceBandwidth,
    val nodeBandwidth: NodeBandwidth
) {
    fun register(model: LinearMetaModel): Try {
        val subprocesses = arrayListOf(
            { return@arrayListOf edgeBandwidth.register(model) },
            { return@arrayListOf serviceBandwidth.register(model) },
            { return@arrayListOf nodeBandwidth.register(model) }
        )

        for (subprocess in subprocesses) {
            when (val result = subprocess()) {
                is Failed -> {
                    return Failed(result.error)
                }

                is Ok -> {}
            }
        }
        return Ok(success)
    }
}
