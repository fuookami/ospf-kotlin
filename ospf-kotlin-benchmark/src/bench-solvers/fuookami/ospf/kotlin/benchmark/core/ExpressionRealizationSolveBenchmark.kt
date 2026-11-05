package fuookami.ospf.kotlin.benchmark.core

import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.LinearSolver
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiLinearSolver
import fuookami.ospf.kotlin.core.solver.scip.ScipLinearSolver
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.RealVar

/**
 * 表达式符号“原地展开”与“中间变量实现”的建模/求解耗时对比。
 * Compare modeling and solving time between in-place expansion and intermediate-variable realization.
 *
 * 运行（需 Gurobi 10 / SCIP 9 原生库可用）： / Run (Gurobi 10 / SCIP 9 native libraries required):
 * `mvn --% -pl ospf-kotlin-benchmark -Pbench,bench-solvers -DskipTests exec:java `
 * `-Dexec.args="ExpressionRealizationSolveBenchmark.* small 1 1 3 json %TEMP%\realization.json"`
 *
 * @property dataset 规模档位（变量 64/256/1024）/ scale preset (64/256/1024 variables)
 * @property realization 实现方式（inline/intermediate）/ realization mode (inline/intermediate)
 * @property solver 求解后端（gurobi/scip）/ solver backend (gurobi/scip)
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class ExpressionRealizationSolveBenchmark {
    @Param("small", "medium", "large")
    lateinit var dataset: String

    @Param("inline", "intermediate")
    lateinit var realization: String

    @Param("gurobi", "scip")
    lateinit var solver: String

    private lateinit var metaModel: LinearMetaModel<Flt64>
    private lateinit var linearSolver: LinearSolver
    private var referenceObjective: Flt64 = Flt64.zero

    /**
     * 构建未计时的元模型与求解器，并校验两种实现方式目标值一致。
     * Build the untimed meta model and solver, and verify both realization modes reach the same objective.
     */
    @Setup
    fun setup(): Unit = runBlocking {
        requireSolverAvailable(solver)
        metaModel = buildModel(dataset = dataset, realization = realization)
        linearSolver = when (solver) {
            "gurobi" -> GurobiLinearSolver()
            else -> ScipLinearSolver()
        }
        referenceObjective = solveOnce(metaModel)

        if (realization != "inline") {
            val inlineModel = buildModel(dataset = dataset, realization = "inline")
            try {
                val inlineObjective = solveOnce(inlineModel)
                check(abs(referenceObjective.toDouble() - inlineObjective.toDouble()) <= 1.0e-6) {
                    "两种实现方式目标值不一致：inline=$inlineObjective, $realization=$referenceObjective / " +
                        "objective mismatch between realization modes"
                }
            } finally {
                inlineModel.close()
            }
        }
    }

    /** 释放元模型。 / Release the meta model. */
    @TearDown
    fun tearDown() {
        metaModel.close()
    }

    /** @return 机制模型构建耗时（建模时间）/ mechanism-model build time (modeling time) */
    @Benchmark
    fun modeling(): Double = runBlocking {
        val mechanism = when (val result = LinearMechanismModel.invoke(
            metaModel = metaModel,
            concurrent = false,
            blocking = true
        )) {
            is Ok -> result.value
            is Failed -> error(result.error.message)
            is Fatal -> error(result.errors.joinToString { it.message })
        }
        mechanism.use { it.tokens.tokens.size.toDouble() }
    }

    /** @return 转储 + 求解耗时（求解时间）/ dump + solve time (solving time) */
    @Benchmark
    fun solving(): Double = runBlocking {
        solveOnce(metaModel).toDouble()
    }

    /**
     * 构建对比用的线性元模型。 / Build the linear meta model used for the comparison.
     *
     * @param dataset 规模档位 / scale preset
     * @param realization 实现方式 / realization mode
     * @return 线性元模型 / linear meta model
     */
    private fun buildModel(dataset: String, realization: String): LinearMetaModel<Flt64> {
        val factor = when (dataset) {
            "medium" -> 4
            "large" -> 16
            else -> 1
        }
        val variableCount = 64 * factor
        val expressionCount = 32 * factor
        val window = 8
        val useIntermediateVariable = realization == "intermediate"

        val model = LinearMetaModel(name = "realization-$realization-$dataset")
        val variables = List(variableCount) { index ->
            RealVar("realization_x$index").also { variable ->
                variable.range.geq(Flt64.zero)
                variable.range.leq(Flt64(100.0))
            }
        }
        check(model.add(variables) is Ok)

        val expressions = List(expressionCount) { index ->
            val monomials = List(window) { offset ->
                LinearMonomial(
                    coefficient = Flt64(1.0 + ((index + offset) % 5)),
                    symbol = variables[(index * window + offset) % variableCount]
                )
            }
            LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = MutableLinearPolynomial(
                    monomials = monomials,
                    constant = Flt64((index % 3).toDouble())
                ),
                name = "realization_e$index"
            ).also { symbol ->
                symbol.useIntermediateVariable = useIntermediateVariable
            }
        }
        check(model.add(expressions) is Ok)

        // 需求约束与表达式容量约束。/ Demand constraint and per-expression capacity constraints.
        check(model.addConstraint(
            relation = LinearInequality(
                lhs = LinearPolynomial(
                    monomials = variables.map { LinearMonomial(Flt64.one, it) },
                    constant = Flt64.zero
                ),
                rhs = LinearPolynomial(monomials = emptyList(), constant = Flt64(10.0 * variableCount)),
                comparison = Comparison.GE
            ),
            name = "realization_demand"
        ) is Ok)
        expressions.forEachIndexed { index, expression ->
            check(model.addConstraint(
                relation = LinearInequality(
                    lhs = LinearPolynomial(
                        monomials = listOf(LinearMonomial(Flt64.one, expression)),
                        constant = Flt64.zero
                    ),
                    rhs = LinearPolynomial(monomials = emptyList(), constant = Flt64(1000.0 + 10.0 * index)),
                    comparison = Comparison.LE
                ),
                name = "realization_cap$index"
            ) is Ok)
        }

        check(model.minimize(
            LinearPolynomial(
                monomials = variables.mapIndexed { index, variable ->
                    LinearMonomial(Flt64(1.0 + (index % 7)), variable)
                } + expressions.map { LinearMonomial(Flt64.one, it) },
                constant = Flt64.zero
            )
        ) is Ok)
        return model
    }

    /**
     * 端到端求解一次并返回目标值。 / Solve end-to-end once and return the objective value.
     *
     * @param model 线性元模型 / linear meta model
     * @return 目标函数值 / objective value
     */
    private suspend fun solveOnce(model: LinearMetaModel<Flt64>): Flt64 {
        val mechanism = when (val result = linearSolver.dump(
            model = model,
            registrationStatusCallBack = null,
            dumpingStatusCallBack = null
        )) {
            is Ok -> result.value
            is Failed -> error(result.error.message)
            is Fatal -> error(result.errors.joinToString { it.message })
        }
        mechanism.use {
            val triad = linearSolver.dump(it)
            return when (val report = linearSolver(model = triad)) {
                is Ok -> report.value.solution?.objective
                    ?: error("求解未返回目标值 / the solver returned no objective value")
                is Failed -> error(report.error.message)
                is Fatal -> error(report.errors.joinToString { it.message })
            }
        }
    }

    /**
     * 校验求解后端类与原生库可用。 / Verify the backend class and native library are available.
     *
     * @param solver 求解后端名称 / solver backend name
     */
    private fun requireSolverAvailable(solver: String) {
        val className = when (solver) {
            "gurobi" -> "gurobi.GRBEnv"
            else -> "jscip.Scip"
        }
        val available = runCatching { Class.forName(className) }.isSuccess
        check(available) {
            "缺少 $solver 后端依赖：请使用 -Pbench,bench-solvers 构建并确认原生库在 PATH 中 / " +
                "backend $solver is unavailable: build with -Pbench,bench-solvers and put the native library on PATH"
        }
    }
}
