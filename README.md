# integral-mall

积分商城项目，包含管理端、小程序端与后端服务代码。

该项目围绕积分兑换场景构建完整业务链路，覆盖商品管理、订单流转、积分账户、背包资产、推荐位、系统配置、群资源等模块，适合中后台系统与小程序协同开发场景。

## 项目结构

```text
.
├── admin-web/      # Vue 3 + TypeScript 管理端
├── mini-program/   # 微信小程序端
├── backend/        # Spring Boot 后端及 SQL 脚本
├── API接口规则.md
├── mysql设计.md
├── 前端开发计划.md
├── 后端开发计划.md
└── 项目需求文档.md
```

## 主要能力

- 管理端：登录鉴权、RBAC、商品中心、订单中心、推荐位、字典中心、系统配置、监控审计
- 小程序端：首页推荐、分类浏览、商品详情、地址管理、兑换下单、订单查询、积分明细、背包资产、客服链路
- 后端：Spring Boot API、统一响应、鉴权拦截、状态同步、SQL 初始化脚本

## 技术栈

### 管理端

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Naive UI

### 小程序端

- 微信小程序原生开发
- 业务接口封装

### 后端

- Spring Boot 3
- Java
- MySQL
- JWT

## 本地启动

### 1. 管理端

```bash
cd admin-web
npm install
npm run dev
```

### 2. 小程序端

使用微信开发者工具打开 `mini-program/` 目录。

### 3. 后端

```bash
cd backend/mall-api
mvn spring-boot:run
```

## 补充文档

- [API接口规则.md](./API接口规则.md)
- [mysql设计.md](./mysql设计.md)
- [前端开发计划.md](./前端开发计划.md)
- [后端开发计划.md](./后端开发计划.md)
- [项目需求文档.md](./项目需求文档.md)
- [项目技术架构文档.md](./项目技术架构文档.md)

## 说明

- 根目录已忽略 `node_modules`、`dist`、`target`、`.env.*` 等本地文件
- 子目录内如需更细粒度配置，可继续维护各自的 `.gitignore`

