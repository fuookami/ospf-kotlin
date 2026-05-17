package fuookami.ospf.kotlin.example.core_demo

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class DemoBuildSummary(
    val name: String,
    val success: Boolean,
    val constraintCount: Int,
    val variableCount: Int,
    val objectiveCategory: ObjectCategory
)

object DemoBuildAssertions {
    fun extractSummary(
        name: String,
        mechanismModel: LinearMechanismModel<Flt64>
    ): DemoBuildSummary {
        return DemoBuildSummary(
            name = name,
            success = true,
            constraintCount = mechanismModel.constraints.size,
            variableCount = mechanismModel.tokens.tokens.size,
            objectiveCategory = mechanismModel.objectFunction.category
        )
    }

    fun assertStructure(
        summary: DemoBuildSummary,
        expectedMinConstraints: Int,
        expectedMinVariables: Int,
        expectedObjectiveCategory: ObjectCategory
    ) {
        assertTrue(summary.success, "${summary.name}: build should succeed")
        assertTrue(
            summary.constraintCount >= expectedMinConstraints,
            "${summary.name}: expected >= $expectedMinConstraints constraints, got ${summary.constraintCount}"
        )
        assertTrue(
            summary.variableCount >= expectedMinVariables,
            "${summary.name}: expected >= $expectedMinVariables variables, got ${summary.variableCount}"
        )
        assertEquals(
            expectedObjectiveCategory, summary.objectiveCategory,
            "${summary.name}: objective category mismatch"
        )
    }
}