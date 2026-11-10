package mx.taller.vehicle;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import mx.taller.config.SecurityConfig;
import mx.taller.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = VehicleQueryController.class, properties = "app.cors.allowed-origins=http://localhost")
@Import(SecurityConfig.class)
class VehicleQueryAuthorizationTest {
  @Autowired MockMvc mvc;
  @MockitoBean VehicleQueryFacade facade;
  @MockitoBean JwtService jwt;

  @ParameterizedTest
  @CsvSource({"ADMINISTRADOR,200", "RECEPCIONISTA,200", "MANAGER,403", "SECRETARIA,403", "CUSTOMER,403", "TREASURY,403", "MECHANIC,403", "MECHANIC_CHIEF,403", "AUDITOR,403"})
  void restrictsRolesInHttpAndController(String role, int expected) throws Exception {
    mvc.perform(get("/api/v1/vehicles").param("criterion", "PLATE").param("value", "UI-2613").with(user("actor").roles(role)))
      .andExpect(status().is(expected));
    if (expected == 200) verify(facade).find("actor", "PLATE", "UI-2613");
    else verifyNoInteractions(facade);
  }

  @Test void anonymousIsUnauthorized() throws Exception {
    mvc.perform(get("/api/v1/vehicles").param("criterion", "PLATE").param("value", "UI-2613"))
      .andExpect(status().isUnauthorized())
      .andExpect(content().encoding("UTF-8"))
      .andExpect(jsonPath("$.message").value("Autenticación requerida"));
    verifyNoInteractions(facade);
  }

  @Test void invalidTokenResponsePreservesAccents() throws Exception {
    when(jwt.parse("invalid")).thenThrow(new IllegalArgumentException("Invalid token"));
    mvc.perform(get("/api/v1/vehicles").param("criterion", "PLATE").param("value", "UI-2613")
      .header("Authorization", "Bearer invalid"))
      .andExpect(status().isUnauthorized())
      .andExpect(content().encoding("UTF-8"))
      .andExpect(jsonPath("$.message").value("Credenciales no válidas"));
    verifyNoInteractions(facade);
  }

  @Test void forbiddenResponsePreservesAccents() throws Exception {
    mvc.perform(get("/api/v1/vehicles").param("criterion", "PLATE").param("value", "UI-2613")
      .with(user("actor").roles("CUSTOMER")))
      .andExpect(status().isForbidden())
      .andExpect(content().encoding("UTF-8"))
      .andExpect(jsonPath("$.message").value("No tiene permisos para esta operación"));
    verifyNoInteractions(facade);
  }

  @Test void missingRepeatedOrAdditionalParametersAreRejected() throws Exception {
    mvc.perform(get("/api/v1/vehicles").with(user("actor").roles("ADMINISTRADOR"))).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/vehicles").param("criterion", "VIN", "PLATE").param("value", "UI-2613")
      .with(user("actor").roles("ADMINISTRADOR"))).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/vehicles").param("criterion", "PLATE").param("value", "UI-2613").param("workshopId", "foreign")
      .with(user("actor").roles("ADMINISTRADOR"))).andExpect(status().isBadRequest());
    verifyNoInteractions(facade);
  }

  @Test void absentOrForeignVehicleReturnsNotFound() throws Exception {
    when(facade.find(any(), any(), any())).thenThrow(new VehicleNotFoundException());
    mvc.perform(get("/api/v1/vehicles").param("criterion", "PLATE").param("value", "UI-2613")
      .with(user("actor").roles("ADMINISTRADOR"))).andExpect(status().isNotFound());
  }
}
