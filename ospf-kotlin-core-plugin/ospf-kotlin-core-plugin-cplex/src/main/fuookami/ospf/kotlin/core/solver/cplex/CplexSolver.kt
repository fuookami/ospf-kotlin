/** CPLEX 求解器基类 / CPLEX solver base */
package fuookami.ospf.kotlin.core.solver.cplex

import ilog.cplex.IloCplex
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.solver.output.SolverStatus

/** CPLEX 求解器抽象基类，提供环境初始化和状态分析的通用实现 / CPLEX solver abstract base class, provides common implementation for environment initialization and status analysis */
abstract class CplexSolver : AutoCloseable {
    protected lateinit var cplex: IloCplex
    protected lateinit var status: SolverStatus

    /** 关闭 CPLEX 模型和环境，释放资源 / Close CPLEX model and environment, release resources */
    override fun close() {
        cplex.endModel()
        cplex.end()
    }

    /**
     * 初始化 CPLEX 求解器 / Initialize CPLEX solver
     *
     * @param name 模型名称 / model name
     * @return 操作结果 / operation result
     */
    protected suspend fun init(name: String): Try {
        cplex = IloCplex()
        cplex.name = name
        return ok
    }

    /** 分析 CPLEX 求解状态 / Analyze CPLEX solving status */
    protected suspend fun analyzeStatus(): Try {
        status = when (cplex.status) {
            IloCplex.Status.Optimal -> {
                SolverStatus.Optimal
            }

            IloCplex.Status.Feasible -> {
                SolverStatus.Feasible
            }

            IloCplex.Status.Unbounded -> {
                SolverStatus.Unbounded
            }

            IloCplex.Status.Infeasible -> {
                SolverStatus.Infeasible
            }

            IloCplex.Status.InfeasibleOrUnbounded -> {
                SolverStatus.InfeasibleOrUnbounded
            }

            else -> {
                if (cplex.solnPoolNsolns > 0) {
                    SolverStatus.Feasible
                } else {
                    SolverStatus.SolvingException
                }
            }
        }
        return ok
    }
}