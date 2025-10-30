# 数据下载管理系统

一个基于 Vue 3 + Vite + Element Plus 的现代化数据下载管理前端应用。

## 功能特性

### 1. 数据查询
- 支持按国家代码、下载类型、任务类型查询数据
- 可设置查询数量限制
- 实时显示查询结果

### 2. 单类型下载
- 生成单一条件的下载链接
- 支持 TXT 和 Excel 两种格式
- 多种任务类型选择（WS验证、iOS验证、RCS验证、性别识别）

### 3. 多条件组合下载
- 支持多个条件组合筛选
- 年龄范围筛选（最小年龄-最大年龄）
- 性别筛选
- 双任务类型组合
- 皮肤排除选项

## 技术栈

- **框架**: Vue 3 (Composition API)
- **构建工具**: Vite
- **UI组件库**: Element Plus
- **HTTP客户端**: Axios
- **图标**: Element Plus Icons

## 项目结构

```
ts-download-app/
├── src/
│   ├── api/                  # API接口
│   │   ├── request.js       # Axios封装
│   │   └── download.js      # 下载相关接口
│   ├── components/          # 组件
│   │   └── DownloadPage.vue # 主页面组件
│   ├── App.vue              # 根组件
│   ├── main.js              # 入口文件
│   └── style.css            # 全局样式
├── index.html               # HTML模板
├── vite.config.js           # Vite配置
├── package.json             # 项目依赖
└── README.md                # 项目说明
```

## 安装和运行

### 1. 安装依赖

```bash
npm install
```

### 2. 配置后端接口

在 `vite.config.js` 中修改代理配置，将 `target` 改为你的后端地址：

```javascript
proxy: {
  '/api': {
    target: 'http://your-backend-url.com',  // 修改为实际后端地址
    changeOrigin: true
  }
}
```

### 3. 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 4. 构建生产版本

```bash
npm run build
```

构建产物将生成在 `dist` 目录。

## API 接口说明

### 查询接口
- **路径**: `/api/download/query`
- **方法**: POST
- **参数**:
  - `countryCode`: 国家代码（如 "US"）
  - `downloadType`: 下载类型（"txt" 或 "excel"）
  - `limit`: 限制数量
  - `taskType`: 任务类型（"wsValid", "iosValid", "rcsValid", "gender"）

### 单类型下载接口
- **路径**: `/api/download/generateDownloadUrl`
- **方法**: POST
- **参数**: 同查询接口

### 多条件下载接口
- **路径**: `/api/download/mergeDownload`
- **方法**: POST
- **参数**:
  - `countryCode`: 国家代码
  - `excludeSkin`: 排除皮肤（1-是，2-否）
  - `firstTaskType`: 第一任务类型
  - `secondTaskType`: 第二任务类型
  - `limit`: 限制数量
  - `maxAge`: 最大年龄
  - `minAge`: 最小年龄
  - `sex`: 性别（0-未知，1-男，2-女）

## 特性说明

### 无超时限制
所有接口请求都没有设置超时时间（timeout: 0），因为下载操作可能需要较长时间。

### Cookie认证
请求配置了 `withCredentials: true`，支持携带 Cookie 进行身份验证。

### 错误处理
- 统一的错误拦截和提示
- 友好的用户提示信息
- 详细的控制台错误日志

### 响应式设计
- 支持桌面端和移动端
- 自适应布局
- 优雅的UI交互

## 开发说明

### 添加新的任务类型

在 `DownloadPage.vue` 中的 `el-select` 组件中添加新的选项：

```vue
<el-option label="新任务类型" value="newTaskType" />
```

### 修改默认值

在 `DownloadPage.vue` 的 `reactive` 定义中修改表单默认值。

### 自定义样式

在 `src/style.css` 中修改全局样式，或在组件的 `<style scoped>` 中添加组件样式。

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88

## License

MIT
