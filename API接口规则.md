# 积分商城 API 接口规则（统一规范）

- 文档版本：v1.0
- 生成日期：2026-03-16
- 适用范围：微信小程序端（App）+ 管理后台（Admin）+ Spring Boot 后端
- 约束级别：**强制执行**（前后端联调、测试、上线均以此文档为准）
- 依据文档：`项目需求文档.md`、`项目技术架构文档.md`、`mysql设计.md`

---

## 1. 目标与原则

1. **路径统一**：所有接口必须使用本规范定义的路径，不允许同义重复路径。
2. **前后端一致**：前端 API 常量、后端 Controller、测试用例、接口文档路径必须一致。
3. **版本化治理**：统一前缀 `/api/v1`，禁止无版本接口进入正式环境。
4. **可扩展不破坏**：新增能力优先新增接口，尽量不破坏已有接口语义。
5. **幂等与审计**：兑换、积分调整等关键写操作必须具备幂等与操作审计。

---

## 2. 全局协议规范

## 2.1 基础路径
- 用户端：`/api/v1/app/**`
- 管理端：`/api/v1/admin/**`

## 2.2 统一响应

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

- `code = 0`：成功
- `code != 0`：失败（参考错误码分层）

## 2.3 分页协议

```json
{
  "pageNo": 1,
  "pageSize": 20,
  "total": 100,
  "list": []
}
```

## 2.4 命名规范
1. 路径统一使用小写+中划线：`/group-resources`
2. 资源名统一复数：`/orders`、`/categories`
3. 查询使用 `GET`，创建使用 `POST`，更新使用 `PUT`，删除使用 `DELETE`
4. 动作用子资源表达：`/orders/{orderId}/approve`
5. ID 参数命名统一：`{userId}`、`{orderId}`、`{spuId}`

## 2.5 鉴权规范
- App 端：`Authorization: Bearer <userToken>`
- Admin 端：`Authorization: Bearer <adminToken>`
- 关键写操作必须携带：`X-Request-Id`（UUID，用于幂等）

## 2.6 错误码分层
- `A***`：认证/授权错误
- `B***`：业务规则错误（积分不足、库存不足、状态流转非法等）
- `S***`：系统错误

---

## 3. API 路径总清单（冻结）

> 说明：以下为当前版本**全量路径清单**。后续新增/变更必须走评审并同步更新本文件。

## 3.1 App 端接口（/api/v1/app）

### 3.1.1 认证与用户

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/app/auth/wx-login` | 微信登录（code 换业务 token） |
| POST | `/api/v1/app/auth/bind-phone` | 绑定手机号 |
| POST | `/api/v1/app/auth/refresh-token` | 刷新 token |
| POST | `/api/v1/app/auth/logout` | 退出登录 |
| GET | `/api/v1/app/users/me` | 获取当前用户信息 |
| PUT | `/api/v1/app/users/me` | 更新当前用户信息 |
| GET | `/api/v1/app/users/me/summary` | 获取个人中心摘要（手机号/积分/订单计数） |

### 3.1.2 首页/分类/商品

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/app/home/recommends` | 首页推荐商品/权益列表 |
| GET | `/api/v1/app/categories` | 分类列表 |
| GET | `/api/v1/app/categories/{categoryId}/products` | 分类下商品列表 |
| GET | `/api/v1/app/products` | 商品搜索/分页列表 |
| GET | `/api/v1/app/products/{productId}` | 商品详情 |
| GET | `/api/v1/app/products/{productId}/exchange-preview` | 兑换确认预览（积分、库存、限购、地址要求） |

### 3.1.3 地址管理

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/app/addresses` | 地址列表 |
| POST | `/api/v1/app/addresses` | 新增地址 |
| GET | `/api/v1/app/addresses/{addressId}` | 地址详情 |
| PUT | `/api/v1/app/addresses/{addressId}` | 编辑地址 |
| DELETE | `/api/v1/app/addresses/{addressId}` | 删除地址 |
| PUT | `/api/v1/app/addresses/{addressId}/default` | 设为默认地址 |

### 3.1.4 兑换与订单

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/app/exchanges/orders` | 提交兑换（幂等） |
| GET | `/api/v1/app/orders` | 我的订单列表 |
| GET | `/api/v1/app/orders/{orderId}` | 订单详情 |
| GET | `/api/v1/app/orders/{orderId}/flows` | 订单状态流转记录 |
| GET | `/api/v1/app/orders/{orderId}/delivery` | 物流信息 |
| POST | `/api/v1/app/orders/{orderId}/cancel` | 用户取消订单（受状态约束） |
| GET | `/api/v1/app/orders/status-counts` | 各状态订单数量 |

### 3.1.5 积分

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/app/points/account` | 积分账户（余额） |
| GET | `/api/v1/app/points/ledger` | 积分流水列表 |
| GET | `/api/v1/app/points/ledger/{ledgerId}` | 积分流水详情 |

### 3.1.6 背包资产

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/app/backpack/assets` | 背包资产列表（支持星级/时间筛选） |
| GET | `/api/v1/app/backpack/assets/{assetId}` | 背包资产详情 |
| GET | `/api/v1/app/backpack/assets/{assetId}/flows` | 背包资产流水 |
| POST | `/api/v1/app/backpack/assets/{assetId}/use` | 资产核销/使用（如适用） |

### 3.1.7 群聊与客服

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/app/group-resources` | 群二维码资源列表/可兑换项 |
| GET | `/api/v1/app/group-resources/{resourceId}` | 群二维码资源详情 |
| GET | `/api/v1/app/customer-service/contact` | 客服入口信息 |

### 3.1.8 公共能力

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/app/dict/{dictTypeCode}/items` | 获取字典项 |
| GET | `/api/v1/app/system-configs/public` | 获取前台可见系统配置 |

---

## 3.2 Admin 端接口（/api/v1/admin）

### 3.2.1 管理端认证

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/admin/auth/login` | 管理员登录 |
| POST | `/api/v1/admin/auth/refresh-token` | 刷新 token |
| POST | `/api/v1/admin/auth/logout` | 管理员退出 |
| GET | `/api/v1/admin/auth/me` | 当前管理员信息 |
| PUT | `/api/v1/admin/auth/password` | 修改密码 |

### 3.2.2 仪表盘/统计

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/dashboard/overview` | 仪表盘总览 |
| GET | `/api/v1/admin/stats/users` | 用户统计 |
| GET | `/api/v1/admin/stats/orders` | 订单统计 |
| GET | `/api/v1/admin/stats/products` | 商品统计 |
| GET | `/api/v1/admin/stats/conversions` | 转化统计 |

### 3.2.3 用户管理

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/users` | 用户列表 |
| GET | `/api/v1/admin/users/{userId}` | 用户详情 |
| PUT | `/api/v1/admin/users/{userId}/status` | 用户冻结/解冻 |
| POST | `/api/v1/admin/users/{userId}/points/adjust` | 手工调整积分 |
| GET | `/api/v1/admin/users/{userId}/points/ledger` | 用户积分流水 |
| GET | `/api/v1/admin/users/{userId}/orders` | 用户订单列表 |
| GET | `/api/v1/admin/users/{userId}/addresses` | 用户地址列表 |

### 3.2.4 积分管理

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/points/ledger` | 全局积分流水查询 |
| GET | `/api/v1/admin/points/rules` | 积分规则列表 |
| POST | `/api/v1/admin/points/rules` | 新增积分规则 |
| PUT | `/api/v1/admin/points/rules/{ruleId}` | 更新积分规则 |
| PUT | `/api/v1/admin/points/rules/{ruleId}/status` | 启停积分规则 |

### 3.2.5 分类管理

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/categories` | 分类列表 |
| POST | `/api/v1/admin/categories` | 新增分类 |
| GET | `/api/v1/admin/categories/{categoryId}` | 分类详情 |
| PUT | `/api/v1/admin/categories/{categoryId}` | 更新分类 |
| DELETE | `/api/v1/admin/categories/{categoryId}` | 删除分类 |
| PUT | `/api/v1/admin/categories/{categoryId}/status` | 分类启停 |
| PUT | `/api/v1/admin/categories/sort` | 分类排序 |

### 3.2.6 商品管理

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/products/spu` | SPU 列表 |
| POST | `/api/v1/admin/products/spu` | 新增 SPU |
| GET | `/api/v1/admin/products/spu/{spuId}` | SPU 详情 |
| PUT | `/api/v1/admin/products/spu/{spuId}` | 更新 SPU |
| PUT | `/api/v1/admin/products/spu/{spuId}/status` | 上下架 |
| PUT | `/api/v1/admin/products/spu/{spuId}/recommend` | 推荐标记 |
| GET | `/api/v1/admin/products/spu/{spuId}/skus` | SKU 列表 |
| POST | `/api/v1/admin/products/spu/{spuId}/skus` | 新增 SKU |
| PUT | `/api/v1/admin/products/skus/{skuId}` | 更新 SKU |
| PUT | `/api/v1/admin/products/skus/{skuId}/stock` | 调整库存 |
| POST | `/api/v1/admin/products/spu/{spuId}/media` | 新增商品媒体 |
| DELETE | `/api/v1/admin/products/media/{mediaId}` | 删除商品媒体 |
| GET | `/api/v1/admin/products/attr-defs` | 属性定义列表 |
| POST | `/api/v1/admin/products/attr-defs` | 新增属性定义 |
| PUT | `/api/v1/admin/products/attr-defs/{attrDefId}` | 更新属性定义 |
| DELETE | `/api/v1/admin/products/attr-defs/{attrDefId}` | 删除属性定义 |
| GET | `/api/v1/admin/products/{spuId}/attr-values` | SPU 属性值列表 |
| POST | `/api/v1/admin/products/{spuId}/attr-values` | 新增 SPU 属性值 |
| PUT | `/api/v1/admin/products/attr-values/{attrValueId}` | 更新属性值 |
| DELETE | `/api/v1/admin/products/attr-values/{attrValueId}` | 删除属性值 |

### 3.2.7 订单管理

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/orders` | 订单列表 |
| GET | `/api/v1/admin/orders/{orderId}` | 订单详情 |
| GET | `/api/v1/admin/orders/{orderId}/items` | 订单明细项 |
| GET | `/api/v1/admin/orders/{orderId}/flows` | 状态流转记录 |
| GET | `/api/v1/admin/orders/{orderId}/delivery` | 物流详情 |
| POST | `/api/v1/admin/orders/{orderId}/approve` | 审核通过 |
| POST | `/api/v1/admin/orders/{orderId}/reject` | 审核驳回（支持积分回补） |
| POST | `/api/v1/admin/orders/{orderId}/ship` | 发货 |
| POST | `/api/v1/admin/orders/{orderId}/complete` | 完成订单 |
| POST | `/api/v1/admin/orders/{orderId}/close` | 关闭订单 |
| PUT | `/api/v1/admin/orders/{orderId}/remark` | 更新订单备注 |

### 3.2.8 背包资产管理

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/backpack/assets` | 背包资产列表 |
| GET | `/api/v1/admin/backpack/assets/{assetId}` | 背包资产详情 |
| GET | `/api/v1/admin/backpack/asset-flows` | 背包资产流水 |
| POST | `/api/v1/admin/backpack/assets/grant` | 人工发放资产 |
| POST | `/api/v1/admin/backpack/assets/{assetId}/invalidate` | 资产失效 |
| POST | `/api/v1/admin/backpack/assets/{assetId}/expire` | 资产过期 |

### 3.2.9 群二维码资源管理

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/group-resources` | 群资源列表 |
| POST | `/api/v1/admin/group-resources` | 新增群资源 |
| GET | `/api/v1/admin/group-resources/{resourceId}` | 群资源详情 |
| PUT | `/api/v1/admin/group-resources/{resourceId}` | 更新群资源 |
| PUT | `/api/v1/admin/group-resources/{resourceId}/status` | 启停群资源 |
| DELETE | `/api/v1/admin/group-resources/{resourceId}` | 删除群资源 |

### 3.2.10 推荐位管理

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/recommend-slots` | 推荐位列表 |
| POST | `/api/v1/admin/recommend-slots` | 新增推荐位 |
| PUT | `/api/v1/admin/recommend-slots/{slotId}` | 更新推荐位 |
| PUT | `/api/v1/admin/recommend-slots/{slotId}/status` | 推荐位启停 |
| GET | `/api/v1/admin/recommend-slots/{slotId}/items` | 推荐位商品列表 |
| POST | `/api/v1/admin/recommend-slots/{slotId}/items` | 新增推荐项 |
| PUT | `/api/v1/admin/recommend-items/{itemId}` | 更新推荐项 |
| DELETE | `/api/v1/admin/recommend-items/{itemId}` | 删除推荐项 |
| PUT | `/api/v1/admin/recommend-slots/{slotId}/items/sort` | 推荐项排序 |

### 3.2.11 RBAC（管理员/角色/权限）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/admin-users` | 管理员列表 |
| POST | `/api/v1/admin/admin-users` | 新增管理员 |
| GET | `/api/v1/admin/admin-users/{adminUserId}` | 管理员详情 |
| PUT | `/api/v1/admin/admin-users/{adminUserId}` | 更新管理员 |
| PUT | `/api/v1/admin/admin-users/{adminUserId}/status` | 冻结/解冻管理员 |
| PUT | `/api/v1/admin/admin-users/{adminUserId}/roles` | 分配角色 |
| POST | `/api/v1/admin/admin-users/{adminUserId}/reset-password` | 重置密码 |
| GET | `/api/v1/admin/roles` | 角色列表 |
| POST | `/api/v1/admin/roles` | 新增角色 |
| GET | `/api/v1/admin/roles/{roleId}` | 角色详情 |
| PUT | `/api/v1/admin/roles/{roleId}` | 更新角色 |
| DELETE | `/api/v1/admin/roles/{roleId}` | 删除角色 |
| PUT | `/api/v1/admin/roles/{roleId}/permissions` | 角色绑定权限 |
| GET | `/api/v1/admin/permissions` | 权限点列表 |
| GET | `/api/v1/admin/permissions/tree` | 权限树 |
| POST | `/api/v1/admin/permissions` | 新增权限点 |
| PUT | `/api/v1/admin/permissions/{permissionId}` | 更新权限点 |
| DELETE | `/api/v1/admin/permissions/{permissionId}` | 删除权限点 |

### 3.2.12 字典/配置/文件/审计

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/dict/types` | 字典类型列表 |
| POST | `/api/v1/admin/dict/types` | 新增字典类型 |
| PUT | `/api/v1/admin/dict/types/{typeId}` | 更新字典类型 |
| DELETE | `/api/v1/admin/dict/types/{typeId}` | 删除字典类型 |
| GET | `/api/v1/admin/dict/types/{typeCode}/items` | 字典项列表 |
| POST | `/api/v1/admin/dict/types/{typeCode}/items` | 新增字典项 |
| PUT | `/api/v1/admin/dict/items/{itemId}` | 更新字典项 |
| DELETE | `/api/v1/admin/dict/items/{itemId}` | 删除字典项 |
| GET | `/api/v1/admin/system-configs` | 系统配置列表 |
| POST | `/api/v1/admin/system-configs` | 新增系统配置 |
| PUT | `/api/v1/admin/system-configs/{configId}` | 更新系统配置 |
| DELETE | `/api/v1/admin/system-configs/{configId}` | 删除系统配置 |
| POST | `/api/v1/admin/files/upload` | 文件上传 |
| GET | `/api/v1/admin/files/{fileId}` | 文件详情 |
| DELETE | `/api/v1/admin/files/{fileId}` | 删除文件 |
| GET | `/api/v1/admin/operation-logs` | 操作日志列表 |
| GET | `/api/v1/admin/operation-logs/{logId}` | 操作日志详情 |
| GET | `/api/v1/admin/outbox-events` | 事件外发表查询 |
| POST | `/api/v1/admin/outbox-events/{eventId}/retry` | 事件重试 |

---

## 4. 关键业务接口约束

## 4.1 兑换下单（`POST /api/v1/app/exchanges/orders`）
1. 必须带 `X-Request-Id`
2. 必须在事务内完成：库存扣减 + 积分扣减 + 流水写入 + 订单创建
3. 失败返回明确业务码：积分不足、库存不足、商品下架、状态非法

## 4.2 积分调整（`POST /api/v1/admin/users/{userId}/points/adjust`）
1. 必填调整原因
2. 必写积分流水
3. 必写操作日志

## 4.3 订单状态流转接口
- `approve`、`reject`、`ship`、`complete`、`close` 必须校验前置状态
- 禁止跨状态非法跳转

---

## 5. 前后端协作落地要求

1. 前端必须建立统一 API 常量文件，常量值与本文件路径保持一致。
2. 后端 Controller 路径必须 100% 对齐本文件，不允许临时改路径。
3. 联调前由后端导出 OpenAPI（Swagger），与本文件做一次自动比对。
4. 若需新增接口：
   - 先改本文件（评审通过）
   - 再开发代码
   - 最后同步前端调用
5. 任何破坏性变更必须升级版本（如 `/api/v2`）。

---

## 6. 版本记录

- v1.0（2026-03-16）：基于现有 PRD、架构文档、MySQL 设计文档产出首版全量路径规范。
