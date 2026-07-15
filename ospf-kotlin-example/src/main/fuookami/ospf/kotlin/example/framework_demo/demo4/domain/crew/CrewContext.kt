@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew

/** 航班恢复调度演示中机组域操作的上下文。Context for crew domain operations in the flight recovery scheduling demo. */
class CrewContext {
    lateinit var aggregation: Aggregation
}
