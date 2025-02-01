-- Create experts table
CREATE TABLE IF NOT EXISTS experts (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    surname VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255),
    post_code VARCHAR(255),
    role VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    chamber_of_commerce_number VARCHAR(255),
    stripe_customer_id VARCHAR(255),
    address VARCHAR(255),
    payment_issues_count INT DEFAULT 0,
    account_blocked BOOLEAN DEFAULT FALSE,
    balance DECIMAL(19,2) DEFAULT 0.00,
    job_title_id VARCHAR(255),
    payment_info_id VARCHAR(255)
); 