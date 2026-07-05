package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.*

/**
 * 聚合路由图、服务和分配变量，并将其注册到模型。Aggregates the route graph, services, and assignment variables, and registers them with the model.
 *
 * @property graph 参数。
 * @property services 参数。
 * @property assignment 参数。
 */
class Aggregation(
    val graph: Graph,
    val services: List<Service>,
    val assignment: Assignment
) {
    /**
     * 将分配变量注册到模型中。Registers the assignment variables with the model.
     *
     * @param model 线性元模型实例。
     * @return 操作结果。
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
