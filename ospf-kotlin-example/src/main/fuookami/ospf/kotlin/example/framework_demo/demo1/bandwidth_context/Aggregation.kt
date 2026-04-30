package fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.model.EdgeBandwidth
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.model.NodeBandwidth
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.model.ServiceBandwidth

class Aggregation(
    val edgeBandwidth: EdgeBandwidth,
    val serviceBandwidth: ServiceBandwidth,
    val nodeBandwidth: NodeBandwidth
) {
    fun register(model: LinearMetaModelFlt64): Try {
        val subprocesses = arrayListOf(
            { return@arrayListOf edgeBandwidth.register(model) },
            { return@arrayListOf serviceBandwidth.register(model) },
            { return@arrayListOf nodeBandwidth.register(model) }
        )

        for (subprocess in subprocesses) {
            when (val result = subprocess()) {
                is Failed -> {
                    return result
                }

                is Fatal -> {
                    return result
                }

                is Ok -> {}
            }
        }
        return ok
    }
}









