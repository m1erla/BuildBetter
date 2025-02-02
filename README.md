# Build Better

# Structures
- Java 17
- Spring Boot 3
- Docker
- MySQL
- CRUD structures

---

## System Login
> Register
> Login (email+password)
> Authorization(Jwt)

---

## Spring boot Steps
> Spring Framework
> Spring Boot
> Spring MVC
> Spring Data (Jpa Hibernate)
> Spring Rest ( RestFull ==>Jersey)
> Spring Security

---

### Reference Documentation
* [GitHub](https://github.com/m1erla/MyKlus)

* [Swagger](http://localhost:8080/swagger-ui/index.html#/)

---

### Project Steps
1. @Bean (ModelMapper)
2. CRUD structure
3. Model Mapper Services
4. JWT Security
5. JWT Token
6. UserEntity (@Entity)
7. UserRepository (@Repository)
8. UserServices (interface)
9. UserServiceImpl(@Service)
10. UserBusinessRules
11. ResourceNotFoundException(@ResponseStatus)
12. WebApiControllers(@RestController)
13. Web Socket
---

### Unit Test
1. TestCrud (interface)
2. @SpringBootTest

---


---


## Api Deployment
```sh


######POSTMAN###########


//POST 
http://localhost:8080/api/v1/auth/authenticate
http://localhost:8080/api/v1/auth/register
http://localhost:8080/api/v1/ads/ad
http://localhost:8080/api/v1/job_titles
http://localhost:8080/api/v1/categories/category
http://localhost:8080/api/v1/services/service


//GET
http://localhost:8080/api/v1/users
http://localhost:8080/api/v1/ads
http://localhost:8080/api/v1/job_titles
http://localhost:8080/api/v1/categories
http://localhost:8080/api/v1/services


//FIND
http://localhost:8080/api/v1/users/1
http://localhost:8080/api/v1/ads/ad/1
http://localhost:8080/api/v1/job_titles/1
http://localhost:8080/api/v1/categories/1
http://localhost:8080/api/v1/services/1



//DELETE
http://localhost:8080/api/v1/users/1
http://localhost:8080/api/v1/ads/ad/1
http://localhost:8080/api/v1/categories/1
http://localhost:8080/api/v1/services/1
http://localhost:8080/api/v1/job_titles/1


```

