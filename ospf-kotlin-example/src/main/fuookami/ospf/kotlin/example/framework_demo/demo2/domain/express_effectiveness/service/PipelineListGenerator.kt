package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * 基于装载模式和参数生成快递效能约束的管线。Generates the pipeline of express effectiveness constraints based on stowage mode and parameters.
 *
 * @property aggregation 参数。
 */
data class PipelineListGenerator(
    private val aggregation: Aggregation
) {
    operator fun invoke(
        stowageMode: StowageMode,
        parameter: Parameter
    ): Ret<PipelineList<AbstractLinearMetaModel<Flt64>>> {
        val pipelines = ArrayList<Pipeline<AbstractLinearMetaModel<Flt64>>>()

        if (aggregation.absoluteOrder != null) {
            pipelines.add(
                ItemPriorityLimit(
                    items = aggregation.items,
                    positions = aggregation.positions,
                    unloading = aggregation.absoluteOrder,
                    stowage = aggregation.stowage,
                    coefficient = {
                        TODO("not implemented yet")
                    }
                )
            )
        }

        if (aggregation.relativeOrder != null) {
            pipelines.add(
                ItemPriorityReverseLimit(
                    orderedItems = aggregation.relativeOrder.orderedItems,
                    orderedPositions = aggregation.relativeOrder.orderedPositions,
                    unloading = aggregation.relativeOrder,
                    coefficient = { lhs, rhs ->
                        TODO("not implemented yet")
                    }
                )
            )
        }

        if (aggregation.mustShipIndices.isNotEmpty()) {
            pipelines.add(
                MustShipLimit(
                    items = aggregation.items,
                    positions = aggregation.positions,
                    stowage = aggregation.stowage,
                    mustShipIndices = aggregation.mustShipIndices
                )
            )
        }

        return Ok(pipelines)
    }
}
