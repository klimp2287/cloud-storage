# Облачное хранилище

## Описание проекта

Дипломная работа курса «Разработчик на Java» от Нетологии.

Облачное хранилище — REST-сервис, предоставляющий пользователям возможность загружать, скачивать, переименовывать и удалять файлы, а также просматривать список загруженных файлов.

Взаимодействие с сервисом осуществляется посредством REST API в соответствии с заранее описанной спецификацией [CloudServiceSpecification.yaml](CloudServiceSpecification.yaml).

Проект состоит из двух частей:
- **Backend** — REST API, реализованный на Java с использованием Spring Boot.
- **Frontend** — веб-интерфейс для взаимодействия с облачным хранилищем.

## Ссылки на проекты

- [Backend — cloud-storage](https://github.com/klimp2287/cloud-storage)
- [Frontend — cloud-storage-front](https://github.com/klimp2287/cloud-storage-front)

## Тестовые пользователи

Для проверки работы приложения предусмотрены два пользователя:

| Логин | Пароль |
|---|---|
| user@one.user | user_one |
| user@two.user | user_two |

## Технологии

- Java 11
- Spring Boot 2.6.1
- Spring Web
- Spring Data JPA
- Spring Security
- Hibernate
- PostgreSQL 14
- Liquibase
- JWT
- Maven
- Docker
- Docker Compose
- JUnit
- Mockito
- Testcontainers

## Реализация

В рамках проекта реализованы следующие возможности:

- Авторизация пользователей с использованием JWT.
- Загрузка файлов в облачное хранилище.
- Скачивание ранее загруженных файлов.
- Удаление файлов.
- Переименование файлов.
- Получение списка файлов пользователя с возможностью ограничения количества отображаемых записей.
- Разделение файлов по пользователям.
- Хранение пользовательских данных и информации о файлах в PostgreSQL.
- Управление структурой базы данных с помощью Liquibase.
- Контейнеризация приложения и базы данных с помощью Docker Compose.

## Запуск приложения

Для запуска проекта необходимо установить:

- [Java 11](https://adoptium.net/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Git](https://git-scm.com/)

### 1. Клонирование репозитория

```bash
git clone https://github.com/klimp2287/cloud-storage.git
cd cloud-storage
```

### 2. Запуск backend и базы данных через Docker Compose

Для запуска приложения вместе с PostgreSQL выполните команду:

```bash
docker compose up -d --build
```

Docker Compose автоматически:
1. Создаст контейнер PostgreSQL.
2. Дождётся готовности базы данных.
3. Соберёт Docker-образ backend-приложения.
4. Запустит backend-сервис.

После успешного запуска:

- Backend API: http://localhost:5050
- PostgreSQL: localhost:5433

Остановка приложения:

```bash
docker compose down
```

Для остановки приложения с удалением контейнеров и сохранением данных базы данных используйте:

```bash
docker compose down
```

Данные PostgreSQL сохраняются в Docker volume `pg_data`.

### 3. Запуск только базы данных через Docker Compose

Если необходимо запустить базу данных отдельно, выполните:

```bash
docker compose up -d database
```

После этого backend можно запустить отдельно из IDE или с помощью Maven.

### 4. Запуск backend через Maven

Для запуска приложения без Docker-контейнера backend необходимо предварительно запустить PostgreSQL.

Выполните команду:

```bash
./mvnw spring-boot:run
```

Для Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

### 5. Запуск frontend

Frontend расположен в отдельном репозитории:

[cloud-storage-front](https://github.com/klimp2287/cloud-storage-front)

Для запуска frontend:

```bash
git clone https://github.com/klimp2287/cloud-storage-front.git
cd cloud-storage-front
npm install
npm run serve
```

Frontend будет доступен по адресу:

http://localhost:8080

Для подключения к backend используется адрес:

http://localhost:5050

## REST API

Спецификация API находится в файле [CloudServiceSpecification.yaml](CloudServiceSpecification.yaml).

Основные методы:

| Метод | Endpoint | Описание |
|---|---|---|
| POST | `/login` | Авторизация пользователя |
| POST | `/logout` | Выход из системы |
| POST | `/file` | Загрузка файла |
| GET | `/file` | Скачивание файла |
| PUT | `/file` | Переименование файла |
| DELETE | `/file` | Удаление файла |
| GET | `/list` | Получение списка файлов |

Для выполнения операций с файлами необходимо передавать JWT-токен в HTTP-заголовке `auth-token`.

## Тестирование

Для проверки корректности работы приложения реализованы:

- Unit-тесты с использованием JUnit и Mockito.
- Интеграционные тесты с использованием Testcontainers.

Всего реализовано **33 теста**:
- 17 unit-тестов.
- 16 интеграционных тестов.

Для запуска тестов выполните:

```bash
./mvnw clean test
```

В Windows:

```powershell
.\mvnw.cmd clean test
```

Все тесты успешно проходят.



