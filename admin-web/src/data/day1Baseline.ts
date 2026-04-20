export type BaselineDoc = {
  title: string
  owner: string
  status: '已确认' | '待确认'
  output: string
}

export type Milestone = {
  day: string
  owner: string
  focus: string
  deliverable: string
}

export type ApiTableMapping = {
  apiPrefix: string
  tables: string[]
  owner: 'FE-MP' | 'FE-ADMIN' | 'FE-Lead'
  milestoneDay: string
}

export const day1Docs: BaselineDoc[] = [
  {
    title: '项目需求文档.md',
    owner: 'FE-Lead',
    status: '已确认',
    output: '需求范围基线（小程序 + 管理端）',
  },
  {
    title: '项目技术架构文档.md',
    owner: 'FE-Lead',
    status: '已确认',
    output: '统一接口协议与前端技术栈基线',
  },
  {
    title: 'mysql设计.md',
    owner: 'FE-Lead',
    status: '已确认',
    output: '字段 1:1 对齐策略（snake_case）',
  },
  {
    title: 'API接口规则.md',
    owner: 'FE-Lead',
    status: '已确认',
    output: '156 条接口路径与响应规范',
  },
]

export const day1Milestones: Milestone[] = [
  {
    day: 'D01',
    owner: 'FE-Lead',
    focus: '项目基线冻结 + 映射产物发布',
    deliverable: '任务看板、里程碑、API-表映射',
  },
  {
    day: 'D02',
    owner: 'FE-Lead / FE-MP / FE-ADMIN',
    focus: '小程序 + 管理端工程初始化',
    deliverable: 'request SDK、拦截器、分页组件、endpoints 常量',
  },
  {
    day: 'D03-D07',
    owner: 'FE-MP',
    focus: '小程序业务闭环（登录→兑换→订单→背包）',
    deliverable: 'P0 模块联调完成',
  },
  {
    day: 'D08-D15',
    owner: 'FE-ADMIN',
    focus: '管理后台全模块建设（RBAC、商品、订单、监控）',
    deliverable: '管理端功能闭环完成',
  },
  {
    day: 'D16-D18',
    owner: 'FE-Lead + QA',
    focus: '全量联调、体验优化、UAT 发布',
    deliverable: '通过率 >= 98%，字段差异 = 0',
  },
]

export const apiTableMappings: ApiTableMapping[] = [
  {
    apiPrefix: '/api/v1/app/auth/**',
    tables: ['pm_user', 'pm_user_auth', 'pm_point_account'],
    owner: 'FE-MP',
    milestoneDay: 'D03',
  },
  {
    apiPrefix: '/api/v1/app/users/**',
    tables: ['pm_user', 'pm_point_account', 'pm_exchange_order'],
    owner: 'FE-MP',
    milestoneDay: 'D03',
  },
  {
    apiPrefix: '/api/v1/app/home/**',
    tables: ['pm_recommend_slot', 'pm_recommend_item', 'pm_product_spu', 'pm_group_resource'],
    owner: 'FE-MP',
    milestoneDay: 'D04',
  },
  {
    apiPrefix: '/api/v1/app/categories/**',
    tables: ['pm_category', 'pm_product_spu'],
    owner: 'FE-MP',
    milestoneDay: 'D04',
  },
  {
    apiPrefix: '/api/v1/app/products/**',
    tables: ['pm_product_spu', 'pm_product_sku', 'pm_product_media', 'pm_product_attr_value'],
    owner: 'FE-MP',
    milestoneDay: 'D04',
  },
  {
    apiPrefix: '/api/v1/app/addresses/**',
    tables: ['pm_user_address'],
    owner: 'FE-MP',
    milestoneDay: 'D05',
  },
  {
    apiPrefix: '/api/v1/app/exchanges/**',
    tables: [
      'pm_exchange_order',
      'pm_exchange_order_item',
      'pm_point_account',
      'pm_point_ledger',
      'pm_order_address_snapshot',
      'pm_idempotent_request',
    ],
    owner: 'FE-MP',
    milestoneDay: 'D05',
  },
  {
    apiPrefix: '/api/v1/app/orders/**',
    tables: [
      'pm_exchange_order',
      'pm_exchange_order_item',
      'pm_order_flow',
      'pm_order_delivery',
      'pm_order_address_snapshot',
    ],
    owner: 'FE-MP',
    milestoneDay: 'D06',
  },
  {
    apiPrefix: '/api/v1/app/points/**',
    tables: ['pm_point_account', 'pm_point_ledger'],
    owner: 'FE-MP',
    milestoneDay: 'D07',
  },
  {
    apiPrefix: '/api/v1/app/backpack/**',
    tables: ['pm_backpack_asset', 'pm_backpack_asset_flow'],
    owner: 'FE-MP',
    milestoneDay: 'D07',
  },
  {
    apiPrefix: '/api/v1/app/group-resources/**',
    tables: ['pm_group_resource'],
    owner: 'FE-MP',
    milestoneDay: 'D07',
  },
  {
    apiPrefix: '/api/v1/app/customer-service/**',
    tables: ['pm_system_config'],
    owner: 'FE-MP',
    milestoneDay: 'D07',
  },
  {
    apiPrefix: '/api/v1/app/dict/**',
    tables: ['pm_dict_item'],
    owner: 'FE-MP',
    milestoneDay: 'D07',
  },
  {
    apiPrefix: '/api/v1/app/system-configs/**',
    tables: ['pm_system_config'],
    owner: 'FE-MP',
    milestoneDay: 'D07',
  },
  {
    apiPrefix: '/api/v1/admin/auth/**',
    tables: ['pm_admin_user', 'pm_admin_user_role', 'pm_admin_role', 'pm_admin_role_permission', 'pm_admin_permission'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D08',
  },
  {
    apiPrefix: '/api/v1/admin/dashboard/**',
    tables: ['pm_user', 'pm_exchange_order', 'pm_point_ledger', 'pm_product_spu'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D15',
  },
  {
    apiPrefix: '/api/v1/admin/stats/**',
    tables: ['pm_user', 'pm_exchange_order', 'pm_point_ledger', 'pm_product_spu', 'pm_recommend_item'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D15',
  },
  {
    apiPrefix: '/api/v1/admin/users/**',
    tables: ['pm_user', 'pm_point_account', 'pm_point_ledger', 'pm_exchange_order', 'pm_user_address', 'pm_operation_log'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D09',
  },
  {
    apiPrefix: '/api/v1/admin/points/**',
    tables: ['pm_point_ledger', 'pm_point_rule', 'pm_point_account'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D09',
  },
  {
    apiPrefix: '/api/v1/admin/categories/**',
    tables: ['pm_category'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D10',
  },
  {
    apiPrefix: '/api/v1/admin/products/**',
    tables: ['pm_product_spu', 'pm_product_sku', 'pm_product_media', 'pm_product_attr_def', 'pm_product_attr_value', 'pm_file'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D11',
  },
  {
    apiPrefix: '/api/v1/admin/orders/**',
    tables: [
      'pm_exchange_order',
      'pm_exchange_order_item',
      'pm_order_delivery',
      'pm_order_flow',
      'pm_order_address_snapshot',
      'pm_operation_log',
    ],
    owner: 'FE-ADMIN',
    milestoneDay: 'D12',
  },
  {
    apiPrefix: '/api/v1/admin/backpack/**',
    tables: ['pm_backpack_asset', 'pm_backpack_asset_flow'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D13',
  },
  {
    apiPrefix: '/api/v1/admin/group-resources/**',
    tables: ['pm_group_resource', 'pm_file'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D13',
  },
  {
    apiPrefix: '/api/v1/admin/recommend-slots/**',
    tables: ['pm_recommend_slot', 'pm_recommend_item'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D10',
  },
  {
    apiPrefix: '/api/v1/admin/recommend-items/**',
    tables: ['pm_recommend_item'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D10',
  },
  {
    apiPrefix: '/api/v1/admin/admin-users/**',
    tables: ['pm_admin_user', 'pm_admin_user_role'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D08',
  },
  {
    apiPrefix: '/api/v1/admin/roles/**',
    tables: ['pm_admin_role', 'pm_admin_role_permission'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D08',
  },
  {
    apiPrefix: '/api/v1/admin/permissions/**',
    tables: ['pm_admin_permission'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D08',
  },
  {
    apiPrefix: '/api/v1/admin/dict/**',
    tables: ['pm_dict_type', 'pm_dict_item'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D14',
  },
  {
    apiPrefix: '/api/v1/admin/system-configs/**',
    tables: ['pm_system_config'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D14',
  },
  {
    apiPrefix: '/api/v1/admin/files/**',
    tables: ['pm_file'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D14',
  },
  {
    apiPrefix: '/api/v1/admin/operation-logs/**',
    tables: ['pm_operation_log'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D15',
  },
  {
    apiPrefix: '/api/v1/admin/outbox-events/**',
    tables: ['pm_outbox_event'],
    owner: 'FE-ADMIN',
    milestoneDay: 'D15',
  },
]

export const day1Checklist = [
  '已完成 4 份基线文档复核',
  '已产出里程碑任务看板并分配负责人',
  '已完成 API 模块与 MySQL 表映射归档',
  '已定义 D02 工程初始化输入清单',
]

export const projectScope = {
  totalApiCount: 156,
  appApiGroupCount: 14,
  adminApiGroupCount: 20,
}
