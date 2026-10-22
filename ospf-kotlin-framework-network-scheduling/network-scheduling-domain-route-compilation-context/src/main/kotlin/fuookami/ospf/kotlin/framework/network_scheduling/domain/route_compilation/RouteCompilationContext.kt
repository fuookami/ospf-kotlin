package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.model.invoke
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.service.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.service.limits.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/**
 * 路线编译上下文。 / Route compilation context.
 *
 * 显式覆盖完整列生成生命周期： / 1. register
 * 2. addColumns
 * 3. removeColumns
 * 4. refreshShadowPrice / extractShadowPrice
 * 5. switchToPhaseTwo
 * 6. extractRouteValues
 * 7. extractSolution / finalize
 *
 * Explicitly covers the full column generation lifecycle:
 * 1. register
 * 2. addColumns
 * 3. removeColumns
 * 4. refreshShadowPrice / extractShadowPrice
 * 5. switchToPhaseTwo
 * 6. extractRouteValues
 * 7. extractSolution / finalize
 *
 * Extra pipeline 生命周期说明 / Extra pipeline lifecycle notes:
 * - extra pipelines 通过管线列表参与 register（管线 invoke）和 shadow price（refresh + extractor）；
 * - addColumns/removeColumns 只修改路线变量和相关中间符号表达式，不影响管线中已注册的约束结构；
 * - extractRouteValues/extractSolution 只提取路线列池中的解，extra pipeline 的约束值由 model 自身管理；
 *
 * - extra pipelines participate in register (pipeline invoke) and shadow price (refresh + extractor)
 *   via the pipeline list;
 * - addColumns/removeColumns only modify route variables and related intermediate symbol expressions,
 *   without affecting already-registered constraint structures in pipelines;
 * - extractRouteValues/extractSolution only extract solutions from the route column pool;
 *   extra pipeline constraint values are managed by the model itself;
 */
class RouteCompilationContext<V : RealNumber<V>>(
    private val instance: VrptwInstance<V>,
    private val valueAdapter: NetworkSchedulingSolverValueAdapter<V>,
    private val extraPipelines: List<CGPipeline<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap>> = emptyList(),
    extraContributions: List<RouteCompilationContribution<V>> = emptyList()
) {
    /** 当前模型专属的扩展贡献快照。 / Model-local extra-contribution snapshot. */
    val extraContributions: List<RouteCompilationContribution<V>> = extraContributions.toList()

    /** 聚合 / Aggregation */
    val aggregation = RouteCompilationAggregation(instance, valueAdapter)

    /** 编译模型 / Compilation model */
    val compilation: RouteCompilation<V> get() = aggregation.compilation

    /** 跨阶段复用的客户覆盖约束管线。 / Customer-coverage pipeline reused across phases. */
    private val customerCoverageConstraint = CustomerCoverageConstraint(instance.customers, compilation)

    /** 跨阶段复用的车队数量约束管线。 / Fleet-size pipeline reused across phases. */
    private val fleetSizeConstraint = FleetSizeConstraint(instance.vehicleTypes, compilation)

    /** 当前管线列表 / Current pipeline list */
    private var _pipelineList: CGPipelineList<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap> = PipelineListGenerator.phaseOne(
        instance = instance,
        compilation = compilation,
        extraPipelines = extraPipelines,
        customerCoverageConstraint = customerCoverageConstraint,
        fleetSizeConstraint = fleetSizeConstraint
    )

    /** 管线列表 / Pipeline list */
    val pipelineList: CGPipelineList<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap> get() = _pipelineList

    /** 当前阶段 / Current phase */
    var phase: PricingPhase = PricingPhase.PhaseOne
        private set

    /** 最近一次从解中提取的额外贡献结果。 / Extra-contribution values from the latest solution extraction. */
    var extractedContributions: Map<String, Any?> = emptyMap()
        private set

    /**
     * 注册到模型。 / Register to model.
     *
     * 注册聚合（变量、符号）和管线列表（约束、目标）。 / Registers aggregation (variables, symbols) and pipeline list (constraints, objectives).
     */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = aggregation.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = _pipelineList(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        for (contribution in extraContributions) {
            when (val result = contribution.register(compilation, model)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }

    /**
     * 添加路线列。 / Add route columns.
     */
    fun addColumns(
        iteration: UInt64,
        newRoutes: List<Route<V>>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<Route<V>>> {
        return when (val result = aggregation.addColumns(iteration, newRoutes, model)) {
            is Ok -> {
                if (phase == PricingPhase.PhaseTwo && result.value.isNotEmpty()) {
                    when (val objectiveResult = compilation.registerRouteCostObjective(
                        objectiveRoutes = result.value,
                        model = model,
                        name = "route_cost_minimization_$iteration"
                    )) {
                        is Ok -> {}
                        is Failed -> return Failed(objectiveResult.error)
                        is Fatal -> return Fatal(objectiveResult.errors)
                    }
                }
                for (contribution in extraContributions) {
                    val columns = when (val columnResult = columnsFor(contribution, result.value)) {
                        is Ok -> columnResult.value
                        is Failed -> return Failed(columnResult.error)
                        is Fatal -> return Fatal(columnResult.errors)
                    }
                    when (val contributionResult = contribution.addColumns(columns, model)) {
                        is Ok -> {}
                        is Failed -> return Failed(contributionResult.error)
                        is Fatal -> return Fatal(contributionResult.errors)
                    }
                }
                result
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 移除路线列。 / Remove route columns.
     */
    fun removeColumns(
        routesToRemove: List<Route<V>>,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        val routesBySignature = compilation.routes.associateBy { it.signature }
        val existingRoutes = routesToRemove.mapNotNull { routesBySignature[it.signature] }
            .distinctBy { it.signature }
        for (contribution in extraContributions) {
            val columns = when (val columnResult = columnsFor(contribution, existingRoutes)) {
                is Ok -> columnResult.value
                is Failed -> return Failed(columnResult.error)
                is Fatal -> return Fatal(columnResult.errors)
            }
            when (val result = contribution.removeColumns(columns, model)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return aggregation.removeColumns(existingRoutes, model)
    }

    /**
     * 提取影子价格。 / Extract shadow prices.
     *
     * 从对偶解中提取影子价格，填充 VrpShadowPriceMap。
     * Extracts shadow prices from the dual solution, populating VrpShadowPriceMap.
     */
    fun extractShadowPrice(
        shadowPriceMap: VrpShadowPriceMap,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        for (pipeline in _pipelineList) {
            when (val ret = pipeline.refresh(shadowPriceMap, model, shadowPrices)) {
                is Ok -> {}
                is Failed -> return Failed(ret.error)
                is Fatal -> return Fatal(ret.errors)
            }
            val extractor = pipeline.extractor() ?: continue
            shadowPriceMap.put(extractor)
        }
        val additionalRows = mutableSetOf<String>()
        for (contribution in extraContributions) {
            val additional = when (val result = contribution.refreshShadowPrices(model, shadowPrices)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            for ((key, value) in additional) {
                if (!additionalRows.add(key)) {
                    return networkSchedulingFailure(
                        "刷新扩展对偶失败：扩展行键 $key 重复 / Failed to refresh extra duals: duplicate extra-row key $key"
                    )
                }
                shadowPriceMap.putAdditionalDual(key, value)
            }
        }
        return ok
    }

    /**
     * 提取影子价格并输出 PricingDuals 快照。 / Extract shadow prices and output PricingDuals snapshot.
     */
    fun extractPricingDuals(
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Ret<PricingDuals> {
        val shadowPriceMap = VrpShadowPriceMap()
        when (val result = extractShadowPrice(shadowPriceMap, model, shadowPrices)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return Ok(shadowPriceMap.toPricingDuals(phase))
    }

    /**
     * 切换到 Phase II。 / Switch to Phase II.
     *
     * 将所有人工变量固定为 0，切换管线列表为 Phase-II 配置。 / Fixes all artificial variables to 0, switches pipeline list to Phase-II configuration.
     */
    fun switchToPhaseTwo(model: AbstractLinearMetaModel<Flt64>): Try {
        if (phase == PricingPhase.PhaseTwo) return ok
        when (val result = aggregation.switchToPhaseTwo(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = RouteCostMinimization(compilation)(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        _pipelineList = PipelineListGenerator.phaseTwo(
            instance = instance,
            compilation = compilation,
            extraPipelines = extraPipelines,
            customerCoverageConstraint = customerCoverageConstraint,
            fleetSizeConstraint = fleetSizeConstraint
        )
        phase = PricingPhase.PhaseTwo
        return ok
    }

    /**
     * 检查 Phase I 是否收敛。 / Check if Phase I has converged.
     */
    fun isPhaseOneConverged(
        model: AbstractLinearMetaModel<Flt64>,
        feasibilityTolerance: Flt64 = instance.tolerances.feasibility
    ): Ret<Boolean> {
        return aggregation.isPhaseOneConverged(model, feasibilityTolerance)
    }

    /**
     * 提取路线变量值。 / Extract route variable values.
     */
    fun extractRouteValues(
        model: AbstractLinearMetaModel<Flt64>,
        integralityTolerance: Flt64 = instance.tolerances.integrality
    ): Ret<List<Pair<Route<V>, Flt64>>> {
        val routeValues = when (val result = aggregation.extractRouteValues(model, integralityTolerance)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        extractedContributions = when (val result = extractContributionValues(model, routeValues)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return Ok(routeValues)
    }

    /**
     * 提取解中的路线。 / Extract routes in the solution.
     */
    fun extractSolution(
        model: AbstractLinearMetaModel<Flt64>,
        integralityTolerance: Flt64 = instance.tolerances.integrality
    ): Ret<List<Route<V>>> {
        val routeValues = when (val result = extractRouteValues(model, integralityTolerance)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return Ok(routeValues.filter { (_, value) -> value geq (Flt64.one - integralityTolerance) }
            .map { (route, _) -> route })
    }

    /** 为额外贡献解析路线变量与同源可加弧系数。 / Resolve route variables and shared additive arc coefficients for extensions.
     * @param contribution 目标扩展贡献 / Target extension
     * @param routes 路线列 / Route columns
     * @return 扩展列快照或系数转换失败 / Extension-column snapshot or coefficient-conversion failure
     */
    private fun columnsFor(
        contribution: RouteCompilationContribution<V>,
        routes: List<Route<V>>
    ): Ret<List<RouteCompilationColumn<V>>> {
        val columns = mutableListOf<RouteCompilationColumn<V>>()
        for (route in routes) {
            val variable = when (val result = compilation.columnVariable(route)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val coefficients = contribution.routeCoefficients(route)
            val solverCoefficients = mutableMapOf<String, Flt64>()
            for ((key, value) in coefficients) {
                val solverValue = when (val result = valueAdapter.toSolverValue(value)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                solverCoefficients[key] = solverValue
            }
            columns.add(RouteCompilationColumn(
                route = route,
                variable = variable,
                coefficients = coefficients,
                solverCoefficients = solverCoefficients
            ))
        }
        return Ok(columns)
    }

    /** 执行额外贡献的最终结果提取。 / Extract final results from every extra contribution.
     * @param model 最终线性主问题 / Final linear master problem
     * @param routeValues 路线列及其解值 / Route columns and their solution values
     * @return 按扩展名称索引的异构结果 / Heterogeneous results keyed by contribution name
     */
    fun extractContributionValues(
        model: AbstractLinearMetaModel<Flt64>,
        routeValues: List<Pair<Route<V>, Flt64>>
    ): Ret<Map<String, Any?>> {
        val values = mutableMapOf<String, Any?>()
        for (contribution in extraContributions) {
            if (contribution.name.isBlank() || contribution.name in values) {
                return networkSchedulingFailure(
                    "提取扩展结果失败：扩展名称为空或重复 / Failed to extract extra results: blank or duplicate contribution name"
                )
            }
            when (val result = contribution.extract(model, routeValues)) {
                is Ok -> values[contribution.name] = result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return Ok(values.toMap())
    }
}
