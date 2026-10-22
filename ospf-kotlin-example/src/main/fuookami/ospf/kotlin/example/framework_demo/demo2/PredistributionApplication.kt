package fuookami.ospf.kotlin.example.framework_demo.demo2

import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 装载应用入口，只编排公共领域注册及求解策略。 / Loading entry delegating domain registration and solve strategy. */
class PredistributionApplication {
    /** 执行请求。 / Execute a request.
     * @param request 显式输入 / Explicit input
     * @param runningHeartBeatCallBack 运行回调 / Running callback
     * @param finnishHeartBeatCallBack 完成回调 / Completion callback
     * @param withRender 是否生成渲染 / Whether to render
     * @return 响应和渲染 / Response and rendering
     */
    suspend operator fun invoke(
        request: RequestDTO,
        runningHeartBeatCallBack: ((RunningHeartBeatDTO) -> Try)? = null,
        finnishHeartBeatCallBack: ((FinnishHeartBeatDTO) -> Unit)? = null,
        withRender: Boolean = false
    ): Pair<ResponseDTO, RenderDTO?> = Demo2ApplicationService.solve(
        request = request,
        mode = StowageMode.Predistribution,
        running = runningHeartBeatCallBack,
        finished = finnishHeartBeatCallBack,
        withRender = withRender
    )
}
