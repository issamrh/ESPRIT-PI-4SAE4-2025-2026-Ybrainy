# YBrainy Backend - Spring Boot

This is the Spring Boot backend for the YBrainy E-Learning & Certifications Platform.

## Features
- **Spring Boot 3.2.2**
- **Spring Security** configured for development (assets permitted)
- **Spring Data JPA** with H2 (Memory) and MySQL support
- **Backoffice Template** integrated in `src/main/resources/static`

## How to Run
1. Ensure you have Java 17 installed.
2. Open the project in your favorite IDE (IntelliJ IDEA is recommended).
3. The project uses Maven. If you have Maven installed, run:
   ```bash
   mvn spring-boot:run
   ```
4. Access the backoffice at: `http://localhost:8080/index.html` (or just `http://localhost:8080/`)

## Credentials (Development)
- **Username:** `admin`
- **Password:** `admin`

## Project Structure
- `src/main/java`: Backend logic, controllers, services, and security config.
- `src/main/resources/static`: Static backoffice template files.
- `docs`: Functional requirements and documentation.
