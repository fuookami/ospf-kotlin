package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.AbstractMaterial
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.Consumption
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.Material
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.Produce
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.GenericSolverValueAdapter
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.SchedulingSolverValueAdapter
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

class ProduceSolvedQuantityFltXTest {
    private val product = QuantityMaterial(index = 0, id = "product")
    private val material = QuantityMaterial(index = 1, id = "material")
    private val model = LinearMetaModel(
        name = "produce-solved-quantity-test",
        converter = SchedulingSolverValueAdapter.Flt64
    )
    private val adapter = GenericSolverValueAdapter(FltX)

    @Test
    fun produceShouldExposeSolvedFltXQuantities() {
        val produce = ConstantProduce(
            material = product,
            quantityValue = Flt64(8.5),
            overValue = Flt64(1.5),
            lessValue = Flt64(0.25)
        )

        val quantity = produce.solvedQuantity(
            product = product,
            model = model,
            adapter = adapter
        )
        val overQuantity = produce.solvedOverQuantity(
            product = product,
            model = model,
            adapter = adapter
        )
        val lessQuantity = produce.solvedLessQuantity(
            product = product,
            model = model,
            adapter = adapter
        )

        assertEquals(NoneUnit, quantity!!.unit)
        assertTrue(quantity.value eq FltX("8.5"))
        assertTrue(overQuantity!!.value eq FltX("1.5"))
        assertTrue(lessQuantity!!.value eq FltX("0.25"))
    }

    @Test
    fun consumptionShouldExposeSolvedFltXQuantities() {
        val consumption = ConstantConsumption(
            material = material,
            quantityValue = Flt64(6.5),
            overValue = Flt64(2.5),
            lessValue = Flt64(0.75)
        )

        val quantity = consumption.solvedQuantity(
            material = material,
            model = model,
            adapter = adapter
        )
        val overQuantity = consumption.solvedOverQuantity(
            material = material,
            model = model,
            adapter = adapter
        )
        val lessQuantity = consumption.solvedLessQuantity(
            material = material,
            model = model,
            adapter = adapter
        )

        assertEquals(NoneUnit, quantity!!.unit)
        assertTrue(quantity.value eq FltX("6.5"))
        assertTrue(overQuantity!!.value eq FltX("2.5"))
        assertTrue(lessQuantity!!.value eq FltX("0.75"))
    }
}

private data class QuantityMaterial(
    override val index: Int,
    val id: String
) : Material {
    override val material: Material get() = this
}

private class ConstantProduce(
    material: AbstractMaterial,
    quantityValue: Flt64,
    overValue: Flt64,
    lessValue: Flt64
) : Produce {
    override val quantity = constantSymbols("produce_quantity", material.index + 1, quantityValue)
    override val overQuantity = constantSymbols("produce_over_quantity", material.index + 1, overValue)
    override val lessQuantity = constantSymbols("produce_less_quantity", material.index + 1, lessValue)
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        return ok
    }

    private fun constantSymbols(
        name: String,
        size: Int,
        value: Flt64
    ): LinearIntermediateSymbols1<Flt64> {
        return LinearIntermediateSymbols1(
            name = name,
            shape = Shape1(size)
        ) { _, _ ->
            LinearExpressionSymbol(value, name = name)
        }
    }
}

private class ConstantConsumption(
    material: AbstractMaterial,
    quantityValue: Flt64,
    overValue: Flt64,
    lessValue: Flt64
) : Consumption {
    override val quantity = constantSymbols("consumption_quantity", material.index + 1, quantityValue)
    override val overQuantity = constantSymbols("consption_over_quantity", material.index + 1, overValue)
    override val lessQuantity = constantSymbols("consumption_less_quantity", material.index + 1, lessValue)
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        return ok
    }

    private fun constantSymbols(
        name: String,
        size: Int,
        value: Flt64
    ): LinearIntermediateSymbols1<Flt64> {
        return LinearIntermediateSymbols1(
            name = name,
            shape = Shape1(size)
        ) { _, _ ->
            LinearExpressionSymbol(value, name = name)
        }
    }
}
