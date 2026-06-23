package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Solver 数值转换适配器 / Solver value conversion adapter for bridging domain V and solver Flt64
 *
 * 职责 / Responsibilities:
 * - 提供 IntoValue<V> 用于 solver symbol 构造与求值 / Provide IntoValue<V> for solver symbol construction and evaluation
 * - 提供解值舍入、下取整等调度专用转换 / Provide scheduling-specific conversions like rounding and floor
 * - 集中非有限值、溢出、精度损失处理 / Centralize handling of non-finite values, overflow, and precision loss
 *
 * 所有 V 与 Flt64 的转换应通过此 adapter 进行，避免各子模块自行转换或散落 converter 逻辑。
 * All V-to-Flt64 conversions should go through this adapter to avoid scattered converter logic across sub-modules.
 */
interface SchedulingSolverValueAdapter<V : RealNumber<V>> : IntoValue<V> {
    /** 底层 IntoValue 转换器，可传递给 solver symbol API / Underlying IntoValue converter, passable to solver symbol API */
    val converter: IntoValue<V>

    /** 将 solver 解值舍入为最接近的合法值（例如整数化）/ Round a solver solution value to the nearest legal value (e.g., integer) */
    fun roundSolution(value: Flt64): V

    /** 将 solver Flt64 下取整为 UInt64，用于整数解判定 / Floor a solver Flt64 to UInt64 for integer solution determination */
    fun floorToUInt64(value: Flt64): UInt64

    /** 对 Flt64 值下取整 / Floor a Flt64 value */
    fun floorValue(value: Flt64): Flt64

    companion object {
        /** Flt64 默认 adapter：恒等转换，用于旧路径和 solver 内部 / Default Flt64 adapter: identity conversion, used by legacy paths and solver internals */
        val Flt64: SchedulingSolverValueAdapter<Flt64> = Flt64SolverValueAdapter

        /**
         * 为指定数值类型 V 创建 adapter / Create adapter for numeric type V
         *
         * Flt64 走 Identity 快速路径，其他类型通过 resolveFlt64ValueConverter 反射解析。
         * Flt64 takes the Identity fast path; other types are resolved via resolveFlt64ValueConverter reflection.
         */
        inline fun <reified V : RealNumber<V>> create(): Ret<SchedulingSolverValueAdapter<V>> {
            return if (V::class == Flt64::class) {
                @Suppress("UNCHECKED_CAST")
                Ok(Flt64 as SchedulingSolverValueAdapter<V>)
            } else {
                resolveFlt64ValueConverter<V>("SchedulingSolverValueAdapter.create")
                    .map { converter -> GenericSolverValueAdapter(converter) }
            }
        }
    }
}

/** solver 默认 adapter 的共享出口 / Shared default adapter for solver internals */
val schedulingSolverValueAdapter: SchedulingSolverValueAdapter<Flt64> = SchedulingSolverValueAdapter.Flt64

/** Flt64 恒等 adapter 实现，等价于 IntoValue.Identity 加上调度专用方法 / Flt64 identity adapter implementation, equivalent to IntoValue.Identity plus scheduling-specific methods */
private object Flt64SolverValueAdapter : SchedulingSolverValueAdapter<Flt64> {
    override val converter: IntoValue<Flt64> get() = IntoValue.Identity
    override fun intoValue(value: Flt64): Flt64 = value
    override val zero: Flt64 get() = Flt64.zero
    override val one: Flt64 get() = Flt64.one
    override fun fromValue(value: Flt64): Flt64 = value
    override fun roundSolution(value: Flt64): Flt64 = value.round()
    override fun floorToUInt64(value: Flt64): UInt64 = value.round().toUInt64()
    override fun floorValue(value: Flt64): Flt64 = value.floor()
}

/** 泛型 adapter 实现，通过 Flt64ValueConverter 桥接任意 V / Generic adapter implementation, bridging arbitrary V via Flt64ValueConverter */
class GenericSolverValueAdapter<V : RealNumber<V>>(
    private val delegate: fuookami.ospf.kotlin.math.algebra.concept.Flt64ValueConverter<V>
) : SchedulingSolverValueAdapter<V> {
    override val converter: IntoValue<V> get() = IntoValue.fromConverter(delegate)
    override fun intoValue(value: Flt64): V = delegate.intoValue(value)
    override val zero: V get() = delegate.zero
    override val one: V get() = delegate.one
    override fun fromValue(value: V): Flt64 = delegate.fromValue(value)
    override fun roundSolution(value: Flt64): V = delegate.intoValue(value.round())
    override fun floorToUInt64(value: Flt64): UInt64 = value.round().toUInt64()
    override fun floorValue(value: Flt64): Flt64 = value.floor()
}

/**
 * solver 边界：UInt64 到 Flt64 的集中转换 / Solver boundary: centralized UInt64-to-Flt64 conversion
 */
fun UInt64.toSolverFlt64(): Flt64 = Flt64(toLong().toDouble())

/**
 * solver 边界：泛型域值 V 到 Flt64 的集中转换 / Solver boundary: centralized generic domain value V-to-Flt64 conversion
 */
fun <V : RealNumber<V>> V.toSolverValue(): Flt64 = toFlt64()
