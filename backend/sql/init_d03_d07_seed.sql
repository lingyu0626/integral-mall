-- 积分商城 D03~D07 初始化种子数据
-- 依赖：先执行 init_d03_d07_schema.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 清理（按依赖顺序逆序）
DELETE FROM pm_idempotent_request;
DELETE FROM pm_backpack_asset_flow;
DELETE FROM pm_backpack_asset;
DELETE FROM pm_order_flow;
DELETE FROM pm_order_delivery;
DELETE FROM pm_order_address_snapshot;
DELETE FROM pm_exchange_order_item;
DELETE FROM pm_exchange_order;
DELETE FROM pm_user_address;
DELETE FROM pm_recommend_item;
DELETE FROM pm_recommend_slot;
DELETE FROM pm_product_media;
DELETE FROM pm_product_attr_value;
DELETE FROM pm_product_attr_def;
DELETE FROM pm_product_sku;
DELETE FROM pm_product_spu;
DELETE FROM pm_category;
DELETE FROM pm_group_resource;
DELETE FROM pm_point_ledger;
DELETE FROM pm_point_rule;
DELETE FROM pm_point_account;
DELETE FROM pm_user_auth;
DELETE FROM pm_user;
DELETE FROM pm_system_config;
DELETE FROM pm_dict_item;
DELETE FROM pm_dict_type;

-- 字典
INSERT INTO pm_dict_type (id, dict_type_code, dict_type_name, status_code)
VALUES
  (1, 'SERVICE_HOURS', '客服服务时间', 'ENABLED');

INSERT INTO pm_dict_item (id, dict_type_code, item_code, item_name, item_value, sort_no, status_code)
VALUES
  (11, 'SERVICE_HOURS', 'WEEKDAY', '周一至周五 09:00-18:00', '周一至周五 09:00-18:00', 1, 'ENABLED'),
  (12, 'SERVICE_HOURS', 'SATURDAY', '周六 10:00-16:00', '周六 10:00-16:00', 2, 'ENABLED');

-- 系统配置（公开）
INSERT INTO pm_system_config (id, config_key, config_name, config_value, value_type_code, group_code, status_code)
VALUES
  (101, 'APP_NOTICE', '平台公告', '积分兑换订单将在 1-3 个工作日内处理', 'STRING', 'APP_PUBLIC', 'ENABLED');

-- 用户与认证
INSERT INTO pm_user (
  id, user_no, open_id, phone, phone_masked, nick_name, avatar_url,
  register_channel_code, status_code, register_at
)
VALUES
  (
    1001, 'U1001', 'openid_mock_1001', '13812345678', '138****5678', '积分用户',
    'https://dummyimage.com/128x128/e8ebf1/7f8796&text=U',
    'WXAPP', 'ACTIVE', NOW(3)
  );

INSERT INTO pm_user_auth (id, user_id, auth_type_code, auth_identifier, status_code, verified_at)
VALUES
  (2001, 1001, 'WX_OPEN_ID', 'openid_mock_1001', 'ACTIVE', NOW(3)),
  (2002, 1001, 'PHONE', '13812345678', 'ACTIVE', NOW(3));

-- 积分账户与流水
INSERT INTO pm_point_account (
  id, user_id, point_balance, point_frozen, point_total_income, point_total_expense, account_status_code, last_change_at
)
VALUES
  (3001, 1001, 16888, 0, 20000, 3112, 'ACTIVE', NOW(3));

INSERT INTO pm_point_ledger (
  id, ledger_no, user_id, account_id, change_type_code, business_type_code, source_type_code, source_no,
  direction_code, change_amount, before_balance, after_balance, occurred_at, channel_code, operator_type_code, operator_id, remark
)
VALUES
  (
    4001, 'PL202603170001', 1001, 3001, 'INIT', 'INIT', 'SYSTEM', 'INIT-1',
    'INCOME', 16888, 0, 16888, NOW(3), 'SYSTEM', 'SYSTEM', 0, '初始积分'
  );

-- 分类
INSERT INTO pm_category (
  id, category_no, parent_id, level_no, category_name, sort_no, status_code, leaf_flag
)
VALUES
  (5001, 'CAT-BAIJIU', 0, 1, '白酒', 1, 'ENABLED', 1),
  (5002, 'CAT-DIGITAL', 0, 1, '数码', 2, 'ENABLED', 1),
  (5003, 'CAT-VIRTUAL', 0, 1, '生活权益', 3, 'ENABLED', 1);

-- 商品 SPU
INSERT INTO pm_product_spu (
  id, product_no, product_name, product_type_code, category_id, unit_name, main_image_url,
  detail_content, point_price, stock_total, stock_available, stock_locked, sale_status_code,
  review_status_code, recommend_flag, sort_no, limit_per_user
)
VALUES
  (
    6001, 'SPU-MOUTAI-001', '飞天茅台53度 500ML×1', 'PHYSICAL', 5001, '件',
    'https://dummyimage.com/640x420/f4f5f7/7a8291&text=Moutai',
    '经典酱香型白酒，限量兑换', 1888, 35, 35, 0, 'ON_SHELF', 'APPROVED', 1, 1, 5
  ),
  (
    6002, 'SPU-WATCH-001', '苹果 Watch Ultra3', 'PHYSICAL', 5002, '件',
    'https://dummyimage.com/640x420/f2f5f8/7a8291&text=Watch',
    '旗舰智能手表，支持多场景运动', 5988, 18, 18, 0, 'ON_SHELF', 'APPROVED', 1, 2, 2
  ),
  (
    6003, 'SPU-GROUP-VIP', '至臻玩家微信群入群资格', 'VIRTUAL', 5003, '份',
    'https://dummyimage.com/640x420/f8f3f1/7a8291&text=Group+VIP',
    '兑换后可获取入群二维码', 8888, 99999, 99999, 0, 'ON_SHELF', 'APPROVED', 1, 3, 1
  ),
  (
    6004, 'SPU-REDPACK-TICKET', '每天发红包牛票', 'VIRTUAL', 5003, '份',
    'https://dummyimage.com/640x420/f8f0f1/7a8291&text=Ticket',
    '活动类虚拟权益', 888, 99999, 99999, 0, 'ON_SHELF', 'APPROVED', 0, 4, 5
  );

-- 商品 SKU（V1 一SPU一SKU）
INSERT INTO pm_product_sku (
  id, spu_id, sku_no, sku_name, point_price, stock_total, stock_available, stock_locked, sale_status_code
)
VALUES
  (6101, 6001, 'SKU-MOUTAI-001', '默认规格', 1888, 35, 35, 0, 'ON_SHELF'),
  (6102, 6002, 'SKU-WATCH-001', '默认规格', 5988, 18, 18, 0, 'ON_SHELF'),
  (6103, 6003, 'SKU-GROUP-001', '默认规格', 8888, 99999, 99999, 0, 'ON_SHELF'),
  (6104, 6004, 'SKU-TICKET-001', '默认规格', 888, 99999, 99999, 0, 'ON_SHELF');

-- 商品媒体
INSERT INTO pm_product_media (id, spu_id, sku_id, media_type_code, media_url, media_name, sort_no, status_code)
VALUES
  (6201, 6001, 6101, 'IMAGE', 'https://dummyimage.com/640x420/f4f5f7/7a8291&text=Moutai', '主图', 1, 'ENABLED'),
  (6202, 6002, 6102, 'IMAGE', 'https://dummyimage.com/640x420/f2f5f8/7a8291&text=Watch', '主图', 1, 'ENABLED'),
  (6203, 6003, 6103, 'IMAGE', 'https://dummyimage.com/640x420/f8f3f1/7a8291&text=Group+VIP', '主图', 1, 'ENABLED'),
  (6204, 6004, 6104, 'IMAGE', 'https://dummyimage.com/640x420/f8f0f1/7a8291&text=Ticket', '主图', 1, 'ENABLED');

-- 推荐位与推荐内容
INSERT INTO pm_recommend_slot (id, slot_code, slot_name, page_code, scene_code, status_code, sort_no)
VALUES
  (7001, 'HOME_TOP', '首页推荐位', 'HOME', 'TOP', 'ENABLED', 1);

INSERT INTO pm_recommend_item (
  id, slot_id, target_type_code, target_id, title, sub_title, image_url, sort_no, status_code
)
VALUES
  (7101, 7001, 'PRODUCT_SPU', 6001, '飞天茅台53度 500ML×1', '限量兑换', 'https://dummyimage.com/1200x480/f4f6f9/657083&text=Banner+1', 1, 'ENABLED'),
  (7102, 7001, 'PRODUCT_SPU', 6002, '苹果 Watch Ultra3', '热门兑换', 'https://dummyimage.com/1200x480/f8f4f7/657083&text=Banner+2', 2, 'ENABLED'),
  (7103, 7001, 'PRODUCT_SPU', 6003, '至臻玩家微信群入群资格', '社群权益', 'https://dummyimage.com/1200x480/f1f8f2/657083&text=Banner+3', 3, 'ENABLED');

-- 群聊资源
INSERT INTO pm_group_resource (
  id, resource_no, group_name, qr_image_url, intro_text, max_member_count, current_member_count, status_code
)
VALUES
  (8001, 'GR-VIP-001', '至尊玩家群', 'https://dummyimage.com/480x480/f2f4f7/5f6878&text=VIP+GROUP', '高净值用户交流群', 500, 126, 'ENABLED'),
  (8002, 'GR-RED-001', '每日红包群', 'https://dummyimage.com/480x480/f4f7f3/5f6878&text=RED+PACKET', '每日红包活动社群', 1000, 328, 'ENABLED');

-- 用户地址：默认不插入测试地址

-- 背包资产与流水：默认不插入测试数据

SET FOREIGN_KEY_CHECKS = 1;
