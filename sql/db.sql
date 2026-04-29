create database superagent;

use superagent;

-- 1.用户表

CREATE TABLE `superagent_user`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`    varchar(64)  NOT NULL COMMENT '用户名',
    `password`    varchar(128) NOT NULL COMMENT '密码（加密存储）',
    `nick_name`   varchar(64)           DEFAULT NULL COMMENT '用户昵称',
    `avatar`      varchar(512)          DEFAULT NULL COMMENT '头像地址',
    `phone`       varchar(20)           DEFAULT NULL COMMENT '手机号',
    `email`       varchar(100)          DEFAULT NULL COMMENT '邮箱',
    `user_status` tinyint      NOT NULL DEFAULT '1' COMMENT '用户状态：1-正常 0-禁用',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint      NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`) COMMENT '用户名唯一索引',
    UNIQUE KEY `uk_phone` (`phone`) COMMENT '手机号唯一索引',
    KEY `idx_user_status` (`user_status`) COMMENT '用户状态索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='系统用户表';

-- 2. 用户安全扩展表（与user表1对1，存储动态安全状态）
CREATE TABLE `superagent_user_security`
(
    `id`               bigint          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`          BIGINT UNSIGNED NOT NULL, -- 直接使用user_id作为主键，确保1对1
    `last_login_time`  DATETIME                 DEFAULT NULL COMMENT '最后一次登录时间',
    `last_login_ip`    VARCHAR(45)              DEFAULT NULL COMMENT '最后一次登录IP，兼容IPv6',
    `last_login_agent` VARCHAR(255)             DEFAULT NULL COMMENT '最后一次登录的User-Agent',
    `login_fail_count` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    `locked_until`     DATETIME                 DEFAULT NULL COMMENT '锁定截至时间，NULL表示未锁定',
    `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment '用户安全扩展表';


-- 3. 用户登录日志表（追加写，无更新）
CREATE TABLE `superagent_user_login_log`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT UNSIGNED NOT NULL,
    `login_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 登录时间
    `login_ip`     VARCHAR(45)     NOT NULL COMMENT '登录IP',
    `login_agent`  VARCHAR(255)             DEFAULT NULL COMMENT '客户端信息',
    `login_result` TINYINT         NOT NULL DEFAULT 0 COMMENT '0:失败 1:成功',
    `fail_reason`  VARCHAR(100)             DEFAULT NULL COMMENT '失败原因，如：密码错误、账号锁定',
    PRIMARY KEY (`id`),
    KEY `idx_user_id_time` (`user_id`, `login_time`)                   -- 常用索引，用于查询某用户的登录历史
    -- 注意：可以按日期分区，例如按 `login_time` 的月份分区
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment '用户登录日志表';


-- 4. 权限表 superagent_permission
CREATE TABLE `superagent_permission`
(
    `id`          bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        varchar(255)    DEFAULT NULL COMMENT '权限名称',
    `code`        varchar(100)    DEFAULT NULL COMMENT '权限代码(权限标识符)',
    `url`         varchar(255)    DEFAULT NULL COMMENT 'URL',
    `type`        varchar(20)     DEFAULT NULL COMMENT '权限类型: menu-菜单, button-按钮',
    `parent_id`   bigint          DEFAULT NULL COMMENT '父权限id',
    `order_no`    int             DEFAULT NULL COMMENT '菜单排序号',
    `icon`        varchar(255)    DEFAULT NULL COMMENT '菜单图标',
    `component`   varchar(255)    DEFAULT NULL COMMENT '菜单对应要渲染的Vue组件名称',
    `create_time` datetime        DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime        DEFAULT NULL COMMENT '更新时间',
    `is_deleted`  int    NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='权限表';

-- 5. 角色表 superagent_role
CREATE TABLE `superagent_role`
(
    `id`          bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role`        varchar(100)    DEFAULT NULL COMMENT '角色',
    `role_name`   varchar(255)    DEFAULT NULL COMMENT '角色名称',
    `create_time` datetime        DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime        DEFAULT NULL COMMENT '更新时间',
    `is_deleted`  int    NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

-- 6. 角色权限关联表 superagent_role_permission
CREATE TABLE `superagent_role_permission`
(
    `id`            bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id`       bigint          DEFAULT NULL COMMENT '角色ID',
    `permission_id` bigint          DEFAULT NULL COMMENT '权限ID',
    `create_time`   datetime        DEFAULT NULL COMMENT '创建时间',
    `update_time`   datetime        DEFAULT NULL COMMENT '更新时间',
    `is_deleted`    int    NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色权限关联表';

-- 7. 用户角色关联表 superagent_user_role
CREATE TABLE `superagent_user_role`
(
    `id`          bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     bigint          DEFAULT NULL COMMENT '用户ID',
    `role_id`     bigint          DEFAULT NULL COMMENT '角色ID',
    `create_time` datetime        DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime        DEFAULT NULL COMMENT '更新时间',
    `is_deleted`  int    NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联表';


-- 插入4个基础角色：管理员、访客、用户、会员
INSERT INTO `superagent_role` (`role`, `role_name`, `create_time`, `update_time`, `is_deleted`)
VALUES ('admin', '管理员', NOW(), NOW(), 0),
       ('guest', '访客', NOW(), NOW(), 0),
       ('user', '普通用户', NOW(), NOW(), 0),
       ('member', '会员用户', NOW(), NOW(), 0);

-- 插入7个功能权限
INSERT INTO `superagent_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `component`,
                                     `create_time`, `update_time`, `is_deleted`)
VALUES
-- 1. 超级智能体使用权限
('超级智能体使用权限', 'agent:super:use', '', 'button', 0, 1, '', '', NOW(), NOW(), 0),
-- 2. 单场景智能体使用权限
('单场景智能体使用权限', 'agent:scene:use', '', 'button', 0, 2, '', '', NOW(), NOW(), 0),
-- 3. token无限制使用权限
('token无限制使用权限', 'token:limit:unlimited', '', 'button', 0, 3, '', '', NOW(), NOW(), 0),
-- 4. token每日100万权限
('token每日100万权限', 'token:limit:daily', '', 'button', 0, 4, '', '', NOW(), NOW(), 0),
-- 5. 文件传入权限
('文件传入权限', 'file:upload', '', 'button', 0, 5, '', '', NOW(), NOW(), 0),
-- 6. 文件下载权限
('文件下载权限', 'file:download', '', 'button', 0, 6, '', '', NOW(), NOW(), 0),
-- 7. 多模态使用权限
('多模态使用权限', 'agent:multimodal:use', '', 'button', 0, 7, '', '', NOW(), NOW(), 0);


--  ===================== 管理员（role_id=1）：所有权限 =====================
INSERT INTO `superagent_role_permission` (`role_id`, `permission_id`, `create_time`, `update_time`, `is_deleted`)
VALUES (1, 1, NOW(), NOW(), 0),
       (1, 2, NOW(), NOW(), 0),
       (1, 3, NOW(), NOW(), 0),
       (1, 4, NOW(), NOW(), 0),
       (1, 5, NOW(), NOW(), 0),
       (1, 6, NOW(), NOW(), 0),
       (1, 7, NOW(), NOW(), 0);

--  ===================== 访客（role_id=2）：仅单场景智能体权限 =====================
INSERT INTO `superagent_role_permission` (`role_id`, `permission_id`, `create_time`, `update_time`, `is_deleted`)
VALUES (2, 2, NOW(), NOW(), 0);

--  ===================== 普通用户（role_id=3）：基础功能权限 =====================
INSERT INTO `superagent_role_permission` (`role_id`, `permission_id`, `create_time`, `update_time`, `is_deleted`)
VALUES (3, 2, NOW(), NOW(), 0), -- 单场景智能体
       (3, 4, NOW(), NOW(), 0), -- token每日100万
       (3, 5, NOW(), NOW(), 0), -- 文件传入
       (3, 6, NOW(), NOW(), 0), -- 文件下载
       (3, 7, NOW(), NOW(), 0);
-- 多模态

--  ===================== 会员用户（role_id=4）：高级权限（无每日token限制） =====================
INSERT INTO `superagent_role_permission` (`role_id`, `permission_id`, `create_time`, `update_time`, `is_deleted`)
VALUES (4, 1, NOW(), NOW(), 0), -- 超级智能体
       (4, 2, NOW(), NOW(), 0), -- 单场景智能体
       (4, 3, NOW(), NOW(), 0), -- token无限制
       (4, 5, NOW(), NOW(), 0), -- 文件传入
       (4, 6, NOW(), NOW(), 0), -- 文件下载
       (4, 7, NOW(), NOW(), 0); -- 多模态

use superagent;

-- 8. AI对话会话表（多的一方：关联用户）
CREATE TABLE `superagent_chat_session`
(
    `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        bigint      NOT NULL COMMENT '用户ID【关联superagent_user.id】', -- 核心：一对多外键字段
    `session_id`     varchar(64) NOT NULL COMMENT '会话唯一ID',
    `agent_id`       int         NOT NULL COMMENT 'agentId',
    `session_name`   varchar(32)          DEFAULT NULL COMMENT '会话名称',
    `last_active`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    `session_status` tinyint     NOT NULL DEFAULT '1' COMMENT '会话状态：1-活跃 0-归档 2-禁用',
    `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     tinyint     NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`),                                                  -- 用户ID索引（加速查询用户的所有会话）
    KEY `idx_last_active` (`last_active`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='AI对话会话表';

-- 9. AI对话消息表
CREATE TABLE `superagent_chat_message`
(
    `id`           bigint                                    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `session_id`   varchar(64)                               NOT NULL COMMENT '会话ID【关联superagent_chat_session.session_id】', -- 核心：一对多关联字段（修正原chat_id歧义）
    `message_type` enum ('USER','ASSISTANT','SYSTEM','TOOL') NOT NULL COMMENT '消息类型：USER/ASSISTANT/SYSTEM/TOOL',
    `content`      longtext                                  NOT NULL COMMENT '消息内容',
    `message_time` datetime                                  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间',
    `metadata`     json                                               DEFAULT NULL COMMENT '元数据（Spring AI扩展：token数、模型信息等）',
    `create_time`  datetime                                  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime                                  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   tinyint                                   NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_message_time` (`message_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='AI对话消息表';


-- 10. mq消息失败类
CREATE TABLE `superagent_mq_fail_message`
(
    `id`             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `message_id`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '消息唯一标识（CorrelationData ID）',
    `exchange`       VARCHAR(255) NOT NULL DEFAULT '' COMMENT '交换机名称',
    `routing_key`    VARCHAR(255) NOT NULL DEFAULT '' COMMENT '路由键',
    `message_body`   TEXT COMMENT '消息体（JSON字符串）',
    `fail_reason`    VARCHAR(500) NOT NULL DEFAULT '' COMMENT '失败原因',
    `retry_count`    INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-待处理 1-已处理 2-已忽略',
    `handler_remark` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '处理备注（人工填写）',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     tinyint      NOT NULL DEFAULT '0' COMMENT '逻辑删除',

    INDEX `idx_status` (`status`),
    INDEX `idx_message_id` (`message_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='MQ发送失败消息记录表';


-- 11.模型供应商配置
CREATE TABLE `superagent_model_provider`
(
    `id`             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `provider_code`  VARCHAR(50)  NOT NULL UNIQUE COMMENT '供应商编码: openai, anthropic, moonshot等',
    `provider_name`  VARCHAR(100) NOT NULL COMMENT '供应商展示名称',
    `provider_type`  VARCHAR(20)  NOT NULL DEFAULT 'api' COMMENT '类型: api, local, proxy',
    `base_url`       VARCHAR(500) COMMENT 'API基础地址',
    `api_version`    VARCHAR(50) COMMENT 'API版本',
    `auth_type`      VARCHAR(20)  NOT NULL DEFAULT 'bearer' COMMENT '认证方式: bearer, api_key, oauth2',
    `timeout_ms`     INT UNSIGNED          DEFAULT 30000 COMMENT '超时时间(毫秒)',
    `max_retries`    TINYINT UNSIGNED      DEFAULT 3 COMMENT '最大重试次数',
    `rate_limit_qps` INT UNSIGNED          DEFAULT 100 COMMENT '每秒请求限制',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '0:禁用 1:启用 2:维护中',
    `priority`       INT          NOT NULL DEFAULT 100 COMMENT '优先级(越小越优先)',
    `config_json`    JSON COMMENT '扩展配置(供应商特有)',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     tinyint      NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    -- 索引定义
    INDEX idx_provider_status_priority (status, priority)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='模型供应商配置';


-- 12.模型主表
CREATE TABLE `superagent_model`
(
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    model_code          VARCHAR(100)    NOT NULL UNIQUE COMMENT '模型编码: glm4, deepseek-v4-pro等',
    model_name          VARCHAR(200)    NOT NULL COMMENT '展示名称',
    provider_id         BIGINT UNSIGNED NOT NULL COMMENT '供应商Id',
    `provider_name`     VARCHAR(100)    NOT NULL COMMENT '供应商展示名称',

    model_type          VARCHAR(50)     NOT NULL DEFAULT 'llm' COMMENT 'llm, embedding, image, audio, multimodal',
    capabilities        JSON COMMENT '能力标签,例如: ["chat","vision","function_calling","json_mode"]',
    context_window      INT UNSIGNED COMMENT '上下文窗口大小(token)',
    max_output_tokens   INT UNSIGNED COMMENT '最大输出token数',

    -- 计费配置 (每百万tokens)
    input_price_per_1m  DECIMAL(12, 6)  NOT NULL DEFAULT 0.000000 COMMENT '输入价格/百万tokens',
    output_price_per_1m DECIMAL(12, 6)  NOT NULL DEFAULT 0.000000 COMMENT '输出价格/百万tokens',
    currency            VARCHAR(3)      NOT NULL DEFAULT 'CNY' COMMENT '币种',
    billing_unit        VARCHAR(20)     NOT NULL DEFAULT 'token' COMMENT '计费单位: token, request, minute',

    -- 参数约束 (JSON存储，前端动态渲染)，默认空对象
    param_constraints   JSON            NOT NULL DEFAULT (JSON_OBJECT()) COMMENT '参数支持及范围约束',

    status              TINYINT         NOT NULL DEFAULT 1 COMMENT '0:下线 1:上线 2:内测',
    is_recommended      TINYINT         NOT NULL DEFAULT 0 COMMENT '是否推荐',
    sort_order          INT             NOT NULL DEFAULT 0 COMMENT '排序',

    description         TEXT COMMENT '模型描述',
    icon_url            VARCHAR(500) COMMENT '图标URL',
    tags                JSON COMMENT '标签: ["new","hot"]',

    `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        tinyint         NOT NULL DEFAULT '0' COMMENT '逻辑删除',

    -- 索引定义
    INDEX idx_model_provider (provider_id),
    INDEX idx_model_status (status),
    INDEX idx_model_type (model_type),
    INDEX idx_model_recommended (is_recommended)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='模型主表';


-- 13. 模型分组表
CREATE TABLE `superagent_model_group`
(
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    group_name    VARCHAR(50) NOT NULL COMMENT '分组名称',
    group_code    VARCHAR(50) NOT NULL UNIQUE COMMENT '分组编码',
    sort_order    INT         NOT NULL DEFAULT 0,
    status        TINYINT     NOT NULL DEFAULT 1,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint     NOT NULL DEFAULT '0' COMMENT '逻辑删除'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='模型分组表';

-- 14. 分组关联表
CREATE TABLE `superagent_model_group_relation`
(
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    model_id      BIGINT UNSIGNED NOT NULL,
    group_id      BIGINT UNSIGNED NOT NULL,
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint         NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    UNIQUE KEY uk_model_group (model_id, group_id),
    INDEX idx_mgr_group_id (group_id) -- 单独索引，便于通过组查模型
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='分组关联表';

-- 15. 用户全局配置表
CREATE TABLE `superagent_user_model_config`
(
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT UNSIGNED NOT NULL,

    temperature   DECIMAL(3, 2)            DEFAULT 0.75,
    top_p         DECIMAL(3, 2)            DEFAULT 0.9,
    top_k         INT UNSIGNED             DEFAULT 10,
    max_tokens    INT UNSIGNED             DEFAULT 200000,

    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint         NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    INDEX idx_umc_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户模型级配置';


-- 16.用户总用量汇总
CREATE TABLE `superagent_user_usage_summary`
(
    id                                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id                            BIGINT UNSIGNED NOT NULL UNIQUE COMMENT '用户ID',

    -- 累计总量 (全生命周期)
    total_requests                     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总请求次数',
    total_input_tokens                 BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总输入token',
    total_input_tokens_cost            DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '总输入token费用',
    total_input_cache_hit_tokens       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总输入缓存命中token',
    total_input_cache_hit_tokens_cost  DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '总输入缓存命中token费用',
    total_input_cache_miss_tokens      BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总输入缓存未命中token',
    total_input_cache_miss_tokens_cost DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '总输入缓存未命中token费用',
    total_output_tokens                BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总输出token',
    total_output_tokens_cost           DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '总输出token费用',
    total_tokens                       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总token',
    total_cost                         DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '总费用',

    `create_time`                      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`                      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`                       tinyint         NOT NULL DEFAULT '0' COMMENT '逻辑删除'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户总用量汇总';


-- 17.用户每日总用量汇总表(包含各个模型)
CREATE TABLE `superagent_user_usage_date`
(
    id                           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id                      BIGINT UNSIGNED NOT NULL UNIQUE COMMENT '用户ID',
    model_id                     BIGINT UNSIGNED NOT NULL,

    requests                     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '请求次数',
    input_tokens                 BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '输入token',
    input_tokens_cost            DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '输入token费用',
    input_cache_hit_tokens       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '输入缓存命中token',
    input_cache_hit_tokens_cost  DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '输入缓存命中token费用',
    input_cache_miss_tokens      BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '输入缓存未命中token',
    input_cache_miss_tokens_cost DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '输入缓存未命中token费用',
    output_tokens                BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '输出token',
    output_tokens_cost           DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '输出token费用',
    tokens                       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'token',
    cost                         DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '费用',

    `create_time`                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`                 tinyint         NOT NULL DEFAULT '0' COMMENT '逻辑删除'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户总用量汇总';


-- 18.用户模型用量汇总
CREATE TABLE `superagent_user_model_usage`
(
    id                                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id                            BIGINT UNSIGNED NOT NULL,
    model_id                           BIGINT UNSIGNED NOT NULL,

    -- 累计用量
    total_requests                     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总请求次数',
    total_input_tokens                 BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总输入token',
    total_input_tokens_cost            DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '总输入token费用',
    total_input_cache_hit_tokens       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总输入缓存命中token',
    total_input_cache_hit_tokens_cost  DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '总输入缓存命中token费用',
    total_input_cache_miss_tokens      BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总输入缓存未命中token',
    total_input_cache_miss_tokens_cost DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '总输入缓存未命中token费用',
    total_output_tokens                BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总输出token',
    total_output_tokens_cost           DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '总输出token费用',
    total_tokens                       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总token',
    total_cost                         DECIMAL(18, 6)  NOT NULL DEFAULT 0.000000 COMMENT '总费用',

    `create_time`                      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`                      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`                       tinyint         NOT NULL DEFAULT '0' COMMENT '逻辑删除',

    UNIQUE KEY uk_user_model (user_id, model_id),
    INDEX idx_umu_user (user_id),
    INDEX idx_umu_model (model_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户模型用量汇总';

