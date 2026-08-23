@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service

import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * 列生成主模型目标系数的参数。Parameter for column generation master model objective coefficients.
 *
 * 默认值移植自 fsra-proof 基础设施参数。
 *
 * @property fleetBalanceSlack 非基地机场车队平衡松弛惩罚 / Non-base airport fleet balance slack penalty
 * @property fleetBalanceBaseSlack 基地机场车队平衡松弛惩罚 / Base airport fleet balance slack penalty
 * @property executorLeisureCoeff 执行器空闲最小化系数 / Executor leisure minimization coefficient
 * @property taskCancelCoeff 任务取消最小化系数 / Task cancellation minimization coefficient
 * @property passengerCancelCoeff 乘客取消最小化系数 / Passenger cancellation minimization coefficient
 * @property passengerClassChangeCoeff 乘客舱位变更加权系数 / Passenger class change weighted coefficient
 * @property passengerFlightChangeCoeff 乘客航班变更加权系数 / Passenger flight change weighted coefficient
*/
data class Parameter(
    val fleetBalanceSlack: Flt64 = Flt64(60.0),
    val fleetBalanceBaseSlack: Flt64 = Flt64(600.0),
    val executorLeisureCoeff: Flt64 = Flt64(0.0),
    val taskCancelCoeff: Flt64 = Flt64(9999.0),
    val passengerCancelCoeff: Flt64 = Flt64(3.0),
    val passengerClassChangeCoeff: Flt64 = Flt64(0.0),
    val passengerFlightChangeCoeff: Flt64 = Flt64(1.0)
)
