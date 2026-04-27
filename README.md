# Antojitos Maps Backend

Backend REST API para **Antojitos Maps**, una plataforma de restaurantes en Bolivia. Construido con Java 21 y Spring Boot 3.2.

## Tabla de contenido

- [Tecnologías](#-tecnologías)
- [Requisitos previos](#-requisitos-previos)
- [Instalación y ejecución](#-instalación-y-ejecución)
- [Variables de entorno](#-variables-de-entorno)
- [Documentación de API (Swagger)](#-documentación-de-api-swagger)
- [Endpoints](#-endpoints)
- [Chatbot con IA](#-chatbot-con-ia)
- [Estructura del proyecto](#-estructura-del-proyecto)

## Tecnologías

| Tecnología | Versión | Descripción |
|---|---|---|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.2.4 | Framework web |
| Spring Data JPA | 3.2.x | Acceso a datos |
| PostgreSQL | - | Base de datos (Supabase) |
| Lombok | - | Reducción de boilerplate |
| SpringDoc OpenAPI | 2.5.0 | Documentación Swagger |
| Cloudflare R2 (S3) | - | Almacenamiento de imágenes |
| Mistral AI | - | Motor de chatbot |

## Requisitos previos

- **Java 21** o superior
- **Maven 3.8+** (o usar el wrapper `./mvnw` incluido)
- Acceso a una base de datos **PostgreSQL**
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
# ── Base de datos ──
APP_DB_URL=jdbc:postgresql://host:puerto/database?sslmode=require
APP_DB_USERNAME=tu_usuario
APP_DB_PASSWORD=tu_password
APP_DB_DRIVER=org.postgresql.Driver

# ── JPA / Hibernate ──
APP_JPA_DIALECT=org.hibernate.dialect.PostgreSQLDialect
APP_JPA_DDL_AUTO=none
APP_JPA_SHOW_SQL=true
APP_DB_INIT_MODE=never

# ── CORS ──
APP_CORS_ALLOWED_ORIGINS=*

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
APP_CHAT_CONVERSATIONS_FILE=conversations.json
```

> **Nota:** El archivo `.env` está en `.gitignore` y nunca se sube al repositorio.

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
- **Restaurant Auth** — Login/registro de owners
- **Admin** — Gestión de administradores
- **Promotions** — Promociones por restaurante
- **Chatbot** — Chatbot con IA (Mistral AI)

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
| `POST` | `/restaurant/create` | Crear restaurante (requiere owner) |
| `POST` | `/restaurant/upload-image` | Subir imagen a Cloudflare R2 |
| `DELETE` | `/restaurant/delete/{id}` | Eliminar restaurante |

### Autenticación (Owners)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/restaurant/login` | Login de owner |
| `POST` | `/restaurant/registry` | Registrar nuevo owner |
| `POST` | `/restaurant/logout` | Logout (auditoría) |

### Administración

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/admin/login` | Login de admin |
| `POST` | `/admin/create` | Crear admin (requiere `X-Admin-Id`) |
| `PUT` | `/admin/edit` | Editar perfil propio |
| `DELETE` | `/admin/delete/{id}` | Borrado lógico de admin |
| `GET` | `/admin/all` | Listar admins activos |
| `GET` | `/admin/deleted` | Listar admins eliminados |
| `GET` | `/admin/restaurants` | Listar restaurantes (moderación) |
| `PATCH` | `/admin/restaurants/{id}/block` | Bloquear/desbloquear restaurante |

### Promociones

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/promotion/restaurant/{restaurantId}` | Listar promociones activas |
| `POST` | `/promotion/restaurant/{restaurantId}` | Crear promoción |

### Chatbot con IA

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/chat` | Enviar mensaje al chatbot (acepta lat/lng para recomendaciones cercanas) |
| `GET` | `/chat/{conversationId}` | Obtener historial de conversación |
| `GET` | `/chat/conversations` | Listar todas las conversaciones |

> Para documentación detallada de cada endpoint (request/response), ver [POSTMAN.md](maps-backend/POSTMAN.md)

## Chatbot con IA

El chatbot utiliza **Mistral AI** como motor de inteligencia artificial.

### Características

- Envío y recepción de mensajes vía API REST (`POST /chat`)
- Cada conversación tiene un **UUID único**
- Historial de conversaciones **persistido en archivo JSON**
- **Contexto estructurado** (`context.json`) con rol, reglas y dominio del asistente
- **System prompt** (`system_prompt.txt`) personalizado: respuestas cortas, amigables y en español
- **Recomendaciones por geolocalización**: si el frontend envía `latitude` y `longitude`, el chatbot consulta la BD y recomienda restaurantes reales dentro de un **radio de 5 km** usando la fórmula de Haversine
- API key segura (nunca expuesta al frontend, configurada por variable de entorno)

### Contexto del chatbot

El comportamiento del chatbot se define mediante dos archivos:

| Archivo | Propósito |
|---------|-----------|
| `context.json` | Contexto estructurado JSON con rol del asistente, dominio (restaurantes/promociones), reglas de comportamiento y ejemplos de respuesta |
| `system_prompt.txt` | Prompt en lenguaje natural que define la personalidad: tono amigable, respuestas cortas (2-3 oraciones), uso de emojis, solo dominio gastronómico |

Ambos archivos se cargan al iniciar el servidor y se inyectan en cada request a Mistral AI.

### Recomendaciones por ubicación

Cuando el frontend envía las coordenadas del usuario:

1. El backend consulta todos los restaurantes no bloqueados de la BD
2. Calcula la distancia con la **fórmula de Haversine**
3. Filtra los restaurantes dentro de un **radio de 5 km**
4. Inyecta la lista como contexto al modelo de IA
5. La IA responde con restaurantes **reales** de la plataforma, con nombre, categoría y distancia

### Configuración

| Variable de entorno | Descripción | Default |
|---------------------|-------------|---------|
| `APP_MISTRAL_API_KEY` | API Key de Mistral AI | (requerida) |
| `APP_MISTRAL_API_URL` | URL del endpoint de Mistral | `https://api.mistral.ai/v1/chat/completions` |
| `APP_MISTRAL_MODEL` | Modelo de Mistral a usar | `mistral-large-latest` |
| `APP_CHAT_SYSTEM_PROMPT_FILE` | Ruta al archivo de system prompt | `system_prompt.txt` |
| `APP_CHAT_CONTEXT_FILE` | Ruta al archivo de contexto JSON | `context.json` |
| `APP_CHAT_CONVERSATIONS_FILE` | Ruta al archivo de conversaciones | `conversations.json` |

## Estructura del proyecto

```
maps-backend/
├── src/main/java/com/antojito/maps_backend/
│   ├── config/              # Configuraciones (OpenAPI, Filtros)
│   ├── controller/          # REST Controllers
│   │   ├── AdminController.java
│   │   ├── AuthController.java
│   │   ├── ChatController.java
│   │   ├── PromotionController.java
│   │   ├── RestauranteController.java
│   │   └── SystemController.java
│   ├── dto/                 # Data Transfer Objects
│   ├── exception/           # Manejo global de errores
│   ├── model/               # Entidades JPA
│   ├── repository/          # Repositorios Spring Data
│   ├── service/             # Lógica de negocio
│   │   ├── AdminService.java
│   │   ├── AuditLogService.java
│   │   ├── ChatService.java
│   │   ├── PromotionService.java
│   │   ├── R2StorageService.java
│   │   └── RestauranteService.java
│   └── MapsBackendApplication.java
├── src/main/resources/
│   ├── application.properties
│   └── schema.sql
├── context.json             # Contexto estructurado del chatbot (rol, reglas, dominio)
├── system_prompt.txt        # System prompt del chatbot (personalidad, tono)
├── conversations.json       # Historial de conversaciones (generado en runtime)
├── POSTMAN.md               # Guía detallada de endpoints
├── pom.xml
└── .env                     # Variables de entorno (no versionado)
```

