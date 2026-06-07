-- =============================================
-- Notebook RAG 知识库数据库表结构
-- 版本: 1.0.0
-- 描述: 创建核心知识库表
-- =============================================

-- 1. 知识库表
CREATE TABLE IF NOT EXISTS kb_knowledge_base (
    id VARCHAR(64) PRIMARY KEY COMMENT '知识库唯一标识',
    name VARCHAR(255) NOT NULL COMMENT '知识库名称',
    description TEXT COMMENT '知识库描述',
    icon VARCHAR(64) DEFAULT 'book' COMMENT '知识库图标',
    category_path VARCHAR(512) COMMENT '分类路径',
    status VARCHAR(16) DEFAULT 'active' COMMENT '状态: active/archived/disabled',
    owner VARCHAR(128) COMMENT '所有者',
    language VARCHAR(16) DEFAULT 'zh_CN' COMMENT '语言: zh_CN/en_US',
    vector_index_id VARCHAR(64) COMMENT '向量索引ID',
    doc_count INT DEFAULT 0 COMMENT '文档数量',
    faq_count INT DEFAULT 0 COMMENT 'FAQ数量',
    hit_count BIGINT DEFAULT 0 COMMENT '命中次数',
    satisfaction DOUBLE DEFAULT 0.0 COMMENT '满意度评分(0-5)',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_status (status),
    INDEX idx_category (category_path),
    INDEX idx_owner (owner),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- 2. 知识点表
CREATE TABLE IF NOT EXISTS kb_knowledge_point (
    id VARCHAR(64) PRIMARY KEY COMMENT '知识点唯一标识',
    kb_id VARCHAR(64) NOT NULL COMMENT '所属知识库ID',
    title VARCHAR(500) NOT NULL COMMENT '知识点标题',
    content TEXT NOT NULL COMMENT '知识点内容',
    summary VARCHAR(1000) COMMENT '内容摘要',
    keywords VARCHAR(500) COMMENT '关键词（逗号分隔）',
    category VARCHAR(255) COMMENT '分类',
    tags VARCHAR(500) COMMENT '标签（逗号分隔）',
    source VARCHAR(255) COMMENT '来源',
    source_url VARCHAR(1000) COMMENT '来源URL',
    difficulty VARCHAR(16) DEFAULT 'medium' COMMENT '难度: easy/medium/hard',
    status VARCHAR(16) DEFAULT 'active' COMMENT '状态: active/archived/disabled',
    priority INT DEFAULT 0 COMMENT '优先级(0-100)',
    view_count INT DEFAULT 0 COMMENT '查看次数',
    like_count INT DEFAULT 0 COMMENT '点赞次数',
    dislike_count INT DEFAULT 0 COMMENT '点踩次数',
    created_by VARCHAR(128) COMMENT '创建者',
    updated_by VARCHAR(128) COMMENT '更新者',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_kb_id_title (kb_id, title),
    INDEX idx_kb_id (kb_id),
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_tags (tags),
    INDEX idx_created_by (created_by),
    FOREIGN KEY (kb_id) REFERENCES kb_knowledge_base(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点表';

-- 3. FAQ对表
CREATE TABLE IF NOT EXISTS kb_faq_pair (
    id VARCHAR(64) PRIMARY KEY COMMENT 'FAQ唯一标识',
    kb_id VARCHAR(64) NOT NULL COMMENT '所属知识库ID',
    question TEXT NOT NULL COMMENT '问题',
    answer TEXT NOT NULL COMMENT '答案',
    category VARCHAR(255) COMMENT '分类',
    keywords VARCHAR(500) COMMENT '关键词',
    difficulty VARCHAR(16) DEFAULT 'easy' COMMENT '难度: easy/medium/hard',
    status VARCHAR(16) DEFAULT 'active' COMMENT '状态: active/archived/disabled',
    priority INT DEFAULT 0 COMMENT '优先级',
    view_count INT DEFAULT 0 COMMENT '查看次数',
    helpful_count INT DEFAULT 0 COMMENT '有用次数',
    not_helpful_count INT DEFAULT 0 COMMENT '无用次数',
    skip_count INT DEFAULT 0 COMMENT '跳过次数',
    created_by VARCHAR(128) COMMENT '创建者',
    updated_by VARCHAR(128) COMMENT '更新者',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_kb_id_question (kb_id, question(255)),
    INDEX idx_kb_id (kb_id),
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_keywords (keywords),
    FOREIGN KEY (kb_id) REFERENCES kb_knowledge_base(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='FAQ对表';

-- 4. 会话日志表
CREATE TABLE IF NOT EXISTS kb_conversation_log (
    id VARCHAR(64) PRIMARY KEY COMMENT '会话日志唯一标识',
    kb_id VARCHAR(64) COMMENT '关联的知识库ID',
    session_id VARCHAR(128) NOT NULL COMMENT '会话ID',
    user_id VARCHAR(128) COMMENT '用户ID',
    turn_id INT NOT NULL COMMENT '对话轮次',
    question TEXT NOT NULL COMMENT '用户问题',
    answer TEXT COMMENT '系统回答',
    context TEXT COMMENT '检索上下文(JSON)',
    source_docs TEXT COMMENT '来源文档(JSON)',
    confidence DOUBLE COMMENT '置信度',
    latency INT COMMENT '响应延迟(ms)',
    feedback VARCHAR(16) COMMENT '反馈: positive/neutral/negative',
    feedback_comment TEXT COMMENT '反馈评论',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_session_id (session_id),
    INDEX idx_kb_id (kb_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (kb_id) REFERENCES kb_knowledge_base(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话日志表';

-- 5. 文档表
CREATE TABLE IF NOT EXISTS kb_document (
    id VARCHAR(64) PRIMARY KEY COMMENT '文档唯一标识',
    kb_id VARCHAR(64) NOT NULL COMMENT '所属知识库ID',
    title VARCHAR(500) NOT NULL COMMENT '文档标题',
    file_name VARCHAR(500) COMMENT '文件名',
    file_path VARCHAR(1000) COMMENT '文件路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    file_type VARCHAR(64) COMMENT '文件类型: pdf/docx/xlsx/txt/html/md',
    mime_type VARCHAR(128) COMMENT 'MIME类型',
    content_preview TEXT COMMENT '内容预览',
    page_count INT COMMENT '页数',
    status VARCHAR(16) DEFAULT 'processing' COMMENT '状态: processing/completed/failed',
    chunk_count INT DEFAULT 0 COMMENT '分块数量',
    vector_indexed BOOLEAN DEFAULT FALSE COMMENT '是否已建立向量索引',
    error_message TEXT COMMENT '错误信息',
    metadata TEXT COMMENT '元数据(JSON)',
    created_by VARCHAR(128) COMMENT '创建者',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_kb_id (kb_id),
    INDEX idx_status (status),
    INDEX idx_file_type (file_type),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (kb_id) REFERENCES kb_knowledge_base(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- 6. 分块表
CREATE TABLE IF NOT EXISTS kb_chunk (
    id VARCHAR(64) PRIMARY KEY COMMENT '分块唯一标识',
    doc_id VARCHAR(64) NOT NULL COMMENT '所属文档ID',
    chunk_index INT NOT NULL COMMENT '分块序号',
    content TEXT NOT NULL COMMENT '分块内容',
    content_type VARCHAR(32) DEFAULT 'text' COMMENT '内容类型: text/table/code/image',
    chunk_type VARCHAR(32) COMMENT '分块类型: separator/recursive/semantic/table',
    char_count INT COMMENT '字符数',
    estimated_tokens INT COMMENT '估算token数',
    metadata TEXT COMMENT '元数据(JSON)',
    vector_id VARCHAR(64) COMMENT '向量ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_doc_id_chunk_index (doc_id, chunk_index),
    INDEX idx_doc_id (doc_id),
    INDEX idx_vector_id (vector_id),
    INDEX idx_content_type (content_type),
    FOREIGN KEY (doc_id) REFERENCES kb_document(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分块表';

-- 7. 向量索引表
CREATE TABLE IF NOT EXISTS kb_vector_index (
    id VARCHAR(64) PRIMARY KEY COMMENT '向量唯一标识',
    chunk_id VARCHAR(64) NOT NULL COMMENT '关联分块ID',
    kb_id VARCHAR(64) COMMENT '关联知识库ID',
    vector_model VARCHAR(128) COMMENT '向量模型',
    vector_dimension INT COMMENT '向量维度',
    vector_data TEXT NOT NULL COMMENT '向量数据(Base64或JSON)',
    embedding_status VARCHAR(16) DEFAULT 'pending' COMMENT '向量化状态: pending/completed/failed',
    es_index_name VARCHAR(255) COMMENT 'Elasticsearch索引名',
    es_doc_id VARCHAR(128) COMMENT 'Elasticsearch文档ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_chunk_id (chunk_id),
    INDEX idx_kb_id (kb_id),
    INDEX idx_es_index (es_index_name),
    INDEX idx_embedding_status (embedding_status),
    FOREIGN KEY (chunk_id) REFERENCES kb_chunk(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='向量索引表';

-- 8. 代理执行记录表
CREATE TABLE IF NOT EXISTS kb_agent_record (
    id VARCHAR(64) PRIMARY KEY COMMENT '代理记录唯一标识',
    kb_id VARCHAR(64) COMMENT '关联知识库ID',
    session_id VARCHAR(128) NOT NULL COMMENT '会话ID',
    user_id VARCHAR(128) COMMENT '用户ID',
    agent_name VARCHAR(128) COMMENT '代理名称',
    agent_type VARCHAR(64) COMMENT '代理类型',
    input_data TEXT COMMENT '输入数据(JSON)',
    output_data TEXT COMMENT '输出数据(JSON)',
    tool_calls TEXT COMMENT '工具调用记录(JSON)',
    execution_time INT COMMENT '执行时间(ms)',
    status VARCHAR(16) DEFAULT 'running' COMMENT '状态: running/completed/failed',
    error_message TEXT COMMENT '错误信息',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_session_id (session_id),
    INDEX idx_kb_id (kb_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (kb_id) REFERENCES kb_knowledge_base(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代理执行记录表';

-- 9. 工单表（客服系统）
CREATE TABLE IF NOT EXISTS kb_ticket (
    id VARCHAR(64) PRIMARY KEY COMMENT '工单唯一标识',
    kb_id VARCHAR(64) COMMENT '关联知识库ID',
    ticket_number VARCHAR(64) NOT NULL COMMENT '工单编号',
    title VARCHAR(500) NOT NULL COMMENT '工单标题',
    description TEXT COMMENT '工单描述',
    priority VARCHAR(16) DEFAULT 'medium' COMMENT '优先级: low/medium/high/urgent',
    status VARCHAR(16) DEFAULT 'open' COMMENT '状态: open/in_progress/resolved/closed',
    category VARCHAR(255) COMMENT '分类',
    tags VARCHAR(500) COMMENT '标签',
    requester_id VARCHAR(128) COMMENT '请求者ID',
    assignee_id VARCHAR(128) COMMENT '受理人ID',
    resolution TEXT COMMENT '解决方案',
    resolution_time TIMESTAMP COMMENT '解决时间',
    satisfaction INT COMMENT '满意度(1-5)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_ticket_number (ticket_number),
    INDEX idx_kb_id (kb_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_category (category),
    INDEX idx_requester_id (requester_id),
    INDEX idx_assignee_id (assignee_id),
    FOREIGN KEY (kb_id) REFERENCES kb_knowledge_base(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单表';

-- 10. 缓存失效事件表
CREATE TABLE IF NOT EXISTS kb_cache_event (
    id VARCHAR(64) PRIMARY KEY COMMENT '事件唯一标识',
    event_type VARCHAR(32) NOT NULL COMMENT '事件类型: doc_update/kb_update/vector_update',
    resource_type VARCHAR(32) NOT NULL COMMENT '资源类型: kb/document/chunk/faq',
    resource_id VARCHAR(64) NOT NULL COMMENT '资源ID',
    event_data TEXT COMMENT '事件数据(JSON)',
    processed BOOLEAN DEFAULT FALSE COMMENT '是否已处理',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    processed_at TIMESTAMP COMMENT '处理时间',

    INDEX idx_event_type (event_type),
    INDEX idx_resource (resource_type, resource_id),
    INDEX idx_processed (processed),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缓存失效事件表';

-- =============================================
-- 初始化数据
-- =============================================

-- 创建默认知识库
INSERT INTO kb_knowledge_base (id, name, description, status, language, created_at, updated_at)
VALUES (
    'default_kb',
    '默认知识库',
    '系统默认知识库，包含通用知识',
    'active',
    'zh_CN',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 创建示例FAQ
INSERT INTO kb_faq_pair (id, kb_id, question, answer, category, status, created_at, updated_at)
VALUES (
    'faq_001',
    'default_kb',
    '什么是RAG？',
    'RAG（Retrieval-Augmented Generation）是一种结合检索和生成的AI技术，通过从知识库中检索相关信息来增强生成的准确性和可靠性。',
    '技术概念',
    'active',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 创建示例知识点
INSERT INTO kb_knowledge_point (id, kb_id, title, content, summary, category, status, created_at, updated_at)
VALUES (
    'kp_001',
    'default_kb',
    '向量检索原理',
    '向量检索是一种基于语义相似度的检索方法，通过将文本转换为向量，在高维空间中计算向量间的相似度来找到相关文档。常用的相似度计算方法包括余弦相似度、欧氏距离等。',
    '向量检索通过将文本映射到向量空间，利用向量相似度进行语义检索，能够捕捉词语间的语义关系。',
    '检索技术',
    'active',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();
