package fuookami.ospf.kotlin.core.analysis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class CriticalityProfileTest {
    @Test
    fun multiTargetProfileSeparatesLocalPersistentAndStructuralEvidence() {
        val a = ConstraintId("a")
        val b = ConstraintId("b")
        val c = ConstraintId("c")
        val observations = listOf(
            CriticalityObservation(ObjectiveTarget.AtLeast(ObjectiveId("o1"), Flt64(1.0)), AnalysisStatus.Unreachable, setOf(a, b)),
            CriticalityObservation(ObjectiveTarget.AtLeast(ObjectiveId("o2"), Flt64(2.0)), AnalysisStatus.Unreachable, setOf(b, c)),
            CriticalityObservation(ObjectiveTarget.AtLeast(ObjectiveId("o3"), Flt64(3.0)), AnalysisStatus.Unknown, setOf(a, b, c))
        )

        val profile = buildCriticalityProfile(observations)
        assertEquals(2, profile.provenTargetCount)
        assertEquals(setOf(CriticalityKind.LocalBottleneck), profile.classifications.getValue(a))
        assertEquals(setOf(CriticalityKind.PersistentBottleneck, CriticalityKind.StructuralBottleneck), profile.classifications.getValue(b))
        assertEquals(setOf(CriticalityKind.LocalBottleneck), profile.classifications.getValue(c))
        assertTrue(profile.constraints(CriticalityKind.StructuralBottleneck).contains(b))
    }
}
