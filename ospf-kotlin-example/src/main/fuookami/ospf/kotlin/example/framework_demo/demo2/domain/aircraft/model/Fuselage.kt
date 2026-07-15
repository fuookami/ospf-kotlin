package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * An emergency liferaft with its weight and balance index.
 * 具有重量和平衡指数的紧急救生筏。
 *
 * @property weight The weight of the liferaft. / 救生筏重量
 * @property index The balance index of the liferaft. / 救生筏平衡指数
*/
data class Liferaft(
    val weight: Quantity<Flt64>,
    val index: Quantity<Flt64>
)

/**
 * Aircraft fuselage properties including dry operating weight, index, and balanced arm.
 * 飞机机身属性（包括干操作重量、指数和平衡臂）。
 *
 * @property liferaft The optional emergency liferaft. / 可选的紧急救生筏
 * @property dow The dry operating weight. / 干操作重量
 * @property doi The dry operating index. / 干操作指数
 * @property balancedArm The balanced arm position. / 平衡臂位置
*/
data class Fuselage(
    val liferaft: Liferaft?,
    val dow: Quantity<Flt64>,
    val doi: Quantity<Flt64>,
    val balancedArm: Quantity<Flt64>,
)
