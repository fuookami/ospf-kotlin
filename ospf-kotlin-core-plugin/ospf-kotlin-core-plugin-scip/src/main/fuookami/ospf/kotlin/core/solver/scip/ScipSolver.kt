/** SCIP 求解器基类 / SCIP solver base */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.scip

import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.time.*
import fuookami.ospf.kotlin.core.solver.config.SCIPSolverConfig
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.report.BackendConfiguration
import fuookami.ospf.kotlin.core.solver.report.BackendParameterValue
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.report.CancellationToken
import fuookami.ospf.kotlin.core.solver.report.CancellationRecord
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.Library
import jscip.*

/** SCIP 求解器抽象基类，提供环境初始化、求解和状态分析的通用实现 / SCIP solver abstract base class, provides common implementation for environment initialization, solving, and status analysis */
@OptIn(ExperimentalTime::class)
abstract class ScipSolver : AutoCloseable {

    /** Companion object managing SCIP native library loading / 伴生对象，管理 SCIP 原生库加载 */
    companion object {
        internal var loadedLibrary = false
        internal var loadedLibraryPath: Path? = null
        internal var loadedLibraryMode: String = "system"

        /** Resolve a stable native SCIP version from explicit runtime metadata. / 从显式运行时元数据解析稳定 SCIP 原生版本。 */
        internal fun runtimeVersion(): String? {
            return listOf(
                System.getProperty("ospf.scip.native.version"),
                System.getProperty("scip.version"),
                System.getenv("SCIP_VERSION")
            ).firstOrNull { !it.isNullOrBlank() }?.trim()
        }

        init {
            loadConfiguredLibrary()
        }

        private fun loadConfiguredLibrary(): Boolean {
            val explicit = System.getProperty("ospf.scip.library")?.takeUnless { it.isBlank() }
            return try {
                if (explicit != null) {
                    val path = Path.of(explicit).toAbsolutePath().normalize()
                    System.load(path.toString())
                    loadedLibraryPath = path
                    loadedLibraryMode = "explicit"
                } else {
                    System.loadLibrary("jscip")
                    loadedLibraryPath = System.getProperty("java.library.path")
                        ?.split(File.pathSeparator)
                        ?.asSequence()
                        ?.map { Path.of(it).resolve(System.mapLibraryName("jscip")) }
                        ?.firstOrNull { java.nio.file.Files.isRegularFile(it) }
                    loadedLibraryMode = "system"
                }
                loadedLibrary = true
                true
            } catch (_: Throwable) {
                loadedLibrary = false
                false
            }
        }

        private val winLibraries = listOf("tbb", "libscip", "jscip")
        private val unixLibraries = listOf("libgcg", "libgmp", "libpthread", "libgfortran", "libquadmath", "libopenblas", "libtbb", "libsplexshared", "libscip", "libjscip")

        /**
         * 从 JAR 包中加载 SCIP 原生库 / Load SCIP native library from JAR package
         *
         * @return 以Try包装的加载结果 / the load result as Try
        */
        fun loadLibraryInJar(): Try {
            val systemType = System.getProperty("os.name")
            val libExtension = if (systemType.lowercase(Locale.getDefault()).indexOf("win") != -1) {
                "dll"
            } else if (systemType.lowercase(Locale.getDefault()).indexOf("mac") != -1) {
                "dylib"
            } else {
                "so"
            }
            val libs = if (systemType.lowercase(Locale.getDefault()).indexOf("win") != -1) {
                winLibraries
            } else if (systemType.lowercase(Locale.getDefault()).indexOf("mac") != -1) {
                emptyList()
            } else {
                unixLibraries
            }

            for (lib in libs) {
                val libFullName = "${lib}.${libExtension}"
                val target = File(System.getProperty("user.dir"), libFullName).absolutePath
                when (val result = Library.loadInJar(libFullName, target)) {
                    is Ok -> {
                        // Unix bundles use the ELF filename `libjscip.so`, while the JVM
                        // logical library name is `jscip`.  Treat both spellings as the
                        // primary binding so runtime provenance is populated consistently.
                        // Unix bundle 使用 `libjscip.so` 文件名，但 JVM 逻辑库名为 `jscip`；
                        // 两种拼写都应登记为主 binding，确保运行时 provenance 一致。
                        if (lib == "jscip" || lib == "libjscip") {
                            loadedLibraryPath = Path.of(target).toAbsolutePath().normalize()
                            loadedLibraryMode = "jar"
                            loadedLibrary = true
                        }
                    }
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            return ok
        }
    }

    protected lateinit var scip: Scip
    protected lateinit var status: SolverStatus
    protected var terminationReason: TerminationReason = TerminationReason.Completed
    protected var solvingTime: Duration? = null
    private var cancellationToken: CancellationToken? = null
    private var cancellationListener: ((CancellationRecord) -> Try)? = null

    /** 关闭 SCIP 求解器，释放资源 / Close SCIP solver, release resources */
    override fun close() {
        cancellationListener?.let { listener ->
            cancellationToken?.unregister(listener)
        }
        cancellationListener = null
        cancellationToken = null
        if (::scip.isInitialized) {
            scip.free()
        }
    }

    /**
     * Register direct SCIP interruption for a solve token. /
     * 为求解令牌注册直接 SCIP 中断监听。
     *
     * @param token 求解取消令牌 / Solve cancellation token
     * @return 注册结果 / Registration result
     */
    protected fun registerCancellation(token: CancellationToken?): Try {
        if (token == null) {
            return ok
        }
        cancellationToken?.let { previousToken ->
            cancellationListener?.let { listener -> previousToken.unregister(listener) }
        }
        val listener: (CancellationRecord) -> Try = {
            try {
                scip.interruptSolve()
                ok
            } catch (error: Exception) {
                Failed(
                    ErrorCode.OREngineTerminated,
                    "SCIP 原生中断失败：${error.message ?: error::class.simpleName} / " +
                        "SCIP native interruption failed: ${error.message ?: error::class.simpleName}"
                )
            }
        }
        cancellationToken = token
        cancellationListener = listener
        return token.register(listener)
    }

    /**
     * Apply the typed SCIP configuration to the native model. /
     * 将类型化 SCIP 配置应用到原生模型。
     *
     * The adapter rejects a configuration belonging to another backend and converts native parameter
     * failures into a structured modeling error. / 适配器拒绝属于其他 backend 的配置，并将原生参数失败转换为结构化建模错误。
     *
     * @param configuration backend 配置 / backend configuration
     * @param requestedThreadCount 请求的线程数 / requested thread count
     * @return 应用结果 / application result
     */
    protected fun applyBackendConfiguration(
        configuration: BackendConfiguration?,
        requestedThreadCount: Int? = null
    ): Try {
        val scipConfiguration = when (configuration) {
            null -> return ok
            is SCIPSolverConfig -> configuration
            else -> {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "SCIP 求解器不能使用 ${configuration.type} backend 配置 / " +
                        "SCIP solver cannot use backend configuration ${configuration.type}"
                )
            }
        }
        if (scipConfiguration.deterministic == true &&
            requestedThreadCount != null &&
            requestedThreadCount != 1
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "SCIP deterministic 配置必须使用单线程 / SCIP deterministic configuration requires one thread"
            )
        }
        return try {
            scipConfiguration.presolve?.let { enabled ->
                scip.setPresolving(
                    if (enabled) {
                        SCIP_ParamSetting.SCIP_PARAMSETTING_DEFAULT
                    } else {
                        SCIP_ParamSetting.SCIP_PARAMSETTING_OFF
                    },
                    true
                )
            }
            scipConfiguration.randomSeed?.let { seed ->
                if (seed < Int.MIN_VALUE || seed > Int.MAX_VALUE) {
                    return Failed(
                        ErrorCode.IllegalArgument,
                        "SCIP randomSeed 超出原生整数范围 / SCIP randomSeed exceeds the native integer range"
                    )
                }
                scip.setIntParam("randomization/randomseedshift", seed.toInt())
            }
            if (scipConfiguration.deterministic == true) {
                scip.setBoolParam("randomization/permutevars", false)
                scip.setBoolParam("randomization/permuteconss", false)
            }
            scipConfiguration.nativeParameters.toSortedMap().forEach { (name, value) ->
                applyNativeParameter(name, value)
            }
            ok
        } catch (error: Throwable) {
            Failed(
                ErrorCode.OREngineModelingException,
                "SCIP 原生配置失败：${error.message ?: error::class.simpleName} / " +
                    "SCIP native configuration failed: ${error.message ?: error::class.simpleName}"
            )
        }
    }

    private fun applyNativeParameter(name: String, value: BackendParameterValue) {
        if (name.isBlank()) {
            throw IllegalArgumentException("SCIP native parameter name must not be blank")
        }
        when (value) {
            is BackendParameterValue.BooleanValue -> scip.setBoolParam(name, value.value)
            is BackendParameterValue.Decimal -> {
                val decimal = value.value.toDoubleOrNull()
                    ?: throw IllegalArgumentException("SCIP real parameter '$name' is not a decimal")
                if (!decimal.isFinite()) {
                    throw IllegalArgumentException("SCIP real parameter '$name' must be finite")
                }
                scip.setRealParam(name, decimal)
            }
            is BackendParameterValue.Text -> scip.setStringParam(name, value.value)
            is BackendParameterValue.Integer -> {
                if (value.value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
                    try {
                        scip.setIntParam(name, value.value.toInt())
                    } catch (_: Throwable) {
                        scip.setLongintParam(name, value.value)
                    }
                } else {
                    scip.setLongintParam(name, value.value)
                }
            }
        }
    }

    /**
     * 初始化 SCIP 求解器 / Initialize SCIP solver
     *
     * @param name 模型名称 / model name
     * @return 操作结果 / operation result
    */
    protected suspend fun init(name: String): Try {
        if (!loadedLibrary) {
            if (!loadConfiguredLibrary()) {
                return Failed(ErrorCode.SolverNotFound, "failed to load jscip library")
            }
        }
        scip = Scip()
        scip.create(name)
        return ok
    }

    /**
     * 执行 SCIP 求解 / Execute SCIP solving
     *
     * @param threadNum 线程数 / number of threads
     * @return 操作结果 / operation result
    */
    protected suspend fun solve(threadNum: UInt64): Try {
        val begin = Clock.System.now()
        if (threadNum gr UInt64.one) {
            scip.solveConcurrent()
            val stage = scip.stage
            if (stage.swigValue() < SCIP_Stage.SCIP_STAGE_INITPRESOLVE.swigValue()) {
                scip.solve()
            }
        } else {
            scip.solve()
        }
        solvingTime = Clock.System.now() - begin

        return ok
    }

    /**
     * 分析 SCIP 求解状态 / Analyze SCIP solving status
     *
     * @return 操作结果 / Operation result
    */
    protected suspend fun analyzeStatus(cancellationToken: CancellationToken? = null): Try {
        val solution = scip.bestSol
        terminationReason = ScipConstraintProgrammingStatusMapper.terminationReason(
            status = scip.status,
            cancellationRequested = cancellationToken?.isCancellationRequested == true
        )
        status = when (scip.status) {
            SCIP_Status.SCIP_STATUS_OPTIMAL -> {
                SolverStatus.Optimal
            }

            SCIP_Status.SCIP_STATUS_INFEASIBLE -> {
                SolverStatus.Infeasible
            }

            SCIP_Status.SCIP_STATUS_UNBOUNDED -> {
                SolverStatus.Unbounded
            }

            SCIP_Status.SCIP_STATUS_INFORUNBD -> {
                SolverStatus.InfeasibleOrUnbounded
            }

            else -> {
                if (solution != null) {
                    SolverStatus.Feasible
                } else {
                    SolverStatus.SolvingException
                }
            }
        }
        return ok
    }
}


