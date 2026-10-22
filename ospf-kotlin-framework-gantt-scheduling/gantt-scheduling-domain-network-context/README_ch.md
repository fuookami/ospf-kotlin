# Gantt Network 上下文

[English](README.md)

将 Gantt 业务请求适配到公共 Network 路径内核的可选桥接模块。
使用 `ospf-kotlin-starter-gantt-network` 引入；普通 Gantt starter 不强制依赖
Network。本模块不依赖 Network application。

`NetworkBunchGenerationContext.generate` 接收业务 `PathProblem`、搜索预算、
当前 `ColumnGenerationScope` 和路径到任务束的转换器。转换器同时取得终止状态
与完整 trace，保留执行器、任务分配、内部活动时间及业务资源信息。主模型、
分支、固定/隐藏状态和最终排程仍归 Gantt 管理。

返回的 `ColumnGenerationBatch` 将候选列与完成度分离。转换拒绝、启发式支配
或搜索截断不能证明不存在改进列。可选的无改进证据必须来自完整搜索和当前
主问题贡献；不能从返回列的最小 reduced cost 推导全局下界。证据作用域必须
匹配当前模型、对偶、分支及执行器/时隙范围。精确定价不等于整数最优。

领域状态保留泛型物理量，数值转换集中在定价/solver 边界。嵌入流模型时可将
`FlowContext` 注册到 Gantt 模型，而不启动第二次 application 求解。

实际航空适配见 Demo4 的 `BunchGenerationContext` 与
`FlightTaskBunchGenerator`；`NetworkBunchGenerationContextTest` 验证任务束转换、
内部状态、截断及作用域/证据隔离。
