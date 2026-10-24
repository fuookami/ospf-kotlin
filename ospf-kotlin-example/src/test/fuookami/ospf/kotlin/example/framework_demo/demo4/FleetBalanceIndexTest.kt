@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

class FleetBalanceIndexTest {
    @Test
    fun `arrival buckets preserve first origin and per batch occurrence order`() = runBlocking {
        ManualIndexed.flush<Aircraft>()
        val fixture = demo4TestFixture()
        val compilation = Compilation(
            tasks = listOf(fixture.firstTask, fixture.secondTask),
            executors = listOf(fixture.aircraft)
        )
        val firstOrigin = FlightTaskBunch(
            aircraft = fixture.aircraft,
            tasks = listOf(fixture.firstTask),
            iteration = Int64.zero,
            cost = demo4Cost(1.0)
        )
        val fleet = FleetBalance(
            aircrafts = listOf(fixture.aircraft),
            originBunches = listOf(firstOrigin, fixture.originBunch),
            compilation = compilation
        )
        assertEquals(fixture.destinationAirport, fleet.limits.single().first.airport)
        val model = LinearMetaModel(name = "fleet-index", converter = IntoValue.Identity)
        assertTrue(compilation.register(model) is Ok)
        fleet.fleet = LinearExpressionSymbols1("fleet", Shape1(fleet.limits.size)) { index, _ ->
            LinearExpressionSymbol(Flt64, name = "fleet_$index")
        }
        val expected = fleet.fleet[0].asMutable().monomials.toMutableList()
        repeat(2) { iteration ->
            val task = demo4FlightTask(
                fixture = fixture,
                actualId = "FLEET-$iteration",
                no = "FB$iteration"
            )
            val arriving = FlightTaskBunch(
                aircraft = fixture.aircraft,
                tasks = listOf(task),
                iteration = Int64(iteration.toLong()),
                cost = demo4Cost(1.0)
            )
            val unmatched = FlightTaskBunch(
                aircraft = fixture.aircraft,
                tasks = listOf(task, fixture.secondTask),
                iteration = Int64(iteration.toLong()),
                cost = demo4Cost(2.0)
            )
            val result = compilation.addColumns(
                iteration = UInt64(iteration),
                newBunches = listOf(arriving, unmatched),
                model = model
            )
            assertTrue(result is Ok)
            val input = listOf(arriving, arriving, unmatched)
            assertTrue(fleet.addColumns(UInt64(iteration), input) is Ok)
            for (bunch in input.filter { fleet.limits.single().first(it) }) {
                expected.add(Flt64.one * compilation.x[iteration][bunch])
            }
            assertEquals(expected, fleet.fleet[0].asMutable().monomials)
        }
    }
}
