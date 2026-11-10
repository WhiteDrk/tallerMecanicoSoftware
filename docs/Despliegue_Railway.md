# MotorDesk en Railway: demostración temporal

Este despliegue usa tres servicios en un mismo proyecto: `MySQL`, `api` y `web`.
La única dirección pública necesaria es la de `web`. Las solicitudes `/api/`
se envían desde Nginx al backend por la red privada de Railway.

## 1. Base de datos

En el proyecto de Railway: **New → Database → MySQL**. Conserva el nombre
`MySQL` para que coincidan las referencias de variables de esta guía.
Verifica que el servicio tenga un volumen persistente.

## 2. Backend (`api`)

Agrega un servicio desde el repositorio de GitHub y selecciona la rama
`codex/railway-demo`. Configura:

- Nombre del servicio: `api`.
- Root Directory: `/backend`.
- Dockerfile: `Dockerfile` (detección automática).
- Healthcheck Path: `/actuator/health`.
- Healthcheck Timeout: `180` segundos.
- Sin dominio público.

En **Variables → Raw Editor**, agrega:

```dotenv
PORT=8080
SERVER_ADDRESS=::
SERVER_FORWARD_HEADERS_STRATEGY=framework
SPRING_DATASOURCE_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}
SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
APP_JWT_SECRET=REEMPLAZAR_POR_UN_SECRETO_NUEVO
```

Genera el secreto en tu terminal con `openssl rand -base64 32` y colócalo
solamente en la variable `APP_JWT_SECRET` de Railway. No lo guardes en GitHub.
El secreto de la nube debe ser distinto al local.

Espera a que MySQL esté listo antes del primer despliegue de `api`.
Flyway aplica las migraciones y el healthcheck comprueba la conexión a MySQL.

## 3. Frontend (`web`)

Agrega otro servicio desde el mismo repositorio y rama. Configura:

- Nombre del servicio: `web`.
- Root Directory: `/frontend`.
- Healthcheck Path: `/healthz`.

Variables:

```dotenv
RAILWAY_DOCKERFILE_PATH=Dockerfile.railway
PORT=8080
API_UPSTREAM=api.railway.internal:8080
```

En **Settings → Networking → Generate Domain**, selecciona el puerto `8080`.
Abre esa dirección HTTPS. El navegador usa el mismo origen para la web y la
API; no necesita conocer el host privado ni una URL de API diferente.

La plantilla Nginx usa el DNS del contenedor y vuelve a resolver el backend
periódicamente para admitir cambios de dirección durante los despliegues.
El backend reconoce los encabezados reenviados por Nginx para identificar el
origen HTTPS del navegador y permitir el login desde el dominio de la web.
El Dockerfile y la configuración Nginx locales siguen disponibles para Compose.

## 4. Datos y cuentas

Subir el código no sube la base local. Las migraciones crean estructura y
catálogos iniciales, pero no crean usuarios de administración ni los vehículos
locales. Por eso el login no estará listo hasta preparar las cuentas y los datos.

Para esta demostración se deben cargar datos ficticios de prueba y cuentas
personales con contraseñas diferentes a las de demostración. Las cuentas de
recepción necesitan su relación con los talleres autorizados. El registro
público de la aplicación crea clientes, no administradores ni recepcionistas.

No publiques respaldos SQL, tokens, contraseñas ni el archivo `.env`. Si se
decide importar un respaldo, se hará por un canal privado y preservando la
consistencia de los identificadores y del historial de Flyway; no se importará
un esquema completo encima de otro ya migrado sin revisar el procedimiento.

## 5. Verificar antes de la carga

1. `api` pasa su healthcheck y `web/healthz` responde `200`.
2. La página abre por HTTPS y permite iniciar sesión con la cuenta preparada.
3. La consulta de un vehículo de prueba con token devuelve `200`.
4. La misma consulta sin token devuelve `401`.
5. Los datos se conservan al reiniciar el servicio de aplicación.

Después se harán pruebas acotadas desde la computadora local: Karate para
concurrencia y validaciones; Burp Community para inspección y manipulación
manual de solicitudes. Los scripts Karate actuales están fijados a localhost:
hay que preparar expresamente el destino de nube antes de usarlos allí.
Registrar número de solicitudes, paralelismo, duración, p95, errores y métricas
de Railway. El paralelismo del ejecutor no equivale a usuarios simultáneos.
Comenzar con un lote pequeño y observar también el consumo del crédito.

## 6. Terminar la demostración

Guardar los resultados y cualquier dato que se quiera conservar. Al terminar,
eliminar los servicios y el volumen del proyecto temporal en Railway. Esto
elimina los datos de nube de forma permanente; el proyecto y la base locales
son independientes. No basta con quitar el dominio público para detener el uso.

## Referencias

- https://docs.railway.com/builds/dockerfiles
- https://railway.com/guides/docker-compose
- https://docs.railway.com/databases/mysql
- https://docs.railway.com/pricing/free-trial
