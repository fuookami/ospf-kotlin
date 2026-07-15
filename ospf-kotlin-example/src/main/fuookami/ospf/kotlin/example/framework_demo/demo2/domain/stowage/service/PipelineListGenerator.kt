package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Generator that assembles the list of constraint pipelines for the stowage optimization model
 * based on the current aggregation and stowage mode.
 * 根据当前聚合和装载模式，组装配载优化模型的约束管道列表的生成器。
 *
 * @property aggregation the stowage aggregation / 配载聚合
*/
data class PipelineListGenerator(
    private val aggregation: Aggregation
) {

    /**
     * Generates the list of constraint pipelines for the given stowage mode.
     * 为给定装载模式生成约束管道列表。
     *
     * @param stowageMode the stowage mode / 装载模式
     * @return the list of constraint pipelines or failure / 约束管道列表或失败
    */
    operator fun invoke(
        stowageMode: StowageMode
    ): Ret<PipelineList<AbstractLinearMetaModel<Flt64>>> {
        val pipelines = ArrayList<Pipeline<AbstractLinearMetaModel<Flt64>>>()

        pipelines.add(
            ItemAssignmentLimit(
                items = aggregation.items,
                stowage = aggregation.stowage,
            )
        )

        pipelines.add(
            AppointmentLimit(
                items = aggregation.items,
                positions = aggregation.positions,
                appointment = aggregation.appointment,
                stowage = aggregation.stowage,
            )
        )

        pipelines.add(
            StowageLimit(
                items = aggregation.items,
                positions = aggregation.positions,
                stowage = aggregation.stowage,
            )
        )

        if (aggregation.positions.any { it.status.predicateWeightNeeded }) {
            pipelines.add(
                PredicateLoadWeightLimit(
                    positions = aggregation.positions,
                    load = aggregation.load,
                )
            )
        }

        if (aggregation.positions.any { it.status.recommendedWeightNeeded }) {
            pipelines.add(
                PredicateLoadWeightLimit(
                    positions = aggregation.positions,
                    load = aggregation.load,
                )
            )
        }

        pipelines.add(
            LoadAmountLimit(
                positions = aggregation.positions,
                load = aggregation.load,
            )
        )

        pipelines.add(
            LoadWeightLimit(
                positions = aggregation.positions,
                load = aggregation.load,
                maxLoadWeight = aggregation.maxLoadWeight
            )
        )

        pipelines.add(
            EmptyForbiddenLimit(
                items = aggregation.items,
                positions = aggregation.positions,
                load = aggregation.load,
            )
        )

        pipelines.add(
            NormalBulkDestinationAssignmentLimit(
                items = aggregation.items,
                positions = aggregation.positions,
                stowage = aggregation.stowage,
            )
        )

        pipelines.add(
            AOGMATBulkConflictLimit(
                items = aggregation.items,
                positions = aggregation.positions,
                stowage = aggregation.stowage
            )
        )

        pipelines.add(
            ELDAdjacentLimit(
                items = aggregation.items,
                positions = aggregation.positions,
                neighbours = aggregation.neighbours[NeighbourType.Physics]!!,
                stowage = aggregation.stowage,
            )
        )

        if (stowageMode == StowageMode.Predistribution) {
            pipelines.add(
                LoadingOrderLimit(
                    positions = aggregation.positions,
                    neighbours = aggregation.neighbours[NeighbourType.TopologicalLoadingOrder]!!,
                    load = aggregation.load,
                )
            )
        }

        pipelines.add(
            BiologicalAdjacentLimit(
                items = aggregation.items,
                positions = aggregation.positions,
                neighbours = aggregation.neighbours[NeighbourType.IndirectPhysics]!!,
                biologicalLimit = aggregation.biologicalLimit,
                stowage = aggregation.stowage,
            )
        )

        pipelines.add(
            BiologicalBulkConflictLimit(
                items = aggregation.items,
                positions = aggregation.positions,
                biologicalLimit = aggregation.biologicalLimit,
                stowage = aggregation.stowage,
            )
        )

        return Ok(pipelines)
    }
}
