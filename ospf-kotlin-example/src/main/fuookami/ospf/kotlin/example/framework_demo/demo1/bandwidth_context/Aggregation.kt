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
 * @property edgeBandwidth 边带宽模型 / Edge bandwidth model
 * @property serviceBandwidth 服务带宽模型 / Service bandwidth model
 * @property nodeBandwidth 节点带宽模型 / Node bandwidth model
 */
class Aggregation(
    val edgeBandwidth: EdgeBandwidth,
    val serviceBandwidth: ServiceBandwidth,
    val nodeBandwidth: NodeBandwidth
) {
    /**
     * 将边带宽、服务带宽和节点带宽模型注册到优化模型。
     * Registers edge bandwidth, service bandwidth, and node bandwidth models to the optimization model.
     *
     * @param model 优化模型 / Optimization model
     * @return 注册结果 / Registration result
     */
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
