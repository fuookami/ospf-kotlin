package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.intermediate.CoreDeferredFunctionFallbackMaterializer
import fuookami.ospf.kotlin.core.model.intermediate.MaxStructure
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/** Dedicated contract test for the MinFunction symbol. */
class MinFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(MinFunction::class.java))
    }

    @Test
    fun negativeInputsUseSignedResultVariableAndPropagateBounds() {
        val x = RealVar("min_negative_x")
        val y = RealVar("min_negative_y")
        x.range.geq(Flt64(-10.0))
        x.range.leq(Flt64(-4.0))
        y.range.geq(Flt64(-6.0))
        y.range.leq(Flt64(-2.0))

        val xPolynomial = LinearPolynomial(
            listOf(LinearMonomial(Flt64.one, x)),
            Flt64.zero
        )
        val yPolynomial = LinearPolynomial(
            listOf(LinearMonomial(Flt64.one, y)),
            Flt64.zero
        )
        val function = MinFunction(
            polynomials = listOf(xPolynomial, yPolynomial),
            converter = IntoValue.Identity,
            name = "min_negative"
        )

        val result = assertIs<RealVar>(function.resultVar)
        assertEquals(Flt64(-10.0), result.lowerBound!!.value.unwrap())
        assertEquals(Flt64(-4.0), result.upperBound!!.value.unwrap())
    }

    @Test
    fun explicitBigMUsesTheSelectedMinimumAsTheTightLowerBound() {
        val function = MinFunction(
            polynomials = listOf(constantPolynomial(1.0), constantPolynomial(3.0)),
            bigM = Flt64(10.0),
            converter = IntoValue.Identity,
            name = "min_explicit_rows"
        )
        val rows = registerRows(function)

        assertEquals(
            listOf(Comparison.LE, Comparison.LE, Comparison.GE, Comparison.GE, Comparison.EQ),
            rows.map { it.sign.toComparison() }
        )
        val lowerRows = rows.filter { it.sign == ConstraintRelation.GreaterEqual }
        assertEquals(listOf(Flt64(-10.0), Flt64(-10.0)), lowerRows.map {
            coefficient(it, function.selectorVars[lowerRows.indexOf(it)])
        })
        assertEquals(listOf(Flt64(-9.0), Flt64(-7.0)), lowerRows.map { it.rhs })

        val minimumSelected = mapOf(
            function.resultVar.key to Flt64.one,
            function.selectorVars[0].key to Flt64.one,
            function.selectorVars[1].key to Flt64.zero
        )
        val maximumSelected = mapOf(
            function.resultVar.key to Flt64.one,
            function.selectorVars[0].key to Flt64.zero,
            function.selectorVars[1].key to Flt64.one
        )
        assertTrue(rows.all { isSatisfied(it, minimumSelected) })
        assertFalse(rows.all { isSatisfied(it, maximumSelected) })
    }

    @Test
    fun automaticBigMUsesEachInputUpperBoundAgainstTheGlobalMinimumLowerBound() {
        val x = RealVar("min_automatic_x").also {
            it.range.geq(Flt64(-2.0))
            it.range.leq(Flt64(4.0))
        }
        val y = RealVar("min_automatic_y").also {
            it.range.geq(Flt64.one)
            it.range.leq(Flt64(3.0))
        }
        val function = MinFunction(
            polynomials = listOf(
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.one),
                LinearPolynomial(listOf(LinearMonomial(Flt64.two, y)), Flt64(-2.0))
            ),
            converter = IntoValue.Identity,
            name = "min_automatic_rows"
        )
        val rows = registerRows(function, listOf(x, y))
        val lowerRows = rows.filter { it.sign == ConstraintRelation.GreaterEqual }

        assertEquals(listOf(Flt64(-6.0), Flt64(-5.0)), lowerRows.map {
            coefficient(it, function.selectorVars[lowerRows.indexOf(it)])
        })
        assertEquals(listOf(Flt64(-5.0), Flt64(-7.0)), lowerRows.map { it.rhs })
    }

    @Test
    fun oneInputAndEqualCandidatesRemainExact() {
        val single = MinFunction(
            polynomials = listOf(constantPolynomial(7.0)),
            converter = IntoValue.Identity,
            name = "min_single_rows"
        )
        val singleRows = registerRows(single)
        assertEquals(3, singleRows.size)
        assertEquals(Flt64(-1.0), coefficient(
            singleRows[1],
            single.selectorVars.single()
        ))
        assertEquals(Flt64(6.0), singleRows[1].rhs)
        assertTrue(singleRows.all {
            isSatisfied(it, mapOf(
                single.resultVar.key to Flt64(7.0),
                single.selectorVars.single().key to Flt64.one
            ))
        })

        val tied = MinFunction(
            polynomials = listOf(constantPolynomial(3.0), constantPolynomial(3.0)),
            bigM = Flt64(10.0),
            converter = IntoValue.Identity,
            name = "min_tied_rows"
        )
        val tiedRows = registerRows(tied)
        val tiedSelections = listOf(0, 1)
        for (selected in tiedSelections) {
            val values = mapOf(
                tied.resultVar.key to Flt64(3.0),
                tied.selectorVars[0].key to if (selected == 0) Flt64.one else Flt64.zero,
                tied.selectorVars[1].key to if (selected == 1) Flt64.one else Flt64.zero
            )
            assertTrue(tiedRows.all { isSatisfied(it, values) })
        }
    }

    @Test
    fun deferredStructureUsesMinimumDirectionAndFallbackRows() {
        val function = MinFunction(
            polynomials = listOf(constantPolynomial(1.0), constantPolynomial(3.0)),
            bigM = Flt64(10.0),
            converter = IntoValue.Identity,
            name = "min_deferred"
        )

        val structure = assertIs<MaxStructure<Flt64>>(function.deferredStructure())
        assertTrue(structure.minimum)
        val fallback = CoreDeferredFunctionFallbackMaterializer.materialize(structure)
        assertTrue(fallback is Ok)
        assertEquals(5, fallback.value.constraints.size)
        assertEquals("min_deferred_min_select", fallback.value.constraints.last().name)
    }

    private fun constantPolynomial(value: Double): LinearPolynomial<Flt64> {
        return LinearPolynomial(emptyList(), Flt64(value))
    }

    private fun registerRows(
        function: MinFunction<Flt64>,
        variables: List<RealVar> = emptyList()
    ): List<Constraint<Flt64, *>> {
        val metaModel = LinearMetaModel<Flt64>(
            name = "${function.name}_model",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(metaModel.minimize(constantPolynomial(0.0)) is Ok)
            if (variables.isNotEmpty()) {
                assertTrue(metaModel.add(variables) is Ok)
            }
            assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            val before = mechanism.constraints.size
            assertTrue(function.registerConstraints(mechanism) is Ok)
            return mechanism.constraints.subList(before, mechanism.constraints.size).toList()
        } finally {
            metaModel.close()
        }
    }

    private fun coefficient(
        row: Constraint<Flt64, *>,
        variable: AbstractVariableItem<*, *>
    ): Flt64 {
        return row.lhs.filterIsInstance<LinearCell<Flt64>>()
            .single { it.token.key == variable.key }
            .coefficient
    }

    private fun isSatisfied(
        row: Constraint<Flt64, *>,
        values: Map<VariableItemKey, Flt64>
    ): Boolean {
        var lhs = Flt64.zero
        for (cell in row.lhs.filterIsInstance<LinearCell<Flt64>>()) {
            lhs += cell.evaluate(values) ?: return false
        }
        return row.sign(lhs, row.rhs)
    }
}
