package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Appointment limit pipeline that constrains the storage positions of items in a stowage solution based on appointment information.
 * 预约限制管道，根据预约信息约束配载方案中物品的存放位置。
 *
 * @property items 物品列表 / the list of cargo items
 * @property positions 舱位列表 / the list of available stowage positions
 * @property appointment 预约信息，定义物品与舱位之间的预约关系 / appointment information defining the relationship between items and positions
 * @property stowage 配载方案，表示物品与舱位之间的配载关系 / stowage solution representing the stowage relationship between items and positions
*/
class AppointmentLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val appointment: Appointment,
    private val stowage: Stowage,
    override val name: String = "appointment_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((i, item) in items.withIndex()) {
            val thisAppointment = appointment[item]
            if (thisAppointment != null) {
                val positionIndex = positions.indexOf(thisAppointment)
                if (positionIndex < 0) {
                    return Failed(
                        ErrorCode.IllegalArgument,
                        "预约舱位不属于当前配载范围：$thisAppointment / Appointed position is not part of the current stowage scope: $thisAppointment"
                    )
                }
                when (val result = model.addConstraint(
                    relation = stowage.stowage[i, positionIndex] eq true,
                    name = "${name}_${item}_${thisAppointment}"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return result
                    }

                    is Fatal -> {
                        return result
                    }
                }
            }
        }
        for ((j, position) in positions.withIndex()) {
            val appointments = appointment[position]
            if (appointments.usize > position.mla) {
                return Failed(
                    ErrorCode.ApplicationFailed,
                    "指定舱位 $position 的装载数量 ${appointments.usize} 超过该舱位可装载 ${position.mla} 的上限 / Assigned item count ${appointments.usize} exceeds position $position capacity ${position.mla}"
                )
            }
        }

        return ok
    }
}
