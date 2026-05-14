# ChronoScope Backend

ChronoScope Backend is the Spring Boot API for ChronoScope, a browser-based task planning system. The backend manages identities, tasks, dependencies, work slots and organization administration, and exposes an endpoint that generates optimized work plans from open tasks and available time windows.

The project is part of the DHBW Software Engineering team project. Besides the implementation itself, the repository is set up to support iterative development, API documentation, automated tests and coverage reporting.

## Tech Stack

- Java 25
- Spring Boot 4.0.4
- Spring Web MVC, Spring Security, OAuth2 Resource Server
- Spring Data JPA and JDBC
- H2 for local/test usage, MySQL/MariaDB for production-like deployments
- Keycloak for authentication, organization membership and admin operations
- MapStruct and Lombok
- springdoc-openapi for OpenAPI generation and Swagger UI
- JUnit, Spring test starters, Failsafe, Surefire and JaCoCo

## Repository Structure

```text
.
|-- openapi/                    # Generated OpenAPI contract
|-- src/main/java/de/ni0/chronoscope
|   |-- algorithm/              # Planning algorithm and data providers
|   |-- config/                 # Security, CORS, request context, OpenAPI config
|   |-- controller/             # REST controllers under /v1
|   |-- exception/              # API exception and ProblemDetail handling
|   |-- mapper/                 # MapStruct mappers and proxy providers
|   |-- model/                  # JPA domain model
|   |-- repository/             # Spring Data repositories
|   `-- service/                # Application services
|-- src/main/resources/         # Spring profile configuration
|-- src/test/java/              # Unit, controller and integration tests
|-- Dockerfile
|-- pom.xml
`-- README.md
```

## Main Features

- JWT-protected REST API under `/v1`
- Task CRUD for static and dynamic tasks
- Dynamic task dependencies and planned scopes
- Work slot management for available planning windows
- Plan generation via the scheduling algorithm
- Identity lookup and account linking
- Organization member and invitation administration through Keycloak
- RFC-style API errors using Spring `ProblemDetail`
- Generated OpenAPI contract in `openapi/openapi.yaml`

## API Areas

| Area | Base path | Purpose |
| --- | --- | --- |
| Identity | `/v1/identity` | Current identity, linked accounts and account linking |
| Tasks | `/v1/tasks` | Static/dynamic task CRUD and dependencies |
| Work slots | `/v1/workslots` | Available time windows for planning |
| Planning | `/v1/plan` | Generate and persist planned task scopes |
| Scopes | `/v1/scopes` | Read generated scopes for dynamic tasks |
| Organizations | `/v1/organizations` | Members and invitations for organization admins |

See `openapi/openapi.yaml` for the detailed request and response contract.

## Requirements

- JDK 25
- PowerShell 7 on Windows, or another shell capable of running the Maven wrapper
- Docker, optional for container builds
- MySQL or MariaDB for production-like runs
- Keycloak credentials for production-like authentication flows

The Maven wrapper is committed, so a separate Maven installation is not required.

## Configuration Profiles

The application uses Spring profiles:

- `dev`: local development profile with H2, debug logging, permissive CORS and a static development bearer token.
- `prod`: production profile with MySQL, Keycloak configuration from environment variables and restricted CORS.
- `generate`: profile used by the OpenAPI generation process.

The Maven build configures the Spring Boot Maven plugin with `dev` as the default run profile. The base Spring configuration declares `prod` as the default application profile when no Maven/plugin profile overrides it.

### Development Auth

With the `dev` profile, the backend accepts the configured local bearer token:

```text
Authorization: Bearer local-test-user
```

The token is decoded by the local development JWT decoder and produces a test identity with access to the `private` and `chronoscope-local` organizations.

### Important Environment Variables

Production-like runs use these variables:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | JDBC URL, defaults to `jdbc:mysql://host.docker.internal:3306/chronoscope` |
| `DB_USERNAME` | Database user, defaults to `chronoscope` |
| `DB_PASSWORD` | Database password |
| `KEYCLOAK_URL` | Keycloak base URL |
| `KEYCLOAK_REALM` | Keycloak realm |
| `KEYCLOAK_CLIENT_ID` | Keycloak admin client ID |
| `KEYCLOAK_CLIENT_SECRET` | Keycloak admin client secret |
| `SMTP_HOST` | SMTP host, defaults to `127.0.0.1` |
| `SMTP_PORT` | SMTP port, defaults to `25` |

## Getting Started

From the repository root:

```powershell
.\mvnw.cmd clean compile
```

Run the application with the development profile:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

By default, Spring Boot serves the application on port `8080` unless `server.port` is supplied externally.

Example request against the development profile:

```powershell
Invoke-RestMethod `
	-Uri "http://localhost:8080/v1/identity" `
	-Headers @{ Authorization = "Bearer local-test-user" }
```

## Testing

Run unit tests:

```powershell
.\mvnw.cmd test
```

Run the full verification lifecycle, including integration tests and coverage checks:

```powershell
.\mvnw.cmd verify
```

The build separates test types as follows:

- Surefire runs unit and slice tests, excluding `*IT.java`.
- Failsafe runs integration tests named `*IT.java` during `integration-test` and `verify`.
- JaCoCo merges unit and integration coverage into `target/jacoco-merged.exec`.
- HTML coverage reports are generated under `target/site/jacoco`, `target/site/jacoco-algorithm` and `target/site/jacoco-remainder`.

Coverage gates enforced during `verify`:

| Scope | Branch coverage |
| --- | --- |
| `de.ni0.chronoscope.algorithm` | 90% |
| Remaining production classes | 70% |

Generated mapper implementations, dev-only infrastructure and the Spring Boot entry point are excluded from coverage checks.

## OpenAPI

The OpenAPI contract is generated into `openapi/openapi.yaml` during the Maven integration-test phase. The generation profile starts the application locally, reads `/v3/api-docs.yaml`, writes the file and then stops the application again.

Generate or refresh the API contract:

```powershell
.\mvnw.cmd verify
```

If you only want to compile or run tests without generating OpenAPI, skip springdoc:

```powershell
.\mvnw.cmd test "-Dspringdoc.skip=true"
```

When the application is started with OpenAPI enabled, Swagger UI is available at:

```text
/swagger-ui/index.html
```

## Docker

Build the container image:

```powershell
docker build -t chronoscope-backend .
```

Run the image with the production profile and required environment variables:

```powershell
docker run --rm -p 9020:9020 `
	-e DB_PASSWORD="change-me" `
	-e KEYCLOAK_URL="https://auth.example.test" `
	-e KEYCLOAK_REALM="ni0" `
	-e KEYCLOAK_CLIENT_ID="chronoscope-admin" `
	-e KEYCLOAK_CLIENT_SECRET="change-me" `
	chronoscope-backend
```

The Dockerfile exposes port `9020`. If the application should listen on that port inside the container, also provide `SERVER_PORT=9020` or set `server.port` through another Spring Boot configuration source.

## Development Notes

- Controllers are mapped under `/v1` and documented with springdoc annotations.
- Task DTOs use polymorphic request/response types with a `type` discriminator for `static` and `dynamic` tasks.
- API errors are returned as Spring `ProblemDetail` responses with an additional `errorCode` extension.
- `ApiNotImplementedException` maps planned-but-unimplemented contract endpoints to HTTP `501`.
- MapStruct generated implementations are written to `target/generated-sources/annotations`.

## Useful Commands

```powershell
# Compile without running tests
.\mvnw.cmd compile

# Run unit tests only
.\mvnw.cmd test

# Run full verification with integration tests, OpenAPI generation and coverage gates
.\mvnw.cmd verify

# Skip OpenAPI generation for faster local feedback
.\mvnw.cmd test "-Dspringdoc.skip=true"

# Skip coverage checks when needed for local investigation
.\mvnw.cmd verify "-Djacoco.skip=true"
```
