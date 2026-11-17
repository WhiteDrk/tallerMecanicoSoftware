Feature: Comprobaciones del taller MotorDesk

  Background:
    * def base = 'https://motordesk-pruebas.up.railway.app'
    * def maxMs = 5000
    * configure connectTimeout = 5000
    * configure readTimeout = 10000
    * configure logging = { report: 'info', console: 'warn', mask: { headers: ['Authorization'], jsonPaths: ['$.password', '$.accessToken'] } }
    * def correo = java.lang.System.getenv('MOTORDESK_EMAIL')
    * def clave = java.lang.System.getenv('MOTORDESK_PASSWORD')
    * assert typeof correo == 'string' && correo.length > 0 && typeof clave == 'string' && clave.length > 0

    Given url base
    And path 'api', 'v1', 'auth', 'login'
    And header Content-Type = 'application/json'
    And request { email: '#(correo)', password: '#(clave)' }
    When method post
    Then status 200
    * def sesion = 'Bearer ' + response.accessToken

  Scenario: Listado de reportes sin sesión
    Given url base
    And path 'api', 'v1', 'test-reports'
    When method get
    Then status 401
    And match response.code == 'UNAUTHENTICATED'
    And assert responseTime <= maxMs

  Scenario: Consulta de vehículo de prueba
    Given url base
    And path 'api', 'v1', 'vehicles'
    And param criterion = 'VIN'
    And param value = '3KPF24AD3R2610131'
    And header Authorization = sesion
    When method get
    Then status 200
    And match response.vin == '3KPF24AD3R2610131'
    And assert responseTime <= maxMs

  Scenario: Catálogo de estados para registrar un cliente
    Given url base
    And path 'api', 'v1', 'address-catalog', 'states'
    And header Authorization = sesion
    When method get
    Then status 200
    And match response == '#array'
    And assert response.length > 0
    And match response[0].name == '#string'
    And assert responseTime <= maxMs
