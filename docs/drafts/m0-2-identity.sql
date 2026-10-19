-- M0-2 学习与评审草稿，2026-10-20。
-- 参考 MySQL 8.4 语法；实际版本尚未锁定，本文件未执行，不是 Flyway 迁移。
-- 前提：InnoDB，严格 SQL 模式，外键及 CHECK 检查开启。
-- 账号先由服务端规范化并校验；成员行仅表达当前关系，历史保留语义待定。
-- 事务与 Owner 生命周期仍须服务端落实，不能仅靠本文件保证。

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account VARCHAR(32) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(64) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_users_account UNIQUE (account)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_bin;

CREATE TABLE workspaces (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,

    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_bin;

CREATE TABLE memberships (
    user_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    owner_workspace_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN role = 'Owner' THEN workspace_id ELSE NULL END
    ) VIRTUAL,

    PRIMARY KEY (user_id, workspace_id),
    CONSTRAINT uk_membership_one_owner UNIQUE (owner_workspace_id),
    KEY idx_memberships_workspace (workspace_id),

    CONSTRAINT ck_memberships_role
        CHECK (role IN ('Owner', 'Editor', 'Reviewer')),

    CONSTRAINT fk_membership_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_membership_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_bin;
