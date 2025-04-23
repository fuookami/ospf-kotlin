package fuookami.ospf.kotlin.core.backend.solver.heuristic

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*

interface ObjectiveNormalization<V> {
    operator fun invoke(
        model: AbstractCallBackModelInterface<*, V>,
        objs: List<V>
    ): List<Flt64>
}

data object MinMaxNormalization : ObjectiveNormalization<Flt64> {
    override fun invoke(model: AbstractCallBackModelInterface<*, Flt64>, objs: List<Flt64>): List<Flt64> {
        val minObj = objs.min()
        val maxObj = objs.max()
        return if (model.compareObjective(minObj, maxObj) is Order.Less) {
            objs.map { Flt64.one - (it - minObj) / (maxObj - minObj) }
        } else {
            objs.map { (it - minObj) / (maxObj - minObj) }
        }
    }
}

data object SumNormalization : ObjectiveNormalization<Flt64> {
    override fun invoke(model: AbstractCallBackModelInterface<*, Flt64>, objs: List<Flt64>): List<Flt64> {
        val minObj = objs.min()
        val maxObj = objs.max()
        val minMaxObjs = objs.map { (it - minObj) / (maxObj - minObj) }
        val sum = minMaxObjs.sum()
        return if (model.compareObjective(minObj, maxObj) is Order.Less) {
            if (sum neq Flt64.zero) {
                minMaxObjs.map { Flt64.one - it / sum }
            } else {
                objs.indices.map { Flt64.one / Flt64(objs.size) }
            }
        } else {
            if (sum neq Flt64.zero) {
                minMaxObjs.map { it / sum }
            } else {
                objs.indices.map { Flt64.one / Flt64(objs.size) }
            }
        }
    }
}
