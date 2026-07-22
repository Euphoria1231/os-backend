# OA 办公管理系统后端

基于 Java 21、Spring Boot 3.5.15、Spring Cloud 2025.0.0 和 Spring Cloud Alibaba
2025.0.0.0 的 Maven 多模块微服务项目。前端只通过 Gateway 访问业务接口，服务注册与配置管理使用
Nacos，数据访问使用 MyBatis，登录状态使用 JWT 和 Redis。

## 模块说明

| 模块 | 端口 | 职责 |
| --- | ---: | --- |
| `oa-common` | - | 统一响应、异常模型和 JWT 公共能力 |
| `oa-gateway` | 8088 | 路由、CORS、JWT 校验、接口权限和操作日志 |
| `user-service` | 9001 | 登录退出、部门、岗位、员工和 RBAC |
| `attendance-service` | 9002 | 上下班打卡、个人记录、Redis 防重复与分布式锁 |
| `flow-service` | 9003 | 请假、加班、直属领导一级审批和待办/已办 |
| `notice-service` | 9004 | 公告发布、公告查询和已读/未读状态 |
| `intelligence-service` | 9005 | 全文检索、AI 分析和数据大屏聚合能力 |

根工程使用 `pom` packaging，只负责依赖和模块管理。IDEA 创建时保留的根目录启动类不是微服务启动入口。

## 运行依赖

- JDK 21
- Maven 3.9+
- MySQL 8
- Redis
- Nacos
- Apifox，用于接口契约管理、Mock、调试和前后端联调

确认本机环境：

```powershell
java -version
mvn.cmd -version
```

## 初始化数据库

按以下顺序执行脚本：

1. `sql/oa_user.sql`
2. `sql/oa_attendance.sql`
3. `sql/oa_flow.sql`
4. `sql/oa_notice.sql`
5. `sql/oa_ai.sql`
6. `sql/init_data.sql`

前五个脚本创建独立逻辑数据库，其中现有四个业务脚本同时创建基础业务表；最后一个脚本写入组织、演示账号、角色、菜单、API 权限和初始公告。
可以直接在数据库管理工具中依次执行。使用 MySQL CLI 时，可在 `cmd.exe` 中从项目根目录执行：

```bat
mysql.exe --default-character-set=utf8mb4 -u root -p < sql\oa_user.sql
mysql.exe --default-character-set=utf8mb4 -u root -p < sql\oa_attendance.sql
mysql.exe --default-character-set=utf8mb4 -u root -p < sql\oa_flow.sql
mysql.exe --default-character-set=utf8mb4 -u root -p < sql\oa_notice.sql
mysql.exe --default-character-set=utf8mb4 -u root -p < sql\oa_ai.sql
mysql.exe --default-character-set=utf8mb4 -u root -p < sql\init_data.sql
```

## 配置本地环境

本地开发默认使用以下地址，无需写入环境文件：

- MySQL：`127.0.0.1:3306`，用户名 `root`
- Redis：`127.0.0.1:6379`，无密码，数据库 `0`
- Nacos：`127.0.0.1:8848`，public namespace，不启用认证

复制环境变量示例文件：

```powershell
Copy-Item .env.example .env
```

然后只需要在 `.env` 中填写两个本地值：

```properties
JWT_SECRET=一段至少32字节的随机密钥
OA_DB_PASSWORD=本地MySQL密码
```

六个服务会在从后端根目录或各自模块目录启动时自动读取同一份根目录 `.env`。其中
`user-service` 和 `oa-gateway` 会共享 `JWT_SECRET`，五个数据服务会共享 `OA_DB_PASSWORD`。
真实 `.env` 已被 Git 忽略，不得提交。

需要连接非本地环境时，仍可通过原有的 `NACOS_*`、`REDIS_*`、`OA_*_DB_URL`、
`OA_*_DB_USERNAME` 和服务专用 `OA_*_DB_PASSWORD` 覆盖默认值；服务专用数据库密码的优先级高于
统一的 `OA_DB_PASSWORD`。

## 发布 Nacos 配置

在 Nacos 配置管理中创建以下配置：

- Data ID：`attendance-service.yaml`
- Group：`DEFAULT_GROUP`
- 配置格式：YAML
- 配置内容：`nacos-config/attendance-service.yaml`

该配置托管上班时间、迟到阈值、打卡锁过期时间和防重复标记过期时间。修改并发布后，
`AttendanceClockProperties` 会通过 `@RefreshScope` 动态刷新。

## Elasticsearch 搜索基础

`intelligence-service` 使用与本地服务端完全一致的
`org.elasticsearch.client:elasticsearch-rest-client:7.13.0`。这里不使用由当前 Spring Data
默认管理的 8.x 客户端，避免与课程固定的 Elasticsearch 7.13.0 服务端产生版本不兼容。

公告索引名为 `oa-notices-v1`，审批索引名为 `oa-applications-v1`。文档 ID 使用
`notice-{业务ID}` 和 `application-{业务ID}`，重复写入同一业务数据时会覆盖同一文档，不会新增重复文档。

两个索引的中文字段使用 `ik_max_word` 写入分词和 `ik_smart` 查询分词。因此 Elasticsearch 必须安装
与服务端同版本的 `analysis-ik 7.13.0`。智能服务启动时会主动检查两个分析器；插件缺失时会明确报告
需要安装的插件和版本，不会静默改用普通分词器。

搜索索引通过 RocketMQ 主题 `oa-search-index-events` 异步同步，默认消费组为
`intelligence-search-index-consumer`。事件包含 `eventId`、`aggregateType`、`operation`、
`aggregateId`、`version` 和可选的 `document`。`eventId` 用于防止重复消费，`version` 必须在同一业务聚合内
单调递增；低于或等于已处理版本的迟到事件会记录为 `IGNORED_OUTDATED`，不会覆盖新数据或误删索引。
首次事件会先创建 version 0 的聚合状态占位行，再通过行锁串行处理同一聚合的并发事件。

UPSERT 事件可以携带完整搜索文档；文档缺失时，智能服务通过来源服务的只读 `search-source` 接口查询，
禁止直接访问 `oa_notice` 或 `oa_flow`。消费结果和聚合版本记录在 `oa_ai`，索引操作失败会回滚事件占用并
抛出异常，由 RocketMQ 重投。即使 Elasticsearch 写入成功后数据库提交失败，确定性文档 ID 也能保证重投幂等。
来源服务需要分别提供 `/internal/notices/search-source/{noticeId}` 和
`/internal/flow/search-source/{applicationId}`，当前提交固定消费端契约，不跨模块实现来源接口。

## OpenAPI、Swagger UI 与 Apifox

五个数据与业务服务通过 Springdoc 生成 OpenAPI 3 文档，Gateway 提供统一访问入口：

| 服务 | OpenAPI 地址 |
| --- | --- |
| 用户与权限 | `http://localhost:8088/openapi/user` |
| 考勤 | `http://localhost:8088/openapi/attendance` |
| 审批 | `http://localhost:8088/openapi/flow` |
| 公告 | `http://localhost:8088/openapi/notice` |
| 搜索与智能平台 | `http://localhost:8088/openapi/intelligence` |

聚合 Swagger UI 地址为 `http://localhost:8088/swagger-ui.html`，可以通过页面顶部下拉框切换服务。
在 Apifox 中选择 OpenAPI/Swagger 导入，依次导入上述五个 OpenAPI 地址即可同步到同一个项目。
接口请求仍统一使用 `http://localhost:8088` 作为环境 Base URL。

## 启动服务

先启动 MySQL、Redis 和 Nacos，再在项目根目录安装各模块：

```powershell
mvn.cmd clean install -DskipTests
```

随后在六个独立终端中依次启动用户服务、其他业务服务和 Gateway：

```powershell
mvn.cmd -f user-service/pom.xml spring-boot:run
mvn.cmd -f attendance-service/pom.xml spring-boot:run
mvn.cmd -f flow-service/pom.xml spring-boot:run
mvn.cmd -f notice-service/pom.xml spring-boot:run
mvn.cmd -f intelligence-service/pom.xml spring-boot:run
mvn.cmd -f oa-gateway/pom.xml spring-boot:run
```

使用 IDEA 时，运行各模块自己的 `*Application` 类，并在 Run Configuration 中设置相同的环境变量。
答辩前应提前启动全部服务并确认以下健康检查返回 `UP`：

```text
http://localhost:8088/actuator/health
http://localhost:9001/actuator/health
http://localhost:9002/actuator/health
http://localhost:9003/actuator/health
http://localhost:9004/actuator/health
http://localhost:9005/actuator/health
```

## 演示账号

初始化脚本提供三个账号，密码统一为 `Oa@123456`，数据库中只保存 BCrypt Hash。

| 账号 | 身份 | 用途 |
| --- | --- | --- |
| `admin` | 超级管理员 | 组织权限维护、公告发布和全业务演示 |
| `leader` | 部门主管 | 审批普通员工提交的申请 |
| `employee` | 普通员工 | 打卡、提交请假/加班、查看公告 |

`employee` 的直属领导是 `leader`，可以直接演示一级审批闭环。

## Apifox 联调

Apifox 环境的 Base URL 统一设置为：

```text
http://localhost:8088
```

先调用登录接口：

```http
POST /api/user/auth/login
Content-Type: application/json

{
  "username": "employee",
  "password": "Oa@123456"
}
```

从响应 `data.token` 取得 JWT，并在 Apifox 环境中配置 Bearer Token。除登录和用户服务健康接口外，
所有 `/api/**` 请求都需要 Token。接口契约、示例和测试用例统一在 Apifox 项目中维护，不引入
Knife4j；前端不得绕过 Gateway 直接访问业务服务端口。

## 测试

在后端根目录执行全部自动化测试：

```powershell
mvn.cmd test -B
```

测试使用 H2 隔离业务数据库，并关闭测试环境中的 Nacos 注册与配置检查，不会写入本地 MySQL。
