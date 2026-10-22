package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkArcId
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkNodeId
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure

/**
 * 不可变车辆类型分配与弧分支遮罩。 / Immutable vehicle-type assignment and arc branch mask.
 *
 * @param K 车辆资源稳定键类型 / Vehicle-resource stable-key type
 */
class BranchMask<K> private constructor(
    val startDepot: NetworkNodeId,
    val endDepot: NetworkNodeId,
    forbiddenNodes: Map<K, Set<NetworkNodeId>>,
    requiredResources: Map<NetworkNodeId, K>,
    forbiddenArcs: Set<ResourceArc<K>>,
    requiredArcs: Set<ResourceArc<K>>
) {
    val forbiddenNodes: Map<K, Set<NetworkNodeId>> = forbiddenNodes.mapValues { it.value.toSet() }
    val requiredResources: Map<NetworkNodeId, K> = requiredResources.toMap()
    val forbiddenArcs: Set<ResourceArc<K>> = forbiddenArcs.toSet()
    val requiredArcs: Set<ResourceArc<K>> = requiredArcs.toSet()

    /** 判断车辆资源是否可访问节点。 / Check whether a vehicle resource may visit a node. */
    fun allowsNode(resourceKey: K, nodeId: NetworkNodeId): Boolean {
        if (nodeId == startDepot || nodeId == endDepot) {
            return true
        }
        if (nodeId in forbiddenNodes[resourceKey].orEmpty()) {
            return false
        }
        return requiredResources[nodeId]?.let { it == resourceKey } ?: true
    }

    /**
     * 判断车辆资源是否可使用弧。 / Check whether a vehicle resource may use an arc.
     *
     * @param resourceKey 资源稳定键 / Stable resource key
     * @param from 起点 / Origin
     * @param to 终点 / Destination
     * @return 是否允许 / Whether the arc is allowed
     */
    fun allowsArc(
        resourceKey: K,
        from: NetworkNodeId,
        to: NetworkNodeId
    ): Boolean {
        return allowsArc(
            resourceKey = resourceKey,
            from = from,
            to = to,
            arcId = defaultResourceArcId(from, to)
        )
    }

    /** 判断带稳定弧标识的资源弧是否可用。 / Check whether an identified resource arc is allowed.
     * @param resourceKey 资源稳定键 / Stable resource key
     * @param from 起点 / Origin
     * @param to 终点 / Destination
     * @param arcId 独立稳定弧标识 / Independent stable arc identity
     * @return 是否允许 / Whether the arc is allowed
     */
    fun allowsArc(
        resourceKey: K,
        from: NetworkNodeId,
        to: NetworkNodeId,
        arcId: NetworkArcId
    ): Boolean {
        return allowsArc(ResourceArc(resourceKey, from, to, arcId))
    }

    /** 判断资源弧是否可用。 / Check whether a resource arc is allowed.
     * @param arc 候选弧 / Candidate arc
     * @return 是否允许 / Whether the arc is allowed
     */
    fun allowsArc(arc: ResourceArc<K>): Boolean {
        val resourceKey = arc.resourceKey
        val from = arc.from
        val to = arc.to
        if (!allowsNode(resourceKey, from) || !allowsNode(resourceKey, to) || arc in forbiddenArcs) {
            return false
        }
        for (required in requiredArcs) {
            if (required.resourceKey == resourceKey && required.from != startDepot && from == required.from && arc != required) {
                return false
            }
            if (required.resourceKey == resourceKey && required.to != endDepot && to == required.to && arc != required) {
                return false
            }
        }
        return true
    }

    /** 判断完整路径是否与遮罩兼容。 / Check whether a complete path is compatible with the mask. */
    fun isRouteCompatible(resourceKey: K, path: List<NetworkNodeId>): Boolean {
        if (path.size < 2 || path.first() != startDepot || path.last() != endDepot) {
            return false
        }
        if (path.any { !allowsNode(resourceKey, it) }) {
            return false
        }
        return isRouteCompatible(resourceKey, path, path.zipWithNext { from, to ->
            defaultResourceArcId(from, to)
        })
    }

    /** 判断包含明确平行弧 ID 的完整路线是否兼容。 / Check a complete route with explicit parallel-arc identities.
     * @param resourceKey 资源稳定键 / Stable resource key
     * @param path 有序节点 / Ordered nodes
     * @param arcIds 有序弧 ID / Ordered arc IDs
     * @return 是否兼容 / Whether compatible
     */
    fun isRouteCompatible(
        resourceKey: K,
        path: List<NetworkNodeId>,
        arcIds: List<NetworkArcId>
    ): Boolean {
        if (path.size < 2 || path.first() != startDepot || path.last() != endDepot ||
            arcIds.size != path.size - 1 || path.any { !allowsNode(resourceKey, it) }) {
            return false
        }
        return path.zipWithNext().zip(arcIds).all { (endpoints, arcId) ->
            allowsArc(resourceKey, endpoints.first, endpoints.second, arcId)
        }
    }

    /** 判断显式资源弧序列组成的路线是否兼容。 / Check a route expressed as explicit resource arcs.
     * @param resourceKey 资源稳定键 / Stable resource key
     * @param arcs 有序资源弧 / Ordered resource arcs
     * @return 是否兼容 / Whether compatible
     */
    fun isResourceArcSequenceCompatible(resourceKey: K, arcs: List<ResourceArc<K>>): Boolean {
        if (arcs.isEmpty() || arcs.any { it.resourceKey != resourceKey } ||
            arcs.first().from != startDepot || arcs.last().to != endDepot ||
            arcs.zipWithNext().any { (first, next) -> first.to != next.from }) {
            return false
        }
        val path = listOf(arcs.first().from) + arcs.map { it.to }
        return isRouteCompatible(resourceKey, path, arcs.map { it.id })
    }

    companion object {
        /**
         * 创建并校验分支遮罩。 / Create and validate a branch mask.
         *
         * @param startDepot 起始仓库 / Start depot
         * @param endDepot 结束仓库 / End depot
         * @param forbiddenNodes 各资源禁止访问的客户 / Customers forbidden for each resource
         * @param requiredResources 客户必须使用的资源 / Required resource for each customer
         * @param forbiddenArcs 禁止弧 / Forbidden arcs
         * @param requiredArcs 必选弧 / Required arcs
         * @return 分支遮罩或冲突失败 / Branch mask or conflict failure
         */
        operator fun <K> invoke(
            startDepot: NetworkNodeId,
            endDepot: NetworkNodeId,
            forbiddenNodes: Map<K, Set<NetworkNodeId>> = emptyMap(),
            requiredResources: Map<NetworkNodeId, K> = emptyMap(),
            forbiddenArcs: Set<ResourceArc<K>> = emptySet(),
            requiredArcs: Set<ResourceArc<K>> = emptySet()
        ): Ret<BranchMask<K>> {
            if (startDepot == endDepot) {
                return networkSchedulingFailure(
                    "创建分支遮罩失败：起止仓库必须使用不同节点 ID / " +
                            "Failed to create branch mask: start and end depots must use different node IDs"
                )
            }
            val normalizedResources = requiredResources.toMutableMap()
            val requiredOutgoing = mutableMapOf<NetworkNodeId, ResourceArc<K>>()
            val requiredIncoming = mutableMapOf<NetworkNodeId, ResourceArc<K>>()
            for (arc in requiredArcs) {
                if (arc in forbiddenArcs) {
                    return networkSchedulingFailure(
                        "创建分支遮罩失败：弧 ${arc.from.value}->${arc.to.value} 同时被禁止和要求 / " +
                                "Failed to create branch mask: arc ${arc.from.value}->${arc.to.value} is both forbidden and required"
                    )
                }
                if (arc.from != startDepot) {
                    val old = requiredOutgoing.put(arc.from, arc)
                    if (old != null && old != arc) {
                        return networkSchedulingFailure(
                            "创建分支遮罩失败：客户 ${arc.from.value} 存在冲突的必选出弧 / " +
                                    "Failed to create branch mask: customer ${arc.from.value} has conflicting required outgoing arcs"
                        )
                    }
                    val oldResource = normalizedResources.put(arc.from, arc.resourceKey)
                    if (oldResource != null && oldResource != arc.resourceKey) {
                        return networkSchedulingFailure(
                            "创建分支遮罩失败：客户 ${arc.from.value} 存在冲突的车辆类型 / " +
                                    "Failed to create branch mask: customer ${arc.from.value} has conflicting vehicle types"
                        )
                    }
                }
                if (arc.to != endDepot) {
                    val old = requiredIncoming.put(arc.to, arc)
                    if (old != null && old != arc) {
                        return networkSchedulingFailure(
                            "创建分支遮罩失败：客户 ${arc.to.value} 存在冲突的必选入弧 / " +
                                    "Failed to create branch mask: customer ${arc.to.value} has conflicting required incoming arcs"
                        )
                    }
                    val oldResource = normalizedResources.put(arc.to, arc.resourceKey)
                    if (oldResource != null && oldResource != arc.resourceKey) {
                        return networkSchedulingFailure(
                            "创建分支遮罩失败：客户 ${arc.to.value} 存在冲突的车辆类型 / " +
                                    "Failed to create branch mask: customer ${arc.to.value} has conflicting vehicle types"
                        )
                    }
                }
            }
            val forbiddenConflict = normalizedResources.entries.firstOrNull { (node, resource) ->
                node in forbiddenNodes[resource].orEmpty()
            }
            if (forbiddenConflict != null) {
                return networkSchedulingFailure(
                    "创建分支遮罩失败：客户 ${forbiddenConflict.key.value} 的必选车辆类型同时被禁止 / " +
                            "Failed to create branch mask: required vehicle type for customer ${forbiddenConflict.key.value} is forbidden"
                )
            }
            return ok(
                BranchMask(
                    startDepot = startDepot,
                    endDepot = endDepot,
                    forbiddenNodes = forbiddenNodes,
                    requiredResources = normalizedResources,
                    forbiddenArcs = forbiddenArcs,
                    requiredArcs = requiredArcs
                )
            )
        }
    }
}
