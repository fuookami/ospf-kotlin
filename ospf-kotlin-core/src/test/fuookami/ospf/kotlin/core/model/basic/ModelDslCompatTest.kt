package fuookami.ospf.kotlin.core.model.basic

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaConstraintGroup
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelDslCompatTest {
    // ========== addConstraint convenience overloads (Model layer) ==========

    @Test
    fun addConstraintVariableShouldDelegateToInequalityEqTrue() {
        val model = LinearMetaModel("dsl_addConstraint_variable")
        val x = BinVar("x")
        model.add(x)

        val result = model.addConstraint(x)
        assertTrue(result is Ok, "addConstraint(variable) should succeed")
        assertEquals(1, model.constraints.size)
        model.close()
    }

    @Test
    fun addConstraintMonomialShouldDelegateToInequalityEqTrue() {
        val model = LinearMetaModel("dsl_addConstraint_monomial")
        val x = RealVar("x")
        model.add(x)

        val monomial = LinearMonomial(Flt64.one, x)
        val result = model.addConstraint(monomial)
        assertTrue(result is Ok, "addConstraint(monomial) should succeed")
        assertEquals(1, model.constraints.size)
        model.close()
    }

    @Test
    fun addConstraintPolynomialShouldDelegateToInequalityEqTrue() {
        val model = LinearMetaModel("dsl_addConstraint_polynomial")
        val x = RealVar("x")
        val y = RealVar("y")
        model.add(x)
        model.add(y)

        val polynomial = LinearPolynomial(
            listOf(LinearMonomial(Flt64.one, x), LinearMonomial(Flt64.one, y)),
            Flt64.zero
        )
        val result = model.addConstraint(polynomial)
        assertTrue(result is Ok, "addConstraint(polynomial) should succeed")
        assertEquals(1, model.constraints.size)
        model.close()
    }

    @Test
    fun addConstraintSymbolShouldDelegateToInequalityEqTrue() {
        val model = LinearMetaModel("dsl_addConstraint_symbol")
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "dsl_symbol"
        )
        model.add(symbol)

        val result = model.addConstraint(symbol)
        assertTrue(result is Ok, "addConstraint(symbol) should succeed")
        assertEquals(1, model.constraints.size)
        model.close()
    }

    // ========== partition overloads (Model layer) ==========

    @Test
    fun partitionVariablesShouldDelegateToInequalityEqOne() {
        val model = LinearMetaModel("dsl_partition_variables")
        val x = BinVar("x")
        val y = BinVar("y")
        model.add(x)
        model.add(y)

        val result = model.partition(listOf(x, y))
        assertTrue(result is Ok, "partition(variables) should succeed")
        assertEquals(1, model.constraints.size)
        model.close()
    }

    @Test
    fun partitionPolynomialShouldDelegateToInequalityEqOne() {
        val model = LinearMetaModel("dsl_partition_polynomial")
        val x = BinVar("x")
        val y = BinVar("y")
        model.add(x)
        model.add(y)

        val polynomial = LinearPolynomial(
            listOf(LinearMonomial(Flt64.one, x), LinearMonomial(Flt64.one, y)),
            Flt64.zero
        )
        val result = model.partition(polynomial)
        assertTrue(result is Ok, "partition(polynomial) should succeed")
        assertEquals(1, model.constraints.size)
        model.close()
    }

    // ========== QuadraticModel convenience overloads ==========

    @Test
    fun quadraticAddConstraintMonomialShouldDelegateToInequalityEqTrue() {
        val model = QuadraticMetaModel("dsl_quad_addConstraint_monomial")
        val x = RealVar("x")
        model.add(x)

        val monomial = QuadraticMonomial(Flt64.one, x, null)
        val result = model.addConstraint(monomial)
        assertTrue(result is Ok, "addConstraint(quadratic monomial) should succeed")
        assertEquals(1, model.constraints.size)
        model.close()
    }

    @Test
    fun quadraticPartitionPolynomialShouldDelegateToInequalityEqOne() {
        val model = QuadraticMetaModel("dsl_quad_partition_polynomial")
        val x = BinVar("x")
        val y = BinVar("y")
        model.add(x)
        model.add(y)

        val polynomial = QuadraticPolynomial(
            listOf(QuadraticMonomial(Flt64.one, x, null), QuadraticMonomial(Flt64.one, y, null)),
            Flt64.zero
        )
        val result = model.partition(polynomial)
        assertTrue(result is Ok, "partition(quadratic polynomial) should succeed")
        assertEquals(1, model.constraints.size)
        model.close()
    }

    // ========== constraintsOfGroup (MetaModel layer) ==========

    @Test
    fun constraintsOfGroupShouldReturnConstraintsForGivenGroup() {
        val model = LinearMetaModel("dsl_constraintsOfGroup")
        val x = BinVar("x")
        model.add(x)

        val group = object : MetaConstraintGroup {
            override val name = "test_group"
        }

        model.registerConstraintGroup(group)
        model.addConstraint(x, group = group)

        val groupConstraints = model.constraintsOfGroup(group)
        assertEquals(1, groupConstraints.size, "constraintsOfGroup should return 1 constraint")

        model.close()
    }
}