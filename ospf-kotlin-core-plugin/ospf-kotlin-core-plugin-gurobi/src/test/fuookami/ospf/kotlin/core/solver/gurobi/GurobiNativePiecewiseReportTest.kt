package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.basic.Variable as SolverVariable
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.report.ConstraintRelation
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.variable.*

class GurobiNativePiecewiseReportTest {
    @Test
    fun restoresValuesAndPoolByTokenKeyAndRebuildsSelectors() {
        val scenario = scenario()
        val report = report(
            values = listOf(Flt64(20.0), Flt64(2.0), Flt64(1.5), Flt64(10.0)),
            pool = listOf(listOf(Flt64(30.0), Flt64(0.5), Flt64(0.5), Flt64(11.0)))
        )

        val restored = requireReport(
            restoreGurobiPiecewiseSolution(
                report = report,
                nativeModel = scenario.nativeModel,
                originalTokens = scenario.originalTokens,
                structures = listOf(scenario.structure)
            )
        )
        val restoredSolution = assertNotNull(restored.solution)
        val originalSolution = assertNotNull(report.solution)

        assertContentEquals(
            listOf(Flt64.zero, Flt64(10.0), Flt64(2.0), Flt64.one, Flt64(1.5), Flt64(20.0)),
            restoredSolution.values
        )
        assertContentEquals(
            listOf(Flt64.one, Flt64(11.0), Flt64(0.5), Flt64.zero, Flt64(0.5), Flt64(30.0)),
            restoredSolution.pool.single()
        )
        assertEquals(originalSolution.objective, restoredSolution.objective)
        assertEquals(report.statistics, restored.statistics)
        assertEquals(report.diagnostics.variableBoundEvaluations, restored.diagnostics.variableBoundEvaluations)
        assertEquals(2, restored.diagnostics.constraintEvaluations.size)
        assertTrue(restored.diagnostics.constraintEvaluations.last().satisfied)
    }

    @Test
    fun clampsOnlyDomainEndpointsAndRejectsFurtherValues() {
        val scenario = scenario()
        val lower = requireReport(
            restoreGurobiPiecewiseSolution(
                report = report(listOf(Flt64.zero, Flt64.zero, Flt64(-5e-7), Flt64.zero)),
                nativeModel = scenario.nativeModel,
                originalTokens = scenario.originalTokens,
                structures = listOf(scenario.structure)
            )
        )
        val lowerSolution = assertNotNull(lower.solution)
        assertEquals(Flt64.one, lowerSolution.values[0])
        assertEquals(Flt64.zero, lowerSolution.values[3])

        val upper = requireReport(
            restoreGurobiPiecewiseSolution(
                report = report(listOf(Flt64.zero, Flt64(3.0), Flt64(2.0 + 5e-7), Flt64.zero)),
                nativeModel = scenario.nativeModel,
                originalTokens = scenario.originalTokens,
                structures = listOf(scenario.structure)
            )
        )
        val upperSolution = assertNotNull(upper.solution)
        assertEquals(Flt64.zero, upperSolution.values[0])
        assertEquals(Flt64.one, upperSolution.values[3])

        val outside = restoreGurobiPiecewiseSolution(
            report = report(listOf(Flt64.zero, Flt64(3.0), Flt64(2.0 + 2e-6), Flt64.zero)),
            nativeModel = scenario.nativeModel,
            originalTokens = scenario.originalTokens,
            structures = listOf(scenario.structure)
        )
        assertTrue(outside is Failed)
    }

    @Test
    fun missingOriginalTokenIsAnError() {
        val scenario = scenario()
        val unknown = RealVar("unknown")
        val result = restoreGurobiPiecewiseSolution(
            report = report(listOf(Flt64.zero, Flt64.zero, Flt64(1.0), Flt64.zero)),
            nativeModel = scenario.nativeModel,
            originalTokens = scenario.originalTokens + token(unknown, 99),
            structures = listOf(scenario.structure)
        )
        assertTrue(result is Failed)
    }

    @Test
    fun piecewiseDataChangesModelFingerprintWithoutReplacingTheNativeFingerprint() {
        val scenario = scenario()
        val report = report(listOf(Flt64.zero, Flt64(2.0), Flt64(1.5), Flt64.zero))
        val changed = scenario.structure.copy(
            slopes = listOf(Flt64.one, Flt64(3.0)),
            intercepts = listOf(Flt64.zero, Flt64(-2.0))
        )

        val original = requireReport(
            restoreGurobiPiecewiseSolution(
                report = report,
                nativeModel = scenario.nativeModel,
                originalTokens = scenario.originalTokens,
                structures = listOf(scenario.structure)
            )
        )
        val different = requireReport(
            restoreGurobiPiecewiseSolution(
                report = report,
                nativeModel = scenario.nativeModel,
                originalTokens = scenario.originalTokens,
                structures = listOf(changed)
            )
        )
        val originalFingerprint = assertNotNull(original.fingerprints.model)
        val reportFingerprint = assertNotNull(report.fingerprints.model)
        val differentFingerprint = assertNotNull(different.fingerprints.model)

        assertEquals("gurobi-pwl-1", originalFingerprint.schemaVersion)
        assertEquals(SolveFingerprinting.sha256("native-linear", "linear-test").value, reportFingerprint.value)
        assertNotEquals(originalFingerprint.value, differentFingerprint.value)
        assertEquals(report.fingerprints.configuration, original.fingerprints.configuration)
        assertEquals(report.fingerprints.solver, original.fingerprints.solver)
    }

    @Test
    fun reportWithoutSolutionKeepsStateAndUpdatesFingerprint() {
        val scenario = scenario()
        val report = report(emptyList()).copy(solution = null, solutionPresence = SolutionPresence.None)
        val restored = requireReport(
            restoreGurobiPiecewiseSolution(
                report = report,
                nativeModel = scenario.nativeModel,
                originalTokens = scenario.originalTokens,
                structures = listOf(scenario.structure)
            )
        )
        val originalFingerprint = assertNotNull(report.fingerprints.model)
        val restoredFingerprint = assertNotNull(restored.fingerprints.model)

        assertEquals(report.problemStatus, restored.problemStatus)
        assertEquals(report.terminationReason, restored.terminationReason)
        assertEquals(report.solutionPresence, restored.solutionPresence)
        assertNull(restored.solution)
        assertEquals(report.statistics, restored.statistics)
        assertEquals(report.diagnostics, restored.diagnostics)
        assertEquals("gurobi-pwl-1", restoredFingerprint.schemaVersion)
        assertNotEquals(originalFingerprint.value, restoredFingerprint.value)
        assertEquals(report.fingerprints.configuration, restored.fingerprints.configuration)
        assertEquals(report.fingerprints.solver, restored.fingerprints.solver)
    }

    @Test
    fun usesStructureConverterForNativePiecewiseData() {
        val scenario = scenario()
        val input = scenario.structure.input.monomials.single().symbol
        val convertedStructure = scenario.structure.copy(
            input = LinearPolynomial(
                listOf(LinearMonomial(Flt64(0.5), input)),
                Flt64.zero
            ),
            breakpoints = listOf(Flt64.zero, Flt64(0.5), Flt64.one),
            slopes = listOf(Flt64(0.5), Flt64.one),
            intercepts = listOf(Flt64.zero, Flt64(-0.5)),
            converter = doubledValueConverter()
        )
        val restored = requireReport(
            restoreGurobiPiecewiseSolution(
                report = report(listOf(Flt64.zero, Flt64(2.0), Flt64(1.5), Flt64.zero)),
                nativeModel = scenario.nativeModel,
                originalTokens = scenario.originalTokens,
                structures = listOf(convertedStructure)
            )
        )
        val solution = assertNotNull(restored.solution)

        assertContentEquals(
            listOf(Flt64.zero, Flt64.zero, Flt64(2.0), Flt64.one, Flt64(1.5), Flt64.zero),
            solution.values
        )
        assertTrue(restored.diagnostics.constraintEvaluations.last().satisfied)
    }

    private data class Scenario(
        val nativeModel: LinearTriadModel,
        val originalTokens: List<Token<Flt64>>,
        val structure: UnivariateLinearPiecewiseStructure<Flt64>
    )

    private fun scenario(): Scenario {
        val input = RealVar("native_input")
        val result = RealVar("native_result")
        val first = RealVar("ordinary_first")
        val second = RealVar("ordinary_second")
        val selector0 = BinVar("native_selector_0")
        val selector1 = BinVar("native_selector_1")
        val structure = UnivariateLinearPiecewiseStructure(
            input = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            breakpoints = listOf(Flt64.zero, Flt64.one, Flt64.two),
            slopes = listOf(Flt64.one, Flt64.two),
            intercepts = listOf(Flt64.zero, -Flt64.one),
            resultVariable = result,
            selectorVariables = listOf(selector0, selector1),
            converter = IntoValue.Identity
        )
        val nativeTokens = listOf(
            token(second, 0),
            token(result, 1),
            token(input, 2),
            token(first, 3)
        )
        val originalTokens = listOf(
            token(selector0, 10),
            token(first, 11),
            token(result, 12),
            token(selector1, 13),
            token(input, 14),
            token(second, 15)
        )
        val variables = nativeTokens.mapIndexed { index, token ->
            SolverVariable(
                index = index,
                lowerBound = Flt64.negativeInfinity,
                upperBound = Flt64.infinity,
                type = token.type,
                origin = token.variable,
                name = token.name
            )
        }
        val nativeModel = LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = variables,
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix<Flt64>(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "native-pwl"
            ),
            tokensInSolver = nativeTokens,
            objective = LinearObjective(ObjectCategory.Minimum, emptyList()),
            functionExpansionPolicy = FunctionExpansionPolicy.AUTO,
            deferredFunctionStructures = listOf(structure)
        )
        return Scenario(nativeModel, originalTokens, structure)
    }

    private fun token(variable: AbstractVariableItem<*, *>, index: Int): Token<Flt64> {
        return Token(variable, index, mutableMapOf(), IntoValue.Identity)
    }

    private fun doubledValueConverter(): IntoValue<Flt64> {
        return object : IntoValue<Flt64> {
            override fun intoValue(value: Flt64): Flt64 = value
            override val zero: Flt64 get() = Flt64.zero
            override val one: Flt64 get() = Flt64.one
            override fun fromValue(value: Flt64): Flt64 = value * Flt64(2.0)
        }
    }

    private fun report(
        values: List<Flt64>,
        pool: List<List<Flt64>> = emptyList()
    ): SolveReport<Flt64> {
        return SolveReport(
            problemStatus = ProblemStatus.Feasible,
            terminationReason = TerminationReason.Completed,
            solutionPresence = SolutionPresence.Optimal,
            solution = SolveSolution(values = values, objective = Flt64(99.0), pool = pool),
            statistics = SolveStatistics(bestBound = Flt64(98.0)),
            diagnostics = SolveDiagnostics(
                constraintEvaluations = listOf(
                    ConstraintEvaluation(
                        constraintId = ConstraintId("linear:existing"),
                        lhs = Flt64.zero,
                        rhs = Flt64.zero,
                        relation = ConstraintRelation.Equal,
                        slack = Flt64.zero,
                        violation = Flt64.zero,
                        tolerance = Flt64(1e-6),
                        satisfied = true
                    )
                ),
                variableBoundEvaluations = listOf(
                    VariableBoundEvaluation(
                        variableId = VariableId("ordinary:first"),
                        side = BoundSide.Lower,
                        bound = Flt64.zero,
                        value = Flt64.one,
                        slack = Flt64.one,
                        violation = Flt64.zero,
                        tolerance = Flt64(1e-6),
                        satisfied = true
                    )
                )
            ),
            fingerprints = SolveFingerprints(
                model = SolveFingerprinting.sha256("native-linear", "linear-test"),
                configuration = SolveFingerprinting.sha256("configuration", "configuration-test"),
                solver = SolveFingerprinting.sha256("solver", "solver-test")
            )
        )
    }

    private fun requireReport(result: Ret<SolveReport<Flt64>>): SolveReport<Flt64> {
        return when (result) {
            is Ok -> result.value
            is Failed -> fail(result.error.message ?: "unexpected failure")
            is Fatal -> fail(result.errors.toString())
        }
    }
}
