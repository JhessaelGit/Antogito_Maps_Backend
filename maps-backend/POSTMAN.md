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
Valida credenciales en owner_account.

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
