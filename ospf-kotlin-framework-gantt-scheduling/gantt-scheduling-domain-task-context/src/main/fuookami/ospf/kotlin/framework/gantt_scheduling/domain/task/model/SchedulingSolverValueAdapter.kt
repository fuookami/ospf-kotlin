@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.resolveFlt64ValueConverter
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * Solver 数值转换适配器，统一领域数值 V 与 solver 内部 Flt64 的双向转换边界。
 *
 * 职责：
 * - 提供 IntoValue<V> 用于 solver symbol 构造与求值。
 * - 提供解值舍入、下取整等调度专用转换。
 * - 集中非有限值、溢出、精度损失处理。
 *
 * 所有 V 与 Flt64 的转换应通过此 adapter 进行，避免各子模块自行转换或散落 converter 逻辑。
 */
interface SchedulingSolverValueAdapter<V : RealNumber<V>> : IntoValue<V> {
    /** 底层 IntoValue 转换器，可传递给 solver symbol API。 */
    val converter: IntoValue<V>

    /** 将 solver 解值舍入为最接近的合法值（例如整数化）。 */
    fun roundSolution(value: Flt64): V

    /** 将 solver Flt64 下取整为 UInt64，用于整数解判定。 */
    fun floorToUInt64(value: Flt64): UInt64

    /** 对 Flt64 值下取整。 */
    fun floorValue(value: Flt64): Flt64

    companion object {
        /** Flt64 默认 adapter：恒等转换，用于旧路径和 solver 内部。 */
        val Flt64: SchedulingSolverValueAdapter<Flt64> = Flt64SolverValueAdapter

        /**
         * 为指定数值类型 V 创建 adapter。
         * Flt64 走 Identity 快速路径，其他类型通过 resolveFlt64ValueConverter 反射解析。
         */
        inline fun <reified V : RealNumber<V>> create(): SchedulingSolverValueAdapter<V> {
            return if (V::class == Flt64::class) {
                @Suppress("UNCHECKED_CAST")
                Flt64 as SchedulingSolverValueAdapter<V>
            } else {
                GenericSolverValueAdapter(resolveFlt64ValueConverter("SchedulingSolverValueAdapter.create"))
            }
        }
    }
}

/** Flt64 恒等 adapter 实现，等价于 IntoValue.Identity 加上调度专用方法。 */
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

/** 泛型 adapter 实现，通过 Flt64ValueConverter 桥接任意 V。 */
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
