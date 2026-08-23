package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.service

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Initializes the soft security aggregation from aircraft and stowage contexts.
 * 从飞机和装载上下文初始化软安全聚合。
*/
data object AggregationInitializer {
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        input: RequestDTO
    ): Ret<Aggregation> {
        val mainDeck = aircraftAggregation.decks.find { it.location == DeckLocation.Main }
            ?: return Failed(
                ErrorCode.IllegalArgument,
                "软安全聚合需要主舱甲板 / Soft security aggregation requires a main-deck configuration"
            )
        if (stowageAggregation.positions.isEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "软安全聚合至少需要一个装载位置 / Soft security aggregation requires at least one stowage position"
            )
        }

        return Ok(Aggregation(
            aircraftModel = aircraftAggregation.aircraftModel,
            mainDeck = mainDeck,
            items = stowageAggregation.items,
            positions = stowageAggregation.positions,
            stowage = stowageAggregation.stowage,
            load = stowageAggregation.load,
            ballast = stowageAggregation.ballast
        ))
    }
}
