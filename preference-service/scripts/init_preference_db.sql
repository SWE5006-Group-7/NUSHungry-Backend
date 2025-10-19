-- Preference Service Database Initialization Script
\c preference_service;
SET timezone = 'Asia/Singapore';

-- favorites 表
DROP TABLE IF EXISTS favorites CASCADE;
CREATE TABLE favorites (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  stall_id BIGINT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  sort_order INTEGER,
  CONSTRAINT uk_user_stall UNIQUE (user_id, stall_id)
);
CREATE INDEX idx_favorites_user ON favorites(user_id);
CREATE INDEX idx_favorites_stall ON favorites(stall_id);

-- search_history 表
DROP TABLE IF EXISTS search_history CASCADE;
CREATE TABLE search_history (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT,
  keyword VARCHAR(255) NOT NULL,
  search_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  search_type VARCHAR(50),
  result_count INTEGER,
  ip_address VARCHAR(50)
);
CREATE INDEX idx_search_history_user ON search_history(user_id);
CREATE INDEX idx_search_history_time ON search_history(search_time DESC);

\echo 'Preference Service database initialization completed!';
