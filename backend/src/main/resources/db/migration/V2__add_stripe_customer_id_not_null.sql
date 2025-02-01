-- Update existing null stripe_customer_id values
UPDATE experts SET stripe_customer_id = CONCAT('LEGACY_', id) WHERE stripe_customer_id IS NULL;

-- Make stripe_customer_id column not nullable
ALTER TABLE experts MODIFY COLUMN stripe_customer_id VARCHAR(255) NOT NULL; 