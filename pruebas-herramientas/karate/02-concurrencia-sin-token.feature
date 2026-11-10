Feature: Concurrencia sobre la proteccion de vehiculos en localhost

  Background:
    * configure connectTimeout = 3000
    * configure readTimeout = 5000
    * configure logging = { report: 'info', console: 'warn', pretty: false }
    * def maxMs = Number(java.lang.System.getenv('KARATE_MAX_MS') || 2000)
    * assert maxMs >= 1 && maxMs <= 30000

  @setup
  Scenario: Preparar un lote limitado de peticiones
    * def total = Number(java.lang.System.getenv('KARATE_CASOS') || 100)
    * assert total >= 1 && total <= 1000 && total % 1 == 0
    * def casos = karate.repeat(total, function(i) { return { numero: i + 1 }; })

  Scenario Outline: Consulta protegida sin token - caso <numero>
    Given url 'http://localhost'
    And path 'api', 'v1', 'vehicles'
    And param criterion = 'ID'
    And param value = '00000000-0000-0000-0000-000000000000'
    When method get
    Then status 401
    And match response.code == 'UNAUTHENTICATED'
    * def tiempoMs = responseTime
    * assert tiempoMs < maxMs

    Examples:
      | karate.setup().casos |
