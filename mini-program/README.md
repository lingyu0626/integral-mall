# 碎片商城小程序（D03 + D07）

## 已完成
### D03（登录与用户）
- `POST /api/v1/app/auth/wx-login`
- `POST /api/v1/app/auth/bind-phone`
- `GET/PUT /api/v1/app/users/me`
- `GET /api/v1/app/users/me/summary`

### D04（首页/分类/商品）
- `GET /api/v1/app/home/recommends`
- `GET /api/v1/app/categories`
- `GET /api/v1/app/categories/{categoryId}/products`
- `GET /api/v1/app/products`
- `GET /api/v1/app/products/{productId}`
- `GET /api/v1/app/products/{productId}/exchange-preview`

### D05（地址与兑换提交）
- `GET /api/v1/app/addresses`
- `POST /api/v1/app/addresses`
- `GET /api/v1/app/addresses/{addressId}`
- `PUT /api/v1/app/addresses/{addressId}`
- `DELETE /api/v1/app/addresses/{addressId}`
- `PUT /api/v1/app/addresses/{addressId}/default`
- `POST /api/v1/app/exchanges/orders`

### D06（订单模块）
- `GET /api/v1/app/orders`
- `GET /api/v1/app/orders/{orderId}`
- `GET /api/v1/app/orders/{orderId}/flows`
- `GET /api/v1/app/orders/{orderId}/delivery`
- `POST /api/v1/app/orders/{orderId}/cancel`
- `GET /api/v1/app/orders/status-counts`

### D07（碎片/背包/群聊客服）
- `GET /api/v1/app/points/account`
- `GET /api/v1/app/points/ledger`
- `GET /api/v1/app/points/ledger/{ledgerId}`
- `GET /api/v1/app/backpack/assets`
- `GET /api/v1/app/backpack/assets/{assetId}`
- `GET /api/v1/app/backpack/assets/{assetId}/flows`
- `POST /api/v1/app/backpack/assets/{assetId}/use`
- `GET /api/v1/app/group-resources`
- `GET /api/v1/app/group-resources/{resourceId}`
- `GET /api/v1/app/customer-service/contact`
- `GET /api/v1/app/dict/{dictTypeCode}/items`
- `GET /api/v1/app/system-configs/public`

## UI 调整
- 主题改为你截图风格：浅灰背景 + 白卡片 + 红色碎片价 + 绿色激活态。
- 增加底部导航（TabBar）：首页 / 分类 / 购物车 / 我的。

## 本轮说明
- 新增“收货地址”页面，支持增删改查、设默认。
- 商品详情“立即兑换”已接入“确认兑换”页，可选择地址并提交兑换请求。
- 新增“我的订单”列表与“订单详情”页面，支持状态筛选、订单取消、状态流转与物流展示。
- 新增“碎片明细”“我的背包（含资产详情/使用）”“群聊与客服”页面，打通 D07 页面链路。

## 测试
在项目根目录执行：
```bash
node --test mini-program/tests/*.test.js
```

## 联调环境
- 小程序请求基地址：`mini-program/utils/env.js`
- 当前联调默认值：`http://127.0.0.1:8080`
- 启动后端：`cd backend/mall-api && mvn spring-boot:run`
