package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.AircraftContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.StowageContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.*

typealias AircraftAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation
typealias StowageAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation

class LoadingEffectivenessContext {
    lateinit var aggregation: Aggregation

    fun init(
        aircraftContext: AircraftContext,
        stowageContext: StowageContext,
        input: RequestDTO
    ): Try {
        if (!::aggregation.isInitialized) {
            when (val result = AggregationInitializer.invoke(
                aircraftAggregation = aircraftContext.aggregation,
                stowageAggregation = stowageContext.aggregation,
                input = input
            )) {
                is Ok -> {
                    aggregation = result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }

    fun register(
        stowageMode: StowageMode,
        parameter: Parameter,
        model: AbstractLinearMetaModelFlt64
    ): Try {
        when (val result = aggregation.register(
            stowageMode = stowageMode,
            model = model
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        val generator = PipelineListGenerator(aggregation)
        val pipelines = when (val result = generator.invoke(
            stowageMode = stowageMode,
            parameter = parameter
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        for (pipeline in pipelines) {
            when (val result = pipeline(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }

    fun registerForBendersMP(
        model: AbstractLinearMetaModelFlt64
    ): Try {
        TODO("not implemented yet")
    }

    fun registerForBendersSP(
        model: AbstractLinearMetaModelFlt64
    ): Try {
        TODO("not implemented yet")
    }

    fun flushForBendersSP(
        model: AbstractLinearMetaModelFlt64,
        solution: List<Flt64>
    ): Try {
        TODO("not implemented yet")
    }
}













