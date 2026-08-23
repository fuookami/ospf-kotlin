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
 * @property aggregation 包含域模型的装车效能聚合 / The loading effectiveness aggregation containing domain models.
*/
class PipelineListGenerator(
    private val aggregation: Aggregation
) {

    /**
     * Generates the list of constraint pipelines based on stowage mode and parameters.
     * 基于装载模式和参数生成约束管线列表。
     *
     * @param stowageMode 决定包含哪些管线的装载模式 / The stowage mode determining which pipelines to include.
     * @param parameter 管线生成的参数配置 / The parameter configuration for pipeline generation.
     * @return 约束管线列表或错误 / The list of constraint pipelines, or an error.
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
                        coefficient = { Flt64.one }
                    )
                )

                pipelines.add(
                    ItemReserveLimit(
                        items = aggregation.items,
                        stowage = aggregation.stowage,
                        coefficient = { Flt64.one }
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
                    coefficient = { Flt64.one }
                )
            )
        }

        pipelines.add(
            SameSourceAdjacentLimit(
                adjacentPositions = aggregation.adjacentPositions,
                sources = aggregation.sources,
                loading = aggregation.transferAdjacentLoading,
                coefficient = { _, _, _ -> parameter.sameFlowTransferIn }
            )
        )

        pipelines.add(
            SameDestinationAdjacent(
                adjacentPositions = aggregation.adjacentPositions,
                destinations = aggregation.destinations,
                loading = aggregation.transferAdjacentLoading,
                coefficient = { _, _, _ -> parameter.sameFlowTransferOut }
            )
        )

        if (aggregation.adviceLoading != null) {
            pipelines.add(
                AdviceLoadAmountLimit(
                    positions = aggregation.positions,
                    loading = aggregation.adviceLoading,
                    coefficient = { parameter.adviceLoadAmount }
                )
            )

            pipelines.add(
                AdviceLoadWeightLimit(
                    positions = aggregation.positions,
                    loading = aggregation.adviceLoading,
                    coefficient = { parameter.adviceLoadWeight }
                )
            )
        }

        if (aggregation.sequentialLoading != null) {
            pipelines.add(
                ItemOrderReverseLimit(
                    orderedItems = aggregation.sequentialLoading.orderedItems,
                    orderedPositions = aggregation.orderedPositions,
                    loading = aggregation.sequentialLoading,
                    coefficient = { _, _ -> parameter.itemOrder }
                )
            )
        }

        if (aggregation.trailerLoading != null) {
            pipelines.add(
                TrailerChangeLimit(
                    adjacentPositions = aggregation.adjacentPositions,
                    orderedTrailers = aggregation.trailerLoading.orderedTrailers,
                    loading = aggregation.trailerLoading,
                    coefficient = { _, _ -> parameter.trailerChange }
                )
            )

            pipelines.add(
                TrailerCirclingLimit(
                    orderedItemsInTrailers = aggregation.trailerLoading.orderedItemsInTrailers,
                    adjacentPositions = aggregation.adjacentPositions,
                    loading = aggregation.trailerLoading,
                    coefficient = { _, _ -> parameter.trailerCircling }
                )
            )
        }

        pipelines.add(
            PriorityOrderLimit(
                items = aggregation.items,
                positions = aggregation.positions,
                stowage = aggregation.stowage,
                bigM = aggregation.bigM
            )
        )

        if (stowageMode != StowageMode.Predistribution && aggregation.cargosBySource.isNotEmpty()) {
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
