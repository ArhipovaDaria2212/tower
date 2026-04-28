# Tower Game Backend

Backend для 2D roguelike игры с fog of war системой.

## Технологический стек

- **Java 21**
- **Spring Boot 4.0.3**
- **Spring Security** с JWT аутентификацией
- **PostgreSQL**
- **JPA/Hibernate**
- **Flyway** (миграции)
- **OpenAPI/Swagger**

## Структура проекта

```
src/main/java/ru/arkhipova/tower/
├── config/          # Конфигурации (Security, OpenAPI)
├── controller/      # REST контроллеры
├── dto/             # Data Transfer Objects
├── entity/          # Сущности JPA
├── exception/       # Обработка исключений
├── repository/      # Spring Data репозитории
├── security/        # JWT безопасность
└── service/         # Бизнес-логика
```

## API Endpoints

### Аутентификация
- `POST /auth/guest` - Создать/получить гостевой аккаунт
- `POST /auth/register` - Регистрация пользователя
- `POST /auth/login` - Вход пользователя

### Прохождение
- `POST /playthrough` - Создать прохождение
- `GET /playthrough/active` - Получить активное прохождение
- `PUT /playthrough/position` - Обновить позицию игрока

### Этажи
- `GET /playthrough/active/floor` - Получить текущий этаж
- `POST /playthrough/active/floor/advance` - Перейти на следующий этаж

### Fog of War
- `GET /floors/{floorId}/fog` - Получить туман
- `POST /floors/{floorId}/fog` - Обновить туман
- `POST /floors/{floorId}/fog/reveal` - Открыть область вокруг игрока

### Иконки карты
- `GET /floors/{floorId}/discovered-icons` - Получить открытые иконки
- `POST /floors/{floorId}/discovered-icons` - Отметить иконку открытой

## Запуск

### Локально

1. Запустить PostgreSQL:
```bash
docker run -d -p 5432:5432 -e POSTGRES_DB=tower -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres postgres:16
```

2. Запустить приложение:
```bash
./gradlew bootRun
```

### Docker Compose

```bash
./gradlew jibDockerBuild
docker-compose up
```

## Swagger UI

После запуска доступен по адресу: http://localhost:8080/swagger-ui.html

## Конфигурация

Параметры в `application.yaml`:
- `spring.datasource.*` - Настройки БД
- `jwt.secret` - Секретный ключ для JWT
- `jwt.expiration` - Время жизни токена (мс)

## База данных

Миграции Flyway находятся в `src/main/resources/db/migration/V1__create_tables.sql`

Создаваемые таблицы:
- `users` - Пользователи
- `guests` - Гости
- `playthroughs` - Прохождения
- `floors` - Этажи
- `fog_chunks` - Чанки тумана
- `discovered_icons` - Открытые иконки
- `entitlements` - Права доступа (paywall)

## Тестовый сценарий

1. Старт: создать гостя
2. Создать/получить playthrough
3. Получить текущий этаж
4. Получить текущий fog
5. Симуляция движения игрока