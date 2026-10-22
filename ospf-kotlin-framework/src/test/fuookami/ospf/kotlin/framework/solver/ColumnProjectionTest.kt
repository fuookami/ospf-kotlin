package fuookami.ospf.kotlin.framework.solver

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*

class ColumnProjectionTest {
    private val x = BinVar("x")
    private val theta = URealVar("theta")
    private fun source(): LinearMetaModel<Flt64> {
        val model = LinearMetaModel<Flt64>(name = "source", converter = IntoValue.Identity)
        model.add(x)
        model.add(theta)
        model.minimize(LinearPolynomial(x) + LinearPolynomial(theta) + Flt64(5))
        return model
    }

    private fun projection(model: LinearMetaModel<Flt64>) = ColumnProjection(
        source = model,
        decisions = listOf(ColumnDecision(id = "x", blockId = "block", variable = x)),
        blockIds = setOf("block")
    )

    private val columns get() = listOf(
        ProjectedColumn(id = "empty", blockId = "block", values = mapOf("x" to Flt64.zero)),
        ProjectedColumn(id = "loaded", blockId = "block", values = mapOf("x" to Flt64.one))
    )

    @Test
    fun `new source row reaches old columns pricing final and resource completion`(): Unit = runBlocking {
        val model = source()
        val projection = projection(model)
        val first = assertIs<Ok<ColumnMasterRegistration<Flt64>, *, *>>(projection.master(
            columns = columns.take(1),
            phaseOne = false,
            integer = false
        )).value
        assertIs<Ok<*, *, *>>(model.state.registerRow(ConstraintId("cut"), LinearPolynomial(theta) - Flt64(2) * LinearPolynomial(x) geq Flt64.one))
        assertIs<Failed<*, *, *>>(projection.requireCurrent(first))
        val master = assertIs<Ok<ColumnMasterRegistration<Flt64>, *, *>>(projection.master(
            columns = columns,
            phaseOne = false,
            integer = false
        )).value
        val snapshot = assertIs<Ok<LinearModelSnapshot<Flt64>, *, *>>(master.model.state.refresh()).value
        val cut = snapshot.rows.single { it.sourceName == "cut" }
        assertEquals(Flt64(-2), cut.coefficients[master.columnVariables.getValue("loaded")])
        assertEquals(Flt64.one, cut.coefficients[theta])
        assertEquals(Flt64.one, cut.rhs)
        assertEquals(Flt64(5), snapshot.objectiveConstant)
        val expanded = assertIs<Ok<LinearMechanismModel<Flt64>, *, *>>(LinearMechanismModel(master.model)).value
        expanded.use { full ->
            val dual = full.constraints.associate { row ->
                @Suppress("UNCHECKED_CAST")
                (row as Constraint<Flt64, Linear>) to if (row.name == "cut") Flt64(3) else Flt64.zero
            }
            assertIs<Ok<*, *, *>>(projection.refreshShadowPrice(master, dual))
            val request = assertIs<Ok<ColumnPricingRequest<Flt64>, *, *>>(projection.pricingRequest("block", false)).value
            assertEquals(Flt64(7), request.coefficients["x"])
            assertEquals(Flt64(7), assertIs<Ok<Flt64, *, *>>(request.reducedCost(mapOf("x" to Flt64.one))).value)
            assertIs<Failed<*, *, *>>(projection.pricingRequest("block", true))
            assertIs<Failed<*, *, *>>(projection.refreshShadowPrice(master, emptyMap()))
        }
        for (refine in listOf(false, true)) {
            val integer = assertIs<Ok<ColumnMasterRegistration<Flt64>, *, *>>(projection.master(
                columns = columns,
                phaseOne = false,
                integer = true,
                refinedResources = if (refine) setOf(x) else emptySet()
            )).value
            val final = assertIs<Ok<LinearModelSnapshot<Flt64>, *, *>>(integer.model.state.refresh()).value
            val finalCut = final.rows.single { it.sourceName == "cut" }
            assertEquals(Flt64(-2), finalCut.coefficients[if (refine) x else integer.columnVariables.getValue("loaded")])
            assertIs<Failed<*, *, *>>(projection.requireCurrent(master))
        }
    }

    @Test
    fun `rebuild column changes direct master edits and foreign projections invalidate certificates`(): Unit = runBlocking {
        val model = source()
        val projection = projection(model)
        val first = assertIs<Ok<ColumnMasterRegistration<Flt64>, *, *>>(projection.master(
            columns = columns,
            phaseOne = true,
            integer = false
        )).value
        val seed = assertIs<Ok<ColumnPricingRequest<Flt64>, *, *>>(projection.pricingRequest(blockId = "block", phaseOne = true, seed = true)).value
        projection.columnsChanged()
        assertIs<Failed<*, *, *>>(projection.requireCurrent(seed.revision))
        assertIs<Failed<*, *, *>>(projection.requireCurrent(first))
        val rebuilt = assertIs<Ok<ColumnMasterRegistration<Flt64>, *, *>>(projection.master(
            columns = columns.take(1),
            phaseOne = false,
            integer = false
        )).value
        assertEquals(setOf("empty"), rebuilt.columnVariables.keys)
        assertIs<Failed<*, *, *>>(projection(model).requireCurrent(rebuilt))
        rebuilt.model.addConstraint(LinearPolynomial(theta) leq Flt64.one)
        assertIs<Failed<*, *, *>>(projection.requireCurrent(rebuilt))
        assertIs<Failed<*, *, *>>(projection.pricingRequest("block", false))
    }

    @Test
    fun `generic projection preserves extended precision and normalizes maximum objective once`(): Unit = runBlocking {
        val model = LinearMetaModel<FltX>(name = "extended", converter = IntoValue.fromConverter(FltX))
        model.add(x)
        model.maximize(LinearPolynomial(
            monomials = listOf(fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(FltX(3), x)),
            constant = FltX(7)
        ))
        val projection = ColumnProjection(
            source = model,
            decisions = listOf(ColumnDecision(id = "x", blockId = "block", variable = x)),
            blockIds = setOf("block")
        )
        val columns = listOf(ProjectedColumn(id = "loaded", blockId = "block", values = mapOf("x" to FltX.one)))
        val master = assertIs<Ok<ColumnMasterRegistration<FltX>, *, *>>(projection.master(
            columns = columns,
            phaseOne = false,
            integer = true
        )).value
        val snapshot = assertIs<Ok<LinearModelSnapshot<FltX>, *, *>>(master.model.state.refresh()).value
        assertEquals(0, FltX(-7).compareTo(snapshot.objectiveConstant))
        assertEquals(0, FltX(-3).compareTo(snapshot.objective.getValue(master.columnVariables.getValue("loaded"))))
        val request = assertIs<Ok<ColumnPricingRequest<FltX>, *, *>>(projection.pricingRequest(
            blockId = "block",
            phaseOne = false,
            seed = true
        )).value
        assertEquals(0, FltX(-3).compareTo(request.coefficients.getValue("x")))
    }

    @Test
    fun `late local rows and tightened bounds require invalid old columns to be removed`(): Unit = runBlocking {
        val model = source()
        val projection = projection(model)
        assertIs<Ok<*, *, *>>(model.state.registerRow(ConstraintId("local-limit"), LinearPolynomial(x) leq Flt64.zero))
        assertIs<Failed<*, *, *>>(projection.master(
            columns = columns,
            phaseOne = false,
            integer = false
        ))
        assertIs<Ok<*, *, *>>(projection.master(
            columns = columns.take(1),
            phaseOne = false,
            integer = false
        ))
        val other = projection(source())
        x.range.leq(false)
        assertIs<Failed<*, *, *>>(other.master(
            columns = columns,
            phaseOne = false,
            integer = true
        ))
    }

    @Test
    fun `duals from an identically named rebuilt master cannot be reused`(): Unit = runBlocking {
        val projection = projection(source())
        val first = assertIs<Ok<ColumnMasterRegistration<Flt64>, *, *>>(projection.master(
            columns = columns,
            phaseOne = false,
            integer = false
        )).value
        val expanded = assertIs<Ok<LinearMechanismModel<Flt64>, *, *>>(LinearMechanismModel(first.model)).value
        expanded.use { full ->
            val duals = full.constraints.associate { row ->
                @Suppress("UNCHECKED_CAST")
                (row as Constraint<Flt64, Linear>) to Flt64.zero
            }
            val second = assertIs<Ok<ColumnMasterRegistration<Flt64>, *, *>>(projection.master(
                columns = columns,
                phaseOne = false,
                integer = false
            )).value
            assertIs<Failed<*, *, *>>(projection.refreshShadowPrice(first, duals))
            assertIs<Failed<*, *, *>>(projection.refreshShadowPrice(second, duals))
            assertIs<Failed<*, *, *>>(projection.pricingRequest("block", false))
        }
    }
}
