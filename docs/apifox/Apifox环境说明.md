# Apifox 接口测试环境说明

## 概述

本文档描述 OA 系统 Apifox 项目的接口测试结构、环境变量和断言约定。

## 项目结构

`
OA 接口测试/
├── 环境
│   └── Local（本地开发环境）
├── 接口集合
│   ├── 认证模块
│   │   ├── POST /auth/login
│   │   └── POST /auth/logout
│   ├── 用户模块
│   │   ├── GET /user/departments
│   │   ├── POST /user/departments
│   │   ├── PUT /user/departments/{id}
│   │   ├── DELETE /user/departments/{id}
│   │   ├── GET /user/positions
│   │   ├── POST /user/positions
│   │   ├── GET /user/employees
│   │   ├── POST /user/employees
│   │   ├── PUT /user/employees/{id}
│   │   └── DELETE /user/employees/{id}
│   ├── 考勤模块
│   │   ├── POST /attendance/punch-in
│   │   ├── POST /attendance/punch-out
│   │   ├── GET /attendance/today-status
│   │   └── GET /attendance/records
│   ├── 审批模块
│   │   ├── POST /flow/leave
│   │   ├── POST /flow/overtime
│   │   ├── GET /flow/pending-tasks
│   │   └── POST /flow/approve
│   ├── 公告模块
│   │   ├── POST /notice/create
│   │   ├── GET /notice/list
│   │   ├── GET /notice/{id}
│   │   └── PUT /notice/read-status
│   └── 智能模块
│       ├── GET /intelligence/search
│       ├── POST /intelligence/ai/query
│       └── GET /intelligence/dashboard
├── 目录脚本
│   └── 前置操作：登录后自动提取 Token
└── 场景
    ├── 员工上班打卡闭环
    ├── 请假审批闭环
    └── 公告已读闭环
`

## 环境变量

| 变量名 | 值 | 说明 |
|--------|-----|------|
| base_url | http://localhost:8088 | Gateway 地址 |
| admin_username | admin | 管理员账号（实际值通过安全方式获取） |
| admin_password | - | 通过安全方式传入，导出的文件已清除 |
| employee_username | employee | 普通员工账号 |
| employee_password | - | 通过安全方式传入，导出的文件已清除 |
| accessToken | - | 登录后自动填充，导出前清除 |

## 前置脚本

在认证模块和需要认证的目录配置前置脚本：

1. 登录接口响应中提取 data.accessToken
2. 保存到环境变量 ccessToken
3. 后续请求在 Header 中自动添加 Authorization: Bearer {{accessToken}}

## 断言约定

### 成功断言
`javascript
pm.test("状态码 200", () => pm.response.to.have.status(200));
pm.test("业务码为成功", () => {
    const json = pm.response.json();
    pm.expect(json.code).to.eql(200);
});
`

### 参数错误断言
`javascript
pm.test("状态码 400", () => pm.response.to.have.status(400));
`

### 未认证断言
`javascript
pm.test("状态码 401", () => pm.response.to.have.status(401));
`

### 无权限断言
`javascript
pm.test("状态码 403", () => pm.response.to.have.status(403));
`

## 导出规范

- 导出前清除 Local 环境中的 Token、密码和 Cookie
- 导出的 JSON 文件保存至 docs/apifox/oa-apifox.json
- 提交前扫描文件确认无真实密钥
