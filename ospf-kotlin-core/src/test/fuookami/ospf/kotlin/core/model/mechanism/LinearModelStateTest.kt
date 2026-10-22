package fuookami.ospf.kotlin.core.model.mechanism

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.AbsFunction
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.variable.*

class LinearModelStateTest {
    @Test
    fun `dynamic rows are canonical idempotent and detached from caller expressions`(): Unit = runBlocking {
        val identities = ModelElementIdentityRegistry()
        val model = LinearMetaModel<Flt64>(name = "rows", converter = IntoValue.Identity, identityRegistry = identities)
        val x = BinVar("x")
        assertIs<Ok<*, *, *>>(model.add(x))
        val terms = arrayListOf(LinearMonomial(Flt64.one, x))
        val expression = LinearPolynomial(terms, Flt64.zero)
        val relation = expression leq Flt64.one
        assertIs<Ok<*, *, *>>(model.state.registerRow(ConstraintId("business"), relation))
        assertIs<Ok<*, *, *>>(model.state.registerRow(ConstraintId("business"), relation))
        assertEquals(1, model.constraints.size)
        assertEquals("business", identities.identity(model.constraints.single())?.id?.value)
        assertIs<Failed<*, *, *>>(model.state.registerRow(ConstraintId("business"), expression leq Flt64.zero))
        assertIs<Failed<*, *, *>>(model.state.registerRow(ConstraintId("unknown"), LinearPolynomial(BinVar("unknown")) leq Flt64.one))
        assertIs<Failed<*, *, *>>(model.state.registerRow(ConstraintId("nan"), expression leq Flt64.nan))
        assertIs<Failed<*, *, *>>(model.state.registerRow(ConstraintId("model-local-bad"), relation))
        assertIs<Failed<*, *, *>>(model.state.registerRow(ConstraintId("strict"), relation.copy(comparison = Comparison.LT)))
        terms.clear()
        assertEquals(1, model.constraints.size)
        val snapshot = assertIs<Ok<LinearModelSnapshot<Flt64>, *, *>>(model.state.refresh()).value
        assertEquals(Flt64.one, snapshot.rows.single().coefficients[x])
        assertEquals(Flt64.one, snapshot.rows.single().rhs)
        assertIs<Ok<*, *, *>>(model.state.requireCurrent(snapshot.revision))
    }

    @Test
    fun `direct row objective and bound edits invalidate revisions and names are not identities`(): Unit = runBlocking {
        val model = LinearMetaModel<Flt64>(name = "revisions", converter = IntoValue.Identity)
        val x = URealVar("x")
        model.add(x)
        model.addConstraint(LinearPolynomial(x) leq Flt64(4), name = "same")
        model.minimize(LinearPolynomial(x))
        val initial = assertIs<Ok<LinearModelSnapshot<Flt64>, *, *>>(model.state.refresh()).value
        assertEquals(initial, assertIs<Ok<LinearModelSnapshot<Flt64>, *, *>>(model.state.refresh()).value)
        model.addConstraint(LinearPolynomial(x) geq Flt64.one, name = "same")
        assertIs<Failed<*, *, *>>(model.state.requireCurrent(initial.revision))
        val withRow = assertIs<Ok<LinearModelSnapshot<Flt64>, *, *>>(model.state.refresh()).value
        assertEquals(2, withRow.rows.map { it.id }.distinct().size)
        model.minimize(LinearPolynomial(Flt64(7)))
        assertIs<Failed<*, *, *>>(model.state.requireCurrent(withRow.revision))
        val withObjective = assertIs<Ok<LinearModelSnapshot<Flt64>, *, *>>(model.state.refresh()).value
        assertEquals(Flt64(7), withObjective.objectiveConstant)
        x.range.leq(Flt64(3))
        assertIs<Failed<*, *, *>>(model.state.requireCurrent(withObjective.revision))
        val other = LinearMetaModel<Flt64>(name = "foreign", converter = IntoValue.Identity)
        assertIs<Failed<*, *, *>>(other.state.requireCurrent(initial.revision))
    }

    @Test
    fun `extended precision is preserved at the model boundary`(): Unit = runBlocking {
        val model = LinearMetaModel<FltX>(name = "extended", converter = IntoValue.fromConverter(FltX))
        val x = BinVar("x")
        model.add(x)
        val coefficient = FltX(3)
        assertIs<Ok<*, *, *>>(model.state.registerRow(
            ConstraintId("extended-row"),
            LinearInequality(
                lhs = LinearPolynomial(listOf(fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(coefficient, x)), FltX.zero),
                rhs = LinearPolynomial(constant = FltX(9)),
                comparison = Comparison.LE
            )
        ))
        val snapshot = assertIs<Ok<LinearModelSnapshot<FltX>, *, *>>(model.state.refresh()).value
        assertEquals(0, coefficient.compareTo(snapshot.rows.single().coefficients.getValue(x)))
        assertEquals(0, FltX(9).compareTo(snapshot.rows.single().rhs))
    }

    @Test
    fun `expanded function rows and auxiliary variables have repeatable session snapshots`(): Unit = runBlocking {
        for (policy in FunctionExpansionPolicy.entries) {
            val model = LinearMetaModel<Flt64>(
                name = "expanded",
                converter = IntoValue.Identity,
                configuration = MetaModelConfiguration(functionExpansionPolicy = policy)
            )
            val x = RealVar("x")
            x.range.geq(Flt64(-2))
            x.range.leq(Flt64(3))
            model.add(x)
            val absolute = AbsFunction(
                polynomial = LinearPolynomial(x),
                converter = IntoValue.Identity,
                bigM = Flt64(6),
                name = "absolute"
            )
            model.add(LinearFunctionSymbolAdapter(absolute, IntoValue.Identity))
            model.minimize(absolute.resultPolynomial)
            val first = assertIs<Ok<LinearModelSnapshot<Flt64>, *, *>>(model.state.refresh()).value
            val second = assertIs<Ok<LinearModelSnapshot<Flt64>, *, *>>(model.state.refresh()).value
            assertTrue(first.variables.size > 1)
            assertTrue(first.rows.isNotEmpty())
            assertEquals(first, second)
        }
    }
}
