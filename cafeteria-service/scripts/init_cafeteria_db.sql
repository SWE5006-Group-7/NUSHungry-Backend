-- ================================================
-- Cafeteria Service Database Initialization Script
-- ================================================
-- 从单体架构迁移 cafeteria、stall、image 表到独立的 PostgreSQL 数据库
-- 数据库: cafeteria_service
-- 版本: 1.0
-- 日期: 2025-01-19
-- ================================================

-- 创建数据库（如果在Docker中使用，此步骤通常已完成）
-- CREATE DATABASE cafeteria_service ENCODING 'UTF8';

-- 连接到数据库
\c cafeteria_service;

-- 设置时区
SET timezone = 'Asia/Singapore';

-- ================================================
-- 表结构: cafeteria (食堂表)
-- ================================================
DROP TABLE IF EXISTS cafeteria CASCADE;

CREATE TABLE cafeteria (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255),
  description VARCHAR(255),
  location VARCHAR(255),
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  nearest_bus_stop VARCHAR(255),
  nearest_carpark VARCHAR(255),
  halal_info VARCHAR(255),
  seating_capacity INTEGER,
  image_url VARCHAR(255),
  term_time_opening_hours VARCHAR(255),
  vacation_opening_hours VARCHAR(255),
  average_rating DECIMAL(3,2) DEFAULT 0.00,
  review_count INTEGER DEFAULT 0
);

-- 创建索引
CREATE INDEX idx_cafeteria_name ON cafeteria(name);
CREATE INDEX idx_cafeteria_location ON cafeteria(latitude, longitude);

-- 添加注释
COMMENT ON TABLE cafeteria IS '食堂信息表';
COMMENT ON COLUMN cafeteria.average_rating IS '平均评分';
COMMENT ON COLUMN cafeteria.review_count IS '评价数量';

-- ================================================
-- 表结构: stall (档口表)
-- ================================================
DROP TABLE IF EXISTS stall CASCADE;

CREATE TABLE stall (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255),
  cuisine_type VARCHAR(255),
  halal_info VARCHAR(255),
  contact VARCHAR(255),
  image_url VARCHAR(255),
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  average_price DOUBLE PRECISION,
  average_rating DOUBLE PRECISION,
  review_count INTEGER DEFAULT 0,
  cafeteria_id BIGINT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_stall_cafeteria FOREIGN KEY (cafeteria_id) 
    REFERENCES cafeteria(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_stall_cafeteria ON stall(cafeteria_id);
CREATE INDEX idx_stall_name ON stall(name);
CREATE INDEX idx_stall_cuisine_type ON stall(cuisine_type);
CREATE INDEX idx_stall_location ON stall(latitude, longitude);
CREATE INDEX idx_stall_rating ON stall(average_rating DESC);

-- 添加注释
COMMENT ON TABLE stall IS '档口信息表';
COMMENT ON COLUMN stall.review_count IS '评价数量';

-- ================================================
-- 表结构: image (图片表)
-- ================================================
DROP TABLE IF EXISTS image CASCADE;

CREATE TABLE image (
  id BIGSERIAL PRIMARY KEY,
  file_name VARCHAR(255) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  thumbnail_url VARCHAR(500),
  file_size BIGINT NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  width INTEGER,
  height INTEGER,
  uploaded_by BIGINT,
  entity_type VARCHAR(50),
  entity_id BIGINT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_image_entity ON image(entity_type, entity_id);
CREATE INDEX idx_image_uploaded_by ON image(uploaded_by, created_at);

-- 添加注释
COMMENT ON TABLE image IS '图片信息表';
COMMENT ON COLUMN image.file_name IS '文件名(含UUID)';
COMMENT ON COLUMN image.original_name IS '原始文件名';
COMMENT ON COLUMN image.file_path IS '文件存储路径';
COMMENT ON COLUMN image.file_url IS '访问URL';
COMMENT ON COLUMN image.thumbnail_url IS '缩略图URL';
COMMENT ON COLUMN image.file_size IS '文件大小(字节)';
COMMENT ON COLUMN image.content_type IS 'MIME类型';
COMMENT ON COLUMN image.width IS '图片宽度';
COMMENT ON COLUMN image.height IS '图片高度';
COMMENT ON COLUMN image.uploaded_by IS '上传用户ID';
COMMENT ON COLUMN image.entity_type IS '关联实体类型(STALL/CAFETERIA/REVIEW)';
COMMENT ON COLUMN image.entity_id IS '关联实体ID';
COMMENT ON COLUMN image.created_at IS '上传时间';

-- ================================================
-- 初始数据: cafeteria (食堂数据)
-- ================================================
INSERT INTO cafeteria (id, name, description, location, latitude, longitude, nearest_bus_stop, nearest_carpark, 
                      halal_info, seating_capacity, image_url, term_time_opening_hours, vacation_opening_hours, 
                      average_rating, review_count) VALUES
(1, 'Fine Food', 'Fine Food', 'Town Plaza', 1.305, 103.773, 'University Town', 'Stephen Riady Centre', 
 'HALAL FOOD OPTIONS AVAILABLE', 410, 'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Fine-Food-1-1024x684-1-898x600.jpg', 
 'Mon-Sun, 8.00am-8.30pm', 'Mon-Sun, 8.00am-8.30pm', 0.00, 0),
(2, 'Flavours @ UTown', 'Flavours @ UTown', 'UTown Stephen Riady Centre', 1.30462, 103.77252, 'University Town', 
 'Stephen Riady Centre', 'HALAL FOOD OPTIONS AVAILABLE', 700, 'https://uci.nus.edu.sg/wp-content/uploads/2025/08/Flavours-938x600.jpg', 
 'Mon-Sun, 7.30am-8.30pm', 'Mon-Sun, 7.30am-8.30pm', 0.00, 0),
(3, 'Central Square @ YIH', 'Central Square @ YIH', 'Yusof Ishak House', 1.29886, 103.77432, 'Yusof Ishak House', 
 'CP4 and CP5', 'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE', 314, 'https://uci.nus.edu.sg/wp-content/uploads/2025/05/YIH-800x600.jpg', 
 'Mon-Fri, 8.00am-8.00pm; Sat, 8.30am-2.30pm', 'Mon-Fri, 8.00am-8.00pm; Sat, 8.30am-2.30pm', 0.00, 0),
(4, 'Frontier', 'Frontier', 'Faculty of Science', 1.2961, 103.7831, 'Lower Kent Ridge Road - Blk S17', 'CP7', 
 'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE', 700, 'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Frontier-Canteen-1024x684-1-898x600.jpg', 
 'Mon-Fri, 7.30am-4.00pm/8.00pm; Sat, 7.30am-3.00pm; Sun/PH closed', 'Mon-Fri, 7.00am-7.00pm; Sat, 7.00am-2.00pm', 0.00, 0),
(5, 'PGP Aircon Canteen', 'PGP Aircon Canteen', 'Prince George''s Park', 1.29112, 103.78036, 'Prince George''s Park', 
 'Prince George''s Park Foyer', 'VEGETARIAN FOOD OPTION AVAILABLE', 308, 'https://uci.nus.edu.sg/wp-content/uploads/2024/02/PGP-canteen.jpg', 
 'Mon-Fri, 7.30am-8.00pm; Sat-Sun, 8.00am-8.00pm', 'Mon-Fri, 7.30am-8.00pm; Sat-Sun, 8.00am-8.00pm', 0.00, 0),
(6, 'Techno Edge', 'Techno Edge', 'College of Design and Engineering', 1.29796, 103.77153, 'Kent Ridge Crescent - Information Technology', 
 'CP17', 'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE', 450, 'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Techno-Edge-1024x684-1-898x600.jpg', 
 'Mon-Fri, 7.00am-8.00pm; Sat, 7.30am-2.00pm; Sun/PH closed', 'Mon-Fri, 7.00am-8.00pm; Sat, 7.30am-2.00pm; Sun/PH closed', 0.00, 0),
(7, 'The Deck', 'The Deck', 'Faculty of Arts & Social Sciences', 1.2948, 103.7715, 'Lower Kent Ridge Rd - Blk S12', 
 'CP11, CP15', 'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE', 1018, 'https://uci.nus.edu.sg/wp-content/uploads/2024/02/deck.jpg', 
 'Mon-Fri, 7.30am-4.00pm/8.00pm; Sat, 7.30am-3.00pm; Sun/PH closed', 'Mon-Fri, 7.00am-6.00pm', 0.00, 0),
(8, 'The Terrace', 'The Terrace', 'Computing 3 (COM3)', 1.2965, 103.7725, 'COM 3', 'CP11', 
 'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE', 756, 'https://uci.nus.edu.sg/wp-content/uploads/2024/02/WhatsApp-Image-2022-12-08-at-1.37.44-PM-1-1024x768-1-800x600.jpeg', 
 'Mon-Fri, 7.30am-7.00pm; Sat, 7.30am-2.00pm; Sun closed', 'Mon-Fri, 7.30am-3.00pm; Sat-Sun closed', 0.00, 0);

-- ================================================
-- 初始数据: stall (档口数据)
-- ================================================
INSERT INTO stall (id, name, cuisine_type, halal_info, contact, image_url, latitude, longitude, average_price, 
                  average_rating, review_count, cafeteria_id, created_at, updated_at) VALUES
(1, 'Taiwan Ichiban', 'Chinese', 'HALAL FOOD OPTIONS AVAILABLE', 'N/A', 
 'https://www.taiwanichiban.com//image/cache/catalog/social/slider1-1920x800.jpg', 
 1.30462, 103.77252, 7.5, 3.8, 2, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Chinese', 'Chinese cuisine', 'HALAL FOOD OPTIONS AVAILABLE', 'N/A', 
 'https://www.shicheng.news/images/image/1720/17206176.avif?1682420429', 
 1.30462, 103.77252, NULL, 4, 3, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Pasta Express', 'Pasta and Western cuisine', 'HALAL FOOD OPTIONS AVAILABLE', 'N/A', 
 'https://i0.wp.com/shopsinsg.com/wp-content/uploads/2020/10/build-your-own-pasta.jpg?w=560&ssl=1', 
 1.30462, 103.77252, NULL, 0, 0, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Ruyi Yuan Vegetarian', 'Vegetarian cuisine', 'HALAL FOOD OPTIONS AVAILABLE', 'N/A', 
 'https://food-cms.grab.com/compressed_webp/merchants/4-C263REEVRJ2YNT/hero/b0183d8d0f9e47fe9e2583c52a558ddf_1641779012807417776.webp', 
 1.305, 103.773, NULL, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'Yong Tao Foo', 'Yong Tao Foo', 'HALAL FOOD OPTIONS AVAILABLE', 'N/A', 
 'https://www.shicheng.news/images/image/1720/17206190.avif?1682420423', 
 1.30462, 103.77252, NULL, 4, 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'Western Crave', 'Western cuisine', 'HALAL FOOD OPTIONS AVAILABLE', 'N/A', 
 'https://scontent.fsin14-1.fna.fbcdn.net/v/t39.30808-6/481079656_944403831193045_946033071545871075_n.jpg', 
 1.29886, 103.77432, NULL, 0, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'Default', 'Default', 'HALAL FOOD OPTIONS AVAILABLE', 'N/A', 
 'https://th.bing.com/th/id/OIP.swinVrT6m0hoTGPwblccPgHaHa?w=192&h=193&c=7&r=0&o=5&dpr=2.4&pid=1.7', 
 1.305, 103.773, NULL, 5, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 重置序列（确保后续插入的ID从正确的值开始）
SELECT setval('cafeteria_id_seq', (SELECT MAX(id) FROM cafeteria));
SELECT setval('stall_id_seq', (SELECT MAX(id) FROM stall));

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

-- 为 stall 表创建自动更新触发器
CREATE TRIGGER update_stall_updated_at
    BEFORE UPDATE ON stall
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ================================================
-- 验证数据
-- ================================================
SELECT 'Cafeteria count: ' || COUNT(*) FROM cafeteria;
SELECT 'Stall count: ' || COUNT(*) FROM stall;
SELECT 'Image count: ' || COUNT(*) FROM image;

-- ================================================
-- 完成
-- ================================================
\echo 'Cafeteria Service database initialization completed!'
\echo 'Tables created: cafeteria, stall, image'
\echo 'Sample data inserted'
\echo 'Indexes and constraints created'
