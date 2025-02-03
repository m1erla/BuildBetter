# BuildBetter

BuildBetter is a Spring Boot based web application. It's a backend API project that includes core features such as user management, advertisement management, and service management.

## 🛠 Technologies & Tools

- Java 17
- Spring Framework
- Spring Boot 3.1.5
- Spring MVC
- Spring Data JPA
- Spring Security
- Spring REST
- JWT Authentication
- WebSocket
- Swagger UI
- Model Mapper
- Maven
- PostgreSQL

## 🏗 System Architecture

### N-Tier Architecture

The project follows N-Tier Architecture pattern with the following layers:

- Presentation Layer (Controllers)
- Business Layer (Business Logic)
- Service Layer (Services)
- Data Access Layer (Repositories)
- Core/Common Layer (Utilities, Exceptions)
- Security Layer (JWT, Authentication)

### Security

- JWT based authentication
- Login with email + password
- Registration system
- Role based authorization

### API Endpoints

#### Authentication

- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/authenticate` - User login

#### User Operations

- `GET /api/v1/users` - List all users
- `GET /api/v1/users/{id}` - User details
- `DELETE /api/v1/users/{id}` - Delete user

#### Advertisement Operations

- `POST /api/v1/ads/ad` - Create new advertisement
- `GET /api/v1/ads` - List advertisements
- `GET /api/v1/ads/ad/{id}` - Advertisement details
- `DELETE /api/v1/ads/ad/{id}` - Delete advertisement

#### Category Operations

- `POST /api/v1/categories/category` - Create new category
- `GET /api/v1/categories` - List categories
- `GET /api/v1/categories/{id}` - Category details
- `DELETE /api/v1/categories/{id}` - Delete category

#### Service Operations

- `POST /api/v1/services/service` - Create new service
- `GET /api/v1/services` - List services
- `GET /api/v1/services/{id}` - Service details
- `DELETE /api/v1/services/{id}` - Delete service

#### Job Titles

- `POST /api/v1/job_titles` - Create new job title
- `GET /api/v1/job_titles` - List job titles
- `GET /api/v1/job_titles/{id}` - Job title details
- `DELETE /api/v1/job_titles/{id}` - Delete job title

## 🚀 Getting Started

### Requirements

- Java 17
- Maven
- PostgreSQL

### Installation

1. Clone the project

```
git clone https://github.com/m1erla/BuildBetter.git
```

2. Configure database settings

- Create `application.properties` or `application.yml` file
- Add database connection details

3. Build and run the project

```
mvn clean install
mvn spring-boot:run
```

4. Access API documentation

```
http://localhost:8080/swagger-ui/index.html
```

## 📝 Features

- CRUD operations
- JWT based authentication
- Real-time communication with WebSocket support
- API documentation with Swagger
- Object transformations with Model Mapper
- Exception handling
- Unit tests

## 🧪 Testing

To test the project:

```
mvn test
```

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Contact

Furkan Karakus - [GitHub](https://github.com/m1erla)

Project Link: [https://github.com/m1erla/BuildBetter](https://github.com/m1erla/BuildBetter)