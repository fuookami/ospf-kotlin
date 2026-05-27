package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.Linear
import fuookami.ospf.kotlin.core.model.mechanism.LinearSubObject
import fuookami.ospf.kotlin.core.model.mechanism.Object
import fuookami.ospf.kotlin.core.model.mechanism.SingleObject
import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTable
import fuookami.ospf.kotlin.core.token.ConcurrentTokenTable
import fuookami.ospf.kotlin.core.token.MutableTokenTable
import fuookami.ospf.kotlin.core.token.TokenTable
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
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

            assertConstraintRegistration(numberCase, mechanismModel, "floor") {
                floor.registerConstraints(mechanismModel)
            }
            assertConstraintRegistration(numberCase, mechanismModel, "ceil") {
                ceil.registerConstraints(mechanismModel)
            }
            assertConstraintRegistration(numberCase, mechanismModel, "round") {
                round.registerConstraints(mechanismModel)
            }
        } finally {
            model.close()
        }
    }

    private fun <V> assertConstraintRegistration(
        numberCase: GenericNumberCase<V>,
        mechanismModel: CollectingLinearMechanismModel<V>,
        label: String,
        register: () -> Try
    ) where V : RealNumber<V>, V : NumberField<V> {
        val before = mechanismModel.collectedConstraints.size
        assertTrue(register() is Ok, "${numberCase.name}: $label registerConstraints should succeed")
        val after = mechanismModel.collectedConstraints.size
        assertTrue(after > before, "${numberCase.name}: $label should append constraints")

        val appended = mechanismModel.collectedConstraints.subList(before, after)
        val coefficients = appended.flatMap { relation -> relation.lhs.monomials.map { it.coefficient } }
        assertTrue(coefficients.isNotEmpty(), "${numberCase.name}: $label appended constraints should contain coefficients")
        assertTrue(
            coefficients.all { it::class == numberCase.one::class },
            "${numberCase.name}: $label coefficient type should stay V instead of leaking Flt64"
        )
        val rhsConstants = appended.map { relation -> relation.rhs.constant }
        assertTrue(
            rhsConstants.all { it::class == numberCase.one::class },
            "${numberCase.name}: $label rhs constant type should stay V"
        )
        assertTrue(
            appended.any {
                it.comparison == Comparison.LE || it.comparison == Comparison.GE || it.comparison == Comparison.EQ
            },
            "${numberCase.name}: $label should produce comparable sign"
        )
    }

    private fun <V> buildLightweightMechanismModel(model: LinearMetaModel<V>): CollectingLinearMechanismModel<V>
            where V : RealNumber<V>, V : NumberField<V> {
        val tokenTable = when (val tokens = model.tokens) {
            is MutableTokenTable<V> -> TokenTable(tokens)
            is ConcurrentMutableTokenTable<V> -> ConcurrentTokenTable(tokens)
            else -> error("Unsupported token table type: ${tokens::class.qualifiedName}")
        }
        return CollectingLinearMechanismModel(
            name = model.name,
            objectCategory = model.objectCategory,
            tokens = tokenTable
        )
    }

    private class CollectingLinearMechanismModel<V>(
        override var name: String,
        objectCategory: fuookami.ospf.kotlin.core.model.basic.ObjectCategory,
        override val tokens: AbstractTokenTable<V>
    ) : AbstractLinearMechanismModel<V> where V : RealNumber<V>, V : NumberField<V> {
        val collectedConstraints = mutableListOf<LinearInequality<V>>()

        override val constraints: List<Constraint<V, *>> = emptyList()
        override val objectFunction: Object = SingleObject(
            category = objectCategory,
            subObjects = emptyList<LinearSubObject<V>>()
        )

        override fun addConstraint(
            relation: LinearInequality<V>,
            name: String?,
            from: Pair<IntermediateSymbol<out V>, Boolean>?
        ): Try {
            if (name != null && name.isNotBlank() && relation.name != name) {
                collectedConstraints.add(
                    LinearInequality(
                        lhs = relation.lhs,
                        rhs = relation.rhs,
                        comparison = relation.comparison,
                        name = name,
                        displayName = relation.displayName
                    )
                )
            } else {
                collectedConstraints.add(relation)
            }
            return ok
        }
    }
}
