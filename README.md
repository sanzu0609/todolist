# Todolist (P0 Bootstrap)

Spring Boot 3.x Todo list skeleton prepared for layered architecture, Thymeleaf UI, and H2 data store.

## Requirements
- Java 21
- Maven 3.9+

## Run (dev profile)
```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```
The app serves on `http://localhost:8080`.

- Home page: `http://localhost:8080/`
- H2 console: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:file:./data/devdb`
  - Username: `sa`
  - Password: *(blank)*

Stop with `Ctrl+C`.

## Run tests
```powershell
mvn test
```
Ensures `contextLoads()` passes and verifies the project builds cleanly.

## Project layout (key folders)
- `src/main/java/org/example/todolist/` – layered package skeleton (config, domain, service, web, etc.)
- `src/main/resources/templates/` – Thymeleaf layout, fragments, and pages
- `src/main/resources/static/` – CSS/JS assets
- `src/main/resources/application-*.properties` – base, dev, and test configurations

## Next steps
- Implement auth (P1), domain persistence (P2), and remaining roadmap items per spec.
