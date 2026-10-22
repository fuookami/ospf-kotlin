package fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.NetworkDesignContext
import fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.NetworkStructureProblem

internal data class RootedTreeSelection(
    val arcs: Set<NetworkArcId>,
    val nodes: Set<NetworkNodeId>,
    val parents: Map<NetworkNodeId, NetworkArcId>
)

/** 带显式父弧和深度证据的根有向树组件。 / Rooted directed tree with explicit parent arcs and depth certificates.
 * @property problem 结构问题 / Structure problem
 * @property designContext 建设上下文 / Construction context
 * @property name 模型命名空间 / Model namespace
 */
class RootedTreeComponent<V : RealNumber<V>>(
    val problem: NetworkStructureProblem<V>,
    val designContext: NetworkDesignContext<V>,
    val name: String
) {
    private val arcVariables = mutableMapOf<NetworkArcId, BinVar>()
    private val activeVariables = mutableMapOf<NetworkNodeId, BinVar>()
    private val depthVariables = mutableMapOf<NetworkNodeId, URealVar>()
    private val incoming = mutableMapOf<NetworkNodeId, List<NetworkArcId>>()
    private var attempted = false
    private var registeredModel: AbstractLinearMetaModel<Flt64>? = null

    private val graph get() = problem.design.flow.graph
    private val treeNodes get() = problem.treeNodes

    /** 注册父弧唯一性、建设联动和有界深度无环约束。 / Register unique-parent, construction-link and bounded-depth acyclicity constraints.
     * @param model 目标元模型 / Target meta model
     * @return 注册结果 / Registration result
     */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (attempted) return networkSchedulingFailure("根树组件不可复用 / Rooted-tree component cannot be reused")
        attempted = true
        val relays = problem.steinerNodes - problem.root - problem.terminals
        for (node in relays) {
            val variable = BinVar("${name}_tree_node_${node.value}")
            when (val result = model.add(variable)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            activeVariables[node] = variable
        }
        val maximumDepth = (treeNodes.size - 1).coerceAtLeast(0)
        for (node in treeNodes - problem.root) {
            val variable = URealVar("${name}_tree_depth_${node.value}")
            when (val result = model.add(variable)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            depthVariables[node] = variable
            when (val result = model.addConstraint(
                depthExpression(node) leq Flt64(maximumDepth.toDouble()) * activeNode(node),
                name = "${name}_tree_depth_bound_${node.value}"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        for (node in treeNodes) {
            if (node in problem.design.facilities.map { it.node }.toSet()) {
                when (val result = model.addConstraint(
                    activeNode(node) leq designContext.aggregation.construction.facilityAvailability(node),
                    name = "${name}_tree_facility_${node.value}"
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        for (arc in problem.design.flow.arcs) {
            val id = arc.networkArc.id
            if (arc.from !in treeNodes || arc.to !in treeNodes) continue
            if (arc.from == arc.to) continue
            val variable = BinVar("${name}_tree_arc_${id.value}")
            when (val result = model.add(variable)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            arcVariables[id] = variable
            when (val result = model.addConstraint(
                arcExpression(id) leq designContext.aggregation.construction.availability(id),
                name = "${name}_tree_service_${id.value}"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            when (val result = model.addConstraint(
                arcExpression(id) leq activeNode(arc.from),
                name = "${name}_tree_parent_active_${id.value}"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            when (val result = model.addConstraint(
                arcExpression(id) leq activeNode(arc.to),
                name = "${name}_tree_child_active_${id.value}"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        for (node in treeNodes) {
            incoming[node] = problem.design.flow.arcs
                .filter { it.to == node && it.from in treeNodes && it.from != it.to }
                .map { it.networkArc.id }
        }
        for (node in treeNodes) {
            val parentExpression = parentArcExpression(node)
            val relation = if (node == problem.root) {
                parentExpression eq Flt64.zero
            } else {
                parentExpression eq activeNode(node)
            }
            when (val result = model.addConstraint(relation, name = "${name}_tree_parent_${node.value}")) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        val bigM = Flt64(treeNodes.size.coerceAtLeast(1).toDouble())
        for (arc in problem.design.flow.arcs) {
            val selected = arcVariables[arc.networkArc.id] ?: continue
            if (arc.to == problem.root) continue
            val relaxation = LinearPolynomial(constant = Flt64.one) - arcExpression(arc.networkArc.id)
            val order = depthExpression(arc.to) geq depthExpression(arc.from) + Flt64.one - bigM * relaxation
            when (val result = model.addConstraint(order, name = "${name}_tree_order_${arc.networkArc.id.value}")) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        registeredModel = model
        return ok
    }

    /** 读取树弧选择表达式。 / Read a tree-arc selection expression.
     * @param arc 稳定弧标识 / Stable arc identity
     * @return 选择表达式或未知弧错误 / Selection expression or unknown-arc error
     */
    fun selectedTreeArc(arc: NetworkArcId): Ret<LinearPolynomial<Flt64>> {
        if (registeredModel == null) return networkSchedulingFailure("根树尚未注册 / Rooted tree is not registered")
        if (problem.design.flow.graph.arc(arc) == null) return networkSchedulingFailure("树弧不存在 / Tree arc does not exist")
        return ok(arcExpression(arc))
    }

    /** 读取树节点启用表达式。 / Read the tree-node activation expression.
     * @param node 节点标识 / Node identity
     * @return 启用表达式或非候选节点错误 / Activation expression or non-candidate-node error
     */
    fun activeTreeNode(node: NetworkNodeId): Ret<LinearPolynomial<Flt64>> {
        if (registeredModel == null) return networkSchedulingFailure("根树尚未注册 / Rooted tree is not registered")
        if (node !in treeNodes) return networkSchedulingFailure("节点不是树候选节点 / Node is not a tree candidate")
        return ok(activeNode(node))
    }

    /** 读取节点的父弧选择和。 / Read the sum of selected parent arcs for a node.
     * @param node 节点标识 / Node identity
     * @return 父弧表达式或非候选节点错误 / Parent-arc expression or non-candidate-node error
     */
    fun parentArc(node: NetworkNodeId): Ret<LinearPolynomial<Flt64>> {
        if (registeredModel == null) return networkSchedulingFailure("根树尚未注册 / Rooted tree is not registered")
        if (node !in treeNodes) return networkSchedulingFailure("节点不是树候选节点 / Node is not a tree candidate")
        return ok(parentArcExpression(node))
    }

    /** 从 solver token 提取并防御性校验树结构。 / Extract and defensively validate the tree from solver tokens.
     * @param model 已求解元模型 / Solved meta model
     * @return 树选择或缺失/无效结果错误 / Tree selection or missing/invalid-result error
     */
    internal fun extractSolution(model: AbstractLinearMetaModel<Flt64>): Ret<RootedTreeSelection> {
        if (registeredModel !== model) return networkSchedulingFailure("根树模型未注册 / Rooted-tree model is not registered")
        val selected = mutableSetOf<NetworkArcId>()
        for ((id, variable) in arcVariables) {
            val value = model.tokens.find(variable)?.result
                ?: return networkSchedulingFailure("缺失树弧结果 / Missing tree-arc result")
            if (value gr Flt64(0.5)) selected.add(id)
        }
        val active = treeNodes.filter { node ->
            activeVariables[node]?.let { variable ->
                model.tokens.find(variable)?.result?.let { it gr Flt64(0.5) }
                    ?: return networkSchedulingFailure("缺失中转节点结果 / Missing relay-node result")
            } ?: true
        }.toSet()
        val parents = mutableMapOf<NetworkNodeId, NetworkArcId>()
        for (node in active - problem.root) {
            val selectedParents = incoming[node].orEmpty().filter { it in selected }
            if (selectedParents.size != 1) return networkSchedulingFailure("树节点父弧数量无效 / Invalid rooted-tree parent count")
            parents[node] = selectedParents.single()
        }
        for (node in active - problem.root) {
            val visited = mutableSetOf<NetworkNodeId>()
            var current = node
            while (current != problem.root) {
                if (!visited.add(current)) return networkSchedulingFailure("求解结果包含有向环 / Solver result contains a directed cycle")
                val parent = parents[current] ?: return networkSchedulingFailure("活动节点未连接到根 / Active node is disconnected from root")
                current = graph.from(graph.arc(parent) ?: return networkSchedulingFailure("树父弧结果不存在 / Parent arc result does not exist"))
            }
        }
        return ok(RootedTreeSelection(
            arcs = selected,
            nodes = active,
            parents = parents.toMap()
        ))
    }

    private fun activeNode(node: NetworkNodeId): LinearPolynomial<Flt64> {
        if (node == problem.root || node in problem.terminals) return LinearPolynomial(constant = Flt64.one)
        val variable = activeVariables[node] ?: return LinearPolynomial()
        var expression = LinearPolynomial()
        expression += variable
        return expression
    }

    private fun arcExpression(arc: NetworkArcId): LinearPolynomial<Flt64> {
        val variable = arcVariables[arc] ?: return LinearPolynomial()
        var expression = LinearPolynomial()
        expression += variable
        return expression
    }

    private fun parentArcExpression(node: NetworkNodeId): LinearPolynomial<Flt64> {
        var expression = LinearPolynomial()
        for (arc in incoming[node].orEmpty()) expression += arcExpression(arc)
        return expression
    }

    private fun depthExpression(node: NetworkNodeId): LinearPolynomial<Flt64> {
        val variable = depthVariables[node] ?: return LinearPolynomial()
        var expression = LinearPolynomial()
        expression += variable
        return expression
    }
}
