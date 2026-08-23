package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Exports the computed loading order as a response DTO.
 * 将计算的装载顺序导出为响应 DTO。
 *
 * @property aggregation 要导出的飞机聚合数据 / The aircraft aggregation data to export from.
*/
data class LoadingOrderOutputExporter(
    private val aggregation: Aggregation
) {
    operator fun invoke(
        input: RequestDTO
    ): Ret<LoadingOrderResponseDTO> {
        val orders = aggregation.decks
            .flatMap { deck ->
                deck.positions.map { position -> deck.location to position }
            }
            .sortedWith(
                compareBy<Pair<DeckLocation, Position>>(
                    { it.first.ordinal },
                    { it.second.loadingOrder.order.toString().toInt() },
                    { it.second.id.toString() }
                )
            )
            .map { (_, position) ->
                "${position.id}: ${position.spaceName} (order=${position.loadingOrder.order})"
            }

        return Ok(LoadingOrderResponseDTO.success(orders))
    }
}
