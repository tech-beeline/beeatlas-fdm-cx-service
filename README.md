# CX Service (`cx-backend`)

Сервис клиентского опыта (CX) на Spring Boot с PostgreSQL.

## Требования

- Java 17 (для локального запуска без контейнера)
- Maven 3.8+
- Podman + podman-compose или Docker + docker compose

## Быстрый старт (через compose)

```bash
podman compose up --build
```

После старта:

- API: `http://localhost:8081`
- Health: `http://localhost:8081/actuator/health`
- Swagger UI: `http://localhost:8081/swagger-ui/`
- OpenAPI JSON: `http://localhost:8081/v2/api-docs`

Остановка:

```bash
podman compose down
```

## Переменные окружения

Поддерживаются значения по умолчанию из `docker-compose.yml`:

- `CX_POSTGRES_DB` (default: `cx_service`)
- `CX_POSTGRES_USER` (default: `postgres`)
- `CX_POSTGRES_PASSWORD` (default: `postgres`)
- `CX_SERVICE_POSTGRES_NODEPORT` (default: `5432`)
- `CX_SERVICE_PORT` (default: `8081`)
- `CX_POSTGRES_HOST` (default: `cx-service-postgres`)

Пример запуска с переопределением:

```bash
CX_SERVICE_PORT=8082 CX_POSTGRES_PASSWORD=secret podman compose up --build
```

## Локальная сборка и запуск без контейнера

Сборка:

```bash
mvn clean package -DskipTests
```

Запуск:

```bash
java -jar target/cx-backend-*.jar
```

## Swagger

В сервисе подключен `springfox` (`springfox-boot-starter`), поэтому документация доступна после запуска приложения.

- Swagger UI: `http://localhost:${CX_SERVICE_PORT:-8081}/swagger-ui/`
- OpenAPI JSON: `http://localhost:${CX_SERVICE_PORT:-8081}/v2/api-docs`

Если Swagger не открывается:

- проверьте, что сервис в статусе `UP` (`/actuator/health`);
- убедитесь, что порт `CX_SERVICE_PORT` проброшен и не занят;
- откройте логи контейнера: `podman logs cx-service`.

## Частые проблемы

### 1) `no such file or directory` на шаге `COPY ... cx-backend-*.jar`

Проверьте, что в `pom.xml` задан корректный `finalName`:

```xml
<finalName>cx-backend-${project.version}</finalName>
```

### 2) `container name is already in use`

Удалите конфликтующие контейнеры:

```bash
podman rm -f cx-service-postgres cx-service
```

И перезапустите:

```bash
podman compose up --build
```

### 3) `requested access to the resource is denied` для `cx-service:latest`

Это следствие неуспешной локальной сборки. Сначала устраните ошибку сборки образа, затем повторите `podman compose up --build`.

## Структура

- `src/` — исходный код сервиса
- `pom.xml` — зависимости и конфигурация сборки Maven
- `Dockerfile` — multi-stage сборка образа сервиса
- `docker-compose.yml` — локальный запуск сервиса и PostgreSQL
