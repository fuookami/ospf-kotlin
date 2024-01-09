package fuookami.ospf.kotlin.framework.gantt_scheduling.cg.service

import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model.*

typealias ConnectionTimeCalculator<E> = (executor: E, prevPrevTask: Task<E>?, prevTask: Task<E>, task: Task<E>) -> Duration
typealias MinimumStartTimeCalculator<E> = (prevCompleteTime: Instant, executor: E, prevTask: Task<E>, connectionTime: Duration) -> Instant
typealias CostCalculator<E, L> = (executor: E, prevTask: Task<E>?, prevLabel: L?, task: Task<E>) -> Cost?
typealias AssignedTaskGenerator<E, L> = (prevLabel: L?, node: Node, minStartTime: Instant, executor: E) -> Task<E>?

private typealias LabelMap<L> = MutableMap<Node, MutableList<L>>

