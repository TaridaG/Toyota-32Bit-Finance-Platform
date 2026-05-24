# Read Me First
The following was discovered as part of building this project:

* The original package name 'com.company.finance-api' is invalid and this project uses 'com.company.finance_api' instead.

# REST API versioning

| | |
|--|--|
| **Canonical (use this)** | `/api/v1/**` via api-gateway |
| **Legacy (deprecated)** | `/api/**` — responses include `Deprecation: true` and `Link` successor |

The gateway rewrites `/api/v1/...` to downstream microservices as `/api/...` (no controller changes required). The React app prefixes all API calls to `/api/v1` automatically.

Example: `GET /api/v1/portfolio/overview` → finance-api `GET /api/portfolio/overview`.

# API documentation (OpenAPI / Swagger)

All HTTP microservices expose **OpenAPI 3** at `/v3/api-docs` and **Swagger UI** at `/swagger-ui.html` via [springdoc-openapi](https://springdoc.org/).

## Unified UI (recommended)

With **api-gateway** running, open the aggregated Swagger UI (dropdown lists every service):

| Environment | Swagger UI | Example OpenAPI JSON |
|-------------|------------|----------------------|
| Local dev (`application-dev`) | http://localhost:9090/swagger-ui/index.html | http://localhost:9090/services/finance/v3/api-docs |
| Docker Compose | http://localhost:8080/swagger-ui/index.html | http://localhost:8080/services/finance/v3/api-docs |

## Per-service (direct)

| Service | Local dev port | Swagger UI |
|---------|----------------|------------|
| finance-api | 8080 | http://localhost:8080/swagger-ui.html |
| market-data-service | 8082 | http://localhost:8082/swagger-ui.html |
| news-service | 8083 (gateway dev) / 8082 (service default) | same pattern |
| reporting-service | 8084 | http://localhost:8084/swagger-ui.html |
| analytics-service | 8085 | http://localhost:8085/swagger-ui.html |
| notification-service | 8086 | http://localhost:8086/swagger-ui.html |
| log-consumer-service | 8087 | http://localhost:8087/swagger-ui.html |

## How to verify

1. Start infrastructure and services (`Docker/docker-compose.yml` or run modules from IDE).
2. Open gateway Swagger UI (table above).
3. Top-right **dropdown** — switch between `finance-api`, `market-data-service`, `news-service`, etc. Each should load paths without errors.
4. Quick HTTP checks (gateway on port 9090 locally):

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:9090/services/finance/v3/api-docs
curl -s -o /dev/null -w "%{http_code}" http://localhost:9090/services/market/v3/api-docs
curl -s -o /dev/null -w "%{http_code}" http://localhost:9090/services/news/v3/api-docs
```

Expected status: **200** for each when the target service is up.

5. Protected endpoints: in Swagger UI click **Authorize**, paste a Keycloak access token (`Bearer` prefix optional), then try a secured route.

# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.10-SNAPSHOT/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.5.10-SNAPSHOT/maven-plugin/build-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.5.10-SNAPSHOT/reference/web/servlet.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/3.5.10-SNAPSHOT/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/3.5.10-SNAPSHOT/reference/actuator/index.html)
* [Spring Security](https://docs.spring.io/spring-boot/3.5.10-SNAPSHOT/reference/web/spring-security.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

