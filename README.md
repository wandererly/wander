# Wander

Wander 是一个基于 Spring Boot 的轻量在线课程平台，提供用户注册登录、课程管理、选课与学习进度、通知与管理员审计等功能，并配有简单的前端静态页面。

**主要功能**
1. JWT 登录与角色权限（学生 / 教师 / 管理员）
2. 课程与章节管理、选课与学习进度
3. 角色申请与管理员审核
4. 通知中心与管理员审计日志
5. 内置 Swagger API 文档

## 技术栈
1. Spring Boot 3.x
2. Spring Security + JWT
3. Spring Data JPA
4. MySQL / H2
5. Flyway（MySQL 迁移）
6. Swagger（springdoc-openapi）

## 快速开始（默认 H2）
1. 确保本机已安装 JDK 17，并正确设置 `JAVA_HOME`
2. 在项目根目录执行：

```bash
./mvnw spring-boot:run
```

Windows：
```bash
./mvnw.cmd spring-boot:run
```

默认启用 `h2` profile，数据库文件保存在 `./data/wander`。

### 访问入口
1. 首页: `http://localhost:8080/`
2. 登录: `http://localhost:8080/login.html`
3. 注册: `http://localhost:8080/register.html`
4. Swagger: `http://localhost:8080/swagger-ui/index.html`
5. H2 Console: `http://localhost:8080/h2-console`

H2 Console JDBC URL:
`jdbc:h2:file:./data/wander`

## MySQL 运行
设置环境变量并启用 mysql profile：

```bash
SPRING_PROFILES_ACTIVE=mysql
DB_URL=jdbc:mysql://localhost:3306/wander?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=your_password
```

再执行：
```bash
./mvnw spring-boot:run
```

Flyway 会自动执行 `src/main/resources/db/migration` 下的迁移脚本。

### 可选：H2 -> MySQL 迁移
仅在 `mysql` profile 下生效，可通过环境变量控制：

```bash
H2_MIGRATION_ENABLED=true
H2_MIGRATION_URL=jdbc:h2:file:./data/wander_coursehub
H2_MIGRATION_USERNAME=sa
H2_MIGRATION_PASSWORD=
H2_MIGRATION_MARKER=./data/h2_migrated.flag
```

## 默认管理员账号
默认开启开发初始化管理员（仅用于开发环境）：

```text
username: admin
email: admin@qq.com
password: admin12345
```

如需关闭：
```bash
APP_BOOTSTRAP_ADMIN_ENABLED=false
```

## 目录结构
1. `src/main/java` 后端代码
2. `src/main/resources/static` 前端静态页面
3. `src/main/resources/db/migration` Flyway 脚本
4. `data/` 运行时数据（H2 数据库、上传文件）

## 常见问题
1. **启动时报 `JAVA_HOME` 未配置**  
请安装 JDK 17 并正确设置环境变量后再启动。
2. **MySQL 连接失败**  
请检查数据库是否已创建 `wander`，并确认 `DB_URL/DB_USERNAME/DB_PASSWORD` 配置正确。


