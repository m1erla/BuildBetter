# BuildBetter - Enterprise SaaS Platform

> **A production-ready, multi-tenant SaaS marketplace platform connecting service providers with clients**

BuildBetter has been transformed into a comprehensive SaaS platform with enterprise-grade features including multi-tenancy, subscription management, advanced analytics, and complete billing infrastructure.

## 🌟 What's New - SaaS Features

### 🏢 Multi-Tenancy & Organizations
- **Workspace Management**: Complete tenant isolation and organization management
- **Team Collaboration**: Role-based access control (Owner, Admin, Member, Viewer)
- **Scalable Architecture**: Support for unlimited organizations

### 💳 Subscription & Billing
- **5 Tier Plans**: FREE, STARTER, PROFESSIONAL, BUSINESS, ENTERPRISE
- **Stripe Integration**: Automated recurring billing with webhooks
- **Trial Management**: 14-day trials with automatic conversion
- **Flexible Billing**: Monthly and annual subscription options
- **Usage Quotas**: Automatic enforcement of plan limits

### 🔒 Enterprise Security
- **Two-Factor Authentication**: Enhanced account security
- **API Key Management**: Programmatic access with scoped permissions
- **Audit Logging**: Complete activity tracking for compliance
- **GDPR Compliance**: Legal document versioning and consent tracking

### 📊 Advanced Analytics
- **SaaS Metrics**: MRR, ARPU, Churn Rate, LTV
- **Usage Tracking**: Real-time resource monitoring
- **Revenue Analytics**: Comprehensive financial dashboards
- **Conversion Funnels**: Trial-to-paid conversion tracking

### ⚡ Rate Limiting & Quotas
- **Per-Organization Limits**: Plan-based API rate limiting
- **Resource Quotas**: Users, ads, storage, API calls
- **Real-time Enforcement**: Automatic quota management

### 📧 Email Notification System
- **Transactional Emails**: Welcome, verification, password reset
- **Billing Notifications**: Payment success/failure, invoices
- **Subscription Alerts**: Trial ending, renewal reminders
- **Template Engine**: Customizable email templates

### 📈 Monitoring & Health
- **Health Endpoints**: Kubernetes-ready probes
- **Prometheus Metrics**: Full observability
- **Structured Logging**: Comprehensive audit trails

## 🛠 Technologies & Tools

### Core Stack
- **Java 17** - Modern JVM features
- **Spring Boot 3.1.5** - Latest Spring framework
- **PostgreSQL** - Production database
- **Stripe** - Payment processing
- **JWT** - Secure authentication
- **WebSocket** - Real-time communication

### SaaS Infrastructure
- **Multi-Tenancy** - Organization-based isolation
- **Rate Limiting** - Request throttling per plan
- **Email Service** - SMTP with templates
- **Audit Logging** - Compliance tracking
- **Feature Flags** - Gradual rollout control

## 🏗 Enhanced Architecture

### N-Tier Architecture with SaaS Extensions
```
┌─────────────────────────────────────┐
│   API Layer (REST + WebSocket)      │
│   - Rate Limiting Interceptor       │
│   - Organization Context            │
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│   Business Layer                     │
│   - Subscription Management         │
│   - Usage Tracking                  │
│   - Audit Service                   │
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│   Data Layer                         │
│   - Multi-tenant Repositories       │
│   - Organization Scoping            │
└─────────────────────────────────────┘
```

## 🚀 Getting Started

### Quick Start (Docker)
```bash
# Clone the repository
git clone https://github.com/m1erla/BuildBetter.git
cd BuildBetter

# Set up environment variables
cp .env.example .env
# Edit .env with your configuration

# Run with Docker
docker-compose up -d

# Initialize subscription plans
psql -d buildbetter -f backend/src/main/resources/db/init-saas.sql
```

### Manual Setup

#### Requirements
- Java 17+
- Maven 3.6+
- PostgreSQL 14+
- Stripe Account
- SMTP Server

#### Installation Steps

1. **Clone and Navigate**
```bash
git clone https://github.com/m1erla/BuildBetter.git
cd BuildBetter/backend
```

2. **Configure Environment**
Create `.env` file:
```env
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
JWT_SECRET_KEY=your_secret_key
STRIPE_API_KEY=sk_test_your_key
STRIPE_WEBHOOK_SECRET=whsec_your_secret
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

3. **Build and Run**
```bash
mvn clean install
mvn spring-boot:run
```

4. **Initialize SaaS Data**
```bash
psql -U postgres -d buildbetter -f src/main/resources/db/init-saas.sql
```

5. **Access Application**
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Health Check: http://localhost:8080/api/v1/health

## 📚 API Documentation

### SaaS Endpoints

#### Organizations
```http
POST   /api/v1/organizations              # Create organization
GET    /api/v1/organizations/{id}          # Get organization
PUT    /api/v1/organizations/{id}          # Update organization
GET    /api/v1/organizations/{id}/members  # List members
POST   /api/v1/organizations/{id}/members  # Add member
DELETE /api/v1/organizations/{id}/members/{userId} # Remove member
```

#### Subscriptions
```http
GET    /api/v1/subscriptions/plans         # List all plans
POST   /api/v1/subscriptions               # Create subscription
GET    /api/v1/subscriptions/current       # Get current subscription
POST   /api/v1/subscriptions/upgrade       # Upgrade plan
POST   /api/v1/subscriptions/downgrade     # Downgrade plan
POST   /api/v1/subscriptions/{id}/cancel   # Cancel subscription
```

#### API Keys
```http
POST   /api/v1/api-keys                    # Create API key
GET    /api/v1/api-keys                    # List API keys
DELETE /api/v1/api-keys/{id}               # Revoke API key
```

#### Admin Dashboard
```http
GET    /api/v1/saas-admin/dashboard        # Complete SaaS metrics
GET    /api/v1/saas-admin/revenue/metrics  # Revenue analytics
GET    /api/v1/saas-admin/subscriptions/analytics # Subscription analytics
GET    /api/v1/saas-admin/usage/summary    # Usage metrics
```

#### Health & Monitoring
```http
GET    /api/v1/health                      # Detailed health status
GET    /api/v1/health/live                 # Liveness probe
GET    /api/v1/health/ready                # Readiness probe
```

### Existing Endpoints (Enhanced)
All existing endpoints remain functional with added organization context and rate limiting.

## 📋 Subscription Plans

| Plan | Price/Month | Users | Ads | Storage | API Calls/Hour | Features |
|------|-------------|-------|-----|---------|----------------|----------|
| **FREE** | $0 | 5 | 10 | 1 GB | 100 | Basic features |
| **STARTER** | $29 | 10 | 50 | 10 GB | 1,000 | API access |
| **PROFESSIONAL** | $99 | 50 | 250 | 50 GB | 5,000 | Custom branding, Priority support |
| **BUSINESS** | $299 | 200 | 1,000 | 200 GB | 20,000 | Advanced analytics |
| **ENTERPRISE** | $999 | Unlimited | Unlimited | Unlimited | 50,000 | White-label, Dedicated support |

## 🔐 Security Features

- ✅ JWT-based authentication
- ✅ Role-based authorization (USER, EXPERT, ADMIN)
- ✅ Two-factor authentication support
- ✅ API key management
- ✅ Audit logging for all actions
- ✅ Password strength enforcement
- ✅ Session management
- ✅ CORS configuration
- ✅ SQL injection prevention
- ✅ XSS protection

## 📊 Metrics & Monitoring

### Key SaaS Metrics
- **MRR** - Monthly Recurring Revenue
- **ARPU** - Average Revenue Per User
- **Churn Rate** - Customer retention
- **Trial Conversion** - Free to paid conversion
- **LTV** - Customer Lifetime Value

### Monitoring Endpoints
- Prometheus metrics: `/actuator/prometheus`
- Health check: `/api/v1/health`
- Application info: `/actuator/info`

## 🚢 Deployment

### Docker Deployment
```bash
docker build -t buildbetter-saas:latest .
docker run -p 8080:8080 --env-file .env buildbetter-saas:latest
```

### Kubernetes Deployment
See `SAAS_DEPLOYMENT_GUIDE.md` for complete Kubernetes manifests and configuration.

### Environment Variables
Complete list of environment variables in `SAAS_DEPLOYMENT_GUIDE.md`.

## 📖 Documentation

- **[SaaS Deployment Guide](SAAS_DEPLOYMENT_GUIDE.md)** - Complete deployment instructions
- **[API Documentation](http://localhost:8080/swagger-ui/index.html)** - Interactive API docs
- **[Initialization SQL](backend/src/main/resources/db/init-saas.sql)** - Database setup

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Integration tests
mvn verify
```

## 📈 Roadmap

- [x] Multi-tenancy architecture
- [x] Subscription management
- [x] Rate limiting
- [x] Email notifications
- [x] Audit logging
- [x] API key management
- [x] Feature flags
- [x] Health monitoring
- [ ] Analytics dashboard UI
- [ ] Mobile app support
- [ ] Advanced reporting
- [ ] AI-powered matching

## 🤝 Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: Add SaaS feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## 💼 Enterprise Support

For enterprise features, custom development, or white-label solutions:
- Email: support@buildbetter.com
- Website: https://buildbetter.com

## 👥 Authors

**Furkan Karakus** - [GitHub](https://github.com/m1erla)

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Stripe for payment infrastructure
- All contributors to this project

---

**Version:** 2.0.0-SAAS | **Status:** Production Ready ✅ | **Last Updated:** January 2025
