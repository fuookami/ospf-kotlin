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
 * @property items the list of cargo items / 物品列表
 * @property positions the list of available stowage positions / 舱位列表
 * @property appointment appointment information defining the relationship between items and positions / 预约信息，定义物品与舱位之间的预约关系
 * @property stowage stowage solution representing the stowage relationship between items and positions / 配载方案，表示物品与舱位之间的配载关系
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
                for ((j, position) in positions.withIndex()) {
                    if (position != thisAppointment && Stowage.stowageNeeded(item, position)) {
                        when (val result = model.addConstraint(
                            relation = stowage.stowage[i, j] eq false,
                            name = "${name}_${item}_${position}"
                        )) {
                            is Ok<fuookami.ospf.kotlin.utils.functional.Success, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                            is Failed<fuookami.ospf.kotlin.utils.functional.Success, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return result
                }

                is Fatal<fuookami.ospf.kotlin.utils.functional.Success, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return result
                }
                        }
                    }
                }
            }
        }
        for ((j, position) in positions.withIndex()) {
            val appointments = appointment[position]
            if (appointments.usize == position.mla) {
                for ((i, item) in items.withIndex()) {
                    if (item !in appointments && Stowage.stowageNeeded(item, position)) {
                        when (val result = model.addConstraint(
                            relation = stowage.stowage[i, j] eq false,
                            name = "${name}_${item}_${position}"
                        )) {
                            is Ok<fuookami.ospf.kotlin.utils.functional.Success, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                            is Failed<fuookami.ospf.kotlin.utils.functional.Success, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return result
                }

                is Fatal<fuookami.ospf.kotlin.utils.functional.Success, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return result
                }
                        }
                    }
                }
            } else {
                return Failed(
                    ErrorCode.ApplicationFailed,
                    "指定舱位 $position 的装载数量 ${appointments.usize} 超过该舱位可装载 ${position.mla} 的上限"
                )
            }
        }

        return ok
    }
}
