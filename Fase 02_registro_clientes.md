# Fase 02 Registro de clientes

**Fecha:** 28 de septiembre de 2026  
**Alcance:** alta de clientes desde la plataforma, sin incorporar órdenes, inventario, talleres múltiples ni módulos no solicitados.

## 1 Estado de la fase

| Resultado | Estado | Decisión de auditoría |
|---|---|---|
| Flujo de registro de cliente | Implementado en código | Apto para pruebas de integración con MySQL. No apto aún para producción hasta ejecutar dichas pruebas y contar con usuarios aprobados. |
| Acceso por rol | Implementado | Solo `OWNER` (administrador del sistema), `MANAGER` y `CUSTOMER_SERVICE` (Recepción) pueden invocar el endpoint. |
| Duplicados | Implementado en aplicación y base de datos | Se prohíbe guardar el mismo correo normalizado o teléfono normalizado. |
| Fotografía | Implementado | Opcional, JPEG/PNG, máximo 7 MB, tipo declarado y contenido decodificado por el servidor. |
| Confirmación visual | Implementado | SweetAlert2 muestra éxito o error según respuesta del API. |

## 2 Componentes desarrollados

| Capa | Archivo | Implementación verificable | Estado |
|---|---|---|---|
| Vista | `frontend/src/components/ClientRegistrationForm.vue` | Formulario con datos personales, contactos, dirección, carga de foto y SweetAlert2. | Hecho |
| Vista | `frontend/src/App.vue` | Agrega el acceso de navegación `Clientes` y muestra el formulario. | Hecho |
| Adaptador de API | `frontend/src/services/clientApi.js` | Empaqueta DTO y foto en `FormData`; adjunta Bearer token de la sesión. | Hecho |
| Controlador | `backend/src/main/java/mx/taller/client/ClientController.java` | Expone `POST /api/v1/clients` multipart y limita el método a tres roles. | Hecho |
| Facade | `backend/src/main/java/mx/taller/client/ClientRegistrationFacade.java` | Coordina duplicados, foto, persistencia y registro de auditoría entre el controlador y repositorios. | Hecho |
| Dominio | `backend/src/main/java/mx/taller/client/Client.java` | Representa los datos requeridos y la fotografía opcional. | Hecho |
| DTO | `backend/src/main/java/mx/taller/client/ClientRegistrationRequest.java` | Valida formato, longitud, correo, teléfonos, fecha y código postal. | Hecho |
| Repositorio | `backend/src/main/java/mx/taller/client/ClientRepository.java` | Consulta preventiva de correo/teléfono normalizados y persistencia JPA. | Hecho |
| Validación de archivo | `backend/src/main/java/mx/taller/client/ClientPhotoValidator.java` | Restringe formato, tamaño y decodificación de imagen. | Hecho |
| Esquema | `backend/src/main/resources/db/migration/V3__clients.sql` | Crea tabla `clients`, campos solicitados y restricciones únicas. | Hecho |
| Seguridad | `backend/src/main/java/mx/taller/config/SecurityConfig.java` | Autoriza `POST /api/v1/clients` para los roles definidos; mantiene denegación por defecto. | Hecho |
| Errores | `backend/src/main/java/mx/taller/config/ApiExceptionHandler.java` | Devuelve respuestas estables para duplicados, foto inválida y datos inválidos. | Hecho |
| Configuración | `backend/src/main/resources/application.yml` | Limita carga multipart a 7 MB por archivo y 8 MB por solicitud. | Hecho |

## 3 Datos capturados

| Grupo | Campos | Validación aplicada |
|---|---|---|
| Identidad | Nombre completo, contacto alternativo, fecha de nacimiento | Obligatorios; máximo de 150 caracteres; fecha pasada. |
| Contacto | Teléfono, teléfono de trabajo, correo, correo de trabajo | Teléfonos con 7–20 caracteres permitidos; correo principal obligatorio; correo laboral opcional. |
| Fotografía | Archivo opcional | Solo JPEG/PNG; máximo 7 MB; verificación del contenido como imagen. |
| Dirección | Calle, colonia, municipio, estado, código postal | Todos obligatorios; código postal mexicano de cinco dígitos. |
| Integridad | Correo y teléfono normalizados | Índices únicos y consulta previa; no se crean duplicados. |

## 4 Patrón Facade y recorrido de datos

| Paso | Componente | Acción | Control |
|---|---|---|---|
| 1 | `ClientRegistrationForm.vue` | Captura datos y archivo. | Validación HTML y límite de archivo en navegador. |
| 2 | `clientApi.js` | Construye `FormData` y envía Bearer JWT. | No serializa la foto como JSON. |
| 3 | `ClientController` | Recibe DTO multipart autenticado. | `@PreAuthorize` y regla de ruta. |
| 4 | `ClientRegistrationFacade` | Ejecuta el caso de uso. | Centraliza la lógica entre vista/API y repositorio. |
| 5 | `ClientPhotoValidator` | Confirma tipo y contenido de la foto. | Rechaza archivos no válidos. |
| 6 | `ClientRepository` y MySQL | Consulta y guarda el cliente. | Unicidad de correo/teléfono incluso ante concurrencia. |
| 7 | `AuditService` | Registra `CLIENT_REGISTERED`. | Conserva actor, IP hasheada y referencia del cliente. |
| 8 | SweetAlert2 | Muestra resultado de la respuesta del API. | Éxito solo ante respuesta HTTP correcta. |

## 5 Diagrama de componentes

| Artefacto | Ubicación | Resultado de validación |
|---|---|---|
| Especificación Archify | `.archify/architecture-registro-clientes-20260928-181545/candidate.json` | Validada por Archify. |
| Diagrama HTML | `.archify/architecture-registro-clientes-20260928-181545/registro-clientes.html` | Entregado y comprobado por Archify. |
| Recibo | `.archify/architecture-registro-clientes-20260928-181545/registro-clientes.finalize.json` | Validación, entrega e integridad aprobadas. Browser-check omitido: el entorno no cuenta con Chrome/Chromium. |

## 6 Verificación realizada

| Verificación | Resultado | Evidencia |
|---|---|---|
| Compilación y pruebas backend | Aprobada | `mvn clean verify` en `backend/`. |
| Build frontend | Aprobado | `npm run build` en `frontend/`. |
| Integridad de cambios | Aprobada | `git diff --check`. |
| Diagrama de componentes | Parcialmente aprobada | Archify validó estructura, entrega e integridad; no se realizó prueba de navegador por ausencia de ejecutable. |

## 7 Lo que no se implementó por no formar parte de la solicitud

| Elemento | Estado | Motivo |
|---|---|---|
| Asociación de cliente a uno o varios talleres | No implementado | Se consideró como requisito futuro, sin modelo ni reglas de asociación definidos. |
| Edición, baja, consulta o listado de clientes | No implementado | Esta fase cubre únicamente registro. |
| Órdenes de trabajo, inventario, presupuesto o pagos | No implementado | Fuera del alcance de Fase 02. |
| Roles nuevos | No implementado | Se reutilizaron los roles existentes: `OWNER`, `MANAGER`, `CUSTOMER_SERVICE`. |
| Formatos WebP, GIF o PDF para foto | No implementado | El alcance seguro se restringió a JPEG/PNG verificables con las librerías actuales. |

## 8 Pendientes para cerrar la fase en entorno real

| Pendiente | Razón | Responsable sugerido |
|---|---|---|
| Prueba de integración con MySQL y Flyway | Verificar la migración V3, BLOB y restricciones únicas en un contenedor real. | Desarrollo y QA |
| Usuario activo por rol | El alta requiere un JWT de un usuario aprobado con rol permitido. | Administración del sistema |
| Política de retención de fotografías | Las fotos son datos personales; definir retención, respaldo, acceso y eliminación. | Responsable de datos y Gerencia |
| Modelo cliente–taller | Diseñar la relación cuando se confirme operación con varios talleres. | Producto y arquitectura |
| Pruebas de concurrencia | Demostrar que solicitudes simultáneas no producen registros duplicados. | QA |

## 9 Decisiones de auditoría

| Decisión | Justificación |
|---|---|
| El propietario (`OWNER`) se trata como administrador del sistema para este caso de uso. | El modelo de roles actual no contiene `SYSTEM_ADMIN`; no se creó un rol adicional fuera del alcance. |
| La foto se almacena en `MEDIUMBLOB`. | El límite funcional de 7 MB queda dentro del tipo; una solución externa de objetos se evaluará si aumenta el volumen. |
| El correo y teléfono son las claves de detección de duplicado. | Son datos requeridos y verificables. Cualquier futura política de coincidencia por nombre/fecha debe definirse antes de implementarla. |
