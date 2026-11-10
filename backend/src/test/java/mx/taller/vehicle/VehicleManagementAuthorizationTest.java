package mx.taller.vehicle;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
import mx.taller.config.SecurityConfig;
import mx.taller.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = VehicleManagementController.class, properties = "app.cors.allowed-origins=http://localhost")
@Import(SecurityConfig.class)
class VehicleManagementAuthorizationTest {
  @Autowired MockMvc mvc;
  @MockitoBean VehicleManagementFacade facade;
  @MockitoBean JwtService jwt;
  final UUID id = UUID.randomUUID();
  final String update = """
    {"vin":"3KPF24AD3R2610131","plate":"UI-2613","brandId":1,"modelId":2,
     "year":2024,"version":"EX","mileage":0,"serialNumber":"SERIE-2613","revision":0}
    """;
  final String deactivate = "{\"confirmationVin\":\"3KPF24AD3R2610131\",\"confirmed\":true,\"revision\":0}";

  @ParameterizedTest @CsvSource({"ADMINISTRADOR,200", "RECEPCIONISTA,200", "MANAGER,403", "SECRETARIA,403", "CUSTOMER,403", "TREASURY,403", "MECHANIC,403", "MECHANIC_CHIEF,403", "AUDITOR,403"})
  void restrictsBothOperations(String role, int expected) throws Exception {
    mvc.perform(put("/api/v1/vehicles/" + id).with(user("actor").roles(role)).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().is(expected));
    mvc.perform(post("/api/v1/vehicles/" + id + "/deactivation").with(user("actor").roles(role)).contentType(MediaType.APPLICATION_JSON).content(deactivate)).andExpect(status().is(expected));
    if (expected == 200) {
      verify(facade).update(eq(id), any(), eq("actor"), any());
      verify(facade).deactivate(eq(id), any(), eq("actor"), any());
    } else verifyNoInteractions(facade);
  }
  @Test void anonymousCannotModify() throws Exception {
    mvc.perform(put("/api/v1/vehicles/" + id).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/v1/vehicles/" + id + "/deactivation").contentType(MediaType.APPLICATION_JSON).content(deactivate)).andExpect(status().isUnauthorized());
    verifyNoInteractions(facade);
  }
  @Test void incompleteInvalidAndUnconfirmedRequestsNeverReachFacade() throws Exception {
    for (String body : new String[] {"{}", update.replace("\"mileage\":0", "\"mileage\":-1"), update.replace("\"version\":\"EX\"", "\"version\":\" \""), update.replace("\"revision\":0", "\"revision\":null"), update.replace("\"vin\":\"3KPF24AD3R2610131\"", "\"vin\":null")})
      mvc.perform(put("/api/v1/vehicles/" + id).with(user("actor").roles("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    for (String body : new String[] {"{}", deactivate.replace("true", "false"), deactivate.replace("true", "null"), deactivate.replace("\"revision\":0", "\"revision\":-1")})
      mvc.perform(post("/api/v1/vehicles/" + id + "/deactivation").with(user("actor").roles("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    verifyNoInteractions(facade);
  }
  @Test void missingOrForeignVehicleIs404AndRaceIs409() throws Exception {
    when(facade.update(any(), any(), any(), any())).thenThrow(new VehicleNotFoundException());
    mvc.perform(put("/api/v1/vehicles/" + id).with(user("actor").roles("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isNotFound());
    when(facade.deactivate(any(), any(), any(), any())).thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(Vehicle.class, id));
    mvc.perform(post("/api/v1/vehicles/" + id + "/deactivation").with(user("actor").roles("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content(deactivate)).andExpect(status().isConflict());
  }
}
