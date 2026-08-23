package fuookami.ospf.kotlin.framework.network_scheduling.application.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/**
 * 分支树节点状态。 / Branch tree node status.
 */
enum class NodeStatus {
    /** 尚未求解 / Not yet solved */
    Pending,
    /** 已求解 / Solved */
    Solved,
    /** 已剪枝 / Pruned */
    Pruned,
    /** 不可行 / Infeasible */
    Infeasible
}

/**
 * 分支树节点。 / Branch tree node.
 *
 * 每个节点持有从根到当前节点的全部不可变分支决策、构造好的不可变 BranchMask，
 * 以及继承和自身的 LP 下界。 / Each node holds all immutable branch decisions from root to current node,
 * the constructed immutable BranchMask, and both inherited and own LP lower bounds.
 *
 * 不跨节点复用可变 model/context。
 * Mutable model/context is NOT shared across nodes.
 */
class BranchNode(
    /** 节点唯一 ID / Unique node ID */
    val id: Int,
    /** 父节点 ID（根节点为 null） / Parent node ID (null for root) */
    val parentId: Int?,
    /** 树深度 / Tree depth */
    val depth: Int,
    /** 从根到本节点的全部决策（不可变） / All decisions from root to this node (immutable) */
    val decisions: List<BranchDecision>,
    /** 从决策构造的不可变遮罩 / Immutable mask constructed from decisions */
    val branchMask: BranchMask<VehicleTypeId>,
    /** 继承的父节点 LP 下界 / Inherited LP lower bound from parent */
    val inheritedLowerBound: Flt64
) {
    /** 本节点 LP 下界（求解后更新） / Own LP lower bound (updated after solving) */
    var lowerBound: Flt64 = inheritedLowerBound
        private set

    /** 节点状态 / Node status */
    var status: NodeStatus = NodeStatus.Pending
        private set

    /**
     * 有效下界：求解后由本节点 LP 下界替换继承值，未求解节点仍使用继承值。 / Effective lower bound: after solving, the node LP bound replaces the inherited value;
     * pending nodes still use the inherited value.
     */
    val effectiveLowerBound: Flt64 get() = lowerBound

    /**
     * 标记节点为已求解并更新下界。 / Mark node as solved and update lower bound.
     *
     * 定价完成后的 LP 目标值作为节点下界。 / LP objective value after pricing completion serves as node lower bound.
     */
    fun solve(lpLowerBound: Flt64) {
        lowerBound = lpLowerBound
        status = NodeStatus.Solved
    }

    /**
     * 标记节点为不可行。 / Mark node as infeasible.
     */
    fun markInfeasible() {
        status = NodeStatus.Infeasible
    }

    /**
     * 标记节点为已剪枝。 / Mark node as pruned.
     */
    fun prune() {
        status = NodeStatus.Pruned
    }

    /**
     * 判断节点是否仍有可能改进 incumbent。 / Whether the node can still improve the incumbent.
     */
    fun canImprove(incumbent: Flt64?, tolerance: Flt64): Boolean {
        if (incumbent == null) return true
        return effectiveLowerBound ls (incumbent - tolerance)
    }

    companion object {
        /**
         * 创建根节点。 / Create root node.
         *
         * @param startDepot 起始仓库 / Start depot
         * @param endDepot 结束仓库 / End depot
         * @param rootLowerBound 根节点的初始下界（通常为 -∞ 或负无穷大） / Initial lower bound for root
         * @return 根节点 / Root node
         */
        fun root(
            startDepot: NetworkNodeId,
            endDepot: NetworkNodeId,
            rootLowerBound: Flt64 = Flt64.negativeInfinity
        ): Ret<BranchNode> {
            val mask = BranchMask<VehicleTypeId>(
                startDepot = startDepot,
                endDepot = endDepot
            )
            return when (mask) {
                is Ok -> Ok(BranchNode(
                    id = 0,
                    parentId = null,
                    depth = 0,
                    decisions = emptyList(),
                    branchMask = mask.value,
                    inheritedLowerBound = rootLowerBound
                ))
                is Failed -> Failed(mask.error)
                is Fatal -> Fatal(mask.errors)
            }
        }

        /**
         * 创建子节点。 / Create child node.
         *
         * @param id 子节点 ID / Child node ID
         * @param parent 父节点 / Parent node
         * @param decision 新增的分支决策 / New branch decision
         * @param startDepot 起始仓库 / Start depot
         * @param endDepot 结束仓库 / End depot
         * @return 子节点或 BranchMask 构造失败 / Child node or BranchMask construction failure
         */
        fun child(
            id: Int,
            parent: BranchNode,
            decision: BranchDecision,
            startDepot: NetworkNodeId,
            endDepot: NetworkNodeId
        ): Ret<BranchNode> {
            val childDecisions = parent.decisions + decision
            val mask = accumulateBranchMask(startDepot, endDepot, childDecisions)
            return when (mask) {
                is Ok -> Ok(BranchNode(
                    id = id,
                    parentId = parent.id,
                    depth = parent.depth + 1,
                    decisions = childDecisions,
                    branchMask = mask.value,
                    inheritedLowerBound = parent.lowerBound
                ))
                is Failed -> Failed(mask.error)
                is Fatal -> Fatal(mask.errors)
            }
        }
    }
}
