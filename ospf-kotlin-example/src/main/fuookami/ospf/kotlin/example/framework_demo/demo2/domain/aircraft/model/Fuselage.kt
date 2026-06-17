package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 具有重量和平衡指数的紧急救生筏。An emergency liferaft with its weight and balance index.
 *
 * @property weight 参数。
 * @property index 参数。
 */
data class Liferaft(
    val weight: Quantity<Flt64>,
    val index: Quantity<Flt64>
)

/**
 * 飞机机身属性（包括干操作重量、指数和平衡臂）。Aircraft fuselage properties including dry operating weight, index, and balanced arm.
 *
 * @property liferaft 参数。
 * @property dow 参数。
 * @property doi 参数。
 * @property balancedArm 参数。
 */
data class Fuselage(
    val liferaft: Liferaft?,
    val dow: Quantity<Flt64>,
    val doi: Quantity<Flt64>,
    val balancedArm: Quantity<Flt64>,
)
