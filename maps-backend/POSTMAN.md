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

### 2) POST /restaurant/create

Descripcion:
Crea un restaurante asociado a un owner registrado.

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

### 3) GET /restaurant/get/{id}

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

### 4) DELETE /restaurant/delete/{id}

Descripcion:
Elimina un restaurante por UUID.

Path param:
- id: UUID del restaurante

Response 204:
- sin body

### 5) GET /app/health

Descripcion:
Estado general del backend.

Response 200 ejemplo:

```json
{
  "status": "UP",
  "timestamp": "2026-04-10T22:11:24.575Z"
}
```

### 6) GET /app/health/db

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

### 7) POST /restaurant/login

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

### 8) POST /restaurant/registry

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

### 9) POST /restaurant/logout

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

## Sugerencia para Postman

Crear una Collection llamada Antojitos Maps y configurar una variable de coleccion:

- baseUrl = http://localhost:8080

Luego definir cada request como:

- {{baseUrl}}/restaurant/all
- {{baseUrl}}/restaurant/create
- {{baseUrl}}/restaurant/get/{{restaurantId}}
- {{baseUrl}}/restaurant/delete/{{restaurantId}}
- {{baseUrl}}/app/health
- {{baseUrl}}/app/health/db
- {{baseUrl}}/restaurant/login
- {{baseUrl}}/restaurant/registry
- {{baseUrl}}/restaurant/logout
