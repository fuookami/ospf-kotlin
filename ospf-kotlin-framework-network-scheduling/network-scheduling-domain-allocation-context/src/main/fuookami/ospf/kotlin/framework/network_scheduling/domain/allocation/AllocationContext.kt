package fuookami.ospf.kotlin.framework.network_scheduling.domain.allocation

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.*

/** 运输地点，可选层级按非递减方向约束服务弧。 / A transport location; optional layers constrain services to non-decreasing direction.
 * @property id 稳定地点标识 / Stable location identity
 * @property layer 可选层级，数值越大表示越下游 / Optional layer; larger values are farther downstream
 * @property attributes 地点物理属性 / Physical location attributes
 */
class AllocationLocation<V : RealNumber<V>>(
    val id: NetworkNodeId,
    val layer: Int? = null,
    attributes: Map<String, Quantity<V>> = emptyMap()
) {
    val attributes = attributes.toMap()
}

/** 服务的建设容量等级；容量代表建成后的总容量。 / A construction capacity level; capacity is the resulting total, not an increment.
 * @property id 等级标识 / Level identity
 * @property capacity 建成后的服务容量 / Resulting service capacity
 * @property cost 建设费用 / Construction cost
 */
data class AllocationCapacityLevel<V : RealNumber<V>>(
    val id: String,
    val capacity: Quantity<V>,
    val cost: Quantity<V>
)

/** 可被请求使用的运输服务。 / A transport service that requests may use.
 * @property id 稳定服务标识 / Stable service identity
 * @property from 起点 / Origin
 * @property to 终点 / Destination
 * @property cost 每请求单位运行费用 / Operating cost per request unit
 * @property capacity 固定运行容量或建设容量包络 / Fixed operating capacity or construction envelope
 * @property capacityLevels 可选建设等级；为空时服务固定启用 / Optional construction levels; empty means always available
 * @property required 是否必须选择建设等级 / Whether a construction level must be selected
 * @property attributes 服务属性 / Service attributes
 */
class AllocationService<V : RealNumber<V>>(
    val id: NetworkArcId,
    val from: NetworkNodeId,
    val to: NetworkNodeId,
    val cost: Quantity<V>,
    val capacity: CapacityBounds<V>,
    capacityLevels: List<AllocationCapacityLevel<V>> = emptyList(),
    val required: Boolean = false,
    attributes: Map<String, Quantity<V>> = emptyMap()
) {
    val capacityLevels = capacityLevels.toList()
    val attributes = attributes.toMap()
}

/** 单个来源至去向的运输请求。 / A shipment request from one origin to one destination.
 * @property id 请求标识，同时用作流商品标识 / Request identity, also used as the flow commodity identity
 * @property origin 起运地点 / Origin location
 * @property destination 目的地点 / Destination location
 * @property quantity 请求数量 / Requested amount
 * @property unit 流量归一化单位 / Normalized flow unit
 * @property integerStep 可选整数流步长；不表示整单不可拆分 / Optional integer-flow step; does not make the whole request indivisible
 * @property allowedServices 可用服务，空值表示全部服务 / Allowed services; null means all services
 * @property costs 按服务覆盖的每单位运行费用 / Per-unit operating cost overrides by service
 */
class AllocationRequest<V : RealNumber<V>>(
    val id: String,
    val origin: NetworkNodeId,
    val destination: NetworkNodeId,
    val quantity: Quantity<V>,
    val unit: PhysicalUnit = quantity.unit,
    val integerStep: Quantity<V>? = null,
    allowedServices: Set<NetworkArcId>? = null,
    costs: Map<NetworkArcId, Quantity<V>> = emptyMap()
) {
    val allowedServices = allowedServices?.toSet()
    val costs = costs.toMap()
}

/** 请求在服务上的资源消耗键。 / Resource-consumption key for a request on a service.
 * @property request 请求标识 / Request identity
 * @property service 服务标识 / Service identity
 */
data class AllocationFlowKey(val request: String, val service: NetworkArcId)

/** 明确的共享配额行。 / Explicit shared quota row.
 * @property id 配额标识 / Quota identity
 * @property bounds 配额容量 / Quota bounds
 * @property usage 每单位请求对配额的消耗 / Quota consumption per request unit
 */
class AllocationQuota<V : RealNumber<V>>(
    val id: String,
    val bounds: CapacityBounds<V>,
    usage: Map<AllocationFlowKey, Quantity<V>>
) {
    val usage = usage.toMap()
}

/** 可选枢纽及其管理的共享配额。 / Optional hub and the shared quotas it controls.
 * @property location 枢纽地点 / Hub location
 * @property openingCost 开设费用 / Opening cost
 * @property required 是否必须开设 / Whether the hub must be opened
 * @property quotaIds 由该枢纽启停控制的配额 / Quotas gated by the hub's activation
 */
class AllocationHub<V : RealNumber<V>>(
    val location: NetworkNodeId,
    val openingCost: Quantity<V>,
    val required: Boolean = false,
    quotaIds: Set<String> = emptySet()
) {
    val quotaIds = quotaIds.toSet()
}

/** 不可变运输分配业务输入。 / Immutable transport-allocation business input.
 * @property locations 地点 / Locations
 * @property services 可选或固定运输服务 / Optional or fixed transport services
 * @property requests 运输请求 / Shipment requests
 * @property quotas 共享配额 / Shared quotas
 * @property hubs 可选枢纽 / Optional hubs
 * @property zero 数值零 / Numeric zero
 * @property costUnit 运行和建设费用的统一单位 / Common unit for operating and construction costs
 * @property serviceUsage 异量纲服务容量的单位请求消耗覆盖 / Per-unit request usage overrides for dimensionally different service capacities
 * @property budget 可选建设预算 / Optional construction budget
 */
class AllocationProblem<V : RealNumber<V>>(
    locations: List<AllocationLocation<V>>,
    services: List<AllocationService<V>>,
    requests: List<AllocationRequest<V>>,
    quotas: List<AllocationQuota<V>> = emptyList(),
    hubs: List<AllocationHub<V>> = emptyList(),
    val zero: V,
    val costUnit: PhysicalUnit = NoneUnit,
    serviceUsage: Map<AllocationFlowKey, Quantity<V>> = emptyMap(),
    val budget: Quantity<V>? = null
) {
    val locations = locations.toList()
    val services = services.toList()
    val requests = requests.toList()
    val quotas = quotas.toList()
    val hubs = hubs.toList()
    val serviceUsage = serviceUsage.toMap()
}

/** 运输分配方案及服务/枢纽建设决策。 / Transport-allocation flows and service/hub construction decisions.
 * @property shipments 各请求在各服务上的运输量 / Quantity shipped for each request-service pair
 * @property flow 底层流费用、资源和供需结果 / Underlying flow cost, resource use, and balance results
 * @property openHubs 已开设枢纽 / Open hubs
 * @property serviceLevels 已选择的服务建设等级 / Selected service construction levels
 * @property constructionCost 建设费用 / Construction cost
 * @property totalCost 运行与建设总费用 / Total operating and construction cost
 */
data class AllocationSolution<V : RealNumber<V>>(
    val shipments: Map<AllocationFlowKey, Quantity<V>>,
    val flow: FlowSolution<V>,
    val openHubs: Set<NetworkNodeId>,
    val serviceLevels: Map<NetworkArcId, String>,
    val constructionCost: Quantity<V>,
    val totalCost: Quantity<V>
)

/** 运输分配到 Flow 与 Network Design 的业务构造和注册边界。 / Business construction and registration boundary over Flow and Network Design.
 * @property problem 业务输入快照 / Business-input snapshot
 * @property design 下层结构决策上下文，供扩展读取建设和流表达式 / Underlying structural context for extending construction and flow expressions
 * @property name 模型命名空间 / Model namespace
 */
class AllocationContext<V : RealNumber<V>> private constructor(
    val problem: AllocationProblem<V>,
    val design: NetworkDesignContext<V>,
    val name: String
) {
    /** 注册运输分配、建设决策及可选费用目标。 / Register allocation, design decisions, and optional cost objective.
     * @param target 元模型 / Meta model
     * @param minimizeCost 是否最小化运行加建设费用 / Whether to minimize operating plus construction cost
     * @return 注册结果 / Registration result
     */
    fun register(target: AbstractLinearMetaModel<Flt64>, minimizeCost: Boolean = true): Try {
        return design.register(target, minimizeCost)
    }

    /** 提取运输分配和建设决策。 / Extract allocation and construction decisions.
     * @param target 已注册并求解的元模型 / Registered and solved meta model
     * @return 业务方案或提取错误 / Business solution or extraction error
     */
    fun extractSolution(target: AbstractLinearMetaModel<Flt64>): Ret<AllocationSolution<V>> {
        return when (val result = design.extractSolution(target)) {
            is Ok -> {
                val inner = result.value
                val shipments = inner.flow.flows.mapKeys { (key, _) -> AllocationFlowKey(key.commodity, key.arc) }
                val constructionCost = inner.constructionCost.convertTo(inner.flow.cost.unit)
                    ?: return networkSchedulingFailure("建设费用无法转换到运行费用单位 / Construction cost cannot be converted to the operating-cost unit")
                val total = inner.flow.cost.value + constructionCost.value
                Ok(AllocationSolution(
                    shipments = shipments,
                    flow = inner.flow,
                    openHubs = inner.openFacilities,
                    serviceLevels = inner.serviceLevels,
                    constructionCost = inner.constructionCost,
                    totalCost = Quantity(total, inner.flow.cost.unit)
                ))
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    companion object {
        /** 校验业务输入并构建独立的底层模型上下文。 / Validate business input and build an independent underlying context.
         * @param problem 业务运输分配问题 / Business transport-allocation problem
         * @param adapter 数值转换边界 / Numeric conversion boundary
         * @param name 模型命名空间 / Model namespace
         * @return 上下文或构造错误 / Context or construction error
         */
        fun <V : RealNumber<V>> create(
            problem: AllocationProblem<V>,
            adapter: NetworkSchedulingSolverValueAdapter<V>,
            name: String = "network_allocation"
        ): Ret<AllocationContext<V>> {
            if (name.isBlank()) {
                return networkSchedulingFailure("分配模型名称不能为空 / Allocation model name cannot be blank")
            }
            val designProblem = when (val result = toNetworkDesignProblem(problem, adapter)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            return ok(AllocationContext(
                problem = problem,
                design = NetworkDesignContext(
                    problem = designProblem,
                    adapter = adapter,
                    name = name
                ),
                name = name
            ))
        }
    }
}

private fun <V : RealNumber<V>> toNetworkDesignProblem(
    problem: AllocationProblem<V>,
    adapter: NetworkSchedulingSolverValueAdapter<V>
): Ret<NetworkDesignProblem<V>> {
    val locationIds = problem.locations.map { it.id }
    val serviceIds = problem.services.map { it.id }
    val requestIds = problem.requests.map { it.id }
    val quotaIds = problem.quotas.map { it.id }
    val hubIds = problem.hubs.map { it.location }
    val controlledQuotaIds = problem.hubs.flatMap { it.quotaIds }
    if (locationIds.any { it.value.isBlank() } || locationIds.toSet().size != locationIds.size ||
        serviceIds.any { it.value.isBlank() } || serviceIds.toSet().size != serviceIds.size ||
        requestIds.any { it.isBlank() } || requestIds.toSet().size != requestIds.size ||
        quotaIds.any { it.isBlank() || it.startsWith("arc:") } || quotaIds.toSet().size != quotaIds.size ||
        hubIds.toSet().size != hubIds.size || controlledQuotaIds.toSet().size != controlledQuotaIds.size) {
        return networkSchedulingFailure("分配输入包含空标识或重复资源 / Allocation input contains a blank identity or duplicate resource")
    }
    when (val zero = adapter.toSolverValue(problem.zero)) {
        is Ok -> if (zero.value != Flt64.zero) {
            return networkSchedulingFailure("分配问题的零值必须可转换为 solver 零 / Allocation zero must convert to solver zero")
        }
        is Failed -> return Failed(zero.error)
        is Fatal -> return Fatal(zero.errors)
    }
    val locations = problem.locations.associateBy { it.id }
    val services = problem.services.associateBy { it.id }
    val requests = problem.requests.associateBy { it.id }
    val quotas = problem.quotas.associateBy { it.id }
    if (problem.services.any { it.from !in locations || it.to !in locations }) {
        return networkSchedulingFailure("服务引用了不存在的地点 / A service references an unknown location")
    }
    for (service in problem.services) {
        val fromLayer = locations[service.from]?.layer
        val toLayer = locations[service.to]?.layer
        if (fromLayer != null && toLayer != null && fromLayer > toLayer) {
            return networkSchedulingFailure("服务方向违反地点层级顺序：${service.id.value} / Service direction violates location layer order: ${service.id.value}")
        }
    }
    if (problem.requests.any {
            it.origin !in locations || it.destination !in locations || it.origin == it.destination ||
                it.allowedServices?.any { id -> id !in services } == true || it.costs.keys.any { id -> id !in services }
        }) {
        return networkSchedulingFailure("请求端点、兼容服务或费用覆盖引用无效 / Invalid request endpoint, compatible service, or cost override")
    }
    for (request in problem.requests) {
        val amount = request.quantity.convertTo(request.unit)
            ?: return networkSchedulingFailure("请求数量单位不兼容：${request.id} / Incompatible request quantity unit: ${request.id}")
        if (!amount.value.isFinite() || amount.value leq problem.zero) {
            return networkSchedulingFailure("请求数量必须为有限正数：${request.id} / Request amount must be finite and positive: ${request.id}")
        }
        request.integerStep?.let { step ->
            val converted = step.convertTo(request.unit)
                ?: return networkSchedulingFailure("整数流步长单位不兼容：${request.id} / Incompatible integer-flow step unit: ${request.id}")
            if (!converted.value.isFinite() || converted.value leq problem.zero) {
                return networkSchedulingFailure("整数流步长必须为有限正数：${request.id} / Integer-flow step must be finite and positive: ${request.id}")
            }
        }
    }
    if (problem.serviceUsage.keys.any {
            it.request !in requests || it.service !in services ||
                requests[it.request]?.allowedServices?.contains(it.service) == false
        }) {
        return networkSchedulingFailure("服务容量消耗引用了不存在的请求或服务 / Service-capacity usage references an unknown request or service")
    }
    if (problem.quotas.any { quota -> quota.usage.keys.any {
            it.request !in requests || it.service !in services ||
                requests[it.request]?.allowedServices?.contains(it.service) == false
        } }) {
        return networkSchedulingFailure("共享配额引用了不存在的请求或服务 / A shared quota references an unknown request or service")
    }
    if (problem.hubs.any { it.location !in locations || it.quotaIds.any { id -> id !in quotas } }) {
        return networkSchedulingFailure("枢纽引用了不存在的地点或共享配额 / A hub references an unknown location or shared quota")
    }

    val nodes = mutableListOf<NetworkNode<V>>()
    for (location in problem.locations) {
        when (val result = NetworkNode(location.id, location.attributes)) {
            is Ok -> nodes.add(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    val arcs = mutableListOf<NetworkArc<V>>()
    val flowArcs = mutableListOf<FlowArc<V>>()
    for (service in problem.services) {
        val arc = when (val result = NetworkArc(
            from = service.from,
            to = service.to,
            cost = service.cost,
            attributes = service.attributes,
            id = service.id
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        arcs.add(arc)
        when (val result = FlowArc(arc, service.capacity)) {
            is Ok -> flowArcs.add(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    val graph = when (val result = NetworkGraph(nodes, arcs)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val commodities = mutableListOf<CommodityFlow<V>>()
    val negativeOne = when (val result = adapter.fromSolverValue(-Flt64.one)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    for (request in problem.requests) {
        val amount = request.quantity.convertTo(request.unit)
            ?: return networkSchedulingFailure("请求数量单位转换失败：${request.id} / Failed to convert request quantity: ${request.id}")
        val supply = when (val result = SupplyDemand(request.origin, Quantity(amount.value, request.unit))) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val demand = when (val result = SupplyDemand(request.destination, Quantity(amount.value * negativeOne, request.unit))) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val commodity = when (val result = FlowCommodity(request.id, listOf(supply, demand))) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        commodities.add(CommodityFlow(
            commodity = commodity,
            unit = request.unit,
            integerStep = request.integerStep,
            costs = request.costs,
            allowedArcs = request.allowedServices
        ))
    }
    val resources = problem.quotas.map { quota ->
        FlowCapacity(
            id = quota.id,
            bounds = quota.bounds,
            usage = quota.usage.mapKeys { (key, _) -> FlowKey(key.request, key.service) }
        )
    }
    val capacityUsage = problem.serviceUsage.mapKeys { (key, _) -> FlowKey(key.request, key.service) }
    val flowProblem = FlowProblem(
        graph = graph,
        arcs = flowArcs,
        commodities = commodities,
        zero = problem.zero,
        costUnit = problem.costUnit,
        capacityUsage = capacityUsage,
        resources = resources
    )
    val designServices = problem.services.filter { it.capacityLevels.isNotEmpty() }.map { service ->
        NetworkService(
            arc = service.id,
            levels = service.capacityLevels.map { level -> CapacityLevel(level.id, level.capacity, level.cost) },
            required = service.required
        )
    }
    val facilities = problem.hubs.map { hub ->
        NetworkFacility(
            node = hub.location,
            cost = hub.openingCost,
            required = hub.required,
            capacityRows = hub.quotaIds
        )
    }
    return ok(NetworkDesignProblem(
        flow = flowProblem,
        services = designServices,
        facilities = facilities,
        budget = problem.budget
    ))
}
