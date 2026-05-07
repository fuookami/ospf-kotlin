package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.model.mechanism.LinearRelationImpl
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticRelationImpl
import fuookami.ospf.kotlin.core.model.mechanism.flattenData
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

class BasicModelEntryTest {
    @Test
    fun basicLinearTriadModelDirectConstructorShouldCreateValidModel() {
        val variable = Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64(10.0),
            type = Continuous,
            origin = null,
            dualOrigin = null,
            slack = null,
            name = "x",
            initialResult = Flt64.zero
        )
        val constraints = LinearConstraintBatch(
            sparseLhs = SparseMatrix<Flt64>(),
            signs = emptyList(),
            rhs = emptyList(),
            names = emptyList(),
            sources = emptyList()
        )
        val model = BasicLinearTriadModel(
            variables = listOf(variable),
            constraints = constraints,
            name = "test-linear"
        )

        assertEquals("test-linear", model.name)
        assertEquals(1, model.variables.size)
        assertEquals("x", model.variables[0].name)
        assertEquals(0, model.constraints.size)
    }

    @Test
    fun basicLinearTriadModelFactoryFromMechanismModelShouldExtractVariablesAndConstraints() {
        val x = RealVar("x")
        val y = RealVar("y")

        val tokens = AutoTokenTable<Flt64>(Linear, false)
        assertTrue(tokens.add(listOf(x, y)) is Ok)

        val relation = LinearInequality<Flt64>(
            lhs = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(Flt64(2.0), x),
                    LinearMonomial(Flt64.one, y)
                ),
                constant = Flt64.zero
            ),
            rhs = LinearPolynomial(emptyList(), Flt64(10.0)),
            comparison = Comparison.LE
        )
        val constraint = LinearConstraintImpl(
            relation = LinearRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            converter = IntoValue.Identity,
            name = "c1"
        )
        val mechanismModel = LinearMechanismModel<Flt64>(
            parent = LinearMetaModel<Flt64>(name = "factory-parent", converter = flt64Converter),
            name = "factory-model",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<LinearSubObject<Flt64>>()),
            tokens = tokens
        )

        val tokenIndexMap = tokens.tokensInSolver.withIndex().associate { (index, token) -> token to index }
        val basicModel = BasicLinearTriadModel.from(mechanismModel, tokenIndexMap)

        assertEquals("factory-model", basicModel.name)
        assertEquals(2, basicModel.variables.size)
        assertEquals(1, basicModel.constraints.size)
        assertEquals(ConstraintRelation.LessEqual, basicModel.constraints.signs[0])
        assertTrue(basicModel.constraints.rhs[0] eq Flt64(10.0))

        mechanismModel.close()
    }

    @Test
    fun basicLinearTriadModelCopyShouldProduceEqualModel() {
        val variable = Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64.one,
            type = Continuous,
            origin = null,
            dualOrigin = null,
            slack = null,
            name = "x",
            initialResult = Flt64.zero
        )
        val constraints = LinearConstraintBatch(
            sparseLhs = SparseMatrix<Flt64>(),
            signs = emptyList(),
            rhs = emptyList(),
            names = emptyList(),
            sources = emptyList()
        )
        val model = BasicLinearTriadModel(
            variables = listOf(variable),
            constraints = constraints,
            name = "copy-test"
        )
        val copied = model.copy()

        assertEquals(model.name, copied.name)
        assertEquals(model.variables.size, copied.variables.size)
        assertEquals(model.constraints.size, copied.constraints.size)
    }

    @Test
    fun basicQuadraticTetradModelDirectConstructorShouldCreateValidModel() {
        val variable = Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64(10.0),
            type = Continuous,
            origin = null,
            dualOrigin = null,
            slack = null,
            name = "x",
            initialResult = Flt64.zero
        )
        val constraints = QuadraticConstraintBatch(
            sparseLhs = SparseQuadraticMatrix(),
            signs = emptyList(),
            rhs = emptyList(),
            names = emptyList(),
            sources = emptyList()
        )
        val model = BasicQuadraticTetradModel(
            variables = listOf(variable),
            constraints = constraints,
            name = "test-quadratic"
        )

        assertEquals("test-quadratic", model.name)
        assertEquals(1, model.variables.size)
        assertEquals("x", model.variables[0].name)
        assertEquals(0, model.constraints.size)
    }

    @Test
    fun basicQuadraticTetradModelFactoryFromMechanismModelShouldExtractVariablesAndConstraints() {
        val x = RealVar("x")
        val y = RealVar("y")

        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        assertTrue(tokens.add(listOf(x, y)) is Ok)

        val relation = QuadraticInequality(
            lhs = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(Flt64.one, x, y),
                    QuadraticMonomial.linear(Flt64(2.0), x)
                ),
                constant = Flt64.zero
            ),
            rhs = QuadraticPolynomial(emptyList(), Flt64(5.0)),
            comparison = Comparison.LE
        )
        val constraint = QuadraticConstraintImpl(
            relation = QuadraticRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            converter = IntoValue.Identity,
            name = "qc1"
        )
        val mechanismModel = QuadraticMechanismModel<Flt64>(
            parent = QuadraticMetaModel<Flt64>(name = "factory-parent-q", converter = flt64Converter),
            name = "factory-model-q",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<QuadraticSubObject<Flt64>>()),
            tokens = tokens
        )

        val tokenIndexMap = tokens.tokensInSolver.withIndex().associate { (index, token) -> token to index }
        val basicModel = BasicQuadraticTetradModel.from(mechanismModel, tokenIndexMap)

        assertEquals("factory-model-q", basicModel.name)
        assertEquals(2, basicModel.variables.size)
        assertEquals(1, basicModel.constraints.size)
        assertEquals(ConstraintRelation.LessEqual, basicModel.constraints.signs[0])
        assertTrue(basicModel.constraints.rhs[0] eq Flt64(5.0))

        mechanismModel.close()
    }

    @Test
    fun basicQuadraticTetradModelCopyShouldProduceEqualModel() {
        val variable = Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64.one,
            type = Continuous,
            origin = null,
            dualOrigin = null,
            slack = null,
            name = "x",
            initialResult = Flt64.zero
        )
        val constraints = QuadraticConstraintBatch(
            sparseLhs = SparseQuadraticMatrix(),
            signs = emptyList(),
            rhs = emptyList(),
            names = emptyList(),
            sources = emptyList()
        )
        val model = BasicQuadraticTetradModel(
            variables = listOf(variable),
            constraints = constraints,
            name = "copy-test-q"
        )
        val copied = model.copy()

        assertEquals(model.name, copied.name)
        assertEquals(model.variables.size, copied.variables.size)
        assertEquals(model.constraints.size, copied.constraints.size)
    }
}
