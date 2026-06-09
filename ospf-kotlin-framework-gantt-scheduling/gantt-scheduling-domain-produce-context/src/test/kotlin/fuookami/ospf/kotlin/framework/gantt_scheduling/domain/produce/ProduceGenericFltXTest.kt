@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce

import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacityColumn
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.CapacityActionProduce
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.Material
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.MaterialDemand
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.MaterialReserves
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.ProductionTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.consumptionV
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.produceV
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.toSolverValue
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow

/**
 * 补充覆盖 FltX 泛型路径与 solver 转换边界。
 * Additional FltX tests covering V-generic paths and solver conversion boundaries.
 */
class ProduceGenericFltXTest {

    // 简单测试物料实现 / Simple test material implementation
    private class TestMaterial(override val index: Int, val label: String) : Material {
        override val material: Material get() = this
        override fun toString() = label
        override fun equals(other: Any?) = other is TestMaterial && index == other.index
        override fun hashCode() = index.hashCode()
    }

    @Suppress("unused")
    private val productA = TestMaterial(0, "productA")
    @Suppress("unused")
    private val productB = TestMaterial(1, "productB")

    // ---- MaterialDemand solver conversion boundary / MaterialDemand solver 转换边界 ----

    @Test
    fun materialDemandFltXShouldConvertAtSolverBoundary() {
        val range = ValueRange(FltX("0"), FltX("100"), Interval.Closed, Interval.Closed, FltX).value!!
        val demand = MaterialDemand<FltX>(
            quantityRangeValue = Quantity(range, NoneUnit),
            lessQuantityValue = Quantity(FltX("5.0"), NoneUnit),
            overQuantityValue = Quantity(FltX("3.0"), NoneUnit)
        )
        val solverUpperBound = demand.quantityRangeValue.value.upperBound.value.unwrap().toSolverValue()
        assertTrue(solverUpperBound eq Flt64(100.0))
        val solverLessQuantity = demand.lessQuantityValue!!.value.toSolverValue()
        assertTrue(solverLessQuantity eq Flt64(5.0))
    }

    // ---- MaterialReserves solver conversion boundary / MaterialReserves solver 转换边界 ----

    @Test
    fun materialReservesFltXShouldConvertAtSolverBoundary() {
        val range = ValueRange(FltX("10"), FltX("500"), Interval.Closed, Interval.Closed, FltX).value!!
        val reserves = MaterialReserves<FltX>(
            quantityRangeValue = Quantity(range, NoneUnit),
            overQuantityValue = Quantity(FltX("15.0"), NoneUnit)
        )
        val solverLowerBound = reserves.quantityRangeValue.value.lowerBound.value.unwrap().toSolverValue()
        assertTrue(solverLowerBound eq Flt64(10.0))
        val solverOverQuantity = reserves.overQuantityValue!!.value.toSolverValue()
        assertTrue(solverOverQuantity eq Flt64(15.0))
    }

    // ---- MaterialDemand FltX precision / MaterialDemand FltX 精度 ----

    @Test
    fun materialDemandWithHighPrecisionFltX() {
        val range = ValueRange(
            FltX("0.000001"), FltX("999999.999999"),
            Interval.Closed, Interval.Closed, FltX
        ).value!!
        val demand = MaterialDemand<FltX>(
            quantityRangeValue = Quantity(range, NoneUnit),
            lessQuantityValue = Quantity(FltX("0.001"), NoneUnit),
            overQuantityValue = Quantity(FltX("0.001"), NoneUnit)
        )
        assertTrue(demand.quantityRangeValue.value.lowerBound.value eq FltX("0.000001"))
        assertTrue(demand.quantityRangeValue.value.upperBound.value eq FltX("999999.999999"))
    }

    @Test
    fun materialDemandQuantityShouldSupportFltX() {
        val range = ValueRange(
            lb = FltX("0"),
            ub = FltX("100"),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = FltX
        ).value!!
        val demand = MaterialDemand<FltX>(
            quantityRangeValue = Quantity(range, NoneUnit),
            lessQuantityValue = Quantity(FltX("1.5"), NoneUnit),
            overQuantityValue = Quantity(FltX("2.5"), NoneUnit)
        )

        val quantityRange = demand.quantityRange()
        val lessQuantity = demand.lessQuantity()
        val overQuantity = demand.overQuantity()

        assertEquals(NoneUnit, quantityRange.unit)
        assertTrue(quantityRange.value.upperBound.value eq FltX("100"))
        assertEquals(NoneUnit, lessQuantity!!.unit)
        assertTrue(lessQuantity.value eq FltX("1.5"))
        assertEquals(NoneUnit, overQuantity!!.unit)
        assertTrue(overQuantity.value eq FltX("2.5"))
    }

    @Test
    fun materialDemandShouldSupportFltXQuantityFields() {
        val range = ValueRange(
            lb = FltX("0"),
            ub = FltX("100"),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = FltX
        ).value!!
        val demand = MaterialDemand<FltX>(
            quantityRangeValue = Quantity(range, NoneUnit),
            lessQuantityValue = Quantity(FltX("1.5"), NoneUnit),
            overQuantityValue = Quantity(FltX("2.5"), NoneUnit)
        )

        assertTrue(demand.quantityRangeValue.value.upperBound.value eq FltX("100"))
        assertTrue(demand.lessQuantityValue!!.value eq FltX("1.5"))
        assertTrue(demand.overQuantityValue!!.value eq FltX("2.5"))
        assertEquals(NoneUnit, demand.quantityRangeValue.unit)
        assertEquals(NoneUnit, demand.lessQuantityValue!!.unit)
        assertEquals(NoneUnit, demand.overQuantityValue!!.unit)
    }

    @Test
    fun materialReservesQuantityShouldSupportFltX() {
        val range = ValueRange(
            lb = FltX("10"),
            ub = FltX("200"),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = FltX
        ).value!!
        val reserves = MaterialReserves<FltX>(
            quantityRangeValue = Quantity(range, NoneUnit),
            lessQuantityValue = Quantity(FltX("3.5"), NoneUnit),
            overQuantityValue = Quantity(FltX("4.5"), NoneUnit)
        )

        assertEquals(NoneUnit, reserves.quantityRange().unit)
        assertTrue(reserves.quantityRange().value.lowerBound.value eq FltX("10"))
        assertTrue(reserves.lessQuantity()!!.value eq FltX("3.5"))
        assertTrue(reserves.overQuantity()!!.value eq FltX("4.5"))
    }

    @Test
    fun materialReservesShouldSupportFltXQuantityFields() {
        val range = ValueRange(
            lb = FltX("10"),
            ub = FltX("200"),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = FltX
        ).value!!
        val reserves = MaterialReserves<FltX>(
            quantityRangeValue = Quantity(range, NoneUnit),
            lessQuantityValue = Quantity(FltX("3.5"), NoneUnit),
            overQuantityValue = Quantity(FltX("4.5"), NoneUnit)
        )

        assertTrue(reserves.quantityRangeValue.value.lowerBound.value eq FltX("10"))
        assertTrue(reserves.lessQuantityValue!!.value eq FltX("3.5"))
        assertTrue(reserves.overQuantityValue!!.value eq FltX("4.5"))
        assertEquals(NoneUnit, reserves.quantityRangeValue.unit)
        assertEquals(NoneUnit, reserves.lessQuantityValue!!.unit)
        assertEquals(NoneUnit, reserves.overQuantityValue!!.unit)
    }

    @Test
    fun productionTaskQuantityShouldSupportFltX() {
        val task = object : ProductionTask<Executor, AssignmentPolicy<Executor>, Material, Material, FltX> {
            override val index = 0
            override val id = "produce-quantity-task"
            override val name = "Produce Quantity Task"
            override val produceQuantityByProduct = mapOf<Material, Quantity<FltX>>(
                productA to Quantity(FltX("6.25"), NoneUnit)
            )
            override val consumptionQuantityByMaterial = mapOf<Material, Quantity<FltX>>(
                productB to Quantity(FltX("2.75"), NoneUnit)
            )

            override fun partialEq(rhs: AbstractTask<Executor, AssignmentPolicy<Executor>>): Boolean? {
                return this === rhs
            }
        }

        val produceQuantity = task.produceQuantity(productA)
        val consumptionQuantity = task.consumptionQuantity(productB)

        assertEquals(NoneUnit, produceQuantity!!.unit)
        assertTrue(produceQuantity.value eq FltX("6.25"))
        assertEquals(NoneUnit, consumptionQuantity!!.unit)
        assertTrue(consumptionQuantity.value eq FltX("2.75"))
    }

    @Test
    fun productionTaskQuantityMapsShouldSupportFltX() {
        val task = object : ProductionTask<Executor, AssignmentPolicy<Executor>, Material, Material, FltX> {
            override val index = 1
            override val id = "produce-quantity-map-task"
            override val name = "Produce Quantity Map Task"
            override val produceQuantityByProduct = mapOf<Material, Quantity<FltX>>(
                productA to Quantity(FltX("8.25"), NoneUnit)
            )
            override val consumptionQuantityByMaterial = mapOf<Material, Quantity<FltX>>(
                productB to Quantity(FltX("4.75"), NoneUnit)
            )

            override fun partialEq(rhs: AbstractTask<Executor, AssignmentPolicy<Executor>>): Boolean? {
                return this === rhs
            }
        }

        val produceQuantity = task.produceQuantityByProduct[productA]
        val consumptionQuantity = task.consumptionQuantityByMaterial[productB]

        assertEquals(NoneUnit, produceQuantity!!.unit)
        assertTrue(produceQuantity.value eq FltX("8.25"))
        assertEquals(NoneUnit, consumptionQuantity!!.unit)
        assertTrue(consumptionQuantity.value eq FltX("4.75"))
    }

    @Test
    fun capacityColumnProduceAndConsumptionShouldSupportFltX() {
        val executor = Executor("executor-1", "Executor 1")
        val action = object : ProductionAction, CapacityActionProduce<Material, Material, FltX> {
            override val id = "action-1"
            override val name = "Action 1"
            override val executor = executor
            override val discrete = true
            override val produce = mapOf<Material, FltX>(productA to FltX("2.5"))
            override val consumption = mapOf<Material, FltX>(productB to FltX("1.25"))

            override fun unitCapacity(timeWindow: TimeWindow<Flt64>) =
                throw UnsupportedOperationException("stub")

            override fun unitCost(time: Instant) =
                throw UnsupportedOperationException("stub")

            override fun upperBound(slot: TimeSlot, timeWindow: TimeWindow<Flt64>) =
                throw UnsupportedOperationException("stub")
        }
        val column = CapacityColumn<Executor, ProductionAction, FltX>(
            executor = executor,
            slotIndex = 0,
            order = 0,
            allocations = mapOf(action to UInt64(4)),
            columnCost = Quantity(FltX("0"), NoneUnit)
        )

        val produce = column.produceV(productA) { FltX(it.toLong()) }
        val consumption = column.consumptionV(productB) { FltX(it.toLong()) }

        assertTrue(produce eq FltX("10.0"))
        assertTrue(consumption eq FltX("5.00"))
    }
}
