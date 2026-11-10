# Consulta vehicular — Implementación y prueba

Fecha: 3 de noviembre de 2026. Estado: implementada y desplegada en localhost; API verificada contra MySQL. Comprobación manual de la interfaz en navegador pendiente: esta sesión no dispone de herramientas de navegador.

## Cómo probar desde la aplicación

1. Abre `http://localhost/` y recarga con Ctrl+F5.
2. Inicia sesión con `recepcion.demo@taller.local`, usando la contraseña temporal de demostración comunicada en la conversación.
3. En el menú, abre **Clientes** y pulsa **Consultar vehículo**, encima del listado. No selecciones ninguna fila ni cliente.
4. Selecciona un criterio, escribe su valor y pulsa **Consultar**.

| Criterio | Dato de demostración | Resultado esperado |
|---|---|---|
| VIN | `3KPF24AD3R2610131` | KIA RIO 2024, Motor Centro |
| Placa | `UI-2613` | El mismo vehículo |
| ID del vehículo | `46a24888-5db1-4f82-b124-d4de039baa48` | El mismo vehículo |
| ID inexistente | `00000000-0000-0000-0000-000000000000` | Mensaje sin coincidencia |
| VIN inválido | `IIIIIIIIIIIIIIIII` | Mensaje de formato; no debe enviarse la consulta |

La ficha contiene identificadores, marca/modelo, año, versión, kilometraje, serie, estatus y nombre del taller. No expone nombre, correo, teléfono ni dirección del cliente. Editar el criterio o dato retira la ficha anterior y cancela la solicitud pendiente.

## Contrato implementado y decisiones

GET `/api/v1/vehicles?criterion=VIN|PLATE|ID&value=...`; exactamente un criterio y un valor, sin clientId ni workshopId. El ID es el UUID ya existente del vehículo. Roles conservados del módulo vehicular: ADMINISTRADOR y RECEPCIONISTA. Otros roles, incluida Gerencia, no reciben nuevos permisos en esta implementación.

| Regla | Implementación |
|---|---|
| VIN | Trim, mayúsculas, 17 caracteres sin I/O/Q; espacios interiores rechazados. |
| Placa | Mayúsculas y eliminación de espacios; conserva guiones, como el registro vigente; 5–8 caracteres alfanuméricos y máximo 10 incluyendo hasta 2 guiones. |
| ID | UUID canónico 8-4-4-4-12. No es ID de cliente ni número de serie. |
| Taller | WorkshopAccessService obtiene talleres activos permitidos del actor; Repository incluye esos IDs en cada búsqueda. |
| Historial | La consulta muestra el estatus real, sin exigir cliente ACTIVO para leer. No habilita alta para suspendidos. Talleres inactivos permanecen excluidos. |
| Ausencia/ajeno | Mismo 404 VEHICLE_NOT_FOUND y mensaje; no se confirma existencia fuera de alcance. |
| Errores | 400 entrada/parámetros inválidos; 401 sin token/token inválido; 403 rol insuficiente. |
| Efectos | Lectura transaccional; no cambia vehículos/clientes/catálogos ni genera evento de movimiento. El acceso HTTP puede quedar en logs existentes de infraestructura. |

## Archivos modificados/incorporados

| Archivo | Responsabilidad |
|---|---|
| `backend/src/main/java/mx/taller/vehicle/VehicleQueryController.java` | GET y rechazo de parámetros ausentes, repetidos o adicionales; autorización de método. |
| `backend/src/main/java/mx/taller/vehicle/VehicleQueryFacade.java` | Normalización, alcance del actor, coordinación de lectura y DTO mínimo. |
| `backend/src/main/java/mx/taller/vehicle/VehicleRepository.java` | Tres recuperaciones exactas con client.workshop.id IN alcance autorizado; EntityGraph para ficha. |
| `backend/src/main/java/mx/taller/vehicle/Vehicle.java` | Getters de lectura; sin cambios de columnas/relaciones. |
| `backend/src/main/java/mx/taller/vehicle/VehicleNotFoundException.java` | Ausencia uniforme. |
| `backend/src/main/java/mx/taller/config/SecurityConfig.java` | Matcher GET exclusivo para Administración/Recepción. |
| `backend/src/main/java/mx/taller/config/ApiExceptionHandler.java` | Traduce ausencia a 404 con código público. |
| `frontend/src/components/VehicleLookupModal.vue` | Formulario por un criterio, ficha, validación, estados y cancelación/descartado de respuestas obsoletas. |
| `frontend/src/components/ClientList.vue` | Botón independiente de filas; conserva registro/asociación. |
| `frontend/src/services/vehicleApi.js` | lookup con URLSearchParams, Bearer, no-store, signal y status/code en errores. |
| `backend/src/test/java/mx/taller/vehicle/VehicleQueryTest.java` | Ocho casos de normalización, recuperación con alcance, ausencia y validación. |
| `backend/src/test/java/mx/taller/vehicle/VehicleQueryAuthorizationTest.java` | Doce casos HTTP, incluidos los nueve roles, anonimato y parámetros. |
| `scripts/verify-vehicle-query.mjs` | Prueba reproducible en localhost con credenciales desde entorno; nunca imprime tokens. |

Se conservó Vue → servicio REST → Controller → Facade → Repository → MySQL. No se agregaron dependencias, tablas ni migraciones, ni se movieron paquetes.

## Pruebas ejecutadas

| Prueba | Resultado real |
|---|---|
| `mvn -q -f backend/pom.xml test` | 32 casos, cero fallos/errores: 20 nuevos de consulta y 12 anteriores. |
| `npm --prefix frontend run build` | Aprobado; advertencia de bundle mayor a 500 kB. |
| Revisión adicional del agente de Seguridad/Auditoría | CUMPLE en revisión estática; sin hallazgos accionables nuevos. |
| Script contra API/Nginx/MySQL local | 25 comprobaciones aprobadas, cero fallos. |
| VIN, placa e ID del demo | 200, mismo UUID y vehículo. |
| VIN minúsculas/extremos y placa con espacios | 200, normalización correcta. |
| Entrada inválida, criterio desconocido, exceso de longitud, parámetros repetidos/extra/ausentes | 400. |
| Anónimo / token inválido / Cliente sin rol | 401 / 401 / 403. |
| Identificador inexistente | 404. |
| Recepción busca vehículo de Servicio Valle por los tres criterios | 404 idéntico a ausencia; Administración obtiene 200 en los tres casos. |
| Flyway | API inició con 15 migraciones validadas, versión 15; ninguna migración nueva. |
| Navegador, foco, diseño móvil y carrera de solicitudes | Pendiente de verificación manual; compilación no acredita estas propiedades funcionales. |

Un primer caso de prueba usaba la palabra `invalid` como placa inválida; siete letras son una placa de formato admitido. Se corrigió el dato del test a `??`, sin relajar reglas del producto; luego pasaron los 32 casos.

La reconstrucción Docker normal se canceló por descargas lentas de imágenes base. Se empaquetó el backend y se incorporaron JAR y dist a las imágenes locales existentes Java 26/Nginx mediante Dockerfiles temporales fuera del repositorio. Se recrearon API/web con Compose `--no-build`; Dockerfiles y compose del proyecto no se modificaron. No quedan builds de descarga ejecutándose.

## Repetir la prueba

Desde la raíz, suministra la contraseña de demostración sin guardarla en archivos:

```bash
read -rsp 'Contraseña de demostración: ' QA_PASSWORD
export QA_PASSWORD
node scripts/verify-vehicle-query.mjs
unset QA_PASSWORD
```

El modo anterior consulta vehículos existentes y registra los inicios de sesión normales. Para comprobar aislamiento creando otro fixture local, usa `QA_CREATE_FIXTURES=1 node scripts/verify-vehicle-query.mjs` con la variable QA_PASSWORD preparada. Ese modo crea y conserva un cliente/vehículo ficticio en otro taller. No ejecutar fuera de desarrollo local. Los scripts antiguos verify-local/verify-workshops crean otros fixtures y no son necesarios para esta prueba.

## Fixture de aislamiento conservado

| Dato | Valor |
|---|---|
| Cliente QA | `a03c4cb2-127b-473f-b0ba-c1cb2b65545f` |
| Vehículo QA | `d8f28508-ac68-42fc-a175-d107f7944db7` |
| Taller | Servicio Valle |
| VIN / placa | `3KPF24AD3R9615891` / `QT15891` |

No se borraron registros existentes. Riesgos heredados fuera de este cambio —mensajes globales de duplicados del alta, recuperación por correo y respuesta de foto de taller >15 MB— no se presentan como corregidos.

## Cierre por fases

| Fase | Estado |
|---|---|
| Especificación | Completada: un criterio, UUID existente, roles conservados y DTO mínimo. |
| Desarrollo | Completado dentro de módulos existentes. |
| Validación backend/API | Completada: 32 pruebas Java y 25 comprobaciones reales. |
| Validación visual | Pendiente manual en navegador. |
| Entrega | Código y prueba disponibles en localhost; sin publicación GitHub. |
