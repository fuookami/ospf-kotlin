package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.*

/**
 * Aggregates the route graph, services, and assignment variables, and registers them with the model.
 * 表示聚合路由图、服务和分配变量，并将其注册到模型。
 *
 * @property graph the route network graph / 路由网络图
 * @property services the list of services / 服务列表
 * @property assignment the service-to-node assignment model / 服务到节点的分配模型
 */
class Aggregation(
    val graph: Graph,
    val services: List<Service>,
    val assignment: Assignment
) {
    /**
     * Registers the assignment variables with the model.
     * 表示将分配变量注册到模型中。
     *
     * @param model the linear meta model instance / 线性元模型实例
     * @return the operation result / 操作结果
     */
    fun register(model: LinearMetaModel<Flt64>): Try {
        val subprocesses = arrayListOf(
            { return@arrayListOf assignment.register(model) }
        )

        for (subprocess in subprocesses) {
            when (val result = subprocess()) {
                is Failed -> {
                    return result
                }

                is Fatal -> {
                    return result
                }

                is Ok -> {}
            }
        }
        return ok
    }
}
