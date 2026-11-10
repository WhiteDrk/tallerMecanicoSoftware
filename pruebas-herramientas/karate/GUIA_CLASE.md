# Demostración en clase: Karate, Burp y reportes privados

Aplicación: https://motordesk-pruebas.up.railway.app

Verificación realizada el 10 de noviembre de 2026: el perfil normal completó
368 casos aprobados. Con el criterio de 100 ms aprobaron los ocho casos de
seguridad y fallaron las diez consultas del primer lote por tiempo; esas diez
recibieron HTTP 200. Son observaciones de esas ejecuciones, no garantías del
resultado que se obtendrá con otra red o en otro momento.

Usar la cuenta de **Lucía** para ejecutar las consultas. Usar la de **Daniel**
para publicar los resultados y abrir **Reportes**. Las contraseñas son las
entregadas para esta demostración; no están guardadas en esta guía ni en GitHub.

## Antes de exponer

- Abrir la aplicación y verificar el VIN `3KPF24AD3R2610131`.
- Mantener Railway activo y tener crédito disponible.
- Abrir la terminal en la raíz del proyecto y Burp Community.
- En Burp usar **Proxy → Open browser** con **Intercept is off**.
- Iniciar sesión como Lucía en el navegador integrado de Burp.
- Preparar una segunda pestaña como Daniel para mostrar Reportes al final.

## 1. Karate: ocho pruebas de seguridad más carga progresiva

Desde la raíz del proyecto:

```bash
python3 pruebas-herramientas/karate/ejecutar-nube.py --demo-clase --publish
```

Primero solicita el correo y la contraseña de recepción. Después ejecuta:

| Caso | Respuesta esperada | Interpretación |
|---|---|---|
| Sin token, aunque se envíe `X-Role: ADMINISTRADOR` | 401 | Una cabecera no sustituye la sesión |
| VIN válido con sesión | 200 | Se consulta el vehículo autorizado |
| Token inventado | 401 | La API exige credenciales válidas |
| JWT con el rol cambiado y la firma original | 401 | Cambiar el texto del rol invalida la firma |
| VIN `' OR '1'='1` | 400 | Esta entrada no cumple el formato y es rechazada |
| Dos criterios en la misma consulta | 400 | La API rechaza parámetros ambiguos |
| Reportes con Lucía y `X-Role: ADMINISTRADOR` | 403 | Una cuenta válida conserva su rol real |
| UUID inexistente | 404 | La consulta no devuelve otros registros |

Un 401, 403 o 400 **aprueba** cuando es la respuesta esperada del caso.
Rechazar este intento de inyección no demuestra que toda la aplicación esté
libre de inyección SQL. El caso de UUID inexistente no prueba aislamiento
entre talleres; eso requiere registros de dos talleres y cuentas distintas.

Si las ocho pruebas pasan, se ejecutan 360 consultas válidas:

| Etapa | Consultas | Paralelismo máximo |
|---|---:|---:|
| Referencia | 10 | 1 |
| Carga 1 | 50 | 5 |
| Carga 2 | 100 | 10 |
| Carga 3 | 200 | 20 |

Son **368 casos** si se completan todas las etapas. El paralelismo no representa
usuarios simultáneos exactos. Se detiene el aumento ante un fallo.

Frase para explicar: “Karate verifica qué debe permitir y qué debe rechazar la
API; después compara sus respuestas al aumentar la concurrencia”.

### Cómo se sube el reporte

Al terminar, `--publish` solicita las credenciales de administración, **Daniel**.
El proceso es: Karate → HTML/JSON/CSV → ZIP sin contraseñas ni tokens → API
autenticada → MySQL → sección Reportes. La terminal imprime el enlace privado.
Al abrirlo, el directivo debe iniciar sesión con un rol autorizado.

Si decides no publicar en esa ejecución, quita `--publish`. La terminal muestra
la ruta del ZIP, que después puedes subir desde **Reportes → Subir reporte**
o con `subir-reporte.py`.

## 2. Contraste: hacer más exigente el tiempo permitido

```bash
python3 pruebas-herramientas/karate/ejecutar-nube.py --demo-clase --max-ms 100 --publish
```

El umbral de 100 ms se aplica a las consultas de la etapa de carga, no a los
ocho casos de seguridad. Si una respuesta tarda 100 ms o más, Karate la marca
como fallida aunque reciba HTTP 200. El aumento de carga se detiene y el
reporte registra los fallos reales.

No hay un fallo garantizado: si las respuestas cumplen el criterio, pasan.
No se cambian códigos esperados para fingir una falla. Este umbral es didáctico,
no un SLA acordado del proyecto. Mostrar en el portal el contraste entre
resultado HTTP correcto y criterio de tiempo incumplido.

## 3. Burp: demostrar que editar el token no te convierte en administrador

### Capturar la consulta inicial

1. En el navegador de Burp, entrar como Lucía y consultar el VIN de prueba.
2. En **Proxy → HTTP history**, localizar la solicitud `GET /api/v1/vehicles`.
3. Clic derecho → **Send to Repeater**.
4. En Repeater pulsar **Send** sin modificarla: se espera `200` y el vehículo.

### A. Una sesión válida no da todos los permisos

Cambiar únicamente la ruta de la primera línea por:

```text
/api/v1/test-reports
```

Conservar el token de Lucía y la versión HTTP que muestra Burp. Pulsar Send:
se espera **403**. Después agregar esta cabecera:

```http
X-Role: ADMINISTRADOR
```

Reenviar: se sigue esperando **403**. La API no concede el rol a partir de una
cabecera inventada.

### B. Cambiar el rol escrito en el JWT

En otra terminal, desde la raíz del proyecto:

```bash
python3 pruebas-herramientas/karate/token_demo.py
```

Copiar el token original de Lucía desde `Authorization` en Repeater y pegarlo
en esa terminal. La entrada no se muestra. El script cambia `roles` a
`ROLE_ADMINISTRADOR` y conserva la firma original: el resultado es un token
inválido, no una credencial de administrador.

Copiar el resultado y reemplazar únicamente el valor de Authorization:

```http
Authorization: Bearer TOKEN_MODIFICADO
```

Enviar otra vez a `/api/v1/test-reports`: se espera **401**, código
`INVALID_TOKEN`.

Frase para explicar: “Puedo leer y modificar el contenido de un JWT, pero el
servidor verifica su firma; el rol no se obtiene simplemente editando el texto”.

### C. Validación fuera de la interfaz

Restaurar el token original de Lucía. Cambiar la ruta por:

```text
/api/v1/vehicles?criterion=VIN&value=%27%20OR%20%271%27%3D%271
```

Se espera **400**, código `INVALID_VEHICLE`. Comparar con la consulta inicial
200. Aquí se prueba el rechazo de esa entrada concreta.

Si la consulta válida empieza a devolver 401 durante la clase, volver a iniciar
sesión y capturar una petición nueva: los tokens de esta aplicación expiran.

## 4. Publicar la evidencia manual de Burp

Guardar capturas propias con la solicitud relevante y la respuesta. Ocultar el
valor de Authorization. Nombres sugeridos: `01-consulta-200.png`,
`02-recepcion-403.png`, `03-token-alterado-401.png`, `04-entrada-400.png`.
Los nombres deben corresponder a lo que realmente observaste, no a lo esperado
si la prueba devolvió algo diferente.

Crear una carpeta para esas capturas:

```bash
mkdir -p pruebas-herramientas/karate/target/evidencia-burp
```

Después de guardarlas ahí:

```bash
python3 pruebas-herramientas/karate/empaquetar-burp.py pruebas-herramientas/karate/target/evidencia-burp --publish
```

Solicita la cuenta de Daniel y publica el ZIP como **Burp**. También puede
subirse el ZIP manualmente desde la aplicación. El portal muestra
**Documentado**, ya que son evidencias manuales: no se inventan resultados
numéricos ni se presenta esto como una prueba de carga de Burp.

## Cierre de la exposición

Mostrar el enlace de cada ejecución dentro de Reportes. Explicar la diferencia
entre ausencia de sesión (401), falta de permisos (403), entrada inválida (400)
y falla por tiempo aunque haya HTTP 200. Descargar los ZIP antes de eliminar
el proyecto y su volumen de Railway.

Referencias: [Burp Repeater](https://portswigger.net/burp/documentation/desktop/tools/repeater/http-messages),
[tiempos en Karate](https://docs.karatelabs.io/http-responses/response-time/).
