package fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.model.*

/**
 * 聚合边、服务和节点带宽模型，并将其注册到优化模型。Aggregates edge, service, and node bandwidth models and registers them with the optimization model.
 *
 * @property edgeBandwidth 参数。
 * @property serviceBandwidth 参数。
 * @property nodeBandwidth 参数。
 */
class Aggregation(
    val edgeBandwidth: EdgeBandwidth,
    val serviceBandwidth: ServiceBandwidth,
    val nodeBandwidth: NodeBandwidth
) {
    fun register(model: LinearMetaModel<Flt64>): Try {
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
