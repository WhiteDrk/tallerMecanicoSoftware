Feature: Proteccion de la consulta de vehiculos

  Scenario: Rechazar una consulta sin autenticacion
    Given url 'http://localhost'
    And path 'api', 'v1', 'vehicles'
    And param criterion = 'ID'
    And param value = '00000000-0000-0000-0000-000000000000'
    When method get
    Then status 401
    And match response.code == 'UNAUTHENTICATED'
