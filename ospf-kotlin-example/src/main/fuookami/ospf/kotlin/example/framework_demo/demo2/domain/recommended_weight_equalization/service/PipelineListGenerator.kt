package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Generates the pipeline of recommended weight equalization constraints for model construction.
 * 生成用于模型构建的推荐重量均衡约束管线。
 *
 * @property aggregation 包含重量均衡数据的聚合 / The aggregation containing weight equalization data
*/
class PipelineListGenerator(
    private val aggregation: Aggregation
) {
    operator fun invoke(
        stowageMode: StowageMode,
        parameter: Parameter
    ): Ret<PipelineList<AbstractLinearMetaModel<Flt64>>> {
        if (stowageMode != StowageMode.WeightRecommendation) {
            return Ok(emptyList())
        }

        val pipelines = ArrayList<Pipeline<AbstractLinearMetaModel<Flt64>>>()

        pipelines.add(
            ItemOrderLimit(
                items = aggregation.items,
                positions = aggregation.positions,
                stowage = aggregation.stowage
            )
        )

        pipelines.add(
            PriorityAppointmentLimit(
                items = aggregation.items,
                positions = aggregation.positions,
                appointment = aggregation.appointment,
                priorityAppointment = aggregation.priorityAppointment,
                stowage = aggregation.stowage
            )
        )

        pipelines.add(
            RecommendedWeightEqualizationLimit(
                aircraftModel = aggregation.aircraftModel,
                positions = aggregation.positions,
                load = aggregation.load
            )
        )

        pipelines.add(
            RecommendedWeightDeviationObjective(
                load = aggregation.load,
                coefficient = { parameter.weightRecommendationBalance }
            )
        )

        return Ok(pipelines)
    }
}
