package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenericQuadraticMetaModelBuildTest {
    @Test
    fun fourNumberTypesShouldBuildQuadraticMetaModelAndDumpMechanismModel() {
        buildQuadratic(GenericNumberCases.flt64)
        buildQuadratic(GenericNumberCases.rtn64)
        buildQuadratic(GenericNumberCases.fltX)
        buildQuadratic(GenericNumberCases.rtnX)
    }

    private fun <V> buildQuadratic(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_quad_x")
        val y = RealVar("${numberCase.name.lowercase()}_quad_y")

        val model = QuadraticMetaModel<V>(
            name = "generic-quadratic-${numberCase.name.lowercase()}",
            objectCategory = ObjectCategory.Minimum,
            converter = numberCase.converter
        )

        try {
            assertTrue(model.add(listOf(x, y)) is Ok, "${numberCase.name}: add variables should succeed")

            val lhs = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(numberCase.one, x, y)),
                constant = numberCase.zero
            )
            val rhs = QuadraticPolynomial<V>(emptyList(), numberCase.zero)
            val relation = QuadraticInequalityOf(lhs = lhs, rhs = rhs, comparison = Comparison.LE)
            assertTrue(model.addConstraint(relation = relation, name = "qc_${numberCase.name.lowercase()}") is Ok,
                "${numberCase.name}: add quadratic constraint should succeed")

            val objective = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(numberCase.one, x, y),
                    QuadraticMonomial.linear(numberCase.one, x)
                ),
                constant = numberCase.zero
            )
            assertTrue(model.minimize(objective) is Ok, "${numberCase.name}: add objective should succeed")

            @Suppress("DEPRECATION")
            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: dump mechanism model should succeed")
            assertEquals(1, mechanismResult.value.constraints.size, "${numberCase.name}: constraint count mismatch")
            assertEquals(ObjectCategory.Minimum, mechanismResult.value.objectFunction.category)
        } finally {
            model.close()
        }
    }
}
