create database superagent;

use superagent;

CREATE TABLE `superagent_user` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `username` varchar(64) NOT NULL COMMENT '用户名',
                                   `password` varchar(128) NOT NULL COMMENT '密码（加密存储）',
                                   `nick_name` varchar(64) DEFAULT NULL COMMENT '用户昵称',
                                   `avatar` varchar(512) DEFAULT NULL COMMENT '头像地址',
                                   `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
                                   `user_status` tinyint NOT NULL DEFAULT '1' COMMENT '用户状态：1-正常 0-禁用',
                                   `model` varchar(64) DEFAULT NULL COMMENT 'AI模型名称',
                                   `temperature` decimal(3,2) DEFAULT '0.7' COMMENT 'AI温度参数',
                                   `top_k` decimal(10,0) DEFAULT NULL COMMENT 'AI Top-K参数',
                                   `top_p` decimal(3,2) DEFAULT NULL COMMENT 'AI Top-P参数',
                                   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除 1-已删除',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_username` (`username`) COMMENT '用户名唯一索引',
                                   UNIQUE KEY `uk_phone` (`phone`) COMMENT '手机号唯一索引',
                                   KEY `idx_user_status` (`user_status`) COMMENT '用户状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';



-- 1. 权限表 superagent_permission
CREATE TABLE `superagent_permission` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                         `name` varchar(255) DEFAULT NULL COMMENT '权限名称',
                                         `code` varchar(100) DEFAULT NULL COMMENT '权限代码(权限标识符)',
                                         `url` varchar(255) DEFAULT NULL COMMENT 'URL',
                                         `type` varchar(20) DEFAULT NULL COMMENT '权限类型: menu-菜单, button-按钮',
                                         `parent_id` bigint DEFAULT NULL COMMENT '父权限id',
                                         `order_no` int DEFAULT NULL COMMENT '菜单排序号',
                                         `icon` varchar(255) DEFAULT NULL COMMENT '菜单图标',
                                         `component` varchar(255) DEFAULT NULL COMMENT '菜单对应要渲染的Vue组件名称',
                                         `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                         `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                         `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
                                         PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 2. 角色表 superagent_role
CREATE TABLE `superagent_role` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `role` varchar(100) DEFAULT NULL COMMENT '角色',
                                   `role_name` varchar(255) DEFAULT NULL COMMENT '角色名称',
                                   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                   `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
                                   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 3. 角色权限关联表 superagent_role_permission
CREATE TABLE `superagent_role_permission` (
                                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                              `role_id` bigint DEFAULT NULL COMMENT '角色ID',
                                              `permission_id` bigint DEFAULT NULL COMMENT '权限ID',
                                              `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                              `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                              `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
                                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 4. 用户角色关联表 superagent_user_role
CREATE TABLE `superagent_user_role` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                        `user_id` bigint DEFAULT NULL COMMENT '用户ID',
                                        `role_id` bigint DEFAULT NULL COMMENT '角色ID',
                                        `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                        `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                        `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
                                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';



-- 插入4个基础角色：管理员、访客、用户、会员
INSERT INTO `superagent_role` (`role`, `role_name`, `create_time`, `update_time`, `is_deleted`)
VALUES
    ('admin', '管理员', NOW(), NOW(), 0),
    ('guest', '访客', NOW(), NOW(), 0),
    ('user', '普通用户', NOW(), NOW(), 0),
    ('member', '会员用户', NOW(), NOW(), 0);

-- 插入7个功能权限
INSERT INTO `superagent_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `component`, `create_time`, `update_time`, `is_deleted`)
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
VALUES
    (1, 1, NOW(), NOW(), 0),
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
VALUES
    (3, 2, NOW(), NOW(), 0),  -- 单场景智能体
    (3, 4, NOW(), NOW(), 0),  -- token每日100万
    (3, 5, NOW(), NOW(), 0),  -- 文件传入
    (3, 6, NOW(), NOW(), 0),  -- 文件下载
    (3, 7, NOW(), NOW(), 0);  -- 多模态

--  ===================== 会员用户（role_id=4）：高级权限（无每日token限制） =====================
INSERT INTO `superagent_role_permission` (`role_id`, `permission_id`, `create_time`, `update_time`, `is_deleted`)
VALUES
    (4, 1, NOW(), NOW(), 0),  -- 超级智能体
    (4, 2, NOW(), NOW(), 0),  -- 单场景智能体
    (4, 3, NOW(), NOW(), 0),  -- token无限制
    (4, 5, NOW(), NOW(), 0),  -- 文件传入
    (4, 6, NOW(), NOW(), 0),  -- 文件下载
    (4, 7, NOW(), NOW(), 0);  -- 多模态

use superagent;

-- AI对话会话表（多的一方：关联用户）
CREATE TABLE `superagent_chat_session` (
                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                           `user_id` bigint NOT NULL COMMENT '用户ID【关联superagent_user.id】', -- 核心：一对多外键字段
                                           `session_id` varchar(64) NOT NULL COMMENT '会话唯一ID',
                                           `agent_id` int NOT NULL COMMENT 'agentId',
                                           `session_name` varchar(32) DEFAULT NULL COMMENT '会话名称',
                                           `last_active` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
                                           `session_status` tinyint NOT NULL DEFAULT '1' COMMENT '会话状态：1-活跃 0-归档 2-禁用',
                                           `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
                                           PRIMARY KEY (`id`),
                                           UNIQUE KEY `uk_session_id` (`session_id`),
                                           KEY `idx_user_id` (`user_id`), -- 用户ID索引（加速查询用户的所有会话）
                                           KEY `idx_last_active` (`last_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话表';

-- AI对话消息表
CREATE TABLE `superagent_chat_message` (
                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                           `session_id` varchar(64) NOT NULL COMMENT '会话ID【关联superagent_chat_session.session_id】', -- 核心：一对多关联字段（修正原chat_id歧义）
                                           `message_type` enum('USER','ASSISTANT','SYSTEM','TOOL') NOT NULL COMMENT '消息类型：USER/ASSISTANT/SYSTEM/TOOL',
                                           `content` longtext NOT NULL COMMENT '消息内容',
                                           `message_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间',
                                           `metadata` json DEFAULT NULL COMMENT '元数据（Spring AI扩展：token数、模型信息等）',
                                           `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
                                           PRIMARY KEY (`id`),
                                           KEY `idx_session_id` (`session_id`),
                                           KEY `idx_message_time` (`message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话消息表';


-- mq消息失败类
CREATE TABLE `superagent_mq_fail_message` (
                                              `id`              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                              `message_id`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '消息唯一标识（CorrelationData ID）',
                                              `exchange`        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '交换机名称',
                                              `routing_key`     VARCHAR(255) NOT NULL DEFAULT '' COMMENT '路由键',
                                              `message_body`    TEXT         COMMENT '消息体（JSON字符串）',
                                              `fail_reason`     VARCHAR(500) NOT NULL DEFAULT '' COMMENT '失败原因',
                                              `retry_count`     INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
                                              `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-待处理 1-已处理 2-已忽略',
                                              `handler_remark`  VARCHAR(500) NOT NULL DEFAULT '' COMMENT '处理备注（人工填写）',
                                              `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                              `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                              `is_deleted`      tinyint      NOT NULL DEFAULT '0' COMMENT '逻辑删除',

                                              INDEX `idx_status` (`status`),
                                              INDEX `idx_message_id` (`message_id`),
                                              INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ发送失败消息记录表';