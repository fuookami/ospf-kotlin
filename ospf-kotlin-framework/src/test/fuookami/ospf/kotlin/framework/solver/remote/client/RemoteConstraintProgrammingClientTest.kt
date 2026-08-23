@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.solver.remote.client

import java.security.MessageDigest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalVariable
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingInfeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolverOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingUnknownOutput
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.report.BoundSide
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.ObjectStoragePort
import fuookami.ospf.kotlin.framework.solver.remote.port.SolverExecutionPort

class RemoteConstraintProgrammingClientTest {
    @Test
    fun legacyCpArtifactFixtureRemainsReadableWithoutModernProofUpgrade() {
        val fixture = checkNotNull(javaClass.getResource("/fixtures/remote-cp-result-v1.json"))
            .readText()
        val solution = Json {
            ignoreUnknownKeys = false
        }.decodeFromString(SerializedSolution.serializer(), fixture)

        assertEquals("1.0", solution.schemaVersion)
        assertEquals(true, solution.feasible)
        assertEquals(true, solution.optimal)
        assertEquals(Flt64(12.5), solution.objectiveValue)
        assertEquals(null, solution.problemStatus)
        assertEquals(null, solution.terminationReason)
        assertEquals(null, solution.proofStatus)
        assertEquals(emptyMap(), solution.variableValuesById)
    }

    @Test
    fun preservesFractionalBestBoundAndRejectsOverflow() = runBlocking {
        val model = ConstraintProgrammingModel("remote-client-best-bound-model")
        val snapshot = model.snapshot().value!!
        val fractional = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = Flt64(0.125),
                    elapsed = Duration.ZERO,
                    statistics = mapOf("bestBound" to "0.5", "gap" to "0.125")
                )
            )
        ).solveOutput(snapshot)
        val output = assertIs<ConstraintProgrammingFeasibleOutput>(assertIs<Ok<*, *, *>>(fractional).value)
        assertEquals(Flt64(0.5), output.bestBound)
        assertEquals(Flt64(0.5), output.report!!.statistics.bestBound)
        assertEquals(Flt64(0.125), output.report!!.statistics.gap)

        val overflow = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    statistics = mapOf("bestBound" to "1e1000")
                )
            )
        ).solveOutput(snapshot)
        assertIs<Failed<*, *, *>>(overflow)
        model.close()
    }

    @Test
    fun roundTripsDefaultColonDelimitedVariableBoundDiagnostics() = runBlocking {
        val model = ConstraintProgrammingModel("remote-client-diagnostics-model")
        val variable = IntVar("x")
        model.registerVariable(variable, IntegerDomain.interval(0, 1).value!!)
        val snapshot = model.snapshot().value!!
        val storage = RecordingObjectStoragePort()
        val variableId = "${variable.identifier}:${variable.index}"
        val diagnostics = mapOf(
            "infeasibility.source" to "ConstraintConflict",
            "infeasibility.variableBoundRefs" to "[\"$variableId:Lower\"]",
            "infeasibility.members" to "[\"bound:$variableId:Lower\"]"
        )
        val serialized = SerializedSolution.infeasible().copy(
            problemStatus = RemoteProblemStatus.INFEASIBLE,
            solutionPresence = RemoteSolutionPresence.NONE,
            proofStatus = RemoteProofStatus.CLAIMED,
            terminationReason = RemoteTerminationReason.COMPLETED,
            diagnostics = diagnostics
        )
        val artifactDigest = digest(serialized)
        val resultRef = storeSolution(storage, "results/cp-diagnostics", serialized.copy(artifactDigest = artifactDigest))
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = false,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    problemStatus = RemoteProblemStatus.INFEASIBLE,
                    terminationReason = RemoteTerminationReason.COMPLETED,
                    solutionPresence = RemoteSolutionPresence.NONE,
                    proofStatus = RemoteProofStatus.CLAIMED,
                    diagnostics = diagnostics,
                    resultRef = resultRef,
                    artifactDigest = artifactDigest
                )
            ),
            resultStoragePort = storage
        ).solveOutput(snapshot)
        val output = assertIs<ConstraintProgrammingInfeasibleOutput>(assertIs<Ok<*, *, *>>(result).value)
        val evidence = output.report!!.diagnostics.infeasibilityEvidence!!
        assertEquals(setOf(VariableId(variableId)), evidence.variableBoundIds)
        assertEquals(setOf(BoundSide.Lower), evidence.variableBoundRefs.map { it.side }.toSet())
        model.close()
    }

    @Test
    fun preservesUnknownArtifactProvenance() = runBlocking {
        val model = ConstraintProgrammingModel("remote-client-unknown-model")
        val snapshot = model.snapshot().value!!
        val storage = RecordingObjectStoragePort()
        val serialized = SerializedSolution(
            feasible = false,
            optimal = false,
            problemStatus = RemoteProblemStatus.UNKNOWN,
            solutionPresence = RemoteSolutionPresence.NONE,
            proofStatus = RemoteProofStatus.NONE,
            terminationReason = RemoteTerminationReason.TIME_LIMIT,
            provenance = mapOf(
                "solverId" to "scip-cp",
                "backend" to "SCIP",
                "backendVersion" to "9.2.4",
                "pluginVersion" to "1.1.0",
                "nativeVersion" to "9.2.4-native"
            )
        )
        val artifactDigest = digest(serialized)
        val resultRef = storeSolution(storage, "results/cp-unknown", serialized.copy(artifactDigest = artifactDigest))
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = false,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    problemStatus = RemoteProblemStatus.UNKNOWN,
                    terminationReason = RemoteTerminationReason.TIME_LIMIT,
                    solutionPresence = RemoteSolutionPresence.NONE,
                    proofStatus = RemoteProofStatus.NONE,
                    provenance = serialized.provenance,
                    resultRef = resultRef,
                    artifactDigest = artifactDigest
                )
            ),
            resultStoragePort = storage
        ).solveOutput(snapshot)
        val output = assertIs<ConstraintProgrammingUnknownOutput>(assertIs<Ok<*, *, *>>(result).value)
        assertEquals("scip-cp", output.report!!.provenance!!.descriptor.solverId)
        assertEquals("SCIP", output.report!!.provenance!!.descriptor.backendName)
        assertEquals("9.2.4", output.report!!.provenance!!.descriptor.backendVersion)
        assertEquals("1.1.0", output.report!!.provenance!!.descriptor.pluginVersion)
        assertEquals("9.2.4-native", output.report!!.provenance!!.nativeVersion)
        model.close()
    }

    @Test
    fun rejectsStableSolutionWithCorruptedArtifactDigest() {
        runBlocking {
            val model = ConstraintProgrammingModel("remote-client-digest-model")
            val snapshot = model.snapshot().value!!
            val resultRef = ObjectRef.of(path = "results/cp-digest")
            val storage = RecordingObjectStoragePort()
            storage.objects[resultRef.path] = Json.encodeToString(
                SerializedSolution(
                    feasible = true,
                    optimal = false,
                    artifactDigest = "corrupted"
                )
            ).encodeToByteArray()
            val port = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    resultRef = resultRef,
                    artifactDigest = "corrupted"
                )
            )

            try {
                val result = RemoteConstraintProgrammingClient(
                    executionPort = port,
                    resultStoragePort = storage
                ).solveOutput(snapshot)
                assertIs<Failed<*, *, *>>(result)
            } finally {
                model.close()
            }
        }
    }

    @Test
    fun rejectsMissingArtifactDigestWhenRawResultDeclaresOne() {
        runBlocking {
            val model = ConstraintProgrammingModel("remote-client-missing-digest-model")
            val snapshot = model.snapshot().value!!
            val resultRef = ObjectRef.of(path = "results/cp-missing-digest")
            val storage = RecordingObjectStoragePort()
            storage.objects[resultRef.path] = Json.encodeToString(
                SerializedSolution(
                    feasible = true,
                    optimal = false
                )
            ).encodeToByteArray()
            val port = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    resultRef = resultRef,
                    artifactDigest = "declared"
                )
            )

            try {
                val result = RemoteConstraintProgrammingClient(
                    executionPort = port,
                    resultStoragePort = storage
                ).solveOutput(snapshot)
                assertIs<Failed<*, *, *>>(result)
            } finally {
                model.close()
            }
        }
    }

    @Test
    fun rejectsMissingArtifactDigestWhenRawResultAlsoOmitsOne() {
        runBlocking {
            val model = ConstraintProgrammingModel("remote-client-missing-both-digests-model")
            val snapshot = model.snapshot().value!!
            val resultRef = ObjectRef.of(path = "results/cp-missing-both-digests")
            val storage = RecordingObjectStoragePort()
            storage.objects[resultRef.path] = Json.encodeToString(
                SerializedSolution(
                    feasible = true,
                    optimal = false
                )
            ).encodeToByteArray()
            val port = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    resultRef = resultRef
                )
            )

            try {
                val result = RemoteConstraintProgrammingClient(
                    executionPort = port,
                    resultStoragePort = storage
                ).solveOutput(snapshot)
                assertIs<Failed<*, *, *>>(result)
            } finally {
                model.close()
            }
        }
    }

    @Test
    fun submitsCpSnapshotThroughSharedExecutionPort() = runBlocking {
        val port = RecordingExecutionPort()
        val client = RemoteConstraintProgrammingClient(executionPort = port)
        val model = ConstraintProgrammingModel("remote-client-cp")
        model.registerVariable(BinVar("remote-client-x"))

        val result = client.solve(
            snapshot = model.snapshot().value!!,
            quantum = 250.milliseconds
        )

        assertIs<Ok<*, *, *>>(result)
        assertEquals(NormalizedModelType.CP, port.payload!!.modelData.modelType)
        assertEquals("cp", port.payload!!.taskMeta.targetType?.value)
        assertEquals(null, port.payload!!.config?.timeLimit)
    }

    @Test
    fun submitsConfiguredCpSnapshotWithoutMixingQuantumIntoSolverConfig() = runBlocking {
        val port = RecordingExecutionPort()
        val client = RemoteConstraintProgrammingClient(
            executionPort = port,
            runtimeConfig = RemoteSolverRuntimeConfig(
                solverConfig = SolverConfig(
                    timeLimit = 750.milliseconds,
                    solutionLimit = 3,
                    mipGapTolerance = Flt64(0.125),
                    threads = 2,
                    solverParams = mapOf("presolve" to "aggressive")
                )
            )
        )
        val model = ConstraintProgrammingModel("remote-client-configured-cp")
        model.registerVariable(BinVar("configured-x"))

        val result = client.solve(
            snapshot = model.snapshot().value!!,
            quantum = 125.milliseconds
        )

        assertIs<Ok<*, *, *>>(result)
        assertEquals(750.milliseconds, port.payload!!.config!!.timeLimit)
        assertEquals(3, port.payload!!.config!!.solutionLimit)
        assertEquals(Flt64(0.125), port.payload!!.config!!.mipGapTolerance)
        assertEquals(2, port.payload!!.config!!.threads)
        assertEquals(mapOf("presolve" to "aggressive"), port.payload!!.config!!.solverParams)
        model.close()
    }

    @Test
    fun materializesStableInt64SolutionWithoutFlt64PrecisionLoss() = runBlocking {
        val value = IntVar("remote-client-int64")
        val model = ConstraintProgrammingModel("remote-client-int64-model")
        model.registerVariable(value, fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain.interval(
            0L,
            9_007_199_254_740_993L
        ).value!!)
        val exactObjective = 9_007_199_254_740_993L
        model.minimize(
            ConstraintProgrammingExpression.Constant(Int64(exactObjective))
        )
        val snapshot = model.snapshot().value!!
        val resultRef = ObjectRef.of(path = "results/cp-int64")
        val storage = RecordingObjectStoragePort()
        val serialized = SerializedSolution(
            feasible = true,
            optimal = true,
            objectiveValue = null,
            objectiveValueInt64 = exactObjective,
            gap = Flt64.zero,
            variableValuesById = mapOf(
                "${value.identifier}:${value.index}" to 9_007_199_254_740_993L
            )
        )
        val artifactDigest = digest(serialized)
        storage.objects[resultRef.path] = Json { encodeDefaults = true }.encodeToString(
            SerializedSolution.serializer(),
            serialized.copy(artifactDigest = artifactDigest)
        ).encodeToByteArray()
        val port = RecordingExecutionPort(
            finalResult = SolveResult(
                    feasible = true,
                    optimal = true,
                    objectiveValue = null,
                    objectiveValueInt64 = exactObjective,
                    gap = Flt64.zero,
                elapsed = Duration.ZERO,
                proofStatus = RemoteProofStatus.CLAIMED,
                resultRef = resultRef,
                artifactDigest = artifactDigest
            )
        )
        try {
            val client = RemoteConstraintProgrammingClient(
                executionPort = port,
                resultStoragePort = storage
            )
            val materialized = client.solveOutput(snapshot)
            val output = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(materialized).value
            )
            assertEquals(
                Int64(9_007_199_254_740_993L),
                output.solution.value(VariableId("${value.identifier}:${value.index}")).value
            )
            assertEquals(
                Int64(exactObjective),
                output.report!!.solution!!.objective
            )
            assertEquals(null, output.objective)
        } finally {
            model.close()
        }
    }

    @Test
    fun rejectsExactObjectiveDuplicatedInFloatingPointField() = runBlocking {
        val value = IntVar("remote-client-exact-objective-duplicate")
        val model = ConstraintProgrammingModel("remote-client-exact-objective-duplicate-model")
        model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
        model.minimize(ConstraintProgrammingExpression.Variable(value))
        val exactObjective = 9_007_199_254_740_993L
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = Flt64(exactObjective.toDouble()),
                    objectiveValueInt64 = exactObjective,
                    gap = null,
                    elapsed = Duration.ZERO
                )
            )
        ).solveOutput(model.snapshot().value!!)
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(result).error.code)
        model.close()
    }

    @Test
    fun materializesStableIntervalSolution() = runBlocking {
        val start = IntVar("remote-client-start")
        val end = IntVar("remote-client-end")
        val model = ConstraintProgrammingModel("remote-client-interval-model")
        model.registerVariable(start, IntegerDomain.interval(0, 10).value!!)
        model.registerVariable(end, IntegerDomain.interval(0, 20).value!!)
        model.registerInterval(
            IntervalVariable(
                id = IntervalId("remote-job"),
                start = ConstraintProgrammingExpression.Variable(start),
                size = ConstraintProgrammingExpression.Constant(Int64(5)),
                end = ConstraintProgrammingExpression.Variable(end)
            )
        )
        val snapshot = model.snapshot().value!!
        val resultRef = ObjectRef.of(path = "results/cp-interval")
        val storage = RecordingObjectStoragePort()
        val serialized = SerializedSolution(
            feasible = true,
            optimal = true,
            objectiveValue = Flt64.zero,
            gap = Flt64.zero,
            variableValuesById = mapOf(
                "${start.identifier}:${start.index}" to 3L,
                "${end.identifier}:${end.index}" to 8L
            ),
            intervalValues = mapOf(
                "remote-job" to SerializedIntervalValue(start = 3L, size = 5L, end = 8L)
            )
        )
        val artifactDigest = digest(serialized)
        storage.objects[resultRef.path] = Json { encodeDefaults = true }.encodeToString(
            SerializedSolution.serializer(),
            serialized.copy(artifactDigest = artifactDigest)
        ).encodeToByteArray()
        val port = RecordingExecutionPort(
            finalResult = SolveResult(
                feasible = true,
                optimal = true,
                objectiveValue = Flt64.zero,
                gap = Flt64.zero,
                elapsed = Duration.ZERO,
                proofStatus = RemoteProofStatus.CLAIMED,
                resultRef = resultRef,
                artifactDigest = artifactDigest
            )
        )
        try {
            val client = RemoteConstraintProgrammingClient(
                executionPort = port,
                resultStoragePort = storage
            )
            val materialized = client.solveOutput(snapshot)
            val output = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(materialized).value
            )
            assertEquals(Int64(3), output.solution.interval(IntervalId("remote-job")).value!!.start)
            assertEquals(Int64(8), output.solution.interval(IntervalId("remote-job")).value!!.end)
        } finally {
            model.close()
        }
    }

    @Test
    fun rejectsStableSolutionThatViolatesOrdinaryConstraint() = runBlocking {
        val value = IntVar("remote-client-constrained")
        val model = ConstraintProgrammingModel("remote-client-constraint-model")
        model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
        model.addConstraint(
            constraint = ConstraintProgrammingConstraint.equal(
                ConstraintProgrammingExpression.Variable(value),
                Int64.one
            ).value!!,
            id = "must-one"
        )
        val snapshot = model.snapshot().value!!
        val storage = RecordingObjectStoragePort()
        val resultRef = storeSolution(
            storage = storage,
            path = "results/cp-constraint-violation",
            solution = SerializedSolution(
                feasible = true,
                optimal = false,
                variableValuesById = mapOf("${value.identifier}:${value.index}" to 0L)
            )
        )
        val port = RecordingExecutionPort(
            finalResult = SolveResult(
                feasible = true,
                optimal = false,
                objectiveValue = null,
                gap = null,
                elapsed = Duration.ZERO,
                resultRef = resultRef
            )
        )
        try {
            val failure = assertIs<Failed<*, *, *>>(
                RemoteConstraintProgrammingClient(
                    executionPort = port,
                    resultStoragePort = storage
                ).solveOutput(snapshot)
            )
            assertEquals(ErrorCode.ORSolutionInvalid, failure.error.code)
        } finally {
            model.close()
        }
    }

    @Test
    fun rejectsStableSolutionOutsideVariableDomain() = runBlocking {
        val value = IntVar("remote-client-domain")
        val model = ConstraintProgrammingModel("remote-client-domain-model")
        model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
        val snapshot = model.snapshot().value!!
        val storage = RecordingObjectStoragePort()
        val resultRef = storeSolution(
            storage = storage,
            path = "results/cp-domain-violation",
            solution = SerializedSolution(
                feasible = true,
                optimal = false,
                variableValuesById = mapOf("${value.identifier}:${value.index}" to 2L)
            )
        )
        val port = RecordingExecutionPort(
            finalResult = SolveResult(
                feasible = true,
                optimal = false,
                objectiveValue = null,
                gap = null,
                elapsed = Duration.ZERO,
                resultRef = resultRef
            )
        )
        try {
            val failure = assertIs<Failed<*, *, *>>(
                RemoteConstraintProgrammingClient(
                    executionPort = port,
                    resultStoragePort = storage
                ).solveOutput(snapshot)
            )
            assertEquals(ErrorCode.ORSolutionInvalid, failure.error.code)
        } finally {
            model.close()
        }
    }

    @Test
    fun rejectsInconsistentStableIntervalDefinition() = runBlocking {
        val start = IntVar("remote-client-bad-start")
        val end = IntVar("remote-client-bad-end")
        val model = ConstraintProgrammingModel("remote-client-bad-interval-model")
        model.registerVariable(start, IntegerDomain.interval(0, 10).value!!)
        model.registerVariable(end, IntegerDomain.interval(0, 20).value!!)
        model.registerInterval(
            IntervalVariable(
                id = IntervalId("remote-bad-job"),
                start = ConstraintProgrammingExpression.Variable(start),
                size = ConstraintProgrammingExpression.Constant(Int64(5)),
                end = ConstraintProgrammingExpression.Variable(end)
            )
        )
        val snapshot = model.snapshot().value!!
        val storage = RecordingObjectStoragePort()
        val resultRef = storeSolution(
            storage = storage,
            path = "results/cp-bad-interval",
            solution = SerializedSolution(
                feasible = true,
                optimal = false,
                variableValuesById = mapOf(
                    "${start.identifier}:${start.index}" to 3L,
                    "${end.identifier}:${end.index}" to 8L
                ),
                intervalValues = mapOf(
                    "remote-bad-job" to SerializedIntervalValue(start = 3L, size = 5L, end = 9L)
                )
            )
        )
        val port = RecordingExecutionPort(
            finalResult = SolveResult(
                feasible = true,
                optimal = false,
                objectiveValue = null,
                gap = null,
                elapsed = Duration.ZERO,
                resultRef = resultRef
            )
        )
        try {
            val failure = assertIs<Failed<*, *, *>>(
                RemoteConstraintProgrammingClient(
                    executionPort = port,
                    resultStoragePort = storage
                ).solveOutput(snapshot)
            )
            assertEquals(ErrorCode.ORSolutionInvalid, failure.error.code)
        } finally {
            model.close()
        }
    }

    @Test
    fun rejectsMissingAndUnknownStableIds() = runBlocking {
        val value = IntVar("remote-client-id-check")
        val model = ConstraintProgrammingModel("remote-client-id-model")
        model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
        val snapshot = model.snapshot().value!!
        val storage = RecordingObjectStoragePort()
        val missingRef = storeSolution(
            storage = storage,
            path = "results/cp-missing-id",
            solution = SerializedSolution(feasible = true, optimal = false)
        )
        val missingResult = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    resultRef = missingRef
                )
            ),
            resultStoragePort = storage
        ).solveOutput(snapshot)
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(missingResult).error.code)

        val unknownRef = storeSolution(
            storage = storage,
            path = "results/cp-unknown-id",
            solution = SerializedSolution(
                feasible = true,
                optimal = false,
                variableValuesById = mapOf("unknown-variable" to 0L)
            )
        )
        val unknownResult = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    resultRef = unknownRef
                )
            ),
            resultStoragePort = storage
        ).solveOutput(snapshot)
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(unknownResult).error.code)
        model.close()
    }

    @Test
    fun rejectsCorruptStableResultReference() = runBlocking {
        val value = IntVar("remote-client-corrupt-result")
        val model = ConstraintProgrammingModel("remote-client-corrupt-result-model")
        model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
        val resultRef = ObjectRef.of(path = "results/cp-corrupt")
        val storage = RecordingObjectStoragePort()
        storage.objects[resultRef.path] = "not-json".encodeToByteArray()
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    resultRef = resultRef
                )
            ),
            resultStoragePort = storage
        ).solveOutput(model.snapshot().value!!)
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(result).error.code)
        model.close()
    }

    @Test
    fun rejectsRawAndStableOptimalityStatusConflict() = runBlocking {
        val value = IntVar("remote-client-status")
        val model = ConstraintProgrammingModel("remote-client-status-model")
        model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
        val storage = RecordingObjectStoragePort()
        val resultRef = storeSolution(
            storage = storage,
            path = "results/cp-status-conflict",
            solution = SerializedSolution(
                feasible = true,
                optimal = true,
                problemStatus = RemoteProblemStatus.FEASIBLE,
                solutionPresence = RemoteSolutionPresence.OPTIMAL,
                variableValuesById = mapOf("${value.identifier}:${value.index}" to 0L)
            )
        )
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    resultRef = resultRef
                )
            ),
            resultStoragePort = storage
        ).solveOutput(model.snapshot().value!!)
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(result).error.code)
        model.close()
    }

    @Test
    fun rejectsSerializedProblemStatusConflict() = runBlocking {
        val value = IntVar("remote-client-problem-status")
        val model = ConstraintProgrammingModel("remote-client-problem-status-model")
        model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
        val storage = RecordingObjectStoragePort()
        val resultRef = storeSolution(
            storage = storage,
            path = "results/cp-problem-status-conflict",
            solution = SerializedSolution(
                feasible = true,
                optimal = false,
                problemStatus = RemoteProblemStatus.UNKNOWN,
                variableValuesById = mapOf("${value.identifier}:${value.index}" to 0L)
            )
        )
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    resultRef = resultRef
                )
            ),
            resultStoragePort = storage
        ).solveOutput(model.snapshot().value!!)
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(result).error.code)
        model.close()
    }

    @Test
    fun rejectsCommaSeparatedDiagnosticListsWithoutStructuredJson() = runBlocking {
        val value = IntVar("remote-client-diagnostic-list")
        val model = ConstraintProgrammingModel("remote-client-diagnostic-list-model")
        model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
        model.addConstraint(
            ConstraintProgrammingConstraint.equal(
                ConstraintProgrammingExpression.Variable(value),
                Int64.one
            ).value!!,
            id = "must-one"
        )
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = false,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    problemStatus = RemoteProblemStatus.UNKNOWN,
                    terminationReason = RemoteTerminationReason.TIME_LIMIT,
                    solutionPresence = RemoteSolutionPresence.NONE,
                    diagnostics = mapOf(
                        "infeasibility.source" to "ConstraintConflict",
                        "infeasibility.constraintIds" to "must-one,unknown"
                    )
                )
            )
        ).solveOutput(model.snapshot().value!!)
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(result).error.code)
        model.close()
    }

    @Test
    fun rejectsStrictResultWithoutRequestFingerprints() = runBlocking {
        val model = ConstraintProgrammingModel("remote-client-strict-binding-model")
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    schemaVersion = "2.0",
                    problemStatus = RemoteProblemStatus.FEASIBLE,
                    terminationReason = RemoteTerminationReason.COMPLETED,
                    solutionPresence = RemoteSolutionPresence.INCUMBENT,
                    runId = "task",
                    attemptId = "slice"
                )
            )
        ).solveOutput(
            snapshot = model.snapshot().value!!,
            taskId = TaskId.of("task"),
            sliceId = SliceId.of("slice")
        )
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(result).error.code)
        model.close()
    }

    @Test
    fun rejectsStrictDiagnosticsWithoutSourceOrWithInvalidIssueEntry() = runBlocking {
        val model = ConstraintProgrammingModel("remote-client-strict-diagnostic-model")
        val missingSourceDiagnostics = mapOf(
            "infeasibility.constraintIds" to "[]",
            "warning.0.code" to "warning-code",
            "warning.0.category" to "Backend",
            "warning.0.message" to "warning"
        )
        val missingSource = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = false,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    schemaVersion = "2.0",
                    problemStatus = RemoteProblemStatus.UNKNOWN,
                    terminationReason = RemoteTerminationReason.BACKEND_FAILURE,
                    solutionPresence = RemoteSolutionPresence.NONE,
                    diagnostics = missingSourceDiagnostics,
                    runId = "task",
                    attemptId = "slice"
                )
            )
        ).solveOutput(
            snapshot = model.snapshot().value!!,
            taskId = TaskId.of("task"),
            sliceId = SliceId.of("slice")
        )
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(missingSource).error.code)

        val invalidIssue = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = false,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    schemaVersion = "2.0",
                    problemStatus = RemoteProblemStatus.UNKNOWN,
                    terminationReason = RemoteTerminationReason.BACKEND_FAILURE,
                    solutionPresence = RemoteSolutionPresence.NONE,
                    diagnostics = missingSourceDiagnostics +
                        ("infeasibility.source" to "None") +
                        ("warning.0.category" to "not-a-category"),
                    runId = "task",
                    attemptId = "slice"
                )
            )
        ).solveOutput(
            snapshot = model.snapshot().value!!,
            taskId = TaskId.of("task"),
            sliceId = SliceId.of("slice")
        )
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(invalidIssue).error.code)
        model.close()
    }

    @Test
    fun rejectsSerializedSolutionPresenceConflict() = runBlocking {
        val value = IntVar("remote-client-solution-presence")
        val model = ConstraintProgrammingModel("remote-client-solution-presence-model")
        model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
        val storage = RecordingObjectStoragePort()
        val resultRef = storeSolution(
            storage = storage,
            path = "results/cp-solution-presence-conflict",
            solution = SerializedSolution(
                feasible = true,
                optimal = false,
                problemStatus = RemoteProblemStatus.FEASIBLE,
                solutionPresence = RemoteSolutionPresence.NONE,
                variableValuesById = mapOf("${value.identifier}:${value.index}" to 0L)
            )
        )
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    resultRef = resultRef
                )
            ),
            resultStoragePort = storage
        ).solveOutput(model.snapshot().value!!)
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(result).error.code)
        model.close()
    }

    @Test
    fun rejectsRawAndStableObjectiveOrGapConflict() = runBlocking {
        val value = IntVar("remote-client-objective")
        val model = ConstraintProgrammingModel("remote-client-objective-model")
        model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
        val storage = RecordingObjectStoragePort()
        val resultRef = storeSolution(
            storage = storage,
            path = "results/cp-objective-conflict",
            solution = SerializedSolution(
                feasible = true,
                optimal = false,
                objectiveValue = Flt64.one,
                gap = Flt64.one,
                variableValuesById = mapOf("${value.identifier}:${value.index}" to 0L)
            )
        )
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = Flt64.zero,
                    gap = Flt64.zero,
                    elapsed = Duration.ZERO,
                    resultRef = resultRef
                )
            ),
            resultStoragePort = storage
        ).solveOutput(model.snapshot().value!!)
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(result).error.code)
        model.close()
    }

    @Test
    fun rejectsObjectiveThatDisagreesWithSnapshotExpression() = runBlocking {
        val value = IntVar("remote-client-objective-expression")
        val model = ConstraintProgrammingModel("remote-client-objective-expression-model")
        model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
        model.minimize(ConstraintProgrammingExpression.Variable(value))
        val snapshot = model.snapshot().value!!
        val storage = RecordingObjectStoragePort()
        val resultRef = storeSolution(
            storage = storage,
            path = "results/cp-objective-expression-conflict",
            solution = SerializedSolution(
                feasible = true,
                optimal = false,
                objectiveValue = Flt64.zero,
                variableValuesById = mapOf("${value.identifier}:${value.index}" to 1L)
            )
        )
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = Flt64.zero,
                    gap = null,
                    elapsed = Duration.ZERO,
                    resultRef = resultRef
                )
            ),
            resultStoragePort = storage
        ).solveOutput(snapshot)
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(result).error.code)
        model.close()
    }

    @Test
    fun rejectsMissingObjectiveForSnapshot() = runBlocking {
        val value = IntVar("remote-client-missing-objective")
        val model = ConstraintProgrammingModel("remote-client-missing-objective-model")
        model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
        model.minimize(ConstraintProgrammingExpression.Variable(value))
        val snapshot = model.snapshot().value!!
        val storage = RecordingObjectStoragePort()
        val resultRef = storeSolution(
            storage = storage,
            path = "results/cp-missing-objective",
            solution = SerializedSolution(
                feasible = true,
                optimal = false,
                variableValuesById = mapOf("${value.identifier}:${value.index}" to 1L)
            )
        )
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    resultRef = resultRef
                )
            ),
            resultStoragePort = storage
        ).solveOutput(snapshot)
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(result).error.code)
        model.close()
    }

    @Test
    fun rejectsRawAndStableGapConflict() = runBlocking {
        val value = IntVar("remote-client-gap")
        val model = ConstraintProgrammingModel("remote-client-gap-model")
        model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
        val storage = RecordingObjectStoragePort()
        val resultRef = storeSolution(
            storage = storage,
            path = "results/cp-gap-conflict",
            solution = SerializedSolution(
                feasible = true,
                optimal = false,
                objectiveValue = Flt64.zero,
                gap = Flt64.one,
                variableValuesById = mapOf("${value.identifier}:${value.index}" to 0L)
            )
        )
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = Flt64.zero,
                    gap = Flt64.zero,
                    elapsed = Duration.ZERO,
                    resultRef = resultRef
                )
            ),
            resultStoragePort = storage
        ).solveOutput(model.snapshot().value!!)
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(result).error.code)
        model.close()
    }

    @Test
    fun rejectsMissingStableResultObject() = runBlocking {
        val value = IntVar("remote-client-missing-result")
        val model = ConstraintProgrammingModel("remote-client-missing-result-model")
        model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    resultRef = ObjectRef.of(path = "results/cp-missing-result")
                )
            ),
            resultStoragePort = RecordingObjectStoragePort()
        ).solveOutput(model.snapshot().value!!)
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(result).error.code)
        model.close()
    }

    @Test
    fun marksCompletedInfeasibleResultAsClaimedOnly() = runBlocking {
        val model = ConstraintProgrammingModel("remote-client-infeasible-model")
        val result = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = false,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    problemStatus = RemoteProblemStatus.INFEASIBLE,
                    terminationReason = RemoteTerminationReason.COMPLETED,
                    solutionPresence = RemoteSolutionPresence.NONE
                )
            )
        ).solveOutput(model.snapshot().value!!)
        val output = assertIs<ConstraintProgrammingInfeasibleOutput>(
            assertIs<Ok<*, *, *>>(result).value
        )
        assertEquals(fuookami.ospf.kotlin.core.solver.report.ProofStatus.Claimed, output.proofStatus)
        model.close()
    }

    @Test
    fun preservesRemoteTerminalReasonMatrixWithoutUpgradingFailureStates() = runBlocking {
        val model = ConstraintProgrammingModel("remote-client-terminal-matrix")
        val snapshot = model.snapshot().value!!
        val limited = listOf(
            RemoteTerminationReason.TIME_LIMIT to TerminationReason.TimeLimit,
            RemoteTerminationReason.NODE_LIMIT to TerminationReason.NodeLimit,
            RemoteTerminationReason.SOLUTION_LIMIT to TerminationReason.SolutionLimit,
            RemoteTerminationReason.CANCELLED to TerminationReason.Cancelled,
            RemoteTerminationReason.INTERRUPTED to TerminationReason.Interrupted,
            RemoteTerminationReason.BACKEND_FAILURE to TerminationReason.BackendFailure
        )
        for ((remoteReason, expectedReason) in limited) {
            val result = RemoteConstraintProgrammingClient(
                executionPort = RecordingExecutionPort(
                    finalResult = SolveResult(
                        feasible = false,
                        optimal = false,
                        objectiveValue = null,
                        gap = null,
                        elapsed = Duration.ZERO,
                        problemStatus = RemoteProblemStatus.UNKNOWN,
                        terminationReason = remoteReason,
                        solutionPresence = RemoteSolutionPresence.NONE,
                        proofStatus = RemoteProofStatus.NONE
                    )
                )
            ).solveOutput(snapshot)
            val output = assertIs<ConstraintProgrammingUnknownOutput>(assertIs<Ok<*, *, *>>(result).value)
            assertEquals(expectedReason, output.terminationReason)
            assertEquals(ProblemStatus.Unknown, output.report!!.problemStatus)
        }

        val incumbent = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = true,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    problemStatus = RemoteProblemStatus.FEASIBLE,
                    terminationReason = RemoteTerminationReason.SOLUTION_LIMIT,
                    solutionPresence = RemoteSolutionPresence.INCUMBENT,
                    proofStatus = RemoteProofStatus.NONE
                )
            )
        ).solveOutput(snapshot)
        val feasible = assertIs<ConstraintProgrammingFeasibleOutput>(assertIs<Ok<*, *, *>>(incumbent).value)
        assertEquals(SolverStatus.Feasible, feasible.status)
        assertEquals(TerminationReason.SolutionLimit, feasible.report!!.terminationReason)
        assertEquals(SolutionPresence.Incumbent, feasible.report!!.solutionPresence)
        model.close()
    }

    @Test
    fun localAndRemoteTerminalSemanticsRemainEquivalentAcrossCompleteMatrix() = runBlocking {
        val model = ConstraintProgrammingModel("remote-client-complete-terminal-matrix")
        val snapshot = model.snapshot().value!!
        data class TerminalCase(
            val name: String,
            val problemStatus: RemoteProblemStatus,
            val terminationReason: RemoteTerminationReason,
            val presence: RemoteSolutionPresence,
            val proof: RemoteProofStatus,
            val feasible: Boolean,
            val expectedProblemStatus: ProblemStatus,
            val expectedTerminationReason: TerminationReason,
            val expectedPresence: SolutionPresence
        )
        val cases = listOf(
            TerminalCase(
                "optimal", RemoteProblemStatus.FEASIBLE, RemoteTerminationReason.COMPLETED,
                RemoteSolutionPresence.OPTIMAL, RemoteProofStatus.VERIFIED, true,
                ProblemStatus.Feasible, TerminationReason.Completed, SolutionPresence.Optimal
            ),
            TerminalCase(
                "feasible", RemoteProblemStatus.FEASIBLE, RemoteTerminationReason.SOLUTION_LIMIT,
                RemoteSolutionPresence.INCUMBENT, RemoteProofStatus.NONE, true,
                ProblemStatus.Feasible, TerminationReason.SolutionLimit, SolutionPresence.Incumbent
            ),
            TerminalCase(
                "infeasible", RemoteProblemStatus.INFEASIBLE, RemoteTerminationReason.COMPLETED,
                RemoteSolutionPresence.NONE, RemoteProofStatus.CLAIMED, false,
                ProblemStatus.Infeasible, TerminationReason.Completed, SolutionPresence.None
            ),
            TerminalCase(
                "unknown", RemoteProblemStatus.UNKNOWN, RemoteTerminationReason.INTERRUPTED,
                RemoteSolutionPresence.NONE, RemoteProofStatus.NONE, false,
                ProblemStatus.Unknown, TerminationReason.Interrupted, SolutionPresence.None
            ),
            TerminalCase(
                "timeout", RemoteProblemStatus.UNKNOWN, RemoteTerminationReason.TIME_LIMIT,
                RemoteSolutionPresence.NONE, RemoteProofStatus.NONE, false,
                ProblemStatus.Unknown, TerminationReason.TimeLimit, SolutionPresence.None
            ),
            TerminalCase(
                "cancelled", RemoteProblemStatus.UNKNOWN, RemoteTerminationReason.CANCELLED,
                RemoteSolutionPresence.NONE, RemoteProofStatus.NONE, false,
                ProblemStatus.Unknown, TerminationReason.Cancelled, SolutionPresence.None
            ),
            TerminalCase(
                "backend-failure", RemoteProblemStatus.UNKNOWN, RemoteTerminationReason.BACKEND_FAILURE,
                RemoteSolutionPresence.NONE, RemoteProofStatus.NONE, false,
                ProblemStatus.Unknown, TerminationReason.BackendFailure, SolutionPresence.None
            )
        )
        for (case in cases) {
            val result = RemoteConstraintProgrammingClient(
                executionPort = RecordingExecutionPort(
                    finalResult = SolveResult(
                        feasible = case.feasible,
                        optimal = case.presence == RemoteSolutionPresence.OPTIMAL,
                        objectiveValue = null,
                        gap = null,
                        elapsed = Duration.ZERO,
                        problemStatus = case.problemStatus,
                        terminationReason = case.terminationReason,
                        solutionPresence = case.presence,
                        proofStatus = case.proof
                    )
                )
            ).solveOutput(snapshot)
            val output = assertIs<ConstraintProgrammingSolverOutput>(assertIs<Ok<*, *, *>>(result).value)
            assertEquals(case.expectedProblemStatus, output.report?.problemStatus, case.name)
            assertEquals(case.expectedTerminationReason, output.report?.terminationReason, case.name)
            assertEquals(case.expectedPresence, output.report?.solutionPresence, case.name)
            when (case.expectedProblemStatus) {
                ProblemStatus.Feasible -> assertIs<ConstraintProgrammingFeasibleOutput>(output)
                ProblemStatus.Infeasible -> assertIs<ConstraintProgrammingInfeasibleOutput>(output)
                else -> assertIs<ConstraintProgrammingUnknownOutput>(output)
            }
        }

        val diagnosticFailure = RemoteConstraintProgrammingClient(
            executionPort = RecordingExecutionPort(
                finalResult = SolveResult(
                    feasible = false,
                    optimal = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = Duration.ZERO,
                    problemStatus = RemoteProblemStatus.UNKNOWN,
                    terminationReason = RemoteTerminationReason.BACKEND_FAILURE,
                    solutionPresence = RemoteSolutionPresence.NONE,
                    proofStatus = RemoteProofStatus.NONE,
                    diagnostics = mapOf("infeasibility.source" to "unknown-source")
                )
            )
        ).solveOutput(snapshot)
        assertEquals(ErrorCode.ORSolutionInvalid, assertIs<Failed<*, *, *>>(diagnosticFailure).error.code)
        model.close()
    }

    private fun storeSolution(
        storage: RecordingObjectStoragePort,
        path: String,
        solution: SerializedSolution
    ): ObjectRef {
        val ref = ObjectRef.of(path = path)
        storage.objects[ref.path] = Json.encodeToString(solution).encodeToByteArray()
        return ref
    }

    private fun digest(solution: SerializedSolution): String {
        val canonical = Json { encodeDefaults = true }.encodeToString(
            SerializedSolution.serializer(),
            solution.copy(artifactDigest = null)
        )
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private class RecordingExecutionPort(
        private val finalResult: SolveResult? = null
    ) : SolverExecutionPort {
        var payload: SolvePayload? = null
        private val handle = ExecutionHandle(
            handleId = HandleId.of("remote-cp-handle"),
            taskId = TaskId.of("remote-cp-task"),
            sliceId = SliceId.of("remote-cp-slice"),
            nodeId = NodeId.of("remote-cp-node"),
            startedAt = Instant.fromEpochMilliseconds(0L)
        )

        override suspend fun start(
            payload: SolvePayload,
            taskId: TaskId,
            sliceId: SliceId,
            nodeId: NodeId,
            tenantId: TenantId
        ): Ret<ExecutionHandle> {
            this.payload = payload
            return Ok(handle)
        }

        override suspend fun resume(
            payload: SolvePayload,
            checkpoint: ObjectRef,
            taskId: TaskId,
            sliceId: SliceId,
            nodeId: NodeId,
            tenantId: TenantId
        ): Ret<ExecutionHandle> = start(payload, taskId, sliceId, nodeId, tenantId)

        override suspend fun awaitSliceEnd(handle: ExecutionHandle, quantum: Duration): Ret<SliceResult> {
            return Ok(
                SliceResult(
                    sliceId = handle.sliceId,
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64.zero,
                    gap = Flt64.zero,
                    elapsed = Duration.ZERO
                )
            )
        }

        override suspend fun exportCheckpoint(handle: ExecutionHandle): Ret<ObjectRef?> = Ok(null)

        override suspend fun fetchFinalResult(handle: ExecutionHandle): Ret<SolveResult?> {
            return Ok(finalResult ?: SolveResult(
                feasible = true,
                optimal = true,
                objectiveValue = Flt64.zero,
                gap = Flt64.zero,
                elapsed = Duration.ZERO
            ))
        }

        override suspend fun stop(handle: ExecutionHandle): Ret<Boolean> = Ok(true)
    }

    private class RecordingObjectStoragePort : ObjectStoragePort {
        val objects = mutableMapOf<ObjectPath, ByteArray>()

        override suspend fun put(
            path: ObjectPath,
            bytes: ByteArray,
            metadata: Map<String, String>
        ): ObjectRef {
            objects[path] = bytes
            return ObjectRef(path = path)
        }

        override suspend fun get(ref: ObjectRef): ByteArray? {
            return objects[ref.path]
        }

        override suspend fun delete(ref: ObjectRef): Boolean {
            return objects.remove(ref.path) != null
        }

        override suspend fun exists(ref: ObjectRef): Boolean {
            return objects.containsKey(ref.path)
        }
    }
}
