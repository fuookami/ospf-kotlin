package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Generates the pipeline of loading effectiveness constraints based on stowage mode and parameters.
 * 基于装载模式和参数生成装车效能约束的管线。
 *
 * @property aggregation The loading effectiveness aggregation containing domain models. / 包含域模型的装车效能聚合
*/
class PipelineListGenerator(
    private val aggregation: Aggregation
) {

    /**
     * Generates the list of constraint pipelines based on stowage mode and parameters.
     * 基于装载模式和参数生成约束管线列表。
     *
     * @param stowageMode The stowage mode determining which pipelines to include. / 决定包含哪些管线的装载模式
     * @param parameter The parameter configuration for pipeline generation. / 管线生成的参数配置
     * @return The list of constraint pipelines, or an error. / 约束管线列表或错误
    */
    operator fun invoke(
        stowageMode: StowageMode,
        parameter: Parameter
    ): Ret<PipelineList<AbstractLinearMetaModel<Flt64>>> {
        val pipelines = ArrayList<Pipeline<AbstractLinearMetaModel<Flt64>>>()

        when (stowageMode) {
            StowageMode.Predistribution -> {
                pipelines.add(
                    ItemAheadLoadLimit(
                        items = aggregation.items,
                        stowage = aggregation.stowage,
                        coefficient = {
                            TODO("not implemented yet")
                        }
                    )
                )

                pipelines.add(
                    ItemReserveLimit(
                        items = aggregation.items,
                        stowage = aggregation.stowage,
                        coefficient = {
                            TODO("not implemented yet")
                        }
                    )
                )
            }

            StowageMode.FullLoad, StowageMode.WeightRecommendation -> {}
        }

        if (aggregation.flight.reweighNeeded(stowageMode)) {
            pipelines.add(
                ItemReweighNeededLimit(
                    items = aggregation.items,
                    stowage = aggregation.stowage,
                    coefficient = {
                        TODO("not implemented yet")
                    }
                )
            )
        }

        pipelines.add(
            SameSourceAdjacentLimit(
                adjacentPositions = aggregation.adjacentPositions,
                sources = aggregation.sources,
                loading = aggregation.transferAdjacentLoading,
                coefficient = { _, _, _ ->
                    TODO("not implemented yet")
                }
            )
        )

        pipelines.add(
            SameDestinationAdjacent(
                adjacentPositions = aggregation.adjacentPositions,
                destinations = aggregation.destinations,
                loading = aggregation.transferAdjacentLoading,
                coefficient = { _, _, _ ->
                    TODO("not implemented yet")
                }
            )
        )

        if (aggregation.adviceLoading != null) {
            pipelines.add(
                AdviceLoadAmountLimit(
                    positions = aggregation.positions,
                    loading = aggregation.adviceLoading,
                    coefficient = {
                        TODO("not implemented yet")
                    }
                )
            )

            pipelines.add(
                AdviceLoadWeightLimit(
                    positions = aggregation.positions,
                    loading = aggregation.adviceLoading,
                    coefficient = {
                        TODO("not implemented yet")
                    }
                )
            )
        }

        if (aggregation.sequentialLoading != null) {
            pipelines.add(
                ItemOrderReverseLimit(
                    orderedItems = aggregation.sequentialLoading.orderedItems,
                    orderedPositions = aggregation.orderedPositions,
                    loading = aggregation.sequentialLoading,
                    coefficient = { _, _ ->
                        TODO("not implemented yet")
                    }
                )
            )
        }

        if (aggregation.trailerLoading != null) {
            pipelines.add(
                TrailerChangeLimit(
                    adjacentPositions = aggregation.adjacentPositions,
                    orderedTrailers = aggregation.trailerLoading.orderedTrailers,
                    loading = aggregation.trailerLoading,
                    coefficient = { _, _ ->
                        TODO("not implemented yet")
                    }
                )
            )

            pipelines.add(
                TrailerCirclingLimit(
                    orderedItemsInTrailers = aggregation.trailerLoading.orderedItemsInTrailers,
                    adjacentPositions = aggregation.adjacentPositions,
                    loading = aggregation.trailerLoading,
                    coefficient = { _, _ ->
                        TODO("not implemented yet")
                    }
                )
            )
        }

        pipelines.add(
            PriorityOrderLimit(
                items = aggregation.items,
                positions = aggregation.positions,
                stowage = aggregation.stowage
            )
        )

        if (aggregation.cargosBySource.isNotEmpty()) {
            pipelines.add(
                SourceEarlyLimit(
                    items = aggregation.items,
                    positions = aggregation.positions,
                    stowage = aggregation.stowage,
                    cargosBySource = aggregation.cargosBySource,
                    earlyEnd = aggregation.earlyEnd
                )
            )
        }

        return Ok(pipelines)
    }
}
