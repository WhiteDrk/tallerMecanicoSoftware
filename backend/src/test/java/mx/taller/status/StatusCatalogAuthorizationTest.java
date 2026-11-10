package mx.taller.status;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
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

/** Exercises HTTP rules and method authorization together, without changing users or MySQL. */
@WebMvcTest(controllers = StatusCatalogController.class, properties = "app.cors.allowed-origins=http://localhost")
@Import(SecurityConfig.class)
class StatusCatalogAuthorizationTest {
  private static final String ACTOR = "00000000-0000-0000-0000-000000000001";
  private static final String PATH = "/api/v1/status-catalog";
  private static final StatusCatalogFacade.StatusResult RESULT =
    new StatusCatalogFacade.StatusResult(1L, "ACTIVO", "Cliente activo", true, true);

  @Autowired MockMvc mvc;
  @MockitoBean StatusCatalogFacade facade;
  @MockitoBean JwtService jwt;

  @ParameterizedTest
  @CsvSource({"ADMINISTRADOR,true", "MANAGER,true", "RECEPCIONISTA,false", "SECRETARIA,false",
    "CUSTOMER,false", "TREASURY,false", "MECHANIC,false", "MECHANIC_CHIEF,false", "AUDITOR,false"})
  void onlyAdministrationAndManagementCanListCreateAndUpdate(String role, boolean allowed) throws Exception {
    when(facade.list()).thenReturn(List.of(RESULT));
    when(facade.create(any(), eq(ACTOR), any())).thenReturn(RESULT);
    when(facade.update(eq(1L), any(), eq(ACTOR), any())).thenReturn(RESULT);
    mvc.perform(get(PATH).with(user(ACTOR).roles(role)))
      .andExpect(status().is(allowed ? 200 : 403));
    mvc.perform(post(PATH).with(user(ACTOR).roles(role)).contentType(MediaType.APPLICATION_JSON)
        .content("{\"value\":\"QA_ESTATUS\",\"description\":\"Prueba de autorización\"}"))
      .andExpect(status().is(allowed ? 201 : 403));
    mvc.perform(put(PATH + "/1").with(user(ACTOR).roles(role)).contentType(MediaType.APPLICATION_JSON)
        .content("{\"description\":\"Descripción actualizada\",\"enabled\":true}"))
      .andExpect(status().is(allowed ? 200 : 403));
    if (allowed) {
      verify(facade).list();
      verify(facade).create(any(), eq(ACTOR), any());
      verify(facade).update(eq(1L), any(), eq(ACTOR), any());
    } else { verifyNoInteractions(facade); }
  }

  @Test void anonymousRequestsAreRejectedBeforeReachingFacade() throws Exception {
    mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
        .content("{\"value\":\"QA_ESTATUS\",\"description\":\"Prueba\"}"))
      .andExpect(status().isUnauthorized());
    mvc.perform(put(PATH + "/1").contentType(MediaType.APPLICATION_JSON)
        .content("{\"description\":\"Prueba\",\"enabled\":true}"))
      .andExpect(status().isUnauthorized());
    verifyNoInteractions(facade);
  }

  @Test void physicalDeletionRemainsForbiddenForBothAllowedRoles() throws Exception {
    for (String role : List.of("ADMINISTRADOR", "MANAGER")) {
      mvc.perform(delete(PATH + "/1").with(user(ACTOR).roles(role))).andExpect(status().isForbidden());
    }
    verifyNoInteractions(facade);
  }
}
