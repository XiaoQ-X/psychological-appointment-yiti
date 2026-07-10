# 学校心理预约后台管理端

React + TypeScript + Vite 管理端，通过 Spring Boot 真实接口完成管理员登录、学生导入、咨询师管理、排班时段、预约管理、高风险审核、预约规则管理和审计日志查询。

## 本地运行

后端默认监听 `http://localhost:18080`，前端开发代理配置在 `vite.config.ts`。

```powershell
npm install
npm run dev
```

默认访问地址：`http://localhost:5173`。

## 质量检查

```powershell
npm run lint
npm run build
```

## 新增管理能力

- 预约规则采用草稿、启用、历史版本机制，已发布版本不可修改。
- 启用规则后，学生端可约窗口、锁定时长、取消提前量和次数上限立即生效。
- 审计日志支持按操作、对象、敏感级别、操作账号和日期分页筛选。
- 审计详情仅展示业务元数据，不展示密码、咨询正文和风险处置备注。
