# 侦探助手 (Detective Helper)

一个帮助侦探整理案件信息的桌面应用程序，提供案件书管理、事件记录、画布编辑和关系图谱等功能。

## 项目结构

```
detective-helper/
├── backend/           # Spring Boot后端模块
├── desktop/          # JavaFX桌面应用模块
├── frontend/         # Vue.js前端模块
└── pom.xml           # Maven父项目配置
```

## 功能特性

### 后端模块
- RESTful API服务
- SQLite数据库支持
- 案件书、页面、事件管理
- 关系图谱数据存储
- 文件上传处理

### 桌面模块
- JavaFX桌面应用
- 内嵌后端服务器
- 画布编辑器
- 事件时间线编辑器
- 关系图谱查看器

### 前端模块
- Vue.js 3 + TypeScript
- Ant Design Vue UI组件库
- 响应式设计
- 案件书架管理
- 工作空间编辑
- 关系图谱可视化

## 技术栈

### 后端
- Java 21
- Spring Boot 3.3.4
- SQLite JDBC
- Jackson JSON处理
- Maven构建工具

### 桌面
- JavaFX 21
- Spring Boot集成
- WebView内嵌前端
- 端口自动分配

### 前端
- Vue.js 3
- TypeScript
- Vite构建工具
- Ant Design Vue
- Pinia状态管理
- Vue Router路由管理
- Axios HTTP客户端

## 快速开始

### 环境要求
- JDK 21+
- Node.js 20+
- Maven 3.8+

### 安装依赖

```bash
# 克隆项目
git clone https://github.com/yourusername/detective-helper.git
cd detective-helper

# 安装后端依赖
cd backend
mvn clean install

# 安装桌面依赖
cd ../desktop
mvn clean install

# 安装前端依赖
cd ../frontend
npm install
```

### 运行项目

#### 运行后端
```bash
cd backend
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

#### 运行桌面应用
```bash
cd desktop
mvn javafx:run
```

#### 运行前端开发服务器
```bash
cd frontend
npm run dev
```

前端开发服务器将在 `http://localhost:5173` 启动。

### 构建项目

```bash
# 在项目根目录
mvn clean package

# 前端单独构建
cd frontend
npm run build
```

## 项目模块说明

### 后端模块 (backend/)
提供RESTful API服务，处理业务逻辑和数据存储。

### 桌面模块 (desktop/)
JavaFX桌面应用，内嵌后端服务器和前端界面。

### 前端模块 (frontend/)
Vue.js单页应用，提供用户界面和交互功能。

## 数据库结构

项目使用SQLite数据库，主要包含以下表：

- `books` - 案件书表
- `pages` - 页面表
- `events` - 事件表
- `relation_graphs` - 关系图表

数据库文件位于 `./data/detective-helper.db`。

## API文档

### 案件书API
- `GET /api/books` - 获取所有案件书
- `POST /api/books` - 创建案件书
- `GET /api/books/{id}` - 获取案件书详情
- `PUT /api/books/{id}` - 更新案件书
- `DELETE /api/books/{id}` - 删除案件书

### 页面API
- `GET /api/pages/book/{bookId}` - 获取案件书的所有页面
- `POST /api/pages` - 创建页面
- `PUT /api/pages/{id}` - 更新页面
- `DELETE /api/pages/{id}` - 删除页面

### 事件API
- `GET /api/events/page/{pageId}` - 获取页面的所有事件
- `POST /api/events` - 创建事件
- `PUT /api/events/{id}` - 更新事件
- `DELETE /api/events/{id}` - 删除事件

### 关系图API
- `GET /api/relation-graphs/book/{bookId}` - 获取案件书的所有关系图
- `POST /api/relation-graphs` - 创建关系图
- `POST /api/relation-graphs/extract` - 自动提取关系图
- `PUT /api/relation-graphs/{id}` - 更新关系图
- `DELETE /api/relation-graphs/{id}` - 删除关系图

## 开发指南

### 添加新的功能模块

1. 在后端创建对应的Controller、Service、Repository
2. 在前端创建对应的API接口
3. 创建前端组件和页面
4. 更新路由配置

### 数据库迁移

修改 `backend/src/main/resources/schema.sql` 文件来更新数据库结构。

### 样式定制

修改 `frontend/src/styles/` 目录下的样式文件。

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request！

## 联系方式

如有问题，请联系项目维护者。