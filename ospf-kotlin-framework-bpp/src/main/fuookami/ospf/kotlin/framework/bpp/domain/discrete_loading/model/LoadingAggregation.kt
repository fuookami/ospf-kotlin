package fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.variable.*

/** 稀疏求解器注册构件；领域到 Flt64 的转换只发生在注册边界。 / Sparse solver registration artifacts.
 * @property assignments 仅包含合法组合的变量 / Variables for legal pairs only
 * @property occupied 整体占位变量 / Whole-position occupancy variables
 * @property layouts 布局选择变量 / Layout-choice variables
 * @property counts 舱位件数 / Position counts
 * @property weights 统一单位下的实际重量 / Actual weights in the normalized unit
 * @property loaded 每件货物的装载表达式 / Per-item loading expressions
 */
class LoadingAggregation {
    val assignments = linkedMapOf<AssignmentKey, BinVar>()
    val occupied = linkedMapOf<String, BinVar>()
    val layouts = linkedMapOf<Pair<String, String>, BinVar>()
    val counts = linkedMapOf<String, LinearPolynomial<Flt64>>()
    val weights = linkedMapOf<String, LinearPolynomial<Flt64>>()
    val loaded = linkedMapOf<String, LinearPolynomial<Flt64>>()
}
