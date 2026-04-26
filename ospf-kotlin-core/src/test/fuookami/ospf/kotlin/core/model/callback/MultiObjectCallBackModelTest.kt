package fuookami.ospf.kotlin.core.model.callback

import fuookami.ospf.kotlin.core.model.basic.MultiObjectLocation
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Order
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultiObjectCallBackModelTest {
    @Test
    fun objectiveValueShouldMapByObjectiveLocationPriority() {
        val location1 = MultiObjectLocation(priority = UInt64(10), weight = Flt64(2))
        val location2 = MultiObjectLocation(priority = UInt64(20), weight = Flt64(3))
        val model = MultiObjectCallBackModel(
            objectiveLocation = listOf(location1, location2)
        )

        val value = model.objectiveValue(
            listOf(
                location2 to Flt64(2),
                location1 to Flt64(3),
                MultiObjectLocation(priority = UInt64(999), weight = Flt64.one) to Flt64(10)
            )
        )

        assertEquals(listOf(Flt64(6), Flt64(6)), value)
    }

    @Test
    fun compareObjectiveShouldUseLexicographicOrderForMinimum() {
        val model = MultiObjectCallBackModel(
            objectCategory = ObjectCategory.Minimum,
            objectiveLocation = listOf(
                MultiObjectLocation(priority = UInt64.zero, weight = Flt64.one),
                MultiObjectLocation(priority = UInt64.one, weight = Flt64.one)
            )
        )

        assertTrue(model.compareObjective(listOf(Flt64.one, Flt64(100)), listOf(Flt64(2), Flt64.zero)) is Order.Less)
        assertTrue(model.compareObjective(listOf(Flt64.one, Flt64.one), listOf(Flt64.one, Flt64.one)) is Order.Equal)
    }

    @Test
    fun compareObjectiveShouldUseLexicographicOrderForMaximum() {
        val model = MultiObjectCallBackModel(
            objectCategory = ObjectCategory.Maximum,
            objectiveLocation = listOf(
                MultiObjectLocation(priority = UInt64.zero, weight = Flt64.one),
                MultiObjectLocation(priority = UInt64.one, weight = Flt64.one)
            )
        )

        assertTrue(model.compareObjective(listOf(Flt64(10), Flt64.zero), listOf(Flt64(8), Flt64(100))) is Order.Less)
        assertTrue(model.compareObjective(listOf(Flt64(8), Flt64.zero), listOf(Flt64(10), Flt64(100))) is Order.Greater)
    }

    @Test
    fun addObjectShouldNormalizeReverseCategoryToModelCategory() {
        val location = MultiObjectLocation(priority = UInt64.zero, weight = Flt64.one)
        val model = MultiObjectCallBackModel(
            objectCategory = ObjectCategory.Minimum,
            objectiveLocation = listOf(location)
        )

        model.addObject(
            category = ObjectCategory.Maximum,
            func = { Flt64(5) },
            location = location
        )

        val objective = model.objective(emptyList())

        assertEquals(listOf(Flt64(-5)), objective)
    }
}
