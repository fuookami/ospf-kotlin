package fuookami.ospf.kotlin.example.heuristic_demo

import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.plugins.heuristic.pso.*

class Demo1 {
    operator fun invoke(): Try {
        val model = CallBackModel()
        val x = RealVar("x")
        model.addVar(x)
        model.addObject(ObjectCategory.Minimum, { solution: Solution -> (solution[0] - Flt64.one).pow(2) })
        val solver = PSO(policy = CommonPSOPolicy(timeLimit = 10.seconds))
        val result = solver(model)
        return Ok(success)
    }
}
