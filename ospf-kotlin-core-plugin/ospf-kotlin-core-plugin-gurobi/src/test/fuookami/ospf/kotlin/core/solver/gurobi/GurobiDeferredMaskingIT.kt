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
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.*

class GurobiDeferredMaskingIT {
    @Test
    fun sharedMaskNestedResultsAndWriteFailuresPreserveBothBranches() = runBlocking {
        for (value in listOf(-2.0, 0.0, 0.5, 3.0)) {
            for (mask in listOf(0.0, 1.0)) {
                for (mode in listOf("eager", "native", "failed", "sdk", "reference")) {
                    verify(value, mask, mode)
                }
            }
        }
    }

    @Test
    fun continuousMaskRetainsRelaxation() = runBlocking {
        for (mode in listOf("continuousEager", "continuousDeferred")) verify(0.0, 0.5, mode)
    }

    private suspend fun verify(value: Double, maskValue: Double, mode: String) {
        val continuous = mode.startsWith("continuous")
        val input = RealVar("mask_input")
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(3.0))
        val mask: AbstractVariableItem<*, *> = if (continuous) RealVar("mask").also {
            it.range.geq(Flt64.zero)
            it.range.leq(Flt64.one)
        } else BinVar("mask")
        val first = MaskingFunction(
            input = LinearPolynomial(listOf(LinearMonomial(Flt64(-2.0), input)), Flt64.one),
            mask = mask,
            converter = IntoValue.Identity,
            name = "first"
        )
        (first.resultVar as RealVar).range.geq(Flt64(-5.0))
        (first.resultVar as RealVar).range.leq(Flt64(5.0))
        val second = MaskingFunction(
            input = first.resultPolynomial,
            mask = mask,
            converter = IntoValue.Identity,
            name = "second"
        )
        val policy = if (mode == "eager" || mode == "continuousEager") FunctionExpansionPolicy.EAGER else FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
        val meta = LinearMetaModel<Flt64>(
            name = "masking_native",
            configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = policy),
            converter = IntoValue.Identity
        )
        try {
            requireOk(meta.add(input))
            requireOk(meta.add(mask))
            requireOk(meta.add(LinearFunctionSymbolAdapter(first, IntoValue.Identity)))
            if (!continuous) requireOk(meta.add(LinearFunctionSymbolAdapter(second, IntoValue.Identity)))
            for ((variable, fixed) in listOf(input to value, mask to maskValue)) {
                requireOk(meta.addConstraint(relation = LinearInequality(
                    lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, variable)), Flt64.zero),
                    rhs = LinearPolynomial(emptyList(), Flt64(fixed)),
                    comparison = Comparison.EQ,
                    name = "fixed_${variable.name}"
                ), name = "fixed_${variable.name}"))
            }
            val expected = if (maskValue == 0.0) 0.0 else 1.0 - 2.0 * value
            if (mode == "reference") {
                requireOk(meta.addConstraint(relation = LinearInequality(
                    lhs = second.resultPolynomial,
                    rhs = LinearPolynomial(emptyList(), Flt64(expected)),
                    comparison = Comparison.EQ,
                    name = "public_output"
                ), name = "public_output"))
                requireOk(meta.minimize(second.resultPolynomial))
            } else requireOk(meta.maximize(if (continuous) first.resultPolynomial else second.resultPolynomial))
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
                val context = "input=$value mask=$maskValue mode=$mode"
                val tokens = mechanism.tokens.tokensInSolver
                val resultVariables = if (continuous) listOf(first.resultVar) else listOf(first.resultVar, second.resultVar)
                for (result in resultVariables) {
                    assertEquals(if (continuous) 2.5 else expected, report.values[tokens.indexOfFirst { it.key == result.key }].toDouble(), 1e-8, context)
                }
                if (mode == "native" || mode == "reference") {
                    assertTrue(report.fingerprints.model!!.schemaVersion.endsWith("masking-1"))
                    val residuals = report.diagnostics.constraintEvaluations.filter { it.constraintId.toString().contains("masking:") }
                    assertEquals(2, residuals.size, context)
                    assertTrue(residuals.all { it.satisfied }, context)
                }
                val shape = when (mode) {
                    "native" -> Triple(4, 2, 4)
                    "reference" -> Triple(4, 3, 4)
                    "continuousEager", "continuousDeferred" -> Triple(3, 6, 0)
                    else -> Triple(4, 10, 0)
                }
                assertEquals(listOf(shape), shapes, context)
                assertEquals(if (mode == "failed" || mode == "sdk") 1 else 0, writes, context)
                if (continuous) assertNull(first.deferredStructure())
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
