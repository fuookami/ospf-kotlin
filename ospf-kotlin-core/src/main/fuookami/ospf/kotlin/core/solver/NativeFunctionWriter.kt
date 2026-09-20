package fuookami.ospf.kotlin.core.solver

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.core.variable.VariableItemKey
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Backend-neutral native writer boundary. / 求解器无关的原生 writer 边界。
 *
 * The erased batch is intentional: each solver SDK has its own model and prepared
 * native data types. / 这里使用类型擦除是有意的：各求解器 SDK 有自己的模型和准备数据类型。
 */
fun interface NativeFunctionWriter<M, V> {
    fun write(
        model: M,
        variables: Map<VariableItemKey, V>,
        batch: List<Any>
    ): Try
}

/** Type-safe adapter used to register heterogeneous writers in one list. / 用于将异构 writer 注册到同一列表中的类型安全适配器。 */
abstract class TypedNativeFunctionWriter<M, V, T : Any>(
    private val type: KClass<T>
) : NativeFunctionWriter<M, V> {
    final override fun write(
        model: M,
        variables: Map<VariableItemKey, V>,
        batch: List<Any>
    ): Try {
        return writeTyped(model, variables, batch.filter { type.isInstance(it) }.map { it as T })
    }

    protected abstract fun writeTyped(
        model: M,
        variables: Map<VariableItemKey, V>,
        batch: List<T>
    ): Try
}

class NativeFunctionWriterRegistry<M, V>(
    private val writers: List<NativeFunctionWriter<M, V>>
) {
    fun write(
        model: M,
        variables: Map<VariableItemKey, V>,
        batches: List<List<Any>>
    ): Try {
        if (writers.size != batches.size) {
            return Failed(
                ErrorCode.IllegalArgument,
                "native writer 与 batch 数量不一致 / Native writer and batch counts do not match"
            )
        }
        for ((writer, batch) in writers.zip(batches)) {
            if (batch.isEmpty()) {
                continue
            }
            when (val result = writer.write(model, variables, batch)) {
                is Ok -> Unit
                is Failed -> return result
                is Fatal -> return result
            }
        }
        return ok
    }
}
