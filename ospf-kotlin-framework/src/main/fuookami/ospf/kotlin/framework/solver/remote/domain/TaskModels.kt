/**
 * 远程求解任务模型
 * Remote solve task models
*/
package fuookami.ospf.kotlin.framework.solver.remote.domain

import kotlin.time.Duration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 任务复杂度。
 * Task complexity.
*/
@Serializable
enum class TaskComplexity {
    /** 简单任务 / Simple task */
    SIMPLE,

    /** 复杂任务 / Complex task */
    COMPLEX
}

/**
 * 时间敏感度。
 * Time sensitivity.
*/
@Serializable
enum class TimeSensitivity {
    /** 实时任务 / Realtime task */
    REALTIME,

    /** 非实时任务 / Non-realtime task */
    NON_REALTIME
}

/**
 * 任务状态。
 * Task status.
*/
@Serializable
enum class TaskStatus {
    /** 已创建 / Created */
    CREATED,

    /** 已接受 / Accepted */
    ACCEPTED,

    /** 已入队 / Queued */
    QUEUED,

    /** 正在分发 / Dispatching */
    DISPATCHING,

    /** 正在运行 / Running */
    RUNNING,

    /** 已暂停 / Suspended */
    SUSPENDED,

    /** 已完成 / Completed */
    COMPLETED,

    /** 正在停止 / Stopping */
    STOPPING,

    /** 已停止 / Stopped */
    STOPPED,

    /** 已失败 / Failed */
    FAILED,

    /** 等待预算释放 / Waiting for budget */
    WAITING_FOR_BUDGET
}

/**
 * 切片状态。
 * Slice status.
*/
@Serializable
enum class SliceStatus {
    /** 已规划 / Planned */
    PLANNED,

    /** 正在运行 / Running */
    RUNNING,

    /** 正在保存检查点 / Checkpointing */
    CHECKPOINTING,

    /** 已暂停 / Suspended */
    SUSPENDED,

    /** 已完成 / Completed */
    COMPLETED,

    /** 已失败 / Failed */
    FAILED
}

/**
 * 任务元数据。
 * Task metadata.
 *
 * @property solverType 求解器类型偏好 / Solver type preference
 * @property targetType 目标类型 / Target type
 * @property timeLimit 时间限制 / Time limit
 * @property solutionLimit 解数量限制 / Solution count limit
 * @property estimatedVariableCount 预估变量数 / Estimated variable count
 * @property estimatedConstraintCount 预估约束数 / Estimated constraint count
 * @property historicalRuntime 历史运行时间 / Historical runtime
 * @property metadata 扩展元数据 / Extension metadata
*/
@Serializable
data class TaskMeta(
    val solverType: SolverTypeName? = null,
    val targetType: TargetTypeName? = null,
    @SerialName("timeLimitMs")
    @Serializable(with = RemoteSolverMillisecondsDurationSerializer::class)
    val timeLimit: Duration? = null,
    val solutionLimit: Int? = null,
    val estimatedVariableCount: Int? = null,
    val estimatedConstraintCount: Int? = null,
    @SerialName("historicalRuntimeMs")
    @Serializable(with = RemoteSolverMillisecondsDurationSerializer::class)
    val historicalRuntime: Duration? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 模型数据。
 * Model data.
 *
 * @property ref 模型对象引用 / Model object reference
 * @property linearModel 内联线性模型 / Inline linear model
 * @property quadraticModel 内联二次模型 / Inline quadratic model
 * @property rawBytes 原始模型字节 / Raw model bytes
 * @property format 原始字节格式 / Raw bytes format
*/
@Serializable
data class ModelData(
    val ref: ObjectRef? = null,
    val linearModel: SerializedLinearModel? = null,
    val quadraticModel: SerializedQuadraticModel? = null,
    val rawBytes: ByteArray? = null,
    val format: String? = null
) {

    /** 是否引用模式 / Whether reference mode */
    val isReference: Boolean get() = ref != null

    /** 是否内联模式 / Whether inline mode */
    val isInline: Boolean get() = linearModel != null || quadraticModel != null || rawBytes != null

    /** 模型类型 / Model type */
    val modelType: NormalizedModelType
        get() = when {
            quadraticModel != null -> NormalizedModelType.QUADRATIC
            linearModel != null -> NormalizedModelType.LINEAR
            else -> NormalizedModelType.UNKNOWN
        }

    companion object {
        /**
         * 创建引用模型数据。
         * Create reference model data.
         *
         * @param ref 对象引用 / Object reference
         * @return 模型数据 / Model data
        */
        fun reference(ref: ObjectRef): ModelData {
            return ModelData(ref = ref)
        }

        /**
         * 创建线性模型数据。
         * Create linear model data.
         *
         * @param model 线性模型 / Linear model
         * @return 模型数据 / Model data
        */
        fun linear(model: SerializedLinearModel): ModelData {
            return ModelData(linearModel = model)
        }

        /**
         * 创建二次模型数据。
         * Create quadratic model data.
         *
         * @param model 二次模型 / Quadratic model
         * @return 模型数据 / Model data
        */
        fun quadratic(model: SerializedQuadraticModel): ModelData {
            return ModelData(quadraticModel = model)
        }

        /**
         * 创建原始字节模型数据。
         * Create raw bytes model data.
         *
         * @param bytes 字节内容 / Bytes
         * @param format 字节格式 / Bytes format
         * @return 模型数据 / Model data
        */
        fun raw(bytes: ByteArray, format: String): ModelData {
            return ModelData(rawBytes = bytes, format = format)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ModelData) {
            return false
        }
        val rawBytesEqual = (rawBytes == null && other.rawBytes == null) ||
            (rawBytes != null && other.rawBytes != null && rawBytes.contentEquals(other.rawBytes))
        return ref == other.ref &&
            linearModel == other.linearModel &&
            quadraticModel == other.quadraticModel &&
            rawBytesEqual &&
            format == other.format
    }

    override fun hashCode(): Int {
        var result = ref?.hashCode() ?: 0
        result = 31 * result + (linearModel?.hashCode() ?: 0)
        result = 31 * result + (quadraticModel?.hashCode() ?: 0)
        result = 31 * result + (rawBytes?.contentHashCode() ?: 0)
        result = 31 * result + (format?.hashCode() ?: 0)
        return result
    }
}

/**
 * 求解配置。
 * Solver config.
 *
 * @property timeLimit 时间限制 / Time limit
 * @property solutionLimit 解数量限制 / Solution count limit
 * @property mipGapTolerance MIP gap 容忍度 / MIP gap tolerance
 * @property threads 线程数 / Thread count
 * @property solverParams 求解器参数 / Solver params
*/
@Serializable
data class SolverConfig(
    @SerialName("timeLimitMs")
    @Serializable(with = RemoteSolverMillisecondsDurationSerializer::class)
    val timeLimit: Duration? = null,
    val solutionLimit: Int? = null,
    val mipGapTolerance: Flt64? = null,
    val threads: Int? = null,
    val solverParams: Map<String, String> = emptyMap()
)

/**
 * 求解载荷。
 * Solve payload.
 *
 * @property modelData 模型数据 / Model data
 * @property configRef 配置引用 / Config reference
 * @property config 内联配置 / Inline config
 * @property snapshotRef 快照引用 / Snapshot reference
 * @property taskMeta 任务元数据 / Task metadata
 * @property extension 扩展字段 / Extension fields
*/
@Serializable
data class SolvePayload(
    val modelData: ModelData,
    val configRef: ObjectRef? = null,
    val config: SolverConfig? = null,
    val snapshotRef: ObjectRef? = null,
    val taskMeta: TaskMeta = TaskMeta(),
    val extension: Map<String, String> = emptyMap()
) {

    /**
     * 引用模式便捷构造器。
     * Reference mode convenience constructor.
    */
    constructor(
        modelRef: ObjectRef,
        configRef: ObjectRef? = null,
        snapshotRef: ObjectRef? = null,
        taskMeta: TaskMeta = TaskMeta(),
        extension: Map<String, String> = emptyMap()
    ) : this(
        modelData = ModelData.reference(modelRef),
        configRef = configRef,
        snapshotRef = snapshotRef,
        taskMeta = taskMeta,
        extension = extension
    )

    /**
     * 内联线性模型便捷构造器。
     * Inline linear model convenience constructor.
    */
    constructor(
        linearModel: SerializedLinearModel,
        config: SolverConfig? = null,
        taskMeta: TaskMeta = TaskMeta(),
        extension: Map<String, String> = emptyMap()
    ) : this(
        modelData = ModelData.linear(linearModel),
        config = config,
        taskMeta = taskMeta,
        extension = extension
    )

    /** 模型引用 / Model reference */
    val modelRef: ObjectRef? get() = modelData.ref
}
