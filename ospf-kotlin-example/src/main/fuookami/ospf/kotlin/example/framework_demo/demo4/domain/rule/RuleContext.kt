@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule

/** 航班恢复调度演示中规则域操作的上下文。Context for rule domain operations in the flight recovery scheduling demo. */
class RuleContext {
    lateinit var aggregation: Aggregation
}
