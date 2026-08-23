package fuookami.ospf.kotlin.framework.solver

import java.security.MessageDigest
import kotlin.time.Duration.Companion.ZERO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSession
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingCheckpointCodec
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolveOptions
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.FakeConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.toSolveReport
import fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingBendersState
import fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingCut
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingConflict
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolverOutput
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.report.EvidenceValidity
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.SolveProof
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.IntVar

class LogicBasedBendersTest {
    @Test
    fun bendersCheckpointRoundTripsCutsThroughVersionedPrimitiveSpi() {
        val cut = BendersMasterCut(
            inequality = constantCut(),
            kind = BendersCutKind.Optimality,
            validity = BendersCutValidity.Global,
            proofStatus = ProofStatus.Verified,
            source = "fixture-cut"
        )
        val report = LogicBasedBendersReport(
            problemStatus = ProblemStatus.Feasible,
            terminationReason = TerminationReason.Completed,
            proof = SolveProof(status = ProofStatus.Verified),
            cuts = listOf(cut)
        )
        val serializer = object : BendersCutSerializationSpi {
            override val schemaVersion: String = "fixture-1"

            override fun encode(cut: BendersMasterCut): Ret<PortableConstraintProgrammingCut> {
                return ok(
                    PortableConstraintProgrammingCut(
                        id = cut.source,
                        schemaVersion = schemaVersion,
                        validity = cut.validity.name,
                        provenance = mapOf("kind" to cut.kind.name),
                        payload = mapOf("source" to cut.source)
                    )
                )
            }

            override fun decode(cut: PortableConstraintProgrammingCut): Ret<BendersMasterCut> {
                return ok(
                    BendersMasterCut(
                        inequality = constantCut(),
                        kind = BendersCutKind.valueOf(cut.provenance.getValue("kind")),
                        validity = BendersCutValidity.valueOf(cut.validity),
                        proofStatus = ProofStatus.Verified,
                        source = cut.payload.getValue("source")
                    )
                )
            }
        }

        val capturedResult = report.toPortableBendersState(
            serializer = serializer,
            masterFingerprint = "master-fixture"
        )
        assertTrue(capturedResult.ok)
        val captured = capturedResult.value!!
        assertEquals(listOf("fixture-cut"), captured.cuts.map { it.id })
        val restoredResult = BendersCheckpointCodec.restore(captured, serializer)
        assertTrue(restoredResult.ok)
        val restored = restoredResult.value!!
        assertEquals(1, restored.cuts.size)
        assertEquals("fixture-cut", restored.cuts.single().source)

        val checkpointModel = ConstraintProgrammingModel("benders-checkpoint")
        val capturedCheckpoint = ConstraintProgrammingCheckpointCodec.capture(
            snapshot = checkpointModel.snapshot().value!!,
            descriptor = FakeConstraintProgrammingSolver().descriptor,
            checkpointId = "benders-checkpoint",
            createdAtEpochMs = 1L,
            benders = captured
        )
        assertTrue(capturedCheckpoint.ok)
        val encodedCheckpoint = ConstraintProgrammingCheckpointCodec.encode(capturedCheckpoint.value!!)
        assertTrue(encodedCheckpoint.ok)
        val decodedCheckpoint = ConstraintProgrammingCheckpointCodec.decodeCompatible(encodedCheckpoint.value!!)
        assertTrue(decodedCheckpoint.ok)
        val restoredCheckpoint = ConstraintProgrammingCheckpointCodec.restore(
            envelope = decodedCheckpoint.value!!,
            snapshot = checkpointModel.snapshot().value!!,
            expectedConfigurationFingerprint = decodedCheckpoint.value!!.configurationFingerprint,
            expectedSolverFingerprint = decodedCheckpoint.value!!.solverFingerprint,
            allowLegacyConfigurationFingerprint = true
        )
        assertTrue(restoredCheckpoint.ok)
        assertEquals(captured, restoredCheckpoint.value!!.envelope.benders)
        assertTrue(
            BendersCheckpointCodec.restore(
                restoredCheckpoint.value!!.envelope.benders!!,
                serializer
            ).ok
        )

        val persisted = BendersCheckpointCodec.captureEncoded(
            report = report,
            serializer = serializer,
            masterFingerprint = "master-fixture"
        )
        assertTrue(persisted.ok)
        val restoredPersisted = BendersCheckpointCodec.restoreEncoded(
            persisted.value!!,
            serializer,
            proofMode = BendersProofMode.Exact
        )
        assertTrue(restoredPersisted.ok)
        assertEquals("fixture-cut", restoredPersisted.value!!.cuts.single().source)

        assertTrue(
            BendersCheckpointCodec.decode(
                persisted.value!!.replace("\"integritySha256\":\"", "\"integritySha256\":\"corrupted")
            ).failed
        )
        assertTrue(
            BendersCheckpointCodec.decode(
                persisted.value!!.replace("\"schemaVersion\":\"1.0\"", "\"schemaVersion\":\"9.0\"")
            ).failed
        )
    }

    @Test
    fun publishedSchemaOneWithoutMasterFingerprintStillVerifiesItsLegacyDigest() {
        val state = PortableConstraintProgrammingBendersState(iteration = 0L)
        val encoded = assertIs<Ok<String, *, *>>(BendersCheckpointCodec.encode(state)).value
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }
        val root = json.parseToJsonElement(encoded).jsonObject
        val legacyState = JsonObject(root.getValue("state").jsonObject.toMutableMap().apply {
            remove("masterFingerprint")
        })
        val unsigned = JsonObject(root.toMutableMap().apply {
            put("state", legacyState)
            put("integritySha256", JsonPrimitive(""))
        })
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(unsigned.toString().toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
        val legacy = JsonObject(unsigned.toMutableMap().apply {
            put("integritySha256", JsonPrimitive(digest))
        }).toString()

        val decoded = BendersCheckpointCodec.decode(legacy)
        assertTrue(decoded.ok)
        assertEquals(null, decoded.value!!.masterFingerprint)
    }

    @Test
    fun bendersCaptureRejectsMasterStateWithoutFingerprint() {
        val report = LogicBasedBendersReport(
            problemStatus = ProblemStatus.Feasible,
            terminationReason = TerminationReason.Completed,
            proof = SolveProof(status = ProofStatus.Verified),
            masterOutput = output(
                value = Flt64.zero,
                obj = Flt64.one,
                bestBound = Flt64.one
            )
        )
        val serializer = object : BendersCutSerializationSpi {
            override val schemaVersion: String = "fingerprint-gate"

            override fun encode(cut: BendersMasterCut): Ret<PortableConstraintProgrammingCut> =
                ok(PortableConstraintProgrammingCut(cut.source, schemaVersion, cut.validity.name))

            override fun decode(cut: PortableConstraintProgrammingCut): Ret<BendersMasterCut> =
                Failed(ErrorCode.IllegalArgument, "unused")
        }

        assertTrue(BendersCheckpointCodec.capture(report, serializer).failed)
    }

    @Test
    fun bendersCheckpointDocumentRejectsDuplicatePrimitiveCutIds() {
        val state = PortableConstraintProgrammingBendersState(
            iteration = 1L,
            cuts = listOf(
                PortableConstraintProgrammingCut(
                    id = "duplicate",
                    schemaVersion = "fixture-1",
                    validity = BendersCutValidity.Global.name
                ),
                PortableConstraintProgrammingCut(
                    id = "duplicate",
                    schemaVersion = "fixture-1",
                    validity = BendersCutValidity.Global.name
                )
            )
        )
        assertTrue(BendersCheckpointCodec.encode(state).failed)
    }

    @Test
    fun bendersCheckpointCarriesFixedBindingsAssumptionsAndConflictEvidence() {
        val masterVariable = BinVar("master-evidence")
        val subproblemVariable = BinVar("subproblem-evidence")
        val assignment = BendersSubproblemAssignment(
            masterValues = mapOf("master" to Flt64.one),
            masterVariables = mapOf("master" to masterVariable),
            fixedValues = mapOf(VariableId("cp:stable") to Int64.one),
            assumptions = listOf(BooleanLiteral(subproblemVariable, id = VariableId("cp:stable"))),
            assumptionToMaster = mapOf("cp:stable" to "master")
        )
        val conflict = ConstraintProgrammingConflict(
            constraintIds = setOf(fuookami.ospf.kotlin.core.solver.report.ConstraintId("cp-constraint")),
            assumptions = listOf(BooleanLiteral(subproblemVariable, id = VariableId("cp:stable"))),
            members = setOf(
                fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember.Constraint(
                    fuookami.ospf.kotlin.core.solver.report.ConstraintId("cp-constraint")
                )
            ),
            validity = EvidenceValidity.Verified
        )
        val report = LogicBasedBendersReport(
            problemStatus = ProblemStatus.Unknown,
            terminationReason = TerminationReason.IterationLimit,
            proof = SolveProof(status = ProofStatus.None),
            assignment = assignment,
            subproblemResult = InfeasibleSubproblemResult(assignment, conflict, ProofStatus.Verified)
        )
        val serializer = object : BendersCutSerializationSpi {
            override val schemaVersion: String = "evidence-1"

            override fun encode(cut: BendersMasterCut): Ret<PortableConstraintProgrammingCut> =
                ok(PortableConstraintProgrammingCut(cut.source, schemaVersion, cut.validity.name))

            override fun decode(cut: PortableConstraintProgrammingCut): Ret<BendersMasterCut> =
                Failed(ErrorCode.IllegalArgument, "unused")
        }

        val captured = assertIs<Ok<PortableConstraintProgrammingBendersState, *, *>>(
            report.toPortableBendersState(
                serializer = serializer,
                masterFingerprint = "master-evidence"
            )
        ).value
        assertEquals(listOf("cp:stable"), captured.assumptions)
        assertEquals(mapOf("cp:stable" to 1L), captured.fixedBindings)
        assertEquals(listOf("constraint:cp-constraint"), captured.conflicts.single().memberIds)
        assertEquals(listOf("cp:stable"), captured.conflicts.single().assumptionIds)

        val restored = assertIs<Ok<BendersResumeState, *, *>>(
            BendersCheckpointCodec.restore(captured, serializer, BendersProofMode.Heuristic)
        ).value
        assertEquals(captured.assumptions, restored.assumptions)
        assertEquals(captured.fixedBindings, restored.fixedBindings)
        assertEquals(captured.conflicts, restored.conflicts)
    }

    @Test
    fun bendersBindingUsesExplicitStableSubproblemIdAcrossRebuiltModels() {
        val masterVariable = BinVar("stable-master")
        val firstSubproblemVariable = BinVar("first-instance")
        val secondSubproblemVariable = BinVar("second-instance")
        val first = ConstraintProgrammingModel("stable-first")
        val second = ConstraintProgrammingModel("stable-second")
        first.registerVariable(VariableId("cp:stable"), firstSubproblemVariable, IntegerDomain.boolean)
        second.registerVariable(VariableId("cp:stable"), secondSubproblemVariable, IntegerDomain.boolean)
        val binding = BinaryBendersVariableBinding(
            listOf(BinaryBendersVariable("master", masterVariable, firstSubproblemVariable, "cp:stable"))
        )
        val firstAssignment = assertIs<Ok<BendersSubproblemAssignment, *, *>>(
            binding.bind(ConstraintProgrammingValueSource.of(mapOf("master" to Flt64.one)), first)
        ).value
        val rebuiltBinding = BinaryBendersVariableBinding(
            listOf(BinaryBendersVariable("master", masterVariable, secondSubproblemVariable, "cp:stable"))
        )
        val secondAssignment = assertIs<Ok<BendersSubproblemAssignment, *, *>>(
            rebuiltBinding.bind(ConstraintProgrammingValueSource.of(mapOf("master" to Flt64.one)), second)
        ).value
        assertEquals(setOf(VariableId("cp:stable")), firstAssignment.fixedValues.keys)
        assertEquals(firstAssignment.fixedValues, secondAssignment.fixedValues)
        assertNotEquals(firstSubproblemVariable.identifier, secondSubproblemVariable.identifier)
    }

    @Test
    fun bendersCheckpointRejectsChangedSubproblemModelFingerprintAfterRebuild() = runBlocking {
        val firstVariable = BinVar("first-instance")
        val secondVariable = BinVar("second-instance")
        val first = ConstraintProgrammingModel("fingerprinted-subproblem")
        val second = ConstraintProgrammingModel("fingerprinted-subproblem")
        first.registerVariable(
            id = VariableId("cp:stable"),
            variable = firstVariable,
            domain = IntegerDomain.boolean,
            scope = "stable",
            origin = "fixture/subproblem-v1"
        )
        second.registerVariable(
            id = VariableId("cp:stable"),
            variable = secondVariable,
            domain = IntegerDomain.boolean,
            scope = "stable",
            origin = "fixture/subproblem-v2"
        )
        val serializer = object : BendersCutSerializationSpi {
            override val schemaVersion: String = "fingerprint-1"

            override fun encode(cut: BendersMasterCut): Ret<PortableConstraintProgrammingCut> {
                return Failed(ErrorCode.Other, "no cuts in fingerprint fixture")
            }

            override fun decode(cut: PortableConstraintProgrammingCut): Ret<BendersMasterCut> {
                return Failed(ErrorCode.Other, "no cuts in fingerprint fixture")
            }
        }
        val report = LogicBasedBendersReport(
            problemStatus = ProblemStatus.Unknown,
            terminationReason = TerminationReason.IterationLimit,
            proof = SolveProof(status = ProofStatus.None)
        )
        try {
            val firstSnapshot = first.snapshot().value!!
            val secondSnapshot = second.snapshot().value!!
            val captured = assertIs<Ok<PortableConstraintProgrammingBendersState, *, *>>(
                report.toPortableBendersState(serializer, firstSnapshot)
            ).value
            assertTrue(!captured.subproblemModelFingerprint.isNullOrBlank())
            assertTrue(
                BendersCheckpointCodec.restore(
                    captured,
                    serializer,
                    expectedSubproblemSnapshot = firstSnapshot
                ).ok
            )
            assertTrue(
                BendersCheckpointCodec.restore(
                    captured,
                    serializer,
                    expectedSubproblemSnapshot = secondSnapshot
                ).failed
            )
            val restored = assertIs<Ok<BendersResumeState, *, *>>(
                BendersCheckpointCodec.restore(
                    captured,
                    serializer,
                    expectedSubproblemSnapshot = firstSnapshot
                )
            ).value
            val (master, masterVariable) = master()
            val engine = LogicBasedBendersEngine(
                masterSolver = BendersMasterProblemSolver { ok(output(Flt64.zero)) },
                subproblemSolver = FakeConstraintProgrammingSolver(),
                binding = binding(masterVariable, secondVariable),
                options = options(masterVariable).copy(resumeState = restored)
            )
            try {
                assertTrue(engine.solve(master, second).failed)
            } finally {
                master.close()
            }
        } finally {
            first.close()
            second.close()
        }
    }

    @Test
    fun resumedBendersStateSeedsMasterAndContinuesFromNextIteration() = runBlocking {
        val (master, masterVariable) = master()
        val subproblemVariable = BinVar("cp-resume-x")
        val subproblem = ConstraintProgrammingModel("resume-subproblem")
        subproblem.registerVariable(subproblemVariable)
        val cut = BendersMasterCut(
            inequality = LinearInequality(
                lhs = LinearPolynomial(
                    listOf(LinearMonomial(Flt64.one, masterVariable)),
                    Flt64.zero
                ),
                rhs = LinearPolynomial(emptyList(), Flt64.one),
                comparison = Comparison.GE
            ),
            kind = BendersCutKind.Optimality,
            validity = BendersCutValidity.Global,
            proofStatus = ProofStatus.Verified,
            source = "resume-cut"
        )
        val serializer = object : BendersCutSerializationSpi {
            override val schemaVersion: String = "resume-1"

            override fun encode(cut: BendersMasterCut): Ret<PortableConstraintProgrammingCut> {
                return ok(
                    PortableConstraintProgrammingCut(
                        id = cut.source,
                        schemaVersion = schemaVersion,
                        validity = cut.validity.name,
                        provenance = mapOf("kind" to cut.kind.name),
                        payload = mapOf("source" to cut.source)
                    )
                )
            }

            override fun decode(cut: PortableConstraintProgrammingCut): Ret<BendersMasterCut> {
                return ok(
                    BendersMasterCut(
                        inequality = LinearInequality(
                            lhs = LinearPolynomial(
                                listOf(LinearMonomial(Flt64.one, masterVariable)),
                                Flt64.zero
                            ),
                            rhs = LinearPolynomial(emptyList(), Flt64.one),
                            comparison = Comparison.GE
                        ),
                        kind = BendersCutKind.valueOf(cut.provenance.getValue("kind")),
                        validity = BendersCutValidity.valueOf(cut.validity),
                        proofStatus = ProofStatus.Verified,
                        source = cut.payload.getValue("source")
                    )
                )
            }
        }
        val state = BendersCheckpointCodec.restore(
            PortableConstraintProgrammingBendersState(
                iteration = 1L,
                cuts = listOf(
                    PortableConstraintProgrammingCut(
                        id = "resume-cut",
                        schemaVersion = "resume-1",
                        validity = BendersCutValidity.Global.name,
                        provenance = mapOf("kind" to BendersCutKind.Optimality.name),
                        payload = mapOf("source" to "resume-cut")
                    )
                )
            ),
            serializer
        )
        assertTrue(state.ok)
        val engine = LogicBasedBendersEngine(
            masterSolver = BendersMasterProblemSolver { model ->
                assertTrue(model.relationConstraints.isNotEmpty())
                ok(output(Flt64.one))
            },
            subproblemSolver = FakeConstraintProgrammingSolver(),
            binding = binding(masterVariable, subproblemVariable),
            options = options(masterVariable, maxIterations = 2).copy(
                resumeState = state.value
            )
        )

        val report = assertIs<Ok<LogicBasedBendersReport, *, *>>(engine.solve(master, subproblem)).value
        assertEquals(ProblemStatus.Feasible, report.problemStatus)
        assertEquals(ProofStatus.Verified, report.proof.status)
        assertEquals(1, report.cuts.size)
        assertEquals("resume-cut", report.cuts.single().source)
        assertTrue(report.iterations.all { it.iteration >= 1 })
    }

    @Test
    fun bendersResumeRejectsUnknownAssumptionAfterSubproblemRebuild() = runBlocking {
        val (master, masterVariable) = master()
        val subproblemVariable = BinVar("cp-rebuild-resume")
        val subproblem = ConstraintProgrammingModel("resume-rebuild-subproblem")
        subproblem.registerVariable(
            id = VariableId("cp:stable"),
            variable = subproblemVariable,
            domain = IntegerDomain.boolean
        )
        val state = BendersResumeState(
            iteration = 1,
            cuts = emptyList(),
            assumptions = listOf("cp:missing")
        )
        val engine = LogicBasedBendersEngine(
            masterSolver = BendersMasterProblemSolver { ok(output(Flt64.zero)) },
            subproblemSolver = FakeConstraintProgrammingSolver(),
            binding = BinaryBendersVariableBinding(
                listOf(
                    BinaryBendersVariable(
                        key = "x",
                        masterVariable = masterVariable,
                        subproblemVariable = subproblemVariable,
                        subproblemVariableId = "cp:stable"
                    )
                )
            ),
            options = options(masterVariable).copy(resumeState = state)
        )

        val result = engine.solve(master, subproblem)
        assertTrue(result.failed)
        master.close()
        subproblem.close()
    }

    @Test
    fun binaryNoGoodCutEncodesBothBinaryValues() {
        val masterVariable = BinVar("x")
        val subproblemVariable = BinVar("cp-x")
        val subproblem = ConstraintProgrammingModel("no-good")
        subproblem.registerVariable(subproblemVariable)
        val binding = BinaryBendersVariableBinding(
            listOf(BinaryBendersVariable("x", masterVariable, subproblemVariable))
        )
        val master = LinearMetaModel(name = "master", converter = IntoValue.Identity)
        master.add(masterVariable)
        val context = { assignment: BendersSubproblemAssignment ->
            BendersCutContext(master, subproblem, assignment, 0, BendersProofMode.Exact)
        }

        val zeroAssignment = assertIs<Ok<BendersSubproblemAssignment, *, *>>(
            binding.bind(ConstraintProgrammingValueSource.of(mapOf("x" to Flt64.zero)), subproblem)
        ).value
        val zeroCut = assertIs<Ok<List<BendersMasterCut>, *, *>>(
            BinaryNoGoodCutOracle().feasibilityCuts(InfeasibleSubproblemResult(zeroAssignment, null), context(zeroAssignment))
        ).value.single()
        assertEquals(BendersCutKind.NoGood, zeroCut.kind)
        assertEquals(Comparison.GE, zeroCut.inequality.comparison)
        assertEquals(Flt64.zero, zeroCut.inequality.lhs.constant)
        assertEquals(Flt64.one, zeroCut.inequality.lhs.monomials.single().coefficient)

        val oneAssignment = assertIs<Ok<BendersSubproblemAssignment, *, *>>(
            binding.bind(ConstraintProgrammingValueSource.of(mapOf("x" to Flt64.one)), subproblem)
        ).value
        val oneCut = assertIs<Ok<List<BendersMasterCut>, *, *>>(
            BinaryNoGoodCutOracle().feasibilityCuts(InfeasibleSubproblemResult(oneAssignment, null), context(oneAssignment))
        ).value.single()
        assertEquals(Flt64.one, oneCut.inequality.lhs.constant)
        assertEquals(-Flt64.one, oneCut.inequality.lhs.monomials.single().coefficient)
    }

    @Test
    fun conflictCoreCutUsesOnlyProjectedMasterVariables() {
        val masterX = BinVar("x")
        val masterY = BinVar("y")
        val subproblemX = BinVar("cp-x")
        val subproblemY = BinVar("cp-y")
        val subproblem = ConstraintProgrammingModel("conflict")
        subproblem.registerVariable(subproblemX)
        subproblem.registerVariable(subproblemY)
        val binding = BinaryBendersVariableBinding(
            listOf(
                BinaryBendersVariable("x", masterX, subproblemX),
                BinaryBendersVariable("y", masterY, subproblemY)
            )
        )
        val assignment = assertIs<Ok<BendersSubproblemAssignment, *, *>>(
            binding.bind(
                ConstraintProgrammingValueSource.of(mapOf("x" to Flt64.zero, "y" to Flt64.one)),
                subproblem
            )
        ).value
        val conflict = ConstraintProgrammingConflict(
            assumptions = listOf(BooleanLiteral(subproblemX, negated = true))
        )
        val master = LinearMetaModel(name = "master", converter = IntoValue.Identity)
        master.add(masterX)
        master.add(masterY)
        val result = BinaryNoGoodCutOracle().feasibilityCuts(
            InfeasibleSubproblemResult(assignment, conflict),
            BendersCutContext(master, subproblem, assignment, 0, BendersProofMode.Exact)
        )
        val cut = assertIs<Ok<List<BendersMasterCut>, *, *>>(result).value.single()
        assertEquals(BendersCutKind.Conflict, cut.kind)
        assertEquals(1, cut.inequality.lhs.monomials.size)
        assertEquals(masterX, cut.inequality.lhs.monomials.single().symbol)
    }

    @Test
    fun unverifiedConflictCoreFallsBackToCompleteAssignmentNoGood() {
        val masterX = BinVar("x")
        val masterY = BinVar("y")
        val subproblemX = BinVar("cp-x-unverified")
        val subproblemY = BinVar("cp-y-unverified")
        val subproblem = ConstraintProgrammingModel("unverified-conflict")
        subproblem.registerVariable(subproblemX)
        subproblem.registerVariable(subproblemY)
        val binding = BinaryBendersVariableBinding(
            listOf(
                BinaryBendersVariable("x", masterX, subproblemX),
                BinaryBendersVariable("y", masterY, subproblemY)
            )
        )
        val assignment = assertIs<Ok<BendersSubproblemAssignment, *, *>>(
            binding.bind(
                ConstraintProgrammingValueSource.of(mapOf("x" to Flt64.zero, "y" to Flt64.one)),
                subproblem
            )
        ).value
        val conflict = ConstraintProgrammingConflict(
            assumptions = listOf(BooleanLiteral(subproblemX, negated = true)),
            validity = EvidenceValidity.Unknown
        )
        val master = LinearMetaModel(name = "master-unverified", converter = IntoValue.Identity)
        master.add(masterX)
        master.add(masterY)
        val result = BinaryNoGoodCutOracle().feasibilityCuts(
            InfeasibleSubproblemResult(assignment, conflict),
            BendersCutContext(master, subproblem, assignment, 0, BendersProofMode.Exact)
        )
        val cut = assertIs<Ok<List<BendersMasterCut>, *, *>>(result).value.single()
        assertEquals(BendersCutKind.NoGood, cut.kind)
        assertEquals(2, cut.inequality.lhs.monomials.size)
    }

    @Test
    fun exactModeRejectsAssignmentValidityCut() = runBlocking {
        val (master, masterVariable) = master()
        val subproblemVariable = BinVar("cp-x")
        val subproblem = ConstraintProgrammingModel("assignment-cut")
        subproblem.registerVariable(subproblemVariable)
        val binding = binding(masterVariable, subproblemVariable)
        val oracle = object : BendersCutOracle {
            override fun feasibilityCuts(
                result: InfeasibleSubproblemResult,
                context: BendersCutContext
            ): Ret<List<BendersMasterCut>> = ok(emptyList())

            override fun optimalityCuts(
                result: FeasibleSubproblemResult,
                context: BendersCutContext
            ): Ret<List<BendersMasterCut>> = ok(
                listOf(
                    BendersMasterCut(
                        inequality = constantCut(),
                        kind = BendersCutKind.Heuristic,
                        validity = BendersCutValidity.Assignment,
                        proofStatus = ProofStatus.Verified,
                        source = "test-assignment"
                    )
                )
            )
        }
        val engine = LogicBasedBendersEngine(
            masterSolver = BendersMasterProblemSolver { ok(output(Flt64.zero)) },
            subproblemSolver = FakeConstraintProgrammingSolver(),
            binding = binding,
            cutOracle = oracle,
            options = options(masterVariable)
        )
        val result = engine.solve(master, subproblem)
        val failure = assertIs<fuookami.ospf.kotlin.utils.functional.Failed<*, *, *>>(result)
        assertEquals(fuookami.ospf.kotlin.utils.error.ErrorCode.IllegalArgument, failure.error.code)
    }

    @Test
    fun duplicateCutsAreAddedOnlyOnce() = runBlocking {
        val (master, masterVariable) = master()
        val subproblemVariable = BinVar("cp-x")
        val subproblem = constrainedSubproblem(subproblemVariable)
        val engine = LogicBasedBendersEngine(
            masterSolver = BendersMasterProblemSolver { ok(output(Flt64.zero)) },
            subproblemSolver = FakeConstraintProgrammingSolver(),
            binding = binding(masterVariable, subproblemVariable),
            options = options(masterVariable, maxIterations = 3, stallIterationLimit = 1)
        )
        val report = assertIs<Ok<LogicBasedBendersReport, *, *>>(engine.solve(master, subproblem)).value
        assertEquals(ProblemStatus.Unknown, report.problemStatus)
        assertEquals(1, report.cuts.size)
        assertEquals(1, report.iterations.size)
        assertEquals(BendersCutKind.Conflict, report.cuts.single().kind)
    }

    @Test
    fun unknownSubproblemCannotBeReportedAsOptimal() = runBlocking {
        val (master, masterVariable) = master()
        val subproblemVariable = BinVar("cp-x")
        val subproblem = ConstraintProgrammingModel("unknown")
        subproblem.registerVariable(subproblemVariable)
        val freeVariable = IntVar("cp-free")
        subproblem.registerVariable(freeVariable, IntegerDomain.interval(0, 2).value!!)
        val engine = LogicBasedBendersEngine(
            masterSolver = BendersMasterProblemSolver { ok(output(Flt64.zero)) },
            subproblemSolver = FakeConstraintProgrammingSolver(enumerationLimit = 1),
            binding = binding(masterVariable, subproblemVariable),
            options = options(masterVariable)
        )
        val report = assertIs<Ok<LogicBasedBendersReport, *, *>>(engine.solve(master, subproblem)).value
        assertEquals(ProblemStatus.Unknown, report.problemStatus)
        assertNotEquals(ProblemStatus.Feasible, report.problemStatus)
        assertEquals(ProofStatus.None, report.proof.status)
    }

    @Test
    fun exactModeDoesNotClaimOptimizationWithoutAnOptimalityCut() = runBlocking {
        val (master, masterVariable) = master()
        val subproblemVariable = BinVar("cp-x-objective")
        val subproblem = constrainedSubproblem(subproblemVariable)
        subproblem.minimize(ConstraintProgrammingExpression.Variable(subproblemVariable))
        val engine = LogicBasedBendersEngine(
            masterSolver = BendersMasterProblemSolver { ok(output(Flt64.one)) },
            subproblemSolver = FakeConstraintProgrammingSolver(),
            binding = binding(masterVariable, subproblemVariable),
            options = options(masterVariable)
        )

        val report = assertIs<Ok<LogicBasedBendersReport, *, *>>(engine.solve(master, subproblem)).value
        assertEquals(ProblemStatus.Feasible, report.problemStatus)
        assertEquals(ProofStatus.None, report.proof.status)
        assertEquals(TerminationReason.IterationLimit, report.terminationReason)
        assertEquals("benders-no-optimality-proof", report.diagnostics.single().code)
    }

    @Test
    fun exactModeRejectsMissingObjectiveFromOptimizingSubproblem() = runBlocking {
        val (master, masterVariable) = master()
        val subproblemVariable = BinVar("cp-x-missing-objective")
        val subproblem = constrainedSubproblem(subproblemVariable)
        subproblem.minimize(ConstraintProgrammingExpression.Variable(subproblemVariable))
        val engine = LogicBasedBendersEngine(
            masterSolver = BendersMasterProblemSolver { ok(output(Flt64.one)) },
            subproblemSolver = MissingObjectiveConstraintProgrammingSolver(),
            binding = binding(masterVariable, subproblemVariable),
            options = options(masterVariable)
        )

        val report = assertIs<Ok<LogicBasedBendersReport, *, *>>(engine.solve(master, subproblem)).value
        assertEquals(ProblemStatus.Unknown, report.problemStatus)
        assertEquals(ProofStatus.None, report.proof.status)
        assertEquals("benders-subproblem-objective-missing", report.diagnostics.single().code)
    }

    @Test
    fun exactModeDoesNotClaimConvergenceFromAWeakHistoricalOptimalityCut() = runBlocking {
        val (master, masterVariable) = master()
        val subproblemVariable = BinVar("cp-x-weak-cut")
        val subproblem = constrainedSubproblem(subproblemVariable)
        subproblem.minimize(ConstraintProgrammingExpression.Variable(subproblemVariable))
        val oracle = object : BendersCutOracle {
            override fun feasibilityCuts(
                result: InfeasibleSubproblemResult,
                context: BendersCutContext
            ): Ret<List<BendersMasterCut>> = ok(emptyList())

            override fun optimalityCuts(
                result: FeasibleSubproblemResult,
                context: BendersCutContext
            ): Ret<List<BendersMasterCut>> = ok(
                if (context.iteration == 0) {
                    listOf(
                        BendersMasterCut(
                            inequality = constantCut(),
                            kind = BendersCutKind.Optimality,
                            validity = BendersCutValidity.Global,
                            proofStatus = ProofStatus.Verified,
                            source = "verified-weak-optimality"
                        )
                    )
                } else {
                    emptyList()
                }
            )
        }
        var calls = 0
        val engine = LogicBasedBendersEngine(
            masterSolver = BendersMasterProblemSolver {
                ++calls
                ok(output(value = Flt64.one, obj = Flt64.one, bestBound = Flt64.zero))
            },
            subproblemSolver = FakeConstraintProgrammingSolver(),
            binding = binding(masterVariable, subproblemVariable),
            cutOracle = oracle,
            options = options(masterVariable, maxIterations = 3).copy(
                completeObjectiveEvaluator = { _, _, output -> ok(output.objective!!) }
            )
        )

        val report = assertIs<Ok<LogicBasedBendersReport, *, *>>(engine.solve(master, subproblem)).value
        assertEquals(2, calls)
        assertEquals(ProofStatus.None, report.proof.status)
        assertEquals("benders-no-convergence-proof", report.diagnostics.single().code)
        assertEquals(Flt64.one, report.iterations.last().convergenceGap)
    }

    @Test
    fun exactModeRequiresCompleteObjectiveContract() = runBlocking {
        val (master, masterVariable) = master()
        val subproblemVariable = BinVar("cp-x-objective-mismatch")
        val subproblem = constrainedSubproblem(subproblemVariable)
        subproblem.minimize(ConstraintProgrammingExpression.Variable(subproblemVariable))
        val oracle = object : BendersCutOracle {
            override fun feasibilityCuts(
                result: InfeasibleSubproblemResult,
                context: BendersCutContext
            ): Ret<List<BendersMasterCut>> = ok(emptyList())

            override fun optimalityCuts(
                result: FeasibleSubproblemResult,
                context: BendersCutContext
            ): Ret<List<BendersMasterCut>> = ok(
                if (context.iteration == 0) {
                    listOf(
                        BendersMasterCut(
                            inequality = constantCut(),
                            kind = BendersCutKind.Optimality,
                            validity = BendersCutValidity.Global,
                            proofStatus = ProofStatus.Verified,
                            source = "verified-but-incomplete"
                        )
                    )
                } else {
                    emptyList()
                }
            )
        }
        val engine = LogicBasedBendersEngine(
            masterSolver = BendersMasterProblemSolver {
                ok(output(value = Flt64.one, obj = Flt64.zero, bestBound = Flt64.zero))
            },
            subproblemSolver = FakeConstraintProgrammingSolver(),
            binding = binding(masterVariable, subproblemVariable),
            cutOracle = oracle,
            options = options(masterVariable, maxIterations = 3)
        )

        val report = assertIs<Ok<LogicBasedBendersReport, *, *>>(engine.solve(master, subproblem)).value
        assertEquals(ProblemStatus.Feasible, report.problemStatus)
        assertEquals(ProofStatus.None, report.proof.status)
        assertEquals("benders-objective-contract-missing", report.diagnostics.single().code)
    }

    @Test
    fun exactModeUsesCompleteObjectiveEvaluatorForFirstStageCost() = runBlocking {
        val (master, masterVariable) = master()
        val subproblemVariable = BinVar("cp-x-complete-objective")
        val subproblem = constrainedSubproblem(subproblemVariable)
        subproblem.minimize(ConstraintProgrammingExpression.Variable(subproblemVariable))
        val oracle = object : BendersCutOracle {
            override fun feasibilityCuts(
                result: InfeasibleSubproblemResult,
                context: BendersCutContext
            ): Ret<List<BendersMasterCut>> = ok(emptyList())

            override fun optimalityCuts(
                result: FeasibleSubproblemResult,
                context: BendersCutContext
            ): Ret<List<BendersMasterCut>> = ok(
                if (context.iteration == 0) {
                    listOf(
                        BendersMasterCut(
                            inequality = constantCut(),
                            kind = BendersCutKind.Optimality,
                            validity = BendersCutValidity.Global,
                            proofStatus = ProofStatus.Verified,
                            source = "verified-complete-objective"
                        )
                    )
                } else {
                    emptyList()
                }
            )
        }
        val engine = LogicBasedBendersEngine(
            masterSolver = BendersMasterProblemSolver {
                ok(output(value = Flt64.one, obj = Flt64(2.0), bestBound = Flt64(2.0)))
            },
            subproblemSolver = FakeConstraintProgrammingSolver(),
            binding = binding(masterVariable, subproblemVariable),
            cutOracle = oracle,
            options = options(masterVariable, maxIterations = 3).copy(
                completeObjectiveEvaluator = { _, _, output ->
                    ok(Flt64.one + output.objective!!)
                }
            )
        )

        val report = assertIs<Ok<LogicBasedBendersReport, *, *>>(engine.solve(master, subproblem)).value
        assertEquals(ProblemStatus.Feasible, report.problemStatus)
        assertEquals(ProofStatus.Verified, report.proof.status)
    }

    @Test
    fun exactModeReportsMismatchFromCompleteObjectiveEvaluator() = runBlocking {
        val (master, masterVariable) = master()
        val subproblemVariable = BinVar("cp-x-objective-mismatch")
        val subproblem = constrainedSubproblem(subproblemVariable)
        subproblem.minimize(ConstraintProgrammingExpression.Variable(subproblemVariable))
        val oracle = object : BendersCutOracle {
            override fun feasibilityCuts(
                result: InfeasibleSubproblemResult,
                context: BendersCutContext
            ): Ret<List<BendersMasterCut>> = ok(emptyList())

            override fun optimalityCuts(
                result: FeasibleSubproblemResult,
                context: BendersCutContext
            ): Ret<List<BendersMasterCut>> = ok(
                if (context.iteration == 0) {
                    listOf(
                        BendersMasterCut(
                            inequality = constantCut(),
                            kind = BendersCutKind.Optimality,
                            validity = BendersCutValidity.Global,
                            proofStatus = ProofStatus.Verified,
                            source = "verified-objective-mismatch"
                        )
                    )
                } else {
                    emptyList()
                }
            )
        }
        val engine = LogicBasedBendersEngine(
            masterSolver = BendersMasterProblemSolver {
                ok(output(value = Flt64.one, obj = Flt64.zero, bestBound = Flt64.zero))
            },
            subproblemSolver = FakeConstraintProgrammingSolver(),
            binding = binding(masterVariable, subproblemVariable),
            cutOracle = oracle,
            options = options(masterVariable, maxIterations = 3).copy(
                completeObjectiveEvaluator = { _, _, output ->
                    ok(Flt64.one + output.objective!!)
                }
            )
        )

        val report = assertIs<Ok<LogicBasedBendersReport, *, *>>(engine.solve(master, subproblem)).value
        assertEquals(ProblemStatus.Feasible, report.problemStatus)
        assertEquals(ProofStatus.None, report.proof.status)
        assertEquals("benders-objective-inconsistent", report.diagnostics.single().code)
    }

    @Test
    fun binaryMasterConvergesAfterConflictCut() = runBlocking {
        val (master, masterVariable) = master()
        val subproblemVariable = BinVar("cp-x")
        val subproblem = constrainedSubproblem(subproblemVariable)
        val engine = LogicBasedBendersEngine(
            masterSolver = BendersMasterProblemSolver { model ->
                if (model.relationConstraints.isEmpty()) {
                    ok(output(Flt64.zero))
                } else {
                    ok(output(Flt64.one))
                }
            },
            subproblemSolver = FakeConstraintProgrammingSolver(),
            binding = binding(masterVariable, subproblemVariable),
            options = options(masterVariable)
        )
        val report = assertIs<Ok<LogicBasedBendersReport, *, *>>(engine.solve(master, subproblem)).value
        assertEquals(ProblemStatus.Feasible, report.problemStatus)
        assertEquals(ProofStatus.Verified, report.proof.status)
        assertEquals(1, report.cuts.size)
        assertEquals(2, report.iterations.size)
        assertEquals(Flt64.one, report.masterOutput?.solution?.single())
    }

    @Test
    fun integerMasterUsesHardFixedValuesAndInstallsMultiConstraintNoGood() = runBlocking {
        val masterVariable = IntVar("integer-master")
        val master = LinearMetaModel(name = "integer-master", converter = IntoValue.Identity)
        master.add(masterVariable)
        val subproblemVariable = IntVar("integer-cp")
        val subproblem = ConstraintProgrammingModel("integer-subproblem")
        subproblem.registerVariable(subproblemVariable, IntegerDomain.interval(0, 1).value!!)
        val expression = ConstraintProgrammingExpression.Variable(subproblemVariable)
        subproblem.addConstraint(ConstraintProgrammingConstraint.equal(expression, Int64.one).value!!)
        val domain = IntegerDomain.interval(0, 1).value!!
        val binding = IntegerBendersVariableBinding(
            listOf(IntegerBendersVariable("x", masterVariable, subproblemVariable, domain))
        )
        val engine = LogicBasedBendersEngine(
            masterSolver = BendersMasterProblemSolver { model ->
                ok(output(if (model.relationConstraints.isEmpty()) Flt64.zero else Flt64.one))
            },
            subproblemSolver = FakeConstraintProgrammingSolver(),
            binding = binding,
            cutOracle = IntegerNoGoodCutOracle(
                listOf(IntegerBendersVariable("x", masterVariable, subproblemVariable, domain))
            ),
            options = LogicBasedBendersOptions(
                masterSolutionSource = { result ->
                    ok(
                        ConstraintProgrammingValueSource.of(
                            mapOf("x" to result.values[masterVariable.index])
                        )
                    )
                }
            )
        )

        val report = assertIs<Ok<LogicBasedBendersReport, *, *>>(engine.solve(master, subproblem)).value
        assertEquals(ProblemStatus.Feasible, report.problemStatus)
        assertEquals(ProofStatus.Verified, report.proof.status)
        assertEquals(1, report.cuts.size)
        assertTrue(report.cuts.single().additionalInequalities.isNotEmpty())
        assertTrue(report.cuts.single().auxiliaryVariables.isNotEmpty())
        assertEquals(Flt64.one, report.masterOutput?.solution?.single())
    }

    private fun master(): Pair<LinearMetaModel<Flt64>, BinVar> {
        val variable = BinVar("x")
        val model = LinearMetaModel(name = "master", converter = IntoValue.Identity)
        model.add(variable)
        return model to variable
    }

    private fun binding(
        masterVariable: BinVar,
        subproblemVariable: BinVar
    ): BendersVariableBinding {
        return BinaryBendersVariableBinding(
            listOf(BinaryBendersVariable("x", masterVariable, subproblemVariable))
        )
    }

    private fun constrainedSubproblem(variable: BinVar): ConstraintProgrammingModel {
        val model = ConstraintProgrammingModel("subproblem", ObjectCategory.Minimum)
        model.registerVariable(variable)
        val expression = ConstraintProgrammingExpression.Variable(variable)
        model.addConstraint(ConstraintProgrammingConstraint.equal(expression, Int64.one).value!!)
        return model
    }

    private fun output(
        value: Flt64,
        obj: Flt64 = Flt64.zero,
        bestBound: Flt64? = null
    ): SolveReport<Flt64> {
        return SolverStatus.Optimal.toSolveReport(
            objective = obj,
            values = listOf(value),
            solveTime = ZERO,
            bestBound = bestBound ?: Flt64.zero,
            gap = Flt64.zero
        )
    }

    private fun options(
        masterVariable: BinVar,
        maxIterations: Int = 100,
        stallIterationLimit: Int = 1
    ): LogicBasedBendersOptions {
        return LogicBasedBendersOptions(
            maxIterations = maxIterations,
            stallIterationLimit = stallIterationLimit,
            masterSolutionSource = { result ->
                ok(
                    ConstraintProgrammingValueSource.of(
                        mapOf("x" to result.values[masterVariable.index])
                    )
                )
            }
        )
    }

    private fun constantCut(): fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality<Flt64> {
        return fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality(
            lhs = LinearPolynomial<Flt64>(listOf(LinearMonomial(Flt64.zero, BinVar("unused"))), Flt64.zero),
            rhs = LinearPolynomial(emptyList(), Flt64.one),
            comparison = Comparison.LE
        )
    }
}

private class MissingObjectiveConstraintProgrammingSolver : ConstraintProgrammingSolver {
    private val delegate = FakeConstraintProgrammingSolver()

    override val descriptor
        get() = delegate.descriptor

    override suspend fun solve(
        model: ConstraintProgrammingModel,
        options: ConstraintProgrammingSolveOptions
    ): Ret<ConstraintProgrammingSolverOutput> {
        return delegate.solve(model, options).map(::withoutObjective)
    }

    override fun createSession(
        model: ConstraintProgrammingModel,
        options: ConstraintProgrammingSolveOptions
    ): Ret<ConstraintProgrammingSession> {
        return delegate.createSession(model, options).map { session ->
            object : ConstraintProgrammingSession {
                override val model: ConstraintProgrammingModel
                    get() = session.model
                override val options: ConstraintProgrammingSolveOptions
                    get() = session.options
                override val isClosed: Boolean
                    get() = session.isClosed

                override suspend fun solve(
                    assumptions: List<BooleanLiteral>,
                    fixedValues: Map<VariableId, Int64>,
                    hints: fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution?
                ): Ret<ConstraintProgrammingSolverOutput> {
                    return session.solve(assumptions, fixedValues, hints).map(::withoutObjective)
                }

                override fun close() {
                    session.close()
                }
            }
        }
    }

    private fun withoutObjective(output: ConstraintProgrammingSolverOutput): ConstraintProgrammingSolverOutput {
        return if (output is ConstraintProgrammingFeasibleOutput) {
            output.copy(objective = null, exactObjective = null)
        } else {
            output
        }
    }
}
