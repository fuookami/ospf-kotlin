package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceAggregation

class ProduceBatchIndexTest {
    @Test
    fun `sparse batch registration matches full scans including first contributions and units`(): Unit = runBlocking {
        val products = (0..3).map { index ->
            Product(
                id = productIdOf("p$index"),
                name = "p$index",
                width = listOf(Quantity(Flt64.one, Meter))
            )
        }
        val materials = (0..2).map { index ->
            Material(
                id = materialIdOf("m$index"),
                name = "m$index",
                widthRange = WidthRange(
                    width = QuantityRange(Quantity(Flt64.one, Meter), Quantity(Flt64(10), Meter)),
                    step = Quantity(Flt64.one, Meter)
                )
            )
        }
        val machines = (0..2).map { index ->
            Machine(
                id = machineIdOf("machine$index"),
                name = "machine$index",
                capacity = if (index == 2) null else Quantity(Flt64(100), Kilogram)
            )
        }
        val demands = products.flatMap { product ->
            listOf(ProductDemand(product, Quantity(Flt64(10), Kilogram)), ProductDemand(product, Quantity(Flt64(10), Meter)))
        }
        val plans = (0..11).map { index ->
            val product = products[index % 3]
            CuttingPlan(
                id = cuttingPlanIdOf("plan$index"),
                material = materials[index % 2],
                machineId = if (index % 4 == 3) null else machines[index % 3].id,
                slices = listOf(CuttingPlanSlice(product, product.width.first())),
                demandContributions = listOf(
                    CuttingPlanDemandContribution(product, Quantity(Flt64(index + 1), Meter)),
                    CuttingPlanDemandContribution(product, Quantity(Flt64(index + 2), Kilogram)),
                    CuttingPlanDemandContribution(product, Quantity(Flt64(999), Kilogram))
                ),
                capacityConsumption = when (index % 4) {
                    0 -> Quantity(Flt64(index + 1), Kilogram)
                    1 -> Quantity(Flt64(-1), Kilogram)
                    2 -> Quantity(Flt64.one, Meter)
                    else -> null
                }
            )
        }
        val aggregation = ProduceAggregation(
            cuttingPlans = plans.take(4),
            demands = demands,
            materials = materials,
            machines = machines
        )
        val model = LinearMetaModel<Flt64>(name = "produce-index", converter = IntoValue.Identity)
        assertIs<Ok<*, *, *>>(aggregation.register(model))
        val symbols = aggregation.demandQuantity.toList() + aggregation.materialQuantity.toList() +
            aggregation.machineBatchQuantity.toList() + aggregation.machineCapacityQuantity.toList()
        for ((index, symbol) in symbols.withIndex()) {
            assertIs<Ok<*, *, *>>(model.addConstraint(LinearPolynomial(symbol) leq Flt64(1000), name = "check_$index"))
        }
        var active = plans.take(4)
        suspend fun check() {
            val snapshot = assertIs<Ok<LinearModelSnapshot<Flt64>, *, *>>(model.state.refresh()).value
            val variables = aggregation.xGroups.flatMap { it.toList() }
            assertEquals(active.size, variables.size)
            val expected = ArrayList<List<Flt64>>()
            for (demand in demands) expected.add(active.map { plan ->
                plan.demandContributions.firstOrNull {
                    it.product.id == demand.product.id && it.quantity.unit == demand.quantity.unit
                }?.quantity?.value ?: Flt64.zero
            })
            for (material in materials) expected.add(active.map { if (it.material.id == material.id) Flt64.one else Flt64.zero })
            for (machine in machines) expected.add(active.map { if (it.machineId == machine.id) Flt64.one else Flt64.zero })
            for (machine in machines) expected.add(active.map { plan ->
                val amount = plan.capacityConsumption
                if (plan.machineId == machine.id && machine.capacity != null && amount != null &&
                    amount.unit == machine.capacity!!.unit && amount.value > Flt64.zero) amount.value else Flt64.zero
            })
            for ((index, coefficients) in expected.withIndex()) {
                val row = snapshot.rows.single { it.sourceName == "check_$index" }
                assertEquals(coefficients, variables.map { row.coefficients[it] ?: Flt64.zero }, "row $index")
            }
        }
        check()
        for (iteration in 1..2) {
            val batch = plans.subList(iteration * 4, (iteration + 1) * 4)
            assertEquals(batch, assertIs<Ok<List<CuttingPlan<Flt64>>, *, *>>(aggregation.addColumns(
                iteration = UInt64(iteration),
                newPlans = batch,
                model = model
            )).value)
            active = active + batch
            check()
        }
        model.close()
    }
}
