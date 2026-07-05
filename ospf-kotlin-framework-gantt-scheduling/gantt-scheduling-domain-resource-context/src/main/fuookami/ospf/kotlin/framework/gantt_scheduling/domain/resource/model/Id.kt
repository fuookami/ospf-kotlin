/**
 * 资源域 id 语义标记接口 / Resource domain id semantic marker interfaces
 *
 * 将原 String id 改为语义化标记接口，业务侧实现接口、子类协变 override，
 * 以保留类型安全（参考 task-context 的标记接口方案）。
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

/**
 * 资源 id 标记接口 / Resource id marker interface
 */
interface ResourceId

/**
 * ResourceId 的默认实现：承载 String 裸值 / Default ResourceId impl wrapping a raw String value
 *
 * 用于测试与 adapter 层构造 ResourceId 实例；生产侧可使用自定义 value class / data class 实现。
 * value class 仅允许单字段，此处为简单 data class 承载单字段并 override toString 以保持原拼接行为。
 */
data class ResourceIdImpl(
    val value: String
) : ResourceId {
    override fun toString(): String = value
}
