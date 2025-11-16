-- 更新 review_reports 表的 handle_note 字段长度
ALTER TABLE review_reports MODIFY COLUMN handle_note VARCHAR(500);
