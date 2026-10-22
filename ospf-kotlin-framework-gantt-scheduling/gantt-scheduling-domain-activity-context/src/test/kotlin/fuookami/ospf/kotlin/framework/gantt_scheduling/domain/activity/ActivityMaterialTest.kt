@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity

import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model.*

/** 用独立库存算术与全部二元组合对照时序物料模型。 / Compare temporal inventory with independent arithmetic over all binary assignments. */
class ActivityMaterialTest {
    private val epoch = Instant.parse("2026-10-22T00:00:00Z")
    private data class Task(val id: String) : TaskId
    private val make = Task("make")
    private val use = Task("use")

    @Test
    fun futureProductionCannotCoverEarlierConsumptionAgainstIndependentOracle() {
        val problem = problem(
            activities = listOf(
                Activity(make, listOf(mode("make", listOf(0, 2), listOf(flow(2.0, 1))))),
                Activity(use, listOf(mode("use", listOf(0, 1, 3), listOf(flow(-2.0)))))
            ),
            materials = listOf(ActivityMaterial("raw", q(0.0)))
        )
        val context = context(problem)
        val model = LinearMetaModel(name = "stock_oracle")
        assertTrue(context.register(model).ok)
        for (production in listOf(0, 2)) {
            for (consumption in listOf(0, 1, 3)) {
                val keys = listOf(key(make, "make", production), key(use, "use", consumption))
                val expected = consumption >= production + 1
                assertEquals(expected, context.validate(keys).feasible)
                assertEquals(expected, feasible(model, values(context, model, keys)))
            }
        }
        assertEveryBinaryAssignmentMatchesValidator(context, model)
        model.close()
    }

    @Test
    fun simultaneousEventsNetAtomicallyAndCompletionAtHorizonIsIncluded() {
        val context = context(problem(
            activities = listOf(Activity(make, listOf(mode("make", listOf(0, 3), listOf(flow(4.0, 1)))))),
            materials = listOf(ActivityMaterial("raw", q(0.0), maximum = q(1.0))),
            events = listOf(ActivityMaterialEvent("raw", at(4), q(-4.0)))
        ))
        val model = LinearMetaModel(name = "simultaneous_stock")
        assertTrue(context.register(model).ok)
        assertFalse(context.validate(listOf(key(make, "make", 0))).feasible)
        val chosen = listOf(key(make, "make", 3))
        assertTrue(context.validate(chosen).feasible)
        model.setSolution(values(context, model, chosen))
        val inventory = assertNotNull(context.extractSolution(model).value).materialInventory.single()
        assertEquals("raw", inventory.material)
        assertEquals(at(4), inventory.levels.last().time)
        assertTrue(inventory.levels.all { it.quantity.value eq Flt64.zero })
        assertEveryBinaryAssignmentMatchesValidator(context, model)
        model.close()
    }

    @Test
    fun openingBoundsCannotBeRepairedRetroactivelyByHorizonStartReceipts() {
        val context = context(problem(
            activities = emptyList(),
            materials = listOf(ActivityMaterial("raw", q(0.0), minimum = q(1.0))),
            events = listOf(ActivityMaterialEvent("raw", at(0), q(2.0)))
        ))
        val model = LinearMetaModel(name = "opening_stock")
        assertTrue(context.register(model).ok)
        assertFalse(context.validate(emptyList()).feasible)
        assertFalse(feasible(model, emptyList()))
        model.close()
    }

    @Test
    fun explicitCancellationChargesUnservedOrderAndNeverCreatesStock() {
        val context = context(problem(
            activities = listOf(Activity(
                id = use,
                modes = listOf(mode("fulfill", listOf(0), listOf(flow(-2.0)))),
                cancellationCost = q(7.0)
            )),
            materials = listOf(ActivityMaterial("raw", q(1.0)))
        ))
        val model = LinearMetaModel(name = "cancel_order")
        assertTrue(context.register(model).ok)
        assertFalse(context.validate(listOf(key(use, "fulfill", 0))).feasible)
        assertTrue(context.validate(emptyList()).feasible)
        model.setSolution(values(context, model, emptyList()))
        val schedule = assertNotNull(context.extractSolution(model).value)
        assertEquals(setOf(use), schedule.cancelled)
        assertTrue(schedule.cost.value eq Flt64(7.0))
        assertTrue(schedule.materialInventory.single().levels.all { it.quantity.value eq Flt64.one })
        assertEveryBinaryAssignmentMatchesValidator(context, model)
        model.close()
    }

    @Test
    fun rollingInventoryUsesOpeningStockAndOnlyExplicitRemainingEvents() {
        val fact = ActivityExecution(
            placement = ActivityPlacement<Flt64>(key(make, "running", -1), TimeRange(at(-1), at(1)), emptyList()),
            status = ActivityExecutionStatus.Running
        )
        val context = context(problem(
            activities = listOf(Activity(use, listOf(mode("consume", listOf(0, 1), listOf(flow(-3.0)))))),
            materials = listOf(ActivityMaterial("raw", q(1.0))),
            events = listOf(ActivityMaterialEvent("raw", at(1), q(2.0))),
            executions = listOf(fact)
        ))
        val model = LinearMetaModel(name = "rolling_stock")
        assertTrue(context.register(model).ok)
        assertFalse(context.validate(listOf(key(use, "consume", 0))).feasible)
        val keys = listOf(key(use, "consume", 1))
        model.setSolution(values(context, model, keys))
        val schedule = assertNotNull(context.extractSolution(model).value)
        assertEquals(listOf(fact), schedule.executions)
        assertTrue(schedule.materialInventory.single().levels.last().quantity.value eq Flt64.zero)
        assertEveryBinaryAssignmentMatchesValidator(context, model)
        model.close()
    }

    @Test
    fun differentMaterialsNeverCompensateEachOtherAndInternalOffsetsAreRespected() {
        val context = context(problem(
            activities = listOf(Activity(make, listOf(ActivityMode(
                id = "transform",
                duration = 2.hours,
                starts = listOf(at(0), at(1)),
                resources = emptyList(),
                cost = q(0.0),
                materialFlows = listOf(flow(-2.0, 1), ActivityMaterialFlow("finished", q(5.0), 2.hours))
            )))),
            materials = listOf(ActivityMaterial("raw", q(0.0)), ActivityMaterial("finished", q(100.0))),
            events = listOf(ActivityMaterialEvent("raw", at(2), q(2.0)))
        ))
        val model = LinearMetaModel(name = "transform_stock")
        assertTrue(context.register(model).ok)
        assertFalse(context.validate(listOf(key(make, "transform", 0))).feasible)
        val keys = listOf(key(make, "transform", 1))
        model.setSolution(values(context, model, keys))
        val stock = assertNotNull(context.extractSolution(model).value).materialInventory.associateBy { it.material }
        assertTrue(stock.getValue("raw").levels.last().quantity.value eq Flt64.zero)
        assertTrue(stock.getValue("finished").levels.last().quantity.value eq Flt64(105.0))
        assertEveryBinaryAssignmentMatchesValidator(context, model)
        model.close()
    }

    @Test
    fun fltXMaterialEventsConvertGramsToKilogramsAtTheBoundary() {
        val problem = ActivitySchedulingProblem(
            horizon = TimeRange(at(0), at(4)),
            activities = listOf(Activity(use, listOf(ActivityMode(
                id = "mass",
                duration = 1.hours,
                starts = listOf(at(0)),
                resources = emptyList(),
                cost = Quantity(FltX.zero, NoneUnit),
                materialFlows = listOf(ActivityMaterialFlow("raw", Quantity(FltX("-500"), Gram)))
            )))),
            resources = emptyList(),
            costUnit = NoneUnit,
            materials = listOf(ActivityMaterial("raw", Quantity(FltX("1.25"), Kilogram)))
        )
        val context = assertNotNull(ActivitySchedulingContext.create(problem, GenericSolverValueAdapter(FltX)).value)
        val model = LinearMetaModel(name = "fltx_stock")
        assertTrue(context.register(model).ok)
        model.setSolution(listOf(Flt64.one))
        val stock = assertNotNull(context.extractSolution(model).value).materialInventory.single()
        assertEquals(Kilogram, stock.levels.last().quantity.unit)
        assertTrue(stock.levels.last().quantity.value eq FltX("0.75"))
        model.close()
    }

    @Test
    fun invalidMaterialReferencesUnitsAndOffsetsAreRejectedEvenWithoutStarts() {
        val material = ActivityMaterial("raw", q(1.0))
        fun withFlow(flow: ActivityMaterialFlow<Flt64>) = problem(
            activities = listOf(Activity(make, listOf(mode("bad", emptyList(), listOf(flow))))),
            materials = listOf(material)
        )
        val cases = listOf(
            withFlow(ActivityMaterialFlow("unknown", q(1.0))),
            withFlow(ActivityMaterialFlow("raw", Quantity(Flt64.one, Kilogram))),
            withFlow(flow(1.0, -1)),
            withFlow(flow(1.0, 2)),
            problem(emptyList(), listOf(material, material)),
            problem(emptyList(), listOf(ActivityMaterial("", q(1.0)))),
            problem(emptyList(), listOf(ActivityMaterial("raw", q(1.0), minimum = q(-1.0)))),
            problem(emptyList(), listOf(ActivityMaterial("raw", q(1.0), minimum = q(2.0), maximum = q(1.0)))),
            problem(emptyList(), listOf(material), listOf(ActivityMaterialEvent("unknown", at(0), q(1.0)))),
            problem(emptyList(), listOf(material), listOf(ActivityMaterialEvent("raw", at(-1), q(1.0)))),
            problem(emptyList(), listOf(material), listOf(ActivityMaterialEvent("raw", at(5), q(1.0)))),
            problem(emptyList(), listOf(material), listOf(ActivityMaterialEvent("raw", at(0), Quantity(Flt64.one, Kilogram))))
        )
        cases.forEachIndexed { index, input -> assertFalse(ActivitySchedulingContext.create(input, schedulingSolverValueAdapter).ok, "case=$index") }
    }

    @Test
    fun surplusAndShortageFixedScenariosRemainNormalInfeasibility() {
        for (quantity in listOf(-2.0, 2.0)) {
            val context = context(problem(
                activities = emptyList(),
                materials = listOf(ActivityMaterial("raw", q(1.0), maximum = q(2.0))),
                events = listOf(ActivityMaterialEvent("raw", at(2), q(quantity)))
            ))
            val model = LinearMetaModel(name = "fixed_stock")
            assertTrue(context.register(model).ok)
            assertFalse(context.validate(emptyList()).feasible)
            assertFalse(feasible(model, emptyList()))
            model.close()
        }
    }

    @Test
    fun materialUnitConversionCannotSilentlyEraseANonzeroQuantity() {
        val input = problem(
            activities = emptyList(),
            materials = listOf(ActivityMaterial("raw", Quantity(Flt64.one, Kilogram))),
            events = listOf(ActivityMaterialEvent("raw", at(0), Quantity(Flt64(1e-323), Gram)))
        )
        assertFalse(ActivitySchedulingContext.create(input, schedulingSolverValueAdapter).ok)
    }

    private fun problem(
        activities: List<Activity<Flt64>>,
        materials: List<ActivityMaterial<Flt64>>,
        events: List<ActivityMaterialEvent<Flt64>> = emptyList(),
        executions: List<ActivityExecution<Flt64>> = emptyList()
    ) = ActivitySchedulingProblem(
        horizon = TimeRange(at(0), at(4)),
        activities = activities,
        resources = emptyList(),
        executions = executions,
        costUnit = NoneUnit,
        materials = materials,
        materialEvents = events
    )

    private fun mode(id: String, starts: List<Int>, flows: List<ActivityMaterialFlow<Flt64>>) = ActivityMode(
        id = id,
        duration = 1.hours,
        starts = starts.map(::at),
        resources = emptyList(),
        cost = q(0.0),
        materialFlows = flows
    )

    private fun context(problem: ActivitySchedulingProblem<Flt64>) = assertNotNull(ActivitySchedulingContext.create(problem, schedulingSolverValueAdapter).value)
    private fun at(hour: Int) = epoch + hour.hours
    private fun q(value: Double) = Quantity(Flt64(value), NoneUnit)
    private fun key(task: Task, mode: String, hour: Int) = ActivityChoiceKey(task, mode, at(hour))
    private fun flow(value: Double, hour: Int = 0) = ActivityMaterialFlow(
        material = "raw",
        quantity = q(value),
        at = hour.hours
    )
    private fun feasible(model: LinearMetaModel<Flt64>, values: List<Flt64>) = model.constraints.all { it.isTrue(values, model.tokens, false) == true }

    private fun values(context: ActivitySchedulingContext<Flt64>, model: LinearMetaModel<Flt64>, keys: List<ActivityChoiceKey>): List<Flt64> {
        val selected = keys.mapNotNull { context.aggregation.variable(it) }.toSet() + context.problem.activities
            .filter { activity -> keys.none { it.task == activity.id } }.mapNotNull { context.aggregation.cancellationVariable(it.id) }
        return model.tokens.tokensInSolver.map { if (it.variable in selected) Flt64.one else Flt64.zero }
    }

    private fun assertEveryBinaryAssignmentMatchesValidator(context: ActivitySchedulingContext<Flt64>, model: LinearMetaModel<Flt64>) {
        val tokens = model.tokens.tokensInSolver
        assertTrue(tokens.size <= 12)
        for (mask in 0 until (1 shl tokens.size)) {
            val values = List(tokens.size) { if (mask and (1 shl it) == 0) Flt64.zero else Flt64.one }
            val keys = context.choices.filter { placement ->
                val variable = context.aggregation.variable(placement.key)
                values[tokens.indexOfFirst { it.variable == variable }] eq Flt64.one
            }.map { it.key }
            val cancellationCorrect = context.problem.activities.all { activity ->
                val cancellation = context.aggregation.cancellationVariable(activity.id)
                cancellation == null || ((values[tokens.indexOfFirst { it.variable == cancellation }] eq Flt64.one) == keys.none { it.task == activity.id })
            }
            assertEquals(cancellationCorrect && context.validate(keys).feasible, feasible(model, values), "mask=$mask")
        }
    }
}
