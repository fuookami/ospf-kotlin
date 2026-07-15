package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/** 航班任务 id。/ Flight task id. */
@JvmInline
value class FlightTaskId(
    val value: String
) : TaskId {
    override fun toString(): String = value
}

/** 航班任务计划 id。/ Flight task plan id. */
@JvmInline
value class FlightTaskPlanId(
    val value: String
) : TaskPlanId {
    override fun toString(): String = value
}

/** 航空器执行器 id。/ Aircraft executor id. */
@JvmInline
value class AircraftId(
    val value: String
) : ExecutorId {
    override fun toString(): String = value
}
