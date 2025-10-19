-- ================================================
-- Admin Service 数据库初始化脚本
-- 数据库: PostgreSQL 16
-- 编码: UTF8
-- ================================================

-- 设置时区
SET TIME ZONE 'Asia/Singapore';

-- ================================================
-- 1. 创建用户表（本地缓存/同步）
-- ================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    avatar_url VARCHAR(500),
    last_login TIMESTAMP,
    CONSTRAINT chk_role CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'))
);

-- 创建索引优化查询
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_enabled ON users(enabled);
CREATE INDEX idx_users_created_at ON users(created_at DESC);

-- ================================================
-- 2. 创建管理员审计日志表
-- ================================================
CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    admin_username VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id BIGINT,
    description TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'SUCCESS',
    error_message TEXT,
    CONSTRAINT chk_audit_status CHECK (status IN ('SUCCESS', 'FAILED', 'PENDING'))
);

-- 创建索引
CREATE INDEX idx_audit_admin_id ON admin_audit_logs(admin_id);
CREATE INDEX idx_audit_action ON admin_audit_logs(action);
CREATE INDEX idx_audit_created_at ON admin_audit_logs(created_at DESC);
CREATE INDEX idx_audit_target ON admin_audit_logs(target_type, target_id);
CREATE INDEX idx_audit_status ON admin_audit_logs(status);

-- ================================================
-- 3. 创建仪表盘缓存表
-- ================================================
CREATE TABLE IF NOT EXISTS dashboard_cache (
    id BIGSERIAL PRIMARY KEY,
    cache_key VARCHAR(100) NOT NULL UNIQUE,
    cache_value TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

-- 创建索引
CREATE INDEX idx_cache_key ON dashboard_cache(cache_key);
CREATE INDEX idx_cache_expires ON dashboard_cache(expires_at);

-- ================================================
-- 4. 创建更新时间触发器函数
-- ================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为 users 表添加触发器
CREATE TRIGGER users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 为 dashboard_cache 表添加触发器
CREATE TRIGGER cache_updated_at
BEFORE UPDATE ON dashboard_cache
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- ================================================
-- 5. 插入初始管理员账号
-- ================================================
-- 默认密码: Admin123! (需要在应用中使用 BCrypt 加密)
INSERT INTO users (username, password, email, role, enabled)
VALUES 
    ('admin', '$2a$10$XqYnJzYy3ZxR3xC8YvGxXeY8qJh2N5VqQvL3kJhFxYvGxXeY8qJh2N', 'admin@nushungry.com', 'ROLE_ADMIN', TRUE),
    ('superadmin', '$2a$10$XqYnJzYy3ZxR3xC8YvGxXeY8qJh2N5VqQvL3kJhFxYvGxXeY8qJh2N', 'superadmin@nushungry.com', 'ROLE_ADMIN', TRUE)
ON CONFLICT (username) DO NOTHING;

-- ================================================
-- 6. 创建视图：活跃管理员
-- ================================================
CREATE OR REPLACE VIEW active_admins AS
SELECT 
    id,
    username,
    email,
    created_at,
    last_login,
    (SELECT COUNT(*) FROM admin_audit_logs WHERE admin_id = users.id) AS action_count
FROM users
WHERE role = 'ROLE_ADMIN' AND enabled = TRUE;

-- ================================================
-- 7. 创建视图：最近审计日志
-- ================================================
CREATE OR REPLACE VIEW recent_audit_logs AS
SELECT 
    a.id,
    a.admin_username,
    a.action,
    a.target_type,
    a.target_id,
    a.description,
    a.created_at,
    a.status
FROM admin_audit_logs a
ORDER BY a.created_at DESC
LIMIT 100;

-- ================================================
-- 8. 清理过期缓存的函数
-- ================================================
CREATE OR REPLACE FUNCTION clean_expired_cache()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM dashboard_cache WHERE expires_at < CURRENT_TIMESTAMP;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ================================================
-- 9. 创建定期清理作业（需要 pg_cron 扩展，可选）
-- ================================================
-- 如果安装了 pg_cron 扩展，可以取消注释以下语句
-- CREATE EXTENSION IF NOT EXISTS pg_cron;
-- SELECT cron.schedule('clean-cache', '0 * * * *', 'SELECT clean_expired_cache();');

-- ================================================
-- 10. 授予权限（如果需要）
-- ================================================
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO admin;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO admin;

-- ================================================
-- 初始化完成
-- ================================================
DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE 'Admin Service 数据库初始化完成！';
    RAISE NOTICE '============================================';
    RAISE NOTICE '默认管理员账号:';
    RAISE NOTICE '  - username: admin';
    RAISE NOTICE '  - password: Admin123!';
    RAISE NOTICE '  - email: admin@nushungry.com';
    RAISE NOTICE '============================================';
    RAISE NOTICE '请在生产环境中立即修改默认密码！';
    RAISE NOTICE '============================================';
END $$;
