# BuildBetter SaaS Platform - Deployment Guide

## Overview

BuildBetter has been transformed into a production-ready SaaS platform with comprehensive multi-tenancy, subscription management, and enterprise features.

## New SaaS Features

### 1. Multi-Tenancy & Organizations
- **Organization Management**: Complete workspace isolation
- **Team Collaboration**: Role-based access control (Owner, Admin, Member, Viewer)
- **Resource Isolation**: Tenant-scoped data segregation

### 2. Subscription & Billing
- **Flexible Plans**: FREE, STARTER, PROFESSIONAL, BUSINESS, ENTERPRISE tiers
- **Stripe Integration**: Automated subscription billing
- **Trial Management**: 14-day free trials with automatic conversion
- **Usage-Based Quotas**: Per-plan limits on users, ads, storage, API calls

### 3. Rate Limiting & Quotas
- **API Rate Limiting**: Per-organization request throttling
- **Usage Tracking**: Real-time metrics for all resources
- **Plan-Based Limits**: Automatic enforcement of subscription quotas

### 4. Email Notifications
- **Transactional Emails**: Welcome, verification, password reset
- **Billing Emails**: Payment success/failure, invoice delivery
- **Subscription Alerts**: Trial ending, subscription renewal
- **Template System**: Customizable email templates

### 5. Enhanced Security
- **2FA Support**: Two-factor authentication infrastructure
- **Audit Logging**: Complete activity tracking
- **API Keys**: Programmatic access with scoped permissions
- **Password Policies**: Configurable strength requirements

### 6. Advanced Admin Dashboard
- **SaaS Metrics**: MRR, ARPU, Churn Rate, Conversion Rate
- **Usage Analytics**: Organization and subscription analytics
- **Revenue Tracking**: Comprehensive financial metrics
- **Real-time Monitoring**: Live system health and performance

### 7. Legal Compliance
- **Terms of Service**: Version-controlled legal documents
- **Privacy Policy**: GDPR/CCPA compliance ready
- **User Consent**: Trackable acceptance records
- **Cookie Policy**: Compliance infrastructure

### 8. Monitoring & Health
- **Health Endpoints**: Kubernetes-ready liveness/readiness probes
- **Prometheus Metrics**: Full observability support
- **Audit Trail**: 90-day retention of all actions
- **Error Tracking**: Comprehensive logging

## Architecture Changes

### New Entities
- `Organization` - Tenant/workspace management
- `OrganizationMember` - Team membership
- `SubscriptionPlan` - Plan definitions
- `Subscription` - Active subscriptions
- `AuditLog` - Activity tracking
- `UsageTracking` - Resource usage metrics
- `ApiKey` - Programmatic access
- `FeatureFlag` - Feature rollout control
- `EmailTemplate` - Email templating
- `LegalDocument` - Terms, privacy, etc.
- `UserConsent` - Compliance tracking
- `PaymentRetry` - Dunning management
- `TwoFactorAuth` - 2FA credentials

### Updated Entities
- `User` - Added organization, email verification, 2FA
- All existing entities maintain backward compatibility

## Environment Variables

### Required Variables
```bash
# Database
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# JWT
JWT_SECRET_KEY=your_secure_jwt_secret
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Stripe
STRIPE_API_KEY=sk_live_your_stripe_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret

# Email (SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# Application
APP_URL=https://yourdomain.com
SUPPORT_EMAIL=support@yourdomain.com
EMAIL_FROM=noreply@yourdomain.com
EMAIL_FROM_NAME=BuildBetter

# Optional
EMAIL_ENABLED=true
LOG_LEVEL=INFO
JPA_DDL_AUTO=update
SHOW_SQL=false
```

## Database Setup

### 1. Initial Migration
The application uses JPA with `ddl-auto=update` for automatic schema management.

### 2. Seed Data - Subscription Plans
Run this SQL to create default subscription plans:

```sql
-- Free Plan
INSERT INTO subscription_plans (id, name, display_name, tier, description,
    price_monthly, price_yearly, max_users, max_ads, max_storage_mb,
    max_api_calls_per_hour, trial_days, is_active)
VALUES (
    gen_random_uuid(),
    'free',
    'Free',
    'FREE',
    'Perfect for getting started',
    0.00,
    0.00,
    5,
    10,
    1000,
    100,
    0,
    true
);

-- Starter Plan
INSERT INTO subscription_plans (id, name, display_name, tier, description,
    price_monthly, price_yearly, max_users, max_ads, max_storage_mb,
    max_api_calls_per_hour, custom_branding, api_access, trial_days, is_active)
VALUES (
    gen_random_uuid(),
    'starter',
    'Starter',
    'STARTER',
    'Great for small teams',
    29.00,
    290.00,
    10,
    50,
    10000,
    1000,
    false,
    true,
    14,
    true
);

-- Professional Plan
INSERT INTO subscription_plans (id, name, display_name, tier, description,
    price_monthly, price_yearly, max_users, max_ads, max_storage_mb,
    max_api_calls_per_hour, custom_branding, priority_support, api_access,
    advanced_analytics, trial_days, is_active)
VALUES (
    gen_random_uuid(),
    'professional',
    'Professional',
    'PROFESSIONAL',
    'Ideal for growing businesses',
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
    14,
    true
);

-- Business Plan
INSERT INTO subscription_plans (id, name, display_name, tier, description,
    price_monthly, price_yearly, max_users, max_ads, max_storage_mb,
    max_api_calls_per_hour, custom_branding, priority_support, api_access,
    advanced_analytics, trial_days, is_active)
VALUES (
    gen_random_uuid(),
    'business',
    'Business',
    'BUSINESS',
    'For established companies',
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
    14,
    true
);

-- Enterprise Plan
INSERT INTO subscription_plans (id, name, display_name, tier, description,
    price_monthly, price_yearly, max_users, max_ads, max_storage_mb,
    max_api_calls_per_hour, custom_branding, priority_support, api_access,
    advanced_analytics, white_label, trial_days, is_active)
VALUES (
    gen_random_uuid(),
    'enterprise',
    'Enterprise',
    'ENTERPRISE',
    'Unlimited everything with white-label support',
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
    true
);
```

### 3. Email Templates
Create default email templates:

```sql
-- Welcome Email
INSERT INTO email_templates (id, type, subject, html_content, text_content, is_active)
VALUES (
    gen_random_uuid(),
    'WELCOME',
    'Welcome to {{appName}}!',
    '<h1>Welcome to {{appName}}, {{userName}}!</h1><p>We''re excited to have you on board.</p><p><a href="{{appUrl}}">Get Started</a></p>',
    'Welcome to {{appName}}, {{userName}}! We''re excited to have you on board. Visit {{appUrl}} to get started.',
    true
);

-- Email Verification
INSERT INTO email_templates (id, type, subject, html_content, is_active)
VALUES (
    gen_random_uuid(),
    'EMAIL_VERIFICATION',
    'Verify your email address',
    '<h1>Verify Your Email</h1><p>Click the link below to verify your email address:</p><p><a href="{{verificationUrl}}">Verify Email</a></p>',
    true
);

-- Add more templates as needed...
```

## Deployment Options

### Option 1: Docker Deployment

```dockerfile
# Already configured in existing Dockerfile
docker build -t buildbetter-saas .
docker run -p 8080:8080 --env-file .env buildbetter-saas
```

### Option 2: Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: buildbetter-saas
spec:
  replicas: 3
  selector:
    matchLabels:
      app: buildbetter
  template:
    metadata:
      labels:
        app: buildbetter
    spec:
      containers:
      - name: buildbetter
        image: buildbetter-saas:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: buildbetter-secrets
              key: db-username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: buildbetter-secrets
              key: db-password
        - name: STRIPE_API_KEY
          valueFrom:
            secretKeyRef:
              name: buildbetter-secrets
              key: stripe-api-key
        livenessProbe:
          httpGet:
            path: /api/v1/health/live
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/v1/health/ready
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: buildbetter-service
spec:
  selector:
    app: buildbetter
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
```

### Option 3: Cloud Platform Deployment

#### Render
- Already configured with existing database
- Add environment variables in Render dashboard
- Deploy from GitHub repository

#### AWS Elastic Beanstalk
```bash
eb init -p docker buildbetter-saas
eb create buildbetter-prod
eb setenv DB_USERNAME=xxx DB_PASSWORD=xxx ...
eb deploy
```

## Stripe Setup

### 1. Create Stripe Products
For each subscription plan, create a product in Stripe Dashboard:
1. Go to Products → Add Product
2. Create monthly and yearly prices
3. Copy the Price IDs and update the database:

```sql
UPDATE subscription_plans
SET stripe_price_id_monthly = 'price_xxx',
    stripe_price_id_yearly = 'price_yyy',
    stripe_product_id = 'prod_xxx'
WHERE name = 'starter';
```

### 2. Configure Webhooks
Add webhook endpoint in Stripe Dashboard:
- URL: `https://yourdomain.com/api/v1/payments/webhook`
- Events to send:
  - `customer.subscription.created`
  - `customer.subscription.updated`
  - `customer.subscription.deleted`
  - `invoice.payment_succeeded`
  - `invoice.payment_failed`
  - `payment_intent.succeeded`
  - `payment_intent.payment_failed`

## Monitoring Setup

### Prometheus Integration
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'buildbetter'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['buildbetter:8080']
```

### Grafana Dashboard
Import the BuildBetter SaaS dashboard (ID: buildbetter-saas)

## Post-Deployment Checklist

- [ ] Database migrations completed
- [ ] Subscription plans seeded
- [ ] Email templates created
- [ ] Stripe products configured
- [ ] Stripe webhooks configured
- [ ] Environment variables set
- [ ] Email SMTP configured and tested
- [ ] Health endpoints responding
- [ ] Admin user created
- [ ] SSL certificates installed
- [ ] Monitoring configured
- [ ] Backup strategy implemented
- [ ] Legal documents uploaded

## API Endpoints

### New SaaS Endpoints

#### Organizations
- `POST /api/v1/organizations` - Create organization
- `GET /api/v1/organizations/{id}` - Get organization
- `PUT /api/v1/organizations/{id}` - Update organization
- `POST /api/v1/organizations/{id}/members` - Add member
- `DELETE /api/v1/organizations/{id}/members/{userId}` - Remove member

#### Subscriptions
- `GET /api/v1/subscriptions/plans` - List plans
- `POST /api/v1/subscriptions` - Create subscription
- `GET /api/v1/subscriptions/current` - Get current subscription
- `POST /api/v1/subscriptions/upgrade` - Upgrade plan
- `POST /api/v1/subscriptions/downgrade` - Downgrade plan
- `POST /api/v1/subscriptions/{id}/cancel` - Cancel subscription

#### API Keys
- `POST /api/v1/api-keys` - Create API key
- `GET /api/v1/api-keys` - List API keys
- `DELETE /api/v1/api-keys/{id}` - Revoke API key

#### Admin Dashboard
- `GET /api/v1/saas-admin/dashboard` - Complete SaaS metrics
- `GET /api/v1/saas-admin/revenue/metrics` - Revenue analytics
- `GET /api/v1/saas-admin/subscriptions/analytics` - Subscription analytics

#### Health & Monitoring
- `GET /api/v1/health` - Detailed health status
- `GET /api/v1/health/live` - Liveness probe
- `GET /api/v1/health/ready` - Readiness probe

## Security Best Practices

1. **Always use HTTPS in production**
2. **Rotate JWT secrets regularly**
3. **Use strong database passwords**
4. **Enable 2FA for admin accounts**
5. **Regular security audits via audit logs**
6. **Monitor failed login attempts**
7. **Keep Stripe webhook secrets secure**
8. **Regular backup of database**

## Scaling Considerations

### Horizontal Scaling
- Stateless application design allows multiple instances
- Use load balancer for distribution
- Session data in JWT (no server-side sessions)

### Database Scaling
- Connection pooling configured
- Consider read replicas for analytics
- Regular index optimization

### Caching Strategy
- Implement Redis for rate limiting (production)
- Cache subscription plan data
- Cache organization settings

## Support & Maintenance

### Monitoring
- Check `/api/v1/health` endpoint regularly
- Monitor Prometheus metrics
- Review audit logs weekly

### Backups
- Daily database backups
- File storage backups (invoices, uploads)
- Configuration backups

### Updates
- Follow semantic versioning
- Test in staging environment
- Database backup before updates

## Troubleshooting

### Common Issues

1. **Email not sending**
   - Check MAIL_USERNAME and MAIL_PASSWORD
   - Verify SMTP server settings
   - Check firewall rules for port 587

2. **Stripe webhook failures**
   - Verify webhook secret matches
   - Check endpoint is publicly accessible
   - Review Stripe dashboard logs

3. **Rate limiting issues**
   - Adjust limits in application.yml
   - Consider Redis for distributed rate limiting

4. **Database connection errors**
   - Verify DATABASE_URL
   - Check connection pool settings
   - Ensure database is accessible

## Migration from Existing BuildBetter

Existing data is preserved. New features are additive:
1. Existing users continue to work
2. Manually create organizations for existing users
3. Assign default FREE plan subscriptions
4. Gradually migrate to organization-scoped model

## License & Support

For enterprise support, custom features, or white-label solutions:
- Email: support@buildbetter.com
- Documentation: https://docs.buildbetter.com

---

**Version:** 2.0.0-SAAS
**Last Updated:** 2025-01-07
**Author:** BuildBetter Team
