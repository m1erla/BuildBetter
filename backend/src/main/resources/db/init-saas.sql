-- BuildBetter SaaS Platform Initialization Script
-- Run this after first deployment to set up subscription plans and email templates

-- =====================================================
-- SUBSCRIPTION PLANS
-- =====================================================

-- Free Plan
INSERT INTO subscription_plans (id, name, display_name, tier, description,
    price_monthly, price_yearly, max_users, max_ads, max_storage_mb,
    max_api_calls_per_hour, custom_branding, priority_support, api_access,
    advanced_analytics, white_label, trial_days, is_active,
    created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'free',
    'Free Plan',
    'FREE',
    'Perfect for individuals and getting started with BuildBetter',
    0.00,
    0.00,
    5,
    10,
    1000,
    100,
    false,
    false,
    false,
    false,
    false,
    0,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Starter Plan
INSERT INTO subscription_plans (id, name, display_name, tier, description,
    price_monthly, price_yearly, max_users, max_ads, max_storage_mb,
    max_api_calls_per_hour, custom_branding, priority_support, api_access,
    advanced_analytics, white_label, trial_days, is_active,
    created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'starter',
    'Starter Plan',
    'STARTER',
    'Great for small teams and growing businesses',
    29.00,
    290.00,
    10,
    50,
    10000,
    1000,
    false,
    false,
    true,
    false,
    false,
    14,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Professional Plan
INSERT INTO subscription_plans (id, name, display_name, tier, description,
    price_monthly, price_yearly, max_users, max_ads, max_storage_mb,
    max_api_calls_per_hour, custom_branding, priority_support, api_access,
    advanced_analytics, white_label, trial_days, is_active,
    created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'professional',
    'Professional Plan',
    'PROFESSIONAL',
    'Ideal for growing businesses with advanced needs',
    99.00,
    990.00,
    50,
    250,
    50000,
    5000,
    true,
    true,
    true,
    true,
    false,
    14,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Business Plan
INSERT INTO subscription_plans (id, name, display_name, tier, description,
    price_monthly, price_yearly, max_users, max_ads, max_storage_mb,
    max_api_calls_per_hour, custom_branding, priority_support, api_access,
    advanced_analytics, white_label, trial_days, is_active,
    created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'business',
    'Business Plan',
    'BUSINESS',
    'For established companies requiring extensive resources',
    299.00,
    2990.00,
    200,
    1000,
    200000,
    20000,
    true,
    true,
    true,
    true,
    false,
    14,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Enterprise Plan
INSERT INTO subscription_plans (id, name, display_name, tier, description,
    price_monthly, price_yearly, max_users, max_ads, max_storage_mb,
    max_api_calls_per_hour, custom_branding, priority_support, api_access,
    advanced_analytics, white_label, trial_days, is_active,
    created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'enterprise',
    'Enterprise Plan',
    'ENTERPRISE',
    'Unlimited resources with white-label support for enterprises',
    999.00,
    9990.00,
    NULL,  -- Unlimited
    NULL,  -- Unlimited
    NULL,  -- Unlimited
    50000,
    true,
    true,
    true,
    true,
    true,
    14,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- =====================================================
-- EMAIL TEMPLATES
-- =====================================================

-- Welcome Email
INSERT INTO email_templates (id, type, subject, html_content, text_content, is_active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'WELCOME',
    'Welcome to {{appName}}!',
    '<!DOCTYPE html><html><head><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}h1{color:#4CAF50;}</style></head><body><h1>Welcome to {{appName}}, {{userName}}!</h1><p>We''re excited to have you on board. BuildBetter is your complete solution for connecting service providers with clients.</p><p><a href="{{appUrl}}" style="background:#4CAF50;color:white;padding:10px 20px;text-decoration:none;border-radius:5px;">Get Started</a></p><p>If you have any questions, feel free to contact our support team.</p></body></html>',
    'Welcome to {{appName}}, {{userName}}! We''re excited to have you on board. Visit {{appUrl}} to get started.',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Email Verification
INSERT INTO email_templates (id, type, subject, html_content, text_content, is_active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'EMAIL_VERIFICATION',
    'Verify your email address',
    '<!DOCTYPE html><html><body><h1>Verify Your Email</h1><p>Thank you for signing up! Please verify your email address by clicking the link below:</p><p><a href="{{verificationUrl}}" style="background:#4CAF50;color:white;padding:10px 20px;text-decoration:none;">Verify Email</a></p><p>If you didn''t create an account, you can safely ignore this email.</p></body></html>',
    'Verify your email by visiting: {{verificationUrl}}',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Password Reset
INSERT INTO email_templates (id, type, subject, html_content, text_content, is_active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'PASSWORD_RESET',
    'Reset your password',
    '<!DOCTYPE html><html><body><h1>Password Reset Request</h1><p>We received a request to reset your password. Click the link below to reset it:</p><p><a href="{{resetUrl}}" style="background:#4CAF50;color:white;padding:10px 20px;text-decoration:none;">Reset Password</a></p><p>This link will expire in 1 hour. If you didn''t request this, please ignore this email.</p></body></html>',
    'Reset your password by visiting: {{resetUrl}}',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Payment Success
INSERT INTO email_templates (id, type, subject, html_content, text_content, is_active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'PAYMENT_SUCCESS',
    'Payment Received - Thank You!',
    '<!DOCTYPE html><html><body><h1>Payment Successful</h1><p>Thank you! We''ve received your payment of {{amount}}.</p><p><a href="{{invoiceUrl}}">Download Invoice</a></p><p>Your subscription is now active.</p></body></html>',
    'Payment of {{amount}} received successfully. Download invoice: {{invoiceUrl}}',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Payment Failed
INSERT INTO email_templates (id, type, subject, html_content, text_content, is_active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'PAYMENT_FAILED',
    'Payment Failed - Action Required',
    '<!DOCTYPE html><html><body><h1>Payment Failed</h1><p>We were unable to process your payment of {{amount}}.</p><p>Reason: {{reason}}</p><p><a href="{{supportUrl}}">Update Payment Method</a></p><p>Please update your payment information to continue using {{appName}}.</p></body></html>',
    'Payment of {{amount}} failed. Reason: {{reason}}. Please update your payment method.',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Trial Ending
INSERT INTO email_templates (id, type, subject, html_content, text_content, is_active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'TRIAL_ENDING',
    'Your trial ends in {{daysLeft}} days',
    '<!DOCTYPE html><html><body><h1>Your Trial is Ending Soon</h1><p>Your {{appName}} trial will end in {{daysLeft}} days.</p><p><a href="{{upgradeUrl}}">Upgrade Now</a></p><p>Continue enjoying all features by upgrading to a paid plan.</p></body></html>',
    'Your trial ends in {{daysLeft}} days. Upgrade at: {{upgradeUrl}}',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Subscription Created
INSERT INTO email_templates (id, type, subject, html_content, text_content, is_active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'SUBSCRIPTION_CREATED',
    'Welcome to {{planName}}!',
    '<!DOCTYPE html><html><body><h1>Subscription Activated</h1><p>Your {{planName}} subscription is now active!</p><p><a href="{{dashboardUrl}}">Go to Dashboard</a></p><p>Enjoy all the features of your new plan.</p></body></html>',
    'Your {{planName}} subscription is active. Dashboard: {{dashboardUrl}}',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- =====================================================
-- FEATURE FLAGS
-- =====================================================

-- Example Feature Flags
INSERT INTO feature_flags (id, flag_key, name, description, is_enabled, rollout_percentage, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'advanced_analytics', 'Advanced Analytics', 'Enable advanced analytics dashboard', true, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'api_access', 'API Access', 'Enable REST API access', true, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'custom_branding', 'Custom Branding', 'Allow custom branding and logos', true, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'white_label', 'White Label', 'Full white-label capabilities', false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'two_factor_auth', 'Two-Factor Authentication', 'Enable 2FA for users', true, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================================================
-- LEGAL DOCUMENTS
-- =====================================================

-- Terms of Service
INSERT INTO legal_documents (id, type, version, title, content, is_current, effective_date, created_at)
VALUES (
    gen_random_uuid(),
    'TERMS_OF_SERVICE',
    '1.0',
    'Terms of Service',
    'BuildBetter Terms of Service - Version 1.0

1. Acceptance of Terms
By accessing and using BuildBetter, you accept and agree to be bound by the terms and provision of this agreement.

2. Use License
Permission is granted to temporarily download one copy of the materials for personal, non-commercial transitory viewing only.

3. Subscription Terms
- Subscriptions are billed monthly or annually
- You can cancel at any time
- Refunds are provided on a case-by-case basis

4. User Responsibilities
- Maintain the security of your account
- Do not share login credentials
- Comply with all applicable laws

[Add complete terms as needed]',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Privacy Policy
INSERT INTO legal_documents (id, type, version, title, content, is_current, effective_date, created_at)
VALUES (
    gen_random_uuid(),
    'PRIVACY_POLICY',
    '1.0',
    'Privacy Policy',
    'BuildBetter Privacy Policy - Version 1.0

1. Information We Collect
We collect information you provide directly to us when you create an account, use our services, or communicate with us.

2. How We Use Your Information
- To provide and maintain our services
- To process transactions
- To send you updates and marketing communications (with consent)

3. Data Security
We implement appropriate security measures to protect your personal information.

4. GDPR Compliance
EU users have rights to access, correct, and delete their personal data.

[Add complete privacy policy as needed]',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- =====================================================
-- Verification
-- =====================================================

SELECT 'Subscription Plans Created:' as status, COUNT(*) as count FROM subscription_plans;
SELECT 'Email Templates Created:' as status, COUNT(*) as count FROM email_templates;
SELECT 'Feature Flags Created:' as status, COUNT(*) as count FROM feature_flags;
SELECT 'Legal Documents Created:' as status, COUNT(*) as count FROM legal_documents;
