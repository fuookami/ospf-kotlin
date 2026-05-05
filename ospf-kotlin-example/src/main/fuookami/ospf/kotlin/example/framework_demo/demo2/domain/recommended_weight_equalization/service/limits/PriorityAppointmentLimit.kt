package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service.limits


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class PriorityAppointmentLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val appointment: Map<Item, Position>,
    private val priorityAppointment: PriorityAppointment,
    private val stowage: Stowage,
    override val name: String = "priority_appointment_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((i, item) in items.withIndex()) {
            if (!item.location.main || item.cargo.contains(CargoCode.Virtual) || appointment.containsKey(item)) {
                continue
            }

            for ((j, position) in positions.withIndex()) {
                if (!Stowage.stowageNeeded(item, position)) {
                    continue
                }

                if (!priorityAppointment(item.cargo.priority, position)) {
                    stowage.stowage[i, j].range.eq(Flt64.zero)
                    when (val result = model.addConstraint(
                        relation = LinearPolynomial(stowage.stowage[i, j]) eq Flt64.zero,
                        name = "${name}_${item}_${position}"
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }

                        is Fatal -> {
                            return Fatal(result.errors)
                        }
                    }
                }
            }
        }

        return ok
    }
}



















