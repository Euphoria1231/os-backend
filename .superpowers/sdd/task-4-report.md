# Task 4 Implementer Report

## Status

DONE

Task 4 已完成检索、可信数据范围过滤、来源服务分页读取、固定索引重建和可观察进度；本报告记录本轮范围、TDD 和最终验证证据。未执行 push，也未清理 `.m2-repository/`、`target/` 或其他用户文件。

## Operation Review

### intelligence-service

- 新增 `search/controller/SearchController.java`，提供公告/审批搜索、两类索引重建和索引健康接口。
- 新增 `search/dto/` 下的搜索分页、搜索结果、重建进度与索引健康响应模型。
- 新增 `search/service/SearchService.java`，实现中文关键词、highlight、分页、公告 `PUBLISHED` 过滤、审批 type/status 过滤和 Elasticsearch 10000 result window 校验。
- 审批搜索数据范围由后端可信 header 决定：`SUPER_ADMIN` 可检索全部，`DEPARTMENT_MANAGER` 限定为本人申请或本人审批，`EMPLOYEE` 仅本人申请。
- 新增 `search/service/SearchIndexAdministrationService.java`，按 100 条分页读取来源数据，执行 delete-by-query、bulk、分页契约/最终数量校验、refresh，并维护 `IDLE/RUNNING/COMPLETED/FAILED` 进度。
- 扩展 `ElasticsearchGateway`、`RestElasticsearchGateway`、`SearchIndexRepository` 和 `ElasticsearchSearchIndexRepository`，支持 search、bulk、delete-by-query、refresh；bulk partial failure 会使重建失败。
- 将 `approverId` 贯通审批 source DTO、Feign gateway、document、normalizer、mapping、事件处理和重建流程。
- 新增 `search/web/GlobalExceptionHandler.java`，统一处理参数、业务和未预期异常响应。
- 更新 `IntelligenceServiceApplicationTests`、事件/Feign/initializer/repository 测试和 `ElasticsearchStubServer`，覆盖查询结构、权限范围、stale document 清理、分页契约、最终数量、partial bulk failure、mapping 和事件字段。

### flow-service

- 新增 `ApplicationSearchSourceController`、`ApplicationSearchSourceResponse` 和 `ApplicationSearchSourcePageResponse`，提供只读 `/internal/flow/search-source` 单条及分页来源接口。
- 扩展 `FlowService`、`FlowMapper` 和 `FlowMapper.xml`，分页读取本服务审批数据；offset 使用 long 计算并在超出 Mapper int 范围时返回 400。
- 更新 `FlowControllerTests`，覆盖单条/分页来源、`approverId` 和 offset overflow。

### notice-service

- 新增 `NoticeSearchSourceController`、`NoticeSearchSourceResponse` 和 `NoticeSearchSourcePageResponse`，提供只读 `/internal/notices/search-source` 单条及分页来源接口。
- 扩展 `NoticeService`、`NoticeMapper` 和 `NoticeMapper.xml`，仅输出 `PUBLISHED` 公告；offset 使用 long 计算并在超出 Mapper int 范围时返回 400。
- 更新 `NoticeControllerTests`，覆盖单条/分页来源和 offset overflow。

### oa-gateway 与初始化数据

- 更新 `AuthenticationGlobalFilter`：先删除客户端伪造的 `X-Employee-Id`、`X-Username`、`X-Roles`、`X-Permissions`，再从 JWT claims 重建四个可信 header。
- 更新 `AuthenticationGlobalFilterTests`，验证可信 roles/permissions 注入及伪造 header 清洗。
- 更新 `sql/init_data.sql`：增加 `DEPARTMENT_MANAGER` 角色，employee 2 为部门主管、employee 3 为普通员工；两类普通角色仅获得全文检索 GET 权限，未获得 rebuild POST 权限。

### 明确未修改范围

- 未新增 `/internal/**` Gateway route，未把来源接口暴露给前端。
- 未新增 internal token、mTLS 或其他 service-to-service authentication 机制。
- 未实现 Task 5～8 的 AI、XXL-Job、Sentinel 或前端功能。
- 未改动 `.env`、依赖锁文件、Nacos 配置或无关模板。

## TDD Evidence

### RED

使用 Java 21 和项目本地 Maven repository 执行定向测试，在实现补齐前得到以下预期失败：

- Gateway：5 tests，2 Failures，0 Errors；缺少可信 `X-Permissions` 重建。
- flow-service：7 tests，2 Failures，0 Errors；缺少 `approverId`，offset overflow 返回 500。
- notice-service：6 tests，1 Failure，0 Errors；offset overflow 返回 500。
- intelligence-service：28 tests，7 Failures，0 Errors；缺少 manager scope、result window、stale cleanup、pagination completeness、event `approverId` 和 mapping 等行为。
- 合计：46 tests，12 Failures，0 Errors。

### GREEN（定向回归）

```powershell
$env:JAVA_HOME='C:\Users\TangSY\.jdks\ms-21.0.11'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn.cmd "-Dmaven.repo.local=D:\Documents\Desktop\newlandedu\code\os-backend\.m2-repository" `
  -pl intelligence-service,oa-gateway,flow-service,notice-service -am `
  "-Dtest=IntelligenceServiceApplicationTests,SearchIndexEventProcessorTests,SearchIndexInitializerTests,ElasticsearchSearchIndexRepositoryTests,FeignSearchDocumentSourceGatewayTests,AuthenticationGlobalFilterTests,FlowControllerTests,NoticeControllerTests" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test -B
```

结果：50 tests，0 Failures，0 Errors，BUILD SUCCESS（Gateway 5、flow 7、notice 6、intelligence 32）。

## Final Verification

最终完整聚合验证（未使用 `-Dtest`）：

```powershell
$env:JAVA_HOME='C:\Users\TangSY\.jdks\ms-21.0.11'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn.cmd "-Dmaven.repo.local=D:\Documents\Desktop\newlandedu\code\os-backend\.m2-repository" `
  -pl intelligence-service,oa-gateway,flow-service,notice-service -am test -B
```

- 完成时间：2026-07-22 21:21:43 +08:00。
- Reactor：`os-backend`、`oa-common`、`oa-gateway`、`flow-service`、`notice-service`、`intelligence-service` 全部 SUCCESS。
- Surefire XML 汇总：15 suites，65 tests，0 Failures，0 Errors，0 Skipped。
- 模块统计：oa-common 8、oa-gateway 10、flow-service 7、notice-service 6、intelligence-service 34。
- Maven exit code：0，BUILD SUCCESS。

执行 `git diff --check`：exit code 0，无 whitespace error；仅出现 Git 的 LF/CRLF 转换 warning。

## Static Review

- 已逐项复核 Task 4 brief 和中间审查问题，确认 manager scope、普通角色搜索权限、可信 permissions header、stale document 清理、分页完整性、result window、offset overflow、测试隔离与 refresh 均已覆盖。
- 变更路径仅位于 intelligence-service、oa-gateway、flow-service、notice-service、`sql/init_data.sql` 和本报告，未发现 Task 5～8 内容。
- 未发现 hardcoded secret、`${}` SQL 拼接、跨服务数据库访问或 `/internal/**` Gateway route。

## Unverified Items

- 未启动真实 Nacos、Elasticsearch/IK、Gateway 和四个服务进行 live end-to-end 联调。
- 未执行真实 MySQL 初始化脚本或 Apifox 导入/接口测试。
- `/internal/**` 来源接口依赖部署网络边界隔离；本任务未新增 service-to-service credential。

## Commit

```text
feat(search): 添加全文检索与索引重建
```
