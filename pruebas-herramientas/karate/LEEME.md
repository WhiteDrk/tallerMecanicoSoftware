# Practica guiada de Karate y Burp

La demostración lista para exponer está en [GUIA_CLASE.md](GUIA_CLASE.md).
Incluye `--demo-clase`, comparación de umbrales, manipulación de JWT en Burp
y publicación de evidencia en el portal.

## Nube y portal de reportes

`03-nube-consulta-autenticada.feature` consulta el vehículo de demostración en
`https://motordesk-pruebas.up.railway.app`. El destino está fijado expresamente
para esta práctica; los dos archivos iniciales siguen limitados a localhost.

```bash
python3 ejecutar-nube.py
```

Pide las credenciales de recepción, inicia sesión una vez, verifica el vehículo
y ejecuta lotes de 10/1, 50/5, 100/10 y 200/20 (casos/paralelismo). Detiene la
escalada ante una falla. El límite es de 75 segundos por lote y el umbral
didáctico predeterminado es de 2 000 ms por respuesta; no es un SLA acordado.

Genera resumen HTML, CSV, JSON y un ZIP con los reportes originales. Para
publicarlo al terminar en **MotorDesk → Reportes**:

```bash
python3 ejecutar-nube.py --publish
```

La publicación solicita una cuenta de administración. Gerencia y Auditoría
pueden consultar el portal; Recepción ejecuta las consultas pero no puede leer
ni publicar los reportes. Para subir un ZIP ya generado sin repetir las pruebas:

```bash
python3 subir-reporte.py target/nube-FECHA-HORA.zip
```

Más detalles: [Portal_Reportes.md](../../docs/Portal_Reportes.md).

La primera prueba comprueba la proteccion de la consulta de vehiculos sin iniciar sesion. No crea ni modifica registros.

## Primera prueba

1. Abrir Docker Desktop e iniciar la aplicacion local.
2. Desde la raiz del proyecto, ejecutar `docker compose up -d`.
3. Abrir `http://localhost` para comprobar que la aplicacion esta disponible.
4. Desde esta carpeta, ejecutar `karate 01-acceso-sin-token.feature`.
5. Abrir `target/karate-reports/karate-summary.html`.

Si se usa un puerto distinto para la web, ajustar la URL del archivo `.feature`.

La ruta real es `GET /api/v1/vehicles?criterion=ID&value=...`.
El UUID utilizado es un valor sintacticamente valido para esta prueba. No se necesita que exista: el servidor debe rechazar la peticion antes de consultar los datos, porque no incluye un token.

## Resultado esperado

- Estado HTTP: `401`.
- Campo JSON `code`: `UNAUTHENTICATED`.
- Resultado de Karate: un escenario aprobado si ambas condiciones se cumplen.

Un `401` es el resultado correcto de este caso. Un error de conexion significa que no fue posible contactar la aplicacion y no demuestra si la autenticacion funciona.

## Continuacion

Despues se probara la consulta de un vehiculo de prueba existente con una cuenta ADMINISTRADOR o RECEPCIONISTA autorizada. Las credenciales y los tokens se configuraran localmente, sin pegarlos en el chat ni guardarlos en los archivos de pruebas.

En Burp se capturara esa consulta desde el navegador integrado, se enviara a Repeater y se comparara la respuesta con y sin el encabezado Authorization.

## Segunda prueba: concurrencia sin token

El archivo `02-concurrencia-sin-token.feature` genera entre 1 y 1000 escenarios independientes. Cada escenario hace una sola peticion a localhost y espera un rechazo `401` con `UNAUTHENTICATED`.

Ejemplo, desde esta carpeta:

```bash
KARATE_CASOS=100 KARATE_MAX_MS=2000 timeout 45s karate run -T 5 -o target/concurrencia-5 02-concurrencia-sin-token.feature
```

- `KARATE_CASOS`: cantidad total de peticiones, con un maximo de 1000 por ejecucion.
- `-T`: limite de escenarios que Karate ejecuta en paralelo. No representa un numero exacto de usuarios ni una tasa fija de peticiones por segundo.
- `KARATE_MAX_MS`: tiempo maximo esperado por respuesta. El valor de 2000 ms es un umbral de practica, no un requisito de rendimiento acordado.
- `timeout 45s`: detiene el proceso si sobrepasa 45 segundos. Una interrupcion puede dejar el reporte incompleto.
- `connectTimeout` y `readTimeout`: limitan la espera de conexion y respuesta.
- `-o`: guarda cada ejecucion en una carpeta distinta para conservar las evidencias.

Subir por etapas: 100 peticiones / 5 escenarios en paralelo; 500 / 25; 1000 / 50. Si todas pasan, se puede explorar 1000 / 200 y 1000 / 500, manteniendo el limite de 45 segundos por lote. Detener la escalada ante el primer fallo y revisar el reporte. Se puede interrumpir con Ctrl+C.

Un fallo de estado, conexion o tiempo debe investigarse por separado. Superar el umbral de tiempo no demuestra por si solo que el servidor haya caido. Que todos los casos aprueben indica solamente que no se encontro el limite bajo ese lote y ese nivel de concurrencia.

Esta prueba mide el rechazo de accesos sin token. No ejercita la consulta del vehiculo ni su acceso a MySQL. Para evaluar esa parte se necesita una cuenta autorizada y un vehiculo de prueba existente.

### Ejercicio: un criterio de latencia mas exigente

```bash
KARATE_CASOS=1000 KARATE_MAX_MS=500 timeout 45s karate run -T 1000 -o target/umbral-500ms 02-concurrencia-sin-token.feature
```

Este ejercicio mantiene el mismo maximo de 1000 peticiones, pero exige menos de 500 ms por respuesta. Es una decision didactica para aprender a interpretar fallos por tiempo, no un requisito de rendimiento del producto. Un caso que reciba `401` y tarde 500 ms o mas falla por latencia, aunque la proteccion de acceso funcione correctamente. No equivale a una caida del servidor ni prueba su capacidad maxima.

Los escalones mas altos exploran concurrencia maxima configurada de 1000. Ese numero no garantiza que el servidor reciba 1000 peticiones exactamente al mismo tiempo: tambien intervienen el cliente Karate, la JVM y la planificacion del equipo.

Para comparar los tiempos HTTP extraidos de los reportes generados:

```bash
python3 resumir-concurrencia.py
```

El script calcula el p95 con el metodo de rango mas cercano y guarda `target/resumen-concurrencia.json`. Separa el tiempo de respuesta HTTP del tiempo total de la suite, que tambien incluye la preparacion de escenarios y los reportes. Si un lote se interrumpe antes de cerrar el HTML, no se incluye como ejecucion completa.
