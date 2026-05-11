package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.LinearMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl
import fuookami.ospf.kotlin.core.model.mechanism.LinearSubObject
import fuookami.ospf.kotlin.core.model.mechanism.SingleObject
import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTable
import fuookami.ospf.kotlin.core.token.ConcurrentTokenTable
import fuookami.ospf.kotlin.core.token.MutableTokenTable
import fuookami.ospf.kotlin.core.token.TokenTable
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertTrue

class FunctionSymbolRoundingGenericRegistrationTest {
    @Test
    @Tag("slow")
    fun floorAndCeilAndRoundShouldRegisterConstraintsForFlt64() {
        runRoundingCase(GenericNumberCases.flt64)
    }

    @Test
    @Tag("slow")
    fun floorAndCeilAndRoundShouldRegisterConstraintsForFltX() {
        runRoundingCase(GenericNumberCases.fltX)
    }

    @Test
    @Tag("slow")
    fun floorAndCeilAndRoundShouldRegisterConstraintsForRtn64() {
        runRoundingCase(GenericNumberCases.rtn64)
    }

    @Test
    @Tag("very-slow")
    fun floorAndCeilAndRoundShouldRegisterConstraintsForRtnX() {
        runRoundingCase(GenericNumberCases.rtnX)
    }

    private fun <V> runRoundingCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_rounding_x")
        val y = RealVar("${numberCase.name.lowercase()}_rounding_y")

        val model = LinearMetaModel<V>(
            name = "generic-rounding-${numberCase.name.lowercase()}",
            converter = numberCase.converter
        )

        try {
            assertTrue(model.add(listOf(x, y)) is Ok, "${numberCase.name}: add variables should succeed")

            val xPoly = LinearPolynomial(
                monomials = listOf(LinearMonomial(numberCase.one, x)),
                constant = numberCase.zero
            )
            val yPoly = LinearPolynomial(
                monomials = listOf(LinearMonomial(numberCase.one, y)),
                constant = numberCase.zero
            )

            val floor = FloorFunction(
                x = xPoly,
                converter = numberCase.converter,
                bigM = numberCase.ten,
                name = "floor_${numberCase.name.lowercase()}"
            )
            val ceil = CeilingFunction(
                x = xPoly,
                converter = numberCase.converter,
                bigM = numberCase.ten,
                name = "ceil_${numberCase.name.lowercase()}"
            )
            val round = RoundingFunction(
                x = xPoly,
                converter = numberCase.converter,
                bigM = numberCase.ten,
                name = "round_${numberCase.name.lowercase()}"
            )

            assertTrue(floor.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: floor auxiliary tokens should succeed")
            assertTrue(ceil.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: ceil auxiliary tokens should succeed")
            assertTrue(round.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: round auxiliary tokens should succeed")

            val mechanismModel = buildLightweightMechanismModel(model)

            val before = mechanismModel.constraints.size
            assertTrue(floor.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: floor registerConstraints should succeed")
            assertTrue(ceil.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: ceil registerConstraints should succeed")
            assertTrue(round.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: round registerConstraints should succeed")
            val after = mechanismModel.constraints.size

            assertTrue(after > before, "${numberCase.name}: rounding constraints should be appended")

            val newConstraint = mechanismModel.constraints.last() as LinearConstraintImpl<V>
            val firstCell = newConstraint.lhs.first()
            val coefficient = firstCell.coefficient
            assertTrue(
                coefficient::class == numberCase.one::class,
                "${numberCase.name}: rounding constraint coefficient type should stay V instead of leaking Flt64"
            )
        } finally {
            model.close()
        }
    }

    private fun <V> buildLightweightMechanismModel(model: LinearMetaModel<V>): LinearMechanismModel<V>
            where V : RealNumber<V>, V : NumberField<V> {
        val tokenTable = when (val tokens = model.tokens) {
            is MutableTokenTable<V> -> TokenTable(tokens)
            is ConcurrentMutableTokenTable<V> -> ConcurrentTokenTable(tokens)
            else -> error("Unsupported token table type: ${tokens::class.qualifiedName}")
        }
        return LinearMechanismModel(
            parent = model,
            name = model.name,
            constraints = emptyList(),
            objectFunction = SingleObject(
                category = model.objectCategory,
                subObjects = emptyList<LinearSubObject<V>>()
            ),
            tokens = tokenTable
        )
    }
}
