package fuookami.ospf.kotlin.core.solver

import kotlin.test.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.basic.Variable as SolverVariable
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.variable.*

class NativeAbsReportTest {
    @Test
    fun restoresAbsHelpersAndPoolInOnePass() {
        val scenario = absScenario()
        val restored = requireReport(
            restoreNativePiecewiseSolution(
                report = report(
                    values = listOf(Flt64(8.0), Flt64(2.0), Flt64(-2.0)),
                    pool = listOf(listOf(Flt64(9.0), Flt64(3.0), Flt64(3.0)))
                ),
                nativeModel = scenario.nativeModel,
                originalTokens = scenario.originalTokens,
                structures = emptyList(),
                backendName = "test",
                absStructures = listOf(scenario.structure)
            )
        )
        val solution = assertNotNull(restored.solution)

        assertContentEquals(
            listOf(Flt64.zero, Flt64(8.0), Flt64.zero, Flt64(-2.0), Flt64(2.0), Flt64(2.0)),
            solution.values
        )
        assertContentEquals(
            listOf(Flt64(3.0), Flt64(9.0), Flt64.one, Flt64(3.0), Flt64(3.0), Flt64.zero),
            solution.pool.single()
        )
        assertEquals(1, restored.diagnostics.constraintEvaluations.size)
        assertTrue(restored.diagnostics.constraintEvaluations.single().satisfied)
    }

    @Test
    fun restoresMixedPiecewiseAndAbsTokensWithCombinedFingerprint() {
        val scenario = mixedScenario()
        val restored = requireReport(
            restoreNativePiecewiseSolution(
                report = report(
                    values = listOf(
                        Flt64(8.0),
                        Flt64(2.0),
                        Flt64(1.5),
                        Flt64(1.5),
                        Flt64(-2.0)
                    ),
                    pool = listOf(
                        listOf(
                            Flt64(9.0),
                            Flt64(3.0),
                            Flt64(0.5),
                            Flt64(0.5),
                            Flt64(3.0)
                        )
                    )
                ),
                nativeModel = scenario.nativeModel,
                originalTokens = scenario.originalTokens,
                structures = listOf(scenario.piecewise),
                backendName = "test",
                absStructures = listOf(scenario.abs)
            )
        )
        val solution = assertNotNull(restored.solution)

        assertContentEquals(
            listOf(
                Flt64.zero,
                Flt64.zero,
                Flt64(8.0),
                Flt64.zero,
                Flt64(1.5),
                Flt64(-2.0),
                Flt64.one,
                Flt64(2.0),
                Flt64(1.5),
                Flt64(2.0)
            ),
            solution.values
        )
        assertContentEquals(
            listOf(
                Flt64(3.0),
                Flt64.one,
                Flt64(9.0),
                Flt64.one,
                Flt64(0.5),
                Flt64(3.0),
                Flt64.zero,
                Flt64(3.0),
                Flt64(0.5),
                Flt64.zero
            ),
            solution.pool.single()
        )
        assertEquals("test-pwl-abs-1", assertNotNull(restored.fingerprints.model).schemaVersion)
        assertEquals(2, restored.diagnostics.constraintEvaluations.size)
        assertTrue(restored.diagnostics.constraintEvaluations.all { it.satisfied })
    }

    @Test
    fun allowsSharedInputsAndNestedResultInputsInMixedRestore() {
        val scenario = sharedNestedScenario()
        val restored = requireReport(
            restoreNativePiecewiseSolution(
                report = report(
                    values = listOf(Flt64(-2.0), Flt64(-2.0), Flt64(2.0), Flt64(2.0)),
                    pool = listOf(listOf(Flt64(0.5), Flt64(0.5), Flt64(0.5), Flt64(0.5)))
                ),
                nativeModel = scenario.nativeModel,
                originalTokens = scenario.originalTokens,
                structures = listOf(scenario.piecewise),
                backendName = "test",
                absStructures = listOf(scenario.sharedAbs, scenario.nestedAbs)
            )
        )
        val solution = assertNotNull(restored.solution)

        assertContentEquals(
            listOf(
                Flt64.one,
                Flt64.zero,
                Flt64.zero,
                Flt64(-2.0),
                Flt64(-2.0),
                Flt64(2.0),
                Flt64.zero,
                Flt64(2.0),
                Flt64(2.0),
                Flt64(2.0),
                Flt64.zero,
                Flt64.zero
            ),
            solution.values
        )
        assertContentEquals(
            listOf(
                Flt64.zero,
                Flt64(0.5),
                Flt64(0.5),
                Flt64(0.5),
                Flt64(0.5),
                Flt64(0.5),
                Flt64.one,
                Flt64.zero,
                Flt64(0.5),
                Flt64.zero,
                Flt64.one,
                Flt64.one
            ),
            solution.pool.single()
        )
        assertEquals(3, restored.diagnostics.constraintEvaluations.size)
        assertTrue(restored.diagnostics.constraintEvaluations.all { it.satisfied })
    }

    @Test
    fun absOnlyUsesItsOwnFingerprintSchemaAndResidual() {
        val scenario = absScenario()
        val restored = requireReport(
            restoreNativePiecewiseSolution(
                report = report(listOf(Flt64(2.0), Flt64(2.0), Flt64(2.0))),
                nativeModel = scenario.nativeModel,
                originalTokens = scenario.originalTokens,
                structures = emptyList(),
                backendName = "test",
                absStructures = listOf(scenario.structure)
            )
        )
        val fingerprint = assertNotNull(restored.fingerprints.model)

        assertEquals("test-abs-1", fingerprint.schemaVersion)
        assertNotEquals("test-pwl-1", fingerprint.schemaVersion)
        assertEquals(1, restored.diagnostics.constraintEvaluations.size)
        assertTrue(restored.diagnostics.constraintEvaluations.single().constraintId.value.startsWith("test-abs:"))
    }

    @Test
    fun absFingerprintIgnoresFallbackBigM() {
        val scenario = absScenario()
        val changedBounds = scenario.structure.copy(
            positiveBigM = Flt64(99.0),
            negativeBigM = Flt64(101.0)
        )
        val original = requireReport(
            restoreNativePiecewiseSolution(
                report = report(listOf(Flt64(8.0), Flt64(2.0), Flt64(-2.0))),
                nativeModel = scenario.nativeModel,
                originalTokens = scenario.originalTokens,
                structures = emptyList(),
                backendName = "test",
                absStructures = listOf(scenario.structure)
            )
        )
        val changed = requireReport(
            restoreNativePiecewiseSolution(
                report = report(listOf(Flt64(8.0), Flt64(2.0), Flt64(-2.0))),
                nativeModel = scenario.nativeModel,
                originalTokens = scenario.originalTokens,
                structures = emptyList(),
                backendName = "test",
                absStructures = listOf(changedBounds)
            )
        )

        assertEquals(
            assertNotNull(original.fingerprints.model).value,
            assertNotNull(changed.fingerprints.model).value
        )
    }

    @Test
    fun rejectsMissingEndpointsRetainedHelpersAndDuplicateStructures() {
        val scenario = absScenario()
        val missingInput = restoreNativePiecewiseSolution(
            report = report(emptyList()),
            nativeModel = nativeModel(
                listOf(token(scenario.result, 0)),
                scenario.structure
            ),
            originalTokens = scenario.originalTokens,
            structures = emptyList(),
            backendName = "test",
            absStructures = listOf(scenario.structure)
        )
        val missingResult = restoreNativePiecewiseSolution(
            report = report(emptyList()),
            nativeModel = nativeModel(
                listOf(token(scenario.input, 0)),
                scenario.structure
            ),
            originalTokens = scenario.originalTokens,
            structures = emptyList(),
            backendName = "test",
            absStructures = listOf(scenario.structure)
        )
        val retainedHelper = restoreNativePiecewiseSolution(
            report = report(emptyList()),
            nativeModel = nativeModel(
                listOf(
                    token(scenario.input, 0),
                    token(scenario.result, 1),
                    token(scenario.positive, 2)
                ),
                scenario.structure
            ),
            originalTokens = scenario.originalTokens,
            structures = emptyList(),
            backendName = "test",
            absStructures = listOf(scenario.structure)
        )
        val duplicate = restoreNativePiecewiseSolution(
            report = report(emptyList()),
            nativeModel = scenario.nativeModel,
            originalTokens = scenario.originalTokens,
            structures = emptyList(),
            backendName = "test",
            absStructures = listOf(scenario.structure, scenario.structure)
        )
        val localInputResultAlias = restoreNativePiecewiseSolution(
            report = report(emptyList()),
            nativeModel = scenario.nativeModel,
            originalTokens = scenario.originalTokens,
            structures = emptyList(),
            backendName = "test",
            absStructures = listOf(
                scenario.structure.copy(
                    input = LinearPolynomial(
                        listOf(LinearMonomial(Flt64.one, scenario.result)),
                        Flt64.zero
                    )
                )
            )
        )

        assertTrue(missingInput is Failed)
        assertTrue(missingResult is Failed)
        assertTrue(retainedHelper is Failed)
        assertTrue(duplicate is Failed)
        assertTrue(localInputResultAlias is Failed)
    }

    @Test
    fun zeroInputUsesAValidDeterministicSign() {
        val scenario = absScenario()
        val restored = requireReport(
            restoreNativePiecewiseSolution(
                report = report(listOf(Flt64(7.0), Flt64.zero, Flt64.zero)),
                nativeModel = scenario.nativeModel,
                originalTokens = scenario.originalTokens,
                structures = emptyList(),
                backendName = "test",
                absStructures = listOf(scenario.structure)
            )
        )
        val solution = assertNotNull(restored.solution)

        assertContentEquals(
            listOf(Flt64.zero, Flt64(7.0), Flt64.one, Flt64.zero, Flt64.zero, Flt64.zero),
            solution.values
        )
    }

    private data class AbsScenario(
        val nativeModel: LinearTriadModel,
        val originalTokens: List<Token<Flt64>>,
        val structure: AbsStructure<Flt64>,
        val input: RealVar,
        val result: URealVar,
        val positive: URealVar,
        val negative: URealVar,
        val sign: BinVar
    )

    private fun absScenario(): AbsScenario {
        val input = RealVar("abs_input")
        val result = URealVar("abs_result")
        val positive = URealVar("abs_positive")
        val negative = URealVar("abs_negative")
        val sign = BinVar("abs_sign")
        val ordinary = RealVar("ordinary")
        val structure = absStructure(input, result, positive, negative, sign)
        val nativeTokens = listOf(
            token(ordinary, 0),
            token(result, 1),
            token(input, 2)
        )
        val originalTokens = listOf(
            token(positive, 10),
            token(ordinary, 11),
            token(sign, 12),
            token(input, 13),
            token(result, 14),
            token(negative, 15)
        )
        return AbsScenario(
            nativeModel = nativeModel(nativeTokens, structure),
            originalTokens = originalTokens,
            structure = structure,
            input = input,
            result = result,
            positive = positive,
            negative = negative,
            sign = sign
        )
    }

    private data class MixedScenario(
        val nativeModel: LinearTriadModel,
        val originalTokens: List<Token<Flt64>>,
        val abs: AbsStructure<Flt64>,
        val piecewise: UnivariateLinearPiecewiseStructure<Flt64>
    )

    private fun mixedScenario(): MixedScenario {
        val absInput = RealVar("mixed_abs_input")
        val absResult = URealVar("mixed_abs_result")
        val positive = URealVar("mixed_abs_positive")
        val negative = URealVar("mixed_abs_negative")
        val sign = BinVar("mixed_abs_sign")
        val pwlInput = RealVar("mixed_pwl_input")
        val pwlResult = RealVar("mixed_pwl_result")
        val selector0 = BinVar("mixed_pwl_selector_0")
        val selector1 = BinVar("mixed_pwl_selector_1")
        val ordinary = RealVar("mixed_ordinary")
        val abs = absStructure(absInput, absResult, positive, negative, sign, "mixed_abs")
        val piecewise = UnivariateLinearPiecewiseStructure(
            input = LinearPolynomial(listOf(LinearMonomial(Flt64.one, pwlInput)), Flt64.zero),
            breakpoints = listOf(Flt64.zero, Flt64.one, Flt64(2.0)),
            slopes = listOf(Flt64.one, Flt64.one),
            intercepts = listOf(Flt64.zero, Flt64.zero),
            resultVariable = pwlResult,
            selectorVariables = listOf(selector0, selector1),
            converter = IntoValue.Identity,
            name = "mixed_pwl"
        )
        val nativeTokens = listOf(
            token(ordinary, 0),
            token(absResult, 1),
            token(pwlResult, 2),
            token(pwlInput, 3),
            token(absInput, 4)
        )
        val originalTokens = listOf(
            token(positive, 10),
            token(selector0, 11),
            token(ordinary, 12),
            token(sign, 13),
            token(pwlInput, 14),
            token(absInput, 15),
            token(selector1, 16),
            token(absResult, 17),
            token(pwlResult, 18),
            token(negative, 19)
        )
        return MixedScenario(
            nativeModel = nativeModel(nativeTokens, abs, piecewise),
            originalTokens = originalTokens,
            abs = abs,
            piecewise = piecewise
        )
    }

    private data class SharedNestedScenario(
        val nativeModel: LinearTriadModel,
        val originalTokens: List<Token<Flt64>>,
        val sharedAbs: AbsStructure<Flt64>,
        val nestedAbs: AbsStructure<Flt64>,
        val piecewise: UnivariateLinearPiecewiseStructure<Flt64>
    )

    private fun sharedNestedScenario(): SharedNestedScenario {
        val sharedInput = RealVar("shared_input")
        val pwlResult = RealVar("nested_input")
        val sharedResult = URealVar("shared_abs_result")
        val nestedResult = URealVar("nested_abs_result")
        val sharedPositive = URealVar("shared_abs_positive")
        val sharedNegative = URealVar("shared_abs_negative")
        val sharedSign = BinVar("shared_abs_sign")
        val nestedPositive = URealVar("nested_abs_positive")
        val nestedNegative = URealVar("nested_abs_negative")
        val nestedSign = BinVar("nested_abs_sign")
        val selector0 = BinVar("nested_pwl_selector_0")
        val selector1 = BinVar("nested_pwl_selector_1")
        val sharedAbs = absStructure(
            input = sharedInput,
            result = sharedResult,
            positive = sharedPositive,
            negative = sharedNegative,
            sign = sharedSign,
            name = "shared_abs"
        )
        val nestedAbs = absStructure(
            input = pwlResult,
            result = nestedResult,
            positive = nestedPositive,
            negative = nestedNegative,
            sign = nestedSign,
            name = "nested_abs"
        )
        val piecewise = UnivariateLinearPiecewiseStructure(
            input = LinearPolynomial(listOf(LinearMonomial(Flt64.one, sharedInput)), Flt64.zero),
            breakpoints = listOf(Flt64(-3.0), Flt64(-1.0), Flt64.one),
            slopes = listOf(Flt64.one, Flt64.one),
            intercepts = listOf(Flt64.zero, Flt64.zero),
            resultVariable = pwlResult,
            selectorVariables = listOf(selector0, selector1),
            converter = IntoValue.Identity,
            name = "nested_pwl"
        )
        val nativeTokens = listOf(
            token(sharedInput, 0),
            token(pwlResult, 1),
            token(sharedResult, 2),
            token(nestedResult, 3)
        )
        val originalTokens = listOf(
            token(selector0, 10),
            token(sharedPositive, 11),
            token(nestedPositive, 12),
            token(sharedInput, 13),
            token(pwlResult, 14),
            token(sharedResult, 15),
            token(selector1, 16),
            token(sharedNegative, 17),
            token(nestedResult, 18),
            token(nestedNegative, 19),
            token(sharedSign, 20),
            token(nestedSign, 21)
        )
        return SharedNestedScenario(
            nativeModel = nativeModel(nativeTokens, sharedAbs, nestedAbs, piecewise),
            originalTokens = originalTokens,
            sharedAbs = sharedAbs,
            nestedAbs = nestedAbs,
            piecewise = piecewise
        )
    }

    private fun absStructure(
        input: RealVar,
        result: URealVar,
        positive: URealVar,
        negative: URealVar,
        sign: BinVar,
        name: String = "native_abs"
    ): AbsStructure<Flt64> {
        return AbsStructure(
            input = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            resultVariable = result,
            positiveVariable = positive,
            negativeVariable = negative,
            signVariable = sign,
            positiveBigM = Flt64(4.0),
            negativeBigM = Flt64(4.0),
            converter = IntoValue.Identity,
            name = name
        )
    }

    private fun nativeModel(
        tokens: List<Token<Flt64>>,
        vararg structures: DeferredFunctionStructure
    ): LinearTriadModel {
        val variables = tokens.mapIndexed { index, token ->
            SolverVariable(
                index = index,
                lowerBound = Flt64.negativeInfinity,
                upperBound = Flt64.infinity,
                type = token.type,
                origin = token.variable,
                name = token.name
            )
        }
        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = variables,
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix<Flt64>(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "native-abs"
            ),
            tokensInSolver = tokens,
            objective = LinearObjective(ObjectCategory.Minimum, emptyList()),
            functionExpansionPolicy = FunctionExpansionPolicy.AUTO,
            deferredFunctionStructures = structures.toList()
        )
    }

    private fun token(variable: AbstractVariableItem<*, *>, index: Int): Token<Flt64> {
        return Token(variable, index, mutableMapOf(), IntoValue.Identity)
    }

    private fun report(
        values: List<Flt64>,
        pool: List<List<Flt64>> = emptyList()
    ): SolveReport<Flt64> {
        return SolveReport(
            problemStatus = ProblemStatus.Feasible,
            terminationReason = TerminationReason.Completed,
            solutionPresence = SolutionPresence.Optimal,
            solution = SolveSolution(values = values, objective = Flt64.zero, pool = pool),
            fingerprints = SolveFingerprints(
                model = SolveFingerprinting.sha256("native-linear", "linear-test")
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
