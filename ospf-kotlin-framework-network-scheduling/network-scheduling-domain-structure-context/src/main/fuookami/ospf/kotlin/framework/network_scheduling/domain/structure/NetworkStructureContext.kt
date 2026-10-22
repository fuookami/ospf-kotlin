package fuookami.ospf.kotlin.framework.network_scheduling.domain.structure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.NetworkDesignContext
import fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.model.NetworkStructureAggregation

/** 根树与设施归属的模型注册入口。 / Model registration entry for rooted trees and facility ownership.
 * @property problem 不可变结构问题 / Immutable structure problem
 * @property adapter 数值边界 / Numeric boundary
 * @property name 模型命名空间 / Model namespace
 * @property designContext 本次注册独占的建设上下文 / Construction context owned by this registration
 * @property aggregation 组件聚合 / Component aggregation
 * @property constructionCost 建设费用表达式，默认目标使用此表达式 / Construction-cost expression used by the default objective
 */
class NetworkStructureContext<V : RealNumber<V>>(
    val problem: NetworkStructureProblem<V>,
    val adapter: NetworkSchedulingSolverValueAdapter<V>,
    val name: String = "network_structure"
) {
    val designContext = NetworkDesignContext(
        problem = problem.design,
        adapter = adapter,
        name = "${name}_design"
    )
    val aggregation = NetworkStructureAggregation(
        problem = problem,
        designContext = designContext,
        name = name
    )
    val constructionCost: LinearPolynomial<Flt64> get() = aggregation.constructionCost

    /** 注册建设变量、根树、设施分区归属及可选建设费用目标。 / Register construction, rooted tree, facility assignments and an optional construction-cost objective.
     * @param model 目标元模型 / Target meta model
     * @param minimizeCost 是否默认最小化建设费用；组合外部目标时传 false / Whether to minimize construction cost by default; pass false when composing another objective
     * @return 注册结果 / Registration result
     */
    fun register(model: AbstractLinearMetaModel<Flt64>, minimizeCost: Boolean = true): Try = aggregation.register(model, minimizeCost)

    /** 读取树弧选择表达式。 / Read a tree-arc selection expression.
     * @param arc 稳定弧标识 / Stable arc identity
     * @return 选择表达式或未知弧错误 / Selection expression or unknown-arc error
     */
    fun selectedTreeArc(arc: NetworkArcId): Ret<LinearPolynomial<Flt64>> = aggregation.selectedTreeArc(arc)

    /** 读取树节点启用表达式；根和终端恒为一。 / Read node activation; the root and terminals are fixed to one.
     * @param node 节点标识 / Node identity
     * @return 启用表达式或非候选节点错误 / Activation expression or non-candidate-node error
     */
    fun activeTreeNode(node: NetworkNodeId): Ret<LinearPolynomial<Flt64>> = aggregation.activeTreeNode(node)

    /** 读取节点的父弧选择和。 / Read the sum of selected parent arcs for a node.
     * @param node 节点标识 / Node identity
     * @return 父弧表达式或未知节点错误 / Parent-arc expression or unknown-node error
     */
    fun parentArc(node: NetworkNodeId): Ret<LinearPolynomial<Flt64>> = aggregation.parentArc(node)

    /** 读取分区归属二元变量。 / Read a partition-owner binary variable.
     * @param partition 分区标识 / Partition identity
     * @param facility 设施节点 / Facility node
     * @return 归属表达式或不合格组合错误 / Assignment expression or ineligible-pair error
     */
    fun partitionAssignment(partition: String, facility: NetworkNodeId): Ret<LinearPolynomial<Flt64>> = aggregation.partitionAssignment(partition, facility)

    /** 提取建设、树和分区结果。 / Extract construction, tree and partition decisions.
     * @param model 已求解的元模型 / Solved meta model
     * @return 结构结果 / Structure solution
     */
    fun extractSolution(model: AbstractLinearMetaModel<Flt64>): Ret<NetworkStructureSolution<V>> = aggregation.extractSolution(model)
}
