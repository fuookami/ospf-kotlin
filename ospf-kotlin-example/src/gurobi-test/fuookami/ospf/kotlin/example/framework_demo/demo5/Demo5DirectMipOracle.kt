@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo5

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.service.BranchAndPriceAlgorithm

/** demo5 的两指标 VRPTW 直接 MIP oracle。 / Two-index VRPTW direct-MIP oracle for demo5. */
internal object Demo5DirectMipOracle {
    /** 直接 MIP 的求解结果。 / Direct-MIP solve result. */
    data class Result(
        val objective: Flt64,
        val bestBound: Flt64,
        val gap: Flt64,
        val customerSequences: List<List<Customer<Flt64>>>
    ) {
        val optimal: Boolean get() = gap.leq(Flt64(1e-6))
    }

    /**
     * 使用与 B&P 相同的实例和成本策略求解直接 MIP。 / Solve the direct MIP with the same instance and cost policies as B&P.
     */
    suspend fun solve(
        instance: VrptwInstance<Flt64>,
        policy: BranchAndPriceAlgorithm.Policy<Flt64>,
        solver: ColumnGenerationSolver
    ): Ret<Result> {
        if (instance.vehicleTypes.size != 1) {
            return networkSchedulingFailure(
                "direct MIP 目前要求单一车辆类型 / Direct MIP currently requires one vehicle type"
            )
        }

        val vehicleType = instance.vehicleTypes.single()
        val nodes = listOf(instance.startDepot.node) +
            instance.customers.map { it.node } +
            instance.endDepot.node
        val startIndex = 0
        val endIndex = nodes.lastIndex
        val customerIndices = 1 until endIndex
        val enabledArcs = mutableListOf<Pair<Int, Int>>()
        val arcCosts = mutableMapOf<Pair<Int, Int>, Flt64>()
        val travelTimes = mutableMapOf<Pair<Int, Int>, Flt64>()

        for (from in 0 until nodes.size) {
            for (to in 0 until nodes.size) {
                if (from == endIndex || to == startIndex || from == to ||
                    (from == startIndex && to == endIndex)
                ) {
                    continue
                }
                val distance = policy.distanceCalculator.distance(nodes[from], nodes[to]).value
                    ?: return oracleFailure("弧 $from->$to 距离计算失败", "distance failed for arc $from->$to")
                val travelTime = policy.travelTimeCalculator.travelTime(
                    from = nodes[from],
                    to = nodes[to],
                    vehicleType = vehicleType
                ).value ?: return oracleFailure(
                    "弧 $from->$to 行驶时间计算失败",
                    "travel time failed for arc $from->$to"
                )
                val arcCost = policy.arcCostCalculator.cost(
                    from = nodes[from],
                    to = nodes[to],
                    distance = distance,
                    travelTime = travelTime,
                    vehicleType = vehicleType
                ).value ?: return oracleFailure(
                    "弧 $from->$to 成本计算失败",
                    "cost failed for arc $from->$to"
                )
                val solverCost = policy.valueAdapter.normalize(arcCost, instance.units.costUnit).value
                    ?: return oracleFailure(
                        "弧 $from->$to 成本单位转换失败",
                        "cost conversion failed for arc $from->$to"
                    )
                enabledArcs.add(from to to)
                arcCosts[from to to] = solverCost
                travelTimes[from to to] = instance.schedulingWindow.valueOf(travelTime)
            }
        }

        val converter: IntoValue<Flt64> = IntoValue.fromConverter(Flt64)
        val model = LinearMetaModel<Flt64>(
            name = "demo5_25_customer_direct_mip",
            converter = converter
        )
        val x = BinVariable2("x", Shape2(nodes.size, nodes.size))
        val enabledArcSet = enabledArcs.toSet()
        for (from in 0 until nodes.size) {
            for (to in 0 until nodes.size) {
                val variable = x[from, to]
                variable.name = "x_${from}_$to"
                if ((from to to) in enabledArcSet) {
                    model.add(variable)
                } else {
                    variable.range.eq(false)
                }
            }
        }

        val serviceStart = URealVariable1("service_start", Shape1(nodes.size))
        for (index in nodes.indices) {
            serviceStart[index].name = "service_start_$index"
            model.add(serviceStart[index])
        }
        val load = URealVariable1("load", Shape1(instance.customers.size))
        for (index in instance.customers.indices) {
            load[index].name = "load_${index + 1}"
            model.add(load[index])
        }

        val fixedCost = policy.valueAdapter.normalize(
            quantity = vehicleType.fixedCost,
            targetUnit = instance.units.costUnit
        ).value ?: return oracleFailure("固定成本单位转换失败", "fixed-cost conversion failed")
        val startOutgoing = sum(enabledArcs.filter { it.first == startIndex }.map { (from, to) ->
            LinearPolynomial(x[from, to])
        })
        val endIncoming = sum(enabledArcs.filter { it.second == endIndex }.map { (from, to) ->
            LinearPolynomial(x[from, to])
        })
        model.minimize(
            polynomial = fixedCost * startOutgoing +
                sum(enabledArcs.map { arc ->
                    arcCosts.getValue(arc) * LinearPolynomial(x[arc.first, arc.second])
                }),
            name = "direct_mip_cost"
        )

        for (customerIndex in customerIndices) {
            model.addConstraint(
                sum(enabledArcs.filter { it.first == customerIndex }.map { (from, to) ->
                    LinearPolynomial(x[from, to])
                }) eq Flt64.one,
                name = "customer_${customerIndex}_out"
            )
            model.addConstraint(
                sum(enabledArcs.filter { it.second == customerIndex }.map { (from, to) ->
                    LinearPolynomial(x[from, to])
                }) eq Flt64.one,
                name = "customer_${customerIndex}_in"
            )
        }
        model.addConstraint(
            startOutgoing - endIncoming eq Flt64.zero,
            name = "depot_flow_balance"
        )
        model.addConstraint(
            startOutgoing leq Flt64(vehicleType.amount.toDouble()),
            name = "fleet_size"
        )

        val readyTimes = nodes.indices.map { index -> readyTime(instance, index) }
        val dueTimes = nodes.indices.map { index -> dueTime(instance, index) }
        val serviceTimes = nodes.indices.map { index -> serviceTime(instance, index) }
        val horizon = dueTimes.maxOrNull()!! - readyTimes.minOrNull()!!
        val maximumTravelTime = travelTimes.values.maxOrNull() ?: Flt64.zero
        val maximumServiceTime = serviceTimes.maxOrNull() ?: Flt64.zero
        val timeBigM = horizon + maximumTravelTime + maximumServiceTime

        for (index in nodes.indices) {
            model.addConstraint(
                LinearPolynomial(serviceStart[index]) geq readyTimes[index],
                name = "time_window_${index}_lb"
            )
            model.addConstraint(
                LinearPolynomial(serviceStart[index]) leq dueTimes[index],
                name = "time_window_${index}_ub"
            )
        }
        for ((from, to) in enabledArcs) {
            model.addConstraint(
                LinearPolynomial(serviceStart[from]) + serviceTimes[from] + travelTimes.getValue(from to to) -
                    LinearPolynomial(serviceStart[to]) leq
                    timeBigM * (LinearPolynomial(Flt64.one) - LinearPolynomial(x[from, to])),
                name = "time_propagation_${from}_$to"
            )
        }

        val capacity = policy.valueAdapter.normalize(
            quantity = vehicleType.capacity,
            targetUnit = instance.units.loadUnit
        ).value ?: return oracleFailure("车辆容量单位转换失败", "vehicle-capacity conversion failed")
        val demands = instance.customers.mapIndexed { index, customer ->
            policy.valueAdapter.normalize(customer.demand, instance.units.loadUnit).value
                ?: return oracleFailure(
                    "客户 ${index + 1} 需求单位转换失败",
                    "demand conversion failed for customer ${index + 1}"
                )
        }
        for (customerIndex in customerIndices) {
            val loadIndex = customerIndex - 1
            model.addConstraint(
                LinearPolynomial(load[loadIndex]) geq demands[loadIndex],
                name = "load_${customerIndex}_lb"
            )
            model.addConstraint(
                LinearPolynomial(load[loadIndex]) leq capacity,
                name = "load_${customerIndex}_ub"
            )
            model.addConstraint(
                LinearPolynomial(load[loadIndex]) geq demands[loadIndex] -
                    capacity * (
                        LinearPolynomial(Flt64.one) - LinearPolynomial(x[startIndex, customerIndex])
                    ),
                name = "load_from_depot_$customerIndex"
            )
        }
        for (from in customerIndices) {
            for (to in customerIndices) {
                if (from == to) continue
                model.addConstraint(
                    LinearPolynomial(load[to - 1]) geq LinearPolynomial(load[from - 1]) + demands[to - 1] -
                        capacity * (LinearPolynomial(Flt64.one) - LinearPolynomial(x[from, to])),
                    name = "load_propagation_${from}_$to"
                )
            }
        }

        val output = when (val result = solver.solveMILP(
            name = "demo5_25_customer_direct_mip",
            metaModel = model
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        model.setSolution(output.solution)
        val sequences = extractCustomerSequences(
            instance = instance,
            model = model,
            x = x,
            startIndex = startIndex,
            endIndex = endIndex
        ).value ?: return oracleFailure("直接 MIP 路线提取失败", "direct-MIP route extraction failed")
        return ok(Result(
            objective = output.obj,
            bestBound = output.bestBound ?: output.possibleBestObj,
            gap = output.mipGap,
            customerSequences = sequences
        ))
    }

    private fun readyTime(instance: VrptwInstance<Flt64>, index: Int): Flt64 {
        val instant = when (index) {
            0 -> instance.startDepot.timeWindow.readyTime
            instance.customers.size + 1 -> instance.endDepot.timeWindow.readyTime
            else -> instance.customers[index - 1].timeWindow.readyTime
        }
        return instance.schedulingWindow.valueOf(instant)
    }

    private fun dueTime(instance: VrptwInstance<Flt64>, index: Int): Flt64 {
        val instant = when (index) {
            0 -> instance.startDepot.timeWindow.dueTime
            instance.customers.size + 1 -> instance.endDepot.timeWindow.dueTime
            else -> instance.customers[index - 1].timeWindow.dueTime
        }
        return instance.schedulingWindow.valueOf(instant)
    }

    private fun serviceTime(instance: VrptwInstance<Flt64>, index: Int): Flt64 {
        return if (index in 1..instance.customers.size) {
            instance.schedulingWindow.valueOf(instance.customers[index - 1].serviceTime)
        } else {
            Flt64.zero
        }
    }

    private fun extractCustomerSequences(
        instance: VrptwInstance<Flt64>,
        model: LinearMetaModel<Flt64>,
        x: BinVariable2,
        startIndex: Int,
        endIndex: Int
    ): Ret<List<List<Customer<Flt64>>>> {
        val selectedArcs = model.tokens.tokens.mapNotNull { token ->
            if (token.variable.belongsTo(x) && token.result?.geq(Flt64(0.5)) == true) {
                token.variable.vectorView.let { it[0] to it[1] }
            } else {
                null
            }
        }
        val nextByCustomer = selectedArcs
            .filter { it.first != startIndex }
            .associate { it.first to it.second }
        val sequences = mutableListOf<List<Customer<Flt64>>>()
        for ((_, firstCustomer) in selectedArcs.filter { it.first == startIndex }) {
            val sequence = mutableListOf<Customer<Flt64>>()
            val visited = mutableSetOf<Int>()
            var current = firstCustomer
            while (current != endIndex) {
                if (current !in 1..instance.customers.size || !visited.add(current)) {
                    return networkSchedulingFailure(
                        "直接 MIP 路线包含非法节点或子环 / Direct-MIP route contains an invalid node or subtour"
                    )
                }
                sequence.add(instance.customers[current - 1])
                current = nextByCustomer[current]
                    ?: return networkSchedulingFailure(
                        "直接 MIP 路线在客户 $current 后中断 / Direct-MIP route stops after customer $current"
                    )
            }
            sequences.add(sequence)
        }
        if (sequences.flatten().map { it.id }.toSet() != instance.customers.map { it.id }.toSet()) {
            return networkSchedulingFailure(
                "直接 MIP 路线未完整覆盖客户 / Direct-MIP routes do not cover all customers"
            )
        }
        return ok(sequences)
    }

    private fun <T> oracleFailure(chinese: String, english: String): Ret<T> {
        return networkSchedulingFailure(
            "构建 direct MIP oracle 失败：$chinese / Failed to build direct-MIP oracle: $english"
        )
    }
}
