package fuookami.ospf.kotlin.framework.network_scheduling.application.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/**
 * 不可变分支决策。 / Immutable branch decision.
 *
 * 四种分支决策对应合同 3.5 节的车辆类型分配分支和弧分支：
 * - ForbidVehicleType / RequireVehicleType：基于 assignmentValue
 * - ForbidArc / RequireArc：基于 edgeValue
 *
 * Four branch decisions corresponding to contract section 3.5:
 * - ForbidVehicleType / RequireVehicleType: based on assignmentValue
 * - ForbidArc / RequireArc: based on edgeValue
 */
sealed class BranchDecision {

    /**
     * 车辆类型 k 禁止服务客户 i。 / Vehicle type k is forbidden to serve customer i.
     *
     * 左侧分支：禁止 k 服务 i。
     * 左侧 branch mask 更新：forbiddenNodes[k] += customerNodeId。
     */
    data class ForbidVehicleType(
        val vehicleTypeId: VehicleTypeId,
        val customerId: CustomerId,
        val customerNodeId: NetworkNodeId
    ) : BranchDecision()

    /**
     * 禁止其它车辆类型服务客户 i，由精确覆盖约束强制客户 i 使用类型 k。 / Other vehicle types are / forbidden to serve customer i; exact coverage constraint forces customer i to use type k.
     *
     * 右侧分支：要求 k 服务 i。
     * 右侧 branch mask 更新：requiredResources[customerNodeId] = vehicleTypeId。
     */
    data class RequireVehicleType(
        val vehicleTypeId: VehicleTypeId,
        val customerId: CustomerId,
        val customerNodeId: NetworkNodeId
    ) : BranchDecision()

    /**
     * 车辆类型 k 禁止使用弧 (i, j)。 / Vehicle type k is forbidden to use arc (i, j).
     *
     * 左侧分支：禁止弧 (i, j)。
     * 左侧 branch mask 更新：forbiddenArcs += ResourceArc(vehicleTypeId, from, to)。
     */
    data class ForbidArc(
        val vehicleTypeId: VehicleTypeId,
        val from: NetworkNodeId,
        val to: NetworkNodeId
    ) : BranchDecision()

    /**
     * 强制弧 (i, j) 取值为 1。 / Force arc (i, j) to take value 1.
     *
     * 右侧分支：要求弧 (i, j)。
     * RequireArc 必须识别 depot：
     * - 当 from 是起始 depot 时，只限制 to 的其它入弧；
     * - 当 to 是结束 depot 时，只限制 from 的其它出弧；
     * - 不得因要求一条 depot 弧而禁止其它车辆路线离开或返回 depot。
     *
     * RequireArc must identify depot:
     * - When from is start depot, only restrict other incoming arcs of to;
     * - When to is end depot, only restrict other outgoing arcs of from;
     * - Must not forbid other vehicle routes from leaving/returning to depot.
     */
    data class RequireArc(
        val vehicleTypeId: VehicleTypeId,
        val from: NetworkNodeId,
        val to: NetworkNodeId
    ) : BranchDecision()
}

/**
 * 将分支决策列表累积为 BranchMask 构造参数。 / Accumulate branch decisions into
 * BranchMask constructor parameters.
 *
 * @param startDepot 起始仓库节点 / Start depot node ID
 * @param endDepot 结束仓库节点 ID / End depot node ID
 * @param decisions 从根到当前节点的全部决策 / All decisions from root to current node
 * @return 不可变 BranchMask 或冲突失败 / Immutable BranchMask or conflict failure
 */
fun accumulateBranchMask(
    startDepot: NetworkNodeId,
    endDepot: NetworkNodeId,
    decisions: List<BranchDecision>
): Ret<BranchMask<VehicleTypeId>> {
    val forbiddenNodes = mutableMapOf<VehicleTypeId, MutableSet<NetworkNodeId>>()
    val requiredResources = mutableMapOf<NetworkNodeId, VehicleTypeId>()
    val forbiddenArcs = mutableSetOf<ResourceArc<VehicleTypeId>>()
    val requiredArcs = mutableSetOf<ResourceArc<VehicleTypeId>>()

    for (decision in decisions) {
        when (decision) {
            is BranchDecision.ForbidVehicleType -> {
                forbiddenNodes.getOrPut(decision.vehicleTypeId) { mutableSetOf() }
                    .add(decision.customerNodeId)
            }

            is BranchDecision.RequireVehicleType -> {
                requiredResources[decision.customerNodeId] = decision.vehicleTypeId
            }

            is BranchDecision.ForbidArc -> {
                forbiddenArcs.add(ResourceArc(decision.vehicleTypeId, decision.from, decision.to))
            }

            is BranchDecision.RequireArc -> {
                requiredArcs.add(ResourceArc(decision.vehicleTypeId, decision.from, decision.to))
            }
        }
    }

    return BranchMask(
        startDepot = startDepot,
        endDepot = endDepot,
        forbiddenNodes = forbiddenNodes.mapValues { it.value.toSet() },
        requiredResources = requiredResources.toMap(),
        forbiddenArcs = forbiddenArcs.toSet(),
        requiredArcs = requiredArcs.toSet()
    )
}
