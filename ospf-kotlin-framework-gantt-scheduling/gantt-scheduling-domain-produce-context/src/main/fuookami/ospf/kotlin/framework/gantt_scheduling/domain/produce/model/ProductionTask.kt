package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

interface Product : Indexed
interface Material : Indexed

data class ProductDemand(
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
    out A : AssignmentPolicy<E>
> : AbstractTask<E, A> {
    val produce: Map<Product, Flt64>
    val consumption: Map<Material, Flt64>
}

fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> AbstractTaskBunch<T, E, A>.produce(product: Product): Flt64 {
    return tasks.mapNotNull {
        when (it) {
            is ProductionTask<*, *> -> {
                it.produce[product]
            }

            else -> {
                null
            }
        }
    }.sumOf { it }
}

fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> AbstractTaskBunch<T, E, A>.consumption(material: Material): Flt64 {
    return tasks.mapNotNull {
        when (it) {
            is ProductionTask<*, *> -> {
                it.consumption[material]
            }

            else -> {
                null
            }
        }
    }.sumOf { it }
}
