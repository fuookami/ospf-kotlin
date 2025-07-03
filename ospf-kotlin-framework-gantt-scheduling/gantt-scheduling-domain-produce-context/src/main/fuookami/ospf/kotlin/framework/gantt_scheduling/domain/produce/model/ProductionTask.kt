package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

interface Material : AbstractMaterial {
    override val material get() = this
}

interface AbstractMaterial : Indexed {
    val material: Material
}

interface Product : Material
interface SemiProduct : Material
interface RawMaterial : Material

data class MaterialDemand(
    val quantity: ValueRange<Flt64>,
    val lessQuantity: Flt64? = null,
    val overQuantity: Flt64? = null
) {
    val lessEnabled: Boolean get() = lessQuantity != null
    val overEnabled: Boolean get() = overQuantity != null
}

open class MaterialReserves(
    val quantity: ValueRange<Flt64>,
    val lessQuantity: Flt64? = null,
    val overQuantity: Flt64? = null,
) {
    val lessEnabled: Boolean get() = lessQuantity != null
    val overEnabled: Boolean get() = overQuantity != null
}

interface ProductionTask<
    out E : Executor,
    out A : AssignmentPolicy<E>,
    P: AbstractMaterial,
    C: AbstractMaterial
> : AbstractTask<E, A> {
    val produce: Map<P, Flt64>
    val consumption: Map<C, Flt64>
}

fun <
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>,
    P: AbstractMaterial
> AbstractTaskBunch<T, E, A>.produce(product: P): Flt64 {
    return tasks.mapNotNull {
        when (it) {
            is ProductionTask<*, *, *, *> -> {
                it.produce[product]
            }

            else -> {
                null
            }
        }
    }.sumOf { it }
}

fun <
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>,
    C: AbstractMaterial
> AbstractTaskBunch<T, E, A>.consumption(material: C): Flt64 {
    return tasks.mapNotNull {
        when (it) {
            is ProductionTask<*, *, *, *> -> {
                it.consumption[material]
            }

            else -> {
                null
            }
        }
    }.sumOf { it }
}
