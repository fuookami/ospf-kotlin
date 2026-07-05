package fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.*

/**
 * 跨所有服务的每个节点聚合入度、出度和流出的中间符号。Intermediate symbols for aggregated in-degree, out-degree, and out-flow at each node across all services.
 *
 * @property nodes 节点列表 / Node list
 * @property services 服务列表 / Service list
 * @property serviceBandwidth 服务带宽模型 / Service bandwidth model
 */
class NodeBandwidth(
    private val nodes: List<Node>,
    private val services: List<Service>,
    private val serviceBandwidth: ServiceBandwidth
) {
    lateinit var inDegree: LinearIntermediateSymbols1<Flt64>
    lateinit var outDegree: LinearIntermediateSymbols1<Flt64>
    lateinit var outFlow: LinearIntermediateSymbols1<Flt64>

    /**
     * 注册所有中间符号到模型。Register all intermediate symbols to the model.
     *
     * @param model 线性元模型 / Linear meta model
     * @return 操作结果 / Operation result
     */
    fun register(model: LinearMetaModel<Flt64>): Try {
        if (!::inDegree.isInitialized) {
            inDegree = flatMap(
                "bandwidth_indegree_node",
                nodes,
                { n -> sum(serviceBandwidth.inDegree[n, _a]) },
                { (_, n) -> "$n" }
            )
        }
        model.add(inDegree)

        if (!::outDegree.isInitialized) {
            outDegree = flatMap(
                "bandwidth_outdegree_node",
                nodes,
                { n ->
                    if (n is NormalNode) {
                        sum(serviceBandwidth.outDegree[n, _a])
                    } else {
                        LinearPolynomial()
                    }
                },
                { (_, n) -> "$n" }
            )
        }
        model.add(outDegree)

        if (!::outFlow.isInitialized) {
            outFlow = flatMap(
                "bandwidth_outflow_node",
                nodes,
                { n ->
                    if (n is NormalNode) {
                        sum(serviceBandwidth.outFlow[n, _a])
                    } else {
                        LinearPolynomial()
                    }
                },
                { (_, n) -> "$n" }
            )
        }
        model.add(outFlow)

        return ok
    }
}
