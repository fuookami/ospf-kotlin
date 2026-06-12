/**
 * 框架数值类型别名
 * Framework number type aliases
 *
 * 为 FltX/Rtn64/RtnX 等数值类型提供线性元模型、二次元模型、求解器输出和解池的便捷类型别名。
 * Provides convenient type aliases for linear/quadratic meta models, solver outputs, and solution pools
 * using FltX/Rtn64/RtnX number types.
 */
package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput

/** FltX 线性元模型 / FltX linear meta model */
typealias FltXLinearMetaModel = LinearMetaModel<FltX>
/** Rtn64 线性元模型 / Rtn64 linear meta model */
typealias Rtn64LinearMetaModel = LinearMetaModel<Rtn64>
/** RtnX 线性元模型 / RtnX linear meta model */
typealias RtnXLinearMetaModel = LinearMetaModel<RtnX>

/** Flt64 二次元模型 / Flt64 quadratic meta model */
typealias Flt64QuadraticMetaModel = QuadraticMetaModel<Flt64>
/** FltX 二次元模型 / FltX quadratic meta model */
typealias FltXQuadraticMetaModel = QuadraticMetaModel<FltX>
/** Rtn64 二次元模型 / Rtn64 quadratic meta model */
typealias Rtn64QuadraticMetaModel = QuadraticMetaModel<Rtn64>
/** RtnX 二次元模型 / RtnX quadratic meta model */
typealias RtnXQuadraticMetaModel = QuadraticMetaModel<RtnX>

/** FltX 可行求解器输出 / FltX feasible solver output */
typealias FltXFeasibleSolverOutput = FeasibleSolverOutput<FltX>
/** Rtn64 可行求解器输出 / Rtn64 feasible solver output */
typealias Rtn64FeasibleSolverOutput = FeasibleSolverOutput<Rtn64>
/** RtnX 可行求解器输出 / RtnX feasible solver output */
typealias RtnXFeasibleSolverOutput = FeasibleSolverOutput<RtnX>

/** FltX 解池 / FltX solution pool */
typealias FltXSolutionPool = List<Solution<FltX>>
/** Rtn64 解池 / Rtn64 solution pool */
typealias Rtn64SolutionPool = List<Solution<Rtn64>>
/** RtnX 解池 / RtnX solution pool */
typealias RtnXSolutionPool = List<Solution<RtnX>>