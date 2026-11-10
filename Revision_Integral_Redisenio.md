# MotorDesk — Rediseño y revisión integral

Fecha de revisión: 13 de octubre de 2026 (America/Mexico_City).

## Dictamen

El rediseño está implementado y desplegado en `http://localhost/`. Los contenedores web/API están activos y MySQL está saludable. Flyway registra 15 migraciones exitosas y versión 15.

La aplicación **no está completamente terminada ni libre de fallos**. La verificación REST inicial ejecutó 77 comprobaciones: 75 aprobadas y 2 fallidas. Posteriormente se corrigió el acceso a Estatus para Administración/Gerencia y se volvió a probar ese módulo. El rechazo de fotografías de taller mayores a 15 MB sigue devolviendo un código equivocado. La recuperación por correo y los módulos operativos que sólo tienen diseños de ejemplo siguen pendientes. Los resultados iniciales de las tablas siguientes se conservan como evidencia histórica; la corrección posterior se detalla al final.

## Cambios de esta revisión

| Archivo | Cambio realizado | Estado |
|---|---|---|
| `frontend/src/App.vue` | Nuevo login y estructura del panel; menú móvil; correo/rol del token en lugar de identidad fija; cierre de sesión; recuperación visual de sesión vigente al recargar. | Completado |
| `frontend/src/App.vue` | Navegación y acciones visuales coherentes con los roles existentes; datos operativos de ejemplo identificados como DEMO; módulos sin implementación marcados como pendientes. | Completado |
| `frontend/src/components/VehicleBlueprint.vue` | Emblema SVG minimalista de llave mecánica y trazos circulares; sustituye la ilustración del automóvil por petición del usuario. Se reutiliza en login y panel. | Completado |
| `frontend/src/style.css` | Paleta grafito/naranja/verde, superficies, tipografía, formularios, tablas, animaciones discretas y reducción de movimiento. Clientes se presentan como tarjetas en móvil. | Completado |
| `frontend/src/style.css` | SweetAlert por encima de los modales de PrimeVue; corrige la confirmación oculta que impedía pulsar OK. | Completado y comprobado en móvil |
| `frontend/src/main.js` | Preset PrimeVue con colores del taller y apariencia clara explícita. | Completado |
| `frontend/src/components/ClientList.vue` | Contenedor responsivo, estado de carga, acciones visibles según rol y método para refrescar el listado tras alta. | Completado |
| `frontend/src/components/ClientRegistrationForm.vue` | Emite `registered`, reinicia los selectores de dirección y permite refrescar el listado tras registrar. | Completado |
| `frontend/src/components/StatusCatalogView.vue` | Distingue un error de carga de un catálogo vacío; conserva el error de permisos identificado. | Completado en la presentación |
| `frontend/package.json`, `frontend/package-lock.json`, `frontend/vite.config.js` | Integra Tailwind con Vite y compila los estilos localmente. Actualiza dependencia transitiva `source-map-js` a 1.2.2. | Completado |
| `frontend/index.html`, `frontend/public/favicon.svg` | Identidad y metadatos; retira el script CDN de Tailwind. | Completado |
| `frontend/nginx.conf` | CSP restringe scripts al origen local tras retirar el CDN. | Completado |
| `frontend/.dockerignore` | Excluye dependencias, builds previos y archivos de entorno del contexto web. Tailwind usa explícitamente `src` como fuente para una compilación reproducible. | Completado |
| `scripts/verify-local.mjs` | 60 verificaciones REST de autenticación, clientes, direcciones, vehículos y aislamiento. | 59 aprobadas / 1 fallida |
| `scripts/verify-workshops.mjs` | 17 verificaciones de talleres, fotografías, RFC, roles y baja lógica. | 16 aprobadas / 1 fallida |
| `Fase_Administracion_Clientes.md`, `Registrar_Vehiculo.md` | Actualización del estado y de las limitaciones confirmadas. | Completado |
| Backend / migraciones | Se inspeccionaron y probaron; en esta revisión no se modificó código backend ni se añadieron migraciones. | Sin cambios |

Las claims JWT se leen en el frontend exclusivamente para presentar identidad y controles. La autorización efectiva continúa en Spring Security, controladores y Facades. Se conserva Vue → cliente REST → Controller → Facade → Repository → MySQL.

## Resultados REST: clientes, identidad y vehículos

| Prueba | Resultado esperado | Resultado observado |
|---|---|---|
| Sin autenticación | 401 | Aprobado |
| Contraseña incorrecta | 401 | Aprobado |
| Login Administración / Recepción | 200 y token | Aprobado en ambas cuentas de demostración |
| Listado de talleres Administración | 200, al menos 3 talleres | Aprobado |
| Talleres disponibles de Recepción | Sólo Motor Centro | Aprobado |
| Administración de talleres por Recepción | 403 | Aprobado |
| Catálogo de estatus por Administración | 200 | **Fallido: 403** |
| Clientes de taller fuera del alcance de Recepción | 403 | Aprobado |
| Estados, municipios por estado, localidades por municipio | 200 y datos relacionados | Aprobado en las 3 consultas |
| Localidad obtiene CP | 200, CP de 5 dígitos | Aprobado |
| CP obtiene estado/municipio/localidades | 200, localidad relacionada | Aprobado |
| CP inexistente `00000` | 400 | Aprobado |
| Email inválido / teléfono alfabético / nacimiento futuro | 400 | Aprobado en las 3 pruebas |
| Dirección que no corresponde al catálogo | 400 | Aprobado |
| Foto tipo inválido / contenido falso JPEG / más de 7 MB | 400 | Aprobado en las 3 pruebas |
| Registro de cliente ficticio válido | 201 e ID | Aprobado |
| Paginación máxima 10 | 200 y hasta 10 registros en la respuesta | Aprobado |
| Tamaño 11 / página negativa | 400 | Aprobado en ambas pruebas |
| Cliente duplicado | 409 | Aprobado |
| Total de clientes antes/después del duplicado | Sin incremento | Aprobado |
| Ordenamiento de nombre ASC / DESC | 200 y orden esperado | Aprobado en ambas pruebas |
| Ordenamiento por estatus | 200 | Aprobado |
| Columna no permitida | Usa fallback `fullName` | Aprobado: no se usa la columna solicitada |
| Edición del cliente ficticio | 200 | Aprobado |
| Mover cliente a un taller ajeno con Recepción | 403 | Aprobado |
| Crear cuenta con intento de indicar rol administrativo | 201, sólo `ROLE_CUSTOMER` | Aprobado |
| Cuenta duplicada | 409 | Aprobado |
| Cliente consulta `/clients/me` | Sólo su perfil | Aprobado |
| Cliente consulta listado / registra clientes / consulta por ID administrativo | 403 | Aprobado en las 3 pruebas |
| Catálogo de marcas / modelos de KIA | 200 y datos | Aprobado en ambas consultas |
| VIN inválido / placa inválida / serie inválida | 400 | Aprobado en las 3 pruebas |
| Año futuro no permitido / kilometraje negativo | 400 | Aprobado en ambas pruebas |
| Modelo que no pertenece a la marca | 400 | Aprobado |
| Alta vehicular con rol CUSTOMER | 403 | Aprobado |
| Alta vehicular por Recepción para cliente ACTIVO | 201 e ID | Aprobado |
| VIN duplicado / placa duplicada normalizada / serie duplicada | 400 | Aprobado en las 3 pruebas |
| Excepción de marca/modelo manual | 201 | Aprobado |
| Marca manual pendiente en menú normal | No aparece | Aprobado |
| Suspensión de cliente ficticio | 204, sin eliminar | Aprobado |
| Lectura administrativa posterior | 200 y SUSPENDIDO | Aprobado |
| Segunda suspensión | 403 | Aprobado |
| Alta vehicular para cliente suspendido | 403 | Aprobado |
| `/clients/me` para cliente suspendido | 403 | Aprobado |

## Resultados REST: talleres

| Prueba | Esperado | Observado |
|---|---|---|
| Consultar gerentes con Administración | 200 | Aprobado |
| Consultar gerentes con Recepción | 403 | Aprobado |
| RFC con formato inválido | 400 | Aprobado |
| Gerente inexistente | 403 | Aprobado |
| Fotografía PNG falsa | 400 | Aprobado |
| Fotografía mayor a 15 MB | 413 | **Fallido: 401. El archivo se rechaza, pero la respuesta no explica el tamaño excedido.** |
| Crear taller con Recepción | 403 | Aprobado |
| Crear taller ficticio con PNG válido | 201 | Aprobado |
| RFC duplicado | 409 | Aprobado |
| Consultar taller por ID | 200 | Aprobado |
| Editar taller con Recepción | 403 | Aprobado |
| Editar taller ficticio con Administración | 200 | Aprobado |
| Desactivar taller con Recepción | 403 | Aprobado |
| Desactivar taller ficticio con Administración | 204 | Aprobado |
| Consultar taller después de desactivarlo | Registro existente e inactivo | Aprobado |
| Editar taller inactivo | 403 | Aprobado |
| Consultar clientes de taller inactivo | 403 | Aprobado |

## Compilación, seguridad de dependencias y pruebas visuales

| Verificación | Evidencia / estado |
|---|---|
| Maven `test` | Aprobado. La suite existente contiene **1 prueba** JWT; no equivale a cobertura completa del backend. |
| Vite local y build Docker | Aprobados. Continúa advertencia de bundle JavaScript mayor a 500 kB. |
| `npm audit --json` | 0 vulnerabilidades conocidas después de actualizar `source-map-js`. No equivale a certificación de seguridad. |
| Flyway/MySQL | 15 migraciones exitosas; versión 15; Hibernate inicia con validación de esquema. |
| Registro por Vue | Sofía Prueba Interfaz; CP 01000 precarga Ciudad de México/Álvaro Obregón y permite San Ángel; SweetAlert `Cliente registrado`. |
| Edición por Vue | Datos y dirección precargados correctamente para la cliente ficticia. |
| Alta vehicular por Vue | KIA/RIO 2024; SweetAlert `Vehículo asociado`; cierre con OK confirmado tras ajustar z-index. |
| Mobile | Panel y clientes revisados en 360×780 y 390×844; menú se abre/cierra y Escape lo cierra. Sin desbordamiento horizontal global; las tablas operativas de ejemplo permiten scroll interno. |
| Escritorio | Pantallas revisadas en 1280×900; listado de talleres y fallo de estatus comprobados por interfaz. |
| Identidad visual | Correo real de la sesión y etiqueta Recepción; ya no muestra Mariana Torres/Gerente. |
| Cuenta desde Login | Cuenta ficticia de Sofía creada y vinculada; SweetAlert Cuenta creada; navegación sin módulos administrativos. Las métricas y órdenes DEMO se muestran sólo a perfiles internos. |
| Caducidad de sesión | Al recargar con token vencido, vuelve al login y retira el token local. |
| Auditoría | Consultas de MySQL confirman CLIENT_REGISTERED, CLIENT_UPDATED, CUSTOMER_ACCOUNT_REGISTERED, CLIENT_SUSPENDED y persistencia del vehículo de interfaz. |

## Hallazgos y limitaciones pendientes

| Prioridad | Hallazgo | Archivo / evidencia | Seguimiento |
|---|---|---|---|
| Resuelto | Estatus devolvía 403 incluso a Administración. | Faltaban reglas HTTP; además el controlador y menú sólo reconocían ADMINISTRADOR. | Corregido para ADMINISTRADOR/MANAGER por método y en las tres capas; consulta/alta/edición y protección verificadas. |
| Media | Fotografía de taller >15 MB devuelve 401. | Prueba multipart de 15 MB + 1 byte. `ApiExceptionHandler` no tiene tratamiento explícito de exceso multipart; posible respuesta de error interceptada por seguridad, pendiente de diagnóstico adicional. | Emitir error de tamaño coherente sin ampliar permisos. No corregido. |
| Alta | Alta pública de cuenta prueba coincidencia de nombre/email, pero no propiedad del correo. | `CustomerAccountRegistrationFacade.java` habilita cuenta y emite token directamente. | Definir y autorizar verificación de identidad/correo antes de producción. |
| Media | Al editar un cliente vinculado, no se sincroniza email/nombre del usuario asociado. | `ClientManagementFacade.update()` modifica Client; no modifica AppUser. | Definir reglas para cambios de contacto e identidad; evita asumir una sincronización inexistente. |
| Media | El rol técnico de cliente sigue siendo `CUSTOMER`, aunque el requisito original lo denomina `CLIENTE`. | `RoleCode`, token emitido y pruebas de registro. | La interfaz lo presenta como Cliente; decidir un cambio de código de rol en una fase autorizada. |
| Media | Nombres sólo tienen NotBlank/Size y teléfonos aceptan la clase de caracteres permitidos, no un mínimo de dígitos normalizados. | `ClientRegistrationRequest.java` y controles HTML. | Definir reglas de nombres y teléfonos y aplicarlas en ambas capas. Las pruebas alfabéticas sí se rechazan, pero no se afirma que toda cadena no humana se rechace. |
| Media | Catálogo vehicular parcial: 33 marcas y 4 modelos visibles. | Conteo MySQL por `enabled=1 AND pending_review=0`. | La excepción manual funciona; la carga extensa y aprobación de pendientes siguen sin pantalla implementada. |
| Pendiente | Recuperación por correo incompleta. | `AuthService.requestRecovery()` contiene comentario de integración de correo; UI informa la situación. | Integrar envío y flujo completo de recuperación/reset, con caso autorizado. |
| Pendiente | Cliente tiene endpoint de perfil propio, pero no pantalla de datos personales implementada. | `ClientController.own()` existe; App sólo presenta inicio para CUSTOMER. | Construir vista propia en el alcance que se autorice. |
| Pendiente | Órdenes, inventario, presupuestos, usuarios y auditoría operativa no tienen módulos completos en el código inspeccionado. | App previo usaba cifras, órdenes y bitácora estáticas. | Se identifican como DEMO / pendientes; no se implementaron procesos nuevos. |
| Pendiente | Cobertura incompleta de roles y concurrencia. | Pruebas realizadas con ADMINISTRADOR, RECEPCIONISTA y CUSTOMER. | No se certifican SECRETARIA/MANAGER, cargas concurrentes, fallos de red, todos los CP, SAT/RFC semántico ni recuperación real por correo. |

## Datos de prueba conservados

Las pruebas escribieron únicamente datos ficticios en MySQL local. Se conservan para trazabilidad; no se eliminaron registros existentes.

| Fixture | Estado final |
|---|---|
| Elena Pruebas MotorDesk / `qa.1791874287927@example.invalid` | SUSPENDIDO; cuenta CUSTOMER vinculada; 2 vehículos QA, uno de catálogo y otro manual. |
| Sofía Prueba Interfaz / `qa.interfaz.20261013@example.invalid` | ACTIVO; cuenta CUSTOMER vinculada desde Login; usada en pruebas visuales de registro y vehículos. |
| Vehículos de interfaz | VIN `3KPF24AD3R2610131` / placa `UI-2613` y VIN `3KPF24AD3R2610132` / placa `UI-2614`; asociados a Sofía. |
| Taller QA Auditoría | Registro ficticio creado/editado y dejado INACTIVO mediante baja lógica. ID `2a170b81-6497-4925-b55d-bbe47257fb61`. |

Los scripts requieren `QA_PASSWORD` por variable de entorno y nunca imprimen tokens. Cada ejecución crea fixtures nuevos; ejecutarlos sólo en desarrollo local. El fallo de estatus que hizo salir con código 1 al script principal quedó corregido posteriormente; no se volvió a ejecutar todo ese script en esta corrección. El script de talleres sigue señalando la respuesta incorrecta del archivo >15 MB.

## Evidencias visuales

| Archivo | Contenido |
|---|---|
| `docs/evidencias/redisenio-panel-desktop.jpg` | Nuevo panel de escritorio, con datos DEMO identificados. |
| `docs/evidencias/redisenio-panel-movil.jpg` | Distribución del panel móvil. |
| `docs/evidencias/redisenio-login-desktop.jpg` | Login final de escritorio. |
| `docs/evidencias/redisenio-login-movil.jpg` | Login final de móvil. |
| `docs/evidencias/registro-cliente-exitoso.jpg` | Confirmación SweetAlert para cliente ficticio. |
| `docs/evidencias/registro-vehiculo-exitoso.jpg` | SweetAlert vehicular visible sobre PrimeVue. |

La compilación local de Tailwind sigue su [documentación oficial de detección de fuentes](https://tailwindcss.com/docs/detecting-classes-in-source-files). No se realizó publicación en GitHub durante esta revisión.

## Corrección posterior: permisos de Estatus

La ruta caía en `anyRequest().denyAll()` antes de llegar al Controller. Se añadieron reglas GET/POST sobre `/api/v1/status-catalog` y PUT sobre `/api/v1/status-catalog/*`, exclusivas para ADMINISTRADOR/MANAGER; el Controller y el menú Vue ahora reconocen ambos roles. Facade y Repository no se modificaron. No hay migración ni cambio de credenciales.

| Verificación específica | Resultado |
|---|---|
| MockMvc: ADMINISTRADOR y MANAGER en GET/POST/PUT | 200 / 201 / 200; ambos roles autorizados. |
| MockMvc: RECEPCIONISTA, SECRETARIA, CUSTOMER, TREASURY, MECHANIC, MECHANIC_CHIEF y AUDITOR | 403 en las tres operaciones; Facade no invocada. |
| MockMvc: solicitudes anónimas / DELETE | 401 / 403 respectivamente. |
| Maven completo | 12 casos, 0 fallos, 0 errores. Mockito requirió ejecutar fuera del sandbox para adjuntar su agente de pruebas. |
| API local: Administración consulta | 200, cuatro estatus base disponibles. |
| API local: Recepción / Cliente / sin sesión | 403 / 403 / 401. |
| API local: alta y edición de estatus ficticio | 201 / 200; se dejó inhabilitado. |
| API local: duplicado / inhabilitar ACTIVO / eliminación física | 400 / 400 / 403; ACTIVO conserva habilitación. |
| Gerencia como cuenta real | No se inició sesión: su contraseña no está confirmada. Se verificó el rol MANAGER con el filtro y controlador reales en MockMvc, sin restablecer credenciales. |
| Vue con Administración | Catálogo cargado sin alerta de permisos; evidencia en `docs/evidencias/estatus-acceso-administracion.jpg`. |

Archivos ajustados: `SecurityConfig.java`, `StatusCatalogController.java`, `frontend/src/App.vue`; nueva prueba `StatusCatalogAuthorizationTest.java`. Estatus QA conservado: ID 5, `QA_PERMISOS_1791877841693`, enabled=false. Los otros pendientes de la revisión inicial permanecen.
