package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.limits.*

class PipelineListGenerator(
    private val aggregation: Aggregation
) {
    operator fun invoke(
        stowageMode: StowageMode,
        parameter: Parameter
    ): Ret<PipelineList<AbstractLinearMetaModelF64>> {
        val pipelines = ArrayList<Pipeline<AbstractLinearMetaModelF64>>()

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

        return Ok(pipelines)
    }
}

