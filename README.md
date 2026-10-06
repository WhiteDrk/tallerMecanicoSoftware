# MotorDesk · Gestión para taller mecánico

Base de una plataforma móvil para órdenes de trabajo, con Vue 3 + PrimeVue, Spring Boot 3/Java 26, MySQL, Nginx y Docker Compose.

> El contenedor ejecuta JDK 26. Mientras Spring Boot 3.5 no pueda empaquetar clases con bytecode Java 26, Maven genera bytecode Java 25 compatible; no se usan características de lenguaje que requieran una versión superior.

## Ejecutar el diseño localmente

```bash
cd frontend
npm install
npm run dev -- --port 5173
```

Abre `http://localhost:5173`. Ingresa cualquier correo y contraseña para recorrer el diseño de Gerencia.

## Contenedores

1. Copia `.env.example` como `.env` y reemplaza todos los secretos.
2. Ejecuta `docker compose up --build`.

Genera la clave JWT con `openssl rand -base64 32`. El servicio no arranca si falta la clave o si no tiene al menos 256 bits en Base64.

La base no se publica al host; solamente Nginx expone el puerto web. Antes de producción faltan integrar proveedor de correo, JWT rotativos, MFA, rate limiting y persistencia real de usuarios/órdenes.
