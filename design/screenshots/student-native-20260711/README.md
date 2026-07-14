# 学生端微信开发者工具真实预览验收记录

验收日期：2026-07-11

项目：学校心理预约小程序学生端

环境：微信开发者工具 Stable v2.01.2510290，Spring Boot + MySQL + Redis 本地真实接口

截图规格：1567 × 1080，均为微信开发者工具完整窗口截图

## 验收结论

- 已完成真实业务闭环：登录 → 咨询师列表 → 咨询师详情 → 选择时段 → 填写预约信息 → 知情同意 → 提交预约 → 预约结果 → 我的预约 → 预约详情 → 取消预约 → 已取消详情 → 紧急求助。
- 创建与取消均调用真实后端接口。数据库确认预约状态为 `CANCELED_BY_STUDENT`，关联时段恢复为 `AVAILABLE`，测试学生有效预约数为 0。
- 清空历史日志并重新编译后，微信开发者工具控制台为 `0 errors / 0 warnings`。
- 登录失效场景已复测：后端返回 401/403 时清理会话并只执行一次登录页跳转，不再停留在空白业务页。
- 取消弹层已修复：补充说明出现后，底部“暂不取消 / 确认取消”操作区仍固定可见。
- 紧急求助页仅验证页面展示和跳转，未拨打任何热线。

## 截图目录

| 序号 | 页面/状态 | 截图 | 验收要点 |
| --- | --- | --- | --- |
| 01 | 学生登录 | [01-login-devtools.png](./01-login-devtools.png) | 表单、密码输入、登录反馈、失效跳转 |
| 02 | 咨询师列表/预约首页 | [02-counselor-home-devtools.png](./02-counselor-home-devtools.png) | 真实咨询师数据、加载完成、列表入口 |
| 03 | 咨询师详情 | [03-counselor-detail-devtools.png](./03-counselor-detail-devtools.png) | 简介、擅长方向、可约日期及时段 |
| 04 | 已选择时段 | [04-slot-selected-devtools.png](./04-slot-selected-devtools.png) | 选择态、日期与时段反馈、下一步按钮 |
| 05 | 预约信息填写 | [05-booking-form-devtools.png](./05-booking-form-devtools.png) | 主题、紧急程度、首次咨询、联系偏好 |
| 06 | 知情同意上半页 | [06-consent-top-devtools.png](./06-consent-top-devtools.png) | 协议标题、主要条款、阅读结构 |
| 06B | 知情同意底部 | [06b-consent-bottom-devtools.png](./06b-consent-bottom-devtools.png) | 勾选同意、提交预约入口 |
| 07 | 预约提交结果 | [07-booking-result-devtools.png](./07-booking-result-devtools.png) | 真实预约号、时间、咨询师、后续入口 |
| 08 | 我的预约列表 | [08-my-appointments-devtools.png](./08-my-appointments-devtools.png) | 预约数据、状态标签、详情跳转 |
| 09 | 预约详情 | [09-appointment-detail-devtools.png](./09-appointment-detail-devtools.png) | 完整预约字段、取消入口、紧急求助入口 |
| 10 | 取消预约弹层 | [10-cancel-dialog-devtools.png](./10-cancel-dialog-devtools.png) | 原因选择、按钮固定、不可撤回提示 |
| 11 | 紧急求助 | [11-emergency-devtools.png](./11-emergency-devtools.png) | 校内支持、热线信息、紧急状态分层 |
| 12 | 已取消详情 | [12-appointment-canceled-devtools.png](./12-appointment-canceled-devtools.png) | 取消原因、取消时间、时段重新开放提示 |

## 视觉审核观察

当前页面已经形成统一的暖白底、鼠尾草绿主色、珊瑚红风险提示和细线呼吸图案体系。图案使用较克制，主要承担层级与情绪缓冲，不会遮挡关键信息；页面也没有大面积渐变、悬浮卡片堆叠或过度圆角。

仍有三项可在下一轮统一优化：

1. `APPOINTMENT DETAIL`、`INFORMED CONSENT` 等英文眉题出现频率较高，容易带来模板化观感。建议统一改为更自然的中文短标题，或只在少量关键页面保留英文辅助标识。
2. 当前联调咨询师名称、校区和房间为较长英文测试数据，视觉上比真实中文数据拥挤。现有省略处理可防止溢出，但正式验收应再用真实中文数据复拍。
3. 部分辅助文字字号和对比度偏克制。开发者工具 75% 模拟比例下略显细小，发布前应在至少一台 375px 宽真机上检查可读性与触控区域。

本记录确认的是页面风格一致性和真实业务可用性。要判断与原型是否达到像素级一比一，需要将原型导出图与上述截图按同一设备尺寸并排或叠图比较。
