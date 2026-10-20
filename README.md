# MediaDesk

面向课程小组与小型内容团队的媒体审阅与交付平台。目标流程是上传素材、生成预览、时间点批注、上传新版本、审核与限时交付。

当前已有项目开发约定、设计基线与 Spring Boot 后端最小骨架。2026-10-20 用户完成本机构建、可执行 JAR 启动及 `/api/ping` 成功响应验证；数据库接入、登录和业务接口尚未实现，M0 尚未整体验收。实际进度、验证情况和下一步见 [HANDOFF.md](HANDOFF.md)。

## 文档入口

| 文件 | 用途 |
| --- | --- |
| [AGENTS.md](AGENTS.md) | AI 开工入口、必须遵守的边界与工作顺序 |
| [HANDOFF.md](HANDOFF.md) | 当前状态、证据与一个接续点 |
| [产品与阶段](docs/PRODUCT.md) | 场景、范围、权限、M0–M7 的进入与退出条件 |
| [架构与不变量](docs/ARCHITECTURE.md) | 状态归属、事务、上传、恢复、鉴权及扩展边界 |
| [开发规则与命令](docs/DEVELOPMENT.md) | API、编码、依赖、配置、已验证命令与部署规则 |
| [质量与证据](docs/QUALITY.md) | 完成定义、测试矩阵、故障实验与真实性要求 |
| [学习协作](docs/LEARNING.md) | 核心能力、AI 分工、学习停止线和节奏 |
| [设计决策](docs/decisions/README.md) | 重要取舍、变更流程与 ADR 模板 |

新对话先读 AGENTS 与 HANDOFF，再按任务读取相应文档。日常只更新受影响的规则与状态，无需每次通读或重写全部文件。

## 开始开发

后端构建、启动命令和验证边界见 [DEVELOPMENT 6](docs/DEVELOPMENT.md#6-已验证命令登记)。当前以本地任务分支开发、验证后直接合入 main 为主，具体流程见 DEVELOPMENT 6.3，接续点见 HANDOFF；其余组件仍按阶段引入。

## 来源与成果

开发基线来自用户确认的《MediaDesk 项目规划与实施安排》v1.0（2026-10-18），推导和约束取舍见 [ADR-0001](docs/decisions/0001-project-governance.md)。产品体验参考 Frame.io，后台媒体处理思路参考 PeerTube；当前没有采用它们的代码底座。

后续采用第三方代码、组件或媒体样例时记录来源、许可证与实际修改范围。运行结果、性能数据、演示与个人贡献只按已有证据表述。
