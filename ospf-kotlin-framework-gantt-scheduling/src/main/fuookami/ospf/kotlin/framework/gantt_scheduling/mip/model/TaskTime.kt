package fuookami.ospf.kotlin.framework.gantt_scheduling.mip.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*

open class TaskTime<E : Executor>(
    val delayEnabled: Boolean = false,
    val advanceEnabled: Boolean = false,
    val overExpirationTimeEnabled: Boolean = false
) {
    lateinit var est: Combination1<*, *>
    lateinit var ect: LinearSymbols1

    lateinit var delay: Combination1<*, *>
    lateinit var advance: Combination1<*, *>
    lateinit var oet: Combination1<*, *>

    open fun register(timeWindow: TimeWindow, tasks: List<Task<E>>, ectCalculator: (task: Task<E>, est: Item<*, *>) -> LinearSymbol, model: LinearMetaModel): Try {
        if (!this::est.isInitialized) {
            est = if (timeWindow.continues) {
                val est = URealVariable1("est", Shape1(tasks.size))
                for (task in tasks) {
                    est[task]!!.name = "${est.name}_${task}"

                    if (task.plan.time != null) {
                        if (!task.advanceEnabled) {
                            est[task]!!.range.geq(timeWindow.dump(task.plan.time!!.start))
                        }
                        if (!task.delayEnabled) {
                            est[task]!!.range.leq(timeWindow.dump(task.plan.time!!.start))
                        }
                    }
                }
                est
            } else {
                val est = UIntVariable1("est", Shape1(tasks.size))
                for (task in tasks) {
                    est[task]!!.name = "${est.name}_${task}"

                    if (task.plan.time != null) {
                        if (!task.advanceEnabled) {
                            est[task]!!.range.geq(timeWindow.dump(task.plan.time!!.start).floor().toUInt64())
                        }
                        if (!task.delayEnabled) {
                            est[task]!!.range.leq(timeWindow.dump(task.plan.time!!.start).floor().toUInt64())
                        }
                    }
                }
                est
            }
        }
        model.addVars(est)

        if (!this::ect.isInitialized) {
            ect = LinearSymbols1("ect", Shape1(tasks.size))
            for (task in tasks) {
                val sym = ectCalculator(task, est[task]!!)
                sym.name = "${ect.name}_${task}"
                ect[task] = sym
            }
        }
        model.addSymbols(ect)

        if (delayEnabled) {
            if (!this::delay.isInitialized) {
                delay = if (timeWindow.continues) {
                    val delay = URealVariable1("delay", Shape1(tasks.size))
                    for (task in tasks) {
                        if (task.plan.time != null && task.delayEnabled) {
                            delay[task]!!.name = "${delay.name}_${task}"
                        } else {
                            delay[task]!!.range.eq(Flt64.zero)
                        }
                    }
                    delay
                } else {
                    val delay = UIntVariable1("delay", Shape1(tasks.size))
                    for (task in tasks) {
                        if (task.plan.time != null && task.delayEnabled) {
                            delay[task]!!.name = "${delay.name}_${task}"
                        } else {
                            delay[task]!!.range.eq(UInt64.zero)
                        }
                    }
                    delay
                }
            }
            model.addVars(delay)
        }

        if (advanceEnabled) {
            if (!this::advance.isInitialized) {
                advance = if (timeWindow.continues) {
                    val advance = URealVariable1("advance", Shape1(tasks.size))
                    for (task in tasks) {
                        if (task.plan.time != null && task.advanceEnabled) {
                            advance[task]!!.name = "${advance.name}_${task}"
                        } else {
                            advance[task]!!.range.eq(Flt64.zero)
                        }
                    }
                    advance
                } else {
                    val advance = UIntVariable1("advance", Shape1(tasks.size))
                    for (task in tasks) {
                        if (task.plan.time != null && task.advanceEnabled) {
                            advance[task]!!.name = "${advance.name}_${task}"
                        } else {
                            advance[task]!!.range.eq(UInt64.zero)
                        }
                    }
                    advance
                }
            }
            model.addVars(advance)
        }

        if (overExpirationTimeEnabled) {
            if (!this::oet.isInitialized) {
                oet = if (timeWindow.continues) {
                    val oet = URealVariable1("oet", Shape1(tasks.size))
                    for (task in tasks) {
                        if (task.plan.expirationTime != null) {
                            oet[task]!!.name = "${advance.name}_${task}"
                        } else {
                            oet[task]!!.range.eq(Flt64.zero)
                        }
                    }
                    oet
                } else {
                    val oet = UIntVariable1("oet", Shape1(tasks.size))
                    for (task in tasks) {
                        if (task.plan.expirationTime != null) {
                            oet[task]!!.name = "${advance.name}_${task}"
                        } else {
                            oet[task]!!.range.eq(UInt64.zero)
                        }
                    }
                    oet
                }
            }
            model.addVars(oet)
        }

        return Ok(success)
    }
}
