package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_generation.service

//import fuookami.ospf.kotlin.utils.functional.Ok
//import fuookami.ospf.kotlin.utils.functional.Ret
//import fuookami.ospf.kotlin.utils.math.UInt64
//import kotlinx.datetime.Instant
//import kotlin.time.Duration
//
//open class TaskBunchGenerator<E : Executor, L : Label<E>>(
//    private val executor: E,
//    private val executorUsability: ExecutorUsability<E>,
//    private val graph: Graph,
//    private val connectionTimeCalculator: ConnectionTimeCalculator<E>,
//    private val minimumStartTimeCalculator: MinimumStartTimeCalculator<E>,
//    private val costCalculator: CostCalculator<E, L>,
//    private val totalCostCalculator: TotalCostCalculator<E>,
//    private val assignedTaskGenerator: AssignedTaskGenerator<E, L>? = null,
//    private val labelBuilderInitializer: LabelBuilderInitializer<E, L> = LabelBuilderInitializer(),
//    private val withOrderChange: Boolean = false,
//    private val maximumLabelPerNode: UInt64 = UInt64(10000UL),
//    private val maximumColumnGeneratedPerExecutor: UInt64 = UInt64(60UL)
//) {
//    companion object {
//        private fun sortNodes(graph: Graph): List<Node> {
//            val inDegree = HashMap<Node, UInt64>()
//            for ((_, node) in graph.nodes) {
//                inDegree[node] = UInt64.zero
//            }
//            for ((_, node) in graph.nodes) {
//                for (edge in graph[node]) {
//                    inDegree[edge.to] = (inDegree[edge.to] ?: UInt64.zero) + UInt64.one
//                }
//            }
//
//            // topological sort
//            val nodes = ArrayList<Node>()
//            while (inDegree.isNotEmpty()) {
//                val removeNeededNodes = inDegree.filterValues { it == UInt64.zero }
//                if (removeNeededNodes.isNotEmpty()) {
//                    for ((node, _) in removeNeededNodes) {
//                        for (edge in graph[node]) {
//                            inDegree[edge.to] = inDegree[edge.to]!! - UInt64.one
//                        }
//                        nodes.add(node)
//                        inDegree.remove(node)
//                    }
//                } else {
//                    val minInDegree = inDegree.values.min()
//                    val minInDegreeNodes = inDegree.filterValues { it == minInDegree }
//                    nodes.addAll(minInDegreeNodes.keys.toList().sortedBy { it.time })
//                    for ((node, _) in minInDegreeNodes) {
//                        for (edge in graph[node]) {
//                            inDegree[edge.to] = inDegree[edge.to]!! - UInt64.one
//                        }
//                        inDegree.remove(node)
//                    }
//                }
//            }
//            return nodes
//        }
//    }
//
//    private val enabledTime by executorUsability::enabledTime
//    private val nodes = if (!withOrderChange) {
//        sortNodes(graph)
//    } else {
//        emptyList()
//    }
//
//    operator fun invoke(iteration: UInt64, shadowPriceMap: ShadowPriceMap<E>): Ret<List<TaskBunch<E>>> {
//        val labels: LabelMap<L> = HashMap()
//        initRootLabel(labels, shadowPriceMap)
//
//        if (withOrderChange) {
//            val labelDeque = ArrayList<L>()
//            labelDeque.addAll(labels[graph[Node.root]!!]!!)
//
//            while (labelDeque.isNotEmpty()) {
//                val prevLabel = labelDeque.first()
//                labelDeque.removeFirst()
//                val prevNode = prevLabel.node
//                val edges = graph[prevNode].sortedBy { it.to.time }
//
//                for (edge in edges) {
//                    val succNode = edge.to
//                    val succLabels = getLabels(labels, succNode)
//                    if (succNode is EndNode) {
//                        if (prevNode !is RootNode) {
//                            val builder = labelBuilderInitializer(succNode, prevLabel)
//                            builder.shadowPrice += shadowPriceMap(prevLabel.task, null, executor)
//                            insertLabel(succLabels, builder())
//                        }
//                    } else if (!prevLabel.visited(succNode)) {
//                        val succLabel = generateTaskLabel(prevLabel, succNode, shadowPriceMap)
//                        if (succLabel != null) {
//                            insertLabel(succLabels, succLabel)
//                            labelDeque.add(succLabel)
//                        }
//                    }
//                }
//            }
//        } else {
//            for (prevNode in nodes) {
//                for (prevLabel in getLabels(labels, prevNode)) {
//                    for (edge in graph[prevNode]) {
//                        val succNode = edge.to
//                        val succLabels = getLabels(labels, succNode)
//
//                        if (succNode is EndNode) {
//                            if (prevNode !is RootNode) {
//                                val builder = labelBuilderInitializer(succNode, prevLabel)
//                                builder.shadowPrice += shadowPriceMap(prevLabel.task, null, executor)
//                                insertLabel(succLabels, builder())
//                            }
//                        } else if (!prevLabel.visited(succNode)) {
//                            val succLabel = generateTaskLabel(prevLabel, succNode, shadowPriceMap)
//                            if (succLabel != null) {
//                                insertLabel(succLabels, succLabel)
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return Ok(selectBunches(iteration, labels[EndNode]!!))
//    }
//
//    private fun initRootLabel(labels: LabelMap<L>, shadowPriceMap: ShadowPriceMap<E>) {
//        assert(labels.isEmpty())
//        val rootNode = graph[Node.root]!!
//        val builder = labelBuilderInitializer(rootNode, enabledTime)
//        builder.shadowPrice += shadowPriceMap(null, null, executor)
//        labels[rootNode] = mutableListOf(builder())
//    }
//
//    private fun getLabels(labels: LabelMap<L>, node: Node): MutableList<L> {
//        if (!labels.containsKey(node)) {
//            labels[node] = ArrayList()
//        }
//        return labels[node]!!
//    }
//
//    @Suppress("UNCHECKED_CAST")
//    protected open fun generateTaskLabel(prevLabel: L, succNode: Node, shadowPriceMap: ShadowPriceMap<E>): L? {
//        assert(succNode is TaskNode<*, *>)
//        succNode as TaskNode<E, *>
//
//        val succTask = succNode.task
//        val minStartTime = getMinStartTime(prevLabel, succNode)
//        val assignedTask = assignedTaskGenerator?.let { it(prevLabel, succNode, minStartTime, executor) }
//            ?: generateAssignedTask(succTask, minStartTime)
//            ?: return null
//
//        val cost = if (prevLabel.node is RootNode) {
//            costCalculator(executor, executorUsability.lastTask, null, assignedTask)
//        } else {
//            costCalculator(executor, prevLabel.task!!, prevLabel, assignedTask)
//        }
//        if (cost == null || !cost.valid) {
//            return null
//        }
//        val shadowPrice = if (prevLabel.node is RootNode) {
//            shadowPriceMap(executorUsability.lastTask, assignedTask, executor)
//        } else {
//            shadowPriceMap(prevLabel.task!!, assignedTask, executor)
//        }
//
//        val builder = labelBuilderInitializer(succNode, prevLabel, assignedTask)
//        builder.cost += cost
//        builder.shadowPrice += shadowPrice
//        return builder()
//    }
//
//    private fun generateAssignedTask(succTask: Task<E>, minStartTime: Instant): Task<E>? {
//        val duration = succTask.duration(executor)
//        val assignedTime = TimeRange(minStartTime, minStartTime + duration)
//        val time = if (succTask.time != null) {
//            if (succTask.time != assignedTime) {
//                assignedTime
//            } else {
//                null
//            }
//        } else if (succTask.timeWindow != null) {
//            if (succTask.timeWindow!!.contains(assignedTime)) {
//                assignedTime
//            } else {
//                null
//            }
//        } else if (succTask.expirationTime != null) {
//            if (assignedTime.end <= succTask.expirationTime!!) {
//                assignedTime
//            } else {
//                null
//            }
//        } else {
//            assignedTime
//        }
//
//        val executor: E? = if (succTask.executor == null || succTask.executor != executor) {
//            executor
//        } else {
//            null
//        }
//
//        val assignmentPolicy = AssignmentPolicy(
//            executor = executor,
//            time = time
//        )
//
//        return if (assignmentPolicy.empty) {
//            succTask
//        } else if (!succTask.assigningEnabled(assignmentPolicy)) {
//            null
//        } else {
//            succTask.assign(assignmentPolicy)
//        }
//    }
//
//    private fun insertLabel(labels: MutableList<L>, label: L) {
//        when (label.node) {
//            is TaskNode<*, *> -> {
//                if (labels.any { it ls label }) {
//                    return
//                }
//                labels.removeAll { label ls it }
//                if (labels.size > maximumLabelPerNode.toInt()) {
//                    for (i in maximumLabelPerNode.toInt() until labels.size) {
//                        labels.removeAt(i)
//                    }
//                }
//
//                for (i in labels.indices) {
//                    if (label.reducedCost ls labels[i].reducedCost) {
//                        labels.add(i, label)
//                        return
//                    }
//                }
//                // add to tail
//                labels.add(label)
//            }
//
//            is EndNode -> {
//                labels.add(label)
//            }
//
//            else -> {}
//        }
//    }
//
//    // extract task bunches of the best labels in end node
//    private fun selectBunches(iteration: UInt64, labels: List<L>): List<TaskBunch<E>> {
//        val bunches = ArrayList<TaskBunch<E>>()
//        val sortedLabels = labels.asIterable().filter { it.isBetterBunch }.sortedBy { it.reducedCost }
//        for (label in sortedLabels) {
//            val newBunch = label.generateBunch(iteration, executor, executorUsability, totalCostCalculator);
//            if (newBunch != null) {
//                bunches.add(newBunch)
//            }
//            if (bunches.size == maximumColumnGeneratedPerExecutor.toInt()) {
//                break
//            }
//        }
//        return bunches
//    }
//
//    // calculate the minimum departure time from previous label
//    // specially, it is the arrival time of last label if the previous label is belongs to root node or succ node is end node
//    // because there is no connection between virtual node and task node
//    @Suppress("UNCHECKED_CAST")
//    private fun getMinStartTime(prevLabel: L, succNode: Node): Instant {
//        assert(succNode is TaskNode<*, *>)
//        succNode as TaskNode<E, *>
//
//        val thisTask = succNode.task
//        return if (prevLabel.node is RootNode && executorUsability.lastTask == null) {
//            minimumStartTimeCalculator(prevLabel.completeTime, executor, thisTask, Duration.ZERO)
//        } else {
//            val prevPrevTask = if (prevLabel.node is RootNode) {
//                null
//            } else if (prevLabel.prevLabel!!.node is RootNode) {
//                executorUsability.lastTask
//            } else {
//                prevLabel.prevLabel.task!!
//            }
//            val prevTask = if (prevLabel.node is RootNode) {
//                executorUsability.lastTask!!
//            } else {
//                prevLabel.task!!
//            }
//            val connectionTime = connectionTimeCalculator(executor, prevPrevTask, prevTask, thisTask)
//            minimumStartTimeCalculator(prevLabel.completeTime, executor, thisTask, connectionTime)
//        }
//    }
//}