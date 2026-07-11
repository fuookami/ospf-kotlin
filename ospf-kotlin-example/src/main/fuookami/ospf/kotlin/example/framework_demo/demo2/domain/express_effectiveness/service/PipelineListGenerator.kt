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
 * Generates the pipeline of express effectiveness constraints based on stowage mode and parameters.
 * 基于装载模式和参数生成快递效能约束的管线。
 *
 * @property aggregation The express effectiveness aggregation containing domain models. / 包含域模型的快递效能聚合
*/
data class PipelineListGenerator(
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
