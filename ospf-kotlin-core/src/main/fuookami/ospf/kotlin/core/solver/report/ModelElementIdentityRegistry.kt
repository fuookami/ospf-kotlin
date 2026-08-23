/** Stable model-element identity registry. / 稳定模型元素身份注册表。 */
package fuookami.ospf.kotlin.core.solver.report

import java.util.EnumMap
import java.util.IdentityHashMap
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

/** Model-element category used by the cross-layer identity registry. / 跨层身份注册表使用的模型元素类别。 */
enum class ModelElementKind {
    Variable,
    Constraint,
    Objective
}

/**
 * Identity binding carried from a modeling entry point to a solver artifact. /
 * 从建模入口传递到求解器 artifact 的身份绑定。
 *
 * @property kind 元素类别 / Element category
 * @property id 类型化稳定标识 / Typed stable identifier
 * @property scope 身份作用域 / Identity scope
 * @property origin 稳定来源 / Stable origin
 * @property provenance 完整来源集合 / Complete source provenance
 */
data class ModelElementIdentity(
    val kind: ModelElementKind,
    val id: ModelElementId,
    val scope: ModelElementScope,
    val origin: ModelElementOrigin? = null,
    val provenance: List<ModelElementOrigin> = origin?.let(::listOf).orEmpty()
)

/**
 * Explicit registry for identities that must survive model rebuilding. /
 * 用于跨模型重建保留身份的显式注册表。
 *
 * The registry is deliberately opt-in. A missing entry is represented as model-local by the
 * intermediate model and must never be advertised as a cross-rebuild stable identity. /
 * 注册表刻意采用显式接入；未注册元素在中间模型中表示为 model-local，绝不能伪装成跨重建稳定身份。
 *
 * @property namespace identity namespace shared by serialized artifacts / 序列化 artifact 共享的身份命名空间
 * @property schemaVersion identity registry schema version / 身份注册表 schema 版本
 */
class ModelElementIdentityRegistry(
    val namespace: String = "ospf",
    val schemaVersion: String = "1.0"
) {
    private val bindings = IdentityHashMap<Any, ModelElementIdentity>()
    private val fallbackBindings = IdentityHashMap<Any, MutableMap<ModelElementKind, ModelElementIdentity>>()
    private val ids = HashMap<ModelElementId, Pair<ModelElementKind, Any>>()
    private val fallbackCounters = HashMap<Pair<ModelElementKind, String>, Int>()

    /**
     * Register a variable identity. / 注册变量身份。
     *
     * @param element Variable object whose identity is being registered. / 要注册身份的变量对象。
     * @param id Stable variable identifier. / 稳定变量标识。
     * @param origin Optional source identity. / 可选的来源身份。
     * @param provenance Complete source identities; defaults to origin when omitted. / 完整来源身份集合；省略时默认使用 origin。
     * @return Registration result. / 注册结果。
     */
    @Synchronized
    fun registerVariable(
        element: Any,
        id: VariableId,
        origin: ModelElementOrigin? = null,
        provenance: List<ModelElementOrigin> = emptyList()
    ): Try {
        return register(
            element = element,
            kind = ModelElementKind.Variable,
            id = ModelElementId(id.value),
            origin = origin,
            provenance = provenance
        )
    }

    /**
     * Register a constraint identity. / 注册约束身份。
     *
     * @param element Constraint object whose identity is being registered. / 要注册身份的约束对象。
     * @param id Stable constraint identifier. / 稳定约束标识。
     * @param origin Optional source identity. / 可选的来源身份。
     * @param provenance Complete source identities; defaults to origin when omitted. / 完整来源身份集合；省略时默认使用 origin。
     * @return Registration result. / 注册结果。
     */
    @Synchronized
    fun registerConstraint(
        element: Any,
        id: ConstraintId,
        origin: ModelElementOrigin? = null,
        provenance: List<ModelElementOrigin> = emptyList()
    ): Try {
        return register(
            element = element,
            kind = ModelElementKind.Constraint,
            id = ModelElementId(id.value),
            origin = origin,
            provenance = provenance
        )
    }

    /**
     * Register an objective identity. / 注册目标身份。
     *
     * @param element Objective object whose identity is being registered. / 要注册身份的目标对象。
     * @param id Stable objective identifier. / 稳定目标标识。
     * @param origin Optional source identity. / 可选的来源身份。
     * @param provenance Complete source identities; defaults to origin when omitted. / 完整来源身份集合；省略时默认使用 origin。
     * @return Registration result. / 注册结果。
     */
    @Synchronized
    fun registerObjective(
        element: Any,
        id: ObjectiveId,
        origin: ModelElementOrigin? = null,
        provenance: List<ModelElementOrigin> = emptyList()
    ): Try {
        return register(
            element = element,
            kind = ModelElementKind.Objective,
            id = ModelElementId(id.value),
            origin = origin,
            provenance = provenance
        )
    }

    /**
     * Resolve a registered variable ID, or return a model-local fallback. / 解析变量 ID，未注册时返回模型内 fallback。
     *
     * @param element Variable object to resolve. / 要解析的变量对象。
     * @param fallbackIndex Model-local index used by the fallback. / fallback 使用的模型内索引。
     * @return Resolved variable identifier. / 解析后的变量标识。
     */
    @Synchronized
    fun variableId(element: Any, fallbackIndex: Int): VariableId {
        return bindings[element]
            ?.takeIf { it.kind == ModelElementKind.Variable }
            ?.id
            ?.let { VariableId(it.value) }
            ?: rememberFallback(element, ModelElementKind.Variable, "model-local-variable:$fallbackIndex")
                .let { VariableId(it.value) }
    }

    /**
     * Resolve a registered constraint ID, or return a model-local fallback. / 解析约束 ID，未注册时返回模型内 fallback。
     *
     * @param element Constraint object to resolve. / 要解析的约束对象。
     * @param fallbackIndex Model-local index used by the fallback. / fallback 使用的模型内索引。
     * @return Resolved constraint identifier. / 解析后的约束标识。
     */
    @Synchronized
    fun constraintId(element: Any, fallbackIndex: Int): ConstraintId {
        return bindings[element]
            ?.takeIf { it.kind == ModelElementKind.Constraint }
            ?.id
            ?.let { ConstraintId(it.value) }
            ?: rememberFallback(element, ModelElementKind.Constraint, "model-local-constraint:$fallbackIndex")
                .let { ConstraintId(it.value) }
    }

    /**
     * Resolve a registered objective ID, or return a model-local fallback. / 解析目标 ID，未注册时返回模型内 fallback。
     *
     * @param element Objective object to resolve. / 要解析的目标对象。
     * @param fallbackIndex Model-local index used by the fallback. / fallback 使用的模型内索引。
     * @return Resolved objective identifier. / 解析后的目标标识。
     */
    @Synchronized
    fun objectiveId(element: Any, fallbackIndex: Int = 0): ObjectiveId {
        return bindings[element]
            ?.takeIf { it.kind == ModelElementKind.Objective }
            ?.id
            ?.let { ObjectiveId(it.value) }
            ?: rememberFallback(element, ModelElementKind.Objective, "model-local-objective:$fallbackIndex")
                .let { ObjectiveId(it.value) }
    }

    /**
     * Return the registered identity for an element. / 返回元素已注册的身份。
     *
     * @param element Model element to inspect. / 要检查的模型元素。
     * @return Registered identity, or null when the element is not registered. / 已注册身份；未注册时返回 null。
     */
    @Synchronized
    fun identity(element: Any): ModelElementIdentity? = bindings[element]

    /**
     * Return all bindings in deterministic ID order. / 按稳定 ID 排序返回全部绑定。
     *
     * @return Registered identities sorted by kind and ID. / 按类别和 ID 排序的已注册身份。
     */
    @Synchronized
    fun entries(): List<ModelElementIdentity> {
        return (bindings.values.asSequence() + fallbackBindings.values.asSequence().flatMap { it.values.asSequence() })
            .distinct()
            .sortedWith(compareBy({ it.kind.name }, { it.id.value }))
            .toList()
    }

    /**
     * Validate namespace, schema, and duplicate identities. / 校验命名空间、schema 和重复身份。
     *
     * @return Validation result. / 校验结果。
     */
    @Synchronized
    fun validate(): Try {
        if (namespace.isBlank() || schemaVersion.isBlank()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "模型身份 namespace/schema 不能为空 / Model identity namespace and schema must not be blank"
            )
        }
        if ((bindings.values + fallbackBindings.values.flatMap { it.values }).any { it.id.value.isBlank() }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "模型元素 ID 不能为空 / Model element IDs must not be blank"
            )
        }
        val duplicate = entries()
            .groupBy { it.id }
            .values
            .firstOrNull { owners -> owners.size > 1 }
        if (duplicate != null) {
            val identity = duplicate.first()
            return Failed(
                ErrorCode.IllegalArgument,
                "模型元素 ID 重复：${identity.id.value} / Duplicate model element ID: ${identity.id.value}"
            )
        }
        return ok
    }

    private fun rememberFallback(
        element: Any,
        kind: ModelElementKind,
        id: String
    ): ModelElementId {
        val byKind = fallbackBindings.getOrPut(element) { EnumMap(ModelElementKind::class.java) }
        val identity = byKind[kind]
        if (identity != null) {
            return identity.id
        }
        val base = id
        var candidate = base
        var suffix = fallbackCounters[kind to base] ?: 0
        while (ids.containsKey(ModelElementId(candidate))) {
            suffix += 1
            candidate = "$base:collision-$suffix"
        }
        fallbackCounters[kind to base] = suffix
        val created = ModelElementIdentity(
            kind = kind,
            id = ModelElementId(candidate),
            scope = ModelElementScope.ModelLocal
        )
        byKind[kind] = created
        ids[created.id] = kind to element
        return created.id
    }

    private fun register(
        element: Any,
        kind: ModelElementKind,
        id: ModelElementId,
        origin: ModelElementOrigin?,
        provenance: List<ModelElementOrigin>
    ): Try {
        if (id.value.isBlank()) {
            return Failed(ErrorCode.IllegalArgument, "模型元素 ID 不能为空 / Model element ID must not be blank")
        }
        if (RESERVED_PREFIXES.any(id.value::startsWith)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "模型元素 ID 使用了保留前缀：${id.value} / " +
                    "Model element ID uses a reserved prefix: ${id.value}"
            )
        }
        val canonicalProvenance = (if (provenance.isEmpty()) {
            listOfNotNull(origin)
        } else {
            provenance
        }).distinct().sortedWith(compareBy({ it.kind }, { it.key }))
        if (canonicalProvenance.any { it.kind.isBlank() || it.key.isBlank() }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "模型元素 provenance 不能为空 / Model element provenance entries must not be blank"
            )
        }
        if (origin != null && origin !in canonicalProvenance) {
            return Failed(
                ErrorCode.IllegalArgument,
                "模型元素 primary origin 必须属于 provenance / Model element primary origin must be included in provenance"
            )
        }
        val existing = bindings[element]
        if (existing != null) {
            return if (
                existing.kind == kind &&
                existing.id == id &&
                existing.origin == origin &&
                existing.provenance == canonicalProvenance
            ) {
                ok
            } else {
                Failed(
                    ErrorCode.IllegalArgument,
                    "模型元素重复绑定身份 / Model element is already bound to another identity"
                )
            }
        }
        val existingIdentity = ids[id]
        if (existingIdentity != null &&
            (existingIdentity.first != kind || existingIdentity.second !== element)
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "模型元素 ID 重复：${id.value} / Duplicate model element ID: ${id.value}"
            )
        }
        val fallback = fallbackBindings[element]?.remove(kind)
        if (fallback != null) {
            ids.remove(fallback.id)
            if (fallbackBindings[element].isNullOrEmpty()) {
                fallbackBindings.remove(element)
            }
        }
        val identity = ModelElementIdentity(
            kind = kind,
            id = id,
            scope = ModelElementScope.Stable,
            origin = origin,
            provenance = canonicalProvenance
        )
        bindings[element] = identity
        ids[id] = kind to element
        return ok
    }

    private companion object {
        val RESERVED_PREFIXES = listOf("model-local-", "artifact:")
    }
}
