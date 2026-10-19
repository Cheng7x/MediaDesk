# 开发规则、环境与命令

本文件是实际环境、运行命令和接口约定的唯一维护位置。目标架构见 [ARCHITECTURE](ARCHITECTURE.md)，验收与证据见 [QUALITY](QUALITY.md)。下列“待验证”均不是已完成事实。

## 1. 工程规模与代码规则

- Java / Spring Boot / MyBatis / MySQL 与 Vue 3 / TypeScript 沿用规划；以当前阶段的最小闭环组织实现。
- 后端按业务分包，包内采用清晰的 Controller / Service / Mapper 等职责；Controller 处理 HTTP 和校验，Service 执行业务及事务，Mapper 处理持久化。
- DTO 与持久化实体分离，不直接向前端暴露数据库实体、密码字段和内部凭据。状态转换集中在可检查的业务入口，不散落到 Controller 和定时器各写一套。
- 使用构造注入、明确类型和成熟库；无实际替换点不为每个类添加接口、工厂、泛型基类或设计模式。
- 错误不能被空 catch、默认成功值或模糊重试掩盖；重试区分暂时失败和永久失败，必须有次数、期限与可见结果。
- 只在有业务价值的事务边界启用事务，核对框架代理调用是否生效。禁止在事务中等待浏览器上传、执行 FFmpeg 或长期等待远程服务。
- 注释解释约束、取舍和失败窗口，避免复述代码。优先修正清晰度和边界，不用无关格式化扩大改动。
- 前端统一请求与错误处理，区分局部/全局状态；不要把所有接口结果塞进全局 store。异步请求处理路由/版本切换与过期结果，状态包含加载、空、失败、重试和无权限。
- 首版用成熟组件库；页面文案面向用户描述可采取的动作，内部异常堆栈和基础设施术语进入诊断记录。

## 2. SQL、数据与迁移

- Flyway 从 M1 管理迁移。已在任何共享或保留环境执行的迁移不原地改写，新建迁移修正；仅明确可丢弃的隔离实验可重置，执行前核对目标。
- 表约束表达唯一性、非空与引用规则；必要的状态/修订号用于条件更新，检查影响行数并映射为明确结果。
- 关键事务、配额竞争和任务认领用真实 MySQL 验证；不能以 H2 或 mock 行为替代 MySQL 事务证据。
- 参数值使用 MyBatis 绑定；不能将不可信输入拼成 SQL。动态排序和列名使用服务端白名单，不允许原样传入 `${...}`。
- 索引从实际查询推导：空间内素材稳定分页、版本、时间点评论、到期上传、Outbox 和可执行任务；记录 SQL 与 EXPLAIN，避免为每个字段机械加索引。
- 列表有稳定排序及唯一键兜底、默认与最大页大小；先普通分页，出现实际大偏移瓶颈后再评估游标。
- 数据库持久时间为 UTC，API 返回带时区格式；容量为字节、视频偏移为毫秒。数据库和接口的 ID、时间精度、枚举及空值约定一致；长整数 ID 在 JavaScript 边界采用安全表示，M0 契约中固定。
- 数据修复、清理、回填和索引重建有范围、幂等性与停止方式；涉及真实数据的不可逆动作先获得明确授权。

### 2.1 M0-2：Membership 字段职责草案

状态：2026-10-19 引导设计中的概念草案，尚无建表、迁移或 Java 实现；不表示首批契约已完成。这里先明确最少业务信息，不固定最终 SQL 字段名、类型或全部列。

| 概念字段 | 记录的事实 | 来源与约束 |
| --- | --- | --- |
| userId | 哪位用户是成员 | 关联 User 的稳定身份；显示名和账号由 User 提供，不在每条成员关系中重复维护 |
| workspaceId | 这条成员关系属于哪个空间 | 关联 Workspace；角色只在该空间内生效 |
| role | 该用户在这个空间的角色 | Owner / Editor / Reviewer，由既定角色规则决定允许的操作 |

成员列表可以关联用户信息与成员关系形成响应，显示名、账号、空间和角色不必全部来自 Membership 一张表。具体显示字段及响应形状在首批接口契约中固定，不直接暴露持久化实体或敏感字段。

三个概念字段是最少业务信息，不代表最终表只能有三列。技术主键、字段类型、引用约束、移除/重新加入语义与查询索引仍需细化。

已确定的约束草案：

- userId、workspaceId 和 role 均必填；具体 SQL 列需声明 NOT NULL。
- userId 与 workspaceId 联合唯一，role 不加入这个唯一组合。同一用户可以加入多个空间，同一空间可以包含多个用户，但同一对用户/空间只对应一条当前成员关系。
- 角色调整修改该成员关系的 role，不通过新增另一条关系来表达。授权、唯一 Owner 保护和并发更新检查仍由相应服务端规则保证，唯一约束本身不授予权限。

以下为建表内部的约束片段示意，列名用 snake_case，对应概念字段 userId、workspaceId；不是完整建表语句，也没有执行：

```sql
UNIQUE (user_id, workspace_id)
```

数据库唯一约束承接并发下的最终防重，不能只用“先查询不存在再插入”替代。重复创建失败如何映射为已是成员、冲突等响应，在邀请/成员接口契约中固定。MySQL 的 UNIQUE 对允许为空的列可接受多个 NULL，因此还需独立声明非空。[MySQL 官方唯一索引说明](https://dev.mysql.com/doc/refman/8.4/en/create-index.html#unique-indexes)支持该机制说明，不代表已经锁定项目数据库版本或通过了约束实验。

### 2.2 M0-2：User 字段职责草案

状态：2026-10-19 引导设计草案，尚未建表、编码或验证登录。用户提出主键、账号、处理后的密码及名称四类信息；当前先明确职责和基本约束，完整类型、长度、迁移及接口表示仍待固定。

| 概念字段 | 用途 | 当前草案 |
| --- | --- | --- |
| id | 供 Membership 等记录稳定引用用户身份 | 暂选 MySQL BIGINT 自增主键，Membership.userId 使用一致的数据库类型；不同时添加没有独立用途的 UUID 字段 |
| account | 登录时定位唯一用户 | 非空且唯一；格式、长度、大小写/空白处理和索引排序规则在首批契约中明确 |
| passwordHash | 保存成熟密码编码器生成的单向密码哈希表示 | 非空；保存编码器需要的完整表示，长度随选定方案核对；不存明文或可解密后还原的密码，不用普通快速摘要替代专用密码哈希 |
| displayName | 成员列表等页面展示名称，对应用户草案中的 name | 非空，可以重复；显示名不承担用户身份或登录唯一性 |

自增整数与 UUID 是不同的 ID 生成方案。本项目当前为模块化单体，用户身份由一个 MySQL 数据库生成已满足需要，因此先采用自增方案；出现跨写入源独立生成等实际需求时再比较替代方案。ID 不作为访问凭证，资源授权仍根据当前成员关系与真实归属执行。主键片段只作设计说明，不是可执行迁移：

```sql
id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY
```

认证的目标是验证输入密码是否匹配，不需要恢复原始密码。实现时使用 Spring Security PasswordEncoder：保存 encode 的完整结果，登录时使用 matches 校验，不自行设计密码算法；选定实现、参数与运行成本在兼容版本确定后验证。请求中的原始密码和持久化 passwordHash 职责分开，响应不返回 passwordHash，日志不记录二者。

机制依据：[MySQL AUTO_INCREMENT](https://dev.mysql.com/doc/refman/8.4/en/example-auto-increment.html)、[Spring Security 密码存储](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)。资料核对不代表已经选择数据库/框架版本，或密码编码和数据库行为已在本项目运行验证。

### 2.3 M0-2：Workspace 字段职责与三表关系草案

状态：2026-10-19 引导设计草案，尚未建表或实现查询。用户提出空间主键、Owner_id 和 memberNum；结合既有 Membership.role 草案，当前选择以下最小职责划分，不代表完整 DDL。

| 信息 | 当前来源 | 选择理由 |
| --- | --- | --- |
| id | Workspace 独立保存，暂选 BIGINT 自增主键 | 稳定标识空间；概念对应用户草案中的 workspace_id，Membership.workspaceId 使用同类型引用 |
| name | Workspace 独立保存，非空 | 对应创建空间的名称输入及空间列表显示；不靠编号辨认空间，当前不要求全站名称唯一 |
| Owner 身份 | 查询该空间中 role 为 Owner 的有效 Membership，取得 userId | 沿用当前成员角色模型，暂不另存独立 ownerId 列，避免同时维护两份所有权事实 |
| memberNum | 聚合该空间的有效 Membership 数量 | 当前不另存人数计数列；避免加入、移除和失败回滚时需同时维护关系与计数，具体有效成员条件随移除语义固定 |

ownerId 是可行的另一种所有权建模方案，但若与 Membership 的 Owner 角色同时保存，必须明确唯一事实来源与一致性约束。当前没有独立所有权或转移需求，先按上述单一来源草案推进；需求改变时重新比较。高频计数查询是否需要持久计数或缓存，应有查询与性能证据再决定。

该草案仍必须保证每个已创建空间有且只有一个有效 Owner，且不能移除或降级唯一 Owner。Membership 的 userId/workspaceId 联合唯一只防同一成员关系重复，不能单独保证每空间只有一个 Owner。Owner 授予仅在空间创建入口发生，邀请与角色调整不授予 Owner；完整数据库/事务约束与并发验证在首批建表和 M1 中落实，不据字段划分认定已经成立。

逻辑引用关系如下；外键约束草案见 2.4，完整主键形式和生命周期契约仍待补齐：

```text
Membership.userId      -> User.id
Membership.workspaceId -> Workspace.id
```

Membership 中的两个引用值使用已有用户/空间的 ID，不分别自增。页面可以关联查询空间及成员资料、聚合人数，响应字段不要求与单张表的持久列一一对应。外键之后还需明确空间创建与 Owner 成员记录需要共同成功的事务边界。

### 2.4 M0-2：Membership 外键约束草案

状态：2026-10-19 讲解与设计片段，未执行。为表达 SQL，当前暂用物理表名 users、workspaces、memberships，分别对应 User、Workspace、Membership；不是已有表清单。

外键承接逻辑引用的存在性约束：Membership.userId 必须对应实际存在的 User.id，workspaceId 必须对应实际存在的 Workspace.id。两个引用列均非空，并与所引用 BIGINT 主键的类型保持一致。物理建表时使用支持外键的 InnoDB，并在真实 MySQL 中验证。

先示范放在 memberships 建表内部的用户引用片段：

```sql
CONSTRAINT fk_membership_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT
```

上述片段表示：被引用用户必须存在；仍有成员记录引用该用户时，不允许物理删除该用户或改动其被引用 ID。当前两个逻辑引用的动作策略均采用 RESTRICT 草案，不用级联删除替代产品的成员移除、隐藏或回收规则。

用户已给出正确的空间引用方向；动作子句经语法修正后为以下片段，仍放在 memberships 建表内部且尚未执行：

```sql
CONSTRAINT fk_membership_workspace
    FOREIGN KEY (workspace_id)
    REFERENCES workspaces(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT
```

动作子句的顺序是 ON DELETE/ON UPDATE 后接 RESTRICT，不是 RESTRAINT ON DELETE/ON UPDATE。这里限制的是被引用的 workspaces 行被物理删除或其引用键 id 被改变；修改不影响该键的 name 等字段不因这条外键被拒绝。移除 memberships 子行不会被这条外键自动禁止，唯一 Owner 保护仍需独立保证。

移除成员是否保留历史、如何重新加入及唯一 Owner 保护仍需明确业务契约，不能由外键动作自动决定。

三类约束各有职责：NOT NULL 防止缺少必要引用，联合唯一防止同一用户/空间成员关系重复，外键防止指向不存在的父记录。外键不授予操作者权限，也不保证空间的 Owner 唯一或必然存在，服务端授权和事务规则仍需独立落实。

预期验证包括：不存在的用户/空间插入被拒绝，合法引用可以建立，引用存在时父行物理删除被拒绝；这些实验尚未运行，实际建表后与并发/回滚一起验证。[MySQL 官方外键说明](https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html)支持机制说明，不代表本机版本已确认或实验通过。

### 2.5 M0-2：创建空间的事务草案

状态：2026-10-19 用户已正确提出两次写入放在同一事务、全部成功后提交；以下整理为设计草案，尚无连接、Java 实现或回滚实验。

业务成功条件是新 Workspace 与创建者的 Owner Membership 同时成立。输入名称先校验；创建者 ID 来自服务端已认证会话，不能信任客户端指定的 ownerId。两次写入使用同一个 MySQL 连接、同一个事务及 InnoDB 表，不允许第一条写入先独立提交。

```text
校验当前登录身份与空间名称
开启事务
写入 Workspace，取得数据库实际生成的 workspaceId
写入 Membership（当前用户 ID，workspaceId，Owner）

如果提交前任一步明确失败：回滚整个事务，返回失败
如果两次写入成功：提交事务，确认提交成功后返回结果
如果提交时连接中断且结果不明：不能宣称一定失败或已回滚，需核对结果
```

workspaceId 不硬编码，也不靠查询 MAX(id) 猜测；后续用选定 MyBatis/JDBC 的生成键机制取回。两次写入必须受同一个有效事务边界管理；框架注解、异常传播和回滚规则在实际实现时验证。事务仅覆盖必要的短数据库操作，不等待上传、转码或远程调用。

数据库语句报错不等于整个事务必然回滚，例如 InnoDB 的重复键错误通常只回滚失败语句。应用必须让整组业务失败进入事务回滚路径，不能吞掉第二次写入异常后继续提交。[MySQL 官方错误处理说明](https://dev.mysql.com/doc/refman/8.4/en/innodb-error-handling.html)支持此区别。

预期验证：成功提交后有一个空间及其一个 Owner；第二次写入被人为制造的约束错误拒绝时，两张表均不留下本次新增记录。提交响应丢失和重复创建请求的核对/防重契约仍待确定，不能把数据库事务视为 HTTP 重试幂等。上述实验均未运行。

### 2.6 M0-2：唯一 Owner 的约束候选

状态：2026-10-19 AI 提供的讲解与约束候选，未建表、未获运行证据，不代表已完成 Owner 生命周期设计。以下片段假定 memberships 每行代表一条当前成员关系；若后续保留失效成员行，需同时调整唯一性和授权条件。

“有且只有一个 Owner”拆成两部分：创建事务共同写入空间和 Owner，保证成功创建时至少有一个；数据库约束限制最多一个。后续成员移除、退出和角色调整必须保护已有 Owner，邀请与角色调整不授予 Owner，第一版不开放所有权转移。

不能用 UNIQUE(workspace_id, role) 实现，因为它也会错误限制每个空间最多一个 Editor 和一个 Reviewer。候选方案用数据库生成列只为 Owner 产生空间 ID，其他角色产生 NULL，再对该列加唯一索引：

```sql
owner_workspace_id BIGINT GENERATED ALWAYS AS (
    CASE WHEN role = 'Owner' THEN workspace_id ELSE NULL END
) VIRTUAL,
CONSTRAINT uk_membership_one_owner UNIQUE (owner_workspace_id)
```

这是建表内部片段，role 的最终类型、合法值与大小写仍需完整 DDL 固定。owner_workspace_id 由数据库从既有字段派生，应用不另外维护；不是另存一份 Owner 用户身份。

推导：同空间的两条 Owner 行都会生成相同空间 ID，唯一索引拒绝第二条；普通成员生成 NULL，MySQL 唯一索引允许多个 NULL，因此普通角色可以有多个成员。并发写入也由数据库唯一性承接，不能只先查询是否已有 Owner。

唯一索引只能保证最多一个，不能禁止删除唯一 Owner 或将其降级；至少一个仍依赖创建事务及后续受保护的业务入口。普通应用写入入口均须维护该规则，不能据这段索引声称所有任意 SQL 都被数据库禁止。

依据：[MySQL 生成列的唯一索引支持](https://dev.mysql.com/doc/refman/8.4/en/create-table-secondary-indexes.html)、[唯一索引的 NULL 规则](https://dev.mysql.com/doc/refman/8.4/en/create-index.html#unique-indexes)。这是结合官方机制推导的项目候选方案；需在选定版本的真实 MySQL 验证第二个 Owner 被拒绝、多个普通成员被允许、不同空间各有 Owner 以及角色更新与并发写入，尚未运行这些实验。

### 2.7 M0-2：首批三表完整 DDL 评审草稿

状态：2026-10-20 AI 汇总用于整体讲解与用户修改的 [SQL 草稿](drafts/m0-2-identity.sql)，未执行，不是 Flyway 迁移。它将 2.1–2.6 的片段组合为完整 CREATE TABLE；下列选值仅为当前可评审候选，未代表首批契约已通过或本机 MySQL 版本已选定。没有连接或修改数据库。

| 选择 | 当前草稿及理由 |
| --- | --- |
| ID | users.id、workspaces.id 为有符号 BIGINT 自增；引用列同类型，便于后续与 Java Long 对齐；接口 ID 表示仍需单独固定 |
| account | VARCHAR(32)，唯一且非空；服务端草案为注册/登录均先去除首尾 ASCII 空格、按 Locale.ROOT 转小写，再校验 `[a-z0-9_]{3,32}`，存规范化值。大小写变体归同一账号，不引入邮箱验证；格式和最小长度仍由服务端校验，不声称 UNIQUE 或列长度能替代这些规则 |
| password_hash | VARCHAR(255)，保存成熟编码器的完整结果；255 为候选容量，选定编码器后核对，不能截断。原始密码不做账号式转换、不入库、不返回或记录 |
| display_name / workspace name | 分别 VARCHAR(64) / VARCHAR(100)，支持中文且不要求唯一；候选服务端规则为去除首尾 ASCII 空格后非空，拒绝超长。NOT NULL 不能排除空串或纯空白，API 校验仍需落实 |
| Membership 主键 | PRIMARY KEY(user_id, workspace_id)；当前关系没有独立编号需求，联合主键已同时表达非空和联合唯一，不再重复相同 UNIQUE。角色修改不改变主键；后续若需引用成员关系的独立生命周期，再重新比较 |
| role | VARCHAR(16) NOT NULL 加 CHECK，只接受 Owner / Editor / Reviewer，延续既有大小写；Java 枚举、API 值和持久值需对齐，不能只依赖前端下拉框 |
| 最多一个 Owner | 沿用 2.6 生成列及唯一索引；不保证 Owner 不能被删除/降级，2.5 事务与受保护入口不可省略 |
| 查询索引 | 联合主键以 user_id 开头，服务于当前用户的空间关系查询；额外 workspace_id 索引服务于空间成员列表/人数聚合，并显式满足该外键的索引要求。实际执行计划和性能未验证 |
| 字符集与比较 | 三表显式 utf8mb4 / utf8mb4_0900_bin，支持中文并明确区分大小写及尾随空格；账号先规范化，角色用固定拼写。此处仅确定草稿比较规则，不引入中文搜索/排序需求 |

VARCHAR 长度为字符上限，不是无条件按字符数预分配空间。32/64/100/255 是当前字段约束候选，不是性能或容量实验结论。整体修改时同时保持列长度、API 校验和页面提示一致。

MySQL 的 CHECK 对 FALSE 拒绝，对 UNKNOWN（如 NULL）可以通过，因此 role 的 NOT NULL 独立保留。该 SQL 参考 MySQL 8.4 的有效 CHECK 与 NO PAD 比较规则，不能在未核对版本/SQL 模式时声称可直接运行。[CHECK 官方说明](https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html)、[Unicode 比较与 NO PAD 说明](https://dev.mysql.com/doc/refman/8.4/en/charset-unicode-sets.html)。

建表依赖顺序为 users/workspaces 后 memberships；DDL 不应混入 2.5 的创建空间业务事务，MySQL 普通 CREATE TABLE 等 DDL 会隐式提交，不能把三次建表当成可共同回滚的业务 INSERT。正式迁移时再落实版本及失败处理。[MySQL 隐式提交说明](https://dev.mysql.com/doc/refman/8.4/en/implicit-commit.html)。

本草稿不擅自增加成员状态、软删除、审计字段或邀请表；成员历史/重新加入语义仍待契约明确，不能把省略字段当成已经实现删除生命周期。后续真实 MySQL 证据至少覆盖重复账号/成员、合法/非法角色（含 NULL、大小写和尾随空格）、外键、第二个 Owner、多个普通成员以及创建事务失败回滚。

用户已提交空间列表关联查询：JOIN、按用户过滤及排序正确；SELECT 返回了成员引用字段，尚缺页面需要的空间名称。修正与接口衔接见 2.8、3.1，不代表查询或接口已实现。

### 2.8 M0-2：我的空间列表查询草稿

状态：2026-10-20 评审用户提交的 SQL，未连接 MySQL、未运行。原查询的 m.workspace_id = w.id、m.user_id = 7、ORDER BY w.id 均符合目标；user_id/workspace_id/role 在当前三表草稿中可解析，但结果缺少 w.name。列表所需投影改为 w.id、w.name、m.role。

以下是后续 MyBatis 绑定参数的候选文本，不是可直接粘贴到数据库控制台执行的纯 SQL：

```sql
SELECT w.id, w.name, m.role
FROM memberships AS m
JOIN workspaces AS w ON m.workspace_id = w.id
WHERE m.user_id = #{currentUserId}
ORDER BY w.id ASC
LIMIT #{fetchSize} OFFSET #{offset};
```

currentUserId 必须来自服务端已认证身份，不从客户端 userId 参数取值。服务端校验 page/size 后计算 offset = (page - 1) * size、fetchSize = size + 1；乘法及数据库参数转换检查范围，溢出返回参数错误。参数使用绑定，不采用字符串拼接或 `${...}`。

单次取 size + 1 条：超出 size 时 hasNext=true，响应 items 仅保留前 size 条；否则 hasNext=false。不新增 COUNT 查询，不返回 total。联合主键使同一用户/空间关系不会重复，唯一的空间 ID 提供稳定排序；跨次分页请求不承诺固定快照。成员生命周期加入失效行后，查询必须同步过滤有效关系。

## 3. API 契约

M0 只形成首批需要的请求/响应与权限契约，后续按阶段增加。接口存在与否以实现和运行证据为准，不一次生成全部空接口。

| 资源 | 规划路径，实施时按契约固定 | 引入阶段 |
| --- | --- | --- |
| 会话 | `/api/auth/login`、`/api/auth/logout`、`/api/me` | M1 |
| 空间与成员 | `/api/workspaces`、`/api/workspaces/{id}/members` | M1 |
| 上传及分片 | `/api/workspaces/{id}/uploads`、`/api/uploads/{id}/parts/sign` | M2 |
| 完成与取消 | `/api/uploads/{id}/complete`、`/api/uploads/{id}` | M2 |
| 素材、版本 | `/api/workspaces/{id}/assets`、`/api/assets/{id}/versions` | M2 起 |
| 任务与进度 | `/api/jobs/{id}`、`/api/jobs/{id}/cancel`、`/api/events` | M3–M4 |
| 评论与审核 | `/api/versions/{id}/comments`、`/api/versions/{id}/reviews` | M5 |
| 分享 | `/api/versions/{id}/shares`、`/api/shares/{id}` | M5 |

每个实际端点需要明确：方法、身份/资源权限、输入校验、成功响应、错误码、状态转换与重复/冲突行为。可以随实现维护简短 Markdown 或生成式契约，不要求提前建设完整文档平台。

- 错误包含 code（稳定机器码）、message（可读信息）与 requestId；不返回内部堆栈，不把所有失败包装成 HTTP 200。
- 400 参数错误；401 未登录；无权或不存在的资源默认 404 以隐藏存在性；已可见资源上不允许的操作为 403；409 状态/修订冲突；413 超出上传上限；429 限流；503 依赖不可用。统一映射，客户端不得依据不稳定异常文本判断。
- 创建类请求需要防重时，固定幂等键的用户/空间/操作作用域、请求指纹、记录寿命和重复响应。相同键不同内容返回冲突；上传完成以会话身份和持久状态保证幂等。
- 状态竞争失败返回当前可安全披露的状态及刷新/重试指引，不能把被拒绝的取消显示成已取消。
- 同站入口优先使用代理保持 Cookie 和 CSRF 行为一致；需要 CORS 时限制明确来源、方法和头。不得在携带凭据时开放任意来源。

### 3.1 M0-2：GET /api/workspaces 契约草案

状态：2026-10-20 由 AI 根据用户的列表查询整理；接口、DTO、认证链路和分页尚未实现或验证，不表示 M0-2 已完成。

目的：已登录用户查看自己当前已加入的空间和各空间内的角色，供 PRODUCT 3.1 第 1 张空间入口使用。读取不修改业务状态，角色值沿用 Owner / Editor / Reviewer。

| 项目 | 草案 |
| --- | --- |
| 方法与路径 | GET /api/workspaces |
| 认证 | 使用有效登录会话；服务端取得当前用户 ID。未登录/会话过期返回 JSON 401，不重定向成登录页 HTML |
| 请求参数 | page 默认 1，正整数；size 默认 20，取值 1–100；不提供 userId 或 role 选择参数，未知查询参数按 400 拒绝 |
| 请求体 | 无 |
| 排序和分页 | 空间 ID 升序，普通 offset 分页；计算与有界查询见 2.8 |
| 成功 | HTTP 200，JSON 包含 items、page、size、hasNext；没有 total |
| 每项字段 | id 为十进制字符串，name 为字符串，role 为上述固定值之一；不返回 User、密码哈希或内部生成列 |
| 空列表 | HTTP 200，items=[]、hasNext=false；无成员关系与翻页超出末尾均为正常空结果，不映射为 403/404 |
| 身份边界 | Owner/Editor/Reviewer 均可查看自己的空间列表；访客分享不能据此访问空间列表。后续进入空间/查询素材仍须重新按当前成员关系授权 |
| 缓存 | 私有身份响应设置 Cache-Control: no-store；首版无成员授权结果缓存 |

示例请求为 GET /api/workspaces?page=1&size=20，以下 JSON 仅为虚构响应形状：

```json
{
  "items": [
    { "id": "20", "name": "活动宣传", "role": "Owner" },
    { "id": "30", "name": "课程作业", "role": "Reviewer" }
  ],
  "page": 1,
  "size": 20,
  "hasNext": false
}
```

数据库 BIGINT / Java Long 与接口 ID 表示分开：本端点把空间 ID 序列化为十进制字符串，前端按不透明字符串使用，不用 Number 转换。其他首批端点应在形成契约时沿用此约定，不能据本例声称全项目已完成 ID 适配。

| HTTP 状态 | 稳定错误码草案 | 含义 |
| --- | --- | --- |
| 400 | INVALID_QUERY | 参数格式、范围、未知参数或 offset 计算不合法；认证有效后校验参数 |
| 401 | AUTH_REQUIRED | 未登录或会话已失效 |
| 503 | SERVICE_UNAVAILABLE | 数据库等必要依赖暂时不可用；不能伪装成空列表 |
| 500 | INTERNAL_ERROR | 未预期服务端故障，响应不泄露堆栈或 SQL |

错误采用第 3 节统一的 code/message/requestId 字段。相同 GET 请求不产生新增记录，不需要为此读取接口设计业务幂等写入；响应会随成员关系变化。登录会话识别、参数绑定、ID 序列化、分页、空结果、越权参数及依赖故障的实际验证均留待 M1。

### 3.2 M0-2：POST /api/workspaces 契约草案

状态：2026-10-20 创建空间接口的最小契约，未实现、未测试；本轮先进入隔离 SQL 实践，不把接口实现作为建表实验前置。

- 输入：有效会话及 CSRF 凭据，JSON 仅接受 name。去除首尾 ASCII 空格后名称为 1–100 个 Unicode 码点，拒绝空白、超长、错误类型和未知字段；不接受客户端 userId、ownerId 或 role。任一已登录普通用户可创建自己的空间。
- 成功：HTTP 201，响应形状为 `{ "id": "20", "name": "活动宣传", "role": "Owner" }`，数字 ID 按 3.1 用字符串表示。仅在提交成功得到确认后返回成功。
- 业务：Controller 处理输入与响应，Service 从已认证身份取得创建者并管理 2.5 的短事务，Mapper 插入空间、取得实际生成键、插入 Owner 成员关系；异常不得吞掉后继续提交。事务/框架兼容及实际回滚留待实现验证。
- 失败：参数错误 400/INVALID_BODY，未登录 401/AUTH_REQUIRED，CSRF 拒绝 403/CSRF_REJECTED；必要依赖明确不可用为 503/SERVICE_UNAVAILABLE，未预期故障为 500/INTERNAL_ERROR。统一 code/message/requestId，不泄露 SQL、堆栈或凭据；过滤器次序与响应映射在 M1 验证。
- 提交结果不明：提交时断连等情况不能声称已回滚。若仍可回复，使用 503/CREATE_RESULT_UNKNOWN；若网络已断，前端表现为结果未确认，并引导刷新自己的空间列表核对，不自动重试。
- 重复语义：当前契约不承诺创建幂等，相同名称允许用于不同空间，重复 POST 可能得到两个不同空间；同名空间不能证明某次重试成功。提交期间限制界面重复点击仅改善体验，不作为服务端防重保证。若后续要求自动重试必须回到同一空间，再设计带数据库唯一约束、请求指纹及持久结果的幂等键契约，不能只用前端按钮或空间名称去重。

以上明确最小行为与限制，不据契约宣称恢复、防重或 CSRF 链路已实现。与数据库实验并行保留的首批未决项仍包括账号来源/最简邀请、成员生命周期及 Asset/AssetVersion 契约。

## 4. 版本与环境登记

M0 先核对本机已有环境，必要时参考 TaskFlow 的可用组合，但重新验证兼容性。初始版本组只在有构建证据后锁定；后续组件到引入阶段再确认。不得频繁为追新更换 Java/Boot，也不能因旧选择而忽视明确的安全或兼容问题。

| 项目 | 当前记录 | 验证时点与证据要求 |
| --- | --- | --- |
| Java、Spring Boot、MyBatis、构建工具/Wrapper | 待验证 | M0：官方兼容说明 + 一次干净构建 |
| Node、包管理器、Vue/TS 与组件库 | 待验证 | M0：安装、类型检查/构建，提交唯一 lockfile |
| MySQL、Flyway | 待验证 | M0 连接，M1 新库迁移与升级验证 |
| S3 兼容实现、SDK、镜像 | 待验证 | M0：私有上传/下载、multipart、签名、CORS 和校验短实验 |
| Docker/Compose、机器可用内存/磁盘 | 待验证 | M0：记录安装情况与资源预算，不默认已可用 |
| FFmpeg/ffprobe、执行上限 | 待验证 | M3：受控样例、超时、取消与清理 |
| RabbitMQ 与客户端 | 待验证 | M4：确认/路由、重投、持久性、双 worker 与最长任务 |
| Redis / Spring Session | 待验证 | M6：会话、限流、缓存及故障策略 |
| Nginx、指标与监控组件 | 待验证 | M7 完整验证；M4 先加必要指标 |

锁定值落到构建文件、Wrapper、lockfile 和固定容器 tag（必要时 digest）中；本表维护选择理由与验证指针，不手抄多份版本号。禁止 `latest` 和无依据的大版本批量升级。新增依赖先检查现有能力、维护状态、许可与引入成本，技术结论优先查所选版本的官方资料。

本地存储若无法以可控成本支持必要能力，记录失败证据与替代方案。改用云服务之前取得用户对费用、账户、素材和网络边界的确认；本文不授权消费。

### 4.1 2026-10-20：本机只读检查与首次 SQL 实践

本轮已执行工具版本查询及 Windows 服务状态读取，未连接数据库、读取密码、启动/停止服务、安装软件或修改配置。以下是发现的已有环境，不是项目依赖锁定，也不是构建或兼容验证：

| 项目 | 实际输出及限制 |
| --- | --- |
| Java / javac | 25.0.3；尚未选择 Spring Boot/MyBatis 或实际构建 |
| Maven | 3.9.16，显示使用 Java 25.0.3；版本命令同时出现 Access is denied，不能据最终退出码 0 称启动环境无异常 |
| Node / npm | v24.16.0 / 11.13.0；没有前端安装或构建证据 |
| MySQL 客户端 | 8.0.46；Windows MySQL80 服务 Running。客户端版本不等于已确认的服务端版本，尚无连接实验 |
| Docker / Compose | 29.5.3 / v5.1.4；读取 Docker 用户配置被拒绝，仅取得版本输出；Windows com.docker.service 为 Stopped，未验证引擎。不能仅凭该服务状态认定 Docker Desktop 引擎不可用；当前本机 MySQL 实践不依赖它 |
| 用户连接工具 | 用户表示 IDEA、VS Code 均可连接；这是用户自述，AI 未操作其连接或查看凭据 |

首次实践目标：用户在自己的 IDEA 中确认本机连接，创建一个全新的隔离练习库，再执行 2.7 的三表草稿，观察实际建表结果。该实验属于 M0-2 设计验证，不代表剩余契约、M0-3 或 M0 整体通过。

按块执行，不把下面的建库、选库和建表当成可盲目连续重跑的脚本：

1. 在 IDEA 的 Database 面板选择本机 localhost/127.0.0.1 的 MySQL 数据源，新建 Query Console。操作依据见 [IDEA 查询控制台说明](https://www.jetbrains.com/help/idea/query-consoles.html)。
2. 在该控制台执行只读检查，核实服务端版本、默认库、外键检查、严格模式与所需排序规则。若不是支持有效 CHECK 的 MySQL（8.0.16 起）或排序规则查询无结果，先记录结果、调整草稿，不通过关闭约束继续；外键检查应为 1，SQL 模式应包含 STRICT_TRANS_TABLES 或 STRICT_ALL_TABLES。

```sql
SELECT VERSION() AS server_version,
       DATABASE() AS current_database,
       @@SESSION.foreign_key_checks AS fk_checks,
       @@SESSION.sql_mode AS sql_mode;

SELECT COLLATION_NAME
FROM information_schema.COLLATIONS
WHERE COLLATION_NAME = 'utf8mb4_0900_bin';
```

3. 只执行以下 CREATE DATABASE。目标固定为 mediadesk_lab_20261020，若已存在或权限不足就停止并反馈，不使用 IF NOT EXISTS 绕过、不删除或覆盖已有库。

```sql
CREATE DATABASE mediadesk_lab_20261020
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_bin;
```

4. 创建成功后，在同一个控制台选库并确认返回值精确为 mediadesk_lab_20261020，再复制 [三表 SQL 草稿](drafts/m0-2-identity.sql) 到该控制台，按 users、workspaces、memberships 的顺序逐条执行 CREATE TABLE。发生错误先停止，保留错误和已成功的步骤，不盲目重复整份草稿。

```sql
USE mediadesk_lab_20261020;
SELECT DATABASE() AS current_database;
```

5. 检查实际对象。预期为三个表，以及 memberships 的联合主键、外键、角色 CHECK 和 Owner 生成列/唯一索引；仅建表成功还不能认定约束行为或事务通过。

```sql
SHOW TABLES FROM mediadesk_lab_20261020;
SHOW CREATE TABLE mediadesk_lab_20261020.memberships;
```

以上 SQL 均为给用户的待执行步骤，尚无执行证据。首轮反馈只需服务端版本、三个表是否出现，以及有错误时的错误号/文字；不要求提交数据库凭据或其他项目内容。之后再在同一隔离库用样例数据验证查询、约束和回滚。机制依据：[MySQL 8.0 CHECK 版本边界](https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html)、[binary/_bin 比较规则](https://dev.mysql.com/doc/refman/8.0/en/charset-binary-collations.html)。

## 5. 资源限制登记

数值均待 M0 实测，不得把未测数字写成产品容量：

| 需固定的限制 | 确定方式 |
| --- | --- |
| 单文件大小、视频时长、空间总配额 | 按可用磁盘、样例与预期演示确定并同步前后端 |
| 分片大小、并发、签名期限、上传过期 | 兼容实现限制 + 断网/刷新/完成竞争实验 |
| worker 并发、最大执行时间、临时磁盘和输出日志上限 | M0 记录初始预算，M3/M4 按实际媒体校准 |
| 任务租约、心跳、恢复周期、重试次数和总期限 | M3/M4 根据任务时长、故障与数据库时间固定 |
| 分享签名期限、删除保留期、数据清理周期 | 按产品语义与安全窗口在对应阶段固定 |
| 最大页大小、SSE 连接/频率、昂贵接口限流 | 按资源预算和验证固定，不能无限制 |

配置具有明确单位、默认值和合法范围；启动时拒绝危险或不合法组合。开发限制、测试样例与公开能力分别记录。

## 6. 已验证命令登记

**目前没有应用工程，因此没有已验证的应用构建、测试、启动或迁移命令。** 仅工具版本查询已执行，见 6.1；4.1 的用户 SQL 实践仍待执行。后续使用实际脚本/Wrapper 建立入口，验证后在此填入准确目录与命令，不将猜测或示例登记为可用命令。

| 操作 | 工作目录 | 已验证命令 | 最近证据 |
| --- | --- | --- | --- |
| 后端构建与关键测试 | 待建 | 未建立 | 未运行 |
| 前端安装、类型检查、构建 | 待建 | 未建立 | 未运行 |
| 启停当前阶段本地依赖 | 待建 | 未建立 | 未运行 |
| 初始化/升级数据库 | 待建 | 未建立 | 未运行 |
| 运行 API / 前端 | 待建 | 未建立 | 未运行 |
| 运行 worker | 待建 | 未建立 | 未运行 |
| 故障实验与性能基线 | 待建 | 未建立 | 未运行 |

新增条目时记录 OS/前置条件、配置样例位置、预期结果、验证日期与证据路径；声明 Windows 或 Linux 支持必须有相应证据。清理数据的命令单列目标、后果与授权条件，不混在普通启动步骤中。

### 6.1 仅版本查询已执行

2026-10-20，在项目根目录的 PowerShell 执行如下只读命令；不代表应用功能已运行。Maven 与 Docker 的读取拒绝及检查边界见 4.1，原异常没有忽略为成功。

| 命令 | 结果 |
| --- | --- |
| `java -version` | 输出 25.0.3 |
| `javac -version` | 输出 25.0.3 |
| `mvn -version` | 输出 3.9.16，同时出现 Access is denied，构建未验证 |
| `node --version` | 输出 v24.16.0 |
| `npm.cmd --version` | 输出 11.13.0 |
| `& 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe' --version` | 输出客户端 8.0.46，不代表服务端已连接 |
| `docker --version` | 输出 29.5.3，同时用户配置读取被拒绝 |
| `docker compose version` | 输出 v5.1.4，同时用户配置读取被拒绝 |

## 7. 配置、安全与部署

- 真实凭据通过环境或本机私有配置提供。工程建立时添加脱敏 `.env.example` 及加载说明，不写入真实值；`.gitignore` 不能替代凭据检查与权限隔离。
- 开发与测试使用隔离数据库、桶前缀、队列和工作目录；故障实验使用生成或获授权样例，不使用个人真实素材。
- 初期按需启动 MySQL/对象存储，M4 再加 MQ，M6 加 Redis，M7 加完整监控，ES 仅在 v1.1。不要要求机器同时运行全部目标服务。
- 服务默认仅本地或内部网络可访问；数据库、MQ、Redis、指标与管理端口不直接暴露公网。公开试用前配置 HTTPS、访问控制与基础配额，部署须有相应授权。
- 日志带 requestId/jobId/attemptId/workspaceId；不记录密码、Cookie、CSRF/分享令牌、签名查询串和长期密钥。高基数 ID 放日志，不作指标标签。
- readiness 检查当前进程必要依赖，liveness 反映进程自身；SSE 经 Nginx 验证缓冲与超时，断开能释放连接。
- M7 备份同时覆盖数据库与对象文件及引用关系，明确保留策略并完成恢复演练；Docker volume 不等于备份。
- CI 逐步建立后端编译/关键测试、前端类型检查/构建、镜像构建；CI 不自动发布，不把真实密钥写入仓库或日志。
