# Manual de demostración de MotorDesk

**Karate desde cero, constructor web, Burp Community e informe para dirección.**

La demostración reproduce **los mismos tres escenarios** desde la terminal y desde
MotorDesk. Burp permite inspeccionar el resultado que explica el fallo. El PDF
convierte ese resultado en un hallazgo con impacto y una acción concreta.

| Tiempo | Parte | Qué debe quedar en pantalla |
|---|---|---|
| 0:00–1:00 | Planteamiento | Tres reglas que queremos comprobar |
| 1:00–4:00 | Karate desde la terminal | El archivo `.feature` y el resultado de cada caso |
| 4:00–6:30 | MotorDesk | Los mismos escenarios en **Crear feature** y su reporte guardado |
| 6:30–8:30 | Burp Community | HTTP 200 y el cuerpo `[]` del catálogo |
| 8:30–10:00 | PDF ejecutivo | Resultado, impacto y acción sugerida |

## Antes de presentar

- Abre [MotorDesk en Railway](https://motordesk-pruebas.up.railway.app). En **Reportes**
  confirma que aparecen **Crear feature** y, al seleccionar un reporte,
  **Descargar PDF ejecutivo**. Si falta alguno, comprueba que los servicios
  **api** y **web** estén en la última versión de la rama `codex/railway-demo`.
- Ten a mano la cuenta de **Lucía Herrera (Recepción)** y la de **Daniel Mendoza
  (Administrador)**. Usa Lucía en la terminal y en el navegador de Burp. Usa
  Daniel para crear y ejecutar el feature desde MotorDesk.
- Abre la terminal y ejecuta `java -version` y `karate --help` para confirmar
  que puedes usar Karate. Ten Burp abierto antes de empezar.
- Guarda una copia del [reporte de la ejecución verificada](https://motordesk-pruebas.up.railway.app/#reportes/73506d26-16a7-4108-9bd6-9d1aef27fd3d)
  para mostrar la evidencia del 16 de noviembre si cambia el entorno durante la clase.

## 1. Explica la regla de cada caso

| Caso | Regla que comprobarás | Resultado observado en la demostración |
|---|---|---|
| Listado de reportes sin sesión | El servidor debe pedir autenticación con HTTP 401 y `UNAUTHENTICATED`. | Aprobado |
| Consulta de un vehículo conocido | El servidor debe devolver HTTP 200 y el VIN `3KPF24AD3R2610131`. | Aprobado |
| Catálogo de estados | El servidor debe devolver HTTP 200 y al menos un estado que se pueda mostrar en el formulario. | Fallido. La respuesta fue HTTP 200 con `[]`. |

> «MotorDesk necesita proteger los reportes, encontrar vehículos registrados y
> ofrecer estados para capturar direcciones. Voy a comprobar una regla concreta
> de cada operación».

## 2. Crea un archivo `.feature` y ejecútalo en Karate

1. Crea un archivo nuevo.

   ```bash
   mkdir -p "$HOME/karate-clase"
   cd "$HOME/karate-clase"
   nano taller.feature
   ```

2. Copia el contenido del [feature de la demostración](../pruebas-herramientas/karate/fundamentos/06-demostracion-taller.feature).
   En Nano pulsa `Ctrl+O`, `Enter` y `Ctrl+X`.
3. Explica la estructura mientras señalas las líneas. `Feature` nombra la prueba.
   `Background` prepara la URL y obtiene una credencial de Recepción.
   Cada `Scenario` representa un caso. `Given` y `And path` preparan la
   dirección, `When method get` consulta el servidor y `Then` y `And` comprueban
   las condiciones. Solo el vehículo y el catálogo adjuntan la credencial.
4. Señala las dos condiciones centrales del caso del catálogo.

   ```gherkin
   Then status 200
   And assert response.length > 0
   ```

   La primera comprueba la respuesta HTTP. La segunda exige que la lista tenga
   datos utilizables para el formulario.
5. Ejecuta los tres escenarios. Escribe la contraseña cuando la terminal la pida.

   ```bash
   export MOTORDESK_EMAIL='lucia.herrera@motordesk.example'
   read -rsp 'Contraseña de Lucía: ' MOTORDESK_PASSWORD
   printf '\n'
   export MOTORDESK_PASSWORD
   karate run -o target/karate-reports taller.feature
   unset MOTORDESK_PASSWORD MOTORDESK_EMAIL
   xdg-open target/karate-reports/karate-summary.html
   ```

6. Abre el caso del catálogo en el HTML, identifica la condición fallida y
   señala que las otras dos operaciones obtuvieron el resultado esperado.
   Cuando Karate detecta un caso fallido, su código de salida señala ese
   resultado y el reporte HTML conserva el detalle.

## 3. Repite el mismo feature desde MotorDesk

1. Inicia sesión como **Daniel** y abre **Reportes → Crear feature**.
2. Escribe **Comprobaciones del taller MotorDesk** y establece **5000 ms**
   como límite por respuesta, igual que en el archivo de la terminal.
3. En el primer escenario selecciona **Listado de reportes sin sesión**.
4. En el segundo selecciona **Consulta de vehículo**, escribe
   `3KPF24AD3R2610131` y elige **200 · Vehículo encontrado**.
5. Pulsa **Agregar escenario** y elige **Catálogo de estados**.
   Escribe **1** como cantidad mínima. Si quieres que ambos resultados tengan
   los mismos nombres, edita el nombre de cada escenario.
6. Pulsa **Ver .feature**. Señala los tres escenarios y compara la petición
   y las validaciones con las del archivo escrito en la terminal.
7. Pulsa **Ejecutar feature**. Abre el resultado recién guardado y señala
   los conteos, el estado HTTP esperado, el recibido y la causa del fallo.

> «La interfaz genera el archivo y ejecuta Karate por detrás. Aquí podemos
> guardar y consultar el resultado sin escribir los comandos para cada prueba».

## 4. Comprueba en Burp qué devolvió el servidor

1. Abre **Proxy → Intercept**. Deja **Intercept is off** y pulsa **Open Browser**.
2. Desde ese navegador, entra a MotorDesk como **Lucía** y abre **Clientes**.
   Al cargar el formulario, MotorDesk solicita el catálogo de estados.
3. En **Proxy → HTTP history**, selecciona `GET /api/v1/address-catalog/states`.
4. Proyecta **Response**. En la ejecución verificada, el estado fue
   **HTTP 200** y el cuerpo fue **`[]`**. Si vas a guardar una captura,
   muestra la respuesta y oculta el encabezado `Authorization` de la petición.
5. Para repetir la consulta, elige **Send to Repeater**, abre **Repeater**
   y pulsa **Send**. Comprueba el contenido que obtienes en ese momento.

> «Burp muestra la respuesta tal como llegó. El arreglo vacío explica por qué
> Karate marcó el caso como fallido aunque la solicitud terminó con HTTP 200».

Si quieres guardar también la evidencia manual, comprime la captura en ZIP.
En MotorDesk entra como Daniel, abre **Reportes → Subir evidencia** y selecciona
**Burp Community**. En **Observaciones** anota la respuesta que viste, su
impacto en el formulario y el siguiente paso.

## 5. Presenta el PDF a dirección

1. En **Reportes**, selecciona la ejecución y pulsa **Descargar PDF ejecutivo**.
   Una cuenta de **Gerencia** puede abrir y descargar el informe.
2. Empieza por el resumen y la gráfica. Explica cuántas operaciones comprobaste
   y cuántas requieren revisión.
3. Pasa al hallazgo. Enlaza **estado vacío** con **formulario sin opciones** y con
   una acción que el equipo puede tomar. Usa los códigos y la referencia técnica
   si alguien pide el detalle.

> «Probamos tres operaciones. Dos cumplieron los criterios. El catálogo llegó
> vacío y el formulario de clientes se quedó sin estados para seleccionar.
> El siguiente paso es revisar la carga de SEPOMEX en la base de datos y repetir
> la comprobación para confirmar que el formulario vuelve a ofrecer opciones».

## Preguntas que te pueden hacer

| Pregunta | Respuesta |
|---|---|
| ¿Por qué un 401 aparece como aprobado? | El caso pide que el servidor rechace una consulta sin sesión. El 401 cumple esa regla. |
| ¿Por qué un 200 puede fallar? | El 200 confirma que la consulta recibió una respuesta satisfactoria a nivel HTTP. Karate también comprueba que el contenido sirva para la operación. Aquí la lista estaba vacía. |
| ¿Por qué usar las dos herramientas? | Karate repite y evalúa casos con criterios definidos. Burp permite observar de forma manual la petición y la respuesta que explican un resultado. |
| ¿El catálogo vacío es una vulnerabilidad? | Este hallazgo afecta el registro de direcciones y se clasifica como un fallo funcional. La prueba de permisos se evalúa por separado. |
| ¿Cómo se comprueba la corrección? | Se repite el caso del catálogo, se confirma que la lista contiene estados y se comprueba el selector en el formulario real. |

Si el catálogo ya tiene datos el día de la clase, presenta el resultado nuevo
como evidencia de lo que funciona en ese momento. Usa el reporte histórico
para explicar el fallo documentado y mostrar qué cambió.
