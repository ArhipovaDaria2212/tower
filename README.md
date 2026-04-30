# Tower Game Backend

Backend для 2D roguelike игры с fog of war системой.

## Технологический стек

- **Java 21**
- **Spring Boot 4.0.3**
- **Spring Security** с JWT аутентификацией
- **Spring WebSocket / STOMP**
- **PostgreSQL**
- **JPA/Hibernate**
- **Flyway** (миграции)
- **OpenAPI/Swagger**

## Структура проекта

```
src/main/java/ru/arkhipova/
├── config/          # Конфигурации (Security, CORS, OpenAPI, WebSocket)
├── controller/      # REST и STOMP контроллеры
├── model/dto        # Data Transfer Objects
├── model/entity     # Сущности JPA
├── model/request    # Запросы
├── model/response   # Ответы
├── exception/       # Обработка исключений
├── repository/      # Spring Data репозитории
├── security/        # JWT безопасность
└── service/         # Бизнес-логика
```

## API

### REST: Аутентификация
- `POST /auth/register` — Регистрация пользователя
- `POST /auth/login` — Вход

### REST: Прохождение
- `POST /playthrough` — Создать прохождение
- `GET /playthrough/active` — Получить активное прохождение (404 если нет)

### REST: Этажи
- `GET /playthrough/active/floor` — Получить текущий этаж
- `POST /playthrough/active/floor/advance` — Перейти на следующий этаж

### REST: Fog of War (низкочастотные операции — только snapshot)
- `GET /floors/{floorId}/fog` — Снимок всего тумана этажа (вызывается один раз при загрузке этажа)

### WebSocket / STOMP (высокочастотные операции)
Endpoint: `ws://<host>:<port>/ws` (SockJS, JWT в STOMP-заголовке `Authorization: Bearer <token>`).

Owner-фильтрация выполняется на стороне сервера через `Floor → Playthrough → User`:
любая попытка отправить или получить туман для чужого `floorId` отдаёт 403.

#### Клиент → сервер (`SEND`)
- `/app/playthrough/position` — обновление координат игрока на каждое движение
- `/app/floors/{floorId}/fog` — пакет изменённых чанков тумана от клиента
- `/app/floors/{floorId}/fog/reveal` — открыть область вокруг игрока (`{playerX, playerY, radius}`)

#### Сервер → клиент (`SUBSCRIBE`)
- `/topic/floors/{floorId}/fog` — обновлённые чанки тумана после `update`/`reveal`
  (бродкастятся всем подписчикам этажа сразу после сохранения)

> Раньше эти операции шли через `PUT /playthrough/position`, `POST /floors/{floorId}/fog`,
> `POST /floors/{floorId}/fog/reveal` и `GET /floors/{floorId}/fog/bounds` — то есть
> по несколько HTTP-запросов на каждое нажатие клавиши. Теперь это идёт поверх
> единственного long-lived STOMP-соединения, а клиент получает обновления через подписку
> на `/topic/floors/{id}/fog`, без поллинга `bounds` после каждого `reveal`.

## Запуск

### Локально

1. Запустить PostgreSQL:
```bash
docker run -d -p 5432:5432 -e POSTGRES_DB=tower -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres postgres:16
```

2. Запустить приложение через IntelliJ IDEA — конфигурация `TowerApplication`
   уже настроена в `.run/TowerApplication.run.xml` со всеми переменными окружения
   (DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, JWT_EXPIRATION_MS, SERVER_PORT,
   CORS_ALLOWED_ORIGINS, LOGGING_LEVEL).

   Либо из CLI с теми же env:
```bash
./gradlew bootRun
```

### Docker Compose

```bash
./gradlew jibDockerBuild
docker-compose up
```

## Swagger UI

После запуска: http://localhost:8080/swagger-ui.html

## Конфигурация

Все параметры задаются через переменные окружения. В IDE источник истины — файл
`.run/TowerApplication.run.xml`. В `application.yaml` нет хардкодов — только `${...}`.

Переменные:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — настройки БД
- `JWT_SECRET` — секрет для подписи JWT (минимум 32 байта)
- `JWT_EXPIRATION_MS` — время жизни токена (мс)
- `SERVER_PORT` — порт HTTP-сервера
- `CORS_ALLOWED_ORIGINS` — список разрешённых origin (через запятую)
- `LOGGING_LEVEL` — уровень логирования

## База данных

Миграции Flyway: `src/main/resources/db/migration/V1__create_tables.sql`

Таблицы:
- `users` — пользователи
- `playthroughs` — прохождения
- `floors` — этажи
- `fog_chunks` — чанки тумана
- `entitlements` — права доступа (paywall)

## Тестовый сценарий

1. Зарегистрироваться / войти
2. Создать playthrough (`POST /playthrough`)
3. Получить текущий этаж (`GET /playthrough/active/floor`)
4. Получить начальный fog snapshot (`GET /floors/{id}/fog`)
5. Подключиться по STOMP к `/ws` (заголовок `Authorization: Bearer <token>`),
   подписаться на `/topic/floors/{id}/fog` для получения изменений тумана
6. На каждое движение слать:
   - `SEND /app/playthrough/position` — координаты
   - `SEND /app/floors/{id}/fog/reveal` — открыть область вокруг игрока
   Ответные изменения чанков придут на `/topic/floors/{id}/fog`.
