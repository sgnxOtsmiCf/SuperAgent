-- ============================================================
-- 模型供应商与模型初始化数据
-- 数据来源: DeepSeek / GLM(智谱) / Kimi(Moonshot) / DashScope(阿里云百炼) 官方网站
-- 更新日期: 2026-04-28
-- ============================================================

USE superagent;

-- ==================== 1. 模型供应商 ====================
INSERT INTO `superagent_model_provider`
(`id`, `provider_code`, `provider_name`, `provider_type`, `base_url`, `api_version`, `auth_type`, `timeout_ms`, `max_retries`, `rate_limit_qps`, `status`, `priority`, `config_json`)
VALUES
    (1, 'deepseek',  'DeepSeek',              'api', 'https://api.deepseek.com',                       'v1', 'bearer',  90000, 3, 100, 1, 10, NULL),
    (2, 'moonshot',  'Moonshot AI (Kimi)',    'api', 'https://api.moonshot.cn/v1',                      'v1', 'bearer',  60000, 3, 100, 1, 20, NULL),
    (3, 'zhipu',     '智谱AI',                'api', 'https://open.bigmodel.cn/api/paas/v4',            'v4', 'bearer',  60000, 3, 100, 1, 30, NULL),
    (4, 'dashscope', '阿里云百炼 (DashScope)', 'api', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'v1', 'api_key', 60000, 3, 100, 1, 40, NULL);


-- ==================== 2. 模型主表 ====================
INSERT INTO `superagent_model`
(`id`, `model_code`,                `model_name`,            `provider_id`, `model_type`,   `capabilities`,                                                       `context_window`, `max_output_tokens`, `input_price_per_1m`, `output_price_per_1m`, `currency`, `billing_unit`, `param_constraints`, `status`, `is_recommended`, `sort_order`, `description`,                                                                 `icon_url`, `tags`)
VALUES
-- ===== DeepSeek =====
( 1, 'deepseek-v4-flash',          'DeepSeek V4 Flash',     1, 'llm',        '["chat","function_calling","json_mode","fim"]',                       1048576,           384000,               1.000000,             2.000000,              'CNY',      'token',        '{"temperature":{"min":0,"max":2},"topP":{"min":0,"max":1},"topK":{"min":1,"max":100},"maxTokens":{"min":1,"max":384000},"enableSearch":{"min":0,"max":1}}',             1,        1,                10,           'DeepSeek 最新一代旗舰模型Flash版，支持1M超长上下文与384K输出，兼顾速度与成本。',              NULL,       '["new","hot"]'),
( 2, 'deepseek-v4-pro',            'DeepSeek V4 Pro',       1, 'llm',        '["chat","reasoning","function_calling","json_mode","fim"]',          1048576,           384000,              12.000000,             24.000000,             'CNY',      'token',        '{"temperature":{"min":0,"max":2},"topP":{"min":0,"max":1},"topK":{"min":1,"max":100},"maxTokens":{"min":1,"max":384000},"thinkingBudget":{"min":0,"max":128000},"enableThinking":{"min":0,"max":1},"enableSearch":{"min":0,"max":1}}',             1,        0,                20,           'DeepSeek V4 Pro旗舰版，支持思考/非思考双模式，更强的Agent与推理能力。1M上下文窗口。',      NULL,       '["new"]'),
( 3, 'deepseek-v3.2',              'DeepSeek V3.2',        1, 'llm',        '["chat","function_calling","json_mode"]',                             163840,            32768,                2.000000,             2.000000,              'CNY',      'token',        '{"temperature":{"min":0,"max":2},"topP":{"min":0,"max":1},"topK":{"min":1,"max":100},"maxTokens":{"min":1,"max":32768}}',             1,        0,                30,           'DeepSeek V3.2 稳定版，平衡推理能力与输出长度，适合日常问答与通用Agent任务。',             NULL,       NULL),

-- ===== Kimi / Moonshot =====
( 4, 'kimi-k2.5',                  'Kimi K2.5',            2, 'llm',        '["chat","vision","function_calling","json_mode","reasoning"]',        262144,            131072,               4.000000,            12.000000,              'CNY',      'token',        '{"temperature":{"min":0,"max":2},"topP":{"min":0,"max":1},"maxTokens":{"min":1,"max":131072},"thinkingBudget":{"min":0,"max":65536},"enableThinking":{"min":0,"max":1}}',             1,        1,                10,           'Kimi 最智能的多模态模型，支持视觉与文本输入、思考与非思考模式。256K上下文。',             NULL,       '["new","hot"]'),
( 5, 'kimi-k2-instruct-0905',      'Kimi K2 Instruct',     2, 'llm',        '["chat","function_calling","json_mode","agent"]',                     262144,            131072,               4.000000,            16.000000,              'CNY',      'token',        '{"temperature":{"min":0,"max":2},"topP":{"min":0,"max":1},"maxTokens":{"min":1,"max":131072}}',             1,        0,                20,           'Kimi K2 指令版，超强代码和Agent能力的MoE模型，256K上下文。',                           NULL,       '["new"]'),
( 6, 'moonshot-v1-8k',             'Moonshot V1 8K',       2, 'llm',        '["chat"]',                                                             8192,              4096,                12.000000,            12.000000,              'CNY',      'token',        '{"temperature":{"min":0,"max":2},"maxTokens":{"min":1,"max":4096}}',             1,        0,                30,           'Moonshot 经典模型，8K上下文窗口，适合简单对话。',                                         NULL,       NULL),
( 7, 'moonshot-v1-32k',            'Moonshot V1 32K',      2, 'llm',        '["chat"]',                                                            32768,             16384,               24.000000,            24.000000,              'CNY',      'token',        '{"temperature":{"min":0,"max":2},"maxTokens":{"min":1,"max":16384}}',             1,        0,                40,           'Moonshot 经典模型，32K上下文窗口，适合中等长度文本处理。',                               NULL,       NULL),
( 8, 'moonshot-v1-128k',           'Moonshot V1 128K',     2, 'llm',        '["chat"]',                                                           131072,             65536,               60.000000,            60.000000,              'CNY',      'token',        '{"temperature":{"min":0,"max":2},"maxTokens":{"min":1,"max":65536}}',             1,        0,                50,           'Moonshot 经典模型，128K上下文窗口，适合长文档处理。',                                    NULL,       NULL),

-- ===== 智谱AI / GLM =====
( 9, 'glm-5',                      'GLM-5',                3, 'llm',        '["chat","function_calling","json_mode","reasoning"]',                 131072,            32768,                4.000000,            18.000000,              'CNY',      'token',        '{"temperature":{"min":0.01,"max":0.99},"topP":{"min":0.01,"max":0.99},"maxTokens":{"min":1,"max":32768},"thinkingBudget":{"min":0,"max":16384},"enableThinking":{"min":0,"max":1},"enableSearch":{"min":0,"max":1}}',             1,        1,                10,           '智谱AI 最新旗舰模型，744B参数MoE架构，超强推理与Agent能力。128K上下文。',                NULL,       '["new","hot"]'),
(10, 'glm-5-turbo',                'GLM-5 Turbo',          3, 'llm',        '["chat","function_calling","json_mode"]',                             131072,            16384,                5.000000,            22.000000,              'CNY',      'token',        '{"temperature":{"min":0.01,"max":0.99},"topP":{"min":0.01,"max":0.99},"maxTokens":{"min":1,"max":16384},"enableSearch":{"min":0,"max":1}}',             1,        0,                20,           'GLM-5 Turbo 高速版，追求更低延迟的推理体验。128K上下文。',                                NULL,       '["new"]'),
(11, 'glm-4.7',                    'GLM-4.7',              3, 'llm',        '["chat","function_calling","json_mode"]',                             131072,            16384,                2.000000,             8.000000,              'CNY',      'token',        '{"temperature":{"min":0.01,"max":0.99},"topP":{"min":0.01,"max":0.99},"maxTokens":{"min":1,"max":16384}}',             1,        0,                30,           'GLM-4.7 高性价比模型，效果与成本均衡之选。128K上下文。',                                  NULL,       NULL),
(12, 'glm-4.6v',                   'GLM-4.6V',             3, 'multimodal', '["chat","vision","function_calling"]',                                131072,             4096,                1.000000,             3.000000,              'CNY',      'token',        '{"temperature":{"min":0.01,"max":0.99},"topP":{"min":0.01,"max":0.99},"maxTokens":{"min":1,"max":4096}}',             1,        0,                40,           'GLM-4.6V 多模态模型，支持图像、视频、文件等多模态输入。128K上下文。',                      NULL,       NULL),
(13, 'glm-4.6v-flash',             'GLM-4.6V Flash',       3, 'multimodal', '["chat","vision"]',                                                   131072,             4096,                0.150000,             1.500000,              'CNY',      'token',        '{"temperature":{"min":0.01,"max":0.99},"topP":{"min":0.01,"max":0.99},"maxTokens":{"min":1,"max":4096}}',             1,        0,                50,           'GLM-4.6V Flash 免费多模态模型，支持图像和视频理解。128K上下文。限时免费。',                NULL,       '["free"]'),

-- ===== 阿里云百炼 / DashScope =====
(14, 'qwen3.5-plus',               'Qwen3.5 Plus',         4, 'llm',        '["chat","vision","function_calling","json_mode","reasoning"]',       1048576,           131072,               0.800000,             4.800000,              'CNY',      'token',        '{"temperature":{"min":0,"max":2},"topP":{"min":0,"max":1},"topK":{"min":1,"max":100},"maxTokens":{"min":1,"max":131072},"thinkingBudget":{"min":0,"max":65536},"enableThinking":{"min":0,"max":1},"enableSearch":{"min":0,"max":1}}',             1,        1,                10,           '通义千问 最新旗舰多模态模型，1M超长上下文，效果/速度/成本均衡。',                         NULL,       '["new","hot"]'),
(15, 'qwen3-max',                  'Qwen3 Max',            4, 'llm',        '["chat","function_calling","json_mode"]',                             262144,            65536,               2.500000,            10.000000,              'CNY',      'token',        '{"temperature":{"min":0,"max":2},"topP":{"min":0,"max":1},"maxTokens":{"min":1,"max":65536},"enableSearch":{"min":0,"max":1}}',             1,        0,                20,           '通义千问 旗舰模型，适合复杂任务，能力最强。256K上下文。',                                    NULL,       NULL),
(16, 'qwen3.5-flash',              'Qwen3.5 Flash',        4, 'llm',        '["chat","function_calling","json_mode"]',                            1048576,            65536,               0.200000,             1.200000,              'CNY',      'token',        '{"temperature":{"min":0,"max":2},"topP":{"min":0,"max":1},"maxTokens":{"min":1,"max":65536},"enableSearch":{"min":0,"max":1}}',             1,        0,                30,           '通义千问 Flash 轻量模型，适合简单任务，速度最快、成本最低。1M上下文。',                    NULL,       '["new"]'),
(17, 'qwen-plus',                  'Qwen Plus',            4, 'llm',        '["chat","function_calling","json_mode"]',                             131072,            32768,               0.800000,             2.000000,              'CNY',      'token',        '{"temperature":{"min":0,"max":2},"topP":{"min":0,"max":1},"maxTokens":{"min":1,"max":32768}}',             1,        0,                40,           '通义千问 Plus 增强版，效果与速度平衡。128K上下文。',                                       NULL,       NULL),
(18, 'qwen-turbo',                 'Qwen Turbo',           4, 'llm',        '["chat"]',                                                            131072,            16384,               0.300000,             0.600000,              'CNY',      'token',        '{"temperature":{"min":0,"max":2},"topP":{"min":0,"max":1},"maxTokens":{"min":1,"max":16384}}',             1,        0,                50,           '通义千问 Turbo 经济版，超低价格，适合高并发简单场景。128K上下文。',                        NULL,       NULL);


-- ==================== 3. 模型分组表 ====================
INSERT INTO `superagent_model_group`
(`id`, `group_name`,   `group_code`,  `sort_order`, `status`)
VALUES
    (1, '大语言模型',      'llm',          10,           1),
    (2, '推理模型',        'reasoning',    20,           1),
    (3, '多模态模型',      'multimodal',   30,           1),
    (4, '代码模型',        'code',         40,           1);


-- ==================== 4. 分组关联表 ====================
INSERT INTO `superagent_model_group_relation`
(`model_id`, `group_id`)
VALUES
-- ===== 大语言模型 (group_id=1) → 所有模型 =====
(1,  1), (2,  1), (3,  1),
(4,  1), (5,  1), (6,  1), (7,  1), (8,  1),
(9,  1), (10, 1), (11, 1), (12, 1), (13, 1),
(14, 1), (15, 1), (16, 1), (17, 1), (18, 1),

-- ===== 推理模型 (group_id=2) =====
-- DeepSeek V4 Pro (思考模式)、Kimi K2.5 (思考模式)、GLM-5、Qwen3.5 Plus
(2,  2),
(4,  2),
(9,  2),
(14, 2),

-- ===== 多模态模型 (group_id=3) =====
-- Kimi K2.5、GLM-4.6V、GLM-4.6V Flash、Qwen3.5 Plus
(4,  3),
(12, 3),
(13, 3),
(14, 3),

-- ===== 代码模型 (group_id=4) =====
-- DeepSeek V4 Pro、DeepSeek V4 Flash(有FIM)、Kimi K2 Instruct、Qwen3.5 Plus
(1,  4),
(2,  4),
(5,  4),
(14, 4);
