# EcoData Hub

EcoData Hub is a Spring Boot web application that integrates economic indicator data from the GUS API with economic news from News API. The UI is rendered with Thymeleaf, data is stored in MariaDB, and access to the application is protected with JWT authentication.

## Features

- browse GUS subjects and indicators,
- load variable data from GUS with visible job progress,
- export indicator, variable data, and news views to JSON,
- browse economic news from News API,
- use protected REST CRUD endpoints for stored GUS and news data,
- inspect protected API documentation through Swagger UI,
- track backend requests with `X-Request-Id` and correlated log entries.

## Requirements

- Docker,
- Docker Compose,
- Java 17 only when running the backend locally without containers.

## Running With Docker Compose

Start the backend and MariaDB containers from the project root:

```bash
docker compose up --build
```

Open the application:

```text
http://localhost:8080/
```

The Compose setup exposes only the backend on host port `8080`. The MariaDB container is available only inside the Docker network as `db:3306`, so it does not conflict with a local MariaDB installation on host port `3306`.

Stop containers:

```bash
docker compose down
```

Recreate the database from scratch and rerun the GUS init import:

```bash
docker compose down -v
docker compose up --build
```

## Running With Docker CLI

Build the backend image:

```bash
docker build -t ecodata-hub .
```

Create a shared Docker network:

```bash
docker network create ecodata-network
```

Start MariaDB:

```bash
docker run -d \
  --name ecodata-db \
  --network ecodata-network \
  -e MARIADB_DATABASE=economy_db \
  -e MARIADB_USER=eco_user \
  -e MARIADB_PASSWORD=eco_pass \
  -e MARIADB_ROOT_PASSWORD=root \
  -v ecodata-mariadb-data:/var/lib/mysql \
  -v "$(pwd)/docker/mariadb/init:/docker-entrypoint-initdb.d:ro" \
  mariadb:11.4
```

If host access to this database is needed, publish it on a free host port, for example `3307`:

```bash
-p 3307:3306
```

Start the backend:

```bash
docker run -d \
  --name ecodata-backend \
  --network ecodata-network \
  -e SPRING_DATASOURCE_URL=jdbc:mariadb://ecodata-db:3306/economy_db \
  -e SPRING_DATASOURCE_USERNAME=eco_user \
  -e SPRING_DATASOURCE_PASSWORD=eco_pass \
  -e APP_ADMIN_USERNAME=admin \
  -e APP_ADMIN_PASSWORD=admin \
  -e APP_JWT_SECRET=change-this-secret \
  -e NEWS_API_KEY=your_news_api_key_here \
  -p 8080:8080 \
  ecodata-hub
```

## Authentication

- user registration: `http://localhost:8080/signup`,
- login: `http://localhost:8080/login`,
- logout: `http://localhost:8080/logout`,
- Swagger UI after login: `http://localhost:8080/swagger-ui.html`,
- all pages, API endpoints, and Swagger documentation require login, except `/login`, `/signup`, static CSS/JS files, `/error`, and `POST /api/auth/login`,
- the admin account is seeded on startup from `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD`,
- browser login stores JWT in the HttpOnly `AUTH_TOKEN` cookie,
- API clients can send `Authorization: Bearer <token>`,
- API token can be obtained with `POST /api/auth/login`.

Example API login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -d "username=admin" \
  -d "password=admin"
```

Read-only API endpoints require role `USER` or `ADMIN`. Mutating `/api/**` endpoints require role `ADMIN`.

## GUS Initial Data

Importing the GUS subject tree by calling the GUS API is slow because requests are rate-limited. The project therefore includes an initial SQL data pack:

```text
docker/mariadb/init/01-gus-initial-data.sql
```

MariaDB automatically imports files mounted into `/docker-entrypoint-initdb.d` only when the database volume is empty. The included pack contains only GUS tables:

- `gus_data_attributes`,
- `gus_subjects`,
- `gus_subject_children`,
- `gus_subject_levels`,
- `gus_subject_import_states`.

It does not import news data or old test tables.

To refresh the SQL pack from an existing Docker database:

```bash
mkdir -p docker/mariadb/init
docker exec ecodata-db mariadb-dump \
  -uroot \
  -proot \
  economy_db \
  gus_data_attributes \
  gus_subjects \
  gus_subject_children \
  gus_subject_levels \
  gus_subject_import_states \
  > docker/mariadb/init/01-gus-initial-data.sql
```

## Request Tracking And Long GUS Jobs

Every backend request receives an `X-Request-Id` response header. The same ID is written to logs through MDC, which helps trace slow GUS, News API, export, and page requests.

Follow backend logs in Compose:

```bash
docker compose logs -f backend
```

Variable data loading runs as a background job. The page polls:

```text
/api/gus/variable-data-jobs/{jobId}
```

The progress bar is based on fetched GUS pages compared with the expected total. The variable data JSON export uses the completed in-memory job result and returns `409 Conflict` if export is requested before the job finishes.

The GUS request delay is configured in `application.properties`:

```properties
gus.request-delay=10s
```

## Project Structure

```text
.
|-- Dockerfile
|-- docker-compose.yaml
|-- docker/
|   |-- mariadb/
|       |-- init/             # SQL files imported by MariaDB on first empty-volume startup
|-- build.gradle
|-- settings.gradle
|-- src/
|   |-- main/
|   |   |-- java/com/ecodatahub/
|   |   |   |-- auth/         # login, signup, JWT, user account persistence
|   |   |   |-- config/       # Spring Security, OpenAPI, HTTP client, request logging
|   |   |   |-- controller/   # MVC pages and REST endpoints
|   |   |   |-- gus/          # GUS API client, domain, repositories, services, web DTOs
|   |   |   |-- news/         # News API client, domain, repositories, services, web DTOs
|   |   |   |-- EcoDataHubApplication.java
|   |   |-- resources/
|   |       |-- application.properties
|   |       |-- static/
|   |       |   |-- css/
|   |       |   |-- js/        # loaders, charts, GUS job progress polling
|   |       |-- templates/    # Thymeleaf pages, including auth views
|   |-- test/                # automated tests
```

## Configuration

Important environment variables:

- `SPRING_DATASOURCE_URL` - MariaDB JDBC URL,
- `SPRING_DATASOURCE_USERNAME` - database user,
- `SPRING_DATASOURCE_PASSWORD` - database password,
- `NEWS_API_KEY` - News API key,
- `APP_ADMIN_USERNAME` - seeded admin username,
- `APP_ADMIN_PASSWORD` - seeded admin password,
- `APP_JWT_SECRET` - JWT signing secret.
