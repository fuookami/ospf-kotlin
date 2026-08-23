package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * An emergency liferaft with its weight and balance index.
 * 具有重量和平衡指数的紧急救生筏。
 *
 * @property weight 救生筏重量 / The weight of the liferaft.
 * @property index 救生筏平衡指数 / The balance index of the liferaft.
*/
data class Liferaft(
    val weight: Quantity<Flt64>,
    val index: Quantity<Flt64>
)

/**
 * Aircraft fuselage properties including dry operating weight, index, and balanced arm.
 * 飞机机身属性（包括干操作重量、指数和平衡臂）。
 *
 * @property liferaft 可选的紧急救生筏 / The optional emergency liferaft.
 * @property dow 干操作重量 / The dry operating weight.
 * @property doi 干操作指数 / The dry operating index.
 * @property balancedArm 平衡臂位置 / The balanced arm position.
*/
data class Fuselage(
    val liferaft: Liferaft?,
    val dow: Quantity<Flt64>,
    val doi: Quantity<Flt64>,
    val balancedArm: Quantity<Flt64>,
)
