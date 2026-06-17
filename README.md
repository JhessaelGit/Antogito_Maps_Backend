# Antojitos Maps Backend

Backend REST API para **Antojitos Maps**, una plataforma de restaurantes en Bolivia. Construido con Java 21 y Spring Boot 3.2.

## Tabla de contenido

- [Tecnologías](#-tecnologías)
- [Requisitos previos](#-requisitos-previos)
- [Instalación y ejecución](#-instalación-y-ejecución)
- [Variables de entorno](#-variables-de-entorno)
- [Documentación de API (Swagger)](#-documentación-de-api-swagger)
- [Modelo de autenticación](#-modelo-de-autenticación)
- [Endpoints](#-endpoints)
- [Chatbot con IA](#-chatbot-con-ia)
- [Estructura del proyecto](#-estructura-del-proyecto)

## Tecnologías

| Tecnología | Versión | Descripción |
|---|---|---|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.2.4 | Framework web |
| Spring Data JPA | 3.2.x | Acceso a datos (PostgreSQL) |
| Spring Data MongoDB | 3.2.x | Persistencia de conversaciones de chat |
| PostgreSQL | - | Base de datos principal (Supabase) |
| MongoDB Atlas | - | Persistencia de historial de chat |
| Firebase Admin SDK | 9.x | Creación y verificación de usuarios |
| Lombok | - | Reducción de boilerplate |
| SpringDoc OpenAPI | 2.5.0 | Documentación Swagger |
| Cloudflare R2 (S3) | - | Almacenamiento de imágenes |
| Mistral AI | - | Motor de chatbot |

## Requisitos previos

- **Java 21** o superior
- **Maven 3.8+** (o usar el wrapper `./mvnw` incluido)
- Acceso a una base de datos **PostgreSQL**
- Instancia de **MongoDB** (Atlas o local) para el historial de chat
- Proyecto de **Firebase** con una cuenta de servicio configurada
- (Opcional) API Key de **Mistral AI** para el chatbot

## Instalación y ejecución

```bash
# 1. Clonar el repositorio
git clone https://github.com/JhessaelGit/Antogito_Maps_Backend.git
cd Antogito_Maps_Backend/maps-backend

# 2. Configurar variables de entorno
#    Crear archivo .env en maps-backend/ (ver sección "Variables de entorno")

# 3. Compilar
./mvnw clean compile

# 4. Ejecutar
./mvnw spring-boot:run

# 5. Verificar
curl http://localhost:8080/app/health
```

El servidor inicia en **http://localhost:8080**

## Variables de entorno

Crear un archivo `.env` en la carpeta `maps-backend/` con las siguientes variables:

```properties
# ── Base de datos (PostgreSQL) ──
APP_DB_URL=jdbc:postgresql://host:puerto/database?sslmode=require
APP_DB_USERNAME=tu_usuario
APP_DB_PASSWORD=tu_password
APP_DB_DRIVER=org.postgresql.Driver

# ── JPA / Hibernate ──
APP_JPA_DIALECT=org.hibernate.dialect.PostgreSQLDialect
APP_JPA_DDL_AUTO=none
APP_JPA_SHOW_SQL=true
APP_DB_INIT_MODE=never

# ── MongoDB (historial de chat) ──
MONGO_URI=mongodb+srv://usuario:password@cluster.mongodb.net/

# ── CORS ──
APP_CORS_ALLOWED_ORIGINS=*

# ── Firebase Admin SDK ──
FIREBASE_CREDENTIALS_BASE64=<base64 del JSON de cuenta de servicio>

# ── Firebase Web API Key (para autenticar email/password server-side) ──
FIREBASE_WEB_API_KEY=AIzaSy...

# ── Cloudflare R2 (almacenamiento de imágenes) ──
APP_R2_S3_API_URL=https://<account-id>.r2.cloudflarestorage.com
APP_R2_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
APP_R2_BUCKET=nombre-bucket
APP_R2_PUBLIC_BASE_URL=https://tu-dominio-publico.com
APP_R2_ACCESS_KEY_ID=tu_access_key
APP_R2_SECRET_ACCESS_KEY=tu_secret_key
APP_R2_UPLOAD_ENABLED=true

# ── Chatbot IA (Mistral AI) ──
APP_MISTRAL_API_KEY=tu_api_key_de_mistral
APP_CHAT_SYSTEM_PROMPT_FILE=system_prompt.txt
APP_CHAT_CONTEXT_FILE=context.json
```

> **Nota:** El archivo `.env` está en `.gitignore` y nunca se sube al repositorio.

> **Firebase:** El valor de `FIREBASE_CREDENTIALS_BASE64` se obtiene codificando en base64 el archivo JSON de la cuenta de servicio de Firebase: `base64 -w 0 serviceAccount.json`

## Documentación de API (Swagger)

Con el servidor en ejecución, accede a la documentación interactiva:

| Recurso | URL |
|---|---|
| **Swagger UI** | http://localhost:8080/swagger-ui/index.html |
| **OpenAPI JSON** | http://localhost:8080/v3/api-docs |
| **OpenAPI YAML** | http://localhost:8080/v3/api-docs.yaml |

La documentación Swagger incluye todos los endpoints organizados por tags:
- **Sistema** — Health checks
- **Restaurantes** — CRUD de restaurantes
- **Restaurant Auth** — Login/registro de owners (sin Firebase en el frontend)
- **Client Auth** — Login/registro de clientes (sin Firebase en el frontend)
- **Admin** — Gestión de administradores
- **Quejas** — Gestión de quejas de restaurantes y promociones
- **Promotions** — Promociones por restaurante
- **Loyalty** — Cuentas de fidelizacion y acumulacion de puntos
- **Coupons** — CRUD de cupones por restaurante
- **Chatbot** — Chatbot con IA (Mistral AI + persistencia MongoDB)

## Modelo de autenticación

> **Importante:** A partir de la versión actual, **el frontend NO necesita el SDK de Firebase**. Todo el ciclo de autenticación (creación de usuario, verificación de credenciales) es gestionado directamente por el backend.

### Flujo simplificado

```
Frontend          Backend              Firebase
   |                  |                    |
   |-- POST /login --> |                   |
   |   {email, pwd}   |-- REST API Auth -->|
   |                  |<-- token/OK -------|
   |                  |-- busca en BD ---->|
   |<-- {uuid, ...} --|                   |
```

### Headers de autorización por rol

| Rol | Header requerido | Obtenido en |
|---|---|---|
| **Cliente** | `X-Client-Id: <uuid>` | Respuesta de `/client/login` o `/client/registry` |
| **Owner** | *(sin header, usa ownerMail/ownerUuid en body)* | Respuesta de `/restaurant/login` |
| **Admin** | `X-Admin-Id: <uuid>` | Respuesta de `/admin/login` |

## Endpoints

### Sistema

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/` | Inicio API |
| `GET` | `/app/health` | Health general del backend |
| `GET` | `/app/health/db` | Health de conexión a BD |

### Restaurantes

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/restaurant/all` | Listar todos los restaurantes |
| `GET` | `/restaurant/get/{id}` | Obtener restaurante por UUID |
| `POST` | `/restaurant/create` | Crear restaurante (requiere ownerMail en body) |
| `POST` | `/restaurant/upload-image` | Subir imagen a Cloudflare R2 |
| `DELETE` | `/restaurant/delete/{id}` | Eliminar restaurante |

### Autenticación de Owners

> El frontend envía **email y password directamente**. El backend gestiona Firebase internamente.

| Método | Endpoint | Body | Descripción |
|--------|----------|------|-------------|
| `POST` | `/restaurant/registry` | `{email, password}` | Crea el owner en Firebase y lo registra en BD |
| `POST` | `/restaurant/login` | `{email, password}` | Autentica contra Firebase y devuelve `ownerId` + `restaurantIds` |
| `POST` | `/restaurant/logout` | `{mail}` | Registra logout en auditoría |

### Autenticación de Clientes

> El frontend envía **email y password directamente**. El backend gestiona Firebase internamente.

| Método | Endpoint | Body / Header | Descripción |
|--------|----------|---------------|-------------|
| `POST` | `/client/registry` | `{email, password, fullName, phone}` | Crea el cliente en Firebase y lo registra en BD |
| `POST` | `/client/login` | `{email, password}` | Autentica contra Firebase y devuelve `uuid` del cliente |
| `POST` | `/client/logout` | `{mail}` | Registra logout en auditoría |

### Administración

> Requiere header `X-Admin-Id` en todas las rutas excepto `/admin/login` y el bootstrap inicial de `/admin/create`.

| Método | Endpoint | Header | Descripción |
|--------|----------|--------|-------------|
| `POST` | `/admin/login` | — | Login con `{email, password}`. Firebase gestionado por el backend |
| `POST` | `/admin/create` | `X-Admin-Id` (opcional para bootstrap) | Crea admin en Firebase y BD. Sin header solo si no existe ningún admin |
| `PUT` | `/admin/edit` | `X-Admin-Id` | Editar perfil propio |
| `DELETE` | `/admin/delete/{id}` | `X-Admin-Id` | Borrado lógico de admin |
| `GET` | `/admin/all` | — | Listar admins activos |
| `GET` | `/admin/deleted` | — | Listar admins eliminados |
| `GET` | `/admin/restaurants` | `X-Admin-Id` | Listar restaurantes (moderación) |
| `PATCH` | `/admin/restaurants/{id}/block` | `X-Admin-Id` | Bloquear/desbloquear restaurante |

### Quejas (Complaints)

Los usuarios registrados pueden reportar restaurantes o promociones. Los administradores revisan y deciden si aceptar (lo que borra lógicamente el objetivo) o rechazar.

| Método | Endpoint | Header | Descripción |
|--------|----------|--------|-------------|
| `POST` | `/complaint/create` | `X-Client-Id` | Crear queja de restaurante (`type: RESTAURANT`) o promoción (`type: PROMOTION`) |
| `GET` | `/complaint/admin/all` | `X-Admin-Id` | Ver todas las quejas |
| `GET` | `/complaint/admin/pending` | `X-Admin-Id` | Ver quejas en estado `PENDING` |
| `POST` | `/complaint/admin/review/{id}` | `X-Admin-Id` | Revisar queja. Body: `{status: "ACCEPTED" \| "REJECTED"}` |

> Si el admin acepta (`ACCEPTED`), el restaurante o promoción objetivo es borrado lógicamente de forma automática.

### Promociones

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/promotion/restaurant/{restaurantId}` | Listar promociones activas de un restaurante |
| `POST` | `/promotion/restaurant/{restaurantId}` | Crear promoción (requiere `ownerUuid` o `ownerMail` en body) |

Tablas relacionadas: `promotions`, `restaurant`, `owner_account`, `owner_restaurant`.

Body para crear:

```json
{
  "ownerUuid": "20a63174-3799-4e7f-98c7-7f2af9e2c42c",
  "title": "2x1 en saltenas",
  "description": "Solo de lunes a viernes",
  "percentDiscount": 25.00,
  "dateStartPromotion": "2026-05-01",
  "dateEndPromotion": "2026-08-31",
  "isActivePromotion": true
}
```

### Fidelizacion

Modulo conectado a las tablas `loyalty_accounts` y `points_history`.

| Metodo | Endpoint | Tabla principal | Descripcion |
|--------|----------|-----------------|-------------|
| `GET` | `/api/loyalty/{clientId}` | `loyalty_accounts` | Obtiene el perfil de fidelizacion del cliente. Si no existe cuenta, crea una en nivel `BRONCE` con 0 puntos |
| `POST` | `/api/loyalty/add-points` | `loyalty_accounts`, `points_history` | Suma puntos al cliente y registra el movimiento en historial |

Body para sumar puntos:

```json
{
  "clientId": "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
  "points": 50,
  "reason": "Compra de producto"
}
```

Response:

```json
{
  "clientId": "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
  "points": 120,
  "level": "PLATA"
}
```

Reglas implementadas:
- `points` debe ser mayor a 0.
- El cliente debe existir en `client`.
- Cada suma crea un registro en `points_history`.
- Niveles: `BRONCE` de 0 a 99 puntos, `PLATA` de 100 a 299, `ORO` desde 300.

### Cupones

Modulo conectado a la tabla `coupons`. Las operaciones de creacion, edicion, pausa y eliminacion validan que el owner exista y este relacionado con el restaurante en `owner_restaurant`.

| Metodo | Endpoint | Tabla principal | Descripcion |
|--------|----------|-----------------|-------------|
| `GET` | `/coupon/restaurant/{restaurantId}` | `coupons` | Lista todos los cupones de un restaurante |
| `GET` | `/coupon/restaurant/{restaurantId}/{couponId}` | `coupons` | Obtiene un cupon especifico del restaurante |
| `POST` | `/coupon/restaurant/{restaurantId}` | `coupons` | Crea un cupon para el restaurante |
| `PUT` | `/coupon/restaurant/{restaurantId}/{couponId}` | `coupons` | Edita un cupon existente |
| `PATCH` | `/coupon/restaurant/{restaurantId}/{couponId}/pause` | `coupons` | Pausa un cupon cambiando su estado a `PAUSED` |
| `DELETE` | `/coupon/restaurant/{restaurantId}/{couponId}` | `coupons` | Elimina un cupon |

Tablas relacionadas: `coupons`, `restaurant`, `client`, `owner_account`, `owner_restaurant`.

Body para crear o editar:

```json
{
  "ownerUuid": "20a63174-3799-4e7f-98c7-7f2af9e2c42c",
  "clientId": "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
  "name": "Descuento de bienvenida",
  "description": "Valido en compras mayores a 50 Bs",
  "startDate": "2026-06-20",
  "expirationDate": "2026-06-30",
  "maxQuantity": 100,
  "discountType": "PERCENTAGE",
  "status": "ACTIVE",
  "qrCode": "QR-COUPON-001"
}
```

Body para pausar o eliminar:

```json
{
  "ownerUuid": "20a63174-3799-4e7f-98c7-7f2af9e2c42c"
}
```

Tambien se puede usar `ownerMail` en lugar de `ownerUuid`.

Response:

```json
{
  "uuid": "6f03af25-8da3-4258-b0b6-16e82fd417f0",
  "restaurantId": "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
  "clientId": "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
  "name": "Descuento de bienvenida",
  "description": "Valido en compras mayores a 50 Bs",
  "startDate": "2026-06-20",
  "expirationDate": "2026-06-30",
  "maxQuantity": 100,
  "discountType": "PERCENTAGE",
  "status": "ACTIVE",
  "qrCode": "QR-COUPON-001",
  "createdAt": "2026-06-17T19:00:00"
}
```

Reglas implementadas:
- El restaurante debe existir en `restaurant`.
- El owner debe existir en `owner_account`.
- El owner debe estar vinculado al restaurante en `owner_restaurant`.
- Si se envia `clientId`, el cliente debe existir en `client`.
- `expirationDate` no puede ser anterior a `startDate`.
- No se permite crear o editar un cupon expirado.
- No se permite crear o editar un cupon agotado (`maxQuantity <= 0`).
- No se permite crear o editar con estado `EXPIRED` o `SOLD_OUT`.
- Acciones criticas registradas en `registro.log`: creacion, edicion, pausa y eliminacion.

### Chatbot con IA

> Las conversaciones se persisten en **MongoDB** vinculadas al `X-Client-Id` del cliente. Usuarios anónimos también pueden chatear (sin persistencia por usuario).

| Método | Endpoint | Header | Descripción |
|--------|----------|--------|-------------|
| `POST` | `/chat` | `X-Client-Id` (opcional) | Enviar mensaje. Acepta `latitude`/`longitude` para recomendaciones cercanas |
| `GET` | `/chat/history` | `X-Client-Id` | Obtener historial completo de conversación del cliente |
| `GET` | `/chat/conversations` | — | Listar resumen de todas las conversaciones |

> Para documentación detallada de cada endpoint (request/response), ver [POSTMAN.md](maps-backend/POSTMAN.md)

## Chatbot con IA

El chatbot utiliza **Mistral AI** como motor de inteligencia artificial, con historial persistido en **MongoDB**.

### Características

- Envío y recepción de mensajes vía API REST (`POST /chat`)
- Historial **persistido en MongoDB** por cliente (`X-Client-Id`)
- Usuarios anónimos mantienen conversación dentro de la misma sesión
- **Contexto estructurado** (`context.json`) con rol, reglas y dominio del asistente
- **System prompt** (`system_prompt.txt`) personalizado: respuestas cortas, amigables y en español
- **Recomendaciones por geolocalización**: si el frontend envía `latitude` y `longitude`, el chatbot consulta la BD y recomienda restaurantes reales dentro de un **radio de 5 km** usando la fórmula de Haversine

### Recomendaciones por ubicación

Cuando el frontend envía las coordenadas del usuario:

1. El backend consulta todos los restaurantes no bloqueados de la BD
2. Calcula la distancia con la **fórmula de Haversine**
3. Filtra los restaurantes dentro de un **radio de 5 km**
4. Inyecta la lista como contexto al modelo de IA
5. La IA responde con restaurantes **reales** de la plataforma, con nombre, categoría y distancia

### Configuración

| Variable de entorno | Descripción | Requerida |
|---------------------|-------------|-----------|
| `APP_MISTRAL_API_KEY` | API Key de Mistral AI | ✅ |
| `MONGO_URI` | URI de conexión a MongoDB | ✅ |
| `APP_MISTRAL_API_URL` | URL del endpoint de Mistral | No (tiene default) |
| `APP_MISTRAL_MODEL` | Modelo de Mistral a usar | No (default: `mistral-large-latest`) |

## Estructura del proyecto

```
maps-backend/
├── src/main/java/com/antojito/maps_backend/
│   ├── config/              # Configuraciones (Firebase, OpenAPI, MongoDB)
│   ├── controller/          # REST Controllers
│   │   ├── AdminController.java      # Auth + gestión de admins
│   │   ├── AuthController.java       # Auth de owners (restaurant/login, registry)
│   │   ├── ChatController.java       # Chatbot IA
│   │   ├── ClientController.java     # Auth de clientes
│   │   ├── ComplaintController.java  # Quejas
│   │   ├── CouponController.java     # CRUD cupones
│   │   ├── LoyaltyController.java    # Fidelizacion
│   │   ├── PromotionController.java  # Promociones
│   │   ├── RestauranteController.java# CRUD restaurantes
│   │   └── SystemController.java     # Health checks
│   ├── dto/                 # Data Transfer Objects
│   ├── exception/           # Manejo global de errores
│   ├── model/               # Entidades JPA + documentos MongoDB
│   ├── repository/          # Repositorios Spring Data (JPA + MongoDB)
│   ├── service/             # Lógica de negocio
│   │   ├── AdminService.java
│   │   ├── AuditLogService.java
│   │   ├── ChatService.java
│   │   ├── ComplaintService.java
│   │   ├── CouponService.java
│   │   ├── LoyaltyService.java
│   │   ├── PromotionService.java
│   │   ├── R2StorageService.java
│   │   └── RestauranteService.java
│   └── MapsBackendApplication.java
├── src/main/resources/
│   ├── application.properties
│   └── schema.sql
├── context.json             # Contexto estructurado del chatbot (rol, reglas, dominio)
├── system_prompt.txt        # System prompt del chatbot (personalidad, tono)
├── POSTMAN.md               # Guía detallada de endpoints con ejemplos
├── pom.xml
└── .env                     # Variables de entorno (no versionado)
```
