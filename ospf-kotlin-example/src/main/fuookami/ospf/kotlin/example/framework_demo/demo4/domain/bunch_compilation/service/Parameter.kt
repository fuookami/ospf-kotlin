@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service

import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * 列生成主模型目标系数的参数。Parameter for column generation master model objective coefficients.
 *
 * 默认值移植自 fsra-proof 基础设施参数。
 *
 * @property fleetBalanceSlack Non-base airport fleet balance slack penalty / 非基地机场车队平衡松弛惩罚
 * @property fleetBalanceBaseSlack Base airport fleet balance slack penalty / 基地机场车队平衡松弛惩罚
 * @property executorLeisureCoeff Executor leisure minimization coefficient / 执行器空闲最小化系数
 * @property taskCancelCoeff Task cancellation minimization coefficient / 任务取消最小化系数
 * @property passengerCancelCoeff Passenger cancellation minimization coefficient / 乘客取消最小化系数
 * @property passengerClassChangeCoeff Passenger class change weighted coefficient / 乘客舱位变更加权系数
 * @property passengerFlightChangeCoeff Passenger flight change weighted coefficient / 乘客航班变更加权系数
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
