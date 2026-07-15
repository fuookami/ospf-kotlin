@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task

/** 航班恢复调度演示中航班任务域操作的上下文。Context for flight task domain operations in the flight recovery scheduling demo. */
class FlightTaskContext {
    lateinit var aggregation: Aggregation
}
