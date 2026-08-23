package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/**
 * 路线编译聚合。 / Route compilation aggregation.
 *
 * 包装 RouteCompilation，提供 register、addColumns、removeColumns、
 * extractRouteValues、extractSolution、switchToPhaseTwo 等高层操作。
 */
class RouteCompilationAggregation<V : RealNumber<V>>(
    val instance: VrptwInstance<V>,
    val valueAdapter: NetworkSchedulingSolverValueAdapter<V>
) {
    val compilation = RouteCompilation(instance, valueAdapter)

    /** 内部管线列表 / Internal pipeline list */
    private val _pipelineList = mutableListOf<CGPipeline<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap>>()

    /** 管线列表 / Pipeline list */
    val pipelineList: List<CGPipeline<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap>>
        get() = _pipelineList.toList()

    /**
     * 添加管线。 / Add pipeline.
     */
    fun addPipeline(pipeline: CGPipeline<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap>) {
        _pipelineList.add(pipeline)
    }

    /**
     * 注册到模型。 / Register to model.
     */
    fun register(model: MetaModel<Flt64>): Try {
        return compilation.register(model)
    }

    /**
     * 添加路线列。 / Add route columns.
     */
    fun addColumns(
        iteration: UInt64,
        newRoutes: List<Route<V>>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<Route<V>>> {
        return compilation.addColumns(iteration, newRoutes, model)
    }

    /**
     * 移除路线列。 / Remove route columns.
     */
    fun removeColumns(
        routesToRemove: List<Route<V>>,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return compilation.removeColumns(routesToRemove, model)
    }

    /**
     * 切换到 Phase II。 / Switch to Phase II.
     */
    fun switchToPhaseTwo(model: AbstractLinearMetaModel<Flt64>): Try {
        return compilation.switchToPhaseTwo(model)
    }

    /**
     * 检查 Phase I 是否收敛。 / Check if Phase I has converged.
     */
    fun isPhaseOneConverged(
        model: AbstractLinearMetaModel<Flt64>,
        feasibilityTolerance: Flt64 = instance.tolerances.feasibility
    ): Ret<Boolean> {
        return compilation.artificialCoverage.isPhaseOneConverged(model, feasibilityTolerance)
    }

    /**
     * 提取路线变量值。 / Extract route variable values.
     */
    fun extractRouteValues(
        model: AbstractLinearMetaModel<Flt64>,
        integralityTolerance: Flt64 = instance.tolerances.integrality
    ): Ret<List<Pair<Route<V>, Flt64>>> {
        return compilation.extractRouteValues(model, integralityTolerance)
    }

    /**
     * 提取解中的路线。 / Extract routes in the solution.
     */
    fun extractSolution(
        model: AbstractLinearMetaModel<Flt64>,
        integralityTolerance: Flt64 = instance.tolerances.integrality
    ): Ret<List<Route<V>>> {
        return compilation.extractSolution(model, integralityTolerance)
    }
}
