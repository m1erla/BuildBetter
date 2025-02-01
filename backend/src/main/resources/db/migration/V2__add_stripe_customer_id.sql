-- Mevcut null değerleri güncelle (eğer varsa)
UPDATE experts SET stripe_customer_id = CONCAT('LEGACY_', id) WHERE stripe_customer_id IS NULL;

-- MySQL için stripe_customer_id sütununu NOT NULL olarak güncelle
ALTER TABLE experts MODIFY COLUMN stripe_customer_id VARCHAR(255) NOT NULL; 