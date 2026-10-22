package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.DiscreteLoadingContext
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.AssignmentKey

/** 航空业务表达式适配，分配和整体占位由框架拥有。 / Aviation expression bridge; the framework owns assignment and occupancy.
 * @property items 货物 / Cargo
 * @property positions 舱位 / Positions
 */
class Stowage(private val items: List<Item>, private val positions: List<Position>) {
    companion object {
        /** 业务合法性。 / Business compatibility.
         * @param item 货物 / Cargo
         * @param position 舱位 / Position
         * @return 是否允许 / Whether allowed
         */
        fun stowageNeeded(item: Item, position: Position): Boolean = item.status.stowageNeeded && position.status.stowageNeeded && position.enabled(item).ok

        /** 调整合法性。 / Adjustment compatibility.
         * @param item 货物 / Cargo
         * @param position 舱位 / Position
         * @return 是否允许 / Whether allowed
         */
        fun adjustmentNeeded(item: Item, position: Position): Boolean = item.status.adjustmentNeeded &&
            ((position.status.stowageNeeded && position.enabled(item).ok) || (position.status.adjustmentNeeded && (position.enabled(item).ok || item in position.loadedItems)))
    }

    lateinit var loadingContext: DiscreteLoadingContext<Flt64>
    lateinit var stowage: LinearIntermediateSymbols2<Flt64>
    lateinit var loaded: LinearIntermediateSymbols1<Flt64>

    /** 稀疏决策查询，非法组合不存在变量。 / Sparse decision lookup; illegal combinations have no variable.
     * @param i 货物索引 / Cargo index
     * @param j 舱位索引 / Position index
     * @return 可选分配变量 / Optional assignment variable
     */
    fun assignmentVariable(i: Int, j: Int): BinVar? = loadingContext.aggregation.assignments[AssignmentKey(items[i].id, positions[j].spaceName)]

    /** 注册共享内核并桥接业务符号。 / Register the shared kernel and bridge business symbols.
     * @param model 完整元模型 / Full meta model
     * @return 注册结果 / Registration result
     */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (!::loadingContext.isInitialized) return Failed(ErrorCode.IllegalArgument, "装载上下文未初始化 / Loading context is not initialized")
        val result = loadingContext.register(model)
        if (result !is Ok) return result
        stowage = LinearIntermediateSymbols2<Flt64>("stowage", Shape2(items.size, positions.size)) { _, v ->
            LinearExpressionSymbol(
                assignmentVariable(v[0], v[1])?.let { LinearPolynomial(it) } ?: LinearPolynomial(),
                name = "stowage_${v.joinToString("_")}"
            )
        }
        val symbols = model.add(stowage)
        if (symbols !is Ok) return symbols
        loaded = LinearIntermediateSymbols1<Flt64>("loaded", Shape1(items.size)) { i, _ ->
            LinearExpressionSymbol(loadingContext.aggregation.loaded.getValue(items[i].id), name = "loaded_${items[i].id}")
        }
        return model.add(loaded)
    }
}
