@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo5

import kotlin.test.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.model.BranchAndPriceStatus
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service.RouteValidator
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.Flt64NetworkSchedulingSolverValueAdapter
import fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.SemanticParameter
import fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.adapter.Demo17InstanceAdapter

/** demo5 SCIP Branch-and-Price 集成测试。 / demo5 SCIP Branch-and-Price integration test. */
class Demo5ScipBranchAndPriceE2ETest {
    @Test
    fun demo17TwentyFiveCustomersShouldReturnLegalState(): Unit = runBlocking {
        val schedulingWindow = Application.defaultSchedulingWindow()
            val instance = when (val result = Demo17InstanceAdapter.first25Customers(schedulingWindow)) {
                is Ok -> result.value
                is Failed -> fail(result.error.toString())
                is Fatal -> fail(result.errors.toString())
            }
            val result = when (val solve = Application.solve(
                instance = instance,
                parameter = SemanticParameter(
                    customerCount = 25,
                    timeLimit = 60.toDuration(DurationUnit.SECONDS),
                    nodeLimit = 50,
                    solver = "scip"
                )
            )) {
                is Ok -> solve.value
                is Failed -> fail(solve.error.toString())
                is Fatal -> fail(solve.errors.toString())
            }

        assertTrue(result.status in setOf(
            BranchAndPriceStatus.Optimal,
            BranchAndPriceStatus.Feasible,
            BranchAndPriceStatus.TimeLimit,
            BranchAndPriceStatus.NodeLimit
        ))
        result.solution?.routes?.forEach { route ->
            val distanceCalculator = EuclideanDistanceCalculator<Flt64>(Meter)
            assertTrue(RouteValidator.validate(
                instance = instance,
                route = route,
                distanceCalculator = distanceCalculator,
                travelTimeCalculator = DistanceAsTravelTimeCalculator(distanceCalculator, schedulingWindow),
                arcCostCalculator = DistanceArcCostCalculator(Meter, NoneUnit),
                routeCostPolicy = Demo17CostPolicy(NoneUnit),
                valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
            ) is Ok)
        }
    }
}
