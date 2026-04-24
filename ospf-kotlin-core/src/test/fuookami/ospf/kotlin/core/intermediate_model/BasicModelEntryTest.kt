package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.Variable
import fuookami.ospf.kotlin.core.model.mechanism.LinearRelationImpl
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticRelationImpl
import fuookami.ospf.kotlin.core.model.mechanism.flattenData
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as MathLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as MathQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as MathLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as MathQuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            sparseLhs = SparseMatrixF64(),
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

        val relation = MathLinearInequality(
            lhs = MathLinearPolynomial(
                monomials = listOf(
                    MathLinearMonomial(Flt64(2.0), x),
                    MathLinearMonomial(Flt64.one, y)
                ),
                constant = Flt64.zero
            ),
            rhs = MathLinearPolynomial(emptyList(), Flt64(10.0)),
            comparison = Comparison.LE
        )
        val constraint = LinearConstraintImpl(
            relation = LinearRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            name = "c1"
        )
        val mechanismModel = LinearMechanismModel<Flt64>(
            parent = LinearMetaModel<Flt64>(name = "factory-parent"),
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
            sparseLhs = SparseMatrixF64(),
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

        val relation = MathQuadraticInequality(
            lhs = MathQuadraticPolynomial(
                monomials = listOf(
                    MathQuadraticMonomial.quadratic(Flt64.one, x, y),
                    MathQuadraticMonomial.linear(Flt64(2.0), x)
                ),
                constant = Flt64.zero
            ),
            rhs = MathQuadraticPolynomial(emptyList(), Flt64(5.0)),
            comparison = Comparison.LE
        )
        val constraint = QuadraticConstraintImpl(
            relation = QuadraticRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            name = "qc1"
        )
        val mechanismModel = QuadraticMechanismModel<Flt64>(
            parent = QuadraticMetaModel<Flt64>(name = "factory-parent-q"),
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
