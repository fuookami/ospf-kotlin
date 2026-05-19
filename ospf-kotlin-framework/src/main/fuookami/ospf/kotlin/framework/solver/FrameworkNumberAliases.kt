package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Rtn64
import fuookami.ospf.kotlin.math.algebra.number.RtnX

typealias FltXLinearMetaModel = LinearMetaModel<FltX>
typealias Rtn64LinearMetaModel = LinearMetaModel<Rtn64>
typealias RtnXLinearMetaModel = LinearMetaModel<RtnX>

typealias Flt64QuadraticMetaModel = QuadraticMetaModel<Flt64>
typealias FltXQuadraticMetaModel = QuadraticMetaModel<FltX>
typealias Rtn64QuadraticMetaModel = QuadraticMetaModel<Rtn64>
typealias RtnXQuadraticMetaModel = QuadraticMetaModel<RtnX>

typealias FltXFeasibleSolverOutput = FeasibleSolverOutput<FltX>
typealias Rtn64FeasibleSolverOutput = FeasibleSolverOutput<Rtn64>
typealias RtnXFeasibleSolverOutput = FeasibleSolverOutput<RtnX>

typealias FltXSolutionPool = List<Solution<FltX>>
typealias Rtn64SolutionPool = List<Solution<Rtn64>>
typealias RtnXSolutionPool = List<Solution<RtnX>>