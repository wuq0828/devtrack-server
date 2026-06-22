# DevTrack Web

DevTrack 缺陷管理系统的前端管理后台。

## 技术栈

- Vue 3 + `<script setup>` + TypeScript
- Vite 构建工具
- Element-Plus 组件库
- Pinia 状态管理
- Vue Router 路由
- axios HTTP 客户端

## 前置条件

后端服务需要先在 **http://localhost:8080** 启动并就绪（健康检查 `GET /devtrack/health`）。
开发环境通过 Vite dev proxy 把 `/devtrack` 代理到 `http://localhost:8080`，因此**无需后端配置 CORS**。

## 快速开始

```bash
# 1. 安装依赖
npm install

# 2. 启动开发服务器（默认 http://localhost:5173）
npm run dev

# 3. 生产构建（会先执行 vue-tsc 类型检查，再用 Vite 打包，产物在 dist/）
npm run build

# 4. 本地预览构建产物
npm run preview
```

## 演示账号

- 用户名：`admin`
- 密码：`admin123`

## 功能

- **登录页** `/login`：用户名密码登录，token 存入 Pinia + localStorage。
- **缺陷列表页** `/defects`（主页面）：
  - 顶部筛选：状态、优先级下拉，关键字搜索。
  - 表格列：编号、标题、状态（彩色 tag）、严重程度、优先级、创建时间。
  - 分页（对接后端 `total / pn / ps`）。
  - 新建缺陷弹窗（标题、描述、严重程度、优先级）。
  - 行内状态流转按钮（按当前状态动态展示可用动作）；`拒绝 / 重新打开` 会弹出输入框要求填写说明。
  - 顶部显示当前登录用户名与退出登录入口。

## 与后端的对接说明

- 所有接口以 `/devtrack` 为前缀，统一响应格式 `{ code, message, data }`，`code === 0` 为成功。
- 登录成功后，后续所有请求自动携带 `Authorization: Bearer <token>` 请求头。
- 响应拦截器统一处理：业务错误（`code !== 0`）弹出 `ElMessage` 错误提示；HTTP 401 自动清理登录态并跳转登录页。
- 演示项目固定 `projectId = 1`。

## 目录结构

```
src/
├── api/            # 接口封装（auth、defect）
├── router/         # 路由与登录守卫
├── store/          # Pinia store（user）
├── types/          # TypeScript 类型定义（DefectDto、各响应体等）
├── utils/          # axios 封装(request.ts) 与枚举/格式化(enums.ts)
├── views/          # 页面（LoginView、DefectListView）
├── styles/         # 全局样式
├── App.vue
└── main.ts
```

## 如需修改后端地址

编辑 `vite.config.ts` 中的 `server.proxy['/devtrack'].target`。
