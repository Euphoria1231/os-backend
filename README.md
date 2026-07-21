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
5. `sql/init_data.sql`

前四个脚本创建独立逻辑数据库和业务表，最后一个脚本写入组织、演示账号、角色、菜单、API 权限和初始公告。
可以直接在数据库管理工具中依次执行。使用 MySQL CLI 时，可在 `cmd.exe` 中从项目根目录执行：

```bat
mysql.exe --default-character-set=utf8mb4 -u root -p < sql\oa_user.sql
mysql.exe --default-character-set=utf8mb4 -u root -p < sql\oa_attendance.sql
mysql.exe --default-character-set=utf8mb4 -u root -p < sql\oa_flow.sql
mysql.exe --default-character-set=utf8mb4 -u root -p < sql\oa_notice.sql
mysql.exe --default-character-set=utf8mb4 -u root -p < sql\init_data.sql
```

## 配置环境变量

`user-service` 和 `oa-gateway` 必须使用相同的 `JWT_SECRET`。密钥不得提交到 Git，且长度至少为
32 字节。下面仅为本地设置方式，每个启动服务的终端都要能够读取所需变量：

```powershell
$env:JWT_SECRET = 'replace-with-a-random-secret-of-at-least-32-bytes'
$env:NACOS_SERVER_ADDR = '127.0.0.1:8848'
$env:NACOS_USERNAME = 'nacos'
$env:NACOS_PASSWORD = 'nacos'

$env:OA_USER_DB_PASSWORD = 'your-mysql-password'
$env:OA_ATTENDANCE_DB_PASSWORD = 'your-mysql-password'
$env:OA_FLOW_DB_PASSWORD = 'your-mysql-password'
$env:OA_NOTICE_DB_PASSWORD = 'your-mysql-password'

$env:REDIS_HOST = '127.0.0.1'
$env:REDIS_PORT = '6379'
```

各服务支持的主要变量如下：

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `JWT_SECRET` | JWT 签名密钥，Gateway 与用户服务必须一致 | 无，必须设置 |
| `JWT_VALIDITY` | Token 有效期 | `PT2H` |
| `NACOS_SERVER_ADDR` | Nacos 地址 | `127.0.0.1:8848` |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | Nacos 登录信息 | `nacos` / `nacos` |
| `NACOS_NAMESPACE` | Nacos namespace ID | 空，即 public |
| `OA_*_DB_URL` | 对应微服务的 JDBC URL | 本机对应逻辑数据库 |
| `OA_*_DB_USERNAME` | 对应微服务的数据库账号 | `root` |
| `OA_*_DB_PASSWORD` | 对应微服务的数据库密码 | 空 |
| `REDIS_HOST` / `REDIS_PORT` | Gateway 与考勤服务使用的 Redis 地址 | `127.0.0.1` / `6379` |
| `REDIS_PASSWORD` / `REDIS_DATABASE` | Gateway 与考勤服务的 Redis 认证和库编号 | 空 / `0` |

`user-service` 使用 Spring Boot 标准 Redis 配置；需要修改默认地址时，在该服务的运行配置中设置
`SPRING_DATA_REDIS_HOST`、`SPRING_DATA_REDIS_PORT`、`SPRING_DATA_REDIS_PASSWORD` 和
`SPRING_DATA_REDIS_DATABASE`。

## 发布 Nacos 配置

在 Nacos 配置管理中创建以下配置：

- Data ID：`attendance-service.yaml`
- Group：`DEFAULT_GROUP`
- 配置格式：YAML
- 配置内容：`nacos-config/attendance-service.yaml`

该配置托管上班时间、迟到阈值、打卡锁过期时间和防重复标记过期时间。修改并发布后，
`AttendanceClockProperties` 会通过 `@RefreshScope` 动态刷新。

## 启动服务

先启动 MySQL、Redis 和 Nacos，再在项目根目录安装各模块：

```powershell
mvn.cmd clean install -DskipTests
```

随后在五个独立终端中依次启动用户服务、其他业务服务和 Gateway：

```powershell
mvn.cmd -f user-service/pom.xml spring-boot:run
mvn.cmd -f attendance-service/pom.xml spring-boot:run
mvn.cmd -f flow-service/pom.xml spring-boot:run
mvn.cmd -f notice-service/pom.xml spring-boot:run
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
