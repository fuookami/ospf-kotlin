package fuookami.ospf.kotlin.core.backend.plugins.copt

import kotlin.time.*
import copt.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

data object COPT {
    enum class Client(val key: String) {
        CaFile("CaFile"),
        CertFile("CertFile"),
        CertKeyFile("CertKeyFile"),
        Cluster("Cluster"),
        Floating("Floating"),
        Password("Password"),
        Port("Port"),
        Priority("Priority"),
        WaitTime("WaitTime"),
        WebServer("WebServer"),
        WebLicenseId("WebLicenseId"),
        WebAccessKey("WebAccessKey"),
        WebTokenDuration("WebTokenDuration"),
    }

    enum class Status(val value: Int) {
        Unstarted(0),
        Optimal(1),
        Infeasible(2),
        Unbounded(3),
        InfeasibleOrUnbounded(4),
        Numerical(5),
        NodeLimit(6),
        Imprecise(7),
        TimeOut(8),
        Unfinished(9),
        Interrupted(10),
    }
}

fun EnvrConfig.set(key: COPT.Client, value: String) {
    this.set(key.key, value)
}

abstract class CoptSolver {
    protected lateinit var env: Envr
    protected lateinit var coptModel: Model
    protected lateinit var status: SolverStatus

    protected fun finalize() {
        coptModel.dispose()
        env.dispose()
    }

    protected suspend fun init(
        server: String,
        port: UInt64,
        password: String,
        connectionTime: Duration,
        name: String,
        callBack: CreatingEnvironmentFunction? = null
    ): Try {
        return try {
            val config = EnvrConfig()
            config.set(COPT.Client.Cluster, server)
            config.set(COPT.Client.Port, port.toString())
            config.set(COPT.Client.Password, password)
            config.set(COPT.Client.WaitTime, connectionTime.toInt(DurationUnit.SECONDS).toString())
            when (val result = callBack?.invoke(config)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            val env = Envr(config)
            coptModel = env.createModel(name)
            ok
        } catch (e: CoptException) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost))
        }
    }

    protected suspend fun init(
        name: String,
        callBack: CreatingEnvironmentFunction? = null
    ): Try {
        return try {
            val config = EnvrConfig()
            when (val result = callBack?.invoke(config)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            env = Envr(config)
            coptModel = env.createModel(name)
            ok
        } catch (e: CoptException) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost))
        }
    }
}
