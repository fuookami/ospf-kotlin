package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.service.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.service.limits.*

/** 使用单次状态完成校验、变量、广义平衡与管线注册的聚合。 / Per-registration aggregate for validation, variables, generalized balances, and pipelines.
 *
 * @param V 领域数值类型 / Domain numeric type
 * @property problem 不可变业务问题 / Immutable business problem
 * @property adapter solver 数值转换边界 / Solver numeric-conversion boundary
 * @property name 模型命名空间 / Model namespace
 * @property solverTolerance solver 单位可行性容差 / Solver-unit feasibility tolerance
 * @property extensionFactories 接收已注册弧表达式的业务扩展管线工厂 / Business pipeline factories receiving registered arc expressions
 */
class GeneralizedFlowAggregation<V : RealNumber<V>>(
    val problem: GeneralizedFlowProblem<V>,
    val adapter: NetworkSchedulingSolverValueAdapter<V>,
    val name: String = "generalized_flow",
    extensionFactories: List<GeneralizedFlowPipelineFactory<V>> = emptyList(),
    val solverTolerance: GeneralizedFlowSolverTolerance = GeneralizedFlowSolverTolerance()
) {
    private val extensionFactories = extensionFactories.toList()
    private var attempted = false
    private var registeredModel: AbstractLinearMetaModel<Flt64>? = null
    private var registeredMinimizeCost: Boolean? = null
    private var preparedArcs: List<PreparedGeneralizedFlowArc<V>> = emptyList()
    private var inputVariables: Map<NetworkArcId, URealVar> = emptyMap()
    private var registeredExpressions: GeneralizedFlowExpressions? = null
    private var nodeSupplyValues: Map<NetworkNodeId, Flt64> = emptyMap()

    /** 已注册的总费用表达式。 / Registered total-cost expression. */
    var cost: LinearPolynomial<Flt64> = LinearPolynomial(constant = Flt64.zero)
        private set

    /** 注册每条弧的投入变量、有限容量、广义平衡与扩展管线。 / Register arc inputs, finite capacities, generalized balances, and extension pipelines.
     *
     * @param model 元模型 / Meta model
     * @param minimizeCost 是否注册内置总费用目标 / Whether to register the built-in total-cost objective
     * @return 注册结果 / Registration result
     */
    fun register(
        model: AbstractLinearMetaModel<Flt64>,
        minimizeCost: Boolean = true
    ): Try {
        if (registeredModel === model) {
            return if (registeredMinimizeCost == minimizeCost) ok
            else networkSchedulingFailure("重复注册的目标选项不一致 / Repeated registration has different objective options")
        }
        if (attempted) {
            return networkSchedulingFailure("广义流聚合不可跨模型或注册失败后复用 / Generalized-flow aggregation cannot be reused across models or after a failed registration")
        }
        attempted = true

        val prepared = when (val result = prepare()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val component = GeneralizedFlowArcComponent(prepared.arcs, name)
        when (val result = component.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val expressions = GeneralizedFlowExpressions(component.inputs, component.outputs)
        val capacityBounds = prepared.arcs.associate { item ->
            item.arc.networkArc.id to (item.lowerInput to item.upperInput)
        }
        when (val result = registerPipeline(
            GeneralizedFlowInputCapacityPipeline(
                inputs = component.inputs,
                bounds = capacityBounds,
                name = "${name}_capacity"
            ),
            model
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val balances = problem.nodes.associate { it.id to LinearPolynomial(constant = Flt64.zero) }.toMutableMap()
        for (item in prepared.arcs) {
            val arcId = item.arc.networkArc.id
            val from = item.arc.networkArc.from
            val to = item.arc.networkArc.to
            val input = component.inputs.getValue(arcId)
            val output = component.outputs.getValue(arcId)
            balances[from] = balances.getValue(from) + input
            balances[to] = balances.getValue(to) - output
        }
        when (val result = registerPipeline(
            GeneralizedFlowNodeBalancePipeline(
                balances = balances,
                netSupplies = prepared.netSupplies,
                name = "${name}_generalized"
            ),
            model
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        var totalCost = LinearPolynomial(constant = Flt64.zero)
        for (item in prepared.arcs) {
            totalCost += item.costCoefficient * component.inputs.getValue(item.arc.networkArc.id)
        }
        for (factory in extensionFactories) {
            when (val result = registerPipeline(factory(expressions), model)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        if (minimizeCost) {
            when (val result = registerPipeline(
                GeneralizedFlowCostObjective(totalCost, "${name}_cost"),
                model
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        this.cost = totalCost
        preparedArcs = prepared.arcs
        inputVariables = component.variables.toMap()
        registeredExpressions = expressions
        nodeSupplyValues = prepared.netSupplies
        registeredMinimizeCost = minimizeCost
        registeredModel = model
        return ok
    }

    /** 访问弧的投入表达式。 / Get an arc input expression.
     *
     * @param arcId 稳定弧标识 / Stable arc identity
     * @return 起点单位投入表达式或注册错误 / Origin-unit input expression or registration error
     */
    fun input(arcId: NetworkArcId): Ret<LinearPolynomial<Flt64>> {
        return registeredExpressions?.input(arcId)
            ?: networkSchedulingFailure("广义流尚未成功注册 / Generalized flow has not been registered successfully")
    }

    /** 访问弧的到达产出表达式。 / Get an arc arrival-output expression.
     *
     * @param arcId 稳定弧标识 / Stable arc identity
     * @return 终点单位产出表达式或注册错误 / Destination-unit output expression or registration error
     */
    fun output(arcId: NetworkArcId): Ret<LinearPolynomial<Flt64>> {
        return registeredExpressions?.output(arcId)
            ?: networkSchedulingFailure("广义流尚未成功注册 / Generalized flow has not been registered successfully")
    }

    /** 从已回填 solver 解中提取弧结果和节点广义守恒校验。 / Extract arc results and generalized node-balance checks from a populated solver solution.
     *
     * @param model 已注册并回填解的模型 / Registered model with a populated solution
     * @return 领域结果或模型/数值转换错误 / Domain solution or model/numeric-conversion error
     */
    fun extractSolution(model: AbstractLinearMetaModel<Flt64>): Ret<GeneralizedFlowSolution<V>> {
        if (registeredModel !== model) {
            return networkSchedulingFailure("广义流模型未注册或模型身份不匹配 / Generalized-flow model is unregistered or has a different identity")
        }

        val nodeTotals = problem.nodes.associate { it.id to NodeTotals() }.toMutableMap()
        val arcResults = mutableMapOf<NetworkArcId, GeneralizedFlowArcResult<V>>()
        var totalCost = Flt64.zero
        for (item in preparedArcs) {
            val arc = item.arc
            val arcId = arc.networkArc.id
            val variable = inputVariables[arcId]
                ?: return networkSchedulingFailure("广义流投入变量缺失：$arcId / Generalized-flow input variable is missing: $arcId")
            val inputValue = model.tokens.find(variable)?.result
                ?: return networkSchedulingFailure("缺失广义流投入结果：$arcId / Missing generalized-flow input result: $arcId")
            if (!inputValue.isFinite()) {
                return networkSchedulingFailure("广义流投入结果必须有限：$arcId / Generalized-flow input result must be finite: $arcId")
            }
            if (inputValue < Flt64.zero) {
                return networkSchedulingFailure("广义流投入不能为负：$arcId / Generalized-flow input cannot be negative: $arcId")
            }
            val capacityTolerance = solverTolerance.forValues(
                listOf(inputValue, item.lowerInput, item.upperInput)
            ) ?: return networkSchedulingFailure(
                "广义流容量容差计算必须有限：$arcId / Generalized-flow capacity tolerance must be finite: $arcId"
            )
            if ((inputValue < item.lowerInput && item.lowerInput - inputValue > capacityTolerance) ||
                (inputValue > item.upperInput && inputValue - item.upperInput > capacityTolerance)
            ) {
                return networkSchedulingFailure(
                    "广义流投入违反容量界：$arcId，投入=$inputValue，范围=[${item.lowerInput}, ${item.upperInput}] / " +
                        "Generalized-flow input violates capacity bounds: $arcId, input=$inputValue, bounds=[${item.lowerInput}, ${item.upperInput}]"
                )
            }

            val convertedOutput = inputValue * item.unitConversionCoefficient
            val outputValue = inputValue * item.outputCoefficient
            val lossOrGain = convertedOutput - outputValue
            val arcCost = inputValue * item.costCoefficient
            if (!convertedOutput.isFinite() || !outputValue.isFinite() || !lossOrGain.isFinite() || !arcCost.isFinite()) {
                return networkSchedulingFailure("广义流弧结果必须有限：$arcId / Generalized-flow arc results must be finite: $arcId")
            }
            totalCost += arcCost
            if (!totalCost.isFinite()) {
                return networkSchedulingFailure("广义流总费用结果必须有限 / Generalized-flow total-cost result must be finite")
            }
            val fromTotals = nodeTotals[arc.networkArc.from]
                ?: return networkSchedulingFailure("广义流起点统计缺失：$arcId / Generalized-flow origin totals are missing: $arcId")
            val toTotals = nodeTotals[arc.networkArc.to]
                ?: return networkSchedulingFailure("广义流终点统计缺失：$arcId / Generalized-flow destination totals are missing: $arcId")
            fromTotals.outgoingInput += inputValue
            toTotals.incomingOutput += outputValue
            if (!fromTotals.outgoingInput.isFinite() || !toTotals.incomingOutput.isFinite()) {
                return networkSchedulingFailure("广义流节点累计结果必须有限：$arcId / Generalized-flow node totals must be finite: $arcId")
            }

            val fromNode = problem.nodes.firstOrNull { it.id == arc.networkArc.from }
                ?: return networkSchedulingFailure("广义流起点不存在：$arcId / Generalized-flow origin node is missing: $arcId")
            val toNode = problem.nodes.firstOrNull { it.id == arc.networkArc.to }
                ?: return networkSchedulingFailure("广义流终点不存在：$arcId / Generalized-flow destination node is missing: $arcId")
            arcResults[arcId] = GeneralizedFlowArcResult(
                input = when (val result = quantity(inputValue, fromNode.unit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                },
                unitConvertedOutput = when (val result = quantity(convertedOutput, toNode.unit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                },
                output = when (val result = quantity(outputValue, toNode.unit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                },
                lossOrGain = when (val result = quantity(lossOrGain, toNode.unit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                },
                cost = when (val result = quantity(arcCost, problem.costUnit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            )
        }

        val nodeResults = mutableMapOf<NetworkNodeId, GeneralizedFlowNodeResult<V>>()
        for (node in problem.nodes) {
            val totals = nodeTotals[node.id]
                ?: return networkSchedulingFailure("广义流节点统计缺失：${node.id} / Generalized-flow node totals are missing: ${node.id}")
            val netSupply = nodeSupplyValues[node.id]
                ?: return networkSchedulingFailure("广义流节点净供需缺失：${node.id} / Generalized-flow node balance is missing: ${node.id}")
            val realized = totals.outgoingInput - totals.incomingOutput
            val residual = realized - netSupply
            if (!realized.isFinite() || !residual.isFinite()) {
                return networkSchedulingFailure("广义流节点平衡结果必须有限：${node.id} / Generalized-flow node-balance results must be finite: ${node.id}")
            }
            val balanceTolerance = solverTolerance.forValues(listOf(realized, netSupply))
                ?: return networkSchedulingFailure(
                    "广义流节点平衡容差计算必须有限：${node.id} / Generalized-flow node-balance tolerance must be finite: ${node.id}"
                )
            if (residual.abs() > balanceTolerance) {
                return networkSchedulingFailure(
                    "广义流节点平衡不满足：${node.id}，残差=$residual / " +
                        "Generalized-flow node balance is violated: ${node.id}, residual=$residual"
                )
            }
            nodeResults[node.id] = GeneralizedFlowNodeResult(
                declaredNetSupply = when (val result = quantity(netSupply, node.unit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                },
                outgoingInput = when (val result = quantity(totals.outgoingInput, node.unit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                },
                incomingOutput = when (val result = quantity(totals.incomingOutput, node.unit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                },
                realizedNetSupply = when (val result = quantity(realized, node.unit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                },
                residual = when (val result = quantity(residual, node.unit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            )
        }
        val totalCostQuantity = when (val result = quantity(totalCost, problem.costUnit)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok(GeneralizedFlowSolution(
            arcs = arcResults.toMap(),
            nodes = nodeResults.toMap(),
            totalCost = totalCostQuantity
        ))
    }

    private fun prepare(): Ret<PreparedGeneralizedFlow<V>> {
        if (name.isBlank()) {
            return networkSchedulingFailure("广义流模型名称不能为空 / Generalized-flow model name cannot be blank")
        }
        if (!solverTolerance.isValid()) {
            return networkSchedulingFailure(
                "广义流 solver 容差必须是有限非负值 / Generalized-flow solver tolerances must be finite and nonnegative"
            )
        }
        val nodeById = problem.nodes.associateBy { it.id }
        val arcIds = problem.arcs.map { it.networkArc.id }
        if (nodeById.size != problem.nodes.size || arcIds.toSet().size != arcIds.size ||
            problem.graph.nodeIds.toSet() != nodeById.keys || problem.graph.arcIds.toSet() != arcIds.toSet() ||
            problem.arcs.any { arc ->
                problem.graph.from(arc.networkArc) != arc.networkArc.from ||
                    problem.graph.to(arc.networkArc) != arc.networkArc.to ||
                    nodeById[arc.networkArc.from] == null || nodeById[arc.networkArc.to] == null
            }
        ) {
            return networkSchedulingFailure("广义流图、节点或弧引用不一致 / Generalized-flow graph, nodes, or arc references are inconsistent")
        }
        if (problem.nodes.any { it.unit.isAffine } || problem.costUnit.isAffine) {
            return networkSchedulingFailure("广义流只支持线性单位，不支持仿射单位 / Generalized flow supports linear units, not affine units")
        }

        val solverZero = when (val result = adapter.toSolverValue(problem.zero)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (!solverZero.isFinite() || solverZero != Flt64.zero) {
            return networkSchedulingFailure("广义流 solver 零值转换无效 / Generalized-flow solver zero conversion is invalid")
        }

        val netSupplies = mutableMapOf<NetworkNodeId, Flt64>()
        for (node in problem.nodes) {
            val net = when (val result = adapter.normalize(node.netSupply, node.unit)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (!net.isFinite()) {
                return networkSchedulingFailure("节点净供需必须有限：${node.id} / Node balance must be finite: ${node.id}")
            }
            netSupplies[node.id] = net
        }

        val prepared = mutableListOf<PreparedGeneralizedFlowArc<V>>()
        for (arc in problem.arcs) {
            val fromNode = nodeById.getValue(arc.networkArc.from)
            val toNode = nodeById.getValue(arc.networkArc.to)
            val lower = when (val result = adapter.normalize(arc.inputCapacity.lower, fromNode.unit)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val upper = when (val result = adapter.normalize(arc.inputCapacity.upper, fromNode.unit)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (!lower.isFinite() || !upper.isFinite() || lower < Flt64.zero || upper < lower) {
                return networkSchedulingFailure("弧投入容量必须是有限非负上下界：${arc.networkArc.id} / Arc input capacity must have finite nonnegative bounds: ${arc.networkArc.id}")
            }

            val yield = when (val result = adapter.normalize(arc.productionYield, NoneUnit)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (!yield.isFinite() || yield < Flt64.zero) {
                return networkSchedulingFailure("弧业务产率必须有限且非负：${arc.networkArc.id} / Arc business yield must be finite and nonnegative: ${arc.networkArc.id}")
            }

            val unitConversion = if (arc.inputToOutputFactor != null) {
                if (arc.inputToOutputFactor.unit.isAffine) {
                    return networkSchedulingFailure(
                        "弧单位转换系数不能使用仿射单位：${arc.networkArc.id} / Arc unit-conversion factor cannot use an affine unit: ${arc.networkArc.id}"
                    )
                }
                val ratioUnit = toNode.unit / fromNode.unit
                when (val result = adapter.normalize(arc.inputToOutputFactor, ratioUnit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            } else {
                if (!fromNode.unit.sameDimension(toNode.unit)) {
                    return networkSchedulingFailure(
                        "跨量纲广义流弧必须显式声明投入到产出转换系数：${arc.networkArc.id} / Cross-dimensional generalized-flow arc requires an explicit input-to-output conversion factor: ${arc.networkArc.id}"
                    )
                }
                when (val result = adapter.normalize(
                    Quantity(problem.zero.constants.one, fromNode.unit),
                    toNode.unit
                )) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            if (!unitConversion.isFinite() || unitConversion < Flt64.zero) {
                return networkSchedulingFailure("单位转换系数必须有限且非负：${arc.networkArc.id} / Unit-conversion factor must be finite and nonnegative: ${arc.networkArc.id}")
            }
            val outputCoefficient = unitConversion * yield
            if (!outputCoefficient.isFinite()) {
                return networkSchedulingFailure("弧投入到达产出系数必须有限：${arc.networkArc.id} / Arc input-to-output coefficient must be finite: ${arc.networkArc.id}")
            }

            val costUnitPerInput = problem.costUnit / fromNode.unit
            val cost = when (val result = adapter.normalize(arc.networkArc.cost, costUnitPerInput)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (!cost.isFinite()) {
                return networkSchedulingFailure("弧投入资费必须有限：${arc.networkArc.id} / Arc input tariff must be finite: ${arc.networkArc.id}")
            }
            prepared.add(PreparedGeneralizedFlowArc(
                arc = arc,
                lowerInput = lower,
                upperInput = upper,
                unitConversionCoefficient = unitConversion,
                outputCoefficient = outputCoefficient,
                costCoefficient = cost
            ))
        }
        return ok(PreparedGeneralizedFlow(netSupplies.toMap(), prepared.toList()))
    }

    private fun registerPipeline(
        pipeline: Pipeline<AbstractLinearMetaModel<Flt64>>,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        pipeline.register(model)
        return pipeline(model)
    }

    private fun quantity(value: Flt64, unit: PhysicalUnit): Ret<Quantity<V>> {
        return when (val result = adapter.fromSolverValue(value)) {
            is Ok -> Ok(Quantity(result.value, unit))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    private class NodeTotals(
        var outgoingInput: Flt64 = Flt64.zero,
        var incomingOutput: Flt64 = Flt64.zero
    )

    private data class PreparedGeneralizedFlow<V : RealNumber<V>>(
        val netSupplies: Map<NetworkNodeId, Flt64>,
        val arcs: List<PreparedGeneralizedFlowArc<V>>
    )
}
