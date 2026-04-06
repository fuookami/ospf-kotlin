# OSPF Kotlin Multiarray Daily

日期: 2026-04-06  
交接目标: 下一个执行环境  
参考实现: `E:\workspace\ospf-rust\ospf-rust-multiarray`

## 1. 本轮审阅结论（Review Findings）

基线验证:
1. 已执行 `mvn -q test`，当前测试通过。
2. 现有测试覆盖不足，多个高风险问题未被触发。

关键问题清单（按严重级别排序）:
1. `Critical` `MultiIndexIterator` 违反 `Iterator` 契约，且复用同一 `IntArray` 导致已产出坐标被后续迭代篡改。  
   文件: `src/main/fuookami/ospf/kotlin/multiarray/AccessOrder.kt`（约 63-94 行）
2. `Critical` `Shape3/Shape4/DynShape` 的 `ColumnMajor vector(index)` 反解公式错误。  
   文件: `src/main/fuookami/ospf/kotlin/multiarray/Shape.kt`（约 513-519, 624-630, 745-755 行）
3. `High` `toStorageOrder` 仅换 shape 不重排数据，导致 `vector -> value` 语义错误。  
   文件: `src/main/fuookami/ospf/kotlin/multiarray/MultiArray.kt`（约 215-221 行）
4. `High` `BlockMultiArray.toMultiArray` 使用未初始化的 `MutableMultiArray(shape)`，存在运行时异常风险。  
   文件: `src/main/fuookami/ospf/kotlin/multiarray/BlockMultiArray.kt`（约 97-109 行）
5. `High` `MappedMultiArrayView` 缺少 map 向量合法性校验（重复映射、非连续映射、越界映射）。  
   文件: `src/main/fuookami/ospf/kotlin/multiarray/MultiArrayView.kt`（约 241-349 行）
6. `Medium` `accessOrder` 参数被忽略（接口行为与签名不一致）。  
   文件: `src/main/fuookami/ospf/kotlin/multiarray/AccessOrder.kt`（约 250-256, 297-323 行）
7. `Medium` `DynShape` 未防御性拷贝输入 `IntArray`，外部可变别名会导致 lazy 缓存与 shape 失真。  
   文件: `src/main/fuookami/ospf/kotlin/multiarray/Shape.kt`（约 644-705 行）
8. `Medium` `MultiArray/MutableMultiArray` 允许“半初始化对象”暴露（`ctor` 可空且未初始化内部 list）。  
   文件: `src/main/fuookami/ospf/kotlin/multiarray/MultiArray.kt`（约 12-23, 166-173, 270-277 行）
9. `Medium` `MultiArrayView`/`MappedMultiArrayView` 的向量索引入口缺少显式维度校验。  
   文件: `src/main/fuookami/ospf/kotlin/multiarray/MultiArrayView.kt`（约 107-109, 168-188, 301-303, 331-349 行）
10. `Performance` 热路径存在不必要分配与对象构造。  
    文件: `MultiArray.kt`, `MultiArrayView.kt`, `BlockMultiArray.kt`

---

## 2. 详细改进计划（Detailed Execution Plan）

### P0: 正确性修复（必须先做）

#### P0-1 修复 `MultiIndexIterator` 契约与别名问题
目标:
1. `hasNext()` 与 `next()` 一致，不出现 `hasNext=true` 后 `next` 抛异常（正常结束除外）。
2. 每次 `next()` 返回独立 `IntArray` 快照，避免后续迭代污染历史值。

实施要点:
1. 重写迭代状态机（建议保留 `current`，但在返回时 `copyOf()`）。
2. `next()` 到末尾时仅在真正“再次请求 next”时抛 `NoSuchElementException`。
3. 为 `RowMajor/ColumnMajor` 分别增加终止场景测试。

验收:
1. 新增测试覆盖 iterator 契约与快照独立性。
2. 全量测试通过。

---

#### P0-2 修复 `ColumnMajor vector(index)` 反解公式
目标:
1. `Shape3/Shape4/DynShape` 在列主序下满足 `index(vector(i)) == i` 全量恒成立。

实施要点:
1. 直接按 Rust 参考实现思路改为“逐维除 offset + 取余”稳定公式。  
   参考: `ospf-rust-multiarray/src/shape.rs`（`vector_of` 逻辑）
2. 补充 3D/4D/Dyn 的 `ColumnMajor` 逆变换测试（含随机点和全遍历小规模）。

验收:
1. 新增 inverse tests 全通过。
2. 旧测试不回归。

---

#### P0-3 修复 `toStorageOrder` 重排语义
目标:
1. 存储序转换后，任意同一向量索引访问值保持不变。

实施要点:
1. 参照 Rust：遍历旧线性索引 `i -> vector -> newIndex`，将值写入新位置。
2. 不改变对外 API 签名，仅修复语义。
3. 增加 `RowMajor <-> ColumnMajor` 双向 round-trip 测试。

验收:
1. `array.toStorageOrder(x).toStorageOrder(original)` 与原数组逐元素一致。

---

#### P0-4 修复 `BlockMultiArray.toMultiArray` 初始化问题
目标:
1. `toMultiArray(defaultValue)` 无未初始化异常，返回完整 dense 数组。

实施要点:
1. 将 `MutableMultiArray(shape)` 改为已初始化构造（如 `newWith` 或带 ctor）。
2. 增加 `toMultiArray` 回归测试（空块、部分块、全块）。

验收:
1. 覆盖运行并验证值正确。

---

#### P0-5 增加 `MappedMultiArrayView` 映射合法性校验
目标:
1. 对重复 map index、非连续占位、越界 index 明确 fail-fast。

实施要点:
1. 构造时校验：
   - `MapIndex.Map` 不可重复；
   - index 必须覆盖 `0..k-1` 连续区间；
   - index 不可越界。
2. 不合法输入抛出清晰异常（可新增专用异常类型）。
3. 参考 Rust 映射校验流程。  
   参考: `ospf-rust-multiarray/src/multi_array_view.rs`（`calculate_view_shape`）

验收:
1. 新增异常路径测试。
2. 合法转置/重映射路径保持可用。

---

### P1: API 一致性与健壮性

#### P1-1 处理被忽略的 `accessOrder` 参数
目标:
1. 接口签名与实际行为一致。

实施方案（二选一，建议 A）:
1. A: 真正实现 `MultiArrayView.iterWithOrder` 与 `fromList(accessOrder)` 的顺序语义。
2. B: 若暂不支持，移除参数并做兼容重载弃用迁移。

验收:
1. 新增顺序行为测试（RowMajor/ColumnMajor 差异可观察）。

---

#### P1-2 `DynShape` 防御性拷贝 + 输入校验
目标:
1. 禁止外部别名修改破坏内部缓存一致性。
2. 统一拒绝负维度，必要时增加 size 溢出保护。

实施要点:
1. `invoke(shape: IntArray)` 与 `withOrder(shape: IntArray, ...)` 中对输入 `copyOf()`。
2. 初始化阶段 `require(shape.all { it >= 0 })`。
3. 可选: 对 `totalSize` 做溢出检测（超界抛异常）。

---

#### P1-3 收紧“半初始化对象”暴露面
目标:
1. 避免外部直接构造出未初始化 `MultiArray`/`MutableMultiArray`。

实施要点:
1. 优先保留工厂入口（`new/newWith/newBy`），限制直接构造可见性或加 `require(ctor != null)`。
2. 保证内部调用路径兼容。

---

### P2: 性能优化（在正确性稳定后）

1. `MultiArray.init` 改 `MutableList(shape.size) { ... }`，减少中间集合。
2. `MappedMultiArrayView.mapVector` 预编译 `DummyIndexIterator`，避免每次 get 重建。
3. `BlockMultiArray` 避免频繁 `IntArray <-> List<Int>` 转换（可引入轻量 key 包装）。

---

## 3. 测试补全计划（Must Add Tests）

新增测试文件建议:
1. `AccessOrderIteratorContractTest.kt`
2. `ShapeColumnMajorInverseTest.kt`
3. `StorageOrderConversionTest.kt`
4. `BlockMultiArrayConversionTest.kt`
5. `MappedViewValidationTest.kt`

最低覆盖要求:
1. `index(vector(i)) == i`：3D/4D/Dyn，RowMajor+ColumnMajor。
2. iterator 契约：`hasNext/next` 边界严格正确。
3. `toStorageOrder` 语义保持：同向量同值。
4. map 校验异常路径全覆盖。

---

## 4. 下一个环境执行顺序（Handoff Runbook）

建议严格按顺序执行:
1. 做 P0-1 与 P0-2，先锁住最核心正确性。
2. 做 P0-3 与 P0-4，修复数据语义与运行时稳定性。
3. 做 P0-5，补齐映射安全边界。
4. 再进入 P1（接口一致性和健壮性）。
5. 最后做 P2 性能优化。

每完成一个子阶段执行:
1. `mvn -q test`
2. 若仅想快速回归模块可用：`mvn -q -Dtest=*Shape*,*MultiArray*,*View*,*Block* test`
3. 记录新增/修改测试及结果到本文件末尾“执行记录”。

---

## 5. 交接注意事项（Notes）

1. 当前问题中，`ColumnMajor vector(index)` 与 `toStorageOrder` 是最容易引入隐性数据错误的两处，优先级最高。
2. 不建议先做性能改动，先保证语义正确与测试覆盖。
3. 修复后建议用小规模穷举 shape（如 `2x3x4`）做逆变换对拍，再扩展到随机测试。

---

## 6. 执行记录（由下一个环境续写）

### 2026-04-06 Execution Session

**完成情况:**
- ✅ P0-1: MultiIndexIterator 返回独立快照，遵守 Iterator 契约
- ✅ P0-2: ColumnMajor vector(index) 使用逆序维度计算
- ✅ P0-3: toStorageOrder 正确重排数据
- ✅ P0-4: BlockMultiArray.toMultiArray 正确初始化
- ✅ P0-5: MappedMultiArrayView 校验映射合法性
- ✅ P1-2: DynShape 防御性拷贝输入数组
- ✅ P1-3: MultiArray 强制使用工厂方法初始化

**测试覆盖:**
- 新增测试文件: 5个
- 新增测试用例: 30+
- 全量测试通过: ✅

**下一步建议:**
- P1-1: 处理被忽略的 accessOrder 参数（需设计决策）
- P2: 性能优化（热路径优化）
