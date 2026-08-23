package fuookami.ospf.kotlin.core.solver.scip

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import jscip.Scip
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalVariable
import fuookami.ospf.kotlin.core.model.constraint_programming.NoOverlap
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolveOptions
import fuookami.ospf.kotlin.core.solver.constraint_programming.FakeConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.MipBackedConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingLoweredLinearModel
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingToLinearModelLowerer
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingInfeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingConflictMinimality
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingUnknownOutput
import fuookami.ospf.kotlin.core.solver.progress.ProgressReporter
import fuookami.ospf.kotlin.core.solver.progress.SolverProgressContext
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.SolveHandle
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.report.BoundSide
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember
import fuookami.ospf.kotlin.core.solver.report.EvidenceMinimality
import fuookami.ospf.kotlin.core.solver.report.EvidenceValidity
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidenceSource
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Failed

class ScipConstraintProgrammingSolverIT {
    @Test
    fun compiledArtifactsRetainSourceAndAuxiliaryMappings() {
        val model = ConstraintProgrammingModel("scip-cp-artifacts", ObjectCategory.Minimum)
        try {
            val value = fuookami.ospf.kotlin.core.variable.IntVar("artifact-value")
            model.registerVariable(value, IntegerDomain.interval(0, 3).value!!)
            val expression = ConstraintProgrammingExpression.Linear(
                terms = listOf(
                    ConstraintProgrammingExpression.Term(value, Int64.one)
                ),
                constant = Int64(2)
            )
            model.addConstraint(
                ConstraintProgrammingConstraint.greaterOrEqual(
                    ConstraintProgrammingExpression.Variable(value),
                    Int64.one
                ).value!!
            )
            model.minimize(expression)

            val scip = Scip()
            try {
                scip.create("scip-cp-artifacts")
                scip.hideOutput(true)
                val compiled = assertIs<Ok<ScipConstraintProgrammingCompiledModel, *, *>>(
                    ScipConstraintProgrammingCompiler(scip, model.snapshot().value!!).compile()
                ).value
                try {
                    val sourceVariableId = "${value.identifier}:${value.index}"
                    assertTrue(
                        compiled.artifacts.values.any {
                            it.role == "source-variable" && it.originId == sourceVariableId
                        }
                    )
                    assertTrue(compiled.artifacts.values.any { it.role == "objective-constant" })
                    assertTrue(compiled.artifacts.values.any { it.role == "compiled-constraint" })
                    assertEquals(compiled.artifacts.size, compiled.artifacts.keys.toSet().size)
                } finally {
                    compiled.close()
                }
            } finally {
                scip.free()
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun scipAndMipSourceProjectionMatchesAcrossRebuiltStableModels() {
        val first = stableProjectionModel(reverseRegistration = false)
        val rebuilt = stableProjectionModel(reverseRegistration = true)
        try {
            val firstLowered = assertIs<Ok<ConstraintProgrammingLoweredLinearModel, *, *>>(
                ConstraintProgrammingToLinearModelLowerer().lower(first)
            ).value
            val rebuiltLowered = assertIs<Ok<ConstraintProgrammingLoweredLinearModel, *, *>>(
                ConstraintProgrammingToLinearModelLowerer().lower(rebuilt)
            ).value
            try {
                val expected = sourceProjection(firstLowered.artifacts.values.map { it.role to it.originId })
                assertEquals(expected, sourceProjection(rebuiltLowered.artifacts.values.map { it.role to it.originId }))

                val firstScip = Scip()
                val rebuiltScip = Scip()
                try {
                    firstScip.create("scip-cp-stable-projection-first")
                    firstScip.hideOutput(true)
                    rebuiltScip.create("scip-cp-stable-projection-rebuilt")
                    rebuiltScip.hideOutput(true)
                    val firstCompiled = assertIs<Ok<ScipConstraintProgrammingCompiledModel, *, *>>(
                        ScipConstraintProgrammingCompiler(firstScip, first.snapshot().value!!).compile()
                    ).value
                    val rebuiltCompiled = assertIs<Ok<ScipConstraintProgrammingCompiledModel, *, *>>(
                        ScipConstraintProgrammingCompiler(rebuiltScip, rebuilt.snapshot().value!!).compile()
                    ).value
                    try {
                        assertEquals(
                            expected,
                            sourceProjection(firstCompiled.artifacts.values.map { it.role to it.originId })
                        )
                        assertEquals(
                            expected,
                            sourceProjection(rebuiltCompiled.artifacts.values.map { it.role to it.originId })
                        )
                    } finally {
                        firstCompiled.close()
                        rebuiltCompiled.close()
                    }
                } finally {
                    firstScip.free()
                    rebuiltScip.free()
                }
            } finally {
                firstLowered.close()
                rebuiltLowered.close()
            }
        } finally {
            first.close()
            rebuilt.close()
        }
    }

    @Test
    fun representativeIntervalBackendsAgreeOnFeasibilityAndObjective() = runBlocking {
        val factories = listOf(
            "fixed" to (::fixedDurationBenchmarkModel to true),
            "optional" to (::optionalDurationBenchmarkModel to false),
            "variable" to (::variableDurationBenchmarkModel to false)
        )
        factories.forEach { (name, specification) ->
            val (factory, nativeSupported) = specification
            val model = factory()
            try {
                val nativeResult = ScipConstraintProgrammingSolver().solve(model)
                if (nativeSupported) {
                    assertTrue(
                        nativeResult.ok,
                        "SCIP CP $name failed: ${when (nativeResult) {
                            is Failed<*, *, *> -> "${nativeResult.code}: ${nativeResult.message}"
                            else -> nativeResult.toString()
                        }}"
                    )
                    assertTrue(
                        nativeResult.value is ConstraintProgrammingFeasibleOutput,
                        "SCIP CP $name returned: ${nativeResult.value}"
                    )
                } else {
                    assertTrue(
                        nativeResult.failed,
                        "SCIP CP $name unexpectedly accepted an unsupported native interval"
                    )
                }
                val native = nativeResult.value as? ConstraintProgrammingFeasibleOutput
                val mipResult = MipBackedConstraintProgrammingSolver(ScipLinearSolver()).solve(model)
                assertTrue(
                    mipResult.ok,
                    "MIP-backed CP $name failed: ${when (mipResult) {
                        is Failed<*, *, *> -> "${mipResult.code}: ${mipResult.message}"
                        else -> mipResult.toString()
                    }}"
                )
                assertTrue(
                    mipResult.value is ConstraintProgrammingFeasibleOutput,
                    "MIP-backed CP $name returned: ${mipResult.value}"
                )
                val mip = mipResult.value as ConstraintProgrammingFeasibleOutput
                val fakeResult = FakeConstraintProgrammingSolver().solve(model)
                assertTrue(
                    fakeResult.ok,
                    "Fake CP $name failed: ${when (fakeResult) {
                        is Failed<*, *, *> -> "${fakeResult.code}: ${fakeResult.message}"
                        else -> fakeResult.toString()
                    }}"
                )
                assertTrue(
                    fakeResult.value is ConstraintProgrammingFeasibleOutput,
                    "Fake CP $name returned: ${fakeResult.value}"
                )
                val fake = fakeResult.value as ConstraintProgrammingFeasibleOutput
                assertEquals(mip.exactObjective, fake.exactObjective)
                assertEquals(mip.solution.intervals.keys, fake.solution.intervals.keys)
                mip.solution.intervals.keys.forEach { intervalId ->
                    assertEquals(mip.solution.interval(intervalId).value, fake.solution.interval(intervalId).value)
                }
                if (native != null) {
                    assertEquals(native.exactObjective, mip.exactObjective)
                    assertEquals(native.solution.intervals.keys, mip.solution.intervals.keys)
                    native.solution.intervals.keys.forEach { intervalId ->
                        assertEquals(native.solution.interval(intervalId).value, mip.solution.interval(intervalId).value)
                    }
                }
            } finally {
                model.close()
            }
        }
    }

    @Test
    fun shouldRespectMaximumObjectiveDirection() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-maximum", ObjectCategory.Maximum)
        try {
            val value = fuookami.ospf.kotlin.core.variable.IntVar("maximum-value")
            model.registerVariable(value, IntegerDomain.interval(0, 3).value!!)
            val expression = ConstraintProgrammingExpression.Variable(value)
            model.maximize(expression)

            val output = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(ScipConstraintProgrammingSolver().solve(model)).value
            )
            assertEquals(Int64(3), output.solution.value(value).value)
            assertEquals(Int64(3), output.exactObjective)
            assertEquals(SolverStatus.Optimal, output.status)
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldIncludeObjectiveConstantInBestBoundAndExactObjective() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-objective-constant", ObjectCategory.Minimum)
        try {
            val value = fuookami.ospf.kotlin.core.variable.IntVar("constant-value")
            model.registerVariable(value, IntegerDomain.interval(0, 3).value!!)
            val objective = ConstraintProgrammingExpression.Linear(
                terms = listOf(
                    ConstraintProgrammingExpression.Term(value, Int64.one)
                ),
                constant = Int64(5)
            )
            model.minimize(objective)

            val output = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(ScipConstraintProgrammingSolver().solve(model)).value
            )
            assertEquals(Int64.zero, output.solution.value(value).value)
            assertEquals(Int64(5), output.exactObjective)
            assertEquals(Flt64(5.0), output.bestBound)
            assertEquals(Flt64(5.0), output.report?.statistics?.bestBound)
        } finally {
            model.close()
        }
    }

    @Test
    fun deterministicModeUsesSingleThreadAndReportsRuntimeIdentity() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-deterministic")
        try {
            val value = fuookami.ospf.kotlin.core.variable.IntVar("value")
            model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
            val output = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(
                    ScipConstraintProgrammingSolver().solve(
                        model,
                        ConstraintProgrammingSolveOptions(
                            deterministic = true,
                            logEnabled = false
                        )
                    )
                ).value
            )
            assertEquals(1, output.report!!.provenance!!.threadCount)
            assertTrue(output.report!!.fingerprints.solver!!.value.startsWith("scip-runtime-2:"))
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldSolveAndRebuildAssumptionModel() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-integration", ObjectCategory.Minimum)
        try {
            val value = fuookami.ospf.kotlin.core.variable.IntVar("value")
            model.registerVariable(value, IntegerDomain.interval(0, 5).value!!)
            val expression = ConstraintProgrammingExpression.Variable(value)
            model.addConstraint(ConstraintProgrammingConstraint.greaterOrEqual(expression, Int64(3)).value!!)
            model.minimize(expression)

            val solver = ScipConstraintProgrammingSolver()
            val output = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(solver.solve(model, ConstraintProgrammingSolveOptions(logEnabled = false))).value
            )
            assertEquals(Int64(3), output.solution.value(value).value)
            assertEquals(SolverStatus.Optimal, output.status)

            val snapshots = ArrayList<Int>()
            val session = assertIs<fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSession>(
                solver.createSession(
                    model,
                    ConstraintProgrammingSolveOptions(
                        progressContext = SolverProgressContext(
                            reporter = ProgressReporter {
                                snapshots += it.overallProgress
                                ok
                            }
                        )
                    )
                ).value
            )
            try {
                val hintedResult = session.solve(
                    hints = fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolution(
                        values = mapOf(VariableId("${value.identifier}:${value.index}") to Int64(4))
                    )
                )
                assertTrue(
                    hintedResult is Ok,
                    "SCIP hint failed: ${if (hintedResult is Failed) hintedResult.error.message else hintedResult}"
                )
                val hinted = assertIs<ConstraintProgrammingFeasibleOutput>(hintedResult.value)
                assertEquals(Int64(3), hinted.solution.value(value).value)
                assertTrue(snapshots.isNotEmpty())

                val infeasible = assertIs<ConstraintProgrammingInfeasibleOutput>(
                    assertIs<Ok<*, *, *>>(session.solve(listOf(BooleanLiteral.False))).value
                )
                assertEquals(fuookami.ospf.kotlin.core.solver.report.ProofStatus.Verified, infeasible.proofStatus)

                val rebuilt = assertIs<ConstraintProgrammingFeasibleOutput>(
                    assertIs<Ok<*, *, *>>(session.solve()).value
                )
                assertEquals(Int64(3), rebuilt.solution.value(value).value)
            } finally {
                session.close()
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldLowerForbiddenTableWithoutAllowingForbiddenTuple() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-forbidden-table", ObjectCategory.Minimum)
        try {
            val first = fuookami.ospf.kotlin.core.variable.BinVar("first")
            val second = fuookami.ospf.kotlin.core.variable.BinVar("second")
            model.registerVariable(first, IntegerDomain.boolean)
            model.registerVariable(second, IntegerDomain.boolean)
            val firstExpression = ConstraintProgrammingExpression.Variable(first)
            val secondExpression = ConstraintProgrammingExpression.Variable(second)
            model.addConstraint(
                ConstraintProgrammingConstraint.forbiddenAssignments(
                    listOf(firstExpression, secondExpression),
                    listOf(listOf(Int64.zero, Int64.zero))
                ).value!!
            )
            model.minimize(firstExpression)

            val output = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(ScipConstraintProgrammingSolver().solve(model)).value
            )
            assertEquals(Int64.zero, output.solution.value(first).value)
            assertEquals(Int64.one, output.solution.value(second).value)
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldValidateOptionsCancellationAndClosedSession() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-boundaries", ObjectCategory.Minimum)
        try {
            val solver = ScipConstraintProgrammingSolver()
            assertTrue(
                solver.createSession(
                    model,
                    ConstraintProgrammingSolveOptions(nodeLimit = UInt64.zero)
                ).failed
            )

            val handle = SolveHandle.create()
            handle.cancel()
            val session = assertIs<fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSession>(
                solver.createSession(
                    model,
                    ConstraintProgrammingSolveOptions(cancellationToken = handle.token)
                ).value
            )
            val cancelled = assertIs<ConstraintProgrammingUnknownOutput>(
                assertIs<Ok<*, *, *>>(session.solve()).value
            )
            assertEquals(TerminationReason.Cancelled, cancelled.terminationReason)
            session.close()
            assertTrue(session.solve().failed)
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldShrinkAssumptionConflictWithProofGate() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-conflict", ObjectCategory.Minimum)
        try {
            val required = fuookami.ospf.kotlin.core.variable.BinVar("required")
            val redundant = fuookami.ospf.kotlin.core.variable.BinVar("redundant")
            model.registerVariable(required, IntegerDomain.boolean)
            model.registerVariable(redundant, IntegerDomain.boolean)
            model.addConstraint(
                ConstraintProgrammingConstraint.equal(
                    ConstraintProgrammingExpression.Variable(required),
                    Int64.zero
                ).value!!,
                id = "force-required-zero"
            )

            val session = assertIs<fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSession>(
                ScipConstraintProgrammingSolver().createSession(
                    model,
                    ConstraintProgrammingSolveOptions(
                        collectConflict = true,
                        shrinkConflict = true
                    )
                ).value
            )
            try {
                val output = assertIs<ConstraintProgrammingInfeasibleOutput>(
                    assertIs<Ok<*, *, *>>(
                        session.solve(
                            assumptions = listOf(BooleanLiteral(required), BooleanLiteral(redundant))
                        )
                    ).value
                )
                val conflict = assertNotNull(output.conflict)
                assertEquals(listOf(BooleanLiteral(required)), conflict.assumptions)
                assertEquals(ConstraintProgrammingConflictMinimality.Irreducible, conflict.minimality)
                assertEquals(
                    setOf(VariableId("${required.identifier}:${required.index}")),
                    conflict.variableIds
                )
                val evidence = assertNotNull(output.report?.diagnostics?.infeasibilityEvidence)
                assertEquals(InfeasibilityEvidenceSource.ConstraintConflict, evidence.source)
                assertEquals(EvidenceValidity.Verified, evidence.validity)
                assertEquals(EvidenceMinimality.Irreducible, evidence.minimality)
            } finally {
                session.close()
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldProjectVerifiedConflictToVariableLowerBound() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-bound-conflict", ObjectCategory.Minimum)
        try {
            val value = fuookami.ospf.kotlin.core.variable.IntVar("bounded-value")
            model.registerVariable(value, IntegerDomain.interval(1, 3).value!!)
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(
                    ConstraintProgrammingExpression.Variable(value),
                    Int64.zero
                ).value!!,
                id = "force-at-most-zero"
            )
            val session = assertIs<fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSession>(
                ScipConstraintProgrammingSolver().createSession(
                    model,
                    ConstraintProgrammingSolveOptions(
                        collectConflict = true,
                        shrinkConflict = true,
                        deterministic = true
                    )
                ).value
            )
            try {
                val output = assertIs<ConstraintProgrammingInfeasibleOutput>(
                    assertIs<Ok<*, *, *>>(session.solve()).value
                )
                val evidence = assertNotNull(output.report?.diagnostics?.infeasibilityEvidence)
                assertEquals(EvidenceValidity.Verified, evidence.validity)
                assertEquals(EvidenceMinimality.Irreducible, evidence.minimality)
                assertTrue(
                    evidence.members.contains(
                        InfeasibilityMember.VariableBound(
                            fuookami.ospf.kotlin.core.solver.report.VariableBoundRef(
                                VariableId("${value.identifier}:${value.index}"),
                                BoundSide.Lower
                            )
                        )
                    )
                )
                assertTrue(evidence.constraintIds.contains(fuookami.ospf.kotlin.core.solver.report.ConstraintId("force-at-most-zero")))
            } finally {
                session.close()
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldKeepSparseDomainAsIndependentConflictMember() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-domain-conflict", ObjectCategory.Minimum)
        try {
            val value = fuookami.ospf.kotlin.core.variable.IntVar("sparse-value")
            model.registerVariable(value, IntegerDomain.values(listOf(0, 2)).value!!)
            model.addConstraint(
                ConstraintProgrammingConstraint.equal(
                    ConstraintProgrammingExpression.Variable(value),
                    Int64.one
                ).value!!,
                id = "force-missing-domain-value"
            )
            val session = assertIs<fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSession>(
                ScipConstraintProgrammingSolver().createSession(
                    model,
                    ConstraintProgrammingSolveOptions(
                        collectConflict = true,
                        shrinkConflict = true,
                        deterministic = true
                    )
                ).value
            )
            try {
                val output = assertIs<ConstraintProgrammingInfeasibleOutput>(
                    assertIs<Ok<*, *, *>>(session.solve()).value
                )
                val evidence = assertNotNull(output.report?.diagnostics?.infeasibilityEvidence)
                assertEquals(EvidenceValidity.Verified, evidence.validity)
                assertTrue(
                    evidence.members.contains(
                        InfeasibilityMember.VariableDomain(
                            fuookami.ospf.kotlin.core.solver.report.VariableDomainRef(
                                VariableId("${value.identifier}:${value.index}")
                            )
                        )
                    )
                )
                assertEquals(
                    EvidenceMinimality.Irreducible,
                    evidence.minimality
                )
            } finally {
                session.close()
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldCompileCircuitWithExactLowering() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-circuit", ObjectCategory.Minimum)
        try {
            val successors = (0 until 3).map { index ->
                fuookami.ospf.kotlin.core.variable.IntVar("circuit-next-$index")
            }
            successors.forEach { model.registerVariable(it, IntegerDomain.interval(0, 2).value!!) }
            val circuit = ConstraintProgrammingConstraint.circuit(
                successors.map { ConstraintProgrammingExpression.Variable(it) }
            ).value!!
            model.addConstraint(circuit, id = "circuit")
            val output = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(ScipConstraintProgrammingSolver().solve(model)).value
            )
            val values = successors.map { output.solution.value(it).value!!.toLong().toInt() }
            assertEquals(3, values.toSet().size)
            var current = 0
            val visited = HashSet<Int>()
            repeat(3) {
                assertTrue(visited.add(current))
                current = values[current]
            }
            assertEquals(0, current)
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldCompileAutomatonWithExactLowering() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-automaton", ObjectCategory.Minimum)
        try {
            val first = fuookami.ospf.kotlin.core.variable.IntVar("automaton-first")
            val second = fuookami.ospf.kotlin.core.variable.IntVar("automaton-second")
            model.registerVariable(first, IntegerDomain.boolean)
            model.registerVariable(second, IntegerDomain.boolean)
            val automaton = ConstraintProgrammingConstraint.automaton(
                expressions = listOf(
                    ConstraintProgrammingExpression.Variable(first),
                    ConstraintProgrammingExpression.Variable(second)
                ),
                initialState = 0,
                finalStates = setOf(2),
                transitions = listOf(
                    ConstraintProgrammingConstraint.AutomatonTransition(0, Int64.zero, 1),
                    ConstraintProgrammingConstraint.AutomatonTransition(1, Int64.one, 2)
                )
            ).value!!
            model.addConstraint(automaton, id = "automaton")
            val output = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(ScipConstraintProgrammingSolver().solve(model)).value
            )
            assertEquals(Int64.zero, output.solution.value(first).value)
            assertEquals(Int64.one, output.solution.value(second).value)
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldRejectNonDeterministicAutomatonBeforeLowering() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-automaton-duplicate", ObjectCategory.Minimum)
        try {
            val value = fuookami.ospf.kotlin.core.variable.IntVar("automaton-duplicate-value")
            model.registerVariable(value, IntegerDomain.boolean)
            model.addConstraint(
                ConstraintProgrammingConstraint.Automaton(
                    expressions = listOf(ConstraintProgrammingExpression.Variable(value)),
                    initialState = 0,
                    finalStates = setOf(1),
                    transitions = listOf(
                        ConstraintProgrammingConstraint.AutomatonTransition(0, Int64.zero, 1),
                        ConstraintProgrammingConstraint.AutomatonTransition(0, Int64.zero, 2)
                    )
                ),
                id = "automaton-duplicate"
            )
            assertTrue(ScipConstraintProgrammingSolver().solve(model).failed)
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldRejectAutomatonTransitionScaleBeforeCreatingAuxiliaries() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-automaton-scale", ObjectCategory.Minimum)
        try {
            val value = fuookami.ospf.kotlin.core.variable.IntVar("automaton-scale-value")
            model.registerVariable(value, IntegerDomain.interval(0, 100).value!!)
            val transitions = (0..100).map { transitionValue ->
                ConstraintProgrammingConstraint.AutomatonTransition(
                    fromState = 0,
                    value = Int64(transitionValue.toLong()),
                    toState = if (transitionValue == 100) 1 else 0
                )
            }
            model.addConstraint(
                ConstraintProgrammingConstraint.Automaton(
                    expressions = listOf(ConstraintProgrammingExpression.Variable(value)),
                    initialState = 0,
                    finalStates = setOf(1),
                    transitions = transitions
                ),
                id = "automaton-scale"
            )
            assertTrue(ScipConstraintProgrammingSolver(decompositionLimit = 4).solve(model).failed)
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldCompileReservoirWithStableTieOrdering() {
        runBlocking {
            val model = ConstraintProgrammingModel("scip-cp-reservoir", ObjectCategory.Minimum)
            try {
                val reservoir = ConstraintProgrammingConstraint.reservoir(
                    events = listOf(
                        ConstraintProgrammingConstraint.Reservoir.Event(
                            ConstraintProgrammingExpression.Constant(Int64.one),
                            ConstraintProgrammingExpression.Constant(Int64(2))
                        ),
                        ConstraintProgrammingConstraint.Reservoir.Event(
                            ConstraintProgrammingExpression.Constant(Int64.one),
                            ConstraintProgrammingExpression.Constant(Int64(-2))
                        )
                    ),
                    initialLevel = Int64.zero,
                    minimumLevel = Int64.zero,
                    maximumLevel = Int64(2)
                ).value!!
                model.addConstraint(reservoir, id = "reservoir")
                assertIs<ConstraintProgrammingFeasibleOutput>(
                    assertIs<Ok<*, *, *>>(ScipConstraintProgrammingSolver().solve(model)).value
                )
            } finally {
                model.close()
            }
        }
    }

    @Test
    fun shouldCompileReservoirVariableLevelChanges() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-reservoir-variable-change", ObjectCategory.Minimum)
        try {
            val firstChange = fuookami.ospf.kotlin.core.variable.IntVar("reservoir-first-change")
            val secondChange = fuookami.ospf.kotlin.core.variable.IntVar("reservoir-second-change")
            model.registerVariable(firstChange, IntegerDomain.interval(-2, 2).value!!)
            model.registerVariable(secondChange, IntegerDomain.interval(-2, 2).value!!)
            model.addConstraint(
                ConstraintProgrammingConstraint.equal(
                    ConstraintProgrammingExpression.Variable(firstChange),
                    Int64(2)
                ).value!!
            )
            model.addConstraint(
                ConstraintProgrammingConstraint.equal(
                    ConstraintProgrammingExpression.Variable(secondChange),
                    Int64(-2)
                ).value!!
            )
            model.addConstraint(
                ConstraintProgrammingConstraint.reservoir(
                    events = listOf(
                        ConstraintProgrammingConstraint.Reservoir.Event(
                            ConstraintProgrammingExpression.Constant(Int64.one),
                            ConstraintProgrammingExpression.Variable(firstChange)
                        ),
                        ConstraintProgrammingConstraint.Reservoir.Event(
                            ConstraintProgrammingExpression.Constant(Int64.one),
                            ConstraintProgrammingExpression.Variable(secondChange)
                        )
                    ),
                    initialLevel = Int64.zero,
                    minimumLevel = Int64.zero,
                    maximumLevel = Int64(2)
                ).value!!
            )
            val output = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(ScipConstraintProgrammingSolver().solve(model)).value
            )
            assertEquals(Int64(2), output.solution.value(firstChange).value)
            assertEquals(Int64(-2), output.solution.value(secondChange).value)
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldRejectReservoirLevelViolation() = runBlocking {
        val model = ConstraintProgrammingModel("scip-cp-reservoir-infeasible", ObjectCategory.Minimum)
        try {
            model.addConstraint(
                ConstraintProgrammingConstraint.reservoir(
                    events = listOf(
                        ConstraintProgrammingConstraint.Reservoir.Event(
                            ConstraintProgrammingExpression.Constant(Int64.one),
                            ConstraintProgrammingExpression.Constant(Int64(2))
                        )
                    ),
                    initialLevel = Int64.zero,
                    minimumLevel = Int64.zero,
                    maximumLevel = Int64.one
                ).value!!
            )
            val output = assertIs<ConstraintProgrammingInfeasibleOutput>(
                assertIs<Ok<*, *, *>>(ScipConstraintProgrammingSolver().solve(model)).value
            )
            assertEquals(fuookami.ospf.kotlin.core.solver.report.ProofStatus.Verified, output.proofStatus)
        } finally {
            model.close()
        }
    }

    private fun sourceProjection(values: List<Pair<String, String?>>): List<String> {
        return values.filter { it.second != null }.map { "${it.first}|${it.second}" }.sorted()
    }

    private fun stableProjectionModel(reverseRegistration: Boolean): ConstraintProgrammingModel {
        val model = ConstraintProgrammingModel("scip-stable-projection", ObjectCategory.Minimum)
        val x = fuookami.ospf.kotlin.core.variable.IntVar("duplicate-display")
        val y = fuookami.ospf.kotlin.core.variable.IntVar("duplicate-display")
        val variables = listOf(
            VariableId("stable:x") to x,
            VariableId("stable:y") to y
        )
        (if (reverseRegistration) variables.asReversed() else variables).forEach { (id, variable) ->
            model.registerVariable(
                id = id,
                variable = variable,
                domain = IntegerDomain.interval(0, 1).value!!,
                scope = "stable",
                origin = "fixture/${id.value}"
            )
        }
        val constraints = listOf(
            ConstraintId("stable:x-lower") to ConstraintProgrammingConstraint.greaterOrEqual(
                ConstraintProgrammingExpression.Variable(x),
                Int64.zero
            ).value!!,
            ConstraintId("stable:y-lower") to ConstraintProgrammingConstraint.greaterOrEqual(
                ConstraintProgrammingExpression.Variable(y),
                Int64.zero
            ).value!!
        )
        (if (reverseRegistration) constraints.asReversed() else constraints).forEach { (id, constraint) ->
            model.addConstraint(
                constraint = constraint,
                id = id,
                name = "duplicate-display",
                scope = "stable",
                origin = "fixture/${id.value}"
            )
        }
        model.minimize(
            expression = ConstraintProgrammingExpression.Variable(x),
            id = ObjectiveId("stable:objective"),
            name = "duplicate-display",
            scope = "stable",
            origin = "fixture/stable:objective"
        )
        return model
    }

    private fun fixedDurationBenchmarkModel(): ConstraintProgrammingModel {
        val model = ConstraintProgrammingModel("benchmark-fixed-duration", ObjectCategory.Minimum)
        val starts = List(2) { index -> fuookami.ospf.kotlin.core.variable.IntVar("fixed-start-$index") }
        val ends = List(2) { index -> fuookami.ospf.kotlin.core.variable.IntVar("fixed-end-$index") }
        val intervals = starts.indices.map { index ->
            model.registerVariable(starts[index], IntegerDomain.singleton(Int64(index.toLong() * 3)))
            model.registerVariable(ends[index], IntegerDomain.singleton(Int64(index.toLong() * 3 + 2)))
            IntervalVariable.fixed(
                id = fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId("fixed-$index"),
                start = ConstraintProgrammingExpression.Variable(starts[index]),
                size = Int64(2),
                end = ConstraintProgrammingExpression.Variable(ends[index])
            ).value!!
        }
        intervals.forEach(model::registerInterval)
        model.addConstraint(NoOverlap.create(intervals).value!!, id = "fixed-no-overlap")
        return model
    }

    private fun optionalDurationBenchmarkModel(): ConstraintProgrammingModel {
        val model = ConstraintProgrammingModel("benchmark-optional-duration", ObjectCategory.Minimum)
        val starts = List(2) { index -> fuookami.ospf.kotlin.core.variable.IntVar("optional-start-$index") }
        val ends = List(2) { index -> fuookami.ospf.kotlin.core.variable.IntVar("optional-end-$index") }
        val presence = List(2) { index -> fuookami.ospf.kotlin.core.variable.BinVar("optional-presence-$index") }
        val intervals = starts.indices.map { index ->
            model.registerVariable(starts[index], IntegerDomain.singleton(Int64(index.toLong() * 3)))
            model.registerVariable(ends[index], IntegerDomain.singleton(Int64(index.toLong() * 3 + 2)))
            model.registerVariable(presence[index], IntegerDomain.singleton(Int64(index.toLong())))
            IntervalVariable.fixed(
                id = fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId("optional-$index"),
                start = ConstraintProgrammingExpression.Variable(starts[index]),
                size = Int64(2),
                end = ConstraintProgrammingExpression.Variable(ends[index]),
                presence = BooleanLiteral(presence[index])
            ).value!!
        }
        intervals.forEach(model::registerInterval)
        model.addConstraint(NoOverlap.create(intervals).value!!, id = "optional-no-overlap")
        return model
    }

    private fun variableDurationBenchmarkModel(): ConstraintProgrammingModel {
        val model = ConstraintProgrammingModel("benchmark-variable-duration", ObjectCategory.Minimum)
        val starts = List(2) { index -> fuookami.ospf.kotlin.core.variable.IntVar("variable-start-$index") }
        val sizes = List(2) { index -> fuookami.ospf.kotlin.core.variable.IntVar("variable-size-$index") }
        val ends = List(2) { index -> fuookami.ospf.kotlin.core.variable.IntVar("variable-end-$index") }
        val intervals = starts.indices.map { index ->
            val start = Int64(index.toLong() * 3)
            val size = Int64(index.toLong() + 1)
            model.registerVariable(starts[index], IntegerDomain.singleton(start))
            model.registerVariable(sizes[index], IntegerDomain.singleton(size))
            model.registerVariable(ends[index], IntegerDomain.singleton(start + size))
            IntervalVariable.create(
                id = fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId("variable-$index"),
                start = ConstraintProgrammingExpression.Variable(starts[index]),
                size = ConstraintProgrammingExpression.Variable(sizes[index]),
                end = ConstraintProgrammingExpression.Variable(ends[index])
            ).value!!
        }
        intervals.forEach(model::registerInterval)
        model.addConstraint(NoOverlap.create(intervals).value!!, id = "variable-no-overlap")
        return model
    }
}
