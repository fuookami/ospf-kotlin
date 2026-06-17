package fuookami.ospf.kotlin.example.heuristic_demo

import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.callback.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.heuristic.pso.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*

/** 演示基于 PSO 的启发式优化在单变量无约束模型上的应用。Demonstrates PSO-based heuristic optimization on a single-variable unconstrained model. */
class Demo1 {
    /**
     * Builds a CallBackModel minimizing (x - 1)^2 via PSO and returns the result.
 *
     * @return 返回结果。
     */
    operator fun invoke(): Try {
        val model = CallBackModel(converter = IntoValue.Identity)
        val x = RealVar("x")
        model.add(x)
        model.addObject(ObjectCategory.Minimum, { solution: List<Flt64> -> (solution[0] - Flt64.one).pow(2) })
        val solver = PSO(policy = PSOPolicy(timeLimit = 10.seconds))
        val result = solver(model)
        return ok
    }
}
