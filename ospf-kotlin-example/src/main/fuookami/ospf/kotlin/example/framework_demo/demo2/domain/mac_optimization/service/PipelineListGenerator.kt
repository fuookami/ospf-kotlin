package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Generates the pipeline of MAC optimization constraints for longitudinal balance, lateral balance, and stabilizers.
 * 生成纵向平衡、横向平衡和安定面的 MAC 优化约束管线。
*/
class PipelineListGenerator(
    private val aggregation: Aggregation
) {
    operator fun invoke(
        stowageMode: StowageMode,
        parameter: Parameter
    ): Ret<PipelineList<AbstractLinearMetaModel<Flt64>>> {
        if (!stowageMode.withMacOptimization) {
            return Ok(emptyList())
        }

        val pipelines = ArrayList<Pipeline<AbstractLinearMetaModel<Flt64>>>()

        pipelines.add(
            LongitudinalBalanceLimit(
                aircraftModel = aggregation.aircraftModel,
                longitudinalBalance = aggregation.longitudinalBalance,
                coefficient = { macRangeType ->
                    when {
                        macRangeType == MACRange.Type.C -> parameter.macRangeC
                        aggregation.aircraftModel.type == AircraftType.B737 -> parameter.B737LongitudinalBalance
                        else -> parameter.longitudinalBalance
                    }
                }
            )
        )

        if (aggregation.lateralBalance != null) {
            pipelines.add(
                LateralBalanceLimit(
                    aircraftModel = aggregation.aircraftModel,
                    lateralBalance = aggregation.lateralBalance,
                    coefficient = { parameter.lateralBalance }
                )
            )
        }

        pipelines.add(
            HorizontalStabilizerLimit(
                horizontalStabilizers = aggregation.horizontalStabilizers,
                coefficient = { parameter.horizontalStabilizerWarn }
            )
        )

        return Ok(pipelines)
    }
}
