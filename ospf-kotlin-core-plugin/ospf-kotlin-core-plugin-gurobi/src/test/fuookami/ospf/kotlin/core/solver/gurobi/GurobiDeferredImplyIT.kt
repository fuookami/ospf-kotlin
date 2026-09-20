package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import gurobi.GRB
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.*

class GurobiDeferredImplyIT {
    @Test
    fun orderedRelationsPreservePublicConsequentAndFallback() = runBlocking {
        for (relation in listOf(Comparison.GT, Comparison.LT, Comparison.GE, Comparison.LE)) {
            for ((antecedent, consequent) in listOf(-1.0 to -1.0, -1.0 to 2.0, 0.0 to 0.0, 1.0 to -1.0, 1.0 to 1.0, 2.0 to 2.0)) {
                for (flag in listOf(0.0, 1.0)) {
                    for (mode in listOf("eager", "native", "failed", "narrow")) {
                        verify(relation, antecedent, consequent, flag, mode, false)
                    }
                }
            }
        }
    }

    @Test
    fun inactiveConsequentAcceptsBusinessGap() = runBlocking {
        for (relation in listOf(Comparison.GT, Comparison.LT)) {
            for (antecedent in listOf(-1.0, 0.5, 1.0)) {
                for (flag in listOf(0.0, 1.0)) {
                    for (mode in listOf("eager", "native", "failed")) {
                        verify(relation, antecedent, 0.5, flag, mode, true)
                    }
                }
            }
        }
    }

    private suspend fun verify(
        relation: Comparison,
        antecedent: Double,
        consequent: Double,
        flag: Double,
        mode: String,
        real: Boolean
    ) {
        fun variable(name: String): AbstractVariableItem<*, *> {
            return if (real) RealVar(name).also {
                it.range.geq(Flt64(-2.0))
                it.range.leq(Flt64(2.0))
            } else IntVar(name).also {
                it.range.geq(Int64(-2))
                it.range.leq(Int64(2))
            }
        }
        fun polynomial(variable: AbstractVariableItem<*, *>) =
            LinearPolynomial(listOf(LinearMonomial(Flt64.one, variable)), Flt64.zero)
        val antecedentVariable = variable("antecedent")
        val consequentVariable = variable("consequent")
        val function = ImplyFunction(
            antecedent = polynomial(antecedentVariable),
            consequent = polynomial(consequentVariable),
            converter = IntoValue.Identity,
            relation = relation,
            strictBoundary = Flt64.one,
            consequentBounds = if (mode == "narrow") ConditionBounds(Flt64(-1.0), Flt64.one) else null,
            name = "implication"
        )
        val policy = if (mode == "eager") FunctionExpansionPolicy.EAGER else FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
        val meta = LinearMetaModel<Flt64>(
            name = "imply_native",
            configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = policy),
            converter = IntoValue.Identity
        )
        try {
            requireOk(meta.add(antecedentVariable))
            requireOk(meta.add(consequentVariable))
            requireOk(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)))
            for ((variable, value) in listOf(antecedentVariable to antecedent, consequentVariable to consequent, function.consequentIndicatorVar to flag)) {
                requireOk(meta.addConstraint(relation = LinearInequality(
                    lhs = polynomial(variable),
                    rhs = LinearPolynomial(emptyList(), Flt64(value)),
                    comparison = Comparison.EQ,
                    name = "fix_${variable.name}"
                ), name = "fix_${variable.name}"))
            }
            requireOk(meta.maximize(polynomial(function.consequentIndicatorVar)))
            requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false)).use { mechanism ->
                val shapes = mutableListOf<Pair<Int, Int>>()
                val solver = GurobiLinearSolver(
                    config = SolverConfig(threadNum = UInt64.one, dumpIntermediateModelBounds = false, functionExpansionPolicy = policy),
                    callBack = GurobiLinearSolverCallBack().afterModeling { _, model, _, _ ->
                        model.update()
                        shapes += model.get(GRB.IntAttr.NumConstrs) to model.get(GRB.IntAttr.NumGenConstrs)
                        ok
                    }
                )
                var writes = 0
                if (mode == "failed") solver.nativeIndicatorWriter = { model, variables, structures ->
                    writes++
                    requireOk(addGurobiNativeIndicator(model, variables, structures))
                    Failed(ErrorCode.OREngineModelingException, "injected failure after native write")
                }
                fun normalized(value: Double): Double = when (relation) {
                    Comparison.GT -> value
                    Comparison.LT -> -value
                    Comparison.GE -> value + 1.0
                    else -> 1.0 - value
                }
                val ant = normalized(antecedent)
                val con = normalized(consequent)
                val expectedAnt = if (ant >= 1.0) 1.0 else 0.0
                val lower = if (mode == "narrow") {
                    if (relation == Comparison.LE || relation == Comparison.GE) 0.0 else -1.0
                } else if (relation == Comparison.LE || relation == Comparison.GE) -1.0 else -2.0
                val upper = if (mode == "narrow") {
                    if (relation == Comparison.LE || relation == Comparison.GE) 2.0 else 1.0
                } else if (relation == Comparison.LE || relation == Comparison.GE) 3.0 else 2.0
                val adjustment = 1.0 - lower
                val feasible = (ant <= 0.0 || ant >= 1.0) && expectedAnt <= flag &&
                    con - adjustment * flag - adjustment * expectedAnt >= lower - adjustment &&
                    con - upper * flag + upper * expectedAnt <= upper
                val report = requireOk(solver.solve(mechanism, IntoValue.Identity))
                val context = "$relation ant=$antecedent con=$consequent flag=$flag mode=$mode"
                if (!feasible) assertEquals(ProblemStatus.Infeasible, report.problemStatus, context)
                else {
                    assertNotEquals(ProblemStatus.Infeasible, report.problemStatus, context)
                    val tokens = mechanism.tokens.tokensInSolver
                    val antIndex = tokens.indexOfFirst { it.key == function.antecedentIndicatorVar.key }
                    val conIndex = tokens.indexOfFirst { it.key == function.consequentIndicatorVar.key }
                    assertEquals(expectedAnt, report.values[antIndex].toDouble(), 1e-8, context)
                    assertEquals(flag, report.values[conIndex].toDouble(), 1e-8, context)
                    if (mode == "native") {
                        assertTrue(report.fingerprints.model!!.schemaVersion.endsWith("indicator-5"))
                        val residuals = report.diagnostics.constraintEvaluations
                        assertTrue(residuals.single { it.constraintId.toString().contains("imply-link:") }.satisfied, context)
                        assertEquals(expectedAnt == 1.0, residuals.any { it.constraintId.toString().contains("implied-condition:") }, context)
                        assertTrue(residuals.all { it.satisfied }, context)
                    }
                }
                assertEquals(listOf(if (mode == "native") 4 to 3 else 8 to 0), shapes, context)
                assertEquals(if (mode == "failed") 1 else 0, writes, context)
            }
        } finally {
            meta.close()
        }
    }

    private fun <T> requireOk(result: Ret<T>): T = when (result) {
        is Ok -> result.value
        is Failed -> fail(result.error.message)
        is Fatal -> fail(result.errors.joinToString { it.message })
    }
}
