package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.functional.sum
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/**
 * 路线编译模型。 / Route compilation model.
 *
 * 管理路线变量、客户覆盖符号、车队使用量符号和路线成本符号。
 * 每次迭代新增路线时创建一组非负整数变量，并同步更新所有中间符号。 / Manages route variables, customer coverage symbols, fleet usage symbols,
 * and route cost symbols. Creates a set of non-negative integer variables for each iteration
 * of new routes, and synchronously updates all intermediate symbols.
 */
class RouteCompilation<V : RealNumber<V>>(
    val instance: VrptwInstance<V>,
    val valueAdapter: NetworkSchedulingSolverValueAdapter<V>
) {
    /** 客户索引映射 / Customer index mapping */
    private val customerIndex: Map<CustomerId, Int> =
        instance.customers.withIndex().associate { (i, c) -> c.id to i }

    /** 车辆类型索引映射 / Vehicle type index mapping */
    private val vehicleTypeIndex: Map<VehicleTypeId, Int> =
        instance.vehicleTypes.withIndex().associate { (i, vt) -> vt.id to i }

    /** 列池 / Column pool */
    val columnPool = RouteColumnPool<V>()

    /** 人工覆盖 / Artificial coverage */
    val artificialCoverage = ArtificialCoverage(instance.customers)

    /** 每次迭代的路线变量 / Route variables per iteration */
    private val _x = mutableListOf<UIntVariable1>()
    val x: List<UIntVariable1> get() = _x.toList()

    /** 每次迭代添加的路线 / Routes added per iteration */
    private val _routesIteration = mutableListOf<List<Route<V>>>()
    val routesIteration: List<List<Route<V>>> get() = _routesIteration.toList()

    /** 所有路线 / All routes */
    val routes: List<Route<V>> get() = _routesIteration.flatten()

    /** 路线成本中间符号 / Route cost intermediate symbol */
    lateinit var routeCost: LinearIntermediateSymbol<Flt64>

    /** 客户覆盖中间符号 / Customer coverage intermediate symbols */
    lateinit var customerCoverage: LinearIntermediateSymbols1<Flt64>

    /** 车队使用量中间符号 / Fleet usage intermediate symbols */
    lateinit var fleetUsage: LinearIntermediateSymbols1<Flt64>

    /** 路线到变量索引的双向映射 / Route-to-variable-index bidirectional mapping */
    private val _routeVariableIndex = mutableMapOf<String, Pair<Int, Int>>()
    val routeVariableIndex: Map<String, Pair<Int, Int>> get() = _routeVariableIndex.toMap()

    /** 是否已注册 / Whether registered */
    private var registered = false

    /**
     * 注册到模型。 / Register to model.
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
     */
    fun register(model: MetaModel<Flt64>): Try {
        if (registered) return ok

        when (val result = artificialCoverage.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (!::routeCost.isInitialized) {
            routeCost = LinearExpressionSymbol(
                Flt64,
                name = "route_cost"
            )
        }
        when (val result = model.add(routeCost)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (!::customerCoverage.isInitialized) {
            customerCoverage = LinearIntermediateSymbols1<Flt64>(
                name = "customer_coverage",
                shape = Shape1(instance.customers.size)
            ) { index, _ ->
                val customer = instance.customers[index]
                val aSymbol = artificialCoverage.artificialCoverage[index]
                LinearExpressionSymbol(
                    aSymbol,
                    Flt64,
                    name = "customer_coverage_${customer.id.value}"
                )
            }
        }
        when (val result = model.add(customerCoverage)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (!::fleetUsage.isInitialized) {
            fleetUsage = LinearIntermediateSymbols1<Flt64>(
                name = "fleet_usage",
                shape = Shape1(instance.vehicleTypes.size)
            ) { index, _ ->
                LinearExpressionSymbol(
                    Flt64,
                    name = "fleet_usage_${instance.vehicleTypes[index].id.value}"
                )
            }
        }
        when (val result = model.add(fleetUsage)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        registered = true
        return ok
    }

    /**
     * 添加路线列。 / Add route columns.
     *
     * @param iteration 迭代次数 / Iteration count
     * @param newRoutes 新路线列表 / List of new routes
     * @param model 线性元模型 / Linear meta model
     * @return 去重后实际添加的路线 / Actually added routes after deduplication
     */
    fun addColumns(
        iteration: UInt64,
        newRoutes: List<Route<V>>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<Route<V>>> {
        val addedRoutes = columnPool.addColumns(newRoutes)
        if (addedRoutes.isEmpty()) return Ok(addedRoutes)

        val xi = UIntVariable1("x_$iteration", Shape1(addedRoutes.size))
        val variableGroupIndex = _x.size
        for ((index, route) in addedRoutes.withIndex()) {
            xi[index].name = "x_${iteration}_${route.vehicleTypeId.value}_${index}"
            _routeVariableIndex[route.signature] = Pair(variableGroupIndex, index)
        }
        when (val result = model.add(xi)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        _x.add(xi)
        _routesIteration.add(addedRoutes)

        routeCost.flush()
        for ((index, route) in addedRoutes.withIndex()) {
            val solverCost = when (val c = valueAdapter.normalize(route.cost, instance.units.costUnit)) {
                is Ok -> c.value
                is Failed -> return Failed(c.error)
                is Fatal -> return Fatal(c.errors)
            }
            (routeCost as LinearExpressionSymbol<Flt64>).asMutable() +=
                solverCost * LinearPolynomial(xi[index])
        }

        for ((custIdx, customer) in instance.customers.withIndex()) {
            val coveringRoutes = addedRoutes.mapIndexedNotNull { routeIdx, route ->
                if (route.stops.any { it.customerId == customer.id }) routeIdx else null
            }
            if (coveringRoutes.isNotEmpty()) {
                val coverage = customerCoverage[custIdx]
                coverage.flush()
                coverage.asMutable() += sum(coveringRoutes.map { LinearPolynomial(xi[it]) })
            }
        }

        for ((vtIdx, vehicleType) in instance.vehicleTypes.withIndex()) {
            val typeRouteIndices = addedRoutes.mapIndexedNotNull { routeIdx, route ->
                if (route.vehicleTypeId == vehicleType.id) routeIdx else null
            }
            if (typeRouteIndices.isNotEmpty()) {
                val usage = fleetUsage[vtIdx]
                usage.flush()
                usage.asMutable() += sum(typeRouteIndices.map { LinearPolynomial(xi[it]) })
            }
        }

        return Ok(addedRoutes)
    }

    /**
     * 移除路线列。 / Remove route columns.
     *
     * 第一版默认不自动删列；仅在显式启用列上限时调用。 / First version defaults to no automatic column removal; only called when
     * column limit is explicitly enabled.
     *
     * @param routesToRemove 待移除路线 / Routes to remove
     * @param model 线性元模型 / Linear meta model
     * @return 操作结果 / Operation result
     */
    fun removeColumns(
        routesToRemove: List<Route<V>>,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        for (route in routesToRemove) {
            val (iterIdx, routeIdx) = _routeVariableIndex[route.signature] ?: continue
            val xi = _x[iterIdx]
            val variable = xi[routeIdx]
            removeVariableTerm(routeCost, variable)
            for ((customerIndex, customer) in instance.customers.withIndex()) {
                if (route.stops.any { it.customerId == customer.id }) {
                    removeVariableTerm(customerCoverage[customerIndex], variable)
                }
            }
            val vehicleTypeIndex = vehicleTypeIndex[route.vehicleTypeId]
                ?: return networkSchedulingFailure(
                    "删除路线列失败：车辆类型 ${route.vehicleTypeId.value} 不存在 / " +
                        "Failed to remove route column: vehicle type ${route.vehicleTypeId.value} does not exist"
                )
            removeVariableTerm(fleetUsage[vehicleTypeIndex], variable)
            xi[routeIdx].range.eq(UInt64.zero)
            model.remove(xi[routeIdx])
            _routeVariableIndex.remove(route.signature)
        }
        columnPool.removeColumns(routesToRemove.toSet())
        return ok
    }

    private fun removeVariableTerm(
        symbol: LinearIntermediateSymbol<Flt64>,
        variable: AbstractVariableItem<*, *>
    ) {
        symbol.flush()
        val polynomial = symbol.asMutable()
        val retained = polynomial.monomials.filterNot { it.symbol == variable }
        val constant = polynomial.constant
        polynomial.clear()
        retained.forEach(polynomial::addMonomial)
        polynomial.setConstant(constant)
    }

    /**
     * 切换到 Phase II。 / Switch to Phase II.
     *
     * @param model 线性元模型 / Linear meta model
     * @return 操作结果 / Operation result
     */
    fun switchToPhaseTwo(model: AbstractLinearMetaModel<Flt64>): Try {
        return artificialCoverage.switchToPhaseTwo(model)
    }

    /**
     * 为指定新增列注册增量路线成本目标。 / Register an incremental route-cost objective for added columns.
     *
     * @param objectiveRoutes 本批新增路线 / Routes added in this batch
     * @param model 线性元模型 / Linear meta model
     * @param name 目标名称 / Objective name
     * @return 操作结果 / Operation result
     */
    fun registerRouteCostObjective(
        objectiveRoutes: List<Route<V>>,
        model: AbstractLinearMetaModel<Flt64>,
        name: String
    ): Try {
        if (objectiveRoutes.isEmpty()) return ok
        val terms = mutableListOf<LinearPolynomial<Flt64>>()
        for (route in objectiveRoutes) {
            val (groupIndex, routeIndex) = _routeVariableIndex[route.signature]
                ?: return networkSchedulingFailure(
                    "注册路线成本目标失败：路线 ${route.signature} 缺少变量索引 / " +
                        "Failed to register route cost objective: route ${route.signature} has no variable index"
                )
            val solverCost = when (val result = valueAdapter.normalize(route.cost, instance.units.costUnit)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            terms.add(solverCost * LinearPolynomial(_x[groupIndex][routeIndex]))
        }
        return model.minimize(
            polynomial = sum(terms),
            name = name
        )
    }

    /**
     * 提取路线变量值。 / Extract route variable values.
     *
     * @param model 线性元模型 / Linear meta model
     * @param integralityTolerance 整数容差 / Integrality tolerance
     * @return 路线及其变量值 / Routes and their variable values
     */
    fun extractRouteValues(
        model: AbstractLinearMetaModel<Flt64>,
        integralityTolerance: Flt64 = instance.tolerances.integrality
    ): Ret<List<Pair<Route<V>, Flt64>>> {
        val result = mutableListOf<Pair<Route<V>, Flt64>>()
        for (route in columnPool.routes) {
            val (iterIdx, routeIdx) = _routeVariableIndex[route.signature] ?: continue
            val xi = _x.getOrNull(iterIdx) ?: continue
            val variable = xi[routeIdx]

            var value: Flt64? = null
            for (token in model.tokens.tokens) {
                if (token.belongsTo(variable)) {
                    value = token.result
                    break
                }
            }
            if (value != null) {
                result.add(route to value)
            }
        }
        return Ok(result)
    }

    /**
     * 提取解中的路线。 / Extract routes in the solution.
     *
     * @param model 线性元模型 / Linear meta model
     * @param integralityTolerance 整数容差 / Integrality tolerance
     * @return 选中的路线 / Selected routes
     */
    fun extractSolution(
        model: AbstractLinearMetaModel<Flt64>,
        integralityTolerance: Flt64 = instance.tolerances.integrality
    ): Ret<List<Route<V>>> {
        val routeValues = when (val r = extractRouteValues(model, integralityTolerance)) {
            is Ok -> r.value
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }
        return Ok(routeValues.filter { (_, value) -> value geq (Flt64.one - integralityTolerance) }
            .map { (route, _) -> route })
    }
}
