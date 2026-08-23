@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Initializer that creates a stowage Aggregation from the aircraft aggregation and request input.
 * 根据飞机聚合和请求输入创建配载聚合的初始化器。
*/
data object AggregationInitializer {
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        input: RequestDTO,
        stowageMode: StowageMode
    ): Ret<Aggregation> {
        if (input.cargos.isEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "配载请求至少需要一件货物 / Stowage request requires at least one cargo"
            )
        }
        if (input.cargos.any { it.name.isBlank() || it.source.isBlank() || it.destination.isBlank() }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "货物名称、来源和目的地不能为空 / Cargo name, source, and destination must not be blank"
            )
        }
        if (input.cargos.map { it.name }.distinct().size != input.cargos.size) {
            return Failed(
                ErrorCode.IllegalArgument,
                "货物名称必须唯一 / Cargo names must be unique"
            )
        }
        if (input.cargos.any { !it.weight.isFinite() || it.weight <= 0.0 || it.priority < 0 }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "货物重量必须为正且有限，优先级不得为负 / Cargo weight must be positive and finite, and priority must not be negative"
            )
        }
        if (input.cargos.any { it.order != null && it.order !in 0..UByte.MAX_VALUE.toInt() }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "货物顺序必须在 0 到 255 之间 / Cargo order must be between 0 and 255"
            )
        }
        if (!input.payloadUpperBound.isFinite() || input.payloadUpperBound <= 0.0) {
            return Failed(
                ErrorCode.IllegalArgument,
                "最大业载必须为正且有限 / Maximum payload must be positive and finite"
            )
        }
        if (aircraftAggregation.positions.size != input.positions.size) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "飞机舱位与请求舱位数量不一致 / Aircraft positions do not match request positions"
            )
        }
        if (input.positions.any { it.maxLoadCount <= 0 }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "舱位最大装载件数必须为正 / Position maximum load count must be positive"
            )
        }
        if (input.positions.any { position ->
                position.loadedItems.size != position.loadedItems.distinct().size
            }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "同一舱位的已装货物不能重复 / Loaded item names must be unique within a position"
            )
        }
        if (input.positions.any { position ->
                position.loadedItems.size > position.maxLoadCount
            }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "舱位已装货物数量超过最大件数 / Loaded item count exceeds the position maximum"
            )
        }
        val cargoNames = input.cargos.map(CargoInput::name).toSet()
        if (input.positions.any { position ->
                position.loadedItems.any { it !in cargoNames }
            }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "舱位引用了未知货物 / A position references an unknown cargo item"
            )
        }
        if (input.positions.any { position ->
                position.ala != null && (position.ala < 0 || position.ala > position.maxLoadCount)
            }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "实际装载件数超出舱位范围 / Actual loaded item count is outside the position range"
            )
        }
        if (input.positions.any { position ->
                position.alw != null &&
                    (!position.alw.isFinite() || position.alw < 0.0 || position.alw > position.maxWeight)
            }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "实际装载重量超出舱位范围 / Actual loaded weight is outside the position range"
            )
        }
        if (input.positions.any { position ->
                position.predicateLoadWeightMin != null &&
                    (!position.predicateLoadWeightMin.isFinite() ||
                        position.predicateLoadWeightMin < 0.0 ||
                        position.predicateLoadWeightMin > position.maxWeight)
            }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "预配载最小重量必须在舱位载重范围内 / Predicate minimum weight must be within the position capacity"
            )
        }
        if (stowageMode == StowageMode.Predistribution && input.positions.any { it.predicateLoadWeightMin == null }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "预配载模式必须提供每个舱位的最小预测重量 / Predistribution mode requires a predicate minimum weight for every position"
            )
        }

        val aircraftModel = aircraftAggregation.aircraftModel
        val items = input.cargos.map { cargo ->
            Item(
                id = cargo.name,
                destination = IATA(cargo.destination),
                source = FlightNo(cargo.source),
                uld = null,
                weight = Quantity(Flt64(cargo.weight), aircraftModel.weightUnit),
                location = ItemLocation.normalMain,
                cargo = ItemCargo(
                    types = setOf(CargoType("GENERAL")),
                    priority = CargoPriority(
                        name = "P${cargo.priority}",
                        priority = UInt64(cargo.priority.toULong()),
                        category = when {
                            cargo.priority >= 8 -> CargoPriorityCategory.High
                            cargo.priority <= 2 -> CargoPriorityCategory.Low
                            else -> CargoPriorityCategory.Normal
                        }
                    )
                ),
                status = ItemStatus.Optional,
                order = cargo.order?.let {
                    ItemOrder(
                        hardstand = null,
                        reweighed = null,
                        carBoard = null,
                        order = UInt8(it.toUByte())
                    )
                }
            )
        }
        val itemsByName = items.associateBy { it.id }
        val loadedItemsByPosition = input.positions.map { position ->
            position.loadedItems.map { name ->
                itemsByName[name] ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "舱位引用了未知货物 $name / Position references unknown cargo $name"
                )
            }.toSet()
        }
        val positions = aircraftAggregation.positions.mapIndexed { index, basePosition ->
            val inputPosition = input.positions[index]
            val status = PositionStatus(
                location = basePosition.location,
                code = PositionStatusCode.Unloaded,
                stowageMode = stowageMode
            )
            val maximumLoadWeight = Quantity(Flt64(inputPosition.maxWeight), aircraftModel.weightUnit)
            Position(
                base = basePosition,
                loadedItems = loadedItemsByPosition[index],
                ala = inputPosition.ala?.let { UInt64(it.toULong()) },
                mla = if (status.predicateWeightNeeded) UInt64.one else UInt64(inputPosition.maxLoadCount.toULong()),
                alw = inputPosition.alw?.let { Quantity(Flt64(it), aircraftModel.weightUnit) },
                mlw = PositionMaximumLoadWeight(
                    aircraftModel = aircraftModel,
                    position = basePosition,
                    mlw = maximumLoadWeight,
                    mzfw = maximumLoadWeight
                ),
                plw = if (status.predicateWeightNeeded) {
                    PositionPredicateLoadWeight(
                        plw = Quantity(Flt64.zero, aircraftModel.weightUnit),
                        min = Quantity(
                            Flt64(inputPosition.predicateLoadWeightMin ?: 0.0),
                            aircraftModel.weightUnit
                        ),
                        max = maximumLoadWeight
                    )
                } else {
                    null
                },
                type = PositionType(emptySet()),
                taboo = PositionStowageTaboo(emptyList()),
                status = status
            )
        }
        val plannedPayload = Quantity(
            items.fold(Flt64.zero) { total, item ->
                total + item.weight.to(aircraftModel.weightUnit)!!.value
            },
            aircraftModel.weightUnit
        )

        return Ok(Aggregation(
            aircraftModel = aircraftModel,
            formula = aircraftAggregation.formula,
            fuselage = aircraftAggregation.fuselage,
            fuel = aircraftAggregation.fuel,
            flight = Flight(
                leg = IATA("DEP-ARR"),
                now = kotlin.time.Instant.fromEpochMilliseconds(0),
                etd = kotlin.time.Instant.fromEpochMilliseconds(0)
            ),
            items = items,
            positions = positions,
            plannedPayload = plannedPayload,
            maxPayload = Quantity(Flt64(input.payloadUpperBound), aircraftModel.weightUnit),
            computedPayload = null,
            maxTotalWeight = emptyMap(),
            computedTotalWeight = emptyMap(),
            withMultiLoadingSchema = true,
            appointment = Appointment(emptyMap()),
            biologicalLimit = BiologicalLimit(emptyList(), emptyList(), emptyList()),
            neighbours = HashMap(aircraftAggregation.neighbours)
        ))
    }
}
