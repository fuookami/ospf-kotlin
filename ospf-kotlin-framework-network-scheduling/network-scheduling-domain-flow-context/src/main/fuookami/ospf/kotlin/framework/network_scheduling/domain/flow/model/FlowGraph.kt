package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/**
 * 网络流图领域聚合，统一管理弧流量变量。 / Network-flow aggregate managing arc-flow variables.
 */
class FlowGraph<V : RealNumber<V>> private constructor(
    val networkGraph: NetworkGraph<V>,
    val nodes: List<FlowNode<V>>,
    val arcs: List<FlowArc<V>>,
    val commodities: List<FlowCommodity<V>>,
    val flowUnit: PhysicalUnit
) {
    private val arcIndex: Map<Pair<NetworkNodeId, NetworkNodeId>, Int> =
        arcs.mapIndexed { index, arc -> (arc.from to arc.to) to index }.toMap()
    private val commodityIndex: Map<String, Int> = commodities.mapIndexed { index, commodity -> commodity.id to index }.toMap()
    private var registeredModel: MetaModel<Flt64>? = null

    /** 兼容单商品访问的供需列表。 / Supply-demand list for single-commodity compatibility. */
    val supplyDemands: List<SupplyDemand<V>> get() = commodities.firstOrNull()?.supplyDemands.orEmpty()

    /** 弧流量变量组合。 / Arc-flow variable combination. */
    lateinit var flowVariables: URealVariable1
        private set

    /** 注册弧流量变量。 / Register arc-flow variables. */
    fun register(model: MetaModel<Flt64>): Try {
        if (registeredModel === model) return ok
        if (registeredModel != null) {
            return networkSchedulingFailure(
                "网络流图已注册到其他模型 / Network-flow graph is already registered to another model"
            )
        }

        flowVariables = URealVariable1(
            name = "flow",
            shape = Shape1(arcs.size * commodities.size)
        )
        for ((commodityIndex, commodity) in commodities.withIndex()) {
            for ((arcIndex, arc) in arcs.withIndex()) {
                val index = commodityIndex * arcs.size + arcIndex
                flowVariables[index].name = "flow_${commodity.id}_${arc.from.value}_${arc.to.value}"
                if (commodityIndex == 0) {
                    arc.variableIndex = arcIndex
                }
            }
        }
        return when (val result = model.add(flowVariables)) {
            is Ok -> {
                registeredModel = model
                ok
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /** 获取某条弧对应的 solver 变量。 / Get the solver variable for an arc. */
    fun flowVariable(arc: FlowArc<V>): Ret<AbstractVariableItem<Flt64, *>> {
        val commodity = commodities.firstOrNull()
            ?: return networkSchedulingFailure(
                "获取弧流量变量失败：流图没有商品 / Failed to get arc-flow variable: flow graph has no commodity"
            )
        return flowVariable(commodity.id, arc)
    }

    /** 获取指定商品与弧对应的 solver 变量。 / Get a commodity-arc solver variable. */
    fun flowVariable(
        commodityId: String,
        arc: FlowArc<V>
    ): Ret<AbstractVariableItem<Flt64, *>> {
        val commodityIndex = this.commodityIndex[commodityId]
            ?: return networkSchedulingFailure(
                "获取弧流量变量失败：商品不属于当前流图 / " +
                        "Failed to get arc-flow variable: commodity does not belong to this flow graph"
            )
        val index = arcIndex[arc.from to arc.to]
            ?: return networkSchedulingFailure(
                "获取弧流量变量失败：弧不属于当前流图 / " +
                        "Failed to get arc-flow variable: arc does not belong to this flow graph"
            )
        if (!::flowVariables.isInitialized) {
            return networkSchedulingFailure(
                "获取弧流量变量失败：流图尚未注册 / " +
                        "Failed to get arc-flow variable: flow graph has not been registered"
            )
        }
        return ok(flowVariables[commodityIndex * arcs.size + index])
    }

    /** 获取某条弧的全部商品流量变量。 / Get all commodity-flow variables of an arc. */
    fun flowVariablesOf(arc: FlowArc<V>): Ret<List<AbstractVariableItem<Flt64, *>>> {
        val variables = mutableListOf<AbstractVariableItem<Flt64, *>>()
        for (commodity in commodities) {
            when (val result = flowVariable(commodity.id, arc)) {
                is Ok -> variables.add(result.value)
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok(variables)
    }

    /** 将领域物理量归一化到流量单位。 / Normalize a domain quantity to the flow unit. */
    fun normalizeFlow(
        quantity: Quantity<V>,
        adapter: NetworkSchedulingSolverValueAdapter<V>
    ): Ret<Flt64> {
        return adapter.normalize(quantity, flowUnit)
    }

    /** 获取节点的供需绑定。 / Get a node's supply-demand binding. */
    fun supplyDemandOf(nodeId: NetworkNodeId): SupplyDemand<V>? {
        return commodities.firstOrNull()?.supplyDemands?.firstOrNull { it.nodeId == nodeId }
    }

    /** 获取指定商品与节点的供需绑定。 / Get a commodity-node balance binding. */
    fun supplyDemandOf(commodityId: String, nodeId: NetworkNodeId): SupplyDemand<V>? {
        return commodities.firstOrNull { it.id == commodityId }
            ?.supplyDemands
            ?.firstOrNull { it.nodeId == nodeId }
    }

    companion object {
        /** 创建并校验网络流图。 / Create and validate a network-flow graph. */
        operator fun <V : RealNumber<V>> invoke(
            networkGraph: NetworkGraph<V>,
            arcs: List<FlowArc<V>>,
            supplyDemands: List<SupplyDemand<V>> = emptyList(),
            flowUnit: PhysicalUnit = NoneUnit
        ): Ret<FlowGraph<V>> {
            val commodity = when (val result = FlowCommodity("default", supplyDemands)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            return create(
                networkGraph = networkGraph,
                arcs = arcs,
                commodities = listOf(commodity),
                flowUnit = flowUnit
            )
        }

        /** 创建多商品网络流图。 / Create a multi-commodity network-flow graph. */
        fun <V : RealNumber<V>> withCommodities(
            networkGraph: NetworkGraph<V>,
            arcs: List<FlowArc<V>>,
            commodities: List<FlowCommodity<V>>,
            flowUnit: PhysicalUnit = NoneUnit
        ): Ret<FlowGraph<V>> {
            return create(
                networkGraph = networkGraph,
                arcs = arcs,
                commodities = commodities,
                flowUnit = flowUnit
            )
        }

        private fun <V : RealNumber<V>> create(
            networkGraph: NetworkGraph<V>,
            arcs: List<FlowArc<V>>,
            commodities: List<FlowCommodity<V>>,
            flowUnit: PhysicalUnit
        ): Ret<FlowGraph<V>> {
            if (commodities.isEmpty()) {
                return networkSchedulingFailure(
                    "创建网络流图失败：至少需要一个商品 / Failed to create flow graph: at least one commodity is required"
                )
            }
            val graphArcKeys = networkGraph.arcs.map { it.from to it.to }.toSet()
            val flowArcKeys = arcs.map { it.from to it.to }
            if (flowArcKeys.toSet().size != flowArcKeys.size) {
                return networkSchedulingFailure(
                    "创建网络流图失败：弧重复 / Failed to create flow graph: duplicate flow arc"
                )
            }
            if (!flowArcKeys.all { it in graphArcKeys }) {
                return networkSchedulingFailure(
                    "创建网络流图失败：流弧不属于网络图 / Failed to create flow graph: flow arc is not in network graph"
                )
            }
            if (flowArcKeys.size != graphArcKeys.size) {
                return networkSchedulingFailure(
                    "创建网络流图失败：必须为每条网络弧提供容量 / " +
                            "Failed to create flow graph: every network arc must have capacity"
                )
            }
            val nodeIds = networkGraph.nodes.map { it.id }.toSet()
            if (commodities.map { it.id }.toSet().size != commodities.size) {
                return networkSchedulingFailure(
                    "创建网络流图失败：商品 ID 重复 / Failed to create flow graph: duplicate commodity ID"
                )
            }
            val missingDemand = commodities.asSequence()
                .flatMap { it.supplyDemands.asSequence() }
                .firstOrNull { it.nodeId !in nodeIds }
            if (missingDemand != null) {
                return networkSchedulingFailure(
                    "创建网络流图失败：供需节点 ${missingDemand.nodeId.value} 不存在 / " +
                            "Failed to create flow graph: balance node ${missingDemand.nodeId.value} does not exist"
                )
            }
            val invalidDimension = arcs.firstOrNull {
                !it.capacity.lower.unit.sameDimension(flowUnit) ||
                        !it.capacity.upper.unit.sameDimension(flowUnit)
            }
            if (invalidDimension != null) {
                return networkSchedulingFailure(
                    "创建网络流图失败：弧容量与流量单位量纲不一致 / " +
                            "Failed to create flow graph: arc capacity and flow unit dimensions differ"
                )
            }
            val invalidBalance = commodities.asSequence()
                .flatMap { it.supplyDemands.asSequence() }
                .firstOrNull { !it.net.unit.sameDimension(flowUnit) }
            if (invalidBalance != null) {
                return networkSchedulingFailure(
                    "创建网络流图失败：节点供需与流量单位量纲不一致 / " +
                            "Failed to create flow graph: node balance and flow unit dimensions differ"
                )
            }
            val balancesByNode = commodities
                .flatMap { commodity ->
                    commodity.supplyDemands.map { demand ->
                        demand.nodeId to (commodity.id to demand.balance)
                    }
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, balances) -> balances.toMap() }
            val nodes = networkGraph.nodes.map { node ->
                FlowNode.wrap(node, balancesByNode[node.id].orEmpty())
            }
            return ok(FlowGraph(networkGraph, nodes, arcs.toList(), commodities.toList(), flowUnit))
        }
    }
}
