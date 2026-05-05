package fuookami.ospf.kotlin.example.heuristic_demo


import fuookami.ospf.kotlin.math.algebra.number.*
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.model.callback.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.heuristic.pso.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class Demo1 {
    operator fun invoke(): Try {
        val model = CallBackModel()
        val x = RealVar("x")
        model.add(x)
        model.addObject(ObjectCategory.Minimum, { solution: List<Flt64> -> (solution[0] - Flt64.one).pow(2) })
        val solver = PSO(policy = PSOPolicy(timeLimit = 10.seconds))
        val result = solver(model)
        return ok
    }
}


