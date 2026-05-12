# POSTMAN Guide - Antojitos Maps Backend

Base URL local: `http://localhost:8080`
Base URL producción: `https://antojitos-maps.herokuapp.com`

---

## Modelo de autenticación

> A partir de la versión actual **el frontend NO usa el SDK de Firebase**. El backend gestiona toda la integración con Firebase internamente. Los endpoints solo requieren `email` y `password` en texto plano.

### Headers de sesión por rol

| Rol | Header | Dónde se obtiene |
|---|---|---|
| Cliente | `X-Client-Id: <uuid>` | Campo `uuid` en respuesta de `/client/login` o `/client/registry` |
| Admin | `X-Admin-Id: <uuid>` | Campo `adminId` en respuesta de `/admin/login` |
| Owner | *(sin header, usa `ownerUuid` en body)* | Campo `ownerId` en respuesta de `/restaurant/login` |

---

## Endpoints disponibles

### 1) GET /restaurant/all

Descripcion: Obtiene la lista de restaurantes no eliminados.

Request: no aplica

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

---

### 2) POST /restaurant/upload-image

Descripcion: Sube una imagen del restaurante a Cloudflare R2 y devuelve la URL publica.

Request:
- Content-Type: `multipart/form-data`
- Campo `file`: archivo de imagen (max 5 MB)
- Campo `name` (opcional): nombre del restaurante para el slug del archivo

Response 200:

```json
{
  "imageUrl": "https://<account-id>.r2.cloudflarestorage.com/<bucket>/restaurantes/nombre-uuid.jpg"
}
```

---

### 3) POST /restaurant/create

Descripcion: Crea un restaurante asociado a un owner ya registrado.

Request body:

```json
{
  "ownerMail": "owner@ejemplo.com",
  "name": "Nuevo Antojito",
  "latitude": -17.4,
  "longitude": -66.1,
  "planSuscription": "BASIC",
  "planExpirationDate": "2027-12-31",
  "isBlocked": false,
  "description": "Descripcion del restaurante",
  "imagenUrl": "https://.../restaurantes/nuevo-antojito.jpg",
  "category": "Comida Rapida"
}
```

Response 201:

```json
{
  "uuid": "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
  "name": "Nuevo Antojito",
  "description": "Descripcion del restaurante",
  "imagenUrl": "https://.../restaurantes/nuevo-antojito.jpg",
  "planSuscription": "BASIC",
  "planExpirationDate": "2027-12-31",
  "isBlocked": false,
  "latitude": -17.4,
  "longitude": -66.1,
  "category": "Comida Rapida"
}
```

---

### 4) GET /restaurant/get/{id}

Descripcion: Obtiene un restaurante por UUID.

Response 200: mismo schema que en `/restaurant/all`.

---

### 5) DELETE /restaurant/delete/{id}

Descripcion: Elimina un restaurante por UUID.

Response 204: sin body.

---

### 6) GET /app/health

Response 200:

```json
{ "status": "UP", "timestamp": "2026-05-12T19:00:00.000Z" }
```

---

### 7) GET /app/health/db

Response 200:

```json
{
  "timestamp": "2026-05-12T19:00:00.000Z",
  "status": "UP",
  "databaseProduct": "PostgreSQL"
}
```

---

### 8) POST /restaurant/login

Descripcion: Autentica un owner con email y password. El backend verifica las credenciales contra Firebase y devuelve los datos del owner con sus restaurantes.

> **Cambio:** Ya no se envía `idToken` de Firebase. El frontend solo envía email y password.

Request body:

```json
{
  "email": "owner@ejemplo.com",
  "password": "Password123!"
}
```

Response 200:

```json
{
  "ownerId": "20a63174-3799-4e7f-98c7-7f2af9e2c42c",
  "mail": "owner@ejemplo.com",
  "restaurantIds": [
    "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9"
  ],
  "message": "login correcto"
}
```

Response 401: credenciales incorrectas o owner no registrado.

---

### 9) POST /restaurant/registry

Descripcion: Registra un owner. El backend crea el usuario en Firebase y lo guarda en la BD.

> **Cambio:** Ya no se envía `idToken`. El frontend solo envía email y password.

Request body:

```json
{
  "email": "nuevo.owner@ejemplo.com",
  "password": "Password123!"
}
```

Response 201:

```json
{ "message": "owner registrado" }
```

---

### 10) POST /restaurant/logout

Request body:

```json
{ "mail": "owner@ejemplo.com" }
```

Response 200:

```json
{ "message": "logout registrado" }
```

---

### 11) POST /admin/login

Descripcion: Autentica un administrador con email y password. Firebase gestionado por el backend.

> **Cambio:** Ya no se envía `idToken`. El frontend solo envía email y password.

Request body:

```json
{
  "email": "admin@antojitos.com",
  "password": "Admin2026!"
}
```

Response 200:

```json
{
  "adminId": "f792617d-0d5d-4881-b5f6-679bcf2c37f8",
  "mail": "admin@antojitos.com",
  "message": "login correcto"
}
```

---

### 12) POST /admin/create

Descripcion: Crea un nuevo administrador. El backend crea el usuario en Firebase y lo registra en la BD.

- Si **no existe ningún admin** en el sistema: funciona sin header (bootstrap inicial).
- Si **ya existe al menos un admin**: requiere header `X-Admin-Id` de un admin activo.

Headers (requerido salvo bootstrap):

```
X-Admin-Id: f792617d-0d5d-4881-b5f6-679bcf2c37f8
```

Request body:

```json
{
  "mail": "nuevo.admin@antojitos.com",
  "password": "NuevoAdmin2026!"
}
```

Response 201:

```json
{
  "adminId": "a1b2c3d4-...",
  "mail": "nuevo.admin@antojitos.com"
}
```

---

### 13) PUT /admin/edit

Headers: `X-Admin-Id: <uuid>`

Request body:

```json
{
  "mail": "admin.editado@antojitos.com",
  "password": "AdminEditado2026!"
}
```

---

### 14) DELETE /admin/delete/{id}

Headers: `X-Admin-Id: <uuid>`

Path param: `id` = UUID del admin a eliminar logicamente.

---

### 15) GET /admin/all

Lista administradores activos. Sin headers.

---

### 16) GET /admin/deleted

Lista administradores eliminados logicamente.

---

### 17) GET /admin/restaurants

Headers: `X-Admin-Id: <uuid>`

Lista todos los restaurantes para moderacion.

---

### 18) PATCH /admin/restaurants/{id}/block

Headers: `X-Admin-Id: <uuid>`

Request body:

```json
{ "isBlocked": true }
```

---

### 19) GET /promotion/restaurant/{restaurantId}

Obtiene promociones activas de un restaurante. Sin autenticacion.

Response 200:

```json
[
  {
    "uuid": "6f03af25-8da3-4258-b0b6-16e82fd417f0",
    "restaurantId": "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
    "title": "2x1 en saltenas",
    "description": "Solo de lunes a viernes",
    "percentDiscount": 25.00,
    "dateStartPromotion": "2026-05-01",
    "dateEndPromotion": "2026-08-31",
    "isActivePromotion": true
  }
]
```

---

### 20) POST /promotion/restaurant/{restaurantId}

Crea una promocion para un restaurante. Requiere `ownerUuid` (preferido) u `ownerMail` en el body para validar permisos.

Request body:

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

Tipos de campos:
- `ownerUuid` / `ownerMail`: identifica al owner (usa `ownerUuid` preferentemente)
- `percentDiscount`: number entre 0 y 100
- `dateStartPromotion` / `dateEndPromotion`: formato `yyyy-MM-dd`
- `isActivePromotion`: boolean (default `true`)

Response 201: mismo schema que GET promotion.

---

### 21) POST /client/registry

Descripcion: Registra un nuevo cliente. El backend crea el usuario en Firebase y lo guarda en la BD.

> **Cambio:** No requiere Firebase SDK en el frontend. Solo email, password y datos del perfil.

Request body:

```json
{
  "email": "cliente@ejemplo.com",
  "password": "Cliente123!",
  "fullName": "Maria Lopez",
  "phone": "71234567"
}
```

Response 201:

```json
{
  "uuid": "f36d21a9-c6f5-4f7e-9ca6-fdb3e491068d",
  "mail": "cliente@ejemplo.com",
  "fullName": "Maria Lopez",
  "phone": "71234567",
  "message": "cliente registrado"
}
```

> Guardar el `uuid` como `X-Client-Id` para usar en chat y quejas.

---

### 22) POST /client/login

Descripcion: Autentica un cliente con email y password.

Request body:

```json
{
  "email": "cliente@ejemplo.com",
  "password": "Cliente123!"
}
```

Response 200: mismo schema que registry.

---

### 23) POST /client/logout

Request body:

```json
{ "mail": "cliente@ejemplo.com" }
```

---

### 24) POST /chat

Descripcion: Envia un mensaje al chatbot con IA (Mistral AI). Las conversaciones se persisten en MongoDB vinculadas al `X-Client-Id`.

Headers (opcional para usuarios registrados):

```
X-Client-Id: f36d21a9-c6f5-4f7e-9ca6-fdb3e491068d
```

Request body (nueva conversacion, con ubicacion):

```json
{
  "message": "Que restaurantes hay cerca?",
  "latitude": -17.3935,
  "longitude": -66.1570
}
```

Request body (continuar conversacion existente):

```json
{
  "conversationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "message": "Y cuales tienen promociones?"
}
```

Campos:
- `message` (string, **requerido**): Mensaje del usuario
- `conversationId` (string, opcional): UUID de conversacion existente. Si no se envia, se crea una nueva
- `latitude` / `longitude` (number, opcionales): coordenadas para recomendaciones de restaurantes cercanos (radio 5 km)

Response 200:

```json
{
  "conversationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "reply": "Tienes 3 restaurantes cerca! Sabor Valluno a 0.8 km..."
}
```

---

### 25) GET /chat/history

Descripcion: Obtiene el historial completo de la conversacion del cliente autenticado (persistida en MongoDB).

> **Nota:** El endpoint es `/chat/history` con header `X-Client-Id`, NO `/chat/{conversationId}`.

Headers (requerido):

```
X-Client-Id: f36d21a9-c6f5-4f7e-9ca6-fdb3e491068d
```

Response 200:

```json
{
  "conversationId": "f36d21a9-c6f5-4f7e-9ca6-fdb3e491068d",
  "createdAt": "2026-05-12T19:00:00.000Z",
  "messages": [
    {
      "role": "user",
      "content": "Que restaurantes hay cerca?",
      "timestamp": "2026-05-12T19:00:00.000Z"
    },
    {
      "role": "assistant",
      "content": "Tienes 3 restaurantes cerca!",
      "timestamp": "2026-05-12T19:00:01.000Z"
    }
  ]
}
```

---

### 26) GET /chat/conversations

Descripcion: Lista un resumen de todas las conversaciones almacenadas en MongoDB.

Response 200:

```json
[
  {
    "conversationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "createdAt": "2026-05-12T19:00:00.000Z",
    "messageCount": 4,
    "preview": "Que restaurantes hay cerca?"
  }
]
```

---

### 27) POST /complaint/create

Descripcion: Crea una queja sobre un restaurante o una promocion. Requiere que el cliente este autenticado.

Headers (requerido):

```
X-Client-Id: f36d21a9-c6f5-4f7e-9ca6-fdb3e491068d
```

Request body (queja de restaurante):

```json
{
  "type": "RESTAURANT",
  "targetUuid": "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
  "description": "El restaurante no vende la comida que anuncia."
}
```

Request body (queja de promocion):

```json
{
  "type": "PROMOTION",
  "targetUuid": "6f03af25-8da3-4258-b0b6-16e82fd417f0",
  "description": "La promocion ya no es valida pero sigue en la app."
}
```

Response 201:

```json
{
  "uuid": "c3e5d321-5fa1-4a4b-9370-0d9f8cfa8ca9",
  "type": "RESTAURANT",
  "targetUuid": "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
  "description": "El restaurante no vende la comida que anuncia.",
  "status": "PENDING",
  "createdAt": "2026-05-12T19:00:00.000"
}
```

---

### 28) GET /complaint/admin/all

Descripcion: Obtiene todas las quejas registradas.

Headers: `X-Admin-Id: <uuid>`

Response 200: lista de quejas (mismo schema que create response).

---

### 29) GET /complaint/admin/pending

Descripcion: Obtiene unicamente las quejas con estado `PENDING`.

Headers: `X-Admin-Id: <uuid>`

---

### 30) POST /complaint/admin/review/{id}

Descripcion: Permite a un administrador aceptar o rechazar una queja.

- Si `status = "ACCEPTED"`: el restaurante o promocion objetivo es **borrado logicamente** de forma automatica.
- Si `status = "REJECTED"`: la queja queda marcada como rechazada sin efecto adicional.

Headers: `X-Admin-Id: <uuid>`

Path param: `id` = UUID de la queja

Request body:

```json
{ "status": "REJECTED" }
```

o

```json
{ "status": "ACCEPTED" }
```

Response 200:

```json
{
  "uuid": "c3e5d321-5fa1-4a4b-9370-0d9f8cfa8ca9",
  "type": "RESTAURANT",
  "targetUuid": "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9",
  "description": "El restaurante no vende la comida que anuncia.",
  "status": "REJECTED",
  "createdAt": "2026-05-12T19:00:00.000"
}
```

---

## Flujo de prueba rápida (orden recomendado)

```
1. POST /restaurant/registry          → crear owner
2. POST /restaurant/create            → crear restaurante con ownerMail
3. POST /restaurant/login             → obtener ownerId
4. POST /promotion/restaurant/{id}    → crear promocion con ownerUuid
5. POST /admin/create                 → bootstrap primer admin
6. POST /admin/login                  → obtener adminId
7. POST /admin/create + X-Admin-Id    → crear segundo admin
8. POST /client/registry              → crear cliente (guardar uuid)
9. POST /client/login                 → confirmar login
10. POST /chat + X-Client-Id          → chatear (con lat/lng para cercanos)
11. GET  /chat/history + X-Client-Id  → verificar persistencia
12. POST /complaint/create + X-Client-Id → crear queja
13. POST /complaint/admin/review/{id} + X-Admin-Id → rechazar/aceptar
```
