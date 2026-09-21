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

class GurobiDeferredPolyMaskingIT {
    @Test
    fun definitionEqualityPublicMaskAndFallbackArePreserved() = runBlocking {
        for (value in listOf(-2.0, 0.0, 0.5, 3.0)) {
            for (gate in listOf(0.0, 0.5, 0.75, 1.0, 1.5)) {
                for (mode in listOf("eager", "native", "failed", "sdk", "reference", "opposite")) {
                    verify(value, gate, mode)
                }
            }
        }
    }

    private suspend fun verify(value: Double, gate: Double, mode: String) {
        val input = RealVar("poly_input")
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(3.0))
        val gateVariable = RealVar("gate")
        gateVariable.range.geq(Flt64(-1.0))
        gateVariable.range.leq(Flt64(2.0))
        val function = MaskingWithPolyMaskFunction(
            input = LinearPolynomial(listOf(LinearMonomial(Flt64(-2.0), input)), Flt64.one),
            maskPoly = LinearPolynomial(listOf(LinearMonomial(Flt64(2.0), gateVariable)), Flt64(-1.0)),
            converter = IntoValue.Identity,
            name = "poly_mask"
        )
        val policy = if (mode == "eager") FunctionExpansionPolicy.EAGER else FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
        val meta = LinearMetaModel<Flt64>(
            name = "poly_mask_native",
            configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = policy),
            converter = IntoValue.Identity
        )
        try {
            requireOk(meta.add(input))
            requireOk(meta.add(gateVariable))
            requireOk(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)))
            val expectedMask = 2.0 * gate - 1.0
            val expected = if (expectedMask == 0.0) 0.0 else 1.0 - 2.0 * value
            for ((variable, fixed) in listOf(input to value, gateVariable to gate)) {
                requireOk(meta.addConstraint(relation = LinearInequality(
                    lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, variable)), Flt64.zero),
                    rhs = LinearPolynomial(emptyList(), Flt64(fixed)),
                    comparison = Comparison.EQ,
                    name = "fix_${variable.name}"
                ), name = "fix_${variable.name}"))
            }
            val maskPolynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, function.maskVar)), Flt64.zero)
            if (mode == "reference" || mode == "opposite") {
                requireOk(meta.addConstraint(relation = LinearInequality(
                    lhs = maskPolynomial,
                    rhs = LinearPolynomial(emptyList(), Flt64(if (mode == "opposite") 1.0 - expectedMask else expectedMask)),
                    comparison = Comparison.EQ,
                    name = "public_mask"
                ), name = "public_mask"))
            }
            if (mode == "reference") requireOk(meta.maximize(maskPolynomial))
            else if (value < 0) requireOk(meta.minimize(function.resultPolynomial))
            else requireOk(meta.maximize(function.resultPolynomial))
            requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false)).use { mechanism ->
                val shapes = mutableListOf<Triple<Int, Int, Int>>()
                val solver = GurobiLinearSolver(
                    config = SolverConfig(threadNum = UInt64.one, dumpIntermediateModelBounds = false, functionExpansionPolicy = policy),
                    callBack = GurobiLinearSolverCallBack().afterModeling { _, model, _, _ ->
                        model.update()
                        shapes += Triple(model.get(GRB.IntAttr.NumVars), model.get(GRB.IntAttr.NumConstrs), model.get(GRB.IntAttr.NumGenConstrs))
                        ok
                    }
                )
                var writes = 0
                if (mode == "failed" || mode == "sdk") solver.nativeMaskingWriter = { model, variables, structures ->
                    writes++
                    if (mode == "sdk") {
                        variables.getValue(input.key).set(GRB.DoubleAttr.UB, 4.0)
                        val result = addGurobiNativeMasking(model, variables, structures)
                        assertTrue(result is Failed)
                        result
                    } else {
                        requireOk(addGurobiNativeMasking(model, variables, structures))
                        Failed(ErrorCode.OREngineModelingException, "injected failure after native write")
                    }
                }
                val report = requireOk(solver.solve(mechanism, IntoValue.Identity))
                val context = "input=$value gate=$gate mode=$mode"
                if (expectedMask != 0.0 && expectedMask != 1.0 || mode == "opposite") {
                    assertEquals(ProblemStatus.Infeasible, report.problemStatus, context)
                } else {
                    val tokens = mechanism.tokens.tokensInSolver
                    assertEquals(expectedMask, report.values[tokens.indexOfFirst { it.key == function.maskVar.key }].toDouble(), 1e-8, context)
                    assertEquals(expected, report.values[tokens.indexOfFirst { it.key == function.resultVar.key }].toDouble(), 1e-8, context)
                    if (mode == "native" || mode == "reference") {
                        assertTrue(report.fingerprints.model!!.schemaVersion.endsWith("masking-2"))
                        assertTrue(report.diagnostics.constraintEvaluations.single { it.constraintId.toString().contains("mask-definition:") }.satisfied, context)
                        assertTrue(report.diagnostics.constraintEvaluations.single { it.constraintId.toString().contains("masking:") }.satisfied, context)
                    }
                }
                val shape = when (mode) {
                    "native" -> Triple(4, 3, 2)
                    "reference", "opposite" -> Triple(4, 4, 2)
                    else -> Triple(4, 7, 0)
                }
                assertEquals(listOf(shape), shapes, context)
                assertEquals(if (mode == "failed" || mode == "sdk") 1 else 0, writes, context)
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
