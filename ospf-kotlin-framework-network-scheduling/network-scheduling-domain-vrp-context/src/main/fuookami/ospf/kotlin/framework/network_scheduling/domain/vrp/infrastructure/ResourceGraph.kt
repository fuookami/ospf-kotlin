package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkNodeId
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure

/** 车辆资源维度的不可变有向多重图。 / Immutable directed multigraph partitioned by vehicle resource. */
class ResourceGraph<K> private constructor(
    nodes: List<NetworkNodeId>,
    arcs: List<ResourceArc<K>>
) {
    val nodes: List<NetworkNodeId> = nodes.toList()
    val arcs: List<ResourceArc<K>> = arcs.toList()

    private val outgoing = this.arcs.groupBy { it.resourceKey to it.from }
    private val between = this.arcs.groupBy { Triple(it.resourceKey, it.from, it.to) }

    /** 获取资源从节点出发的弧。 / Get arcs leaving a node for a resource.
     * @param resourceKey 资源稳定键 / Stable resource key
     * @param from 起点 / Origin
     * @return 弧快照 / Arc snapshot
     */
    fun outgoing(resourceKey: K, from: NetworkNodeId): List<ResourceArc<K>> =
        outgoing[resourceKey to from].orEmpty()

    /** 获取指定端点间的全部平行弧。 / Get all parallel arcs between two endpoints.
     * @param resourceKey 资源稳定键 / Stable resource key
     * @param from 起点 / Origin
     * @param to 终点 / Destination
     * @return 弧快照 / Arc snapshot
     */
    fun between(resourceKey: K, from: NetworkNodeId, to: NetworkNodeId): List<ResourceArc<K>> =
        between[Triple(resourceKey, from, to)].orEmpty()

    companion object {
        /** 创建并校验资源多重图。 / Create and validate a resource multigraph.
         * @param nodes 图节点 / Graph nodes
         * @param arcs 资源弧，平行弧须使用不同 ID / Resource arcs; parallel arcs need distinct IDs
         * @return 图或输入校验失败 / Graph or input-validation failure
         */
        operator fun <K> invoke(
            nodes: List<NetworkNodeId>,
            arcs: List<ResourceArc<K>>
        ): Ret<ResourceGraph<K>> {
            val nodeSet = nodes.toSet()
            if (nodes.any { it.value.isBlank() } || nodeSet.size != nodes.size) {
                return networkSchedulingFailure("资源图节点标识为空或重复 / Blank or duplicate resource-graph node identity")
            }
            if (arcs.any { it.id.value.isBlank() } || arcs.map { it.resourceKey to it.id }.toSet().size != arcs.size) {
                return networkSchedulingFailure("资源图弧标识为空或同一资源内重复 / Blank or duplicate arc identity within a resource")
            }
            if (arcs.any { it.from !in nodeSet || it.to !in nodeSet }) {
                return networkSchedulingFailure("资源图弧引用缺失节点 / Resource arc references a missing node")
            }
            return ok(ResourceGraph(nodes, arcs))
        }
    }
}
