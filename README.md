# DevTrack 缺陷管理系统(后端脚手架)

替代 TAPD 的自研缺陷管理平台,后端对齐团队 `nx-skyline-server` 工程约定。
完整设计见 `~/Desktop/DevTrack_缺陷管理系统_技术方案.md`。

> 本脚手架交付的是 **可运行并已端到端验证的骨架**,覆盖:
> 自研 Token 鉴权、缺陷 CRUD、配置化状态机流转、飞书 Webhook 通知、pytest 自动建单集成、
> **RBAC 权限(功能权限 + 角色级流转约束)、飞书扫码登录 SSO、TAPD CSV 迁移、Vue3 前端**。

## 技术栈(对齐 nx-skyline 实证栈)

| 维度 | 选型 |
|---|---|
| 语言/框架 | Java 17 + Spring Boot 3.2.0 |
| 工程形态 | Maven 多模块(`devtrack-common` / `devtrack-app`) |
| 持久层 | Spring Data JPA(实体 `extends BaseModel`,软删 `@SQLRestriction`) |
| 鉴权 | 自研 Token + Filter(`AuthTokenFilter` + `UserContext`,48h 过期),**非 Sa-Token** |
| Web 约定 | `CommonResponse<T>` 统一返回体,`/devtrack/{域}/*`,业务层叫 `Manager` |
| 本地库 | H2 内存库(零基础设施) |
| 生产库 | MySQL 8 + Flyway(`db/migration/V1__baseline.sql`) |

## 模块结构

```
devtrack-server/
├── devtrack-common/          # DTO / Request / 枚举 / CommonResponse / BizException
├── devtrack-app/             # 应用主体
│   ├── controller/           # Auth(含飞书登录) / Defect(CRUD+详情+看板+统计) / Integration / Migration / Health
│   │   └── filter/           # AuthTokenFilter(自研 Token 鉴权)
│   ├── manager/              # Defect/Comment/Iteration/Requirement/TestCase/Relation/Attachment/Notification/CustomField/Dashboard/ApiToken/Automation/Feishu*/TapdMigration/Email
│   ├── security/             # PermissionManager + Perms(RBAC + 数据范围)+ TokenStore(内存/Redis)+ FeishuSignatureVerifier
│   ├── storage/              # StorageService:LocalFileStorage(默认)/ MinioStorage(生产)
│   ├── dao/                  # JpaRepository(全部领域实体)
│   ├── model/                # BaseModel / Defect / Wf* / Role / UserRole / Comment / StatusHistory / ActivityLog / Iteration / Requirement / TestCase / Relation
│   ├── config/               # Password(BCrypt) / OpenApi(Swagger) / Feishu / RestClient
│   ├── init/                 # DataInitializer(幂等播种 角色+账号+状态+流转)
│   └── resources/
│       ├── application.yml / -local.yml(H2) / -prod.yml(MySQL+Flyway)
│       └── db/migration/V1__baseline.sql, V2__requirement_testcase.sql, V3__notification_customfield.sql
│   └── src/test/                # 单元 + 集成测试(mvn test,11 用例)
├── devtrack-web/             # Vue3 + Vite + Element-Plus 前端(已并入)
├── integration-pytest/       # pytest 上报插件(失败自动建单 + 去重)
├── migration-samples/        # TAPD 导出 CSV 样例
├── smoke_test.sh             # 缺陷主流程冒烟
└── smoke_test2.sh            # RBAC + 飞书登录 + 迁移冒烟
```

## 本地快速启动(零基础设施,H2 内存库)

前置:JDK 17+(本机 JDK 22 可)、Maven 3.8+(`brew install maven`)。

```bash
cd ~/Desktop/devtrack-server
mvn -q -DskipTests spring-boot:run -pl devtrack-app
# 或:mvn -q -DskipTests package && java -jar devtrack-app/target/devtrack-app.jar
```

启动后:
- 服务地址 `http://localhost:8080`
- H2 控制台 `http://localhost:8080/h2-console`(JDBC URL: `jdbc:h2:mem:devtrack`)
- 演示账号(由 `DataInitializer` 幂等播种,用于演示 RBAC 差异):

  | 账号 | 密码 | 角色 | 能力 |
  |---|---|---|---|
  | `admin` | `admin123` | SYS_ADMIN(全局) | 全权 |
  | `qa` | `qa123` | QA(项目1) | 建/改/流转缺陷,可 verify |
  | `dev` | `dev123` | DEV(项目1) | 改/流转缺陷,**不能 verify** |
  | `guest` | `guest123` | GUEST(项目1) | 只读 |

## 一键冒烟测试(两个脚本,均已验证可跑)

服务起来后,另开终端:

```bash
cd ~/Desktop/devtrack-server
./smoke_test.sh     # 登录→建缺陷→列表→流转→pytest 失败自动建单+去重
./smoke_test2.sh    # RBAC 权限差异 + 飞书扫码登录(dev模式) + TAPD CSV 迁移
./smoke_test3.sh    # 缺陷详情(历史+评论+可用流转) + 看板 + 统计 + 数据范围
./smoke_test4.sh    # BCrypt 登录 + 迭代/燃尽图 + 飞书可交互卡片回调
./smoke_test5.sh    # 需求 + 测试用例 + 三向关联 + Swagger/API 文档
./smoke_test6.sh    # 迭代报表 + API Token 存库 SHA256
./smoke_test7.sh    # 附件上传/下载 + 操作记录 + 批量流转 + 燃尽图理想线
./smoke_test8.sh    # 自定义字段 + @提及/指派站内信 + 全文搜索 + 数据大屏
./smoke_test9.sh    # AI 生成用例+评审入库 + 多端标签 + 版本关联 + SLA + 缺陷查重
```

## 前端(Vue3)

```bash
cd ~/Desktop/devtrack-server/devtrack-web
npm install
npm run dev         # http://localhost:5173,已配 Vite 代理到 8080(开发期免 CORS)
```
后端先在 8080 跑起来,浏览器开 5173 用 `admin/admin123` 登录,即可看缺陷列表/新建/流转。

## 核心接口

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | `/devtrack/auth/login` | 无 | 登录拿 token |
| POST | `/devtrack/defect/create` | Bearer token | 建缺陷 |
| POST | `/devtrack/defect/list` | Bearer token | 列表筛选分页 |
| POST | `/devtrack/defect/transition` | Bearer token | 配置化状态机流转(写历史+审计) |
| POST | `/devtrack/defect/detail` | Bearer token | 缺陷详情(缺陷+流转历史+评论+可用流转) |
| POST | `/devtrack/defect/comment` | Bearer token | 加评论 |
| POST | `/devtrack/defect/board` | Bearer token | 看板(按状态分列) |
| POST | `/devtrack/defect/stats` | Bearer token | 统计(按状态/优先级/严重度) |
| POST | `/devtrack/defect/assign-iteration` | Bearer token | 缺陷归入/移出迭代 |
| POST | `/devtrack/iteration/create\|list\|close` | Bearer token | 迭代 CRUD |
| POST | `/devtrack/iteration/burndown` | Bearer token | 燃尽图数据 |
| POST | `/devtrack/iteration/report` | Bearer token | 迭代报表(完成率/各状态分布) |
| POST | `/devtrack/apitoken/create` | Bearer token + SYS_ADMIN | 创建 API 机器令牌(返回明文一次) |
| POST | `/devtrack/defect/activity` | Bearer token | 缺陷操作历史 |
| POST | `/devtrack/defect/batch-transition` | Bearer token | 批量状态流转(逐条独立) |
| POST | `/devtrack/attachment/upload\|list` · GET `/{id}/download` | Bearer token | 附件上传/列表/下载 |
| POST | `/devtrack/defect/set-fields` · `/search` | Bearer token | 设自定义字段值 / 全文搜索 |
| POST | `/devtrack/customfield/defs\|create` | Bearer token | 自定义字段定义 |
| POST | `/devtrack/notification/list\|unread-count\|mark-read\|mark-all-read` | Bearer token | 站内信通知 |
| POST | `/devtrack/dashboard/overview` | Bearer token | 数据大屏聚合 |
| POST | `/devtrack/feishu/card-callback` | 飞书(签名校验) | 卡片按钮回调(我接单/标记已解决)+ URL 验证 |
| POST | `/devtrack/integrations/test-reports` | API Token | pytest/CI 上报,失败自动建单去重 |
| GET | `/devtrack/auth/feishu/authorize-url` | 无 | 取飞书扫码授权 URL |
| POST | `/devtrack/auth/feishu/login?code=` | 无 | 飞书 code 换登录 token(未配 appId 走 dev 模式) |
| POST | `/devtrack/migration/tapd` | Bearer token + MIGRATION:RUN | 上传 TAPD 导出 CSV 迁移(multipart: file + projectId) |
| POST | `/devtrack/requirement/create\|list` | Bearer token | 需求管理 |
| POST | `/devtrack/testcase/create\|list\|update-status` | Bearer token | 测试用例管理 |
| POST | `/devtrack/relation/link\|unlink\|list` | Bearer token | 需求-缺陷-用例三向关联 |
| GET | `/devtrack/health` | 无 | 健康检查 |
| GET | `/swagger-ui/index.html`、`/v3/api-docs` | 无 | API 文档 / Swagger UI |

## 生产部署(对齐团队 EKS + buildspec)

- `--spring.profiles.active=prod`,经 `DB_URL/DB_USERNAME/DB_PASSWORD/REDIS_HOST/FEISHU_WEBHOOK_URL/DEVTRACK_API_TOKEN` 注入。
- schema 由 Flyway 管理(`V1__baseline.sql`),Hibernate `ddl-auto=none`。
- Dockerfile / Deployment 可直接复用 `nx-skyline-server/APP-META/` 模板,替换 jar 名为 `devtrack-app`。

## 接团队基础库(可选,生产建议)

当前为**自包含**实现(自带 `BaseModel`/`CommonResponse`/`BizException` 等)以便本地零依赖运行。
接入团队 CodeArtifact 后,可在父 `pom.xml` 启用 `nx-common-springboot-*` 依赖,删除本地等价类,改为 `import com.nx.common.*`。

## 已实现的能力

- ✅ 自研 Token 鉴权(AuthTokenFilter + UserContext,48h 过期)
- ✅ 缺陷 CRUD + 配置化状态机流转(wf_transition 表驱动)
- ✅ 飞书 Webhook 通知 + 飞书扫码登录 SSO(dev 模式可本地验证)
- ✅ pytest/Allure 失败自动建单 + historyId 去重
- ✅ RBAC:功能权限(BUG:CREATE/VIEW/TRANSITION/MIGRATION:RUN 等)+ 角色级流转约束(verify 限 QA)+ **数据范围(列表只看可见项目,SYS_ADMIN 不受限)**
- ✅ TAPD CSV 迁移(字段/状态映射 + tapd:id 幂等去重 + 未映射状态报告)
- ✅ **流转历史落库(dt_status_history,含停留时长)+ 操作审计(dt_activity_log)+ 评论(dt_comment)**
- ✅ **缺陷详情(历史时间线+评论+可用流转)/ 看板(按状态分列)/ 统计报表**
- ✅ **迭代管理(增/查/关)+ 燃尽图;缺陷归入迭代**
- ✅ **飞书可交互卡片(我接单/标记已解决)+ 卡片回调处理(URL 验证 + 动作落库)**
- ✅ **密码 BCrypt 哈希;登录态 TokenStore(本地内存 / 生产 Redis 可切换)**
- ✅ **需求 + 测试用例模块;需求-缺陷-用例三向关联(幂等 link/unlink/查询)**
- ✅ **飞书卡片回调签名校验(SHA256,配 encryptKey 生效);API 文档 Swagger UI**
- ✅ **迭代报表(完成率/各状态分布);API Token 存库 SHA-256(明文仅创建时返回一次)**
- ✅ **附件上传(本地文件系统 / MinIO 可切换);操作历史展示;缺陷批量流转;燃尽图理想线**
- ✅ **站内信通知 + @提及 + 邮件通道(日志/SMTP 可切换);缺陷自定义字段;全文搜索;数据大屏**
- ✅ **CI/CD:Dockerfile(多阶段)+ docker-compose 全栈(app/mysql/redis/minio)+ GitHub Actions;前端移动端响应式**
- ✅ **AI 提效:从 PRD 生成测试用例(Anthropic SDK · claude-opus-4-8,无 ANTHROPIC_API_KEY 时启发式兜底)+ 评审入库**
- ✅ **多端标签;版本关联(缺陷修复版本);SLA 超期预警;缺陷查重(2-gram 中文相似)**
- ✅ **单元 + 集成测试(19 个用例:RBAC / 签名 / CSV / SHA256 / 燃尽图+报表 / 用例 / 存储 / @提及 / 端到端)**
- ✅ Vue3 前端(大屏 / 列表+批量 / 看板 / 详情抽屉(关联/附件/操作记录/自定义字段/标签) / 迭代 / 需求 / 用例 / **AI用例** / 通知铃铛 / 响应式)

> 路径:项目在 **`~/devtrack-server`**(已移出 macOS 受保护的 ~/Desktop)。AI 用例默认启发式兜底;`export ANTHROPIC_API_KEY=...` 后走 Claude 真模型。
> 仍待补(对照路线图):执行轮次 / 覆盖率矩阵 / 甘特 / 飞书文档读取 / 观测云日志 / 飞书审批 / 回归用例。

## 测试

```bash
mvn test     # 19 个测试:RBAC / 签名 / CSV / SHA256 / 燃尽图+报表 / 用例 / 本地存储 / @提及 / 端到端集成
```

## 一键全栈(Docker)

```bash
docker compose up -d   # app + MySQL + Redis + MinIO,后端在 :8080(首次会构建镜像)
```
CI 见 `.github/workflows/ci.yml`(后端 `mvn test` + 前端 `npm run build`)。

## 待办(后续期)

- 跨项目行级数据范围再加强(SQL 层拦截器,目前在各入口校验)
- 飞书卡片回调真实应用接入(配置回调地址 + encryptKey);API Token 存库 SHA-256
- 关联在前端缺陷详情/用例页可视化;需求-迭代关联报表
