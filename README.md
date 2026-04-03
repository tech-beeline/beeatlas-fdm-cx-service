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

В Swagger настроена схема **Bearer** для заголовка `Authorization` — это нужно для удобства в UI (кнопка **Authorize**). В самом сервисе отдельной проверки JWT/OAuth по этому заголовку нет: доступ к бизнес-API завязан на пользовательские заголовки ниже.

Если Swagger не открывается:

- проверьте, что сервис в статусе `UP` (`/actuator/health`);
- убедитесь, что порт `CX_SERVICE_PORT` проброшен и не занят;
- откройте логи контейнера: `podman logs cx-service`.

## Заголовки запросов

Глобально подключён `HeaderInterceptor` (`ru.beeline.cxbackend.controller.HeaderInterceptor`, регистрация в `MyWebMvcConfigurer`). Он выполняется **до** вызова методов контроллеров и для обрабатываемого запроса обязан успешно прочитать пользовательские заголовки — иначе выбрасывается `ForbiddenException` (**403**).

**Итог для API:** почти ко **всем методам контроллеров** (например `BIController`, `CJController`, `CjStepController`, `BIReferenceController`, `BICJStepController`, `ApplicationController` и т.д.) запросы нужно отправлять **с четырьмя заголовками**, если путь запроса **не** попадает под исключения ниже.

В Swagger на части методов стоит аннотация `@CustomHeaders` — она перечисляет те же заголовки для UI. **Требование задаётся не аннотацией, а интерцептором:** методы без `@CustomHeaders` при том же правиле пути тоже должны получать заголовки.

Имена заголовков заданы в `ru.beeline.cxbackend.utils.Constant`:

| Заголовок | Назначение | Формат |
|-----------|------------|--------|
| `user-id` | Идентификатор пользователя | Строка с числом (далее парсится в `Long`) |
| `user-permission` | Права пользователя | Список через **запятую** (допускаются символы `[` `]` `"`, они при разборе убираются) |
| `user-products-ids` | Идентификаторы продуктов | То же: список через запятую |
| `user-roles` | Роли пользователя | То же: список через запятую |

Значения сохраняются в `RequestContext` и дальше используются в сервисах и клиентах (например `DocumentClient`, `ProductClient`).

**Исключения — заголовки не проверяются**, если в `request.getRequestURI()` есть подстрока (логика в `HeaderInterceptor`):

- `/actuator/prometheus`
- `/swagger`
- `/error`
- `/v2/product/cj` (например, попадают `GET .../api/cx/v2/product/cj` и связанные пути с этой подстрокой)
- `/api-docs`
- `/api/v1/cj/` (например, `PATCH .../api/v1/cj/{id}`)

Все остальные пути, в том числе к методам контроллеров под `/api/cx/...`, без этих заголовков приведут к **403** при отсутствии или некорректном значении заголовков.

Пример запроса с заголовками:

```bash
curl -s -H "user-id: 12345" \
  -H "user-permission: READ,WRITE" \
  -H "user-products-ids: 1,2,3" \
  -H "user-roles: ROLE_USER" \
  "http://localhost:8081/<ваш-путь>"
```

### Заголовки ответа (трассировка)

`TraceIdResponseFilter` при валидном текущем span добавляет в ответ заголовок **`traceparent`** (W3C Trace Context / OpenTelemetry), чтобы связать ответ с трассировкой.

## Структура

- `src/` — исходный код сервиса
- `pom.xml` — зависимости и конфигурация сборки Maven
- `Dockerfile` — multi-stage сборка образа сервиса
- `docker-compose.yml` — локальный запуск сервиса и PostgreSQL
