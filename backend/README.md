# 积分商城后端（启动阶段）

> 启动日期：2026-03-17  
> 目标：按《后端开发计划.md》推进 Spring Boot 后端落地，并与小程序/管理端联调。

## 当前状态
- 已进入后端计划执行阶段（D01-D03 准备段）
- 前端小程序端已完成 D03-D07 页面与接口接入，可进入真实接口联调

## 目录约定（计划）
```text
backend/
  docs/                   # 契约冻结、联调记录、回归报告
  mall-api/               # Spring Boot 工程（待初始化）
```

## 下一步（本轮之后）
1. 初始化 `mall-api` Spring Boot 3.x（JDK17）工程骨架
2. 落地统一响应体与全局异常
3. 冻结并发布 OpenAPI（先覆盖 App 端 38 条接口）

## SQL 脚本（已生成）

- `backend/sql/init_d03_d07_schema.sql`：D03~D07 建表脚本（26 张表，来自 `mysql设计.md`）
- `backend/sql/init_d03_d07_seed.sql`：联调种子数据（用户/商品/地址/积分/背包/群资源）

执行示例：
```bash
mysql -h127.0.0.1 -uroot -p your_db < backend/sql/init_d03_d07_schema.sql
mysql -h127.0.0.1 -uroot -p your_db < backend/sql/init_d03_d07_seed.sql
```
