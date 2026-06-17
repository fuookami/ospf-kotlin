package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

data class PipelineListGenerator(
    private val aggregation: Aggregation
) {
    operator fun invoke(
        stowageMode: StowageMode,
        parameter: Parameter
    ): Ret<PipelineList<AbstractLinearMetaModel<Flt64>>> {
        val pipelines = ArrayList<Pipeline<AbstractLinearMetaModel<Flt64>>>()

        if (aggregation.ballast != null) {
            pipelines.add(
                AdviceBallastWeightLimit(
                    aircraftModel = aggregation.aircraftModel,
                    ballast = aggregation.ballast,
                    coefficient = {
                        TODO("not implemented yet")
                    }
                )
            )
        }

        when (stowageMode) {
            StowageMode.FullLoad -> {
                if (aggregation.aircraftModel.type == AircraftType.B757
                    && aggregation.items.count { it.location.contains(ItemLocationTag.Main) } >= 5
                ) {
                    pipelines.add(
                        EmptyHatedLimit(
                            positions = aggregation.positions,
                            load = aggregation.load,
                            coefficient = {
                                TODO("not implemented yet")
                            }
                        )
                    )
                }
            }

            StowageMode.Predistribution, StowageMode.WeightRecommendation -> {}
        }

        if (aggregation.aircraftModel.mainDeckDoorEmptyPrefer) {
            pipelines.add(
                MainDeckDoorEmptyLimit(
                    items = aggregation.items,
                    positions = aggregation.positions,
                    deck = aggregation.mainDeck,
                    stowage = aggregation.stowage,
                    coefficient = { item ->
                        val coefficient = if (item.cargo.contains(CargoCode.Empty) && item.uld?.category == ULDCategory.Pallet) {
                            Flt64.one
                        } else if (!item.cargo.contains(CargoCode.Empty) && item.uld?.category == ULDCategory.Pallet) {
                            Flt64.two
                        } else if (item.uld?.category == ULDCategory.Container) {
                            Flt64.five
                        } else {
                            Flt64.zero
                        }
                        TODO("not implemented yet")
                    }
                )
            )
        }

        pipelines.add(
            DivideEmptyLoadingLimit(
                adjacentPositions = aggregation.divideEmptyLoading.adjacentPositions,
                divideEmptyLoading = aggregation.divideEmptyLoading,
                emptyBetweenCargoCoefficient = { _, _ ->
                    TODO("not implemented yet")
                },
                emptyCargoBetweenCargoCoefficient = { _, _ ->
                    TODO("not implemented yet")
                },
                emptyBetweenEmptyCargoCoefficient = { _, _ ->
                    TODO("not implemented yet")
                }
            )
        )

        return Ok(pipelines)
    }
}

