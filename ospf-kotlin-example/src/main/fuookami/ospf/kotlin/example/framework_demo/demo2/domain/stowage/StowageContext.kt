package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage


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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.*

internal typealias AircraftAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation

class StowageContext {
    lateinit var aggregation: Aggregation

    fun init(
        aircraftContext: AircraftContext,
        input: RequestDTO
    ): Try {
        when (val result = AggregationInitializer(
            aircraftAggregation = aircraftContext.aggregation,
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

        return ok
    }
    
    fun register(
        stowageMode: StowageMode,
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
        val pipelines = when (val result = generator.invoke(stowageMode)) {
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

    fun analyze(
        solution: List<Flt64>,
        model: AbstractLinearMetaModelFlt64
    ): Ret<fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Solution> {
        val analyzer = SolutionAnalyzer(aggregation)
        val stowageSolution = when (val result = analyzer(solution, model)) {
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

        return Ok(stowageSolution)
    }
    
    fun analyze(
        solution: fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Solution,
        input: RequestDTO
    ): Ret<ResponseDTO> {
        TODO("not implemented yet")
    }
}













