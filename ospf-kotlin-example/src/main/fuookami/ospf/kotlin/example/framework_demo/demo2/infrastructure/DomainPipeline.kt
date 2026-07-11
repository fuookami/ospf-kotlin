package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Common domain pipeline context for FullLoad and Predistribution modes, encapsulating the 8-domain init/register chain to reduce duplication.
 * FullLoad 和 Predistribution 模式的通用域管线上下文，封装 8 个域的初始化/注册链以减少重复。
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

    /**
     * Initializes all eight domain contexts in sequence, returning early on failure.
     * 按顺序初始化所有八个域上下文，失败时提前返回。
     *
     * @param request The request DTO containing problem input data. / 包含问题输入数据的请求 DTO
     * @return Initialization result, ok on success or error on failure. / 初始化结果，成功返回 ok，失败返回错误
    */
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

    /**
     * Registers all eight domain contexts into the optimization model with the given stowage mode and parameters.
     * 将所有八个域上下文注册到优化模型中，使用给定的配载模式和参数。
     *
     * @param stowageMode The stowage mode for registration. / 注册使用的配载模式
     * @param parameter The solving parameters. / 求解参数
     * @param model The linear meta-model to register constraints and variables into. / 要注册约束和变量的线性元模型
     * @return Registration result, ok on success or error on failure. / 注册结果，成功返回 ok，失败返回错误
    */
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
 * Common domain pipeline context for Predistribution mode, extending FullLoadPipelineContext with RedundancyContext.
 * Predistribution 模式的通用域管线上下文，扩展 FullLoadPipelineContext 并添加 RedundancyContext。
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

    /**
     * Initializes the FullLoad pipeline contexts and the redundancy context, returning early on failure.
     * 初始化 FullLoad 管线上下文和余度上下文，失败时提前返回。
     *
     * @param request The request DTO containing problem input data. / 包含问题输入数据的请求 DTO
     * @return Initialization result, ok on success or error on failure. / 初始化结果，成功返回 ok，失败返回错误
    */
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

    /**
     * Registers the FullLoad pipeline contexts and the redundancy context into the optimization model.
     * 将 FullLoad 管线上下文和余度上下文注册到优化模型中。
     *
     * @param stowageMode The stowage mode for registration. / 注册使用的配载模式
     * @param parameter The solving parameters. / 求解参数
     * @param model The linear meta-model to register constraints and variables into. / 要注册约束和变量的线性元模型
     * @return Registration result, ok on success or error on failure. / 注册结果，成功返回 ok，失败返回错误
    */
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
