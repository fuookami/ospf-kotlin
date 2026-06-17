package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * 生成用于模型构建的冗余约束管线。Generates the pipeline of redundancy constraints for model construction.
 *
 * @property private val aggregation 参数。
 */
class PipelineListGenerator(
    private val aggregation: Aggregation
) {
    operator fun invoke(
        stowageMode: StowageMode,
        parameter: Parameter
    ): Ret<PipelineList<AbstractLinearMetaModel<Flt64>>> {
        val pipelines = kotlin.collections.ArrayList<Pipeline<AbstractLinearMetaModel<Flt64>>>()

        pipelines.add(
            ExperimentalLongitudinalBalanceLimit(
                aircraftModel = aggregation.aircraftModel,
                longitudinalBalance = aggregation.experimentalLongitudinalBalance,
                coefficient = {
                    TODO("not implemented yet")
                }
            )
        )

        pipelines.add(
            RedundancyLimit(
                redundancy = aggregation.redundancy,
                coefficient = {
                    TODO("not implemented yet")
                }
            )
        )

        return Ok(pipelines)
    }
}
