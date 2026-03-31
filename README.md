# HRM Demo

一个基于 Spring Boot 的人事一体化演示系统，包含员工、部门、考勤、请假、绩效、薪酬、权限与系统设置等模块。前端为 Vue 单页应用，后端提供 REST API，最终以静态资源方式由后端统一提供访问。

## 运行环境
- JDK 21
- Maven 3.8+
- MySQL 8+
- Node.js 18+（仅用于构建前端静态资源）

## 启动前检查
- MySQL 已启动，且已创建 `hrm` 数据库
- `application.yml` 中数据库账号密码已正确填写
- 首次运行需执行前端构建，将 `frontend/dist` 拷贝到 `src/main/resources/static`

## 快速开始

1. 创建数据库并导入结构
- 数据库名：`hrm`
- 执行脚本：`hrm.sql`
- 修改 `src/main/resources/application.yml` 中的 `spring.datasource` 为本机账号密码

2. 构建前端（只需首次或前端有改动时执行）

```powershell
cd d:\system\demo\frontend
npm install
npm run build
Copy-Item -Recurse -Force d:\system\demo\frontend\dist\* d:\system\demo\src\main\resources\static
```

3. 构建与运行后端

使用 IDE 直接运行 `com.hrm.HrmApplication`，或在命令行执行：

```powershell
mvn -q -DskipTests package
java -jar target/demo-*.jar
```

4. 访问系统
- 首页：`http://localhost:8080/`
- 登录页：`http://localhost:8080/login`

> 可选：使用脚本一键构建与启动
> - 开发模式（前后端分离，自动打开 `http://localhost:5173`）：`scripts/run-dev.ps1`
> - 生产模式（前端打包进后端，访问 `http://localhost:8080`）：`scripts/build-and-run.ps1`

## 默认账号
如数据库中已有管理员账号，请使用已有账号登录。若没有可通过登录页注册管理员账号（首次注册默认 ADMIN 角色）。

> 提示：若曾因权限问题导致接口 403，请清除浏览器 LocalStorage 中的 `hrm_token` 后重新登录。

## 模块功能
- 员工管理：新增/编辑员工、查看员工列表
- 部门管理：新增/编辑部门、设置负责人
- 考勤管理：考勤记录录入、更新、列表
- 请假管理：请假申请、审批、列表
- 绩效管理：绩效周期、指标、评审、审批流
- 薪酬管理：薪资计算、税前/税后、规则配置
- 权限管理：用户/角色/权限配置
- 系统设置：请假规则、绩效等级、税率区间
- 组织架构：职级、岗位、晋升路径

## 数据初始化
项目启动时**不会**自动初始化演示数据。

手动初始化方式：
- 页面入口：系统设置 -> 一键初始化演示数据
- 接口入口：`POST /api/demo/init`（需要 `config:manage` 权限）

## 常见问题

1. `Request failed` / 接口 403
- 删除浏览器 LocalStorage 的 `hrm_token` 并重新登录
- 确保数据库有权限数据与管理员角色

2. `/performance` 或 `/payroll` 打开提示静态资源不存在
- 请确保已执行前端构建并把 `frontend/dist` 拷贝到 `src/main/resources/static`

3. 启动报 SQL 错误
- 检查是否缺失表或字段（项目内含自动修复逻辑）
- 检查数据库权限与连接配置

4. Maven 报 `JAVA_HOME` 未设置
- 使用 IDE 运行，或在系统环境变量中设置 `JAVA_HOME`

## 结构说明
- `src/main/java`：后端源码
- `src/main/resources/static`：前端静态资源
- `src/main/resources/application.yml`：配置文件
- `hrm.sql`：数据库结构参考
 - `frontend`：前端 Vue 工程源码

## 手动验收清单（建议）
- 登录/注册是否正常
- 部门列表、员工列表是否有数据
- 新增/编辑员工、部门是否成功
- 考勤/请假/绩效/薪酬的新增与列表是否成功
- 系统设置规则是否可保存与加载

---
