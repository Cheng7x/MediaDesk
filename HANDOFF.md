# 当前交接

更新日期：2026-10-27（Asia/Singapore）

## 当前阶段

- M0-1 已交付；M0 首批契约、存储/环境短验证仍未整体验收。当前进入 M1 身份功能的前置实践，不将 local 登录示例标作阶段完成。
- 用户已亲手写完 MyBatis 用户查询、密码编码和 Spring Security 本地表单登录代码；2026-10-27 提交前已通过干净编译与打包，见 DEVELOPMENT 6.5。登录运行结果仍待验证。
- 空间/成员契约已有设计基线，尚无业务实现；正式登录/注销/CSRF、邀请、Asset/AssetVersion、迁移及前端仍待推进。路线见 [PRODUCT 4.1](docs/PRODUCT.md#41-当前实践到首个业务闭环)。

## 已知事实与验证边界

| 事项 | 当前事实与证据 |
| --- | --- |
| 提交范围 | 按用户要求在 main 收口一次本地提交，包含数据库接入、本地登录、成员契约、项目编辑器设置及同步文档；不推送 |
| 用户查询 | UserAccount 无参构造和属性方法齐全；UserMapper 的 @Mapper 已移到接口，账号查询用参数绑定与列别名 |
| 认证代码 | PasswordConfig、DatabaseUserDetailsService、LocalSecurityConfig、DevIdentityController 已存在，职责与 DEVELOPMENT 6.4 一致；未发现阻塞性的静态问题，不等同运行验证通过 |
| 本地 HTTP | 使用 127.0.0.1:8081；local 下诊断接口 SELECT 1，/dev/me 受保护；内置 /login 表单，默认 Session/CSRF/注销；正式 JSON 身份 API 尚未写 |
| 数据库证据 | 用户提供 MySQL 服务端 8.0.46、Connector/J 9.7.0、SSL yes 的 IDEA Test Connection 输出；三表隔离 SQL 查询、约束拒绝和回滚已反馈符合预期 |
| 配置排错 | 用户通过断点发现密码环境变量名前导空格，修复后要求继续；AI 未读取真实凭据或请求数据库 |
| 构建/运行 | 当前认证版本已用缓存 Maven 3.10.0 完成 clean verify（BUILD SUCCESS），10 个主源码与 1 个演示源码编译成功，JAR 不含 PasswordHashDemo。演示用户插入、正确/错误密码、Session 保持和注销仍待运行反馈 |
| 构建入口限制 | 本次受限执行中的 Wrapper 报 Cannot index into a null array；直接调用同版本缓存 Maven 并获准下载依赖后构建通过。未修改 Wrapper，不能据替代命令成功认定该入口已修复 |
| 自动测试 | 当前无自动化测试用例；src/test/java 中的 PasswordHashDemo 只是独立 main，本次仅编译，不能算功能测试通过 |
| 已完成清理 | 用户已将 PasswordHashDemo 移到 src/test/java 同包，两个无用导入也已清理；AI 核对并同步此前过时的待办 |
| 其他能力 | 空间事务、资源授权、迁移、前端、上传、处理、MQ、Redis 尚未实现；不据登录代码认定这些能力成立 |

## 本轮变更

- 同步 README、PRODUCT、ARCHITECTURE、DEVELOPMENT、QUALITY、LEARNING 与本文件，修正“尚无数据库/登录代码”等过时描述，保留历史实验的证据边界。
- 固定下一步 GET /api/me 的输入输出、稳定用户 ID、白名单 DTO、no-store 和 API JSON 401；这些新代码仍由用户编写。
- 本次提交前核对源码与配置、运行干净构建、检查应用包，并同步演示类位置及验证边界；未启动应用或操作数据库。

## 一个接续点

**用户实现 [DEVELOPMENT 3.4](docs/DEVELOPMENT.md) 的身份 API：扩展 MediaDeskPrincipal，调整 UserDetailsService 返回类型，编写 MeResponse/MeController 和 API AuthenticationEntryPoint，并在 local 安全链中为 API 请求选择 JSON 401。** 沿用现有表单登录，完成未登录 401 → 登录后返回三个身份字段 → 注销后 401 的一次闭环。随后收口会话/CSRF 与迁移，再进入空间查询/创建事务。

用户亲手改代码，AI 提供足量 Spring 机制、示例、评审和文档；“继续”不是代写授权。云服务、付费调用和公网发布未获授权。
