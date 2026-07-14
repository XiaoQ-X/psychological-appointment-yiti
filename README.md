# 学校心理预约一体化系统

面向本校学生的心理咨询预约系统，覆盖学生预约、咨询师处理和学校心理中心后台管理。项目当前处于 MVP 内测准备阶段，核心业务闭环已经实现，尚未进入生产部署。

## 项目结构

| 目录 | 技术栈 | 说明 |
| --- | --- | --- |
| `psych-appointment-miniprogram` | 微信原生小程序 | 学生端与咨询师端，共用一个小程序 AppID |
| `psych-appointment-admin-web` | React 19、TypeScript、Vite | 学校心理中心后台管理端 |
| `psych-appointment-backend` | Java 17、Spring Boot、MySQL、Redis | 认证、排班、预约、风险审核、咨询记录与审计接口 |

## 已实现功能

- 管理员账号初始化和学生 Excel 批量导入
- 学生、咨询师和管理员三类账号登录
- 咨询师管理、排班模板和可预约时段生成
- 学生查询咨询师、锁定时段、提交预约和取消预约
- 学生预约记录、咨询师预约列表和咨询完成记录
- 后台预约管理和高风险审核、转介、关闭流程
- 动态预约规则版本管理及学生预约限制
- 关键后台操作审计日志和分页筛选
- 学生端紧急求助、知情同意和预约结果页面

## 系统架构

```text
微信原生小程序（学生 / 咨询师）
                  \
                   Spring Boot API ── MySQL
                  /                  └─ Redis
React 后台管理端
```

核心数据和当前预约锁由 MySQL 持久化；Redis 已接入基础设施，供后续缓存和分布式并发控制扩展；Flyway 管理数据库版本。手机号、咨询记录和风险处置正文等敏感字段由后端加密保存。

## 本地环境

- Java 17 和 Maven 3.9+
- Node.js 20+ 和 npm
- Docker Desktop
- 微信开发者工具
- MySQL 8.4、Redis 7.4（可通过 Docker Compose 启动）

## 启动后端

进入后端目录，根据示例创建本地 `.env`，并替换全部示例密码和密钥。`.env` 不会被 Git 跟踪。

```powershell
cd psych-appointment-backend
Copy-Item .env.example .env
docker compose up -d
```

Spring Boot 从当前 Shell 或 IDE 启动配置读取环境变量。详细配置和接口示例见 [`psych-appointment-backend/README.md`](psych-appointment-backend/README.md)。

```powershell
mvn spring-boot:run
```

默认后端地址为 `http://localhost:8080`。项目本地联调也可通过 `SERVER_PORT=18080` 使用 `http://localhost:18080`。

## 启动管理端

```powershell
cd psych-appointment-admin-web
npm ci
npm run dev
```

默认访问 `http://localhost:5173`，开发服务器会将 `/api` 代理到 `http://localhost:18080`。

## 导入微信小程序

使用微信开发者工具导入 `psych-appointment-miniprogram`。本地调试默认请求 `http://localhost:18080`，真机联调时需改为可访问的 HTTPS 后端域名或局域网地址。

详细说明见 [`psych-appointment-miniprogram/README.md`](psych-appointment-miniprogram/README.md)。微信 `AppSecret` 只能配置在后端安全环境中，禁止写入小程序代码或提交到仓库。

## 质量检查

后端测试：

```powershell
cd psych-appointment-backend
mvn test
```

管理端检查：

```powershell
cd psych-appointment-admin-web
npm ci
npm run lint
npm run build
```

小程序 JavaScript 语法检查：

```powershell
cd psych-appointment-miniprogram
Get-ChildItem -Recurse -Filter *.js | ForEach-Object { node --check $_.FullName }
```

## 上线前部署

生产 Profile、Docker Compose 模板和发布检查表位于 [`deploy`](deploy/README.md)。模板不会携带真实域名、证书、密钥或学校数据，也不会自动发布到生产环境。

```powershell
cd deploy
Copy-Item production.env.example .env
docker compose --env-file .env -f docker-compose.prod.yml config
```

正式发布前逐项完成 [`deploy/PRE_RELEASE_CHECKLIST.md`](deploy/PRE_RELEASE_CHECKLIST.md)。

## 产品文档

- [竞品调研](心理预约微信小程序竞品调研.md)
- [PRD、页面原型与数据库设计](学校心理预约小程序PRD_页面原型_数据库设计.md)
- [学生端页面原型与流转图](学生端页面原型设计稿说明_页面流转图.md)

## 安全与隐私

- 禁止提交 `.env`、AppSecret、数据库密码、Token 密钥和真实学生数据。
- 禁止将真实学生 Excel、CSV、数据库备份、咨询记录或日志提交到仓库。
- 生产环境必须使用 HTTPS、独立强密钥、最小权限数据库账号和可信反向代理配置。
- 仓库中的账号、密码和密钥示例仅用于说明，部署前必须替换。

## 当前阶段

核心 MVP、三端主要业务闭环、最终视觉风格延展和生产部署骨架已完成。当前处于上线前验收阶段，尚需确定正式域名与证书、学校联系人、微信隐私合规材料，完成标准安全扫描、体验版真机回归和内测发布审批。
