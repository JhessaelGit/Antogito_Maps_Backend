# POSTMAN Guide - Antojitos Maps Backend

Base URL local:

http://localhost:8080

## Endpoints disponibles

### 1) GET /restaurant/all

Descripcion:
Obtiene la lista de restaurantes.

Request:
- Body: no aplica
- Params: no aplica

Response 200 ejemplo:

```json
[
  {
    "uuid": "d319d467-74f1-4524-bd62-caf588892e3f",
    "name": "Sabor Valluno",
    "description": "Comida tipica cochabambina con menu ejecutivo y delivery.",
    "imagenUrl": "https://.../restaurantes/sabor-valluno.jpg",
    "planSuscription": "PREMIUM",
    "planExpirationDate": "2026-10-10",
    "isBlocked": false,
    "latitude": -17.3922,
    "longitude": -66.1561,
    "category": "Comida Tipica"
  }
]
```

### 2) POST /restaurant/upload-image

Descripcion:
Sube una imagen del restaurante a Cloudflare R2 y devuelve la URL publica.

Request:
- Content-Type: multipart/form-data
- Campo file: archivo de imagen (max 5 MB)
- Campo name (opcional): nombre del restaurante para el slug del archivo

Response 200 ejemplo:

```json
{
  "imageUrl": "https://<account-id>.r2.cloudflarestorage.com/<bucket>/restaurantes/nuevo-antojito-uuid.jpg"
}
```

Response 413 ejemplo:

```json
{
  "timestamp": "2026-04-10T22:11:24.575Z",
  "status": 413,
  "error": "Payload Too Large",
  "message": "La imagen no puede exceder 5 MB",
  "path": "/restaurant/upload-image"
}
```

### 3) POST /restaurant/create

Descripcion:
Crea un restaurante asociado a un owner registrado. El campo imagenUrl debe ser la URL de Cloudflare obtenida en /restaurant/upload-image.

Request body ejemplo:

```json
{
  "ownerMail": "owner.nuevo@antojitosmaps.com",
  "name": "Nuevo Antojito",
  "latitude": -17.4,
  "longitude": -66.1,
  "planSuscription": "BASIC",
  "planExpirationDate": "2026-12-31",
  "isBlocked": false,
  "description": "Registro de prueba",
  "imagenUrl": "https://.../restaurantes/nuevo-antojito.jpg",
  "category": "Comida Rapida"
}
```

Response 201 ejemplo:

```json
{
  "uuid": "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
  "name": "Nuevo Antojito",
  "description": "Registro de prueba",
  "imagenUrl": "https://.../restaurantes/nuevo-antojito.jpg",
  "planSuscription": "BASIC",
  "planExpirationDate": "2026-12-31",
  "isBlocked": false,
  "latitude": -17.4,
  "longitude": -66.1,
  "category": "Comida Rapida"
}
```

Response 404 ejemplo (owner no existe):

```json
{
  "timestamp": "2026-04-10T22:11:24.575Z",
  "status": 404,
  "error": "Not Found",
  "message": "No existe owner con mail <ownerMail>",
  "path": "/restaurant/create"
}
```

### 4) GET /restaurant/get/{id}

Descripcion:
Obtiene un restaurante por UUID.

Path param:
- id: UUID del restaurante

Response 200 ejemplo:

```json
{
  "uuid": "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
  "name": "Nuevo Antojito",
  "description": "Registro de prueba",
  "imagenUrl": "https://.../restaurantes/nuevo-antojito.jpg",
  "planSuscription": "BASIC",
  "planExpirationDate": "2026-12-31",
  "isBlocked": false,
  "latitude": -17.4,
  "longitude": -66.1,
  "category": "Comida Rapida"
}
```

Response 404 ejemplo:

```json
{
  "timestamp": "2026-04-10T22:11:24.575Z",
  "status": 404,
  "error": "Not Found",
  "message": "No existe restaurante con uuid <id>",
  "path": "/restaurant/get/<id>"
}
```

### 5) DELETE /restaurant/delete/{id}

Descripcion:
Elimina un restaurante por UUID.

Path param:
- id: UUID del restaurante

Response 204:
- sin body

### 6) GET /app/health

Descripcion:
Estado general del backend.

Response 200 ejemplo:

```json
{
  "status": "UP",
  "timestamp": "2026-04-10T22:11:24.575Z"
}
```

### 7) GET /app/health/db

Descripcion:
Estado de la conexion a base de datos.

Response 200 ejemplo:

```json
{
  "timestamp": "2026-04-10T22:11:24.575Z",
  "status": "UP",
  "databaseProduct": "PostgreSQL",
  "databaseUrl": "jdbc:postgresql://..."
}
```

Response 503 ejemplo:

```json
{
  "timestamp": "2026-04-10T22:11:24.575Z",
  "status": "DOWN",
  "error": "Connection refused"
}
```

### 8) POST /restaurant/login

Descripcion:
Valida credenciales en owner_account y devuelve la identidad del owner con los restaurantes asociados.

Request body ejemplo:

```json
{
  "mail": "owner.sabor@antojitosmaps.com",
  "password": "OwnerSabor2026!"
}
```

Response 200 ejemplo:

```json
{
  "ownerId": "20a63174-3799-4e7f-98c7-7f2af9e2c42c",
  "mail": "owner.sabor@antojitosmaps.com",
  "restaurantIds": [
    "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
    "58f58d45-2d7c-47ff-a6ff-c0d57cb021c2"
  ],
  "message": "login correcto"
}
```

Response 401 ejemplo:

```json
{
  "timestamp": "2026-04-10T22:11:24.575Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Credenciales invalidas",
  "path": "/restaurant/login"
}
```

### 9) POST /restaurant/registry

Descripcion:
Registra un owner primero (sin restaurante).

Request body ejemplo:

```json
{
  "mail": "owner.nuevo@antojitosmaps.com",
  "password": "OwnerNuevo2026!"
}
```

Response 201 ejemplo:

```json
{
  "message": "owner registrado"
}
```

Response 400 ejemplo (owner ya existe):

```json
{
  "timestamp": "2026-04-10T22:11:24.575Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Ya existe un owner con ese mail",
  "path": "/restaurant/registry"
}
```

### 10) POST /restaurant/logout

Descripcion:
Registra logout en auditoria.

Request body ejemplo:

```json
{
  "mail": "owner.sabor@antojitosmaps.com"
}
```

Response 200 ejemplo:

```json
{
  "message": "logout registrado"
}
```

### 11) POST /admin/login

Descripcion:
Autentica administrador con mail y password.

Request body ejemplo:

```json
{
  "mail": "admin@antojitosmaps.com",
  "password": "Admin2026!"
}
```

Response 200 ejemplo:

```json
{
  "adminId": "f792617d-0d5d-4881-b5f6-679bcf2c37f8",
  "mail": "admin@antojitosmaps.com",
  "message": "login correcto"
}
```

### 12) POST /admin/create

Descripcion:
Crea un nuevo administrador. Requiere header X-Admin-Id de un admin activo.
Si no existe ningun admin activo, permite bootstrap inicial sin header.

Headers:
- X-Admin-Id: UUID del admin autenticado (opcional solo para bootstrap inicial)

Request body ejemplo:

```json
{
  "mail": "nuevo.admin@antojitosmaps.com",
  "password": "NuevoAdmin2026!"
}
```

### 13) PUT /admin/edit

Descripcion:
Actualiza el perfil del admin autenticado (mail y password).

Headers:
- X-Admin-Id: UUID del admin autenticado (requerido)

Request body ejemplo:

```json
{
  "mail": "admin.editado@antojitosmaps.com",
  "password": "AdminEditado2026!"
}
```

### 14) DELETE /admin/delete/{id}

Descripcion:
Realiza borrado logico de otro administrador.

Headers:
- X-Admin-Id: UUID del admin autenticado (requerido)

Path param:
- id: UUID del admin a eliminar logicamente

### 15) GET /admin/all

Descripcion:
Lista administradores activos.

### 16) GET /admin/deleted

Descripcion:
Lista administradores eliminados logicamente.

### 17) GET /admin/restaurants

Descripcion:
Lista todos los restaurantes para moderacion.

Headers:
- X-Admin-Id: UUID del admin autenticado (requerido)

### 18) PATCH /admin/restaurants/{id}/block

Descripcion:
Permite bloquear o desbloquear un restaurante actualizando isBlocked.

Headers:
- X-Admin-Id: UUID del admin autenticado (requerido)

Path param:
- id: UUID del restaurante

Request body ejemplo:

```json
{
  "isBlocked": true
}
```

### 19) GET /promotion/restaurant/{restaurantId}

Descripcion:
Obtiene promociones activas de un restaurante especifico.

Identificacion del restaurante:
- Path param: `restaurantId` (UUID)

Autenticacion:
- No requiere header de autenticacion.

Response 200 ejemplo:

```json
[
  {
    "uuid": "6f03af25-8da3-4258-b0b6-16e82fd417f0",
    "restaurantId": "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
    "title": "2x1 en saltenas",
    "description": "Solo de lunes a viernes",
    "percentDiscount": 25.00,
    "dateStartPromotion": "2026-04-20",
    "dateEndPromotion": "2026-04-30",
    "isActivePromotion": true
  }
]
```

Response 404 ejemplo:

```json
{
  "timestamp": "2026-04-20T22:11:24.575Z",
  "status": 404,
  "error": "Not Found",
  "message": "No existe restaurante con uuid <restaurantId>",
  "path": "/promotion/restaurant/<restaurantId>"
}
```

### 20) POST /promotion/restaurant/{restaurantId}

Descripcion:
Crea una promocion para un restaurante. El restaurante se toma del path param.

Identificacion del restaurante:
- Path param: `restaurantId` (UUID)

Autenticacion/autorizacion:
- No usa token ni sesion.
- No requiere headers especiales.
- Requiere `ownerUuid` o `ownerMail` en body (preferido `ownerUuid`).
- El backend valida que el owner exista en `owner_account` y que este asociado al restaurante en `owner_restaurant`.

Request body exacto:

```json
{
  "ownerUuid": "20a63174-3799-4e7f-98c7-7f2af9e2c42c",
  "title": "2x1 en saltenas",
  "description": "Solo de lunes a viernes",
  "percentDiscount": 25.00,
  "dateStartPromotion": "2026-04-20",
  "dateEndPromotion": "2026-04-30",
  "isActivePromotion": true
}
```

Tipos:
- `ownerUuid`: UUID string (preferido)
- `ownerMail`: string (email, alternativo si no envias `ownerUuid`)
- `title`: string
- `description`: string (opcional)
- `percentDiscount`: number (0 a 100)
- `dateStartPromotion`: date string `yyyy-MM-dd`
- `dateEndPromotion`: date string `yyyy-MM-dd`
- `isActivePromotion`: boolean (opcional, default `true`)

Formato de fecha:
- Se usa `LocalDate`.
- Formato esperado: `yyyy-MM-dd`.
- No se usa hora ni timezone en estos campos.

Response 201 ejemplo:

```json
{
  "uuid": "6f03af25-8da3-4258-b0b6-16e82fd417f0",
  "restaurantId": "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
  "title": "2x1 en saltenas",
  "description": "Solo de lunes a viernes",
  "percentDiscount": 25.00,
  "dateStartPromotion": "2026-04-20",
  "dateEndPromotion": "2026-04-30",
  "isActivePromotion": true
}
```

Response 403 ejemplo (owner sin permisos sobre el restaurante):

```json
{
  "timestamp": "2026-04-20T22:11:24.575Z",
  "status": 403,
  "error": "Forbidden",
  "message": "El owner no tiene permisos para crear promociones en este restaurante",
  "path": "/promotion/restaurant/<restaurantId>"
}
```

Response 400 ejemplo (fechas invalidas):

```json
{
  "timestamp": "2026-04-20T22:11:24.575Z",
  "status": 400,
  "error": "Bad Request",
  "message": "La fecha de fin no puede ser anterior a la fecha de inicio",
  "path": "/promotion/restaurant/<restaurantId>"
}
```

### 21) POST /chat

Descripcion:
Envia un mensaje al chatbot con IA (Mistral AI). Si no se envia conversationId, se crea una nueva conversacion con UUID. Si se envia un conversationId existente, se continua la conversacion.

Si se envian `latitude` y `longitude`, el chatbot consulta la base de datos y recomienda restaurantes reales dentro de un radio de 5 km de la ubicacion del usuario.

El chatbot utiliza un contexto estructurado (`context.json`) y un system prompt (`system_prompt.txt`) que definen su rol, reglas de comportamiento y dominio (restaurantes en Bolivia).

Request body ejemplo (nueva conversacion, sin ubicacion):

```json
{
  "message": "Hola, que restaurantes me recomiendas?"
}
```

Request body ejemplo (continuar conversacion, sin ubicacion):

```json
{
  "conversationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "message": "Y cuales tienen promociones?"
}
```

Request body ejemplo (con ubicacion del usuario):

```json
{
  "message": "Que restaurantes tengo cerca?",
  "latitude": -17.3935,
  "longitude": -66.1570
}
```

Request body ejemplo (continuar conversacion con ubicacion):

```json
{
  "conversationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "message": "Algun otro restaurante cerca de aqui?",
  "latitude": -17.3935,
  "longitude": -66.1570
}
```

Campos:
- `message` (string, requerido): Mensaje del usuario.
- `conversationId` (string, opcional): UUID de conversacion existente. Si no se envia, se crea una nueva.
- `latitude` (number, opcional): Latitud del usuario para recomendaciones cercanas.
- `longitude` (number, opcional): Longitud del usuario para recomendaciones cercanas.

Comportamiento con ubicacion:
- Si se envian `latitude` y `longitude`, el backend consulta los restaurantes no bloqueados de la BD.
- Calcula la distancia Haversine y filtra los que estan dentro de un radio de 5 km.
- Inyecta la lista de restaurantes cercanos como contexto al modelo de IA.
- La IA responde con restaurantes reales de la plataforma, incluyendo nombre, categoria y distancia.

Response 200 ejemplo:

```json
{
  "conversationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "reply": "¡Tienes buenas opciones cerca! 🍽️ Sabor Valluno (Comida Tipica) esta a 0.8 km y El Buen Gusto (Parrilla) a 1.2 km. ¡Miralos en el mapa!"
}
```

Response 400 ejemplo (mensaje vacio):

```json
{
  "timestamp": "2026-04-22T22:11:24.575Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Error de validacion",
  "path": "/chat",
  "validationErrors": {
    "message": "El mensaje no puede estar vacio"
  }
}
```

Response 502 ejemplo (error con IA):

```json
{
  "timestamp": "2026-04-22T22:11:24.575Z",
  "status": 502,
  "error": "Bad Gateway",
  "message": "Error al comunicarse con el modelo de IA: Connection refused",
  "path": "/chat"
}
```

### 22) GET /chat/{conversationId}

Descripcion:
Obtiene el historial completo de una conversacion por su UUID.

Path param:
- conversationId: UUID de la conversacion

Response 200 ejemplo:

```json
{
  "conversationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "createdAt": "2026-04-22T22:11:24.575Z",
  "messages": [
    {
      "role": "user",
      "content": "Hola, que restaurantes me recomiendas?",
      "timestamp": "2026-04-22T22:11:24.575Z"
    },
    {
      "role": "assistant",
      "content": "Te recomiendo visitar Sabor Valluno, tienen comida tipica cochabambina.",
      "timestamp": "2026-04-22T22:11:25.123Z"
    }
  ]
}
```

Response 404 ejemplo:

```json
{
  "timestamp": "2026-04-22T22:11:24.575Z",
  "status": 404,
  "error": "Not Found",
  "message": "No existe conversacion con id f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "path": "/chat/f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

### 23) GET /chat/conversations

Descripcion:
Lista un resumen de todas las conversaciones almacenadas.

Request:
- Body: no aplica
- Params: no aplica

Response 200 ejemplo:

```json
[
  {
    "conversationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "createdAt": "2026-04-22T22:11:24.575Z",
    "messageCount": 4,
    "preview": "Hola, que restaurantes me recomiendas?"
  }
]
```

## Sugerencia para Postman

Crear una Collection llamada Antojitos Maps y configurar una variable de coleccion:

- baseUrl = http://localhost:8080

Luego definir cada request como:

- {{baseUrl}}/restaurant/all
- {{baseUrl}}/restaurant/upload-image
- {{baseUrl}}/restaurant/create
- {{baseUrl}}/restaurant/get/{{restaurantId}}
- {{baseUrl}}/restaurant/delete/{{restaurantId}}
- {{baseUrl}}/app/health
- {{baseUrl}}/app/health/db
- {{baseUrl}}/restaurant/login
- {{baseUrl}}/restaurant/registry
- {{baseUrl}}/restaurant/logout
- {{baseUrl}}/admin/login
- {{baseUrl}}/admin/create
- {{baseUrl}}/admin/edit
- {{baseUrl}}/admin/delete/{{adminId}}
- {{baseUrl}}/admin/all
- {{baseUrl}}/admin/deleted
- {{baseUrl}}/admin/restaurants
- {{baseUrl}}/admin/restaurants/{{restaurantId}}/block
- {{baseUrl}}/promotion/restaurant/{{restaurantId}}
- {{baseUrl}}/chat
- {{baseUrl}}/chat/{{conversationId}}
- {{baseUrl}}/chat/conversations

