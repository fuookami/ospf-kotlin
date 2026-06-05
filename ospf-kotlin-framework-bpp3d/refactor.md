# BPP3D 形状泛型化与圆柱支持重构交接

日期：2026-05-31
最近更新：2026-06-05

本文档记录 BPP3D “形状泛型化 + 圆柱支持”重构的当前状态。总目标保持不变：在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，继续推进 BPP3D 从 cuboid-only 业务模型收敛到 shape-aware / generic shape 模型。

## 1. 已完成事项摘要

1. BPP3D 主链已从长方体基线推进到 shape-aware 主链，支持长方体、竖直圆柱和已知坐标终态路径下的 X/Z 横向圆柱。
2. packing 终态几何、renderer 输出、README、测试夹具和人工视觉确认已形成阶段性闭环。
3. application/generic 已知坐标输入路径已允许 X/Z 横向圆柱进入真实几何校验，默认候选生成路径仍保持竖直圆柱门禁。
4. 业务层主要 shape-aware 入口、圆柱轴向契约、unsupported contract 和边界脚本基线已完成阶段性收敛。
5. 旧长方体路径、旧 DTO、旧 CSV、Gurobi 普通回归和 CSV dataset suite 已保持兼容。
6. 本轮补齐了横向圆柱已知坐标生产验收的 explicit bins、multi-bin、renderer 输出、支撑和未开放路径防误用测试。

## 2. 总目标与当前边界

### 2.1 总目标

后续目标是在尽可能少的迭代内完成 BPP3D shape-aware / generic shape 生产契约的跨层收口。凡是新增开放的能力，必须同时具备几何、支撑、solver、renderer、文档和测试闭环；无法闭环的能力必须收敛为明确 unsupported contract，不能保留半开放状态。

### 2.2 当前保留边界

1. BPP3D 完全泛型化尚未完成；底层 placement/projection 体系仍允许保留必要 cuboid 结构性绑定。
2. X/Z 横向圆柱目前只允许在已知坐标终态 packing/rendering 路径表达和校验。
3. 默认自动候选生成、layer generation、circle packing、BLA、block loading、DFS/MLHS、stacking、hanging 尚未开放 X/Z 横向圆柱。
4. X/Z 横向圆柱不得复用 Y 轴 circle packing 平面假设作为候选生成或最终可行性证明。
5. depth boundary 仍是 application 后验硬校验，尚未下沉为 MILP 原生约束。
6. 连续半径优化不进入默认生产链路，除非先完成数据契约、建模契约和 Gurobi 回归。

## 3. 本轮执行结果

本轮实际完成范围集中在“横向圆柱已知坐标生产验收路径”和“未开放路径防误用”。没有新增默认候选生成能力，也没有修改 solver、CSV 生产解析、renderer DTO 字段或外部 renderer。

### 3.1 已完成

1. application/generic 测试覆盖显式 known-coordinate bins 输入、自动构造 known-coordinate bins 的多轴 X/Z 场景，以及同层混合横向圆柱轴向拒绝。
2. packing/rendering 测试覆盖跨 bin 的 X/Z 横向圆柱、Z 轴横向圆柱全长长方体支撑，以及既有局部支撑拒绝和 mixed shape 场景。
3. simple block generation 明确只允许 `Axis3.Y` 竖直圆柱，X/Z 横向圆柱都有 negative tests。
4. DFS/MLHS space-splitting 明确保持 cuboid-only，X/Y/Z 所有圆柱轴向都有 negative tests。
5. README / README_ch 增加 Shape 路径支持矩阵，统一 allowed path、unsupported path、BLA、depth boundary 和 renderer 口径。
6. 边界脚本基线重新验证通过；仓内 renderer DTO 测试在 BPP3D reactor 中通过。

### 3.2 保留与未触发

1. 本轮未修改生产逻辑；横向圆柱已知坐标能力复用既有 `Packer`、`PackingGeometryGuard` 和 `PackingRendererAdapter`。
2. layer generation、circle packing、BLA、block loading、DFS/MLHS、stacking、hanging 未开放 X/Z 横向圆柱自动能力。
3. depth boundary 未下沉到 MILP 原生约束。
4. 本轮未修改 renderer DTO、renderer fixture、packing renderer adapter 或显示语义，因此未触发外部 renderer 自动验收和人工视觉确认。

## 4. 本轮验证

已通过：

1. `generic-boundary-check.ps1`：`STRICT_GENERIC_BOUNDARY_PASS`
2. `shape-boundary-check.ps1`：`SHAPE_BOUNDARY_PASS`
3. `geometry-boundary-check.ps1`：`GEOMETRY_BOUNDARY_PASS`
4. `geometry-module-dry-run.ps1`：`GEOMETRY_MODULE_DRY_RUN_PASS`，warnings=8，internal baseline ok=8
5. application focused tests：15 tests passed
6. packing focused tests：21 tests passed
7. block-loading focused tests：6 tests passed
8. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true`：BPP3D reactor passed
9. Gurobi plugin install：reactor passed；Dokka 阶段存在 Kotlin metadata 版本噪声，但构建成功
10. Gurobi 普通回归：26 tests，0 failures，0 errors，1 skipped
11. Gurobi CSV dataset suite：26 tests，0 failures，0 errors，0 skipped

提交前复核：

1. `git diff --check -- ospf-kotlin-framework-bpp3d`：通过，仅有 CRLF 工作区提示。
2. BPP3D 提交隔离：仅提交 BPP3D 路径，不混入 quantities、CSP1D 或外部 renderer 改动。

## 5. 后续目标

1. 继续压缩业务层对底层 cuboid/placement/projection 的直接感知，把公开语义稳定到 item-domain / shape-domain API。
2. 对 layer generation、circle packing 和 BLA 做横向圆柱候选开放评估；无法证明的路径继续统一拒绝。
3. 对 block loading、DFS/MLHS、stacking 和 hanging 做完整决策，消除重复 guard，并维持清晰 unsupported contract。
4. 统一 layer assignment、CSV/Gurobi、dynamic radius/diameter、axis metadata 和 depth boundary policy 的 shape-aware 解释。
5. 评估 depth boundary MILP 原生下沉；若不下沉，记录不可下沉原因并补足防误用门禁。
6. 若后续触发 renderer DTO、fixture、adapter 或显示语义变化，必须同步外部 renderer 并重新执行自动与人工视觉验收。

## 6. 后续验收标准

1. 新增开放能力具备真实几何、支撑、solver、renderer、文档和测试闭环。
2. 保留 unsupported 的能力具备统一错误信息、negative tests 和门禁保护。
3. BPP3D 必跑门禁全部通过。
4. 被实际改动触发的完整验收全部执行并记录。
5. README、README_ch、refactor.md 与代码能力口径一致。
6. BPP3D 改动独立提交，非 BPP3D 改动和外部 renderer 改动不混入。
