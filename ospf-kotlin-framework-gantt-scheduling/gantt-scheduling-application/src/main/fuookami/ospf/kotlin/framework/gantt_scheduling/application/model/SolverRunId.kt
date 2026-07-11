/**
 * 应用层 solver run id 值类型 / Application layer solver run id value type
 *
 * 用于 BranchAndPriceAlgorithm 等算法入口的 id 参数，需与 core LinearMetaModel(id: String) 互操作，
 * 故采用 @JvmInline value class 承载 String 裸值，而非纯标记接口。
 * Use @JvmInline value class (not marker interface) because it must interop with core LinearMetaModel(name: String).
*/
package fuookami.ospf.kotlin.framework.gantt_scheduling.application.model
/**
 * Inline value class representing a unique identifier for a solver run, interoperable with LinearMetaModel.
 * 表示求解器运行唯一标识的内联值类，可与 LinearMetaModel 互操作
*/
@JvmInline
value class SolverRunId(val value: String) {
    override fun toString(): String = value
}
