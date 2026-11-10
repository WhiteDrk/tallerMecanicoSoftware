# Practica de concurrencia del 9 de noviembre de 2026

Destino: `http://localhost/api/v1/vehicles`, consulta GET sin token con criterio ID. Cada escenario realiza una peticion y exige `401` y codigo JSON `UNAUTHENTICATED`.

## Escalada con umbral de practica de 2000 ms

| Peticiones | Concurrencia maxima configurada | Aprobados | Fallidos | p95 HTTP (ms) | Maximo HTTP (ms) |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 100 | 5 | 100 | 0 | 29 | 47 |
| 500 | 25 | 500 | 0 | 80 | 108 |
| 1000 | 50 | 1000 | 0 | 71 | 145 |
| 1000 | 200 | 1000 | 0 | 66 | 637 |
| 1000 | 500 | 1000 | 0 | 1258 | 1749 |
| 1000 | 1000 | 1000 | 0 | 1228 | 1602 |

Cada fila corresponde a una sola corrida. La variacion entre corridas, el cliente Karate, la JVM y la planificacion del equipo impiden interpretar la tabla como una capacidad maxima certificada o como una progresion estrictamente creciente.

## Ejercicio con umbral de practica de 500 ms

- Peticiones: 1000.
- Concurrencia maxima configurada: 1000 escenarios.
- Aprobados: 968.
- Fallidos: 32.
- Todos los fallos ocurrieron en `assert tiempoMs < maxMs`.
- Estados HTTP: 1000 respuestas `401`.
- Promedio HTTP: 102.77 ms.
- p95 HTTP: 311 ms.
- Maximo HTTP: 2796 ms.
- Duracion completa de la suite: 5229 ms.
- Despues del lote, la portada de la aplicacion respondio HTTP 200.

Reporte de este ejercicio: `target/umbral-500ms/karate-summary.html`.
Datos del ejercicio: `target/umbral-500ms/resumen.json`.
Datos de la escalada: `target/resumen-concurrencia.json`.

## Interpretacion

Los 32 fallos muestran que esa corrida no cumplio el criterio didactico de menos de 500 ms por respuesta. El rechazo de acceso siguio funcionando. No se observo una caida de la aplicacion, ni se determino su capacidad maxima.

La carga ejercita el rechazo de accesos sin token, no la consulta de vehiculos ni MySQL. La concurrencia configurada es el maximo de escenarios que Karate puede ejecutar en paralelo, no una medicion de peticiones simultaneas en el servidor. No hubo telemetria de recursos del servidor disponible para atribuir el aumento de latencia a un componente concreto.

Para cargar la consulta completa se necesita una cuenta autorizada y un vehiculo de prueba existente. Esos datos se deben introducir localmente; no hace falta compartir contrasenas ni tokens en el chat.
