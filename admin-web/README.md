# 碎片商城管理端（D01 + D08）

## 当前完成
- ✅ D01：基线战情室页面（里程碑 + 文档复核 + API/MySQL 映射）
- ✅ D02：工程初始化底座
  - `src/api/http.ts`：request SDK + 鉴权拦截器 + 错误处理 + X-Request-Id
  - `src/api/endpoints.ts`：156 条接口常量（由规则文档自动生成）
  - `src/components/AppPagination.vue`：统一分页组件
- ✅ D08：管理端主框架 + RBAC 基础模块
  - 登录页：`/login`
  - 后台框架：左侧菜单 + 顶部栏 + 路由守卫
  - 业务模块：`/dashboard`、`/users`、`/products`、`/orders`、`/points`
  - 权限模块：`/admin-users`、`/roles`、`/permissions`
  - 联调策略：全量直连真实 `/api/v1/admin/**`（已移除 Mock 回退）
- ✅ D10：分类与推荐位
  - 分类模块：`/categories`（分类列表、增改删、启停、排序）
  - 推荐模块：`/recommend-slots`（推荐位增改、启停、推荐项管理、推荐项排序）
- ✅ D11：商品中心扩展
  - 商品SPU：`/products`（搜索筛选、新增编辑、上下架、推荐开关）
  - SKU管理：`/products` 内弹窗（新增编辑、库存快速调整）
  - 媒体管理：`/products` 内弹窗（媒体新增/删除）
  - 属性定义：`/product-attrs`（属性定义增改删）
- ✅ D12：订单中心流转
  - 订单查询：`/orders`（按订单号/用户/状态查询）
  - 状态流转：审核通过、审核驳回、发货、完成、关闭
  - 订单备注：备注编辑保存
  - 订单详情：明细、物流、状态流转记录
- ✅ D13：背包资产与群资源
  - 背包资产：`/backpack-assets`（资产查询、人工发放、失效、过期、流水查看）
  - 群资源：`/group-resources`（群二维码资源增改删、启停）
- ✅ D14：平台能力
  - 字典中心：`/dict-center`（字典类型与字典项双栏管理）
  - 系统配置：`/system-configs`（配置增改删）
  - 文件中心：`/file-center`（上传、查询、删除）
- ✅ D15：监控审计
  - 仪表盘：`/dashboard`（用户/订单/商品/转化统计，日志与事件监控预览）
  - 操作日志：`/operation-logs`（查询、详情）
  - 事件外发表：`/outbox-events`（查询、事件重试）

## 关键文件
- `scripts/generate-endpoints.py`：从 `../API接口规则.md` 生成接口常量
- `src/api/endpoints.ts`：自动生成文件（不要手改）
- `src/api/admin/rbac.ts`：管理员/角色/权限 API 封装
- `src/api/admin/categories.ts`：分类 API 封装
- `src/api/admin/recommends.ts`：推荐位/推荐项 API 封装
- `src/api/admin/products.ts`：商品中心 API 封装（SPU/SKU/媒体/属性定义）
- `src/api/admin/orders.ts`：订单中心 API 封装（查询/详情/流转/备注）
- `src/api/admin/backpack.ts`：背包资产 API 封装
- `src/api/admin/group-resources.ts`：群资源 API 封装
- `src/api/admin/dict.ts`：字典 API 封装
- `src/api/admin/system-configs.ts`：系统配置 API 封装
- `src/api/admin/files.ts`：文件 API 封装
- `src/api/admin/monitor.ts`：统计/日志/事件 API 封装
- `src/mock/rbac.ts`：RBAC Mock 数据源
- `src/mock/admin.ts`：后台业务 Mock 数据源（含分类/推荐）
- `src/mock/product-center.ts`：商品中心 Mock 数据源
- `src/mock/orders-center.ts`：订单中心 Mock 数据源
- `src/mock/backpack-group.ts`：背包资产与群资源 Mock 数据源
- `src/mock/platform-center.ts`：字典/系统配置/文件 Mock 数据源
- `src/mock/monitor-center.ts`：统计/日志/事件 Mock 数据源
- `tests/*.spec.ts`：基线和 SDK 工具测试

> 说明：`src/mock/**` 目前仅保留为本地类型与历史数据参考，接口请求已不再回退 Mock。

## 运行要求
- Node.js `>=22.12.0`
- 推荐：`nvm use`（项目已提供 `.nvmrc`）

## 启动与测试
```bash
nvm use
npm install
npm run dev
npm run test:run
npm run build
```

## 联调环境配置
- 开发环境：`.env.development`
  - `VITE_API_BASE_URL=http://127.0.0.1:8080`
- 如后端地址变更，请修改该值后重启前端

## 重新生成接口常量
```bash
python3 scripts/generate-endpoints.py
```
