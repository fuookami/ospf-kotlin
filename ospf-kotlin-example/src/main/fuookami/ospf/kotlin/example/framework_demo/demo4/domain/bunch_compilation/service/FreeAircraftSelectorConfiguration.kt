@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service

import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * 自由飞机选择器的配置参数。Configuration parameters for the free aircraft selector.
 *
 * @property badReducedAmount 缩减成本策略释放数。
 * @property highCostAmount 高成本策略释放数。
 * @property highAircraftChangeAmount 高变更策略释放数。
 * @property randAmount 随机策略释放数。
 * @property tabuAmount 禁忌列表容量。
 * @property fixBar 缩减成本阈值。
 * @property randomSeed 随机种子。
 */
data class FreeAircraftSelectorConfiguration(
    val badReducedAmount: UInt64 = UInt64(3UL),
    val highCostAmount: UInt64 = UInt64(3UL),
    val highAircraftChangeAmount: UInt64 = UInt64(3UL),
    val randAmount: UInt64 = UInt64(3UL),
    val tabuAmount: UInt64 = UInt64(10UL),
    val fixBar: Flt64 = Flt64(0.0),
    val randomSeed: Long = 42L
)
