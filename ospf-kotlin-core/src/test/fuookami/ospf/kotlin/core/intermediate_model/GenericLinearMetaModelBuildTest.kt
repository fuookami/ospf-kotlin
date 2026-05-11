package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenericLinearMetaModelBuildTest {
    @Test
    fun fourNumberTypesShouldBuildLinearMetaModelAndDumpMechanismModel() {
        buildLinear(GenericNumberCases.flt64)
        buildLinear(GenericNumberCases.rtn64)
        buildLinear(GenericNumberCases.fltX)
        buildLinear(GenericNumberCases.rtnX)
    }

    private fun <V> buildLinear(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_linear_x")
        val y = RealVar("${numberCase.name.lowercase()}_linear_y")

        val model = LinearMetaModel<V>(
            name = "generic-linear-${numberCase.name.lowercase()}",
            objectCategory = ObjectCategory.Minimum,
            converter = numberCase.converter
        )

        try {
            assertTrue(model.add(listOf(x, y)) is Ok, "${numberCase.name}: add variables should succeed")

            val lhs = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(numberCase.one, x),
                    LinearMonomial(numberCase.two, y)
                ),
                constant = numberCase.zero
            )
            val rhs = LinearPolynomial<V>(emptyList(), numberCase.zero)
            val relation = LinearInequality(lhs = lhs, rhs = rhs, comparison = Comparison.LE)
            assertTrue(model.addConstraint(relation = relation, name = "c_${numberCase.name.lowercase()}") is Ok,
                "${numberCase.name}: add constraint should succeed")

            val objective = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(numberCase.two, x),
                    LinearMonomial(numberCase.one, y)
                ),
                constant = numberCase.zero
            )
            assertTrue(model.minimize(objective) is Ok, "${numberCase.name}: add objective should succeed")

            @Suppress("DEPRECATION")
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: dump mechanism model should succeed")
            assertEquals(1, mechanismResult.value.constraints.size, "${numberCase.name}: constraint count mismatch")
            assertEquals(ObjectCategory.Minimum, mechanismResult.value.objectFunction.category)
        } finally {
            model.close()
        }
    }
}
