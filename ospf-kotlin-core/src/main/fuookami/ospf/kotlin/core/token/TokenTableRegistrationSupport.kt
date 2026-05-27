/**
 * Token 注册与缓存预热支持（含并发路径）。
 * Token registration and cache warm-up support (including concurrent path).
 *
 * 负责符号依赖分层注册、aux token 准备与 flatten/range/value 缓存写入。
 * Handles layered registration by symbol dependencies, auxiliary-token preparation, and flatten/range/value cache writes.
 */
package fuookami.ospf.kotlin.core.token

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.associateWithNotNull
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatus
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.intermediate.MemoryCleanupPolicy
import fuookami.ospf.kotlin.core.model.intermediate.buildBatchSlices
import fuookami.ospf.kotlin.core.model.intermediate.computeBatchDispatchPlan
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.SolverBoundaryCasts
import fuookami.ospf.kotlin.core.symbol.solverFlattenedMonomials

/**
 * Token 注册与缓存预热支持（含并发路径）。
 * Token registration and cache warm-up support (including concurrent path).
 *
 * 说明：该文件负责符号依赖分层注册、aux token 准备与 flatten/range/value 缓存写入。
 * Note: this file handles layered registration by symbol dependencies, auxiliary-token preparation, and flatten/range/value cache writes.
 *
 * 非目标：不承担函数约束建模与求解流程控制，这些职责在 MetaModel/MechanismModel 与 solver 侧完成。
 * Non-goal: function-constraint modeling and solve-flow orchestration are out of scope and handled by MetaModel/MechanismModel and solver side.
 */

// 求解器边界转换：委托给集中的 SolverBoundaryCasts。 / Solver-boundary conversion: delegates to centralized SolverBoundaryCasts.
private fun IntermediateSymbol<*>.registerAuxTokensStar(tokens: AddableTokenCollection<*>): Try {
    return SolverBoundaryCasts.registerAuxiliaryTokensStar(this, tokens)
}

// 求解器边界转换：委托给集中的 SolverBoundaryCasts。 / Solver-boundary conversion: delegates to centralized SolverBoundaryCasts.
private fun IntermediateSymbol<*>.prepareStar(
    fixedValues: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): Flt64? {
    return SolverBoundaryCasts.prepareStar(this, fixedValues, tokenTable)
}

private fun AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>.cacheSymbolContext(symbol: IntermediateSymbol<*>) {
    bindTokenTableContext(symbol, this)
    when (symbol) {
        is LinearIntermediateSymbol<*> -> {
            cacheLinearFlatten(symbol, symbol.solverFlattenedMonomials)
        }

        is QuadraticIntermediateSymbol<*> -> {
            cacheQuadraticFlatten(symbol, symbol.solverFlattenedMonomials)
        }
    }
    cacheRange(symbol, SolverBoundaryCasts.rangeAsFlt64(symbol))
}

private fun AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>.cacheSymbolContexts(symbols: Iterable<IntermediateSymbol<*>>) {
    for (symbol in symbols) {
        cacheSymbolContext(symbol)
    }
}

@Suppress("USELESS_CAST")
fun Collection<IntermediateSymbol<*>>.register(
    tokenTable: MutableTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    fixedValues: Map<Symbol, Flt64>? = null,
    callBack: RegistrationStatusCallBack? = null
): Try {
    val (emptySymbols, notEmptySymbols) = this@register.partition {
        it is LinearIntermediateSymbol<*> && it.solverFlattenedMonomials.run {
            monomials.isEmpty() && constant eq Flt64.zero
        }
    }
    tokenTable.cache(emptySymbols.associateWith { Flt64.zero }.mapKeys { it.key as IntermediateSymbol<*> })
    tokenTable.cacheSymbolContexts(emptySymbols)

    val completedSymbols = emptySymbols.toMutableSet()
    callBack?.invoke(
        RegistrationStatus(
            emptySymbolAmount = emptySymbols.usize,
            readySymbolAmount = completedSymbols.usize,
            totalSymbolAmount = tokenTable.symbols.usize
        )
    )
    var dependencies = notEmptySymbols.associateWith { symbol ->
        symbol.dependencies.filter { dependency ->
            dependency !in completedSymbols
        }.toMutableSet()
    }.toMap()
    for ((symbol, deps) in dependencies) {
        tokenTable.addSymbolWithDependencies(symbol, deps)
    }
    var readySymbols = dependencies.filter { it.value.isEmpty() }.keys
    dependencies = dependencies.filterValues { it.isNotEmpty() }.toMap()
    while (readySymbols.isNotEmpty()) {
        for (symbol in readySymbols) {
            // 为函数符号注册辅助 token；函数约束由 MetaModel/MechanismModel 注册，不属于 token 注册阶段。
            // Register auxiliary tokens for function symbols; function constraints are registered by MetaModel/MechanismModel, not this phase.
            when (val result = symbol.registerAuxTokensStar(tokenTable)) {
                is Ok -> {}
                is Failed -> { return Failed(result.error) }
                is Fatal -> { return Fatal(result.errors) }
            }
        }
        // 批量写入 ready 符号的 value 缓存，保持与并发路径一致。
        // Batch-write value cache for ready symbols, aligned with the concurrent path.
        tokenTable.cache(
            symbols = readySymbols.associateWithNotNull {
                it.prepareStar(fixedValues, tokenTable)
            }.mapKeys { it.key as IntermediateSymbol<*> }
        )
        tokenTable.cacheSymbolContexts(readySymbols)
        MemoryCleanupPolicy.cleanupAfterBatch()
        callBack?.invoke(
            RegistrationStatus(
                emptySymbolAmount = emptySymbols.usize,
                readySymbolAmount = completedSymbols.usize + readySymbols.usize,
                totalSymbolAmount = tokenTable.symbols.usize
            )
        )

        completedSymbols.addAll(readySymbols)
        val newReadySymbols = dependencies.filter {
            !completedSymbols.contains(it.key) && it.value.all { dependency ->
                completedSymbols.contains(
                    dependency
                )
            }
        }.keys.toSet()
        dependencies = dependencies.filter { !newReadySymbols.contains(it.key) }
        for ((_, dependency) in dependencies) {
            dependency.removeAll(readySymbols)
        }
        readySymbols = newReadySymbols
        MemoryCleanupPolicy.cleanupAfterBatch()
    }

    return ok
}

@Suppress("USELESS_CAST")
suspend fun Collection<IntermediateSymbol<*>>.register(
    tokenTable: ConcurrentMutableTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    fixedValues: Map<Symbol, Flt64>? = null,
    callBack: RegistrationStatusCallBack? = null
): Try {
    return coroutineScope {
        val (emptySymbols, notEmptySymbols) = this@register.partition {
            it is LinearIntermediateSymbol<*> && it.solverFlattenedMonomials.run {
                monomials.isEmpty() && constant eq Flt64.zero
            }
        }
        tokenTable.cache(emptySymbols.associateWith { Flt64.zero }.mapKeys { it.key as IntermediateSymbol<*> })
        tokenTable.cacheSymbolContexts(emptySymbols)

        val completedSymbols = emptySymbols.toMutableSet()
        callBack?.invoke(
            RegistrationStatus(
                emptySymbolAmount = emptySymbols.usize,
                readySymbolAmount = completedSymbols.usize,
                totalSymbolAmount = tokenTable.symbols.usize
            )
        )
        var dependencies = notEmptySymbols.associateWith { symbol ->
            symbol.dependencies.filter { dependency ->
                dependency !in completedSymbols
            }.toMutableSet()
        }.toMap()
        for ((symbol, deps) in dependencies) {
            tokenTable.addSymbolWithDependencies(symbol, deps)
        }
        var readySymbols = dependencies.filter { it.value.isEmpty() }.keys
        dependencies = dependencies.filterValues { it.isNotEmpty() }.toMap()
        while (readySymbols.isNotEmpty()) {
            for (symbol in readySymbols) {
                // 为函数符号注册辅助 token；函数约束由 MetaModel/MechanismModel 注册，不属于 token 注册阶段。
                // Register auxiliary tokens for function symbols; function constraints are registered by MetaModel/MechanismModel, not this phase.
                when (val result = symbol.registerAuxTokensStar(tokenTable)) {
                    is Ok -> {}
                    is Failed -> { return@coroutineScope Failed(result.error) }
                    is Fatal -> { return@coroutineScope Fatal(result.errors) }
                }
            }
            MemoryCleanupPolicy.cleanupBeforeConcurrentBatch()

            val dispatchPlan = computeBatchDispatchPlan(readySymbols.size)
            if (dispatchPlan.availableProcessors > 1) {
                val thisCompletedSymbolAmountLock = Any()
                var thisCompletedSymbolAmount = completedSymbols.usize
                val jobs = if (dispatchPlan.shouldUseParallelPath) {
                    val segment = dispatchPlan.segmentSize
                    val readySymbolList = readySymbols.toList().shuffled()
                    buildBatchSlices(
                        itemCount = readySymbolList.size,
                        segmentSize = segment
                    ).map { slice ->
                        launch(Dispatchers.Default) {
                            val thisReadSymbol = readySymbolList
                                .subList(slice.fromIndex, slice.toIndexExclusive)
                            // B2: Batch write value cache via prepare + cache
                            // B2: 通过 prepare + cache 批量写入 value 缓存
                            tokenTable.cache(
                                symbols = thisReadSymbol.associateWithNotNull {
                                    it.prepareStar(fixedValues, tokenTable)
                                }.mapKeys { it.key as IntermediateSymbol<*> }
                            )
                            // B2: Batch write flatten/range cache
                            // B2: 批量写入 flatten/range 缓存
                            tokenTable.cacheSymbolContexts(thisReadSymbol)
                            MemoryCleanupPolicy.cleanupAfterAsyncBatch()

                            if (callBack != null) {
                                synchronized(thisCompletedSymbolAmountLock) {
                                    thisCompletedSymbolAmount += thisReadSymbol.usize
                                    callBack(
                                        RegistrationStatus(
                                            emptySymbolAmount = emptySymbols.usize,
                                            readySymbolAmount = thisCompletedSymbolAmount,
                                            totalSymbolAmount = tokenTable.symbols.usize
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    listOf(
                        launch(Dispatchers.Default) {
                            // B2: Batch write value cache via prepare + cache
                            // B2: 通过 prepare + cache 批量写入 value 缓存
                            tokenTable.cache(
                                symbols = readySymbols.associateWithNotNull {
                                    it.prepareStar(fixedValues, tokenTable)
                                }.mapKeys { it.key as IntermediateSymbol<*> }
                            )
                            // B2: Batch write flatten/range cache
                            // B2: 批量写入 flatten/range 缓存
                            tokenTable.cacheSymbolContexts(readySymbols)

                            if (callBack != null) {
                                synchronized(thisCompletedSymbolAmountLock) {
                                    thisCompletedSymbolAmount += readySymbols.usize
                                    callBack(
                                        RegistrationStatus(
                                            emptySymbolAmount = emptySymbols.usize,
                                            readySymbolAmount = thisCompletedSymbolAmount,
                                            totalSymbolAmount = tokenTable.symbols.usize
                                        )
                                    )
                                }
                            }
                        }
                    )
                }

                completedSymbols.addAll(readySymbols)
                val newReadySymbols = dependencies.filter {
                    !completedSymbols.contains(it.key) && it.value.all { dependency ->
                        completedSymbols.contains(
                            dependency
                        )
                    }
                }.keys.toSet()
                dependencies = dependencies.filter { !newReadySymbols.contains(it.key) }
                for ((_, dependency) in dependencies) {
                    dependency.removeAll(readySymbols)
                }
                readySymbols = newReadySymbols
                jobs.joinAll()
                MemoryCleanupPolicy.cleanupAfterBatch()
            } else {
                tokenTable.cache(
                    symbols = readySymbols.associateWithNotNull {
                        it.prepareStar(fixedValues, tokenTable)
                    }.mapKeys { it.key as IntermediateSymbol<*> }
                )
                tokenTable.cacheSymbolContexts(readySymbols)

                callBack?.invoke(
                    RegistrationStatus(
                        emptySymbolAmount = emptySymbols.usize,
                        readySymbolAmount = completedSymbols.usize + readySymbols.usize,
                        totalSymbolAmount = tokenTable.symbols.usize
                    )
                )

                completedSymbols.addAll(readySymbols)
                val newReadySymbols = dependencies.filter {
                    !completedSymbols.contains(it.key) && it.value.all { dependency ->
                        completedSymbols.contains(
                            dependency
                        )
                    }
                }.keys.toSet()
                dependencies = dependencies.filter { !newReadySymbols.contains(it.key) }
                for ((_, dependency) in dependencies) {
                    dependency.removeAll(readySymbols)
                }
                readySymbols = newReadySymbols
                MemoryCleanupPolicy.cleanupAfterBatch()
            }
        }

        ok
    }
}
