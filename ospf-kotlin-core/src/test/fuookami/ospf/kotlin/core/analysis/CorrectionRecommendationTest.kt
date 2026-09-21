package fuookami.ospf.kotlin.core.analysis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.solver.report.ConstraintId

class CorrectionRecommendationTest {
    @Test
    fun weightedCandidatesAreSortedByBusinessCost() {
        val first = CorrectionCandidate(DiagnosticSource.Constraint(ConstraintId("b")), 2.0, 2.0)
        val second = CorrectionCandidate(DiagnosticSource.Constraint(ConstraintId("a")), 1.0, 1.0)
        val result = weightedCorrectionSet(listOf(first, second))
        assertEquals("constraint:a", result.members.first().source.stableId)
        assertEquals(5.0, result.totalCost, 1e-9)
    }

    @Test
    fun policyCanAllowNonPositiveWeightsWhenExplicitlyConfigured() {
        val candidate = CorrectionCandidate(
            DiagnosticSource.Constraint(ConstraintId("zero")),
            0.0,
            1.0
        )
        val result = weightedCorrectionSet(
            listOf(candidate),
            RelaxabilityPolicy(requirePositiveWeight = false)
        )
        assertEquals(0.0, result.totalCost, 1e-9)
    }
}
