# Explore With Me (Микросервисная архитектура)

Рефакторинг монолитного приложения "Explore With Me" в микросервисную архитектуру с использованием Spring Cloud.

## Архитектура проекта

Проект разделён на независимые сервисы:

1. `discovery-server` — Eureka Server (реестр микросервисов).
2. `config-server` — централизованное управление конфигурациями.
3. `gateway-server` — API Gateway (единая точка входа, маршрутизация).
4. `user-service` — сервис управления пользователями.
5. `event-service` — сервис управления событиями и категориями.
6. `request-service` — сервис обработки заявок на участие в событиях.
7. `compilation-service` — сервис подборок событий.
8. `comment-service` — сервис комментариев к событиям.
9. `stats-server` — сервис сбора и хранения статистики просмотров.

## Межсервисное взаимодействие
- Сервисы регистрируются в `discovery-server` (**Eureka Server**).
- Конфигурации хранятся централизованно в `config-server` (**Config Server**).
- Внешние запросы проходят через `gateway-server` (**Spring Cloud Gateway**).
- Общение между сервисами происходит через **OpenFeign**.

## Как запустить проект

### Требования
- Docker
- Java 21 (для локального запуска отдельных модулей)
- Maven 3.8+

### Запуск через Docker Compose
1. Перейдите в корневую директорию проекта
2. Выполните команду:
   ```bash
   docker-compose up --build
3. После завершения сборки и старта всех контейнеров, API Gateway будет доступен по адресу: http://localhost:8080.
4. Панель Eureka Server: http://localhost:8761

### Локальный запуск (для разработки)
1. Запустите discovery-server и config-server.
2. Запустите нужные микросервисы.

###  Стек технологий
- **Java 21**
- **Spring Cloud** (Eureka, Gateway, Config Server, OpenFeign)
- **Spring Data JPA** (PostgreSQL)
- **Docker & Docker Compose**
