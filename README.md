# MotorDesk · Gestión para taller mecánico

Base de una plataforma móvil para órdenes de trabajo, con Vue 3 + PrimeVue, Spring Boot 3/Java 26, MySQL, Nginx y Docker Compose.

> El contenedor ejecuta JDK 26. Mientras Spring Boot 3.5 no pueda empaquetar clases con bytecode Java 26, Maven genera bytecode Java 25 compatible; no se usan características de lenguaje que requieran una versión superior.

## Ejecutar el diseño localmente

```bash
cd frontend
npm install
npm run dev -- --port 5173
```

Abre `http://localhost:5173` para desarrollo visual. La aplicación requiere una cuenta real habilitada; ya no acepta credenciales arbitrarias. Para los flujos completos de login y registro, usa `http://localhost/` con los contenedores activos: Nginx sirve el frontend y dirige `/api` al backend.

## Contenedores

1. Copia `.env.example` como `.env` y reemplaza todos los secretos.
2. Ejecuta `docker compose up --build`.

Genera la clave JWT con `openssl rand -base64 32`. El servicio no arranca si falta la clave o si no tiene al menos 256 bits en Base64.

La base no se publica al host; solamente Nginx expone el puerto web. Usuarios, talleres, clientes, vehículos y auditoría se persisten en MySQL. El panel de órdenes y cifras es una demostración identificada como tal. Recuperación real por correo y otros procesos operativos siguen pendientes.

## Revisión actual

### Demostración temporal en Railway

La configuración y los pasos para publicar `web`, `api` y MySQL están en
[Despliegue_Railway.md](docs/Despliegue_Railway.md). El frontend de nube usa
`frontend/Dockerfile.railway`; el backend admite el puerto `PORT` y expone
`/actuator/health`. Los datos y las cuentas se preparan por separado y no se
publican en GitHub.

### Consulta de vehículos

En `http://localhost/`, ingresa como Administración o Recepción y abre **Clientes → Consultar vehículo**. Busca por uno de VIN, placa o UUID del vehículo; no necesitas seleccionar cliente. Ejemplo local: VIN `3KPF24AD3R2610131` o placa `UI-2613`. La API filtra por talleres autorizados y no devuelve datos personales del cliente.

La implementación y sus pruebas están en [docs/Prueba_Consulta_Vehiculo.md](docs/Prueba_Consulta_Vehiculo.md). Para repetir la verificación de API, define `QA_PASSWORD` en el entorno y ejecuta `node scripts/verify-vehicle-query.mjs`.

El rediseño y los resultados de la verificación del 13 de octubre de 2026 están documentados en [Revision_Integral_Redisenio.md](Revision_Integral_Redisenio.md). De 77 verificaciones REST iniciales, 75 pasaron. Posteriormente se corrigió y verificó el acceso a Estatus para Administración/Gerencia; sigue pendiente la respuesta correcta al exceso de tamaño de foto de taller. No se declara el sistema completo ni listo para producción.

Las pruebas reproducibles están en `scripts/verify-local.mjs` y `scripts/verify-workshops.mjs`. Requieren `QA_PASSWORD` por variable de entorno, se limitan a localhost y crean registros ficticios persistentes de QA.
