package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/**
 * 将 fallback 约束批次写入尚未编号的机制模型。 / Append fallback rows before solver indexing.
 *
 * @property model 支持约束回滚的线性机制模型 / Linear mechanism model supporting row rollback
 */
class MechanismFunctionFallbackTarget(
    private val model: LinearMechanismModel<Flt64>
) : DeferredFunctionFallbackTarget {
    /** 当前约束行数。 / Current number of constraint rows. */
    override val constraintCount: Int
        get() = model.constraints.size

    /** 追加 fallback 约束。 / Append fallback constraints.
     *
     * @param constraints 待追加的线性约束 / Linear constraints to append
     * @return 追加结果 / Append result
     */
    override fun append(constraints: List<LinearInequality<Flt64>>): Try {
        for (constraint in constraints) {
            when (val result = model.addConstraint(relation = constraint, name = constraint.name)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }

    /** 回滚到指定约束行。 / Roll back to the specified constraint row.
     *
     * @param constraintCount 目标约束行数 / Target number of constraint rows
     * @return 回滚结果 / Rollback result
     */
    override fun rollback(constraintCount: Int): Try {
        return model.rollbackConstraintsTo(constraintCount)
    }

    /**
     * 事务成功后绑定结构节点与约束区域。 / Bind structures to row regions after the transaction succeeds.
     *
     * @param structures 待展开的结构快照 / Structure snapshots to materialize
     * @param materializer 通用约束生成器 / Generic constraint generator
     * @return 完整的区域绑定或展开错误 / Complete region bindings or a materialization error
     */
    fun materialize(
        structures: List<DeferredFunctionStructure>,
        materializer: DeferredFunctionFallbackMaterializer
    ): Ret<List<DeferredFunctionConstraintRegion>> {
        val boundStructures = model.deferredFunctionConstraintRegions.map { it.structure }.toMutableList()
        for (structure in structures) {
            // 引用同一性去重：无结果变量的结构（或同实例重复提交）也必须拒绝重复物化。
            // Reference-identity dedup: structures without a result variable (or a resubmitted
            // instance) must also be rejected from double materialization.
            if (boundStructures.any { it === structure }) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "函数 fallback 已绑定或在批次中重复。 / Function fallback is already bound or duplicated in the batch."
                )
            }
            val resultKey = structure.resultVariableOrNull()?.key
            if (resultKey != null && boundStructures.any {
                    it.resultVariableOrNull()?.key == resultKey
                }
            ) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "函数 fallback 已绑定或在批次中重复。 / Function fallback is already bound or duplicated in the batch."
                )
            }
            boundStructures += structure
        }
        return when (val result = materializeDeferredFunctionFallbacks(
            structures = structures,
            target = this,
            materializer = materializer
        )) {
            is Ok -> {
                for (region in result.value) {
                    model.recordDeferredFunctionConstraintRegion(
                        structure = region.structure,
                        firstConstraintIndex = region.firstConstraintIndex,
                        constraintCount = region.constraintCount
                    )
                }
                result
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }
}

/**
 * 在独立约束容器中展开延迟节点，保留源模型。 / Expand deferred nodes in an isolated row container, preserving the source.
 *
 * @param model 尚未编号的机制模型 / Mechanism model before solver indexing
 * @param nativeFunctionKeys 由调用方负责原生建模的结果变量键 / Result keys whose native relations the caller must build
 * @return 已物化副本；无延迟节点时返回源模型 / Materialized copy, or the source when no nodes are deferred
 */
internal fun materializeMechanismFunctionFallbacks(
    model: LinearMechanismModel<Flt64>,
    nativeFunctionKeys: Set<VariableItemKey> = emptySet()
): Ret<LinearMechanismModel<Flt64>> {
    if (model.functionExpansionPolicy == FunctionExpansionPolicy.EAGER) {
        return Ok(model)
    }
    val boundKeys = model.deferredFunctionConstraintRegions.mapNotNull {
        it.structure.resultVariableOrNull()?.key
    }.toSet()
    val pending = model.deferredFunctionStructures.filter {
        it.supportsDeferredFallback() &&
            it.resultVariableOrNull()?.key !in boundKeys &&
            it.resultVariableOrNull()?.key !in nativeFunctionKeys
    }
    if (pending.isEmpty()) {
        return Ok(model)
    }
    for (structure in pending) {
        val validation = when (structure) {
            is UnivariateLinearPiecewiseStructure<*> -> structure.validateFallbackInputBounds()
            is MaxStructure<*> -> structure.validateFallbackInputBounds()
            else -> ok
        }
        when (validation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
    }
    val copy = LinearMechanismModel(
        parent = model.parent,
        name = model.name,
        constraints = model.linearConstraints,
        objectFunction = model.objectFunction,
        tokens = model.tokens,
        ownedTokenResources = AutoCloseable {},
        deferredFunctionStructures = model.deferredFunctionStructures
    )
    for (region in model.deferredFunctionConstraintRegions) {
        copy.recordDeferredFunctionConstraintRegion(
            structure = region.structure,
            firstConstraintIndex = region.firstConstraintIndex,
            constraintCount = region.constraintCount
        )
    }
    return when (val materialized = MechanismFunctionFallbackTarget(copy).materialize(
        structures = pending,
        materializer = CoreDeferredFunctionFallbackMaterializer
    )) {
        is Ok -> Ok(copy)
        is Failed -> Failed(materialized.error)
        is Fatal -> Fatal(materialized.errors)
    }
}
