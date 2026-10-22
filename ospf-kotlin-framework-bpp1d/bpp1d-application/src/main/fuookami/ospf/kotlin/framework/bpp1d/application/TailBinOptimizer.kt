package fuookami.ospf.kotlin.framework.bpp1d.application

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.framework.bpp1d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp1d.domain.packing.*

/** 尾箱重排入口；外部约束通过完整结果接收策略复核，不把全局约束错误应用于局部问题。
 * / Tail-bin rearrangement with a full-result acceptance policy, avoiding misapplication of global constraints.
 * @property solver 普通装箱入口 / Ordinary packing entry
 */
class TailBinOptimizer<V : FloatingNumber<V>>(private val solver: Bpp1dMilp<V>) {
    /** 保留前部箱体，重求尾部并复核完整需求和库存。 / Preserve prefix bins, repack the tail, verify complete demand and stock.
     * @param problem 原始问题 / Original problem
     * @param current 已验证结果 / Verified result
     * @param window 尾箱数量 / Tail-bin count
     * @param accept 完整结果业务接收策略 / Full-result business acceptance policy
     * @param config 局部模型配置 / Local model configuration
     * @return 重排后的完整结果或失败 / Repacked full result or failure
     */
    suspend fun optimize(
        problem: PackingProblem<V>,
        current: PackingResult<V>,
        window: Int,
        accept: (PackingResult<V>) -> Try = { ok },
        config: Bpp1dSolveConfig<V> = Bpp1dSolveConfig()
    ): Ret<PackingResult<V>> {
        if (window < 1 || config.extensions.isNotEmpty()) return Failed(ErrorCode.IllegalArgument, "尾箱窗口必须为正，扩展须使用完整结果策略 / Positive tail window required; use full-result policies for extensions")
        fun patterns(plans: List<PackingPlan<V>>): Map<PackingPattern<V>, UInt64> {
            return plans.map { plan -> PackingPattern<V>(plan.bin.type.id, plan.placements.groupingBy { it.itemId }.eachCount().mapValues { UInt64(it.value.toULong()) }) }
                .groupingBy { it }.eachCount().mapValues { UInt64(it.value.toULong()) }
        }
        val validation = PackingContext<V>().pack(problem, patterns(current.plans))
        if (validation is Failed) return Failed(validation.error)
        if (validation is Fatal) return Fatal(validation.errors)
        if (current.plans.isEmpty()) return Ok(current)
        val prefix = current.plans.dropLast(minOf(window, current.plans.size))
        val remaining = current.plans.takeLast(window).flatMap { it.placements }.groupingBy { it.itemId }.eachCount()
        val usedStock = prefix.groupingBy { it.bin.type.id }.eachCount()
        val local = problem.copy(
            items = problem.items.map { it.copy(amount = UInt64((remaining[it.id] ?: 0).toULong())) },
            binTypes = problem.binTypes.map { type -> type.copy(inventory = type.inventory?.let { it - UInt64((usedStock[type.id] ?: 0).toULong()) }) }
        )
        val solved = when (val result = solver.solve(local, config)) {
            is Ok -> result.value.packing
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val candidate = when (val result = PackingContext<V>().pack(problem, patterns(prefix + solved.plans))) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        fun score(result: PackingResult<V>): V {
            val zero = problem.binTypes.first().cost.constants.zero
            return result.plans.fold(zero) { acc, plan -> acc + when (problem.objective) {
                PackingObjective.BinCount -> zero.constants.one
                PackingObjective.Cost -> plan.bin.type.cost
                PackingObjective.ConsumedLength -> plan.bin.type.length.convertTo(problem.binTypes.first().length.unit)!!.value
            } }
        }
        if (score(candidate) > score(current)) return Ok(current)
        return when (val accepted = accept(candidate)) {
            is Ok -> Ok(candidate)
            is Failed -> Failed(accepted.error)
            is Fatal -> Fatal(accepted.errors)
        }
    }
}
