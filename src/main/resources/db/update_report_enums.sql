-- 更新 review_reports 表的枚举值以匹配代码定义
-- 执行时间: 2025-10-12

-- 更新 reason 列的 ENUM 值
-- 添加: OFF_TOPIC (与摊位无关), DUPLICATE (重复评价)
-- 移除: ADVERTISING, PRIVACY (旧的不再使用的值)
ALTER TABLE review_reports MODIFY COLUMN reason
ENUM('SPAM', 'OFFENSIVE', 'INAPPROPRIATE', 'FALSE_INFO', 'OFF_TOPIC', 'DUPLICATE', 'OTHER') NOT NULL;

-- 更新 status 列的 ENUM 值
-- 添加: REVIEWING (处理中), RESOLVED (已处理), CLOSED (已关闭)
-- 移除: ACCEPTED (改为 RESOLVED)
ALTER TABLE review_reports MODIFY COLUMN status
ENUM('PENDING', 'REVIEWING', 'RESOLVED', 'REJECTED', 'CLOSED') NOT NULL;
