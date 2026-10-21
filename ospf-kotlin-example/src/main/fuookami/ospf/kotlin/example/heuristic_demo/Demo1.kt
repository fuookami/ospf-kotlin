package fuookami.ospf.kotlin.example.heuristic_demo

import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.callback.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.TimeLimit
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*

/** 演示基于 PSO 的启发式优化在单变量无约束模型上的应用。Demonstrates PSO-based heuristic optimization on a single-variable unconstrained model. */
class Demo1 {

    /**
     * 通过 PSO 构建最小化 (x - 1)^2 的 CallBackModel 并返回结果。
     * Builds a CallBackModel minimizing (x - 1)^2 via PSO and returns the result.
     *
     * @return PSO solver execution result / PSO 求解器执行结果
    */
    operator fun invoke(): Try {
        val model = CallBackModel(converter = IntoValue.Identity)
        val x = RealVar("x")
        model.add(x)
        model.addObject(ObjectCategory.Minimum, { solution: List<Flt64> -> (solution[0] - Flt64.one).pow(2) })
        val solver = particleSwarm<Flt64, Flt64>(
            size = 30,
            stopping = TimeLimit(10.seconds),
            converter = IntoValue.Identity,
            entropy = Generator { Random.nextFlt64() }
        )
        runBlocking { solver(model) }
        return ok
    }
}
