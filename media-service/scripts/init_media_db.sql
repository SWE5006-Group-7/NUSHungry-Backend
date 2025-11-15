-- ================================================
-- Media Service Database Initialization Script
-- ================================================
-- 从单体架构迁移 media 相关表到独立的 PostgreSQL 数据库
-- 数据库: media_service
-- 版本: 1.0
-- 日期: 2025-01-19
-- ================================================

-- 创建数据库（如果在Docker中使用，此步骤通常已完成）
-- CREATE DATABASE media_service ENCODING 'UTF8';

-- 连接到数据库
\c media_service;

-- 设置时区
SET timezone = 'Asia/Singapore';

-- ================================================
-- 表结构: media_files (文件元数据表)
-- ================================================
DROP TABLE IF EXISTS media_files CASCADE;

CREATE TABLE media_files (
  id BIGSERIAL PRIMARY KEY,
  file_name VARCHAR(255) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  file_size BIGINT NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  storage_type VARCHAR(50) DEFAULT 'local',
  uploader_id BIGINT,
  entity_type VARCHAR(50),
  entity_id BIGINT,
  upload_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_file_size CHECK (file_size > 0),
  CONSTRAINT chk_storage_type CHECK (storage_type IN ('local', 'minio', 's3', 'oss'))
);

-- 创建索引
CREATE INDEX idx_media_files_entity ON media_files(entity_type, entity_id);
CREATE INDEX idx_media_files_uploader ON media_files(uploader_id);
CREATE INDEX idx_media_files_upload_time ON media_files(upload_time DESC);
CREATE INDEX idx_media_files_content_type ON media_files(content_type);

-- 添加注释
COMMENT ON TABLE media_files IS '文件元数据表';
COMMENT ON COLUMN media_files.file_name IS '文件名(含UUID)';
COMMENT ON COLUMN media_files.original_name IS '原始文件名';
COMMENT ON COLUMN media_files.file_path IS '文件存储路径';
COMMENT ON COLUMN media_files.file_url IS '访问URL';
COMMENT ON COLUMN media_files.file_size IS '文件大小(字节)';
COMMENT ON COLUMN media_files.content_type IS 'MIME类型';
COMMENT ON COLUMN media_files.storage_type IS '存储类型(local/minio/s3/oss)';
COMMENT ON COLUMN media_files.uploader_id IS '上传用户ID';
COMMENT ON COLUMN media_files.entity_type IS '关联实体类型(STALL/CAFETERIA/REVIEW/USER)';
COMMENT ON COLUMN media_files.entity_id IS '关联实体ID';
COMMENT ON COLUMN media_files.upload_time IS '上传时间';

-- ================================================
-- 表结构: image_metadata (图片元数据表)
-- ================================================
DROP TABLE IF EXISTS image_metadata CASCADE;

CREATE TABLE image_metadata (
  id BIGSERIAL PRIMARY KEY,
  media_file_id BIGINT NOT NULL,
  width INTEGER,
  height INTEGER,
  format VARCHAR(50),
  thumbnail_url VARCHAR(500),
  compressed BOOLEAN DEFAULT FALSE,
  compression_ratio DECIMAL(5,2),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_image_media_file FOREIGN KEY (media_file_id) 
    REFERENCES media_files(id) ON DELETE CASCADE,
  CONSTRAINT chk_dimensions CHECK (width > 0 AND height > 0)
);

-- 创建索引
CREATE INDEX idx_image_metadata_file ON image_metadata(media_file_id);
CREATE INDEX idx_image_metadata_format ON image_metadata(format);

-- 添加注释
COMMENT ON TABLE image_metadata IS '图片元数据表';
COMMENT ON COLUMN image_metadata.media_file_id IS '关联的文件ID';
COMMENT ON COLUMN image_metadata.width IS '图片宽度';
COMMENT ON COLUMN image_metadata.height IS '图片高度';
COMMENT ON COLUMN image_metadata.format IS '图片格式(jpg/png/webp等)';
COMMENT ON COLUMN image_metadata.thumbnail_url IS '缩略图URL';
COMMENT ON COLUMN image_metadata.compressed IS '是否已压缩';
COMMENT ON COLUMN image_metadata.compression_ratio IS '压缩比';

-- ================================================
-- 表结构: upload_sessions (上传会话表)
-- ================================================
DROP TABLE IF EXISTS upload_sessions CASCADE;

CREATE TABLE upload_sessions (
  id BIGSERIAL PRIMARY KEY,
  session_id VARCHAR(100) UNIQUE NOT NULL,
  user_id BIGINT,
  file_count INTEGER DEFAULT 0,
  total_size BIGINT DEFAULT 0,
  status VARCHAR(50) DEFAULT 'IN_PROGRESS',
  started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP WITH TIME ZONE,
  CONSTRAINT chk_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

-- 创建索引
CREATE INDEX idx_upload_sessions_user ON upload_sessions(user_id);
CREATE INDEX idx_upload_sessions_status ON upload_sessions(status);
CREATE INDEX idx_upload_sessions_started ON upload_sessions(started_at DESC);

-- 添加注释
COMMENT ON TABLE upload_sessions IS '上传会话表';
COMMENT ON COLUMN upload_sessions.session_id IS '会话ID';
COMMENT ON COLUMN upload_sessions.user_id IS '用户ID';
COMMENT ON COLUMN upload_sessions.file_count IS '文件数量';
COMMENT ON COLUMN upload_sessions.total_size IS '总大小(字节)';
COMMENT ON COLUMN upload_sessions.status IS '状态';

-- ================================================
-- 创建更新时间戳触发器函数
-- ================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为 media_files 表创建自动更新触发器
CREATE TRIGGER update_media_files_updated_at
    BEFORE UPDATE ON media_files
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ================================================
-- 创建清理过期会话函数
-- ================================================
CREATE OR REPLACE FUNCTION clean_expired_sessions()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    -- 删除 7 天前的已完成或失败会话
    DELETE FROM upload_sessions
    WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED')
      AND started_at < CURRENT_TIMESTAMP - INTERVAL '7 days';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ================================================
-- 创建统计视图
-- ================================================
CREATE OR REPLACE VIEW file_statistics AS
SELECT 
    storage_type,
    content_type,
    COUNT(*) as file_count,
    SUM(file_size) as total_size,
    AVG(file_size) as avg_size,
    MAX(file_size) as max_size,
    MIN(file_size) as min_size
FROM media_files
GROUP BY storage_type, content_type;

COMMENT ON VIEW file_statistics IS '文件统计视图';

-- ================================================
-- 创建图片统计视图
-- ================================================
CREATE OR REPLACE VIEW image_statistics AS
SELECT 
    im.format,
    COUNT(*) as image_count,
    AVG(im.width) as avg_width,
    AVG(im.height) as avg_height,
    COUNT(*) FILTER (WHERE im.compressed = TRUE) as compressed_count,
    AVG(im.compression_ratio) FILTER (WHERE im.compressed = TRUE) as avg_compression_ratio
FROM image_metadata im
GROUP BY im.format;

COMMENT ON VIEW image_statistics IS '图片统计视图';

-- ================================================
-- 初始数据（测试数据）
-- ================================================
-- 由于文件数据通常通过应用上传，这里不插入初始数据
-- 如果需要从单体架构迁移数据，请使用迁移脚本

-- ================================================
-- 验证数据
-- ================================================
SELECT 'Media files count: ' || COUNT(*) FROM media_files;
SELECT 'Image metadata count: ' || COUNT(*) FROM image_metadata;
SELECT 'Upload sessions count: ' || COUNT(*) FROM upload_sessions;

-- ================================================
-- 完成
-- ================================================
\echo 'Media Service database initialization completed!'
\echo 'Tables created: media_files, image_metadata, upload_sessions'
\echo 'Views created: file_statistics, image_statistics'
\echo 'Indexes and constraints created'
\echo 'Cleanup function: clean_expired_sessions()'
