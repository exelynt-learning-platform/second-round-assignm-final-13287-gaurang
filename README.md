# ShopFlow Backend

A production-style e-commerce backend built with Spring Boot 3, JWT authentication, and Stripe payment integration.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2 |
| Security | Spring Security 6 + JWT (jjwt 0.12) |
| Database | MySQL 8 via Spring Data JPA |
| Payments | Stripe Java SDK 24 |
| Build | Maven |
| Tests | JUnit 5 + Mockito |

---

## Project Structure

```
src/main/java/com/shopflow/
├── config/         # Security, JWT filter, Stripe init
├── controller/     # AuthController, ProductController, CartController,
│                   # OrderController, PaymentController
├── service/        # Business logic layer
├── repository/     # Spring Data JPA interfaces
├── entity/         # JPA entities (User, Product, Cart, CartItem, Order, OrderItem)
├── dto/
│   ├── request/    # Incoming request bodies
│   └── response/   # API response shapes
├── exception/      # Custom exceptions + GlobalExceptionHandler
└── util/           # JwtHelper

src/test/java/com/shopflow/service/
├── AuthServiceTest.java
├── CartServiceTest.java
├── OrderServiceTest.java
└── ProductServiceTest.java
```

---

## Setup

### 1. Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8 running locally
- A Stripe account (free test mode is fine)

### 2. Create the database

```sql
CREATE DATABASE shopflow_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Configure environment variables

Use environment variables instead of hardcoding secrets:

```bash
# database
setx SHOPFLOW_DB_USER "root"
setx SHOPFLOW_DB_PASSWORD "YOUR_MYSQL_PASSWORD"
# optional if DB is not localhost default
setx SHOPFLOW_DB_URL "jdbc:mysql://localhost:3306/shopflow_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC"

# jwt (use a strong random key in production)
setx SHOPFLOW_JWT_SECRET "YOUR_LONG_RANDOM_SECRET"

# stripe test mode
setx SHOPFLOW_STRIPE_SECRET_KEY "sk_test_..."
setx SHOPFLOW_STRIPE_WEBHOOK_SECRET "whsec_..."
setx SHOPFLOW_STRIPE_CURRENCY "inr"
```

Then reopen terminal before running so new env vars are available.

You can use `.env.example` as a checklist of required variables.

> Tables are created automatically on first run via `spring.jpa.hibernate.ddl-auto=update`.

### 4. Run the app

```bash
mvn spring-boot:run
```

Server starts on `http://localhost:8080`.

### 5. Run tests

```bash
mvn test
```

---

## API Reference

All protected endpoints require the header:
```
Authorization: Bearer <token>
```

## Postman Testing Steps

Use this order in Postman for quick end-to-end testing:

1. **Register user**
   - `POST /api/auth/register`
2. **Login user**
   - `POST /api/auth/login`
   - copy `token` from response
3. **Set auth header for protected APIs**
   - `Authorization: Bearer <token>`
4. **Create product** (admin token required)
   - `POST /api/products`
5. **Browse products**
   - `GET /api/products`
6. **Add item to cart**
   - `POST /api/cart/items`
7. **View cart**
   - `GET /api/cart`
8. **Place order from cart**
   - `POST /api/orders`
9. **Create payment intent**
   - `POST /api/payments/intent/{orderId}`
10. **Webhook test (optional local)**
   - run Stripe CLI and forward to `/api/payments/webhook`

### Suggested Postman Environment Variables

- `baseUrl` = `http://localhost:8080`
- `token` = JWT from login response
- `productId` = created product id
- `cartItemId` = cart item id
- `orderId` = created order id

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | No | Register new customer |
| POST | `/api/auth/login` | No | Login, returns JWT |

**Register body:**
```json
{
  "fullName": "Gaurang Mali",
  "email": "gaurang@example.com",
  "password": "secure123"
}
```

**Login body:**
```json
{
  "email": "gaurang@example.com",
  "password": "secure123"
}
```

**Response (both):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "gaurang@example.com",
  "fullName": "Gaurang Mali",
  "role": "CUSTOMER"
}
```

---

### Products

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/products?page=0&size=12` | No | Paginated product list |
| GET | `/api/products/{id}` | No | Single product |
| GET | `/api/products/search?q=mouse` | No | Keyword search |
| POST | `/api/products` | ADMIN | Create product |
| PUT | `/api/products/{id}` | ADMIN | Update product |
| DELETE | `/api/products/{id}` | ADMIN | Soft-delete product |

**Create/Update body:**
```json
{
  "name": "Wireless Mouse",
  "description": "Ergonomic 2.4GHz wireless mouse",
  "price": 799.00,
  "stockQty": 50,
  "imageUrl": "https://example.com/mouse.jpg"
}
```

---

### Cart

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/cart` | Yes | View cart with totals |
| POST | `/api/cart/items` | Yes | Add product to cart |
| PATCH | `/api/cart/items/{cartItemId}?quantity=3` | Yes | Update item quantity |
| DELETE | `/api/cart/items/{cartItemId}` | Yes | Remove item |

**Add item body:**
```json
{
  "productId": 1,
  "quantity": 2
}
```

---

### Orders

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/orders` | Yes | Place order from cart |
| GET | `/api/orders` | Yes | My order history |
| GET | `/api/orders/{orderId}` | Yes | Single order details |

**Place order body:**
```json
{
  "shippingAddress": "B-612, Hinjewadi Phase 1, Pune",
  "pincode": "411057"
}
```

---

### Payments

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/payments/intent/{orderId}` | Yes | Get Stripe clientSecret |
| POST | `/api/payments/webhook` | Stripe-signed | Webhook for payment events |

**Payment intent response:**
```json
{
  "clientSecret": "pi_3xxx_secret_xxx",
  "paymentIntentId": "pi_3xxx",
  "amount": 6998.00,
  "currency": "inr"
}
```

After receiving `clientSecret`, use Stripe.js on the frontend to confirm payment. Stripe will then POST to `/api/payments/webhook` to confirm success or failure.

---

## Stripe Webhook Setup (Local Testing)

Install the Stripe CLI and forward events to your local server:

```bash
stripe login
stripe listen --forward-to localhost:8080/api/payments/webhook
```

Set the webhook signing secret into env var `SHOPFLOW_STRIPE_WEBHOOK_SECRET`.

---

## Database Relationships

```
users      1 ──── 1   carts
users      1 ──── N   orders
carts      1 ──── N   cart_items
cart_items N ──── 1   products
orders     1 ──── N   order_items
order_items N ──── 1  products
```

---

## Error Response Format

All errors follow a consistent structure:

```json
{
  "timestamp": "2026-03-27T10:30:00",
  "status": 400,
  "error": "Validation failed",
  "details": {
    "email": "Enter a valid email",
    "password": "Password must be at least 6 characters"
  }
}
```

---

## Making an Admin User

After registering, update the role directly in MySQL:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@shopflow.com';
```

---

## Key Design Decisions

- **Soft deletes on products** — setting `active=false` instead of hard-deleting keeps historical order data intact.
- **Price snapshot on order items** — `priceAtPurchase` is stored at checkout time so order totals remain accurate even if the product price changes later.
- **Cart isolation** — every cart operation validates that the item belongs to the requesting user's cart before any mutation.
- **Stripe webhook verification** — the webhook endpoint verifies Stripe's HMAC signature before trusting the event payload.
