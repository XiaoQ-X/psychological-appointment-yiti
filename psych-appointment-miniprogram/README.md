# 学校心理预约小程序学生端

微信原生小程序学生端骨架，连接本地后端 `http://localhost:18080`。

## 功能范围

- 学生登录
- 咨询师列表
- 咨询师详情
- 可约时段查询
- 预约表单提交
- 预约成功/待审核结果
- 我的预约列表
- 预约详情
- 学生取消预约
- 紧急求助
- 知情同意详情

## 导入方式

1. 打开微信开发者工具。
2. 选择“导入项目”。
3. 项目目录选择：

```text
D:\项目开发\psych-appointment-miniprogram
```

4. 当前 `project.config.json` 已配置正式 AppID：`wx8b1d65e32bbb4bf8`。
5. 本地联调时在微信开发者工具中关闭“校验合法域名、web-view、TLS 版本以及 HTTPS 证书”。

## 后端地址

默认配置在：

```text
config/index.js
```

```js
module.exports = {
  baseUrl: 'http://localhost:18080',
  defaultLookAheadDays: 14
};
```

如果要在真机预览中访问本机后端，需要把 `localhost` 改成电脑局域网 IP，并确保手机和电脑在同一网络。

## 页面结构

```text
pages/login/login                         学生登录
pages/student/counselors/index            咨询师列表
pages/student/counselor-detail/index      咨询师详情和时段
pages/student/booking-form/index          预约表单
pages/student/booking-result/index        预约成功/待审核结果
pages/student/appointments/index          我的预约
pages/student/appointment-detail/index    预约详情和取消
pages/student/emergency/index             紧急求助
pages/student/consent/index               知情同意详情
```

## 页面原型说明

学生端低保真页面原型说明和页面流转图见：

```text
D:\项目开发\学生端页面原型设计稿说明_页面流转图.md
```

## 联调账号

使用后台管理端导入的本地测试账号。禁止将真实学生账号、初始密码或导入文件写入文档并提交到仓库。

后端需保持运行：

```text
http://localhost:18080
```

## 说明

- 所有请求通过 `utils/request.js` 统一处理。
- 登录 token 存储在微信本地缓存。
- 401 会清理登录态并跳回登录页。
- 预约提交会先调用 slot lock，再提交预约表单。
- 普通预约提交后进入预约成功页，高风险命中后进入待审核提示页。
- 紧急求助页无需依赖登录状态，可从登录页、预约首页、表单和详情进入。
- 学生端不会展示加密主诉明文或咨询师内部咨询记录。
