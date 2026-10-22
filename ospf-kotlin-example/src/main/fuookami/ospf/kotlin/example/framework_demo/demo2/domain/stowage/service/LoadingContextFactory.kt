package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.geometry2d.Box2Geometry
import fuookami.ospf.kotlin.framework.bpp.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.DiscreteLoadingContext
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.*
import fuookami.ospf.kotlin.framework.bpp.domain.partition_loading.*
import fuookami.ospf.kotlin.framework.bpp1d.domain.discrete_loading.Bpp1dDiscreteLoading
import fuookami.ospf.kotlin.framework.bpp2d.domain.discrete_loading.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 严格 DTO 到框架的适配边界。 / Strict DTO-to-framework boundary. */
internal data object LoadingContextFactory {
    /** 显式分区和航空局部重量变量归属。 / Explicit partition and aviation-local weight variable ownership.
     * @param input 请求 / Request
     * @param aggregation 已注册的航空领域 / Registered aviation domain
     * @return 分区和附加决策 / Partitions and extra decisions
     */
    fun partitionConfiguration(input: RequestDTO, aggregation: Aggregation): Pair<List<LoadingPartition>, List<PartitionDecision>> {
        val partitions = input.positions.groupBy { it.partitionId }.map { (id, positions) -> LoadingPartition(id, positions.map { it.name }.toSet()) }
        val owners = input.positions.associate { it.name to it.partitionId }
        val extras = aggregation.positions.flatMapIndexed { index, position ->
            val local = arrayListOf<PartitionDecision>()
            if (position.status.predicateWeightNeeded) local.add(PartitionDecision("estimated:${position.spaceName}", owners.getValue(position.spaceName), aggregation.load.y[index].value))
            if (position.status.recommendedWeightNeeded) local.add(PartitionDecision("recommended:${position.spaceName}", owners.getValue(position.spaceName), aggregation.load.z[index].value))
            local
        }
        return partitions to extras
    }

    /** 构造几何内核，显式互斥只能追加。 / Build geometry; explicit conflicts can only add restrictions.
     * @param input 显式请求 / Explicit request
     * @param aggregation 航空领域 / Aviation domain
     * @return 对应维度的共享上下文 / Shared context for the selected dimension
     */
    fun build(input: RequestDTO, aggregation: Aggregation): Ret<DiscreteLoadingContext<Flt64>> {
        val positions = aggregation.positions.associateBy { it.spaceName }
        val names = positions.keys
        if (input.cargos.any { it.allowedPositions?.any { id -> id !in names } == true }) return Failed(ErrorCode.IllegalArgument, "允许舱位引用未知名称 / Unknown allowed position")
        val fixed = input.positions.flatMap { p -> p.loadedItems.map { it to p.name } }
        if (fixed.map { it.first }.distinct().size != fixed.size) return Failed(ErrorCode.IllegalArgument, "货物不能固定到多个舱位 / Cargo cannot be fixed to multiple positions")
        val fixedById = fixed.toMap()
        val cargos = input.cargos.associateBy { it.name }
        val spaces = input.spaces.associateBy { it.id }
        val positionDecks = input.positions.associate { it.name to spaces.getValue(it.spaceId).location }
        val problem = DiscreteLoadingProblem(
            items = aggregation.items.map { item -> LoadingItem(
                id = item.id,
                weight = item.weight,
                required = item.status != ItemStatus.Optional && item.status != ItemStatus.Reserved,
                fixedPositionId = fixedById[item.id]
            ) },
            positions = input.positions.map { p -> LoadingPosition(
                id = p.name,
                maxCount = UInt64(p.maxLoadCount.toULong()),
                maxWeight = Quantity(Flt64(p.maxWeight), input.weightUnit.unit),
                fixedOccupied = p.loadedItems.isNotEmpty(),
                enabled = positions.getValue(p.name).status.available || p.loadedItems.isNotEmpty()
            ) },
            allowedAssignments = aggregation.items.flatMap { item ->
                aggregation.positions.filter { position ->
                    (cargos.getValue(item.id).allowedPositions?.contains(position.spaceName) != false) &&
                        cargos.getValue(item.id).deckLocation == positionDecks.getValue(position.spaceName) &&
                        (Stowage.stowageNeeded(item, position) || Stowage.adjustmentNeeded(item, position) || item in position.loadedItems)
                }.map { AssignmentKey(item.id, it.spaceName) }
            }.toSet(),
            conflicts = input.positionConflicts.map { PositionConflict(it.first, it.second) }.toSet(),
            layoutGroups = input.layoutGroups.map { group -> LayoutSelectionGroup(
                id = group.id,
                positionIds = group.positions,
                layouts = group.layouts.map { LoadingLayout(it.id, it.positions) }
            ) },
            weightUnit = aggregation.aircraftModel.weightUnit
        )
        fun length(v: Double) = Quantity(Flt64(v), input.lengthUnit.unit)
        return if (input.aircraftType == AircraftTypeInput.B737 || input.aircraftType == AircraftTypeInput.B757) {
            Bpp1dDiscreteLoading.context(
                problem = problem,
                geometry = input.positions.map { p -> LoadingInterval(
                    id = p.name, spaceId = p.spaceId, start = length(p.geometry.front), end = length(p.geometry.back)
                ) },
                adapter = Flt64PackingValueAdapter
            )
        } else {
            Bpp2dDiscreteLoading.context(
                problem = problem,
                geometry = input.positions.map { p -> LoadingRectangle(
                    id = p.name,
                    spaceId = p.spaceId,
                    bounds = Box2Geometry.of(
                        x = length(p.geometry.front), y = length(p.geometry.left),
                        width = length(p.geometry.back - p.geometry.front), height = length(p.geometry.right - p.geometry.left)
                    )
                ) },
                adapter = Flt64PackingValueAdapter
            )
        }
    }
}
