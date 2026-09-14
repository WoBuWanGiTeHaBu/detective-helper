# 推演录 · 前端长期约定（frontend/）

> 只在跨会话仍然成立的事写这里；逐轮过程见 `YYYY-MM-DD.md`。

## 数据与接口

- **一切业务数据走后端 REST，不用 localStorage 存数据。** localStorage 只允许存「界面偏好」
  （工具栏位置 `wb.toolbar.pos`、列宽等）。
- 图类资源的接口一律**同构**：`GET/POST /books/{bookId}/{res}`、`GET/PUT/DELETE /{res}/{id}`、
  `PUT /{res}/{id}/data` 整体覆盖保存。已有 `relation-graphs`，`family-trees` 照抄（见 `docs/后端接口需求.md` 第六节）。
- **详情接口的归一必须放在 API 边界**（`src/api/*.ts` 里做 `normalize*Data`）：
  后端把 `data` 当 JSON **字符串**返回、解析结果平铺在顶层，所以规则是
  「**优先顶层数组，缺失再解析字符串**」。漏了这一步就会出现「新建时正常、重新打开是空图」。
- 后端未实现的接口要在 `docs/后端接口需求.md` 写清接口表 + 载荷 + curl 验证 + 可直接发出去的 prompt。

## 画布几何（最容易踩坑）

- 画布坐标与 `.stage` 内的相对坐标**恒等 1:1**，节点/连线/注解/时间线都用绝对坐标，不做换算。
- 无限画布用 `originX/originY`（≤ 0）表示左上方向：图层 SVG 同时设
  `viewBox="{origin} {origin+size}"` **和** `style.left/top = origin`，两者必须同量偏移否则整体错位。
- 背景纹理要跟着 pan 走：pattern 的 `x/y` 取 `pan % tile`。
- **「移动画布」与「移动内容」是两个独立工具，必须互斥**：前者只动视口（`moveMode`），
  后者平移页内对象/注解/时间线的真实坐标并落盘（`activeTool === 'moveContent'`）。
  不要再把两者塞进同一个按钮。

## 三类节点的视觉语言

| | 关系图（抽屉 / 独立页） | 画布节点 |
| --- | --- | --- |
| 人物 | **圆形 + 名字在里面**（`personRadius` 自适应） | 胶囊（要显示职业·年龄等副信息） |
| 事件 | 方框 | 方框 |
| 事物 | 菱形 | 菱形 |

筛选用「人物↔人物」「事物↔事物」时，要**真正从节点集合过滤并重排**，不能只过滤列表。

## 库内已踩过的坑（改这些文件前先看一眼）

- **族谱代数必须按「配偶单元」松弛**，不能按个人 —— 外来配偶（父母未录入）会把
  后面几代的子女排到上面去、连线向上折返。（详见 2026-09-13.md 第六轮）
- **受控输入不要绑 computed(props)**：组件有高频变化的本地状态时会被重渲染拍回旧值，
  表现为「打字即清空」。用本地 draft ref。
- **同一文件的多处修改必须串行 Edit**，并行发多个 Edit 只有最后一个生效。
- 关系线法线要按**无向基准方向**算（`normalFlip`），否则 A→B 与 B→A 的偏移互相抵消。
- 抽屉/面板高度要 `min(容器 clientHeight)`，否则底部按钮会被顶出屏幕。

## 本地校验习惯

- 改完先 `node_modules/typescript/bin/tsc --noEmit -p tsconfig.json`，再 `vite build`。
- 纯算法（排布类）先写 node 用例验证，用 `node_modules/esbuild/bin/esbuild <file> --bundle --format=esm` 打包后跑。
- 视觉校验：本机 **Edge headless + CDP**（`--remote-debugging-port`，node 原生 WebSocket 手写驱动）
  截图；`agent-browser` 不在 PATH。Edge 必须作为**常驻后台任务**启动，用完先杀进程再删 profile 目录。
- 临时校验产物（预览路由/页面、截图目录）**交付前必须删干净**。
