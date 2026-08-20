-- Unknown how to generate base type type

alter type vector owner to postgres;

-- Unknown how to generate base type type

alter type halfvec owner to postgres;

-- Unknown how to generate base type type

alter type sparsevec owner to postgres;

-- Unknown how to generate base type type

alter type hstore owner to postgres;

-- Unknown how to generate base type type

alter type ghstore owner to postgres;

create table ollama_vector_store
(
    id        uuid default uuid_generate_v4() not null
        primary key,
    content   text,
    metadata  json,
    embedding vector(1536)
);

alter table ollama_vector_store
    owner to agent_mesh;

create index ollama_vector_store_index
    on ollama_vector_store using hnsw (embedding vector_cosine_ops);

create table openai_vector_store
(
    id        uuid default uuid_generate_v4() not null
        primary key,
    content   text,
    metadata  json,
    embedding vector(1536)
);

alter table openai_vector_store
    owner to agent_mesh;

create index openai_vector_store_index
    on openai_vector_store using hnsw (embedding vector_cosine_ops);

create table "user"
(
    id               bigserial
        primary key,
    username         varchar(50)         not null
        unique,
    password_hash    varchar(255)        not null,
    email            varchar(100),
    user_role        integer   default 0 not null,
    experience_level integer   default 0,
    created_at       timestamp default CURRENT_TIMESTAMP,
    updated_at       timestamp default CURRENT_TIMESTAMP,
    is_delete        smallint  default 0,
    member_expire_at timestamp
);

comment on table "user" is '用户信息表';

comment on column "user".id is '用户id';

comment on column "user".username is '用户名';

comment on column "user".password_hash is '密码';

comment on column "user".email is '邮箱';

comment on column "user".user_role is '用户角色 (INT 类型)-0: 正式会员, -1: 大会员, 90: 管理员, 99: 超级管理员';

comment on column "user".experience_level is '经验值';

comment on column "user".created_at is '创建时间';

comment on column "user".updated_at is '更新时间';

comment on column "user".is_delete is '逻辑删除';

comment on column "user".member_expire_at is '会员过期时间';

alter table "user"
    owner to postgres;

grant select, update, usage on sequence user_id_seq to agent_mesh;

create index idx_user_role
    on "user" (user_role, is_delete);

create index idx_user_member
    on "user" (experience_level, member_expire_at, is_delete);

grant delete, insert, references, select, trigger, truncate, update on "user" to agent_mesh;

create table sys_dict
(
    id            bigserial
        primary key,
    dict_type     varchar(50)  not null,
    dict_key      integer      not null,
    dict_value    varchar(100) not null,
    dict_ext_json jsonb,
    sort_order    integer  default 0,
    is_delete     smallint default 0,
    unique (dict_type, dict_key)
);

comment on column sys_dict.dict_type is '字典对应字段名';

comment on column sys_dict.dict_key is '字典建';

comment on column sys_dict.dict_value is '字典值';

comment on column sys_dict.dict_ext_json is '额外信息';

comment on column sys_dict.is_delete is '逻辑删除';

alter table sys_dict
    owner to postgres;

grant select, update, usage on sequence sys_dict_id_seq to agent_mesh;

grant delete, insert, references, select, trigger, truncate, update on sys_dict to agent_mesh;

create table model_provider
(
    id                   bigserial
        primary key,
    user_id              bigint       not null
        references "user",
    provider_name        varchar(50)  not null,
    provider_code        varchar(50),
    base_url             varchar(255) not null,
    api_key_encrypted    text,
    api_secret_encrypted text,
    status               smallint  default 1,
    is_public            boolean   default false,
    is_delete            smallint  default 0,
    created_at           timestamp default CURRENT_TIMESTAMP,
    updated_at           timestamp default CURRENT_TIMESTAMP,
    unique (user_id, provider_name)
);

comment on table model_provider is '模型提供商配置表：存储用户配置的 LLM 服务商信息 (如 OpenAI, Azure, Ollama 等)';

comment on column model_provider.id is '主键：自增 ID';

comment on column model_provider.user_id is '归属用户ID：关联到具体用户，实现多租户数据隔离';

comment on column model_provider.provider_name is '提供商名称：用户自定义的显示名称 (例："我的个人 OpenAI")';

comment on column model_provider.provider_code is '提供商代码：标准化标识 (例："openai", "azure")，用于前端图标匹配或逻辑路由';

comment on column model_provider.base_url is 'API 基础地址：用户配置的 API 入口 URL (例：https://api.openai.com/v1)';

comment on column model_provider.api_key_encrypted is '加密 API Key：敏感字段，必须经应用层加密后存储，严禁明文';

comment on column model_provider.api_secret_encrypted is '加密 API Secret：可选，部分厂商 (如 Azure) 需要的密钥，需加密存储';

comment on column model_provider.status is '启用状态：1=启用, 0=禁用。禁用后该提供商下所有模型不可用';

comment on column model_provider.is_public is '是否公开：false=仅自己可见; true=共享给同组织/团队 (预留功能)';

comment on column model_provider.is_delete is '逻辑删除标记：0=正常, 1=已删除';

comment on column model_provider.created_at is '创建时间';

comment on column model_provider.updated_at is '最后更新时间';

alter table model_provider
    owner to postgres;

grant select, update, usage on sequence model_provider_id_seq to agent_mesh;

create index idx_provider_user_active
    on model_provider (user_id, status, is_delete);

grant delete, insert, references, select, trigger, truncate, update on model_provider to agent_mesh;

create table ai_model
(
    id                 bigserial
        primary key,
    user_id            bigint       not null
        references "user",
    provider_id        bigint       not null
        references model_provider,
    model_name         varchar(100) not null,
    model_display_name varchar(100),
    model_type         varchar(20)  not null,
    context_window     integer,
    max_tokens         integer,
    input_cost_per_1k  numeric(10, 6) default 0,
    output_cost_per_1k numeric(10, 6) default 0,
    currency_type      varchar(10)    default 'CNY'::character varying,
    is_active          boolean        default true,
    is_delete          smallint       default 0,
    created_at         timestamp      default CURRENT_TIMESTAMP,
    updated_at         timestamp      default CURRENT_TIMESTAMP,
    unique (user_id, provider_id, model_name)
);

comment on table ai_model is '模型实例表：存储具体可用的模型列表 (如 gpt-4, qwen-turbo)，关联到具体的 Provider';

comment on column ai_model.id is '主键：自增 ID';

comment on column ai_model.user_id is '归属用户ID：冗余字段，加速“查询某用户所有可用模型”的场景，避免连表';

comment on column ai_model.provider_id is '所属提供商ID：关联 model_provider 表';

comment on column ai_model.model_name is '模型代码名：调用 API 时使用的标准标识 (例："gpt-4-turbo")';

comment on column ai_model.model_display_name is '模型显示名：前端展示的友好名称 (例："GPT-4 Turbo (高速版)")';

comment on column ai_model.model_type is '模型类型：能力分类，枚举值 [CHAT, EMBEDDING, IMAGE]';

comment on column ai_model.context_window is '上下文窗口：模型支持的最大 Token 数 (输入 + 输出)';

comment on column ai_model.max_tokens is '最大输出长度：单次生成允许的最大 Token 数限制';

comment on column ai_model.input_cost_per_1k is '输入成本：每 1k 输入 Token 的费用 (用户可自定义，用于统计)';

comment on column ai_model.output_cost_per_1k is '输出成本：每 1k 输出 Token 的费用 (用户可自定义，用于统计)';

comment on column ai_model.currency_type is '货币单位：成本统计的币种 (例："CNY", "USD")';

comment on column ai_model.is_active is '是否活跃：false 表示暂时不在智能体配置列表中显示，但保留数据';

comment on column ai_model.is_delete is '逻辑删除标记：0=正常, 1=已删除';

comment on column ai_model.created_at is '创建时间';

comment on column ai_model.updated_at is '最后更新时间';

alter table ai_model
    owner to postgres;

grant select, update, usage on sequence ai_model_id_seq to agent_mesh;

create index idx_model_user_active
    on ai_model (user_id, is_active, is_delete);

create index idx_model_provider
    on ai_model (provider_id);

grant delete, insert, references, select, trigger, truncate, update on ai_model to agent_mesh;

create table mcp_servers
(
    id                    bigserial
        primary key,
    owner_id              bigint                                       not null
        references "user",
    server_name           varchar(100)                                 not null,
    transport_type        varchar(20) default 'SSE'::character varying not null,
    endpoint_url          varchar(500),
    command_args          jsonb,
    auth_config_encrypted text,
    env_vars_encrypted    text,
    status                smallint    default 1,
    last_heartbeat        timestamp,
    is_delete             smallint    default 0,
    created_at            timestamp   default CURRENT_TIMESTAMP,
    updated_at            timestamp   default CURRENT_TIMESTAMP,
    constraint chk_mcp_url_or_cmd
        check ((((transport_type)::text = ANY
                 ((ARRAY ['SSE'::character varying, 'STREAMABLE_HTTP'::character varying])::text[])) AND
                (endpoint_url IS NOT NULL)) OR (((transport_type)::text = 'STDIO'::text) AND (command_args IS NOT NULL)))
);

comment on table mcp_servers is 'MCP 服务器配置表：存储用户配置的 Model Context Protocol 服务连接信息';

comment on column mcp_servers.id is '主键：自增 ID';

comment on column mcp_servers.owner_id is '所有者用户ID：MCP 服务的归属用户';

comment on column mcp_servers.server_name is '服务名称：用户自定义的友好名称 (例："本地文件读取服务")';

comment on column mcp_servers.transport_type is '传输协议：枚举值 [SSE, STDIO, STREAMABLE_HTTP]';

comment on column mcp_servers.endpoint_url is '接入 URL：SSE 或 HTTP 模式下的服务端地址 (STDIO 模式下为空)';

comment on column mcp_servers.command_args is '启动命令参数：JSON 数组，STDIO 模式下启动进程的命令和参数 (例：["npx", "-y", "..."])';

comment on column mcp_servers.auth_config_encrypted is '加密认证配置：存储 Header Token 或 Basic Auth 密码等，需加密';

comment on column mcp_servers.env_vars_encrypted is '加密环境变量：MCP 服务运行所需的环境变量，需加密';

comment on column mcp_servers.status is '服务状态：1=运行中, 0=停止';

comment on column mcp_servers.last_heartbeat is '最后心跳时间：由后端定时任务更新，用于监控服务在线状态';

comment on column mcp_servers.is_delete is '逻辑删除标记：0=正常, 1=已删除';

comment on column mcp_servers.created_at is '创建时间';

comment on column mcp_servers.updated_at is '最后更新时间';

alter table mcp_servers
    owner to postgres;

grant select, update, usage on sequence mcp_servers_id_seq to agent_mesh;

create index idx_mcp_servers_owner
    on mcp_servers (owner_id, status, is_delete);

grant delete, insert, references, select, trigger, truncate, update on mcp_servers to agent_mesh;

create table agent
(
    id                       bigserial
        primary key,
    user_id                  bigint       not null
        references "user",
    name                     varchar(100) not null,
    description              text,
    avatar_url               varchar(255),
    system_prompt            text         not null,
    role_definition          text,
    decision_model_id        bigint,
    response_model_id        bigint,
    is_tool_enabled          boolean     default false,
    tool_schema_json         jsonb,
    tool_description         varchar(500),
    version                  varchar(20) default '1.0.0'::character varying,
    status                   smallint    default 1,
    is_delete                smallint    default 0,
    created_at               timestamp   default CURRENT_TIMESTAMP,
    updated_at               timestamp   default CURRENT_TIMESTAMP,
    visibility               smallint    default 0,
    team_ids                 jsonb,
    model_selection_strategy varchar(50) default 'ADAPTIVE'::character varying,
    budget_constraint        numeric(10, 4)
);

comment on table agent is '智能体主表：存储用户创建的 AI 智能体配置';

comment on column agent.id is '主键：自增 ID';

comment on column agent.user_id is '归属用户ID：智能体的创建者';

comment on column agent.name is '智能体名称';

comment on column agent.description is '智能体简介';

comment on column agent.avatar_url is '头像 URL';

comment on column agent.system_prompt is '系统提示词：定义智能体核心行为和角色的 Prompt';

comment on column agent.role_definition is '角色定义补充：额外的角色设定描述';

comment on column agent.decision_model_id is '决策模型 ID：负责思考、规划、调用工具的模型 (高智力模型)';

comment on column agent.response_model_id is '回复模型 ID：负责最终生成文本的模型 (可是低成本模型)';

comment on column agent.is_tool_enabled is '是否启用工具：false 表示该智能体不使用任何工具';

comment on column agent.tool_schema_json is '工具配置覆写：智能体级别的特定工具参数配置';

comment on column agent.tool_description is '工具描述微调：针对该智能体的工具描述优化';

comment on column agent.version is '配置版本号：用于版本管理或回滚';

comment on column agent.status is '智能体状态：1=发布, 0=草稿/停用';

comment on column agent.is_delete is '逻辑删除标记：0=正常, 1=已删除';

comment on column agent.created_at is '创建时间';

comment on column agent.updated_at is '最后更新时间';

comment on column agent.visibility is '可见性级别：0=私有，1=团队共享，2=公开';

comment on column agent.team_ids is '团队 ID 列表（JSON 格式）';

alter table agent
    owner to postgres;

grant select, update, usage on sequence agent_id_seq to agent_mesh;

create index idx_agent_user
    on agent (user_id, is_delete);

create index idx_agent_visibility
    on agent (visibility, user_id);

grant delete, insert, references, select, trigger, truncate, update on agent to agent_mesh;

create table agent_tool_relation
(
    id            bigserial
        primary key,
    agent_id      bigint      not null
        references agent
            on delete cascade,
    tool_type     varchar(20) not null,
    tool_ref_id   bigint      not null,
    config_params jsonb,
    sort_order    integer  default 0,
    is_delete     smallint default 0,
    unique (agent_id, tool_ref_id)
);

comment on table agent_tool_relation is '智能体 - 工具关联表：定义某个智能体可以使用哪些工具 (多对多关系)';

comment on column agent_tool_relation.id is '主键：自增 ID';

comment on column agent_tool_relation.agent_id is '所属智能体 ID：关联 agent 表，级联删除';

comment on column agent_tool_relation.tool_type is '工具来源类型：冗余字段，记录工具来源 (SYSTEM/USER_HTTP/USER_MCP)，便于快速过滤';

comment on column agent_tool_relation.tool_ref_id is '工具引用 ID：关联 tools 表的主键';

comment on column agent_tool_relation.config_params is '工具特定配置：JSON 格式，针对该智能体对该工具的参数覆写 (如 timeout, 特定 key)';

comment on column agent_tool_relation.sort_order is '排序顺序：工具在列表中的显示顺序或调用优先级';

comment on column agent_tool_relation.is_delete is '逻辑删除标记：0=正常, 1=已删除 (用于暂时移除工具)';

alter table agent_tool_relation
    owner to postgres;

grant select, update, usage on sequence agent_tool_relation_id_seq to agent_mesh;

create index idx_agent_tool_agent
    on agent_tool_relation (agent_id, is_delete);

grant delete, insert, references, select, trigger, truncate, update on agent_tool_relation to agent_mesh;

create table knowledge_base
(
    id                 bigserial
        primary key,
    user_id            bigint       not null,
    name               varchar(200) not null,
    description        text,
    vector_store_type  varchar(20)  not null,
    vector_store_table varchar(100) not null,
    embedding_model_id bigint,
    chunk_size         integer   default 500,
    chunk_overlap      integer   default 50,
    status             smallint  default 1,
    is_delete          smallint  default 0,
    created_at         timestamp default CURRENT_TIMESTAMP,
    updated_at         timestamp default CURRENT_TIMESTAMP,
    visibility         smallint  default 0,
    team_ids           jsonb
);

comment on table knowledge_base is '知识库主表';

comment on column knowledge_base.visibility is '可见性级别：0=私有，1=团队共享，2=公开';

comment on column knowledge_base.team_ids is '团队 ID 列表（JSON 格式）';

alter table knowledge_base
    owner to postgres;

grant select, update, usage on sequence knowledge_base_id_seq to agent_mesh;

create index idx_kb_user
    on knowledge_base (user_id, is_delete);

create index idx_kb_visibility
    on knowledge_base (visibility, user_id);

grant delete, insert, references, select, trigger, truncate, update on knowledge_base to agent_mesh;

create table knowledge_base_document
(
    id               bigserial
        primary key,
    kb_id            bigint       not null
        constraint kb_doc_kb_id_fkey
            references knowledge_base
            on delete cascade,
    doc_name         varchar(200) not null,
    doc_type         varchar(20)  not null,
    source_url       varchar(500),
    content_hash     varchar(64),
    chunk_count      integer   default 0,
    vector_ids       jsonb,
    metadata_json    jsonb,
    status           smallint  default 1,
    is_delete        smallint  default 0,
    created_at       timestamp default CURRENT_TIMESTAMP,
    updated_at       timestamp default CURRENT_TIMESTAMP,
    related_tool_ids jsonb     default '[]'::jsonb
);

comment on table knowledge_base_document is '知识库文档表';

comment on column knowledge_base_document.id is '主键：自增 ID';

comment on column knowledge_base_document.kb_id is '所属知识库 ID';

comment on column knowledge_base_document.doc_name is '文档名称';

comment on column knowledge_base_document.doc_type is '文档类型：[TEXT, PDF, WORD, EXCEL, MARKDOWN, URL]';

comment on column knowledge_base_document.source_url is '源文件 URL 或路径';

comment on column knowledge_base_document.content_hash is '内容哈希：用于去重和版本控制';

comment on column knowledge_base_document.chunk_count is '分块数量';

comment on column knowledge_base_document.vector_ids is '向量 ID 列表：存储在向量数据库中的 ID 数组';

comment on column knowledge_base_document.metadata_json is '元数据：作者、创建时间等额外信息';

comment on column knowledge_base_document.status is '状态：1=处理完成，0=处理中，-1=处理失败';

comment on column knowledge_base_document.is_delete is '逻辑删除标记';

comment on column knowledge_base_document.created_at is '创建时间';

comment on column knowledge_base_document.updated_at is '最后更新时间';

comment on column knowledge_base_document.related_tool_ids is '关联工具 ID 列表：JSON 数组格式，用于 RAG 驱动的工具推荐';

alter table knowledge_base_document
    owner to postgres;

grant select, update, usage on sequence knowledge_base_document_id_seq to agent_mesh;

create index idx_kb_doc_kb
    on knowledge_base_document (kb_id, is_delete);

create index idx_kb_doc_related_tools
    on knowledge_base_document using gin (related_tool_ids);

grant delete, insert, references, select, trigger, truncate, update on knowledge_base_document to agent_mesh;

create table agent_kb_relation
(
    id                   bigserial
        primary key,
    agent_id             bigint not null
        references agent
            on delete cascade,
    kb_id                bigint not null
        references knowledge_base
            on delete cascade,
    search_top_k         integer       default 3,
    similarity_threshold numeric(3, 2) default 0.7,
    sort_order           integer       default 0,
    is_delete            smallint      default 0,
    created_at           timestamp     default CURRENT_TIMESTAMP,
    updated_at           timestamp,
    constraint agent_kb_agent_id_kb_id_key
        unique (agent_id, kb_id)
);

comment on table agent_kb_relation is '智能体 - 知识库关联表：定义某个智能体可以使用哪些知识库';

comment on column agent_kb_relation.updated_at is '更新时间';

alter table agent_kb_relation
    owner to postgres;

grant select, update, usage on sequence agent_kb_relation_id_seq to agent_mesh;

create index idx_agent_kb_agent
    on agent_kb_relation (agent_id, is_delete);

grant delete, insert, references, select, trigger, truncate, update on agent_kb_relation to agent_mesh;

create table model_usage_cost
(
    id            bigserial
        primary key,
    user_id       bigint      not null,
    agent_id      bigint      not null,
    model_id      bigint      not null,
    model_type    varchar(20) not null,
    input_tokens  integer        default 0,
    output_tokens integer        default 0,
    total_tokens  integer        default 0,
    cost          numeric(10, 6) default 0,
    currency_type varchar(10)    default 'CNY'::character varying,
    status        smallint       default 1,
    created_at    timestamp      default CURRENT_TIMESTAMP
);

comment on table model_usage_cost is '模型调用成本记录表';

comment on column model_usage_cost.id is '主键 ID';

comment on column model_usage_cost.user_id is '用户 ID';

comment on column model_usage_cost.agent_id is '智能体 ID';

comment on column model_usage_cost.model_id is '模型 ID';

comment on column model_usage_cost.model_type is '模型类型：INTERNAL_DECISION, FINAL_RESPONSE';

comment on column model_usage_cost.input_tokens is '输入 Token 数';

comment on column model_usage_cost.output_tokens is '输出 Token 数';

comment on column model_usage_cost.total_tokens is '总 Token 数';

comment on column model_usage_cost.cost is '成本（元）';

comment on column model_usage_cost.currency_type is '货币类型';

comment on column model_usage_cost.status is '调用状态：1=SUCCESS, 0=FAILED';

comment on column model_usage_cost.created_at is '创建时间';

alter table model_usage_cost
    owner to postgres;

grant select, update, usage on sequence model_usage_cost_id_seq to agent_mesh;

create index idx_cost_user
    on model_usage_cost (user_id, created_at);

create index idx_cost_agent
    on model_usage_cost (agent_id, created_at);

grant delete, insert, references, select, trigger, truncate, update on model_usage_cost to agent_mesh;

create table conversation_log
(
    id                  bigserial
        primary key,
    user_id             bigint       not null
        constraint conv_log_user_id_fkey
            references "user",
    agent_id            bigint       not null
        constraint conv_log_agent_id_fkey
            references agent,
    session_id          varchar(100) not null,
    user_query          text         not null,
    final_response      text,
    intent_type         varchar(50),
    intent_confidence   numeric(3, 2),
    decision_path       jsonb,
    invoked_tool_ids    jsonb,
    searched_kb_ids     jsonb,
    decision_model_id   bigint
        constraint conv_log_decision_model_id_fkey
            references ai_model,
    response_model_id   bigint
        constraint conv_log_response_model_id_fkey
            references ai_model,
    total_input_tokens  integer        default 0,
    total_output_tokens integer        default 0,
    total_cost          numeric(10, 6) default 0,
    execution_time_ms   bigint         default 0,
    status              smallint       default 1,
    error_message       text,
    user_rating         smallint,
    user_feedback       text,
    created_at          timestamp      default CURRENT_TIMESTAMP,
    updated_at          timestamp      default CURRENT_TIMESTAMP
);

comment on table conversation_log is '对话日志主表';

comment on column conversation_log.id is '主键 ID';

comment on column conversation_log.user_id is '用户 ID';

comment on column conversation_log.agent_id is '智能体 ID';

comment on column conversation_log.session_id is '会话 ID（用于 grouping 多轮对话）';

comment on column conversation_log.user_query is '用户问题';

comment on column conversation_log.final_response is '最终回答';

comment on column conversation_log.intent_type is '识别的意图类型';

comment on column conversation_log.intent_confidence is '意图置信度 (0-1)';

comment on column conversation_log.decision_path is '决策路径（JSON 格式）';

comment on column conversation_log.invoked_tool_ids is '调用的工具 ID 列表';

comment on column conversation_log.searched_kb_ids is '检索的知识库 ID 列表';

comment on column conversation_log.decision_model_id is '使用的决策模型 ID';

comment on column conversation_log.response_model_id is '使用的回复模型 ID';

comment on column conversation_log.total_input_tokens is '总输入 Token 数';

comment on column conversation_log.total_output_tokens is '总输出 Token 数';

comment on column conversation_log.total_cost is '总成本（元）';

comment on column conversation_log.execution_time_ms is '执行耗时（毫秒）';

comment on column conversation_log.status is '对话状态：1=SUCCESS, 0=FAILED, -1=PARTIAL_SUCCESS';

comment on column conversation_log.error_message is '错误信息（如果有）';

comment on column conversation_log.user_rating is '用户反馈评分（1-5 星）';

comment on column conversation_log.user_feedback is '用户反馈备注';

comment on column conversation_log.created_at is '创建时间';

comment on column conversation_log.updated_at is '最后更新时间';

alter table conversation_log
    owner to postgres;

grant select, update, usage on sequence conversation_log_id_seq to agent_mesh;

create index idx_conv_log_user
    on conversation_log (user_id, created_at);

create index idx_conv_log_agent
    on conversation_log (agent_id, created_at);

create index idx_conv_log_session
    on conversation_log (session_id);

create index idx_conv_log_intent
    on conversation_log (intent_type);

create index idx_conv_log_status
    on conversation_log (status, created_at);

grant delete, insert, references, select, trigger, truncate, update on conversation_log to agent_mesh;

create table user_cost_threshold
(
    id                     bigserial
        primary key,
    user_id                bigint not null,
    agent_id               bigint,
    daily_threshold        numeric(10, 2),
    weekly_threshold       numeric(10, 2),
    monthly_threshold      numeric(10, 2),
    total_threshold        numeric(10, 2),
    alert_enabled          boolean   default true,
    auto_downgrade_enabled boolean   default false,
    downgrade_strategy     varchar(50),
    target_model_id        bigint,
    notification_method    varchar(50),
    notification_target    varchar(500),
    last_alert_time        timestamp,
    alert_count_today      integer   default 0,
    status                 smallint  default 0,
    created_at             timestamp default CURRENT_TIMESTAMP,
    updated_at             timestamp default CURRENT_TIMESTAMP,
    constraint user_cost_threshold_user_id_key
        unique (user_id, agent_id)
);

comment on table user_cost_threshold is '用户成本阈值配置表';

comment on column user_cost_threshold.id is '主键 ID';

comment on column user_cost_threshold.user_id is '用户 ID';

comment on column user_cost_threshold.agent_id is '智能体 ID（可选，为空表示全局配置）';

comment on column user_cost_threshold.daily_threshold is '日成本阈值（元）';

comment on column user_cost_threshold.weekly_threshold is '周成本阈值（元）';

comment on column user_cost_threshold.monthly_threshold is '月成本阈值（元）';

comment on column user_cost_threshold.total_threshold is '总成本阈值（元）';

comment on column user_cost_threshold.alert_enabled is '是否启用告警';

comment on column user_cost_threshold.auto_downgrade_enabled is '是否启用自动降级';

comment on column user_cost_threshold.downgrade_strategy is '降级策略：DOWNGRADE_MODEL（降级模型）, DISABLE_AGENT（禁用智能体）, REDUCE_CALLS（限制调用）';

comment on column user_cost_threshold.target_model_id is '降级目标模型 ID（当策略为 DOWNGRADE_MODEL 时）';

comment on column user_cost_threshold.notification_method is '通知方式：EMAIL（邮件）, SMS（短信）, WEBHOOK（回调）';

comment on column user_cost_threshold.notification_target is '通知接收地址（邮箱/手机号/Webhook URL）';

comment on column user_cost_threshold.last_alert_time is '最后告警时间';

comment on column user_cost_threshold.alert_count_today is '告警次数（今日）';

comment on column user_cost_threshold.status is '状态：0-正常，1-已暂停';

comment on column user_cost_threshold.created_at is '创建时间';

comment on column user_cost_threshold.updated_at is '更新时间';

alter table user_cost_threshold
    owner to postgres;

grant select, update, usage on sequence user_cost_threshold_id_seq to agent_mesh;

create index idx_user_cost_threshold_user_id
    on user_cost_threshold (user_id);

create index idx_user_cost_threshold_agent_id
    on user_cost_threshold (agent_id);

grant delete, insert, references, select, trigger, truncate, update on user_cost_threshold to agent_mesh;

create table agent_dependency
(
    id                  bigserial
        primary key,
    agent_id            bigint                                        not null
        references agent
            on delete cascade,
    depends_on_agent_id bigint                                        not null
        references agent
            on delete cascade,
    dependency_type     varchar(50) default 'CALL'::character varying not null,
    priority            integer     default 0,
    created_by          bigint                                        not null
        references "user",
    created_at          timestamp   default CURRENT_TIMESTAMP,
    updated_at          timestamp   default CURRENT_TIMESTAMP,
    constraint agent_dependency_unique_pair
        unique (agent_id, depends_on_agent_id)
);

comment on table agent_dependency is '智能体依赖关系表：存储智能体之间的依赖关系';

comment on column agent_dependency.id is '主键：自增 ID';

comment on column agent_dependency.agent_id is '智能体 ID';

comment on column agent_dependency.depends_on_agent_id is '被依赖的智能体 ID';

comment on column agent_dependency.dependency_type is '依赖类型：CALL(调用), DATA_SHARE(数据共享), WORKFLOW(工作流)';

comment on column agent_dependency.priority is '优先级：数字越小优先级越高';

comment on column agent_dependency.created_by is '创建人用户 ID';

comment on column agent_dependency.created_at is '创建时间';

comment on column agent_dependency.updated_at is '更新时间';

alter table agent_dependency
    owner to postgres;

grant select, update, usage on sequence agent_dependency_id_seq to agent_mesh;

create index idx_agent_dep_agent
    on agent_dependency (agent_id);

create index idx_agent_dep_depends_on
    on agent_dependency (depends_on_agent_id);

create index idx_agent_dep_user
    on agent_dependency (created_by);

grant delete, insert, references, select, trigger, truncate, update on agent_dependency to agent_mesh;

create table team
(
    id          bigserial
        primary key,
    name        varchar(100) not null,
    description varchar(500),
    owner_id    bigint       not null,
    member_ids  jsonb,
    created_at  timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at  timestamp with time zone default CURRENT_TIMESTAMP,
    is_delete   smallint                 default 0
);

comment on table team is '团队表';

comment on column team.id is '主键：自增 ID';

comment on column team.name is '团队名称';

comment on column team.description is '团队描述';

comment on column team.owner_id is '团队所有者 ID';

comment on column team.member_ids is '成员 ID 列表（JSON 格式）';

comment on column team.created_at is '创建时间';

comment on column team.updated_at is '更新时间';

comment on column team.is_delete is '逻辑删除标记';

alter table team
    owner to postgres;

grant select, update, usage on sequence team_id_seq to agent_mesh;

grant delete, insert, references, select, trigger, truncate, update on team to agent_mesh;

create table team_resource_share
(
    id              bigserial
        primary key,
    team_id         bigint      not null,
    resource_type   varchar(50) not null,
    resource_id     bigint      not null,
    permission_type smallint                 default 1,
    granted_by      bigint      not null,
    granted_at      timestamp with time zone default CURRENT_TIMESTAMP
);

comment on table team_resource_share is '团队资源共享表';

comment on column team_resource_share.id is '主键：自增 ID';

comment on column team_resource_share.team_id is '团队 ID';

comment on column team_resource_share.resource_type is '资源类型：AGENT, KNOWLEDGE_BASE, TOOL';

comment on column team_resource_share.resource_id is '资源 ID';

comment on column team_resource_share.permission_type is '权限类型：1=读取，2=写入，3=删除，4=管理';

comment on column team_resource_share.granted_by is '授权人 ID';

comment on column team_resource_share.granted_at is '授权时间';

alter table team_resource_share
    owner to postgres;

grant select, update, usage on sequence team_resource_share_id_seq to agent_mesh;

create index idx_team_resource
    on team_resource_share (team_id, resource_type, resource_id);

grant delete, insert, references, select, trigger, truncate, update on team_resource_share to agent_mesh;

create table dashscope_vector_store
(
    id        text not null
        primary key,
    content   text,
    metadata  jsonb,
    embedding vector(1024)
);

alter table dashscope_vector_store
    owner to postgres;

create index dashscope_vector_store_embedding_idx
    on dashscope_vector_store using ivfflat (embedding vector_cosine_ops);

grant delete, insert, references, select, trigger, truncate, update on dashscope_vector_store to agent_mesh;

create table vector_store
(
    id        uuid default uuid_generate_v4() not null
        primary key,
    content   text,
    metadata  json,
    embedding vector(1536)
);

alter table vector_store
    owner to agent_mesh;

create index spring_ai_vector_index
    on vector_store using hnsw (embedding vector_cosine_ops);

create table workflow_definition
(
    id                    bigserial
        primary key,
    workflow_name         varchar(200) not null,
    description           text,
    agent_id              bigint,
    version               varchar(50) default '1.0'::character varying,
    nodes_json            jsonb        not null,
    start_node_id         varchar(100),
    global_variables_json jsonb,
    timeout_ms            bigint      default 60000,
    enabled               boolean     default true,
    user_id               bigint       not null,
    created_at            timestamp   default CURRENT_TIMESTAMP,
    updated_at            timestamp   default CURRENT_TIMESTAMP,
    is_delete             smallint    default 0
);

comment on table workflow_definition is '工作流定义表';

comment on column workflow_definition.id is '主键ID';

comment on column workflow_definition.workflow_name is '工作流名称';

comment on column workflow_definition.description is '工作流描述';

comment on column workflow_definition.agent_id is '关联的智能体ID';

comment on column workflow_definition.version is '工作流版本';

comment on column workflow_definition.nodes_json is '节点定义(JSON格式)';

comment on column workflow_definition.start_node_id is '起始节点ID';

comment on column workflow_definition.global_variables_json is '全局变量(JSON格式)';

comment on column workflow_definition.timeout_ms is '超时时间(毫秒)';

comment on column workflow_definition.enabled is '是否启用';

comment on column workflow_definition.user_id is '创建者用户ID';

alter table workflow_definition
    owner to postgres;

grant select, update, usage on sequence workflow_definition_id_seq to agent_mesh;

create index idx_workflow_user_id
    on workflow_definition (user_id);

create index idx_workflow_agent_id
    on workflow_definition (agent_id);

grant delete, insert, references, select, trigger, truncate, update on workflow_definition to agent_mesh;

create table workflow_execution_history
(
    id                  bigserial
        primary key,
    execution_id        varchar(100) not null
        unique,
    workflow_id         bigint       not null
        references workflow_definition,
    user_id             bigint       not null,
    input_params_json   jsonb,
    output_result_json  jsonb,
    execution_path_json jsonb,
    node_results_json   jsonb,
    success             boolean,
    error_message       text,
    total_duration_ms   bigint,
    started_at          timestamp default CURRENT_TIMESTAMP,
    completed_at        timestamp
);

comment on table workflow_execution_history is '工作流执行历史表';

comment on column workflow_execution_history.execution_id is '执行ID';

comment on column workflow_execution_history.workflow_id is '工作流ID';

comment on column workflow_execution_history.user_id is '用户ID';

comment on column workflow_execution_history.input_params_json is '输入参数(JSON)';

comment on column workflow_execution_history.output_result_json is '输出结果(JSON)';

comment on column workflow_execution_history.execution_path_json is '执行路径(JSON)';

comment on column workflow_execution_history.node_results_json is '节点执行结果(JSON)';

comment on column workflow_execution_history.success is '是否成功';

comment on column workflow_execution_history.error_message is '错误信息';

comment on column workflow_execution_history.total_duration_ms is '总耗时(毫秒)';

alter table workflow_execution_history
    owner to postgres;

grant select, update, usage on sequence workflow_execution_history_id_seq to agent_mesh;

create index idx_execution_workflow_id
    on workflow_execution_history (workflow_id);

create index idx_execution_user_id
    on workflow_execution_history (user_id);

create index idx_execution_started_at
    on workflow_execution_history (started_at);

grant delete, insert, references, select, trigger, truncate, update on workflow_execution_history to agent_mesh;

create function vector_in(cstring, oid, integer) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_in(cstring, oid, integer) owner to postgres;

create function vector_out(vector) returns cstring
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_out(vector) owner to postgres;

create function vector_typmod_in(cstring[]) returns integer
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_typmod_in(cstring[]) owner to postgres;

create function vector_recv(internal, oid, integer) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_recv(internal, oid, integer) owner to postgres;

create function vector_send(vector) returns bytea
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_send(vector) owner to postgres;

create function l2_distance(vector, vector) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function l2_distance(vector, vector) owner to postgres;

create function inner_product(vector, vector) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function inner_product(vector, vector) owner to postgres;

create function cosine_distance(vector, vector) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function cosine_distance(vector, vector) owner to postgres;

create function l1_distance(vector, vector) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function l1_distance(vector, vector) owner to postgres;

create function vector_dims(vector) returns integer
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_dims(vector) owner to postgres;

create function vector_norm(vector) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_norm(vector) owner to postgres;

create function l2_normalize(vector) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function l2_normalize(vector) owner to postgres;

create function binary_quantize(vector) returns bit
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function binary_quantize(vector) owner to postgres;

create function subvector(vector, integer, integer) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function subvector(vector, integer, integer) owner to postgres;

create function vector_add(vector, vector) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_add(vector, vector) owner to postgres;

create function vector_sub(vector, vector) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_sub(vector, vector) owner to postgres;

create function vector_mul(vector, vector) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_mul(vector, vector) owner to postgres;

create function vector_concat(vector, vector) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_concat(vector, vector) owner to postgres;

create function vector_lt(vector, vector) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_lt(vector, vector) owner to postgres;

create function vector_le(vector, vector) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_le(vector, vector) owner to postgres;

create function vector_eq(vector, vector) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_eq(vector, vector) owner to postgres;

create function vector_ne(vector, vector) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_ne(vector, vector) owner to postgres;

create function vector_ge(vector, vector) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_ge(vector, vector) owner to postgres;

create function vector_gt(vector, vector) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_gt(vector, vector) owner to postgres;

create function vector_cmp(vector, vector) returns integer
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_cmp(vector, vector) owner to postgres;

create function vector_l2_squared_distance(vector, vector) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_l2_squared_distance(vector, vector) owner to postgres;

create function vector_negative_inner_product(vector, vector) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_negative_inner_product(vector, vector) owner to postgres;

create function vector_spherical_distance(vector, vector) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_spherical_distance(vector, vector) owner to postgres;

create function vector_accum(double precision[], vector) returns double precision[]
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_accum(double precision[], vector) owner to postgres;

create function vector_avg(double precision[]) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_avg(double precision[]) owner to postgres;

create function vector_combine(double precision[], double precision[]) returns double precision[]
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_combine(double precision[], double precision[]) owner to postgres;

create function vector(vector, integer, boolean) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector(vector, integer, boolean) owner to postgres;

create function array_to_vector(integer[], integer, boolean) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function array_to_vector(integer[], integer, boolean) owner to postgres;

create function array_to_vector(real[], integer, boolean) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function array_to_vector(real[], integer, boolean) owner to postgres;

create function array_to_vector(double precision[], integer, boolean) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function array_to_vector(double precision[], integer, boolean) owner to postgres;

create function array_to_vector(numeric[], integer, boolean) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function array_to_vector(numeric[], integer, boolean) owner to postgres;

create function vector_to_float4(vector, integer, boolean) returns real[]
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_to_float4(vector, integer, boolean) owner to postgres;

create function ivfflathandler(internal) returns index_am_handler
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function ivfflathandler(internal) owner to postgres;

create function hnswhandler(internal) returns index_am_handler
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hnswhandler(internal) owner to postgres;

create function ivfflat_halfvec_support(internal) returns internal
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function ivfflat_halfvec_support(internal) owner to postgres;

create function ivfflat_bit_support(internal) returns internal
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function ivfflat_bit_support(internal) owner to postgres;

create function hnsw_halfvec_support(internal) returns internal
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hnsw_halfvec_support(internal) owner to postgres;

create function hnsw_bit_support(internal) returns internal
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hnsw_bit_support(internal) owner to postgres;

create function hnsw_sparsevec_support(internal) returns internal
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hnsw_sparsevec_support(internal) owner to postgres;

create function halfvec_in(cstring, oid, integer) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_in(cstring, oid, integer) owner to postgres;

create function halfvec_out(halfvec) returns cstring
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_out(halfvec) owner to postgres;

create function halfvec_typmod_in(cstring[]) returns integer
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_typmod_in(cstring[]) owner to postgres;

create function halfvec_recv(internal, oid, integer) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_recv(internal, oid, integer) owner to postgres;

create function halfvec_send(halfvec) returns bytea
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_send(halfvec) owner to postgres;

create function l2_distance(halfvec, halfvec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function l2_distance(halfvec, halfvec) owner to postgres;

create function inner_product(halfvec, halfvec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function inner_product(halfvec, halfvec) owner to postgres;

create function cosine_distance(halfvec, halfvec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function cosine_distance(halfvec, halfvec) owner to postgres;

create function l1_distance(halfvec, halfvec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function l1_distance(halfvec, halfvec) owner to postgres;

create function vector_dims(halfvec) returns integer
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_dims(halfvec) owner to postgres;

create function l2_norm(halfvec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function l2_norm(halfvec) owner to postgres;

create function l2_normalize(halfvec) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function l2_normalize(halfvec) owner to postgres;

create function binary_quantize(halfvec) returns bit
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function binary_quantize(halfvec) owner to postgres;

create function subvector(halfvec, integer, integer) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function subvector(halfvec, integer, integer) owner to postgres;

create function halfvec_add(halfvec, halfvec) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_add(halfvec, halfvec) owner to postgres;

create function halfvec_sub(halfvec, halfvec) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_sub(halfvec, halfvec) owner to postgres;

create function halfvec_mul(halfvec, halfvec) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_mul(halfvec, halfvec) owner to postgres;

create function halfvec_concat(halfvec, halfvec) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_concat(halfvec, halfvec) owner to postgres;

create function halfvec_lt(halfvec, halfvec) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_lt(halfvec, halfvec) owner to postgres;

create function halfvec_le(halfvec, halfvec) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_le(halfvec, halfvec) owner to postgres;

create function halfvec_eq(halfvec, halfvec) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_eq(halfvec, halfvec) owner to postgres;

create function halfvec_ne(halfvec, halfvec) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_ne(halfvec, halfvec) owner to postgres;

create function halfvec_ge(halfvec, halfvec) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_ge(halfvec, halfvec) owner to postgres;

create function halfvec_gt(halfvec, halfvec) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_gt(halfvec, halfvec) owner to postgres;

create function halfvec_cmp(halfvec, halfvec) returns integer
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_cmp(halfvec, halfvec) owner to postgres;

create function halfvec_l2_squared_distance(halfvec, halfvec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_l2_squared_distance(halfvec, halfvec) owner to postgres;

create function halfvec_negative_inner_product(halfvec, halfvec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_negative_inner_product(halfvec, halfvec) owner to postgres;

create function halfvec_spherical_distance(halfvec, halfvec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_spherical_distance(halfvec, halfvec) owner to postgres;

create function halfvec_accum(double precision[], halfvec) returns double precision[]
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_accum(double precision[], halfvec) owner to postgres;

create function halfvec_avg(double precision[]) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_avg(double precision[]) owner to postgres;

create function halfvec_combine(double precision[], double precision[]) returns double precision[]
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_combine(double precision[], double precision[]) owner to postgres;

create function halfvec(halfvec, integer, boolean) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec(halfvec, integer, boolean) owner to postgres;

create function halfvec_to_vector(halfvec, integer, boolean) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_to_vector(halfvec, integer, boolean) owner to postgres;

create function vector_to_halfvec(vector, integer, boolean) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_to_halfvec(vector, integer, boolean) owner to postgres;

create function array_to_halfvec(integer[], integer, boolean) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function array_to_halfvec(integer[], integer, boolean) owner to postgres;

create function array_to_halfvec(real[], integer, boolean) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function array_to_halfvec(real[], integer, boolean) owner to postgres;

create function array_to_halfvec(double precision[], integer, boolean) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function array_to_halfvec(double precision[], integer, boolean) owner to postgres;

create function array_to_halfvec(numeric[], integer, boolean) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function array_to_halfvec(numeric[], integer, boolean) owner to postgres;

create function halfvec_to_float4(halfvec, integer, boolean) returns real[]
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_to_float4(halfvec, integer, boolean) owner to postgres;

create function hamming_distance(bit, bit) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hamming_distance(bit, bit) owner to postgres;

create function jaccard_distance(bit, bit) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function jaccard_distance(bit, bit) owner to postgres;

create function sparsevec_in(cstring, oid, integer) returns sparsevec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_in(cstring, oid, integer) owner to postgres;

create function sparsevec_out(sparsevec) returns cstring
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_out(sparsevec) owner to postgres;

create function sparsevec_typmod_in(cstring[]) returns integer
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_typmod_in(cstring[]) owner to postgres;

create function sparsevec_recv(internal, oid, integer) returns sparsevec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_recv(internal, oid, integer) owner to postgres;

create function sparsevec_send(sparsevec) returns bytea
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_send(sparsevec) owner to postgres;

create function l2_distance(sparsevec, sparsevec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function l2_distance(sparsevec, sparsevec) owner to postgres;

create function inner_product(sparsevec, sparsevec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function inner_product(sparsevec, sparsevec) owner to postgres;

create function cosine_distance(sparsevec, sparsevec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function cosine_distance(sparsevec, sparsevec) owner to postgres;

create function l1_distance(sparsevec, sparsevec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function l1_distance(sparsevec, sparsevec) owner to postgres;

create function l2_norm(sparsevec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function l2_norm(sparsevec) owner to postgres;

create function l2_normalize(sparsevec) returns sparsevec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function l2_normalize(sparsevec) owner to postgres;

create function sparsevec_lt(sparsevec, sparsevec) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_lt(sparsevec, sparsevec) owner to postgres;

create function sparsevec_le(sparsevec, sparsevec) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_le(sparsevec, sparsevec) owner to postgres;

create function sparsevec_eq(sparsevec, sparsevec) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_eq(sparsevec, sparsevec) owner to postgres;

create function sparsevec_ne(sparsevec, sparsevec) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_ne(sparsevec, sparsevec) owner to postgres;

create function sparsevec_ge(sparsevec, sparsevec) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_ge(sparsevec, sparsevec) owner to postgres;

create function sparsevec_gt(sparsevec, sparsevec) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_gt(sparsevec, sparsevec) owner to postgres;

create function sparsevec_cmp(sparsevec, sparsevec) returns integer
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_cmp(sparsevec, sparsevec) owner to postgres;

create function sparsevec_l2_squared_distance(sparsevec, sparsevec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_l2_squared_distance(sparsevec, sparsevec) owner to postgres;

create function sparsevec_negative_inner_product(sparsevec, sparsevec) returns double precision
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_negative_inner_product(sparsevec, sparsevec) owner to postgres;

create function sparsevec(sparsevec, integer, boolean) returns sparsevec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec(sparsevec, integer, boolean) owner to postgres;

create function vector_to_sparsevec(vector, integer, boolean) returns sparsevec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function vector_to_sparsevec(vector, integer, boolean) owner to postgres;

create function sparsevec_to_vector(sparsevec, integer, boolean) returns vector
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_to_vector(sparsevec, integer, boolean) owner to postgres;

create function halfvec_to_sparsevec(halfvec, integer, boolean) returns sparsevec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function halfvec_to_sparsevec(halfvec, integer, boolean) owner to postgres;

create function sparsevec_to_halfvec(sparsevec, integer, boolean) returns halfvec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function sparsevec_to_halfvec(sparsevec, integer, boolean) owner to postgres;

create function array_to_sparsevec(integer[], integer, boolean) returns sparsevec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function array_to_sparsevec(integer[], integer, boolean) owner to postgres;

create function array_to_sparsevec(real[], integer, boolean) returns sparsevec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function array_to_sparsevec(real[], integer, boolean) owner to postgres;

create function array_to_sparsevec(double precision[], integer, boolean) returns sparsevec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function array_to_sparsevec(double precision[], integer, boolean) owner to postgres;

create function array_to_sparsevec(numeric[], integer, boolean) returns sparsevec
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function array_to_sparsevec(numeric[], integer, boolean) owner to postgres;

create function hstore_in(cstring) returns hstore
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_in(cstring) owner to postgres;

create function hstore_out(hstore) returns cstring
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_out(hstore) owner to postgres;

create function hstore_recv(internal) returns hstore
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_recv(internal) owner to postgres;

create function hstore_send(hstore) returns bytea
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_send(hstore) owner to postgres;

create function hstore_version_diag(hstore) returns integer
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_version_diag(hstore) owner to postgres;

create function fetchval(hstore, text) returns text
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function fetchval(hstore, text) owner to postgres;

create function slice_array(hstore, text[]) returns text[]
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function slice_array(hstore, text[]) owner to postgres;

create function slice(hstore, text[]) returns hstore
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function slice(hstore, text[]) owner to postgres;

create function isexists(hstore, text) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function isexists(hstore, text) owner to postgres;

create function exist(hstore, text) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function exist(hstore, text) owner to postgres;

create function exists_any(hstore, text[]) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function exists_any(hstore, text[]) owner to postgres;

create function exists_all(hstore, text[]) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function exists_all(hstore, text[]) owner to postgres;

create function isdefined(hstore, text) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function isdefined(hstore, text) owner to postgres;

create function defined(hstore, text) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function defined(hstore, text) owner to postgres;

create function delete(hstore, text) returns hstore
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function delete(hstore, text) owner to postgres;

create function delete(hstore, text[]) returns hstore
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function delete(hstore, text[]) owner to postgres;

create function delete(hstore, hstore) returns hstore
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function delete(hstore, hstore) owner to postgres;

create function hs_concat(hstore, hstore) returns hstore
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hs_concat(hstore, hstore) owner to postgres;

create function hs_contains(hstore, hstore) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hs_contains(hstore, hstore) owner to postgres;

create function hs_contained(hstore, hstore) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hs_contained(hstore, hstore) owner to postgres;

create function tconvert(text, text) returns hstore
    immutable
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function tconvert(text, text) owner to postgres;

create function hstore(text, text) returns hstore
    immutable
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore(text, text) owner to postgres;

create function hstore(text[], text[]) returns hstore
    immutable
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore(text[], text[]) owner to postgres;

create function hstore(text[]) returns hstore
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore(text[]) owner to postgres;

create function hstore_to_json(hstore) returns json
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_to_json(hstore) owner to postgres;

create function hstore_to_json_loose(hstore) returns json
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_to_json_loose(hstore) owner to postgres;

create function hstore_to_jsonb(hstore) returns jsonb
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_to_jsonb(hstore) owner to postgres;

create function hstore_to_jsonb_loose(hstore) returns jsonb
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_to_jsonb_loose(hstore) owner to postgres;

create function hstore(record) returns hstore
    immutable
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore(record) owner to postgres;

create function hstore_to_array(hstore) returns text[]
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_to_array(hstore) owner to postgres;

create function hstore_to_matrix(hstore) returns text[]
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_to_matrix(hstore) owner to postgres;

create function akeys(hstore) returns text[]
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function akeys(hstore) owner to postgres;

create function avals(hstore) returns text[]
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function avals(hstore) owner to postgres;

create function skeys(hstore) returns setof text
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function skeys(hstore) owner to postgres;

create function svals(hstore) returns setof text
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function svals(hstore) owner to postgres;

create function each(hs hstore, out key text, out value text) returns setof record
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function each(hstore, out text, out text) owner to postgres;

create function populate_record(anyelement, hstore) returns anyelement
    immutable
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function populate_record(anyelement, hstore) owner to postgres;

create function hstore_eq(hstore, hstore) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_eq(hstore, hstore) owner to postgres;

create function hstore_ne(hstore, hstore) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_ne(hstore, hstore) owner to postgres;

create function hstore_gt(hstore, hstore) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_gt(hstore, hstore) owner to postgres;

create function hstore_ge(hstore, hstore) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_ge(hstore, hstore) owner to postgres;

create function hstore_lt(hstore, hstore) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_lt(hstore, hstore) owner to postgres;

create function hstore_le(hstore, hstore) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_le(hstore, hstore) owner to postgres;

create function hstore_cmp(hstore, hstore) returns integer
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_cmp(hstore, hstore) owner to postgres;

create function hstore_hash(hstore) returns integer
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_hash(hstore) owner to postgres;

create function ghstore_in(cstring) returns ghstore
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function ghstore_in(cstring) owner to postgres;

create function ghstore_out(ghstore) returns cstring
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function ghstore_out(ghstore) owner to postgres;

create function ghstore_compress(internal) returns internal
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function ghstore_compress(internal) owner to postgres;

create function ghstore_decompress(internal) returns internal
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function ghstore_decompress(internal) owner to postgres;

create function ghstore_penalty(internal, internal, internal) returns internal
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function ghstore_penalty(internal, internal, internal) owner to postgres;

create function ghstore_picksplit(internal, internal) returns internal
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function ghstore_picksplit(internal, internal) owner to postgres;

create function ghstore_union(internal, internal) returns ghstore
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function ghstore_union(internal, internal) owner to postgres;

create function ghstore_same(ghstore, ghstore, internal) returns internal
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function ghstore_same(ghstore, ghstore, internal) owner to postgres;

create function ghstore_consistent(internal, hstore, smallint, oid, internal) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function ghstore_consistent(internal, hstore, smallint, oid, internal) owner to postgres;

create function gin_extract_hstore(hstore, internal) returns internal
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function gin_extract_hstore(hstore, internal) owner to postgres;

create function gin_extract_hstore_query(hstore, internal, smallint, internal, internal) returns internal
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function gin_extract_hstore_query(hstore, internal, smallint, internal, internal) owner to postgres;

create function gin_consistent_hstore(internal, smallint, hstore, integer, internal, internal) returns boolean
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function gin_consistent_hstore(internal, smallint, hstore, integer, internal, internal) owner to postgres;

create function hstore_hash_extended(hstore, bigint) returns bigint
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_hash_extended(hstore, bigint) owner to postgres;

create function ghstore_options(internal) returns void
    immutable
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function ghstore_options(internal) owner to postgres;

create function hstore_subscript_handler(internal) returns internal
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function hstore_subscript_handler(internal) owner to postgres;

create function uuid_nil() returns uuid
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_nil() owner to postgres;

create function uuid_ns_dns() returns uuid
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_ns_dns() owner to postgres;

create function uuid_ns_url() returns uuid
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_ns_url() owner to postgres;

create function uuid_ns_oid() returns uuid
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_ns_oid() owner to postgres;

create function uuid_ns_x500() returns uuid
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_ns_x500() owner to postgres;

create function uuid_generate_v1() returns uuid
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_generate_v1() owner to postgres;

create function uuid_generate_v1mc() returns uuid
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_generate_v1mc() owner to postgres;

create function uuid_generate_v3(namespace uuid, name text) returns uuid
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_generate_v3(uuid, text) owner to postgres;

create function uuid_generate_v4() returns uuid
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_generate_v4() owner to postgres;

create function uuid_generate_v5(namespace uuid, name text) returns uuid
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_generate_v5(uuid, text) owner to postgres;

create function check_agent_model_ownership() returns trigger
    language plpgsql
as
$$
BEGIN
    -- 验证 decision_model_id
    IF NEW.decision_model_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM ai_model
            WHERE id = NEW.decision_model_id AND user_id = NEW.user_id AND is_delete = 0
        ) THEN
            RAISE EXCEPTION 'Security Error: Decision model ID % does not belong to user % or is deleted.', NEW.decision_model_id, NEW.user_id;
        END IF;
    END IF;

    -- 验证 response_model_id
    IF NEW.response_model_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM ai_model
            WHERE id = NEW.response_model_id AND user_id = NEW.user_id AND is_delete = 0
        ) THEN
            RAISE EXCEPTION 'Security Error: Response model ID % does not belong to user % or is deleted.', NEW.response_model_id, NEW.user_id;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

alter function check_agent_model_ownership() owner to postgres;

create function update_updated_at_column() returns trigger
    language plpgsql
as
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

alter function update_updated_at_column() owner to postgres;

create operator <-> (procedure = l2_distance, leftarg = vector, rightarg = vector, commutator = <->);

alter operator <->(vector, vector) owner to postgres;

create operator <#> (procedure = vector_negative_inner_product, leftarg = vector, rightarg = vector, commutator = <#>);

alter operator <#>(vector, vector) owner to postgres;

create operator <=> (procedure = cosine_distance, leftarg = vector, rightarg = vector, commutator = <=>);

alter operator <=>(vector, vector) owner to postgres;

create operator <+> (procedure = l1_distance, leftarg = vector, rightarg = vector, commutator = <+>);

alter operator <+>(vector, vector) owner to postgres;

create operator + (procedure = vector_add, leftarg = vector, rightarg = vector, commutator = +);

alter operator +(vector, vector) owner to postgres;

create operator - (procedure = vector_sub, leftarg = vector, rightarg = vector);

alter operator -(vector, vector) owner to postgres;

create operator * (procedure = vector_mul, leftarg = vector, rightarg = vector, commutator = *);

alter operator *(vector, vector) owner to postgres;

create operator || (procedure = vector_concat, leftarg = vector, rightarg = vector);

alter operator ||(vector, vector) owner to postgres;

create operator <-> (procedure = l2_distance, leftarg = halfvec, rightarg = halfvec, commutator = <->);

alter operator <->(halfvec, halfvec) owner to postgres;

create operator <#> (procedure = halfvec_negative_inner_product, leftarg = halfvec, rightarg = halfvec, commutator = <#>);

alter operator <#>(halfvec, halfvec) owner to postgres;

create operator <=> (procedure = cosine_distance, leftarg = halfvec, rightarg = halfvec, commutator = <=>);

alter operator <=>(halfvec, halfvec) owner to postgres;

create operator <+> (procedure = l1_distance, leftarg = halfvec, rightarg = halfvec, commutator = <+>);

alter operator <+>(halfvec, halfvec) owner to postgres;

create operator + (procedure = halfvec_add, leftarg = halfvec, rightarg = halfvec, commutator = +);

alter operator +(halfvec, halfvec) owner to postgres;

create operator - (procedure = halfvec_sub, leftarg = halfvec, rightarg = halfvec);

alter operator -(halfvec, halfvec) owner to postgres;

create operator * (procedure = halfvec_mul, leftarg = halfvec, rightarg = halfvec, commutator = *);

alter operator *(halfvec, halfvec) owner to postgres;

create operator || (procedure = halfvec_concat, leftarg = halfvec, rightarg = halfvec);

alter operator ||(halfvec, halfvec) owner to postgres;

create operator <~> (procedure = hamming_distance, leftarg = bit, rightarg = bit, commutator = <~>);

alter operator <~>(bit, bit) owner to postgres;

create operator <%> (procedure = jaccard_distance, leftarg = bit, rightarg = bit, commutator = <%>);

alter operator <%>(bit, bit) owner to postgres;

create operator <-> (procedure = l2_distance, leftarg = sparsevec, rightarg = sparsevec, commutator = <->);

alter operator <->(sparsevec, sparsevec) owner to postgres;

create operator <#> (procedure = sparsevec_negative_inner_product, leftarg = sparsevec, rightarg = sparsevec, commutator = <#>);

alter operator <#>(sparsevec, sparsevec) owner to postgres;

create operator <=> (procedure = cosine_distance, leftarg = sparsevec, rightarg = sparsevec, commutator = <=>);

alter operator <=>(sparsevec, sparsevec) owner to postgres;

create operator <+> (procedure = l1_distance, leftarg = sparsevec, rightarg = sparsevec, commutator = <+>);

alter operator <+>(sparsevec, sparsevec) owner to postgres;

create operator -> (procedure = fetchval, leftarg = hstore, rightarg = text);

alter operator ->(hstore, text) owner to postgres;

create operator -> (procedure = slice_array, leftarg = hstore, rightarg = text[]);

alter operator ->(hstore, text[]) owner to postgres;

create operator ? (procedure = exist, leftarg = hstore, rightarg = text, join = pg_catalog.matchingjoinsel, restrict = pg_catalog.matchingsel);

alter operator ?(hstore, text) owner to postgres;

create operator ?| (procedure = exists_any, leftarg = hstore, rightarg = text[], join = pg_catalog.matchingjoinsel, restrict = pg_catalog.matchingsel);

alter operator ?|(hstore, text[]) owner to postgres;

create operator ?& (procedure = exists_all, leftarg = hstore, rightarg = text[], join = pg_catalog.matchingjoinsel, restrict = pg_catalog.matchingsel);

alter operator ?&(hstore, text[]) owner to postgres;

create operator - (procedure = delete, leftarg = hstore, rightarg = text);

alter operator -(hstore, text) owner to postgres;

create operator - (procedure = delete, leftarg = hstore, rightarg = text[]);

alter operator -(hstore, text[]) owner to postgres;

create operator - (procedure = delete, leftarg = hstore, rightarg = hstore);

alter operator -(hstore, hstore) owner to postgres;

create operator || (procedure = hs_concat, leftarg = hstore, rightarg = hstore);

alter operator ||(hstore, hstore) owner to postgres;

create operator %% (procedure = hstore_to_array, rightarg = hstore);

alter operator %%(none, hstore) owner to postgres;

create operator %# (procedure = hstore_to_matrix, rightarg = hstore);

alter operator %#(none, hstore) owner to postgres;

create operator #= (procedure = populate_record, leftarg = anyelement, rightarg = hstore);

alter operator #=(anyelement, hstore) owner to postgres;

create aggregate avg(vector) (
    sfunc = vector_accum,
    stype = double precision[],
    finalfunc = vector_avg,
    combinefunc = vector_combine,
    initcond = '{0}',
    parallel = safe
    );

alter aggregate avg(vector) owner to postgres;

create aggregate sum(vector) (
    sfunc = vector_add,
    stype = vector,
    combinefunc = vector_add,
    parallel = safe
    );

alter aggregate sum(vector) owner to postgres;

create aggregate avg(halfvec) (
    sfunc = halfvec_accum,
    stype = double precision[],
    finalfunc = halfvec_avg,
    combinefunc = halfvec_combine,
    initcond = '{0}',
    parallel = safe
    );

alter aggregate avg(halfvec) owner to postgres;

create aggregate sum(halfvec) (
    sfunc = halfvec_add,
    stype = halfvec,
    combinefunc = halfvec_add,
    parallel = safe
    );

alter aggregate sum(halfvec) owner to postgres;

create operator family vector_ops using btree;

alter operator family vector_ops using btree add
    operator 5 >(vector, vector),
    operator 4 >=(vector, vector),
    operator 1 <(vector, vector),
    operator 2 <=(vector, vector),
    operator 3 =(vector, vector),
    function 1(vector, vector) vector_cmp(vector, vector);

alter operator family vector_ops using btree owner to postgres;

create operator class vector_ops default for type vector using btree as
    operator 5 >(vector, vector),
    operator 1 <(vector, vector),
    operator 2 <=(vector, vector),
    operator 3 =(vector, vector),
    operator 4 >=(vector, vector),
    function 1(vector, vector) vector_cmp(vector, vector);

alter operator class vector_ops using btree owner to postgres;

create operator family vector_l2_ops using ivfflat;

alter operator family vector_l2_ops using ivfflat add
    operator 1 <->(vector, vector) for order by float_ops,
    function 1(vector, vector) vector_l2_squared_distance(vector, vector),
    function 3(vector, vector) l2_distance(vector, vector);

alter operator family vector_l2_ops using ivfflat owner to postgres;

create operator class vector_l2_ops default for type vector using ivfflat as
    operator 1 <->(vector, vector) for order by float_ops,
    function 1(vector, vector) vector_l2_squared_distance(vector, vector),
    function 3(vector, vector) l2_distance(vector, vector);

alter operator class vector_l2_ops using ivfflat owner to postgres;

create operator family vector_ip_ops using ivfflat;

alter operator family vector_ip_ops using ivfflat add
    operator 1 <#>(vector, vector) for order by float_ops,
    function 1(vector, vector) vector_negative_inner_product(vector, vector),
    function 4(vector, vector) vector_norm(vector),
    function 3(vector, vector) vector_spherical_distance(vector, vector);

alter operator family vector_ip_ops using ivfflat owner to postgres;

create operator class vector_ip_ops for type vector using ivfflat as
    operator 1 <#>(vector, vector) for order by float_ops,
    function 1(vector, vector) vector_negative_inner_product(vector, vector),
    function 4(vector, vector) vector_norm(vector),
    function 3(vector, vector) vector_spherical_distance(vector, vector);

alter operator class vector_ip_ops using ivfflat owner to postgres;

create operator family vector_cosine_ops using ivfflat;

alter operator family vector_cosine_ops using ivfflat add
    operator 1 <=>(vector, vector) for order by float_ops,
    function 3(vector, vector) vector_spherical_distance(vector, vector),
    function 1(vector, vector) vector_negative_inner_product(vector, vector),
    function 4(vector, vector) vector_norm(vector),
    function 2(vector, vector) vector_norm(vector);

alter operator family vector_cosine_ops using ivfflat owner to postgres;

create operator class vector_cosine_ops for type vector using ivfflat as
    operator 1 <=>(vector, vector) for order by float_ops,
    function 4(vector, vector) vector_norm(vector),
    function 1(vector, vector) vector_negative_inner_product(vector, vector),
    function 2(vector, vector) vector_norm(vector),
    function 3(vector, vector) vector_spherical_distance(vector, vector);

alter operator class vector_cosine_ops using ivfflat owner to postgres;

create operator family vector_l2_ops using hnsw;

alter operator family vector_l2_ops using hnsw add
    operator 1 <->(vector, vector) for order by float_ops,
    function 1(vector, vector) vector_l2_squared_distance(vector, vector);

alter operator family vector_l2_ops using hnsw owner to postgres;

create operator class vector_l2_ops for type vector using hnsw as
    operator 1 <->(vector, vector) for order by float_ops,
    function 1(vector, vector) vector_l2_squared_distance(vector, vector);

alter operator class vector_l2_ops using hnsw owner to postgres;

create operator family vector_ip_ops using hnsw;

alter operator family vector_ip_ops using hnsw add
    operator 1 <#>(vector, vector) for order by float_ops,
    function 1(vector, vector) vector_negative_inner_product(vector, vector);

alter operator family vector_ip_ops using hnsw owner to postgres;

create operator class vector_ip_ops for type vector using hnsw as
    operator 1 <#>(vector, vector) for order by float_ops,
    function 1(vector, vector) vector_negative_inner_product(vector, vector);

alter operator class vector_ip_ops using hnsw owner to postgres;

create operator family vector_cosine_ops using hnsw;

alter operator family vector_cosine_ops using hnsw add
    operator 1 <=>(vector, vector) for order by float_ops,
    function 1(vector, vector) vector_negative_inner_product(vector, vector),
    function 2(vector, vector) vector_norm(vector);

alter operator family vector_cosine_ops using hnsw owner to postgres;

create operator class vector_cosine_ops for type vector using hnsw as
    operator 1 <=>(vector, vector) for order by float_ops,
    function 1(vector, vector) vector_negative_inner_product(vector, vector),
    function 2(vector, vector) vector_norm(vector);

alter operator class vector_cosine_ops using hnsw owner to postgres;

create operator family vector_l1_ops using hnsw;

alter operator family vector_l1_ops using hnsw add
    operator 1 <+>(vector, vector) for order by float_ops,
    function 1(vector, vector) l1_distance(vector, vector);

alter operator family vector_l1_ops using hnsw owner to postgres;

create operator class vector_l1_ops for type vector using hnsw as
    operator 1 <+>(vector, vector) for order by float_ops,
    function 1(vector, vector) l1_distance(vector, vector);

alter operator class vector_l1_ops using hnsw owner to postgres;

create operator family halfvec_ops using btree;

alter operator family halfvec_ops using btree add
    operator 5 >(halfvec, halfvec),
    operator 4 >=(halfvec, halfvec),
    operator 3 =(halfvec, halfvec),
    operator 2 <=(halfvec, halfvec),
    operator 1 <(halfvec, halfvec),
    function 1(halfvec, halfvec) halfvec_cmp(halfvec, halfvec);

alter operator family halfvec_ops using btree owner to postgres;

create operator class halfvec_ops default for type halfvec using btree as
    operator 1 <(halfvec, halfvec),
    operator 3 =(halfvec, halfvec),
    operator 5 >(halfvec, halfvec),
    operator 2 <=(halfvec, halfvec),
    operator 4 >=(halfvec, halfvec),
    function 1(halfvec, halfvec) halfvec_cmp(halfvec, halfvec);

alter operator class halfvec_ops using btree owner to postgres;

create operator family halfvec_l2_ops using ivfflat;

alter operator family halfvec_l2_ops using ivfflat add
    operator 1 <->(halfvec, halfvec) for order by float_ops,
    function 3(halfvec, halfvec) l2_distance(halfvec, halfvec),
    function 5(halfvec, halfvec) ivfflat_halfvec_support(internal),
    function 1(halfvec, halfvec) halfvec_l2_squared_distance(halfvec, halfvec);

alter operator family halfvec_l2_ops using ivfflat owner to postgres;

create operator class halfvec_l2_ops for type halfvec using ivfflat as
    operator 1 <->(halfvec, halfvec) for order by float_ops,
    function 1(halfvec, halfvec) halfvec_l2_squared_distance(halfvec, halfvec),
    function 5(halfvec, halfvec) ivfflat_halfvec_support(internal),
    function 3(halfvec, halfvec) l2_distance(halfvec, halfvec);

alter operator class halfvec_l2_ops using ivfflat owner to postgres;

create operator family halfvec_ip_ops using ivfflat;

alter operator family halfvec_ip_ops using ivfflat add
    operator 1 <#>(halfvec, halfvec) for order by float_ops,
    function 5(halfvec, halfvec) ivfflat_halfvec_support(internal),
    function 1(halfvec, halfvec) halfvec_negative_inner_product(halfvec, halfvec),
    function 3(halfvec, halfvec) halfvec_spherical_distance(halfvec, halfvec),
    function 4(halfvec, halfvec) l2_norm(halfvec);

alter operator family halfvec_ip_ops using ivfflat owner to postgres;

create operator class halfvec_ip_ops for type halfvec using ivfflat as
    operator 1 <#>(halfvec, halfvec) for order by float_ops,
    function 1(halfvec, halfvec) halfvec_negative_inner_product(halfvec, halfvec),
    function 5(halfvec, halfvec) ivfflat_halfvec_support(internal),
    function 4(halfvec, halfvec) l2_norm(halfvec),
    function 3(halfvec, halfvec) halfvec_spherical_distance(halfvec, halfvec);

alter operator class halfvec_ip_ops using ivfflat owner to postgres;

create operator family halfvec_cosine_ops using ivfflat;

alter operator family halfvec_cosine_ops using ivfflat add
    operator 1 <=>(halfvec, halfvec) for order by float_ops,
    function 5(halfvec, halfvec) ivfflat_halfvec_support(internal),
    function 2(halfvec, halfvec) l2_norm(halfvec),
    function 1(halfvec, halfvec) halfvec_negative_inner_product(halfvec, halfvec),
    function 3(halfvec, halfvec) halfvec_spherical_distance(halfvec, halfvec),
    function 4(halfvec, halfvec) l2_norm(halfvec);

alter operator family halfvec_cosine_ops using ivfflat owner to postgres;

create operator class halfvec_cosine_ops for type halfvec using ivfflat as
    operator 1 <=>(halfvec, halfvec) for order by float_ops,
    function 3(halfvec, halfvec) halfvec_spherical_distance(halfvec, halfvec),
    function 1(halfvec, halfvec) halfvec_negative_inner_product(halfvec, halfvec),
    function 2(halfvec, halfvec) l2_norm(halfvec),
    function 5(halfvec, halfvec) ivfflat_halfvec_support(internal),
    function 4(halfvec, halfvec) l2_norm(halfvec);

alter operator class halfvec_cosine_ops using ivfflat owner to postgres;

create operator family halfvec_l2_ops using hnsw;

alter operator family halfvec_l2_ops using hnsw add
    operator 1 <->(halfvec, halfvec) for order by float_ops,
    function 3(halfvec, halfvec) hnsw_halfvec_support(internal),
    function 1(halfvec, halfvec) halfvec_l2_squared_distance(halfvec, halfvec);

alter operator family halfvec_l2_ops using hnsw owner to postgres;

create operator class halfvec_l2_ops for type halfvec using hnsw as
    operator 1 <->(halfvec, halfvec) for order by float_ops,
    function 3(halfvec, halfvec) hnsw_halfvec_support(internal),
    function 1(halfvec, halfvec) halfvec_l2_squared_distance(halfvec, halfvec);

alter operator class halfvec_l2_ops using hnsw owner to postgres;

create operator family halfvec_ip_ops using hnsw;

alter operator family halfvec_ip_ops using hnsw add
    operator 1 <#>(halfvec, halfvec) for order by float_ops,
    function 3(halfvec, halfvec) hnsw_halfvec_support(internal),
    function 1(halfvec, halfvec) halfvec_negative_inner_product(halfvec, halfvec);

alter operator family halfvec_ip_ops using hnsw owner to postgres;

create operator class halfvec_ip_ops for type halfvec using hnsw as
    operator 1 <#>(halfvec, halfvec) for order by float_ops,
    function 1(halfvec, halfvec) halfvec_negative_inner_product(halfvec, halfvec),
    function 3(halfvec, halfvec) hnsw_halfvec_support(internal);

alter operator class halfvec_ip_ops using hnsw owner to postgres;

create operator family halfvec_cosine_ops using hnsw;

alter operator family halfvec_cosine_ops using hnsw add
    operator 1 <=>(halfvec, halfvec) for order by float_ops,
    function 3(halfvec, halfvec) hnsw_halfvec_support(internal),
    function 1(halfvec, halfvec) halfvec_negative_inner_product(halfvec, halfvec),
    function 2(halfvec, halfvec) l2_norm(halfvec);

alter operator family halfvec_cosine_ops using hnsw owner to postgres;

create operator class halfvec_cosine_ops for type halfvec using hnsw as
    operator 1 <=>(halfvec, halfvec) for order by float_ops,
    function 3(halfvec, halfvec) hnsw_halfvec_support(internal),
    function 1(halfvec, halfvec) halfvec_negative_inner_product(halfvec, halfvec),
    function 2(halfvec, halfvec) l2_norm(halfvec);

alter operator class halfvec_cosine_ops using hnsw owner to postgres;

create operator family halfvec_l1_ops using hnsw;

alter operator family halfvec_l1_ops using hnsw add
    operator 1 <+>(halfvec, halfvec) for order by float_ops,
    function 3(halfvec, halfvec) hnsw_halfvec_support(internal),
    function 1(halfvec, halfvec) l1_distance(halfvec, halfvec);

alter operator family halfvec_l1_ops using hnsw owner to postgres;

create operator class halfvec_l1_ops for type halfvec using hnsw as
    operator 1 <+>(halfvec, halfvec) for order by float_ops,
    function 3(halfvec, halfvec) hnsw_halfvec_support(internal),
    function 1(halfvec, halfvec) l1_distance(halfvec, halfvec);

alter operator class halfvec_l1_ops using hnsw owner to postgres;

create operator family bit_hamming_ops using ivfflat;

alter operator family bit_hamming_ops using ivfflat add
    operator 1 <~>(bit, bit) for order by float_ops,
    function 5(bit, bit) ivfflat_bit_support(internal),
    function 3(bit, bit) hamming_distance(bit, bit),
    function 1(bit, bit) hamming_distance(bit, bit);

alter operator family bit_hamming_ops using ivfflat owner to postgres;

create operator class bit_hamming_ops for type bit using ivfflat as
    operator 1 <~>(bit, bit) for order by float_ops,
    function 1(bit, bit) hamming_distance(bit, bit),
    function 5(bit, bit) ivfflat_bit_support(internal),
    function 3(bit, bit) hamming_distance(bit, bit);

alter operator class bit_hamming_ops using ivfflat owner to postgres;

create operator family bit_hamming_ops using hnsw;

alter operator family bit_hamming_ops using hnsw add
    operator 1 <~>(bit, bit) for order by float_ops,
    function 1(bit, bit) hamming_distance(bit, bit),
    function 3(bit, bit) hnsw_bit_support(internal);

alter operator family bit_hamming_ops using hnsw owner to postgres;

create operator class bit_hamming_ops for type bit using hnsw as
    operator 1 <~>(bit, bit) for order by float_ops,
    function 1(bit, bit) hamming_distance(bit, bit),
    function 3(bit, bit) hnsw_bit_support(internal);

alter operator class bit_hamming_ops using hnsw owner to postgres;

create operator family bit_jaccard_ops using hnsw;

alter operator family bit_jaccard_ops using hnsw add
    operator 1 <%>(bit, bit) for order by float_ops,
    function 3(bit, bit) hnsw_bit_support(internal),
    function 1(bit, bit) jaccard_distance(bit, bit);

alter operator family bit_jaccard_ops using hnsw owner to postgres;

create operator class bit_jaccard_ops for type bit using hnsw as
    operator 1 <%>(bit, bit) for order by float_ops,
    function 1(bit, bit) jaccard_distance(bit, bit),
    function 3(bit, bit) hnsw_bit_support(internal);

alter operator class bit_jaccard_ops using hnsw owner to postgres;

create operator family sparsevec_ops using btree;

alter operator family sparsevec_ops using btree add
    operator 4 >=(sparsevec, sparsevec),
    operator 2 <=(sparsevec, sparsevec),
    operator 3 =(sparsevec, sparsevec),
    operator 1 <(sparsevec, sparsevec),
    operator 5 >(sparsevec, sparsevec),
    function 1(sparsevec, sparsevec) sparsevec_cmp(sparsevec, sparsevec);

alter operator family sparsevec_ops using btree owner to postgres;

create operator class sparsevec_ops default for type sparsevec using btree as
    operator 1 <(sparsevec, sparsevec),
    operator 2 <=(sparsevec, sparsevec),
    operator 3 =(sparsevec, sparsevec),
    operator 4 >=(sparsevec, sparsevec),
    operator 5 >(sparsevec, sparsevec),
    function 1(sparsevec, sparsevec) sparsevec_cmp(sparsevec, sparsevec);

alter operator class sparsevec_ops using btree owner to postgres;

create operator family sparsevec_l2_ops using hnsw;

alter operator family sparsevec_l2_ops using hnsw add
    operator 1 <->(sparsevec, sparsevec) for order by float_ops,
    function 3(sparsevec, sparsevec) hnsw_sparsevec_support(internal),
    function 1(sparsevec, sparsevec) sparsevec_l2_squared_distance(sparsevec, sparsevec);

alter operator family sparsevec_l2_ops using hnsw owner to postgres;

create operator class sparsevec_l2_ops for type sparsevec using hnsw as
    operator 1 <->(sparsevec, sparsevec) for order by float_ops,
    function 1(sparsevec, sparsevec) sparsevec_l2_squared_distance(sparsevec, sparsevec),
    function 3(sparsevec, sparsevec) hnsw_sparsevec_support(internal);

alter operator class sparsevec_l2_ops using hnsw owner to postgres;

create operator family sparsevec_ip_ops using hnsw;

alter operator family sparsevec_ip_ops using hnsw add
    operator 1 <#>(sparsevec, sparsevec) for order by float_ops,
    function 1(sparsevec, sparsevec) sparsevec_negative_inner_product(sparsevec, sparsevec),
    function 3(sparsevec, sparsevec) hnsw_sparsevec_support(internal);

alter operator family sparsevec_ip_ops using hnsw owner to postgres;

create operator class sparsevec_ip_ops for type sparsevec using hnsw as
    operator 1 <#>(sparsevec, sparsevec) for order by float_ops,
    function 1(sparsevec, sparsevec) sparsevec_negative_inner_product(sparsevec, sparsevec),
    function 3(sparsevec, sparsevec) hnsw_sparsevec_support(internal);

alter operator class sparsevec_ip_ops using hnsw owner to postgres;

create operator family sparsevec_cosine_ops using hnsw;

alter operator family sparsevec_cosine_ops using hnsw add
    operator 1 <=>(sparsevec, sparsevec) for order by float_ops,
    function 2(sparsevec, sparsevec) l2_norm(sparsevec),
    function 1(sparsevec, sparsevec) sparsevec_negative_inner_product(sparsevec, sparsevec),
    function 3(sparsevec, sparsevec) hnsw_sparsevec_support(internal);

alter operator family sparsevec_cosine_ops using hnsw owner to postgres;

create operator class sparsevec_cosine_ops for type sparsevec using hnsw as
    operator 1 <=>(sparsevec, sparsevec) for order by float_ops,
    function 2(sparsevec, sparsevec) l2_norm(sparsevec),
    function 1(sparsevec, sparsevec) sparsevec_negative_inner_product(sparsevec, sparsevec),
    function 3(sparsevec, sparsevec) hnsw_sparsevec_support(internal);

alter operator class sparsevec_cosine_ops using hnsw owner to postgres;

create operator family sparsevec_l1_ops using hnsw;

alter operator family sparsevec_l1_ops using hnsw add
    operator 1 <+>(sparsevec, sparsevec) for order by float_ops,
    function 1(sparsevec, sparsevec) l1_distance(sparsevec, sparsevec),
    function 3(sparsevec, sparsevec) hnsw_sparsevec_support(internal);

alter operator family sparsevec_l1_ops using hnsw owner to postgres;

create operator class sparsevec_l1_ops for type sparsevec using hnsw as
    operator 1 <+>(sparsevec, sparsevec) for order by float_ops,
    function 3(sparsevec, sparsevec) hnsw_sparsevec_support(internal),
    function 1(sparsevec, sparsevec) l1_distance(sparsevec, sparsevec);

alter operator class sparsevec_l1_ops using hnsw owner to postgres;

create operator family btree_hstore_ops using btree;

alter operator family btree_hstore_ops using btree add
    operator 4 #>=#(hstore, hstore),
    operator 2 #<=#(hstore, hstore),
    operator 3 =(hstore, hstore),
    operator 1 #<#(hstore, hstore),
    operator 5 #>#(hstore, hstore),
    function 1(hstore, hstore) hstore_cmp(hstore, hstore);

alter operator family btree_hstore_ops using btree owner to postgres;

create operator class btree_hstore_ops default for type hstore using btree as
    operator 3 =(hstore, hstore),
    operator 5 #>#(hstore, hstore),
    operator 4 #>=#(hstore, hstore),
    operator 2 #<=#(hstore, hstore),
    operator 1 #<#(hstore, hstore),
    function 1(hstore, hstore) hstore_cmp(hstore, hstore);

alter operator class btree_hstore_ops using btree owner to postgres;

create operator family hash_hstore_ops using hash;

alter operator family hash_hstore_ops using hash add
    operator 1 =(hstore, hstore),
    function 2(hstore, hstore) hstore_hash_extended(hstore, bigint),
    function 1(hstore, hstore) hstore_hash(hstore);

alter operator family hash_hstore_ops using hash owner to postgres;

create operator class hash_hstore_ops default for type hstore using hash as
    operator 1 =(hstore, hstore),
    function 1(hstore, hstore) hstore_hash(hstore);

alter operator class hash_hstore_ops using hash owner to postgres;

create operator family gist_hstore_ops using gist;

alter operator family gist_hstore_ops using gist add
    operator 7 @>(hstore, hstore),
    operator 9 ?(hstore, text),
    operator 10 ?|(hstore, text[]),
    operator 11 ?&(hstore, text[]),
    function 6(hstore, hstore) ghstore_picksplit(internal, internal),
    function 1(hstore, hstore) ghstore_consistent(internal, hstore, smallint, oid, internal),
    function 2(hstore, hstore) ghstore_union(internal, internal),
    function 3(hstore, hstore) ghstore_compress(internal),
    function 4(hstore, hstore) ghstore_decompress(internal),
    function 5(hstore, hstore) ghstore_penalty(internal, internal, internal),
    function 7(hstore, hstore) ghstore_same(ghstore, ghstore, internal),
    function 10(hstore, hstore) ghstore_options(internal);

alter operator family gist_hstore_ops using gist owner to postgres;

create operator class gist_hstore_ops default for type hstore using gist as storage ghstore function 6(hstore, hstore) ghstore_picksplit(internal, internal),
	function 5(hstore, hstore) ghstore_penalty(internal, internal, internal),
	function 7(hstore, hstore) ghstore_same(ghstore, ghstore, internal),
	function 1(hstore, hstore) ghstore_consistent(internal, hstore, smallint, oid, internal),
	function 2(hstore, hstore) ghstore_union(internal, internal);

alter operator class gist_hstore_ops using gist owner to postgres;

create operator family gin_hstore_ops using gin;

alter operator family gin_hstore_ops using gin add
    operator 7 @>(hstore, hstore),
    operator 9 ?(hstore, text),
    operator 10 ?|(hstore, text[]),
    operator 11 ?&(hstore, text[]),
    function 4(hstore, hstore) gin_consistent_hstore(internal, smallint, hstore, integer, internal, internal),
    function 1(hstore, hstore) pg_catalog.bttextcmp(unknown, unknown),
    function 3(hstore, hstore) gin_extract_hstore_query(hstore, internal, smallint, internal, internal),
    function 2(hstore, hstore) gin_extract_hstore(hstore, internal);

alter operator family gin_hstore_ops using gin owner to postgres;

create operator class gin_hstore_ops default for type hstore using gin as storage text function 3(hstore, hstore) gin_extract_hstore_query(hstore, internal, smallint, internal, internal),
	function 2(hstore, hstore) gin_extract_hstore(hstore, internal);

alter operator class gin_hstore_ops using gin owner to postgres;

-- Cyclic dependencies found

create operator <> (procedure = halfvec_ne, leftarg = halfvec, rightarg = halfvec, commutator = <>, negator = =, join = pg_catalog.eqjoinsel, restrict = pg_catalog.eqsel);

alter operator <>(halfvec, halfvec) owner to postgres;

create operator = (procedure = halfvec_eq, leftarg = halfvec, rightarg = halfvec, commutator = =, negator = <>, join = pg_catalog.eqjoinsel, restrict = pg_catalog.eqsel);

alter operator =(halfvec, halfvec) owner to postgres;

-- Cyclic dependencies found

create operator <> (procedure = hstore_ne, leftarg = hstore, rightarg = hstore, commutator = <>, negator = =, join = pg_catalog.neqjoinsel, restrict = pg_catalog.neqsel);

alter operator <>(hstore, hstore) owner to postgres;

create operator = (procedure = hstore_eq, leftarg = hstore, rightarg = hstore, commutator = =, negator = <>, join = pg_catalog.eqjoinsel, restrict = pg_catalog.eqsel, hashes, merges);

alter operator =(hstore, hstore) owner to postgres;

-- Cyclic dependencies found

create operator <> (procedure = sparsevec_ne, leftarg = sparsevec, rightarg = sparsevec, commutator = <>, negator = =, join = pg_catalog.eqjoinsel, restrict = pg_catalog.eqsel);

alter operator <>(sparsevec, sparsevec) owner to postgres;

create operator = (procedure = sparsevec_eq, leftarg = sparsevec, rightarg = sparsevec, commutator = =, negator = <>, join = pg_catalog.eqjoinsel, restrict = pg_catalog.eqsel);

alter operator =(sparsevec, sparsevec) owner to postgres;

-- Cyclic dependencies found

create operator <> (procedure = vector_ne, leftarg = vector, rightarg = vector, commutator = <>, negator = =, join = pg_catalog.eqjoinsel, restrict = pg_catalog.eqsel);

alter operator <>(vector, vector) owner to postgres;

create operator = (procedure = vector_eq, leftarg = vector, rightarg = vector, commutator = =, negator = <>, join = pg_catalog.eqjoinsel, restrict = pg_catalog.eqsel);

alter operator =(vector, vector) owner to postgres;

-- Cyclic dependencies found

create operator <@ (procedure = hs_contained, leftarg = hstore, rightarg = hstore, commutator = @>, join = pg_catalog.matchingjoinsel, restrict = pg_catalog.matchingsel);

alter operator <@(hstore, hstore) owner to postgres;

create operator @> (procedure = hs_contains, leftarg = hstore, rightarg = hstore, commutator = <@, join = pg_catalog.matchingjoinsel, restrict = pg_catalog.matchingsel);

alter operator @>(hstore, hstore) owner to postgres;

-- Cyclic dependencies found

grant select, update, usage on sequence tool_version_id_seq to agent_mesh;

-- Cyclic dependencies found

-- Cyclic dependencies found

-- Cyclic dependencies found

grant select, update, usage on sequence tools_id_seq to agent_mesh;

-- Cyclic dependencies found

-- Cyclic dependencies found

-- Cyclic dependencies found

grant delete, insert, references, select, trigger, truncate, update on tool_version to agent_mesh;

-- Cyclic dependencies found

-- Cyclic dependencies found

create table tool_version
(
    id                  bigserial
        primary key,
    tool_id             bigint      not null
        references tools
            on delete cascade,
    version_number      varchar(20) not null,
    version_name        varchar(100),
    description         text,
    source_type         varchar(20) not null,
    input_schema        jsonb       not null,
    output_schema       jsonb,
    custom_endpoint_url varchar(500),
    mcp_server_id       bigint,
    is_active           boolean   default false,
    is_current          boolean   default false,
    parent_version_id   bigint,
    change_log          text,
    created_by          bigint      not null,
    created_at          timestamp default CURRENT_TIMESTAMP,
    unique (tool_id, version_number)
);

comment on table tool_version is '工具版本管理表：存储工具的各个版本快照，支持版本切换和回滚';

comment on column tool_version.id is '主键：自增 ID';

comment on column tool_version.tool_id is '工具 ID：关联 tools 表';

comment on column tool_version.version_number is '版本号：格式如 1.0.0, 1.1.0, 2.0.0';

comment on column tool_version.version_name is '版本名称：可选的友好名称';

comment on column tool_version.description is '版本变更说明';

comment on column tool_version.source_type is '工具来源：冗余字段 [SYSTEM, USER_HTTP, USER_MCP, USER_AGENT]';

comment on column tool_version.input_schema is '输入参数 Schema 快照';

comment on column tool_version.output_schema is '输出参数 Schema 快照';

comment on column tool_version.custom_endpoint_url is '自定义执行 URL 快照';

comment on column tool_version.mcp_server_id is '关联 MCP 服务 ID 快照';

comment on column tool_version.is_active is '是否当前激活版本：true 表示正在使用的版本';

comment on column tool_version.is_current is '是否最新版本：true 表示该工具的最新版本';

comment on column tool_version.parent_version_id is '父版本 ID：用于版本溯源';

comment on column tool_version.change_log is '变更日志：详细记录本次版本的改动';

comment on column tool_version.created_by is '创建人用户 ID';

comment on column tool_version.created_at is '创建时间';

alter table tool_version
    owner to postgres;

create table tools
(
    id                   bigserial
        primary key,
    owner_id             bigint,
    source_type          varchar(20)  not null,
    tool_code_name       varchar(100) not null,
    display_name         varchar(100) not null,
    description          text,
    mcp_server_id        bigint
        references mcp_servers,
    input_schema         jsonb        not null,
    output_schema        jsonb,
    custom_endpoint_url  varchar(500),
    is_enabled           boolean   default true,
    is_delete            smallint  default 0,
    created_at           timestamp default CURRENT_TIMESTAMP,
    updated_at           timestamp default CURRENT_TIMESTAMP,
    current_version_id   bigint
        references tool_version,
    health_status        smallint  default 0,
    last_health_check    timestamp,
    consecutive_failures integer   default 0,
    last_error_message   text,
    unique (owner_id, tool_code_name)
);

comment on table tools is '统一工具表：存储所有可用工具定义 (系统内置 + 用户自定义 HTTP + 用户 MCP 暴露的工具)';

comment on column tools.id is '主键：自增 ID';

comment on column tools.owner_id is '归属用户ID：NULL 表示系统内置工具 (所有用户可见); 非 NULL 表示用户私有工具';

comment on column tools.source_type is '工具来源：枚举值 [SYSTEM, USER_HTTP, USER_MCP, USER_AGENT]';

comment on column tools.tool_code_name is '工具代码名：LLM 调用时使用的唯一标识符 (例："get_weather")';

comment on column tools.display_name is '工具显示名：前端展示的友好名称';

comment on column tools.description is '工具描述：功能说明，用于帮助 LLM 理解何时调用该工具';

comment on column tools.mcp_server_id is '关联 MCP 服务ID：若来源为 USER_MCP，则指向 mcp_servers 表；否则为 NULL';

comment on column tools.input_schema is '输入参数 Schema：JSON Schema 格式，定义 LLM 需要传递的参数结构';

comment on column tools.output_schema is '输出参数 Schema：可选，定义预期返回值的结构';

comment on column tools.custom_endpoint_url is '自定义执行 URL：USER_HTTP 模式必填；SYSTEM/MCP 模式通常为空或使用默认路由';

comment on column tools.is_enabled is '是否启用：false 表示暂时对智能体隐藏';

comment on column tools.is_delete is '逻辑删除标记：0=正常, 1=已删除';

comment on column tools.created_at is '创建时间';

comment on column tools.updated_at is '最后更新时间';

comment on column tools.current_version_id is '当前使用的版本 ID：关联 tool_version 表';

comment on column tools.health_status is '工具健康状态：0=未知，1=健康，2=异常，3=禁用';

comment on column tools.last_health_check is '最后健康检查时间';

comment on column tools.consecutive_failures is '连续失败次数';

comment on column tools.last_error_message is '最后错误信息';

alter table tools
    owner to postgres;

create index idx_tools_lookup
    on tools (owner_id, source_type, is_enabled, is_delete);

create index idx_tools_health_status
    on tools (health_status, is_enabled, is_delete);

grant delete, insert, references, select, trigger, truncate, update on tools to agent_mesh;

create index idx_tool_version_tool
    on tool_version (tool_id, is_current, is_active);

create index idx_tool_version_active
    on tool_version (tool_id, is_active);

-- Cyclic dependencies found

create operator #<# (procedure = hstore_lt, leftarg = hstore, rightarg = hstore, commutator = #>#, negator = #>=#, join = pg_catalog.scalarltjoinsel, restrict = pg_catalog.scalarltsel);

alter operator #<#(hstore, hstore) owner to postgres;

-- Cyclic dependencies found

create operator #># (procedure = hstore_gt, leftarg = hstore, rightarg = hstore, commutator = #<#, negator = #<=#, join = pg_catalog.scalargtjoinsel, restrict = pg_catalog.scalargtsel);

alter operator #>#(hstore, hstore) owner to postgres;

-- Cyclic dependencies found

create operator #<=# (procedure = hstore_le, leftarg = hstore, rightarg = hstore, commutator = #>=#, negator = #>#, join = pg_catalog.scalarlejoinsel, restrict = pg_catalog.scalarlesel);

alter operator #<=#(hstore, hstore) owner to postgres;

create operator #>=# (procedure = hstore_ge, leftarg = hstore, rightarg = hstore, commutator = #<=#, negator = #<#, join = pg_catalog.scalargejoinsel, restrict = pg_catalog.scalargesel);

alter operator #>=#(hstore, hstore) owner to postgres;

-- Cyclic dependencies found

create operator < (procedure = halfvec_lt, leftarg = halfvec, rightarg = halfvec, commutator = >, negator = >=, join = pg_catalog.scalarltjoinsel, restrict = pg_catalog.scalarltsel);

alter operator <(halfvec, halfvec) owner to postgres;

-- Cyclic dependencies found

create operator > (procedure = halfvec_gt, leftarg = halfvec, rightarg = halfvec, commutator = <, negator = <=, join = pg_catalog.scalargtjoinsel, restrict = pg_catalog.scalargtsel);

alter operator >(halfvec, halfvec) owner to postgres;

-- Cyclic dependencies found

create operator <= (procedure = halfvec_le, leftarg = halfvec, rightarg = halfvec, commutator = >=, negator = >, join = pg_catalog.scalarlejoinsel, restrict = pg_catalog.scalarlesel);

alter operator <=(halfvec, halfvec) owner to postgres;

create operator >= (procedure = halfvec_ge, leftarg = halfvec, rightarg = halfvec, commutator = <=, negator = <, join = pg_catalog.scalargejoinsel, restrict = pg_catalog.scalargesel);

alter operator >=(halfvec, halfvec) owner to postgres;

-- Cyclic dependencies found

create operator < (procedure = sparsevec_lt, leftarg = sparsevec, rightarg = sparsevec, commutator = >, negator = >=, join = pg_catalog.scalarltjoinsel, restrict = pg_catalog.scalarltsel);

alter operator <(sparsevec, sparsevec) owner to postgres;

-- Cyclic dependencies found

create operator > (procedure = sparsevec_gt, leftarg = sparsevec, rightarg = sparsevec, commutator = <, negator = <=, join = pg_catalog.scalargtjoinsel, restrict = pg_catalog.scalargtsel);

alter operator >(sparsevec, sparsevec) owner to postgres;

-- Cyclic dependencies found

create operator <= (procedure = sparsevec_le, leftarg = sparsevec, rightarg = sparsevec, commutator = >=, negator = >, join = pg_catalog.scalarlejoinsel, restrict = pg_catalog.scalarlesel);

alter operator <=(sparsevec, sparsevec) owner to postgres;

create operator >= (procedure = sparsevec_ge, leftarg = sparsevec, rightarg = sparsevec, commutator = <=, negator = <, join = pg_catalog.scalargejoinsel, restrict = pg_catalog.scalargesel);

alter operator >=(sparsevec, sparsevec) owner to postgres;

-- Cyclic dependencies found

create operator < (procedure = vector_lt, leftarg = vector, rightarg = vector, commutator = >, negator = >=, join = pg_catalog.scalarltjoinsel, restrict = pg_catalog.scalarltsel);

alter operator <(vector, vector) owner to postgres;

-- Cyclic dependencies found

create operator > (procedure = vector_gt, leftarg = vector, rightarg = vector, commutator = <, negator = <=, join = pg_catalog.scalargtjoinsel, restrict = pg_catalog.scalargtsel);

alter operator >(vector, vector) owner to postgres;

-- Cyclic dependencies found

create operator <= (procedure = vector_le, leftarg = vector, rightarg = vector, commutator = >=, negator = >, join = pg_catalog.scalarlejoinsel, restrict = pg_catalog.scalarlesel);

alter operator <=(vector, vector) owner to postgres;

create operator >= (procedure = vector_ge, leftarg = vector, rightarg = vector, commutator = <=, negator = <, join = pg_catalog.scalargejoinsel, restrict = pg_catalog.scalargesel);

alter operator >=(vector, vector) owner to postgres;

