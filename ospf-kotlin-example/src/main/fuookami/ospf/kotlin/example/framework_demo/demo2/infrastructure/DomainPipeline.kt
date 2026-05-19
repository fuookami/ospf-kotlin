package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Common domain pipeline context for FullLoad and Predistribution modes.
 * Encapsulates the 8-domain init/register chain to reduce duplication.
 */
class FullLoadPipelineContext {
    val aircraftContext = AircraftContext()
    val stowageContext = StowageContext()
    val macContext = MacContext()
    val airworthinessSecurityContext = AirworthinessSecurityContext()
    val softSecurityContext = SoftSecurityContext()
    val macOptimizationContext = MacOptimizationContext()
    val expressEffectivenessContext = ExpressEffectivenessContext()
    val loadingEffectivenessContext = LoadingEffectivenessContext()

    fun init(request: RequestDTO): Try {
        aircraftContext.init(input = request).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        stowageContext.init(aircraftContext = aircraftContext, input = request).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        macContext.init(aircraftContext = aircraftContext, stowageContext = stowageContext, input = request).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        airworthinessSecurityContext.init(aircraftContext = aircraftContext, stowageContext = stowageContext, macContext = macContext, input = request).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        softSecurityContext.init(aircraftContext = aircraftContext, stowageContext = stowageContext, input = request).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        macOptimizationContext.init(aircraftContext = aircraftContext, stowageContext = stowageContext, macContext = macContext, input = request).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        expressEffectivenessContext.init(aircraftContext = aircraftContext, stowageContext = stowageContext, input = request).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        loadingEffectivenessContext.init(aircraftContext = aircraftContext, stowageContext = stowageContext, input = request).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        return ok
    }

    fun register(stowageMode: StowageMode, parameter: Parameter, model: AbstractLinearMetaModel<Flt64>): Try {
        stowageContext.register(stowageMode = stowageMode, model = model).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        macContext.register(stowageMode = stowageMode, model = model).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        airworthinessSecurityContext.register(stowageMode = stowageMode, model = model).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        softSecurityContext.register(stowageMode = stowageMode, parameter = parameter, model = model).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        macOptimizationContext.register(stowageMode = stowageMode, parameter = parameter, model = model).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        expressEffectivenessContext.register(stowageMode = stowageMode, parameter = parameter, model = model).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        loadingEffectivenessContext.register(stowageMode = stowageMode, parameter = parameter, model = model).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        return ok
    }
}

/**
 * Common domain pipeline context for Predistribution mode.
 * Extends FullLoadPipelineContext with RedundancyContext.
 */
class PredistributionPipelineContext {
    private val fullLoad = FullLoadPipelineContext()
    val redundancyContext = RedundancyContext()

    val aircraftContext get() = fullLoad.aircraftContext
    val stowageContext get() = fullLoad.stowageContext
    val macContext get() = fullLoad.macContext
    val airworthinessSecurityContext get() = fullLoad.airworthinessSecurityContext
    val softSecurityContext get() = fullLoad.softSecurityContext
    val macOptimizationContext get() = fullLoad.macOptimizationContext
    val expressEffectivenessContext get() = fullLoad.expressEffectivenessContext
    val loadingEffectivenessContext get() = fullLoad.loadingEffectivenessContext

    fun init(request: RequestDTO): Try {
        fullLoad.init(request).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        redundancyContext.init(aircraftContext = fullLoad.aircraftContext, stowageContext = fullLoad.stowageContext, input = request).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        return ok
    }

    fun register(stowageMode: StowageMode, parameter: Parameter, model: AbstractLinearMetaModel<Flt64>): Try {
        fullLoad.register(stowageMode = stowageMode, parameter = parameter, model = model).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        redundancyContext.register(stowageMode = stowageMode, parameter = parameter, model = model).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        return ok
    }
}