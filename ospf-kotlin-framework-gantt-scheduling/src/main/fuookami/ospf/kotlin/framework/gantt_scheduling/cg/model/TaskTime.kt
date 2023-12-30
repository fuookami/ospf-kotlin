package fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model

import kotlin.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*

open class TaskTime<E : Executor>(
    val redundancyRange: Duration? = null,
) {
    val withRedundancy get() = redundancyRange != null

    lateinit var est: LinearSymbols1
    lateinit var ect: LinearSymbols1

    lateinit var redundancy: Combination1<*, *>
    lateinit var delay: Combination1<*, *>
    lateinit var advance: Combination1<*, *>
    lateinit var oet: Combination1<*, *>

    open fun register(timeWindow: TimeWindow, tasks: List<Task<E>>, model: LinearMetaModel): Try {
        if (redundancyRange != null) {
            if (!this::redundancy.isInitialized) {
                redundancy = if (timeWindow.continues) {
                    val rangeValue = timeWindow.dump(redundancyRange).abs()

                    val redundancy = RealVariable1("time_redundancy", Shape1(tasks.size))
                    for (task in tasks) {
                        redundancy[task]!!.name = "${redundancy.name}_${task}"
                        redundancy[task]!!.range.geq(-rangeValue)
                        redundancy[task]!!.range.leq(rangeValue)

                        if (task.plan.time != null && !task.advanceEnabled) {
                            redundancy[task]!!.range.geq(Flt64.zero)
                        }
                        if (task.plan.time != null && !task.delayEnabled) {
                            redundancy[task]!!.range.leq(Flt64.zero)
                        }
                    }
                    redundancy
                } else {
                    val rangeValue = timeWindow.dump(redundancyRange).abs().floor().toInt64()

                    val redundancy = IntVariable1("time_redundancy", Shape1(tasks.size))
                    for (task in tasks) {
                        redundancy[task]!!.name = "${redundancy.name}_${task}"
                        redundancy[task]!!.range.geq(-rangeValue)
                        redundancy[task]!!.range.leq(rangeValue)

                        if (task.plan.time != null && !task.advanceEnabled) {
                            redundancy[task]!!.range.geq(Int64.zero)
                        }
                        if (task.plan.time != null && !task.delayEnabled) {
                            redundancy[task]!!.range.leq(Int64.zero)
                        }
                    }
                    redundancy
                }
            }
            model.addVars(redundancy)

            if (!this::delay.isInitialized) {
                delay = UIntVariable1("delay", Shape1(tasks.size))
                for (task in tasks) {
                    delay[task]!!.name = "${delay.name}_${task}"
                }
            }
            model.addVars(delay)

            if (!this::advance.isInitialized) {
                advance = UIntVariable1("advance", Shape1(tasks.size))
                for (task in tasks) {
                    advance[task]!!.name = "${advance.name}_${task}"
                }
            }
            model.addVars(advance)

            if (!this::oet.isInitialized) {
                oet = UIntVariable1("oet", Shape1(tasks.size))
                for (task in tasks) {
                    oet[task]!!.name = "${oet.name}_${task}"
                }
            }
            model.addVars(oet)
        }

        if (!this::est.isInitialized) {
            est = LinearSymbols1("est", Shape1(tasks.size))
            for (task in tasks) {
                est[task] = if (withRedundancy) {
                    LinearSymbol(LinearPolynomial(redundancy[task]!!), "${est.name}_${task}")
                } else {
                    LinearSymbol(LinearPolynomial(), "${est.name}_${task}")
                }
            }
        }
        model.addSymbols(est)

        if (!this::ect.isInitialized) {
            ect = LinearSymbols1("ect", Shape1(tasks.size))
            for (task in tasks) {
                ect[task] = if (withRedundancy) {
                    LinearSymbol(LinearPolynomial(redundancy[task]!!), "${ect.name}_${task}")
                } else {
                    LinearSymbol(LinearPolynomial(), "${ect.name}_${task}")
                }
            }
        }
        model.addSymbols(ect)

        return Ok(success)
    }

    @OptIn(DelicateCoroutinesApi::class, ObsoleteCoroutinesApi::class)
    open fun addColumns(
        iteration: UInt64,
        timeWindow: TimeWindow,
        bunches: List<TaskBunch<E>>,
        tasks: List<Task<E>>,
        compilation: Compilation<E>,
        compilationAddingPromise: BroadcastChannel<Boolean>? = null,
        scope: CoroutineScope? = compilationAddingPromise?.let { GlobalScope }
    ): Ret<BroadcastChannel<Boolean>?> {
        assert(bunches.isNotEmpty())

        val xi = compilation.x[iteration.toInt()]
        for (task in tasks) {
            for (bunch in bunches) {
                val actualTask = bunch.get(task) ?: continue
                val time = actualTask.time!!

                val est = this.est[task] as LinearSymbol
                est.flush()
                (est.polynomial as LinearPolynomial) += timeWindow.dump(time.start) * xi[bunch]!!

                val ect = this.ect[task] as LinearSymbol
                ect.flush()
                (ect.polynomial as LinearPolynomial) += timeWindow.dump(time.end) * xi[bunch]!!
            }
        }

        return if (scope != null) {
            val promise = BroadcastChannel<Boolean>(Channel.BUFFERED)
            scope.launch {
                compilationAddingPromise?.openSubscription()?.receive()
                flush(tasks)
                promise.send(true)
            }
            Ok(promise)
        } else {
            flush(tasks)
            Ok(null)
        }
    }

    private fun flush(tasks: List<Task<E>>) {
        for (task in tasks) {
            (est[task] as LinearSymbol).cells
            (ect[task] as LinearSymbol).cells
        }
    }
}
