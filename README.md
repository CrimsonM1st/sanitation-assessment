# 智慧环卫考评系统

## 项目简介

这是一个基于 Spring Boot 开发的智慧环卫考评系统后端项目。

项目用于实现环卫考评任务的创建、查询、状态更新和统计分析，并围绕数据库事务、Redis 缓存、并发控制和 Docker 部署进行实践。

目前已实现的主要功能：

- 考评任务创建
- 根据 ID 查询任务
- 分页与条件查询
- 任务状态及分数更新
- 参数校验与全局异常处理
- MySQL 数据持久化
- 乐观锁并发控制
- 操作审计日志
- Redis 数据缓存
- 缓存穿透防护
- 缓存击穿防护
- 随机 TTL 缓解缓存雪崩
- Docker Compose 一键部署

## 技术栈

| 技术              | 用途                        |
|-------------------|-----------------------------|
| Java 21           | 项目开发语言                |
| Spring Boot 3.5   | Web 应用开发框架            |
| Spring MVC        | REST 接口开发               |
| Spring Validation | 请求参数校验                |
| MyBatis-Plus      | 数据库访问与分页            |
| MySQL 8           | 业务数据持久化              |
| Redis 7           | 数据缓存与分布式锁          |
| H2                | 自动化测试数据库            |
| JUnit 5           | 单元测试与集成测试          |
| Mockito           | Mock 测试                   |
| Maven             | 项目构建与依赖管理          |
| Docker            | 应用容器化                  |
| Docker Compose    | 应用、MySQL、Redis 服务编排 |

## 项目结构

```text
src/main/java/com/example/sanitationassessment
├── cache          Redis 缓存组件
├── common         通用返回对象
├── config         项目配置
├── controller     HTTP 接口层
├── domain         领域对象
├── dto            请求参数对象
├── entity         数据库实体
├── event          Spring 业务事件与监听器
├── exception      业务异常与全局异常处理
├── lock           Redis 分布式锁
├── mapper         MyBatis-Plus Mapper
├── service        业务逻辑
└── vo             响应包装对象
```

## 核心业务规则

当前考评任务支持以下状态：

```text
PENDING
PROCESSING
COMPLETED
CANCELLED
```

主要业务规则：

- `COMPLETED` 状态必须提供分数。
- 分数必须在 0～100 之间。
- `PENDING` 和 `PROCESSING` 状态不应提供分数。
- 已完成任务不允许回退到其他状态。
- 状态更新使用乐观锁防止并发覆盖。

## 本地运行

### 环境要求

本地运行前需要准备：

- JDK 21
- Maven 3.9 或更高版本
- MySQL 8
- Redis 7

### 初始化数据库

创建数据库：

```sql
CREATE DATABASE sanitation
    DEFAULT CHARACTER SET utf8mb4;
```

然后执行：

```text
sql/schema.sql
```

该脚本会创建：

```text
assessment_task
assessment_task_audit_log
```

### 环境变量

项目支持通过环境变量修改配置。

| 环境变量      | 说明                 | 默认值                                   |
|---------------|----------------------|------------------------------------------|
| `SERVER_PORT` | Spring Boot 服务端口 | `8080`                                   |
| `DB_URL`      | MySQL JDBC 地址      | `jdbc:mysql://localhost:3306/sanitation` |
| `DB_USERNAME` | MySQL 用户名         | `root`                                   |
| `DB_PASSWORD` | MySQL 密码           | 空                                       |
| `REDIS_HOST`  | Redis 主机地址       | `localhost`                              |
| `REDIS_PORT`  | Redis 服务端口       | `6379`                                   |

请勿在代码、README 或 Git 仓库中提交真实密码。

### 启动项目

可以在 IntelliJ IDEA 中启动：

```text
SanitationAssessmentApplication
```

也可以打包后运行：

```bash
mvn clean package
java -jar target/sanitation-assessment-0.0.1-SNAPSHOT.jar
```

默认访问地址：

```text
http://localhost:8080
```

## Docker Compose 部署

### 准备环境变量

复制示例配置：

```text
.env.example → .env
```

修改 `.env` 中的数据库密码：

```dotenv
MYSQL_ROOT_PASSWORD=请设置root密码
MYSQL_DATABASE=sanitation
MYSQL_USER=sanitation_app
MYSQL_PASSWORD=请设置应用密码
APP_PORT=8080
```

`.env` 包含本地密码，不应提交到 Git 仓库。

### 打包项目

Dockerfile 会复制已经打包完成的 JAR，因此启动 Compose 前需要执行：

```bash
mvn clean package
```

预期生成：

```text
target/sanitation-assessment-0.0.1-SNAPSHOT.jar
```

### 检查 Compose 配置

```bash
docker compose config
```

### 启动全部服务

```bash
docker compose up -d --build
```

Compose 会启动：

- Spring Boot 应用
- MySQL 8
- Redis 7

查看容器状态：

```bash
docker compose ps
```

查看应用日志：

```bash
docker compose logs -f app
```

### 停止服务

```bash
docker compose down
```

该命令会删除容器和网络，但保留 MySQL、Redis 数据卷。

### 删除服务及数据

```bash
docker compose down -v
```

> 警告：`-v` 会删除 MySQL 和 Redis 数据卷，其中的业务数据将无法通过普通重启恢复。

## 接口说明

### 创建考评任务

```http
POST /assessment-tasks
Content-Type: application/json
```

请求示例：

```json
{
  "departmentName": "环卫一部",
  "status": "PROCESSING",
  "score": null
}
```

响应示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "departmentName": "环卫一部",
    "status": "PROCESSING",
    "score": null
  }
}
```

### 根据 ID 查询任务

```http
GET /assessment-tasks/{id}
```

示例：

```http
GET /assessment-tasks/1
```

### 分页与条件查询

```http
GET /assessment-tasks
```

支持的查询参数：

| 参数             | 说明     |
|------------------|----------|
| `departmentName` | 部门名称 |
| `status`         | 任务状态 |
| `pageNum`        | 页码     |
| `pageSize`       | 每页数量 |

示例：

```http
GET /assessment-tasks?status=PROCESSING&pageNum=1&pageSize=10
```

### 更新任务状态

```http
PUT /assessment-tasks/{id}/status
Content-Type: application/json
```

请求示例：

```json
{
  "status": "COMPLETED",
  "score": 90
}
```

## Redis 缓存设计

项目使用 Cache Aside 模式。

### 查询流程

```text
查询 Redis
→ 缓存命中则直接返回
→ 缓存未命中则查询 MySQL
→ 将查询结果写入 Redis
→ 返回结果
```

### 更新流程

```text
更新 MySQL
→ 数据库事务成功提交
→ 发布任务变更事件
→ 监听器删除 Redis 缓存
```

缓存删除发生在事务成功提交之后，避免事务未提交时并发请求把数据库旧值重新写入缓存。

### 缓存穿透

当数据库中不存在某个任务时，Redis 会短时间保存空值标记。

```text
正常数据 TTL：约 10～12 分钟
空值缓存 TTL：约 1 分钟
```

这样可以防止同一个不存在的 ID 被反复查询数据库。

### 缓存击穿

热点缓存失效时，项目使用 Redis 互斥锁限制数据库查询。

处理流程：

```text
缓存未命中
→ 尝试获取 Redis 锁
→ 获得锁后再次检查缓存
→ 仍未命中才查询数据库
→ 重建缓存
→ 使用 Lua 安全释放锁
```

锁竞争失败时会进行有限次数的等待和重试。最终仍无法获得锁时返回：

```text
HTTP 503 Service Unavailable
```

### 缓存雪崩

正常缓存会在基础 TTL 上增加随机时间，避免大量 Key 在同一时间集中失效。

## 并发控制

### 乐观锁

任务表包含：

```text
version
```

更新时会将版本号加入 SQL 条件。

如果数据已被其他请求修改，当前更新影响行数为 0，接口返回：

```text
HTTP 409 Conflict
```

### Redis 分布式锁

加锁使用的核心语义：

```text
SET key token NX EX timeout
```

释放锁使用 Lua 脚本原子完成：

```text
比较 token
→ token 属于当前请求才删除锁
```

这样可以避免旧请求误删其他请求后来获得的锁。

## 事务与审计日志

创建任务时会同时写入：

```text
assessment_task
assessment_task_audit_log
```

两个操作位于同一个数据库事务中。

如果审计日志写入失败，任务创建也会回滚，以保证数据一致性。

## 自动化测试

运行全部测试：

```bash
mvn test
```

当前测试结果：

```text
Tests run: 55
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

测试覆盖内容包括：

- 参数校验
- Controller 接口
- 业务规则
- 分页查询
- 状态更新
- 全局异常处理
- 数据库事务回滚
- 乐观锁冲突
- Redis 缓存命中与未命中
- 空值缓存
- Redis 分布式锁
- 缓存重建有限重试
- Spring 事务事件监听

## 后续计划

- [ ] 用户登录与 JWT 认证
- [ ] 角色及权限管理
- [ ] Excel 导入导出
- [ ] 文件上传
- [ ] 接口限流
- [ ] 消息通知
- [ ] 部署到云服务器