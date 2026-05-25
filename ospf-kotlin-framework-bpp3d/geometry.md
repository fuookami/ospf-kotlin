# BPP/CSP Geometry 重构交接计划

日期：2026-05-24

状态：待执行

## 目标

将 `bpp3d-infrastructure` 中坐标系、长方体、视图、投影、放置等内容拆分为“纯几何核”和“BPP3D 业务包装”两层。

同时为后续 `bpp2d` 和 `csp2d` 预留二维几何核，使二维装箱、二维下料和三维装箱共享一致的坐标、尺寸、放置、相交、包含、旋转建模方式。

几何核需要覆盖矩形/长方体和圆/圆柱体两类基础形状。圆柱体本轮先按主轴对齐模型处理，即圆柱轴线只能沿 `X`、`Y`、`Z` 之一，不处理任意倾斜圆柱。

最终目标不是把现有文件整体迁入 `math.geometry`，而是先提炼稳定、无业务语义的几何模型，再让 BPP3D / BPP2D / CSP2D 继续组合这些模型表达各自领域规则。

## 边界原则

### 可以上移或抽象的内容

- 三维坐标轴、二维/三维点、向量、矩形、长方体。
- 二维圆、三维圆柱体，以及它们的面积、体积、包围盒和投影。
- 轴对齐长方体的尺寸、体积、投影、包含、重叠、交集。
- 轴对齐圆柱体的半径、高度、轴向、体积、投影、包围盒、包含、重叠。
- 坐标平面和投影平面，例如 `XY`、`XZ`、`YZ`。
- 轴置换和方向映射，例如三维长方体旋转后的尺寸映射。
- 不引用 BPP3D 货物、容器、层、堆叠、数量统计的 `Placement2` / `Placement3` 几何核。
- 不引用 BPP2D / CSP2D 物品、板材、切割模式、余料策略的二维 `Rectangle2` / `Box2` / `Placement2` 几何核。

### 必须留在 BPP3D 的内容

- `Cuboid<T>` 作为可装载货物/箱体的领域接口。
- `CuboidView<T>` 中与 `unit`、`orientation`、`bottomSupport`、`enabledOrientations` 绑定的业务语义。
- `Projection<T, P>`、`PlaneProjection`、`PileProjection`、`MultiPileProjection` 中和货物、数量、重量、堆叠相关的逻辑。
- `Container2`、`Container3` 中的装载量、剩余空间、loading rate、amount 统计和可行性判断。
- solver adapter、领域统计、启发式规则和兼容 facade。

### 必须留在 BPP2D / CSP2D 的内容

- BPP2D 的 item、bin、packing、loading rate、数量统计、启发式规则。
- CSP2D 的 item、plate、cutting pattern、kerf、余料、切割顺序、切割可行性规则。
- 与业务对象绑定的 orientation 启用规则、放置策略、切割策略。
- solver adapter、领域统计、application/starter facade。

### 模块依赖约束

当前依赖方向是 `ospf-kotlin-quantities` 依赖 `ospf-kotlin-math`，而 `ospf-kotlin-math` 不依赖 `ospf-kotlin-quantities`。

因此：

- 无单位的纯几何类型可以考虑进入 `ospf-kotlin-math/src/main/.../math/geometry`。
- 带 `Quantity<V>` 的几何类型不能直接放入 `ospf-kotlin-math`，除非重构模块依赖。
- 带单位几何的推荐落点是新模块，或 `ospf-kotlin-quantities` 下的 geometry 子包。

## 二维/三维统一建模原则

- 二维和三维共享命名风格：`Axis2` / `Axis3`、`Point2` / `Point3`、`Vector2` / `Vector3`、`Placement2` / `Placement3`。
- 二维和三维共享形状抽象：`Shape2` / `Shape3` 只表达几何空间关系，不表达业务含义。
- 二维矩形和三维长方体共享尺寸语义：二维为 `width + height`，三维为 `width + height + depth`。
- 二维圆和三维圆柱体共享半径语义：二维为 `radius`，三维为 `radius + height + axis`。
- 带位置对象和纯尺寸对象分离：
  - `Rectangle2` / `Cuboid3` 只表达尺寸。
  - `Circle2` / `Cylinder3` 只表达半径、高度、轴向等形状尺寸。
  - `Box2` / `Box3` 表达 `position + size`。
- 圆形和圆柱体需要提供包围盒，用于和现有矩形/长方体装载流程桥接。
- 旋转和轴置换分离：
  - 二维用 `AxisPermutation2` 或等价模型表达横竖旋转。
  - 三维用 `AxisPermutation3` 表达六种主轴置换。
  - 圆在二维旋转后形状不变。
  - 圆柱体在三维旋转后只改变轴向，截面半径不变。
- 领域 orientation 不直接等于几何 permutation。BPP2D、CSP2D、BPP3D 可以包装同一个底层 permutation，但各自保留业务启用规则。
- 几何层只回答“形状之间的空间关系”，不回答“是否允许装载、是否允许切割、是否满足需求”。

## 事项

### G0：依赖与现状审计

- [ ] 审计 `bpp3d-infrastructure` 中所有几何相关类型和调用点。
- [ ] 确认 `math`、`quantities`、`bpp3d-infrastructure` 的依赖方向。
- [ ] 输出类型归类清单：纯几何、带单位几何、BPP3D 业务包装。
- [ ] 明确本轮是否新建带单位几何模块，或先在 BPP3D 内部完成抽象萃取。

### G1：提炼三维长方体与圆柱体纯几何模型

- [ ] 新增无业务语义的 `Cuboid3` / `Box3` / `AxisAlignedBox3` 概念。
- [ ] 新增无业务语义的 `Cylinder3` / `AxisAlignedCylinder3` 概念。
- [ ] 区分尺寸对象和带位置对象：
  - `Cuboid3`：只表达 `width`、`height`、`depth`。
  - `Cylinder3`：只表达 `radius`、`height`、`axis`。
  - `Box3` / `AxisAlignedBox3`：表达 `position + size`。
- [ ] 为 `Cylinder3` 提供圆柱轴线、圆形截面、体积、投影和包围盒。
- [ ] 提供体积、尺寸读取、坐标范围、包含、重叠、交集等纯几何操作。
- [ ] 保持 BPP3D `Cuboid<T>` 不直接消失，而是组合新的几何对象。
- [ ] 后续如果 BPP3D 支持圆柱货物，应新增业务 wrapper，而不是让 `Cylinder3` 持有货物、重量或装载规则。

### G2：提炼坐标轴、平面和方向置换

- [ ] 新增 `Axis3` 表达 `X`、`Y`、`Z`。
- [ ] 新增 `AxisPlane3` 或等价模型表达 `XY`、`XZ`、`YZ`。
- [ ] 新增 `AxisPermutation3` 表达三维尺寸映射。
- [ ] 将 BPP3D `Orientation` 改为包装或映射通用轴置换。
- [ ] 保留 BPP3D 的业务命名，例如 `Bottom`、`Side`、`Front`，但底层委托给通用平面。

### G3：剥离 View 的纯几何部分

- [ ] 从 `CuboidView<T>` 中提炼纯几何视图，例如 `Cuboid3View`。
- [ ] `Cuboid3View` 只包含原始长方体、轴置换、旋转后尺寸。
- [ ] BPP3D `CuboidView<T>` 保留 `unit`、业务 `Orientation`、支撑语义和兼容调用。
- [ ] 现有 `view(orientation)` 行为保持不变。

### G4：剥离 Projection 的纯几何部分

- [ ] 从 `ProjectivePlane` 中提炼纯坐标平面投影能力。
- [ ] 提供 `Point3 -> Point2` 和 `Point2 + distance -> Point3` 的通用转换。
- [ ] 提供长方体在指定平面上的 footprint / projection shape。
- [ ] BPP3D `Projection<T, P>` 保留 `unit`、`view`、`weight`、`amount` 和堆叠相关逻辑。
- [ ] 保证 `Bottom`、`Side`、`Front` 等现有调用方不需要一次性大规模改写。

### G5：剥离 Placement 的纯几何部分

- [ ] 新增无业务语义的二维/三维放置模型。
- [ ] 几何 `Placement2` 只包含二维 `Shape2` 和二维 position。
- [ ] 几何 `Placement3` 只包含三维 `Shape3` 和三维 position。
- [ ] 几何放置模型提供 `maxX/maxY/maxZ`、`contains`、`overlap`、`intersection`。
- [ ] `Placement2` 支持矩形与圆的基础关系判断。
- [ ] `Placement3` 支持长方体与圆柱体的基础关系判断。
- [ ] BPP3D 放置模型保留 `unit`、`view`、`parent`、`weight`、层关系和支撑关系。

### G6：迁移 BPP3D 包装层

- [ ] 让 `Cuboid<T>` 组合新的几何 shape。
- [ ] 让 `CuboidView<T>` 组合新的几何 view。
- [ ] 让 `Projection<T, P>` 组合新的几何 projection。
- [ ] 让 `Placement2/3<T>` 组合新的几何 placement。
- [ ] 保持 public/domain API 行为兼容。
- [ ] 避免在领域层继续直接复制底层几何计算。

### G7：补充二维几何核规划

- [ ] 新增二维坐标轴模型，例如 `Axis2.X`、`Axis2.Y`。
- [ ] 新增二维尺寸模型，例如 `Rectangle2`。
- [ ] 新增二维圆形模型，例如 `Circle2`。
- [ ] 新增二维带位置模型，例如 `Box2` / `AxisAlignedBox2`。
- [ ] 新增二维旋转或轴置换模型，例如 `AxisPermutation2`。
- [ ] 新增二维放置模型，例如纯几何 `Placement2`。
- [ ] 提供二维 `contains`、`overlap`、`intersection`、`area`、坐标范围等通用操作。
- [ ] 提供圆的面积、直径、包围盒、圆圆关系、圆矩形关系。
- [ ] 明确 BPP2D wrapper 和 CSP2D wrapper 只组合二维几何核，不把业务规则下沉到几何层。

### G8：补充圆和圆柱体支持

- [ ] 新增二维 `Circle2`，包含 `radius`、`diameter`、`area`、`boundingBox`。
- [ ] 新增三维 `Cylinder3`，包含 `radius`、`height`、`axis`、`volume`、`boundingBox`。
- [ ] 定义圆柱体在 `XY`、`XZ`、`YZ` 平面上的投影规则。
- [ ] 定义圆柱体经过 `AxisPermutation3` 后的轴向变化规则。
- [ ] 定义圆、矩形、圆柱体、长方体之间的最小可用空间关系判断。
- [ ] 明确任意角度旋转圆柱、斜圆柱、曲面精确布尔运算不属于本轮目标。

### G9：测试与文档收口

- [ ] 为几何核补充独立单元测试。
- [ ] 为 BPP3D wrapper 补充兼容回归测试。
- [ ] 为未来 BPP2D / CSP2D wrapper 补充规划测试清单。
- [ ] 补充 Flt64 与 FltX 两类数值路径测试。
- [ ] 更新残留审计文档，说明哪些类型仍留在 BPP3D 的原因。
- [ ] 更新 `daily.md` 或后续交接文件，记录本轮完成状态。

## 详细步骤

### Step 1：建立迁移清单

1. 搜索并列出以下类型及调用点：
   - `Cuboid`
   - `CuboidView`
   - `ProjectivePlane`
   - `Projection`
   - `Placement2`
   - `Placement3`
   - `Container2Shape`
   - `Container3Shape`
2. 将每个成员按以下分类标注：
   - 纯几何。
   - 带单位几何。
   - BPP3D 业务语义。
   - 兼容层或 solver 边界。
3. 形成迁移顺序，优先迁移无业务语义且调用面小的 helper。

### Step 2：先在 BPP3D 内部抽象几何核

1. 在 `bpp3d-infrastructure` 内新增内部几何核包。
2. 先不移动到 `math.geometry`，避免一次性打破模块依赖。
3. 将纯几何计算迁入新类型。
4. 用 wrapper 保持原有 API 兼容。
5. 跑定向测试确认行为不变。

### Step 3：决定通用几何最终落点

1. 如果类型不依赖 `Quantity<V>`，迁入 `ospf-kotlin-math/.../math/geometry`。
2. 如果类型依赖 `Quantity<V>`，优先考虑：
   - 新增独立 quantity-geometry 模块。
   - 或放入 `ospf-kotlin-quantities` 的 geometry 子包。
3. 禁止让 `ospf-kotlin-math` 直接依赖 `ospf-kotlin-quantities`，除非本轮明确包含模块依赖重构。

### Step 4：替换 BPP3D 调用

1. `Cuboid<T>` 改为委托几何 shape 计算尺寸和体积。
2. `CuboidView<T>` 改为委托几何 view 计算旋转后尺寸。
3. `ProjectivePlane` 改为委托通用 plane 做点和平面转换。
4. `Placement2/3<T>` 改为委托几何 placement 做包含、重叠和交集。
5. 只保留业务判断在 BPP3D wrapper 中。

### Step 5：清理重复实现

1. 删除或收敛 BPP3D 中重复的坐标比较、范围判断、投影计算。
2. 确认业务 wrapper 不再重复实现几何核已经提供的逻辑。
3. 对保留在 BPP3D 的逻辑写明原因。

### Step 6：验收与审计

1. 执行 infrastructure 定向测试。
2. 执行 BPP3D 聚合编译。
3. 执行 starter 和 example 编译。
4. 执行静态审计，确认没有引入循环依赖或反向依赖。
5. 更新交接文档和残留说明。

### Step 7：为 BPP2D / CSP2D 预留二维落地路径

1. 在通用几何核中补齐二维基础对象：
   - `Axis2`
   - `Point2`
   - `Vector2`
   - `Rectangle2`
   - `Circle2`
   - `Box2`
   - `AxisPermutation2`
   - `Placement2`
2. 明确 BPP2D 未来 wrapper 形态：
   - `Bpp2dItem` / `Bpp2dBin` 组合 `Rectangle2`、`Circle2` 或 `Box2`。
   - `Bpp2dPlacement` 组合几何 `Placement2`。
   - 旋转启用规则留在 BPP2D。
3. 明确 CSP2D 未来 wrapper 形态：
   - `Csp2dItem` / `Csp2dPlate` 组合 `Rectangle2`、`Circle2` 或 `Box2`。
   - 切割 pattern、kerf、余料和切割顺序留在 CSP2D。
   - 几何层只提供矩形空间关系和分割后的区域表达。
4. 对二维和三维共用的操作使用一致测试命名，避免 BPP2D / CSP2D 后续重复实现。

### Step 8：为圆柱体预留 BPP3D 落地路径

1. 在通用几何核中补齐三维圆柱对象：
   - `Cylinder3`
   - `AxisAlignedCylinder3`
   - `Cylinder3View`
2. 明确圆柱体只支持主轴对齐：
   - 轴向为 `X` 时，高度沿 `X`。
   - 轴向为 `Y` 时，高度沿 `Y`。
   - 轴向为 `Z` 时，高度沿 `Z`。
3. 明确 BPP3D 未来 wrapper 形态：
   - 圆柱货物 wrapper 组合 `Cylinder3`。
   - 圆柱放置 wrapper 组合几何 `Placement3`。
   - 重量、可用方向、堆叠支撑和装载规则留在 BPP3D。
4. 圆柱体与长方体混装时，几何层只提供基础空间关系；装载策略和可行性规则由 BPP3D 决定。

## 修改清单

### 预计新增

- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/Shape2.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/Shape3.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/Axis2.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/Rectangle2.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/Circle2.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/Box2.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/AxisPermutation2.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/Cuboid3.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/Cylinder3.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/Box3.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/Axis3.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/AxisPlane3.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/AxisPermutation3.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/Cuboid3View.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/Cylinder3View.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/Placement2.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../geometry/Placement3.kt`

如果确认最终迁入通用模块，则对应文件应移动到以下之一：

- `ospf-kotlin-math/src/main/.../math/geometry`
- `ospf-kotlin-quantities/src/main/.../physics/quantity/geometry`
- 新增 quantity-geometry 模块

### 预计修改

- `bpp3d-infrastructure/src/main/.../Cuboid.kt`
- `bpp3d-infrastructure/src/main/.../Orientation.kt`
- `bpp3d-infrastructure/src/main/.../Projection.kt`
- `bpp3d-infrastructure/src/main/.../Placement.kt`
- `bpp3d-infrastructure/src/main/.../Container.kt`
- `bpp3d-infrastructure/src/main/.../api/QuantityInfrastructureApi.kt`

### 预计新增或修改测试

- `bpp3d-infrastructure/src/test/.../Rectangle2GeometryTest.kt`
- `bpp3d-infrastructure/src/test/.../Circle2GeometryTest.kt`
- `bpp3d-infrastructure/src/test/.../Box2GeometryTest.kt`
- `bpp3d-infrastructure/src/test/.../AxisPermutation2Test.kt`
- `bpp3d-infrastructure/src/test/.../CuboidGeometryTest.kt`
- `bpp3d-infrastructure/src/test/.../CylinderGeometryTest.kt`
- `bpp3d-infrastructure/src/test/.../AxisPermutationTest.kt`
- `bpp3d-infrastructure/src/test/.../ProjectionGeometryTest.kt`
- `bpp3d-infrastructure/src/test/.../PlacementGeometryTest.kt`
- `bpp3d-infrastructure/src/test/.../OrientationTest.kt`
- `bpp3d-domain-layer-assignment-context/src/test/.../FltXDirectCompileProofTest.kt`

### 测试用例描述（交接执行版）

下个会话实现几何核时，优先按以下用例落测试；每组测试都应至少覆盖 `Flt64` 与一个 `FltX` 路径，除非该类型明确是无单位、无数值泛型的纯枚举或置换模型。

1. `AxisPermutation2Test`
   - `rotateRectangleShouldSwapWidthAndHeight`：二维矩形横竖旋转后 `width/height` 对调，面积不变。
   - `identityPermutationShouldKeepRectangleSize`：恒等置换不改变尺寸和坐标范围。
   - `circleShouldRemainSameAfter2DRotation`：圆旋转后半径、直径、面积和包围盒不变。

2. `AxisPermutation3Test`
   - `sixCuboidPermutationsShouldMapDimensionsExactly`：三维长方体六种主轴置换分别映射到期望的 `width/height/depth`。
   - `permutationInverseShouldRestoreCuboidSize`：任意置换接反置换后恢复原尺寸。
   - `cylinderAxisShouldFollowPermutation`：圆柱体轴向随置换从 `X/Y/Z` 正确迁移，半径不变，高度沿新轴。

3. `Rectangle2GeometryTest`
   - `rectangleAreaShouldUseWidthTimesHeight`：矩形面积维度和数值正确。
   - `rectangleRangeShouldExposeMinAndMaxCoordinates`：带位置矩形的 `minX/maxX/minY/maxY` 正确。
   - `rectangleContainsPointShouldRespectBoundaryMode`：点包含测试覆盖闭区间、开区间和边界点。
   - `rectangleOverlapAndIntersectionShouldHandleTouchingEdges`：相交、仅接边、完全分离三种情况结果明确。

4. `Circle2GeometryTest`
   - `circleAreaAndDiameterShouldUseRadius`：圆的直径、面积和单位维度正确。
   - `circleBoundingBoxShouldMatchDiameter`：圆的包围盒宽高均为直径，中心位置正确。
   - `circleContainsPointShouldHandleCenterBoundaryAndOutside`：中心点、圆周点、外部点覆盖。
   - `circleCircleRelationShouldCoverOverlapTouchAndSeparate`：圆圆重叠、外切、内含、分离覆盖。
   - `circleRectangleRelationShouldUseBoundingOrExactPolicyExplicitly`：圆矩关系必须明确是包围盒桥接还是精确几何判断，并据此断言。

5. `Box2GeometryTest`
   - `box2ShouldCombinePositionAndShape`：二维带位置对象组合 `Point2 + Rectangle2/Circle2` 后坐标范围正确。
   - `box2ContainsShouldDelegateToShapeAndPosition`：包含逻辑由 shape 和 position 共同决定。
   - `box2IntersectionShouldReturnExpectedRectangleForRectangles`：矩形盒交集返回正确尺寸和位置。

6. `CuboidGeometryTest`
   - `cuboidVolumeShouldUseWidthHeightDepth`：长方体体积维度和数值正确。
   - `box3RangeShouldExposeAllAxisBounds`：三维带位置盒的 `min/max X/Y/Z` 正确。
   - `box3ContainsPointShouldRespectBoundaryMode`：三维点包含覆盖边界和外部点。
   - `box3OverlapAndIntersectionShouldHandleTouchingFaces`：重叠、仅贴面、仅贴边、完全分离结果明确。

7. `CylinderGeometryTest`
   - `cylinderVolumeShouldUseBaseAreaTimesHeight`：圆柱体体积维度和数值正确。
   - `axisAlignedCylinderBoundingBoxShouldDependOnAxis`：轴向为 `X/Y/Z` 时包围盒尺寸分别正确。
   - `cylinderProjectionShouldReturnCircleOrRectangleByPlane`：投影到垂直于轴的平面为圆，投影到含轴平面为矩形。
   - `cylinderCuboidRelationShouldUseBoundingOrExactPolicyExplicitly`：圆柱与长方体关系必须明确是包围盒桥接还是精确几何判断，并据此断言。

8. `ProjectionGeometryTest`
   - `planeShouldConvertPoint3ToPoint2AndBack`：`XY/XZ/YZ` 平面完成 `Point3 -> Point2 -> Point3` 往返，保留指定距离坐标。
   - `cuboidFootprintShouldMatchPlaneDimensions`：长方体在三个平面的 footprint 尺寸正确。
   - `bpp3dBottomSideFrontShouldDelegateToAxisPlane`：BPP3D `Bottom/Side/Front` 的现有结果与几何平面委托结果一致。

9. `PlacementGeometryTest`
   - `placement2ShouldReportMaxCoordinates`：二维放置的最大坐标由 position 和 shape 决定。
   - `placement3ShouldReportMaxCoordinates`：三维放置的最大坐标由 position 和 shape 决定。
   - `placementContainsShouldWorkForRectangleCircleCuboidCylinder`：矩形、圆、长方体、圆柱体放置均覆盖包含判断。
   - `placementOverlapShouldNotDependOnBusinessUnit`：几何放置重叠测试不引用 `unit`、`weight`、`amount`、`bottomSupport` 等业务字段。

10. `Bpp3dGeometryWrapperCompatibilityTest`
    - `cuboidViewShouldKeepExistingOrientationBehavior`：`CuboidView<T>` 的 `unit`、`orientation`、旋转后尺寸保持现有行为。
    - `projectionShouldKeepWeightAndAmountAggregation`：BPP3D `Projection<T, P>` 保留重量、数量、堆叠统计，不下沉到几何核。
    - `placementShouldKeepParentAndSupportSemantics`：BPP3D `Placement2/3<T>` 保留 parent、层关系、支撑语义。
    - `solverBoundaryShouldRemainUnchanged`：layer-assignment solver adapter 的 `Flt64` 边界不因几何核拆分扩散。

11. 架构审计测试或脚本
    - `geometryPackageShouldNotImportBpp3dDomain`：通用几何目录不得引用 `bpp3d`、`bpp2d`、`csp2d` 包。
    - `geometryPackageShouldNotContainBusinessTerms`：通用几何目录不得出现 `weight`、`amount`、`loadingRate`、`bottomSupport`、`kerf`、`cuttingPattern` 等业务术语。
    - `mathModuleShouldNotDependOnQuantitiesUnlessExplicitlyApproved`：若几何核进入 `math`，确认 `ospf-kotlin-math/pom.xml` 未新增 `ospf-kotlin-quantities` 依赖。

### 后续 BPP2D / CSP2D 预计新增

当前 `ospf-kotlin-framework-bpp2d` 和 `ospf-kotlin-framework-csp2d` 尚未展开源码目录。后续创建源码时，建议直接按以下 wrapper 边界落地：

- `ospf-kotlin-framework-bpp2d/.../Bpp2dRectangleItem.kt`
- `ospf-kotlin-framework-bpp2d/.../Bpp2dBin.kt`
- `ospf-kotlin-framework-bpp2d/.../Bpp2dPlacement.kt`
- `ospf-kotlin-framework-csp2d/.../Csp2dRectangleItem.kt`
- `ospf-kotlin-framework-csp2d/.../Csp2dPlate.kt`
- `ospf-kotlin-framework-csp2d/.../Csp2dCuttingPattern.kt`
- `ospf-kotlin-framework-csp2d/.../Csp2dPlacement.kt`

### 不应修改或只做兼容验证

- solver adapter 的数值边界。
- application/starter 的 public facade，除非需要补充兼容入口。
- 领域统计逻辑，除非它直接依赖被迁移的 placement 几何计算。

## 验收标准

### 编译验收

- [ ] `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -am -DskipTests compile` 通过。
- [ ] `mvn -pl ospf-kotlin-starters/ospf-kotlin-starter-bpp3d -am -DskipTests compile` 通过。
- [ ] `mvn -pl ospf-kotlin-example -am -DskipTests compile` 通过。

### 行为验收

- [ ] 原有 `Cuboid`、`Orientation`、`Projection`、`Placement` 行为测试通过。
- [ ] 新增几何核测试覆盖尺寸映射、投影、包含、重叠、交集。
- [ ] 二维几何核测试覆盖矩形旋转、包含、重叠、交集、面积和坐标范围。
- [ ] 圆形几何核测试覆盖面积、直径、包围盒、圆圆关系、圆矩形关系。
- [ ] 圆柱体几何核测试覆盖体积、轴向变换、包围盒、投影、圆柱体与长方体基础关系。
- [ ] BPP3D wrapper 测试确认业务语义未被迁入通用几何层。
- [ ] BPP2D / CSP2D 规划文档确认未来 wrapper 只组合二维几何核。
- [ ] Flt64 与 FltX 路径均有测试覆盖。

### 架构验收

- [ ] 通用几何层不引用 `bpp3d` 包。
- [ ] 通用几何层不引用 `bpp2d` 或 `csp2d` 包。
- [ ] 通用几何层不包含 `unit`、`weight`、`amount`、`loadingRate`、`bottomSupport` 等 BPP3D 业务术语。
- [ ] 通用几何层不包含 `bin`、`plate`、`kerf`、`cuttingPattern`、`packingRate` 等 BPP2D / CSP2D 业务术语。
- [ ] `ospf-kotlin-math` 不直接依赖 `ospf-kotlin-quantities`，除非本轮明确完成模块依赖重构并记录原因。
- [ ] 带 `Quantity<V>` 的几何类型有明确归属，且不会制造循环依赖。
- [ ] BPP3D wrapper 只保留业务语义和兼容入口。
- [ ] BPP2D / CSP2D wrapper 预留方案只保留业务语义和兼容入口。
- [ ] 圆柱体仅按主轴对齐几何处理；任意倾斜圆柱如需支持，必须单独立项。

### 静态审计

- [ ] `rg "package .*bpp3d" ospf-kotlin-math/src/main` 无新增命中。
- [ ] `rg "package .*bpp2d|package .*csp2d" ospf-kotlin-math/src/main` 无新增命中。
- [ ] `rg "ospf-kotlin-quantities" ospf-kotlin-math/pom.xml` 无新增命中，除非已记录模块依赖重构。
- [ ] `rg "bottomSupport|loadingRate|amount|enabledOrientations" <通用几何目录>` 无命中。
- [ ] `rg "kerf|cuttingPattern|plate|bin|packingRate" <通用几何目录>` 无命中。
- [ ] `rg "toLegacy\\(" ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/test` 无新增命中。

### 交接验收

- [ ] 本文件对应事项已更新勾选状态。
- [ ] 若存在未迁移项，已记录未迁移原因和后续处理方式。
- [ ] 当前工作区已提交，或记录本轮基线 commit。
