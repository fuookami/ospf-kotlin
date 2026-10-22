package fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.NetworkDesignContext
import fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.NetworkStructureProblem
import fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.NetworkStructureSolution
import fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.service.NetworkStructureConstructionCostObjective

/** 组合建设、根树和分区归属组件。 / Aggregate for construction, rooted tree and partition ownership.
 * @property problem 结构问题 / Structure problem
 * @property designContext 独占建设上下文 / Owned construction context
 * @property name 模型命名空间 / Model namespace
 * @property tree 根树组件 / Rooted-tree component
 * @property coverage 设施覆盖组件 / Facility-coverage component
 * @property constructionCost 建设费用表达式 / Construction-cost expression
 * @property constructionCostObjective 建设费用目标 pipeline / Construction-cost objective pipeline
 */
class NetworkStructureAggregation<V : RealNumber<V>>(
    val problem: NetworkStructureProblem<V>,
    val designContext: NetworkDesignContext<V>,
    val name: String = "network_structure"
) {
    val tree = RootedTreeComponent(
        problem = problem,
        designContext = designContext,
        name = name
    )
    val coverage = FacilityCoverageComponent(
        problem = problem,
        designContext = designContext,
        name = name
    )
    val constructionCost: LinearPolynomial<Flt64>
        get() = designContext.aggregation.construction.constructionCost
    val constructionCostObjective = NetworkStructureConstructionCostObjective(
        cost = { constructionCost },
        name = "${name}_construction_cost"
    )

    private var attempted = false
    private var registeredModel: AbstractLinearMetaModel<Flt64>? = null
    private var registeredObjective: Boolean? = null

    /** 注册底层建设变量、结构组件及可选建设费用目标；不注册商品流。 / Register construction, structure and an optional construction-cost objective, without commodity flows.
     * @param model 目标元模型 / Target meta model
     * @param minimizeCost 是否最小化建设费用 / Whether to minimize construction cost
     * @return 注册结果 / Registration result
     */
    fun register(model: AbstractLinearMetaModel<Flt64>, minimizeCost: Boolean = true): Try {
        if (registeredModel === model) {
            return if (registeredObjective == minimizeCost) ok
            else networkSchedulingFailure("重复结构注册的目标选项不一致 / Inconsistent repeated structure-registration objective option")
        }
        if (attempted) return networkSchedulingFailure("结构上下文不可复用 / Structure context cannot be reused")
        attempted = true
        when (val result = validateProblem()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = designContext.aggregation.construction.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = tree.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = coverage.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (minimizeCost) {
            when (val result = constructionCostObjective(model)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        registeredModel = model
        registeredObjective = minimizeCost
        return ok
    }

    /** 读取树弧选择表达式。 / Read a tree-arc selection expression.
     * @param arc 稳定弧标识 / Stable arc identity
     * @return 选择表达式或未知弧错误 / Selection expression or unknown-arc error
     */
    fun selectedTreeArc(arc: NetworkArcId): Ret<LinearPolynomial<Flt64>> = tree.selectedTreeArc(arc)

    /** 读取树节点启用表达式。 / Read a tree-node activation expression.
     * @param node 节点标识 / Node identity
     * @return 启用表达式或非候选节点错误 / Activation expression or non-candidate-node error
     */
    fun activeTreeNode(node: NetworkNodeId): Ret<LinearPolynomial<Flt64>> = tree.activeTreeNode(node)

    /** 读取节点的父弧选择和。 / Read the sum of selected parent arcs for a node.
     * @param node 节点标识 / Node identity
     * @return 父弧表达式或非候选节点错误 / Parent-arc expression or non-candidate-node error
     */
    fun parentArc(node: NetworkNodeId): Ret<LinearPolynomial<Flt64>> = tree.parentArc(node)

    /** 读取分区归属二元变量。 / Read a partition-owner binary variable.
     * @param partition 分区标识 / Partition identity
     * @param facility 设施节点 / Facility node
     * @return 归属表达式或不合格组合错误 / Assignment expression or ineligible-pair error
     */
    fun partitionAssignment(partition: String, facility: NetworkNodeId): Ret<LinearPolynomial<Flt64>> = coverage.assignment(partition, facility)

    /** 提取结构结果并校验其父子关系。 / Extract structure and validate parent-child relations.
     * @param model 已求解元模型 / Solved meta model
     * @return 结构结果 / Structure solution
     */
    fun extractSolution(model: AbstractLinearMetaModel<Flt64>): Ret<NetworkStructureSolution<V>> {
        if (registeredModel !== model) return networkSchedulingFailure("结构模型未注册 / Structure model is not registered")
        val construction = when (val result = designContext.aggregation.construction.extractSolution(model)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val tree = when (val result = this.tree.extractSolution(model)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val owners = when (val result = coverage.extractSolution(model)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok(NetworkStructureSolution(
            construction = construction,
            selectedTreeArcs = tree.arcs,
            connectedNodes = tree.nodes,
            parentArcs = tree.parents,
            partitionOwners = owners
        ))
    }

    private fun validateProblem(): Try {
        val flowProblem = problem.design.flow
        val graph = flowProblem.graph
        if (problem.root.value.isBlank() || graph.node(problem.root) == null ||
            problem.treeNodes.any { it.value.isBlank() || graph.node(it) == null }) {
            return networkSchedulingFailure("根、终端或中转节点为空或不存在 / Root, terminal or relay node is blank or missing")
        }
        val flowArcIds = flowProblem.arcs.map { it.networkArc.id }
        if (flowArcIds.toSet().size != flowArcIds.size || flowArcIds.toSet() != graph.arcIds.toSet() ||
            flowProblem.arcs.any { arc -> graph.from(arc.networkArc) != arc.from || graph.to(arc.networkArc) != arc.to }) {
            return networkSchedulingFailure("流弧与有向图快照不一致 / Flow arcs do not match the directed graph snapshot")
        }
        val facilities = problem.design.facilities.map { it.node }
        if (facilities.toSet().size != facilities.size || facilities.any { graph.node(it) == null }) {
            return networkSchedulingFailure("候选设施重复或不存在 / Duplicate or missing candidate facility")
        }
        val services = problem.design.services.map { it.arc }
        if (services.toSet().size != services.size || services.any { it !in flowArcIds }) {
            return networkSchedulingFailure("候选服务重复或不存在 / Duplicate or missing candidate service")
        }
        if (problem.partitions.any { it.id.isBlank() } ||
            problem.partitions.map { it.id }.toSet().size != problem.partitions.size ||
            problem.partitions.any { partition -> partition.eligibleFacilities.any { graph.node(it) == null } }) {
            return networkSchedulingFailure("分区标识重复、为空或引用缺失设施节点 / Duplicate, blank partition identity or missing facility node")
        }
        if (problem.quotas.map { it.facility }.toSet().size != problem.quotas.size ||
            problem.quotas.any { quota ->
                graph.node(quota.facility) == null || quota.minimum < 0 ||
                    (quota.maximum != null && quota.maximum < quota.minimum)
            }) {
            return networkSchedulingFailure("设施配额重复、越界或引用缺失节点 / Duplicate, invalid or missing facility quota")
        }
        return ok
    }
}
