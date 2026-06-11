@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.CapacityIntermediateValues
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.SlotBasedCapacityResult
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.SlotConstraints
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.TaskReverseBuilder
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.convertTo
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ActionAllocation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractPlannedTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTaskBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.GenericSolverValueAdapter
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow

private val stubTimeRange = TimeRange(
    start = Instant.parse("2024-01-01T08:00:00Z"),
    end = Instant.parse("2024-01-01T10:00:00Z")
)

private object StubAction : ProductionAction {
    override val id = "stub"
    override val name = "stub"
    override val executor = Executor("e1", "executor-1")
    override val discrete = false
    override fun <V : RealNumber<V>> unitCapacity(timeWindow: TimeWindow<V>): V =
        throw UnsupportedOperationException("stub")
    override fun <V : RealNumber<V>> unitCost(time: Instant, fromDouble: (Double) -> V): V =
        throw UnsupportedOperationException("stub")
    override fun <V : RealNumber<V>> upperBound(slot: TimeSlot, timeWindow: TimeWindow<V>): UInt64 =
        throw UnsupportedOperationException("stub")
}

class SlotBasedCapacityResultFltXTest {
    private val slot = stubTimeRange
    private val productA = "product-A"
    private val materialB = "material-B"
    private val resourceC = "resource-C"
    private val adapter = GenericSolverValueAdapter(FltX)

    @Test
    fun solverCapacityIntermediateValuesShouldConvertToGenericQuantityValues() {
        val result = SlotBasedCapacityResult<ProductionAction, String, String, Flt64>(
            slot = slot,
            slotIndex = 0,
            actionAllocations = emptyList(),
            totalCostQuantityValue = Quantity(Flt64(12.5), NoneUnit),
            produceQuantityByProduct = mapOf(productA to Quantity(Flt64(100.0), NoneUnit)),
            consumptionQuantityByMaterial = mapOf(materialB to Quantity(Flt64(50.0), NoneUnit)),
            resourceUsageQuantityByResource = mapOf(resourceC to Quantity(Flt64(75.5), NoneUnit))
        )
        val intermediate = CapacityIntermediateValues<ProductionAction, String, String, Flt64>(
            slots = listOf(slot),
            results = mapOf(slot to result)
        )

        val converted = intermediate.convertTo(adapter)

        assertTrue(converted.results[slot]!!.totalCostQuantityValue.value eq FltX("12.5"))
        assertTrue(converted.produceQuantity(slot, productA)!!.value eq FltX("100.0"))
        assertTrue(converted.consumptionQuantity(slot, materialB)!!.value eq FltX("50.0"))
        assertTrue(converted.resourceUsageQuantity(slot, resourceC)!!.value eq FltX("75.5"))
        assertEquals(NoneUnit, converted.results[slot]!!.totalCostQuantityValue.unit)
    }

    @Test
    fun slotBasedCapacityResultShouldSupportFltX() {
        val result = SlotBasedCapacityResult<ProductionAction, String, String, FltX>(
            slot = slot,
            slotIndex = 0,
            actionAllocations = listOf(
                ActionAllocation(
                    action = StubAction,
                    slot = slot,
                    slotIndex = 0,
                    amount = UInt64(3),
                    duration = 2.hours
                )
            ),
            totalCostQuantityValue = Quantity(FltX("12.50"), NoneUnit),
            produceQuantityByProduct = mapOf(productA to Quantity(FltX("100.0"), NoneUnit)),
            consumptionQuantityByMaterial = mapOf(materialB to Quantity(FltX("50.0"), NoneUnit)),
            resourceUsageQuantityByResource = mapOf(resourceC to Quantity(FltX("75.5"), NoneUnit))
        )

        assertTrue(result.totalCostQuantityValue.value eq FltX("12.50"))
        assertTrue(result.produceQuantityByProduct[productA]!!.value eq FltX("100.0"))
        assertTrue(result.consumptionQuantityByMaterial[materialB]!!.value eq FltX("50.0"))
        assertTrue(result.resourceUsageQuantityByResource[resourceC]!!.value eq FltX("75.5"))
        assertEquals(1, result.actionAllocations.size)
        assertEquals(0, result.slotIndex)
    }

    @Test
    fun slotBasedCapacityResultShouldSupportFltXQuantityFields() {
        val result = SlotBasedCapacityResult<ProductionAction, String, String, FltX>(
            slot = slot,
            slotIndex = 0,
            actionAllocations = emptyList(),
            totalCostQuantityValue = Quantity(FltX("12.50"), NoneUnit),
            produceQuantityByProduct = mapOf(productA to Quantity(FltX("100.0"), NoneUnit)),
            consumptionQuantityByMaterial = mapOf(materialB to Quantity(FltX("50.0"), NoneUnit)),
            resourceUsageQuantityByResource = mapOf(resourceC to Quantity(FltX("75.5"), NoneUnit))
        )

        assertTrue(result.totalCostQuantityValue.value eq FltX("12.50"))
        assertEquals(NoneUnit, result.totalCostQuantityValue.unit)
        assertEquals(NoneUnit, result.totalCostQuantity().unit)
        assertTrue(result.produceQuantityByProduct[productA]!!.value eq FltX("100.0"))
        assertEquals(NoneUnit, result.produceQuantityByProduct[productA]!!.unit)
        assertTrue(result.consumptionQuantityByMaterial[materialB]!!.value eq FltX("50.0"))
        assertEquals(NoneUnit, result.consumptionQuantityByMaterial[materialB]!!.unit)
        assertTrue(result.resourceUsageQuantityByResource[resourceC]!!.value eq FltX("75.5"))
        assertEquals(NoneUnit, result.resourceUsageQuantityByResource[resourceC]!!.unit)
    }

    @Test
    fun capacityIntermediateValuesShouldSupportFltX() {
        val slotResult = SlotBasedCapacityResult<ProductionAction, String, String, FltX>(
            slot = slot,
            slotIndex = 0,
            actionAllocations = emptyList(),
            totalCostQuantityValue = Quantity(FltX("5.0"), NoneUnit),
            produceQuantityByProduct = mapOf(productA to Quantity(FltX("20.0"), NoneUnit)),
            consumptionQuantityByMaterial = mapOf(materialB to Quantity(FltX("10.0"), NoneUnit)),
            resourceUsageQuantityByResource = mapOf(resourceC to Quantity(FltX("15.0"), NoneUnit))
        )

        val intermediate = CapacityIntermediateValues<ProductionAction, String, String, FltX>(
            slots = listOf(slot),
            results = mapOf(slot to slotResult)
        )

        assertTrue(intermediate.produceQuantity(slot, productA)!!.value eq FltX("20.0"))
        assertTrue(intermediate.consumptionQuantity(slot, materialB)!!.value eq FltX("10.0"))
        assertTrue(intermediate.resourceUsageQuantity(slot, resourceC)!!.value eq FltX("15.0"))
    }

    @Test
    fun slotConstraintsFromShouldWorkWithFltX() {
        val result = SlotBasedCapacityResult<ProductionAction, String, String, FltX>(
            slot = slot,
            slotIndex = 0,
            actionAllocations = emptyList(),
            totalCostQuantityValue = Quantity(FltX("0"), NoneUnit),
            produceQuantityByProduct = mapOf(productA to Quantity(FltX("5.0"), NoneUnit)),
            consumptionQuantityByMaterial = mapOf(materialB to Quantity(FltX("3.0"), NoneUnit)),
            resourceUsageQuantityByResource = mapOf(resourceC to Quantity(FltX("7.0"), NoneUnit))
        )

        val constraints = SlotConstraints.from(result)

        assertTrue(constraints.maxProduceQuantity[productA]!!.value eq FltX("5.0"))
        assertTrue(constraints.minProduceQuantity[productA]!!.value eq FltX("5.0"))
        assertTrue(constraints.maxConsumptionQuantity[materialB]!!.value eq FltX("3.0"))
        assertTrue(constraints.minConsumptionQuantity[materialB]!!.value eq FltX("3.0"))
        assertTrue(constraints.maxResourceUsageQuantity[resourceC]!!.value eq FltX("7.0"))
        assertTrue(constraints.minResourceUsageQuantity[resourceC]!!.value eq FltX("7.0"))
        assertEquals(0, constraints.slotIndex)
    }

    @Test
    fun slotConstraintsShouldSupportFltXQuantityFields() {
        val constraints = SlotConstraints<String, String, FltX>(
            slot = slot,
            slotIndex = 0,
            maxProduceQuantity = mapOf(productA to Quantity(FltX("5.0"), NoneUnit)),
            minProduceQuantity = mapOf(productA to Quantity(FltX("4.0"), NoneUnit)),
            maxConsumptionQuantity = mapOf(materialB to Quantity(FltX("3.0"), NoneUnit)),
            minConsumptionQuantity = mapOf(materialB to Quantity(FltX("2.0"), NoneUnit)),
            maxResourceUsageQuantity = mapOf(resourceC to Quantity(FltX("7.0"), NoneUnit)),
            minResourceUsageQuantity = mapOf(resourceC to Quantity(FltX("6.0"), NoneUnit))
        )

        assertTrue(constraints.maxProduceQuantity[productA]!!.value eq FltX("5.0"))
        assertEquals(NoneUnit, constraints.maxProduceQuantity[productA]!!.unit)
        assertTrue(constraints.minProduceQuantity[productA]!!.value eq FltX("4.0"))
        assertEquals(NoneUnit, constraints.minProduceQuantity[productA]!!.unit)
        assertTrue(constraints.maxConsumptionQuantity[materialB]!!.value eq FltX("3.0"))
        assertEquals(NoneUnit, constraints.maxConsumptionQuantity[materialB]!!.unit)
        assertTrue(constraints.minConsumptionQuantity[materialB]!!.value eq FltX("2.0"))
        assertEquals(NoneUnit, constraints.minConsumptionQuantity[materialB]!!.unit)
        assertTrue(constraints.maxResourceUsageQuantity[resourceC]!!.value eq FltX("7.0"))
        assertEquals(NoneUnit, constraints.maxResourceUsageQuantity[resourceC]!!.unit)
        assertTrue(constraints.minResourceUsageQuantity[resourceC]!!.value eq FltX("6.0"))
        assertEquals(NoneUnit, constraints.minResourceUsageQuantity[resourceC]!!.unit)
    }

    @Test
    fun slotConstraintsFromWithToleranceShouldWorkWithFltX() {
        val result = SlotBasedCapacityResult<ProductionAction, String, String, FltX>(
            slot = slot,
            slotIndex = 0,
            actionAllocations = emptyList(),
            totalCostQuantityValue = Quantity(FltX("0"), NoneUnit),
            produceQuantityByProduct = mapOf(productA to Quantity(FltX("5.0"), NoneUnit)),
            consumptionQuantityByMaterial = mapOf(materialB to Quantity(FltX("3.0"), NoneUnit)),
            resourceUsageQuantityByResource = mapOf(resourceC to Quantity(FltX("7.0"), NoneUnit))
        )

        val tolerance = FltX("1.5")
        val constraints = SlotConstraints.from(result, tolerance)

        // max = value + tolerance
        assertTrue(constraints.maxProduceQuantity[productA]!!.value eq FltX("6.5"))
        assertTrue(constraints.maxConsumptionQuantity[materialB]!!.value eq FltX("4.5"))
        assertTrue(constraints.maxResourceUsageQuantity[resourceC]!!.value eq FltX("8.5"))

        // min = max(value - tolerance, zero) -- all positive here
        assertTrue(constraints.minProduceQuantity[productA]!!.value eq FltX("3.5"))
        assertTrue(constraints.minConsumptionQuantity[materialB]!!.value eq FltX("1.5"))
        assertTrue(constraints.minResourceUsageQuantity[resourceC]!!.value eq FltX("5.5"))
    }

    @Test
    fun slotConstraintsFromWithToleranceClampsAtZero() {
        val result = SlotBasedCapacityResult<ProductionAction, String, String, FltX>(
            slot = slot,
            slotIndex = 0,
            actionAllocations = emptyList(),
            totalCostQuantityValue = Quantity(FltX("0"), NoneUnit),
            produceQuantityByProduct = mapOf(productA to Quantity(FltX("1.0"), NoneUnit)),
            consumptionQuantityByMaterial = emptyMap(),
            resourceUsageQuantityByResource = emptyMap()
        )

        // tolerance > value, so min should clamp at zero
        val tolerance = FltX("5.0")
        val constraints = SlotConstraints.from(result, tolerance)

        assertTrue(constraints.maxProduceQuantity[productA]!!.value eq FltX("6.0"))
        assertTrue(constraints.minProduceQuantity[productA]!!.value eq FltX("0"))
    }

    @Test
    fun capacityIntermediateValuesSlotConstraintsShouldWorkWithFltX() {
        val result = SlotBasedCapacityResult<ProductionAction, String, String, FltX>(
            slot = slot,
            slotIndex = 0,
            actionAllocations = emptyList(),
            totalCostQuantityValue = Quantity(FltX("0"), NoneUnit),
            produceQuantityByProduct = mapOf(productA to Quantity(FltX("10.0"), NoneUnit)),
            consumptionQuantityByMaterial = emptyMap(),
            resourceUsageQuantityByResource = emptyMap()
        )

        val intermediate = CapacityIntermediateValues<ProductionAction, String, String, FltX>(
            slots = listOf(slot),
            results = mapOf(slot to result)
        )

        val constraints = intermediate.slotConstraints(slot)
        assertNotNull(constraints)
        assertTrue(constraints!!.maxProduceQuantity[productA]!!.value eq FltX("10.0"))
    }

    @Test
    fun taskReverseBuilderCanBeInstantiatedWithFltX() {
        // 校验泛型实现接受 FltX 数值类型 / Verifies generic implementation accepts FltX as its V type.
        class FltXTaskReverseBuilder : TaskReverseBuilder<
                AbstractTaskBunch<AbstractPlannedTask<*, Executor, AssignmentPolicy<Executor>>, Executor, AssignmentPolicy<Executor>, FltX>,
                FltX,
                AbstractPlannedTask<*, Executor, AssignmentPolicy<Executor>>,
                Executor,
                AssignmentPolicy<Executor>>()
        val builder = FltXTaskReverseBuilder()
        assertNotNull(builder)
    }
}
