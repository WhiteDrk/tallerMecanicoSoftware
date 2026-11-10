# Reportes de pruebas en MotorDesk

Para la exposición con Karate y Burp, consultar la
[guía de clase](../pruebas-herramientas/karate/GUIA_CLASE.md).

La sección **Reportes** utiliza el inicio de sesión existente. Administración
puede subir evidencia; Administración, Gerencia y Auditoría pueden consultar
los resultados y descargar el paquete. Recepción, Clientes y otros roles no
pueden acceder, ni siquiera mediante una URL directa a la API.

GitHub contiene el código del portal. Los ZIP y sus resúmenes se guardan en
MySQL mediante la migración V17. No se publican como archivos estáticos del
frontend ni se suben al repositorio público.

## Uso

1. Entrar con una cuenta de Administración y abrir **Reportes**.
2. Seleccionar **Subir reporte**, título, herramienta, notas y un ZIP de hasta 8 MB.
3. Para Karate, usar el ZIP generado por `ejecutar-nube.py`, que contiene
   `resultados.json` y los reportes HTML originales.
4. El portal muestra el resultado, fecha, total de casos y las métricas por etapa.
5. **Copiar enlace** permite compartir una ejecución; quien abra ese enlace
   debe iniciar sesión con un rol autorizado. **Descargar reporte completo**
   recupera el ZIP original.

Los archivos de Burp Community pueden empaquetarse en ZIP con capturas y
documentos. Si no incluyen métricas estructuradas, aparecen como **Documentado**,
sin simular un resultado aprobado ni atribuirles mediciones de carga.

## Automatizar la subida

Desde `pruebas-herramientas/karate`:

```bash
python3 ejecutar-nube.py --publish
```

Solicita la cuenta de recepción para las consultas y, al terminar, la cuenta de
administración para publicar el ZIP. Las contraseñas se introducen sin mostrarse
en la terminal. El token de publicación no se guarda en GitHub ni en el reporte.

Para publicar una ejecución ya existente, sin repetir la carga:

```bash
python3 subir-reporte.py target/nube-FECHA-HORA.zip --title "Consulta de vehículos en nube"
```

## Alcance de la implementación

- Historial de las últimas 50 publicaciones; cada reporte tiene enlace propio.
- Resumen de Karate validado: casos, aprobados, fallidos, HTTP, p95, máximo y paralelismo.
- Evidencia conservada en MySQL; almacenamiento dependiente del volumen de Railway.
- ZIP limitado a 8 MB, 1 000 entradas y 64 MB descomprimidos. No se extrae en disco.
- El portal presenta datos estructurados y descarga el original; no ejecuta el
  HTML ni los scripts de un archivo subido dentro de la aplicación.
- La publicación queda registrada en la auditoría. Las respuestas privadas usan `no-store`.
- El ZIP generado por el runner se revisa para retirar tokens y contraseñas.
- No hay ejecución de pruebas desde el servidor: Karate sigue ejecutándose en
  la computadora local y publica los resultados cuando se usa `--publish`.
- Si se elimina MySQL o su volumen al terminar la demostración, se pierden sus
  reportes. Descargar previamente los ZIP que se quieran conservar.

Un resultado aprobado describe el alcance ejecutado; no acredita por sí solo
la capacidad máxima del servidor. Las métricas de CPU y memoria de Railway
se documentan aparte si se necesitan en la entrega.
