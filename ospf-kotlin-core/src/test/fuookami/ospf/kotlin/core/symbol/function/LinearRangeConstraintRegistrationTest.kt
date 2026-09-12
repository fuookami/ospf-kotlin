package fuookami.ospf.kotlin.core.symbol.function

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok

/** Constraint-level coverage for the linear range family. */
class LinearRangeConstraintRegistrationTest {
    @Test
    fun semiRegistersBothDomainRows() {
        val metaModel = LinearMetaModel<Flt64>(
            name = "semi-rows", converter = IntoValue.Identity
        )
        try {
            val function = SemiFunction(
                lb = Flt64(2.0), ub = Flt64(5.0),
                converter = IntoValue.Identity, name = "semi_rows"
            )
            assertTrue(metaModel.minimize(LinearPolynomial(emptyList(), Flt64.zero)) is Ok)
            assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            assertTrue(function.registerConstraints(mechanism) is Ok)
            val names = mechanism.constraints.map { it.name }
            assertTrue("semi_rows_semi_upper" in names)
            assertTrue("semi_rows_semi_lower" in names)
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun slackRangeRegistersTheExactThreeCandidateMax() {
        val metaModel = LinearMetaModel<Flt64>(
            name = "slack-rows", converter = IntoValue.Identity
        )
        try {
            val function = SlackRangeFunction(
                input = LinearPolynomial(emptyList(), Flt64(4.0)),
                lower = Flt64.one,
                upper = Flt64(3.0),
                bigM = Flt64(20.0),
                converter = IntoValue.Identity,
                name = "slack_rows"
            )
            assertTrue(metaModel.minimize(LinearPolynomial(emptyList(), Flt64.zero)) is Ok)
            assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            val before = mechanism.constraints.size
            assertTrue(function.registerConstraints(mechanism) is Ok)
            val appended = mechanism.constraints.subList(before, mechanism.constraints.size)
            assertEquals(7, appended.size)
            assertEquals(3, appended.count { it.sign == ConstraintRelation.GreaterEqual })
            assertEquals(3, appended.count { it.sign == ConstraintRelation.LessEqual })
            assertEquals(1, appended.count { it.sign == ConstraintRelation.Equal })
            assertTrue(appended.any { row ->
                row.lhs.filterIsInstance<LinearCell<Flt64>>()
                    .any { cell -> cell.coefficient == Flt64(20.0) }
            })
            assertEquals(Flt64.one, function.evaluate(emptyMap()))
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun inStepRangeRegistersFloorAndBoundRowsWithoutAnUnusedBigMArgument() {
        val metaModel = LinearMetaModel<Flt64>(
            name = "step-rows", converter = IntoValue.Identity
        )
        try {
            val zero = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
            val function = InStepRangeFunction(
                lb = zero,
                ub = LinearPolynomial(emptyList(), Flt64(7.0)),
                step = Flt64(3.0),
                converter = IntoValue.Identity,
                name = "step_rows"
            )
            assertTrue(metaModel.minimize(zero) is Ok)
            assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            val before = mechanism.constraints.size
            assertTrue(function.registerConstraints(mechanism) is Ok)
            val appended = mechanism.constraints.subList(before, mechanism.constraints.size)
            assertEquals(4, appended.size)
            assertTrue(appended.any { it.name == "step_rows_bounds" })
            assertTrue(appended.any { it.name == "step_rows_q_floor_ub" })
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun inStepRangeIndicatorRegistersPointBandAndComplementRows() {
        val metaModel = LinearMetaModel<Flt64>(
            name = "indicator-rows", converter = IntoValue.Identity
        )
        try {
            val function = InStepRangeIndicatorFunction(
                input = LinearPolynomial(emptyList(), Flt64(4.0)),
                lower = Flt64.one,
                upper = Flt64(7.0),
                step = Flt64(3.0),
                bigM = Flt64(100.0),
                converter = IntoValue.Identity,
                name = "indicator_rows"
            )
            assertTrue(metaModel.minimize(LinearPolynomial(emptyList(), Flt64.zero)) is Ok)
            assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            assertEquals(7, function.helperVariables.size)
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            val before = mechanism.constraints.size
            assertTrue(function.registerConstraints(mechanism) is Ok)
            val appended = mechanism.constraints.subList(before, mechanism.constraints.size)
            assertEquals(16, appended.size)
            assertTrue(appended.any { it.name == "indicator_rows_pt0_band_ub" })
            assertTrue(appended.any { it.name == "indicator_rows_pt2_out_ub" })
            assertTrue(appended.any { it.name == "indicator_rows_or_ub" })
            assertEquals(Flt64.one, function.evaluate(emptyMap()))
        } finally {
            metaModel.close()
        }
    }
}
