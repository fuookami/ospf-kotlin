package fuookami.ospf.kotlin.core.solver

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/**
 * 求解器无关的原生 writer 边界。 / Backend-neutral native writer boundary.
 *
 * 这里使用类型擦除是有意的：各求解器 SDK 有自己的模型和准备数据类型。 /
 * The erased batch is intentional: each solver SDK has its own model and prepared native data types.
 *
 * @param M 求解器模型类型 / Solver model type
 * @param V 求解器变量值类型 / Solver variable value type
 */
fun interface NativeFunctionWriter<M, V> {
    /**
     * 将一批原生结构写入求解器模型。 / Write a batch of native structures to the solver model.
     *
     * @param model 求解器模型 / Solver model
     * @param variables 变量值映射 / Variable value map
     * @param batch 待写入的类型擦除结构 / Type-erased structures to write
     * @return 写入结果 / Write result
     */
    fun write(
        model: M,
        variables: Map<VariableItemKey, V>,
        batch: List<Any>
    ): Try
}

/**
 * 用于将异构 writer 注册到同一列表中的类型安全适配器。 /
 * Type-safe adapter used to register heterogeneous writers in one list.
 *
 * @param M 求解器模型类型 / Solver model type
 * @param V 求解器变量值类型 / Solver variable value type
 * @param T writer 处理的结构类型 / Structure type handled by the writer
 * @property type writer 处理的结构类型标记 / Runtime type marker for the handled structure
 */
abstract class TypedNativeFunctionWriter<M, V, T : Any>(
    private val type: KClass<T>
) : NativeFunctionWriter<M, V> {
    /**
     * 将匹配类型的结构写入求解器模型。 / Write structures matching the registered type to the solver model.
     *
     * @param model 求解器模型 / Solver model
     * @param variables 变量值映射 / Variable value map
     * @param batch 待写入的类型擦除结构 / Type-erased structures to write
     * @return 写入结果 / Write result
     */
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

/**
 * 按顺序协调多个原生 writer。 / Coordinates multiple native writers in order.
 *
 * @param M 求解器模型类型 / Solver model type
 * @param V 求解器变量值类型 / Solver variable value type
 * @property writers 原生 writer 列表 / Native writer list
 */
class NativeFunctionWriterRegistry<M, V>(
    private val writers: List<NativeFunctionWriter<M, V>>
) {
    /**
     * 将各批结构交给对应 writer。 / Dispatch each structure batch to its corresponding writer.
     *
     * @param model 求解器模型 / Solver model
     * @param variables 变量值映射 / Variable value map
     * @param batches 与 writer 一一对应的结构批次 / Structure batches corresponding to writers
     * @return 写入结果 / Write result
     */
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
