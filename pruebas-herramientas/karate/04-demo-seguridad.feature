Feature: Demostracion de autenticacion y validaciones en MotorDesk

  Background:
    * configure connectTimeout = 5000
    * configure readTimeout = 10000
    * configure logging = { report: 'info', console: 'warn', pretty: false, mask: { headers: ['Authorization', 'Cookie'], jsonPaths: ['$.password', '$.accessToken'] } }

  Scenario: Una cabecera de administrador no sustituye iniciar sesion
    Given url 'https://motordesk-pruebas.up.railway.app'
    And path 'api', 'v1', 'vehicles'
    And param criterion = 'VIN'
    And param value = '3KPF24AD3R2610131'
    And header X-Role = 'ADMINISTRADOR'
    When method get
    Then status 401
    And match response.code == 'UNAUTHENTICATED'

  Scenario Outline: <caso>
    Given url 'https://motordesk-pruebas.up.railway.app'
    And path 'api', 'v1', recurso
    And params consulta
    And header Authorization = credencial == 'inventada' ? 'Bearer token-inventado' : 'Bearer ' + java.lang.System.getenv(credencial)
    And header X-Role = 'ADMINISTRADOR'
    When method get
    Then status <estado>
    * assert codigo == 'NINGUNO' || response.code == codigo
    * assert estado != 200 || response.vin == '3KPF24AD3R2610131'

    Examples:
      | caso                                             | recurso      | consulta!                                                              | credencial                    | estado!| codigo                |
      | Consulta valida con la cuenta de recepcion        | vehicles     | { criterion: 'VIN', value: '3KPF24AD3R2610131' }                          | MOTORDESK_ACCESS_TOKEN        | 200    | NINGUNO               |
      | Un token inventado es rechazado                   | vehicles     | { criterion: 'VIN', value: '3KPF24AD3R2610131' }                          | inventada                     | 401    | INVALID_TOKEN         |
      | Escribir ADMINISTRADOR dentro del JWT no da acceso | test-reports | {}                                                                     | MOTORDESK_TAMPERED_TOKEN      | 401    | INVALID_TOKEN         |
      | La entrada de inyeccion no cumple el formato VIN   | vehicles     | { criterion: 'VIN', value: "' OR '1'='1" }                                | MOTORDESK_ACCESS_TOKEN        | 400    | INVALID_VEHICLE       |
      | Criterios duplicados son rechazados                | vehicles     | { criterion: ['VIN', 'PLATE'], value: '3KPF24AD3R2610131' }                | MOTORDESK_ACCESS_TOKEN        | 400    | INVALID_VEHICLE       |
      | X-Role administrador no eleva el permiso de Lucia  | test-reports | {}                                                                     | MOTORDESK_ACCESS_TOKEN        | 403    | FORBIDDEN             |
      | Un UUID inexistente no devuelve otros vehiculos    | vehicles     | { criterion: 'ID', value: '00000000-0000-0000-0000-000000000000' }         | MOTORDESK_ACCESS_TOKEN        | 404    | VEHICLE_NOT_FOUND     |
