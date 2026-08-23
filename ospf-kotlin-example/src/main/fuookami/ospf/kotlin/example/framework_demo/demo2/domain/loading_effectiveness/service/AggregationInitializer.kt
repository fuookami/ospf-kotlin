package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Initializes the loading effectiveness aggregation from aircraft and stowage contexts.
 * 从飞机和装载上下文初始化装载效能聚合。
*/
data object AggregationInitializer {

    /**
     * Creates a loading effectiveness aggregation from aircraft and stowage aggregations.
     * 从飞行器和配载聚合创建装车效能聚合。
     *
     * @param aircraftAggregation 飞行器域聚合 / The aircraft domain aggregation.
     * @param stowageAggregation 配载域聚合 / The stowage domain aggregation.
     * @param input 包含输入参数的请求 DTO / The request DTO containing input parameters.
     * @param stowageMode 当前配载模式 / The current stowage mode.
     * @return 装车效能聚合或错误 / The loading effectiveness aggregation, or an error.
    */
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        input: RequestDTO,
        stowageMode: StowageMode
    ): Ret<Aggregation> {
        if (input.trailers.any { it.name.isBlank() }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "拖车名称不能为空 / Trailer names must not be blank"
            )
        }
        if (input.trailers.map { it.name }.distinct().size != input.trailers.size) {
            return Failed(
                ErrorCode.IllegalArgument,
                "拖车名称必须唯一 / Trailer names must be unique"
            )
        }
        if (input.trailers.any { it.order !in 0..UByte.MAX_VALUE.toInt() }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "拖车顺序必须在 0 到 255 之间 / Trailer order must be between 0 and 255"
            )
        }
        if (input.trailers.any { trailer ->
                trailer.items.size != trailer.items.distinct().size
            }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "同一拖车的货物不能重复 / Trailer item names must be unique within a trailer"
            )
        }
        if (input.trailers.flatMap { it.items }.size != input.trailers.flatMap { it.items }.distinct().size) {
            return Failed(
                ErrorCode.IllegalArgument,
                "每件货物最多只能属于一辆拖车 / Each cargo item may belong to at most one trailer"
            )
        }

        val itemsByName = stowageAggregation.items.associateBy { it.id }
        val trailers = input.trailers.map { trailer ->
            val items = trailer.items.map { itemName ->
                itemsByName[itemName] ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "拖车引用了未知货物 $itemName / Trailer references unknown cargo $itemName"
                )
            }
            Trailer(
                type = when (trailer.type) {
                    TrailerTypeInput.Hardstand -> TrailerType.Hardstand
                    TrailerTypeInput.Transit -> TrailerType.Transit
                    TrailerTypeInput.Warehouse -> TrailerType.Warehouse
                },
                order = UInt8(trailer.order.toUByte()),
                name = trailer.name,
                items = items
            )
        }

        return Ok(
            Aggregation(
                aircraftModel = aircraftAggregation.aircraftModel,
                stowageMode = stowageMode,
                flight = stowageAggregation.flight,
                items = stowageAggregation.items,
                positions = stowageAggregation.positions,
                trailers = trailers,
                stowage = stowageAggregation.stowage,
                load = stowageAggregation.load,
                cargosBySource = input.cargos
                    .mapIndexed { index, cargo -> cargo.source to index }
                    .groupBy({ it.first }, { it.second }),
                earlyEnd = (stowageAggregation.positions.size - 1).coerceAtLeast(0) / 2,
                bigM = stowageAggregation.positions.size.toDouble()
            )
        )
    }
}
