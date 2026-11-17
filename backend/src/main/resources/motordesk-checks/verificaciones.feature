Feature: Verificaciones de MotorDesk

Background:
  * configure connectTimeout = 2000
  * configure readTimeout = 3000
  * configure followRedirects = false
  * configure logging = { mask: { headers: ['Authorization', 'Cookie'] } }
  * url portalApi

@disponibilidad
Scenario: Disponibilidad de la API
  Given path 'actuator', 'health'
  When method get
  Then assert responseStatus == 200
  And assert responseTime <= limiteMs
  And assert response.status == 'UP'

@reportes-privados
Scenario: Reportes requieren iniciar sesión
  Given path 'api', 'v1', 'test-reports'
  When method get
  Then assert responseStatus == 401
  And assert responseTime <= limiteMs
  And assert response.code == 'UNAUTHENTICATED'

@descarga-privada
Scenario: Descargar un reporte requiere iniciar sesión
  Given path 'api', 'v1', 'test-reports', reportePrueba, 'archive'
  When method get
  Then assert responseStatus == 401
  And assert responseTime <= limiteMs
  And assert response.code == 'UNAUTHENTICATED'

@catalogo
Scenario: Consulta del catálogo de estados
  Given path 'api', 'v1', 'address-catalog', 'states'
  And header Authorization = sesion
  When method get
  Then assert responseStatus == 200
  And assert responseTime <= limiteMs
  And assert Array.isArray(response) && response.length > 0 && typeof response[0].name == 'string'

@vehiculo
Scenario: Consulta de un vehículo por VIN
  Given path 'api', 'v1', 'vehicles'
  And params { criterion: 'VIN', value: '#(vinPrueba)' }
  And header Authorization = sesion
  When method get
  Then assert responseStatus == 200
  And assert responseTime <= limiteMs
  And assert response.vin == vinPrueba
