package fuookami.ospf.kotlin.framework.network_scheduling.domain.structure

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.NetworkDesignProblem
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.model.NetworkConstructionSolution

/** 可由哪些设施负责的分区覆盖请求。 / Partition-coverage request and its eligible owners.
 * @property id 稳定分区标识 / Stable partition identity
 * @property eligibleFacilities 可负责该分区的设施 / Facilities eligible to own this partition
 * @property required 是否必须归属一个设施 / Whether this partition must be assigned
 */
class FacilityPartitionRequest(
    val id: String,
    eligibleFacilities: Set<NetworkNodeId>,
    val required: Boolean = true
) {
    val eligibleFacilities = eligibleFacilities.toSet()
}

/** 设施被分配的分区数量配额。 / Assignment-count quota for a facility.
 * @property facility 设施节点 / Facility node
 * @property minimum 最少归属分区数 / Minimum owned partitions
 * @property maximum 最多归属分区数，空表示不额外限制 / Maximum owned partitions, null means no additional limit
 */
data class FacilityAssignmentQuota(
    val facility: NetworkNodeId,
    val minimum: Int = 0,
    val maximum: Int? = null
)

/** 根有向树与设施分区归属问题。 / Rooted directed-tree and facility-partition assignment problem.
 * @property design 建设问题 / Network construction problem
 * @property root 必须作为根的节点 / Required root node
 * @property terminals 必须连接到根的节点 / Nodes that must connect to the root
 * @property steinerNodes 可选中转节点 / Optional relay nodes
 * @property partitions 独立于商品流的分区覆盖请求 / Partition requests independent of commodity flows
 * @property quotas 设施分区归属数量配额 / Facility assignment-count quotas
 */
class NetworkStructureProblem<V : RealNumber<V>>(
    val design: NetworkDesignProblem<V>,
    val root: NetworkNodeId,
    terminals: Set<NetworkNodeId>,
    steinerNodes: Set<NetworkNodeId> = emptySet(),
    partitions: List<FacilityPartitionRequest> = emptyList(),
    quotas: List<FacilityAssignmentQuota> = emptyList()
) {
    val terminals = terminals.toSet()
    val steinerNodes = steinerNodes.toSet()
    val partitions = partitions.toList()
    val quotas = quotas.toList()
    val treeNodes: Set<NetworkNodeId> get() = setOf(root) + terminals + steinerNodes
}

/** 已注册结构的建设、树和分区归属结果。 / Construction, tree and partition assignments.
 * @property construction 设施与服务建设选择 / Facility and service construction decisions
 * @property selectedTreeArcs 选中的有向树弧 / Selected directed tree arcs
 * @property connectedNodes 根树中的节点 / Nodes in the rooted tree
 * @property parentArcs 每个非根连接节点的父弧 / Parent arc for each connected non-root node
 * @property partitionOwners 已归属分区的设施 / Facility owning each assigned partition
 */
data class NetworkStructureSolution<V : RealNumber<V>>(
    val construction: NetworkConstructionSolution<V>,
    val selectedTreeArcs: Set<NetworkArcId>,
    val connectedNodes: Set<NetworkNodeId>,
    val parentArcs: Map<NetworkNodeId, NetworkArcId>,
    val partitionOwners: Map<String, NetworkNodeId>
)
