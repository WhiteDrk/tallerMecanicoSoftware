Feature: Consulta autenticada de vehiculos en MotorDesk Railway

  Background:
    * configure connectTimeout = 5000
    * configure readTimeout = 10000
    * configure logging = { report: 'info', console: 'warn', pretty: false, mask: { headers: ['Authorization', 'Cookie'], jsonPaths: ['$.password', '$.accessToken'] } }
    * def maxMs = Number(java.lang.System.getenv('NUBE_MAX_MS') || 2000)
    * assert maxMs >= 100 && maxMs <= 10000

  @setup
  Scenario: Preparar un lote acotado de consultas
    * def total = Number(java.lang.System.getenv('NUBE_CASOS') || 10)
    * assert total >= 1 && total <= 200 && total % 1 == 0
    * def casos = karate.repeat(total, function(i) { return { numero: i + 1 }; })

  Scenario Outline: Consulta autorizada de vehiculo - caso <numero>
    Given url 'https://motordesk-pruebas.up.railway.app'
    And path 'api', 'v1', 'vehicles'
    And param criterion = 'VIN'
    And param value = '3KPF24AD3R2610131'
    And header Authorization = 'Bearer ' + java.lang.System.getenv('MOTORDESK_ACCESS_TOKEN')
    When method get
    Then status 200
    And match response.vin == '3KPF24AD3R2610131'
    And match response.workshopName == 'MotorDesk Pruebas Nube'
    * assert responseTime < maxMs

    Examples:
      | karate.setup().casos |
