/**
 * 档案算子 / Archive operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.archive

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.heuristic.Individual
import fuookami.ospf.kotlin.core.solver.heuristic.compareIndividuals

/**
 * 档案接口，负责维护「当前最优集合」，是单目标与多目标共享的收敛载体。 / Archive interface that maintains the current best set, the convergence carrier shared by single- and multi-objective search.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
*/
interface Archive<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {

    /** 当前档案中的个体，按优度排列 / Individuals currently in the archive, ordered by quality */
    val individuals: List<Individual<ObjValue, V>>

    /**
     * 用新个体更新档案 / Update the archive with new individuals
     *
     * @param model 回调模型接口 / Callback model interface
     * @param candidates 新个体 / New individuals
     * @return 档案最优是否被改善 / Whether the archive best improved
    */
    fun update(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        candidates: List<Individual<ObjValue, V>>
    ): Boolean

    /** 清空档案 / Clear the archive */
    fun clear()
}

/**
 * 精英档案，保留固定数量的最优个体。 / Elite archive keeping a fixed number of best individuals.
 *
 * @property amount 保留的个体数量 / Number of individuals to keep
*/
class BestArchive<ObjValue, V>(
    val amount: Int
) : Archive<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    private val entries = ArrayList<Individual<ObjValue, V>>()

    override val individuals: List<Individual<ObjValue, V>>
        get() = entries.toList()

    override fun update(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        candidates: List<Individual<ObjValue, V>>
    ): Boolean {
        val previousBest = entries.firstOrNull()
        entries.addAll(candidates)
        entries.sortWith { lhs, rhs ->
            compareIndividuals(
                model = model,
                lhs = lhs,
                rhs = rhs
            )
        }
        while (entries.size > amount) {
            entries.removeAt(entries.lastIndex)
        }

        val currentBest = entries.firstOrNull() ?: return false
        return if (previousBest == null) {
            true
        } else {
            compareIndividuals(
                model = model,
                lhs = currentBest,
                rhs = previousBest
            ) < 0
        }
    }

    override fun clear() {
        entries.clear()
    }
}

/**
 * 历史最优档案，只保留迄今为止的最优个体。 / Historical best archive keeping only the best individual so far.
*/
class HistoryBestArchive<ObjValue, V> : Archive<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    private var entry: Individual<ObjValue, V>? = null

    override val individuals: List<Individual<ObjValue, V>>
        get() = listOfNotNull(entry)

    override fun update(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        candidates: List<Individual<ObjValue, V>>
    ): Boolean {
        var improved = false
        for (candidate in candidates) {
            val current = entry
            if (current == null || compareIndividuals(
                model = model,
                lhs = candidate,
                rhs = current
            ) < 0) {
                entry = candidate
                improved = true
            }
        }
        return improved
    }

    override fun clear() {
        entry = null
    }
}
