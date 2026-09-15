package fuookami.ospf.kotlin.core.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.BoundSide
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.SolverCapabilities
import fuookami.ospf.kotlin.core.solver.report.SolverDescriptor
import fuookami.ospf.kotlin.core.solver.report.SolverModelType
import fuookami.ospf.kotlin.core.solver.report.VariableBoundRef
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64

class AnalysisProtocolTest {
    @Test
    fun analysisStatusKeepsUnknownAndUnsupportedDistinct() {
        assertEquals(AnalysisStatus.Reachable, AnalysisStatus.from(ProblemStatus.Feasible))
        assertEquals(AnalysisStatus.Unreachable, AnalysisStatus.from(ProblemStatus.Infeasible))
        assertEquals(AnalysisStatus.Unknown, AnalysisStatus.from(ProblemStatus.Unknown))
        assertFalse(AnalysisStatus.Unknown.isProven)
        assertFalse(AnalysisStatus.Unsupported.isProven)
    }

    @Test
    fun objectiveTargetsHaveStableIdentityAndDirectionalSemantics() {
        val objectiveId = ObjectiveId("payload")
        val lower = ObjectiveTarget.AtLeast(objectiveId, Flt64(10))
        val upper = ObjectiveTarget.AtMost(objectiveId, Flt64(20))

        assertTrue(lower.isSatisfied(Flt64(10)))
        assertFalse(lower.isSatisfied(Flt64(9)))
        assertTrue(upper.isSatisfied(Flt64(20)))
        assertFalse(upper.isSatisfied(Flt64(21)))
        assertEquals(Flt64(2), lower.signedDistance(Flt64(12)))
        assertEquals("objective-target:payload:at-least:10.0", lower.stableId)
        assertFailsWith<IllegalArgumentException> {
            ObjectiveTarget.AtLeast(ObjectiveId(""), Flt64(1))
        }
    }

    @Test
    fun diagnosticSourcesOnlyProjectToOriginalEvidence() {
        val constraint = DiagnosticSource.Constraint(ConstraintId("capacity"))
        val lower = DiagnosticSource.VariableLowerBound(VariableId("x"))
        val upper = DiagnosticSource.VariableUpperBound(VariableBoundRef(VariableId("x"), BoundSide.Upper))
        val domain = DiagnosticSource.SparseDomain(VariableId("x"))
        val target = DiagnosticSource.ObjectiveTarget(
            ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(11))
        )

        assertEquals("constraint:capacity", constraint.stableId)
        assertEquals("variable:x:lower", lower.stableId)
        assertEquals("variable:x:upper", upper.stableId)
        assertEquals("variable:x:domain", domain.stableId)
        assertTrue(constraint.asInfeasibilityMember() is InfeasibilityMember.Constraint)
        assertTrue(lower.asInfeasibilityMember() is InfeasibilityMember.VariableBound)
        assertTrue(domain.asInfeasibilityMember() is InfeasibilityMember.VariableDomain)
        assertNull(target.asInfeasibilityMember())
        assertFailsWith<IllegalArgumentException> {
            DiagnosticSource.VariableLowerBound(
                VariableBoundRef(VariableId("x"), BoundSide.Upper)
            )
        }
    }

    @Test
    fun capabilityMatrixMapsExistingSolverCapabilities() {
        val descriptor = SolverDescriptor(
            solverId = "test",
            backendName = "test-backend",
            capabilities = SolverCapabilities(
                modelTypes = setOf(SolverModelType.MIP),
                dual = true,
                warmStart = true,
                constraintProgrammingFeatures = mapOf(
                    ConstraintProgrammingFeature.Assumption to ConstraintProgrammingSupportLevel.Native,
                    ConstraintProgrammingFeature.ConflictCore to ConstraintProgrammingSupportLevel.Native,
                    ConstraintProgrammingFeature.AllDifferent to ConstraintProgrammingSupportLevel.ExactLowering
                )
            )
        )
        val matrix = CapabilityMatrix.from(descriptor)

        assertTrue(matrix.supports(AnalysisCapability.Activity))
        assertTrue(matrix.supports(AnalysisCapability.FixedIntegerLpSensitivity))
        assertTrue(matrix.supports(AnalysisCapability.AssumptionSolving))
        assertTrue(matrix.isNative(AnalysisCapability.NativeUnsatCore))
        assertTrue(matrix.supports(AnalysisCapability.Conflict))
        assertTrue(matrix.supports(ConstraintProgrammingFeature.AllDifferent))
        assertEquals(CapabilitySupport.Unsupported, CapabilityMatrix().support(AnalysisCapability.NativeUnsatCore))
    }

    @Test
    fun sessionRetainsSnapshotAndClearsDerivedCachesOnClose() {
        val snapshot = ConstraintProgrammingModelSnapshot(
            name = "analysis-session",
            objectCategory = ObjectCategory.Maximum,
            variables = emptyList(),
            intervals = emptyList(),
            expressions = emptyList(),
            constraints = emptyList(),
            objectives = emptyList(),
            constraintGroups = emptyList()
        )
        val session = CriticalConstraintAnalysisSession.fromSnapshot(
            snapshot = snapshot,
            baselineSolution = ConstraintProgrammingSolution(),
            baselineObjectiveValue = Flt64(5)
        )

        session.cache(AnalysisCacheKind.Activity, "probe", "value")
        assertEquals(snapshot, session.snapshot)
        assertEquals(1, session.cacheSizes[AnalysisCacheKind.Activity])
        assertFalse(session.isClosed)

        session.close()

        assertTrue(session.isClosed)
        assertEquals(0, session.cacheSizes.values.sum())
        assertNull(session.cached<String>(AnalysisCacheKind.Activity, "probe"))
        assertTrue(session.cache(AnalysisCacheKind.Activity, "after-close", "value").failed)
    }

    @Test
    fun invalidSnapshotSessionUsesStructuredValidationFailure() {
        val invalid = ConstraintProgrammingModelSnapshot(
            name = "invalid-session",
            objectCategory = ObjectCategory.Maximum,
            variables = emptyList(),
            intervals = emptyList(),
            expressions = emptyList(),
            constraints = emptyList(),
            objectives = emptyList(),
            constraintGroups = emptyList(),
            identitySchemaVersion = ""
        )

        val session = CriticalConstraintAnalysisSession.fromSnapshot(invalid)
        assertTrue(session.validation.failed)
        assertTrue(
            CriticalConstraintAnalysisSession.tryFromSnapshot(invalid).failed
        )
        assertTrue(
            ConstraintActivityAnalyzer().analyze(session).failed
        )
        session.close()
    }

    @Test
    fun sessionValidatesBaselineSolutionAtResultBoundary() {
        val model = ConstraintProgrammingModel("baseline-validation", ObjectCategory.Maximum)
        val x = IntVar("x")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 10).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(expression, Int64(5)).value!!,
                id = ConstraintId("capacity")
            )
            model.maximize(expression, id = ObjectiveId("payload"))
            val snapshot = model.snapshot().value!!
            val variableId = VariableId("${x.identifier}:${x.index}")

            assertTrue(
                CriticalConstraintAnalysisSession.tryFromSnapshot(
                    snapshot = snapshot,
                    baselineSolution = ConstraintProgrammingSolution(
                        values = mapOf(variableId to Int64(11))
                    )
                ).failed
            )
            assertTrue(
                CriticalConstraintAnalysisSession.tryFromSnapshot(
                    snapshot = snapshot,
                    baselineSolution = ConstraintProgrammingSolution(
                        values = mapOf(variableId to Int64(10))
                    )
                ).failed
            )
            assertTrue(
                CriticalConstraintAnalysisSession.tryFromSnapshot(
                    snapshot = snapshot,
                    baselineSolution = ConstraintProgrammingSolution()
                ).failed
            )
            val valid = CriticalConstraintAnalysisSession.tryFromSnapshot(
                snapshot = snapshot,
                baselineSolution = ConstraintProgrammingSolution(
                    values = mapOf(variableId to Int64(5))
                ),
                baselineObjectiveValue = Flt64(5)
            )
            assertFalse(valid.failed)
            valid.value!!.close()
            assertTrue(
                CriticalConstraintAnalysisSession.tryFromSnapshot(
                    snapshot = snapshot,
                    baselineSolution = ConstraintProgrammingSolution(
                        values = mapOf(variableId to Int64(5))
                    ),
                    baselineObjectiveValue = Flt64(4)
                ).failed
            )
        } finally {
            model.close()
        }
    }
}
