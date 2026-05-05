package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac


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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.service.*

internal typealias AircraftAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation
internal typealias StowageAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation

class MacContext {
    lateinit var aggregation: Aggregation

    fun init(
        aircraftContext: AircraftContext,
        stowageContext: StowageContext,
        input: RequestDTO
    ): Try {
        when (val result = AggregationInitializer(
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

        return ok
    }

    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
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

        return ok
    }

    fun registerForBendersMP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        // MAC computation constraints go into the master problem.
        when (val result = aggregation.register(
            stowageMode = StowageMode.FullLoad,
            model = model
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }

    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        // MAC does not contribute to the sub problem.
        return ok
    }

    fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}













