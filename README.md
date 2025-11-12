# Bank Cards API

REST API для управления банковскими картами.

---

## 🚀 Запуск проекта
### Шаги запуска

1. **Создайте файл `.env` в корне проекта** с переменными:

```env
DB_USERNAME=bankuser
DB_PASSWORD=bankpass123
JWT_SECRET=your-super-secret-jwt-key-should-be-at-least-256-bits-long
```

2. **Запустите контейнеры**:
```bash
docker-compose up --build
```

3. **Документация Swagger UI доступна на**: http://localhost:8080/swagger-ui.html

## 🔐 JWT
Все эндпоинты, кроме `/api/auth/login`, `/api/auth/register` и `/api/auth/refresh`, требуют JWT-токен в заголовке `Authorization: Bearer <token>`

1. Получение **JWT токена**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "securePassword123"
  }'
```
Ответ:
```bash
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "refresh_eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```
