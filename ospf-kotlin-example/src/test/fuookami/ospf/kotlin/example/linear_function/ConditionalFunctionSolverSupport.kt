package fuookami.ospf.kotlin.example.linear_function

import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.AbstractLinearSolver
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.scip.ScipLinearSolver
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.*
import org.junit.jupiter.api.Assumptions.assumeTrue

private const val CONDITIONAL_SOLVER_PROPERTY = "ospf.conditional.solver"

internal data class ConditionalSolverCase(
    val name: String,
    val create: () -> AbstractLinearSolver
)

private fun isClassAvailable(name: String): Boolean {
    return runCatching { Class.forName(name) }.isSuccess
}

/**
 * Select the backend named by the active Maven profile.
 * 按当前 Maven profile 指定的名称选择求解器后端。
 *
 * An absent property keeps the legacy aggregate profile behavior and returns both cases.
 * 未设置该属性时保留旧聚合 profile 行为，返回两个 solver case。
 */
internal fun conditionalSolverCases(): List<ConditionalSolverCase> {
    val candidates = listOf(
        ConditionalSolverCase("scip") { ScipLinearSolver() },
        ConditionalSolverCase("gurobi") { createGurobiSolver() }
    )
    val selectedName = System.getProperty(CONDITIONAL_SOLVER_PROPERTY)
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.isNotEmpty() }

    if (selectedName != null) {
        val candidate = candidates.firstOrNull { it.name == selectedName }
            ?: error(
                "Unsupported conditional solver profile '$selectedName'; " +
                    "expected scip or gurobi."
            )
        val runtimeClass = when (selectedName) {
            "scip" -> "jscip.Scip"
            "gurobi" -> "gurobi.GRBEnv"
            else -> error("Unsupported conditional solver profile '$selectedName'.")
        }
        check(isClassAvailable(runtimeClass)) {
            "Conditional solver profile '$selectedName' is active, but $runtimeClass is not available."
        }
        return listOf(candidate)
    }

    return candidates.filter { candidate ->
        when (candidate.name) {
            "scip" -> isClassAvailable("jscip.Scip")
            "gurobi" -> isClassAvailable("gurobi.GRBEnv")
            else -> false
        }
    }
}

/**
 * 获取当前可用的条件求解器；未启用 solver profile 时允许测试明确跳过。
 * Get available conditional solvers, allowing tests to skip explicitly when no solver profile is active.
 */
internal fun conditionalSolverCasesOrSkip(): List<ConditionalSolverCase> {
    val cases = conditionalSolverCases()
    assumeTrue(
        cases.isNotEmpty(),
        "No conditional solver runtime is available; activate a SCIP or Gurobi test profile."
    )
    return cases
}

private fun createGurobiSolver(): AbstractLinearSolver {
    return Class.forName("fuookami.ospf.kotlin.core.solver.gurobi.GurobiLinearSolver")
        .getDeclaredConstructor()
        .newInstance() as AbstractLinearSolver
}

/**
 * Solve a meta model and apply the result using the triad model's variable identity.
 * 按三元模型中的变量身份回填元模型，避免展开辅助变量造成索引错位。
 */
internal suspend fun solveConditionalMetaModel(
    solver: AbstractLinearSolver,
    model: LinearMetaModel<Flt64>
): Ret<SolveReport<Flt64>> {
    val mechanism = when (val result = solver.dump(model, null, null)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    return mechanism.use { mechanismModel ->
        val triad = solver.dump(mechanismModel)
        try {
            val result = solver(triad)
            if (result is Ok && result.value.solution != null) {
                val values = result.value.values
                check(values.size >= triad.tokensInSolver.size) {
                    "Solver returned ${values.size} values for ${triad.tokensInSolver.size} variables"
                }
                model.setSolution(
                    triad.tokensInSolver
                        .mapIndexed { index, token -> token.variable to values[index] }
                        .toMap()
                )
            }
            result
        } finally {
            triad.close()
        }
    }
}
