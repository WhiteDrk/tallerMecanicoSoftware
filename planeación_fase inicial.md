# Planeación — Fase inicial

**Proyecto:** MotorDesk — Sistema de gestión para taller mecánico  
**Fecha de auditoría:** 24 de septiembre de 2026  
**Alcance revisado:** código fuente, configuración de contenedores y documentación presentes en el repositorio local.

## 1. Dictamen ejecutivo

La primera fase entregó una **base técnica y un prototipo visual**. La infraestructura declarativa, el modelo inicial de identidad y la migración de datos existen; sin embargo, los flujos de negocio y los controles de seguridad de ejecución no están implementados de extremo a extremo.

**Decisión de auditoría:** clasificar el proyecto como **Fase 1 en construcción**. Puede continuar hacia la fase funcional, pero no debe exponerse a usuarios reales ni tratar datos personales/financieros hasta cerrar los bloqueos de seguridad indicados en este documento.

## 2. Inventario por fase, módulo y estado

| Fase | Módulo | Datos/artefactos identificados | Estado | Decisión |
|---|---|---|---|---|
| 1A | Base de infraestructura | `docker-compose.yml`, MySQL 8.4, servicio API, Nginx, red `private`, volumen `mysql_data` | Parcial | Aprobado como definición de desarrollo; pendiente de levantar y verificar los tres contenedores. |
| 1B | Configuración de secretos | `.env.example`, variables `MYSQL_*`, `JWT_SECRET`, `WEB_PORT` | Parcial | Aprobado únicamente como plantilla. Crear `.env` local con secretos reales; nunca versionarlo. |
| 1C | Backend | Spring Boot 3.5.6, Java 26, JPA, Flyway, validación y Spring Security | Parcial | Aprobado como esqueleto. Falta compilar, pruebas automatizadas y endpoints de dominio. |
| 1D | Identidad y roles | Tablas `users`, `roles`, `user_roles`; ocho roles preconfigurados | Parcial | Modelo inicial aprobado. Falta servicio persistente de usuarios, autorización granular y administración de roles. |
| 1E | Autenticación | Rutas `/register`, `/login`, `/password-recovery`; BCrypt con factor 12 configurado | No terminado | No habilitar en producción: no guarda hash, no autentica contra base de datos, no emite JWT válido, no existe MFA ni recuperación por correo. |
| 1F | Auditoría | Tabla `audit_events` con actor, acción, entidad, IP hasheada y JSON de detalle | Parcial | Estructura aprobada. Falta capturar eventos desde la aplicación, control de integridad, consultas y retención. |
| 1G | Órdenes de trabajo | Regla de autorización `GET /api/v1/orders/**`; datos ficticios en interfaz | No iniciado | Definir máquina de estados, entidades, permisos por transición, evidencia y aprobación de Gerencia. |
| 1H | Inventario, proveedores y presupuesto | Solo opciones de navegación y métricas de demostración | No iniciado | Diseñar entidades, movimientos, políticas de ajuste, compras, cuentas y autorizaciones. |
| 1I | Frontend | Vue 3, PrimeVue, Vite, pantalla de acceso y tablero responsive; `preview.html` | Parcial | Aprobado como prototipo visual. Conectar a API real, manejar sesiones, validación, errores, accesibilidad y vistas por rol. |
| 1J | Proxy web | Nginx con proxy `/api/` hacia API y cabeceras básicas | Parcial | Mantener; agregar TLS, HSTS, CSP, rate limiting, límites de cuerpo y observabilidad antes de producción. |

## 3. Roles registrados en la base de datos

| Código | Rol de negocio |
|---|---|
| `OWNER` | Dueño |
| `MANAGER` | Gerente |
| `TREASURY` | Tesorería |
| `MECHANIC_CHIEF` | Jefe de mecánicos |
| `MECHANIC` | Mecánico |
| `CUSTOMER_SERVICE` | Atención a cliente |
| `CUSTOMER` | Cliente |
| `AUDITOR` | Auditor |

**Observación:** la lista de roles existe, pero todavía no representa una matriz completa de permisos. La única restricción declarada cubre lectura general de órdenes y acceso a auditoría para Dueño/Auditor. No hay controles implementados para crear, actualizar, aprobar, asignar ni cancelar operaciones.

## 4. Documentación objetiva del código generado

### Raíz e infraestructura

- `.gitignore`: excluye `.env`, dependencias y artefactos de compilación del control de versiones.
- `.env.example`: plantilla de variables de MySQL, clave JWT y puerto web; contiene valores de ejemplo, no credenciales operativas.
- `docker-compose.yml`: declara contenedores `mysql`, `api` y `web`; MySQL no publica puerto al host, Nginx publica el puerto web y todos comparten la red interna `private`.
- `README.md`: instrucciones de ejecución local y advertencias de seguridad pendientes.

### Backend (`backend/`)

- `pom.xml`: dependencias Spring Web, Security, JPA, Validation, Flyway/MySQL y pruebas; Java configurado en versión 26.
- `Dockerfile`: compilación multi-etapa Maven y ejecución como usuario no privilegiado `app` en un JRE 26.
- `src/main/java/mx/taller/TallerApplication.java`: punto de entrada de Spring Boot.
- `src/main/java/mx/taller/config/SecurityConfig.java`: seguridad sin sesión (stateless), BCrypt con coste 12 y reglas de rutas. No contiene filtro JWT ni `AuthenticationProvider`; por tanto no protege solicitudes con token aún.
- `src/main/java/mx/taller/auth/AuthController.java`: define contratos HTTP de registro, inicio de sesión y recuperación. Sus respuestas son de demostración; no persiste ni verifica usuarios.
- `src/main/resources/application.yml`: origen de datos por variables de entorno, JPA en modo `validate`, Flyway habilitado y secreto JWT por variable de entorno.
- `src/main/resources/db/migration/V1__identity_and_audit.sql`: crea tablas de roles, usuarios, relación usuario-rol y eventos de auditoría; inserta los ocho roles iniciales.

### Frontend (`frontend/`)

- `package.json` y `vite.config.js`: configuración del frontend Vue/Vite y dependencias de PrimeVue.
- `src/main.js`: arranca Vue e inicializa el tema Aura de PrimeVue.
- `src/App.vue`: prototipo de inicio de sesión, recuperación visual, tablero de Gerencia, filtros y diálogo con seguimiento de órdenes. Los datos de órdenes están definidos en memoria y no viajan a la API.
- `src/style.css`: estilos base de la aplicación.
- `index.html`: documento de entrada de Vite y carga de Tailwind por CDN para el prototipo.
- `preview.html`: demostración HTML autónoma, responsive y ejecutable sin compilación; sus credenciales y métricas son ficticias.
- `nginx.conf`: sirve archivos estáticos, reenvía `/api/` a `api:8080` y agrega tres cabeceras HTTP básicas.
- `Dockerfile`: construye el frontend con Node y lo sirve con Nginx.

## 5. Credenciales para conexión a base de datos

No existe un archivo `.env` con credenciales reales en el repositorio auditado. La **plantilla** se encuentra en `.env.example` y define:

- Host desde contenedor: `mysql`
- Puerto interno: `3306`
- Base por defecto: `taller_db`
- Usuario por defecto: `taller_app`
- Contraseñas: deben establecerse en `MYSQL_PASSWORD` y `MYSQL_ROOT_PASSWORD` dentro de un archivo local `.env`.

**Instrucciones seguras:** copie `.env.example` a `.env`, reemplace todos los valores de ejemplo por secretos únicos y mantenga `.env` fuera de Git. Para un cliente local como DBeaver, use host `localhost` solo después de publicar explícitamente un puerto de MySQL para entorno de desarrollo; la configuración actual no expone MySQL, lo cual es correcto por defecto.

## 6. Hallazgos y decisiones de seguridad

| Prioridad | Hallazgo | Riesgo | Decisión requerida |
|---|---|---|---|
| Crítica | `/login` devuelve texto marcador en lugar de JWT firmado; no existe autenticación real. | Acceso no confiable. | Implementar usuarios, hash BCrypt al registro, verificación de contraseña, JWT corto con refresh token rotativo y revocación. |
| Crítica | Registro y recuperación no persisten ni entregan enlaces seguros. | Cuentas sin ciclo de vida controlado. | Implementar tokens de un solo uso con expiración, correo transaccional y protección anti-enumeración. |
| Alta | La autorización no define permisos de escritura ni permisos por entidad. | Incumple mínimo privilegio. | Aprobar una matriz RBAC/ABAC por acción y estado de orden. |
| Alta | No existe evidencia de MFA, rate limiting, bloqueo por intentos, TLS o CSP. | Riesgo de toma de cuenta y ataques web. | Incluir estos controles antes de piloto externo. |
| Alta | Eventos de auditoría se crean en tabla pero no se registran desde servicios. | Trazabilidad incompleta. | Implementar interceptor/eventos de dominio y conservar evidencia de aprobación. |
| Media | No hay pruebas automatizadas ni pipeline CI. | Regresiones no detectadas. | Crear pruebas unitarias, integración MySQL/Testcontainers, SAST, análisis de dependencias y despliegue controlado. |
| Media | `frontend/verify-write` no pertenece al producto. | Artefacto técnico innecesario. | Eliminarlo antes del siguiente commit. |

## 7. Plan de continuidad recomendado

1. **Fase 2 — identidad segura:** entidades/repositorios, JWT real, refresh tokens, MFA, alta aprobada por Gerencia, recuperación y pruebas.
2. **Fase 3 — operación de órdenes:** cliente, vehículo, orden, catálogo de servicios, bitácora, adjuntos, flujo de estados y autorizaciones.
3. **Fase 4 — inventario y finanzas:** piezas, existencias, movimientos, proveedores, presupuestos, pagos y reglas de separación de funciones.
4. **Fase 5 — auditoría y calidad:** matriz de riesgos, controles ISO 27001 aplicables, logs centralizados, respaldos, monitoreo, pruebas y procedimiento de incidentes.
5. **Fase 6 — piloto:** datos de prueba, aceptación por rol, prueba móvil, análisis de vulnerabilidades, respaldo/restauración y liberación controlada.

## 8. Publicación recomendada

### Opción recomendada para piloto: Railway o Render

1. Subir el repositorio a GitHub.
2. Crear una base **MySQL administrada** en Railway, Aiven o DigitalOcean Managed Databases; no use el contenedor de desarrollo como base productiva.
3. Crear un servicio backend desde la carpeta `backend/`, con build `mvn -DskipTests package` y arranque `java -jar target/taller-api-0.1.0.jar`.
4. Cargar secretos como variables del proveedor: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` y `APP_JWT_SECRET`. No subir `.env`.
5. Crear un servicio frontend desde `frontend/`: `npm ci && npm run build`; publicar `dist/`. En Vercel, Netlify o Cloudflare Pages configure la variable/reescritura de `/api` hacia la URL HTTPS del backend.
6. Configurar un dominio propio, TLS administrado, redirección HTTPS, cabeceras de seguridad, copias de seguridad y alertas antes de abrir el sistema a clientes.

### Opción de infraestructura con contenedores: VPS + Docker Compose

Use una VPS de DigitalOcean, Hetzner, AWS EC2 o Azure VM con Docker Engine y Docker Compose. Copie el repositorio, cree `.env`, configure Nginx con certificados Let's Encrypt y ejecute `docker compose up -d --build`. Esta opción exige que el equipo opere parches, respaldos, firewall, monitoreo y recuperación; por ello no se recomienda para producción sin responsable de infraestructura.

### Restricción de GitHub Pages

GitHub Pages sirve únicamente contenido estático: puede publicar el frontend compilado, pero **no ejecuta Spring Boot ni MySQL**. Para este proyecto úselo solo como demostración visual estática, no como alojamiento integral.

## 9. Criterio de cierre de fase inicial

La fase inicial se cerrará cuando el equipo apruebe la matriz de permisos y los flujos de órdenes, el proyecto compile de forma reproducible, los contenedores levanten correctamente con secretos locales, y queden registradas las decisiones de seguridad pendientes. Hasta entonces, su estado permanece **parcial / no apto para producción**.
